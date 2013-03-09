package com.qozix.mapview;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.View;

import com.qozix.geom.Coordinate;
import com.qozix.layouts.ZoomPanLayout;
import com.qozix.mapview.geom.ManagedGeolocator;
import com.qozix.mapview.hotspots.HotSpotManager;
import com.qozix.mapview.markers.CalloutManager;
import com.qozix.mapview.markers.MarkerManager;
import com.qozix.mapview.paths.PathManager;
import com.qozix.mapview.tiles.TileManager;
import com.qozix.mapview.tiles.TileRenderListener;
import com.qozix.mapview.zoom.ZoomListener;
import com.qozix.mapview.zoom.ZoomManager;

/**
 * The MapView widget is a subclass of ViewGroup that provides a mechanism to asynchronously display tile-based images,
 * with additional functionality for 2D dragging, flinging, pinch or double-tap to zoom, adding overlaying Views (markers),
 * multiple levels of detail, and support for faux-geolocation (by specifying top-left and bottom-right coordinates).
 * 
 * <p>It might be best described as a hybrid of com.google.android.maps.MapView and iOS's CATiledLayer, and is appropriate for a variety of uses
 * but was intended for map-type applications, especially high-detail or custom implementations (e.g., inside a building).</p>
 * 
 * <p>A minimal implementation might look like this:</p>
 *  
 * <pre>{@code
 * MapView mapView = new MapView(this);
 * mapView.addZoomLevel(3090, 2536, "path/to/tiles/%col%-%row%.jpg");
 * }</pre>
 * 
 * A more advanced implementation might look like this:
 * <pre>{@code
 * MapView mapView = new MapView(this);
 * mapView.addMapEventListener(someMapEventListener);
 * mapView.registerGeolocator(42.379676, -71.094919, 42.346550, -71.040280);
 * mapView.addZoomLevel(6180, 5072, "tiles/boston-1000-%col%_%row%.jpg", 256, 256);
 * mapView.addZoomLevel(3090, 2536, "tiles/boston-500-%col%_%row%.jpg", 256, 256);
 * mapView.addZoomLevel(1540, 1268, "tiles/boston-250-%col%_%row%.jpg", 256, 256);
 * mapView.addZoomLevel(770, 634, "tiles/boston-125-%col%_%row%.jpg", 128, 128);
 * mapView.addMarker(someView, 42.35848, -71.063736);
 * mapView.addMarker(anotherView, 42.3665, -71.05224);
 * }</pre>
 * 
 * <p>Licensed under <a href="http://creativecommons.org/licenses/by/3.0/legalcode" target="_blank">Creative Commons</a></p>
 */
public class MapView extends ZoomPanLayout {

	private static final String TAG = MapView.class.getSimpleName();

	private HashSet<MapEventListener> mapEventListeners = new HashSet<MapEventListener>();
	
	private ZoomManager zoomManager = new ZoomManager();
	private HotSpotManager hotSpotManager = new HotSpotManager();
	
	private TileManager tileManager;
	private MarkerManager markerManager;
	private PathManager pathManager;
	private CalloutManager calloutManager;
	
	private ManagedGeolocator geolocator;
	private boolean isUsingGeolocation;
	
	/**
	 * Constructor to use when creating a MapView from code.  Inflating from XML is not currently supported.
	 * @param context (Context) The Context the MapView is running in, through which it can access the current theme, resources, etc.
	 */
	public MapView( Context context ) {
		
		super( context );
				
		tileManager = new TileManager( context, zoomManager );
		super.addChild( tileManager );		
		
		pathManager = new PathManager( context, zoomManager );
		super.addChild( pathManager );
		
		markerManager = new MarkerManager( context, zoomManager );
		super.addChild( markerManager );		
		
		calloutManager = new CalloutManager( context, zoomManager );
		super.addChild( calloutManager );

		updateClipFromCurrentZoom();
		
		zoomManager.addZoomListener( zoomListener );
		tileManager.setTileRenderListener( renderListener );
		
		addZoomPanListener( zoomPanListener );
		addGestureListener( gestureListener );
		
	}
   
	//------------------------------------------------------------------------------------
	// PUBLIC API
	//------------------------------------------------------------------------------------

	//------------------------------------------------------------------------------------
	// Getters/Setters
	//------------------------------------------------------------------------------------
	
	/**
	 * Get the current zoom level (index) used by the MapView
	 * @return (int) current zoom level used by the MapView
	 */
	public int getZoom(){
		return zoomManager.getZoom();
	}
	
	/**
	 * Changes the zoom level used by the MapView.
	 * The value is an index and represent the range 0 to n,
	 * where n = the total number of zoom levels added
	 * @param zoom (int) zoom index to use
	 */
	public void setZoom( int zoom ) {
		zoomManager.setZoom( zoom );
		setScale( zoomManager.getScale() );
	}
	
	/**
	 * Register a map event listener callback object for this MapView.
	 * @param listener (MapEventListener) an implementation of the MapEventListener interface
	 */
	public void addMapEventListener( MapEventListener listener ) {
		mapEventListeners.add( listener );
	}
	
	/**
	 * Removes a MapEventListener object from those listening to this MapView.
	 * @param listener (MapEventListener) an implementation of the MapEventListener interface
	 */
	public void removeMapEventListener( MapEventListener listener ) {
		mapEventListeners.remove( listener );
	}
	
	//------------------------------------------------------------------------------------
	// Rendering API
	//------------------------------------------------------------------------------------
	
	/**
	 * Request that the current tile set is re-examined and re-drawn.
	 * The request is added to a queue and is not guaranteed to be processed at any particular
	 * time, and will never be handled immediately.
	 */
	public void requestRender(){
		tileManager.requestRender();
	}
	
	/**
	 * Notify the map view that it may stop rendering tiles.  The rendering thread will be
	 * attempt to be interrupted, but no guarantee is provided when.
	 */
	public void cancelRender() {
		tileManager.cancelRender();
	}
	
	//------------------------------------------------------------------------------------
	// Setup API
	//------------------------------------------------------------------------------------
	
	/**
	 * Register a tile set to be used for a particular zoom level.
	 * Each tile set to be used must be registered using this method,
	 * and at least one zoom level must be registered for the MapView to render any tiles.
	 * Zoom levels will be ordered from smallest to largest, based on the area (width * height)
	 * 	 
	 * @param wide (int) total width of the tile set
	 * @param tall (int) total height of the tile set
	 * @param pattern (String) string path to the location of the map tiles (in the assets directory), with %col% and %row% to be replaced by their respective integer positions.  E.g., "folder/tile-%col%-%row%.jpg"
	 */
	public void addZoomLevel( int wide, int tall, String pattern ) {
		zoomManager.addZoomLevel(wide, tall, pattern );
	}

	/**
	 * Register a tile set to be used for a particular zoom level.
	 * Each tile set to be used must be registered using this method,
	 * and at least one zoom level must be registered for the MapView to render any tiles.
	 * Zoom levels will be ordered from smallest to largest, based on the area (width * height)
	 * 	 
	 * @param wide (int) total width of the tile set
	 * @param tall (int) total height of the tile set
	 * @param pattern (String) string path to the location of the map tiles (in the assets directory), with %col% and %row% to be replaced by their respective integer positions.  E.g., "folder/tile-%col%-%row%.jpg"
	 * @param tileWidth (int) size of each tiled column
	 * @param tileHeight (int) size of each tiled row
	 */
	public void addZoomLevel( int wide, int tall, String pattern, int tileWidth, int tileHeight ){
		zoomManager.addZoomLevel( wide, tall, pattern, tileWidth, tileHeight );
	}
	
	//------------------------------------------------------------------------------------
	// Geolocation API
	//------------------------------------------------------------------------------------
	
	/**
	 * Register a set of offset points to use when calculating position within the MapView.
	 * This can be used for lat/lng coordinates instead of pixel-based positioning.
	 * If a geolocation rectangle is specified, many positioning methods will be able to translate coordinates into
	 * relative pixel values.
	 * To remove this process, use unregisterGeolocator
	 * @param left (double) the left edge of the rectangle used when calculating position (longitude of the bottom-right coordinate)
	 * @param top (double) the top edge of the rectangle used when calculating position (latitude of the top-left coordinate)
	 * @param right (double) the right edge of the rectangle used when calculating position (longitude of the top-left coordinate)
	 * @param bottom (double) the bottom edge of the rectangle used when calculating position (latitude of the bottom-right coordinate)
	 */
	public void registerGeolocator(double left, double top, double right, double bottom ){
		isUsingGeolocation = true;
		if ( geolocator == null ) {
			geolocator = new ManagedGeolocator( zoomManager );
			isUsingGeolocation = true;
		}		
		geolocator.setCoordinates( left, top, right, bottom );
	}
	
	/**
	 * Removes the geolocation rectangle from this MapView's registry.  Positioning methods will use pixels values.
	 */
	public void unregisterGeolocator(){
		isUsingGeolocation = false;
		geolocator = null;
	}
	
	/**
	 * Converts pixels values to coordinates using the geolocation rectangle provided by @link #registerGeolocator(double, double, double, double)
	 * @throws UnsupportedOperationException if no geolocation rectangle has been previously registered.
	 * @param x (double) pixel value of the x-axis to be translated
	 * @param y (double) pixel value of the y-axis to be translated
	 * @return (double[]) a 2-element array of coordinates, with latitude at position 0 and longitude at position 1
	 */
	public double[] pixelsToLatLng( double x, double y ){
		if( !isUsingGeolocation ) {
			throw new UnsupportedOperationException("MapView requires that a geolocation rectangle is registered before it can translate pixels to Coordinates");
		}
		double[] position = new double[2];
		Point point = new Point( (int) x, (int) y );
		Coordinate coordinate = geolocator.translate( point );
		position[0] = coordinate.longitude;
		position[1] = coordinate.latitude;
		return position;
	}
	
	/**
	 * Converts pixels values to coordinates using the geolocation rectangle provided by @link #registerGeolocator(double, double, double, double)
	 * @throws UnsupportedOperationException if no geolocation rectangle has been previously registered.
	 * @param points (double[]) 2-element array of pixel values.  The x-value should be at position 0, and the y-value at position 1
	 * @return (double[]) a 2-element array of coordinates, with latitude at position 0 and longitude at position 1
	 */
	public double[] pixelsToLatLng( double[] points ) {
		return pixelsToLatLng( points[0], points[1] );
	}
	
	/**
	 * Converts coordinates values to pixels using the geolocation rectangle provided by @link #registerGeolocator(double, double, double, double)
	 * @throws UnsupportedOperationException if no geolocation rectangle has been previously registered.
	 * @param lat (double) latitude of the value to be translated to y-axis pixel
	 * @param lng (double) longitude of the value to be translated to x-axis pixel
	 * @return (double[]) a 2-element array of pixel positions, with x at position 0 and y at position 1
	 */
	public double[] latLngToPixels( double lat, double lng ){
		if( !isUsingGeolocation ) {
			throw new UnsupportedOperationException("MapView requires that a geolocation rectangle is registered before it can translate coordinates to pixels");
		}
		double[] position = new double[2];
		Coordinate coordinate = new Coordinate( lat, lng );
		Point point = geolocator.translate( coordinate );
		position[0] = point.x;
		position[1] = point.y;
		return position;
	}
	
	/**
	 * Converts coordinates values to pixels using the geolocation rectangle provided by @link #registerGeolocator(double, double, double, double)
	 * @throws UnsupportedOperationException if no geolocation rectangle has been previously registered.
	 * @param points (double[]) 2-element array of pixel values.  Latitude should be at position 0, and longitude at position 1
	 * @return (double[]) a 2-element array of pixel positions, with x at position 0 and y at position 1
	 */
	public double[] latLngToPixels( double[] coordinates ) {
		return latLngToPixels( coordinates[0], coordinates[1] );
	}
	
	//------------------------------------------------------------------------------------
	// Positioning API
	//------------------------------------------------------------------------------------
	
	/**
	 * Scroll the MapView to the x and y positions provided.
	 * This signature will use pixel values if no geolocator is registered, otherwise
	 * it will compute the coordinates based on the rectangle provided to the
	 * geolocation registration.  Equivalent to moveTo( x, y, false)
	 * @param x (double) x position the MapView should focus to
	 * @param y (double) y position the MapView should focus to
	 */
	public void moveTo( double x, double y ){
		moveTo( x, y, false );
	}
	
	/**
	 * Scroll the MapView to the x and y positions provided.
	 * This signature will use pixel values if no geolocator is registered or if @param
	 * absolute is false, otherwise it will compute the coordinates based on the
	 * rectangle provided to the geolocation registration.
	 * @param x (double) x position the MapView should focus to
	 * @param y (double) y position the MapView should focus to
	 * @param absolute (boolean) true to always use pixel values and omit geolocation translation
	 */
	public void moveTo( double x, double y, boolean absolute ) {
		int[] position = getPosition( x, y, absolute );
		Point point = new Point( position[0], position[1] );
		scrollToPoint( point );
	}
	
	/**
	 * Scroll the MapView to the x and y positions provided and center to the screen.
	 * This signature will use pixel values if no geolocator is registered, otherwise
	 * it will compute the coordinates based on the rectangle provided to the
	 * geolocation registration.  Equivalent to moveToAndCenter( x, y, false)
	 * @param x (double) x position the MapView should focus to
	 * @param y (double) y position the MapView should focus to
	 */
	public void moveToAndCenter( double x, double y ) {
		moveToAndCenter( x, y, false );
	}
	
	/**
	 * Scroll the MapView to the x and y positions provided and center to the screen.
	 * This signature will use pixel values if no geolocator is registered or if @param
	 * absolute is false, otherwise it will compute the coordinates based on the
	 * rectangle provided to the geolocation registration.
	 * @param x (double) x position the MapView should focus to
	 * @param y (double) y position the MapView should focus to
	 * @param absolute (boolean) true to always use pixel values and omit geolocation translation
	 */
	public void moveToAndCenter( double x, double y, boolean absolute ) {
		int[] position = getPosition( x, y, absolute );
		Point point = new Point( position[0], position[1] );
		scrollToAndCenter( point );
	}
	
	/**
	 * Scroll the MapView to the x and y positions provided, using scrolling animation.
	 * This signature will use pixel values if no geolocator is registered, otherwise
	 * it will compute the coordinates based on the rectangle provided to the
	 * geolocation registration.  Equivalent to slideTo( x, y, false)
	 * @param x (double) x position the MapView should focus to
	 * @param y (double) y position the MapView should focus to
	 */
	public void slideTo( double x, double y ){
		slideTo( x, y, false );
	}
	
	/**
	 * Scroll the MapView to the x and y positions provided, using scrolling animation.
	 * This signature will use pixel values if no geolocator is registered or if @param
	 * absolute is false, otherwise it will compute the coordinates based on the rectangle
	 * provided to the geolocation registration.
	 * @param x (double) x position the MapView should focus to
	 * @param y (double) y position the MapView should focus to
	 * @param absolute (boolean) true to always use pixel values and omit geolocation translation
	 */
	public void slideTo( double x, double y, boolean absolute ){
		int[] position = getPosition( x, y, absolute );
		Point point = new Point( position[0], position[1] );
		slideToPoint( point );
	}
	
	/**
	 * Scroll the MapView to the x and y positions provide and center to the screen,
	 * using scrolling animation. This signature will use pixel values if no geolocator 
	 * is registered otherwise it will compute the coordinates based on the rectangle
	 * provided to the geolocation registration.
	 * @param x (double) x position the MapView should focus to
	 * @param y (double) y position the MapView should focus to
	 */	
	public void slideToAndCenter( double x, double y ) {
		slideToAndCenter( x, y, false );
	}
	
	/**
	 * Scroll the MapView to the x and y positions provide and center to the screen,
	 * using scrolling animation. This signature will use pixel values if no geolocator 
	 * is registered or if @param absolute is false, otherwise it will compute the
	 * coordinates based on the rectangle provided to the geolocation registration.
	 * @param x (double) x position the MapView should focus to
	 * @param y (double) y position the MapView should focus to
	 * @param absolute (boolean) true to always use pixel values and omit geolocation translation
	 */	
	public void slideToAndCenter( double x, double y, boolean absolute ) {
		int[] position = getPosition( x, y, absolute );
		Point point = new Point( position[0], position[1] );
		slideToAndCenter( point );
	}
	
	//------------------------------------------------------------------------------------
	// Marker, Callout and HotSpot API
	//------------------------------------------------------------------------------------
	
	/**
	 * Markers added to this MapView will have anchor logic applied on the values provided here.
	 * E.g., setMarkerAnchorPoints(0.5f, 1.0f) will have markers centered horizontally, positioned
	 * vertically to a value equal to 0 - height.
	 * Note that individual markers can be assigned specific anchors - this method applies a default
	 * value to all markers added without specifying anchor values.
	 * @param anchorX (float) the x-axis position of a marker will be offset by a number equal to the negative width of the marker multiplied by this value 
	 * @param anchorY (float) the y-axis position of a marker will be offset by a number equal to the negative height of the marker multiplied by this value
	 */
	public void setMarkerAnchorPoints( float anchorX, float anchorY ) {
		markerManager.setAnchors( anchorX, anchorY );
	}
	
	/**
	 * Add a marker to the the MapView.  The marker can be any View.
	 * No LayoutParams are required; the View will be laid out using WRAP_CONTENT for both width and height, and positioned based on the parameters
	 * @param view (View) View instance to be added to the MapView
	 * @param x (double) x position the View instance should be positioned at
	 * @param y (double) y position the View instance should be positioned at
	 * @return (View) the View instance added to the MapView
	 */
	public View addMarker( View view, double x, double y ){
		return addMarker( view, x, y, false );
	}
	
	/**
	 * Add a marker to the the MapView.  The marker can be any View.
	 * No LayoutParams are required; the View will be laid out using WRAP_CONTENT for both width and height, and positioned based on the parameters
	 * @param view (View) View instance to be added to the MapView
	 * @param x (double) x position the View instance should be positioned at
	 * @param y (double) y position the View instance should be positioned at
	 * @param absolute (boolean) true to always use pixel values and omit geolocation translation
	 * @return (View) the View instance added to the MapView
	 */
	public View addMarker( View view, double x, double y, boolean absolute ){
		int[] position = getPosition( x, y, absolute );
		markerManager.addMarker( view, position[0], position[1] );
		return view;
	}
	
	/**
	 * Add a marker to the the MapView.  The marker can be any View.
	 * No LayoutParams are required; the View will be laid out using WRAP_CONTENT for both width and height, and positioned based on the parameters
	 * @param view (View) View instance to be added to the MapView
	 * @param x (double) x position the View instance should be positioned at
	 * @param y (double) y position the View instance should be positioned at
	 * @param aX (float) the x-axis position of a marker will be offset by a number equal to the negative width of the marker multiplied by this value 
	 * @param aY (float) the y-axis position of a marker will be offset by a number equal to the negative height of the marker multiplied by this value
	 * @return (View) the View instance added to the MapView
	 */
	public View addMarker( View view, double x, double y, float aX, float aY ){
		return addMarker( view, x, y, aX, aY, false );
	}
	
	/**
	 * Add a marker to the the MapView.  The marker can be any View.
	 * No LayoutParams are required; the View will be laid out using WRAP_CONTENT for both width and height, and positioned based on the parameters
	 * @param view (View) View instance to be added to the MapView
	 * @param x (double) x position the View instance should be positioned at
	 * @param y (double) y position the View instance should be positioned at
	 * @param aX (float) the x-axis position of a marker will be offset by a number equal to the negative width of the marker multiplied by this value 
	 * @param aY (float) the y-axis position of a marker will be offset by a number equal to the negative height of the marker multiplied by this value
	 * @param absolute (boolean) true to always use pixel values and omit geolocation translation
	 * @return (View) the View instance added to the MapView
	 */
	public View addMarker( View view, double x, double y, float aX, float aY, boolean absolute ){
		int[] position = getPosition( x, y, absolute );
		markerManager.addMarker( view, position[0], position[1], aX, aY );
		return view;
	}
	
	/**
	 * Add a marker to the the MapView.  The marker can be any View.
	 * No LayoutParams are required; the View will be laid out using WRAP_CONTENT for both width and height, and positioned based on the parameters
	 * This signature requires an additional parameter that represents the zoom level (int) that this marker should be shown at.
	 * At any other zoom level, the marker View will have it's visibility set to View.NONE
	 * @param view (View) View instance to be added to the MapView
	 * @param x (double) x position the View instance should be positioned at
	 * @param y (double) y position the View instance should be positioned at
	 * @param zoom (int) the zoom level (index) that this view should be shown at
	 * @return (View) the View instance added to the MapView
	 */
	public View addMarkerAtZoom( View view, double x, double y, int zoom ){
		return addMarkerAtZoom( view, x, y, zoom, false );
	}
	
	/**
	 * Add a marker to the the MapView.  The marker can be any View.
	 * No LayoutParams are required; the View will be laid out using WRAP_CONTENT for both width and height, and positioned based on the parameters
	 * This signature requires an additional parameter that represents the zoom level (int) that this marker should be shown at.
	 * At any other zoom level, the marker View will have it's visibility set to View.NONE
	 * @param view (View) View instance to be added to the MapView
	 * @param x (double) x position the View instance should be positioned at
	 * @param y (double) y position the View instance should be positioned at
	 * @param zoom (int) the zoom level (index) that this view should be shown at
	 * @param absolute (boolean) true to always use pixel values and omit geolocation translation
	 * @return (View) the View instance added to the MapView
	 */
	public View addMarkerAtZoom( View view, double x, double y, int zoom, boolean absolute ){
		int[] position = getPosition( x, y, absolute );
		markerManager.addMarkerAtZoom( view, position[0], position[1], zoom );
		return view;
	}
	
	/**
	 * Add a marker to the the MapView.  The marker can be any View.
	 * No LayoutParams are required; the View will be laid out using WRAP_CONTENT for both width and height, and positioned based on the parameters
	 * This signature requires an additional parameter that represents the zoom level (int) that this marker should be shown at.
	 * At any other zoom level, the marker View will have it's visibility set to View.NONE
	 * @param view (View) View instance to be added to the MapView
	 * @param x (double) x position the View instance should be positioned at
	 * @param y (double) y position the View instance should be positioned at
	 * @param aX (float) the x-axis position of a marker will be offset by a number equal to the negative width of the marker multiplied by this value 
	 * @param aY (float) the y-axis position of a marker will be offset by a number equal to the negative height of the marker multiplied by this value
	 * @param zoom (int) the zoom level (index) that this view should be shown at
	 * @return (View) the View instance added to the MapView
	 */
	public View addMarkerAtZoom( View view, double x, double y, float aX, float aY, int zoom ){
		return addMarkerAtZoom( view, x, y, aX, aY, zoom, false );
	}	
	
	/**
	 * Add a marker to the the MapView.  The marker can be any View.
	 * No LayoutParams are required; the View will be laid out using WRAP_CONTENT for both width and height, and positioned based on the parameters
	 * This signature requires an additional parameter that represents the zoom level (int) that this marker should be shown at.
	 * At any other zoom level, the marker View will have it's visibility set to View.NONE
	 * @param view (View) View instance to be added to the MapView
	 * @param x (double) x position the View instance should be positioned at
	 * @param y (double) y position the View instance should be positioned at
	 * @param aX (float) the x-axis position of a marker will be offset by a number equal to the negative width of the marker multiplied by this value 
	 * @param aY (float) the y-axis position of a marker will be offset by a number equal to the negative height of the marker multiplied by this value
	 * @param zoom (int) the zoom level (index) that this view should be shown at
	 * @param absolute (boolean) true to always use pixel values and omit geolocation translation
	 * @return (View) the View instance added to the MapView
	 */
	public View addMarkerAtZoom( View view, double x, double y, float aX, float aY, int zoom, boolean absolute ){
		int[] position = getPosition( x, y, absolute );
		markerManager.addMarkerAtZoom( view, position[0], position[1], aX, aY, zoom );
		return view;
	}

	/**
	 * Removes a marker View from the MapView's view tree.
	 * @param view The marker View to be removed.
	 * @return (boolean) true if the view was in the view tree and was removed, false if it was not in the view tree
	 */
	public boolean removeMarker( View view ) {
		if( markerManager.indexOfChild( view ) > -1 ) {
			markerManager.removeView( view );
			return true;
		}
		return false;
	}
	
	/**
	 * Add a callout to the the MapView.  The callout can be any View.
	 * No LayoutParams are required; the View will be laid out using WRAP_CONTENT for both width and height, and positioned based on the parameters
	 * Callout views will always be positioned at the top of the view tree (at the highest z-index), and will always be removed during any touch event
	 * that is not consumed by the callout View.
	 * @param view (View) View instance to be added to the MapView
	 * @param x (double) x position the View instance should be positioned at
	 * @param y (double) y position the View instance should be positioned at
	 * @return (View) the View instance added to the MapView
	 */
	public View addCallout( View view, double x, double y ){
		return addCallout( view, x, y, false );
	}
	
	/**
	 * Add a callout to the the MapView.  The callout can be any View.
	 * No LayoutParams are required; the View will be laid out using WRAP_CONTENT for both width and height, and positioned based on the parameters
	 * Callout views will always be positioned at the top of the view tree (at the highest z-index), and will always be removed during any touch event
	 * that is not consumed by the callout View.
	 * @param view (View) View instance to be added to the MapView
	 * @param x (double) x position the View instance should be positioned at
	 * @param y (double) y position the View instance should be positioned at
	 * @param absolute (boolean) true to always use pixel values and omit geolocation translation
	 * @return (View) the View instance added to the MapView
	 */
	public View addCallout( View view, double x, double y, boolean absolute ){
		int[] position = getPosition( x, y, absolute );
		calloutManager.addMarker( view, position[0], position[1] );
		return view;
	}
	
	/**
	 * Add a callout to the the MapView.  The callout can be any View.
	 * No LayoutParams are required; the View will be laid out using WRAP_CONTENT for both width and height, and positioned based on the parameters
	 * Callout views will always be positioned at the top of the view tree (at the highest z-index), and will always be removed during any touch event
	 * that is not consumed by the callout View.
	 * @param view (View) View instance to be added to the MapView
	 * @param x (double) x position the View instance should be positioned at
	 * @param y (double) y position the View instance should be positioned at
	 * @param aX (float) the x-axis position of a callout view will be offset by a number equal to the negative width of the callout view multiplied by this value 
	 * @param aY (float) the y-axis position of a callout view will be offset by a number equal to the negative height of the callout view multiplied by this value
	 * @return (View) the View instance added to the MapView
	 */
	public View addCallout( View view, double x, double y, float aX, float aY ){
		return addCallout( view, x, y, aX, aY, false );
	}
	
	/**
	 * Add a callout to the the MapView.  The callout can be any View.
	 * No LayoutParams are required; the View will be laid out using WRAP_CONTENT for both width and height, and positioned based on the parameters
	 * Callout views will always be positioned at the top of the view tree (at the highest z-index), and will always be removed during any touch event
	 * that is not consumed by the callout View.
	 * @param view (View) View instance to be added to the MapView
	 * @param x (double) x position the View instance should be positioned at
	 * @param y (double) y position the View instance should be positioned at
	 * @param aX (float) the x-axis position of a callout view will be offset by a number equal to the negative width of the callout view multiplied by this value 
	 * @param aY (float) the y-axis position of a callout view will be offset by a number equal to the negative height of the callout view multiplied by this value
	 * @param absolute (boolean) true to always use pixel values and omit geolocation translation
	 * @return (View) the View instance added to the MapView
	 */
	public View addCallout( View view, double x, double y, float aX, float aY, boolean absolute ){
		int[] position = getPosition( x, y, absolute );
		calloutManager.addMarker( view, position[0], position[1], aX, aY );
		return view;
	}
	
	/**
	 * Removes a callout View from the MapView's view tree.
	 * @param view The callout View to be removed.
	 * @return (boolean) true if the view was in the view tree and was removed, false if it was not in the view tree
	 */
	public boolean removeCallout( View view ) {
		if( calloutManager.indexOfChild( view ) > -1 ) {
			calloutManager.removeView( view );
			return true;
		}
		return false;
	}
	
	/**
	 * Register a rectangle that should fire an OnClickListener when a touch event occurs that intersects that rectangle.
	 * The rectangle moves and scales with the map view.
	 * Note that while the second parameter takes a View.OnClickListener instance, the View instance passed to the OnClick method
	 * will be null, since the HotSpot is not an actual View but an abstract representation of one.
	 * @param rectangle (Rect) the rectangle that is tested against touch events that occur on the MapView
	 * @param listener (View.OnClickListener) the OnClickListener instance that is fired if the rectangle intersects a touch event
	 */
	public void addHotSpot( Rect rectangle, View.OnClickListener listener ){
		hotSpotManager.addHotSpot( rectangle, listener );
	}
	
	/**
	 * Remove a hotspot registered with addHotSpot
	 * @param rectangle (Rect) the rectangle to test for
	 * @param listener (View.OnClickListener) the listener that was registered to this hotspot
	 * @return (boolean) true if a hotspot was removed, false if not
	 */
	public void removeCallout( Rect rectangle, View.OnClickListener listener ) {
		hotSpotManager.removeHotSpot( rectangle, listener );
	}
	
	//------------------------------------------------------------------------------------
	// Marker, Callout and HotSpot API
	//------------------------------------------------------------------------------------
	
	/**
	 * Draw a path (line) on the MapView that will be rendered beneath any Marker or Callout views.
	 * @param positions (List<double[]>) List of 2-element double arrays.  Each element represents a position; each position represents a point.
	 * @return (View) the PathView instance that represents the line drawn.  The PathView instance has methods that allow manipulation of the
	 * path width, color, shadow, and corner effect, but is not documented at this time.
	 */
	public View drawPath( List<double[]> positions ) {
		LinkedList<Point> points = getPath( positions );
		return pathManager.drawPath( points );
	}
	/**
	 * Draw a path (line) on the MapView that will be rendered beneath any Marker or Callout views.
	 * @param positions (double[]...) array (or var-args) of 2-element double arrays.  Each element or argument represents a position; each position represents a point.
	 * @return (View) the PathView instance that represents the line drawn.  The PathView instance has methods that allow manipulation of the
	 * path width, color, shadow, and corner effect, but is not documented at this time.
	 */
	public View drawPath(double[]... positions ) {
		return drawPath( Arrays.asList( positions ));
	}
	
	/**
	 * Draw a path (line) on the MapView that will be rendered beneath any Marker or Callout views.
	 * This signature requires an additional parameter that represents the zoom level (int) that this path should be shown at.
	 * At any other zoom level, the path will not be displayed.
	 * @param zoom (int) the zoom level (index) that this view should be shown at
	 * @param positions (List<double[]>) List of 2-element double arrays.  Each element represents a position; each position represents a point.
	 * @return (View) the PathView instance that represents the line drawn.  The PathView instance has methods that allow manipulation of the
	 * path width, color, shadow, and corner effect, but is not documented at this time.
	 */
	public View drawPathAtZoom( int zoom, List<double[]> positions) {
		LinkedList<Point> points = getPath( positions );
		return pathManager.drawPathAtZoom( points, zoom );
	}
	
	/**
	 * Draw a path (line) on the MapView that will be rendered beneath any Marker or Callout views.
	 * This signature requires an additional parameter that represents the zoom level (int) that this path should be shown at.
	 * At any other zoom level, the path will not be displayed.
	 * @param zoom (int) the zoom level (index) that this view should be shown at
	 * @param positions (double[]...) array (or var-args) of 2-element double arrays.  Each element or argument represents a position; each position represents a point.
	 * @return (View) the PathView instance that represents the line drawn.  The PathView instance has methods that allow manipulation of the
	 * path width, color, shadow, and corner effect, but is not documented at this time.
	 */
	public View drawPathAtZoom(int zoom, double[]... positions ){
		return drawPathAtZoom( zoom, Arrays.asList( positions ) );
	}
	
	/**
	 * Removes a path View from the MapView's view tree.
	 * @param view The path View to be removed.
	 * @return (boolean) true if the view was in the view tree and was removed, false if it was not in the view tree
	 */
	public boolean removePath( View view ) {
		if( pathManager.indexOfChild( view ) > -1 ) {
			pathManager.removeView( view );
			return true;
		}
		return false;
	}
	

	//------------------------------------------------------------------------------------
	// PRIVATE API
	//------------------------------------------------------------------------------------
	
	/**
	 * @throws UnsupportedOperationException MapView does not allow direct insertion of child views. 
	 */
	@Override
	public void addChild( View view ) {
		throw new UnsupportedOperationException( "MapView does not allow direct insertion of child views." );
	}

	// make sure we keep the viewport UTD, and if layout changes we'll need to recompute what tiles to show
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout( changed, l, t, r, b );
		if ( changed ) {
			updateViewport();
			requestRender();
		}
	}	

	// set clipping bounds to the scaled size of the current zoom level
	private void updateClipFromCurrentZoom(){
		int w = zoomManager.getComputedCurrentWidth();
		int h = zoomManager.getComputedCurrentHeight();
		setSize( w, h );
	}
	
	// let the zoom manager know what tiles to show based on our position and dimensions
	private void updateViewport(){
		int left = getScrollX();
		int top = getScrollY();
		int right = left + getWidth();
		int bottom = top + getHeight();
		zoomManager.updateViewport( left, top, right, bottom );
	}   
	
	// tell the tile renderer to not start any more tasks, but it can continue with any that are already running
	private void suppressRender() {
		tileManager.suppressRender();
	}

	// private geolocation helper
	private int[] getPosition( double x, double y, boolean absolute ){
		int[] position = new int[2];
		if ( !absolute && isUsingGeolocation ){
			Coordinate c = new Coordinate( x, y );
			Point p = geolocator.translate( c );
			x = p.x;
			y = p.y;
		}
		position[0] = (int) x;
		position[1] = (int) y;
		return position;
	}
	
	// private geolocation helper
	private LinkedList<Point> getPath( List<double[]> positions ) {
		LinkedList<Point> points = new LinkedList<Point>();
		if( isUsingGeolocation ) {
			for( double[] coordinates : positions ) {
				Coordinate coordinate = new Coordinate( coordinates[0], coordinates[1] );
				Point point = geolocator.translate( coordinate );
				points.add( point );
			}
		} else {
			for( double[] pixels : positions ) {
				Point point = new Point( (int) pixels[0], (int) pixels[1] );
				points.add( point );
			}
		}
		return points;
	}
	
	//------------------------------------------------------------------------------------
	// Private Listeners
	//------------------------------------------------------------------------------------

	private ZoomPanListener zoomPanListener = new ZoomPanListener() {
		@Override
		public void onZoomPanEvent(){
			
		}						
		@Override
		public void onScrollChanged( int x, int y ) {
			updateViewport();
			for ( MapEventListener listener : mapEventListeners ) {
				listener.onScrollChanged( x, y );
			}
		}			
		@Override
		public void onScaleChanged( double scale ) { 
			zoomManager.setScale( scale );
			for ( MapEventListener listener : mapEventListeners ) {
				listener.onScaleChanged( scale );
			}
		}
		@Override
		public void onZoomStart( double scale ) {
			zoomManager.lockZoom();
			zoomManager.setScale( scale );
			for ( MapEventListener listener : mapEventListeners ) {
				listener.onZoomStart( scale );
			}
		}
		@Override
		public void onZoomComplete( double scale ) {
			zoomManager.unlockZoom();
			zoomManager.setScale( scale );
			requestRender();  // put this here instead of gesture listener so we catch animations and pinches
			for ( MapEventListener listener : mapEventListeners ) {
				listener.onZoomComplete( scale );
			}
		}
	};
	
	private ZoomListener zoomListener = new ZoomListener(){
		@Override
		public void onZoomLevelChanged( int oldZoom, int currentZoom ) {
			updateClipFromCurrentZoom();
			requestRender();
			for ( MapEventListener listener : mapEventListeners ) {
				listener.onZoomLevelChanged( oldZoom, currentZoom );
			}
		}
		/*
		 * do *not* update scale in response to changes in the zoom manager
		 * transactions are one-way - set scale on MapView (ZoomPanLayout)
		 * and pass those to ZoomManager, which then distributes, manages
		 * and notifies all other interested parties.
		 */
		@Override
		public void onZoomScaleChanged( double scale ) {
			
		}
	};
	
	private GestureListener gestureListener = new GestureListener(){

		@Override
		public void onDoubleTap( Point point ) {
			for ( MapEventListener listener : mapEventListeners ) {
				listener.onDoubleTap( point.x, point.y );
			}		
		}
		@Override
		public void onDrag( Point point ) {
			suppressRender();
			for ( MapEventListener listener : mapEventListeners ) {
				listener.onDrag( point.x, point.y );
			}
		}
		@Override
		public void onFingerDown( Point point ) {
			suppressRender();
			for ( MapEventListener listener : mapEventListeners ) {
				listener.onFingerDown( point.x, point.y );
			}
		}
		@Override
		public void onFingerUp( Point point ) {
			if ( !isFlinging() ) {
				requestRender();
			}
			for ( MapEventListener listener : mapEventListeners ) {
				listener.onFingerUp( point.x, point.y );
			}
		}
		@Override
		public void onFling( Point startPoint, Point finalPoint ) {
			suppressRender();
			for ( MapEventListener listener : mapEventListeners ) {
				listener.onFling( startPoint.x, startPoint.y, finalPoint.x, finalPoint.y );
			}
		}
		@Override
		public void onFlingComplete() {
			requestRender();
			for ( MapEventListener listener : mapEventListeners ) {
				listener.onFlingComplete();
			}
		}
		@Override
		public void onPinch( Point point ) {
			suppressRender();
			for ( MapEventListener listener : mapEventListeners ) {
				listener.onPinch( point.x, point.y );
			}
		}
		@Override
		public void onPinchComplete( Point point ) {
			requestRender();
			for ( MapEventListener listener : mapEventListeners ) {
				listener.onPinchComplete( point.x, point.y );
			}
		}
		@Override
		public void onPinchStart( Point point ) {
			suppressRender();
			for ( MapEventListener listener : mapEventListeners ) {
				listener.onPinchStart( point.x, point.y );
			}
		}
		@Override
		public void onTap( Point point ) {
			Point scaledPoint = new Point();
			scaledPoint.x = (int) ( point.x / getScale() );
			scaledPoint.y = (int) ( point.y / getScale() );
			hotSpotManager.processHit( scaledPoint );
			for ( MapEventListener listener : mapEventListeners ) {
				listener.onTap( point.x, point.y );
			}
		}
	};
	
	private TileRenderListener renderListener = new TileRenderListener(){
		@Override
		public void onRenderCancelled() {
			
		}
		@Override
		public void onRenderComplete() {
			for ( MapEventListener listener : mapEventListeners ) {
				listener.onRenderComplete();
			}
		}
		@Override
		public void onRenderStart() {
			for ( MapEventListener listener : mapEventListeners ) {
				listener.onRenderStart();
			}
		}		
	};
	
	//------------------------------------------------------------------------------------
	// Public static interfaces and classes
	//------------------------------------------------------------------------------------
	
	public static interface MapEventListener {
		/**
		 * Fires when a ACTION_DOWN event is raised from the MapView
		 * @param x (int) the x position of the event
		 * @param y (int) the y position of the event
		 */
		public void onFingerDown( int x, int y );
		/**
		 * Fires when a ACTION_UP event is raised from the MapView
		 * @param x (int) the x position of the event
		 * @param y (int) the y position of the event
		 */
		public void onFingerUp( int x, int y );
		/**
		 * Fires while the MapView is being dragged
		 * @param x (int) the x position of the event
		 * @param y (int) the y position of the event
		 */
		public void onDrag( int x, int y );
		/**
		 * Fires when a user double-taps the MapView
		 * @param x (int) the x position of the event
		 * @param y (int) the y position of the event
		 */
		public void onDoubleTap( int x, int y );
		/**
		 * Fires when a user taps the MapView
		 * @param x (int) the x position of the event
		 * @param y (int) the y position of the event
		 */
		public void onTap( int x, int y );
		/**
		 * Fires while a user is pinching the MapView
		 * @param x (int) the x position of the event
		 * @param y (int) the y position of the event
		 */
		public void onPinch( int x, int y );
		/**
		 * Fires when a user starts a pinch action
		 * @param x (int) the x position of the event
		 * @param y (int) the y position of the event
		 */
		public void onPinchStart( int x, int y );
		/**
		 * Fires when a user completes a pinch action
		 * @param x (int) the x position of the event
		 * @param y (int) the y position of the event
		 */
		public void onPinchComplete( int x, int y );
		/**
		 * Fires when a user initiates a fling action
		 * @param sx (int) the x position of the start of the fling
		 * @param sy (int) the y position of the start of the fling
		 * @param dx (int) the x position of the end of the fling
		 * @param dy (int) the y position of the end of the fling
		 */
		public void onFling( int sx, int sy, int dx, int dy );
		/**
		 * Fires when a fling action has completed
		 */
		public void onFlingComplete();
		/**
		 * Fires when the MapView's scale has updated
		 * @param scale (double) the new scale of the MapView (0-1)
		 */
		public void onScaleChanged( double scale );
		/**
		 * Fires when the MapView's scroll position has updated
		 * @param x (int) the new x scroll position of the MapView
		 * @param y (int) the new y scroll position of the MapView
		 */
		public void onScrollChanged( int x, int y );
		/**
		 * Fires when a zoom action starts (typically through a pinch of double-tap action,
		 * or by programmatic animated zoom methods.
		 * @param scale (double) the new scale of the MapView (0-1)
		 */
		public void onZoomStart( double scale );
		/**
		 * Fires when a zoom action ends (typically through a pinch of double-tap action,
		 * or by programmatic animated zoom methods.
		 * @param scale (double) the new scale of the MapView (0-1)
		 */
		public void onZoomComplete( double scale );
		/**
		 * Fires when the MapView should start using a new ZoomLevel
		 * @param oldZoom (int) the zoom level the MapView was using before the change
		 * @param currentZoom (int) the zoom level the MapView has changed to
		 */
		public void onZoomLevelChanged( int oldZoom, int currentZoom );
		/**
		 * Fires when the rendering thread has started to update the visible tiles.
		 */
		public void onRenderStart();
		/**
		 * Fires when the rendering thread has completed updating the visible tiles, but before cleanup
		 */
		public void onRenderComplete();
	}

}
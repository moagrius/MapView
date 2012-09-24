package com.qozix.map;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;

import com.qozix.map.geom.Coordinate;
import com.qozix.map.geom.Geolocator;
import com.qozix.map.geom.XPoint;
import com.qozix.map.hotspots.HotSpot;
import com.qozix.map.layers.MarkerLayer;
import com.qozix.map.layers.PathLayer;
import com.qozix.map.layers.TileLayer;
import com.qozix.map.layers.TouchLayer;
import com.qozix.map.layouts.FixedLayout;
import com.qozix.map.listeners.TouchLayerEventListener;
import com.qozix.map.zoom.ZoomLevel;
import com.qozix.map.zoom.ZoomManager;
import com.qozix.map.zoom.ZoomSet;


/**
 * The MapView widget is a subclass of ViewGroup that provides a mechanism to asynchronously display tile-based images,
 * with additional functionality for 2D dragging, flinging, pinch or double-tap to zoom, adding overlaying Views (markers),
 * multiple levels of detail, and support for faux-geolocation (by specifying top-left and bottom-right coordinates).
 * 
 * <p>It might be best described as a hybrid of com.google.android.maps.MapView and iOS's CATiledLayer, and is appropriate for a variety of uses
 * but was intended for map-type applications, especially high-detail or custom implementations (e.g., inside a building).</p>
 * 
 * <p>Configuration can be very simple, but most configuration options can be set with instance methods.</p>
 * 
 * <p>A minimal implementation might look like this:</p>
 *  
 * <pre>MapView mapView = new MapView(this);
mapView.setBaseMapSize(1440, 900);
mapView.addZoomLevel("path/to/tiles/%col%-%row%.jpg");
mapView.initialize();</pre>
 * 
 * A more advanced implementation might look like this:
 * <pre>MapView mapView = new MapView(this);
mapView.addOnReadyRunnable(someRunnable);
mapView.addOnReadyRunnable(anotherRunnable);
mapView.setTileTransitionDuration(1000);
mapView.setMapEventListener(someMapEventListener);
mapView.registerGeolocator(42.379676, -71.094919, 42.346550, -71.040280);
mapView.addZoomLevel(6180, 5072, "tiles/boston-1000-%col%_%row%.jpg","downsamples/boston-pedestrian.jpg", 256, 256);
mapView.addZoomLevel(3090, 2536, "tiles/boston-500-%col%_%row%.jpg", "downsamples/boston-overview.jpg", 256, 256);
mapView.addZoomLevel(1540, 1268, "tiles/boston-250-%col%_%row%.jpg", "downsamples/boston-overview.jpg", 256, 256);
mapView.addZoomLevel(770, 634, "tiles/boston-125-%col%_%row%.jpg", "downsamples/boston-overview.jpg", 128, 128);
mapView.initialize();
mapView.addMarker(someView, 42.35848, -71.063736);
mapView.addMarker(anotherView, 42.3665, -71.05224);
</pre>

 * <p>Licensed under <a href="http://creativecommons.org/licenses/by/3.0/legalcode" target="_blank">Creative Commons</a></p>
 */

public class MapView extends ViewGroup {

	private static final int RENDER = 1;
	
	private ZoomManager zoomManager;
	
	private Geolocator geolocator;
	
	private TouchLayer touchLayer = null;
	private TileLayer tileLayer = null;
	private PathLayer pathLayer = null;
	private MarkerLayer markerLayer = null; 
	
	private Rect visible = new Rect();
	
	private float actualScale = 1.0f;  // actual scale between 0 and 1, vs largest possible size
	private float computedScale = 1.0f;  // scale for a particular zoom level
	private float relativeScale = 1.0f;   // scale to apply to a zoom level, so a 0.3 actual scale at the second largest zoom level would be 0.6
	
	private boolean isReady = false;
	private ArrayList<Runnable> onReadyListeners = new ArrayList<Runnable>();
	
	private double maxScale = 1.0;
	private double minScale = 1.0;
	private boolean userDefinedMinScale = false;  // if user hasn't defined a min scale, figure out the smallest scale that shows the whole map (no background bleed)...  maybe move this to just check if ZoomManager has 1 level
	private boolean initiallyScaleToFit = false;
	
	private int layoutPasses = 0;  // onLayout should be invoked once per child before onReady runnables are invoked
	
	private boolean shouldSuppressTileSetUpdates = false;  // if true, don't change tile sets
	
	private TouchLayerEventListener mapEventListener = null;
	
	private ArrayList<HotSpot> hotSpots = new ArrayList<HotSpot>();
	private ArrayList<HotSpot> qualifiedHotSpots = new ArrayList<HotSpot>();
	
	/**
	 * Constructor to use when creating a MapView from code.  Inflating from XML is not currently supported.
	 * @param context (Context) The Context the MapView is running in, through which it can access the current theme, resources, etc.
	 */
	public MapView(Context context) {
		
		super(context);
		
		zoomManager = new ZoomManager();
		
    	touchLayer = new TouchLayer(context);
    	touchLayer.setGestureListener(touchLayerListener);
    	addView(touchLayer);
    	
    	tileLayer = new TileLayer(context);
    	touchLayer.addLayer(tileLayer);    	
    	
    	pathLayer = new PathLayer(context);
    	touchLayer.addLayer(pathLayer);
    	
    	markerLayer = new MarkerLayer(context);
    	touchLayer.addLayer(markerLayer);
		
    	requestUpdate();
		
	}
	
	/**
	 * Set the maximum possible scale for the tiles.
	 * default is 1
	 * values greater than 1 will scale image tiles up and are likely to introduce distortion
	 * @param m (double) the maximum scale value (1 = 100%, 2 = 200%, 0.5 = 50%)
	 */
	public void setMaxScale(double m){
		maxScale = m;
		if(touchLayer != null){
			touchLayer.setScaleLimits(minScale, maxScale);
		}
	}
	
	/**
	 * Set the minimum possible scale for the tiles.
	 * default is 1, but is only considered if a minimum scale has been explicitly set,
	 * otherwise the minimum scale will be computed based on the available dimensions of the MapView, so that no bleed shows through
	 * @param m (double) the minimum scale value (1 = 100%, 2 = 200%, 0.5 = 50%)
	 */
	public void setMinScale(double m){
		minScale = m;
		userDefinedMinScale = true;
		if(touchLayer != null){
			touchLayer.setScaleLimits(minScale, maxScale);
		}
	}
	
	/**
	 * If this method is invoked with a parameter of boolean true, before initialization
	 * the MapView tileset will be created scaled to fit the available dimensions of the containing MapView 
	 * @param value (boolean) true to have the MapView render initially within the available dimensions of the containing MapView
	 */
	public void setScaleToFit(boolean value){
		initiallyScaleToFit = value;
	}
	
	/**
	 * Markers added to this MapView will have anchor logic applied on the values provided here.
	 * E.g., setMarkerAnchorPoints(0.5f, 1.0f) will have markers centered horizontally and vertically above it's anchor (Point)
	 * @param x (float) the x-axis position of a marker will be adjusted by a number equal to the negative width of the marker multiplied by this value 
	 * @param y (float) the y-axis position of a marker will be adjusted by a number equal to the negative height of the marker multiplied by this value
	 */
	public void setMarkerAnchorPoints(float x, float y){
		markerLayer.setAnchors(x, y);
	}
	
	/**
	 * Optionally invoke this method before adding zoom levels.
	 * Each level will have its dimension set to this value, halved for it's distance from the topmost level,
	 * if width and height are not explicitly set for that level
	 * E.g., 
	 * <pre>mapView.setBaseMapSize(500, 500);
mapView.addZoomLevel("level0/%col%-%row%.jpg");  // will be 500x500
mapView.addZoomLevel("level1/%col%-%row%.jpg");  // will be 250x250
mapView.addZoomLevel("level2/%col%-%row%.jpg");  // will be 125x125</pre>
	 * @param width (int) width of the tiled image at a scale of 1.0f
	 * @param height (int) height of the tiled image at a scale of 1.0f
	 */
	public void setBaseMapSize(int width, int height){
		zoomManager.setBaseMapSize(width, height);
	}
	
	/**
	 * Invoking this method instructs the MapView to either engage zoom level tile sets normally,
	 * or not at all.  Passing a false shouldSuppress parameter will mean that only the current tile set will be used
	 * @param shouldSuppress (boolean) indicates if the MapView should use additional tile sets if scaled beyond the constraints of the current set
	 */
	public void supressTileSetUpdate(boolean shouldSuppress){
		shouldSuppressTileSetUpdates = shouldSuppress;
	}
	
	/**
	 * Set the duration (in milliseconds) of the fade-in animation used as new images tiles are rendered
	 * @param d (int) Number of milliseconds in duration for the alpha animation
	 */
	public void setTileTransitionDuration(int d){
		tileLayer.setTransitionDuration(d);
	}
	
	/**
	 * Sets whether new image tiles should be rendered using an AlphaAnimation
	 * @param shouldAnimate (boolean) use an alpha animation as new tiles are rendered (true for animations, false for no animations)
	 */
	public void setTileTranstionEnabled(boolean shouldAnimate){
		tileLayer.setTileTranstionEnabled(shouldAnimate);
	}
	
	/**
	 * Register a set of offset points to use when calculating position within the MapView.
	 * This can be used for lat/lng coordinates instead of pixel-based positioning.
	 * If a geolocation rectangle is specified, all positioning will be based on relative computation.
	 * To remove this process, use unregisterGeolocator
	 * @param top (double) the top edge of the rectangle used when calculating position (equivalent to the longitude of the top-left coordinate)
	 * @param right (double) the right edge of the rectangle used when calculating position (equivalent to the latitude of the top-left coordinate)
	 * @param bottom (double) the bottom edge of the rectangle used when calculating position (equivalent to the longitude of the bottom-right coordinate)
	 * @param left (double) the left edge of the rectangle used when calculating position (equivalent to the latitude of the bottom-right coordinate)
	 */
	public void registerGeolocator(double top, double right, double bottom, double left){
		
		geolocator = new Geolocator();
		Coordinate tl = new Coordinate(top, left);
		Coordinate br = new Coordinate(bottom, right);
		geolocator.setCoordinates(tl, br);
		
		ZoomLevel zl = zoomManager.getZoomLevel();
		int mapWidth = zl.getMapWidth();
		int mapHeight = zl.getMapHeight();
		
		geolocator.setSize(mapWidth, mapHeight);
	}
	
	/**
	 * Removes a geolocation rectangle from the MapView.
	 * Further positioning will be pixel based, from 0,0 (top left) to width,height (bottom right)
	 */
	public void unregisterGeolocator(){
		geolocator = null;
	}
	
	
	/**
	 * Register a map event listener callback object for this MapView. The parameter is an implementation of the OnMapEventListener interface.  A variety of touch and motion events are passed to this instance.
	 * @param l (OnMapEventListener) an implementation of the OnMapEventListener interface
	 */
	public void setOnMapEventListener(OnMapEventListener l){
		mapEventListener = l;
	}
	
	/**
	 * Sets up many internal mechanisms required for functionality.
	 * This method must be invoked <strong>after</strong> zoom levels are defined via addZoomLevel,
	 * and before the MapView will render.
	 */
	public void initialize(){
		
		ZoomSet zoomSet = zoomManager.computeZoomSet(1);
		zoomManager.setZoom(zoomSet.level);
		
		ZoomLevel zl = zoomManager.getZoomLevel();
		
		int mapWidth = zl.getMapWidth();
		int mapHeight = zl.getMapHeight();
		
		if(!userDefinedMinScale){
			Display display = ((Activity) getContext()).getWindowManager().getDefaultDisplay();
			int screenWidth = display.getWidth();
			int screenHeight = display.getHeight();
			
			double minimumScaleX = (double) screenWidth / (double) mapWidth;
			double minimumScaleY = (double) screenHeight / (double) mapHeight;
			minScale = Math.max(minimumScaleX, minimumScaleY);
		}
		
		if(getIsUsingGeolocation()){
			geolocator.setSize(mapWidth, mapHeight);
		}
		
		touchLayer.setSize(mapWidth, mapHeight);
    	touchLayer.setScaleLimits(minScale, maxScale);
    	
    	tileLayer.update(zl);
    	
	}
	
	/**
	 * Minimal implementation of addZoomLevel.  Register a tile set to be used for a particular zoom level.
	 * Each tile set to be used must be registered using this method,
	 * and at least one zoom level must be registered for the MapView to render any tiles.
	 * Successive calls to this method indicate higher (smaller) zoom levels, so the first call should register the largest tile set,
	 * and the last call should register the smallest.
	 * Since this signature does not require explicit tile set sizes, make sure to have called setBaseMapSize previously, or have added 
	 * a previous tile set had dimensions provided (which will be automatically
	 * halved for each level distant from the last with provided dimensions).
	 * 
	 * @param pattern (String) string path to the location of the map tiles (in the assets directory), with %col% and %row% to be replaced by their respective integer positions.  E.g., "folder/tile-%col%-%row%.jpg"
	 */
	public void addZoomLevel(String pattern){
		zoomManager.addZoomLevel(pattern);
	}
	
	/**
	 * Register a tile set to be used for a particular zoom level.
	 * Each tile set to be used must be registered using this method,
	 * and at least one zoom level must be registered for the MapView to render any tiles.
	 * Successive calls to this method indicate higher (smaller) zoom levels, so the first call should register the largest tile set,
	 * and the last call should register the smallest. 
	 * Since this signature does not require explicit tile set sizes, make sure to have called setBaseMapSize previously, or have added 
	 * a previous tile set had dimensions provided (which will be automatically
	 * halved for each level distant from the last with provided dimensions).
	 * 
	 * @param pattern (String) string path to the location of the map tiles (in the assets directory), with %col% and %row% to be replaced by their respective integer positions.  E.g., "folder/tile-%col%-%row%.jpg"
	 * @param sample (String) string path to the location of a downsampled image
	 */
	public void addZoomLevel(String pattern, String sample){
		zoomManager.addZoomLevel(pattern, sample);
	}
	
	/**
	 * Register a tile set to be used for a particular zoom level.
	 * Each tile set to be used must be registered using this method,
	 * and at least one zoom level must be registered for the MapView to render any tiles.
	 * Successive calls to this method indicate higher (smaller) zoom levels, so the first call should register the largest tile set,
	 * and the last call should register the smallest. 
	 * 	 
	 * @param wide (int) total width of the tile set
	 * @param tall (int) total height of the tile set
	 * @param pattern (String) string path to the location of the map tiles (in the assets directory), with %col% and %row% to be replaced by their respective integer positions.  E.g., "folder/tile-%col%-%row%.jpg"
	 * @param sample (String) string path to the location of a downsampled image
	 */
	public void addZoomLevel(int wide, int tall, String pattern, String sample){
		zoomManager.addZoomLevel(wide, tall, pattern, sample);
	}
	
	/**
	 * Register a tile set to be used for a particular zoom level.
	 * Each tile set to be used must be registered using this method,
	 * and at least one zoom level must be registered for the MapView to render any tiles.
	 * Successive calls to this method indicate higher (smaller) zoom levels, so the first call should register the largest tile set,
	 * and the last call should register the smallest. 
	 
	 * @param wide (int) total width of the tile set
	 * @param tall (int) total height of the tile set
	 * @param pattern (String) string path to the location of the map tiles (in the assets directory), with %col% and %row% to be replaced by their respective integer positions.  E.g., "folder/tile-%col%-%row%.jpg"
	 * @param sample (String) string path to the location of a downsampled image
	 * @param tileWidth (int) size of each tiled column
	 * @param tileHeight (int) size of each tiled row
	 */
	public void addZoomLevel(int wide, int tall, String pattern, String sample, int tileWidth, int tileHeight){
		zoomManager.addZoomLevel(wide, tall, pattern, sample, tileWidth, tileHeight);
	}
	
	/**
	 * Explicitly set the scale of the MapView.
	 * @param factor (float) value to scale this MapView to
	 */
	public void setZoom(final float factor){
		addOnReadyListener(new Runnable(){
			@Override
			public void run() {
				touchLayer.setScale(factor);
				setScale(factor);
				confirmOrUpdateTileSet();
			}
		});		
	}
	
	/**
	 * Get the current scale of the MapView
	 * @return (float) current scale of the MapView
	 */
	public float getZoom(){
		return actualScale;
	}	
	
	/**
	 * Register a Runnable instance to the onReady queue.  Each Runnable will have it's run method called when the MapView has been rendered.
	 * @param runnable Runnable instance to run immediately or queue until MapView is ready
	 */
	public void addOnReadyListener(Runnable runnable){
		if(isReady){
			runnable.run();
		} else {
			onReadyListeners.add(runnable);
		}
	}
	
	/**
	 * Remove all Runnables from the onReady queue.
	 */
	public void clearOnReadyListeners(){
		onReadyListeners.clear();
	}
	
	/**
	 * Remove a Runnable instance from the onReady queue.
	 * @param runnable Runnable instance to remove from the onReady queue
	 */
	public void removeOnReadyListener(Runnable runnable){
		onReadyListeners.remove(runnable);
	}
	
	/**
	 * Immediately position the MapView's focus to the Point provided.  Note that this signature's Point parameter will always use pixels values - use moveTo(double, double) when using geolocation.
	 * @param point (Point) position the MapView should focus to
	 */
	public void moveTo(Point point){
		touchLayer.scrollToPoint(point);
	}
	
	/**
	 * Immediately position the MapView's focus to the x and y positions provided.  This signature will use pixel values if no geolocator is registered, otherwise it will compute the coordiantes based on the rectangle provided to the geolocation registration.
	 * @param x (double) x position the MapView should focus to
	 * @param y (double) y position the MapView should focus to
	 */
	public void moveTo(double x, double y){
		Point point = getComputedPoint(x, y);
		moveTo(point);
	}
	
	/**
	 * Position the MapView's focus to the Point provided, using scrolling animation.  Note that this signature's Point parameter will always use pixels values - use slideTo(double, double) when using geolocation.
	 * @param point (Point) position the MapView should focus to
	 */
	public void slideTo(Point point){
		touchLayer.slideToPoint(point);
	}
	
	/**
	 * Position the MapView's focus to the Point provided, using scrolling animation.    This signature will use pixel values if no geolocator is registered, otherwise it will compute the coordiantes based on the rectangle provided to the geolocation registration.
	 * @param x (double) x position the MapView should focus to
	 * @param y (double) y position the MapView should focus to
	 */
	public void slideTo(double x, double y){
		Point point = getComputedPoint(x, y);
		slideTo(point);
	}
	
	/**
	 * Immediately center the MapView's focus to the Point provided.  Note that this signature's Point parameter will always use pixels values - use moveToAndCenter(double, double) when using geolocation.
	 * @param point (Point) position the MapView should center to
	 */
	public void moveToAndCenter(Point point){
		moveToAndCenter(point, true);
	}
	
	/**
	 * Immediately center the MapView's focus to the x and y positions provided.  This signature will use pixel values if no geolocator is registered, otherwise it will compute the coordiantes based on the rectangle provided to the geolocation registration.
	 * @param x (double) x position the MapView should center to
	 * @param y (double) y position the MapView should center to
	 */
	public void moveToAndCenter(double x, double y){
		Point point = getComputedPoint(x, y);
		moveToAndCenter(point, false);
	}	

	/**
	 * Center the MapView's focus to the Point provided, using scrolling animation.  Note that this signature's Point parameter will always use pixels values - use slideToAndCenter(double, double) when using geolocation.
	 * @param point (Point) position the MapView should center to
	 */
	public void slideToAndCenter(Point point){
		slideToAndCenter(point, true);		
	}	
	
	/**
	 * Center the MapView's focus to the Point provided, using scrolling animation.    This signature will use pixel values if no geolocator is registered, otherwise it will compute the coordiantes based on the rectangle provided to the geolocation registration.
	 * @param x (double) x position the MapView should center to
	 * @param y (double) y position the MapView should center to
	 */
	public void slideToAndCenter(double x, double y){
		Point point = getComputedPoint(x, y);
		slideToAndCenter(point, false);
	}

	/**
	 * Draw a path (line) on the topmost layer of the MapView
	 * @param points (ArrayList<Point>) ArrayList of Point instances describing the line to be drawn
	 */
	public void drawPath(ArrayList<Point> points){
		pathLayer.drawPathFromPoints(points);
	}	
	
	/**
	  * Add a marker to the the MapView.  The marker can be any View.
	 * No LayoutParams are required; the View will be laid out using WRAP_CONTENT for both width and height, and positioned based on the parameters
	 * @param view (View) View instance to be added to the MapView
	 * @param point (Point) point the View instance should be positioned at
	 * @return (View) the View instance added to the MapView
	 */
	public View addMarker(View view, Point point){
		Point computedPoint = getComputedPoint(point);
		return markerLayer.addMarker(view, computedPoint);
	}
	
	/**
	 * Add a marker to the the MapView.  The marker can be any View.
	 * No LayoutParams are required; the View will be laid out using WRAP_CONTENT for both width and height, and positioned based on the parameters
	 * @param view (View) View instance to be added to the MapView
	 * @param x (double) x position the View instance should be positioned at
	 * @param y (double) y position the View instance should be positioned at
	 * @return (View) the View instance added to the MapView
	 */
	public View addMarker(View view, double x, double y){
		Point computedPoint = getComputedPoint(x, y);
		return markerLayer.addMarker(view, computedPoint);
	}
	
	/**
	 * Remove a marker (View) from the MapView
	 * @param view
	 */
	public void removeMarker(View view){
		if(markerLayer.indexOfChild(view) > -1){
			markerLayer.removeView(view);
		}		
	}
	
	/**
	 * Remove all markers (Views) from the MapView
	 */
	public void removeMarkers(){
		markerLayer.removeAllViews();
	}
	
	/**
	 * Register a rectangle that should fire an OnClickListener when a touch event occurs that intersects that rectangle.
	 * The rectangle moves and scales with the map view.
	 * Note that while the second parameter takes a View.OnClickListener instance, the View instance passed to the OnClick method
	 * will be null, since the HotSpot is not an actual View but an abstract representation of one.
	 * @param r (Rect) the rectangle that is tested against touch events that occur on the MapView
	 * @param l (View.OnClickListener) the OnClickListener instance that is fired if the rectangle intersects a touch event
	 */
	public void addHotSpot(Rect r, View.OnClickListener l){
		HotSpot hotSpot = new HotSpot(r, l);
		hotSpots.add(hotSpot);
	}
	
	/**
	 * Remove a hotspot registered with addHotSpot
	 * @param r (Rect) the rectangle to test for
	 * @return (boolean) true if a hotspot was removed, false if not
	 */
	public boolean removeHotSpot(Rect r){
		HotSpot toRemove = null;
		for(HotSpot hotSpot : hotSpots){
			if(hotSpot.getRect().equals(r)){
				toRemove = hotSpot;
				break;
			}
		}
		if(toRemove != null){
			hotSpots.remove(toRemove);
			return true;
		}		
		return false;
	}
	
	
	
	
	
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

		measureChildren(widthMeasureSpec, heightMeasureSpec);

		int width = 0;
		int height = 0;		

		int count = getChildCount();
		for (int i = 0; i < count; i++) {
			View child = getChildAt(i);
			if (child.getVisibility() != GONE) {
				FixedLayout.LayoutParams lp = (FixedLayout.LayoutParams) child.getLayoutParams();
				int right = lp.x + child.getMeasuredWidth();
				int bottom = lp.y + child.getMeasuredHeight();
				width = Math.max(width, right);
				height = Math.max(height, bottom);
			}
		}

		height = Math.max(height, getSuggestedMinimumHeight());
		width = Math.max(width, getSuggestedMinimumWidth());
		width = resolveSize(width, widthMeasureSpec);
		height = resolveSize(height, heightMeasureSpec);
		setMeasuredDimension(width, height);
		
	}
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		int count = getChildCount();
		for (int i = 0; i < count; i++) {
			View child = getChildAt(i);
			if (child.getVisibility() != GONE) {
				child.layout(0, 0, child.getMeasuredWidth(), child.getMeasuredHeight());
			}
		}
		if(!isReady){			
			if(++layoutPasses >= (getChildCount() - 1)){
				isReady = true;				
				onReady();				   	
			}				
		}			
	}

	private void onReady(){
		
		// set scale to be available area
		if(!userDefinedMinScale){
			
			ZoomLevel zl = zoomManager.getZoomLevel();
			
			int mapWidth = zl.getMapWidth();
			int mapHeight = zl.getMapHeight();
			
			double minimumScaleX = (double) getWidth() / (double) mapWidth;
			double minimumScaleY = (double) getHeight() / (double) mapHeight;
			minScale = Math.max(minimumScaleX, minimumScaleY);
			
			touchLayer.setScaleLimits(minScale, maxScale);
			
			if(initiallyScaleToFit){
				post(new Runnable(){
					@Override
					public void run(){
						touchLayer.setScale((float) minScale);
						setScale((float) minScale);
					}							
				});
			}
			
		}				

		for(Runnable runnable : onReadyListeners){
			post(runnable);
		}
		
		clearOnReadyListeners();
		
		requestUpdate();
		
	}

	private boolean getIsUsingGeolocation(){
		return geolocator != null;
	}
	
	private ZoomManager getZoomManager(){
		return zoomManager;
	}
	
	private void setScale(float factor){  // react to an actual scale
		
		actualScale = factor;
		relativeScale = zoomManager.getScaleForZoom(factor);
		
		ZoomLevel zoomLevel = zoomManager.getZoomLevel();
		
		tileLayer.setScale(relativeScale);
		pathLayer.setScale(actualScale);
		markerLayer.setScale(actualScale);
		
		int mapWidth = zoomLevel.getMapWidth();
		int mapHeight = zoomLevel.getMapHeight();
		int wide = (int) (mapWidth * relativeScale);
		int tall = (int) (mapHeight * relativeScale);
		
		if(this.getIsUsingGeolocation()){
			geolocator.setSize(wide, tall);
		}		
		
		requestUpdate();
	}	

	private void confirmOrUpdateTileSet(){
		if(shouldSuppressTileSetUpdates){  // if using a single zoom level, just scale it
			computedScale = actualScale;
		} else {
			int lastLevel = zoomManager.getZoom();		
			ZoomSet zs = zoomManager.computeZoomSet(actualScale);
			computedScale = (float) zs.scale;
			if(zs.level != lastLevel){
				zoomManager.setZoom(zs.level);			
				tileLayer.update(zoomManager.getZoomLevel());
			}
			tileLayer.setScale(computedScale);
			requestUpdate();
		}
	}
	
	
	private Point getComputedPoint(double x, double y){
		Point point;
		if(getIsUsingGeolocation()){
			Coordinate coordinate = new Coordinate(x, y);
			point = geolocator.getPoint(coordinate);
		} else {
			point = new Point((int) x, (int) y);
		}
		return point;
	}
	
	private Point getComputedPoint(int x, int y){
		return getComputedPoint((double) x, (double) y);
	}
	
	private Point getComputedPoint(Point p){
		return getComputedPoint(p.x, p.y);
	}
	
	private void slideToAndCenter(Point point, boolean shouldScale){  // TODO: update other Point receiving method with scale options
		Point destination;
		if(shouldScale){
			XPoint xpoint = new XPoint(point);
			xpoint.scale(actualScale);
			destination = xpoint;
		} else {
			destination = point;
		}
		touchLayer.slideToAndCenter(destination);
	}	
	
	private void moveToAndCenter(Point point, boolean shouldScale){
		Point destination;
		if(shouldScale){
			XPoint xpoint = new XPoint(point);
			xpoint.scale(actualScale);
			destination = xpoint;
		} else {
			destination = point;
		}
		touchLayer.scrollToAndCenter(destination);
	}
	
	
	
	
	
	/* untested */
	/*
	public ViewGroup addMarkerLayer(){
		MarkerLayer layer = new MarkerLayer(getContext());
		touchLayer.addLayer(layer);
		return layer;
	}
	
	public void removeMarkerLayer(View layer){
		if(layer instanceof MarkerLayer){
			removeView(layer);
		}
	}
	
	public View addMarker(View v, double x, double y, MarkerLayer layer){
		Point point = getPoint(x, y);
		return addMarker(v, point, layer);
	}
	
	public View addMarker(View v, Point p, MarkerLayer layer){
		return layer.addMarker(v, p);
	}
	*/
	
	
	private TileLayer getTileLayer(){
		return tileLayer;
	}
	
	private MarkerLayer getMarkerLayer(){
		return markerLayer;
	}
	
	
	private void qualifyHotSpots(Point p){
		XPoint xpoint = new XPoint(p);
		xpoint.scale(1 / actualScale);
		qualifiedHotSpots.clear();
		for(HotSpot hotSpot : hotSpots){
			if(hotSpot.test(xpoint)){
				qualifiedHotSpots.add(hotSpot);
			}
		}
	}
	
	private void checkHotSpots(Point p){
		XPoint xpoint = new XPoint(p);
		xpoint.scale(1 / actualScale);
		for(HotSpot hotSpot : qualifiedHotSpots){
			if(hotSpot.test(xpoint)){
				hotSpot.touch();
			}
		}
	}
	
	private void updateVisibleRect() {
		visible.left = touchLayer.getScrollX();
		visible.top = touchLayer.getScrollY();
		visible.right = visible.left + touchLayer.getWidth();
		visible.bottom = visible.top + touchLayer.getHeight();
	}
    
    private Handler handler = new Handler(){
    	@Override
		public void handleMessage(final Message message) {
			switch (message.what) {
			case RENDER : 
				updateVisibleRect();
				tileLayer.renderTiles(visible, computedScale);
				break;
			}
    	}
    };
    
    private void requestUpdate(){
    	if(handler.hasMessages(RENDER)){
			handler.removeMessages(RENDER);
		}
		handler.sendEmptyMessageDelayed(RENDER, 200);
    }
    
    private TouchLayerEventListener touchLayerListener = new TouchLayerEventListener(){

		@Override
		public void onDrag(Point p) {
			requestUpdate();
			if(mapEventListener != null){
				mapEventListener.onDrag(p);
			}
		}

		@Override
		public void onFling(Point from, Point to) {
			if(mapEventListener != null){
				mapEventListener.onFling(from, to);
			}
		}

		@Override
		public void onScaleChanged(float factor) {  // TODO: - decouple - bridge class between coordinate manager and touchlayout
			setScale(factor);
			if(mapEventListener != null){
				mapEventListener.onScaleChanged(factor);
			}
		}

		@Override
		public void onScrollChanged(Point p) {
			requestUpdate();
			if(mapEventListener != null){
				mapEventListener.onScrollChanged(p);
			}
		}

		@Override
		public void onDoubleTap(Point p) {
			if(mapEventListener != null){
				mapEventListener.onDoubleTap(p);
			}
		}
    	
		@Override
		public void onZoomComplete(float factor){
			confirmOrUpdateTileSet();
			if(mapEventListener != null){
				mapEventListener.onZoomComplete(factor);
			}
		}

		@Override
		public void onTap(Point p) {
			checkHotSpots(p);
			if(mapEventListener != null){
				mapEventListener.onTap(p);
			}
		}
		
		@Override
		public void onFingerDown(Point p){
			qualifyHotSpots(p);
			if(mapEventListener != null){
				mapEventListener.onFingerDown(p);
			}
		}
    };

    public interface OnMapEventListener extends TouchLayerEventListener {
    	/**
    	 * Fires when the MapView's scale has updated
    	 * @param factor (float) the new scale of the MapView
    	 */
    	public void onScaleChanged(float factor);
    	/**
    	 * Fires when the MapView's scroll position has updated
    	 * @param point (Point) the new scroll position of the MapView
    	 */
    	public void onScrollChanged(Point point);
    	/**
    	 * Fires while the MapView is being dragged
    	 * @param point (Point) the position of the event source (finger) performing the drag operation
    	 */
    	public void onDrag(Point point);
    	/**
    	 * Fires while the MapView is being flung
    	 * @param from (Point) the original scroll position of the MapView
    	 * @param to (Point) the final (destination) scroll position of the MapView
    	 */
    	public void onFling(Point from, Point to);
    	/**
    	 * Fires when the MapView has been double tapped
    	 * @param point (Point) the position of the event source (finger) performing the double tap
    	 */
    	public void onDoubleTap(Point point);
    	/**
    	 * Fires when the MapView has been tapped
    	 * @param point (Point) the position of the event source (finger) performing the tap
    	 */
    	public void onTap(Point point);
    	/**
    	 * Fires when the MapView has been touched.  This even occurs when the finger touches the screen - onTap happens when the finger is lifted
    	 * @param point (Point) the position of the event source (finger) performing the touch event
    	 */
    	public void onFingerDown(Point point);
    	/**
    	 * Fires when a zoom operation has completed
    	 * @param factor (float) the new scale of the MapView
    	 */
    	public void onZoomComplete(float factor);
    }
}

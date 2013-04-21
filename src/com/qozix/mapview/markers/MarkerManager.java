package com.qozix.mapview.markers;

import android.content.Context;
import android.view.View;

import com.qozix.layouts.TranslationLayout;
import com.qozix.mapview.viewmanagers.ViewSetManager;
import com.qozix.mapview.zoom.ZoomListener;
import com.qozix.mapview.zoom.ZoomManager;

/*
 * TODO: need to consolidate positioning logic - works as is, but does too many unnecessary and possibly messy calculations
 * should work with adding at runtime, in response to user event, sliding, etc. 
 */


public class MarkerManager extends TranslationLayout implements ZoomListener {

	private ZoomManager zoomManager;
	private ViewSetManager viewSetManager = new ViewSetManager();
	
	public MarkerManager( Context context, ZoomManager zm ) {
		super( context );
		zoomManager = zm;
		zoomManager.addZoomListener( this );
	}	
	
	public View addMarker( View v, int x, int y ){
		LayoutParams lp = new LayoutParams( LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, x, y );
		addView( v, lp );
		return v;
	}

	public View addMarker( View v, int x, int y, float aX, float aY ) {
		LayoutParams lp = new LayoutParams( LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, x, y, aX, aY );
		addView( v, lp );
		return v;
	}
	
	public View addMarkerAtZoom( View v, int x, int y, int z ){
		addMarker( v, x, y );
		viewSetManager.addViewAtLevel( v, z );
		filterMarkers();
		return v;
	}
	
	public View addMarkerAtZoom( View v, int x, int y, float aX, float aY, int z ) {
		addMarker( v, x, y, aX, aY );
		viewSetManager.addViewAtLevel( v, z );
		filterMarkers();
		return v;
	}
	
	public void filterMarkers(){
		int zoom = zoomManager.getZoom();
		viewSetManager.purgeViewSets();
		viewSetManager.updateDisplay( zoom );
	}
	
	@Override
	public void onZoomLevelChanged( int oldZoom, int newZoom ) {
		filterMarkers();
	}

	@Override
	public void onZoomScaleChanged( double scale ) {
		setScale( scale );
	}

}

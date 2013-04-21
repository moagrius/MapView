package com.qozix.mapview.geom;

import android.util.Log;

import com.qozix.geom.Geolocator;
import com.qozix.mapview.zoom.ZoomListener;
import com.qozix.mapview.zoom.ZoomManager;

public class ManagedGeolocator extends Geolocator implements ZoomListener {

	private ZoomManager zoomManager;
	
	public ManagedGeolocator( ZoomManager zm ){
		zoomManager = zm;
		zoomManager.addZoomListener( this );
		update();
	}
	
	private void update(){
		int w = zoomManager.getComputedCurrentWidth();
		int h = zoomManager.getComputedCurrentHeight();
		setSize( w, h );
	}
	
	@Override
	public void onZoomLevelChanged( int oldZoom, int newZoom ) {
		update();
	}

	@Override
	public void onZoomScaleChanged( double scale ) {
		
	}	

}

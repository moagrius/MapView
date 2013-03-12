package com.qozix.mapview.paths;

import java.util.List;

import android.content.Context;
import android.graphics.Point;
import android.view.View;

import com.qozix.layouts.StaticLayout;
import com.qozix.mapview.viewmanagers.ViewSetManager;
import com.qozix.mapview.zoom.ZoomListener;
import com.qozix.mapview.zoom.ZoomManager;

public class PathManager extends StaticLayout implements ZoomListener {

	private double scale = 1;
	private ZoomManager zoomManager;
	private ViewSetManager viewSetManager = new ViewSetManager();
	
	public PathManager( Context context, ZoomManager zm ) {
		super( context );
		zoomManager = zm;
		zoomManager.addZoomListener( this );
	}
	
	public void setScale( double s ){
		scale = s;
		for(int i = 0; i < getChildCount(); i++){
			View child = getChildAt( i );
			if(child instanceof PathView){
				PathView pathView = (PathView) child;
				pathView.setScale( scale );
			}
		}
	}
	
	public View drawPath( List<Point> points ) {
		PathView pathView = new PathView( getContext() );
		pathView.setScale( scale );
		pathView.drawPath( points );
		addView( pathView );
		return pathView;
	}
	
	public View drawPathAtZoom( List<Point> points, int zoom ){
		View pathView = drawPath( points );
		viewSetManager.addViewAtLevel( pathView, zoom );
		filterPathViews();
		return pathView;
	}

	public void filterPathViews(){
		int zoom = zoomManager.getZoom();
		viewSetManager.purgeViewSets();
		viewSetManager.updateDisplay( zoom );
	}
	
	@Override
	public void onZoomLevelChanged( int oldZoom, int newZoom ) {
		filterPathViews();
	}

	@Override
	public void onZoomScaleChanged( double scale ) {
		setScale( scale );
	}

}

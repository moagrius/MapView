package com.qozix.mapview.markers;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;

import com.qozix.mapview.zoom.ZoomManager;

public class CalloutManager extends MarkerManager {

	public CalloutManager( Context context, ZoomManager zoomManager ) {
		super( context, zoomManager );
	}
	
	private void clear(){
		while( getChildCount() > 0 ) {
			View child = getChildAt( 0 );
			removeView( child );
		}
	}
	
	@Override
	public boolean onTouchEvent( MotionEvent event ) {
		clear();
		return false;
	}

}

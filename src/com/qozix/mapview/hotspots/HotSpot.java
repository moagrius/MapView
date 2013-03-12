package com.qozix.mapview.hotspots;

import android.graphics.Rect;
import android.view.View;

public class HotSpot {
	public Rect area;
	public View.OnClickListener listener;
	public HotSpot( Rect r, View.OnClickListener l ){
		area = r;
		listener = l;
	}
	@Override
	public boolean equals( Object o ){
		if(o instanceof HotSpot ){
			HotSpot h = (HotSpot) o;
			return listener == h.listener && area.equals( h.area );
		}
		return false;
	}
}

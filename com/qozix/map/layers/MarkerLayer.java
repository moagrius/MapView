package com.qozix.map.layers;

import com.qozix.map.layouts.FixedLayout;
import com.qozix.map.layouts.FixedLayout.LayoutParams;

import android.content.Context;
import android.graphics.Point;
import android.view.View;

public class MarkerLayer extends TranslationLayer {
	
	public MarkerLayer(Context context){
		super(context);
	}
	
	public View addMarker(View v, Point p){
		return addMarker(v, p.x, p.y);
	}
	
	public View addMarker(View v, int x, int y){
		LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, x, y);
		addView(v, lp);
		return v;
	}
	
}

package com.qozix.map.layers;

import com.qozix.map.layouts.FixedLayout;
import com.qozix.map.layouts.FixedLayout.LayoutParams;

import android.content.Context;
import android.view.View;

public class TranslationLayer extends FixedLayout {
	
	private float scale = 1.0f;
	
	private float anchorX = 1.0f;
	private float anchorY = 1.0f;
	
	public TranslationLayer(Context context){
		super(context);
	}
	
	public void setScale(float factor){
		scale = factor;
		requestLayout();
	}
	
	public void setAnchors(float x, float y){
		anchorX = x;
		anchorY = y;
		requestLayout();
	}
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
	    int count = getChildCount();
	    for (int i = 0; i < count; i++) {
	        View child = getChildAt(i);
	        if (child.getVisibility() != GONE) {
	            LayoutParams lp = (LayoutParams) child.getLayoutParams();
	            int w = child.getMeasuredWidth();
	            int h = child.getMeasuredHeight();
	            int scaledX = (int) (0.5 + (lp.x * scale));
	            int scaledY = (int) (0.5 + (lp.y * scale));
	            int x = scaledX - (int) (w * anchorX);
	            int y = scaledY - (int) (h * anchorY);
	            child.layout(x, y, x + w, y + h);
	        }
	    }
	}

}

package com.qozix.layouts;

import android.content.Context;
import android.view.View;

/*
 * considers scale for positioning only (not rendering), and allows anchors for individual children (or default anchor offsets)
 */

public class TranslationLayout extends AnchorLayout {
	
	protected double scale = 1;
		
	public TranslationLayout(Context context){
		super(context);
	}
	
	public void setScale(double d){
		scale = d;
		requestLayout();
	}
	
	public double getScale() {
		return scale;
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
				TranslationLayout.LayoutParams lp = (TranslationLayout.LayoutParams) child.getLayoutParams();
				// get anchor offsets
				float aX = (lp.aX == null) ? anchorX : lp.aX;
	            float aY = (lp.aY == null) ? anchorY : lp.aY;
	            // offset dimensions by anchor values
	            int computedWidth = (int) (child.getMeasuredWidth() * aX);
	            int computedHeight = (int) (child.getMeasuredHeight() * aY);
	            // get offset position
	            int scaledX = (int) (0.5 + (lp.x * scale));
	            int scaledY = (int) (0.5 + (lp.y * scale));
	            // add computed dimensions to actual position
	            int right = scaledX + computedWidth;
				int bottom = scaledY + computedHeight;
				// if it's larger, use that
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
	            LayoutParams lp = (LayoutParams) child.getLayoutParams();
	            // get sizes
	            int w = child.getMeasuredWidth();
	            int h = child.getMeasuredHeight();
	            // get offset position
	            int scaledX = (int) (0.5 + (lp.x * scale));
	            int scaledY = (int) (0.5 + (lp.y * scale));
	            // user child's layout params anchor position if set, otherwise default to anchor position of layout
	            float aX = (lp.aX == null) ? anchorX : lp.aX;
	            float aY = (lp.aY == null) ? anchorY : lp.aY;
	            // apply anchor offset to position
	            int x = scaledX - (int) (w * aX);
	            int y = scaledY - (int) (h * aY);
	            // set it
	            child.layout(x, y, x + w, y + h);
	        }
	    }
	}

}

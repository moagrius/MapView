package com.qozix.layouts;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/*
 * allows anchors for individual children (or default anchor offsets)
 */

public class AnchorLayout extends ViewGroup {
	
	protected float anchorX = 1.0f;
	protected float anchorY = 1.0f;
	
	public AnchorLayout(Context context){
		super(context);
	}
	
	public void setAnchors(float x, float y){
		anchorX = x;
		anchorY = y;
		requestLayout();
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
				AnchorLayout.LayoutParams lp = (AnchorLayout.LayoutParams) child.getLayoutParams();
				// get anchor offsets
				float aX = (lp.aX == null) ? anchorX : lp.aX;
	            float aY = (lp.aY == null) ? anchorY : lp.aY;
	            // offset dimensions by anchor values
	            int computedWidth = (int) (child.getMeasuredWidth() * aX);
	            int computedHeight = (int) (child.getMeasuredHeight() * aY);
	            // add computed dimensions to actual position
	            int right = lp.x + computedWidth;
				int bottom = lp.y + computedHeight;
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
	           // user child's layout params anchor position if set, otherwise default to anchor position of layout
	            float aX = (lp.aX == null) ? anchorX : lp.aX;
	            float aY = (lp.aY == null) ? anchorY : lp.aY;
	            // apply anchor offset to position
	            int x = lp.x - (int) (w * aX);
	            int y = lp.y - (int) (h * aY);
	            // set it
	            child.layout(x, y, x + w, y + h);
	        }
	    }
	}

	@Override
	protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
		return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0, 0);
	}
	

	@Override
	protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
		return p instanceof AnchorLayout.LayoutParams;
	}

	@Override
	protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
		return new LayoutParams(p);
	}

	public static class LayoutParams extends ViewGroup.LayoutParams {

		public int x = 0;
		public int y = 0;
		public Float aX = null;
		public Float aY = null;

		public LayoutParams(ViewGroup.LayoutParams source) {
			super(source);
		}


		public LayoutParams(int width, int height){
			super(width, height);
		}
		
		public LayoutParams(int width, int height, int left, int top) {
			super(width, height);
			x = left;
			y = top;
		}
		
		public LayoutParams(int width, int height, int left, int top, float anchorX, float anchorY) {
			super(width, height);
			x = left;
			y = top;
			aX = anchorX;
			aY = anchorY;
		}
		
	}
}

package com.qozix.map.hotspots;

import android.graphics.Point;
import android.graphics.Rect;
import android.view.View;

public class HotSpot {

	private Rect rect = null;
	private Object data = null;	
	private View.OnClickListener listener = null;
	
	public HotSpot(){
		
	}
	
	public HotSpot(Rect r){
		rect = r;
	}
	
	public HotSpot(Rect r, View.OnClickListener l){
		rect = r;
		listener = l;
	}

	public HotSpot(int left, int top, int right, int bottom, View.OnClickListener l){
		rect = new Rect(left, top, right, bottom);
		listener = l;
	}
	
	public HotSpot(int left, int top, int right, int bottom){
		rect = new Rect(left, top, right, bottom);
	}
	
	
	public Rect getRect(){
		return rect;
	}
	
	public void setRect(Rect r){
		rect = r;
	}
	
	public Object getTag(){
		return data;
	}
	
	public void setTag(Object o){
		data = o;
	}
	
	public void setOnTapListener(View.OnClickListener l){
		listener = l;
	}
	
	public boolean test(Point p){
		if(rect != null){
			if(p.x >= rect.left){
	            if(p.x <= rect.right){
	                if(p.y >= rect.top){
	                    if(p.y <= rect.bottom){
	                        return true;
	                    }
	                }
	            }
	        }
		}
		return false;
	}
	
	public void touch(){
		if(listener != null){
			listener.onClick(null);
		}
	}
	
}

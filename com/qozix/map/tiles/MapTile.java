package com.qozix.map.tiles;

import android.graphics.Bitmap;
import android.graphics.Rect;

public class MapTile {

	public MapTileView view;
	
	public boolean showing;
	public Rect boundary;
	public String filename;
	public Bitmap bitmap;
	
	public Rect getScaledRect(float scale){
		return new Rect(
			(int) (boundary.left * scale),
			(int) (boundary.top * scale),
			(int) (boundary.right * scale),
			(int) (boundary.bottom * scale)
		);
	}
	
	public int getWidth(){
		return boundary.right - boundary.left;
	}
	
	public int getHeight(){
		return boundary.bottom - boundary.top;
	}
}

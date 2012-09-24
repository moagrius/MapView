package com.qozix.map.tiles;

import java.util.ArrayList;

import android.content.Context;
import android.util.Log;

public class MapTileViewFactory {
	
	private ArrayList<MapTileView> employed = new ArrayList<MapTileView>();
	private ArrayList<MapTileView> unemployed = new ArrayList<MapTileView>();

	public MapTileViewFactory() {
		
	}

	public MapTileView employView(Context context) {
		MapTileView v;
		if (unemployed.size() > 0) {
			v = unemployed.get(0);
			unemployed.remove(v);
		} else {
			v = new MapTileView(context);
		}
		employed.add(v);
		Log.d("MapTileViewFactory", "employed=" + employed.size() + ", unemployed=" + unemployed.size());
		return v;
	}
	
	public void retireView(MapTileView v){
        if(employed.contains(v)){
            employed.remove(v);
            unemployed.add(v);
        }
    }
}

	

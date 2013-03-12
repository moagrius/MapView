package com.qozix.mapview.tiles;

import java.util.LinkedList;

public class MapTilePool {
	
	private LinkedList<MapTile> employed = new LinkedList<MapTile>();
	private LinkedList<MapTile> retired = new LinkedList<MapTile>();
	
	public MapTile employ(){
		MapTile m = retired.poll();
		if ( m == null ) {
			m = new MapTile();
		}
		employed.add( m );
		return m;
	}
	
	public void retire( MapTile m ) {
		employed.remove( m );
		retired.add( m );
	}
	
	public void retireAll() {
		retired.addAll( employed );
		employed.clear();
	}
}

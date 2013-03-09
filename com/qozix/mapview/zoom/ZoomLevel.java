package com.qozix.mapview.zoom;

import java.util.LinkedList;

import android.graphics.Rect;

import com.qozix.mapview.tiles.MapTile;

public class ZoomLevel implements Comparable<ZoomLevel> {

	private static final int DEFAULT_TILE_SIZE = 256;

	private int tileWidth;
	private int tileHeight;
	private int mapWidth;
	private int mapHeight;
	
	private int area;

	private int rowCount;
	private int columnCount;

	private String pattern;

	private ZoomManager zoomManager;

	public ZoomLevel( ZoomManager zm, int mw, int mh, String p ) {
		this( zm, mw, mh, p, DEFAULT_TILE_SIZE, DEFAULT_TILE_SIZE );
	}

	public ZoomLevel( ZoomManager zm, int mw, int mh, String p, int tw, int th ) {
		zoomManager = zm;
		mapWidth = mw;
		mapHeight = mh;
		pattern = p;
		tileWidth = tw;
		tileHeight = th;
		rowCount = (int) ( mapHeight / tileHeight );
		columnCount = (int) ( mapWidth / tileWidth );
		area = mapWidth * mapHeight;
	}

	public LinkedList<MapTile> getIntersections() {
		int zoom = zoomManager.getZoom();
		double scale = zoomManager.getRelativeScale();
		double offsetWidth = tileWidth * scale;
		double offsetHeight = tileHeight * scale;
		LinkedList<MapTile> intersections = new LinkedList<MapTile>();
		Rect boundedRect = new Rect( zoomManager.getViewport() );
		boundedRect.top = Math.max( boundedRect.top, 0 );
		boundedRect.left = Math.max( boundedRect.left, 0 );
		boundedRect.right = Math.min( boundedRect.right, mapWidth );
		boundedRect.bottom = Math.min( boundedRect.bottom, mapHeight );
		int sr = (int) Math.floor( boundedRect.top / offsetHeight );
		int er = (int) Math.ceil( boundedRect.bottom / offsetHeight );
		int sc = (int) Math.floor( boundedRect.left / offsetWidth );
		int ec = (int) Math.ceil( boundedRect.right / offsetWidth );
		for ( int r = sr; r <= er; r++ ) {
			for ( int c = sc; c <= ec; c++ ) {
				MapTile m = new MapTile( zoom, r, c, tileWidth, tileHeight, pattern );
				intersections.add( m );
			}
		}
		return intersections;
	}

	public int getTileWidth() {
		return tileWidth;
	}

	public int getTileHeight() {
		return tileHeight;
	}

	public int getMapWidth() {
		return mapWidth;
	}

	public int getMapHeight() {
		return mapHeight;
	}

	public String getPattern() {
		return pattern;
	}

	public int getRowCount() {
		return rowCount;
	}

	public int getColumnCount() {
		return columnCount;
	}

	public String getTilePath( int col, int row ) {
		String path = pattern;
		path = path.replace( "%col%", Integer.toString( col ) );
		path = path.replace( "%row%", Integer.toString( row ) );
		return path;
	}

	public int getArea() {
		return area;
	}

	@Override
	public int compareTo( ZoomLevel o ) {
		return (int) ( getArea() - o.getArea() );
	}

	@Override
	public boolean equals( Object o ) {
		if ( o instanceof ZoomLevel ) {
			ZoomLevel zl = (ZoomLevel) o;
			return ( zl.getMapWidth() == getMapWidth() )
				&& ( zl.getMapHeight() == getMapHeight() );
		}
		return false;
	}

	@Override
	public int hashCode() {
		long bits = ( Double.doubleToLongBits( mapWidth ) * 43 ) + ( Double.doubleToLongBits( mapHeight ) * 47 );
		return ( ( (int) bits ) ^ ( (int) ( bits >> 32 ) ) );
	}

	@Override
	public String toString() {
		return "(w=" + mapWidth + ", h=" + mapHeight + ", p=" + pattern + ")";
	}

}
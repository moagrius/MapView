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
	
	private long area;

	private int rowCount;
	private int columnCount;

	private String pattern;
	private String downsample;

	private ZoomManager zoomManager;

	public ZoomLevel( ZoomManager zm, int mw, int mh, String p ) {
		this( zm, mw, mh, p, null, DEFAULT_TILE_SIZE, DEFAULT_TILE_SIZE );
	}
	
	public ZoomLevel( ZoomManager zm, int mw, int mh, String p, String d ) {
		this( zm, mw, mh, p, d, DEFAULT_TILE_SIZE, DEFAULT_TILE_SIZE );
	}
	
	public ZoomLevel( ZoomManager zm, int mw, int mh, String p, int tw, int th ) {
		this( zm, mw, mh, p, null, tw, th );
	}

	public ZoomLevel( ZoomManager zm, int mw, int mh, String p, String d, int tw, int th ) {
		zoomManager = zm;
		mapWidth = mw;
		mapHeight = mh;
		pattern = p;
		downsample = d;
		tileWidth = tw;
		tileHeight = th;
		rowCount = (int) ( mapHeight / tileHeight );
		columnCount = (int) ( mapWidth / tileWidth );
		area = (long) ( mapWidth * mapHeight );
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
	
	public String getDownsample() {
		return downsample;
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

	public long getArea() {
		return area;
	}

	@Override
	public int compareTo( ZoomLevel o ) {
		return (int) Math.signum( getArea() - o.getArea() );
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
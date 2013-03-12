package com.qozix.geom;

import java.util.ArrayList;

import android.graphics.Point;

public class Geolocator {

	private Coordinate topLeft = new Coordinate();
	private Coordinate bottomRight = new Coordinate();

	private int width = 0;
	private int height = 0;

	public void setCoordinates( Coordinate tl, Coordinate br ) {
		topLeft = tl;
		bottomRight = br;
	}
	
	public void setCoordinates( double left, double top, double right, double bottom ){
		topLeft = new Coordinate( left, top );
		bottomRight = new Coordinate( right, bottom );
	}

	public void setSize( int w, int h ) {
		width = w;
		height = h;
	}

	public Point translate( Coordinate c ) {

		Point p = new Point();

		double longitudanalDelta = bottomRight.longitude - topLeft.longitude;
		double longitudanalDifference = c.longitude - topLeft.longitude;
		double longitudanalFactor = longitudanalDifference / longitudanalDelta;
		p.x = (int) ( longitudanalFactor * width );

		double latitudanalDelta = bottomRight.latitude - topLeft.latitude;
		double latitudanalDifference = c.latitude - topLeft.latitude;
		double latitudanalFactor = latitudanalDifference / latitudanalDelta;
		p.y = (int) ( latitudanalFactor * height );
		
		return p;

	}

	public Coordinate translate( Point p ) {

		Coordinate c = new Coordinate();

		double relativeX = p.x / (double) width;
		double deltaX = bottomRight.longitude - topLeft.longitude;
		c.longitude = topLeft.longitude + deltaX * relativeX;

		double relativeY = p.y / (double) height;
		double deltaY = bottomRight.latitude - topLeft.latitude;
		c.latitude = topLeft.latitude + deltaY * relativeY;

		return c;
	}
	
	public int[] coordinatesToPixels( double lat, double lng ) {
		
		int[] positions = new int[2];

		double longitudanalDelta = bottomRight.longitude - topLeft.longitude;
		double longitudanalDifference = lng - topLeft.longitude;
		double longitudanalFactor = longitudanalDifference / longitudanalDelta;
		positions[0] = (int) ( longitudanalFactor * width );
		
		double latitudanalDelta = bottomRight.latitude - topLeft.latitude;
		double latitudanalDifference = lat - topLeft.latitude;
		double latitudanalFactor = latitudanalDifference / latitudanalDelta;
		positions[1] = (int) ( latitudanalFactor * height );
		
		return positions;
		
	}
	
	public ArrayList<Point> getPointsFromCoordinates( ArrayList<Coordinate> coordinates ) {
		ArrayList<Point> points = new ArrayList<Point>();
		for ( Coordinate coordinate : coordinates ) {
			Point point = translate( coordinate );
			points.add( point );
		}
		return points;
	}

	public ArrayList<Coordinate> getCoordinatesFromPoints( ArrayList<Point> points ) {
		ArrayList<Coordinate> coordinates = new ArrayList<Coordinate>();
		for ( Point point : points ) {
			Coordinate coordinate = translate( point );
			coordinates.add( coordinate );
		}
		return coordinates;
	}

	public boolean contains( Coordinate coordinate ) {
		double minLat = Math.min( topLeft.latitude, bottomRight.latitude );
		double maxLat = Math.max( topLeft.latitude, bottomRight.latitude );
		double minLng = Math.min( topLeft.longitude, bottomRight.longitude );
		double maxLng = Math.max( topLeft.longitude, bottomRight.longitude );
		return coordinate.latitude >= minLat
			&& coordinate.latitude <= maxLat
			&& coordinate.longitude >= minLng
			&& coordinate.longitude <= maxLng;
	}
}

package com.qozix.map.geom;

import java.util.ArrayList;

import android.graphics.Point;

public class Geolocator {

	private Coordinate tl = new Coordinate(0, 0);
	private Coordinate br = new Coordinate(0, 0);
	
	private int width = 0;
	private int height = 0;
	
	public Geolocator(){
		
	}
	
	public ArrayList<Point> getPointsFromCoordinates(ArrayList<Coordinate> coordinates){
		ArrayList<Point> points = new ArrayList<Point>();
		for(Coordinate coordinate : coordinates){
			Point point = getPoint(coordinate);
			points.add(point);
		}
		return points;
	}
	
	public ArrayList<Coordinate> getCoordinatesFromPoints(ArrayList<Point> points){
		ArrayList<Coordinate> coordinates = new ArrayList<Coordinate>();
		for(Point point : points){
			Coordinate coordinate = getCoordinate(point);
			coordinates.add(coordinate);
		}
		return coordinates;
	}
	
	public void setCoordinates(Coordinate topLeft, Coordinate bottomRight){
		tl = topLeft;
		br = bottomRight;
	}
	
	public void setSize(int wide, int tall){
		width = wide;
		height = tall;
	}
	
	public Point getPoint(double lat, double lng){
		Point p = new Point();
		p.x = getX(lng);
		p.y = getY(lat);
		return p;
	}
	
	public Point getPoint(Coordinate coordinate){
		Point p = new Point();
		p.x = getX(coordinate.lng);
		p.y = getY(coordinate.lat);
		return p;
	}
	
	public Coordinate getCoordinate(int x, int y){
		Coordinate c = new Coordinate();
		c.lng = getLng(x);
		c.lat = getLat(y);
		return c;
	}
	
	public Coordinate getCoordinate(Point p){
		Coordinate c = new Coordinate();
		c.lng = getLng(p.x);
		c.lat = getLat(p.y);
		return c;		
	}
	
	public double getLat(double y){
		double relative = y / (double) height;
		double delta = br.lat - tl.lat;
		return tl.lat + (delta * relative);		
	}
	
	public double getLat(int y){
		return getLat((double) y);
	}
	
	public double getLng(double x){
		double relative = x / (double) width;
		double delta = br.lng - tl.lng;
		return tl.lng + (delta * relative);	
	}
	
	public double getLng(int x){
		return getLng((double) x);
	}
	
	public int getX(double lng){		
		double delta = br.lng - tl.lng;
		double diff = lng - tl.lng; 
		double factor = diff / delta;
		double position = factor * (double) width;
		return (int) (0.5 + position);
	}
	
	public int getY(double lat){
		double delta = br.lat - tl.lat;
		double diff = lat - tl.lat; 
		double factor = diff / delta;
		double position = factor * (double) height;
		return (int) (0.5 + position);
	}
	
	public boolean contains(Coordinate coordinate){
		double minLat = Math.min(tl.lat, br.lat);
		double maxLat = Math.max(tl.lat, br.lat);
		double minLng = Math.min(tl.lng, br.lng);
		double maxLng = Math.max(tl.lng, br.lng);
		return coordinate.lat >= minLat && coordinate.lat <= maxLat && coordinate.lng >= minLng && coordinate.lng <= maxLng;
	}
	
}

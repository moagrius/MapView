package com.qozix.map.geom;

public class Coordinate {

	public double lng;
	public double lat;
	
	public Coordinate(double latitude, double longitude){
		lat = latitude;
		lng = longitude;
	}
	
	public Coordinate(){
		
	}
	
	@Override
	public String toString(){
		return "lat=" + lat + ", lng=" + lng;
	}
	
}

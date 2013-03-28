package com.qozix.geom;

public class Coordinate {

	public double latitude;
	public double longitude;

	public Coordinate() {
	}

	public Coordinate( double lat, double lng ) {
		set( lat, lng );
	}

	public Coordinate( Coordinate src ) {
		set( src.latitude, src.longitude );
	}

	public void set( double lat, double lng ) {
		latitude = lat;
		longitude = lng;
	}

	public final boolean equals( double lat, double lng ) {
		return latitude == lat && longitude == lng;
	}

	@Override
	public boolean equals( Object o ) {
		if ( o instanceof Coordinate ) {
			Coordinate c = (Coordinate) o;
			return latitude == c.latitude && longitude == c.longitude;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return (int) ( latitude * 32713 + longitude );
	}

	@Override
	public String toString() {
		return "Coordinate(" + latitude + ", " + longitude + ")";
	}
}
package com.qozix.mapview.zoom;

import java.util.HashSet;

import android.graphics.Rect;

public class ZoomManager {

	private static final double BASE_2 = Math.log( 2 );
	private static final double PRECISION = 6;
	private static final double DECIMAL = Math.pow( 10, PRECISION );
	private static final double OFFSET = 1 / DECIMAL;
	
	private ZoomLevelSet zoomLevels = new ZoomLevelSet();
	private HashSet<ZoomListener> zoomListeners = new HashSet<ZoomListener>();
	private HashSet<ZoomSetupListener> zoomSetupListeners = new HashSet<ZoomSetupListener>();

	private double scale = 1;
	private double minScale = 0;
	private double maxScale = 1;
	private double historicalScale;
	
	private double relativeScale;  // at 0.375 actual scale, that's an 0.5 zoom scale and 0.75 relative scale
	private double invertedScale;  // at 0.375 actual scale, than's an 0.5 zoom scale and 2.375 inverted scale
	private double computedScale;  // at 0.375 actual scale, that's an 0.5 zoom scale and 0.75 computed scale
	
	private int zoom;
	private int lastZoom = -1;
	
	private int numZoomLevels;
	
	private ZoomLevel currentZoomLevel;
	private ZoomLevel highestZoomLevel;
	private ZoomLevel lowestZoomLevel;
	
	private int computedCurrentWidth;
	private int computedCurrentHeight;
	
	private int baseMapWidth;
	private int baseMapHeight;
	
	private boolean zoomLocked = false;
	
	private Rect viewport = new Rect();

	private static double getAtPrecision( double s ) {
		return Math.round( s * DECIMAL ) / DECIMAL;
	}
	
	public static int computeZoom( double scale, int numZoomLevels ){
		int zoom = (int) ( numZoomLevels + Math.floor( Math.log( scale - OFFSET ) / BASE_2 ) );
		zoom = Math.max( zoom, 0 );
		zoom = Math.min( zoom, numZoomLevels - 1);
		return zoom;
	}
	
	public static double computeRelativeScale( double scale, int numZoomLevels, int zoom ){
		double relativeScale = scale * ( 1 << ( ( numZoomLevels - 1 ) - zoom ) );
		return getAtPrecision( relativeScale );
	}
	
	public static double computeInvertedScale( int numZoomLevels, int zoom ){
		return 1 << ( ( numZoomLevels - zoom ) - 1 );
	}	
	
	public static double computeOffsetScale( double scale, int numZoomLevels, int zoom ) {
		return computeInvertedScale( numZoomLevels, zoom ) + computeRelativeScale( scale, numZoomLevels, zoom ) - 1;
	}

	public static double computeScaleForZoom( double scale, int zoom ) {
		double computedScale = ( scale / Math.pow( 2, -zoom ) ) - 1;
		return getAtPrecision( computedScale );
	}

	public ZoomManager(){
		update( true );
	}

	public double getScale() {
		return scale;
	}

	public void setScale( double s ) {
		// constrain between minimum and maximum allowed values
		s = Math.min( s, maxScale );
		s = Math.max( s, minScale );
		// round to PRECISION decimal places
		s = getAtPrecision( s );
		// is it changed?
		boolean changed = ( scale != s );
		// set it
		scale = s;
		// update computed values
		update( changed );		
	}
	
	public void updateViewport( int left, int top, int right, int bottom ) {
		viewport.set( left, top, right, bottom );
	}
	
	public Rect getViewport() {
		return viewport;
	}
	
	private void update( boolean changed ){
		
		// if no levels, fast-fail
		if(numZoomLevels == 0){
			zoom = 0;
			relativeScale = invertedScale = scale;
			currentZoomLevel = highestZoomLevel = lowestZoomLevel = null;
			computedCurrentWidth = computedCurrentHeight = 0;
			return;
		}
		
		// update zoom if unlocked
		if(!zoomLocked){
			zoom = computeZoom( scale, numZoomLevels );
		}		
		
		// update current zoom level
		currentZoomLevel = zoomLevels.get( zoom );
		
		// update computed scales
		relativeScale = computeRelativeScale( scale, numZoomLevels, zoom );
		computedScale = computeOffsetScale( scale, numZoomLevels, zoom );
		invertedScale = computeInvertedScale( numZoomLevels, zoom );
		
		// update current dimensions
		baseMapWidth = currentZoomLevel.getMapWidth();
		baseMapHeight = currentZoomLevel.getMapHeight();
		computedCurrentWidth = (int) ( baseMapWidth * invertedScale );
		computedCurrentHeight = (int) ( baseMapHeight * invertedScale );
		
		// broadcast scale change
		if( changed ) {
			for ( ZoomListener listener : zoomListeners ) {
				listener.onZoomScaleChanged( scale );
			}
			
		}
		
		// if there's a change in zoom, update appropriate values
		if ( zoom != lastZoom ) {			
			// notify all interested parties
			for ( ZoomListener listener : zoomListeners ) {
				listener.onZoomLevelChanged( lastZoom, zoom );
			}
			lastZoom = zoom;
		}
	
	}

	public void lockZoom(){
		zoomLocked = true;
	}
	
	public void unlockZoom(){
		zoomLocked = false;
	}
	
	public void setZoom( int z ) {
		int maxZoom = numZoomLevels - 1;
		z = Math.max(z, 0);
		z = Math.max(z, maxZoom);
		double s = 1 / (double) ( 1 << ( maxZoom - z ) );
		setScale( s );
	}

	public void addZoomListener( ZoomListener l ) {
		zoomListeners.add( l );
	}

	public void removeZoomListener( ZoomListener l ) {
		zoomListeners.remove( l );
	}
	
	public void addzoomSetupListener( ZoomSetupListener l ) {
		zoomSetupListeners.add( l );
	}

	public void removezoomSetupListener( ZoomSetupListener l ) {
		zoomSetupListeners.remove( l );
	}

	public void addZoomLevel( int wide, int tall, String pattern ) {
		ZoomLevel zoomLevel = new ZoomLevel( this, wide, tall, pattern );
		registerZoomLevel( zoomLevel );
	}

	public void addZoomLevel( int wide, int tall, String pattern, int tileWidth, int tileHeight ) {
		ZoomLevel zoomLevel = new ZoomLevel( this, wide, tall, pattern, tileWidth, tileHeight );
		registerZoomLevel( zoomLevel );
	}
	
	private void registerZoomLevel( ZoomLevel zoomLevel ) {
		zoomLevels.addZoomLevel( zoomLevel );
		numZoomLevels = zoomLevels.size();
		highestZoomLevel = zoomLevels.getLast();
		lowestZoomLevel = zoomLevels.getFirst();
		update( true );
		for ( ZoomSetupListener listener : zoomSetupListeners ) {
			listener.onZoomLevelAdded();
		}
	}

	public ZoomLevel getCurrentZoomLevel() {
		return currentZoomLevel;
	}
	
	public ZoomLevel getHighestZoomLevel(){
		return highestZoomLevel;
	}
	
	public ZoomLevel getLowestZoomLevel(){
		return lowestZoomLevel;
	}
	
	public int getComputedCurrentWidth(){
		return computedCurrentWidth;
	}
	
	public int getComputedCurrentHeight(){
		return computedCurrentHeight;
	}
	
	public int getZoom() {
		return zoom;
	}

	public int getNumZoomLevels() {
		return numZoomLevels;
	}

	public int getMaxZoom() {
		return numZoomLevels - 1;
	}

	public double getRelativeScale() {
		return relativeScale;
	}

	public double getInvertedScale(){
		return invertedScale;
	}
	
	public double getComputedScale(){
		return computedScale;
	}
	
	public int getBaseMapWidth() {
		return baseMapWidth;
	}
	
	public int getBaseMapHeight() {
		return baseMapHeight;
	}
	
	public double getHistoricalScale(){
		return historicalScale;
	}
	
	public void saveHistoricalScale(){
		historicalScale = scale;
	}

}

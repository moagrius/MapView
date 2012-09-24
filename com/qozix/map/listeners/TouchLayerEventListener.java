package com.qozix.map.listeners;

import android.graphics.Point;

/**
 * @author Mike Dunn
 * @version 0.1
 * 
 * MapEventListener is an interface that implements several callback methods that are fired by a MapView widget for touch, translation and transformation operations.
 */

/**
 * @constructor 
 */

public interface TouchLayerEventListener {
	/**
	 * Fires when the MapView's scale has updated
	 * @param factor (float) the new scale of the MapView
	 */
	public void onScaleChanged(float factor);
	/**
	 * Fires when the MapView's scroll position has updated
	 * @param p (Point) the new scroll position of the MapView
	 */
	public void onScrollChanged(Point point);
	/**
	 * Fires while the MapView is being dragged
	 * @param point (Point) the position of the event source (finger) performing the drag operation
	 */
	public void onDrag(Point point);
	/**
	 * Fires while the MapView is being flung
	 * @param from (Point) the original scroll position of the MapView
	 * @param to (Point) the final (destination) scroll position of the MapView
	 */
	public void onFling(Point from, Point to);
	/**
	 * Fires when the MapView has been double tapped
	 * @param point (Point) the position of the event source (finger) performing the double tap
	 */
	public void onDoubleTap(Point point);
	/**
	 * Fires when the MapView has been tapped
	 * @param point (Point) the position of the event source (finger) performing the tap
	 */
	public void onTap(Point point);
	/**
	 * Fires when the MapView has been touched.  This even occurs when the finger touches the screen - onTap happens when the finger is lifted
	 * @param point (Point) the position of the event source (finger) performing the touch event
	 */
	public void onFingerDown(Point point);
	/**
	 * Fires when a zoom operation has completed
	 * @param factor (float) the new scale of the MapView
	 */
	public void onZoomComplete(float factor);
}
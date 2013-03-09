package com.qozix.mapview.zoom;

public interface ZoomListener {
	public void onZoomLevelChanged(int oldZoom, int currentZoom);
	public void onZoomScaleChanged(double scale);
}

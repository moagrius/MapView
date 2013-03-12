package com.qozix.mapview.tiles;

public interface TileRenderListener {
	public void onRenderStart();
	public void onRenderCancelled();
	public void onRenderComplete();
}

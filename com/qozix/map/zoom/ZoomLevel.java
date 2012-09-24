package com.qozix.map.zoom;

public class ZoomLevel {
	
	private int tileWidth = 256;
	private int tileHeight = 256;
	private int wide;
	private int tall;
	private String pattern;
	private String downsample = null;
	
	public ZoomLevel(int w, int t, String p, String d){
		setMapWidth(w);
		setMapHeight(t);
		setPattern(p);
		setDownsample(d);
	}
	
	public ZoomLevel(int w, int t, String p){
		this(w, t, p, null);
	}
	
	public ZoomLevel(int w, int t, String p, String d, int tw, int th){
		this(w, t, p, d);
		setTileWidth(tw);
		setTileHeight(th);
	}
	
	public int getTileWidth(){
		return tileWidth;
	}
	
	public int getTileHeight(){
		return tileHeight;
	}
	
	public void setTileWidth(int size){
		tileWidth = size;
	}
	
	public void setTileHeight(int size){
		tileHeight = size;
	}
	
	public int getMapWidth(){
		return wide;
	}
	
	public void setMapWidth(int size){
		wide = size;
	}
	
	public int getMapHeight(){
		return tall;
	}
	
	public void setMapHeight(int size){
		tall = size;
	}
	
	public String getPattern(){
		return pattern;
	}
	
	public void setPattern(String patt){
		pattern = patt;
	}
	
	public boolean hasDownsample(){
		return downsample != null;
	}
	
	public String getDownsample(){
		return downsample;
	}
	
	public void setDownsample(String d){
		downsample = d;
	}
	
	
	public int getRowCount(){
		return (int) Math.ceil((double) getMapHeight() / getTileHeight());
	}
	
	public int getColumnCount(){
		return (int) Math.ceil((double) getMapWidth() / getTileWidth());
	}
	
	public String getTilePath(int col, int row){
		String path = pattern;
		path = path.replace("%col%", "" + col);
		path = path.replace("%row%", "" + row);
		return path;
	}
	
	
}
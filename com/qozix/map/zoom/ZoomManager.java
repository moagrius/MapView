package com.qozix.map.zoom;

import java.util.ArrayList;

public class ZoomManager {
	
	private static double BASE_2 = Math.log(2);
	
	private ArrayList<ZoomLevel> levels = new ArrayList<ZoomLevel>();
	private int currentZoomIndex;
	
	private int storedBaseMapWidth = 0;
	private int storedBaseMapHeight = 0;
	
	public ZoomManager(){
		
	}
	
	public int getBaseMapWidth(){
		return levels.get(0).getMapWidth();
	}
	
	public int getBaseMapHeight(){
		return levels.get(0).getMapHeight();
	}
	
	public void setBaseMapWidth(int width){
		storedBaseMapWidth = width;
		if(levels.size() > 0){
			levels.get(0).setMapWidth(width);
		}
	}
	
	public void setBaseMapHeight(int height){
		storedBaseMapHeight = height;
		if(levels.size() > 0){
			levels.get(0).setMapHeight(height);
		}
	}
	
	public void setBaseMapSize(int width, int height){
		setBaseMapWidth(width);
		setBaseMapHeight(height);
	}
	
	public void addZoomLevel(String pattern){
		addZoomLevel(pattern, null);
	}
	
	public void addZoomLevel(String pattern, String sample){
		int width = 0;
		int height = 0;
		int numLevels = levels.size();
		if(numLevels > 0){
			ZoomLevel baseLevel = levels.get(0);
			width = baseLevel.getMapWidth() >> numLevels;
			height = baseLevel.getMapHeight() >> numLevels;
		} else {
			width = storedBaseMapWidth;
			height = storedBaseMapHeight;
		}
		addZoomLevel(width, height, pattern, sample);
	}
	
	public void addZoomLevel(int wide, int tall, String pattern, String sample){
		ZoomLevel level = new ZoomLevel(wide, tall, pattern, sample);
		levels.add(level);
		setZoom(0);
	}
	
	public void addZoomLevel(int wide, int tall, String pattern, String sample, int tileWidth, int tileHeight){
		ZoomLevel level = new ZoomLevel(wide, tall, pattern, sample, tileWidth, tileHeight);
		levels.add(level);
		setZoom(0);
	}
	
	public void setZoom(int z){
		z = Math.max(z, 0);
		z = Math.min(z, levels.size() - 1);
		currentZoomIndex = z;
	}
	
	public int getZoom(){
		return currentZoomIndex;
	}
	
	public int getMaxZoom(){
		return levels.size() - 1;
	}
	
	public ZoomLevel getZoomLevel(){
		return levels.get(currentZoomIndex);
	}
	
	public boolean hasZoomAtLevel(int i){
		try {
			ZoomLevel l = levels.get(i);
			return (l != null);
		} catch(Exception e){
			
		}
		return false;
	}
	
	public boolean canZoomUp(){
		return (currentZoomIndex < (levels.size() - 1)) && hasZoomAtLevel(currentZoomIndex + 1);
	}
	
	public boolean canZoomDown(){
		return (currentZoomIndex > 0) && hasZoomAtLevel(currentZoomIndex - 1);
	}
	
	public ZoomLevel zoomUp(){
		if(canZoomUp()){
			setZoom(currentZoomIndex + 1);
		}
		return getZoomLevel();
	}
	
	public ZoomLevel zoomDown(){
		if(canZoomDown()){
			setZoom(currentZoomIndex - 1);
		}
		return getZoomLevel();
	}
	
	public int getZoomPower(){
		return 1 << (currentZoomIndex - 1);
	}
	
	public int getActualScale(){
		return 1 << currentZoomIndex;
	}
	
	public float getComputedScale(double factor){
		return (float) (getActualScale() + factor);
	}
	
	public ZoomSet getConfigFromScale(double factor){
		int currentLevel = getZoom();
		int maxLevel = getMaxZoom();
		int newLevel = currentLevel + (int) Math.ceil(Math.log(factor) / BASE_2);
	    newLevel = Math.max(newLevel, 0);
	    newLevel = Math.min(newLevel, maxLevel);
	    double newScale = factor * Math.pow(2, currentLevel - newLevel);
        return new ZoomSet(newLevel, newScale);	    
	}
	
	// take an actual scale and get zoom level and relative scale
	public ZoomSet computeZoomSet(double factor){  // DEBUG reversed entire order
		int level = (int) (0 - Math.round(Math.log(factor) / BASE_2));
		double scale = factor / Math.pow(2, -level);
		return new ZoomSet(level, scale);
	}
	
	// take zoom level and relative scale to get back actual scale
	public float getScaleForZoom(float factor){
		return (float) (factor / Math.pow(2, -getZoom()));
	}
	
	public double getScale(ZoomSet set){  // debug - probably needs swapped
		return Math.pow(2, -set.level) * set.scale;
	}
	
	
	
	
}

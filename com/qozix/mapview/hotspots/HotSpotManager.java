package com.qozix.mapview.hotspots;

import java.util.Iterator;
import java.util.LinkedList;

import android.graphics.Point;
import android.graphics.Rect;
import android.view.View;

public class HotSpotManager {

	public LinkedList<HotSpot> spots = new LinkedList<HotSpot>();
	
	public void addHotSpot( Rect r, View.OnClickListener l ){
		HotSpot hotSpot = new HotSpot( r, l );
		spots.add( hotSpot );
	}
	
	public void removeHotSpot( Rect r, View.OnClickListener l ){
		HotSpot comparison = new HotSpot( r, l );
		Iterator<HotSpot> iterator = spots.iterator();
		while(iterator.hasNext()){
			HotSpot hotSpot = iterator.next();
			if(comparison.equals( hotSpot )){
				iterator.remove();
			}
		}
	}
	
	public void clear(){
		spots.clear();
	}
	
	// work from end of list - match the last one added (equivalant to z-index)
	private HotSpot getMatch( Point point ){
		for(int i = spots.size(); i > 0; i--){
			HotSpot hotSpot = spots.get( i - 1);
			if(hotSpot.area.contains( point.x, point.y )){
				return hotSpot;
			}
		}
		return null;
	}
	
	public void processHit( Point point ){
		HotSpot match = getMatch( point );
		if( match != null){
			match.listener.onClick( null );
		}
	}
}

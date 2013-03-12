package com.qozix.mapview.viewmanagers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import android.view.View;
import android.view.ViewParent;

public class ViewSetManager {

	private HashMap<Integer, HashSet<View>> map = new HashMap<Integer, HashSet<View>>();
	
	public HashSet<View> getSetAtLevel( int level ) {
		if(!map.containsKey( level )){
			HashSet<View> viewSet = new HashSet<View>();
			map.put( level, viewSet );
		}
		return map.get( level );
	}
	
	public void addViewAtLevel( View view, int level ){
		HashSet<View> viewSet = getSetAtLevel( level );
		viewSet.add( view );
	}
	
	public void removeViewAtLevel( View view, int level ) {
		HashSet<View> viewSet = map.get( level );
		viewSet.remove( view );
	}
	
	public void removeAllViewsAtLevel( int level ) {
		map.remove( level );
	}
	
	public boolean removeView( View view ){
		for(Entry<Integer, HashSet<View>> e : map.entrySet()){
			HashSet<View> viewSet = e.getValue();
			Iterator<View> iterator = viewSet.iterator();
			while(iterator.hasNext()) {
				View comparison = iterator.next();
				if( comparison == view ) {
					iterator.remove();
					return true;
				}
			}
		}
		return false;
	}
	
	// remove all views from all sets that aren't in the view tree
	public void purgeViewSets() {
		for(Entry<Integer, HashSet<View>> e : map.entrySet()){
			HashSet<View> viewSet = e.getValue();
			Iterator<View> iterator = viewSet.iterator();
			while(iterator.hasNext()) {
				View view = iterator.next();
				ViewParent parent = view.getParent();
				if( parent == null ) {
					iterator.remove();
				}
			}
		}
	}
	
	public void updateDisplay( int level ){
		for(Map.Entry<Integer, HashSet<View>> e : map.entrySet()) {
			Integer intendedLevel = e.getKey();
			HashSet<View> viewSet = e.getValue();
			int visibility = intendedLevel.equals( level ) ? View.VISIBLE : View.GONE;
			for(View v : viewSet) {
				v.setVisibility( visibility );
			}
		}
	}
}

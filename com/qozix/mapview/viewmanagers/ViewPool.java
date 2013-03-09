package com.qozix.mapview.viewmanagers;

import java.util.LinkedList;

import android.content.Context;
import android.util.Log;
import android.view.View;

public class ViewPool<E extends View> {

	private ViewFactory<E> factory;
	private LinkedList<View> employed = new LinkedList<View>();
	private LinkedList<View> unemployed = new LinkedList<View>();

	public ViewPool( ViewFactory<E> f ) {
		factory = f;
	}

	public View employView( Context context ) {
		View v;
		if ( unemployed.size() > 0 ) {
			v = unemployed.get( 0 );
			unemployed.remove( v );
		} else {
			v = factory.fetch( context );
		}
		employed.add( v );
		Log.d( "ViewPool", "employed.size=" + employed.size() + ", unemployed.size=" + unemployed.size());
		return v;
	}

	public void retireView( View v ) {
		if ( employed.contains( v ) ) {
			employed.remove( v );
			unemployed.add( v );
		}
	}

}
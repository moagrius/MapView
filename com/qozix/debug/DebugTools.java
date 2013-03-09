package com.qozix.debug;

import java.util.Collection;

import android.util.Log;

public class DebugTools {
	
	private static final String TAG = DebugTools.class.getSimpleName();

	private DebugTools(){
		
	}
	
	public static void log( Object... args ) {
		StringBuilder sb = new StringBuilder();
		for ( Object o : args ) {
			sb.append( o.toString() );
		}
		Log.d( TAG, sb.toString() );
	}
	
	public static void logCollection( Collection<?> c ) {
		StringBuilder sb = new StringBuilder();
		for( Object o : c ) {
			sb.append( o.toString() );
		}
		log( sb.toString() );
	}
}

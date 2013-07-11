package com.qozix.mapview.viewmanagers;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;

import com.qozix.mapview.tiles.MapTileDecoder;
import com.qozix.mapview.tiles.MapTileDecoderAssets;

public class DownsampleManager {

	private MapTileDecoder decoder = new MapTileDecoderAssets();
	
	private String lastFileName;
	
	public void setDecoder( MapTileDecoder d ){
		decoder = d;
	}
	
	public void setDownsample( View view, String fileName ) {		
		if ( fileName == null ) {
			setDownsampleBackground( view, null );
			lastFileName = null;
			return;
		}
		if ( fileName.equals( lastFileName )) {
			return;
		}		
		lastFileName = fileName;
		Context context = view.getContext();
		Bitmap bitmap = decoder.decode( fileName, context );
		BitmapDrawable bitmapDrawable = new BitmapDrawable( context.getResources(), bitmap );
		setDownsampleBackground( view, bitmapDrawable );
	}
	
	// suppress deprecation because we're doing the only thing we can do with Android breaking API
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	@SuppressWarnings("deprecation")
	private void setDownsampleBackground( View view, Drawable drawable ){
		if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
			view.setBackgroundDrawable( drawable );
		} else {
			view.setBackground( drawable );
		}
	}
}

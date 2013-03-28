package com.qozix.mapview.viewmanagers;

import java.io.InputStream;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.View;

public class DownsampleManager {

	private static final BitmapFactory.Options OPTIONS = new BitmapFactory.Options();
	static {
		OPTIONS.inPreferredConfig = Bitmap.Config.RGB_565;
	}
	
	private String lastFileName;
	
	// NFI why this is required - caused by if ( fileName == null )
	@SuppressWarnings("unused")
	public void setDownsample( View view, String fileName ) {		
		if ( fileName.equals( lastFileName )) {
			return;
		}		
		lastFileName = fileName;
		if ( fileName == null ) {
			view.setBackgroundDrawable( null );
			return;
		}
		Context context = view.getContext();
		AssetManager assets = context.getAssets();
		try {
			InputStream input = assets.open( fileName );
			if ( input != null ) {
				try {
					Bitmap bitmap = BitmapFactory.decodeStream( input, null, OPTIONS );
					BitmapDrawable bitmapDrawable = new BitmapDrawable( bitmap );
					view.setBackgroundDrawable( bitmapDrawable );
				} catch( Exception e ) {
					
				}
			}
		} catch (Exception e ) {
			
		}
	}
}

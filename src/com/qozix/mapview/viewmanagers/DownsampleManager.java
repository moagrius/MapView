package com.qozix.mapview.viewmanagers;

import java.io.InputStream;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.View;

import com.qozix.mapview.tiles.MapTileDecoder;
import com.qozix.mapview.tiles.MapTileDecoderAssets;

public class DownsampleManager {

	private static final BitmapFactory.Options OPTIONS = new BitmapFactory.Options();
	static {
		OPTIONS.inPreferredConfig = Bitmap.Config.RGB_565;
	}
	
	private String lastFileName;
	private MapTileDecoder decoder = new MapTileDecoderAssets();
	
	public void setDecoder( MapTileDecoder d ){
		if (d == null)
			throw new IllegalArgumentException("d cannot be NULL.");
		decoder = d;
	}
	
	public void setDownsample( View view, String fileName ) {		
		if ( fileName == null ) {
			view.setBackgroundDrawable( null );
			lastFileName = null;
			return;
		}
		// TODO: Maybe the tile decoder should decide this?
		if ( fileName.equals( lastFileName )) {
			return;
		}		
		lastFileName = fileName;
		Context context = view.getContext();
		
		try {
			Bitmap bitmap = decoder.decode(fileName, context);
			
			if (bitmap != null) {
				BitmapDrawable bitmapDrawable = new BitmapDrawable( bitmap );
				view.setBackgroundDrawable( bitmapDrawable );
			}
		} catch (Exception e ) {
			// TODO: Swallowing exceptions is not a good idea IMO
		}
	}
}

package com.qozix.mapview.tiles;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * Implementation of MapTileDecoder that loads bitmaps from a HTTP server
 * thanks to https://github.com/mohlendo for pointing out the need and a solution
 */
public class MapTileDecoderHttp implements MapTileDecoder {

	private static final BitmapFactory.Options OPTIONS = new BitmapFactory.Options();
	static {
		OPTIONS.inPreferredConfig = Bitmap.Config.RGB_565;
	}
	
	@Override
	public Bitmap decode( String fileName, Context context ) {
		
		try {
			URL url = new URL(fileName);
			try {
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				InputStream input = connection.getInputStream();
				if ( input != null ) {
					try {
						return BitmapFactory.decodeStream( input, null, OPTIONS );										
					} catch ( OutOfMemoryError oom ) {
						// oom - you can try sleeping (this method won't be called in the UI thread) or try again (or give up)
					} catch ( Exception e ) {
						// unknown error decoding bitmap
					}
				}
			} catch ( IOException e ) {
				// io error
			}			
		} catch ( MalformedURLException e1 ) {
			// bad url
		}
		return null;
	}


}

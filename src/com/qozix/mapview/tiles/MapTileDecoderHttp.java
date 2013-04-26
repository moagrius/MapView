package com.qozix.mapview.tiles;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

/**
 * Implementation of MapTileDecoder that loads bitmaps from a HTTP server
 * thanks to https://github.com/mohlendo for pointing out the need and a solution
 */
public class MapTileDecoderHttp implements MapTileDecoder {

    private static final String TAG =  MapTileDecoderHttp.class.getSimpleName();

	private static final BitmapFactory.Options OPTIONS = new BitmapFactory.Options();
	static {
		OPTIONS.inPreferredConfig = Bitmap.Config.RGB_565;
	}
	
	@Override
	public Bitmap decode( String fileName, Context context ) {
        URL url;
		try {
            url = new URL(fileName);
        } catch (MalformedURLException e) {
            Log.e(TAG, "Your URL '" + fileName + "' is not an URL");
            return null;
        }

        HttpURLConnection connection = null;
        InputStream input = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            input = connection.getInputStream();
            if (input != null) {
                try {
                    return BitmapFactory.decodeStream( input, null, OPTIONS );
                } catch ( OutOfMemoryError oom ) {
                    // oom - you can try sleeping (this method won't be called in the UI thread) or try again (or give up)
                } catch ( Exception e ) {
                    // unknown error decoding bitmap
                }
            }
        } catch ( IOException e ) {
            Log.e(TAG, "Cannot download tile for URL: " + fileName, e);
        } finally {
            //close the stream and url connection
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    // ignore
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
        return null;
	}
}

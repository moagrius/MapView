package com.qozix.mapview.viewmanagers;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.View;
import com.qozix.mapview.tiles.MapTileCache;

public class DownsampleManager {

	private static final BitmapFactory.Options OPTIONS = new BitmapFactory.Options();
	static {
		OPTIONS.inPreferredConfig = Bitmap.Config.RGB_565;
	}
	
	private String lastFileName;
	
	public void setDownsample( View view, String fileName ) {		
		if ( fileName == null ) {
			view.setBackgroundDrawable( null );
			lastFileName = null;
			return;
		}
		if ( fileName.equals( lastFileName )) {
			return;
		}		
		lastFileName = fileName;
		Context context = view.getContext();

        InputStream input = null;
		try {
			if(fileName.startsWith("http")) {
                input = loadBitmapFromHttp(context, fileName);
            } else {
                input = loadBitmapFromAssetManager(context, fileName);
            }
			if ( input != null ) {
				try {
					Bitmap bitmap = BitmapFactory.decodeStream( input, null, OPTIONS );
					BitmapDrawable bitmapDrawable = new BitmapDrawable( bitmap );
					view.setBackgroundDrawable( bitmapDrawable );
				} catch( Exception e ) {
					
				}
			}
		} catch (Exception e ) {
			
		} finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    //igonore
                }
            }
        }
    }

    private InputStream loadBitmapFromAssetManager(Context context, String fileName) throws IOException {
        AssetManager assets = context.getAssets();
        return assets.open( fileName );
    }

    private InputStream loadBitmapFromHttp(Context context, String fileName) throws IOException {
        URL url = new URL(fileName);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        return connection.getInputStream();

    }
}

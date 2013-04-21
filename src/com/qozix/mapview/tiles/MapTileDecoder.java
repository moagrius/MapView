package com.qozix.mapview.tiles;

import android.content.Context;
import android.graphics.Bitmap;

public interface MapTileDecoder {

	public Bitmap decode( String fileName, Context context );

}

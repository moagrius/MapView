package com.qozix.mapdemo;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.qozix.map.MapView;

public class MapViewDemoActivity extends Activity {

	private MapView mapView;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
        super.onCreate(savedInstanceState);
        
        FrameLayout frame = new FrameLayout(this);
        setContentView(frame);
        
        int mapWidth = 6180;
        int mapHeight = 5072;
        
        mapView = new MapView(this);
        mapView.setBackgroundColor(0xFFF5F5F5);
        mapView.setTileTranstionEnabled(true);
        mapView.setTileTransitionDuration(400);
        mapView.setBaseMapSize(mapWidth, mapHeight);
        
		mapView.addZoomLevel("tiles/boston-1000-%col%_%row%.jpg", "downsamples/boston-pedestrian.jpg");
		mapView.addZoomLevel("tiles/boston-500-%col%_%row%.jpg", "downsamples/boston-overview.jpg");
		mapView.addZoomLevel("tiles/boston-250-%col%_%row%.jpg", "downsamples/boston-overview.jpg");
		mapView.addZoomLevel("tiles/boston-125-%col%_%row%.jpg", "downsamples/boston-overview.jpg");
		
		//Coordinate tl = new Coordinate(42.379676, -71.094919);
		//Coordinate br = new Coordinate(42.346550, -71.040280);
				
		mapView.setMarkerAnchorPoints(0.5f, 1.0f);
		mapView.registerGeolocator(42.379676, -71.040280, 42.346550, -71.094919);
		
		mapView.initialize();
		
		
		
		ImageView marker = new ImageView(this);
		marker.setImageResource(R.drawable.ic_launcher);
		mapView.addMarker(marker, 42.35848, -71.063736);
		
		
		
		mapView.addOnReadyListener(new Runnable(){
			@Override
			public void run() {
				mapView.moveToAndCenter(42.35848, -71.063736);
			}			
		});
		
		
		FrameLayout.LayoutParams mapViewLayout = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		frame.addView(mapView, mapViewLayout);
        
    }
}
package com.qozix.map.layers;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageView;

import com.qozix.map.layouts.FixedLayout;
import com.qozix.map.layouts.FixedLayout.LayoutParams;
import com.qozix.map.tiles.MapTile;
import com.qozix.map.tiles.MapTileView;
import com.qozix.map.tiles.MapTileViewFactory;
import com.qozix.map.zoom.ZoomLevel;


public class TileLayer extends FixedLayout {
	
	private ArrayList<MapTile> tiles = new ArrayList<MapTile>();
	private ArrayList<MapTile> showing = new ArrayList<MapTile>();
	private ArrayList<MapTile> onscreen = new ArrayList<MapTile>();
	private ArrayList<MapTile> offscreen = new ArrayList<MapTile>();
	
	private ArrayList<TransformationLayer> deadLayers = new ArrayList<TransformationLayer>();
	
	private boolean shouldTransitionTiles = true;
	private int transitionDuration = 250;
	
	private RenderTask renderer = null;
	
	private TransformationLayer activeLayer;
	private ImageView downsample = null;

	private boolean doneRendering = false;
	
	private BitmapFactory.Options bounds = new BitmapFactory.Options();
	{		
    	bounds.inPreferredConfig = Bitmap.Config.RGB_565;
	}
	
	private MapTileViewFactory factory = new MapTileViewFactory();
	
	private LayoutParams scalingLayerLayout = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 0, 0);

	private int tileWidth;
	private int tileHeight;
	
	public TileLayer(Context context){
		super(context);
	}
	
	public boolean isDoneRendering(){
		return doneRendering;
	}
	
	public void cancelRender(){
		if(renderer != null && renderer.getStatus() != AsyncTask.Status.FINISHED){
			renderer.cancel(true);
			renderer = null;
		}
	}
	
	public TransformationLayer getActiveLayer(){
		return activeLayer;
	}
	
	public void setScale(float factor){
		activeLayer.setScale(factor);
	}
	
	public void setTransitionDuration(int d){
		transitionDuration = d;
	}
	
	public void setTileTranstionEnabled(boolean shouldAnimate){
		shouldTransitionTiles = shouldAnimate;
	}
	
	public void update(ZoomLevel zl) {
		
		if(!doneRendering){
			cancelRender();
		}
		
		tileWidth = zl.getTileWidth();
		tileHeight = zl.getTileHeight();
		
		if(activeLayer != null){
			deadLayers.add(activeLayer);  // move the last layer into the dead pool
		}
	
		tiles.clear();  // empty the current tile set
		
		activeLayer = new TransformationLayer(getContext());  // create a new ScalingLayer to hold each MapTileView set
		
		addView(activeLayer, scalingLayerLayout);  // add the ScalingLayer to the TouchLayer
	
		
		// render downsample
		if(zl.hasDownsample()){
			
			boolean isFirstDownsample = downsample == null;
			
			Log.d("TileLayer", "hasDownsample");
			
			try {
				
				InputStream input = getContext().getAssets().open(zl.getDownsample());
				
				try {
					
					Bitmap bitmap = BitmapFactory.decodeStream(input, null, bounds);
					
					int mapWidth = zl.getMapWidth();
					int mapHeight = zl.getMapHeight();
					
					downsample = new ImageView(getContext());
					downsample.setAdjustViewBounds(true);
					downsample.setScaleType(ImageView.ScaleType.FIT_XY);
					downsample.setImageBitmap(bitmap);
					downsample.setMinimumHeight(mapHeight);
					downsample.setMinimumWidth(mapWidth);
					downsample.setMaxHeight(mapHeight);
					downsample.setMaxWidth(mapWidth);
					
					if(!isFirstDownsample){
						downsample.setVisibility(View.GONE);
					}
					
					LayoutParams downsampleLayout = new LayoutParams(mapWidth, mapHeight);
					
					activeLayer.addView(downsample, 0, downsampleLayout);	
					
					Log.d("TileLayer", "downsample send to UI");
					
				} catch(OutOfMemoryError oome){
					Log.d("TileLayer", "OOM on downsample");
				}
				
				
			} catch(IOException e){
				Log.d("TileLayer", "problem getting downsample: " + e.getMessage());
			}
			
		} else {
			
			Log.d("TileLayer", "no downsample for this ZoomLevel");
			
		}
		
		
		int tileWidth = zl.getTileWidth();
		int tileHeight = zl.getTileHeight();
		int rowCount = zl.getRowCount();
		int colCount = zl.getColumnCount();

		for(int r = 0; r <= rowCount; r++){
			
			for(int c = 0; c <= colCount; c++){

				MapTile tile = new MapTile();
				
				Rect rect = new Rect();
				rect.top = r * tileHeight;
				rect.left = c * tileWidth;
				rect.bottom = rect.top + tileHeight;
				rect.right = rect.left + tileWidth;
				
				tile.boundary = rect;
				tile.filename = zl.getTilePath(c, r);
				
				tiles.add(tile);
				
			}
		}		
		
	}
	
	public void renderTiles(Rect visible, float scale){
		
		onscreen.clear();
		offscreen.clear();
		
		for(MapTile tile : tiles){
			Rect scaled = tile.getScaledRect(scale);
			if(Rect.intersects(scaled, visible)) {
				if(!tile.showing){
					onscreen.add(tile);
				}
			} else {
				offscreen.add(tile);
			}
		}
		for(MapTile tile : offscreen){
			hideTile(tile);
		}

		cancelRender();
		
		renderer = new RenderTask();
		renderer.execute();
		
		doneRendering = false;
		
	}
	
	private boolean isCleaningUp = false;
	
	private void cleanup(){
		if(isCleaningUp){
			return;
		}
		isCleaningUp = true;
		for(TransformationLayer deadLayer : deadLayers){
			if(deadLayer != null){
				ArrayList<MapTileView> deadTiles = new ArrayList<MapTileView>();
				for(int i = 0; i < deadLayer.getChildCount(); i++){
					View child = deadLayer.getChildAt(i);
					if(child instanceof MapTileView){
						MapTileView mapTileView = (MapTileView) child;
						deadTiles.add(mapTileView);
					}
				}
				for(MapTileView deadTile : deadTiles){
					factory.retireView(deadTile);
					if(deadLayer.indexOfChild(deadTile) > -1){
						deadLayer.removeView(deadTile);
					}
				}
			}
			if(indexOfChild(deadLayer) > -1){				
				removeView(deadLayer);
			}
		}

		if(downsample != null){
			downsample.setVisibility(View.VISIBLE);
		}

		deadLayers.clear();
		
		isCleaningUp = false;
		
		postInvalidate();
		
	}
	
	public void hideTile(MapTile tile){
		if(!tile.showing){
			return;
		}
		tile.showing = false;	
		if(showing.contains(tile)){
			showing.remove(tile);
		}
		tile.bitmap = null;
		if(tile.view != null){
			tile.view.setImageBitmap(null);
			factory.retireView(tile.view);
			if(activeLayer.indexOfChild(tile.view) > -1){
				activeLayer.removeView(tile.view);
			} else {
				ViewGroup p = (ViewGroup) tile.view.getParent();
				if(p != null){
					p.removeView(tile.view);
				}
			}
			tile.view = null;
		}
	}
	
	private int numPendingAnimations = 0;
	
	private class RenderTask extends AsyncTask<Void, MapTile, Void>{
		
		@Override
		protected Void doInBackground(Void... params) {
			numPendingAnimations = 0;
			for(MapTile tile : onscreen){
				if(isCancelled()){
					break;
				}
				if(!tile.showing){
					tile.showing = true;
					try {
						InputStream input = getContext().getAssets().open(tile.filename);
						try {
							tile.bitmap = BitmapFactory.decodeStream(input, null, bounds);
						} catch(OutOfMemoryError oome){
							Log.d("TileLayer", "OOM");
						}
					} catch (IOException e) {
						
					}					    
				}
				doneRendering = false;
				numPendingAnimations++;
				publishProgress(tile);
			}			
			return null;
		}

		@Override
		protected void onProgressUpdate(MapTile... params) {
			if(!isCancelled()){
				MapTile tile = params[0];
				MapTileView view = factory.employView(getContext());
				view.setTile(tile);
				LayoutParams lp = new LayoutParams(tileWidth, tileHeight, tile.boundary.left, tile.boundary.top);
				activeLayer.addView(view, lp);
				if(shouldTransitionTiles){
					AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
					fadeIn.setDuration(transitionDuration);
					fadeIn.setFillAfter(true);
					fadeIn.setAnimationListener(fadeComplete);
					view.startAnimation(fadeIn);
				}
				
			}
		}
		
		@Override
		protected void onPostExecute(Void param){
			doneRendering = true;
			if(!shouldTransitionTiles){
				cleanup();			
			}
		}
		
	};
	
	private AnimationListener fadeComplete = new AnimationListener(){
		@Override
		public void onAnimationEnd(Animation a) {
			numPendingAnimations--;
			if(numPendingAnimations <= 0){
				cleanup();
			}
		}
		@Override
		public void onAnimationRepeat(Animation a) {
			
		}
		@Override
		public void onAnimationStart(Animation a) {
			
		}		
	};
	
}


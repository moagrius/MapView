package com.qozix.mapview.tiles;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import android.os.AsyncTask;
import android.util.Log;

class TileRenderTask extends AsyncTask<Void, MapTile, Void> {

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final WeakReference<TileManager> reference;
    private final AtomicInteger numberOfTilesToRender = new AtomicInteger();

    // package level access
	TileRenderTask( TileManager tm ) {
		super();
		reference = new WeakReference<TileManager>( tm );
	}
	
	@Override
	protected void onPreExecute() {
		final TileManager tileManager = reference.get();
		if ( tileManager != null ) {
			tileManager.onRenderTaskPreExecute();
		}		
	}

	@Override
	protected Void doInBackground( Void... params ) {
		// have we been stopped or dereffed?
		TileManager tileManager = reference.get();
		// if not go ahead, but check again in each iteration
		if ( tileManager != null ) {
			// avoid concurrent modification exceptions by duplicating
			LinkedList<MapTile> renderList = tileManager.getRenderList();
			// start rendering, checking each iteration if we need to break out
			for ( MapTile m : renderList ) {
				// check again if we've been stopped or gc'ed
				tileManager = reference.get();
				if ( tileManager == null ) {
					return null;
				}
				// quit if we've been forcibly stopped
				if ( tileManager.getRenderIsCancelled() ) {
					return null;
				}
				// quit if task has been cancelled or replaced
				if ( isCancelled() ) {
					return null;
				}

                // create new AsyncTask for each tile and render them
                new AsyncTask<MapTile, Void, MapTile>() {

                    @Override
                    protected MapTile doInBackground(MapTile... params) {
                        Log.d("MapTile render", Thread.currentThread().getName());
                        numberOfTilesToRender.incrementAndGet();
                        TileManager tileManager = reference.get();
                        if ( tileManager == null ) {
                            return null;
                        }
                        // quit if we've been forcibly stopped
                        if ( tileManager.getRenderIsCancelled() ) {
                            return null;
                        }
                        // quit if task has been cancelled or replaced
                        if ( isCancelled() ) {
                            return null;
                        }
                        MapTile mapTile = params[0];
                        // decode the map tile bitmap
                        tileManager.decodeIndividualTile(mapTile);
                        return mapTile;
                    }

                    @Override
                    protected void onPostExecute(MapTile mapTile) {
                        // stop if tile is null
                        if (mapTile == null) {
                            return;
                        }
                        TileManager tileManager = reference.get();
                        if(tileManager != null) {
                            // if not cancelled render the tile
                            if(!tileManager.getRenderIsCancelled()) {
                                tileManager.renderIndividualTile(mapTile);
                            }

                            // when we have finished every render task, inform the manager
                            if(numberOfTilesToRender.decrementAndGet() == 0) {
                                tileManager.onRenderTaskPostExecute();
                            }
                        }
                    }
                }.executeOnExecutor(executor, m);
			}
		}		
		return null;
	}


	@Override
	protected void onCancelled() {
        executor.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!executor.awaitTermination(30, TimeUnit.SECONDS))
                    System.err.println("Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            executor.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }

		// have we been stopped or dereffed?
		TileManager tileManager = reference.get();
		// if not go ahead but check other cancel states
		if ( tileManager != null ) {
			tileManager.onRenderTaskCancelled();
		}
	}
}
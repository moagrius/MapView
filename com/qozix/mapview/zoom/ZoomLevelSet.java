package com.qozix.mapview.zoom;

import java.util.Collections;
import java.util.LinkedList;

/*
 * this collection should be:
 * 1. Unique
 * 2. Sorted
 * 3. Indexed
 * So it's either a TreeSet with a get(int index) method
 * or a LinkedList with uniqueness and sorting implemented
 * Since adding ZoomLevels is likely to be infrequent, and
 * done outside of heavy rendering work on the UI thread,
 * and fetching might happen rapidly and repeatedly in 
 * response to user or touch events, we're opting for
 * the sorted List (for now)
 */
 

public class ZoomLevelSet extends LinkedList<ZoomLevel> {
	
	private static final long serialVersionUID = -1742428277010988084L;

	public void addZoomLevel( ZoomLevel zoomLevel ) {
		// ensure uniqueness
		if( contains( zoomLevel ) ) {
			return;
		}
		add( zoomLevel );
		// sort it
		Collections.sort( this );
	}
	
}

/*
public class ZoomLevelSet extends TreeSet<ZoomLevel> {

	private static final long serialVersionUID = -4028578060932067224L;

	public ZoomLevel get( int index ) {
		if ( index >= ( size() - 1) ) {
			return null;
		}
		if ( index < 0 ) {
			return null;
		}
		Iterator<ZoomLevel> i = iterator();
		while( i.hasNext() ) {
			ZoomLevel e = i.next();
			if ( index-- == 0 ) {
				return e;
			}
		}
		return null;
	}
}
*/


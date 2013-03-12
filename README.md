<p><strong>Update (March 8, 2013):</strong></p>

<p>While this component is still in beta, the version now available should behave predictably.</p>
<p>
  This commit entails a complete rewrite to more closely adhere to Android framework conventions, especially as regards layout mechanics.
  The changes were more significant than even a major version update would justify, and in practice this version should be considered an
  entirely new component.  Since we're still debugging I'm not bothering to deprecate methods or even make any attempt at backwards compatability;
  the previous version in it's entirely should be considered deprecated, and replaced with this release.
</p>
<p>
  The <a href="http://moagrius.github.com/MapView/documentation/reference/com/qozix/mapview/MapView.html">documentation</a> has been updated as well.
</p>

<p>An quick-n-dirty, undated, unversioned and incomplete changelog:</p>

<ol>
  <li>
    The component no longer requires (or even supports) initialization.  Rendering is throttled through handlers and managed directly
    via onLayout and event listeners.  This also means no more "onReady" anything - it just runs.  Just add some zoom levels and start using it.  In theory, zoom levels can be added dynamically later
    (after the code block it was instantiated - e.g., through a user action), but this is untested.
  </li>
  <li>
    Zoom levels are now managed in a TreeSet, so are naturally unique and ordered by area (so they can now be added in any order, and you're no
    longer required to add them smallest-to-largest)
  </li>
  <li>
    Image tiles now use caching, both in-memory (25% of total space available) and on-disk (up to 8MB).  For the on-disk cache, you'll now need to include
    external-write permission: <pre>&lt;uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" /&gt;</pre>.
    Testing for permission is on the todo list - right now you'll just get a runtime failure.
  </li>
  <li>
    Now supports multiple listeners (addMapEventListener instead of setMapEventListener).  Note that the interface is now MapEventListener, not OnMapEventListener.
    We now also provide access to a lot more events, including events for the rendering process and zoom events (where zoom indicates a change in zoom level, 
    in addition to the already existing events when the scale changed)
  </li>
  <li>
    We now intercept touch move events, so Markers won't prevent dragging.  The platform is still janky about consuming and bubbling events, but I think this is about as good as it's going to get.
  </li>
  <li>
    Removed support for downsamples.  That behavior wasn't visible in most other similar programs (e.g., google maps, CATiledLayer); it seemed to confuse some people; and it really hurt performance.
    I may consider a single low-res image as a background (rather than a different downsample per zoom level), depending on user feedback.
  </li>
  <li>
    Removed support for tile transitions.  There was no way to keep this from eating up too much memory when abused (fast, repeated pinches between zoom levels), when we wanted to keep the previous
    tile set visible until the new one was rendered.
  </li>
  <li>
    Added support for zoom-level specific Markers and Paths - you can now specify a zoom index to addMarker/Path calls, which will hide those Views on all other zoom levels.
  </li>
  <li>
    Drastic refactorization and optimization of the core classes.  
  </li>
</ol>

<p>
  If you test this on a device, please let me know the results - in the case of either success or failure.  I've personally run it on several devices running several different versions of the OS, but
  would be very interested in results from the wild.  I'll be working on putting up a jar and a sample app.
</p>

<p>Finally, thanks to everyone that's been in touch with comments and ideas on how to make this widget better.  I appreciate all the input</p>

<h1>MapView</h1>
<p>The MapView widget is a subclass of ViewGroup that provides a mechanism to asynchronously display tile-based images,
 with additional functionality for 2D dragging, flinging, pinch or double-tap to zoom, adding overlaying Views (markers),
 multiple levels of detail, and support for faux-geolocation (by specifying top-left and bottom-right coordinates).</p>
 
 <p>It might be best described as a hybrid of com.google.android.maps.MapView and iOS's CATiledLayer, and is appropriate for a variety of uses
 but was intended for map-type applications, especially high-detail or custom implementations (e.g., inside a building).</p>
 
 <p>A minimal implementation might look like this:</p>
  
 <pre>MapView mapView = new MapView(this);
mapView.addZoomLevel(1440, 900, "path/to/tiles/%col%-%row%.jpg");</pre>
 
 A more advanced implementation might look like this:
 <pre>MapView mapView = new MapView(this);
mapView.registerGeolocator(42.379676, -71.094919, 42.346550, -71.040280);
mapView.addZoomLevel(6180, 5072, "tiles/boston-1000-%col%_%row%.jpg", 512, 512);
mapView.addZoomLevel(3090, 2536, "tiles/boston-500-%col%_%row%.jpg", 256, 256);
mapView.addZoomLevel(1540, 1268, "tiles/boston-250-%col%_%row%.jpg", 256, 256);
mapView.addZoomLevel(770, 634, "tiles/boston-125-%col%_%row%.jpg", 128, 128);
mapView.addMarker(someView, 42.35848, -71.063736);
mapView.addMarker(anotherView, 42.3665, -71.05224);
</pre>

<h4>Installation</h4>
<p>
  The widget is straight java, so you can just use the .java files found here (with the dependencies mentioned below), or you can download
  <a href="http://moagrius.github.com/MapView/mapviewlib.jar">the jar</a>.
  Simple instructions are available <a target="_blank" href="http://moagrius.github.com/MapView/installation.html">here</a>.
</p>

<h4>Dependencies</h4>
<p>
  If you're targetting APIs less than 12, you'll need the 
  <a target="_blank" href="http://developer.android.com/tools/extras/support-library.html">Android compatability lib</a>
  for the LruCache implementation.
</p>
<p>
  <a target="_blank" href="https://github.com/JakeWharton/DiskLruCache">Jake Wharton's DiskLruCache</a> is also used.
  <a target="_blank" href="https://oss.sonatype.org/content/repositories/releases/com/jakewharton/disklrucache/1.3.1/disklrucache-1.3.1.jar">Here's</a> a direct link to that jar.
  However, that package is bundled with mapviewlib.jar so is only needed if you're using the java files directly in your project.
</p>

<h4>Maven users</h4>
```xml
<dependency>
	<groupId>com.github.moagrius</groupId>
	<artifactId>MapView</artifactId>
	<version>1.0.0</version>
</dependency>
```

<h4>Documentation</h4>
<p>Javadocs are <a href="http://moagrius.github.com/MapView/documentation/reference/com/qozix/mapview/MapView.html">here</a>.</p>

<h4>License</h4>
<p>Licensed under <a href="http://creativecommons.org/licenses/by/3.0/legalcode" target="_blank">Creative Commons</a></p>
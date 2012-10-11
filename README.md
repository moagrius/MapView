<p><strong>Update:</strong> This lib is still very early beta, and updating for public use has introduced some bugs and invalid signatures.  Any addZoomLevel signature is failing without a valid asset image path provided for the @downsample parameter.  The complete addZoomLevel signature should be preferred (with or without tilesize).  Updates to come.</p>

<h1>MapView</h1>
<p>The MapView widget is a subclass of ViewGroup that provides a mechanism to asynchronously display tile-based images,
 with additional functionality for 2D dragging, flinging, pinch or double-tap to zoom, adding overlaying Views (markers),
 multiple levels of detail, and support for faux-geolocation (by specifying top-left and bottom-right coordinates).
 
 <p>It might be best described as a hybrid of com.google.android.maps.MapView and iOS's CATiledLayer, and is appropriate for a variety of uses
 but was intended for map-type applications, especially high-detail or custom implementations (e.g., inside a building).</p>
 
 <p>Configuration can be very simple, but most configuration options can be set with instance methods.</p>
 
 <p>A minimal implementation might look like this:</p>
  
 <pre>MapView mapView = new MapView(this);
mapView.setBaseMapSize(1440, 900);
mapView.addZoomLevel("path/to/tiles/%col%-%row%.jpg");
mapView.initialize();</pre>
 
 A more advanced implementation might look like this:
 <pre>MapView mapView = new MapView(this);
mapView.addOnReadyRunnable(someRunnable);
mapView.addOnReadyRunnable(anotherRunnable);
mapView.setTileTransitionDuration(1000);
mapView.setMapEventListener(someMapEventListener);
mapView.registerGeolocator(42.379676, -71.094919, 42.346550, -71.040280);
mapView.addZoomLevel(6180, 5072, "tiles/boston-1000-%col%_%row%.jpg","downsamples/boston-pedestrian.jpg", 256, 256);
mapView.addZoomLevel(3090, 2536, "tiles/boston-500-%col%_%row%.jpg", "downsamples/boston-overview.jpg", 256, 256);
mapView.addZoomLevel(1540, 1268, "tiles/boston-250-%col%_%row%.jpg", "downsamples/boston-overview.jpg", 256, 256);
mapView.addZoomLevel(770, 634, "tiles/boston-125-%col%_%row%.jpg", "downsamples/boston-overview.jpg", 128, 128);
mapView.initialize();
mapView.addMarker(someView, 42.35848, -71.063736);
mapView.addMarker(anotherView, 42.3665, -71.05224);
</pre>

<p>Javadocs are <a href="http://moagrius.github.com/MapView/documentation">here</a>.

 <p>Licensed under <a href="http://creativecommons.org/licenses/by/3.0/legalcode" target="_blank">Creative Commons</a></p>
</p>
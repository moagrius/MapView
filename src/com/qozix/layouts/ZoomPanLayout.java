package com.qozix.layouts;

import java.lang.ref.WeakReference;
import java.util.HashSet;

import android.content.Context;
import android.graphics.Point;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;

import com.qozix.animation.Tween;
import com.qozix.animation.TweenListener;
import com.qozix.animation.easing.Strong;
import com.qozix.widgets.Scroller;

public class ZoomPanLayout extends ViewGroup {

	private static final String TAG = ZoomPanLayout.class.getSimpleName();

	private static final int MINIMUM_VELOCITY = 50;
	private static final int ZOOM_ANIMATION_DURATION = 500;
	private static final int SLIDE_DURATION = 500;
	private static final int VELOCITY_UNITS = 1000;
	private static final int DOUBLE_TAP_TIME_THRESHOLD = 250;
	private static final int SINGLE_TAP_DISTANCE_THRESHOLD = 50;
	private static final double MINIMUM_PINCH_SCALE = 0.5;
	private static final float FRICTION = 0.99f;	

	private int baseWidth;
	private int baseHeight;

	private int scaledWidth;
	private int scaledHeight;

	private double scale = 1;
	private double historicalScale = 1;

	private double minScale = 0.2;
	private double maxScale = 1;

	private boolean scaleToFit = true;

	private Point pinchStartScroll = new Point();
	private Point pinchStartOffset = new Point();
	private double pinchStartDistance;

	private Point doubleTapStartScroll = new Point();
	private Point doubleTapStartOffset = new Point();
	private double doubleTapDestinationScale;

	private Point firstFinger = new Point();
	private Point secondFinger = new Point();
	private Point lastFirstFinger = new Point();
	private Point lastSecondFinger = new Point();
	
	private Point scrollPosition = new Point();
	
	private Point singleTapHistory = new Point();
	private Point doubleTapHistory = new Point();
	
	private Point actualPoint = new Point();
	private Point destinationScroll = new Point();

	private boolean secondFingerIsDown = false;
	private boolean firstFingerIsDown = false;	

	private boolean isTapInterrupted = false;
	private boolean isBeingFlung = false;
	
	private long lastTouchedAt;
	
	private boolean shouldIntercept = false;
	
	private ScrollActionHandler scrollActionHandler;

	private Scroller scroller;
	private VelocityTracker velocity;

	private HashSet<GestureListener> gestureListeners = new HashSet<GestureListener>();
	private HashSet<ZoomPanListener> zoomPanListeners = new HashSet<ZoomPanListener>();

	private StaticLayout clip;

	private TweenListener tweenListener = new TweenListener() {
		@Override
		public void onTweenComplete() {
			isTweening = false;
			for ( ZoomPanListener listener : zoomPanListeners ) {
				listener.onZoomComplete( scale );
				listener.onZoomPanEvent();
			}
		}
		@Override
		public void onTweenProgress( double progress, double eased ) {
			double originalChange = doubleTapDestinationScale - historicalScale;
			double updatedChange = originalChange * eased;
			double currentScale = historicalScale + updatedChange;
			setScale( currentScale );
			maintainScrollDuringScaleTween();
		}
		@Override
		public void onTweenStart() {
			isTweening = true;
			for ( ZoomPanListener listener : zoomPanListeners ) {
				listener.onZoomStart( scale );
				listener.onZoomPanEvent();
			}
		}
	};

	private boolean isTweening;
	private Tween tween = new Tween();
	{
		tween.setAnimationEase( Strong.EaseOut );
		tween.addTweenListener( tweenListener );
	}

	public ZoomPanLayout( Context context ) {

		super( context );
		setWillNotDraw( false );
		
		scrollActionHandler = new ScrollActionHandler( this );

		scroller = new Scroller( context );
		scroller.setFriction( FRICTION );

		clip = new StaticLayout( context );
		super.addView( clip );

		updateClip();
	}

	@Override
	protected void onMeasure( int widthMeasureSpec, int heightMeasureSpec ) {
		measureChildren( widthMeasureSpec, heightMeasureSpec );
		int w = clip.getMeasuredWidth();
		int h = clip.getMeasuredHeight();
		w = Math.max( w, getSuggestedMinimumWidth() );
		h = Math.max( h, getSuggestedMinimumHeight() );
		w = resolveSize( w, widthMeasureSpec );
		h = resolveSize( h, heightMeasureSpec );
		setMeasuredDimension( w, h );
	}

	@Override
	protected void onLayout( boolean changed, int l, int t, int r, int b ) {
		clip.layout( 0, 0, clip.getMeasuredWidth(), clip.getMeasuredHeight() );
		if ( changed ) {
			calculateMinimumScaleToFit();
		}
	}

	public void setScaleToFit( boolean v ) {
		scaleToFit = v;
		calculateMinimumScaleToFit();
	}

	public void setScaleLimits( double min, double max ) {
		// if scaleToFit is set, don't allow overwrite
		if ( !scaleToFit ) {
			minScale = min;
		}
		maxScale = max;
		setScale( scale );
	}
	
	public void setShouldIntercept( boolean intercept ){
		shouldIntercept = intercept;
	}

	private void calculateMinimumScaleToFit() {
		if ( scaleToFit ) {
			double minimumScaleX = getWidth() / (double) baseWidth;
			double minimumScaleY = getHeight() / (double) baseHeight;
			double recalculatedMinScale = Math.max( minimumScaleX, minimumScaleY );
			if ( recalculatedMinScale != minScale ) {
				minScale = recalculatedMinScale;
				setScale( scale );
			}
		}
	}

	public void setSize( int wide, int tall ) {
		baseWidth = wide;
		baseHeight = tall;
		scaledWidth = (int) ( baseWidth * scale );
		scaledHeight = (int) ( baseHeight * scale );
		updateClip();
	}

	public int getBaseWidth() {
		return baseWidth;
	}

	public int getBaseHeight() {
		return baseHeight;
	}

	public int getScaledWidth() {
		return scaledWidth;
	}

	public int getScaledHeight() {
		return scaledHeight;
	}

	/**
	 * Sets the scale (0-1) of the ZoomPanLayout
	 * @param scale (double) The new value of the ZoomPanLayout scale
	 */
	public void setScale( double d ) {
		d = Math.max( d, minScale );
		d = Math.min( d, maxScale );
		if ( scale != d ) {
			scale = d;
			scaledWidth = (int) ( baseWidth * scale );
			scaledHeight = (int) ( baseHeight * scale );
			updateClip();
			invalidate();
			for ( ZoomPanListener listener : zoomPanListeners ) {
				listener.onScaleChanged( scale );
				listener.onZoomPanEvent();
			}
		}
	}

	/**
	 * Retrieves the current scale of the ZoomPanLayout
	 * @return (double) the current scale of the ZoomPanLayout
	 */
	public double getScale() {
		return scale;
	}
	
	public boolean isFlinging(){
		return isBeingFlung;
	}
	
	protected View getClip() {
		return clip;
	}

	private void updateClip() {
		updateViewClip( clip );
		for ( int i = 0; i < clip.getChildCount(); i++ ) {
			View child = clip.getChildAt( i );
			updateViewClip( child );
		}
		constrainScroll();
	}

	private void updateViewClip( View v ) {
		LayoutParams lp = v.getLayoutParams();
		lp.width = scaledWidth;
		lp.height = scaledHeight;
		v.setLayoutParams( lp );
	}

	public boolean addGestureListener( GestureListener listener ) {
		return gestureListeners.add( listener );
	}

	public boolean removeGestureListener( GestureListener listener ) {
		return gestureListeners.remove( listener );
	}

	public boolean addZoomPanListener( ZoomPanListener listener ) {
		return zoomPanListeners.add( listener );
	}

	public boolean removeZoomPanListener( ZoomPanListener listener ) {
		return zoomPanListeners.remove( listener );
	}

	@Override
	public void computeScroll() {
		if ( scroller.computeScrollOffset() ) {
			Point destination = new Point( scroller.getCurrX(), scroller.getCurrY() );
			scrollToPoint( destination );
			postInvalidate();  // should not be necessary but is...
			dispatchScrollActionNotification();
		}
	}
	
	private void dispatchScrollActionNotification(){
		if ( scrollActionHandler.hasMessages( 0 )) {
			scrollActionHandler.removeMessages( 0 );
		}
		scrollActionHandler.sendEmptyMessageDelayed( 0, 100 );
	}
	
	private void handleScrollerAction() {
		Point point = new Point();
		point.x = getScrollX();
		point.y = getScrollY();
		for( GestureListener listener : gestureListeners ) {
			listener.onScrollComplete( point );
		}
		if ( isBeingFlung ) {
			isBeingFlung = false;
			for( GestureListener listener : gestureListeners ) {
				listener.onFlingComplete( point );
			}
		}		
	}

	private void constrainPoint( Point point ) {
		int x = point.x;
		int y = point.y;
		int mx = Math.max( 0, Math.min( x, getLimitX() ) );
		int my = Math.max( 0, Math.min( y, getLimitY() ) );
		if ( x != mx || y != my ) {
			point.set( mx, my );
		}
	}

	public void scrollToPoint( Point point ) {
		constrainPoint( point );
		int ox = getScrollX();
		int oy = getScrollY();
		int nx = (int) point.x;
		int ny = (int) point.y;
		scrollTo( nx, ny );
		if ( ox != nx || oy != ny ) {
			for ( ZoomPanListener listener : zoomPanListeners ) {
				listener.onScrollChanged( nx, ny );
				listener.onZoomPanEvent();
			}
		}
	}

	public void scrollToAndCenter( Point point ) { // TODO:
		int x = (int) -(getWidth() * 0.5);
		int y = (int) -(getHeight() * 0.5);
		point.offset( x , y );
		scrollToPoint( point );
	}

	public void slideToPoint( Point point ) { // TODO:
		constrainPoint( point );
		int startX = getScrollX();
		int startY = getScrollY();
		int dx = point.x - startX;
		int dy = point.y - startY;
		scroller.startScroll( startX, startY, dx, dy, SLIDE_DURATION );
	}

	public void slideToAndCenter( Point point ) { // TODO:
		int x = (int) -(getWidth() * 0.5);
		int y = (int) -(getHeight() * 0.5);
		point.offset( x , y );
		slideToPoint( point );
	}

	private void constrainScroll() { // TODO:
		Point currentScroll = new Point( getScrollX(), getScrollY() );
		Point limitScroll = new Point( currentScroll );
		constrainPoint( limitScroll );
		if ( !currentScroll.equals( limitScroll ) ) {
			scrollToPoint( currentScroll );
		}
	}

	private int getLimitX() {
		return scaledWidth - getWidth();
	}

	private int getLimitY() {
		return scaledHeight - getHeight();
	}

	public void addChild( View child ) {
		LayoutParams lp = new LayoutParams( scaledWidth, scaledHeight );
		clip.addView( child, lp );
	}

	public void removeChild( View v ) {
		if ( clip.indexOfChild( v ) > -1 ) {
			clip.removeView( v );
		}
	}

	@Override
	public void addView( View child ) {
		throw new UnsupportedOperationException( "ZoomPanLayout does not allow direct addition of child views.  Use addChild() instead." );
	}

	@Override
	public void removeView( View child ) {
		throw new UnsupportedOperationException( "ZoomPanLayout does not allow direct removal of child views.  Use removeChild() instead." );
	}

	public void smoothScaleTo( double destination, int duration ) {
		if ( isTweening ) {
			return;
		}
		doubleTapDestinationScale = destination;
		tween.setDuration( duration );
		tween.start();
	}

	private void saveHistoricalScale() {
		historicalScale = scale;
	}

	private void savePinchHistory() {
		int x = (int) ( ( firstFinger.x + secondFinger.x ) * 0.5 );
		int y = (int) ( ( firstFinger.y + secondFinger.y ) * 0.5 );
		pinchStartOffset.set( x , y );
		pinchStartScroll.set( getScrollX(), getScrollY() );
		pinchStartScroll.offset( x, y );
	}

	private void maintainScrollDuringPinchOperation() {
		double deltaScale = scale / historicalScale;
		int x = (int) ( pinchStartScroll.x * deltaScale ) - pinchStartOffset.x;
		int y = (int) ( pinchStartScroll.y * deltaScale ) - pinchStartOffset.y;
		destinationScroll.set( x, y );
		scrollToPoint( destinationScroll );
	}

	private void saveDoubleTapHistory() {
		doubleTapStartOffset.set( firstFinger.x, firstFinger.y );
		doubleTapStartScroll.set( getScrollX(), getScrollY() );
		doubleTapStartScroll.offset( doubleTapStartOffset.x, doubleTapStartOffset.y );
	}

	private void maintainScrollDuringScaleTween() {
		double deltaScale = scale / historicalScale;
		int x = (int) ( doubleTapStartScroll.x * deltaScale ) - doubleTapStartOffset.x;
		int y = (int) ( doubleTapStartScroll.y * deltaScale ) - doubleTapStartOffset.y;
		destinationScroll.set( x, y );
		scrollToPoint( destinationScroll );
	}

	private void saveHistoricalPinchDistance() {
		int dx = firstFinger.x - secondFinger.x;
		int dy = firstFinger.y - secondFinger.y;
		pinchStartDistance = Math.sqrt( dx * dx + dy * dy );
	}

	private void setScaleFromPinch() {
		int dx = firstFinger.x - secondFinger.x;
		int dy = firstFinger.y - secondFinger.y;
		double pinchCurrentDistance = Math.sqrt( dx * dx + dy * dy );
		double currentScale = pinchCurrentDistance / pinchStartDistance;
		currentScale = Math.max( currentScale, MINIMUM_PINCH_SCALE );
		currentScale = historicalScale * currentScale;
		setScale( currentScale );
	}

	private void performDrag() {
		Point delta = new Point();
		if ( secondFingerIsDown && !firstFingerIsDown ) {
			delta.set( lastSecondFinger.x, lastSecondFinger.y );
			delta.offset( -secondFinger.x, -secondFinger.y );
		} else {
			delta.set( lastFirstFinger.x, lastFirstFinger.y );
			delta.offset( -firstFinger.x, -firstFinger.y );
		}
		scrollPosition.offset( delta.x, delta.y );
		scrollToPoint( scrollPosition );
	}

	private boolean performFling() {
		if ( secondFingerIsDown ) {
			return false;
		}
		velocity.computeCurrentVelocity( VELOCITY_UNITS );
		double xv = velocity.getXVelocity();
		double yv = velocity.getYVelocity();
		double totalVelocity = Math.abs( xv ) + Math.abs( yv );
		if ( totalVelocity > MINIMUM_VELOCITY ) {
			scroller.fling( getScrollX(), getScrollY(), (int) -xv, (int) -yv, 0, getLimitX(), 0, getLimitY() );
			invalidate();
			return true;
		}
		return false;
	}
	
	// if the taps occurred within threshold, it's a double tap
	private boolean determineIfQualifiedDoubleTap(){
		long now = System.currentTimeMillis();
		long ellapsed = now - lastTouchedAt;
		lastTouchedAt = now;
		return ( ellapsed <= DOUBLE_TAP_TIME_THRESHOLD )
			&& ( Math.abs( firstFinger.x - doubleTapHistory.x ) <= SINGLE_TAP_DISTANCE_THRESHOLD )
			&& ( Math.abs( firstFinger.y - doubleTapHistory.y ) <= SINGLE_TAP_DISTANCE_THRESHOLD );

	}
	
	private void saveTapActionOrigination(){
		singleTapHistory.set( firstFinger.x, firstFinger.y );
	}
	
	private void saveDoubleTapOrigination(){
		doubleTapHistory.set( firstFinger.x, firstFinger.y );
	}
	
	private void setTapInterrupted( boolean v ){
		isTapInterrupted = v;
	}
	
	// if the touch event has traveled past threshold since the finger first when down, it's not a tap
	private boolean determineIfQualifiedSingleTap(){
		return !isTapInterrupted
			&& ( Math.abs( firstFinger.x - singleTapHistory.x ) <= SINGLE_TAP_DISTANCE_THRESHOLD )
			&& ( Math.abs( firstFinger.y - singleTapHistory.y ) <= SINGLE_TAP_DISTANCE_THRESHOLD );
	}
	
	private void processEvent( MotionEvent event ) {
		
		// copy for history
		lastFirstFinger.set( firstFinger.x, firstFinger.y );
		lastSecondFinger.set( secondFinger.x, secondFinger.y );

		// set false for now
		firstFingerIsDown = false;
		secondFingerIsDown = false;

		// determine which finger is down and populate the appropriate points
		for ( int i = 0; i < event.getPointerCount(); i++ ) {
			int id = event.getPointerId( i );
			int x = (int) event.getX( i );
			int y = (int) event.getY( i );
			switch ( id ) {
			case 0 :
				firstFingerIsDown = true;
				firstFinger.set( x, y );
				actualPoint.set( x, y );
				break;
			case 1 :
				secondFingerIsDown = true;
				secondFinger.set( x, y );
				actualPoint.set( x, y );
				break;
			}
		}
		// record scroll position and adjust finger point to account for scroll offset
		scrollPosition.set( getScrollX(), getScrollY() );
		actualPoint.offset( scrollPosition.x, scrollPosition.y );

		// update velocity for flinging
		// TODO: this can probably be moved to the ACTION_MOVE switch
		if ( velocity == null ) {
			velocity = VelocityTracker.obtain();
		}
		velocity.addMovement( event );
	}
	
	@Override
	public boolean onInterceptTouchEvent (MotionEvent event) {
		// update positions
		processEvent( event);
		// if we wan't to intercept events (and allow drag on children)...
		if ( shouldIntercept ) {
			// get the type of action
			final int action = event.getAction() & MotionEvent.ACTION_MASK;		
			// if it's a move event...
			if ( action == MotionEvent.ACTION_MOVE ) {			
				// and capture it (so touch listeners on the children don't consume it and prevent scrolling)
				return true;
			}
			// otherwise, let the child handle it
		}		
		return false;
	}

	@Override
	public boolean onTouchEvent( MotionEvent event ) {
		// update positions
		processEvent( event );
		// get the type of action
		final int action = event.getAction() & MotionEvent.ACTION_MASK;
		// react based on nature of touch event
		switch ( action ) {
		// first finger goes down
		case MotionEvent.ACTION_DOWN :
			if ( !scroller.isFinished() ) {
				scroller.abortAnimation();
			}
			isBeingFlung = false;
			setTapInterrupted( false );
			saveTapActionOrigination();
			for ( GestureListener listener : gestureListeners ) {
				listener.onFingerDown( actualPoint );
			}
			break;
		// second finger goes down
		case MotionEvent.ACTION_POINTER_DOWN :
			setTapInterrupted( true );
			saveHistoricalPinchDistance();
			saveHistoricalScale();
			savePinchHistory();
			for ( GestureListener listener : gestureListeners ) {
				listener.onFingerDown( actualPoint );
			}
			for ( GestureListener listener : gestureListeners ) {
				listener.onPinchStart( pinchStartOffset );
			}
			for ( ZoomPanListener listener : zoomPanListeners ) {
				listener.onZoomStart( scale );
				listener.onZoomPanEvent();
			}
			break;
		// either finger moves
		case MotionEvent.ACTION_MOVE :
			// if both fingers are down, that means it's a pinch
			if ( firstFingerIsDown && secondFingerIsDown ) {
				setScaleFromPinch();
				maintainScrollDuringPinchOperation();
				for ( GestureListener listener : gestureListeners ) {
					listener.onPinch( pinchStartOffset );
				}
			// otherwise it's a drag
			} else {
				performDrag();
				for ( GestureListener listener : gestureListeners ) {
					listener.onDrag( actualPoint );
				}
			}
			break;
		// first finger goes up
		case MotionEvent.ACTION_UP :
			if ( performFling() ) {
				isBeingFlung = true;
				Point startPoint = new Point( getScrollX(), getScrollY() );
				Point finalPoint = new Point( scroller.getFinalX(), scroller.getFinalY() );
				for ( GestureListener listener : gestureListeners ) {
					listener.onFling( startPoint, finalPoint );
				}
			}
			if ( velocity != null ) {
				velocity.recycle();
				velocity = null;
			}			
			// could be a single tap...
			if ( determineIfQualifiedSingleTap() ){
				for ( GestureListener listener : gestureListeners ) {
					listener.onTap( actualPoint );
				}
			}
			// or a double tap
			if ( determineIfQualifiedDoubleTap() ) {
				saveHistoricalScale();
				saveDoubleTapHistory();
				double destination = Math.min( 1, scale * 2 );
				smoothScaleTo( destination, ZOOM_ANIMATION_DURATION ); 
				for ( GestureListener listener : gestureListeners ) {
					listener.onDoubleTap( actualPoint );
				}
			}
			// either way it's a finger up event
			for ( GestureListener listener : gestureListeners ) {
				listener.onFingerUp( actualPoint );
			}
			// save coordinates to measure against the next double tap
			saveDoubleTapOrigination();
			break;
		// second finger goes up
		case MotionEvent.ACTION_POINTER_UP :
			setTapInterrupted( true );
			for ( GestureListener listener : gestureListeners ) {
				listener.onFingerUp( actualPoint );
			}
			for ( GestureListener listener : gestureListeners ) {
				listener.onPinchComplete( pinchStartOffset );
			}
			for ( ZoomPanListener listener : zoomPanListeners ) {
				listener.onZoomComplete( scale );
				listener.onZoomPanEvent();
			}
			break;

		}

		return true;
		
	}
	
	private static class ScrollActionHandler extends Handler {
		private final WeakReference<ZoomPanLayout> reference;
		public ScrollActionHandler( ZoomPanLayout zoomPanLayout ) {
			super();
			reference = new WeakReference<ZoomPanLayout>( zoomPanLayout );
		}
		@Override
		public void handleMessage( Message msg ) {
			ZoomPanLayout zoomPanLayout = reference.get();
			if ( zoomPanLayout != null ) {
				zoomPanLayout.handleScrollerAction();
			}
		}
	}

	public static interface ZoomPanListener {
		public void onScaleChanged( double scale );
		public void onScrollChanged( int x, int y );
		public void onZoomStart( double scale );
		public void onZoomComplete( double scale );
		public void onZoomPanEvent();
	}

	public static interface GestureListener {
		public void onFingerDown( Point point );
		public void onScrollComplete( Point point );
		public void onFingerUp( Point point );
		public void onDrag( Point point );
		public void onDoubleTap( Point point );
		public void onTap( Point point );
		public void onPinch( Point point );
		public void onPinchStart( Point point );
		public void onPinchComplete( Point point );
		public void onFling( Point startPoint, Point finalPoint );
		public void onFlingComplete( Point point );
	}

}

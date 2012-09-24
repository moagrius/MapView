package com.qozix.map.layers;

import android.content.Context;
import android.graphics.Point;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.FrameLayout;

import com.qozix.map.animation.Animator;
import com.qozix.map.geom.XPoint;
import com.qozix.map.layouts.FixedLayout;
import com.qozix.map.layouts.FixedLayout.LayoutParams;
import com.qozix.map.listeners.TouchLayerEventListener;
import com.qozix.map.widgets.Scroller;

public class TouchLayer extends FixedLayout {

	private static final int DOUBLE_TAP_INTERVAL = 250;
	private static final int MINIMUM_VELOCITY = 50;
	private static final int ZOOM_ANIMATION_DURATION = 500;
	private static final int SLIDE_DURATION = 500;
	
	private int width;
	private int height;

	private float scale = 1;
	private float lastScale = 1;

	private double minScale = 0.2;
	private double maxScale = 1;
	
	private XPoint topLeft = new XPoint();
	private XPoint bottomRight = new XPoint();

	private int pinchStartDistance;
	
	private XPoint pinchStartScroll = new XPoint();
	private XPoint pinchStartOffset = new XPoint();
	
	private XPoint doubleTapStartScroll = new XPoint();
	private XPoint doubleTapStartOffset = new XPoint();
	
	private XPoint firstFinger = new XPoint();
	private XPoint secondFinger = new XPoint();
	private XPoint lastFirstFinger = new XPoint();
	private XPoint lastSecondFinger = new XPoint();
	private XPoint scrollPosition = new XPoint();
	private XPoint lastScrollPosition = new XPoint();
	
	private XPoint actualPoint = new XPoint();

	private boolean secondFingerIsDown = false;
	private boolean firstFingerIsDown = false;
	
	private Long lastTouchedAt = null;

	private LayoutParams layout = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 0, 0);
	private LayoutParams dimensions = new LayoutParams(0, 0, 0, 0);
	
	private FrameLayout container;

	private TouchLayerEventListener listener;

	private Scroller scroller;
	private VelocityTracker velocity = null;

	public TouchLayer(Context context) {

		super(context);
		setWillNotDraw(false);
				
		container = new FrameLayout(context);
		addView(container, dimensions);
		
		scroller = new Scroller(context);
		scroller.setFriction(0.99f);
	}

	public void setScaleLimits(double min, double max){
		minScale = min;
		maxScale = max;
	}
	
	public void setSize(int wide, int tall) {
		width = wide;
		height = tall;
		updateClip();
	}
	
	public int getBaseWidth(){
		return width;
	}
	
	public void setBaseWidth(int wide){
		width = wide;
		updateClip();
	}
	
	public int getBaseHeight(){
		return height;
	}
	
	public void setBaseHeight(int tall){
		height = tall;
		updateClip();
	}
	
	public void setScale(float factor){
		scale = constrainScale(factor);
		updateClip();
	}
	
	public float getScale(){
		return scale;
	}
	
	public void setLastScale(float factor){
		lastScale = constrainScale(factor);
	}
	
	private float constrainScale(float factor){
		factor = (float) Math.max(factor, minScale);
		factor = (float) Math.min(factor, maxScale);
		return factor;
	}
	
	private void updateClip(){
		dimensions.width = getScaledWidth();
		dimensions.height = getScaledHeight();
		container.setLayoutParams(dimensions);
		constrainScroll();		
	}
	
	private int getScaledWidth(){
		return (int) (width * scale);
	}
	
	private int getScaledHeight(){
		return (int) (height * scale);
	}

	@Override
	public void computeScroll() {
		if (scroller.computeScrollOffset()) {
			XPoint destination = new XPoint(scroller.getCurrX(), scroller.getCurrY());
			scrollToPoint(destination);
			postInvalidate();
			if(listener != null){
				listener.onScrollChanged(destination);
			}
		}
	}
	
	private void offsetPointToCenter(Point point){
		int offsetX = (int) (getWidth() / 2f);
		int offsetY = (int) (getHeight() / 2f);
		point.x -= offsetX;
		point.y -= offsetY;
	}
	
	public void scrollToPoint(Point point) {
		XPoint xpoint = new XPoint(point);
		xpoint.constrain(getTopLeft(), getBottomRight());
		scrollTo(xpoint.x, xpoint.y);
	}
	
	public void scrollToAndCenter(Point point){
		offsetPointToCenter(point);
		scrollToPoint(point);
	}
	
	public void slideToPoint(Point point){
		XPoint xpoint = new XPoint(point);
		xpoint.constrain(getTopLeft(), getBottomRight());
		scroller.startScroll(getScrollX(), getScrollY(), xpoint.x - getScrollX(), xpoint.y - getScrollY(), SLIDE_DURATION);
	}
	
	public void slideToAndCenter(Point destination){
		offsetPointToCenter(destination);
		slideToPoint(destination);
	}

	public void constrainScroll(){
		XPoint currentScroll = new XPoint(getScrollX(), getScrollY());
		XPoint limitScroll = new XPoint(currentScroll);
		limitScroll.constrain(getTopLeft(), getBottomRight());
		if(!currentScroll.equals(limitScroll)){
			scrollToPoint(currentScroll);	
		}
	}
	
	
	private XPoint getTopLeft() {
		return topLeft;
	}

	private XPoint getBottomRight() {
		bottomRight.setXY(getLimitX(), getLimitY());
		return bottomRight;
	}

	private int getLimitX() {
		return getScaledWidth() - getWidth();
	}

	private int getLimitY() {
		return getScaledHeight() - getHeight();
	}
	
	public View getView(){
		return container;
	}

	public void addLayer(View v){
		container.addView(v, layout);
	}
	
	public void removeLayer(View v){
		if(container.indexOfChild(v) > -1){
			container.removeView(v);
		}		
	}
	
	public void smoothScaleTo(double destination, int duration){
		Animator animator = new Animator();
		animator.setEasing(5);
		animator.setDuration(duration);
		animator.setValues(scale, destination);
		animator.setAnimationListener(animationListener);
		animator.start();
	}
	
	private Animator.AnimationListener animationListener = new Animator.AnimationListener() {
		
		@Override
		public void onAnimationStart() {
			
		}
		
		@Override
		public void onAnimationProgress(double progress, double newScale) {
			double deltaScale = newScale / lastScale;
			setScale((float) newScale);
			listener.onScaleChanged((float) newScale);
			XPoint newScrollPoint = new XPoint(doubleTapStartScroll);
			newScrollPoint.scale(deltaScale);
			newScrollPoint.subtract(doubleTapStartOffset);
			newScrollPoint.constrain(getTopLeft(), getBottomRight());
			scrollToPoint(newScrollPoint);
			lastScrollPosition.copy(newScrollPoint);
		}
		
		@Override
		public void onAnimationComplete() {
			if(listener != null){
				listener.onZoomComplete(scale);
			}			
		}
	};
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {

		final int action = event.getAction() & MotionEvent.ACTION_MASK;
		
		lastFirstFinger.copy(firstFinger);
		lastSecondFinger.copy(secondFinger);
		lastScrollPosition.copy(scrollPosition);
		
		firstFingerIsDown = false;
		secondFingerIsDown = false;
		
		for(int i = 0; i < event.getPointerCount(); i++){
			int id = event.getPointerId(i);
			int x = (int) event.getX(i);
			int y = (int) event.getY(i);
			switch(id){
			case 0 :
				firstFingerIsDown = true;
				firstFinger.setXY(x, y);
				actualPoint.setXY(x, y);
				break;
			case 1 :
				secondFingerIsDown = true;
				secondFinger.setXY(x, y);
				actualPoint.setXY(x, y);
				break;
			}
		}

		scrollPosition.setXY(getScrollX(), getScrollY());

		actualPoint.add(scrollPosition);
		
		if (velocity == null) {
			velocity = VelocityTracker.obtain();
		}
		velocity.addMovement(event);

		switch (action) {

		case MotionEvent.ACTION_DOWN :  // first finger goes down
			firstFingerIsDown = true;
			if (!scroller.isFinished()) {
				scroller.abortAnimation();
			}			
			long now = System.currentTimeMillis();
			if(lastTouchedAt != null){					
				int ellapsed = (int) (now - lastTouchedAt);
				if(ellapsed < DOUBLE_TAP_INTERVAL){
					lastScale = scale;
					doubleTapStartOffset.copy(firstFinger);
					doubleTapStartScroll.setXY(getScrollX(), getScrollY());
					doubleTapStartScroll.add(doubleTapStartOffset);
					float destScale = constrainScale(scale * 2);
					smoothScaleTo(destScale, ZOOM_ANIMATION_DURATION);
					if(listener != null){
						listener.onDoubleTap(actualPoint);
					}
				}
			}	
			if(listener != null){
				listener.onFingerDown(actualPoint);
			}
			lastTouchedAt = now;
			break;
			
		case MotionEvent.ACTION_POINTER_DOWN :  // second finger goes down
			lastScale = scale;
			pinchStartDistance = firstFinger.distance(secondFinger);
			pinchStartOffset = XPoint.average(firstFinger, secondFinger);
			pinchStartScroll.setXY(getScrollX(), getScrollY());
			pinchStartScroll.add(pinchStartOffset);
			break;

		case MotionEvent.ACTION_MOVE :  // either finger moves
			if (firstFingerIsDown && secondFingerIsDown) {
				if(listener != null){
					int pinchCurrentDistance = firstFinger.distance(secondFinger);
					float currentScale = (float) pinchCurrentDistance / (float) pinchStartDistance;
					currentScale = (float) Math.max(currentScale, 0.5);
					setScale(lastScale * currentScale);
					double deltaScale = getScale() / lastScale;  // should be same as currentScale but that creates ripples
					listener.onScaleChanged(scale);
					XPoint newScrollPoint = new XPoint(pinchStartScroll);
					newScrollPoint.scale(deltaScale);
					newScrollPoint.subtract(pinchStartOffset);
					newScrollPoint.constrain(getTopLeft(), getBottomRight());
					scrollToPoint(newScrollPoint);
				}	
			} else {
				XPoint delta = new XPoint();
				if(secondFingerIsDown && !firstFingerIsDown){  // should never happen, but let's be redundant
					delta.copy(lastSecondFinger);
					delta.subtract(secondFinger);
				} else {  // otherwise, should be first finger
					delta.copy(lastFirstFinger);
					delta.subtract(firstFinger);
				}				
				XPoint destination = new XPoint(scrollPosition);
				destination.add(delta);
				destination.constrain(getTopLeft(), getBottomRight());
				scrollToPoint(destination);
				invalidate();
				if(listener != null){
					listener.onDrag(destination);
				}
			}
			break;
			
		case MotionEvent.ACTION_UP :  // first finger goes up
			velocity.computeCurrentVelocity(1000);
			final int xv = (int) velocity.getXVelocity();
			final int yv = (int) velocity.getYVelocity();
			final int totalVelocity = Math.abs(xv) + Math.abs(yv);
			if (totalVelocity > MINIMUM_VELOCITY && !secondFingerIsDown) {  // secondFingerIsDown should always be false, but let's be redundant		
				scroller.fling(getScrollX(), getScrollY(), -xv, -yv, 0, getLimitX(), 0, getLimitY());
				Point fromScroll = scrollPosition.clone();
				scrollPosition.setXY(scroller.getFinalX(), scroller.getFinalY());
				invalidate();
				if(listener != null){
					listener.onFling(fromScroll, scrollPosition);
				}				
			}
			if (velocity != null) {
				velocity.recycle();
				velocity = null;
			}
			if(listener != null){
				listener.onTap(actualPoint);
			}
			break;
			
		case MotionEvent.ACTION_POINTER_UP :  // second finger goes up
			if(listener != null){
				listener.onZoomComplete(scale);
			}
			break;
			
		}

		return true;

	}
	
	public void setGestureListener(TouchLayerEventListener l) {
		listener = l;
	}


	
}

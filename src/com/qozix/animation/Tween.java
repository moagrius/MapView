package com.qozix.animation;

import java.util.ArrayList;

import android.os.Handler;
import android.os.Message;

import com.qozix.animation.easing.EasingEquation;
import com.qozix.animation.easing.Linear;

public class Tween {

	private double ellapsed;
	private double startTime;
	private double duration = 500;

	private ArrayList<TweenListener> listeners = new ArrayList<TweenListener>();
	private EasingEquation ease = Linear.EaseNone;

	public double getProgress() {
		return ellapsed / duration;
	}

	public double getEasedProgress() {
		return ease.compute( ellapsed, 0, 1, duration );
	}

	public void setAnimationEase( EasingEquation e ) {
		if ( e == null ) {
			e = Linear.EaseNone;
		}
		ease = e;
	}

	public void addTweenListener( TweenListener l ) {
		listeners.add( l );
	}

	public void removeTweenListener( TweenListener l ) {
		listeners.remove( l );
	}

	public double getDuration() {
		return duration;
	}

	public void setDuration( double time ) {
		duration = time;
	}

	public void start() {
		stop();
		ellapsed = 0;
		startTime = System.currentTimeMillis();
		for ( TweenListener l : listeners ) {
			l.onTweenStart();
		}
		handler.sendEmptyMessage( 0 );
	}

	public void stop() {
		if ( handler.hasMessages( 0 ) ) {
			handler.removeMessages( 0 );
		}
	}

	private Handler handler = new Handler() {
		@Override
		public void handleMessage( final Message message ) {
			ellapsed = System.currentTimeMillis() - startTime;
			ellapsed = Math.min( ellapsed, duration );
			double progress = getProgress();
			double eased = getEasedProgress();
			for ( TweenListener l : listeners ) {
				l.onTweenProgress( progress, eased );
			}
			if ( ellapsed >= duration ) {
				if ( hasMessages( 0 ) ) {
					removeMessages( 0 );
				}
				for ( TweenListener l : listeners ) {
					l.onTweenComplete();
				}
			} else {
				sendEmptyMessage( 0 );
			}

		}
	};

}

package com.qozix.map.animation;

import android.os.Handler;
import android.os.Message;

public class Animator {

	public static interface AnimationListener {
		public void onAnimationStart();
		public void onAnimationProgress(double progress, double value);
		public void onAnimationComplete();
	}
	
	private static final int RENDER = 1;
	
	private long duration = 500;
	private long startTime;
	private double startValue = 0;
	private double endValue = 0;
	private int easing = 0;
	
	private AnimationListener listener = null;
	
	public Animator(){
		
	}
	
	public void setEasing(int ease){
		easing = ease;
	}
	
	public int getEasing(){
		return easing;
	}
	
	public void setValues(double start, double end){
		startValue = start;
		endValue = end;
	}
	
	public void setAnimationListener(AnimationListener l){
		listener = l;
	}
	
	public long getDuration(){
		return duration;
	}
	
	public void setDuration(long time){
		duration = time;
	}
	
	public void start(){
		startTime = System.currentTimeMillis();
		handler.sendEmptyMessage(RENDER);
		if(listener != null){
			listener.onAnimationStart();
		}
	}
	
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(final Message message) {
			switch (message.what) {
			case RENDER:
				try {
					double ellapsed = System.currentTimeMillis() - startTime;
					double total = (double) duration;
					double progress = ellapsed / total;
					if(easing != 0){
						if(easing > 0){
							progress = 1f - Math.pow(1f - progress, easing);
						} else {
							progress = Math.pow(progress, -easing);
						}
					}
					progress = Math.min(1, progress);
					progress = Math.max(0, progress);
					double value = startValue + (endValue - startValue) * progress;
					if(listener != null){
						listener.onAnimationProgress(progress, value);
					}
					boolean isComplete = (progress == 1);
					if(isComplete){
						if (hasMessages(RENDER)) {
							removeMessages(RENDER);
						}						
						if(listener != null){
							listener.onAnimationComplete();
						}
					} else {
						sendEmptyMessage(RENDER);
					}
				} catch (Exception e) {
					
				}
			}			
				
		}
	};
	
}

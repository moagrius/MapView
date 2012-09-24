package com.qozix.map.layers;

import com.qozix.map.layouts.FixedLayout;

import android.content.Context;
import android.graphics.Canvas;

public class TransformationLayer extends FixedLayout {

    private float scale = 1;

    public TransformationLayer(Context context) {
        super(context);
        setWillNotDraw(false);
    }

    public void setScale(float factor){
        scale = factor;
        invalidate();
    }
    
    public float getScale(){
        return scale;
    }

    @Override
    public void onDraw(Canvas canvas){
        canvas.scale(scale, scale);
        super.onDraw(canvas);
    }

}
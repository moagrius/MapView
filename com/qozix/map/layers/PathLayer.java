package com.qozix.map.layers;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.view.View;

public class PathLayer extends View {
	
    private Paint paint = new Paint();
    private Path originalPath = new Path();
    private Path drawingPath = new Path();
    
    private int color = 0x00000000;
    private int thickness = 7;
    
    private Matrix matrix;
    
    private float scale = 1;

    public PathLayer(Context context) {
        
    	super(context);
    	
    	setWillNotDraw(false);
        
    	paint.setStyle(Paint.Style.STROKE);
    	paint.setAntiAlias(true);
    	paint.setColor(color);
        paint.setStrokeWidth(thickness);    
        paint.setShadowLayer(4, 2, 2, 0x66000000);
        paint.setPathEffect(new CornerPathEffect(5));
        
    }
    
    public void setColor(int c){
    	color = c;
    	paint.setColor(color);
    	invalidate();
    }
    
    public int getColor(){
    	return color;
    }
    
    public void setThickness(int w){
    	thickness = w;
    	paint.setStrokeWidth(thickness);   
    	invalidate();
    }
    
    public int getThickness(){
    	return thickness;
    }

    public float getScale(){
    	return scale;
    }
    
    public void setScale(float factor){
    	matrix = new Matrix();
    	drawingPath.set(originalPath);
		matrix.setScale(factor, factor);
		originalPath.transform(matrix, drawingPath);
		scale = factor;
		invalidate();
    }
    
    @Override
    public void onDraw(Canvas canvas) {   	
    	canvas.drawPath(drawingPath, paint);
    	super.onDraw(canvas);    	
    }
    
    public void drawPathFromPoints(ArrayList<Point> points){
    	
    	Point start = points.get(0);
        
    	originalPath.reset();
    	originalPath.moveTo(start.x, start.y);
        
        int l = points.size();
        for(int i = 1; i < l; i++){
        	Point p = points.get(i);
        	originalPath.lineTo(p.x, p.y);
        }
        
        drawingPath.set(originalPath);
        
    }

}
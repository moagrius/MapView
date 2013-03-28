package com.qozix.mapview.paths;

import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.view.View;


public class PathView extends View {

	private static final int DEFAULT_COLOR = 0xBB489FFF;

	private Paint paint = new Paint();
	private Path originalPath = new Path();
	private Path drawingPath = new Path();

	private double scale = 1;

	public PathView( Context context ) {
		super( context );
		setWillNotDraw( false );
		paint.setStyle( Paint.Style.STROKE );
		paint.setAntiAlias( true );
		paint.setColor( DEFAULT_COLOR );
		paint.setStrokeWidth( 7 );
		paint.setShadowLayer( 4, 2, 2, 0x66000000 );
		paint.setPathEffect( new CornerPathEffect( 5 ) );
	}

	public void setColor( int c ) {
		paint.setColor( c );
		invalidate();
	}

	public void setCornerRadii( float r ) {
		paint.setPathEffect( new CornerPathEffect( r ) );
		invalidate();
	}
	
	public void setShadowLayer(float radius, float dx, float dy, int color){
		paint.setShadowLayer( radius, dx, dy, color );
	}
	
	public void setStrokeWidth( float w ){
		paint.setStrokeWidth( w );
	}

	public double getScale() {
		return scale;
	}
	
	public Paint getPaint(){
		return paint;
	}

	public void setScale( double s ) {
		float factor = (float) s;
		Matrix matrix = new Matrix();
		drawingPath.set( originalPath );
		matrix.setScale( factor, factor );
		originalPath.transform( matrix, drawingPath );
		scale = s;
		invalidate();
	}

	public void drawPath( List<Point> points ) {
		Point start = points.get( 0 );
		originalPath.reset();
		originalPath.moveTo( (float) start.x, (float) start.y );
		int l = points.size();
		for ( int i = 1; i < l; i++ ) {
			Point p = points.get( i );
			originalPath.lineTo( (float) p.x, (float) p.y );
		}
		drawingPath.set( originalPath );
		invalidate();
	}

	@Override
	public void onDraw( Canvas canvas ) {
		canvas.drawPath( drawingPath, paint );
		super.onDraw( canvas );
	}

}
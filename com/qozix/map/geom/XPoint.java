package com.qozix.map.geom;

import android.graphics.Point;

public class XPoint extends Point {
	
	public XPoint(){
		
	}
	
	public XPoint(int ax, int ay){
		setXY(ax, ay);
	}
	
	public XPoint(XPoint p){
		copy(p);
	}
	
	public XPoint(Point p){
		x = p.x;
		y = p.y;
	}
	
	public XPoint clone(){
		return new XPoint(x, y);
	}
	
	public void setXY(int ax, int ay){
		x = ax;
		y = ay;
	}
	
	public void copy(Point p){
		x = p.x;
		y = p.y;
	}
	
	public void scale(float factor){
		x = (int) (x * factor);
		y = (int) (y * factor);
	}
	
	public void scale(double factor){
		scale((float) factor);
	}
	
	public XPoint getScaled(float factor){
		XPoint p = clone();
		p.scale(factor);
		return p;
	}
	
	public XPoint getScaled(double factor){
		return getScaled((float) factor);
	}
	
	public int distance(Point v){
		int dx = x - v.x;
		int dy = y - v.y;
		return (int) Math.sqrt(dx * dx + dy * dy);
	}
	
	public void subtract(Point v){
		x -= v.x;
		y -= v.y;
	}
	
	public void add(Point v){
		x += v.x;
		y += v.y;
	}
	
	public void min(Point v){
		x = Math.min(x, v.x);
		y = Math.min(y, v.y);
	}
	
	public void max(Point v){
		x = Math.max(x, v.x);
		y = Math.max(y, v.y);
	}
	
	public void constrain(Point mx, Point mn){
		max(mx);
		min(mn);
	}
	
	public static XPoint average(Point p1, Point p2){
		XPoint p = new XPoint();
		p.x = (int) ((double) (p1.x + p2.x) / 2);
		p.y = (int) ((double) (p1.y + p2.y) / 2);
		return p;
	}
	
	@Override
	public String toString(){
		return "x=" + x + ", y=" + y;
	}
	
}

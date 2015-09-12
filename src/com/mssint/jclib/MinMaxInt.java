package com.mssint.jclib;

/**
 * 
 * Will generalise with a co-ordinate pair and stuff later....
 * 
 * @author Alan
 *
 */
public class MinMaxInt {

	private int origin = Integer.MAX_VALUE;
	private int max = Integer.MIN_VALUE;
	@SuppressWarnings("unused")
	private int extent = Integer.MIN_VALUE;
	
	public MinMaxInt(){
	}
	
	public MinMaxInt(int origin, int extent){
		this.origin = origin;
		this.extent = extent;
	}
	
	public void setOrigin(int origin){
		if(origin < this.origin) this.origin = origin;
	}

	public int getOrigin(){
		return origin;
	}
	
	public void setMaximum(int maximum){
		if(maximum > max) max = maximum;
	}

	public int getMaximum(){
		return max;
	}
	
	public int getExtent(){
		return max - origin;
	}

	public void reset(){
		origin = Integer.MAX_VALUE;
		max = Integer.MIN_VALUE;
		extent = Integer.MIN_VALUE;
	}
	
}

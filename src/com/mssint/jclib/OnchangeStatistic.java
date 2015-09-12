package com.mssint.jclib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class OnchangeStatistic {
	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(OnchangeStatistic.class);
	public static enum STAT_FUNCTION {
		SUM,
		AVERAGE,
		COUNT,
		MAXIMUM,
		MINIMUM,
		SUM_SQUARES,
		MEAN_SQUARES,
		VARIANCE,
		STD_DEVIATION
	};
	private Var target;
	private Var watch;
	private Extract ex;
	private STAT_FUNCTION func;
	private int count;
	private double total;
	private double min, max;
	
	public OnchangeStatistic(STAT_FUNCTION function, Var target, Var watch) {
		this.target = target;
		this.watch = watch;
		this.func = function;
		ex = null;
		init();
	}
	public OnchangeStatistic(STAT_FUNCTION function, Var target, Extract ex, FrameVar watch) {
		this.target = target;
		this.watch = watch;
		this.func = function;
		this.ex = ex;
		init();
	}
	private void init() {
		count = 0;
		total = 0;
		min = Double.MAX_VALUE;
		max = 0;
	}
	
	public void update() throws IllegalStateException {
		if(target == null || watch == null) return;
		count++;
		Var wv;
		if(ex == null) wv = watch;
		else wv =  ex.getVar((FrameVar) watch);
		total = total + wv.getDouble();
		
		if(wv.lt(min)) min = wv.getDouble();
		if(wv.gt(max)) max = wv.getDouble();
		
		switch(func) {
		case SUM:
			target.set(target.add(wv));
			break;
		case COUNT:
			target.set(target.add(1));
			break;
		case AVERAGE:
			target.set(total / count);
			break;
		case MINIMUM:
			target.set(min);
			break;
		case MAXIMUM:
			target.set(max);
			break;
		case MEAN_SQUARES:
		case STD_DEVIATION:
		case SUM_SQUARES:
		case VARIANCE:
			throw new IllegalStateException("Function " + func + " not coded yet.");
		}
	}
}

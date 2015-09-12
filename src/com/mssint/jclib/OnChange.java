package com.mssint.jclib;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mssint.jclib.OnchangeStatistic.STAT_FUNCTION;


/**
 * OnChange is a class which will perform a "footer" method after a value has changed, and a "header"
 * method when a value has changed. The value which is monitored is a Var type variable,
 * passed in the constructor.
 * @author Peter Colman (pete@mssint.com)
 *
 */
public class OnChange {
	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(OnChange.class);
	
	private Var activeVar; 	///This is the Var being watched.
	private Var testVar;	///This is a Var to hold the test value
	private Var saveVar;	///This save the value during footer production
	@SuppressWarnings("unused")
	private String saveBuffer; //for extract saves.
	private String varName;
	
	private String footerName;
	private Method footer;
	private int footerLine;
	
	private String headerName;
	private Method header;
	private int headerLine;

	private Extract extract;
//	private boolean first = true;
	
	private ApplicationState app;
	
	private ArrayList<OnchangeStatistic> stats;
	
	private String changeVarName = null;
	
	/**
	 * Create's a new OnChange object when the watched data item is a Var, Group or FrameVar.
	 * setHeader and setFooter objects should be called to add frame's for execution.
	 * @param app A pointer to the ApplicationState object.
	 * @param ochVar The variable (Var, Group or FrameVar) being watched.
	 * @param ochVarName The variable name
	 * @throws IllegalStateException
	 */
	public OnChange(ApplicationState app, Var ochVar, String ochVarName) throws IllegalStateException {
		ochInit(app, ochVar, ochVarName);
	}
	private void ochInit(ApplicationState app, Var ochVar, String ochVarName) throws IllegalStateException {
		if(app == null)
			throw new IllegalArgumentException("ApplicationState app cannot be null");
		if(ochVar == null)
			throw new IllegalArgumentException("Var ochVar cannot be null");
		if(ochVarName == null)
			throw new IllegalArgumentException("Var ochVarName cannot be null");
		testVar = new Var(ochVar);
		testVar.set("");
		activeVar = ochVar;
		varName = ochVarName;
		this.app = app;
		footer = null;
		header = null;
		footerName = null;
		headerName = null;
		extract = null;
	}
	
	/**
	 * Create's a new OnChange object when the watched data item is an offset in an Extract object.
	 * setHeader and setFooter objects should be called to add frame's for execution.
	 * @param app A pointer to the ApplicationState object.
	 * @param ex The Extract object being watched.
	 * @param ochVar The FrameVar defining the region in the Extract buffer being watched.
	 * @param ochVarName The FrameVar name
	 * @throws IllegalStateException
	 */
	public OnChange(ApplicationState app, Extract ex, FrameVar ochVar, String ochVarName) throws IllegalStateException {
		if(ex == null)
			throw new IllegalArgumentException("Extract ex cannot be null");
		ochInit(app, (Var)ochVar,ochVarName);
		extract = ex;
		activeVar = ochVar;
		saveVar = extract.getVar(ochVar);
	}
	
	private Method getMethod(String name) {
		Class<?> params[] = {Report.class, Integer.TYPE};

		Class<? extends ApplicationState> myClass = app.getClass();
		
		try {
			Method method = myClass.getDeclaredMethod(name, params);
			return method;
		} catch(NoSuchMethodException e) {
			throw new IllegalArgumentException("FATAL: Method '"+name+"' not found in class '"+myClass.getName()+"'.");
		}
	}
	
	/**
	 * Set the name of the footer method to be executed at the moment of change. 
	 * The method is expected to have the signature methodname(Report rep, int line).
	 * @param methodName A String representation of the method name.
	 * @return The current OnChange object.
	 */
	public OnChange setFooter(String methodName) {
		return setFooter(methodName, 0);
	}
	
	/**
	 * Set the name of the footer method to be executed at the moment of change. 
	 * The method is expected to have the signature methodname(Report rep, int line).
	 * @param methodName A String representation of the method name.
	 * @param line The value of the line parameter to pass to the method.
	 * @return The current OnChange object.
	 */
	public OnChange setFooter(String methodName, int line) {
		if(methodName == null || methodName.length() == 0) {
			footer = null;
			footerName = null;
		} else {
			footerName = methodName;
			footer = getMethod(footerName);
		}
		footerLine = line;
		return this;
	}
	
	/**
	 * Set the name of the header method to be executed after the moment of change. 
	 * The method is expected to have the signature methodname(Report rep, int line).
	 * @param methodName A String representation of the method name.
	 * @return The current OnChange object.
	 */
	public OnChange setHeader(String methodName) {
		return setHeader(methodName, 0);
	}
	/**
	 * Set the name of the header method to be executed after the moment of change. 
	 * The method is expected to have the signature methodname(Report rep, int line).
	 * @param methodName A String representation of the method name.
	 * @param line The value of the line parameter to pass to the method.
	 * @return The current OnChange object.
	 */
	public OnChange setHeader(String methodName, int line) {
		if(methodName == null || methodName.length() == 0) {
			header = null;
			headerName = null;
		} else {
			headerName = methodName;
			header = getMethod(headerName);
		}
		headerLine = line;
		return this;
	}

	/**
	 * Determines if the value/variable being watched for this OnChange object has changed.
	 * @return true if the value has changed, otherwise false.
	 * @throws IllegalStateException 
	 */
	public boolean changed() throws IllegalStateException {
		return isChanged();
	}
	private boolean isChanged() throws IllegalStateException {
		if(extract == null) {
			changeVarName = varName;
			if(testVar.equals(activeVar)) return false;
			if(app != null && app.GLB != null)
		    	app.GLB.CHANGE.set(changeVarName);
		} else {
			changeVarName = varName;
			if(testVar.eq(extract.getVar((FrameVar)activeVar))) return false;
			if(app != null && app.GLB != null)
		    	app.GLB.CHANGE.set(changeVarName);
		}
//		update();
		return true;
	}
	public String getChangeVarName() {
		return changeVarName;
	}
	
	public int invokeFooter(Report rep) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, IllegalStateException {
		if(footer == null) return -1;
		footer.invoke(app, new Object [] {rep, footerLine});
		return 0;
	}
	public boolean isSavedValue = false;
	public void setUnchangedValue() throws IllegalStateException {
		if(isSavedValue || !isChanged()) return;
		if(extract == null) {
			if(saveVar == null) saveVar = new Var(activeVar);
			saveVar.set(activeVar);
			activeVar.set(testVar);
		} else {
			saveBuffer = extract.getBuffer();
			extract.setVar((FrameVar)activeVar, testVar);
		}
		isSavedValue = true;
	}
	public void setNormalValue() {
		if(!isSavedValue) return;
		if(extract == null) {
			activeVar.set(saveVar);
		} 
		isSavedValue = false;
	}
	public void initialiseLastBuffer() {
		if(extract != null)
			extract.initialiseLastBuffer();
	}
	public void setExtractToLastBuffer() {
		if(extract != null)
			extract.setLastBuffer();
	}
	public void setExtractToCurrentBuffer() {
		if(extract != null)
			extract.recoverLastBuffer();
	}
	
	public int invokeHeader(Report rep) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		Integer val;
		if(header == null)
			val = -1;
		else
			val = (Integer)header.invoke(app, new Object [] {rep, headerLine});
		if(extract == null) {
			testVar.set(activeVar);
		} else {
			testVar.set(extract.getVar((FrameVar)activeVar));
		}
		return val;
	}
	public ApplicationState getApp() {
		return app;
	}
	
	private void newStatFunction(STAT_FUNCTION func, Var target, Var watch) {
		if(stats == null) stats = new ArrayList<OnchangeStatistic>();
		stats.add(new OnchangeStatistic(func,target, watch));
	}
	private void newStatFunction(STAT_FUNCTION func, Var target, Extract ex, FrameVar watch) {
		if(stats == null) stats = new ArrayList<OnchangeStatistic>();
		stats.add(new OnchangeStatistic(func,target, ex, watch));
	}

	protected void update() throws IllegalStateException {
		if(stats == null) return;
		for(OnchangeStatistic s : stats)
			s.update();
	}
	
	public OnChange sum(Var target, Var watch) {
		newStatFunction(STAT_FUNCTION.SUM, target, watch);
		return this;
	}
	public OnChange sum(Var target, long watch) {
		newStatFunction(STAT_FUNCTION.SUM, target, new Var(watch));
		return this;
	}
	public OnChange sum(Var target, Extract ex, FrameVar watch) {
		newStatFunction(STAT_FUNCTION.SUM, target, ex, watch);
		return this;
	}
	public OnChange count(Var target, Var watch) {
		newStatFunction(STAT_FUNCTION.COUNT, target, watch);
		return this;
	}
	public OnChange count(Var target, Extract ex, FrameVar watch) {
		newStatFunction(STAT_FUNCTION.COUNT, target, ex, watch);
		return this;
	}
	public OnChange max(Var target, Var watch) {
		newStatFunction(STAT_FUNCTION.MAXIMUM, target, watch);
		return this;
	}
	public OnChange max(Var target, Extract ex, FrameVar watch) {
		newStatFunction(STAT_FUNCTION.MAXIMUM, target, ex, watch);
		return this;
	}
	public OnChange min(Var target, Var watch) {
		newStatFunction(STAT_FUNCTION.MINIMUM, target, watch);
		return this;
	}
	public OnChange min(Var target, Extract ex, FrameVar watch) {
		newStatFunction(STAT_FUNCTION.MINIMUM, target, ex, watch);
		return this;
	}
	public OnChange meanSquares(Var target, Var watch) {
		newStatFunction(STAT_FUNCTION.MEAN_SQUARES, target, watch);
		return this;
	}
	public OnChange meanSquares(Var target, Extract ex, FrameVar watch) {
		newStatFunction(STAT_FUNCTION.MEAN_SQUARES, target, ex, watch);
		return this;
	}
	public OnChange stdDeviation(Var target, Var watch) {
		newStatFunction(STAT_FUNCTION.STD_DEVIATION, target, watch);
		return this;
	}
	public OnChange stdDeviation(Var target, Extract ex, FrameVar watch) {
		newStatFunction(STAT_FUNCTION.STD_DEVIATION, target, ex, watch);
		return this;
	}
	public OnChange sumSquares(Var target, Var watch) {
		newStatFunction(STAT_FUNCTION.SUM_SQUARES, target, watch);
		return this;
	}
	public OnChange sumSquares(Var target, Extract ex, FrameVar watch) {
		newStatFunction(STAT_FUNCTION.SUM_SQUARES, target, ex, watch);
		return this;
	}
	public OnChange variance(Var target, Var watch) {
		newStatFunction(STAT_FUNCTION.VARIANCE, target, watch);
		return this;
	}
	public OnChange variance(Var target, Extract ex, FrameVar watch) {
		newStatFunction(STAT_FUNCTION.VARIANCE, target, ex, watch);
		return this;
	}
	public OnChange average(Var target, Var watch) {
		newStatFunction(STAT_FUNCTION.AVERAGE, target, watch);
		return this;
	}
	public OnChange average(Var target, Extract ex, FrameVar watch) {
		newStatFunction(STAT_FUNCTION.AVERAGE, target, ex, watch);
		return this;
	}

}

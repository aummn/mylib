package com.mssint.jclib;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class will handle frame processing for migrated LINC reports.
 * <p>last rebuilt %DATE; </p>
 * @version %BUILD;
 */
public class FrameState {
	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(FrameState.class);
	boolean isInitialised = false;
	
	
	//public Map<String, ParameterWrapper> parameters;
	private ArrayList<FrameVar> vars;
	private int maxLine;
	private TreeMap<String, Integer> maxLangLine;
	private int currentIndex;
	private ApplicationState app;
	
	public FrameState() {
		this.app = null;
	}
	
	public FrameState setApplicationState(ApplicationState app) {
		this.app = app;
		return this;
	}
	
	//Used to update the merged frame
	public FrameState init(ArrayList<FrameVar> varsList, int maxLineNumber) {
	    if(isInitialised) return this;
		this.vars = varsList;
		this.maxLine = maxLineNumber;
		isInitialised = true;
		return this;
	}

	public FrameState init() {
	    if(isInitialised) return this;
		return init(null);
	}
	
	/**
	 * Creates a single merged FrameState object, made up of the definitions
	 * of both this object's fields and mergeFrame's fields.
	 * 
	 * After initialisation in this manner, a reference to any value in this
	 * is the same as in mergeFrame. They become the same object.
	 * 
	 * @param mergeFrame - the FrameState object to merge with this one.
	 */
	public FrameState init(FrameState mergeFrame) {
		if(isInitialised) return this;
	    vars = new ArrayList<FrameVar>();
		maxLine = 0;
		maxLangLine = new TreeMap<String, Integer>();
		currentIndex = 0;
		int offset = 0;
		Field[] mfields;
		boolean doMerge;
		Field[] fields = getClass().getDeclaredFields();
		if(mergeFrame != null) {
			mfields = mergeFrame.getClass().getDeclaredFields();
			doMerge = true;
		} else {
			mfields = null;
			doMerge = false;
		}
		
		int i = 0; //Index for this.fields
		int j = 0; //index for mergeFrame.fields
		Object object = null;
		Object mobject = null;
		boolean doLocal;
	    // loop through all the declared fields
		while(true) {
			try {
				if(object == null && i < fields.length) 
					object = fields[i].get(this);
		    } catch (IllegalAccessException ex) { i++; continue; }
		    try {
				if(doMerge && mobject == null && mfields != null && j < mfields.length)
					mobject = mfields[j].get(mergeFrame);
			} catch (IllegalAccessException ex) { j++; continue; }
			if(object == null && mobject == null) break; // no more fields
			
			//Work out which one comes first.
			FrameVar lv, mv;
		    if(object != null && !(object instanceof FrameVar)) { 
				i++;
				object = null;
				continue;
			} 
		    if(mobject != null && !(mobject instanceof FrameVar)) {
				j++;
				mobject = null;
				continue;
			} 

			if(object != null && mobject == null) doLocal = true;
		    else if(object == null && mobject != null) doLocal = false;
			else { //Both are valid
				lv = (FrameVar)object;
			    mv = (FrameVar)mobject;
				if(lv.getRow() < mv.getRow()) doLocal = true;
			    else if(lv.getRow() > mv.getRow()) doLocal = false;
			    else if(lv.getCol() < mv.getCol()) doLocal = true;
			    else if(lv.getCol() > mv.getCol()) doLocal = false;
				else doLocal = true;
			}

			if(doLocal) {
				offset = setupFrameVar(object, fields[i++].getName(), offset);
				//Add this object to mergeFrame
				object = null;
		    } else {
		    	if(mfields != null)
		    		offset = setupFrameVar(mobject, mfields[j++].getName(), offset);
				//Add this mobject to this frame
				mobject = null;
			}
		}
		// Sort into row,col order
		//Collections.sort(vars, new FileComparator());
		//Update merged frame with correct values.
		if(mergeFrame != null) mergeFrame.init(vars, maxLine);
		isInitialised = true;
		return this;
	}
	
	private int setupFrameVar(Object object, String name, int offset) {
		if (object != null) {
	        if(object instanceof FrameVar) {
	            try {
	                //Util.debug("ADDING "+ name);
	                FrameVar v = (FrameVar)object;
	                v.setName(name);
	                int exlen = v.getExtractString().length();
	                v.setExlen(exlen);
	                v.setOffset(offset);
	                offset += exlen;
	                vars.add(v);
	                if(maxLine < v.getRow()) maxLine = v.getRow();
	                if(v.langIsSet()) {
	                	if(maxLangLine == null)
	                		maxLangLine = new TreeMap<String, Integer>();
	                	String l = v.getLang();
	                	Integer max = maxLangLine.get(l);
	                	if(max == null || max < v.getRow()) {
	                		max = v.getRow();
	                		maxLangLine.put(l, max);
	                	}
	                } else if(maxLangLine != null) { //We need to increment all languages to this line
	            		for(Map.Entry<String,Integer> entry : maxLangLine.entrySet()) {
	            			String l = entry.getKey();
	            			Integer max = entry.getValue();
	            			if(max < v.getRow()) {
	            				max = v.getRow();
		                		maxLangLine.put(l, max);
	            			}
	            		}

	                }
	                //parameters.put(fields[i].getName(),
	                //  new ParameterWrapper(object));
	            } catch(IllegalArgumentException ex)  {
	                // don't care
	            }
	        } else {
	            Util.error("Illegal type "+ name 
	                + ": Only FrameVar class supported.");
	        }
	    }
	    return offset;
	}
	
	public int getMaxLine() {
		if(!isInitialised) init(null);
		String lng = setupLang();
		if(lng == null) return maxLine;
		if(maxLangLine == null) return maxLine;
		
		Integer max = maxLangLine.get(lng);
		if(max == null) return -1;
		return max;
	}

	public int getMaxLine(String lng) {
		if(lng == null) return maxLine;
		if(maxLangLine == null) return maxLine;
		Integer max = maxLangLine.get(lng);
		if(max == null) return -1;
		return max;
	}
	
	public int getMaxLineDefault() {
		return maxLine;
	}
	public String getPrintString(int line) {
		return getPrintString(null, line);
	}
	
	public String getPrintString(String lang, int line) {
		if(!isInitialised) init(null);
	    if(vars.get(currentIndex).getRow() > line) currentIndex = 0;
	    while(vars.get(currentIndex).getRow() < line) currentIndex++;
		if(currentIndex > vars.size()) currentIndex = 0;
	    if(vars.get(currentIndex).getRow() != line) return "";
	    StringBuilder sb = new StringBuilder();
		while(vars.get(currentIndex).getRow() == line) {
			FrameVar v = vars.get(currentIndex++);
			if(v.langIsSet()) {
				if(lang == null)
					lang = setupLang();
				if(!v.getLang().equalsIgnoreCase(lang)) {
					if(currentIndex >= vars.size()) {
						currentIndex = 0;
						break;
					} else
						continue;
				}
			}
			while((sb.length() + 1) < v.getCol()) sb.append(" ");
			if((sb.length() + 1) > v.getCol()) sb.delete(v.getCol() - 1, sb.length());
			String nx = v.getPrintString();
			sb.append(nx);
			if(currentIndex >= vars.size()) {
				currentIndex = 0;
				break;
			}
		}
		return sb.toString();
	}
	
	public String getExtractString() {
		if(!isInitialised) init(null);
		StringBuilder sb = new StringBuilder();
		String lang = null;
		for(int i=0; i < vars.size(); i++) {
			FrameVar v = vars.get(i);
			if(v.langIsSet()) {
				if(lang == null)
					lang = setupLang();
				if(!v.getLang().equalsIgnoreCase(lang))
					continue;
			}
			sb.append(v.getExtractString());
		}
		return sb.toString();
	}
	
	private String setupLang() {
		if(app == null) return "en";
		if(app.GLB == null) return "en";
		return app.GLB.getLocale();
	}
	
	public String getLangDefault() {
		if(app == null) return "en";
		if(app.GLB == null) return "en";
		String l = app.GLB.getLocaleDefault();
		if(l == null) l = "en";
		return l;
	}
	

	public String toString() {
		return getExtractString();
	}
	
	public void clear() {
		if(!isInitialised) init(null);
	    for(int i=0; i < vars.size(); i++) {
	        vars.get(i).clear();
	    }
	}
	
	public void printLayout() {
		if(!isInitialised) init(null);
		for(int i=0; i < vars.size(); i++) {
			FrameVar v = vars.get(i);
			if(Config.DEBUG) System.out.println("POS LEN NAME");
			if(Config.DEBUG) System.out.println(
				Util.format("000",v.getOffset()) + " " +
				Util.format("000", v.getExlen()) + " " +
				v.getName()
			);
		}
	}
	
	/*
	private static class FileComparator implements Comparator<FrameVar> {
		public int compare(FrameVar v1, FrameVar v2) {
			if(v1 == v2) return 0;
			//FrameVar v1 = (FrameVar)o1;
			//FrameVar v2 = (FrameVar)o2;

			if(v1.getRow() < v2.getRow()) return -1;
			else if(v1.getRow() > v2.getRow()) return 1;
			else if(v1.getCol() < v2.getCol()) return -1;
			else return 1;
		}
	}
	*/
}

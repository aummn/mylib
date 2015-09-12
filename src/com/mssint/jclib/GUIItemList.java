package com.mssint.jclib;

import java.util.ArrayList;
import java.lang.String;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({ "serial", "hiding" })
public class GUIItemList<String> extends ArrayList<String> {
	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(GUIItemList.class);

	private ArrayList<String> indexValues = new ArrayList<String>();
	private boolean _isCleared = false;
	private boolean _clearFirst = false;
	private boolean _isStatic;
	
	public GUIItemList() {
		super();
	}

	public boolean isClearFirst() {
		return _clearFirst;
	}
	
	public void clearFirst() {
		_clearFirst = true;
	}
	
	public boolean containsIndexValue(String value) {
		String index;
		if((index = getIndex(value)) != null) {
			if(indexValues.contains(index))
				return true;
			else
				return false;
		} else
			return true;
	}
	
	@SuppressWarnings("unchecked")
	public boolean add(Object o) {
		String o2 = (String)o;
		setIndex(o2);
		return super.add(o2);
	}

	@SuppressWarnings("unchecked")
	public boolean remove(Object o){
		String o2 = (String)o;
		String index = getIndex(o2);
		int j = -1;
		for(int i = 0; i < this.size(); i++){
			if(get(i).toString().substring(1, index.toString().length() + 1).equals(index)){
				j = i;
				break;
			}
		}
		indexValues.remove(index);
		if(j >= 0){
			super.remove(j);
			return true;
		}
		else
			return false; 
	}
	
	public boolean isCleared(){
		return _isCleared;
	}
	
	@SuppressWarnings("unchecked")
	public String getIndex(String value){
		java.lang.String delimeter;
		java.lang.String theValue = (java.lang.String) value;
		if(theValue != null) {
			if(theValue.length() > 0) {
				delimeter = Util.getDelimiter(theValue);
//				System.out.println("theValue=\""+theValue+"\" delimiter=\""+delimeter+"\"");
				java.lang.String[] dataItems = theValue.split(delimeter);
				if(dataItems != null && dataItems.length >=3 && dataItems[1].equals(" "))
					return (String) dataItems[2];
				else if(dataItems != null && dataItems.length >=2) {
					return (String) dataItems[1];
				} else
					return null;
			}
		}
		return null;
	}

	private void setIndex(String value){
		String index;
		if((index = getIndex(value)) != null){
//			indexValues.remove(index);
			indexValues.add(index);
		}
	}

	@SuppressWarnings("unused")
	private void removeIndex(String value){
		String index;
		if((index = getIndex(value)) != null){
			indexValues.remove(index);
		}
	}

	public void isStatic(boolean isStatic) {
		_isStatic = isStatic;
	}

	public boolean isStatic() {
		return _isStatic;
	}

}

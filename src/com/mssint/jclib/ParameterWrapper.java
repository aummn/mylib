package com.mssint.jclib;

import java.util.ArrayList;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jdom.Element;


public class ParameterWrapper implements java.io.Serializable {
	private static final long serialVersionUID = -4214768530751110816L;
	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(ParameterWrapper.class);
	
	public static final int TYPE_UNKNOWN = 0;
	public static final int TYPE_ITEM = 1;
	public static final int TYPE_TEXTITEM = 2;
	public static final int TYPE_COPYFROM = 3;
	public static final int TYPE_SELECTITEM = 4;
	public static final int TYPE_LISTBOXITEM = 5;
	public static final int TYPE_BUTTONITEM = 6;
	public static final int TYPE_CHECKITEM = 7;
	public static final int TYPE_RADIOITEM = 8;
	public static final int TYPE_IMAGEITEM = 9;

	private static ArrayList<Integer> ListItemTypes = new ArrayList<Integer>(Arrays.asList(
												TYPE_SELECTITEM, TYPE_LISTBOXITEM));

	
	private Var var = null;
	private Var[] varArray = null;
	private int parameterType = TYPE_UNKNOWN;
	private int selectedIndex = -1;
	private int position = -1;

	private String onValue = null;
	private String offValue = null;
	public boolean staticallyInitialised;
	public boolean oneRelativeArray;
	
	public static final String defaultDelimiter = "/";
	
	public ParameterWrapper(Object source) throws IllegalArgumentException {
		this(source, false);
	}
	
	/**
	 * Parameter is either Var, Var[], GUIItem or GUIItemList
	 * @param source
	 */
	@SuppressWarnings("unchecked")
	public ParameterWrapper(Object source, boolean oneRelative) throws IllegalArgumentException {
		oneRelativeArray = oneRelative;
		// null is not allowed
		if (source == null) {
			throw new IllegalArgumentException();
		} else if (source instanceof Var) {
			var = (Var) source;
		} else if(source instanceof GUIItemList) {
			GUIItemList<String> objGUIItemList = ((GUIItemList<String>)source);
			try {
				varArray = new Var[objGUIItemList.size()];
				for(int i =0; i < objGUIItemList.size(); i++) {
					varArray[i] = new Var(objGUIItemList.get(i));
				}
			} catch(Exception ex) {
				throw new IllegalArgumentException("Illegal type for '"+source+"' expect ArrayList of Var." );
			}
		} else if (source.getClass().isArray()
				&& (((Object[]) source).length == 0 ||
						((Object[]) source)[0] instanceof Var)) {
			varArray = (Var[]) source;
		} else // nothing else is allowed
			throw new IllegalArgumentException("Illegal type for '"+source+"'");
	}

	public Object getParameter() {
		return isVar() ? (Object)this.var : (Object)this.varArray;
	}

	public void set(String value, int index) {
		if(varArray == null || varArray[index] == null)
			return;
		if(parameterType != ParameterWrapper.TYPE_CHECKITEM){
			varArray[index].set(value);
		} else {
			varArray[index].set(onValue);
		}
	}
	
	public void set(String value, int index, int relpos){
		if(parameterType != ParameterWrapper.TYPE_CHECKITEM){
			varArray[index].set(value);
		} else {
			Var v;
			if(index == -1) v = var;
			else v = varArray[index];
			if(relpos == -1) {
				v.set(onValue);
			} else {
				v.replaceAll(" ", offValue);
				v.setSubstr(onValue, relpos + 1, 1);
			}
		}
	}
	
	public void setLincValue(String value) {
		setLincValue(value, 0);
	}
	
	public void setLincValue(String value, int index) {
		Var v;
		if(isVar()) v = var;
		else if(oneRelativeArray) v = varArray[index + 1];
		else v = varArray[index];
		if(v.isNum()) v.set(value, true);
		else v.set(value);
	}

	public void set(String[] values) {
		if(isVar()) {
			if(parameterType != ParameterWrapper.TYPE_CHECKITEM) {
				if(var.isNum()) {
					var.set(extractNumeric(values[0]));
				} else {
					var.set(values[0]);
				}
			} else {
				var.set(onValue);
			}
		} else {
			int relAdj = oneRelativeArray ? 1 : 0;
			
			if(ListItemTypes.contains(parameterType)) {
				setSelectedIndex(values);
			}

			for (int i = 0; i < values.length; i++) {
				if(parameterType != ParameterWrapper.TYPE_CHECKITEM) {
					if(varArray[i + relAdj].isNum()) {
						varArray[i + relAdj].set(extractNumeric(values[i]));
					} else {
						varArray[i + relAdj].set(values[i]);
					}
				} else {
					varArray[i + relAdj].set(onValue);
				}
			}

		}
	}

	private String decimalSeparator = null;
	private String thousandsSeparator = null;
	/**
	 * Returns a consistently formatted string which can be passed to Var.set().
	 * Accepts inputs of the form: 12,123.12-, -12,123.12, +12.0, 12+, 12.-, etc.
	 * @param value
	 * @return
	 */
	public String extractNumeric(String value) {
		if(value == null || value.length() == 0 || value.trim().length() == 0)
			return " ";
		StringBuilder sb = new StringBuilder();
		if(decimalSeparator == null) {
			decimalSeparator = (Config.getProperty("numeric.decimal.separator") == null) ? 
				Config.DEFAULT_DECIMAL_SEPARATOR : 
				Config.getProperty("numeric.decimal.separator");
			if(decimalSeparator.equals(","))
				thousandsSeparator = ".";
			else thousandsSeparator = ",";
		}
		
		int x;
		for(x = 0; x < value.length();x++) {
			if(value.charAt(x) != ' ')
				break;
		}
		if(x > 0) sb.append(value.substring(x));
		else sb.append(value);
		while((x = sb.indexOf(thousandsSeparator)) != -1)
			sb.deleteCharAt(x);
		if((x = sb.indexOf("-")) != -1) {
			if(x != 0) {
				sb.deleteCharAt(x);
				sb.insert(0, '-');
			}
		}
		while((x = sb.indexOf("+")) != -1)
			sb.deleteCharAt(x);
		
		int dec = sb.indexOf(decimalSeparator);
		if(dec != -1) {
			if(!decimalSeparator.equals("."))
				sb.replace(dec, dec+1, ".");
			if(sb.length() == (dec+1))
				sb.append('0');
			if(dec == 0)
				sb.insert(0, '0');
		}
		return sb.toString();
	}

	public void clear()  {
		if (isVar()){
			if(parameterType == ParameterWrapper.TYPE_CHECKITEM){
				var.set(offValue);
			} else {
				var.set("");
			}
		} else {
			if(varArray != null){
				if(!staticallyInitialised){
					for (int i = 0; i < varArray.length; i++) {
						if(parameterType != ParameterWrapper.TYPE_CHECKITEM){
							varArray[i].set("");
						} else {
							varArray[i].set(offValue);
						}
					}
				}
			}
		}
	}

	public void clearIfInquiry()  {
		if (isVar()) {
			if(var.testAttr(Var.INQUIRY))
				var.set("");
		} else {
			if(varArray != null){
				if(!staticallyInitialised){
					for (int i = 0; i < varArray.length; i++) {
						if(varArray[i].testAttr(Var.INQUIRY))
							varArray[i].set(offValue);
					}
				}
			}
		}
	}

	public int getParameterType() {
		return parameterType;
	}

	public void setParameterType(int parameterType){
		this.parameterType = parameterType;
	}
	
	public void setParameterOptions(Element element) {
		String typeNormalized = element.getName().toUpperCase().trim();
		if (typeNormalized.equals("ITEM")){
			parameterType = TYPE_ITEM;
		}
		else if (typeNormalized.equals("TEXTITEM")){
			parameterType = TYPE_TEXTITEM;
		}
		else if (typeNormalized.equals("SELECTITEM")){
			parameterType = TYPE_SELECTITEM;
		}
		else if (typeNormalized.equals("LISTBOXITEM")){
			parameterType = TYPE_LISTBOXITEM;
		}
		else if (typeNormalized.equals("BUTTONITEM")){
			parameterType = TYPE_BUTTONITEM;
		}
		else if (typeNormalized.equals("CHECKITEM")){
			parameterType = TYPE_CHECKITEM;
			setOnOffValues(element);
		}
		else if (typeNormalized.equals("RADIOITEM")){
			parameterType = TYPE_RADIOITEM;
		}
		else if (typeNormalized.equals("IMAGEITEM")){
			parameterType = TYPE_IMAGEITEM;
		} else {
			System.err.println("Illegal parameter: " + element.getName());
		}
	}
	
	/**
	 * Only applicable to check items...
	 * @param element
	 */
	private void setOnOffValues(Element element) {
		onValue = element.getChildText("on");
		offValue = element.getChildText("off");
	}

	/**
	 * Checks need to indicate their on off value
	 * We are explicitly setting all the data to off here.
	 * 
	 * We do this every time we process the request so that we can set
	 * on only those that have been selected in the form. 
	 * 
	 */
	public void setChecksToOffValue(){
		if(parameterType == ParameterWrapper.TYPE_CHECKITEM){
			if(isVar()) 
				var.set(offValue);
			else  {
				for (int i = 0; i < varArray.length; i++)
					varArray[i].set(offValue);
			}
			
		}
	}
	
	public void set(ParameterWrapper wrapper)  {
		if(isVar()) 
			var.set(wrapper.getVar());
		else  {
			Var[] sourceArray = wrapper.getVarArray();
			// must be of type Var[] and source and target length 
			// must the same - otherwise return silently 
			if(sourceArray == null) {
				return; 
			} else {
				if((wrapper.oneRelativeArray && oneRelativeArray) ||
						(!wrapper.oneRelativeArray && !oneRelativeArray)) {
					for (int i = 0; i < sourceArray.length; i++)
						varArray[i].set(sourceArray[i]);
				} else if(wrapper.oneRelativeArray && !oneRelativeArray) {
					for(int i=0;i<varArray.length;i++)
						varArray[i].set(sourceArray[i+1]);
				} else if(!wrapper.oneRelativeArray && oneRelativeArray) {
					for(int i=1;i<varArray.length;i++)
						varArray[i].set(sourceArray[i-1]);
				}
				/*
				if(ListItemTypes.contains(wrapper.parameterType)) {
					// set value of each field
					if(varArray == null)
						varArray = (Var[]) sourceArray;
					else
						for (int i = 0; i < sourceArray.length &&  i < varArray.length; i++)
							varArray[i].set(sourceArray[i]);
				} else if(sourceArray.length == varArray.length) {
					for (int i = 0; i < sourceArray.length; i++)
						varArray[i].set(sourceArray[i]);
				} else {
					return;
				}
				*/
			}
		}
		this.selectedIndex = wrapper.getSelectedIndex();
		this.parameterType = wrapper.getParameterType();
		this.staticallyInitialised = wrapper.staticallyInitialised;
	}
	
	public ParameterWrapper copy()  {
		if(var != null)  {
			Var newVar = new Var(var);
			newVar.set(var);
			return new ParameterWrapper(newVar);
		}
		Var [] newArray;
		int addOne;
		if(oneRelativeArray) {
			newArray = new Var[varArray.length - 1];
			addOne = 1;
		} else {
			newArray = new Var[varArray.length];
			addOne = 0;
		}
		for (int i = 0; i < newArray.length; i++) {
			Var newVar = new Var(varArray[i + addOne]);
			newVar.set(varArray[i + addOne]);
			newArray[i] = newVar;
		}
		ParameterWrapper pw = new ParameterWrapper(newArray);
		pw.selectedIndex = getSelectedIndex();
		pw.parameterType = getParameterType();
		pw.staticallyInitialised = staticallyInitialised;
		return pw;
	}
	
	/**
	 * In the case where we are dealing with drop down lists
	 * we need to know the selected index of the drop down list.
	 * @return the selected index of the html grouplist
	 */
	public int getSelectedIndex() {
		return selectedIndex;
	}

	/**
	 * In the case where we are dealing with drop down lists
	 * we need to know the selected index by its value of the drop down list.
	 * @return the value of the selected item null if not set or data not initialised.
	 */
	public String getSelectedValue(){
		String value = null;
		if(ListItemTypes.contains(parameterType)){
			String delimiter = getDelimiter();
			String[] dataParts;
			try{
				dataParts = varArray[getSelectedIndex()].toString().split(delimiter);
				value = dataParts[dataParts.length - 2];
			}
			catch(Exception ignore){}
		}
		return value;			
	}
	
	
	/**
	 * Helper function determining if we are dealing with one of the GUI element types
	 * stored as a List.
	 * @return true if GUI element types as a List false for all others.
	 */
	public boolean isListType(){
		return !isVar() && ListItemTypes.contains(parameterType);
	}

	private int getBaseSelectedIndex(){
		return (ListItemTypes.contains(parameterType)) ? 1 : 0;
	}
	
	/**
	 * As the data from Linc is presented in a delimited for e.g. /NY/New York
	 * we will only know the NY part of the selected index.   
	 * @param value
	 * @throws UnsupportedOperationException id value is null
	 */
	public void setSelectedIndex(String [] values) throws UnsupportedOperationException {
		if(isListType()) {
			if(values[0] != null) {
				int theSelectedIndex = getBaseSelectedIndex();
				if(ListItemTypes.contains(parameterType)) {
					String delimiter = getDelimiter();
					String[] dataParts;
					int start = oneRelativeArray ? 1 : 0;
					for(int i = start; i < varArray.length; i++ ) {
						dataParts = varArray[i].toString().split(delimiter);
						if(dataParts.length >= 2 && 
								dataParts[dataParts.length - 2].equals(values[0])) {
							theSelectedIndex = i; 
							break;
						}
					}
				}
				selectedIndex = theSelectedIndex;
			}
		}
	}

	/**
	 * What is the delimiter in play in this Linc name value pair i.e. /NY/New York the
	 * Note that the first element is not used i.e. the way Linc works is that the 
	 * first element of the array is a place holder/part of the spec. We may re-purpose it for the selected index....
	 * @return the first char
	 * @throws UnsupportedOperationException if not a select type. 
	 */
	public String getDelimiter() throws UnsupportedOperationException {
		if(!ListItemTypes.contains(parameterType)){
			throw new UnsupportedOperationException("You cannot use this method on a non select type.");
		}
		if(varArray != null) {
			if(varArray.length >= 2) {
				if(varArray[1].length() == 0)
					return defaultDelimiter;
				return Character.toString(varArray[1].charAt(0));
			}
			return defaultDelimiter;
		}
		else {
			return defaultDelimiter;
		}
	}
	
	public String getString()  {
		return getNormalizedString(isVar() ? var : varArray[0]);
	}
	
	public String getString(int posNo)  {
		return getNormalizedString(isVar() ? var : varArray[posNo]);
	}

	public String getLincString()  {
		return getLincString(isVar() ? var : varArray[0]);
	}

	public String getLincString(int posNo)  {
		return getLincString(isVar() ? var : varArray[posNo]);
	}
	
	public String getLincString(Var v) {
		return v.getLincString();
	}
	
	public String toString()  {
		if(isVar())
			return getString();
		StringBuffer buff = new StringBuffer();
		buff.append("{");
		for (int i = 0; i < varArray.length; i++) {
			buff.append("[" + 
					getNormalizedString(varArray[i]) + "], ");
		}
		buff.append("}");
		return buff.toString();
	}
	
	public Var getVar()  {
		return this.var;
	}
	
	public Var[] getVarArray()  {
		return this.varArray;
	}
	
	private String getNormalizedString(Var source)  {
		// take care of nulls - might happen in array
		if(source == null)
			return "";
		if(parameterType == TYPE_CHECKITEM && offValue != null) {
			source.replaceAll(" ", offValue);
		}
		String sourceString = source.rtrim();
		// if value is not set, return ""
		if(sourceString.length() == 0)
			return "";
		// return the result
		return sourceString;
	}
	
	public boolean isVar()  {
		return var != null;
	}
	
	public void resetPosition() {
		position = -1;
	}
	
	public int nextPosition() {
		if(position == -1) {
			if(oneRelativeArray) position = 1;
			else position = 0;
			return position;
		} else {
			return ++position;
		}
	}

	public boolean isButton() {
		return (parameterType == TYPE_BUTTONITEM);
	}

	public boolean isCheckItem() {
		return (parameterType == TYPE_CHECKITEM);
	}
	
	public static ParameterWrapper createWrapper(String[] values) {
		if(values.length == 1)
			return new ParameterWrapper(new Var(values[0]));
		Var[] array = new Var[values.length];
		for (int i = 0; i < array.length; i++)
			array[i] = new Var(values[i]);
		return new ParameterWrapper(array);
	}

}

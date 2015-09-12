package com.mssint.jclib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FrameVar extends Var {
	private static final long serialVersionUID = -7629718683276972151L;
	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(FrameVar.class);
	private short row;
	private short col;
	private short offset;
	private short exlen;
	private boolean descending = false;
	private Object extObj;
	private Extract ex;
	private String fieldName;
	private String separator; //thousands separator
	private String lang;
	

	public String name;
	
	public FrameVar(int row, int col, Extract ex , Object o, String field) {
	    this.row = (short)(row > 0 ? row : 1);
	    this.col = (short)(col > 0 ? col : 1);
	    separator = null;
	    if(o instanceof CursorState) {
			setExtObj(o);
			this.ex = ex;
			fieldName = field;
		} else {
			setExtObj(null);
			Util.error("Unable to allocate frame variable with object "+ o);
		}
	    lang = null;
	}
	
	/**
	 * For setting up frame items which get there data from an extract.
	 * @param row Row position
	 * @param col Column position
	 * @param ex Instance of Extract class
	 * @param fv Instance of FrameState FrameVar
	 */
	public FrameVar(int row, int col, Extract ex, FrameVar fv) {
		this.row = (short)(row > 0 ? row : 1);
		this.col = (short)(col > 0 ? col : 1);
	    separator = null;
	    lang = null;
		this.ex = ex;
		setExtObj(fv);
	}

/* Commented out because I need int,int,String,String for occodes)
	public FrameVar(int row, int col, String lang, String text) {*/
		
		/*try {
		//Ford solved the problem like this.
//			String encoding = SystemEncoding.getEncoding(lang);
			String encoding = "iso-8859-1";
			text = new String(text.getBytes("iso-8859-1"),encoding);
		} 
		catch (Exception e) {
			System.out.println("Error in encoding");
		}
		*/

		/*this.row = (short)(row > 0 ? row : 1);
		this.col = (short)(col > 0 ? col : 1);
	    separator = null;
	    displayLength = (short) text.length();
	    value = new StringBuilder(text);
	    scale = 0;
	    this.attr = CHAR;
	    this.attr |= ISASSIGNED;
	    this.attr |= AUTO;
		setExtObj(null);
		this.lang = lang;
	}*/

	public FrameVar(int row, int col, Object o) {
		initialiseWithObject(row, col, null, o);
	}
	
	public FrameVar(int row, int col, String fmt, Object o) {
		initialiseWithObject(row, col, fmt, o);
	}
	
	public int getLen() {
		if(getExtObj() == null) return this.displayLength;
		else if(getExtObj() instanceof Var) return ((Var)getExtObj()).getLen();
		else if(getExtObj() instanceof FrameState) return ex.getVar((FrameVar)getExtObj()).getLen();
		else if(getExtObj() instanceof CursorState) return ex.getVar(getExtObj(), fieldName).getLen();
		else return this.displayLength;
	}
	
	public short getDec() {
		if(getExtObj() == null) return this.scale;
		else if(getExtObj() instanceof Var) return ((Var)getExtObj()).getDec();
		else if(getExtObj() instanceof FrameState) return ex.getVar((FrameVar)getExtObj()).getDec();
		else if(getExtObj() instanceof CursorState) return ex.getVar(getExtObj(), fieldName).getDec();
		else return this.scale;
	}
	
	
	private void initialiseWithObject(int row, int col, String fmt, Object o) {
		this.row = (short)(row > 0 ? row : 1);
		this.col = (short)(col > 0 ? col : 1);
	    separator = null;
	    int attr=0;
	    
		if(o instanceof String) {
			String s;
			if(fmt==null)
				s=(String)o;
			else {
				s = fmt;
				String s1 = (String)o;
				ocCodes=s1.split(",");
			}
		    displayLength = (short) s.length();
		    value = new StringBuilder(s);
		    scale = 0;
		    this.attr = CHAR;
		    this.attr |= ISASSIGNED;
		    this.attr |= AUTO;
			setExtObj(null);
		} else if(o instanceof Var) {
			if(fmt != null) attr = setAttributes(fmt);
		    else attr = 0;
			setExtObj(o);
			Var v = (Var)o;
			this.attr = v.attr;
		    v.attr |= attr;
			//The following code removed to fix a Ford bug. This needs to be altered at migrator
			//level so that attributes can be applied tp database vars correctly in future.
			//DO NOT SIMPLY UNCOMMENT IF ANOTHER LEADING ZERO BUG HAPPENS!
//			if(((Var)o).attr(NUMERIC)) 
//				v.attr |= ZEROFILL;
		} else {
			Util.error("Unable to allocate frame variable with object "+ o);
		}
	    this.attr |= attr;
	    if(fmt != null)
	    	setSpecialAttributes(fmt);
	}

	public FrameVar(int row, int col, String fmt, int len, int dec, String ocCodes) {
	    setExtObj(null);
	    this.row = (short)(row > 0 ? row : 1);
	    this.col = (short)(col > 0 ? col : 1);
	    separator = null;
	    this.attr = setAttributes(fmt);
	    lang = null;
	    setSpecialAttributes(fmt);
		this.attr |= AUTOCLEAR;
	    this.displayLength = (short)len;
	    this.scale = (short)dec;
	    if (testAttr(NUMERIC)) {
	        if (dec > 0)
	            exp = (byte) (len - dec - 1);
	        else
	            exp = (byte) len;
	        if (dec == 0) {
	            this.attr |= LONG;
	            setLongValue(0);
	        } else {
	            this.attr |= DOUBLE;
	            setDoubleValue(0);
	        }
//	        if(!attr(UNSIGNED)) this.len++;
	    } else if (testAttr(LONG | DOUBLE)) {
	        this.attr |= NUMERIC;
	    }
	    this.ocCodes=ocCodes.split(",");
		
	}
	
	private void setSpecialAttributes(String fmt) {
		String[] list = fmt.split("[,|;: ]");
		for (int i = 0; i < list.length; i++) {
			if(list[i].toLowerCase().startsWith("separator")) {
				String [] sa = list[i].split("=");
				if(sa.length > 1 && sa[1].length() > 0) {
					separator = sa[1];
				} else separator = ",";
			}
		}
	}
	
	public boolean isCharType() {
		if(getExtObj() == null) return testAttr(CHAR) ? true:false;
		else if(getExtObj() instanceof Var)
			return ((Var)getExtObj()).testAttr(CHAR) ? true:false;
		else return true;
	}
	
	public void setOffset(int offset) { this.offset = (short)offset; }
	public void setExlen(int exlen) { 
		this.exlen = (short)exlen; 
	}
	public int getOffset() { return offset; }
	public int getExlen() { return exlen; }
	
	public void setName(String name) { this.name = name; }
	/**
	 * Returns the name of this var if known.
	 */
	public String getName() {
		if(fieldName != null && fieldName.length() > 0)
			return fieldName;
		return name;
	}

	public FrameVar(int row, int col, String fmt, int len) {
	    this(row, col, fmt, len, 0, "");
	}
	public FrameVar(int row, int col, String fmt, int len, int dec) {
	    this(row, col, fmt, len, dec, "");
	}
	public FrameVar(int row, int col, String fmt, int len, String ocCodes) {
	    this(row, col, fmt, len, 0, ocCodes);
	}
	
	public Var getVar() {
		if(getExtObj() != null && getExtObj() instanceof Var) return (Var)getExtObj();
		return this;
	}
	
	public int getRow() { return row; }
	public int getCol() { return col; }
	
	public String getPrintString() {
		if(getExtObj() == null) return this.getFmtString(separator);
		else if(ex != null && getExtObj() instanceof FrameVar) {
			Var x = ex.getVar((FrameVar)getExtObj());
			return x.getFmtString(separator);
//			return ex.getVar((FrameVar)extObj).getFmtString(separator);
		}
		else if(getExtObj() instanceof Var) return ((Var)getExtObj()).getFmtString(separator);
		else return "";
	}
	
	public void print() {
		Object o = getExtObj();
		String n = getName();
		if(o == null) {
			
		} else if(o instanceof Var) ((Var)o).print(n);
		else if(o instanceof FrameVar) ((FrameVar)o).print();
		else if(o instanceof CursorState) ((CursorState)o).print(null);
		else System.out.println("Unknown field type.");
	}
	
	public String getExtractString() {
	    if(getExtObj() == null) return this.getLincString();
	    else if(getExtObj() instanceof FrameVar) return ex.getVar((FrameVar)getExtObj()).getLincString();
	    else if(getExtObj() instanceof Var) return ((Var)getExtObj()).getLincString();
		else if(getExtObj() instanceof CursorState) return ex.getVar(getExtObj(), fieldName).getLincString();
		else return "";
	}

	/**
	 * Get the length of the string resulting from getString();. This will include space for a sign,
	 * leading spaces and decimal points.
	 * @return The printed length of a variable.
	 */
	public int getPrintedLen() {
	    if(getExtObj() == null) return super.getPrintedLen(separator);
	    else if(getExtObj() instanceof FrameVar) return ex.getVar((FrameVar)getExtObj()).getPrintedLen(separator);
	    else if(getExtObj() instanceof Var) return ((Var)getExtObj()).getPrintedLen(separator);
		else if(getExtObj() instanceof CursorState) return ex.getVar(getExtObj(), fieldName).getPrintedLen(separator);
		else return 0;
	}

	public String getString() {
	    if(getExtObj() == null) return super.getString();
	    else if(getExtObj() instanceof FrameVar) return ex.getVar((FrameVar)getExtObj()).getString();
	    else if(getExtObj() instanceof Var) return ((Var)getExtObj()).getString();
	    else if(getExtObj() instanceof CursorState) return ex.getVar(getExtObj(), fieldName).getString();
	    else return "";
	}
	public int getInt() {
	    if(getExtObj() == null) return super.getInt();
	    else if(getExtObj() instanceof FrameVar) return ex.getVar((FrameVar)getExtObj()).getInt();
	    else if(getExtObj() instanceof Var) return ((Var)getExtObj()).getInt();
	    else if(getExtObj() instanceof CursorState) return ex.getVar(getExtObj(), fieldName).getInt();
	    else return 0;
	}
	public long getLong() {
	    if(getExtObj() == null) return super.getLong();
	    else if(getExtObj() instanceof FrameVar) return ex.getVar((FrameVar)getExtObj()).getLong();
	    else if(getExtObj() instanceof Var) return ((Var)getExtObj()).getLong();
	    else if(getExtObj() instanceof CursorState) return ex.getVar(getExtObj(), fieldName).getLong();
	    else return 0;
	}
	public double getDouble() {
	    if(getExtObj() == null) return super.getDouble();
	    else if(getExtObj() instanceof FrameVar) return ex.getVar((FrameVar)getExtObj()).getDouble();
	    else if(getExtObj() instanceof Var) return ((Var)getExtObj()).getDouble();
	    else if(getExtObj() instanceof CursorState) return ex.getVar(getExtObj(), fieldName).getDouble();
	    else return 0;
	}

	
	public void clear() {
	    if(getExtObj() == null && testAttr(AUTOCLEAR)) {
			if (testAttr(CHAR)) set("");
		    else set(0);
			attr &= ~(ISASSIGNED);
		}
		//if(extObj == null && attr(AUTOCLEAR)) clear();
	}
	
	public FrameVar descend() { descending = true; return this; }
	public FrameVar ascend() { descending = false; return this; }
	public FrameVar descending() { descending = true; return this; }
	public FrameVar ascending() { descending = false; return this; }

	/**
	 * Returns the status of the descending flag and reset's it to false. 
	 * @return true if the descending flag is set, otherwise false.
	 */
	public boolean isDescending() {
		boolean desc = this.descending;
		this.descending = false;
		return desc;
	}
	
	public Var newClone(String s) {
		Var v;
		if(exlen == 0) exlen = (short)displayLength;
		if(getExtObj() == null) v = new Var(this);
	    else if(getExtObj() instanceof FrameState) v = ex.getVar((FrameVar)getExtObj());
	    else if(getExtObj() instanceof CursorState) v = ((CursorState)getExtObj()).getFrameVar(fieldName);
	    else if(getExtObj() instanceof Var) v=new Var((Var)getExtObj());
		else return null;
		
		
		if(s.length() < (offset + exlen + 1)) {
			if(s.length() < (offset + 1)) v.extractSet("");
			else v.extractSet(s.substring(offset));
		} else v.extractSet(s.substring(offset, offset + exlen));
		v.attr |= Var.EXTRACT;
		return v;
	}

	public boolean langIsSet() {
		if(lang != null) return true;
		return false;
	}

	public String getLang() {
		return lang;
	}

	public void setExtObj(Object extObj) {
		this.extObj = extObj;
	}

	public Object getExtObj() {
		return extObj;
	}
}

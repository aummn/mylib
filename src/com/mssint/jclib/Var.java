package com.mssint.jclib;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Title: Var
 * </p>
 * <p>
 * Description: Java LINC libraries
 * </p>
 * <p>
 * The Var class is used as a way of storing all migrated variables (that aren't
 * groups - see Groups class). It "protects" the coder from types and
 * facilitates LINC style declarations. Each instance has int, long, double and
 * StringBuilder variables defined and the relevant one is used to store the
 * data. (StringBuilder is used in preference to String for efficiency). The
 * variable used is taken from the constructor method.
 * </p>
 * <p>
 * last rebuilt %DATE;
 * </p>
 * 
 * @author Peter Colman
 * @author Martin Hinson
 * @author Steve Rainbird
 * @version %BUILD;
 */ 


public class Var implements java.io.Serializable {
	@SuppressWarnings("unused")
//	private static final Logger log = LoggerFactory.getLogger(Var.class);
	private static final long serialVersionUID = 1L;
	
	protected static String charset = Config.getProperty("jclib.charset");

	/** Value types */
	static enum VTYPE {
		VT_UNDEFINED,
		VT_INT,
		VT_LONG,
		VT_DOUBLE,
		VT_STRING
	};
	
	protected int attr; // Attr type
	protected int displayLength;
	protected short scale;
	protected byte exp; // Exponent length
	protected VarArray vArray;
	// Data storage:
	protected Object value;
	protected Picture picture;
	protected String [] ocCodes;
	private String BRIGHT = "\u0010";
	private String LANDSCAPE = "\u0011";
	private String LINESPERINCH6 = "\u0012";
	private String LINESPERINCH8 = "\u0013";
	private String MEDIUMWEIGHT = "\u0014";
	private String TOPMARGIN2 = "\u0015";
	private String UNDERLINE  = "\u0016";
	private String UNDERLINEOFF = "\u0017";
	private String BRIGHTOFF = "\u0018";


	
	/** Used as part of the attribute word to indicate a character or string type */
	public static final int ORDER_DESC	= 00000000001;
	public static final int CHAR        = 00000000002;
	public static final int STRING      = 00000000002;
	public static final int NUMERIC     = 00000000004;
	public static final int NUMBER      = 00000000004;
	public static final int UNSIGNED    = 00000000010;
	public static final int UNUMERIC    = (NUMERIC | UNSIGNED);
	public static final int UNUMBER     = (NUMERIC | UNSIGNED);

	public static final int COMP		= 00000000020;
	public static final int LONG        = 00000000040;
	public static final int DOUBLE      = 00000000100;
	public static final int AUTOCLEAR	= 00000000200;
	public static final int PICTURE		= 00000000400;
	public static final int AUTO        = 00000001000;
	public static final int ZEROBLANK   = 00000002000;
	public static final int ZEROFILL    = 00000004000;
	public static final int PIP         = 00000010000;
	public static final int DATE        = 00000020000;
	public static final int INPUT	    = 00000040000;
	public static final int MODIFIED    = 00000100000;
	public static final int OVERFLOW    = 00000200000;
	public static final int DBUPDATE    = 00000400000;
	public static final int ISASSIGNED  = 00001000000;
	//public static final int GROUP_MAIN  = 00002000000;
    //public static final int GROUP_MEMBER= 00004000000;
    public static final int EXPORT      = 00002000000;
    public static final int UNUSED      = 00004000000; //This attribute is really unsued!
    public static final int LEADING_SIGN= 00010000000;
	public static final int GROUP       = 00020000000;
	public static final int AUTOVAR     = 00040000000;

	public static final int DOLLAR     	= 00100000000;
	public static final int STARFILL    = 00200000000;
	public static final int EDITPLUS    = 00400000000;
	public static final int NEGCR     	= 01000000000;
	public static final int NEGDR       = 02000000000;
    public static final int INQUIRY 	= 04000000000;
    public static final int NOINIT	 	= 010000000000;
    public static final int EXTRACT	 	= 020000000000;
	
	public static final int ASCENDING   = 00000000001;
	public static final int DESCENDING  = 00000000002;
	public static final int ASC   		= 00000000001;
	public static final int DESC  		= 00000000002;
	
	public boolean attr(int a) {
		return (attr & a) != 0;
	}

	/** Used for appending STRING Vars as necessary */
	public static final String spaces; // 14000 spaces
	static {
		StringBuffer buffer = new StringBuffer();
		for(int i = 0; i < 5000000; i++)
			buffer.append(" ");
		spaces = buffer.toString();
	}

	// 1234567890123456789012345678901234567890

	static final double powerFloat[] = { 1.0, 10.0, 100.0, 1000.0, 10000.0,
		100000.0, 1000000.0, 10000000.0, 100000000.0, 1000000000.0,
		10000000000.0, 100000000000.0, 1000000000000.0, 10000000000000.0,
		100000000000000.0, 1000000000000000.0, 10000000000000000.0,
		100000000000000000.0, 1000000000000000000.0,
		10000000000000000000.0, 100000000000000000000.0,
		1000000000000000000000.0, 10000000000000000000000.0 
	};

	static final long powerLong[] = { 1, 10, 100, 1000, 10000,
		100000, 1000000, 10000000, 100000000, 1000000000,
		10000000000L, 100000000000L, 1000000000000L, 10000000000000L,
		100000000000000L, 1000000000000000L, 10000000000000000L,
		100000000000000000L, 1000000000000000000L
	};

	static final int powerInt[] = { 1, 10, 100, 1000, 10000,
		100000, 1000000, 10000000, 100000000, 1000000000
	};
	

	/** Used by arithmetic routines */
	public static final int FPU_PREC = 15;

	/** Used by arithmetic routines */
	public static final int MAX_MULTIPLIER = 22;

	private int parameterType = ParameterWrapper.TYPE_UNKNOWN; 
	
	/*
	 * Constructors ============
	 */

	/**
	 * This builds a Var based on attribute information given in attr and
	 * according to the length and number of decimal places given in len and
	 * dec. This is the main constructor method called by all others.
	 * 
	 * @param attr
	 *            attribute(s) describing the type
	 * @param len
	 *            length of the item
	 * @param dec
	 *            number of decimal places (where appropriate)
	 */
	public Var(int attr, int len, int dec) {
		this.attr = attr;
		this.displayLength = len;
		this.scale = (short) dec;
		value = null;
		if((attr & (LONG|DOUBLE)) != 0)
			this.attr |= NUMERIC;
		if((attr & (CHAR|PICTURE)) != 0) {
			attr &= ~(LONG|DOUBLE|NUMERIC);
			StringBuilder sb = new StringBuilder();
			value = sb;
		}
		else if((attr & (NUMERIC)) != 0) {
			if (dec > 0)
				exp = (byte)(len - dec); // Don't forget to
															// allow for "." in
															// decs
			else
				exp = (byte) len;
			if (dec == 0) {
				this.attr &= ~(DOUBLE|PICTURE|CHAR);
				this.attr |= LONG;
				value = new Long(0);
			} else {
				this.attr &= ~(LONG|PICTURE|CHAR);
				this.attr |= DOUBLE;
				value = new Double(0);
			}
		}
	}

	public Var(int attr, int len, int dec, Picture pic) {
		this.attr = attr;
		this.displayLength = len;
		this.scale = (short) dec;
		this.picture = pic;
		value = null;
		if((attr & (LONG|DOUBLE)) != 0)
			this.attr |= NUMERIC;
		
		if((attr & (CHAR|PICTURE)) != 0) {
			attr &= ~(LONG|DOUBLE|NUMERIC);
			value = (StringBuilder)new StringBuilder();
		}
		else if((attr & (NUMERIC)) != 0) {
			if (dec > 0)
				exp = (byte) (len - dec);
			else
				exp = (byte) len;
			if (dec == 0) {
				this.attr |= LONG;
				value = new Long(0);
			} else {
				this.attr |= DOUBLE;
				value = new Double(0);
			}
		}
	}

	//Getters and setters for the value object
	protected long getLongValue() {
		return value == null ? 0 : ((Long)value).longValue();
	}
	protected void setLongValue(long v) { value = new Long(v); }
	protected double getDoubleValue() {
		return value == null ? 0.0 : ((Double)value).doubleValue();
	}
	protected void setDoubleValue(double v) { value = new Double(v); }
	/**
	 * This builds a Var based on attribute information given in attr and with a
	 * length as given in len. No decimals are given, so this could be used for
	 * character or integer definitions.
	 * 
	 * @param attr
	 *            the Var's attributes
	 * @param len
	 *            the Var's length
	 */
	public Var(int attr, int len) {
		this(attr, len, 0);
	}
	
	public Var() {
		attr = 0;
		value = null;
		scale = 0;
		displayLength = 0;
		vArray = null;
		exp = 0;
	}

	/**
	 * This creates a Var with the attributes of another.
	 * 
	 * @param v
	 *            the Var whose attributes are copied
	 */
	public Var(Var v) {
		this(v.getAttr(), v.getLen(), v.getDec());
		this.set(v);
	}
	
	private int getAttr() {
		return attr;
	}

	/**
	 * A method for creating a Var by giving the attributes in a more 'user
	 * friendly' fashion.
	 * 
	 * @param s
	 *            a list of attributes given as a separated list of words. The
	 *            possibilities are: "char string numeric unsigned integer
	 *            unumeric long double auto zeroblank zerofill pip date
	 *            database"
	 *            <p>
	 *            These can be in any case and should be separated by ",|;:" or
	 *            space
	 *            </p>
	 * @param len
	 *            the total length of the Var
	 * @param dec
	 *            the number of decimal places
	 */
	public Var(String s, int len, int dec) {
		this.attr = setAttributes(s);
		this.displayLength = len;
		this.scale = (short) dec;
		value = null;
		if((attr & (CHAR)) != 0) {
			attr &= ~(NUMERIC|LONG|DOUBLE);
			value = (StringBuilder) new StringBuilder();
		} else if((attr & (NUMERIC)) != 0) {
			if (dec > 0)
				exp = (byte)(len - dec - 1); // Don't forget to
															// allow for "." in
															// decs
			else
				exp = (byte)len;
			if (dec == 0) {
				this.attr |= LONG;
				setLongValue(0);
			} else {
				this.attr |= DOUBLE;
				setDoubleValue(0);
			}
		} else if ((attr & (LONG | DOUBLE)) != 0) {
			this.attr |= NUMERIC;
			if(dec == 0) setLongValue(0);
			else setDoubleValue(0);
		}
	}
	
	public Group redefine() {
		return new Group(this);
	}

	public int setAttributes(String s) {
		String[] list = s.split("[,|;: ]");
		int a = 0;
		for (int i = 0; i < list.length; i++) {
			if (list[i].equalsIgnoreCase("char"))
				a |= CHAR;
			else if (list[i].equalsIgnoreCase("string"))
				a |= CHAR;
			else if (list[i].equalsIgnoreCase("numeric"))
				a |= NUMERIC;
			else if (list[i].equalsIgnoreCase("number"))
				a |= NUMERIC;
			else if (list[i].equalsIgnoreCase("unsigned"))
				a |= UNSIGNED;
			else if (list[i].equalsIgnoreCase("integer"))
				a |= LONG;
			else if (list[i].equalsIgnoreCase("unumeric"))
				a |= UNUMERIC;
			else if (list[i].equalsIgnoreCase("unumber"))
				a |= UNUMERIC;
			else if (list[i].equalsIgnoreCase("long"))
				a |= LONG;
			else if (list[i].equalsIgnoreCase("double"))
				a |= DOUBLE;
			else if (list[i].equalsIgnoreCase("auto"))
				a |= AUTO;
			else if (list[i].equalsIgnoreCase("zeroblank"))
				a |= ZEROBLANK;
			else if (list[i].equalsIgnoreCase("dollar"))
				a |= DOLLAR;
			else if (list[i].equalsIgnoreCase("starfill"))
				a |= STARFILL;
			else if (list[i].equalsIgnoreCase("editplus"))
				a |= EDITPLUS;
			else if (list[i].equalsIgnoreCase("leading_sign"))
				a |= LEADING_SIGN;
			else if (list[i].equalsIgnoreCase("negcr"))
				a |= NEGCR;
			else if (list[i].equalsIgnoreCase("negdr"))
				a |= NEGDR;
			else if (list[i].equalsIgnoreCase("zerofill"))
				a |= ZEROFILL;
			else if (list[i].equalsIgnoreCase("pip"))
				a |= PIP;
			else if (list[i].equalsIgnoreCase("date"))
				a |= DATE;
		}
		return a;
	}

	/**
	 * Creates a Var using a 'user friendly' syntax for the attribute.
	 * 
	 * @param s
	 *            a list of attributes given as a separated list of words. The
	 *            possibilities are: "char string numeric unsigned integer
	 *            unumeric long double auto zeroblank zerofill pip date
	 *            database"
	 *            <p>
	 *            These can be in any case and should be separated by ",|;:" or
	 *            space
	 *            </p>
	 * @param len
	 *            the total length of the Var.
	 */
	public Var(String s, int len) {
		this(s, len, 0);
	}

	/*
	 * Constructors for basic types ============================
	 */
	/**
	 * Creates a Var from an long type.
	 * 
	 * @param l
	 *            the long to convert to a Var.
	 */
	public Var(long l) {
		setLongValue(l);
		String s = Long.toString(Math.abs(l));
		displayLength = s.length();
		scale = 0;
		exp = (byte)displayLength;
		if(getLongValue() < 0) attr = NUMERIC;
		else attr = UNUMERIC;
		attr |= LONG;
		attr |= ISASSIGNED;
		attr |= AUTO;
	}

	/**
	 * Creates a Var from an double type.
	 * 
	 * @param d
	 *            the double to convert to a Var.
	 */
	public Var(double d) {

	    //BigDecimal bd = new BigDecimal(d)).setScale(dec, BigDecimal.ROUND_HALF_DOWN);
	    //BigDecimal bd = new BigDecimal(d);

		String s = Double.toString(Math.abs(d));
	    setDoubleValue(d);
		int e = s.indexOf('E');
		if(e != -1) {
			int x = Integer.parseInt(s.substring(e+1));
			if(x > 0) {
				displayLength = (e - 1);
				scale = (short)(e - x - 2);
			}
		} else {
			int x = s.indexOf('.');
			if (x != -1) {
				displayLength =  s.length() - 1;
				scale = (short) (s.length() - x - 1);
				exp = (byte)(displayLength - scale - 1); // Don't forget to allow
			} else {
				displayLength =  s.length();
				scale = 0;
				exp = (byte)displayLength;
			}
		}
		if(getDoubleValue() < 0) attr = NUMERIC;
		else attr = UNUMERIC;
		attr |= DOUBLE;
		attr |= ISASSIGNED;
		attr |= AUTO;
	}

	/**
	 * Creates a Var from a String type.
	 * 
	 * @param s
	 *            the String to make into a Var.
	 */
	public Var(String s) {
		if(s == null) s = "";
		displayLength =  s.length();
		value = new StringBuilder(s);
		scale = 0;
		attr = CHAR;
		attr |= ISASSIGNED;
		attr |= AUTO;
	}
	
	//This method is only used during Group creation. 
/*	protected void dupVarSegment(int offt, int l, int times) {
		if(value == null) value = new StringBuilder();
		StringBuilder sv = (StringBuilder)value;
		if(sv.length() < offt + l) {
			sv.append(new Var(CHAR, (offt + l) - sv.length()).getString());
			this.len = sv.length();
		}
		if(sv.length() < this.len) sv.append(new Var(CHAR, this.len - sv.length()).getString());
		else if(sv.length() > this.len) sv.setLength(this.len);
		//Text of segment to duplicate:
		String s = sv.substring(offt, offt + l);
		
		//Insert segment into string at required location
		for(int i=1; i<times; i++) {
			//Only add new length if the Var is too short.
			//if(this.len <  newlen) {
				sv.insert(offt + (l * i), s);
				this.len += l;
			//}
			//log.debug("dupVarSegment("+offt+","+ l +","+times+ ") length now is "+sv.length()+ "("+len+")");
		}
	}*/
	
	/**
	 * Get the precision of the double value dv
	 * @param dv
	 * @return
	 */
	public int getPrecision(double dv) {
		if(dv == 0) return 1;
		return (int)Math.log10(Math.abs(dv)) + 1;
	}

	/**
	 * Get the precision of the long value lv
	 * @param dv
	 * @return
	 */
	public int getPrecision(long lv) {
		if(lv == 0) return 1;
		return (int)Math.log10(Math.abs(lv)) + 1;
	}
	
	
	/**
	 * returns the number of decimal places for this Var or Group.
	 * 
	 * @return the number of decimal places as a short.
	 */
	public short getDec() {
		return scale;
	}

	/**
	 * Gives the length of this Var in characters including the decimal point
	 * where appropriate. Trailing spaces are ignored in the length caclulation.
	 * 
	 * @return the length
	 */
	public int length() {
		return getTrim().length();
	}
	
	/**
	 * Return the size of this variable.
	 * @return
	 */
	public int size() {
		return displayLength;
	}
	
	public int maxLength() {
		return displayLength;
	}

	/**
	 * Checks if the value passed, representing an attribute, is set.
	 * <p>
	 * The list of attributes are:<br>
	 * CHAR<br>
	 * STRING<br>
	 * NUMERIC<br>
	 * UNSIGNED<br>
	 * UNUMERIC<br>
	 * INTEGER<br>
	 * LONG <br>
	 * DOUBLE<br>
	 * AUTO <br>
	 * ZEROBLANK<br>
	 * ZEROFILL<br>
	 * PIP <br>
	 * DATE <br>
	 * DATABASE<br>
	 * MODIFIED<br>
	 * OVERFLOW<br>
	 * DBUPDATE<br>
	 * ISASSIGNED
	 * </p>
	 * 
	 * @return true if the attribute is set
	 */
	private final boolean _xtestAttr(int a) {
		return ((attr & a) != 0);
	}
	
	public final boolean testAttr(int a) {
		return ((attr & a) != 0);
	}

	/**
	 * indicates whether the MODIFIED part of the attribute word is set.
	 * 
	 * @return true if set
	 */
	public final boolean modified() {
		return ((attr & MODIFIED) != 0);
	}

	/**
	 * indicates whether the OVERFLOW part of the attribute word is set.
	 * 
	 * @return true if set
	 */
	public boolean overflow() {
		return ((attr & OVERFLOW) != 0);
	}


	/**
	 * clears the attributes given in the parameter.
	 * 
	 * @param a
	 *            a boolean representation of the attributes being cleared
	 */
	public void clearAttr(int a) {
		attr &= ~(a);
	}

	/**
	 * sets the DBUPDATE attribute.
	 */
	public void AutoClearOn() {
		attr |= DBUPDATE;
	}

	/**
	 * resets the DBUPDATE attribute.
	 */
	public void AutoClearOff() {
		attr &= ~(DBUPDATE);
	}
	
	/**
	 * Initialises AUTOVAR item to maximum 'maxlen' characters.
	 */
	public void initialise(int maxlen, int ival, char cval) throws IllegalStateException {
		if((attr & (AUTOVAR)) == 0) {
			throw new IllegalStateException("Cannot call initialise() on non-auto item."); 
		}
		exp = (byte) ival;
		StringBuilder sv;
		if(value == null) {
			value = new StringBuilder();
			sv = (StringBuilder)value;
		} else {
			sv = (StringBuilder)value;
			sv.delete(0, sv.length());
		}
		for(int i=0; i<maxlen; i++) sv.append(cval);
		displayLength =  maxlen;
	}
	
	/**
	 * Initialise numeric items to 0 and string items to "";
	 */
	public void initialise() {
		if(vArray == null) {
			if((attr & (LONG|DOUBLE)) != 0) set(0);
			else set("");
		} else {
			for (int i = 0; i < vArray.size() ;i++) {
				if((attr & (LONG|DOUBLE)) != 0) vArray.index(i).set(0);
				else vArray.index(i).set("");
				
			}
		}
	}

//================================================== Setter methods
	/**
	 * Assigns a String to a Var. If the Var is declared as being a numeric
	 * type, the string is assumed to hold the string representation of numbers
	 * which are first converted.
	 * 
	 * @param val
	 *            the string to assign
	 */
	//Overridden in Group
	public Var set(String val) {
		return set(val, false);
	}
	
	/**
	 * Assign String to Var. If Var is a Double, and val does NOT contain a period, then if
	 * implicitDecimal is true, assign one in the right place.
	 * @param val
	 * @param implicitDecimal
	 * @return
	 */
	public Var set(String val, boolean implicitDecimal) {
		attr |= ISASSIGNED;
		if(val == null) val = "";
		if((attr & (AUTO)) != 0) displayLength =  val.length();
		if(this.overflow()) this.attr &= ~(OVERFLOW);
		
		if((attr & (CHAR|PICTURE)) != 0) {
			StringBuilder sv;
			if(value == null) {
				value = new StringBuilder("");
				sv = (StringBuilder)value;
			} else {
				if(!(value instanceof StringBuilder)) {
					System.out.println("value was not StringBuilder ZZZ");
					String s;
					if(value instanceof Long) 
						s = Long.toString((Long)value);
					else if(value instanceof Double)
						s = Double.toString((Double)value);
					else s = "";
					sv = new StringBuilder();
					sv.append(s);
					value = (StringBuilder)sv;
				}
				sv = (StringBuilder)value;
				sv.delete(0, sv.length());
			}
			if(val.length() > displayLength) {
				if((attr & (PICTURE)) != 0) sv.append(picture.format(val, (attr & (ZEROBLANK)) != 0));
				else if(displayLength == 0) sv.append(val);
				else sv.append(val.substring(0, displayLength));
			} else {
				if((attr & (PICTURE)) != 0) sv.append(picture.format(val, (attr & (ZEROBLANK)) != 0));
				else {
					sv.append(val);
//					System.out.println("len="+len+" val.length()="+val.length());
					if(displayLength > val.length()) 
						sv.append(spaces.substring(0, displayLength - val.length()));
				}
			}
			if ((attr & (DBUPDATE)) != 0)
				attr &= ~(MODIFIED);
		} else if((attr & (LONG)) != 0) {
			long lv = lincStrToLong(val, displayLength, CHAR); //Will set OVERFLOW flag
			//If the string contains decimal points, then remember the decimal places in case of this
			//being the source of a complex move.

			if ((attr & (UNSIGNED)) != 0)
				lv = Math.abs(lv);
			if ((attr & (DBUPDATE)) != 0)
				attr &= ~(MODIFIED);
			setLongValue(lv);

			int i = val.indexOf('.');
			if(i != -1) {
				picture = new Picture(scale, val.substring(i+1));
			}
		} else if((attr & (DOUBLE)) != 0)
			set(lincStrToDouble(val, displayLength, scale, implicitDecimal));
		return this;
	}
	
	public void extractSet(String s) {
		set(s, true);
	}
	
	public Var increment(long amount) {
		if((attr & (LONG)) != 0) {
			long i = getLongValue();
			i += amount;
			setLongValue(i);
		} else if((attr & (DOUBLE)) != 0) {
			double d = getDoubleValue();
			d += amount;
			setDoubleValue(d);
		} else {
			set(getInt() + amount);
		}
		return this;
	}
	//overridden in Group
	public Var set(Integer ival) {	return set(ival.intValue()); }
	public Var set(Long lval) { return set(lval.longValue()); }
	public Var set(Double dval) { return set(dval.doubleValue());}

	
	public Var compute(int val) { //overridden in Group
		return set((long)val);
	}

	public Var set(int val) { //overridden in Group
		return set((long)val);
	}
	
	public Var set(char val) {
		return set(Character.toString(val));
	}
	
	public char charAt(int pos) {
		if(value instanceof StringBuilder) return ((StringBuilder)value).charAt(pos);
		return getString().charAt(pos);
	}
	
	public Var compute(long val) { //overridden in Group
		return set(val);
	}
	
	public Var set(long val) { //overridden in Group
		//sv = null;
		attr |= ISASSIGNED;
		attr &= ~(OVERFLOW);
		if ((attr & (CHAR|PICTURE)) != 0) {
			Long i = new Long(val);
			set(i.toString());
		} else if ((attr & (LONG)) != 0) {
			if(exp == 0) exp = (byte) displayLength;
			long lv = val % (long)Math.pow(10, exp);
			if (lv != val)
				attr |= OVERFLOW;
			if ((attr & (UNSIGNED)) != 0)
				lv = Math.abs(lv);
			if ((attr & (DBUPDATE)) != 0)
				attr &= ~(MODIFIED);
			setLongValue(lv);
		} else if ((attr & (DOUBLE)) != 0) {
			val = (long) (val % Math.pow(10, displayLength-scale));
			set((double) val);
		}
		return this;
	}

	public Var compute(double val) throws ArithmeticException { //overridden in Group
		return set(val);
	}
	
	public Var set(double val) throws ArithmeticException { //overridden in Group
		//sv = null;
		if(Double.isInfinite(val)) 
			throw new ArithmeticException("Divide by ZERO"); //10.0/0
			
        if(Double.isNaN(val)) val=0; // 0.0/0
        
		attr |= ISASSIGNED;
		if ((attr & (CHAR|PICTURE)) != 0) {
			Double d = new Double(val);
			set(d.toString());
		} else if((attr & (LONG)) != 0) {
			set((long) val);
		} else if ((attr & (DOUBLE)) != 0) {
			double dv = f_chop(val, displayLength, scale);
			
			//if (dv != val)
			if((int)val > (int)dv)
				attr |= OVERFLOW;
			if ((attr & (UNSIGNED)) != 0)
				dv = Math.abs(dv);
			if ((attr & (DBUPDATE)) != 0)
				attr &= ~(MODIFIED);
			setDoubleValue(dv);
		}
		return this;
	}

	public Var compute(Var v) { //overridden in Group
		return set(v);
	}
	
	/**
	 * Assigns a Var or Group with a Var.
	 * 
	 * @param v
	 *            the int to assign
	 */
	public Var set(Var v) { //overridden in Group
		//sv = null;
		attr |= ISASSIGNED;
		if (this.overflow()) this.attr &= ~(OVERFLOW);
		if(v == null) return this;
		if(v.overflow()) this.attr |= OVERFLOW;
		
		if((v.attr & (AUTOVAR)) != 0) {
			try {
				set(v.getString(this.attr,displayLength, scale));
			} catch(Exception e) { ; }
		} else {
			//Special case where we are numeric, sending field is group and our length is longer
			if((v.attr & (GROUP)) != 0 && ((attr & (LONG)) != 0 || (attr & (DOUBLE)) != 0) && v.displayLength < displayLength) {
				Var v2 = new Var(zeros.substring(0, displayLength - v.displayLength) + v.getString());
				if((attr & (LONG)) != 0) set(v2.getLong(displayLength));
				else if((attr & (DOUBLE)) != 0) set(v2.getDouble(displayLength, scale));
			/*}  this doesnt work but needs to be looked at else if(v.testAttr(CHAR) && ((attr & (NUMBER)) != 0) && ((attr & (PICTURE)) != 0)) {
				//If we are assigning a pic x field to a field with a picture the picture is ignored 
				attr &= ~(PICTURE);
				if((attr & (LONG)) != 0) set(v.getLong(displayLength));
				else if((attr & (DOUBLE)) != 0) set(v.getDouble(displayLength, scale));
				else if((attr & (GROUP)) != 0) set(v.getLincString(true));
				else set(v.getLincAbsString(true));
				attr |= PICTURE;*/
			} else {
				if((attr & (LONG)) != 0) set(v.getLong(displayLength));
				else if((attr & (DOUBLE)) != 0) set(v.getDouble(displayLength, scale));
				else if((attr & (GROUP)) != 0) set(v.getLincString(true));
				else if((attr & EXTRACT) != 0)  set(v.getLincString(true));
				else set(v.getString(true));
			}
		}
		return this;
	}
	
	/**
	 * Assigns a hex String to a Var. 
	 * 
	 * @param val
	 *            the string to assign
	 */
	public Var setHex(String v) {
    	StringBuilder sb = new StringBuilder();
    	for(int i=0;i<v.length();i += 2) {
    		char hexValue;
    		if((i + 2) > v.length())
    			hexValue=(char)Integer.parseInt(v.substring(i), 16);
    		else
    			hexValue=(char)Integer.parseInt(v.substring(i, i+2), 16);
    		sb.append(hexValue);
    	}
    	set(sb.toString());
    	return this;
   	}
    /**
	 * Fills a Var with the same hex characters repeated. 
	 * 
	 * @param val
	 *            the string to assign
	 */
    public Var fillHex(String v) {
    	StringBuilder sb = new StringBuilder();
    	for(int i=0;i<v.length();i += 2) {
    		char hexValue = (char)Integer.parseInt(v.substring(i, i+2), 16);
    		sb.append(hexValue);
    	}
    	fill(sb.toString());
    	return this;
   	}


	/**
	 * Fills a Var with the same characters repeated.
	 * 
	 * @param s
	 *            the fill string
	 */
	public Var fill(String s) { 
		StringBuilder tmp = new StringBuilder();
		if(s == "") s = " ";
		while(tmp.length() <= displayLength) 
			tmp.append(s);
		return(set(tmp.substring(0,displayLength)));
	}

	/**
	 * Fills a Var with the same characters repeated.
	 * 
	 * @param s
	 *            the fill string
	 */
	public Var fill(int s) { 
		StringBuilder tmp = new StringBuilder();
		while(tmp.length() <= displayLength) 
			tmp.append(s);
		return(set(tmp.substring(0,displayLength)));
	}

	/**
	 * clears the value of the Var to be 0 for numerics or "" for CHAR.
	 */
	public void clear() { //NOT overridden in Group
		if((attr & (CHAR|PICTURE)) != 0)	set("");
		else set(0);
		attr &= ~(ISASSIGNED);
	}

	/* Truncate an integer value from the left according to length 'l'. */
	/*
	private int i_chop(int v, int l) {
		int lval;

		if (l < 0 || l > 9) {
			attr |= OVERFLOW;
			return (v);
		}

		lval = v % (int)lpowers[l];
		if (v == lval)
			attr &= ~(OVERFLOW);
		else
			attr |= OVERFLOW;
		return (lval);
	}
	*/

	
	private static final long DBL_SIGN = 0x8000000000000000L;
	@SuppressWarnings("unused")
	private static final long DBL_EXP =  0x7fe0000000000000L;
	@SuppressWarnings("unused")
	private static final long DBL_MANT = 0x001fffffffffffffL;
	
	@SuppressWarnings("unused")
	private double nf_chop(double v, int ll, int d) {
//		BigDecimal bd = new BigDecimal(v).setScale(d+1, RoundingMode.DOWN);
		boolean neg = (Double.doubleToLongBits(v) & DBL_SIGN) != 0 ? true : false;
		String ds = Double.toString(Math.abs(v));
		System.out.println("ds="+ds);
		BigDecimal bd = new BigDecimal(ds).setScale(d,RoundingMode.DOWN);
//		bd.setScale(d, RoundingMode.HALF_DOWN);
		if(neg) return -bd.doubleValue();
		else return bd.doubleValue();
	}

	
	/*
	 * Truncate a double value from the left and right according to length
	 * 'l,d'.
	 */
	protected double f_chop(double v, int ll, int d) {
		int mf;
		int lp;
		boolean sign;
		int l = ll;

		if (l <= 0 || d < 0)
			return (v);

		if (l >= d)
			l -= d;
		// display("\n%s(%18.18f, %d, %d);", __func__, v, ll, d);

		if (v < 0) {
			sign = true;
			v = -v;
		} else
			sign = false;

		if (d < FPU_PREC) {
			if (v < 1.0) {
				lp = 0;
				double xx = v;
				//Discount 0's after decimal point
				while(lp < FPU_PREC) {
					xx *= 10;
					if((int)xx > 0) break;
					lp++;
				}
			} else
				lp = (int) Math.floor(Math.log10(v)) + 1;
			if (lp > l) { // First truncate from left
				v = v % powerFloat[l]; // Truncate left
				// lp = l;
			}
			if ((d + lp) >= (FPU_PREC+1))
				return (sign ? -v : v);
			mf = FPU_PREC - lp - 1;

			if (mf >= 0 && mf <= MAX_MULTIPLIER) {
				// if((mf - d) > 2) mf -= (mf - d) / 2;
				if (mf <= d)
					mf = d;
				v *= powerFloat[mf];
				
				//In case of lost precision (e.g. 123456.123 - 123456.14) reduce rounding point until
				//we reach a 9 or 0 or are 2 greater than dec.
				while(mf > (d+4)) {
					double x = v % 1;
					if(x >= 0.9 && ((x/10) % 1) >= 0.9) break;
					mf--;
					v /= 10;
				}
				
//				System.out.println("v%1="+(v%1));
//				System.out.println("v%1="+Math.round(v));
				if (v % 1 < 0.5) {
					// display("floor: lp=%d, d=%d, mf=%d v=%.4f
					// fmod=%.3f",lp,d,mf,v,fmod(v,1));
					v = Math.floor(v);
				} else {
					// display("ceil: lp=%d, d=%d, mf=%d v=%.4f
					// fmod=%.3f",lp,d,mf,v,fmod(v,1));
					v = Math.ceil(v);
				}
				// display("result: v=%.4f", v);
				v /= powerFloat[mf - d];
				// display("v/%.0f: v=%.18f", fpowers[mf - d],v);
				v = Math.floor(v); // Truncate right 
				// display("trunc(v): v=%.18f", v);
				v /= powerFloat[d];
				// display("v/%.0f: v=%.18f", fpowers[d],v);
			}
		}

		if (l < MAX_MULTIPLIER) {
			if (v >= powerFloat[l]) {
				// ovf_len = ll;
				// ovf_dec = d;
				// ovf_val = v;
				attr |= OVERFLOW;
			} 
			v = v % powerFloat[l]; /* Truncate left */
		} else {
			if (v >= powerFloat[MAX_MULTIPLIER])
				attr |= OVERFLOW;
			
			v = v % powerFloat[MAX_MULTIPLIER];
		}

		if (sign)
			return -v;
		return v;
	}

	/**
	 * Truncates a decimal number as a whole number.
	 * @param v
	 * @return
	 */
	protected double f_chop(double v) {
		int mf;
		int lp;
		boolean sign;

		if (v < 0) {
			sign = true;
			v = -v;
		} else {
			sign = false;
		}

		if (v < 1.0) {
			lp = 0;
			double xx = v;
			//Discount 0's after decimal point
			while(lp < FPU_PREC) {
				xx *= 10;
				if((int)xx > 0) break;
				lp++;
			}
		} else {
			lp = (int) Math.floor(Math.log10(v)) + 1;
		}

		if (lp >= (FPU_PREC+1))
			return (sign ? -v : v);
		mf = FPU_PREC - lp - 1;

		if (mf >= 0 && mf <= MAX_MULTIPLIER) {
			// if((mf - d) > 2) mf -= (mf - d) / 2;
			v *= powerFloat[mf];

			//In case of lost precision (e.g. 123456.123 - 123456.14) reduce rounding point until
			//we reach a 9 or 0 or are 2 greater than dec.
			while(mf > (4)) {
				double x = v % 1;
				if(x >= 0.9 && ((x/10) % 1) >= 0.9) break;
				mf--;
				v /= 10;
			}

			if (v % 1 < 0.5) {
				v = Math.floor(v);
			} else {
				v = Math.ceil(v);
			}
			v /= powerFloat[mf];
			v = Math.floor(v); // Truncate right 
		}

		//			v = v % fpowers[MAX_MULTIPLIER];

		if (sign)
			return -v;
		return v;
	}

	/**
	 * Assign the parameter to the Var and set the MODIFIED attribute.
	 * 
	 * @param val
	 *            the variable to assign
	 */
	public void flag(double val) { //NOT overidden in Group
		set(val);
		attr |= MODIFIED;
	}

	/**
	 * Assign the parameter to the Var and set the MODIFIED attribute.
	 * 
	 * @param val
	 *            the variable to assign
	 */
	public void flag(long val) { //NOT overidden in Group
		set(val);
		attr |= MODIFIED;
	}

	/**
	 * Assign the parameter to the Var and set the MODIFIED attribute.
	 * 
	 * @param val
	 *            the variable to assign
	 */
	public void flag(String val) {//NOT overidden in Group
		set(val);
		attr |= MODIFIED;
	}

	/**
	 * Assign the parameter to the Var and set the MODIFIED attribute.
	 * 
	 * @param val
	 *            the variable to assign
	 */
	public void flag(int val) {//NOT overidden in Group
		set(val);
		attr |= MODIFIED;
	}

	/**
	 * Assign the parameter to the Var and set the MODIFIED attribute.
	 * 
	 * @param v
	 *            the variable to assign
	 */
	public void flag(Var v) {//NOT overidden in Group
		set(v);
		attr |= MODIFIED;
	}

	/**
	 * Assign the parameter to the Var and set the MODIFIED attribute.
	 * 
	 * @param ival
	 *            the variable to assign
	 */
	public void flag(Integer ival) {//NOT overidden in Group
		flag(ival.intValue());
	}

	/**
	 * Assign the parameter to the Var and set the MODIFIED attribute.
	 * 
	 * @param lval
	 *            the variable to assign
	 */
	public void flag(Long lval) {//NOT overidden in Group
		flag(lval.longValue());
	}

	/**
	 * Assign the parameter to the Var and set the MODIFIED attribute.
	 * 
	 * @param dval
	 *            the variable to assign
	 */
	public void flag(Double dval) {//NOT overidden in Group
		flag(dval.doubleValue());
	}

	/**
	 * Add a Var to a Var giving a Var.
	 * 
	 * @param val
	 *            the Var to add
	 * @return a Var with the result of the addition
	 */
	public Var add(Var val) { //NOT overidden in Group
		if (this.overflow())
			this.attr &= ~(OVERFLOW);
		Var v = new Var(this);
		if ((attr & (LONG)) != 0)
			v.set(val.getLong() + this.getLong());
		else if ((attr & (DOUBLE)) != 0)
			v.set(val.getDouble() + this.getDouble());
		if (v.overflow())
			this.attr |= OVERFLOW;
		return v;
	}

	/**
	 * Add the parameter to "this" returning the resulting sum.
	 * 
	 * @param val
	 *            the value to add
	 * @return the result of the addition
	 */
	public Var add(long val) {//NOT overidden in Group
		if (this.overflow())
			this.attr &= ~(OVERFLOW);
		Var v = new Var(this);
		if ((attr & (LONG)) != 0)
			v.set(val + this.getLong());
		else if ((attr & (DOUBLE)) != 0)
			v.set(val + this.getDouble());
		if (v.overflow())
			this.attr |= OVERFLOW;
		return v;
	}

	/**
	 * Add the parameter to "this" returning the resulting sum.
	 * 
	 * @param val
	 *            the value to add
	 * @return the result of the addition
	 */
	public Var add(double val) {//NOT overidden in Group
		if (this.overflow())
			this.attr &= ~(OVERFLOW);
		Var v = new Var(this);
		if ((attr & (LONG)) != 0)
			v.set((long) val + this.getLong());
		else if ((attr & (DOUBLE)) != 0)
			v.set(val + this.getDouble());
		if (v.overflow())
			this.attr |= OVERFLOW;
		return v;
	}

	/**
	 * Subtract the value of the parameter from the Var resulting in a Var.
	 * 
	 * @param val
	 *            the amount to substract
	 * @return the result of the subtraction
	 */
	public Var minus(Var val) {//NOT overidden in Group
		if (this.overflow())
			this.attr &= ~(OVERFLOW);
		Var v = new Var(this);
		if ((attr & (LONG)) != 0)
			v.set(this.getLong() - val.getLong());
		else if ((attr & (DOUBLE)) != 0)
			v.set(this.getDouble() - val.getDouble());
		if (v.overflow())
			this.attr |= OVERFLOW;
		return v;
	}

	/**
	 * Subtract the value of the parameter from the Var resulting in a Var.
	 * 
	 * @param val
	 *            the amount to substract
	 * @return the result of the subtraction
	 */
	public Var minus(long val) {//NOT overidden in Group
		if (this.overflow())
			this.attr &= ~(OVERFLOW);
		Var v = new Var(this);
		if ((attr & (LONG)) != 0)
			v.set(this.getLong() - val);
		else if ((attr & (DOUBLE)) != 0)
			v.set(this.getDouble() - val);
		if (v.overflow())
			this.attr |= OVERFLOW;
		return v;
	}

	/**
	 * Subtract the value of the parameter from the Var resulting in a Var.
	 * 
	 * @param val
	 *            the amount to substract
	 * @return the result of the subtraction
	 */
	public Var minus(double val) {//NOT overidden in Group
		if (this.overflow())
			this.attr &= ~(OVERFLOW);
		Var v = new Var(this);
		if ((attr & (LONG)) != 0)
			v.set(this.getLong() - (long) val);
		else if ((attr & (DOUBLE)) != 0)
			v.set(this.getDouble() - val);
		if (v.overflow())
			this.attr |= OVERFLOW;
		return v;
	}

	/**
	 * @param val
	 *            the amount to divide by.
	 * @return gives the result of the division or 0 for any arithmetic issues.
	 */
	public Var divide(Var val) {//NOT overidden in Group
		if (this.overflow())
			this.attr &= ~(OVERFLOW);
		int newdec = scale + getPrecision(val.getDouble());
		if(val.scale > newdec) newdec = val.scale;
		
		Var v = new Var(Var.NUMBER, displayLength+newdec, newdec);
		try {
			v.set(this.getDouble() / val.getDouble());
		} catch (ArithmeticException ae) {
			v.set(0);
			v.attr |= OVERFLOW;
		}

		if (v.overflow())
			this.attr |= OVERFLOW;
		return v;
	}

	/**
	 * Divides the divisor by the divider and truncates the answer to the number of
	 * decimal places in this.
	 * @param divisor The value to be divided
	 * @param divider The divider value
	 * @return
	 */
	public Var setDivide(Var divisor, Var divider) {//NOT overidden in Group
		if (this.overflow())
			this.attr &= ~(OVERFLOW);
		if(divider.eq(0)) {
			this.set(0);
			return this;
		}
		try {
			this.set(divisor.getDouble() / divider.getDouble());
		} catch (ArithmeticException ae) {
			this.set(0);
			this.attr |= OVERFLOW;
		}
		return this;
	}
	/**
	 * Divides the divisor by the divider and truncates the answer to the number of
	 * decimal places in this.
	 * @param divisor The value to be divided
	 * @param divider The divider value
	 * @param remainder
	 * @return
	 */
	public Var setDivide(Var divisor, Var divider, Var remainder) {//NOT overidden in Group
		if (this.overflow())
			this.attr &= ~(OVERFLOW);
		if(divider.eq(0)) {
			this.set(0);
			return this;
		}
		try {
			Var saveDivisor = new Var (divisor); //In case giving(this) is the same as divisor
			this.set(divisor.getDouble() / divider.getDouble());
			remainder.set(saveDivisor.getDouble() - (divider.getDouble() * this.getDouble()));
		} catch (ArithmeticException ae) {
			this.set(0);
			this.attr |= OVERFLOW;
		}
		return this;
	}
	/**
	 * Divides the divisor by the divider and truncates the answer to the number of
	 * decimal places in this.
	 * @param divisor The value to be divided
	 * @param divider The divider value
	 * @return
	 */
	public Var setDivide(double divisor, Var divider) {//NOT overidden in Group
		return setDivide(new Var(divisor), divider);
	}
	/**
	 * Divides the divisor by the divider and truncates the answer to the number of
	 * decimal places in this.
	 * @param divisor The value to be divided
	 * @param divider The divider value
	 * @return
	 */
	public Var setDivide(Var divisor, double divider) {//NOT overidden in Group
		return setDivide(divisor,new Var(divider));
	}
	/**
	 * Divides the divisor by the divider and truncates the answer to the number of
	 * decimal places in this.
	 * @param divisor The value to be divided
	 * @param divider The divider value
	 * @return
	 */
	public Var setDivide(double divisor, double divider, Var remainder) {//NOT overidden in Group
		return setDivide(new Var(divisor), new Var(divider), remainder);
	}
	/**
	 * Divides the divisor by the divider and truncates the answer to the number of
	 * decimal places in this.
	 * @param divisor The value to be divided
	 * @param divider The divider value
	 * @param remainder
	 * @return
	 */
	public Var setDivide(double divisor, Var divider, Var remainder) {//NOT overidden in Group
		return setDivide(new Var(divisor), divider, remainder);
	}
	/**
	 * Divides the divisor by the divider and truncates the answer to the number of
	 * decimal places in this.
	 * @param divisor The value to be divided
	 * @param divider The divider value
	 * @param remainder
	 * @return
	 */
	public Var setDivide(Var divisor, double divider, Var remainder) {//NOT overidden in Group
		return setDivide(divisor,new Var(divider), remainder);
	}
	/**
	 * Divides the divisor by the divider and truncates the answer to the number of
	 * decimal places in this.
	 * @param divisor The value to be divided
	 * @param divider The divider value
	 * @return
	 */
	public Var setDivide(double divisor, double divider) {//NOT overidden in Group
		return setDivide(new Var(divisor), new Var(divider));
	}
	
	/**
	 * Divides the divisor by the divider and rounds the answer to the number of
	 * decimal places in this.
	 * @param divisor The value to be divided
	 * @param divider The divider value
	 * @return
	 */
	public Var setDivideRounded(Var divisor, Var divider) {//NOT overidden in Group
		if (this.overflow())
			this.attr &= ~(OVERFLOW);
		if(divider.eq(0)) {
			this.set(0);
			return this;
		}
		try {
			if (scale == 0)
				this.set(new Var(divisor.getDouble() / divider.getDouble()).round());
			else
				this.set(new Var(divisor.getDouble() / divider.getDouble()).roundDouble(scale));
			} catch (ArithmeticException ae) {
			this.set(0);
			this.attr |= OVERFLOW;
		}
		return this;
	}

	/**
	 * Divides the divisor by the divider and rounds the answer to the number of
	 * decimal places in this.
	 * @param divisor The value to be divided
	 * @param divider The divider value
	 * @return
	 */
	public Var setDivideRounded(double divisor, Var divider) {//NOT overidden in Group
		return setDivideRounded(new Var(divisor), divider);
	}
	/**
	 * Divides the divisor by the divider and rounds the answer to the number of
	 * decimal places in this.
	 * @param divisor The value to be divided
	 * @param divider The divider value
	 * @return
	 */
	public Var setDivideRounded(Var divisor, double divider) {//NOT overidden in Group
		return setDivideRounded(divisor, new Var(divider));
	}
	/**
	 * Divides the divisor by the divider and rounds the answer to the number of
	 * decimal places in this.
	 * @param divisor The value to be divided
	 * @param divider The divider value
	 * @return
	 */
	public Var setDivideRounded(double divisor, int divider) {//NOT overidden in Group
		return setDivideRounded(new Var(divisor), new Var(divider));
	}
	
	/**
	 * Divides the divisor by the divider and rounds the answer to the number of
	 * decimal places in this.
	 * @param divisor The value to be divided
	 * @param divider The divider value
	 * @param remainder
	 * @return
	 */
	public Var setDivideRounded(Var divisor, Var divider, Var remainder) {//NOT overidden in Group
		if (this.overflow())
			this.attr &= ~(OVERFLOW);
		if(divider.eq(0)) {
			this.set(0);
			return this;
		}
		try {
			if (scale == 0)
				this.set(new Var(divisor.getDouble() / divider.getDouble()).round());
			else
				this.set(new Var(divisor.getDouble() / divider.getDouble()).roundDouble(scale));
			remainder.set(divisor.getDouble() % divider.getDouble());
		} catch (ArithmeticException ae) {
			this.set(0);
			this.attr |= OVERFLOW;
		}
		return this;
	}

	/**
	 * Divides the divisor by the divider and rounds the answer to the number of
	 * decimal places in this.
	 * @param divisor The value to be divided
	 * @param divider The divider value
	 * @param remainder
	 * @return
	 */
	public Var setDivideRounded(double divisor, Var divider, Var remainder) {//NOT overidden in Group
		return setDivideRounded(new Var(divisor), divider, remainder);
	}
	/**
	 * Divides the divisor by the divider and rounds the answer to the number of
	 * decimal places in this.
	 * @param divisor The value to be divided
	 * @param divider The divider value
	 * @param remainder
	 * @return
	 */
	public Var setDivideRounded(Var divisor, double divider, Var remainder) {//NOT overidden in Group
		return setDivideRounded(divisor, new Var(divider), remainder);
	}
	/**
	 * Divides the divisor by the divider and rounds the answer to the number of
	 * decimal places in this.
	 * @param divisor The value to be divided
	 * @param divider The divider value
	 * @param remainder
	 * @return
	 */
	public Var setDivideRounded(double divisor, int divider, Var remainder) {//NOT overidden in Group
		return setDivideRounded(new Var(divisor), new Var(divider), remainder);
	}
	/**
	 * This implements returnval = this / val.
	 * 
	 * @param val
	 *            the amount to divide by.
	 * @return gives the result of the division or 0 for any arithmetic issues.
	 */
	public Var divide(long val) {//NOT overidden in Group
		if (this.overflow())
			this.attr &= ~(OVERFLOW);
		int decimalIncr = getPrecision(val);
		Var v = new Var(Var.NUMBER, displayLength, scale + decimalIncr);
		try {
			v.set(this.getDouble() / val);
		} catch (ArithmeticException ae) {

			v.set(0);
			v.attr |= OVERFLOW;
		}
		if (v.overflow())
			this.attr |= OVERFLOW;
		return v;
	}

	/**
	 * This implements returnval = this / val.
	 * 
	 * @param val
	 *            the amount to divide by.
	 * @return gives the result of the division or 0 for any arithmetic issues.
	 */
	public Var divide(double val) {//NOT overidden in Group
		if (this.overflow())
			this.attr &= ~(OVERFLOW);
		int decimalIncr = getPrecision(val);
		Var v = new Var(Var.NUMBER, displayLength, scale + decimalIncr);
		try {
			v.set(this.getDouble() / val);
		} catch (ArithmeticException ae) {
			v.set(0);
			v.attr |= OVERFLOW;
		}
		if (v.overflow())
			this.attr |= OVERFLOW;
		return v;
	}

	/*
	private int max(int a, int b) {
		if (a > b)
			return a;
		else
			return b;
	}
	*/
	
	private int min(int a, int b) {
		if (a < b)
			return a;
		return b;
	}
	/**
	 * Implements resultvar = this * val.
	 * 
	 * @param val
	 *            the amount by which to multiply.
	 * @return the result of the multiplication.
	 */
	public Var times(Var val) {//NOT overidden in Group
		if (this.overflow())
			this.attr &= ~(OVERFLOW);
		
		Var v;
		
		if((attr & (DOUBLE)) != 0 || (val.attr & (DOUBLE)) != 0) {
			double dv;
			if((attr & (LONG)) != 0) dv = val.getDouble() * getLong();
			else if((val.attr & (LONG)) != 0) dv = val.getLong() * getDouble();
			else dv = val.getDouble() * getDouble();
			int d = val.scale + scale;
			int l = getPrecision(dv) + d;
			v = new Var(NUMBER, l, d);
			v.set(dv);
		} else {
			long lv = val.getLong() * getLong();
			v = new Var(NUMBER, getPrecision(lv));
			v.set(lv);
		}
		if (v.overflow())
			this.attr |= OVERFLOW;
		return v;
	}

	/**
	 * Implements resultvar = this * val.
	 * 
	 * @param val
	 *            the amount by which to multiply.
	 * @return the result of the multiplication.
	 */
	public Var times(long val) {//NOT overidden in Group
		if (this.overflow())
			this.attr &= ~(OVERFLOW);
		Var v;
		if ((attr & (LONG)) != 0) {
			long l = this.getLong() * val;
			v = new Var(NUMBER, getPrecision(l));
			v.set(l);
		} else {
			double d = this.getDouble() * val;
			v = new Var(NUMBER, getPrecision(d) + scale, scale);
			v.set(d);
		}
		if (v.overflow())
			this.attr |= OVERFLOW;
		return v;
	}

	/**
	 * Implements resultvar = this * val.
	 * 
	 * @param val
	 *            the amount by which to multiply.
	 * @return the result of the multiplication.
	 */
	public Var times(double val) {//NOT overidden in Group
		Var v;
		if (this.overflow())
			this.attr &= ~(OVERFLOW);
		if ((attr & (LONG)) != 0) {
			double d = this.getLong() * val;
			v = new Var(d);
		} else {
			double d = this.getDouble() * val;
			v = new Var(NUMBER, getPrecision(d) + (2 * scale), 2 * scale);
			v.set(d);
		}
		if (v.overflow())
			this.attr |= OVERFLOW;
		return v;
	}

	/**
	 * Implements resultVar = this % val.
	 * 
	 * @param val
	 * @return the result of the modulus
	 */
	public Var mod(Var val) {//NOT overidden in Group
		Var v = new Var(this);
		if ((attr & (LONG)) != 0)
			v.set(this.getLong() % val.getLong());
		else if ((attr & (DOUBLE)) != 0)
			v.set(this.getDouble() % val.getDouble());
		return v;
	}

	/**
	 * Implements resultVar = this % val.
	 * 
	 * @param val
	 * @return the result of the modulus
	 */
	public Var mod(long val) {//NOT overidden in Group
		Var v = new Var(this);
		if ((attr & (LONG)) != 0)
			v.set(this.getLong() % val);
		else if ((attr & (DOUBLE)) != 0)
			v.set(this.getDouble() % val);
		return v;
	}

	/**
	 * Implements resultVar = this % val.
	 * 
	 * @param val
	 * @return the result of the modulus
	 */
	public Var mod(double val) {//NOT overidden in Group
		Var v = new Var(this);
		if ((attr & (LONG)) != 0)
			v.set(this.getLong() % (long) val);
		else if ((attr & (DOUBLE)) != 0)
			v.set(this.getDouble() % val);
		return v;
	}
	
	/**
	 * The DIV function returns an integer equal to the integer part 
	 * of the quotient after division.
	 * 
	 * @param val
	 * @return the result of the div
	 */
	public Var div(Var val) {//NOT overidden in Group
		Var v = new Var(this);
		Var v2 = new Var(this);
		if ((attr & (LONG)) != 0)
			v.set(this.getLong() / val.getLong());
		else if ((attr & (DOUBLE)) != 0)
			v.set(this.getDouble() / val.getDouble());
		v2.set(v.getInt());
		return v2;
	}

	/**
	 * The DIV function returns an integer equal to the integer part 
	 * of the quotient after division.
	 * 
	 * @param val
	 * @return the result of the div
	 */
	public Var div(long val) {//NOT overidden in Group
		Var v = new Var(this);
		Var v2 = new Var(this);
		if ((attr & (LONG)) != 0)
			v.set(this.getLong() / val);
		else if ((attr & (DOUBLE)) != 0)
			v.set(this.getDouble() / val);
		v2.set(v.getInt());
		return v2;
	}

	/**
	 * The DIV function returns an integer equal to the integer part 
	 * of the quotient after division.
	 * 
	 * @param val
	 * @return the result of the div
	 */
	public Var div(double val) {//NOT overidden in Group
		Var v = new Var(this);
		Var v2 = new Var(this);
		if ((attr & (LONG)) != 0)
			v.set(this.getLong() / (long) val);
		else if ((attr & (DOUBLE)) != 0)
			v.set(this.getDouble() / val);
		v2.set(v.getInt());
		return v2;
	}
	
	/**
	 * gives 'this' raised to the power given.
	 * 
	 * @param g
	 *            a Var
	 * @return this raised to the power or null if this isn't NUMERIC.
	 */
	public Var power(Var g) {
		if ((attr & (NUMERIC)) == 0) return null;
		Double d = new Double(Math.pow(getDouble(), g.getDouble()));
		String s = d.toString();//NOT overidden in Group
		Var retval = null;
		int i;
		if ((i = s.indexOf('.')) != -1) {
			retval = new Var(NUMERIC, s.length(), s.length() - i - 1);
			retval.set(d);
		} else {
			retval = new Var(NUMERIC, s.length(), 0);
			retval.set(d);
		}
		return retval;
	}

	/**
	 * gives 'this' raised to the power given.
	 * 
	 * @param g
	 *            a Long
	 * @return this raised to the power or null if this isn't NUMERIC.
	 */
	public Var power(Long g) {//NOT overidden in Group
		if ((attr & (NUMERIC)) == 0) return null;
		Double d = new Double(Math.pow(getDouble(), g));
		String s = d.toString();
		Var retval = null;
		int i;
		if ((i = s.indexOf('.')) != -1) {
			retval = new Var(NUMERIC, s.length(), s.length() - i - 1);
			retval.set(d);
		} else {
			retval = new Var(NUMERIC, s.length(), 0);
			retval.set(d);
		}
		return retval;
	}

	/**
	 * gives 'this' raised to the power given.
	 * 
	 * @param g
	 *            a long
	 * @return this raised to the power or null if this isn't NUMERIC.
	 */
	public Var power(long g) {//NOT overidden in Group
		Long l = new Long(g);
		return power(l);
	}

	/**
	 * gives 'this' raised to the power given.
	 * 
	 * @param g
	 *            a Double
	 * @return this raised to the power or null if this isn't NUMERIC.
	 */
	public final Var power(Double g) {
		if ((attr & (NUMERIC)) == 0) return null;
		BigDecimal d = new BigDecimal(Math.pow(getDouble(), g));
		String s = d.toString();
		Var retval = null;
		int i;
		if ((i = s.indexOf('.')) != -1) {
			retval = new Var(NUMERIC, s.length(), s.length() - i - 1);
			retval.set(d.doubleValue());
		} else {
			retval = new Var(NUMERIC, s.length(), 0);
			retval.set(d.doubleValue());
		}
		return retval;
	}

	/**
	 * gives 'this' raised to the power given.
	 * 
	 * @param g
	 *            a double
	 * @return this raised to the power or null if this isn't NUMERIC.
	 */
	public Var power(double g) {//NOT overidden in Group
		Double l = new Double(g);
		return power(l);
	}

	/**
	 * gives 'this' raised to the power given.
	 * 
	 * @param g
	 *            a Integer
	 * @return this raised to the power or null if this isn't NUMERIC.
	 */
	public final Var power(Integer g) {//NOT overidden in Group
		if ((attr & (NUMERIC)) == 0) return null;
		//BigDecimal d = new BigDecimal(Math.pow(getDouble(), g));
		BigDecimal d = new BigDecimal(getString());
		return(new Var(d.pow(g).toString()));
		//Var retval = null;
		/*int i;
		if ((i = s.indexOf('.')) != -1) {
			retval = new Var(NUMERIC, s.length(), s.length() - i - 1);
			retval.set(d.doubleValue());
		} else {
			retval = new Var(NUMERIC, s.length(), 0);
			retval.set(d.doubleValue());
		}*/
		//return retval;
	}

	/**
	 * gives 'this' raised to the power given.
	 * 
	 * @param g
	 *            a int
	 * @return this raised to the power or null if this isn't NUMERIC.
	 */
	public Var power(int g) {//NOT overidden in Group
		Integer l = new Integer(g);
		return power(l);
	}

	/**
	 * round the value of a Var (to zero decimal places).
	 * 
	 * @return the rounded result if its a decimal, the int or long if there are
	 *         no decimals, or null if it's a STRING.
	 */
	public Var round() {//NOT overidden in Group
		if ((attr & (CHAR)) != 0)
			return null;
		if ((attr & (LONG)) != 0) {
			Var v = new Var(this);
			v.set(this);
			return v;
		}
		long l = Math.round(getDoubleValue());
		Var v = new Var(NUMERIC, displayLength - scale, 0);
		v.set(l);
		return v;
	}
	
	/**
	 * round the value of a Var (to places decimal places).
	 * 
	 * @param places Number of deciaml places to round to
	 * 
	 * @return the rounded result if its a decimal, the int or long if there are
	 *         no decimals, or null if it's a STRING.
	 */
	
    public Var roundDouble(int places) {
		if ((attr & (CHAR)) != 0)
			return null;
		if ((attr & (LONG)) != 0) {
			Var v = new Var(this);
			v.set(this);
			return v;
		}
		int power = (int)Math.pow(10, places);
		double d = getDouble();
		boolean sign;
		if(d < 0) {
			sign = true;
			d = Math.abs(d);
		} else sign = false;
		d *= power;
		d = f_chop(d, displayLength, scale - places);
		d = Math.round(d);
		d /= power;
		if(sign) d = -d;
		return new Var(d);
    }



	/**
	 * truncates the decimals from Var.
	 * 
	 * @return the truncated result if its a decimal, the int or long if there
	 *         are no decimals, or null if it's a STRING.
	 */
	public Var trunc() {
		if ((attr & (CHAR)) != 0)
			return null;
		if ((attr & (LONG)) != 0) {
			Var v = new Var(this);
			v.set(this);
			return v;
		}
		double l = Math.floor(getDoubleValue());
		Var v = new Var(NUMERIC, displayLength - scale - 1, 0);
		v.set(l);
		return v;
	}
	/**
	 * Truncate the value of a Var (to places decimal places).
	 * 
	 * @param places Number of deciaml places to truncate to
	 * 
	 * @return the truncated result if its a decimal, the int or long if there are
	 *         no decimals, or null if it's a STRING.
	 */
	
    public Var truncDouble(int places) {
		if ((attr & (CHAR)) != 0)
			return null;
		if ((attr & (LONG)) != 0) {
			Var v = new Var(this);
			v.set(this);
			return v;
		}
		//double d = Math.round(getDouble() * Math.pow(10, (double) places)) / Math.pow(10,
		        //(double) places);
		double d = Math.floor(getDouble() * Math.pow(10, places)) / Math.pow(10,
		        places);
		Var v = new Var(this);
		v.set(d);
		return v;
    }
	/**
	 * prints the value of the current Var prefixed by vname.
	 * 
	 * @param vname
	 *            text to add to output
	 */
	public void print(String vname) {//NOT overridden in Group
		if ((attr & (CHAR|PICTURE)) != 0) {
			System.out.print("    " + vname + " CHAR(" + displayLength);
		} else if ((attr & (LONG | DOUBLE)) != 0) {
			System.out.print("    " + vname + " NUMERIC(" + displayLength);
			if ((attr & (DOUBLE)) != 0 && scale > 0)
				System.out.print("," + scale);
		}
		System.out.print(") = ");
		print();
	}

	public int indexOf(String regex, int from) {
		if(value instanceof StringBuilder)
			return ((StringBuilder)value).indexOf(regex, from);
		else return getString().indexOf(regex, from);
	}

	public int indexOf(String regex) {
		if(value instanceof StringBuilder)
			return ((StringBuilder)value).indexOf(regex);
		else return getString().indexOf(regex);
	}
	
	/**
	 * prints the value of the current Var .
	 */
	public void print() {//NOT overridden in Group
		if ((attr & (CHAR|PICTURE)) != 0) {
			System.out.println("\"" + getString() + "\"");
		} else if((attr & (LONG)) != 0) {
			System.out.println(getLongValue());
		} else if((attr & (DOUBLE)) != 0) {
			System.out.println(getDoubleValue());
		} else {
			System.out.println("Unknown data type: " + attr);
		}
	}

	/**
	 * prints the value of the current Var plus type and length information.
	 */
	public final void list() {//NOT overridden in Group
		System.out.print("  ITEM: type=" + attr + " len=" + displayLength + "," + scale
				+ " exp=" + exp + " Value=");
		if((attr & (CHAR|STRING|PICTURE)) != 0) {
			System.out.print("\"" + getString() + "\"");
		} else if((attr & (LONG)) != 0) {
			System.out.print(getLongValue());
		} else if((attr & (DOUBLE)) != 0) {
			System.out.print(getDoubleValue());
		} else {
			System.out.print("Unknown data type: " + attr);
		}
		System.out.print(" attr(");
		String comma = "";
		/*if((attr & (GROUP)) != 0) {
		    if((attr & (GROUP_MAIN)) != 0) System.out.print("SUB-GROUP,");
		    else if((attr & (GROUP_MEMBER)) != 0) System.out.print("GROUP-MEMBER,");
		    else if((attr & (GROUP)) != 0) System.out.print("GROUP-MASTER,");
		}*/
		if ((attr & (CHAR)) != 0) {
			System.out.print("CHAR");
			comma = ",";
		}
		if ((attr & (STRING)) != 0) {
			System.out.print(comma + "STRING");
			comma = ",";
		}
		if ((attr & (NUMERIC)) != 0) {
			System.out.print(comma + "NUMERIC");
			comma = ",";
		}
		if ((attr & (UNSIGNED)) != 0) {
			System.out.print(comma + "UNSIGNED");
			comma = ",";
		}
		if ((attr & (LONG)) != 0) {
			System.out.print(comma + "LONG");
			comma = ",";
		}
		if ((attr & (DOUBLE)) != 0) {
			System.out.print(comma + "DOUBLE");
			comma = ",";
		}
		if ((attr & (AUTO)) != 0) {
			System.out.print(comma + "AUTO");
			comma = ",";
		}
		if ((attr & (ZEROBLANK)) != 0) {
			System.out.print(comma + "ZEROBLANK");
			comma = ",";
		}
		if ((attr & (STARFILL)) != 0) {
			System.out.print(comma + "STARFILL");
			comma = ",";
		}
		if ((attr & (NEGDR)) != 0) {
			System.out.print(comma + "NEGDR");
			comma = ",";
		}
		if ((attr & (NEGCR)) != 0) {
			System.out.print(comma + "NEGCR");
			comma = ",";
		}
		if ((attr & (EDITPLUS)) != 0) {
			System.out.print(comma + "EDITPLUS");
			comma = ",";
		}
		if ((attr & (LEADING_SIGN)) != 0) {
			System.out.print(comma + "LEADING_SIGN");
			comma = ",";
		}
		if ((attr & (DOLLAR)) != 0) {
			System.out.print(comma + "DOLLAR");
			comma = ",";
		}
		if ((attr & (ZEROFILL)) != 0) {
			System.out.print(comma + "ZEROFILL");
			comma = ",";
		}
		if ((attr & (PIP)) != 0) {
			System.out.print(comma + "PIP");
			comma = ",";
		}
		if ((attr & (DATE)) != 0) {
			System.out.print(comma + "DATE");
			comma = ",";
		}
		if ((attr & (MODIFIED)) != 0) {
			System.out.print(comma + "MODIFIED");
			comma = ",";
		}
		if ((attr & (OVERFLOW)) != 0) {
			System.out.print(comma + "OVERFLOW");
			comma = ",";
		}
		if ((attr & (DBUPDATE)) != 0) {
			System.out.print(comma + "DBUPDATE");
			comma = ",";
		}
		if ((attr & (ISASSIGNED)) != 0) {
			System.out.print(comma + "ISASSIGNED");
			comma = ",";
		}
		System.out.println(")");
	}
	
	/**
	 * returns the value of the Var as an int.
	 * 
	 * @return the int result or 0 if the Var is a String and cannot be
	 *         converted
	 */
	public int getInt() {//overridden in Group
		if ((attr & (LONG)) != 0 && (attr & (PICTURE)) == 0)
			return (int)(getLongValue() % 1000000000L);
		else if ((attr & (DOUBLE)) != 0 && (attr & (PICTURE)) == 0)
			return (int)(((long)getDoubleValue()) % 1000000000L);
		else {
		    int b, e;
			boolean sign;
			StringBuilder sv = (StringBuilder)value;
			if(sv.length() < displayLength) {
				sv = new StringBuilder(getString());
				value = sv;
			}
		    for(b=0; b < displayLength && sv.charAt(b) == ' '; b++) ;
			if(b >= displayLength || b >= sv.length()) return 0;
			if(sv.charAt(b) == '-') { sign = true; b++; }
			else sign = false;
		    for(e=b; e < displayLength && (Character.isDigit(sv.charAt(e)) || sv.charAt(e) == '-'); e++) ;
		    if(e == b) return 0;
			if((e - b) > 9) b = e - 9; //Truncate from LHS
		    try {
		        if(sign) return -Integer.parseInt(sv.substring(b, e));
				return Integer.parseInt(sv.substring(b, e));
		    } catch (NumberFormatException ex) {
		        return 0;
		    }
		}
	}

	/**
	 * returns the value of the Var as an long.
	 * 
	 * @return the long result or 0 if the Var is a String and cannot be
	 *         converted
	 */
	public long getLong() {
		return getLong(displayLength);
	}
	/**
	 * returns the value of the Var as an long.
	 * @param len If the 
	 * @return the long result or 0 if the Var is a String and cannot be
	 *         converted
	 */
	public long getLong(int len) {//overridden in Group
		//if(attr(GROUP)) return lincStrToLong(getString());
		if ((attr & (LONG)) != 0 && (attr & (PICTURE)) == 0)
			return getLongValue();
		else if ((attr & (DOUBLE)) != 0 && (attr & (PICTURE)) == 0)
			return (long) getDoubleValue();
		else {
			int b, e;
			boolean sign;
			StringBuilder sv = (StringBuilder)value;
			if(sv == null || sv.length() < 1) return 0;
			if(sv.length() > len) sv = new StringBuilder(sv.substring(0, len));
			len = sv.length();
			for(b=0; b < len && sv.charAt(b) == ' '; b++) ;
		    if(b >= len) return 0;
			if(sv.charAt(b) == '-') { sign = true; b++; }
		    else sign = false;
			for(e=b; e < len && Character.isDigit(sv.charAt(e)); e++) ;
			if(e == b) return 0;
			if((e - b) > 18) b = e - 18;
			try {
			    if(sign) return -Long.parseLong(sv.substring(b, e));
				return Long.parseLong(sv.substring(b, e));
			} catch (NumberFormatException ex) {
				return 0;
			}
		}
	}

	public double getDouble() {//overridden in Group
		return getDouble(displayLength, scale);
	}
	/**
	 * returns the value of the Var as an double.
	 * 
	 * @return the double result or 0 if the Var is a String and cannot be
	 *         converted
	 */
	public double getDouble(int len, int decimals) {//overridden in Group
		if ((attr & (LONG)) != 0 && (attr & (PICTURE)) == 0)
			return getLongValue() % 1000000000000000L;
		else if ((attr & (DOUBLE)) != 0 && (attr & (PICTURE)) == 0) {
//			if(length < len || decimals != dec) {
				return f_chop(getDoubleValue(), len, decimals);
//			} else return getDoubleValue();
		} else {
		    int b, e;
		    boolean sign;
		    StringBuilder sv = (StringBuilder)value;
			if(sv == null || sv.length() < 1) return 0;
		    if(sv.length() > len) sv = new StringBuilder(sv.substring(0, len));
			int maxLen = 15;
		    for(b=0; b < sv.length() && sv.charAt(b) == ' '; b++) ;
		    if(b >= sv.length()) return 0;
			if(sv.charAt(b) == '-') { sign = true; b++; }
		    else sign = false;
		    int pPos = -1;
			for(e=b; e < sv.length() && (Character.isDigit(sv.charAt(e)) || sv.charAt(e) == '.'); e++) {
				if(sv.charAt(e) == '.') {
					maxLen = 16;
					pPos = e;
				}
			} //1234.1234
		    if(e == b) return 0;
		    if((e - b) > maxLen) {
				// reduce by: (e-b)-Maxlen
				if(pPos != -1) {
				    //First truncate from right of period
					if((e - pPos) > ((e-b) - maxLen)) e = b + maxLen;
					else {
						e = pPos;
						maxLen = 15;
					}
				    if((e - b) > maxLen) b = e - maxLen;
				} else b = e - maxLen; //Truncate from LHS
			}

			try {
				double mv = Double.parseDouble(sv.substring(b,e));
			    if(sign) return -mv;
				return mv;
			} catch (NumberFormatException ex) {
				return 0;
			}
		}
	}

	/**
	 * returns the value of the Var as an float.
	 * 
	 * @return the float result or 0 if the Var is a String and cannot be
	 *         converted
	 */
	public float getFloat() {//overridden in Group
		if((attr & (LONG)) != 0 && (attr & (PICTURE)) == 0) return getLongValue();
		else if((attr & (DOUBLE)) != 0 && (attr & (PICTURE)) == 0) return (float)f_chop(getDoubleValue(), displayLength, scale);
		else return (float)getDouble();
	}

	protected static final String zeros = "00000000000000000000"; // 20 of them!
	private static final String stars = "********************"; // 20 of them!
	/**
	 * returns the value of the Var as an String.
	 * 
	 * @param rz
	 *            retain zeros (Y or N)
	 * @return the String result or "" if the Var is not assigned.
	 */
	public String getString(boolean rz) {//overridden in Group
		boolean negval = false;
		String res;
	    int len = this.displayLength;
		
		if ((attr & (CHAR|PICTURE)) != 0) {
			StringBuilder sv = (StringBuilder)value;
			if (sv == null)  {
				if(rz)  {
					StringBuffer sb = new StringBuffer(spaces.substring(0, len));
					return sb.toString();
				}
				return " "; 
			}
			if(!(value instanceof StringBuilder)) {
				System.out.println("value not a StringBuilder object as expected. Fixing.");
				String s;
				if(value instanceof Long) 
					s = Long.toString((Long)value);
				else if(value instanceof Double) 
					s = Double.toString((Double)value);
				else s = "";
				sv = new StringBuilder();
				sv.append(s);
				value = (StringBuilder)sv;
			}
			if (sv.length() > len && len > 0) {
				try {
					return sv.substring(0, len);
				} catch(Exception e) {
					System.out.println("Got exception AAA");
					sv = new StringBuilder();					
				}
			} else
				if(sv.length() > 0)
					return sv.toString();
				else
					if(rz)  {
						StringBuffer sb = new StringBuffer(spaces.substring(0, len));
						return sb.toString();
					}
					else
						return " "; 
		} 
		if ((attr & (LONG)) != 0) {
			if((attr & (AUTOVAR)) != 0 && len == 0) return "0";
			long lv = getLongValue();
			if(lv < 0) negval = true;
			res = Long.toString(Math.abs(lv));
		} else if ((attr & (DOUBLE)) != 0) {
			double dv = getDoubleValue();
			if(dv < 0.0) negval = true;
		    BigDecimal bd;
		    try {
		    	bd = new BigDecimal(f_chop(Math.abs(dv),len,scale)).setScale(scale, BigDecimal.ROUND_HALF_DOWN);
		    } catch (Exception e) {
		    	bd = new BigDecimal("0").setScale(scale, BigDecimal.ROUND_HALF_DOWN);
		    	attr |= OVERFLOW;
			}
		    res = bd.toPlainString();
		    if((attr & EXTRACT) != 0 && scale > 0) {
		    	int ix = res.indexOf('.');
		    	if(ix != -1)
		    		res = res.substring(0, ix) + res.substring(ix+1);
		    }
		} else
			return "Unknown datatype " + attr;
		
		//Only INTEGER, LONG and DOUBLE make it this far.
		
		if((attr & EXTRACT) == 0 && scale > 0) len++;
		if(rz) {
			if(res.length() < len) 
				res = zeros.substring(0, len - res.length()) + res;
			else if(res.length() > len)
				res = res.substring(res.length() - len);
		} else {
			if(res.length() > len) res = res.substring(res.length() - len);
		}
		
		if(negval && (attr & (UNSIGNED)) == 0) 
			return "-" + res;
		return res;
	}
	
	//Overridden in Group
	/**
	 * When a non-elementary item is going to receive data from another item.
	 * Negative values are absoluted.
	 * Numbers are prefixed with 0 to the required length
	 * @return
	 */
	public String getRawString() {
		String r;
		if ((attr & (LONG)) != 0) {
			long lv = getLongValue();
			r = Long.toString(Math.abs(lv));
			if(r.length() < displayLength) {
				r = zeros.substring(0, displayLength - r.length()) + r;
			} else if(r.length() > displayLength) {
				r = r.substring(0, displayLength);
			}
		} else if ((attr & (DOUBLE)) != 0) {
			double dv = getDoubleValue();
			if(scale > 0)
				dv *= powerFloat[scale];
			BigDecimal bd = new BigDecimal(Math.abs(dv)).setScale(0, BigDecimal.ROUND_HALF_DOWN);
			r = bd.toPlainString();
			if(r.length() < displayLength) {
				r = zeros.substring(0, displayLength - r.length()) + r;
			} else if(r.length() > displayLength) {
				r = r.substring(0, displayLength);
			}
		} else {
			r = getString();
		}
		
		return r;	
//		return adjustLengthRight(((StringBuilder)value).toString(), displayLength);
	}


	
	
	
	/**
	 * returns the value of the Var as an String. 
	 * 
	 * Strips leading zeros and returns a space if zero
	 * @return the String result or "" if the Var is not assigned.
	 */
	public String getStringLeftAlign(boolean rz) {
		//Was getString but extract fields get  no decimal point 
		String res;
		if(scale > 0) {
			String tmpRes=getString(false);
			if((attr & (EXTRACT)) != 0) {
				int lenRes=tmpRes.length();
				res=tmpRes.substring(0,lenRes - scale) + "." + tmpRes.substring(lenRes - scale);
			} else res=tmpRes;
			double d=getDouble();
			if(d < 0 && d > -1)
				res="-" + res.substring(2);
			res=res.replaceAll("^0*", "");
		} else
			res=this.getString(false);
		if (!rz && res.equals("0")) return "";
		else return formatThousands(res.trim(),null,true);
	}
	public String getMessageString() {
		if(scale==0) return getStringLeftAlign(true);
		String res=getStringLeftAlign(true);
		res=res.replaceAll("0*$", "");
		if(Config.DECIMALCHAR == '.')
			res=res.replaceAll("\\.$", "");
		else
			res=res.replaceAll(Config.DECIMALCHAR + "$", "");
		return res;
	}
	/**
	 * returns the value of the Var as an String. 
	 * 
	 * Strips leading zeros and returns a space if zero
	 * @return the String result or "" if the Var is not assigned.
	 */
	public String getStringLeftAlign() {
		return getStringLeftAlign(false);
	}

	protected String formatThousands(String str, String separator, boolean dec) {
		if(str == null) return str;
		StringBuilder sb = new StringBuilder();
		int idx;
		if(separator == null) {
			if(dec && Config.DECIMALCHAR != '.') {
				idx = str.indexOf('.');
				if(idx == -1) return str;
				sb.append(str.substring(0, idx));
				sb.append(Config.DECIMALCHAR);
				sb.append(str.substring(idx+1));
				return sb.toString();
			} 
			return str;
		}
		if(dec) {
			idx = str.indexOf('.');
			if(idx != -1) sb.append(str.substring(0, idx));
			else sb.append(str);
		} else {
			sb.append(str);
			idx = -1;
		}
		if(sb.length() > 3) { //123456
			for(int i=sb.length() - 3; i > 0; i-=3) {
				if(i <= 0) break;
				sb.insert(i, separator);
			}
		}
		if(dec && idx != -1) {
			char point;
			if(separator != null && separator.length() > 0 && separator.equals(".") &&
					Config.DECIMALCHAR == '.')
				point = ',';
			else point = Config.DECIMALCHAR;
			sb.append(point);
			sb.append(str.substring(idx+1));
		}
		return sb.toString();
	}
	
	public String getFmtString(String separator) {
		boolean negval = false;
		String str;
		String ex = "";
		String px = "";
		int len = this.displayLength;
		String occodes=getOccodes();
		
		if(separator != null && separator.length() > 0)
			len += (((int)((len - scale - 1)/3)) * separator.length());

		if((attr & (CHAR|PICTURE)) != 0) return occodes + getString(true);
	    if ((attr & (LONG)) != 0) {
	    	long lv = getLongValue();
	    	if(lv == 0 && (attr & (ZEROBLANK)) != 0) {
	    		str = "";
	    	} else if((attr & (DATE)) != 0) {
	    		str = DC.formatDate((int)lv);
	    		len = str.length();
	    	} else {
	    		if(lv < 0) negval = true;
	    		if((attr & (PIP)) != 0 && lv == 0) str = "";
	    		else str = Long.toString(Math.abs(lv));
	    		str = formatThousands(str, separator, false);
	    	}
	    } else if((attr & (DOUBLE)) != 0) {
	    	double dv = getDoubleValue();
	        if(dv < 0.0) negval = true;
			if((attr & (PIP)) != 0 && dv == 0) str = "";
			else {
		        BigDecimal bd = new BigDecimal(f_chop(Math.abs(dv),len,scale)).setScale(scale, BigDecimal.ROUND_HALF_DOWN);
		        if((attr & (ZEROBLANK)) != 0 && bd.equals(new BigDecimal(0).setScale(scale)))
		        	str = "";
		        else {
		        	str = bd.toPlainString();
		        	if(str.length() > 1 && str.startsWith("0.") && (attr & (ZEROBLANK)) != 0) //For type X
		        	   str=str.substring(1);
		        	else
		        	   str = formatThousands(str, separator, true);
		        }
			}
		} else str = "???????????";
		if ((attr & (UNSIGNED)) == 0) {

			if ((attr & (ZEROBLANK)) == 0 || str != "")
				if ((attr & (NEGDR)) != 0) {
					if (negval)
						ex = "DR";
					else
						ex = "CR";
				} else if ((attr & (NEGCR)) != 0) {
					if (negval)
						ex = "CR";
					else
						ex = "";
				} else if ((attr & (EDITPLUS)) != 0 && !negval)
					ex = "+";
				else if ((attr & (LEADING_SIGN)) != 0) {
					if (negval)
						px = "-";
					else
						px = " ";
				} else if (negval)
					ex = "-";
				else
					ex = " ";
			else
				ex = " ";
		}
	    if((attr & (DOLLAR)) != 0) {
	        len++;
	        //Dont see wht PIP has to do with it
		    //if((attr & (PIP)) == 0 || str.length() > 0) str = "$" + str;
		    if(str.length() > 0) str = "$" + str;
			if((attr & (ZEROFILL)) != 0) this.attr &= ~(ZEROFILL);
		}
	    if(px.length() > 0) {
	    	len++;
	    	if(negval) str = px + str;
	    }
		if(scale > 0) len++;
		if(str.length() > len) str = str.substring(str.length() - len);
		else if(str.length() < len) {
			if((attr & (PIP)) != 0 && str.length() == 0) {
			    str = spaces.substring(0, len - str.length()) + str;
				ex = spaces.substring(0,ex.length());
			} else if((attr & (ZEROBLANK)) == 0 && (attr & (ZEROFILL)) != 0) 
				str = zeros.substring(0, len - str.length()) + str;
		    else if((attr & (ZEROBLANK)) == 0 && (attr & (STARFILL)) != 0) 
		        str = stars.substring(0, len - str.length()) + str;
			else 
			    str = spaces.substring(0, len - str.length()) + str;
		}
		
		return occodes + str + ex;
	}
	private String getOccodes () {
		String s="";
		String outputOccodes = Config.getProperty("output.control.codes");
		if(outputOccodes == null || !outputOccodes.equals("Y")) return s;
		if(ocCodes == null) return s;
		for (int i = 0; i < ocCodes.length; i++){
			if(ocCodes[i].equals("BRIGHT")) s+=BRIGHT;
			else if(ocCodes[i].equals("LANDSCAPE")) s+=LANDSCAPE; 
			else if(ocCodes[i].equals("LINESPERINCH6")) s+=LINESPERINCH6;
			else if(ocCodes[i].equals("LINESPERINCH8")) s+=LINESPERINCH8;
			else if(ocCodes[i].equals("MEDIUMWEIGHT")) s+=MEDIUMWEIGHT;
			else if(ocCodes[i].equals("TOPMARGIN2")) s+=TOPMARGIN2;
			else if(ocCodes[i].equals("UNDERLINE")) s+=UNDERLINE;
			else if(ocCodes[i].equals("UNDERLINEOFF")) s+=UNDERLINEOFF;
			else if(ocCodes[i].equals("BRIGHTOFF")) s+=BRIGHTOFF;
		}
		return s;
	}

	/*
	private StringBuilder getStringBuilder() {
		return (StringBuilder)value;
	}
	*/

	/**
	 * returns the value of the Var as an String.
	 * 
	 * @return the String result or "" if the Var is not assigned.
	 */
	public String getString() { //overridden in Group
		return getString(true);
	}
	
	/**
	 * returns the value of the AUTOVAR as an String.
	 * 
	 * @return the String result or "" if the Var is not assigned.
	 */
	public String getString(int type, int l, int d) throws IllegalStateException {
		if((attr & (AUTOVAR)) == 0) 
			throw new IllegalStateException("Cannot call getString(int,int,int) on non-auto Var.");	
		if((attr & (NUMERIC)) != 0 && displayLength == 0) return Util.zeroes(l);
		if(l > displayLength) l = displayLength;
		if(d > 0 && l > d) l -= d - 1 ;
		String s;
		 
		StringBuilder sv = (StringBuilder)value;
		if(d > 0) s = sv.substring(0,l) + "." + sv.substring(0,d);
		else s = sv.substring(0,l);
		if((type & (DOUBLE|LONG)) != 0) 
			s = s.replaceAll(sv.substring(0,1), Long.toString((long) Math.pow(10, exp)));
		return s;
	}
	
	/**
	 * returns the value of the AUTOVAR as an Var.
	 * @param type
	 *            var type
	 * @param l
	 *            length of the item
	 * @param d
	 *            number of decimal places (where appropriate)
	 * 
	 * @return the Var result or "" if the Var is not assigned.
	 */
	public Var toVar(int type, int l, int d) {
		if((attr & (AUTOVAR)) == 0) return new Var(0); // throw new Exception("Cannot call getString(int,int) on non-auto Var.");	
		if(l > displayLength && displayLength > 0) l = displayLength;
		if(d > 0 && l > d) l -= d - 1 ;
		Var v = new Var(type,l,d);

		StringBuilder sv = (StringBuilder)value;
		if(displayLength == 0 && (attr & (NUMERIC)) != 0) v.set(Util.zeroes(l));
		else if(d > 0) v.set(sv.substring(0,l) + "." + sv.substring(0,d));
		else v.set(sv.substring(0,l));
		return v;
	}
	/**
	 * returns the value of the AUTOVAR as an Var.
	 * @param type
	 *            var type
	 * @param l
	 *            length of the item
	 * 
	 * @return the Var result or "" if the Var is not assigned.
	 */
	public Var toVar(int type, int l) {
		return toVar(type,l,0);
	}
	/**
	 * Returns numeric values in LINC form. That is, implied decimal and
	 * sign overstamp. String values will be returned normally. Leading
	 * zeroes are retained.
	 * @return number as a string
	 */
	public String getLincString() {
		return getLincString(true);
	}
	
	/**
	 * Returns numeric values in LINC form. That is, implied decimal and
	 * sign overstamp. String values will be returned normally. Leading
	 * zeroes are retained depending on rz.
	 * @param rz If true, retain leading zeros or else discard them.
	 * @return number as a string
	 */
	public String getLincString(boolean rz) {
		if((attr & (CHAR|GROUP|PICTURE)) != 0) return getString(rz);
		else if((attr & (DOUBLE)) != 0) return doubleToLincString(getDoubleValue(), rz);
		else if((attr & (LONG)) != 0) return longToLincString(getLongValue(), rz);
		else return "";
	}

	public String getLincAbsString(boolean rz) {
		if((attr & (CHAR|GROUP|PICTURE)) != 0) return getString(rz);
		else if((attr & (DOUBLE)) != 0) return doubleToLincString(Math.abs(getDoubleValue()), rz);
		else if((attr & (LONG)) != 0) return longToLincString(Math.abs(getLongValue()), rz);
		else return "";
	}

	/**
	 * returns a String representation of Var in its shortest form, ie with no
	 * trailing spaces.
	 * 
	 * @return the shortest String possible.
	 */
	public String getTrim() {//NOT overridden in Group
	    if ((attr & (GROUP)) != 0) {
			//return this.getString().replaceAll("\\s+$", "");
			return this.getString().replaceAll(" +$", "");
	    } else if ((attr & (CHAR|PICTURE)) != 0) {
			StringBuilder sv = (StringBuilder)value;
	    	if(sv == null || (attr & (ISASSIGNED)) == 0)
				return ""; 
			else if (sv.length() > displayLength)
				return sv.substring(0, displayLength);
			else
				return rtrim();
	    } else if ((attr & (LONG)) != 0)
			return Long.toString(getLong());
		else if ((attr & (DOUBLE)) != 0)
			return Double.toString(getDouble());
		else
			return "Unknown datatype " + attr;
	}

	/**
	 * Checks for equality between the this and the parameter.
	 * Note that for comparison between CHAR and NUMBER types, 0 is regarded as 
	 * equal to spaces. To change this behaviour, set "jclib.space.zero.equal = false"
	 * in the properties file.
	 * 
	 * @param v The Var type to compare to
	 * @return true if they are equal otherwise false.
	 */
	public boolean eq(Var v) {
		return compareTo(v) == 0;
	}

	/**
	 * Checks for equality between the Var and the parameter. See eq()
	 * 
	 * @param v
	 * @return true if they are same otherwise false.
	 */
	public boolean equals(Var v) {
		return compareTo(v) == 0;
	}

	/**
	 * Checks for equality between the Var and the parameter. Trailing spaces
	 * are removed before the comparison is made.
	 * Note that for comparison between CHAR and NUMBER types, 0 is regarded as 
	 * equal to spaces. To change this behaviour, set "jclib.space.zero.equal = false"
	 * in the properties file.
	 * 
	 * @param s
	 * @return true if they are same otherwise false.
	 */
	public boolean eq(String s) {
		return compareTo(s) == 0;
	}

	/**
	 * Checks for equality between the Var and the parameter. Trailing spaces
	 * are removed before the comparison is made.
	 * 
	 * @param s
	 * @return true if they are same otherwise false.
	 */
	public boolean equals(String s) {
		return compareTo(s) == 0;
	}

	/**
	 * Checks for equality between the Var and the parameter.
	 * Note that for comparison between CHAR and NUMBER types, 0 is regarded as 
	 * equal to spaces. To change this behaviour, set "jclib.space.zero.equal = false"
	 * in the properties file.
	 * 
	 * @param l
	 * @return true if they are same otherwise false.
	 */
	public boolean eq(long l) {
		return compareTo(l) == 0;
	}

	/**
	 * Checks for equality between the Var and the parameter.
	 * 
	 * @param l
	 * @return true if they are same otherwise false.
	 */
	public boolean equals(long l) {
		return compareTo(l) == 0;
	}

	/**
	 * Checks for equality between the Var and the parameter.
	 * Note that for comparison between CHAR and NUMBER types, 0 is regarded as 
	 * equal to spaces. To change this behaviour, set "jclib.space.zero.equal = false"
	 * in the properties file.
	 * 
	 * @param d
	 * @return true if they are same otherwise false.
	 */
	public boolean eq(double d) {
		return compareTo(d) == 0;
	}

	/**
	 * Checks for equality between the Var and the parameter.
	 * Note that for comparison between CHAR and NUMBER types, 0 is regarded as 
	 * equal to spaces. To change this behaviour, set "jclib.space.zero.equal = false"
	 * in the properties file.
	 * 
	 * @param d
	 * @return true if they are same otherwise false.
	 */
	public boolean equals(double d) {
		return compareTo(d) == 0;
	}

	/**
	 * Checks for inequality between the Var and the parameter.
	 * Note that for comparison between CHAR and NUMBER types, 0 is regarded as 
	 * equal to spaces. To change this behaviour, set "jclib.space.zero.equal = false"
	 * in the properties file.
	 * 
	 * @param v
	 * @return true if they are different otherwise false.
	 */
	public boolean ne(Var v) {
		return compareTo(v) != 0;
	}

	/**
	 * Checks for inequality between the Var and the parameter.
	 * Note that for comparison between CHAR and NUMBER types, 0 is regarded as 
	 * equal to spaces. To change this behaviour, set "jclib.space.zero.equal = false"
	 * in the properties file.
	 * 
	 * @param s
	 * @return true if they are different otherwise false.
	 */
	public boolean ne(String s) {
		return compareTo(s) != 0;
	}

	/**
	 * Checks for inequality between the Var and the parameter.
	 * Note that for comparison between CHAR and NUMBER types, 0 is regarded as 
	 * equal to spaces. To change this behaviour, set "jclib.space.zero.equal = false"
	 * in the properties file.
	 * 
	 * @param s
	 * @return true if they are different otherwise false.
	 */
	public boolean ne(long s) {
		return compareTo(s) != 0;
	}

	/**
	 * Checks for inequality between the Var and the parameter.
	 * Note that for comparison between CHAR and NUMBER types, 0 is regarded as 
	 * equal to spaces. To change this behaviour, set "jclib.space.zero.equal = false"
	 * in the properties file.
	 * 
	 * @param s
	 * @return true if they are different otherwise false.
	 */
	public boolean ne(double d) {
		return compareTo(d) != 0;
	}
	/**
	 * Check for equality with string s repeated
	 * @param s
	 * @return true if they are equal otherwise false.
	 */
	public boolean eqAll(String s) {
		StringBuilder sb = new StringBuilder(displayLength);
		for (int len=0;len < displayLength;len+=s.length()) 
			sb.append(s);
		return compareTo(sb.substring(0,displayLength)) == 0;
	}
	/**
	 * Check for inequality with string s repeated
	 * @param s
	 * @return true if they are not equal otherwise false.
	 */
	public boolean neAll(String s) {
		StringBuilder sb = new StringBuilder(displayLength);
		for (int len=0;len < displayLength;len+=s.length()) 
			sb.append(s);
		return compareTo(sb.substring(0,displayLength)) != 0;
	}
	/**
	 * Check for equality with Var v repeated
	 * @param v
	 * @return true if they are equal otherwise false.
	 */
	public boolean eqAll(Var v) {
		StringBuilder sb = new StringBuilder(displayLength);
		for (int len=0;len < displayLength;len+=v.length()) 
			sb.append(v.getString());
		return compareTo(sb.substring(0,displayLength)) == 0;
	}
	/**
	 * Check for inequality with var s repeated
	 * @param v
	 * @return true if they are not equal otherwise false.
	 */
	public boolean neAll(Var v) {
		StringBuilder sb = new StringBuilder(displayLength);
		for (int len=0;len < displayLength;len+=v.length()) 
			sb.append(v.getString());
		return compareTo(sb.substring(0,displayLength)) != 0;
	}

	/**
	 * gives the char at offset offs in the Var.
	 * 
	 * @param offs
	 *            the location of the char required
	 * @return the char.
	 */
	public char getChar(int offs) {
		if ((attr & (CHAR|PICTURE)) != 0) {
			StringBuilder sv = (StringBuilder)value;
		    if(sv == null || sv.length() < (offs+1)) return '\u0000';
			return sv.charAt(offs);
		}
		return getString().charAt(offs);
	}

	/**
	 * compares this to the parameter given, returning true if this is larger.
	 * 
	 * @param v
	 *            for comparison
	 * @return true if this is greater otherwise false
	 */
	public boolean gt(Var v) {
		return compareTo(v) > 0;
	}

	/**
	 * compares this to the parameter given, returning true if this is larger.
	 * 
	 * @param v
	 *            for comparison
	 * @return true if this is greater otherwise false
	 */
	public boolean gt(String v) {
		return compareTo(v) > 0;
	}

	/**
	 * compares this to the parameter given, returning true if this is larger.
	 * 
	 * @param i
	 *            for comparison
	 * @return true if this is greater otherwise false
	 */
	public boolean gt(long i) {
		return compareTo(i) > 0;
	}

	/**
	 * compares this to the parameter given, returning true if this is larger.
	 * 
	 * @param d
	 *            for comparison
	 * @return true if this is greater otherwise false
	 */
	public boolean gt(double d) {
		return compareTo(d) > 0;
	}

	/**
	 * compares this to the parameter given, returning true if this is larger or
	 * equal.
	 * 
	 * @param v
	 *            for comparison
	 * @return true if this is greater or equal, otherwise false
	 */
	public boolean ge(Var v) {
		return compareTo(v) >= 0;
	}

	/**
	 * compares this to the parameter given, returning true if this is larger or
	 * equal.
	 * 
	 * @param i
	 *            for comparison
	 * @return true if this is greater or equal, otherwise false
	 */
	public boolean ge(long i) {
		return compareTo(i) >= 0;
	}

	/**
	 * compares this to the parameter given, returning true if this is larger or
	 * equal.
	 * 
	 * @param d
	 *            for comparison
	 * @return true if this is greater or equal, otherwise false
	 */
	public boolean ge(double d) {
		return compareTo(d) >= 0;
	}

	/**
	 * compares this to the parameter given, returning true if this is larger or
	 * equal.
	 * 
	 * @param v
	 *            for comparison
	 * @return true if this is greater or equal, otherwise false
	 */
	public boolean ge(String v) {
		return compareTo(v) >= 0;
	}

	/**
	 * compares this to the parameter given, returning true if this is smaller.
	 * 
	 * @param v
	 *            for comparison
	 * @return true if this is smaller, otherwise false
	 */
	public boolean lt(Var v) {
		return compareTo(v) < 0;
	}

	/**
	 * compares this to the parameter given, returning true if this is smaller.
	 * 
	 * @param v
	 *            for comparison
	 * @return true if this is smaller, otherwise false
	 */
	public boolean lt(String v) {
		return this.getString().compareTo(v) < 0;
	}

	/**
	 * compares this to the parameter given, returning true if this is smaller.
	 * 
	 * @param i
	 *            for comparison
	 * @return true if this is smaller, otherwise false
	 */
	public boolean lt(long i) {
		if((attr & (DOUBLE)) != 0) return lt((double)i);
		return getLong() < i;
	}

	/**
	 * compares this to the parameter given, returning true if this is smaller.
	 * 
	 * @param i
	 *            for comparison
	 * @return true if this is smaller, otherwise false
	 */
	public boolean lt(double i) {
		return getDouble() < i;
	}

	/**
	 * compares this to the parameter given, returning true if this is smaller
	 * or equal.
	 * 
	 * @param v
	 *            for comparison
	 * @return true if this is smaller or equal, otherwise false
	 */
	public boolean le(Var v) {
		if((v.attr & (AUTOVAR)) != 0 && v.displayLength == 0) {
			if((attr & (PICTURE)) != 0)
				return this.getString().compareTo(v.getString(CHAR,displayLength,0)) <= 0;
			else if ((attr & (DOUBLE)) != 0) {
				return this.getDouble() <= 0.0;
			} else if ((attr & (NUMERIC)) != 0) {
				return this.getLong() <= 0;
			} else
				return this.getString().compareTo(Util.zeroes(displayLength)) <= 0;
		}
		if((attr & (PICTURE)) != 0)
			return this.getString().compareTo(v.getString()) <= 0;
		else if ((attr & (DOUBLE)) != 0 || (v.attr & (DOUBLE)) != 0) {
			return this.getDouble() <= v.getDouble();
		} else if ((attr & (NUMERIC)) != 0 || (v.attr & (NUMERIC)) != 0) {
			return this.getLong() <= v.getLong();
		} else
			return this.getString().compareTo(v.getString()) <= 0;
	}

	/**
	 * compares this to the parameter given, returning true if this is smaller
	 * or equal.
	 * 
	 * @param i
	 *            for comparison
	 * @return true if this is smaller or equal, otherwise false
	 */
	public boolean le(long i) {
		if((attr & (DOUBLE)) != 0) return le((double)i);
		return getLong() <= i;
	}

	/**
	 * compares this to the parameter given, returning true if this is smaller
	 * or equal.
	 * 
	 * @param i
	 *            for comparison
	 * @return true if this is smaller or equal, otherwise false
	 */
	public boolean le(double i) {
		return getDouble() <= i;
	}

	/**
	 * compares this to the parameter given, returning true if this is smaller
	 * or equal.
	 * 
	 * @param v
	 *            for comparison
	 * @return true if this is smaller or equal, otherwise false
	 */
	public boolean le(String v) {
		return this.getString().compareTo(v) <= 0;
	}

	public int compareTo(String c) {
		//Get the pointer/datatype/offsets etc for c

		if(value == null) {
			if(c == null || c.trim().length() == 0) return 0;
			return -1;
		}
		StringBuilder x;
		if((attr & (LONG|DOUBLE)) != 0) x = new StringBuilder(getString());
		else x = (StringBuilder)value;
		int rv;
		int l = x.length();
		if(l < c.length()) {
			rv = x.substring(0, l).compareTo(c.substring(0, l));
			if(rv == 0) {
				if(c.substring(l).trim().length() == 0) return 0;
				return -1;
			}
			return rv;
		} else {
			rv = x.substring(0, c.length()).compareTo(c);
			if(rv == 0 && l > c.length()) {
				if(x.substring(c.length(), l).trim().length() == 0) return 0;
				return 1;
			}
			return rv;
		}
	}
	
	public int compareTo(long v) {
		if((attr & (DOUBLE)) != 0 || scale > 0) {
//			double d = getDouble(displayLength, scale);
			double d = getDouble();
			if(d < (double)v) return -1;
			else if((double)d > v) return 1;
			else return 0;
		} else if((attr & (CHAR)) != 0) {
			return compareTo(Long.toString(v));
		}
		long l = getLong(displayLength);
		if(l < v) return -1;
		else if(l > v) return 1;
		else return 0;
	}
	
	public int compareTo(double v) {
		if((attr & (LONG)) != 0) {
			double d = getDouble(displayLength, 0);
			if(d < v) return -1;
			else if(d > v) return 1;
			else return 0;
		} else if((attr & (CHAR)) != 0) {
			return compareTo(Double.toString(v));
		}
		double dd = getDouble();
		double vv = f_chop(v, displayLength, scale);
		if(dd < vv) return -1;
		else if(dd > vv) return 1;
		else return 0;
	}

	@SuppressWarnings("unused")
	private StringBuilder getMyStringBuilder() {
		if(value == null) {
			if((attr & (CHAR)) == 0) return null;
			value = new StringBuilder(spaces.substring(0, displayLength));
		}
		return (StringBuilder)value;
	}

	public int compareTo(Var v) {
		long base;
		if((attr & (PICTURE|CHAR)) != 0 || (v.attr & (PICTURE)) != 0) base = CHAR;
		else if((attr & (DOUBLE)) != 0 || (v.attr & (DOUBLE)) != 0 || ((attr & (NUMERIC)) != 0 && scale > 0) || ((v.attr & (NUMERIC)) != 0 && v.scale > 0) ) base = DOUBLE;
		else if((attr & (LONG|NUMERIC)) != 0 || (v.attr & (LONG|NUMERIC)) != 0) base = LONG;
		else base = CHAR;
		
		if(base == DOUBLE) {
			double dd = getDouble();
			double vv;
			if(Config.COMPARE_SPACEZERO_NOT_EQUAL && (v.attr & (CHAR)) != 0) {
				String x = v.getString().trim();
				if(x.length() == 0 && eq(0.0)) return 1;
			}
			if((v.attr & (AUTOVAR)) != 0) vv = v.getDouble();
			else vv = v.getDouble(v.displayLength, v.scale);
			if(dd < vv) return -1;
			else if(dd > vv) return 1;
			else return 0;
		} else if(base == LONG) {
			long ll = getLong(displayLength);
			long vv;
			if((v.attr & (AUTOVAR)) != 0) vv = v.getLong();
			else vv = v.getLong(v.displayLength);
			if(Config.COMPARE_SPACEZERO_NOT_EQUAL && (v.attr & (CHAR)) != 0) {
				String x = v.getString().trim();
				if(x.length() == 0 && vv == 0) return 1;
			}
			if(ll < vv) return -1;
			else if(ll > vv) return 1;
			else return 0;
		} else {
			if((v.attr & (AUTOVAR)) != 0) return compareTo(v.getString(CHAR, displayLength, 0));
			else if(!Config.COMPARE_SPACEZERO_NOT_EQUAL && (v.attr & (NUMERIC)) != 0) {
				String x = getString().trim();
				if(x.length() == 0) {
					if(v.eq(0)) return 0;
				}
			} 
			return compareTo(v.getString());
		} 
	}
	
	
	/**
	 * appends a String to the Var. This is implementing the LINC ATTACH or
	 * ATTACH&amp;SPACE verbs.
	 * 
	 * @param var
	 *            the String containing the text to append
	 * @return the Var with the joined strings.
	 */
	public Var append(String var) {
		Var ret = new Var(Var.NUMERIC, 4);
		ret.set(this.getTrim().length() + var.trim().length());
		set(this.getTrim() + var);
		return ret;
	}

	/**
	 * appends a Var to the Var. This is implementing the LINC ATTACH or
	 * ATTACH&amp;SPACE verbs.
	 * 
	 * @param var
	 *            the Var containing the text to append
	 * @return the Var with the joined strings.
	 */
	public Var append(Var var) {
		Var ret = new Var(Var.NUMERIC, 4);
		ret.set(this.getTrim().length() + var.getTrim().length());
		set(this.getTrim() + var.getString());
		return ret;
	}

	/**
	 * appends a String and a Var to the Var. This is implementing the LINC
	 * ATTACH or ATTACH&amp;SPACE verbs.
	 * 
	 * @param v1
	 *            the string to append
	 * @param v2
	 *            the Var to append
	 * @return the Var with the joined strings returnVar = this + v1 + v2
	 */
	public Var append(String v1, Var v2) {
		Var ret = new Var(Var.NUMERIC, 4);
		ret.set(this.getTrim().length()
				+ (v1.trim().length() != 0 ? v1.trim().length() : 1)
				+ v2.getTrim().length());
		set(this.getTrim() + (v1.trim().length() != 0 ? v1.trim() : " ")
				+ v2.getString());
		return ret;
	}

	/**
	 * appends two Vars to the Var. This is implementing the LINC ATTACH or
	 * ATTACH&amp;SPACE verbs.
	 * 
	 * @param v1
	 *            the first Var to append
	 * @param v2
	 *            the second Var to append
	 * @return the Var with the joined strings returnVar = this + v1 + v2
	 */
	public Var append(Var v1, Var v2) {
		Var ret = new Var(Var.NUMERIC, 4);
		ret.set(this.getTrim().length()
				+ (v1.getTrim().length() != 0 ? v1.getTrim().length() : 1)
				+ v2.getTrim().length());
		set(this.getTrim() + (v1.getTrim().length() != 0 ? v1.getTrim() : " ")
				+ v2.getString());
		return ret;
	}

	/**
	 * appends two Strings to the Var. This is implementing the LINC ATTACH or
	 * ATTACH&amp;SPACE verbs.
	 * 
	 * @param v1
	 *            the first string to append
	 * @param v2
	 *            the second string to append
	 * @return the Var with the joined strings returnVar = this + v1 + v2
	 */
	public Var append(String v1, String v2) {
		Var ret = new Var(Var.NUMERIC, 4);
		ret.set(this.getTrim().length()
				+ (v1.trim().length() != 0 ? v1.trim().length() : 1)
				+ v2.trim().length());
		set(this.getTrim() + (v1.trim().length() != 0 ? v1.trim() : " ") + v2);
		return ret;
	}

	/**
	 * appends a Var and a String to the Var. This is implementing the LINC
	 * ATTACH or ATTACH&amp;SPACE verbs.
	 * 
	 * @param v1
	 *            the Var to append
	 * @param v2
	 *            the string to append
	 * @return the Var with the joined strings returnVar = this + v1 + v2
	 */
	public Var append(Var v1, String v2) {
		Var ret = new Var(Var.NUMERIC, 4);
		ret.set(this.getTrim().length()
				+ (v1.getTrim().length() != 0 ? v1.getTrim().length() : 1)
				+ v2.trim().length());
		set(this.getTrim() + (v1.getTrim().length() != 0 ? v1.getTrim() : " ")
				+ v2);
		return ret;
	}

	/**
	 * Gives the substring from a starting location for a length.
	 * 
	 * @param start
	 *            the starting point, 0 relative
	 * @param len
	 *            the length
	 * @return the resulting string or "" 
	 */
	public String substr(int start, long len) {
		return substr(start, (int)len);
	}

	/**
	 * Returns the substring of the variable, beginning with 'start' until the end of
	 * the string. If this variable is numeric with decimals, but was created as a 
	 * result of an exX.getVar(fnn.varname) then the returned string will NOT contain
	 * a decimal point.
	 * @param start The starting position for the substring, 0 relative.
	 * @returns the substring as a string. If start is greater than the end-of-string, returns ""
	 */
	public String substr(int start) {
		return substr(start, this.displayLength - start);
	}
	
	public String substr(int start, int len) {
		if((attr & (AUTOVAR)) != 0 && this.displayLength == 0) return zeros.substring(0,len);
		if(start >= this.displayLength) return "";
		if(start + len >= this.displayLength) len = this.displayLength - start;
		return this.getLincString().substring(start, start + (int)len);
	}
	public String substr(int start, Var len) {
		return(substr(start, len.getInt()));
	}
	public String substr(Var start, long len) {
	    return(substr(start.getInt(), len));
	}
	public String substr(Var start, int len) {
	    return(substr(start.getInt(), (long)len));
	}
	public String substr(Var start, Var len) {
	    return(substr(start.getInt(), len.getInt()));
	}

	/**
	 * Returns the substring of the variable, beginning with 'start' until the end of
	 * the string. If this variable is numeric with decimals, but was created as a 
	 * result of an exX.getVar(fnn.varname) then the returned string will NOT contain
	 * a decimal point.
	 * @param start The starting position for the substring, 0 relative.
	 * @returns the substring as a Var. If start is greater than the end-of-string, returns ""
	 */
	public Var substring(int start) {
		return substring(start, this.displayLength - start);
	}
	
	public Var substring(int start, int len) {
		if((attr & (AUTOVAR)) != 0 && this.displayLength == 0) return new Var(zeros.substring(0,len));
		if(start >= this.displayLength) return new Var("");
		if(start + len >= this.displayLength) len = this.displayLength - start;
		return new Var(this.getLincString().substring(start, start + (int)len));
	}
	public Var substring(int start, Var len) {
		return(substring(start, len.getInt()));
	}
	public Var substring(Var start, long len) {
	    return(substring(start.getInt(), (int)len));
	}
	public Var substring(Var start, int len) {
	    return(substring(start.getInt(), len));
	}
	public Var substring(Var start, Var len) {
	    return(substring(start.getInt(), len.getInt()));
	}
	
	/**
	 * Cobol version of substring (1 relative)
	 * Returns the substring of the variable, beginning with 'start' until the end of
	 * the string. If this variable is numeric with decimals, but was created as a 
	 * result of an exX.getVar(fnn.varname) then the returned string will NOT contain
	 * a decimal point.
	 * @param start The starting position for the substring, 1 relative.
	 * @returns the substring as a Var. If start is greater than the end-of-string, returns ""
	 */
	public Var refMod(int start) {
		return refMod(start, this.displayLength - start);
	}
	
	public Var refMod(int start, int len) {
		start--;
		if((attr & (AUTOVAR)) != 0 && this.displayLength == 0) return new Var(zeros.substring(0,len));
		if(start >= this.displayLength) return new Var("");
		if(start + len >= this.displayLength) len = this.displayLength - start;
		return new Var(this.getLincString().substring(start, start + (int)len));
	}
	public Var refMod(int start, Var len) {
		return(refMod(start, len.getInt()));
	}
	public Var refMod(Var start, long len) {
	    return(refMod(start.getInt(), (int)len));
	}
	public Var refMod(Var start, int len) {
	    return(refMod(start.getInt(), len));
	}
	public Var refMod(Var start, Var len) {
	    return(refMod(start.getInt(), len.getInt()));
	}


	/**
	 * Changes the sub string from start with src.
	 * 
	 * @param src
	 *            the string to use as the replacement
	 * @param start
	 *            the location at which to replace it. start is 1 relative.
	 *            If start < 1, start will be set to 1.
	 */
	public void setSubstr(String src, int start) 
		throws ArrayIndexOutOfBoundsException { //Overridden in GROUP
		if(src.length() < 1 || start > displayLength) 
			//No linc doesnt throw Exceptions!!
			//throw new ArrayIndexOutOfBoundsException("Out of bounds index " + start);
			return;
		start--; if(start < 0) start = 0; //make it zero relative
		attr |= ISASSIGNED;
	    StringBuilder sv;
		if((attr & (NUMERIC)) != 0) {
		    sv = new StringBuilder();
			sv.append(this.getString());
			if(scale > 0) {
				if (start > displayLength - scale) start++;
			}
		} else {
			if(value == null) {
				sv = new StringBuilder();
				value = sv;
			} else {
				sv = (StringBuilder)value;
			}
		}
		//At this stage sv is either temporary, or points to permanent storage.		
		if((start + src.length()) > (displayLength - scale)) src = src.substring(0, displayLength - start);
	    if(sv.length() > displayLength) sv.delete(displayLength, sv.length());
		else if(sv.length() < displayLength) sv.append(makeSpaces(displayLength - sv.length()));
		
		sv.replace(start, start + src.length(), src);
	    if((attr & (NUMERIC)) != 0) {
	    	if((attr & (GROUP|CHAR)) == 0) {
	    		if(scale > 0) sv.insert(sv.length() - scale, ".");
	    		this.set(sv.toString());
	    	}
	    	else {
	    		value = sv;
	    	}
	    }
		sv= null;
		return;
	}
	
	public final void setSubstr(Var src, Var start) throws ArrayIndexOutOfBoundsException 
		{setSubstr(src.getLincString(), start.getInt());}
	public final void setSubstr(String src, Var start) throws ArrayIndexOutOfBoundsException 
		{setSubstr(src, start.getInt());}
	public final void setSubstr(Var src, int start) throws ArrayIndexOutOfBoundsException 
		{setSubstr(src.getLincString(), start);}
	public final void setSubstr(int src, Var start) throws ArrayIndexOutOfBoundsException 
		{setSubstr(Integer.toString(src), start.getInt());}
	public final void setSubstr(int src, int start) throws ArrayIndexOutOfBoundsException 
		{setSubstr(Integer.toString(src), start);}
	public final void setSubstr(Var src, Var start, int len) throws ArrayIndexOutOfBoundsException 
		{setSubstr(src.getLincString().substring(0,len), start.getInt());}
	public final void setSubstr(Var src, Var start, Var len) throws ArrayIndexOutOfBoundsException 
		{setSubstr(src.getLincString().substring(0,len.getInt()), start.getInt());}
	public final void setSubstr(Var src, int start, int len) throws ArrayIndexOutOfBoundsException 
		{setSubstr(src.getLincString().substring(0,len), start);}
	public final void setSubstr(String src, Var start, int len) throws ArrayIndexOutOfBoundsException 
	{setSubstr(src, start.getInt(), len);}
	
	public final void setSubstr(String src, int start, int len) throws ArrayIndexOutOfBoundsException	{
		if(src=="") src=" ";
		setSubstr(src.substring(0,len), start);
	}
	public final void setSubstr(String src, Var start, Var len) throws ArrayIndexOutOfBoundsException	{
		if(src=="") src=" ";
		setSubstr(src.substring(0,len.getInt()), start.getInt());
	}
	
	/*
	private int getRealOffset(int offset) { //Only sensible in groups
		return offset;
	}
	*/
	
	public int getMyOffset() {return 0;}

	protected Var getRealVar() {
		return this;
	}

	public void insertAt(int start, String s) throws IllegalAccessException {
		//Get actual stringbuilder and offset in case of group type
		if(start > displayLength) return;
		start += getMyOffset();
		Var target = getRealVar();
		
		if((target.attr & (CHAR)) == 0) 
			throw new IllegalAccessException("Target Var is not CHAR type");
		
		StringBuilder sv = (StringBuilder)target.value;
		if(sv == null) sv = new StringBuilder();
		StringBuilder v = sv;
		if(v.length() < target.displayLength) 
			v.append(new Var(CHAR, target.displayLength - v.length()).getString());
		
		if(start + s.length() > this.displayLength) s = s.substring(0, this.displayLength - start); 
		v.insert(start, s);
		
	}

	public void insertSorted(String s, int len, int order) throws IllegalAccessException {
		if(s.length() > len) s = s.substring(0, len);
		else if(s.length() < len) s = adjustLengthRight(s, len);
		
		boolean inserted = false;
		for(int i=0; (i+len) < this.displayLength; i += len) {
			int dif;
			if(equals(" ")) dif = 0;
			else dif = s.compareTo(this.substr(i, i + len));
			if(order == DESCENDING) dif = -dif;
			if(dif <= 0) { //this is our insertion point
				insertAt(i, s);
				inserted = true;
				break;
			}
		}
		if(!inserted) {
			//set glb.status
		}
	}
	public void insertSorted(Var s, int len, int order)  throws IllegalAccessException { 
		insertSorted(s.getString(), len, order); 
	}
	public void insertSorted(Var s, Var len, int order)  throws IllegalAccessException { 
		insertSorted(s.getString(), len.getInt(), order); 
	}
	public void insertSorted(String s, Var len, int order)  throws IllegalAccessException { 
		insertSorted(s, len.getInt(), order); 
	}

	public void upshift() { //Overridden in Group
		if((attr & (CHAR|GROUP)) == 0) return;
		if(value == null) return;
		StringBuilder sv = (StringBuilder)value;

		for(int i=0; i<sv.length(); i++) {
			sv.setCharAt(i, Character.toUpperCase(sv.charAt(i)));
		}
	}

	public void upshift(int start, int len) {
		if((attr & (CHAR|GROUP|PICTURE)) == 0) return;
		if(value == null) return;
		StringBuilder sv = (StringBuilder)value;

		if(sv.length() < len) len = sv.length();
		for(int i=start; i<len; i++) {
			sv.setCharAt(i, Character.toUpperCase(sv.charAt(i)));
		}
	}
	public Var toUpper() { //Overridden in Group
		if((attr & (CHAR|GROUP|PICTURE)) == 0) return null;
		Var v = new Var(getString());
		v.upshift();
		return v;
	}
	public Var toLower() { //Overridden in Group
		if((attr & (CHAR|GROUP|PICTURE)) == 0) return null;
		Var v = new Var(getString());
		v.downshift();
		return v;
	}


	public void downshift() { //Overridden in Group
		if((attr & (CHAR|GROUP|PICTURE)) == 0) return;
		if(value == null) return;
		if(value instanceof StringBuilder) {
			StringBuilder sv = (StringBuilder)value;

			for(int i=0; i<sv.length(); i++) {
				sv.setCharAt(i, Character.toLowerCase(sv.charAt(i)));
			}
		}
	}

	public void downshift(int start, int len) {
		if((attr & (CHAR|GROUP|PICTURE)) == 0) return;
		if(value == null) return;
		if(value instanceof StringBuilder) {
			StringBuilder sv = (StringBuilder)value;

			if(sv.length() < len) len = sv.length();
			for(int i=start; i<len; i++) {
				sv.setCharAt(i, Character.toLowerCase(sv.charAt(i)));
			}
		}
	}

	/**
	 * converts this Var to a String. If the Var is numeric, the string
	 * representation of the numeric is given.
	 * 
	 * @return the String. If the Var is unassigned or null, "" is returned.
	 */
	public String toString() {
		/*if(!attr(GROUP) && !attr(ISASSIGNED)) return "";
		else*/ 
		try {
			return getString().trim();
		} catch (Exception e) {}
		return "";
		/*
		else if (attr(INTEGER))
			return getIntegerForGroup();
		else if (attr(LONG))
			return getLongForGroup();
		else if (attr(DOUBLE))
			return getDoubleForGroup();
		else if (attr(CHAR)) {
			return getCharForGroup();
		} else
			return "";
			*/
	}

	/**
	 * creates an array of Vars all with the same type and length. If the type
	 * is numeric, they cannot have decimals.
	 * 
	 * @param sz
	 *            the number of elements in the array.
	 * @param type
	 *            the attributes describing the type.
	 * @param len
	 *            the length of the field of each element.
	 * @return the instantiated array of Vars of the given type and length.
	 */
	public static Var[] createArray(int sz, int type, int len) {
		return createArray(sz, type, len, 0);
	}

	/**
	 * creates an array of Vars all with the same type, length and decimals.
	 * 
	 * @param sz
	 *            the number of elements in the array.
	 * @param type
	 *            the attributes describing the type.
	 * @param len
	 *            the length of the field of each element.
	 * @param dec
	 *            the number of decimals if the type is numeric.
	 * @return the instantiated array of Vars of the given type and length.
	 */
	public static Var[] createArray(int sz, int type, int len, int dec) 
	{
		Var[] var = new Var[sz + 1];
		for (int i = 0; i < sz + 1; i++) {
			var[i] = new Var(type, len, dec);
		}
		return var;
	}

	/**
	 * creates a two dimensional array of Vars all with the same type and
	 * length.
	 * 
	 * @param sz1
	 *            the number of elements in the 1st dimension of the array.
	 * @param sz2
	 *            the number of elements in the 2nd dimension of the array.
	 * @param type
	 *            the attributes describing the type.
	 * @param len
	 *            the length of the field of each element.
	 * @return the instantiated 2D array of Vars of the given type and length.
	 */
	public static Var[][] createArray2D(int sz1, int sz2, int type, int len) {
		return createArray2D(sz1, sz2, type, len, 0);
	}

	public static Var[][][] createArray3D(int sz1, int sz2, int sz3,
			int type, int len) {
		return createArray3D(sz1, sz2, sz3, type, len, 0);
	}

	public Var occurs(int size)  throws IllegalStateException {
		if(vArray != null)
			throw new IllegalStateException("Cannot call occurs more than once on a Var");
		vArray = new VarArray(this, size);
		return this;
	}
	
	public Var index(int idx) 
		throws  IndexOutOfBoundsException, IllegalArgumentException {
		if(vArray == null) {
			throw new IllegalArgumentException("Var has no index.");
		}
		if(idx < 1 || idx > vArray.size()) {
			throw new IndexOutOfBoundsException("Index out of range: " + idx);
		}
		return vArray.index(idx - 1);
	}
	
	public Var index(Var idx) {
		return index(idx.getInt());
	}
	public Var index(int idx, int idx2)	throws  Exception {
		throw new Exception("2d index not yet implemented");
	}
	public Var index(Var idx, Var idx2)	throws  Exception {
		return index(idx.getInt(), idx2.getInt());
	}
	public Var index(Var idx, int idx2)	throws  Exception {
		return index(idx.getInt(), idx2);
	}
	public Var index(int idx, Var idx2)	throws  Exception {
		return index(idx, idx2.getInt());
	}
	/**
	 * creates a two dimensional array of Vars all with the same type and
	 * length.
	 * 
	 * @param sz1
	 *            the number of elements in the 1st dimension of the array.
	 * @param sz2
	 *            the number of elements in the 2nd dimension of the array.
	 * @param type
	 *            the attributes describing the type.
	 * @param len
	 *            the length of the field of each element.
	 * @param dec
	 *            the number of decimals if the type is numeric.
	 * @return the instantiated 2D array of Vars of the given type and length.
	 */
	public static Var[][] createArray2D(int sz1, int sz2, int type, int len,
			int dec) {
		Var[][] var = new Var[sz1 + 1][sz2 + 1];
		for (int i = 0; i < sz1 + 1; i++) {
			for (int j = 0; j < sz2 + 1; j++) {
				var[i][j] = new Var(type, len, dec);
			}
		}
		return var;
	}

	public static Var[][][] createArray3D(int sz1, int sz2, int sz3, int type,
			int len, int dec) {
		Var[][][] var = new Var[sz1 + 1][sz2 + 1][sz3 + 1];
		for (int i = 0; i < sz1 + 1; i++) {
			for (int j = 0; j < sz2 + 1; j++) {
				for (int k = 0; k < sz3 + 1; k++) {
					var[i][j][k] = new Var(type, len, dec);
				}
			}
		}
		return var;
	}

	/*
	 * Date Conversion methods =======================
	 */
	/**
	 * Gives the current date of the system as a DC.
	 * 
	 * @return the system date as a DC.
	 */
	public DC systemDate() {
		DC d = new DC();
		Calendar clock = SystemDate.getCalendar();
		/*
		checkDate: try{
			Connection conn = DBConnectionFactory.getConnection();
			String Sql = "select new_date from datetab";
			CallableStatement stmnt = conn.prepareCall(Sql);
			ResultSet rs = stmnt.executeQuery();
			int newDate = 0;
			if(rs != null && rs.next()) {
				newDate = rs.getInt(1);
				rs.close();
			} else break checkDate;
		    Group lDate = new Group();
	       		Group lYYYY = lDate.addMember(Var.UNUMERIC, 4);
	       		Group lMM = lDate.addMember(Var.UNUMERIC, 2);
	       		Group lDD = lDate.addMember(Var.UNUMERIC, 2);
	       	lDate.set(newDate);
			stmnt.close();
			conn.close(); // Temporary
			clock.set(lYYYY.getInt(),lMM.getInt() - 1,lDD.getInt());
		} catch(Exception e) { ; }
		*/
		
		d.day = clock.get(Calendar.DAY_OF_MONTH);
		d.month = clock.get(Calendar.MONTH) + 1;
		d.year4 = clock.get(Calendar.YEAR);
		d.year2 = d.year4 % 100;
		d.hour = clock.get(Calendar.HOUR_OF_DAY);
		d.minute = clock.get(Calendar.MINUTE);
		d.second = clock.get(Calendar.SECOND);
		d.millisecond = clock.get(Calendar.MILLISECOND);

		return d;
	}

	private int lookupMonth(String s, Glb GLB) {
		int month;
		for (month = 0; month < DC.monthname.length; month++) {
			if (s.substring(0, 3)
					.compareTo(DC.monthname[month].substring(0, 3)) == 0)
				break;
		}
		month++;
		if (month > 12) {
			GLB.STATUS.set("*****");
			month = 0;
		}
		return month;
	}

	private boolean verifyDate(int day, int mnth, int yr4, Glb GLB) {
		int[] tab;
		boolean dateOk = true;

		if (yr4 % 4 != 0 || yr4 == 1900)
			tab = DC.daytab;
		else
			tab = DC.leapdaytab;

		if (mnth < 1 || mnth > 12)
			dateOk = false;
		else if (day < 1 || day > tab[mnth])
			dateOk = false;
		if (!dateOk)
			GLB.STATUS.set("*****");
		return dateOk;
	}

	/**
	 * Returns days from Jan 01 1800.
	 * 
	 * @param d
	 *            a DC containing the date to be converted
	 * @return the number of days since 1/1/1990 or -1 if the given date is
	 *         before 1/1/1900
	 */
	protected int toJulian(DC d, int glb_base) {
		return toJulian(d.day, d.month, d.year4, glb_base);
	}

	/**
	 * Returns days from Jan 01 GLB.BASE.
	 * 
	 * @param day
	 *            the day
	 * @param mnth
	 *            the month
	 * @param yr4
	 *            the year including the century
	 * @return the number of days since 1/1/GLB.BASE or -1 if the given date is
	 *         before 1/1/GLB.BASE
	 */
	protected int toJulian(int day, int mnth, int yr4, int glb_base) {
		if (yr4 < glb_base)
			return -1;
		if (mnth < 1 || mnth > 12)
			return -1;
		if (day < 1)
			return -1;
		long days = 0;
		/*int years = yr4 - 1800;
		int days = (years * 365);
		// add number of leapyears since 1900, which was not leap year.
		days += (years - 1) / 4;
		// is current year a leapyear?
		int[] tab;
		if ((years % 4) != 0 || years == 0)
			tab = DC.daytab;
		else
			tab = DC.leapdaytab;

		if (day > (tab[mnth] - tab[mnth - 1]))
			return -1;
		days += tab[mnth - 1];
		days += day - 1;*/
		
		GregorianCalendar  start = new GregorianCalendar (glb_base,0,1,0,0,0);
		GregorianCalendar  end = new GregorianCalendar (yr4,mnth - 1,day,12,1,1); //changed HOUR_OF_DAY from 1 to 12 -- to avoid daylight saving issue
		start.setLenient(false);
		end.setLenient(false);
		long MILLIS_PER_DAY = 1000 * 60 * 60 * 24;

	    
	    if(end.before(start)) return -1;
	    
	    long deltaMillis;
	    try {
	    	deltaMillis = end.getTimeInMillis() - start.getTimeInMillis();
	    } catch(IllegalArgumentException e) {
	    	return -1;
	    }
	    days = deltaMillis / MILLIS_PER_DAY;
		return (int)days;
	}

	private DC toGregorian(int daynum00,int glb_base) {
		if (daynum00 < 0)
			daynum00 = 0;
		/*int years = (int) (((double) daynum00 + 1.0) / 365.25);
		int days = daynum00 - (years * 365 + ((years - 1) / 4));
		int[] tab;
		if ((years % 4) != 0 || years == 0)
			tab = DC.daytab;
		else
			tab = DC.leapdaytab;

		int months;
		for (months = 0; days >= tab[months]; months++)
			;
		days -= tab[months - 1];*/
		GregorianCalendar  start = new GregorianCalendar (glb_base,0,1,0,0,0);
		GregorianCalendar  end = new GregorianCalendar ();

		long MILLIS_PER_DAY = 1000 * 60 * 60 * 24;
	    
		//long millisEnd = ((daynum00 - 1) * MILLIS_PER_DAY) + start.getTimeInMillis();
		long millisEnd = (daynum00 * MILLIS_PER_DAY) + start.getTimeInMillis();

		end.setTimeInMillis(millisEnd);

		DC d = new DC();
		d.year2 = end.get(Calendar.YEAR) % 100;
		d.year4 = end.get(Calendar.YEAR);
		d.month = end.get(Calendar.MONTH) + 1;
		d.day = end.get(Calendar.DAY_OF_MONTH);
		return (d);
	}

	/**
	 * Accepts string in the following formats: ddMMMyy, ddMMMccyy (for UK,US
	 * and I) dd/mm/yy, dd/mm/ccyy, ddmmyy, ddmmccyy, dd-MMM-(cc)yy (for UK)
	 * mm/dd/yy, mm/dd/ccyy, mmddyy, mmddccyy MMM-dd-(cc)yy (for US) yy/mm/dd,
	 * ccyy/mm/dd, yymmdd, ccyymmdd (cc)yy-MMM-dd (for I) Returns a NULL date if
	 * no matching format is found. GLB.STATUS is set to "*****"
	 */
	private DC splitDateString(String s, Glb GLB) {
		int day = 0, month = 0, yr2 = 0, yr4 = 0;

		if (s == null)
			return null;
		try {
			if (Character.getType(s.charAt(2)) == Character.DECIMAL_DIGIT_NUMBER) {
				// form is ddmmyy, mmddyy or yymmdd
				// form is ddmmccyy, mmddccyy or ccyymmdd
				if (GLB.intl == DC.UK) {
					day = Integer.valueOf(s.substring(0, 2)).intValue();
					month = Integer.valueOf(s.substring(2, 4)).intValue();
					yr2 = Integer.valueOf(s.substring(4)).intValue();
				} else if (GLB.intl == DC.US) {
					month = Integer.valueOf(s.substring(0, 2)).intValue();
					day = Integer.valueOf(s.substring(2, 4)).intValue();
					yr2 = Integer.valueOf(s.substring(4)).intValue();
				} else {
					if (s.length() == 6) {
						yr2 = Integer.valueOf(s.substring(0, 2)).intValue();
						month = Integer.valueOf(s.substring(2, 4)).intValue();
						day = Integer.valueOf(s.substring(4)).intValue();
					} else if (s.length() == 8) {
						yr2 = Integer.valueOf(s.substring(0, 4)).intValue();
						month = Integer.valueOf(s.substring(4, 6)).intValue();
						day = Integer.valueOf(s.substring(6)).intValue();
					}
				}
			} else if (" -/".indexOf(s.charAt(2)) >= 0) {
				if (GLB.intl == DC.UK) { // dd/mm/(cc)yy, dd-MMM-(cc)yy
					day = Integer.valueOf(s.substring(0, 2)).intValue();
					if (Character.getType(s.charAt(3)) == Character.DECIMAL_DIGIT_NUMBER) {
						month = Integer.valueOf(s.substring(3, 5)).intValue();
						yr2 = Integer.valueOf(s.substring(6)).intValue();
					} else {
						month = lookupMonth(s.substring(3, 6), GLB);
						yr2 = Integer.valueOf(s.substring(7)).intValue();
					}
				} else if (GLB.intl == DC.US) { // mm/dd/(cc)yy
					month = Integer.valueOf(s.substring(0, 2)).intValue();
					day = Integer.valueOf(s.substring(3, 5)).intValue();
					yr2 = Integer.valueOf(s.substring(6)).intValue();
				} else { /* yy/mm/dd, yy-MMM-dd */
					yr2 = Integer.valueOf(s.substring(0, 2)).intValue();
					if (Character.getType(s.charAt(3)) == Character.DECIMAL_DIGIT_NUMBER) {
						month = Integer.valueOf(s.substring(3, 5)).intValue();
						day = Integer.valueOf(s.substring(6, 8)).intValue();
					} else {
						month = lookupMonth(s.substring(3, 6), GLB);
						day = Integer.valueOf(s.substring(7, 9)).intValue();
					}
				}
			} else if (" -/".indexOf(s.charAt(3)) >= 0) {
				/* US: MMM-dd-(cc)yy, MMM dd (cc)yy */
				month = lookupMonth(s.substring(0, 3), GLB);
				day = Integer.valueOf(s.substring(4, 6)).intValue();
				yr2 = Integer.valueOf(s.substring(7)).intValue();
			} else if (" -/".indexOf(s.charAt(4)) >= 0) {
				/* I: ccyy-MMM-dd, ccyy/mm/dd */
				yr2 = Integer.valueOf(s.substring(0, 4)).intValue();
				if (Character.getType(s.charAt(5)) == Character.DECIMAL_DIGIT_NUMBER) {
					month = Integer.valueOf(s.substring(5, 7)).intValue();
					day = Integer.valueOf(s.substring(8, 10)).intValue();
				} else {
					month = lookupMonth(s.substring(5, 8), GLB);
					day = Integer.valueOf(s.substring(9, 11)).intValue();
				}
			} else { /* form is ddMMM(cc)yy, MMMdd(cc)yy, (cc)yyMMMdd */
				if (GLB.intl == DC.UK) {
					day = Integer.valueOf(s.substring(0, 2)).intValue();
					month = lookupMonth(s.substring(2, 5), GLB);
					yr2 = Integer.valueOf(s.substring(5)).intValue();
				} else if (GLB.intl == DC.US) {
					month = lookupMonth(s.substring(0, 3), GLB);
					day = Integer.valueOf(s.substring(3, 5)).intValue();
					yr2 = Integer.valueOf(s.substring(5, 7)).intValue();
				} else {
					if (s.length() >= 9) {
						yr2 = Integer.valueOf(s.substring(0, 4)).intValue();
						month = lookupMonth(s.substring(4, 7), GLB);
						day = Integer.valueOf(s.substring(7, 9)).intValue();
					} else {
						yr2 = Integer.valueOf(s.substring(0, 2)).intValue();
						month = lookupMonth(s.substring(2, 5), GLB);
						day = Integer.valueOf(s.substring(5, 7)).intValue();
					}
				}
			}
		} catch (Exception e) {
			// System.out.println("Exception in splitDateString:
			// "+e.toString());
			// e.printStackTrace();
			return null;
		}
		if (yr2 >= 100) {
			yr4 = yr2;
			yr2 = yr4 % 100;
		} else {
//			yr4 = yr2 + GLB.CENTURY.getInt() * 100;
			yr4 = yr2ToYr4(yr2,GLB);
		}

		if (!verifyDate(day, month, yr4, GLB)) {
			if (month < 1 || month > 12)
				day = month = 1;
			else
				day = 1;
		}

		DC d = new DC();
		d.day = day;
		d.month = month;
		d.year4 = yr4;
		d.year2 = yr2;

		return d;
	}

	private int yr2ToYr4(int yr2, Glb GLB) {
		int yr4;
		int start = GLB.CENTURY_START.getInt() / 100;
		yr2 %= 100;
		if (yr2 < start) {
			yr4 = yr2 + (DC.baseCentury * 100);
		} else
			yr4 = yr2 + ((DC.baseCentury - 1) * 100);
		return yr4;
	}

	/**
	 * returns the number of days from the epoch year (normally 1/1/1957). It's
	 * used to implement LINC simple data converts.
	 * 
	 * @param GLB
	 *            the Glb class is needed so global variables can be set.
	 * @return the day number.
	 */
	public Var toDayNumber(Glb GLB) {
		int dn = 0;
		Var v = new Var(NUMERIC, 5);
		String s = new String(getString().trim());
		int slength = s.length();

		if (slength < 6 || slength > 7) {
			GLB.STATUS.set("*****");
			v.set(0);
		    GLB.TOTAL.set(0);
			return v;
		}

		if (slength == 6) {
			try {
				dn = Integer.valueOf(s).intValue();
			} catch (NumberFormatException e) {
				dn = 0;
			}
			if(dn == 0) {
				GLB.STATUS.set("*****");
				v.set(0);
			    GLB.TOTAL.set(0);
				return v;
			}
		}

		GLB.STATUS.set("     ");

		DC d = splitDateString(s, GLB);
		if (d == null) { // Linc tries again changing first day to 1, then
							// day and month to 1 if that fails too
			if (GLB.intl == DC.UK)
				s = "01" + s.substring(2);
			else if (GLB.intl == DC.US)
				s = s.substring(0, 2) + "01" + s.substring(4);
			else if (GLB.intl == DC.I)
				s = s.substring(0, 4) + "01";
			d = splitDateString(s, GLB);
			if (d == null) {
				if (GLB.intl == DC.US)
					s = "01" + s.substring(2);
				else if (GLB.intl == DC.UK)
					s = s.substring(0, 2) + "01" + s.substring(4);
				else if (GLB.intl == DC.I)
					s = s.substring(0, 2) + "0101";
				d = splitDateString(s, GLB);
				if (d == null) {
					GLB.STATUS.set("*****");
					v.set(0);
				    GLB.TOTAL.set(0);
					return v;
				}
			}
		}
		dn = toJulian(d, GLB.BASE.getInt());
		if (dn < 0) {
			GLB.STATUS.set("*****");
		    GLB.TOTAL.set(0);
			v.set(0);
			return v;
		}
		GLB.TOTAL.set(dn);
		v.set(dn);
		return v;
	}

	/**
	 * It takes a day number and converts it to a date. It's used to implement
	 * LINC simple data converts.
	 * 
	 * @param GLB
	 *            the Glb class is needed so global variables can be set.
	 * @return the date
	 */
	public Var toDate(Glb GLB) {
		GLB.STATUS.set("     ");
		DC d = toGregorian(getInt(), GLB.BASE.getInt());
		GLB.CENTURY.set(d.year4 / 100);
		int n;
		if (GLB.intl == DC.US)
			n = d.month * 10000 + d.day * 100 + d.year2;
		else if (GLB.intl == DC.I)
			n = d.year2 * 10000 + d.month * 100 + d.day;
		else
			n = d.day * 10000 + d.month * 100 + d.year2;
		Var v = new Var(NUMERIC, 6);
		v.set(n);
		return v;
	}

	private String twoDig(int p) {
		if (p > 9)
			return (Integer.toString(p));
		return ("0" + Integer.toString(p));
	}

	private String fourDig(int p) {
		if (p > 999)
			return (Integer.toString(p));
		else if (p > 99)
			return ("0" + Integer.toString(p));
		else
			return ("000" + Integer.toString(p));
	}

	/**
	 * It takes a day number and converts it to an alphanumeric representation
	 * of a date. It's used to implement LINC simple data converts.
	 * 
	 * @param GLB
	 *            the Glb class is needed so global variables can be set.
	 * @return the date in alphas
	 */
	public Var toAlpha(Glb GLB) {
		String s;
		Var v;
		GLB.STATUS.set("     ");
		DC d = toGregorian(getInt(), GLB.BASE.getInt());
		if (d == null) {
			GLB.STATUS.set("*****");
			v = new Var(CHAR, 0);
		} else {
			GLB.CENTURY.set(d.year4 / 100);
			if (GLB.intl == DC.UK)
				s = twoDig(d.day) + DC.monthname[d.month - 1].substring(0, 3)
						+ twoDig(d.year2);
			else if (GLB.intl == DC.US)
				s = DC.monthname[d.month - 1].substring(0, 3) + twoDig(d.day)
						+ twoDig(d.year2);
			else
				s = twoDig(d.year2) + DC.monthname[d.month - 1].substring(0, 3)
						+ twoDig(d.day);
			v = new Var(CHAR, s.length());
			v.set(s);
		}
		return v;
	}

	private int atoi(String s) {
		try {
			return Integer.valueOf(s).intValue();
		} catch (Exception e) {
			return 0;
		}
	}

	private int atoi(String s, int len) {
		try {
			return atoi(s.substring(0, len));
		} catch (Exception e) {
			return 0;
		}
	}

	/* 
	 private long atol(String s) {
		try {
		    int i = s.indexOf(".");
		    if(i == -1) return Long.valueOf(s).longValue();
		    else return Long.valueOf(s.substring(0, i)).longValue();
		} catch (Exception e) {
			return 0L;
		}
	}
	*/

	/*
	 private long atol(String s, int len) {
		try {
			return atol(s.substring(0, len));
		} catch (Exception e) {
			return 0L;
		}
	}
	*/
	
	private double atod(String s) {
		try {
			return Double.valueOf(s).doubleValue();
		} catch (Exception e) {
			return 0L;
		}
	}

	/*private double atod(String s, int len) {
		try {
			return atod(s.substring(0, len));
		} catch (Exception e) {
			return 0L;
		}
	}
	*/
	
	/**
	 * This method takes a date in the given "format", applies the "adjust"ment
	 * and sets global variables to various date styles possible. It is used to
	 * effect the complex LINC date convert.
	 * 
	 * @param format
	 *            the date format of this.
	 * @param adjust
	 *            the number of days to adjust the date by.
	 * @param GLB
	 *            the Glb class is needed so global variables can be set.
	 */
	public void dateConvert(int format, long adjust, Glb GLB) {
		dateConvert(format, adjust, GLB, false);
		return;
	}

	private void dateConvert(int format, long adjust, Glb GLB, boolean editOnly) {
		int err = 0;
		int yr4 = 0;
		int yr2 = 0;
		int month = 0;
		int day = 0;
		int dow, doy = 0;
		int daynum57 = -1;
		int daynum00 = 0;
		int weeknum;
		int[] tab = null;
		String fmtstr = null;
		DC vd = null;
		
		String mysv = getString();

		switch (format) {
		case DC.DAYNUM:
			daynum00 = getInt();
			//Changed back base = 1920 start =1930 gpc number to day -6 etc
			//vd = toGregorian(daynum57 + GLB.epoch, GLB.BASE.getInt());
			vd = toGregorian(daynum00, GLB.BASE.getInt());
			break;
		case DC.YYDDD:
			yr2 = atoi(mysv, 2);
			doy = atoi(rtrim().substring(2));
			if (yr2 % 4 != 0)
				tab = DC.daytab;
			else
				tab = DC.leapdaytab;
			if (doy < 1 || doy > tab[12])
				err++;
			else {
				for (month = 0; tab[month] < doy; month++)
					;
				day = doy - tab[month - 1];
			}
			break;
		case DC.UK_ALPHA:
			String[] list = mysv.split("[ /,-]");
			if (list.length < 3)
				break;
			day = atoi(list[0]);
			month = lookupMonth(list[1], GLB);
			yr2 = atoi(list[2]);
			break;
		case DC.US_ALPHA:
			list = mysv.split("[ /,-]");
			if (list.length < 3)
				break;
			month = lookupMonth(list[0], GLB);
			day = atoi(list[1]);
			yr2 = atoi(list[2]);
			break;
		case DC.IN_ALPHA:
			list = mysv.split("[ /,-]");
			if (list.length < 3)
				break;
			yr2 = atoi(list[0]);
			month = lookupMonth(list[1], GLB);
			day = atoi(list[2]);
			break;
		case DC.DDMMYY:
			fmtstr = "DDMMYY";
			break;
		case DC.MMDDYY:
			fmtstr = "MMDDYY";
			break;
		case DC.YYMMDD:
			fmtstr = "YYMMDD";
			break;
		case DC.DD_MM_YY:
			fmtstr = "DD_MM_YY";
			break;
		case DC.DD_MMM_YY:
			fmtstr = "DD_MMM_YY";
			break;
		case DC.DDMMMYY:
			fmtstr = "DDMMMYY";
			break;
		case DC.MM_DD_YY:
			fmtstr = "MM_DD_YY";
			break;
		case DC.MMM_DD_YY:
			fmtstr = "MMM_DD_YY";
			break;
		case DC.MMMDDYY:
			fmtstr = "MMMDDYY";
			break;
		case DC.YY_MM_DD:
			fmtstr = "YY_MM_DD";
			break;
		case DC.YY_MMM_DD:
			fmtstr = "YY_MMM_DD";
			break;
		case DC.YYMMMDD:
			fmtstr = "YYMMMDD";
			break;
		case DC.DD_MM_CCYY:
			fmtstr = "DD_MM_CCYY";
			break;
		case DC.DD_MMM_CCYY:
			fmtstr = "DD_MMM_CCYY";
			break;
		case DC.DDMMMCCYY:
			fmtstr = "DDMMMCCYY";
			break;
		case DC.DDMMCCYY:
			fmtstr = "DDMMCCYY";
			break;
		case DC.MM_DD_CCYY:
			fmtstr = "MM_DD_CCYY";
			break;
		case DC.MMDDCCYY:
			fmtstr = "MMDDCCYY";
			break;
		case DC.MMM_DD_CCYY:
			fmtstr = "MMM_DD_CCYY";
			break;
		case DC.MMMDDCCYY:
			fmtstr = "MMMDDCCYY";
			break;
		case DC.CCYY_MM_DD:
			fmtstr = "CCYY_MM_DD";
			break;
		case DC.CCYY_MMM_DD:
			fmtstr = "CCYY_MMM_DD";
			break;
		case DC.CCYYDDD:
			yr4 = atoi(mysv, 2);
			yr2 = atoi(rtrim().substring(2,4), 2);
			yr4 = yr2 + (yr4 * 100);
			
			doy = atoi(rtrim().substring(4));
			if (yr2 % 4 != 0)
				tab = DC.daytab;
			else
				tab = DC.leapdaytab;
			if (doy < 1 || doy > tab[12])
				err++;
			else {
				for (month = 0; tab[month] < doy; month++)
					;
				day = doy - tab[month - 1];
			}
			break;
		case DC.CCYYMMDD:
			fmtstr = "CCYYMMDD";
			break;
		case DC.CCYYMMMDD:
			fmtstr = "CCYYMMMDD";
			break;
		default:
			// display("Error in %s: Bad Date Format.\n",self);
			err++;
			GLB.setStatus("*****");
			return;
			// daynum00 = set_sysdate(&yr4, &month, &day);
		}
		if (fmtstr != null) {
			// split_date(fmtstr, dbuf, &day, &month, &tyear);
			int cc = 0;
			int f, d;
			if(mysv.length() > fmtstr.length() && (attr & (NUMERIC)) != 0) {
				mysv = mysv.substring(mysv.length() - fmtstr.length());
			}
			
			for (d = 0, f = 0; f < fmtstr.length() && d < mysv.length();) {
				if (fmtstr.charAt(f) == 'D' && fmtstr.charAt(f + 1) == 'D') {
					day = ((mysv.charAt(d) - '0') * 10)
							+ (mysv.charAt(d + 1) - '0');
					f += 2;
					d += 2;
				} else if (fmtstr.charAt(f) == 'M'
						&& fmtstr.charAt(f + 1) == 'M') {
					if (fmtstr.charAt(f + 2) == 'M') {
						month = lookupMonth(mysv.substring(d, d + 3), GLB);
						f += 3;
						d += 3;
					} else {
						month = ((mysv.charAt(d) - '0') * 10)
								+ (mysv.charAt(d + 1) - '0');
						f += 2;
						d += 2;
					}
				} else if (fmtstr.charAt(f) == 'Y'
						&& fmtstr.charAt(f + 1) == 'Y') {
					yr2 = ((mysv.charAt(d) - '0') * 10)
							+ (mysv.charAt(d + 1) - '0');
					f += 2;
					d += 2;
				} else if (fmtstr.charAt(f) == 'C'
						&& fmtstr.charAt(f + 1) == 'C') {
					cc = ((mysv.charAt(d) - '0') * 10) + (mysv.charAt(d + 1) - '0');
					f += 2;
					d += 2;
				} else {
					f++;
					d++;
				}
			}
			if (cc != 0) {
				yr4 = yr2 + (cc * 100);
			}
		}

		if (err > 0) {
			daynum00 = GLB.epoch;
			daynum57 = 0;
			vd = toGregorian(daynum00, GLB.BASE.getInt());
			GLB.setStatus("*****");
			//return;
		}
		if (vd != null) {
			yr4 = vd.year4;
			yr2 = vd.year2;
			day = vd.day;
			month = vd.month;
		}
		if (yr4 == 0) {
			if (yr2 < 100) {
				if(GLB.CENTURY.eq(0))
					yr4 = yr2ToYr4(yr2, GLB);
				else
					yr4=(GLB.CENTURY.getInt()) * 100 + yr2;
			} else {
				yr4 = yr2;
				yr2 %= 100;
			}
		}
		if (daynum57 < 0) {
			daynum00 = toJulian(day, month, yr4,GLB.BASE.getInt());
			daynum57 = daynum00 - GLB.epoch;
		} else
			daynum00 = daynum57 + GLB.epoch;

		/* check if date is valid */
		if (yr4 < GLB.BASE.getInt()) {
			GLB.setStatus("*****");
			err++;
			//Util.debug("Year out of range");
			//Manual say if invalid date the todays date is used.
			//return;
		}
		if (month < 1 || month > 12) {
			GLB.setStatus("*****");
			err++;
			//Util.debug("Month out of range");
			//return;
		}
		if (tab == null) {
			if (yr4 % 4 != 0 || yr4 == 1900)
				tab = DC.daytab;
			else
				tab = DC.leapdaytab;
		}
		if ((month > 0 && month < 13) && (day < 1 || day > (tab[month] - tab[month - 1])) ) {
			// display("Error in %s: Bad day: fmt=%d date='%s'.\n",
			// self,format,data);
			GLB.setStatus("*****");
			err++;
			//return;
			// set_sysdate(&yr4, &month, &day);
		}
		if (err == 0) GLB.setStatus(" ");
		if(editOnly) return;
		if (err > 0) {
			vd = GLB.TODAY.systemDate();
			daynum00 = toJulian(vd ,GLB.BASE.getInt());
			daynum57 = daynum00 - GLB.epoch;
			vd = toGregorian(daynum00, GLB.BASE.getInt());
			yr4 = vd.year4;
			yr2 = vd.year2;
			day = vd.day;
			month = vd.month;
		}
			
		String dcSetsGlbCentury = Config.getProperty("dc.sets.glb.century");
		GLB.DC_CC.set(yr4 / 100);
		if(dcSetsGlbCentury == null || dcSetsGlbCentury.equals("Y"))
			GLB.CENTURY.set(GLB.DC_CC);

		if (adjust != 0) {
			daynum00 += adjust;
			daynum57 += adjust;
			vd = toGregorian(daynum00, GLB.BASE.getInt());
			//Changed back base = 1920 start =1930
			//vd = toGregorian(daynum57, GLB.BASE.getInt());
			day = vd.day;
			month = vd.month;
			yr2 = vd.year2;
			yr4 = vd.year4;
			GLB.DC_CC.set(yr4 / 100);
			if(dcSetsGlbCentury == null || dcSetsGlbCentury.equals("Y"))
				GLB.CENTURY.set(GLB.DC_CC);
			if (yr4 % 4 != 0 || yr4 == 1900)
				tab = DC.daytab;
			else
				tab = DC.leapdaytab;
		}
		//doy = tab[month - 1] + day; /* January 1st = 1 */
		Calendar cal = SystemDate.getCalendar();

		cal.set(yr4, month -1, day);
		
		dow = cal.get(Calendar.DAY_OF_WEEK) - 1;
	    doy = cal.get(Calendar.DAY_OF_YEAR);
		/* Calculate DOW for JAN 1st, this year */
		//jan1 = (((yr4 - 1900) * 365) + ((yr4 - 1901) / 4) + 1) % 7;
		//weeknum = (doy + jan1 - 1) / 7 + 1;
	    weeknum = cal.get(Calendar.WEEK_OF_YEAR);
		/* Fill in the global LINC environment */
		GLB.DC_YYDDD.set(yr2 * 1000 + doy);
		GLB.DC_WEEKNO.set(weeknum);
		//GLB.DC_DAYNUM.set(daynum57);
		GLB.DC_DAYNUM.set(daynum00);
		GLB.DC_DDMMYY.set(day * 10000 + month * 100 + yr2);
		GLB.DC_MMDDYY.set(month * 10000 + day * 100 + yr2);
		GLB.DC_YYMMDD.set(yr2 * 10000 + month * 100 + day);
		GLB.DC_DDMMCCYY.set(day * 1000000 + month * 10000 + yr4);
		GLB.DC_MMDDCCYY.set(month * 1000000 + day * 10000 + yr4);
		GLB.DC_CCYYDDD.set(yr4 * 1000 + doy);
		GLB.DC_CCYYMMDD.set(yr4 * 10000 + month * 100 + day);

		GLB.DC_DD_MM_YY.set(twoDig(day) + "/" + twoDig(month) + "/"
				+ twoDig(yr2));
		GLB.DC_MM_DD_YY.set(twoDig(month) + "/" + twoDig(day) + "/"
				+ twoDig(yr2));
		GLB.DC_YY_MM_DD.set(twoDig(yr2) + "/" + twoDig(month) + "/"
				+ twoDig(day));
		GLB.DC_DD_MMM_YY.set(twoDig(day) + " "
				+ DC.monthname[month - 1].substring(0, 3) + " " + twoDig(yr2));
		GLB.DC_MMM_DD_YY.set(DC.monthname[month - 1].substring(0, 3) + " "
				+ twoDig(day) + " " + twoDig(yr2));
		GLB.DC_YY_MMM_DD.set(twoDig(yr2) + " "
				+ DC.monthname[month - 1].substring(0, 3) + " " + twoDig(day));
		GLB.DC_DDMMMYY.set(twoDig(day)
				+ DC.monthname[month - 1].substring(0, 3) + twoDig(yr2));
		GLB.DC_MMMDDYY.set(DC.monthname[month - 1].substring(0, 3)
				+ twoDig(day) + twoDig(yr2));
		GLB.DC_YYMMMDD.set(twoDig(yr2)
				+ DC.monthname[month - 1].substring(0, 3) + twoDig(day));
		GLB.DC_UK_ALPHA.set(twoDig(day) + " " + DC.monthname[month - 1]
				+ Integer.toString(yr4));
		GLB.DC_US_ALPHA.set(DC.monthname[month - 1] + " " + twoDig(day) + " "
				+ Integer.toString(yr4));
		GLB.DC_IN_ALPHA.set(Integer.toString(yr4) + " "
				+ DC.monthname[month - 1] + " " + twoDig(day));

		GLB.DC_DD_MM_CCYY.set(twoDig(day) + "/" + twoDig(month) + "/"
				+ fourDig(yr4));
		GLB.DC_DD_MMM_CCYY.set(twoDig(day) + " "
				+ DC.monthname[month - 1].substring(0, 3) + " " + fourDig(yr4));
		GLB.DC_DDMMMCCYY.set(twoDig(day)
				+ DC.monthname[month - 1].substring(0, 3) + fourDig(yr4));
		GLB.DC_MM_DD_CCYY.set(twoDig(month) + "/" + twoDig(day) + "/"
				+ fourDig(yr4));
		GLB.DC_MMM_DD_CCYY.set(DC.monthname[month - 1].substring(0, 3) + " "
				+ twoDig(day) + " " + fourDig(yr4));
		GLB.DC_MMMDDCCYY.set(DC.monthname[month - 1].substring(0, 3)
				+ twoDig(day) + fourDig(yr4));
		GLB.DC_CCYY_MM_DD.set(fourDig(yr4) + "/" + twoDig(month) + "/"
				+ twoDig(day));
		GLB.DC_CCYY_MMM_DD.set(fourDig(yr4) + " "
				+ DC.monthname[month - 1].substring(0, 3) + " " + twoDig(day));
		GLB.DC_CCYYMMMDD.set(fourDig(yr4)
				+ DC.monthname[month - 1].substring(0, 3) + twoDig(day));

		GLB.DC_TODAY.set(DC.dayname[dow]);
	}

	/**
	 * This method takes a date in the given "format", applies the "adjust"ment
	 * and sets global variables to various date styles possible. It is used to
	 * effect the complex LINC date convert.
	 * 
	 * @param format
	 *            the date format of this.
	 * @param adjust
	 *            the number of days to adjust the date by.
	 * @param GLB
	 *            the Glb class is needed so global variables can be set.
	 */
	public void dateConvert(int format, Var adjust, Glb GLB) {
		dateConvert(format, adjust.getInt(), GLB, false);
	}
	
	/**
	 * This method takes a date in the given "format", and validates it
	 * It is used to effect the complex LINC date convert edit only.
	 * 
	 * @param format
	 *            the date format of this.
	 * @param GLB
	 *            the Glb class is needed so global variables can be set.
	 */
	public void dateEdit(int format, Glb GLB) {
		dateConvert(format, 0, GLB, true);
	}

	/**
	 * This method takes the current machine date and converts into a YYMM
	 * format as an int.
	 * 
	 * @param GLB
	 *            the Glb class is needed so global variables can be set.
	 * @return the int representing YYMM
	 */
	public int dateYYMM(Glb GLB) {
		return dateYYMM(0, GLB);
	}

	/**
	 * This method takes the date given as a day number and converts into a YYMM
	 * format as an int.
	 * 
	 * @param daynum57
	 *            the day number.
	 * @param GLB
	 *            the Glb class is needed so global variables can be set.
	 * @return the int representing YYMM
	 */
	public int dateYYMM(int daynum57, Glb GLB) {
		DC d;

		if (daynum57 == 0)
			d = systemDate();
		else
			d = toGregorian(daynum57 + GLB.epoch, GLB.BASE.getInt());
		return d.year2 * 100 + d.month;
	}

	/**
	 * remove leading whitespace from this giving the result as a String.
	 * 
	 * @return String with no leading spaces
	 */
	public String ltrim() {
		//if(attr(GROUP) || !attr(CHAR|PICTURE)) return getString().replaceAll("^\\s+", "");
		if((attr & (GROUP)) != 0 || (attr & (CHAR|PICTURE)) == 0) return getString().replaceAll("^ +", "");
	    else if(value == null) return "";
	    else {
	        if(value instanceof StringBuilder) {
	            StringBuilder sv = (StringBuilder)value;
	            //return sv.toString().replaceAll("^\\s+", "");
	            return sv.toString().replaceAll("^ +", "");
	        } else if(value instanceof Long) return ((Long)value).toString();
	        else if(value instanceof Double) return ((Double)value).toString();
			else throw new IllegalStateException("Unknown object type");
	    }
	}

	/**
	 * remove trailing whitespace from this giving the result as a String.
	 * 
	 * @return String with no trailing spaces
	 */
	public String rtrim() {
	    //if(attr(GROUP) || !attr(CHAR|PICTURE)) return getString().replaceAll("\\s+$", "");
	    if((attr & (GROUP)) != 0 || (attr & (CHAR|PICTURE)) == 0) return getString().replaceAll(" +$", "");
		else if(value == null) return "";
	    else {
	        if(value instanceof StringBuilder) {
	            StringBuilder sv = (StringBuilder)value;
	            //return sv.toString().replaceAll("\\s+$", "");
	            return sv.toString().replaceAll(" +$", "");
	        } else if(value instanceof Long) return ((Long)value).toString();
	        else if(value instanceof Double) return ((Double)value).toString();
	        else throw new IllegalStateException("Unknown object type");
	    }
	}

	/**
	 * replace multiple whitespaces in "this" between words with single blank.
	 * 
	 * @return the cleaned Var as a String
	 */
	public String itrim() {
	    if((attr & (GROUP)) != 0 || (attr & (CHAR|PICTURE)) == 0) return getString().replaceAll("\\b\\s{2,}\\b", " ");
	    else if(value == null) return "";
		else {
			if(value instanceof StringBuilder) {
				StringBuilder sv = (StringBuilder)value;
				return sv.toString().replaceAll("\\b\\s{2,}\\b", " ");
		    } else if(value instanceof Long) return ((Long)value).toString();
		    else if(value instanceof Double) return ((Double)value).toString();
		    else throw new IllegalStateException("Unknown object type");
		}
	}

	public Var replace(int startPos, int len, String replacement) {
		if(startPos < 0 || startPos > (displayLength - 1)) return this;
		if((startPos + len) > displayLength) len = displayLength - startPos;
		if(replacement.length() > len)
			replacement = replacement.substring(0, len);
		else if(replacement.length() < len) {
			StringBuilder sb = new StringBuilder();
			while(sb.length() < len) {
				sb.append((replacement));
			}
			if(replacement.length() > len)
				replacement = sb.substring(0, len);
			else replacement = sb.toString();
		}
		if(value instanceof StringBuilder) {
			StringBuilder sv = (StringBuilder)value;
			if(sv.length() < displayLength) {
				sv = new StringBuilder(getString());
				value = sv;
			}
			sv.replace(startPos, startPos+len, replacement);
		} else {
			StringBuilder sv = new StringBuilder(getString());
			sv.replace(startPos, startPos+len, replacement);
			set(sv.toString());
		}
		return this;
	}
	/**
	 * remove leading and trailing whitespace from a Var.
	 * 
	 * @return the cleaned Var as a String
	 */
	public String lrtrim() {
		String res = rtrim();
		Var source = new Var(CHAR, res.length());
		source.set(rtrim());
		return source.ltrim();
	}

	/**
	 * Test whether string is numeric.
	 * 
	 * @return True or false
	 */
	public Boolean isNum() {
		//System.out.println("in isnum");
		if ((attr & (CHAR|PICTURE|GROUP)) == 0) return true;
		final char[] digits = getString(true).toCharArray();

		//System.out.println("length=" + digits.length);
		for (int i = 0; i < digits.length; i++) {
		  if (!Character.isDigit(digits[i])) {
		    return false;
		  }
		  //System.out.println(">" + digits[i] + "< is ok");
		}
		return true;
	}
	
	/**
	 * Test whether string is alphabetic upper case.
	 * 
	 * @return True or false
	 */
	public Boolean isAlphaUpper() {
		if ((attr & (CHAR|PICTURE|GROUP)) == 0)
			return false;
		final char[] chars = getString().toCharArray();
		 for (int x = 0; x < chars.length; x++) {      
		   final char c = chars[x];
		   if (((c >= 'A') && (c <= 'Z'))) continue; // uppercase
		   if ((c == ' ')) continue; // space
		   return false;
		 }  
		 return true;
	}
	/**
	 * Test whether string is alphabetic lower case.
	 * 
	 * @return True or false
	 */
	public Boolean isAlphaLower() {
		if ((attr & (CHAR|PICTURE|GROUP)) == 0)
			return false;
		final char[] chars = getString().toCharArray();
		 for (int x = 0; x < chars.length; x++) {      
		   final char c = chars[x];
		   if ((c >= 'a') && (c <= 'z')) continue; // lowercase
		   if ((c == ' ')) continue; // space
		   return false;
		 }  
		 return true;
	}
	/**
	 * Test whether string is alphabetic.
	 * 
	 * @return True or false
	 */
	public boolean isAlpha() {
		 final char[] chars = getString().toCharArray();
		 for (int x = 0; x < chars.length; x++) {      
		   final char c = chars[x];
		   if ((c >= 'a') && (c <= 'z')) continue; // lowercase
		   if ((c >= 'A') && (c <= 'Z')) continue; // uppercase
		   //if ((c >= '0') && (c <= '9')) continue; // numeric
		   if ((c == ' ')) continue; // space
		   return false;
		 }  
		 return true;
	}
	/**
	 * Test whether string is alphanumeric.
	 * 
	 * @return True or false
	 */
	public boolean isAlphaNumeric() {
		 final char[] chars = getString().toCharArray();
		 for (int x = 0; x < chars.length; x++) {      
		   final char c = chars[x];
		   if ((c >= 'a') && (c <= 'z')) continue; // lowercase
		   if ((c >= 'A') && (c <= 'Z')) continue; // uppercase
		   if ((c >= '0') && (c <= '9')) continue; // numeric
		   if ((c == ' ')) continue; // space
		   return false;
		 }  
		 return true;
	}


	/**
	 * <p>detach() returns a token, delimited by 'delim' and starting from the position
	 * 'pos'. The length of the resultant string is stored in glb.LENGTH and the 
	 * offset to the next token is stored in glb.detachOffset.
	 * </p>
	 * <p>If the Glb object is not null then after detach has completed, glb.status will
	 * contain spaces if there was no error. If pos &lt; 1, or pos > the objects length,
	 * then glb.status will be set to "*****". glb.LENGTH will contain the length of 
	 * the returned token and glb.detachOffset will be set equal to the offset of the
	 * start of the next token.
	 * </p>
	 * <p>If the character at 'pos' in the string is the delimiter, a single space is returned
	 * and GLB.LENGTH is set to 1.
	 * </p>
	 * A sequence of repeated delimiters is treated as a single delimiter. 
	 * 
	 * 
	 * @param glb a reference to the applications instantiated Glb object.
	 * If glb is null then the various GLB. values will not be updated.
	 * @param pos the starting point (1 relative)
	 * @param delim the delimiter
	 * @return The resulting token
	 */
	public String detach(Glb glb, int pos) {
		return detach(glb, pos, " ");
	}
	public String detach(Glb glb, int pos, String delim) {
		pos--;
		if(pos < 0 || pos > displayLength) {
			if(glb != null) {
				glb.LENGTH.set(0);
				glb.setStatus("*****");
			}
			return "";
		}
		if(delim == null || delim.length() < 1) delim = " ";
		String s = getString();
		
		String rval;
		int idx = s.indexOf(delim, pos);
		if(idx == -1) {
			if(glb != null) {
				glb.setStatus(0);
				glb.LENGTH.set(s.length() - pos);
				glb.detachOffset = pos + s.length() + 1;
			}
			return s.substring(pos);
		} else if(idx == pos) { //occurence at start of string
			if(glb != null) {
				glb.setStatus(0);
				glb.LENGTH.set(1);
			}
			rval = " ";
		} else {
			rval = s.substring(pos, idx);
			if(glb != null) {
				glb.setStatus(0);
				glb.LENGTH.set(rval.length());
			}
		}

		if(glb != null) {
			glb.detachOffset = idx + delim.length();
			int i;
			for(i=glb.detachOffset;i<(s.length() - delim.length()); i += delim.length()) {
				if(s.indexOf(delim, i) == i) continue;
				else break;
			}
			glb.detachOffset = i + 1;
		}
		return rval;
	}

	public String detach(Glb glb, Var pos) {
		return detach(glb, pos.getInt(), " ");
	}
	public String detach(Glb glb, Var pos, String delim) {
		return detach(glb, pos.getInt(), delim);
	}

	public String detach(Glb glb, int pos, Var delim) {
	    return detach(glb, pos, delim.getString());
	}

	public String detach(Glb glb, Var pos, Var delim) {
	    return detach(glb, pos.getInt(), delim.getString());
	}

	protected void setStringForGroup(String value)  {
		if ((this.attr & (DOUBLE)) != 0 && (this.attr & (PICTURE)) == 0) {
			set(atod(value) / Math.pow(10, exp));
		} else
			set(value);
		this.value = new StringBuilder(value);
	}
	
	
	private String overstamp(String value)  {
		// take the last char
		char lastChar = value.charAt(value.length() - 1);
		// check its value
		int lastCharValue = lastChar - '0';
		// prepare new value
		char newChar = (char) ('p' + lastCharValue);
		// return result
		return value.substring(0, value.length() - 1) + newChar;
	}
	
	private String overstampPositives(String value) {
		return value;
		/*
		if(value.length() == 0)
			return value;
		char lastChar = value.charAt(value.length() - 1);
		// check its value
		int lastCharValue = lastChar - '0';
		// prepare new value
		char newChar;
		if(lastCharValue == 0)
			newChar = '{';
		else
			newChar = (char) ( 'A' + lastCharValue - 1);
		
		// return result
		return value.substring(0, value.length() - 1) + newChar;
		*/
	}

	protected double lincStrToDouble(String s) {
		return lincStrToDouble(s, displayLength, scale, false);
	}
	
	protected double lincStrToDouble(String s, boolean implicitDecimals) {
		return lincStrToDouble(s, displayLength, scale, true);
	}

	protected double lincStrToDouble(byte [] b, boolean implicitDecimals) {
		String s = new String(b);
		return lincStrToDouble(s, displayLength, scale, true);
	}
	
	protected double lincStrToDouble(String s, int len, int decimals) {
		return lincStrToDouble(s, len, decimals, false);
	}

	protected double lincStrToDouble(String s, int len, int decimals, boolean implicitDecimal) {
	    if(s == null || s.length() < 1) return 0;
		StringBuilder sb;
		
		sb = new StringBuilder(s);

		//If we have a sign, process as normal string
		int i = sb.indexOf("-");
		if(i != -1) {
			sb.deleteCharAt(i);
			while(sb.charAt(0) == ' ') {
				sb.deleteCharAt(0);
			}
			try {
				if((attr & (UNSIGNED)) != 0) return -f_chop(Double.valueOf(sb.toString()).doubleValue(),
						len, decimals);
				double d = -Double.valueOf(sb.toString()).doubleValue();
				d = f_chop(d, len, decimals);
				return d;
		    } catch (NumberFormatException e) { 
		    	return 0.0; 
		    }
		}

		boolean sign = false;
		int cval = -1;
		char c = sb.charAt(sb.length() - 1);
		
		/* These are the overstamp characters which can be found in UNISYS data
			0123456789
			pqrstuvwxy  - negative value
			 ABCDEFGHI  - positive value
			 JKLMNOPQR  - negative value
			  STUVWXYZ  - positive value
			}           - negative value
			{           - positive value 
		*/
		if(c >= 'p' && c <= 'y') {cval = c - 'p'; sign = true; }
		else if(c >= 'A' && c <= 'I') {cval = c - 'A' + 1; }
		else if(c >= 'J' && c <= 'R') {cval = c - 'J' + 1; sign = true; }
		else if(c >= 'S' && c <= 'Z') {cval = c - 'S' + 2; }
		else if(c == '}') {cval = 0; sign = true; }
		else if(c == '{') {cval = 0;}
		
		if(cval != -1) {
		    c = (char)('0' + cval);
		    sb.deleteCharAt(sb.length() - 1);
		    sb.append(c);
		}
		
		//Adjust decimals
	    int idx = sb.indexOf(".");
	    if(idx != -1) {
	    	if(decimals  == 0) sb.delete(idx, sb.length());
	    	else if(decimals < scale) sb.delete(sb.length() - (scale - decimals), sb.length());
	    	else if((sb.length() - idx - 1) > decimals)
	    		sb.delete(idx + decimals + 1, sb.length());
	    } else {
	    	if(implicitDecimal) {
	    		while(sb.length() < scale) sb.insert(0, '0');
	    		if(scale > 0) sb.insert(sb.length() - scale, '.');
	    		if(scale > decimals) sb.delete(sb.length() - (scale - decimals), sb.length());
	    	} 
	    }
		if((sb.length() - 1) > len)
			sb.delete(0, sb.length() - len - 1);
		
		//convert to double
		double dv;
		try {
			dv = Double.valueOf(sb.toString()).doubleValue();
			dv = f_chop(dv, len, decimals);
	    } catch (NumberFormatException e) { dv = 0.0; }
		if(sign && (attr & (UNSIGNED)) == 0) return -dv;
		return dv;
	}

	protected double anyStrToDouble(String s) {
	    if(s == null || s.length() < 1) return 0;
		StringBuilder sb = new StringBuilder(s);
	    boolean sign = false;
		int start;
		int end = sb.length();
		
		for(start=0;start < sb.length(); start++) {
			char c = sb.charAt(start);
		    if(!Character.isWhitespace(c)) break;
			
//			if(!Character.isWhitespace(sb.charAt(start))) break;
		}
	    for(;end > start ; end--) if(!Character.isWhitespace(sb.charAt(end-1))) break;
		if(sb.charAt(start) == '-') {
			sign = true;
			start++;
		} else if(sb.charAt(end-1) == '-') {
			sign = true;
			end--;
		}
		
		int dec = this.scale;
		int cval = -1;
		char c = sb.charAt(end - 1);

	    int i = sb.indexOf(".");
		if(c >= 'p' && c <= 'y') {cval = c - 'p'; sign = true; }
		else if(c >= 'A' && c <= 'I') {cval = c - 'A' + 1; }
		else if(c >= 'J' && c <= 'R') {cval = c - 'J' + 1; sign = true; }
		else if(c >= 'S' && c <= 'Z') {cval = c - 'S' + 2; }
		else if(c == '}') {cval = 0; sign = true; }
		else if(c == '{') {cval = 0;}
		
		if(cval != -1) {
			c = (char)('0' + cval);
			sb.replace(end - 1, end, Character.toString(c));
		}
		
		String sval;
		if(i == -1) {
			if(end > (displayLength - dec)) sval = sb.substring(end - (displayLength - dec), end);
			else sval = sb.substring(start, end);
		} else {
			if(i > (displayLength - dec)) sval = sb.substring(i - (displayLength - dec), i);
			else sval = sb.substring(start, i);
			if(dec < (end - i - 1)) sval += sb.substring(i, i+dec+1);
			else sval += sb.substring(i, end);
			dec = 0;
		}
		//convert to double
		double dv;
		try {
			dv = Double.valueOf(sval).doubleValue();
		} catch (NumberFormatException e) { dv = 0.0; }
		for(;dec>0;dec--) dv /= 10;
		if(sign && (attr & (UNSIGNED)) == 0) return -dv;
		return dv;
	}

	protected long lincStrToLong(String s) {
		return lincStrToLong(s, displayLength, CHAR);
	}
	
	/**
	 * Converts a Unisys COBOL (or LINC) style numeric string into a long.
	 * If the length parameter is shorter than the source string, the value will be
	 * truncated. If the source datatype is numeric then truncation will be from the
	 * right, or else it will be truncated from the left.
	 * @param s The String to convert
	 * @param len The length of the receiving variable
	 * @param dtype The datatype attribute of the source variable (LONG, DOUBLE or CHAR)
	 * @return
	 */
	protected long lincStrToLong(String s, int len, int dtype) {
	    if(s == null || s.length() < 1) return 0;
		StringBuilder sb;
//		if(s.length() > (length + dec)) sb = new StringBuilder(s.substring(0, length+dec));
		sb = new StringBuilder(s);
		int idx;

		if(sb.indexOf("-") != -1) {
		    idx = sb.indexOf(".");
		    if(idx != -1) sb.delete(idx, sb.length());
			try {
				if((attr & (UNSIGNED)) != 0) return -Long.valueOf(sb.toString()).longValue();
				return Long.valueOf(sb.toString()).longValue();
		    } catch (NumberFormatException e) { return 0; }
		}

		boolean sign = false;
		int cval = -1;
		char c = sb.charAt(sb.length() - 1);
		
		if(c >= 'p' && c <= 'y') {cval = c - 'p'; sign = true; }
		else if(c >= 'A' && c <= 'I') {cval = c - 'A' + 1; }
		else if(c >= 'J' && c <= 'R') {cval = c - 'J' + 1; sign = true; }
		else if(c >= 'S' && c <= 'Z') {cval = c - 'S' + 2; }
		else if(c == '}') {cval = 0; sign = true; }
		else if(c == '{') {cval = 0;}
		else if(c == ' ') {cval = 0;}
		
	    //Replace last signed digit with normal digit, if necessary
		if(cval != -1) {
			c = (char)('0' + cval);
			sb.deleteCharAt(sb.length() - 1);
			sb.append(c);
		}
		
		//remove decimals
	    idx = sb.indexOf(".");
	    if(idx != -1) sb.delete(idx, sb.length());
	    else if(scale > 0) sb.delete(sb.length() - scale, sb.length());
	    
	    //shorten string, if necessary.
	    if(sb.length() > len) {
		    if((dtype & LONG) != 0 || (dtype & DOUBLE) != 0) { 
		    	//Truncate from left
		    	sb.delete(0, sb.length() - len);
		    } else {
		    	//Truncate from right
		    	sb.delete(len, sb.length());
		    }
		    if((attr & NUMERIC) != 0)
		    	attr |= OVERFLOW;
	    }
	    
		//convert to long
		long lv;  
		try {
			lv = Long.valueOf(sb.toString()).longValue();
		} catch (NumberFormatException e) {
			lv = toNumber(sb.toString());
		}
		if(sign && (dtype & UNSIGNED) == 0) return -lv;
		return lv;
	}
	
	protected long toNumber(String s) {
//		s = s.trim();
//		if(s.length() == 0) s = "0";
		StringBuilder os = new StringBuilder(s);
		StringBuilder ns = new StringBuilder();
		long lv=0;
		for(int i = 0; i < os.length(); i++) ns.append(charToNumber(os.charAt(i)));
		try {
			lv = Long.valueOf(ns.toString());
		} catch (NumberFormatException e) { lv = 0; }
		return lv;
	}
	protected char charToNumber (char c){
	    switch(c) {
        case ' ': return '0';
        case '!': return '1';
        case '"': return '2';
        case '#': return '3';
        case '$': return '4';
        case '%': return '5';
        case '&': return '6';
        case '\'': return '7';
        case '(': return '8';
        case ')': return '9';
        case '*': return '0';
        case '+': return '0';
        case ',': return '0';
        case '-': return '0';
        case '.': return '0';
        case '/': return '0';
        case '0': return '0';
        case '1': return '1';
        case '2': return '2';
        case '3': return '3';
        case '4': return '4';
        case '5': return '5';
        case '6': return '6';
        case '7': return '7';
        case '8': return '8';
        case '9': return '9';
        case ':': return '0';
        case ';': return '0';
        case '<': return '0';
        case '=': return '0';
        case '>': return '0';
        case '?': return '0';
        case '@': return '0';
        case 'A': return '1';
        case 'B': return '2';
        case 'C': return '3';
        case 'D': return '4';
        case 'E': return '5';
        case 'F': return '6';
        case 'G': return '7';
        case 'H': return '8';
        case 'I': return '9';
        case 'J': return '0';
        case 'K': return '0';
        case 'L': return '0';
        case 'M': return '0';
        case 'N': return '0';
        case 'O': return '0';
        case 'P': return '0';
        case 'Q': return '1';
        case 'R': return '2';
        case 'S': return '3';
        case 'T': return '4';
        case 'U': return '5';
        case 'V': return '6';
        case 'W': return '7';
        case 'X': return '8';
        case 'Y': return '9';
        case 'Z': return '0';
        case '[': return '0';
        case '\\': return '0';
        case ']': return '0';
        case '^': return '0';
        case '_': return '0';
        case '`': return '0';
        case 'a': return '1';
        case 'b': return '2';
        case 'c': return '3';
        case 'd': return '4';
        case 'e': return '5';
        case 'f': return '6';
        case 'g': return '7';
        case 'h': return '8';
        case 'i': return '9';
        case 'j': return '0';
        case 'k': return '0';
        case 'l': return '0';
        case 'm': return '0';
        case 'n': return '0';
        case 'o': return '0';
        case 'p': return '0';
        case 'q': return '1';
        case 'r': return '2';
        case 's': return '3';
        case 't': return '4';
        case 'u': return '5';
        case 'v': return '6';
        case 'w': return '7';
        case 'x': return '8';
        case 'y': return '9';
        case 'z': return '0';
        case '{': return '0';
        case '|': return '0';
        case '}': return '0';
        case '~': return '0';
        default: return '0';
    }
	}
	
	

	protected String longToLincString(long val, boolean rz) {
	    boolean sign;
	    int tlen = displayLength;
	    if(val < 0) {
	        if((attr & (UNSIGNED)) == 0) sign = true;
			else sign = false;
	        val = -val;
	    } else sign = false;
//	    for(int dec = this.dec; dec > 0; dec--) val *= 10;
	    //BigInteger bd = new BigInteger(val).setScale(0, BigDecimal.ROUND_DOWN);
	    //String s = bd.toString();
		String s = Long.toString(val);
//		if(!attr(UNSIGNED)) tlen--;
	    //Apply overstamp
	    if(rz && s.length() < tlen) s = zeros.substring(0,tlen - s.length()) + s;
	    else if(s.length() > tlen) s = s.substring(s.length() - tlen);

	    if(sign) s = overstamp(s);
		else if((attr & (UNSIGNED)) == 0)
			s = overstampPositives(s);

	    
	    return s;
	}

	protected String doubleToLincString(double val, boolean rz) {
		boolean sign = false;
		int tlen = displayLength;

		if(val < 0.0) {
			if((attr & (UNSIGNED)) == 0) sign = true;
			val = -val;
		}
	    for(int dec = this.scale; dec > 0; dec--) val *= 10;

		//truncate the decimal
		BigDecimal bd = new BigDecimal(f_chop(val,tlen + scale,0)).setScale(0, BigDecimal.ROUND_HALF_DOWN);
		String s = bd.toString();
//		if(!attr(UNSIGNED)) tlen--;
		
		if(rz && s.length() < tlen) s = zeros.substring(0,tlen - s.length()) + s;
		else if(s.length() > tlen) {
			s = s.substring(s.length() - tlen);
			attr |= OVERFLOW;
		}
		//Apply overstamp
		if(sign) s = overstamp(s);
		else if((attr & (UNSIGNED)) == 0)
			s = overstampPositives(s);
		
		return s;
	}
	
	private String makeSpaces(int numberOfSpaces)  {
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < numberOfSpaces; i++)
			sb.append(" ");
		return sb.toString();
	}
	
	//If the string is too long, truncate from rhs for CHAR and LHS for number.
	//If the string is too short, pad the left with the pad character.
	//If stamp is true then negative numbers are overstamped, otherwise a leading sign is used.
	protected String adjustLengthLeft(String s, int len, char pad, boolean stamp) {
		if(s.length() == len) return s;
		else if(s.length() > len) {
			if((attr & (NUMERIC)) != 0)  return s.substring(s.length() - len);
			return s.substring(0, len);
		} else {
			StringBuilder sb = new StringBuilder();
			if(pad == '0') {
				int idx = s.indexOf("-");
				if(idx != -1) {
					s = s.substring(idx + 1);
					if(stamp) s = overstamp(s);
					else {
						sb.append('-');
						len--;
					}
				} else {
					if(stamp) s = overstampPositives(s);
				}
			}
			
			for(int i=0; i<(len - s.length()); i++) sb.append(pad);
			sb.append(s);
			return sb.toString();
		}
	}
	
	//If the string is too long, truncate from rhs.
	//If the string is too short, pad the right with spaces.
	protected String adjustLengthRight(String s, int len) {
		if(s == null) s = "";
		//System.out.println("spaces.lenght=" + spaces.length());
		if(s.length() == len) return s;
		else if(s.length() > len) return s.substring(0, len);
		else return s + spaces.substring(0, len - s.length());
	}

	/**
	 * replace all occurences of string by another string
	 * 
	 * @param from
	 *            String to be replaced
	 * @param to
	 *            Replacement String 
	 */
	public void replaceAll(String from, String to) {
		int pointer = 0;
		StringBuilder orig = new StringBuilder(getString());
		StringBuilder result = new StringBuilder();
		if(from.equals("")) from =  " ";
		if(to.equals("")) to =  Strings.rpad(" ",from.length(),' ');
		if(from.length() != to.length()) return;
		pointer = orig.indexOf(from);
		while(pointer >= 0) {
			result.append(orig.substring(0,pointer));
			result.append(to);
			result.append(orig.substring(pointer + to.length()));
			//log.debug("result=" + result.toString());
			pointer = orig.indexOf(from,pointer + to.length());
			orig.delete(0, orig.length());
			orig.append(result);
			result.delete(0, result.length());
		}
		if((attr & (PICTURE)) != 0) {
			StringBuilder sb = (StringBuilder)value;
			sb.replace(0, sb.length(), orig.toString());
		} else set(orig.toString());
	}
	
	/**
	 * replace all occurences of string by another string
	 * 
	 * @param from
	 *            String to be replaced
	 * @param to
	 *            Replacement String 
	 */
	public void replaceAll(Var from, String to) {
		replaceAll(from.getString(), to);
	}
	
	/**
	 * replace all occurences of string by another string
	 * 
	 * @param from
	 *            String to be replaced
	 * @param to
	 *            Replacement String 
	 */
	public void replaceAll(String from, Var to) {
		replaceAll(from, to.getString());
	}
	/**
	 * replace all occurences of string by another string
	 * 
	 * @param from
	 *            String to be replaced
	 * @param to
	 *            Replacement String 
	 */
	public void replaceAll(Var from, Var to) {
		replaceAll(from.getString(), to.getString());
	}
	public void replaceAll(String from, int to) {
		replaceAll(from, String.valueOf(to));
	}
	/**
	 * replace all occurences of string by another string
	 * 
	 * @param from
	 *            String to be replaced
	 * @param to
	 *            Replacement String 
	 */
	public void replaceAll(Var from, int to) {
		replaceAll(from.getString(), String.valueOf(to));
	}
	/**
	 * replace first occurrence of one string by another string
	 * 
	 * @param from
	 *            String to be replaced
	 * @param to
	 *            Replacement string
	 */
	public void replaceFirst(String from, String to) {
		int pointer = 0;
		StringBuilder orig = new StringBuilder(getString());
		StringBuilder result = new StringBuilder();
		if(from.equals("")) from =  " ";
		if(to.equals("")) to =  Strings.rpad(" ",from.length(),' ');
		if(from.length() != to.length()) return;
		pointer = orig.indexOf(from);
		if(pointer < 1) return;
		result.append(orig.substring(0,pointer));
		result.append(to);
		result.append(orig.substring(pointer + to.length()));
		set(result.toString());
	}
	/**
	 * replace first occurrence of one string by another string
	 * 
	 * @param from
	 *            String to be replaced
	 * @param to
	 *            Replacement string
	 */
	public void replaceFirst(Var from, String to) {
		replaceFirst(from.getString(), to);
	}
	/**
	 * replace first occurrence of one string by another string
	 * 
	 * @param from
	 *            String to be replaced
	 * @param to
	 *            Replacement string
	 */
	public void replaceFirst(String from, Var to) {
		replaceFirst(from, to.getString());
	}
	/**
	 * replace first occurrence of one string by another string
	 * 
	 * @param from
	 *            String to be replaced
	 * @param to
	 *            Replacement string
	 */
	public void replaceFirst(Var from, Var to) {
		replaceFirst(from.getString(), to.getString());
	}
	/**
	 * replace all leading occurences of one string by another string
     *
	 * @param from
	 *            String to be replaced
	 * @param to
	 *            Replacement string 
	 */
	public void replaceLeading(String from, String to) {
		int pointer = 0;
		int count = 0;
		StringBuilder orig = new StringBuilder(getString());
		StringBuilder result = new StringBuilder();
		if(from.equals("")) from =  " ";
		if(to.equals("")) to =  Strings.rpad(" ",from.length(),' ');
		if(from.length() != to.length()) return;
		pointer = orig.indexOf(from);
		if(pointer != 0) return;
		while(true) {
			result.append(orig.substring(0,pointer));
			result.append(to);
			result.append(orig.substring(pointer + to.length()));
			count++;
			orig.delete(0, orig.length());
			orig.append(result);
			result.delete(0, result.length());
			pointer = orig.indexOf(from,pointer + from.length());
			if(pointer != from.length() * count) break;
		}
		set(orig.toString());
	}
	/**
	 * replace all leading occurences of one string by another string
     *
	 * @param from
	 *            String to be replaced
	 * @param to
	 *            Replacement string 
	 */
	public void replaceLeading(Var from, String to) {
		replaceLeading(from.getString(), to);
	}
	/**
	 * replace all leading occurences of one string by another string
     *
	 * @param from
	 *            String to be replaced
	 * @param to
	 *            Replacement string 
	 */
	public void replaceLeading(String from, Var to) {
		replaceLeading(from, to.getString());
	}
	/**
	 * replace all leading occurences of one string by another string
     *
	 * @param from
	 *            String to be replaced
	 * @param to
	 *            Replacement string 
	 */
	public void replaceLeading(Var from, Var to) {
		replaceLeading(from.getString(), to.getString());
	}
	/**
	 * replace all leading occurences of one string by another string
     *
	 * @param from
	 *            String to be replaced
	 * @param to
	 *            Replacement string 
	 */
	public void replaceLeading(int from, String to) {
		replaceLeading(""+from, to);
	}
	/**
	 * replace characters from beginning to specified position
     *
	 * @param to
	 *            End position
	 * @param repChar
	 *            Replacement character 
	 */
	public void replaceCharactersBeforeInitial(String pchar, String repChar) {
		int startPos=getString().indexOf(pchar);
		if (startPos == -1)
			set("");
		else
			replaceCharacters(0,startPos -1,repChar);
	}
	/**
	 * replace characters from beginning to specified position
     *
	 * @param to
	 *            End position
	 * @param repChar
	 *            Replacement character 
	 */
	public void replaceCharactersAfterInitial(String pchar, String repChar) {
		int startPos=getString().indexOf(pchar);
		if (startPos == -1) return;
		replaceCharacters(startPos + 1,getLen() - 1,repChar);
	}
	/**
	 * replace characters specified 
     *
	 * @param from
	 *            Start position
	 * @param to
	 *            End position
	 * @param repChar
	 *            Replacement character 
	 */
	public void replaceCharacters(int from, int to, String repChar) {
		StringBuilder orig = new StringBuilder(getString());
		StringBuilder result = new StringBuilder();
		if(repChar.equals("")) repChar =  " ";
		if(to < from) return;
		result.append(orig.substring(0, from));
		for(int i=from;i<=to;i++) result.append(repChar);
		result.append(orig.substring(Math.min(to + 1,displayLength)));
		set(result.toString());
	}
	/**
	 * Count all occurences of a string
	 * 
	 * @param lit
	 *            String to be counted
	 * @return count of occurences
	 */
	public long countAll(String ... lits) {
		int pointer = 0;
		int res = 0;
		StringBuilder orig = new StringBuilder(getString());
		for(String lit : lits) {
			if(lit.equals("")) lit =  " ";
			pointer = orig.indexOf(lit);
			while(pointer >= 0) {
				res++;
				pointer = orig.indexOf(lit,pointer + lit.length());
			}
		}
		return res;
	}
	/**
	 * Count all occurences of a string
	 * 
	 * @param lit
	 *            String to be counted
	 * @return count of occurences
	 */
	public long countAll(Var ... lits) {
		String [] litArr = new String[lits.length];
		int i=0;
		for(Var lit : lits) {
			litArr[i++]=lit.getString();
		}
		
		return(countAll(litArr));
	}
	/**
	 * Count leading occurences of a string
	 * 
	 * @param lit
	 *            String to be counted
	 * @return count of occurences
	 */
	public long countLeading(String lit) {
		int pointer = 0;
		int res = 0;
		StringBuilder orig = new StringBuilder(getString());
		if(lit.equals("")) lit =  " ";
		pointer = orig.indexOf(lit);
		if(pointer != 0) return 0;
		while(true) {
			res++;
			pointer = orig.indexOf(lit,pointer + lit.length());
			if(pointer != lit.length() * res) break;
		}
		return res;
	}
	/**
	 * Count leading occurences of a string
	 * 
	 * @param lit
	 *            String to be counted
	 * @return count of occurences
	 */
	public long countLeading(Var lit) {
		return(countLeading(lit.getString()));
	}
	/**
	 * Count leading occurences of a string
	 * 
	 * @param lit
	 *            String to be counted
	 * @return count of occurences
	 */
	public long countLeading(int lit) {
		return(countLeading(String.valueOf(lit)));
	}
	/**
	 * Get part of a Var 
	 * 
	 * @param delim
	 *            delimited
	 * @param lit
	 *            String to be counted
	 * @return Var up to delimiter
	 */
	public String getDelim(String delim) {
//		System.out.println("Var getDelim(String delim): ["+delim+"]");
		if("".equals(delim)) delim = " ";
		String s = new String(getBytes());
		int start = s.indexOf(delim);
		//log.debug("start=" + start);
		//log.debug("delim>" + delim + "<");
		if(start == -1) return s;
		return s.substring(0,start);
	}
	
	/**
	 * Get part of a Var 
	 * 
	 * @param delim
	 *            delimited
	 * @param lit
	 *            String to be counted
	 * @return Var up to delimiter
	 */
	public String getDelim(Var delim) {
//		System.out.println("Var getDelim(Var delim): ["+delim.getString()+"]");
		return(getDelim(delim.getString()));
	}
	/**
	 * Overwrite part of a Var 
	 * 
	 * @param ptr
	 *            1st character to be overwritten
	 * @param string
	 *            String to be used to overwrite
	 * @return ptr + length of string
	 */
	public int setPart(int ptr,String string) {
		if(ptr > displayLength) return ptr;
		int lPtr=ptr - 1;
		if(string.equals("")) string =  " ";
		if(lPtr + string.length() < displayLength) {
			set(getString().substring(0,lPtr) + string + getString().substring(lPtr + string.length()));
		} else 
			set(getString().substring(0,lPtr) + string);
		return(min((lPtr + string.length() + 1),(displayLength + 1)));
	}
	/**
	 * Overwrite part of a Var 
	 * 
	 * @param ptr
	 *            1st character to be overwritten
	 * @param string
	 *            String to be used to overwrite
	 * @return ptr + length of string
	 */
	public int setPart(int ptr,Var string) {
		return(setPart(ptr,string.getString()));
	}
	/**
	 * Overwrite part of a Var 
	 * 
	 * @param ptr
	 *            1st character to be overwritten
	 * @param string
	 *            String to be used to overwrite
	 * @return ptr + length of string
	 */
	public int setPart(Var ptr,Var string) {
		return(setPart(ptr.getInt(),string.getString()));
	}
	/**
	 * Overwrite part of a Var 
	 * 
	 * @param ptr
	 *            1st character to be overwritten
	 * @param string
	 *            String to be used to overwrite
	 * @return ptr + length of string
	 */
	public int setPart(Var ptr,String string) {
		return(setPart(ptr.getInt(),string));
	}
	/**
	 * Unstring a var from a pointer 
	 * 
	 * @param ptr
	 *            Position to start from.
	 * @param delimiter
	 *            Delimiter to be used (may be regex).
	 * @param vlist
	 *            Array of targets.
	 * @return Array of counts and delimiters.
	 */
	public String [][] oldunstringDelimFrom(Var ptr, String delimiter, Var ... vlist) {
		int lPtr = ptr.getInt();
		int savePtr = 0;
		String [][] counts = new String[2][vlist.length];
		if(delimiter.equals("")) delimiter = " ";
		String [] s = getString().substring(lPtr - 1).split(delimiter); 
		//log.debug("0=" + s[0]);
		//log.debug("1=" + s[1]);
		//log.debug("2=" + s[2]);
		//Put the values into the var list: 
		int i; 
		for(i = 0; i < s.length && i < vlist.length; i++) { 
			vlist[i].set(s[i]);
			counts[0][i] = "" + s[i].length();
			if (i < s.length - 1) {
				savePtr = lPtr;
				lPtr = getString().indexOf(s[i + 1], lPtr - 1 + s[i].length()) + 1;
				counts[1][i] = getString().substring(savePtr - 1 + s[i].length(), lPtr - 1);
			} else counts[1][i] = "";
		} 
		//If there were more vars than strings, clear them: 
		for( ; i < vlist.length; i++) { 
			vlist[i].clear(); 
			counts[0][i] = "0";
			counts[1][i] = "";
		} 
		ptr.set(lPtr);
		return counts;
	} 
	/**
	 * Unstring a var from a pointer 
	 * 
	 * @param ptr
	 *            Position to start from.
	 * @param delimiter
	 *            Delimiter to be used (may be regex).
	 * @param vlist
	 *            Array of targets.
	 * @return Array of counts and delimiters.
	 */
	public String [][] unstringDelimFrom(Var ptr, ArrayList<String> delimiters, Var ... vlist) {
//		if(vlist.length > 0)
//			throw new IllegalArgumentException("TEMPORARY UNAVAILABLE METHOD");
		int lPtr = ptr.getInt();
		int savePtr = 0;
		String [][] counts = new String[3][vlist.length];
		String pattern = "";
		String delimiter = "";
		for (int i=0; i < delimiters.size() ; i++) {
			delimiter=delimiters.get(i);
			if(i>0) pattern+="|";
			if(delimiter.equals("")) pattern += " ";
			else if (delimiter.endsWith("~AlL~")) {
				pattern += Pattern.quote(delimiter.substring(0,delimiter.indexOf("~AlL~"))) + "+";
			} else pattern += Pattern.quote(delimiter);
		}
		String string=getString();
		Pattern p = Pattern.compile(pattern);
		if(vlist.length == 1) { //Most case just have one result.  So to optimise
		   Matcher m = p.matcher(string.substring(lPtr - 1));
		   if(m.find() && m.start() < string.length() - 1 ) {
	          // Text since last match
			   //System.out.println("lPtr=" + lPtr);
			   //System.out.println("m.start()=" + m.start());
			   //System.out.println("m.end()=" + m.end());
			   //System.out.println("string.length=" + string.length());
			  
			   vlist[0].set(string.substring(lPtr - 1,m.start() + lPtr - 1));
			   // The delimiter itself
			   counts[1][0]=m.group();
			   counts[0][0]="" + string.substring(lPtr,m.start() + lPtr).length();
			   ptr.set(lPtr + m.end());
		   }else {
			   vlist[0].set(string.substring(lPtr - 1));
			   counts[1][0]="";
			   counts[0][0]="" + string.substring(lPtr - 1).length();
			   ptr.set(string.length() + 1);
		   }
		   counts[2][0]="1";
		   return counts;
		}
		String [] s = p.split(string.substring(lPtr - 1));
		
		//Put the values into the var list: 
		int i; 
		for(i = 0; i < s.length && i < vlist.length; i++) { 
			vlist[i].set(s[i]);
			counts[0][i] =  Integer.toString(s[i].length());
			if (i < s.length - 1) {
				savePtr = lPtr;
				lPtr = string.indexOf(s[i + 1], lPtr - 1 + s[i].length()) + 1;
				counts[1][i] = string.substring(savePtr - 1 + s[i].length(), lPtr - 1);
			} else {
				counts[1][i] = "";
				ptr.set(string.length() + 1);
			}
		} 
		counts[2][0] = String.valueOf(i);
		//If there were more vars than strings, clear them: 
		for( ; i < vlist.length; i++) { 
			vlist[i].clear(); 
			counts[0][i] = "0";
			counts[1][i] = "";
		} 
		ptr.set(lPtr);
		return counts;
	} 

	/**
	 * Unstring a var from a pointer 
	 * 
	 * @param ptr
	 *            Position to start from.
	 * @param delimiter
	 *            Delimiter to be used (may be regex).
	 * @param vlist
	 *            Array of targets.
	 * @return Array of and delimiters.
	 */
	public String [][] unstringDelimFrom(Var ptr, Var delimiter, Var ... vlist) {
		return(unstringDelimFrom(ptr,delimiter, vlist)); 
	} 
	/**
	 * Unstring a var from a pointer 
	 * 
	 * @param ptr
	 *            Position to start from.
	 * @param delimiter
	 *            Delimiter to be used (may be regex).
	 * @param vlist
	 *            Array of targets.
	 * @return Array of and delimiters.
	 */
	public String [][] unstringDelimFrom(int ptr, Var delimiter, Var ... vlist) { 
		return(unstringDelimFrom(new Var(ptr),delimiter, vlist)); 
	} 
	/**
	 * Unstring a var from a pointer 
	 * 
	 * @param ptr
	 *            Position to start from.
	 * @param delimiter
	 *            Delimiter to be used (may be regex).
	 * @param vlist
	 *            Array of targets.
	 * @return Array of and delimiters.
	 */
	public String [][] unstringDelimFrom(Var ptr, String delimiter, Var ... vlist) {
		ArrayList<String> delimiters = new ArrayList<String>();
		delimiters.add(delimiter);
		return(unstringDelimFrom(ptr,delimiters, vlist)); 
	} 
	/**
	 * Unstring a var from a pointer 
	 * 
	 * @param ptr
	 *            Position to start from.
	 * @param delimiter
	 *            Delimiter to be used (may be regex).
	 * @param vlist
	 *            Array of targets.
	 * @return Array of and delimiters.
	 */
	public String [][] unstringDelimFrom(int ptr, String delimiter, Var ... vlist) { 
		ArrayList<String> delimiters = new ArrayList<String>();
		delimiters.add(delimiter);
		return(unstringDelimFrom(new Var(ptr),delimiters, vlist)); 
	} 
	/**
	 * Unstring a var from a pointer 
	 * 
	 * @param ptr
	 *            Position to start from.
	 * @param delimiter
	 *            Delimiter to be used (may be regex).
	 * @param vlist
	 *            Array of targets.
	 * @return Array of and delimiters.
	 */
	public String [][] unstringDelimFrom(int ptr, ArrayList<String> delimiter, Var ... vlist) { 
		return(unstringDelimFrom(new Var(ptr),delimiter, vlist)); 
	} 
	/**
	 * Unstring a var from a pointer 
	 * 
	 * @param ptr
	 *            Position to start from.
	 * @param delimiter
	 *            Delimiter to be used (may be regex).
	 * @param vlist
	 *            Array of targets.
	 * @return Array of and delimiters.
	 */
	public String [][] unstringDelim(ArrayList<String> delimiter, Var ... vlist) {
		return(unstringDelimFrom(1,delimiter, vlist));
	} 

	public String [][] unstringDelim(String [] delimiter, Var ... vlist) {
		ArrayList<String> delim = new ArrayList<String>(delimiter.length);
		for(int i=0; i<delimiter.length; i++)
			delim.add(delimiter[i]);
		return(unstringDelimFrom(1,delim, vlist));
	} 
	/**
	 * Unstring a var from a pointer 
	 * 
	 * @param ptr
	 *            Position to start from.
	 * @param delimiter
	 *            Delimiter to be used (may be regex).
	 * @param vlist
	 *            Array of targets.
	 * @return Array of and delimiters.
	 */
	public String [][] unstringDelim(Var delimiter, Var ... vlist) { 
		return(unstringDelimFrom(1,delimiter, vlist)); 
	} 
	
	/**
	 * Unstring a var from a pointer 
	 * 
	 * @param ptr
	 *            Position to start from.
	 * @param delimiter
	 *            Delimiter to be used (may be regex).
	 * @param vlist
	 *            Array of targets.
	 * @return Array of and delimiters.
	 */
	public String [][] unstringDelim(String delimiter, Var ... vlist) { 
		return(unstringDelimFrom(1,delimiter, vlist)); 
	} 
	
	/**
	 * Unstring a var from a pointer 
	 * 
	 * @param ptr
	 *            Position to start from.
	 * @param vlist
	 *            Array of targets and for counts.
	 */
	
	public void unstringForFrom (int ptr, Var ... vlist) {
		int i; 
		for(i = 0; i < vlist.length; i++) {
			vlist[i].set(this.substr(ptr - 1, vlist[++i]));
			ptr += vlist[i].getInt();
		} 
	}
	/**
	 * Unstring a var from a pointer 
	 * 
	 * @param ptr
	 *            Position to start from.
	 * @param vlist
	 *            Array of targets and for counts.
	 */
	public void unstringForFrom (Var ptr, Var ... vlist) {
		unstringForFrom (ptr.getInt(), vlist);
	}
	
	/**
	 * Unstring a var from a pointer 
	 * 
	 * @param vlist
	 *            Array of targets and for counts.
	 */
	public void unstringFor (Var ... vlist) {
		unstringForFrom (1, vlist);
	}
	
	/**
	 * Count number of characters before string or strings
	 * 
	 * @param delims
	 *            Array of delimiters.
	 * @return number of characters.
	 */
	private int checkReturn(int ret) {
		return (ret>0?ret:getLen());
	}
	public int unstringTallyChars(Object ... delims) {
		
		String sdelim;
		Var vdelim;
		int idelim;
		String regex;
		if(delims.length == 1) {
			if(delims[0] instanceof String) {
				sdelim=(String)delims[0];
				return checkReturn(getString().indexOf(sdelim));
			} else if(delims[0] instanceof Integer) {
				idelim=(Integer)delims[0];
				return checkReturn(getString().indexOf(idelim));
			} else if(delims[0] instanceof Var) {
				vdelim=(Var)delims[0];
				return checkReturn(getString().indexOf(vdelim.getString()));
			}
		} else {
			regex="";
			for(int i=0;i<delims.length;i++) {
				if(delims[i] instanceof String) {
					sdelim=(String)delims[i];
					regex+=sdelim;
				} else if(delims[i] instanceof Integer) {
					idelim=(Integer)delims[i];
					regex+=idelim;
				} else if(delims[i] instanceof Var) {
					vdelim=(Var)delims[i];
					regex+=vdelim.getString();
				}
				if(i < delims.length - 1) regex+="|";
			}
			 Pattern p = Pattern.compile(regex);
	         Matcher m = p.matcher(getString());
	         if (m.find()) {
	             return m.start();
	         } else return getLen();
		}
		return 0;
	}
	
	/**
	 * Get defined length of Var
	 * 
	 * @return defined length
	 */
	public int getLen() {
		return displayLength;
	}
	
	public void setLen(int ilen) {
		displayLength= ilen;
	}
	
	/**
	 * Get the length of the string resulting from getString();. This will include space for a sign,
	 * leading spaces and decimal points.
	 * @return The printed length of a variable.
	 */
	public int getPrintedLen(String separator) {
		if((attr & (CHAR|GROUP|PICTURE)) != 0) return displayLength;
		int l = displayLength;
		if(separator != null && separator.length() > 0) {
			l += (((int)((l - scale - 1)/3)) * separator.length());
		}
		if(scale > 0) l++;
		if((attr & (UNSIGNED)) == 0) {
			if((attr & (DOLLAR)) != 0) l++;
			if((attr & (NEGCR)) != 0 || (attr & (NEGDR)) != 0) l += 2;
			else l++;
		}
		return l;
	}
	
	public boolean isInRange(int from, int to) {
		return(ge(from) && le(to));
	}
	public boolean isInRange(String from, String to) {
		return(ge(from) && le(to));
	}

	public Var format(String fmt) {
		picture = new Picture(fmt);
		String cval = getString();
		value = null;
		attr |= PICTURE;
		displayLength =  picture.length();
		scale = (short)picture.decimals();
		set(cval);
		
		return this;
	}
	
	public Var bwz() throws Exception {
		if((attr & (PICTURE)) == 0 && (attr & (NUMERIC)) == 0)
			throw new Exception("bwz() can only be applied to numeric or picture Group.");
		if((attr & (PICTURE)) == 0) {
			String nines = "99999999999999999999999999999999";
			String m;
			String d;
			if(scale > 0) {
				m = nines.substring(0,displayLength-scale) + ".";
				d = nines.substring(0, scale);
			} else {
				m = nines.substring(0, displayLength);
				d = "";
			}
			format(m + d);
		}
		attr |= ZEROBLANK;
		return this;
	}

	/**
	 * Used by EJB's to indicate the GUI type in use where the Webmanager/EJB componenet 
	 * is serving GUI controls.
	 * @param type one of the GUI item types from ParameterWrapper.
	 */
	public void setParameterType(int type){
		parameterType = type;
	}
	
	/**
	 * returns the parameter type the Var is representing when used with Webmanager/EJB
	 * scenario.
	 * @return A valid value from ParameterWrapper 0 --> Unknown
	 */
	public int getParameterType(){
		return parameterType;
	}

	/**
	 * Returns the value as a b
	 * @return
	 */
	public byte[] getBytes() {
		return getString().getBytes();
	}
	/**
	 * Returns the value as a String
	 * @return
	 * @throws UnsupportedEncodingException 
	 */
	public byte[] getBytes(String encoding) throws UnsupportedEncodingException {
		return getString().getBytes(encoding);
	}

	/**
	 * Sets the value of the Var by converting the byte array to a String using the default
	 * encoding.
	 * @param b The byte array.
	 */
	public Var set(byte[] b) {
		set(new String(b));
		return this;
	}
	
	public Var set(byte [] src, int from, int len) {
		set(new String(src, from, len));
		return this;
	}

	public Var set(byte [] src, int from, int len, String encoding) {
		try {
			set(new String(src, from, len, encoding));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return this;
	}

	
	/**
	 * Sets the value of the Var by converting the byte array to a String using the 
	 * encoding specified.
	 * @param b The byte array to convert
	 * @param encoding The encoding to use in the conversion
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public Var set(byte[] b, String encoding) throws UnsupportedEncodingException {
		set(new String(b, encoding));
		return this;
	}

	/**
	 * Join string s to the current var, ensuring that there is exactly one space between
	 * the strings. If the attached string is spaces, no join is made. If 'this' is only spaces
	 * or has not been assigned, no space is added.
	 * @param s The string to attach
	 * @return this
	 */
	public Var attachSpace(String s) {
		String v = getString().replaceAll(" +$", "");
		if(s == null) s = "";
		//else s = s.trim(); // Keep leading spaces
		else s = s.replaceAll(" +$", "");
		if(v.length() == 0) set(s);
		else if(s.length() == 0) ;
		else {
			set(v + " " + s);
		}
		return this;
	}
	
	public Var attachSpace(Var str) {
		//return attachSpace(str.toString());
		return attachSpace(str.getLincString());
	}

	public Var attach(String s) {
		String v = getString().replaceAll(" +$", "");
		if(s == null) s = "";
		//else s = s.trim(); //trim loses leading spaces Steve 27/04/2010
		else s = s.replaceAll(" +$", "");
		if(v.length() == 0) set(s);
		else if(s.length() == 0) ;
		else {
			set(v + s);
		}
		return this;
	}
	
	
	public Var attach(Var str) {
		//return attach(str.toString()); toString does a trim
		return attach(str.getLincString());
	}

	/**
	 * Var's have no name.
	 * @return  null
	 */
	public String getName() {
		return null;
	}
	
	
	/**
	 * Emulate Lincs MVA when moving from an array
	 * @param source The source array
	 * @param size The size of the array
	 * @return this
	 */
	public Var setFromArray (Var [] source, int size) {
		for(int i=0;i<size;i++) {
			setSubstr(source[i+1],(i * source[i].getLen()) + 1);
		}

		return this;
	}
	
	
	/**
	 * Used to Emulate Lincs MVA when moving to an array
	 * @param source The source Var
	 * @param i The array occurence being set
	 * @return this
	 */
	public Var setMva (Var source, int i) {
		 if ((attr & (DOUBLE)) != 0) {
			try {
				set(Double.parseDouble(source.substr((i * getLen()), getLen())) / powerFloat[scale]);
			} catch (NumberFormatException nfe) {
				set(source.substr((i * getLen()), getLen()));
			}
		} else {
			set(source.substr((i * getLen()), getLen()));
		}
		return this;
	}

	public Var descending() {
		attr |= ORDER_DESC;
		return this;
	}
	public Var desc() {
		attr |= ORDER_DESC;
		return this;
	}
	public Var integerOfDate() {
		int yyyy = Integer.valueOf(substr(0,4)).intValue();
		int mm = Integer.valueOf(substr(4,2)).intValue();
		int dd = Integer.valueOf(substr(6)).intValue();
		return(new Var(toJulian(dd, mm, yyyy, 1601) + 1));
	}
	public Var dateOfInteger() {
		DC d = toGregorian(getInt() - 1, 1601);
		return(new Var(d.year4 * 10000 + d.month * 100 + d.day));
	}

	public void setMinValue() {
		if((attr & (CHAR)) != 0 || (attr & (PICTURE)) != 0)
			set(spaces);
		else set(zeros);
	}
	
	public void setMaxValue() {
		StringBuilder sb = new StringBuilder();
		if((attr & (CHAR)) != 0 || (attr & (PICTURE)) != 0)
			for(int i=0;i<displayLength;i++) sb.append('~');
		else if(scale > 0) {
			for(int i=0;i<(displayLength-scale);i++) sb.append('9');
			sb.append('.');
			for(int i=(displayLength-scale);i<displayLength;i++) sb.append('9');
		} else
			for(int i=0;i<displayLength;i++) sb.append('9');
		set(sb.toString());
	}
	public Var complexMove(Var src, int srcPos, int srcLen, int destPos) {
		if(srcPos == 0 || srcLen == 0 || destPos == 0) return this;
		if(src.picture != null && (src.attr & (LONG|DOUBLE)) != 0) {
			String x = src.substr(srcPos - 1, srcLen) + src.picture.getExtraDecimalString();
			if(x.length() > src.displayLength)
				x = x.substring(x.length() - src.displayLength);
			setSubstr(x, destPos);
		} else 
			setSubstr(src.substr(srcPos - 1, srcLen), destPos);
		return this;
	}
	public Var complexMove(Var src, int srcPos, int srcLen, Var destPos) {
		return complexMove(src, srcPos, srcLen, destPos.getInt());
	}
	public Var complexMove(Var src, int srcPos, Var srcLen, Var destPos) {
		return complexMove(src, srcPos, srcLen.getInt(), destPos.getInt());
	}
	public Var complexMove(Var src, int srcPos, Var srcLen, int destPos) {
		return complexMove(src, srcPos, srcLen.getInt(), destPos);
	}
	public Var complexMove(Var src, Var srcPos, Var srcLen, Var destPos) {
		return complexMove(src, srcPos.getInt(), srcLen.getInt(), destPos.getInt());
	}
	public Var complexMove(Var src, Var srcPos, int srcLen, int destPos) {
		return complexMove(src, srcPos.getInt(), srcLen, destPos);
	}
	public Var complexMove(Var src, Var srcPos, Var srcLen, int destPos) {
		return complexMove(src, srcPos.getInt(), srcLen.getInt(), destPos);
	}
	public Var complexMove(Var src, Var srcPos, int srcLen, Var destPos) {
		return complexMove(src, srcPos.getInt(), srcLen, destPos.getInt());
	}
	public Var complexMove(Var src, Var destPos) {
		return complexMove(src, 1, src.maxLength(), destPos.getInt());
	}
	public Var complexMove(Var src, int destPos) {
		return complexMove(src, 1, src.maxLength(), destPos);
	}
	public Var complexMove(String src, int srcPos, int srcLen, int destPos) {
		return complexMove(new Var(src), srcPos, srcLen, destPos);
	}
	public Var complexMove(String src, int srcPos, int srcLen, Var destPos) {
		return complexMove(new Var(src), srcPos, srcLen, destPos.getInt());
	}
	public Var complexMove(String src, int srcPos, Var srcLen, Var destPos) {
		return complexMove(new Var(src), srcPos, srcLen.getInt(), destPos.getInt());
	}
	public Var complexMove(String src, int srcPos, Var srcLen, int destPos) {
		return complexMove(new Var(src), srcPos, srcLen.getInt(), destPos);
	}
	public Var complexMove(String src, Var srcPos, Var srcLen, Var destPos) {
		return complexMove(new Var(src), srcPos.getInt(), srcLen.getInt(), destPos.getInt());
	}
	public Var complexMove(String src, Var srcPos, int srcLen, int destPos) {
		return complexMove(new Var(src), srcPos.getInt(), srcLen, destPos);
	}
	public Var complexMove(String src, Var srcPos, Var srcLen, int destPos) {
		return complexMove(new Var(src), srcPos.getInt(), srcLen.getInt(), destPos);
	}
	public Var complexMove(String src, Var srcPos, int srcLen, Var destPos) {
		return complexMove(new Var(src), srcPos.getInt(), srcLen, destPos.getInt());
	}
	public Var complexMove(String src, Var destPos) {
		return complexMove(new Var(src), 1, src.length(), destPos.getInt());
	}
	public Var complexMove(String src, int destPos) {
		return complexMove(new Var(src), 1, src.length(), destPos);
	}
	public Var complexMove(long src, int srcPos, int srcLen, int destPos) {
		return complexMove(new Var(src), srcPos, srcLen, destPos);
	}
	public Var complexMove(long src, int srcPos, int srcLen, Var destPos) {
		return complexMove(new Var(src), srcPos, srcLen, destPos.getInt());
	}
	public Var complexMove(long src, int srcPos, Var srcLen, Var destPos) {
		return complexMove(new Var(src), srcPos, srcLen.getInt(), destPos.getInt());
	}
	public Var complexMove(long src, int srcPos, Var srcLen, int destPos) {
		return complexMove(new Var(src), srcPos, srcLen.getInt(), destPos);
	}
	public Var complexMove(long src, Var srcPos, Var srcLen, Var destPos) {
		return complexMove(new Var(src), srcPos.getInt(), srcLen.getInt(), destPos.getInt());
	}
	public Var complexMove(long src, Var srcPos, int srcLen, int destPos) {
		return complexMove(new Var(src), srcPos.getInt(), srcLen, destPos);
	}
	public Var complexMove(long src, Var srcPos, Var srcLen, int destPos) {
		return complexMove(new Var(src), srcPos.getInt(), srcLen.getInt(), destPos);
	}
	public Var complexMove(long src, Var srcPos, int srcLen, Var destPos) {
		return complexMove(new Var(src), srcPos.getInt(), srcLen, destPos.getInt());
	}
	public Var complexMove(long src, Var destPos) {
		return complexMove(new Var(src), 1, 18, destPos.getInt());
	}
	public Var complexMove(long src, int destPos) {
		return complexMove(new Var(src), 1, 18, destPos);
	}
	public Var complexMove(long src, long destPos) {
		return complexMove(new Var(src), 1, 18, new Var(destPos));
	}
	
	public Var numval() {
		try {
			if( eq(" "))
				return new Var(0);
			else {
				String s = lrtrim();
				char last=s.charAt(s.length() - 1) ;
				if(last == '-' || last == '+' ) {
					s=last + s.substring(0, s.length() - 1);
				}
				return new Var(Double.valueOf(s));
			}
		} catch (NumberFormatException e) {
			return new Var(0);
		}
	}
	public Var reverse() {
		try {
			if( eq(" "))
				return this;
			else {
				return new Var(new StringBuilder(getString()).reverse().toString());
			}
		} catch (NumberFormatException e) {
			return new Var(0);
		}
	}

	public byte getFirstByte() {
		if((attr & CHAR) != 0) {
			if(value == null)
				return (byte)' ';
			return(byte) ((StringBuilder)value).charAt(0);
		}
		return getBytes()[0];
	}

	/**
	 * Regards the string as a byte array and convert accordingly. 
	 * @return
	 */
	public int getIntAsComp() {
		// TODO Auto-generated method stub
		if((attr & CHAR) != 0) {
			if(value == null)
				return 0;
			byte [] data = ((StringBuilder)value).toString().getBytes();
			int len;
			if(data.length > 4) len = 4;
			else len = data.length;
			
			int val = 0;
			
			for(int i=0; i < len; i++) {
				val |= (data[i] & 0xff) << ((len - i - 1) * 8);
			}
			return val;

			
			
		}
		return getInt();
	}
}

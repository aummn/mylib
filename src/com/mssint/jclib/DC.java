package com.mssint.jclib;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: DC Date conversion utility class</p>
 * <p>Description: Provides LINC date formats and variables.</p>
 * @author Peter Colman
 * @author Martin Hinson
 * @version %BUILD;
 */

public class DC {
	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(DC.class);
	
	// date format
	private static final SimpleDateFormat timeFormat =
		new SimpleDateFormat("HHmmssSS");
	
	/** a LINC constant */
	//Not needed glb.CENTURY_START used instead
	//static public int pivotYear = 57;
	/** a LINC constant */
	static public int baseCentury = 20;

	/** a LINC date format */
	static public final int DAYNUM		= 0;
	/** a LINC date format */
	static public final int DDMMYY		= 1;
	/** a LINC date format */
	static public final int MMDDYY		= 2;
	/** a LINC date format */
	static public final int YYDDD		= 3;
	/** a LINC date format */
	static public final int YYMMDD		= 4;
	/** a LINC date format */
	static public final int DD_MM_YY	= 5;
	/** a LINC date format */
	static public final int DD_MMM_YY	= 6;
	/** a LINC date format */
	static public final int DDMMMYY		= 7;
	/** a LINC date format */
	static public final int MM_DD_YY	= 8;
	/** a LINC date format */
	static public final int MMM_DD_YY	= 9;
	/** a LINC date format */
	static public final int MMMDDYY		=10;
	/** a LINC date format */
	static public final int UK_ALPHA	=11;
	/** a LINC date format */
	static public final int US_ALPHA	=12;
	/** a LINC date format */
	static public final int IN_ALPHA	=13;
	/** a LINC date format */
	static public final int YY_MM_DD	=14;
	/** a LINC date format */
	static public final int YY_MMM_DD	=15;
	/** a LINC date format */
	static public final int YYMMMDD		=16;
	/** a LINC date format */
	static public final int DD_MM_CCYY	=17;
	/** a LINC date format */
	static public final int DD_MMM_CCYY	=18;
	/** a LINC date format */
	static public final int DDMMMCCYY	=19;
	/** a LINC date format */
	static public final int DDMMCCYY	=20;
	/** a LINC date format */
	static public final int MM_DD_CCYY	=21;
	/** a LINC date format */
	static public final int MMDDCCYY	=22;
	/** a LINC date format */
	static public final int MMM_DD_CCYY	=23;
	/** a LINC date format */
	static public final int MMMDDCCYY	=24;
	/** a LINC date format */
	static public final int CCYY_MM_DD	=25;
	/** a LINC date format */
	static public final int CCYY_MMM_DD	=26;
	/** a LINC date format */
	static public final int CCYYDDD		=27;
	/** a LINC date format */
	static public final int CCYYMMDD	=28;
	/** a LINC date format */
	static public final int CCYYMMMDD	=29;

	/** Days in the month for a non-leap year */
	static public final int[] daytab = {
		0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334, 365
	};
	/** Days in the month for a leap year */
	static public final int[] leapdaytab = {
		0, 31, 60, 91, 121, 152, 182, 213, 244, 274, 305, 335, 366
	};

	/** month names in English */
	static public final String[] monthnameEnglish = {
		"JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE",
		"JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER"
	};
	/** day names in English */
	static public final String[] daynameEnglish = {
		"SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY",
		"THURSDAY", "FRIDAY", "SATURDAY"
	};
	/** month names in Dutch */
	static public final String[] monthnameDutch = {
		"JANUARI", "FEBRUARI", "MAART", "APRIL", "MEI", "JUNI",
		"JULI", "AUGUSTUS", "SEPTEMBER", "OKTOBER", "NOVEMBER", "DECEMBER"
	};
	/** day names in Dutch */
	static public final String[] daynameDutch = {
		"ZONDAG", "MAANDAG", "DINSDAG", "WOENSDAG",
		"DONDERDAG", "VRIJDAG", "ZATERDAG"
	};
	/** month names in Spanish */
	static final String[] monthnameSpanish = {
		"ENERO", "FEBRERO", "MARZO", "ABRIL", "PUEDE", "JUNIO",
		"JULIO", "AGOSTO", "SEPTIEMBRE", "OCTUBRE", "NOVIEMBRE", "DICIEMBRE"
	};


	/** Month name being used currently */
	static public final String[] monthname = monthnameEnglish;
	/** Day name being used currently */
	static public final String[] dayname = daynameEnglish;

	//Class variables
	/** International date format */
	static public final int US = 1;
	/** International date format */
	static public final int UK = 2;
	/** International date format */
	static public final int I  = 3;

	//The instance variables
	/** Variable for storing part of the date or time */
	public int day;
	/** Variable for storing part of the date or time */
	public int month;
	/** Variable for storing part of the date or time */
	public int year4;
	/** Variable for storing part of the date or time */
	public int year2;
	/** Variable for storing part of the date or time */
	public int hour;
	/** Variable for storing part of the date or time */
	public int minute;
	/** Variable for storing part of the date or time */
	public int second;
	/** Variable for storing part of the date or time */
	public int millisecond;
	
	/**
	 * It returns the current time as an integer<br>
	 * like 061224 for Dec 24, 2006 
	 */
	public static String getDate(String format)  {
		String jFormat = " ";
		Date date;
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMdd");
		
		Var TODAY = new Var(Var.UNUMERIC,8);
		DC d = TODAY.systemDate();
		try{
			date = dateFormat.parse(Strings.lpad(String.valueOf(d.year2 * 10000 + d.month * 100 + d.day),6,'0'));
		} catch(ParseException e) {
			date = SystemDate.getDate();
		}

		if(format.equals("CCYYMMDD")) jFormat="yyyyMMdd";
		else if(format.equals("YYMMDD")) jFormat="yyMMdd";
		else if(format.equals("DDMMCCYY")) jFormat="ddMMyyyy";
		else if(format.equals("DDMMYY")) jFormat="ddMMyy";
		else if(format.equals("MMDDCCYY")) jFormat="MMddyyyy";
		else if(format.equals("MMDDYY")) jFormat="MMddyy";
		else if(format.equals("YYDDD")) jFormat="MMDDD";

		dateFormat = new SimpleDateFormat(jFormat);
		return dateFormat.format(date);
		//return dateFormat.format(SystemDate.getDate());
	}
	
	/**
	 * Returns the date in DDMMYY format
	 * @return String representation of date in DDMMYY format.
	 */
	public static String getDate()  {
		return getDate("YYMMDD");
	}
	
	/**
	 * Attempts to format a string into dd/MM/yyyy HH:mm Date,
	 * the System exits/aborts if this cannot be achieved.
	 * @param d a string representation of date time
	 * @return dATE of format dd/MM/yyyy HH:mm
	 */
	public static Date parseDate(String d) {
		Date date;
		try {
		    SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm");
			date = df.parse(d);
		} catch(ParseException e) {
			Util.warning("parse error: Expected format \"dd/MM/yyyy hh:mm\"");
			Util.abort(e.toString());
			date = SystemDate.getDate();
		}
		return date;
	}
	
	public static String formatDate(int date) {
		int m,d,y;
		String str;
		if(date > 999999) { //4 digit year
			y = date % 10000;
			m = (date / 10000) % 100;
			d = (date / 1000000) % 100;
			str = String.format("%02d/%02d/%04d", d,m,y);
		} else {
			y = date % 100;
			m = (date / 100) % 100;
			d = (date / 10000) % 100;
			str = String.format("%02d/%02d/%02d", d,m,y);
		}
		return str;
	}
	
	/**
	 * It returns the current date as an integer 
	 * @return 19:42:23 will be 19422300
	 */
	public static String getTime()  {
		String s = timeFormat.format(SystemDate.getDate()).substring(0, 8);
		return s;
	}
	
	/**
	 * Date utility function to return yyMM from a provided date
	 * @param d the Date in question
	 * @return String representing the yyMM part.
	 */
	public static String YYMM(Date d) {
		return (new SimpleDateFormat("yyMM")).format(d);
	}
	
	/**
	 * Date utility function to return yyMM for the current date
	 * @return String representing the yyMM part.
	 */
	public static String YYMM() { return YYMM(SystemDate.getDate()); }
	
	/**
	 * Given a date this function return e.g. "01JUN09"
	 * @param d a Date
	 * @return a String with the following format "01JUN09"
	 */
	public static String toAlpha(Date d) {
	    return ((new SimpleDateFormat("ddMMMyy")).format(d)).toUpperCase();
	}

	/**
	 * Return todays date in ddMMMyy format e.g. "01JUN09"
	 * @return a String with the following format "01JUN09"
	 */
	public static String toAlpha() { return toAlpha(SystemDate.getDate()); }

	/**
	 * Format date to "dd MMM yy" as a String
	 * @param d the date to be formated
	 * @return String representing date in the following format "dd MMM yy" e.g. "01 JUN 09"
	 */
	public static String DD_MMM_YY(Date d) {
	    return ((new SimpleDateFormat("dd MMM yy")).format(d)).toUpperCase();
	}
	
	/**
	 * Format todays date to "dd MMM yy" as a String 
	 * @return String representing date in the following format "dd MMM yy" e.g. "01 JUN 09"
	 */
	public static String DD_MMM_YY() { return DD_MMM_YY(SystemDate.getDate()); }

	/**
	 * Given a date this function return e.g. "01JUN09"
	 * @param d a Date
	 * @return a String with the following format "01JUN09"
	 */
	public static String DDMMMYY(Date d) {
		return ((new SimpleDateFormat("ddMMMyy")).format(d)).toUpperCase();
	}
	
	/**
	 * Return todays date in ddMMMyy format e.g. "01JUN09"
	 * @return a String with the following format "01JUN09"
	 */
	public static String DDMMMYY() { return DDMMMYY(SystemDate.getDate()); }

	/**
	 * Get the day of the week from a given date
	 * @param d a specified Date
	 * @return Day of the week e.g. Monday
	 */
	public static String DOW(Date d) {
		return ((new SimpleDateFormat("EEEE")).format(d));
	}

	/**
	 * Get the day of the week from todays date
	 * @return Day of the week e.g. Monday
	 */
	public static String DOW() { return DOW(SystemDate.getDate()); }

	/**
	 * Get the day of the week from a given date
	 * @param d a specified Date
	 * @return Day number of the week e.g. Monday = 1
	 */
	public static String NDOW(Date d) {
		return ((new SimpleDateFormat("u")).format(d));
	}

	/**
	 * Get the day of the week from todays date
	 * @return Day number of the week e.g. Monday = 1
	 */
	public static String NDOW() { return NDOW(SystemDate.getDate()); }

	/**
	 * Given a date return the time without seconds e.g. 14:13
	 * @param d a specified date
	 * @return String in HH:MM format
	 */
	public static String TIME(Date d) {
		return ((new SimpleDateFormat("HH:mm")).format(d));
	}

	/**
	 * Get the current time without seconds e.g. 14:13
	 * @return String in HH:MM format
	 */
	public static String TIME() { return TIME(SystemDate.getDate()); }
}

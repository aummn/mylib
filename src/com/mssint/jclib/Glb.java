package com.mssint.jclib;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Title: GLB class
 * </p>
 * <p>
 * Description: The GLB class holds LINC style GLB.* variables.
 * </p>
 * <p>
 * Comment: The GLB class hold all of the LINC GLB.* type variables.<br>
 * Some GLB.* variables can be initialised from the environment. The equivalent
 * environment variables will be named GLB_VARNAME. Variables which can be
 * initialised are indicated in the documentation for that variable.
 * </p>
 * <p>
 * last rebuilt %DATE;
 * </p>
 * 
 * @author Peter Colman
 * @version %BUILD;
 */
public class Glb {
	private static final Logger log = LoggerFactory.getLogger(Glb.class);

	// file name for properties
	private static final String PROPERTIES_FILE_NAME = Config.PROPERTIES_FILE_NAME;

	// default values
	private static final int DEFAULT_GLB_BASE_VALUE = 1900;

	private static final int DEFAULT_GLB_CENTURY_VALUE = 20;

	private static final int DEFAULT_GLB_INTL_VALUE = DC.UK;

	private static final String DEFAULT_GLB_PRODUCT_VALUE = "LINC";

	public static Properties properties;

	// initialise properties
	static {
		//System.out.println("glb:static:PROPERTIES_FILE_NAME=" + Config.PROPERTIES_FILE_NAME);
		properties = Config.getProperties();
		if(properties == null) {
			//System.out.println("properties are null");
			//log.error("Config class not initialised correctly. Loading default properties file.");
			properties = new Properties();;
			try {
				InputStream inputStream = Glb.class.getClassLoader()
					.getResourceAsStream(PROPERTIES_FILE_NAME);
				properties.load(inputStream);
		//System.out.println("PROPERTIES_FILE_NAME=" + PROPERTIES_FILE_NAME);
			} catch (NullPointerException e) {
				log.error("Properties file not found: "
					+ PROPERTIES_FILE_NAME, new FileNotFoundException(e.toString()));
			} catch (IOException e) {
				log.error("Properties file not found: "
					+ PROPERTIES_FILE_NAME, new FileNotFoundException(e.toString()));
			}
		}
	}

	// The GLB.* stuff

	public Var ADD = new Var(Var.CHAR, 3); // char(3)
	public Var CHG = new Var(Var.CHAR, 3); // char(3)
	public Var INQ = new Var(Var.CHAR, 3); // char(3)
	public Var FIR = new Var(Var.CHAR, 3); // char(3)
	public Var LAS = new Var(Var.CHAR, 3); // char(3)
	public Var NEX = new Var(Var.CHAR, 3); // char(3)
	public Var BAC = new Var(Var.CHAR, 3); // char(3)
	public Var PUR = new Var(Var.CHAR, 3); // char(3)
	public Var BASE = new Var(Var.NUMERIC, 4); // numeric(4)
	public Var CENTURY_START = new Var(Var.NUMERIC, 4); // numeric(4)
	public Var CENTURY = new Var(Var.NUMERIC, 2); // numeric(2)
	public Var CLOSE = new Var(Var.CHAR, 7); // CLOSE OR CLOSING
	public Var CHANGE = new Var(Var.CHAR, 30); // char(5)
	public Var DATE = new Var(Var.CHAR, 7); // char(7)
	public Var DC_DAYNUM = new Var(Var.NUMERIC, 5); // numeric(5)
	public Var DC_DD_MM_YY = new Var(Var.CHAR, 8); // char(8)
	public Var DC_DD_MMM_YY = new Var(Var.CHAR, 9); // char(9)
	public Var DC_DDMMMYY = new Var(Var.CHAR, 7); // char(7)
	public Var DC_DDMMYY = new Var(Var.NUMERIC, 6); // numeric(6)
	public Var DC_IN_ALPHA = new Var(Var.CHAR, 25); // char(25)
	public Var DC_MM_DD_YY = new Var(Var.CHAR, 8); // char(8)
	public Var DC_MMDDYY = new Var(Var.NUMERIC, 6); // numeric(6)
	public Var DC_MMM_DD_YY = new Var(Var.CHAR, 9); // char(9)
	public Var DC_MMMDDYY = new Var(Var.CHAR, 7); // char(7)
	public Var DC_TODAY = new Var(Var.CHAR, 15); // char(15)
	public Var DC_UK_ALPHA = new Var(Var.CHAR, 25); // char(25)
	public Var DC_US_ALPHA = new Var(Var.CHAR, 25); // char(25)
	public Var DC_WEEKNO = new Var(Var.NUMERIC, 2); // numeric(2)
	public Var DC_YY_MM_DD = new Var(Var.CHAR, 8); // char(8)
	public Var ERROR = new Var(Var.CHAR, 5); // char(5)
	public Var INITSTN = new Var(Var.CHAR, 17); // char(17)
	public Var LANGUAGE = new Var(Var.CHAR, 10); // char(10)
	public Var LOCALE = new Var(Var.CHAR, 10); // char(10)
	public Var PARAM = new Var(Var.CHAR, 4000); // char(4000)
	public Var SELF = new Var(Var.CHAR, 10); // char(10)
	public Var SELFDB = new Var(Var.CHAR, 10); // char(10)
	public Var SELFENV = new Var(Var.CHAR, 10); // char(10)
	public Var DESTENV = new Var(Var.CHAR, 10); // char(10)
	public Var SELFHOST = new Var(Var.CHAR, 17); // char(17)
	public Var DESTHOST = new Var(Var.CHAR, 17); // char(17)
	public Var STALANG = new Var(Var.CHAR, 10); // char(10)
	public Var REPLANG = new Var(Var.CHAR, 10); // char(10)
	public Var ORIGIN = new Var(Var.CHAR, 10); // char(10)
	public Var ORIGINHOST = new Var(Var.CHAR, 17); // char(17)
	public Var SPACES = new Var(Var.CHAR, 2028); // char(0) This can be any length!
	public Var FILEINFO = new Var(Var.CHAR, 1); // char(1)
	public Var MACHINE = new Var(Var.CHAR, 1); // char(1)
	public Var LENGTH = new Var(Var.NUMERIC, 4); // numeric(4)
	public Var STATUS = new Var(Var.CHAR, 5); // char(5)
	public Var TASK = new Var(Var.NUMERIC, 2); // numeric(2)
	public Var DEL = new Var(Var.CHAR, 3); // char(3)
	public Var TOTAL = new Var(Var.NUMERIC, 12, 2); // numeric(12,2)
	public Var TODAY = new Var(Var.CHAR, 7); // char(7)
	public Var TODAYS_MONTH = new Var(Var.CHAR, 3); // char(3)
	public Var TODAYS_DATE_NUM = new Var(Var.NUMERIC, 6); // numeric(6)
	public Var TODAYS_DAY = new Var(Var.NUMERIC, 2); // numeric(2)
	public Var TODAYS_MONTH_NUM = new Var(Var.NUMERIC, 2); // numeric(2)
	public Var TODAYS_YEAR = new Var(Var.NUMERIC, 2); // numeric(2)
	public Var TODAY_DDMMYY = new Var(Var.NUMERIC, 6); // numeric(6)
	public Var TODAY_MMDDYY = new Var(Var.NUMERIC, 6); // numeric(6)
	public Var TODAY_YYMMDD = new Var(Var.NUMERIC, 6); // numeric(6)
	public Var TODAYS_DATE = new Var(Var.NUMERIC, 6); // numeric(6)
	public Var YYDDD = new Var(Var.NUMERIC, 5); // numeric(5)
	public Var YYMMDD = new Var(Var.NUMERIC, 6); // numeric(6)
	public Var YYMMMDD = new Var(Var.CHAR, 7); // char(7)
	public Var YY_MMM_DD = new Var(Var.CHAR, 9); // char(9)
	public Var DC_YYDDD = new Var(Var.NUMERIC, 5); // numeric(5)
	public Var DC_YYMMDD = new Var(Var.NUMERIC, 6); // numeric(6)
	public Var DC_YYMMMDD = new Var(Var.CHAR, 7); // char(7)
	public Var DC_YY_MMM_DD = new Var(Var.CHAR, 9); // char(9)
	public Var ZERO = new Var(Var.NUMERIC|Var.AUTOVAR, 0); // numeric(0)
	public Var ZEROS = ZERO;
	public Var ZEROES = ZERO;
	public Var LOW = new Var(Var.CHAR|Var.AUTOVAR, 1); // numeric(0)
	public Var HIGH = new Var(Var.CHAR|Var.AUTOVAR, 0);
	public Var RECOVER = new Var(Var.NUMERIC, 1); // numeric(1)
	public Var PRINTHOST = new Var(Var.CHAR, 17); // char(17)
	public Var DC_DD_MM_CCYY = new Var(Var.CHAR, 10); // char(10)
	public Var DC_DD_MMM_CCYY = new Var(Var.CHAR, 11); // char(11)
	public Var DC_DDMMMCCYY = new Var(Var.CHAR, 9); // char(9)
	public Var DC_DDMMCCYY = new Var(Var.NUMERIC, 8); // numeric(8)
	public Var DC_MM_DD_CCYY = new Var(Var.CHAR, 10); // char(10)
	public Var DC_MMDDCCYY = new Var(Var.NUMERIC, 8); // numeric(8)
	public Var DC_MMM_DD_CCYY = new Var(Var.CHAR, 11); // char(11)
	public Var DC_MMMDDCCYY = new Var(Var.CHAR, 9); // char(9)
	public Var DC_CCYY_MM_DD = new Var(Var.CHAR, 10); // char(10)
	public Var DC_CCYY_MMM_DD = new Var(Var.CHAR, 11); // char(11)
	public Var DC_CCYYDDD = new Var(Var.NUMERIC, 7); // numeric(7)
	public Var DC_CCYYMMDD = new Var(Var.NUMERIC, 8); // numeric(8)
	public Var DC_CCYYMMMDD = new Var(Var.CHAR, 9); // char(9)
	public Var DC_CC = new Var(Var.NUMERIC, 2); // numeric(2)
	public Var REPNAME = new Var(Var.CHAR, 10); // char(10)
	public Var MATCH = new Var(Var.CHAR, 1); // char(1)
	public Var ACTMTH = new Var(Var.NUMERIC, 4); // numeric(4)
	public Var TIME = new Var(Var.NUMERIC, 8); // hhmmssmm
	public Var MIXNO = new Var(Var.NUMERIC, 8); // hhmmssmm
	public Var WORK = new Var(Var.CHAR, 2000); // char(2000)
	public Var OLDWORK = new Var(Var.CHAR, 2000); // char(2000)
	public Var PC2 = new Var(Var.CHAR,1); // char(1)
	public Var DESTINATION = new Var(Var.CHAR, 10); // char(10)
	public Var SELFXNID = new Var(Var.CHAR, 6); // char(6)
	public Var EXTDBSTATUS = new Var(Var.CHAR, 12); // char(12)
	public Var USER = new Var(Var.CHAR, 17); // char(17)
	public Var USERCODE = new Var(Var.CHAR, 17); // char(17)
	public Var STN = new Var(Var.CHAR, 17); // char(17)
	public int COPY; // numeric(2)
	public int MAXCOPY; // numeric(2)
	public Var BALANCE = new Var(Var.NUMERIC, 18); // numeric(18)
	public Var SOURCE = new Var(Var.CHAR, 1); // char(1)
	public Var ASCPRT = new Var(Var.CHAR, 17); // char(17)
	public Var SYSVERSION = new Var(Var.NUMERIC, 4); // numeric(4)
	public Var VERSIONID = new Var(Var.CHAR, 20); // char(20)
	public Var SUBSYS = new Var(Var.NUMERIC, 4); // numeric(4)
	public Var CORSTATUS = new Var(Var.CHAR, 5); // char(5)
	public Var OLTPTYPE = new Var(Var.CHAR, 8); // char(8)
	public Var OLTPVALUE = new Var(Var.NUMERIC, 9); // numeric(9)
	public Var HUBSTATUS = new Var(Var.CHAR, 5); // char(5)
	public Var REPVERSION = new Var(Var.NUMERIC, 4); // numeric(4)
	public Var PRIORITY = new Var(Var.NUMERIC, 2); // numeric(2)
	public Var PRODUCT = new Var(Var.CHAR, 10); // char(10)
	public Var APPNAME = new Var(Var.CHAR, 12); // char(12)
	public Var GUI = new Var(Var.CHAR, 1); // char(1)
	public Var SECONDARY = new Var(Var.CHAR, 1); // char(1)
	public Var REQUEST = new Var(Var.CHAR, 1); // char(1)
	public Var STYLE = new Var(Var.CHAR, 10).set("NOFORM"); // char(10)
	public Var SYSGENDATE = new Var(Var.NUMERIC, 8);
	public Var SYSGENTIME = new Var(Var.NUMERIC, 4);
	public Var REPGENDATE = new Var(Var.NUMERIC, 8);
	public Var REPGENTIME = new Var(Var.NUMERIC, 4);
	public Var ACTIONKEY = new Var(Var.CHAR, 2);
	public Var APPNO = new Var(Var.NUMERIC, 1);
	public Var HUBTIMEOUT = new Var(Var.NUMERIC, 6);
	public Var PARAMFLAG = new Var(Var.CHAR, 1);
	public Var MAINSQLCODE = new Var(Var.NUMERIC, 9);
	public Var REPORT = new Var(Var.CHAR, 1);
	public Var PITCH = new Var(Var.NUMERIC, 3);
	public Var PRIV = new Var(Var.NUMERIC, 2);
	public Var REMOTESTATION = new Var(Var.CHAR, 17);
	public Var URL = new Var(Var.CHAR, 100);

	//Synonyms
	public Var INITFULLSTN;
	public Var FULLSTN;
	public Var STATION;

	/**
	 * After detach() is called on a string, detachOffset will contain the 
	 * position (1 relative) of the start of the next word.
	 */
	public int detachOffset;


//	private boolean trannoUpdated = false;

	private Var glbStatusVar = null;

	public int tranno = 0;

	/** For date conversion routines */
	public int intl;

	/** For date conversion routines */
	public int epoch = 0;
	private Var epochTmp = new Var(Var.NUMERIC, 6);

	/**
	 * Create the Glb instance. This should only be done once for the on-line
	 * and once for each report
	 */
	public Glb() {
		initAll();
	}

	/** Set up all GLOBAL Variables */
	public void initAll() {
		initOnce();
		initMulti();
        String s = System.getenv("GLB_INITSTN");
        if(s != null) INITSTN.set(s);
	}

	/**
	 * Set up all date based GLOBAL Variables that may need refreshing over
	 * midnight.
	 */
	public void initMulti() {
                //STATION.set("AC001");
		STATUS.set("     ");
		CENTURY.set(0);
		//Deleted 07/08/2007 because it needs intl set below setGlbDate();
		DESTINATION.set(" ");
		SELFXNID.set(" ");
		EXTDBSTATUS.set(" ");

		// load GLB_BASE, GLB_CENTURY and GLB_INTL
		// use default values if empty
		String glbBase = properties.getProperty("glb.base");
		//System.out.println("glbBase=" + glbBase);
		if (glbBase == null || glbBase.equals(""))
			BASE.set(DEFAULT_GLB_BASE_VALUE);
		else
			try {
				BASE.set(Integer.parseInt(glbBase));
			} catch (NumberFormatException ex) {
				log.error("Illegal glb.base property");
				BASE.set(DEFAULT_GLB_BASE_VALUE);
			}
		//System.out.println("glbBase=" + glbBase);
		//System.out.println("BASE=" + BASE);
		String glbCenturyStart = properties.getProperty("glb.century.start");
		if (glbCenturyStart == null || glbCenturyStart.equals(""))
			CENTURY_START.set(BASE);
		else
			try {
				CENTURY_START.set(Integer.parseInt(glbBase));
			} catch (NumberFormatException ex) {
				log.error("Illegal glb.century.start property");
				CENTURY_START.set(BASE);
			}
		
		String glbCentury = properties.getProperty("glb.century");
		if (glbCentury == null || glbCentury.equals(""))
			CENTURY.set(0);
		else
			try {
				CENTURY.set(Integer.parseInt(glbCentury));
			} catch (NumberFormatException ex2) {
				log.error("Illegal glb.century property");
				CENTURY.set(0);
			}
		String glbIntl = properties.getProperty("glb.intl");
		if(log.isDebugEnabled())
			log.debug("glb.intl="+glbIntl);
		if (glbIntl == null
				|| (!glbIntl.equals("U") && !glbIntl.equals("S") && !glbIntl
						.equals("I")))
			intl = DEFAULT_GLB_INTL_VALUE;
		else {
			if (glbIntl.equals("U"))
				intl = DC.UK;
			else if (glbIntl.equals("S"))
				intl = DC.US;
			else
				intl = DC.I;
		}

		if (intl == DC.UK)
			epochTmp.set(10157);
		else if (intl == DC.US)
			epochTmp.set(10157);
		else
			epochTmp.set(570101);
		epoch = 0;
		TOTAL.set(epochTmp.toDayNumber(this));
		epoch = TOTAL.getInt();
		TOTAL.set(0);
		setGlbDate(); //Moved to here
	}
	String replang = null;
	String stalang = null;
	String language = null;
	/** Set up GLOBAL Variables that only ever need setting once. */
	public void initOnce() {
		ZEROS.set(0);
		ZERO.set(0);
		SPACES.set(" ");
		
		/*
		Connection conn = DBConnectionFactory.getConnection();
		String Sql = "select sys_context('userenv','CURRENT_USER') from dual";
		CallableStatement stmnt = conn.prepareCall(Sql);
		ResultSet rs = stmnt.executeQuery();
		String user = " ";
		if(rs != null && rs.next()) {
			//System.out.println("1");
			user = rs.getString(1);
		    rs.close();
		}
		SELF.set(user.toUpperCase());
		Sql = "select substr(name,1,10) from v$database";
		stmnt = conn.prepareCall(Sql);
		rs = stmnt.executeQuery();
		String db = " ";
		if(rs != null && rs.next()) {
			db = rs.getString(1);
		    rs.close();
		}
		SELFDB.set(db);
		Sql = "select substr(machine,1,17) " +
				"from v$session " +
				"where sid=(select sid from v$mystat where rownum =1)";
		stmnt = conn.prepareCall(Sql);
		rs = stmnt.executeQuery();
		String machine = " ";
		if(rs != null && rs.next()) {
			machine = rs.getString(1);
		    rs.close();
		}
		SELFHOST.set(machine);
		//Sql = "select substr(osuser,1,17) " +
				//"from v$session " +
				//where sid=(select sid from v$mystat where rownum =1)";
		Sql = "select sys_context('userenv','CURRENT_USER') from dual";
		stmnt = conn.prepareCall(Sql);
		rs = stmnt.executeQuery();
		String usercode = " ";
		if(rs != null && rs.next()) {
			usercode = rs.getString(1);
		    rs.close();
		}
		USER.set(usercode);
		USERCODE.set(usercode);
	    stmnt.close();
	    conn.close();
	    */
		// select substr(name,1,10) into self from v$database);
		// select substr(name,1,10) into selfenv from v$database);
		SELF.set(properties.getProperty("jclib.system.name"));
		SELFENV.set(SELF);
		DESTENV.set(SELFENV);
		// select substr(machine,1,17)
		// into selfhost from v$session
		// where sid=(select sid from v$mystat where rownum =1));
		DESTHOST.set("Not set");
		ORIGINHOST.set(SELFHOST);
		ORIGIN.set(SELF);
		String s = properties.getProperty("glb.machine");
		if(s == null) s = "U";
		MACHINE.set(s);
		try {
			char hexValue = (char)Integer.parseInt("FF", 16);
			HIGH.initialise(3000, 9, hexValue);
			//HIGH.initialise(3000, 9, '~');
			hexValue = (char)Integer.parseInt("00", 16);
			 LOW.initialise(3000, 0, hexValue);
		} catch(Exception e) { ; }
		INQ.set("INQ");
		ADD.set("ADD");
		CHG.set("CHG");
		DEL.set("DEL");
		FIR.set("FIR");
		NEX.set("NEX");
		LAS.set("LAS");
		BAC.set("BAC");
		PUR.set("PUR");
		setupLanguageOptions();
		if(stalang == null)
			stalang = Config.getProperty("glb.language");
		if(replang == null)
			replang = Config.getProperty("glb.replang");
		if(language == null)
			language = Config.getProperty("glb.language");
		if(stalang == null && replang == null && language == null) {
			STALANG.set("ENGLISH");
			REPLANG.set("ENGLISH");
			LANGUAGE.set("ENGLISH");
		} else {
			String def = "ENGLISH";
			if(language != null) {LANGUAGE.set(language);def=language;}
			if(replang != null) {REPLANG.set(replang);def=replang;}
			if(stalang != null) {STALANG.set(stalang); def=stalang;}
			if(language == null) LANGUAGE.set(def);
			if(replang == null) REPLANG.set(def);
			if(stalang == null) STALANG.set(def); 
		}
		/*
		LANGUAGE.set("ENGLISH   ");
		*/
		// MSS_TERMINAL.set(userenv("terminal"));
		// select substr(osuser,1,17)
		// into usercode from v$session
		// where sid=(select sid from v$mystat where rownum =1));
		// -- code below if separate users and user_ssion table exists
		// --select substr(osuser,1,17)
		// --into usercode from user_session
		// --where rownum = 1);
		// STN.set(mss_terminal);
		// STATION.set(mss_terminal);
		ERROR.set("     ");
		PARAM.set(" ");
		LENGTH.set(00);
		COPY = 0;
		MAXCOPY = 0;
		TOTAL.set(00);
		BALANCE.set(00);
		SOURCE.set("T");
		WORK.set(" ");
		OLDWORK.set(" ");
		STATUS.set(" ");
		ASCPRT.set("pq3isu");
		SYSVERSION.set(1234);
		SUBSYS.set(1);
		CORSTATUS.set(" ");
		// 2PC.set(" ");
		OLTPVALUE.set(0);
		OLTPTYPE.set(" ");
		HUBSTATUS.set(" ");
		REPVERSION.set(0);
		PRIORITY.set(0);
		String glbProduct = properties.getProperty("glb.product");
		if (glbProduct == null || glbProduct.equals(""))
			PRODUCT.set(DEFAULT_GLB_PRODUCT_VALUE);
		else
			PRODUCT.set(glbProduct);
		//PRODUCT.set("LINC");
		//PRODUCT.set("LDAIII");
Config.log.info("PRODUCT={}",PRODUCT.getString());
		APPNAME.set(" ");
		RECOVER.set(0);
		GUI.set("Y");
		TASK.set(0);
		SECONDARY.set(" ");
		FILEINFO.set("N");
		PRINTHOST.set(" ");
		ACTIONKEY.set(" ");
		APPNO.set(0);
		APPNAME.set(" ");

		INITFULLSTN = INITSTN;
		FULLSTN = STN;
		STATION = STN;
		SYSGENDATE.set(0);
		SYSGENTIME.set(0);
		REPGENDATE.set(0);
		REPGENTIME.set(0);
	}
   
	protected static final String LOCALE_FULL_NAME = "locale.fullname.";
    protected static final String LOCALE_CHARSET = "locale.charset.";
    protected static final String LOCALE_FONT_SET = "locale.fontset.";
    protected static final String LOCALES = "system.multi.locale";
    protected static final String DEFAULT_LOCALE = "system.default.locale";
    protected Map<String,String>localeList = null;
    protected String defaultLocale;
    private boolean initialised = false;
    
	private void setupLanguageOptions() {
		if(initialised) return;
		String s = properties.getProperty(LOCALES);
		String def_lc = properties.getProperty(DEFAULT_LOCALE);
		String locale = null;

		if(s != null) {
			String [] list = s.split("[, ]");
			for(String lc : list) {
				if(lc.length() == 0) continue;
				if(localeList == null) localeList = new HashMap<String, String>();
				String lang = properties.getProperty(LOCALE_FULL_NAME + lc);
				if(lang == null || lang.length() == 0) continue;
				localeList.put(lang.toUpperCase(), lc.toLowerCase());
				if(def_lc != null && def_lc.equalsIgnoreCase(lc)) {
					language = lang.toUpperCase();
					locale = def_lc;
				} else if(language == null) {
					//Set default to the first in list
					language = lang.toUpperCase();
					locale = lc;
				}
			}
			if(locale == null) {
				locale = "en";
				language = "ENGLISH";
			}
			LANGUAGE.set(language);
			LOCALE.set(locale);
			REPLANG.set(language);
			STALANG.set(language);
		}
		if(def_lc == null)
			defaultLocale = "en";
		else
			defaultLocale = def_lc;
	}
	
	public String getLocale() {
		String lang = LANGUAGE.toString();
		String lc;
		if(localeList != null)
			lc = localeList.get(lang);
		else lc = LOCALE.toString();
		if(lc == null) return defaultLocale;
		return lc;
	}
	
	public String getLocaleDefault() {
		return defaultLocale;
	}

	/**
	 * Used for clearing GLB.STATUS or the variable being used for GLB.STATUS
	 * values.
	 * 
	 * @param stat
	 *            a zero value clears GLB.STATUS or the variable representing
	 *            GLB.STATUS to " ". Used for compatibility with the
	 *            migrate!LINC SQC version.
	 */
	public void setStatus(int stat) {
		if (stat == 0) {
			if (glbStatusVar != null)
				glbStatusVar.set("     ");
			else
				STATUS.set("     ");
			glbStatusVar = null;
		}
	}

	/**
	 * Used for setting GLB.STATUS or the variable used for storing GLB.STATUS
	 * values to stat.
	 * 
	 * @param stat
	 *            sets GLB.STATUS (or the designated variable) to this
	 */
	public void setStatus(String stat) {
		if (glbStatusVar != null)
			glbStatusVar.set(stat);
		else
			STATUS.set(stat);
		glbStatusVar = null;
	}

	/**
	 * Used for setting GLB.STATUS or the variable used for storing GLB.STATUS
	 * values to stat.
	 * 
	 * @param stat
	 *            sets GLB.STATUS (or the designated variable) to this
	 */
	public void setStatus(Var stat) {
		if (glbStatusVar != null)
			glbStatusVar.set(stat);
		else
			STATUS.set(stat);
		glbStatusVar = null;
	}

	/**
	 * Used for setting the variable to use for GLB.STATUS values.
	 * 
	 * @param stat
	 *            the variable for holding GLB.STATUS values.
	 */
	public void glbStatus(Var stat) {
		if (stat != null)
			glbStatusVar = stat;
	}
	
	/**
	 * Used for unsetting the variable to use for GLB.STATUS values.
	 * 
	
	 */
	public void resetGlbStatus() {
		glbStatusVar = null;
	}

	/**
	 * returns GLB.TIME.
	 * 
	 * @return TIME
	 */
	public Var getTime() {
		return TIME;
	}

	/**
	 * returns GLB.MIX.
	 * 
	 * @return MIX
	 */
	public Var MIX() {
		return new Var(Util.getPid());
	}

	/**
	 * returns GLB.STATION.
	 * 
	 * @return STATION
	 */
	public Var getStation() {
		return STATION;
	}

	private String twoDig(int p) {
		if (p > 9)
			return (Integer.toString(p));
		return ("0" + Integer.toString(p));
	}

	/**
	 * Used to reset data and time to current date and time
	 */
	public void setGlbDate() {
		DC d = TODAY.systemDate();

		TODAYS_DATE_NUM.set(d.month * 10000 + d.day * 100 + d.year2);
		TODAY_MMDDYY.set(d.month * 10000 + d.day * 100 + d.year2);
		TODAY_DDMMYY.set(d.day * 10000 + d.month * 100 + d.year2);
		TODAY_YYMMDD.set(d.year2 * 10000 + d.month * 100 + d.day);

		TODAYS_DAY.set(d.day);
		if (intl == DC.UK) {
			TODAY.set(twoDig(d.day) + DC.monthname[d.month - 1].substring(0, 3)
					+ twoDig(d.year2));
			TODAYS_DATE.set(TODAY_DDMMYY);
		} else if (intl == DC.US) {
			TODAY.set(DC.monthname[d.month - 1].substring(0, 3) + twoDig(d.day)
					+ twoDig(d.year2));
			TODAYS_DATE.set(TODAY_MMDDYY);
		} else {
			TODAY.set(twoDig(d.year2)
							+ DC.monthname[d.month - 1].substring(0, 3)
							+ twoDig(d.day));
			TODAYS_DATE.set(TODAY_YYMMDD);
		}
		DATE.set(TODAY);
		TODAYS_MONTH.set(DC.monthname[d.month - 1].substring(0, 3));
		TODAYS_MONTH_NUM.set(d.month);
		TODAYS_YEAR.set(d.year2);
		YYMMDD.set(TODAY_YYMMDD);
		TIME.set(d.hour * 1000000 + d.minute * 10000 + d.second * 100
				+ d.millisecond / 10);
		/*I dont think this should be doing this -- Steve 20/01/2009
		if (intl == DC.UK)
			TODAY.dateConvert(DC.DDMMMYY, 0, this);
		else if (intl == DC.US)
			TODAY.dateConvert(DC.MMMDDYY, 0, this);
		else
			TODAY.dateConvert(DC.YYMMMDD, 0, this);
		*/
	}

	/**
	 * retrieves GLB.WORK from the database.
	 * 
	 * @throws SQLException
	 */
	public void getGlbWork() throws SQLException {
		Connection conn = DBConnectionFactory.getConnection();
                //STATION.set("AC001");
	    USERCODE.set("");
		String Sql = "select work_au,tranno_au, url from glb_audit10 "
				+ "where station_au = ?";
		CallableStatement stmnt = conn.prepareCall(Sql);
		stmnt.setString(1, STATION.getString());
		ResultSet rs = stmnt.executeQuery();
		String work_au = " ";
		int tranno_au = 0;
		if(rs != null && rs.next()) {
			work_au = rs.getString(1);
			tranno_au = rs.getInt(2);
			URL.set(rs.getString(3));
		    rs.close();
		} else {
			stmnt.close();
			conn.commit();
			conn.close();
			throw new SQLException("GLB.WORK not found STATION=" + STATION);
		}
		WORK.set(work_au);
//	    System.out.println("getGlbWork: ["+WORK.toString());
		tranno = tranno_au;
		stmnt.close();
		conn.commit();
		conn.close(); // Temporary
		return;
	}

	/**
	 * updates GLB.WORK to the database.
	 * 
	 * @throws SQLException
	 */
	public void updateGlbWork() throws SQLException {
		Connection conn = DBConnectionFactory.getConnection();
		String Sql = "update glb_audit10 set work_au = ?, tranno_au = ?, url = ? "
				+ "where station_au = ?";
		CallableStatement stmnt = null;
		try {
			stmnt = conn.prepareCall(Sql);
			stmnt.setString(1, WORK.getString());
			stmnt.setInt(2, tranno);
			stmnt.setString(3, URL.toString().replaceAll("https:", "http:"));
			stmnt.setString(4, STATION.getString());
			int count = stmnt.executeUpdate();
			stmnt.close();
			stmnt = null;
			//	    System.out.println("updateGlbWork: ["+WORK.toString());

			if(count == 0) {
				Sql = "insert into glb_audit10 " +
						"(station_au, tranno_au, work_au, url) values" +
						"(?, ?, ?, ?)";
				stmnt = conn.prepareCall(Sql);
				stmnt.setString(1, STATION.getString());
				stmnt.setInt(2, tranno);
				stmnt.setString(3, WORK.getString());
				stmnt.setString(4, URL.getString().replaceAll("https:", "http:"));
				stmnt.execute();
			}
		} finally {
			if(stmnt != null)
				stmnt.close();
			conn.commit();
			conn.close();
		}
		return;
	}

	/**
	 * increments tranno.
	 */
	public void updateTranno() {
		//if (WORK.ne(OLDWORK)) {
		//if(ERROR.ne("*****")) {
			//if (!trannoUpdated) {
				tranno++;
				//trannoUpdated = true;
			//}
		//}
		if(tranno == 1000000) tranno=1;
		return;
	}

	/**
	 * gets GLB.UNIQUE tranno.
	 * 
	 * @throws SQLException
	 */
	public int UNIQUE() throws SQLException {
		int l_unique;
		String Sql = "select glbunique.nextval from dual";
		Connection conn = DBConnectionFactory.getConnection();
		CallableStatement stmnt = conn.prepareCall(Sql);
		ResultSet rs = stmnt.executeQuery();
		if(rs.next()) {
			l_unique = rs.getInt(1);
			rs.close();
		} else l_unique = -1;
		stmnt.close();
		conn.commit();
		conn.close();
		return l_unique;
	}
	
	public int checkInd(int inIndex, int upperBound) {
		if (inIndex < 1) {
			STATUS.set("*****");
			return 1;
		} else if (inIndex > upperBound) {
			STATUS.set("*****");
			return upperBound;
		} else {
			STATUS.clear();
			return inIndex;
		}
	}
	
	public int checkInd(int inIndex, Var upperBound){
		return checkInd(inIndex, upperBound.getInt());
	}
	
	public int checkInd(Var inIndex, int upperBound){
		return checkInd(inIndex.getInt(),upperBound);
	}
	
	public int checkInd(Var inIndex, Var upperBound){
		return checkInd(inIndex.getInt(),upperBound.getInt());
	}
}

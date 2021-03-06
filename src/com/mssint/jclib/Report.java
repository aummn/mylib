package com.mssint.jclib;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Provides the mechanism to generate and spool reports.
 * 
 * 
 * 
 * <p>Title: Report  class</p>
 * <p>last rebuilt %
 * DATE; </p>
 * @version %BUILD;
  */
public class Report {
	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(Report.class);

	public static final int PAGELEN_SET=0000000001; //Pagelen set explicitly
	public static final int RELEASE    =0000000002; //Release before next print
	public static final int AUTOPRINT  =0000000004; //Print on release or EOJ
	public static final int PRINTED    =0000000010; //Already printed
	public static final int WRITTEN    =0000000020; //File has been written to
	public static final int LINEUP     =0000000040; //Perform a lineup before printing
	public static final int INCREMENT  =0000000100; //File number can be incremented.
	public static final int NOINCREMENT=0000000200; //once only: do not increment pf_number
	public static final int FD_WRITE   =0000000400; //File is open for writing
	public static final int FIRST_ACCESS = 0000002000; //Last command was setTitle()
	public static final int NO_EMPTY_CREATE = 0000004000; //Last command was setTitle()

	//-------------- Defines for Extract class -----------//
	public static final int FD_READ    =0000001000; //File is open for reading

	//-------------- Global static references ------------//
	protected static String reportName = null; //Name of batch program
	private static ArrayList<Report> reportList;
	private static Object parentObject = null;
	public static ApplicationState applicationState = null;
	private static boolean initialisedStatic = false;
	//private static int P_AUTOPRINT = 0; //0=no print, 1=print, 2=print&delete
	
	private boolean initialised = false;
	private String reportPath = null; //Path to report directory
	private String defaultDevice = null;
	private boolean standardHeader; //Whether to print standard headers
	private String extensionClassName;
	private Class<?> extensionClass;
	private ReportExtension extension;
	private boolean standardHeaderSet = false;
	private String jclibReportName;

	private Method[] bpFrames = null;

	//-------------- Local's -----------------//
	protected char repId;		//The report id - 0 for rep, 1 to 26 for repA to repZ
	protected int attr;		//Current attributes
	protected String title;	//The "title" of the report
	protected String fileName;	//The current filename
	private int sequence;	//Report sequence number for multiple releases.
	private int lineNumber;	//Current actual line number (already printed)
	private int logicalAdvance; //Number of lines to skip before next print
	private int pageNumber;	//Current page number
	private int pageLength;	//Number of lines per page
	private int pitch;	//pitch
	private String device; //LINC style printing device (LP, RP, TP, VD)
	protected MssFile fd;
	protected boolean permanent = true;
	private String onChangeName = null;
	
	private boolean printingHeader; //Indicates that a header is being printed.
	private boolean skipOnWrite; //Form feed prior to next write
	private boolean printBanner = false;
	private int saveDays = 0;     
 	protected boolean titleSet;
	protected int copies;
	protected String station;
	private String lastStation;
	protected String formId;
	private String user;
	private String backup;
	private String printat;
	

	
	/**
	 * Constructor specifying Report Identifier and file name of report data.
	 * @param repId the report Identifier.
	 * @param fileOrOptions the filename where the data for the report is held, or else options.
	 * @throws ClassNotFoundException 
	 */
	public Report(String repId, String fileOrOptions) throws ClassNotFoundException {
		this(repId, fileOrOptions, null);
	}
	
	/**
	 * Constructor specifying Report Identifier and file name of report data.
	 * Note that the last non-null string will be checked to see if it contains an '=' sign. 
	 * If it does, it is treated as an options string. So calling Report("xx=yy", null, null) will
	 * result in 'repId' being checked as an options string and not as repId.
	 * 
	 * @param repId the report Identifier. 
	 * @param fileName the filename where the data for the report is held, or else options.
	 * @param options The options string.
	 * @throws ClassNotFoundException 
	 */
	public Report(String repId, String fileName, String options) throws ClassNotFoundException {
		//repId is the first character (A=1, etc)
		if(options != null) setOptions(options);
		else if(fileName != null) fileName = setOptions(fileName);
		else if(repId != null) repId = setOptions(repId);
		if(repId == null) repId = "";
		if(fileName == null) fileName = "";
		
		init();
	    title = null;
	    fileName = null;
		sequence = 0;
		this.repId = 0;
		lineNumber = 0;
		logicalAdvance = 0;
		pageNumber = 0;
		pageLength = 0;
		printingHeader = false;
		skipOnWrite = false;
		station = " ";
		lastStation = "";
		device = defaultDevice;

		
		if(repId.length() > 0) {
			char c = repId.charAt(0);
			if(c >= 'A' && c <= 'Z') this.repId = (char)(c - 'A' + 1);
			else if(c >= 'a' && c <= 'z') this.repId = (char)(c - 'a' + 1);
		}
		reportList.add(this);
		if(extensionClassName != null) {
			extensionClass = Class.forName(extensionClassName);
			extension = null;
		} else {
			extensionClass = null;
			extension = null;
		}
		
	}
	
	/**
	 * Create a new Report object. 
	 * @param repIdOrOptions Specifies either the report ID (A through Z) or options in the forms xx=yy.
	 * @throws ClassNotFoundException
	 */
	public Report(String repIdOrOptions) throws ClassNotFoundException {
		this(repIdOrOptions, null, null);
	}
	
	/**
	 * Default constructor. If used the Report name and filename
	 * will need to be provided at some point.
	 * @throws ClassNotFoundException 
	 */
	public Report() throws ClassNotFoundException { this(null, null, null); }
	
	/**
	 * Close down & release all reports
	 */
	public static void releaseAll() {
		if(reportList == null)
			return;
		for(Report rep : reportList) {
			String device=null;
			String fileName=null;
			String station=null;
			if(rep != null && rep.getFileName() != null) {
				device=rep.getDevice();
				fileName=rep.getFileName().trim();
				station=rep.getStation().trim();
				//System.out.println(device+":"+fileName+":"+station);
			}
			rep.release();
			if(fileName != null && device.equals("EX")) {
					
			 	Path fromPath = Paths.get(fileName);
			    Path toPath = Paths.get("C:\\linc\\lincntsql\\roc16\\" + station);
				 
			    try {
			    	Files.createDirectories(Paths.get("C:\\linc\\lincntsql\\roc16"));
					Files.move(fromPath,toPath,StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	/**
	 * update print_info
	 * @throws SQLException 
	 */
	public static void updateAllPrintinfo(Connection conn, Var orient) throws SQLException {
		if(reportList == null)
			return;
		for(Report rep : reportList) 
			rep.updatePrintinfo(conn, orient);
	}
	
	public void updatePrintinfo(Connection conn, Var orient) throws SQLException {
		if(getFileName() != null) {
		   String sql= "insert into print_info (" +
		           "[name]," +        //e.g. PRBNTLST.150331.230947
		           "[status]," +      //"C" = completed "P" = printed "D" = deleted
		           "[delete]," +      //"D" = delete after printing
		           "[numcopies]," +   //e.g. 1
		           "[orientation]," + //"L" or "P"
		           "[device]," +      //TP,LP,VD,RP
		           "[formdepth]," +   //
		           "[formid]," +      //
		           "[pitch]," +       //
		           "[printer]," +     //e.g. "ADMIN"
		           "[completed]," +   //e.g. "150331.231027"
		           "[printed]," +     //e.g. "150331.231033"
		           "[deleted]" +      //e.g. "150331.231035"
		           ") values (?,?,?,?,?,?,?,?,?,?,?,?,?)";
		   PreparedStatement stmnt = conn.prepareStatement(sql);
		   stmnt.setString(1,getFileName().substring(getFileName().lastIndexOf('\\') + 1).trim());
		   stmnt.setString(2,"C");
		   stmnt.setString(3," ");
		   stmnt.setInt(4,getNumCopies());
		   stmnt.setString(5,orient.rtrim());
		   stmnt.setString(6,getDevice());
		   stmnt.setInt(7,getFormDepth());
		   stmnt.setString(8,getFormId());
		   stmnt.setInt(9,getPitch());
		   stmnt.setString(10,getStation());
		   SimpleDateFormat sdfDate = new SimpleDateFormat("yyMMdd.HHmmss");
		   Date now = new Date();
		   stmnt.setString(11,sdfDate.format(now));
		   stmnt.setString(12," ");
		   stmnt.setString(13," ");
		   stmnt.executeUpdate();
		   stmnt.close();
		}
	}
		
	
	/**
	 * Indicate whether or not Print Banner is required
	 * @param yn "Y" or "YES" indicates print banner required, 
	 * anything else is assumed as no print banner
	 */
	public void setPrintBanner(Var yn) {
		setPrintBanner(yn.getString().trim());
	}
	/**
	 * Indicate whether or not Print Banner is required
	 * @param yn "Y" or "YES" indicates print banner required, 
	 * anything else is assumed as no print banner
	 */
	public void setPrintBanner(String yn) {
		if(yn == null) printBanner = false;
		else if(yn.compareToIgnoreCase("Y") == 0) printBanner = true;
		else if(yn.compareToIgnoreCase("YES") == 0) printBanner = true;
		else printBanner = false;
	}
	/**
	 * Gets an indicator as to whether there is a print banner or not.
	 * @return "Y" if print banner is set "N" otherwise
	 */
	public String getPrintBanner() { return printBanner ? "Y" : "N"; }

	//---------------- Static methods -------------//
	public static String getReportName() {
		return reportName;
	}
	public static void setReportName(String repname) {
		reportName = repname;
	}
	
	public String getFileName() {
		if(fd == null) return null;
		return fd.getFileName();
	}
	
	/**
	 * Set the number of copies to be printed.
	 * @param copies
	 */
	public void setNumCopies(int copies) {
		this.copies = copies;
	}
	/**
	 * Set the number of copies to be printed.
	 * @param copies
	 */
	public void setNumCopies(Var copies) {
		this.copies = copies.getInt();
	}
	public int getNumCopies() {
		return copies;
	}
	
	public void setStation(String stn) {
		station = stn;
	}
	public void setStation(Var stn) {
		station = stn.toString();
	}
	public String getStation() { return station; }
	/**
	 * What is the print spool device identifier.
	 * @return the print spool device identifier
	 */
	public String getDevice() { return device; }
	
	/**
	 * Set the print spool device 
	 * @param str the device identifier.
	 */
	public void setDevice(String dev) { device = dev; }

	/**
	 * Set the print spool device 
	 * @param str the device identifier.
	 */
	public void setDevice(Var dev) { device = dev.getString().trim(); }
	
	public void setFormId(Var id) {formId = id.getString(); }
	public void setFormId(String id) {formId = id; }
	public String getFormId() { return formId; }
	/**
	 * Get duration in days for how long we will store the report.
	 * @return
	 */
	public int getSaveDays() { return saveDays; }
	
	/**
	 * Sets the number of days to save the report.
	 * @param days
	 */
	public void setSaveDays(int days) { saveDays = days; }
	
	/**
	 * Overridden please see {@link #setSaveDays(int)} except the integer
	 * value is contained in the Var 
	 * @param days
	 */
	public void setSaveDays(Var days) { saveDays = days.getInt(); }
	
	/**
	 * get pitch
	 */
	public int getPitch() { return pitch; }
	
	/**
	 * set pitch
	 */
	public void setPitch(int pitch) { this.pitch = pitch; }
	
	/**
	 * set pitch
	 */
	public void setPitch(Var pitch) { this.pitch = pitch.getInt(); }
	
	/**
	 * Set the Parent/container object for the specified Reprt Name
	 * @param parent ApplicationState under which this report is being generated.
	 * @param REPORTNAME
	 */
	public static void setParent(Object parent, String REPORTNAME) {
		parentObject = parent;
		applicationState = (ApplicationState)parent;
		reportName = REPORTNAME;
		applicationState.REPORTNAME = reportName;
	}
	
	/**
	 * Checks the options string for options of the form "xx=yy,xx1=yy1,...". If the options string
	 * does not contain any options, will return the original string.
	 * @param options The allowed options. The following options are legal:
	 * jclib.report.dir=report-path
	 * jclib.glb.device=LP, etc
	 * jclib.standard.header=true or false
	 * jclib.report.name=reportname
	 * jclib.report.extension=extension class
	 * 
	 * Settings set using this method override Config or environmental settings.
	 * 
	 * @return The original string if there was no '=' in it.
	 */
	public String setOptions(String options) {
		if(options == null) return null;
		if(options.length() == 0) return options;
		if(options.indexOf('=') == -1) return options;
		String [] opts = options.split(",");
		for(String opt : opts) {
			String [] assign = opt.split("=");
			if(assign.length != 2) continue;
			if(assign[0].equalsIgnoreCase("jclib.report.dir")) {
				reportPath = assign[1];
			} else if(assign[0].equalsIgnoreCase("jclib.glb.device")) {
				defaultDevice = assign[1];
			} else if(assign[0].equalsIgnoreCase("jclib.standard.header")) {
				if(assign[1].equalsIgnoreCase("true"))
					standardHeader = true;
				else standardHeader = false;
				standardHeaderSet  = true;
			} else if(assign[0].equalsIgnoreCase("jclib.report.name")) {
				jclibReportName = assign[1];
			} else if(assign[0].equalsIgnoreCase("jclib.report.extension")) {
				extensionClassName = assign[1];
			}
		}
		return null;
	}
	/**
	 *  Initialise the static portions of Report
	 */
	private void init() {
		if(initialised) return;

		if(reportPath == null) reportPath = System.getenv("REPORT_DIR");
	    if(reportPath == null) reportPath = Config.getProperty("jclib.report.dir");
	    if(reportPath == null) reportPath = ".";

	    //TODO reportNameRule = Config.getProperty("jclib.report.name");
	    //TODO P_AUTOPRINT = Util.stringToInt(Config.getProperty("jclib.auto.print"));

		if(defaultDevice == null) {
			String s = System.getenv("GLB_DEVICE");
			if(s != null) defaultDevice=s;
		}
	    if(defaultDevice == null) defaultDevice = Config.getProperty("jclib.glb.device");
		if(defaultDevice== null) defaultDevice = "LP";

		if(!standardHeaderSet) standardHeader = Config.getBooleanProperty("jclib.standard.header");

		if(extensionClassName == null)
			extensionClassName = Config.getProperty("jclib.report.extension");

	    if(jclibReportName == null)
	    	jclibReportName = Config.getProperty("jclib.report.name");

	    initialised = true;

	    if(initialisedStatic) return;
		reportList = new ArrayList<Report>();
		initialisedStatic = true;
		return;
	}
	
	/**
	 * Returns the formatted file name
	 * @return the formatted file name
	 */
	public String newFileName() {
		String e = jclibReportName;
		String fname;
		if(this instanceof Extract && title != null && title.length() > 0) {
        	fname = title;
		} else if(e == null) {
			String env;
			if(isReport()) env = "PRINT";
			else if(isExtract()) env = "EXTRACT";
			else env = "UNKNOWN";
        	char letter = (char)((repId-1) + 'A');
        	if(repId > 0) env = env + letter;
        	String envName = System.getenv(env);
        	if(envName == null) envName = System.getProperty("jclib."+env);
        	if(envName != null) {
        		fname = envName;
        	} else if(title != null && title.length() > 1) {
	        	fname = title;
	        } else {
		        fname = reportName;
		        if(fname == null) fname = "JCLIB";
		        if(repId > 0) fname = fname + "." + letter;
		        fname = fname + "." + DC.getDate("YYMMDD") + "." + DC.getTime().substring(0,6);
		        if(isExtract()) permanent = false;
	        }
		} else {
			fname = applicationState.envParser.parseProperty(e, this);
		}
		titleSet = false;
		return fname;
	}

	
	private int lineCount() {
		return lineNumber + logicalAdvance;
	}

	private void setLinesPerPage() {
	    if(device.equals("LP")) pageLength = 60;
	    else if(device.equals("RP")) pageLength = 60;
	    else if(device.equals("TP")) pageLength = 66;
	    else if(device.equals("VD")) pageLength = 48;
		else pageLength = 60;
	}
	
	private void writeLine(String buf) throws IOException {
		if(!attr(FD_WRITE)) {
			if(pageLength == 0) setLinesPerPage();
			if(attr(NOINCREMENT)) attr &= ~NOINCREMENT;
			else sequence++;
			fileName = newFileName();
			String f = Util.getFileName(reportPath, fileName);
			Util.mkdir(Util.dirName(f));
			fd = new MssFile(f, "w");
			if (device.equals("EX"))
				fd.setRecordSeparator("0D0A");
			if(extensionClass != null) {
				try {
					extension = (ReportExtension) extensionClass.newInstance();
				} catch (InstantiationException e) {
					throw new IOException(e.getMessage());
				} catch (IllegalAccessException e) {
					throw new IOException(e.getMessage());
				}
				extension.load(this);
			} else extension = null;
			if(fd != null) attr |= FD_WRITE;
			pageNumber = 0;
			lastStation = "";
		}
		
		if(lineCount() >= pageLength) skipOnWrite = true;
		
		if(pageNumber == 0 || (!printingHeader && skipOnWrite)) {
			printingHeader = true;
			if(skipOnWrite) {
				fd.writeNonl("\f");
			}
			pageNumber++;
			lineNumber = 0;
			logicalAdvance = 0;
			skipOnWrite = false;

		    if(standardHeader) writeLine(standardHeaderLine());
			//Class parentClass = parentObject.getClass();
		    Object paramsObj[] = {this, 0};
			try {
				for(int i=0; i<bpFrames.length;i++)
					if(bpFrames[i] != null) bpFrames[i].invoke(parentObject, paramsObj);
			} catch(IllegalAccessException e) {
				// TODO
			} catch(InvocationTargetException e) {
				// TODO
			} catch(Exception e) {
			}
			printingHeader = false;
		}
		if(!printingHeader) {
			if(logicalAdvance < 0) logicalAdvance = 0;
			while(logicalAdvance > 0) {
				fd.writeLine("");
				logicalAdvance--;
				lineNumber++;
				if(lineNumber >= pageLength) {
					lineNumber = pageLength;
					logicalAdvance = 0;
					break;
				}
			}
		}
		fd.writeLine(buf);
		lineNumber++;
	}

	/**
	 * Get the formatted Header i.e. Report Name & Run date 
	 * @return the formatted report header.
	 */
	public String standardHeaderLine() {
	    String spaces = "                               "; //31 spaces
		StringBuilder sb = new StringBuilder();
		String s = reportName;
		if(s.length() > 14) s = s.substring(0,14);
		sb.append("REPORT: " + s);
		if(s.length() < 14) sb.append(spaces.substring(0, 14 - s.length()));
		
		/*
		sb.append(" GENERATED: " + DC.DD_MMM_YY(applicationState.genDate));
	    sb.append(" " + DC.TIME(applicationState.genDate));
		*/
		sb.append(spaces + "RUN: ");
		
	    s = DC.DOW()+" "+ DC.DDMMMYY()+" "+DC.TIME(); // max 23 spaces
		sb.append(s + spaces.substring(0, 37 - s.length()) + "PAGE ");
		
		s = Integer.toString(getPageNumber());
		if(s.length() < 5) sb.append(spaces.substring(0, 5 - s.length()) + s);
		else sb.append(s);
		
		return sb.toString();
	}
	
	/**
	 * Returns the current sequence value i.e. a count of the number of writeLines executed 
	 * Please note that if NOINCREMENT is set this does not get incremented the first time it is called.
	 * @return The number of writeLines performed.
	 */
	public int getSequence() { return sequence; }
	
	/**
	 * See {@link #getSequence()}
	 * @return the sequence with preceding 0's e.g. if sequence was 5 the return would be "005" if the value
	 * is 12345 the value will be "12345"
	 */
	public String getSequenceString() { return Util.format("000", sequence); }
	
	/**
	 * The capitalised value of the first value of the reportId provided during class construction.
	 * @return A - Z or empty string if not provided.
	 */
	public String getShadowName() { 
		if(repId == 0) return "";
		return Character.toString((char)(repId + 'A' - 1));
	}
	
	/**
	 * Set a new title for the print spool
	 * @param title
	 */
	public void setTitle(String ptitle ) {
		title = ptitle;
	}
	/**
	 * Set a new title for the print spool
	 * @param title
	 */
	public void setTitle(Var ptitle ) {
		title = ptitle.toString();
	}
	/**
	 * Get the print spool title
	 * @return
	 */
	public String getTitle() { return title; }

	/**
	 * Check it is an extract as opposed to a report.
	 * @return true if report, false if extract
	 */
	public boolean isExtract() { 
		if(this instanceof Extract) return true;
		return false;
	}
	
	/**
	 * Check it is a report as opposed to an extract.
	 * @return true if report, false if extract
	 */
	public boolean isReport() {
		if(!isExtract()) return true;
		return false;
	}
	
	/**
	 * Set the starting FramState (in fact it's primary method) 
	 * @param order the ordinal value which must be >= 1 <= 98
	 * @param aMethod the named FrameState method 
	 */
	public void beginPage(int order, String aMethod) {
		Class<?> parentClass = parentObject.getClass();
		Class<?> params[] = {Report.class, int.class};

	    if(order < 1 || order > 98) {
	        Util.warning("beginPage order must be from 1 to 98. "+order+" is an illegal value.");
			return;
	    }
	    if(bpFrames == null) {
			bpFrames = new Method[100];
			for(int i=0; i<100; i++) bpFrames[i] = null;
		}
		try {
		    //thisMethod = parentClass.getDeclaredMethod(aMethod, params);
			bpFrames[order] = parentClass.getMethod(aMethod, params);
		} catch(Exception e) {
			Util.error("beginPage(): Method '" + aMethod + "(Report, int)' not found in '"+parentClass.getName()+"'");
		}
	}
	
	/**
	 * Clears down all FrameState registered methods.
	 */
	public void beginPageClear() {
	    if(bpFrames == null) return;
		for(int i=0;i<bpFrames.length;i++) bpFrames[i] = null;
		skipPage();
	}
	
	/**
	 * Clears down a specific FrameState registered method.
	 * @param order the ordinal of the FramteState method to clear down
	 */
	public void beginPageClear(int order) { if(bpFrames != null) bpFrames[order] = null; }

	/**
	 * force a page to be skipped when next write occurs
	 */
	public void skipPage() {
		lineNumber = 0;
		logicalAdvance = 0;
		skipOnWrite = true;
	}
	
	/**
	 * Check to see if an MssFile setting/attribute is in effect. 
	 * @param val an int representing the flag value in question 
	 * @return true if set, false otherwise.
	 */
	public boolean attr(int val) {
		if((attr & val) == 0) return false;
		return true;
	}
	
	/**
	 * Advance 1 line
	 */
	public void skipLine() throws IOException {
		skipLine(1);
	}
	
	/**
	 * Advance n lines if this exceeds the number of remaining lines on the page 
	 * we will advance to the next page.
	 * @param pcount number of lines to advance
	 */
	public void skipLine(long count) throws IOException {
		if(printingHeader) while(count-- > 0) writeLine("");
		else logicalAdvance += count;
		if((lineNumber + logicalAdvance) > pageLength) skipPage();
	}
	
	/**
	 * Prints each value from the current line on a line and clears the values of the provided 
	 * FrameState. Please see {@link FrameState#getPrintString(int)} and {@link FrameState#clear()}. 
	 * @param frame the FrameState holding the data
	 * @throws IOException
	 */
	public void print(FrameState frame) throws IOException { 
		print(frame, 0);
	}
	
	/**
	 * Prints each value of the FrameVar skipping a specified number of lines, clears the values of the provided 
	 * FrameState. Please see {@link FrameState#getPrintString(int)} and {@link FrameState#clear()} 
	 * @param frame the FrameState holding data
	 * @throws IOException
	 */
	public void print(FrameState frame, int line) throws IOException {
		if(attr(RELEASE)) release();
		if(frame == null) return;
		if(line > 0 && line < lineCount()) {
			skipPage();
		}
		while(line-- > 1) skipLine();
		
		int maxLine = frame.getMaxLine();
		String tmpLang;
		if(maxLine == -1) { //Defined language not used in this frame
			tmpLang = frame.getLangDefault();
			maxLine = frame.getMaxLine(tmpLang);
			if(maxLine == -1)
				maxLine = frame.getMaxLineDefault();
		} else tmpLang = null;
		for(int i=1; i <= maxLine; i++) {
//			String s = frame.getPrintString(tmpLang, i);
			String s = frame.getPrintString(tmpLang, i);
			writeLine(s);
			//Util.debug("print line "+i+": [" + s + "]");
		}
		frame.clear();
	}
	
	/**
	 * The ascii value of the first value of the reportId provided during class construction.
	 * @return 64 through 90 (A - Z ascii values) or 32 i.e. space if no report id was set 
	 * during construction.
	 */
	public String getReportId() {
		if(repId == 0) return " ";
		int id = repId + 'A' - 1;
		if(id < 'A' || id > 'Z') id = ' ';
		return Integer.toString(id);
	}

	/**
	 * Gets the current line to which writing will occur in the current page.
	 * @return the current line number
	 */
	public int getLineNumber() { return(lineCount()); }
	
	/**
	 * Overloaded version of {@link #setLineNumber(int)}
	 * @param line a Var of int representing the line number to set the next write too. 
	 */
	public void setLineNumber(Var line) { setLineNumber(line.getInt()); }
	
	/**
	 * Set the line number:
	 * If the line number is > than the page
	 * @param line
	 */
	public void setLineNumber(long line) {
	    if(line < 1) line = 1;
		else if(line > pageLength) line = pageLength;
		lineNumber = (int)line - logicalAdvance;
		if(lineNumber < 1) {
			lineNumber = (int)line;
			logicalAdvance = 0;
		}
	}
	
	/**
	 * What is the current page in the report.
	 * @return the page number where we are currently writing data too.
	 */
	public int getPageNumber() { return pageNumber; }
	
	/**
	 * How many lines before a form feed, lines per page?
	 * @return the number of lines in a page
	 */
	public int getFormDepth() { return pageLength; }
	
	/**
	 * Explicitly set the page number
	 * @param page the page number being set
	 */
	public void setPageNumber(int page) { pageNumber = page; }
	
	/**
	 * Explicitly set the page number, ov
	 * @param page the page number being set
	 */
	public void setPageNumber(Var page) { pageNumber = page.getInt(); }
	
	/**
	 * Set the page length i.e. number of lines on a page before a form feed is written
	 * In effect the same as {@link #setPageNumber(int)}
	 * @param length number of lines per page
	 */
	public void setFormDepth(double length) { pageLength = (int) length; } 

	/**
	 * Set the page length i.e. number of lines on a page before a form feed is written
	 * In effect the same as {@link #setPageNumber(int)}
	 * @param length number of lines per page
	 */
	public void setFormDepth(int length) { pageLength = length; } 

	/**
	 * Set the page length i.e. number of lines on a page before a form feed is written
	 * In effect the same as {@link #setPageNumber(int)} except a Var is being used.
	 * @param length number of lines per page as a Var
	 */
	public void setFormDepth(Var length) { pageLength = length.getInt(); }
	
	/**
	 * Set the user string.
	 * @param user The name of the user
	 */
	public void setUser(String user) { this.user = user; }
	public void setUser(Var user) { this.user = user.getString(); }
	
	/**
	 * Returns a pUser as a Var
	 * @return
	 */
	public Var getUser() { return new Var(user); }
	
	/**
	 * Close the underlying file and set the release attribute. 
	 */
	public void release() {
		if(fd != null) {
			if(extension != null)
				extension.release();
			fd.close();
			fd = null;
			attr &= ~(FD_WRITE);
		}
		attr &= ~(RELEASE);
	}
	
	//ON.CHANGE handling
	ArrayList<OnChange> ochList = null;
	
	/**
	 * This function will perform any on.change code which has been created or else, if OCH
	 * data has not been created, will return true, allowing och code to be created. The OCH
	 * data is executed by calling performOnChange()
	 * Example usage:
	 * if(rep.onChange()) {
	 * 		rep.onChange(new OnChange(this, ochVar).setFooter("frame01").setHeader("frame02");
	 * 		rep.onChange(new OnChange(this, ochVar2).setFooter("frame03").setHeader("frame04");
	 * }
	 * @return false if OCH data exists, true otherwise.
	 * @throws Exception 
	 */
	public boolean onChange() throws Exception {
		if(ochList == null) return true;
		performOnChange();
		return false;
	}
	
	private boolean firstOch = true;
	/**
	 * Add a new OnChange element to the report.
	 * @param och The OnChange object to add
	 * @return The OnChange object just added.
	 */
	public OnChange onChange(OnChange och) {
		if(ochList == null) ochList = new ArrayList<OnChange>();
		firstOch = true;
		ochList.add(och);
		return och;
	}
	/**
	 * Performs all the OnChange frames, as required by changed data. This is normally
	 * not called directly, but by the onChange() call.
	 * @throws Exception
	 */
	public void performOnChange() throws Exception {
		if(ochList == null) return;
		int item;
		
		onChangeName = null;
		//if first time, execute all headers in ascending order
		if(firstOch) {
			for(item = 0; item < ochList.size(); item++) {
				ochList.get(item).initialiseLastBuffer();
				ochList.get(item).changed();
//				ochList.get(item).setNormalValue();
				ochList.get(item).invokeHeader(this);
				ochList.get(item).update();
			}
			firstOch = false;
			return;
		}
		
		//Run through list from 0 to end.
		for(item = 0; item < ochList.size(); item++) {
			//if change happened then:
			boolean changed = ochList.get(item).changed();
			if(changed) {
				if(onChangeName == null)
					onChangeName = ochList.get(item).getChangeVarName();
				//Ensure we're using "unchanged" values for all on.change vars
//				for(int i=item; i < ochList.size(); i++)
//					ochList.get(i).setUnchangedValue();
				//For each extract file, set old buffer as active
				/*
				 * Looks like this is wrong - the tail uses the 'new' buffer, not the old as we thought.
				for(int i=item; i < ochList.size(); i++) {
					ochList.get(i).setExtractToLastBuffer();
				}
				*/
				//run through list backwards from end to this item and execute footers.
				for(int i=ochList.size()-1;i>=item;i--)
					ochList.get(i).invokeFooter(this);
				
				//For each extract file, set current buffer as active
				/*
				for(int i=item; i < ochList.size(); i++)
					ochList.get(i).setExtractToCurrentBuffer();
				*/
				//revert from "unchanged" values to the new "changed" values
//				for(int i=item; i < ochList.size(); i++)
//					ochList.get(i).setNormalValue();
				//run from this item to end, executing headers
				for(;item < ochList.size(); item++)
					ochList.get(item).invokeHeader(this);
			}
		}
		for(item = 0; item < ochList.size(); item++) {
			ochList.get(item).update();
		}
		
	}
	
	/**
	 * Closes the OnChange loop and removes the list of OnChange objects. Before removing the
	 * list, it runs through all the items and invokes the footer frames.
	 * @throws Exception
	 */
	public void onChangeClose() throws Exception {
		if(ochList == null) return;
		//Run through all the footers.
//		for(int i=ochList.size()-1;i>=0;i--)
//			ochList.get(i).setExtractToLastBuffer();
		for(int i=ochList.size()-1;i>=0;i--) {
			ochList.get(i).invokeFooter(this);
		}
//		for(int i=ochList.size()-1;i>=0;i--)
//			ochList.get(i).setExtractToCurrentBuffer();
		ochList = null;
		firstOch = true;
		onChangeName = null;
	}
	public String onChangeName() {
		return onChangeName;
	}
	
	/**
	 * Check whether GLB.STN has changed for this reports. if it has then release report
	 * 
	 * @return 1 if 1st call 2 if stn has changed otherwise 0.
	 * @throws Exception 
	 */
	public int checkRelease () {
		if(lastStation.length() == 0) {
			lastStation=station;
			return 1;
		}
		if(!lastStation.equals(station)){
			release();
			lastStation=station;
			return 2;
		}
		return 0;
	}
	
	public void setBackup(Var pBackup) {
		setBackup(pBackup.getString().trim());
	}
	public void setBackup(String pBackup) {
		backup=pBackup;
	}
	public void setPrintat(Var pPrintat) {
		setPrintat(pPrintat.getString().trim());
	}
	public void setPrintat(String pPrintat) {
		printat=pPrintat;
	}
}

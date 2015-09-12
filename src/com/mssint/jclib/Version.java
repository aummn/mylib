package com.mssint.jclib;


/**
*
* 1.02.16 2010/01/18
*	Add initialise() method to Group
*
* 1.02.17 2010/01/31 - Alan
*	fixed IE6 issue -- XmlManager -- processRequest
*	null pointer exceptions on GUI forms xmlManager processGUIItems()
*	corrected indexes for 1 based data data assignment ParameterWrapper set(string[] values)
*
* 1.02.18 2010/09/10 - Alan
*	corrected Var function toJulian to cope with daylight savings.
*
* Date		:	15/02/2010	
* Author	:	Alan 
* Version	:	1.1.19
* 
* Updated system to support , as a decimal separator, note this is only handled from a validation perspective 
* in the GUI currently, ParameterWrapper. 
*
* Version 1.2.20 ????
*
* Date		:	09/03/2010	
* Author	:	Pete 
* Version	:	1.1.21
* 
* McsLite using "properties" instead of "this.properties" - caused null pointer exception.
* 
* Date		:	11/03/2010	
* Author	:	Pete 
* Version	:	1.1.22
* 
* Date		:	24/03/2010	
* Author	:	Alan 
* Version	:	1.2.23 
* 
* OnlineWrapperEJB now handles trailing or leading spaces on repositoryNames (drop-down or list-box lists for GUI)
* 
* Date		:	05/04/2010	
* Author	:	Alan 
* Version	:	1.2.24 
* 
* Var lincstrtodouble does not handle trailing sign elements.
* extractNumeric(...) in ParameterWrapper altered to remove trailing sign. 
* If -ve minus sign set at beginning.
*
* Date		:	06/04/2010	
* Author	:	Pete 
* Version	:	1.2.25
* 
* CursorState, FrameVar, Group
*             - fixed issues with extract string lengths.
* OnChange    - fixed exception when only a FOOTING was specified.
* ScreenState, CursorState
*             - Added cursor persistence facility
*
* Date		:	14/04/2010	
* Author	:	Alan
* Version	:	1.2.26
* 
* 
* changes to support over-writing combo-box values via a new index lookup mechanism
* along with clear down ability of listbox and combo box data when using sendllistadd with a " " or "  " etc....            
*
*             
* Date		:	18/04/2010	
* Author	:	Alan
* Version	:	1.2.27
* 
* 
* OnlineWrapperEJB fixes to differentiate between repository clear down and actual space delimeted key/value 
* pair.             
*             
* Date		:	20/04/2010
* Author	:	Pete
* Version	:	1.2.28
*
* Comparison between string and long should use string comparison not numeric.
*
* Date		:	28/04/2010
* Author	:	Pete
* Version	:	1.2.29
*
* Fix problem with loading sendlist values when value < 3 characters.
*
* Date		:	28/04/2010
* Author	:	Pete
* Version	:	1.2.30
*
* Possible null pointer exception (Var)
* Null pointer (ScreenState)
* Add linenumbers to error messages in copyfrom forms (ScreenState)
* Correctly initialise OnChange on first header production.(Report)
* Fix callPipe(): handle exception in try/catch (OnlineWrapperEJB)
* Fix handling of Windows paths (MssFile)
* Fix error message propagation when startup of one screen calls startup of another (ApplicationState)
*
* Date		:	16/05/2010
* Author	:	Pete
* Version	:	1.2.31
* 
* Add jclib.decimal property
* Provide thousands separator facility
* Add leading sign 
*
* Date		:	16/06/2010
* Author	:	Pete
* Version	:	1.2.32
* 
* Set GLB.LANGUAGE, GLB.STALANG and GLB.REPLANG by config.
*
* Date		:	22/06/2010
* Author	:	Pete
* Version	:	1.2.33
* 
* Add new class SystemDate
* All Date functions now use SystemDate instead of standard date routines
* Add glbStyle and GLB.STYLE support
* Handle multi-language in frames
* Allow extract file records to auto-adjust record lengths to longest record
* beginPageClear() should trigger a skipPage on next write.
* 
* Date		:	22/06/2010
* Author	:	Pete 
* Version	:	1.2.34
* 
* Date		:	21/07/2010
* Author	:	Pete 
* Version	:	1.2.35
* 
* New McsConnectionFactory
*
* Date		:	10/08/2010
* Author	:	Pete 
* Version	:	1.2.38
*
* Fix onChange to properly handle Database types as pointers in FrameVar. 
* 
* Date		:	10/08/2010
* Author	:	Pete 
* Version	:	1.2.39
*
* Many double issues. 
* 
* Date		:	12/08/2010
* Author	:	Pete 
* Version	:	1.2.41
*
* More rounding issues and string assignment
*
* Date		:	16/08/2010
* Author	:	Pete 
* Version	:	1.2.43
*
* Preserve spaces when moving from char/group to numeric group ad back.
*
* Date		:	18/08/2010
* Author	:	Pete 
* Version	:	1.2.44
*
* Various group assignment problems.
*
* Date		:	19/08/2010
* Author	:	Pete 
* Version	:	1.2.45
*
* Fixed record length adjustment and record-separator for extract files.
* Force delete of PERMANENT extract files on first open
*
* Date		:	06/09/2010
* Author	:	Steve
* Version	:	1.2.46
*
* Fixed problem with NumberExceptions
*
* Date		:	24/10/2010
* Author	:	Pete
* Version	:	1.2.50
*
* Added caching for cursors
*
* Date		:	03/11/2010
* Author	:	Pete
* Version	:	1.2.51
*
* Fix clearing of files on release()
* Enable stats to OnChange
* Fix OnChange stats problems
* Add static releaseAll methods to Extract and Report classes
*
* Date		:	09/11/2010
* Author	:	Pete
* Version	:	1.2.52
*
* Fix clearing of files on release()
*
* Date		:	17/11/2010
* Author	:	Pete
* Version	:	1.2.53
*
* Handle report line counts on a lang basis
* 
* Date		:	01/12/2010
* Author	:	Pete
* Version	:	1.2.54
*
* Problem with cache and persisted cursors.
* Cache now handles conditional profiles.
* 
* Date		:	12/01/2011
* Author	:	Steve
* Version	:	1.2.55
*
* Prevent release() from being called during printing of headers.
*
* Date		:	25/01/2011
* Author	:	Pete
* Version	:	1.2.56
*
* Fixup handling of Util.setArray() and Util.getArray() methods.
* Ensure that frame lines which do not set any specific languge are used for all prints.
*
* Date		:	07/02/2011
* Author	:	Steve
* Version	:	1.2.57
*
* Fixup setDivide() handling of remainders.
*
* Date		:	10/02/2011
* Author	:	Pete
* Version	:	1.2.58
*
* Fixup multidimensional array problem
*
* Date		:	24/02/2011
* Author	:	Pete
* Version	:	1.2.59
*
* getString() from numeric group should always be numbers - no spaces.
*
* Date		:	28/02/2011
* Author	:	Pete
* Version	:	1.2.60
*
* If a frame has no entries for a particular frame then print the default language lines.
*
* Date		:	10/03/2011
* Author	:	Pete
* Version	:	1.2.61
*
* Add an option to Report() to allow things like "jclib.report.header=true" in Report constructors.
*
* Date		:	22/03/2011
* Author	:	Pete
* Version	:	1.2.62
*
* Correctly handle length adjustments for $ and separator attributes
* Fix static vs local handling of status variables
*
* Date		:	01/04/2011
* Author	:	Pete
* Version	:	1.2.63
*
* Added option "jclib.space.zero.equal" for deciding whether zero == space or not.
*
* Date		:	04/04/2011
* Author	:	Pete
* Version	:	1.2.64
* 
* Fixed 'times' to properly handle increased scale
*
* Date		:	12/04/2011
* Author	:	Pete
* Version	:	1.2.65
*
* Fixed log10() for negative numbers.
*
* Date		:	01/07/2011
* Author	:	Pete
* Version	:	1.2.66
*
* Dynamic select boxes get cleared during preScreen phase
*
* Date		:	07/07/2011
* Author	:	Pete
* Version	:	1.2.67
*
* Dynamic select boxes get cleared in the EJB directly at the start of the commit and startup phase
*
* Date		:	10/07/2011
* Author	:	Pete
* Version	:	1.2.68
*
* Some documentation, fixed names, added dummy staticListAdd() calls.
* Fixed naming problem with dynamic lists.
* 
* Date		:	14/07/2011
* Author	:	Pete
* Version	:	1.2.69
*
* Added new /nof/repository servlet, to handle the creation of static lists
* Added code to get staticListAdd() to work.
* 
* Date		:	08/08/2011
* Author	:	Pete
* Version	:	1.2.70
* 
* Don't remove duplicate entries in lists.
* 
* Date		:	11/08/2011
* Author	:	Steve
* Version	:	1.2.70
*
* Changed Sql.sleepuntiwoken to return string not set GLB.PARAM
* 
* Date		:	17/08/2011
* Author	:	Pete
* Version	:	1.2.71
*
* Fixed confusion over lists inside copyfroms as well as 1-relative array confusion
* 
* Date		:	30/08/2011
* Author	:	Pete
* Version	:	1.2.72
*
* Extract fetch() - if record number out of range, set Glb.status instead of throwing exception
* 
* Date		:	13/09/2011
* Author	:	Steve
* Version	:	1.2.73
*
* Update Glb.PARAMFLAG for wake up calls.
*
* Date		:	16/06/2013
* Author	:	Pete
* Version	:	1.2.74
*
* Cleanup some warnings:
* - Removed unused variables
* - Fixed leaks - ensure files closed on exceptions
* - Removed unused imports
* - Added @suppress in Dasdl
*
* Update Glb.PARAMFLAG for wake up calls.
*
* Date		:	03/07/2013
* Author	:	Pete
* Version	:	1.2.75
*
* Added some stuff for NOF
*/

public class Version {
//	private static final Logger log = LoggerFactory.getLogger(Version.class);
	public static final int VERSION_MAJOR = 1;
	public static final int VERSION_MINOR = 3;
	public static final int VERSION_RELEASE = 8;

	public static String NAME = "Jclib";
	public static String COMPANY = "MSS International Ltd.";
	
	@SuppressWarnings("unused")
	public static String version() {
		String m = VERSION_MINOR < 10 ? 
			"0" + VERSION_MINOR : Integer.toString(VERSION_MINOR);
		String r = VERSION_RELEASE < 10 ? 
			"0" + VERSION_RELEASE : Integer.toString(VERSION_RELEASE);
		return VERSION_MAJOR + "." + m + "." + r; 
	}
	
	public static String print() {
		return NAME + " Version " + version(); 
	}
	public static String printFull() {
		return print() + " - (c) " + COMPANY; 
	}
	
	public static void main(String [] args) {
		if(args.length > 0) {
			if(args[0].equals("-v")) System.out.println(version());
			else System.err.println("usage: Version [-v]");
		} else
			System.out.println(printFull());
	}
}

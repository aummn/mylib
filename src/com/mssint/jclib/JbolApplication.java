package com.mssint.jclib;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.sql.Connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This is the primary class for Jbol applications. 
 * All applications should extend JbolApplication.
 * 
 * This class acts as the framework/container for executing
 * a migrated Cobol program i.e. its sections {@see} JbolSection and paragraphs {@see} Paragraph.
 * 
 * It provides the execution framework and flow of control to the contained sections and their contained paragraphs. 
 * 
 * @author MssInt
 * 
 */
public class JbolApplication extends Thread {
	private static final Logger log = LoggerFactory.getLogger(JbolApplication.class);
	
	{
		log.info("Loaded {}", this.getClass().getName());
	}

	public JbolApplication jbolApp = this;
//	public Group lowValues = new Group(Var.CHAR,3000);
//	public Group highValues = new Group(Var.CHAR,3000);
	private static Var zeros;
	private static Var effs;
	
	private ArrayList<Paragraph> paragraphs = new ArrayList<Paragraph>();
	private Map<String, Integer> paragraphSequences = new HashMap<String, Integer>();
	private int currentSequence = 0;
	private MssFile tempFile = null;
	private boolean tempModeRead = true;

	protected boolean restarted = false;
	private boolean canThrowThreadDeath = false;
	private boolean endExecution = false;
	
	//Handle DMERROR messages for Unisys
	public int [] DMSTATUS = new int[3];
	public static final int DMCATEGORY = 0;
	public static final int DMERRORTYPE = 1;
	public static final int DMSTRUCTURE = 2;
	public boolean DMERROR = false;
	
	public static final int NOTFOUND = 1; 
	public static final int DUPLICATES = 2;
	public static final int DEADLOCK = 3;
	public static final int DATAERROR = 4;
	public static final int NOTLOCKED = 5;
	public static final int KEYCHANGED = 6; 
	public static final int SYSTEMERROR = 7;
	public static final int READONLY = 8;
	public static final int IOERROR = 9;
	public static final int LIMITERROR = 10;
	public static final int OPENERROR = 11;
	public static final int CLOSEERROR = 12;
	public static final int NORECORD = 13;
	public static final int INUSE = 14;
	public static final int AUDITERROR = 15;
	public static final int ABORT = 16;
	public static final int SECURITYERROR = 17;
	public static final int VERSIONERROR = 18;
	public static final int FATALERROR = 19;
	public static final int INTEGRITYERROR = 20;
	public static final int INTLIBERROR = 21;
	public static final int TAPESETERROR = 22;
	
	private Console console;
	
	private int snapshotSize;
	//Class used to store Group snapshot values.
	protected class GroupValues {
		private GroupByteArray gba; //Pointer to the Group base array
		private byte [] snapshot; //The snapshot of the group data.
		public GroupValues(GroupByteArray gba) {
			this.gba = gba;
			snapshot = null;
		}
		/**
		 * Take a snapshot of the current value.
		 */
		public void save() {
			snapshot = gba.createSnapshot();
			snapshotSize += snapshot.length;
		}
		/**
		 * Restore the saved snapshot value
		 */
		public void restore() {
			gba.loadSnapshot(snapshot);
		}
	}
	private List<GroupValues>groupSnapshotList;
	
	/**
	 * Each executed Paragraph will log it's execution if this flag is true.
	 * By default it is set to false and should modified via {@link #traceParagraphs(boolean)} 
	 */
	public boolean trackParagraphs;
	
	
	public boolean DMSTATUS(int status) { 
	    if(DMSTATUS[DMCATEGORY] == status) return true;
		return false; 
	} 
	
	/**
	 * Clear the Database Management flags/settings. 
	 * This is to simulate the Unisys 
	 */
	public void clearDmstatus () {
	    DMERROR = false; 
	    DMSTATUS[DMCATEGORY] = 0; 
	    DMSTATUS[DMERRORTYPE] = 0; 
	    DMSTATUS[DMSTRUCTURE] = 0;
	}
	
	/**
	 * Set the Database Management flags/settings.
	 * This is to simulate the Unisys 
	 */
	private void setDmstatus(int structure,int category,int errorType) {
	    DMERROR = true; 
	    DMSTATUS[DMCATEGORY] = category; 
	    DMSTATUS[DMERRORTYPE] = errorType; 
	    DMSTATUS[DMSTRUCTURE] = structure;
	}
	
	/**
	 * Given a connection let's set the DMS status to be 
	 * clear if the connection is not null and to an error status
	 * if it is null   
	 * @param con
	 * @return
	 */
	public boolean openDb(Connection con){
		clearDmstatus();
		if(con == null) {
			setDmstatus(0,11,92);
			return false;
		}
		return true;
	}

	//Instance level initialiser. Register the instance with the Sql class.
	{
		Sql.init(this);
	}
	
	/**
	 * Allows for threaded execution
	 */
	public void run() {
		if(log.isDebugEnabled())
			log.debug("run method called");
		try {
			canThrowThreadDeath = true;
			runProgram(startParagraph);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Add/register a Paragraph with the JbolApplication instance.
	 * @param para
	 * @return
	 */
	public int addParagraph(String name, Paragraph para) {
		if(para == null || name == null)
			return -1;

		int sequence = paragraphs.size();
		paragraphs.add(para);
		paragraphSequences.put(name, sequence);
		return sequence;
	}
	
	protected void replaceParagraph(int sequence, Paragraph para) {
		paragraphs.set(sequence, para);
	}
	/**
	 * How many Paragraphs have been added/registered
	 * @return
	 */
	public int getParagraphSize() { return paragraphs.size(); }

	/**
	 * Log the End of Job
	 * Date & Time, Program/Process Id, User amd Program Name.
	 */
	public void endOfJob(int rVal) {
		Date d = SystemDate.getDate();
		SimpleDateFormat dt = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		log.info("EOJ: " + dt.format(d) + 
		        " Program=" + this.getClass().getSimpleName() +
				" PID=" + Util.getPid() +
				" User="+ System.getProperty("user.name"));
				//" Java Version="+System.getProperty("java.version"));
		//System.exit(rVal);
		
	}
	
	/**
	 * If not termination of execution log end of job details {@link #endOfJob(int)}
	 */
	public void endOfJob() {
		if(!endExecution) endOfJob(0);
	}
	

	/**
	 * Constructor
	 * @throws Exception 
	 */
	public JbolApplication() throws Exception {
		trackParagraphs = false;
//		lowValues.fillHex("00");
//		highValues.fillHex("FF");
	}

	/**
	 * Enable/Disable executing Paragraphs logging via their perform() method
	 * @param w true if logging is to be enabled false for no logging.
	 */
	public void traceParagraphs(boolean w) { trackParagraphs = w; }
	
	/**
	 * Get the temporary working file available to Sections or their Paragraphs. 
	 * @return an instance of the temporary working file.
	 */
	public MssFile getTempFile() { return tempFile; }
	
	/**
	 * Allow caller to specify the temporary working file.
	 * @param fd
	 */
	public void setTempFile(MssFile fd) { tempFile = fd; }
	
	/**
	 * Close and drop reference to temporary file.
	 */
	public void clearTempFile() {
		if(tempFile != null && tempFile.isOpen()) tempFile.close();
		tempFile = null; 
	}
	
	private Paragraph startParagraph = null;
	
	/**
	 * Executes the entire program, starting at the first paragraph of the
	 * first section. As each paragraph returns, if it returns a Paragraph then
	 * runProgram() will execute that paragraph and continue execution at the next.
	 * Otherwise, if null is returned, runProgram() will execute the next 
	 * paragraph in the sequence. When no more paragraphs are available in the
	 * list, runProgram() will return, thus ending the program.
	 */
	public void setProgramStart(Paragraph startPara) {
		startParagraph = startPara;
	}
	
	/**
	 * Run an already instantiated program, starting with any paragraph. Program execution 
	 * continues normally until the end of the program, or else until an endProgram() is 
	 * encountered.
	 * @param startPara The section.paragraph to start.
	 * @return 0
	 * @throws Exception any exception that occurs during the running of the program.
	 */
	public int runProgram(Paragraph startPara) throws Exception
	{
		Paragraph para = startPara;
		endExecution = false;
		
		while(true) {
			if(para == null) {
				currentSequence++;
				if(currentSequence >= paragraphs.size()) {
					//We've reached the end of the program
					break;
				}
				para = paragraphs.get(currentSequence);
			} else currentSequence = para.getSequence();
			if(para == null) {
				throw new IllegalStateException("Error: Unexpected null paragraph. Sequence=currentSequence");
			}
			//Execute the paragraph.
			try {
				para = para.perform();
			} catch (EndJbolProgramException e) {
//				System.out.println(e.getMessage());
				return -1;
			}
			if(endExecution) break;
		}
		if(!endExecution) endOfJob();
		return 0;
	}
	
	/**
	 * 
	 */
	public void _endProgram() {
		endExecution = true;
		throw new EndJbolProgramException("STOP RUN called for "+this.getClass().getName());
	}
	
	public boolean isEndProgram() {
		return endExecution;
	}
	
	/**
	 * Not implemented as yet!
	 * @param section
	 */
	public void setInterruptSection(JbolSection section) {
		//TODO - something needs to be done here
		;
	}
	
	public void createTempFile() {
		tempFile = new MssFile("./", "wt");
		tempModeRead = false;
	}
	
	/**
	 * Utility function to create an array of dimension 2 for the 2 provided objects.
	 * @param a
	 * @param b
	 * @return
	 */
	public Object [] performArray(Object a, Object b) {
		return new Object [] { a, b };
	}

	/**
	 * Write a Var to the Temporary File
	 * @param record the data to be written as a Var
	 */
	public void writeTemp(Var record) {
		writeTemp(record.getString());
	}
	
	/**
	 * Write a String to the Temporary file.
	 * @param str the String to write.
	 */
	public void writeTemp(String str) {
		if(tempFile == null || tempModeRead) {
			tempFile = new MssFile("./", "wt");
			tempModeRead = false;
		}
		try {
			//tempFile.write(str + "\n");
			tempFile.write(str);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Read the first line from the temporary file
	 * @return the line as a String. 
	 * If the temporary file has not been initialised or has been cleared then null is returned.
	 * @throws IOException cannot access file.
	 */
	public String readTemp() throws IOException {
		if(!tempModeRead) {
			tempFile.flush();
			tempFile.reOpen("r");
			tempModeRead = true;
		}
		if(tempFile == null) return null;
		String str;
		try {
			str = tempFile.readLine();
		} catch (IOException e) {
			return null;
		}
		return str;
	}
	
	/**
	 * Attempts to read the value into the provided Var
	 * @param v the Var into which the line must be read.
	 * @return true if the Var was read false if it cannot be read into the Var.
	 * @throws IOException cannot access file.
	 */
	public boolean readTemp(Var v) throws IOException {
		if(!tempModeRead) {
			tempFile.flush();
			tempFile.reOpen("r");
			tempModeRead = true;
		}
		if(tempFile == null) return false;
		boolean rval = tempFile.read(v);
		return rval;
	}
	
	private int _taskValue = 0;
	boolean _taskValueInitialised = false;
	
	public boolean setTaskValue(Var v) {
		_taskValue = v.getInt();
		_taskValueInitialised = true;
		return true;
	}
	
	public boolean setTaskValue(int i) {
		_taskValueInitialised = true;
		_taskValue = i;
		return true;
	}
	
	public int getTaskValue() {
		if(!_taskValueInitialised) {
			String tv = System.getenv("GLB_TASK");
			if(tv != null) {
				try {
					_taskValue = Integer.parseInt(tv);
				} catch (Exception e) {
					_taskValue = 0;
				}
			}
		}
		_taskValueInitialised = true;
		return _taskValue;
	}
	
	/**
	 * Equivalent to the COBOL perform. Executes a paragraph or section and 
	 * returns control to the immediate next line when complete. A section is
	 * the instance of the class which encloses the defined Paragraph methods.
	 * 
	 * When executing a section (class), each paragraph (method) defined in the
	 * class is executed in sequence, unless interrupted by a <b>gotoParagraph()</b>.
	 * @param para The paragraph (type Paragraph) or section to execute.
	 * @return A pointer to the next Paragraph to perform
	 */
	public Paragraph perform(Paragraph para) throws Exception {
		//log.debug("perform(Paragraph) "+para.getClass().getName());
		int endSequence = para.getSequence();
		int thisSequence;
		while(true) {
			
			thisSequence = para.getSequence();
			
			
			final Object [] params = {};
			try {
//				if(section.jbolApplication.trackParagraphs)
//					if(log.isDebugEnabled())
//						log.debug("Start Paragraph "+methodName);
				para = (Paragraph)para.method.invoke(para.instanceClass, params);
//				if(section.jbolApplication.trackParagraphs)
//					if(log.isDebugEnabled())
//						log.debug("End Paragraph "+methodName);
			} catch(IllegalAccessException e) {
				Throwable ne = e.getCause();
				throw (Exception)ne;
			} catch(InvocationTargetException e) {
				String s = e.getTargetException().toString();
				if(s.compareTo("java.lang.ThreadDeath") == 0)
					throw new ThreadDeath();
				Throwable ne = e.getCause();
				throw new RuntimeException(ne);
//				throw (Exception);
			}
			
			
			
			
//			para = para.perform();
			if(para == null) {
				if(thisSequence == endSequence) break;
				if(endExecution) break;
				thisSequence++;
				para = paragraphs.get(thisSequence);
			}
		}
		return para;
	}
	
	private boolean exitPerform = false;
	
	/**
	 * Equivalent to the COBOL PERFORM THRU. Executes paragraphs starting at
	 * para1 and continues executing (in sequence) until para2 returns. 
	 * Control to the immediate next line when complete. A section is
	 * the instance of the class which encloses the defined Paragraph methods.
	 * When executing a section (class), each paragraph (method) defined in the
	 * class is executed in sequence, unless interrupted by a <b>gotoParagraph()</b>.
	 * @param para1 The paragraph (type Paragraph) or section to execute from.
	 * @param para2 The paragraph (type Paragraph) or section to execute to.
	 * @return A pointer to the next Paragraph to perform
	 */
	public Paragraph perform(Paragraph para1, Paragraph para2) throws Exception {
		Paragraph para;
		int endSequence;
		int thisSequence;
		if(para2 == null) endSequence = para1.getSequence();
		else endSequence = para2.getSequence();

		para = para1;
		while(true) {
			thisSequence = para.getSequence();
			para = para.perform();
			if(para == null) {
				if(exitPerform) {
					exitPerform = false;
					break;
				}
				if(thisSequence == endSequence) break;
				thisSequence++;
				if(thisSequence >= paragraphs.size()) {
					endOfJob();
					break;
				}
				if(endExecution) {
					break;
				}
				para = paragraphs.get(thisSequence);
			}
		}
		return null;
	}
	
	public Paragraph exitPerform() {
		exitPerform = true;
		return null;
	}
	
	/**
	 * Equivalent to the COBOL perform. Executes a section and 
	 * returns control to the immediate next line when complete. A section is
	 * the instance of the class which encloses the defined Paragraph methods.
	 * Each paragraph (method) defined in the class is executed in sequence, 
	 * unless interrupted by a <b>gotoParagraph()</b>.
	 * @param section The JBolSection to execute.
	 * @return A pointer to the next Paragraph to perform
	 */
	public Paragraph perform(JbolSection section) throws Exception {
		if(section == null || section.getParagraphCount() == 0) return null;
		Paragraph p1, p2;
		p1 = section.getFirstParagraph();
		p2 = section.getLastParagraph();
		return perform(p1, p2);
	}

	/**
	 * 
	 * @param o1
	 * @param o2
	 * @return
	 * @throws Exception
	 */
	public Paragraph perform(Object o1, Object o2) throws Exception {
		Paragraph p1, p2;
		if(o1 instanceof Paragraph) p1 = (Paragraph)o1;
		else if(o1 instanceof JbolSection) p1 = ((JbolSection)o1).getFirstParagraph(); 
		else p1 = null;
		if(o2 instanceof Paragraph) p2 = (Paragraph)o2;
		else if(o2 instanceof JbolSection) p2 = ((JbolSection)o2).getLastParagraph(); 
		else p2 = null;
		
		return perform(p1, p2);
	}

	/**
	 * Simulate the GOTO functionality of Cobol in particular to a Paragraph.
	 * @param para the Paragraph to jump/branch too. 
	 * @return A pointer to the next Paragraph to perform
	 */
	public Paragraph gotoParagraph(Paragraph para) {
		return para;
	}
	
	/**
	 * Simulate the GOTO functionality of Cobol in particular to the first Paragraph within a section.
	 * Is equivalent in function to {@link #gotoSection(JbolSection)}
	 * @param section the Section to jump/branch too. 
	 * @return A pointer to the next Paragraph to perform
	 */
	public Paragraph gotoParagraph(JbolSection section) {
		if(section == null) return null;
		return section.getFirstParagraph();
	}
	
	/**
	 * Simulate the GOTO functionality of Cobol in particular to the first Paragraph within a section.
	 * Is equivalent in function to {@link #gotoParagraph(JbolSection)}
	 * @param section the Section to jump/branch too. 
	 * @return A pointer to the next Paragraph to perform
	 */
	public Paragraph gotoSection(JbolSection section) {
		if(section == null) return null;
		return section.getFirstParagraph();
	}
	
	/**
	 * Get usercode we are running under
	 * @return user code
	 */
	public String getUserCode() {
		return System.getProperty("user.name");
	}
	/**
	 * Get host name we are running on
	 * @return host name
	 */
	public String getHostName() {
		InetAddress i=null;
		try {
			i = InetAddress.getLocalHost();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return i.getHostName();
	}
	/**
	 * setMaxWait does nothing yet
	 */
	public void setMaxWait(int maxWait) {
		return;
	}
	/**
	 * setMaxWait does nothing yet
	 * @return 0
	 */
	public int getMaxWait() {
		return 0;
	}
	/**
	 * getSwitch does nothing yet
	 * @return 0
	 */
	public int getSwitch(int sw) {
		return 0;
	}
	
	/**
	 * getLineNumber returns the current line number
	 * @return  line number
	 */
	public int getLineNumber() {
		int lineNumber=new Exception().getStackTrace()[1].getLineNumber();
		return lineNumber;
	}
    /**
	 * Get Our familyname (this is irrelevant in Java)
	 * @return Dummy 
	 */
	public String getMyFamilyName() {
	   return " ";
    }

	
	public int cblDeleteFile(String fileName) {
		File file = new File(fileName.trim());
		 
		file.delete();
		return 0;
	}
	public int cblDeleteFile(Var fileName) {
		return cblDeleteFile(fileName.getString());
	}
	public int cblLocateFile(Var fileSpec, Var userMode, Group actualFileSpec, Var existFlag, Var pathFlag) {
		//This is not a 100% replication of CBL_LOCATE_FILE but is enough to provide Integra with what they need
		String s = fileSpec.getString();
		String fileName = s;
		if(s.substring(0, 1).equals("$")) {
			int i = s.indexOf('/');
			String path=System.getenv(s.substring(1, i));
			if (path == null) return 1;
			fileName = path + "/" + s.substring(i + 1);
			pathFlag.set(1);
		} else pathFlag.set(0);
		//Config.log.debug("cblLocateFile name=" + fileName);
		File file = new File(fileName.trim());
		actualFileSpec.getMember(0).set(fileName.length());
		actualFileSpec.getMember(1).set(fileName);
		 
		if(file.exists()) existFlag.set(0);
		else existFlag.set(1);
		return 0;
	}
	public int cblCheckFileExist(String fileName, String fileDetails) {
		File file = new File(fileName.trim());
		 
		if(file.exists()) return 0;
		else return -1;
	}
	public int cblCheckFileExist(Var fileName, Var fileDetails) {
		return cblCheckFileExist(fileName.getString(), fileDetails.getString());
	}
	public int cblGetCurrentDir(Var flags, Var Size, Var dirName) {
		File currentDirectory = new File(new File(".").getAbsolutePath());
		try {
			dirName.set(currentDirectory.getCanonicalPath());
		} catch (IOException e) {
			return -1;
		}
		return 0;
	}
	public int cblCopyFile(String oldName, String newName) throws IOException {
		File oFile = new File(oldName.trim());
		File nFile = new File(newName.trim());
		 
		Files.copy(oFile.toPath(), nFile.toPath());
		return 0;
	}
	public int cblCopyFile(Var oldName, Var newName) throws IOException {
		return cblCopyFile(oldName.getString(), newName.getString());
	}
	public int cblCopyFile(Var oldName, String newName) throws IOException {
		return cblCopyFile(oldName.getString(), newName);
	}
	public int cblCopyFile(String oldName, Var newName) throws IOException {
		return cblCopyFile(oldName, newName.getString());
	}
	
	public String getZeros(int len) {
		if(zeros == null)
			zeros = new Var(Var.CHAR,3000).fill(0);
		return zeros.substr(0, len);
	}
	public String getEffs(int len) {
		if(effs == null)
			effs = new Var(Var.CHAR,3000).fill("F");
		return effs.substr(0, len);
	}
	
	//Console operations
	/**
	 * Display text on the console
	 * @param text
	 */
	public void display(String text) {
		if(console == null)
			console = new Console();
		if(!text.endsWith("\n"))
			text += "\n";
		console.display(text);
	}
	
	/**
	 * Display text at the specified row/column
	 * @param row The row to display the text, 1 relative.
	 * @param col The column to display the text, 1 relative.
	 * @param text Text to display
	 */
	public void displayAt(int row, int col, String text) {
		if(console == null)
			console = new Console();
		console.displayAt(row, col, text);
	}

	/**
	 * Display text at the specified row/column
	 * @param row The row to display the text, 1 relative.
	 * @param col The column to display the text, 1 relative.
	 * @param text Text to display
	 */
	public void displayAt(int row, int col, Var text) {
		if(console == null)
			console = new Console();
		console.displayAt(row, col, text.getString());
	}
	
	/**
	 * Clears the console screen
	 */
	public void clearScreen() {
		if(console == null)
			console = new Console();
		console.clearScreen();
	}
	
	/**
	 * Pause the program and read text typed by the user, from the console.
	 * @return
	 */
	public String accept() {
		if(console == null)
			console = new Console();
		try {
			return console.accept();
		} catch (IOException e) {
			log.info("No console found");
		}
		return "";
	}

	/**
	 * Place the cursor at the specified row and column, and then pause the
	 * program and read text typed by the user, from the console.
	 * @param row Row to position cursor
	 * @param col Column to position cursor
	 * @return The text typed by the user, or an empty string.
	 */
	public String acceptAt(int row, int col) {
		if(console == null)
			console = new Console();
		try {
			return console.acceptAt(row, col);
		} catch (IOException e) {
			log.info("No console found");
		}
		return "";
	}

	/**
	 * Place the cursor at the specified row and column, and then pause the
	 * program and read text typed by the user, from the console.
	 * @param row Row to position cursor
	 * @param col Column to position cursor
	 * @param var A Var or Group which will be set with whatever was typed.
	 * @return The text typed by the user, or an empty string.
	 */
	public String acceptAt(int row, int col, Var var) {
		if(console == null)
			console = new Console();
		try {
			return console.acceptAt(row, col, var);
		} catch (IOException e) {
			log.info("No console found");
		}
		return "";
	}
	
	/**
	 * Takes a snapshot of all Group data for later restoring. Use loadGroupSnapshot() to restore the
	 * snapshot. Note that only Group variables defined as fields in the enclosing class are saved.
	 */
	public void createGroupSnapshot() {
		long time = System.currentTimeMillis();
		if(groupSnapshotList == null) {
			groupSnapshotList = new ArrayList<JbolApplication.GroupValues>();
			//Create a list of all Groups used in the class.
			for(Field field : this.getClass().getDeclaredFields()) {
				try {
					field.setAccessible(true);
					Object object = field.get(this);
					if(object == null)
						continue;
					if(!(object instanceof Group))
						continue;
					Group g = (Group)object;
					GroupByteArray gba = g.getGroupByteArray();
					if(gba == null)
						continue;
					if(gba.isRegistered())
						continue;
					groupSnapshotList.add(new GroupValues(gba));
					gba.register();
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		snapshotSize = 0;
		//Save the group values.
		for(GroupValues g : groupSnapshotList) {
			g.save();
		}
		
		time = System.currentTimeMillis() - time;
		if(log.isInfoEnabled())
			log.info("Class {}: Group snapshot taken in {} milliseconds. Total size is {} bytes.", this.getClass().getName(), time, snapshotSize);
		
//		System.out.println("Snapshot: "+ groupSnapshotList.size()+" Group objects processed. Time to save="+time);
	}
	
	/**
	 * Restores the values of all Group data saved at the last execution of createGroupSnapshot().
	 */
	public void loadGroupSnapshot() {
//		long time = System.currentTimeMillis();
		if(groupSnapshotList == null)
			return;
		for(GroupValues g : groupSnapshotList) {
			g.restore();
		}
//		time = System.currentTimeMillis() - time;
//		System.out.println("Snapshot: Time to restore="+time);
	}

	/**
	 * Return the sequence for a method of this name.
	 * @param method
	 * @return
	 */
	public int getSequence(String method) {
		Integer seq = paragraphSequences.get(method);
		if(seq == null) {
			return -1;
		}
		return seq;
	}

	/**
	 * Return the paragraph indexed by sequence
	 * @param sequence
	 * @return
	 */
	public Paragraph getParagraph(int sequence) {
		return paragraphs.get(sequence);
	}
	
}

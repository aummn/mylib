package com.mssint.jclib;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Arrays;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.SimpleDateFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * MssFile provides a rich and generalised set of functionality for dealing with many 
 * different file types i.e. the internal layout of the files. 
 * File encoding and character set can are definable.
 * 
 * Internal layout can be Fixed, Record Separator or Variable length in structure.
 * 
 * Files can be accessed in Random, Sequential or Relative manners.
 * 
 * @author MssInt
 *
 */
public class MssFile {
	private static final Logger log = LoggerFactory.getLogger(MssFile.class);
	private static final int READ 		= 000000001;
	private static final int WRITE 		= 000000002;
	private static final int APPEND 	= 000000004;
	private static final int TRUNCATE 	= 000000010;
	
	public static final int EBCDIC			= 000000020; //File is in ebcdic
	public static final int ASCII			= 000000040; //File is in ascii (default)
	public static final int BINARY			= 000000100; //Binary file.
	public static final int VARIABLE_LENGTH = 000000200; //reclen is first 4 bytes of record
	public static final int FIXED_LENGTH	= 000000400; //File has fixed length records
	public static final int RECORD_SEPARATOR= 000001000; //File is variable len with record separator
	public static final int SIZEINVISIBLE	= 000002000; //For varlen records - hide size bytes
	public static final int SIZEVISIBLE		= 000004000; //For varlen records - show size bytes
	public static final int NO_TRANSLATE	= 000010000; //EBCDIC file is not translated
	public static final int TRANSLATE		= 000020000; //EBCDIC file is translated
	public static final int RELATIVE		= 000040000; //
	public static final int RANDOM			= 000100000; //
	public static final int SEQUENTIAL		= 000200000; //
	public static final int AUTO_LENGTH	 	= 000400000; //Calculate recordlength from read/write
	public static final int NO_WAIT			= 001000000; //Don't wait for file on open. Just fail.
	public static final int MODE_CONFIG		= 002000000; //Mode was set by config file
	public static final int RAWWRITE		= 004000000; //Don't add recsep, etc. on output
	public static final int SORT_EBCDIC		= 040000000; //Asciii file is sorted in EBCDIC
	private static final int FILE_EQUATED	= 010000000; //The filename was picked from getenv
	private static final int FILE_VARNAME	= 020000000; //will come from fileVar
	private static String ebcdicEncoding	= null;
	private static String asciiEncoding	= null;
	
	private static boolean staticInitialised = false;
	private static String  defaultFilepath = null;
	
	private Var fileStatus;
	public static final byte [] success = "00".getBytes();
	public static final byte [] notFound = "35".getBytes();
	//????public static final byte [] notExist = "05".getBytes();
	public static final byte [] noNext = "10".getBytes();
	public static final byte [] alreadyExists = "22".getBytes();
	public static final byte [] noRecordFound = "23".getBytes();
	public static final byte [] alreadyOpen = "41".getBytes();
	public static final byte [] notOpen = "42".getBytes();
	
	public static final byte REC_DELETED = 0x00;
	public static final byte REC_EXISTS = 0x0a;
	
//	private static FileWriter out;
	
//	static {
//		try {
//			out = new FileWriter("/tmp/MssFile.out");
//		} catch (IOException e) {
//			out = null;
//		}
//	}
	
	{
		asciiEncoding	= Config.getProperty("jclib.encoding");
		if(asciiEncoding == null) asciiEncoding = "iso8859_1";
		ebcdicEncoding = Config.getProperty("jclib.ebcdic.encoding");
		if(ebcdicEncoding == null) ebcdicEncoding = "cp1047"; //default latin1
	}
	
	
	private String encoding = asciiEncoding;
	private boolean titleChange;
	
	/**
	 * How is the File Mode of access an how has the file been named at creation/usage time.
	 * Flags are set according to the mode of creation. 
	 * A file that is READ will not be TRUNCATE.
	 * Caller needs to discern the values via bitwise interrogation of the returned value
	 * Please see the following for more detail on modes {@link #setMode(int)}
	 * @return an integer value representing the set file mode flags
	 */
	public int getMode() {
		return attr & ~(READ|WRITE|APPEND|TRUNCATE|FILE_EQUATED|FILE_VARNAME);
	}
	
	/**
	 * Verify a specific file mode is set.
	 * Please see the following for more detail on modes {@link #setMode(int)}
	 * @param m the mode
	 * @return true if set, false otherwise
	 */
	public boolean isModeSet(int m) {
		return (attr & m) != 0;
	}
	
	private RandomAccessFile ioStream;
	private int attr;
	private File fd;
	private String fileName;
	private String titleIdentifier;
	private byte [] recordSeparator;
	private Var fileVar;
	boolean firstWrite = true;
	int recordLength;
	private int statusLen; //For fixed length RELATIVE records, an extra status byte is added.

	boolean fileIsOpen;
	byte [] byteBuffer;
	byte [] reclenBuffer;
	private boolean quiet;
	private String note = "";
	
	private long lastModified;
	
	private boolean thisIsReWrite; //We are doing a reWrite()
	private boolean fileChange;
	private int lastLength;   //The length of the record at currentRecordNumber

	//Keep track of record number and position in file
	private int rnAccessLast; //The record number most recently accessed.
	private int rnAccessNext; //record number which would be accessed if we read/wrote now.
	private int rnRequested; //Record number requested by seek, etc.
	private int recordCount; //Count of number of records - -1 if unknown.
	private long offsetAccessLast; //File pointer at last read/write.
	private long offsetAddress;
	
	
	private Var indexVariable;
	private MssRelativeIndex relIndex = null;
	private boolean indexActive = false;
	private int iosPosition = 0;
	private final int iosSize = 8192;
	private byte [] iosCache = new byte[iosSize];
	private boolean noCache = true;
	
	private static void initialiseStatics() {
		if(staticInitialised) return;
		defaultFilepath = Config.getProperty("jclib.default.filepath");
		if(defaultFilepath != null) { 
			if(defaultFilepath.charAt(defaultFilepath.length() - 1) != '/')
				defaultFilepath += "/";
		}

		if(log.isDebugEnabled())
			log.debug("defaultFilepath = " + defaultFilepath);
		staticInitialised = true;
	}
	
	/**
	 * Set the default file path for all instances of MssFile in the jvm.
	 * @param path the universal defualt path.
	 */
	public static void setDefaultFilepath(String path) {
		defaultFilepath = path;
		if(defaultFilepath != null) { 
			if(defaultFilepath.charAt(defaultFilepath.length() - 1) != '/')
				defaultFilepath += "/";
		}
	}
	
	/**
	 * Please see {@link File#lastModified()}
	 * If file does not exist 0 is returned
	 * @return Please see {@link File#lastModified()} 
	 */
	public long lastModified() {
		if(fd == null) return 0;
		return fd.lastModified();
	}
	
	/**
	 * Get the flag representation of all attributes that have been set on the instance of MssFile. 
	 * @return the flags as an int
	 */
	public int getAttributes() { return attr; }
	
	/**
	 * Substitute the current attributes with the provided ines 
	 * @param attributes
	 */
	public void replaceAttributes(int attributes) { attr = attributes; }
	
	protected File getFileInstance() { return fd; }
	
	/**
	 * Get the default file path {@link MssFile#setDefaultFilepath(String)}
	 * @return the default file path for all MssFile instances running in the jvm.
	 */
	public static String defaultFilepath() {
		return defaultFilepath;
	}
	
	/**
	 * Check to see if an MssFile setting/attribute is in effect. 
	 * @param val an int representing the flag value in question 
	 * @return true if set, flase otherwise.
	 */
	public boolean attr(int val) {
		if((attr & val) == 0) return false;
		return true;
	}
	
	/**
	 * Set the variable to use as the indexer into the file.
	 * @param v the indexer 
	 */
	public void setIndexVariable(Var v) { 
		indexVariable = v;
		if(indexActive) relIndex.setIndexVar(v);
	}
	
	/**
	 * Get the currently set index Var
	 * @return the Var that is the index variable.
	 */
	public Var getIndexVariable() { return indexVariable; }
	
	/**
	 * Set the variable to use as the fileStats.
	 * @param v the file status var 
	 */
	public void setFileStatusVar(Var v) { 
		fileStatus = v;
	}
	
	/**
	 * Get the currently set filestatus Var
	 * @return the Var that is the filestatus variable.
	 */
	public Var getfileStatsVar() { return fileStatus; }
	/**
	 * Attempt to set the file via an environment setting 
	 * @param fname the environment variable value
	 * @return null if fname empty or null, 
	 * fname if no environment variable value found,
	 * FILE_EQUATED if found.
	 */
	private String fileEquate(String fname) {
		if(fname == null || fname.length() == 0) return null;
		String s = System.getenv(fname);
		if(s == null) {
			attr &= ~(FILE_EQUATED);
			return fname;
		}
		attr |= FILE_EQUATED;
		return s;
	}
	
	private String fullPath(String name) {
		if(defaultFilepath == null) return name;
		if(name == null) return defaultFilepath;
		if(name.charAt(0) == '/') return name;
		if(name.indexOf(':') != -1) return name;
		return defaultFilepath + name;
	}
	
	/**
	 * Reads the properties file to see if this file name matches any of the attribute
	 * settings in the properties.
	 * @throws IOException 
	 */
	private void setModes() throws IOException {
		if(fileName == null || fileName.length() == 0) return;
		if(Config.getProperties() == null) return;
		Object [] list = Config.getPropertyKeys("file.attribute.");
		if(list == null) return;
		//System.out.println("Got "+list.length+" keys");
		String mode = null;
		for(int i=0; i<list.length;i++) {
			String key = list[i].toString();
			String prop = Config.getProperty(key);
			if(prop == null) continue;
			String [] props = prop.split(",");
			if(props.length != 2) continue;
			//System.out.println("key="+key+" = "+prop);
			Pattern pattern = Pattern.compile(props[0], Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(fileName);
			if(matcher.find()) {
				mode = props[1];
				if(log.isDebugEnabled())
					log.debug("Matched file="+fileName+" with "+props[0]+" - mode="+mode);
				setMode(mode + "|mode_config");;
				break;
			}
		}
	}

	/**
	 * Set the mode of operation for this file
	 * @param mode The following modes are recognized:
	 * 	EBCDIC - the file is in ebcdic. Data will be converted on read and write.
	 *  VARIABLE_LENGTH - Each record length is stored as the first 4 characters of record.
	 *  FIXED_LENGTH - each record is the same length
	 * On creating a file type, a default mode is set. This default may be changed 
	 * by setting the property file.attribute.default in the properties file to another
	 * set of modes.
	 * 
	 * Calling setMode() (along with setRecordLength() and setRecordSeparator() allows 
	 * you to alter the attributes for a particular file instance. This must be done before
	 * a file is actually opened. The call will be ignored once a file is open.
	 * 
	 * It is also possible to set defaults for specific files, based on wildcards or
	 * regular expressions, by adding entries prefixed with file.attribute. to the
	 * properties file. Whenever a file is named (either during the create or when 
	 * setTitle() is called), these entries are searched. If a match is found then 
	 * the associated modes are set.
	 * 
	 * Property file entries are of the form:
	 * 		file.attribute.id = regex, attributes
	 * The "id" portion can be anything but must be unique. The id is required for 
	 * uniqueness and to determine the sequence (using an alphabetic sort) in which the 
	 * entries are evaluated.
	 * 
	 * 'regex' is a standard Java regular expression, which must match all or part of a 
	 * file. The first entry (determined in order of the id) matched by the regex is used.
	 * Further regex expressions which also match the file will be ignored.
	 * 
	 * The attributes can be any of the following items (not case sensitive) separated by 
	 * a single '|' symbol.
	 * 
	 * Attributes
	 * EBCDIC				- file is in EBCDIC
	 * ASCII				- file is in ASCII (this is the default)
	 * BINARY				- binary file - no translations.
	 * SIZEINVISIBLE		- For a VARIABLE_LENGTH file, the 4 digits indicating the
	 * 						- record length are not visible to the programmer. MssFile
	 * 						- takes responsibility for maintaining this field.
	 * SIZEVISIBLE			- The 4 size bytes are visible to the programmer, and it is
	 * 						- the programmers responsibility to maintain these.
	 * FIXED_LENGTH			- The file consists of fixed length records. If no size is 
	 * 						- given then the length of the first record is taken to be
	 * 						- the record length. Once a record length is established,
	 * 						- new records will be truncated or padded with spaces.
	 * 						- The length may be provided with the mode (fixed_length=80) or
	 * 						- else it may be provided by the setRecordLength(int) method.
	 * RECORD_SEPARATOR		- Records are separated by a record separator, which may be a
	 * 						- single character or a sequence of characters. 
	 * 						- The separator is specified as a hex string. By default,
	 * 						- the record separator is the operating system default (\n on
	 * 						- Unix, \r on Mac and \r\n on Windows). The separator string
	 * 						- may be provided with the mode (as in record_separator=0a0d)
	 * 						- or by calling setRecordSeparator(String sep)
	 * VARIABLE_LENGTH		- Records are stored with a size indicator, specified as 4
	 * 						- ascii or ebcdic digits at the start of the record.
	 * RELATIVE				- File is assigned an index, or requires one from the programmer.
	 * 						  If the file is FIXED_LENGTH then the a separate index file is not
	 * 						  required because MssFile can calculate relative positions based on
	 * 						  record-length. For VARIABLE_LENGTH files, an additional index file
	 * 						  is created, named .{filename}.idx which contains a list of offsets
	 *                        to the start of records. Indexes are 1 relative.
	 * RANDOM				- The indexed file can be randomly accessed. Records can be read or
	 * 						  written by index number. If a record already exists throw an 
	 * 						  exception for write(). If a write() is more than 1 greater than 
	 * 						  the number of records, throw an exception. In other words, write()
	 * 						  may only be used to append records. Use reWrite() to overwrite 
	 * 						  existing records.
	 * SEQUENTIAL			- Records are written sequentially and an index created if they are
	 * 						  VARIABLE_LENGTH
	 * ENCODING				- Set the ascii encoding to be used when reading/writing this file.
	 *                        e.g.: encoding=cp1250
	 * 
	 * Example properties file entry:
	 * file.attribute.set1 = /AK/, asxii|fixed_length=100
	 * file.attribute.set2 = .ebc, ebcdic|variable_length|size_invisible
	 * 
	 * Set the default for all files:
	 * file.attribute.default = Ebcdic | variable_length
	 * @throws IOException 
	 * @throws IOException 
	 */
	public void setMode(int mode) throws IOException {
		setModeInternal(mode);
		if(!quiet && log.isDebugEnabled()) log.debug(fileName+": setMode() = "+printAttributes());
	}
	
	private void setModeInternal(int mode) throws IOException {
		//if(isOpen()) TODO: This is silly - better to check that nothing has been read. 
		//	throw new IOException("Cannot set attributes on open file.");
		attr |= mode;
		//Ensure mutual exclusion of attributes.
		if((mode & SIZEINVISIBLE) != 0) attr &= ~(SIZEVISIBLE);
		else if((mode & SIZEVISIBLE) != 0) attr &= ~(SIZEINVISIBLE);

		if((mode & VARIABLE_LENGTH) != 0) attr &= ~(FIXED_LENGTH|RECORD_SEPARATOR);
		else if((mode & FIXED_LENGTH) != 0) attr &= ~(VARIABLE_LENGTH|RECORD_SEPARATOR);
		else if((mode & RECORD_SEPARATOR) != 0) attr &= ~(VARIABLE_LENGTH|FIXED_LENGTH);
		
		if((mode & RANDOM) != 0) attr &= ~(SEQUENTIAL);
		else if((mode & SEQUENTIAL) != 0) attr &= ~(RANDOM);
		if((mode & (RANDOM|SEQUENTIAL)) != 0) attr |= RELATIVE;
		
		//Default for RELATIVE is SEQUENTIAL
		if((mode & RELATIVE) != 0 && (attr & (SEQUENTIAL|RANDOM)) == 0) attr |= SEQUENTIAL;
		
		//if((mode & TRANSLATE)  != 0) attr &= ~(NO_TRANSLATE);
		//else if((mode & NO_TRANSLATE) != 0) attr &= ~(TRANSLATE);
		
		if((mode & ASCII) != 0) {
			attr &= ~(EBCDIC|BINARY);
			encoding = asciiEncoding;
		} else if((mode & EBCDIC) != 0) {
			attr &= ~(ASCII|BINARY);
			//if(!attr(TRANSLATE|NO_TRANSLATE)) attr |= NO_TRANSLATE;
			encoding = ebcdicEncoding;
		} else if((mode & BINARY) != 0) {
			attr &= ~(ASCII|EBCDIC);
		}
		
		if(attr(BINARY) && attr(RECORD_SEPARATOR|VARIABLE_LENGTH))
			throw new IOException("Cannot have a BINARY file with variable-length records.");
	}
	
	/**
	 * Given a string literal attribute/setting set the mode accordingly.
	 * Please see {@link #setMode(int)} for full description of options.
	 * Several options provide alternate names i.e.
	 * <br>
	 * Variable Length mode synonyms: 
	 * VARIABLE_LENGTH, VARIABLE LENGTH, VARIABLELENGTH, VARIABLELEN
	 * <br>
	 * Size Invisible mode synonyms: 
	 * SIZEINVISIBLE, SIZE_INVISIBLE
	 * <br>
	 * Size Visible mode synonyms: 
	 * SIZEVISIBLE, SIZE_VISIBLE
	 * <br>
	 * Fixed Length mode synonyms:
	 * FIXED LENGTH, FIXEDLENGTH
	 * <br>
	 * Record Separator mode synonyms: 
	 * RECORD_SEPARATOR, RECORDSEPARATOR, RECORD SEPARATOR
	 * <br>
	 * Raw Write mode synonyms:
	 * RAW_WRITE, RAWWRITE, RAW
	 * 
	 * @param mode
	 * @throws IOException
	 */
	public void setMode(String mode) throws IOException {
		if(mode == null) return;
		String charset = null;
		String [] modes = mode.split("\\|");
		for(int i=0; i<modes.length;i++) {
			String m = modes[i].trim();

			String [] md = m.split("=");
			String s = md[0].trim().toUpperCase();
			if(s.compareTo("EBCDIC") == 0) setModeInternal(EBCDIC);
			else if(s.compareTo("ASCII") == 0) setModeInternal(ASCII);
			else if(s.compareTo("BINARY") == 0) setModeInternal(BINARY);
			else if(s.compareTo("MODE_CONFIG") == 0) setModeInternal(MODE_CONFIG);
			else if(s.compareTo("NO_TRANSLATE") == 0) setModeInternal(NO_TRANSLATE);
			else if(s.compareTo("TRANSLATE") == 0) setModeInternal(TRANSLATE);
			else if(s.compareTo("VARIABLE_LENGTH") == 0 ||
					s.compareTo("VARIABLE LENGTH") == 0 ||
					s.compareTo("VARIABLELENGTH") == 0 ||
					s.compareTo("VARIABLELEN") == 0) setModeInternal(VARIABLE_LENGTH);
			else if(s.compareTo("SIZEINVISIBLE") == 0 ||
					s.compareTo("SIZE_INVISIBLE") == 0 ||
					s.compareTo("SIZE INVISIBLE") == 0)setModeInternal(SIZEINVISIBLE);
			else if(s.compareTo("SIZEVISIBLE") == 0 ||
					s.compareTo("SIZE_VISIBLE") == 0 ||
					s.compareTo("SIZE VISIBLE") == 0) setModeInternal(SIZEVISIBLE);
			else if(s.compareTo("FIXED_LENGTH") == 0 ||
					s.compareTo("FIXED LENGTH") == 0 || 
					s.compareTo("FIXEDLENGTH") == 0) {
					setModeInternal(FIXED_LENGTH);
					if(md.length > 1) 
						setRecordLength(Util.parseInt(md[1]));
			}
			else if(s.compareTo("ENCODING") == 0 ||
					s.compareTo("CHARSET") == 0) {
				if(md.length > 1) charset = md[1];
			}
			else if(s.compareTo("RANDOM") == 0) setModeInternal(RANDOM);
			else if(s.compareTo("RELATIVE") == 0) setModeInternal(RELATIVE);
			else if(s.compareTo("SEQUENTIAL") == 0) setModeInternal(SEQUENTIAL);
			else if(s.compareTo("RECORD_SEPARATOR") == 0 ||
					s.compareTo("RECORDSEPARATOR") == 0 ||
					s.compareTo("RECORD SEPARATOR") == 0) {
					setModeInternal(RECORD_SEPARATOR);
					if(md.length > 1) 
						setRecordSeparator(md[1]);
			} else if((s.compareTo("RAW_WRITE") == 0) ||
					(s.compareTo("RAWWRITE") == 0) ||
					(s.compareTo("RAW") == 0)) {
				setModeInternal(RAWWRITE);
			} else if(s.compareTo("SORT") == 0) {
				if(md.length > 1) {
					String x = md[1].trim().toUpperCase();
					if(x.compareTo("EBCDIC") == 0) {
						setModeInternal(SORT_EBCDIC);
					}
				}
			}
		}
		if(charset != null) setEncoding(charset);
		if(!quiet && log.isDebugEnabled()) log.debug(fileName+": setMode() = "+printAttributes());
	}
	
	/**
	 * Set the encoding for reading/writing this file. All standard Java code pages are supported.
	 * Note that if you are expecting to use fixed length or variable length records, only single
	 * byte code pages should be selected.
	 * @param charset The code page to use for file encoding.
	 */
	public void setEncoding(String charset) {
		encoding = charset;
	}

	/**
	 * Get a string representation of the file structure and access modes.
	 * @return a | delimited string of file structure and access modes.
	 * If a record separator is in play its value will be dumped if null the 0x0a is the separator.
	 */
	public String printAttributes() {
		StringBuilder sb = new StringBuilder();
		//if(attr(EBCDIC)) {
		//	sb.append("EBCDIC");
		//	if(attr(NO_TRANSLATE)) sb.append("|NO_TRANSLATE");
		//	else if(attr(TRANSLATE)) sb.append("|TRANSLATE");
		//} else sb.append("ASCII");
		sb.append("("+encoding+")");
		if(attr(VARIABLE_LENGTH)) {
			sb.append("|VARIABLE_LENGTH");
			if(attr(SIZEINVISIBLE)) sb.append("|SIZE_INVISIBLE");
			else sb.append("|SIZE_VISIBLE");
		} else if(attr(FIXED_LENGTH)) {
			sb.append("|FIXED_LENGTH");
			if(recordLength > 0) sb.append("="+recordLength);
		} else if(attr(RECORD_SEPARATOR)) {
			sb.append("|RECORD_SEPARATOR=");
			if(recordSeparator == null) 
				setSystemSeparator();
			sb.append("0x");
			for(int i=0;i<recordSeparator.length;i++)
				sb.append(String.format("%02x", recordSeparator[i]));
		}
		if(attr(RELATIVE)) sb.append("|RELATIVE");
		if(attr(RANDOM)) sb.append("|RANDOM");
		else if(attr(SEQUENTIAL)) sb.append("|SEQUENTIAL");
		
		return sb.toString();
	}
	
	private byte [] setSystemSeparator() {
		recordSeparator = System.getProperty("line.separator").getBytes();
		return recordSeparator;
	}
	

	private void setDefaultModes() throws IOException {
		String def = Config.getProperty("file.attribute.default");
		if(def == null) {
			setModeInternal(ASCII|RECORD_SEPARATOR);
			recordLength = 0;
			setSystemSeparator();
		} else setMode(def);
	}
	
	/**
	 * Set record length. All writes will be to this length. Short strings will be 
	 * padded and long strings will be truncated on write. While reading, records 
	 * will be fixed at this length.
	 * @param len The length of the records.
	 */
	public void setRecordLength(int len) {
		if(len <= 0 || len > 32565) 
			throw new IllegalArgumentException("length must be between 1 and 32565");
		recordLength = len;
		attr &= ~(VARIABLE_LENGTH|RECORD_SEPARATOR);
		attr |= FIXED_LENGTH;
		if(attr(RELATIVE)) {
			statusLen = 1;
		}
	}
	
	/**
	 * Only valid on on fixed length file structure.
	 * @return between 1 & 32565, 0 if not FIXED_LENGTH mode.
	 */
	public int getRecordLength() {
		return recordLength;
	}
	
	/**
	 * The default record separator is "\n" - set to whatever else you need for reading
	 * or writing.
	 * @param sep The string to use as a record separator. This must be specified as a 
	 * HEX string. For example, to specify \r\n, the string should be "0D0A"
	 * @throws IOException 
	 */
	public void setRecordSeparator(String sep) throws IOException {
		if(sep == null) {
			recordSeparator = null;
			return;
		}
		if(sep.length() == 0) {
			recordSeparator = null;
			attr &= ~(VARIABLE_LENGTH|FIXED_LENGTH);
			attr |= RECORD_SEPARATOR;
			return;
		}
		if((sep.length() % 2) != 0) 
			throw new IOException("Separator string '"+sep+"' not an even number of characters.");
		
		recordSeparator = new byte[sep.length()/2];
		for(int i=0; i<sep.length(); i += 2) {
			try {
				recordSeparator[i/2] = (byte)Integer.parseInt(sep.substring(i, i+2), 16);
			} catch (NumberFormatException e) {
				throw new IOException("Separator string contains non-hexadecimal characters ("+
						sep.substring(i, i+2) + ")");
			}
		}
		attr &= ~(VARIABLE_LENGTH|FIXED_LENGTH);
		attr |= RECORD_SEPARATOR;
	}
	
	/**
	 * Return record Separator
	 * @return byte array representing record separator
	 */
	public byte [] getRecordSeparator() {
		if(recordSeparator == null)
			setSystemSeparator();
		return recordSeparator;
	}
	
	/**
	 * Is the MssFile open for access?
	 * @return true if open, false otherwise.
	 */
	public boolean isOpen() { return fileIsOpen; }
	
	/**
	 * Is the MssFile open for access? Variant on {@link #isOpen()}
	 * @return 0 if open, -1 if not.
	 */
	public int fileOpen() { 
		if(fileIsOpen)
			return 0;
		return -1;
	}

	/**
	 * Create a new MssFile class and open the file according to the mode.
	 * @param fileName The name of the file to open
	 * @param mode Mode of opening:
	 * 		r  - open in readonly mode
	 *      r+ - Open file in read/write, no truncation.
	 *      w  - truncate file if exists, and open for writing
	 *      w+ - Open file in read/write mode. Tuncate if exists.
	 *      a  - Open in write mode for appending.
	 *      a+ - Open for appending in read/write mode.
	 */
	public MssFile(String fileName, String mode) {
		this();
		try {
			setDefaultModes();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		titleIdentifier = fileName;
		setSystemSeparator();
		if((fileName == null || fileName.length() == 0) &&
				(mode == null || mode.length() != 2 || mode.charAt(1) != 't')) return;
		fileName = fileEquate(fileName);
	    if(mode == null || mode.length() == 0) mode = "r";
	    if(mode.length() > 1 && mode.charAt(1) == 't') { //Temp file
			String pathName = Util.dirName(fileName);
			File path = new File(pathName);
			try {
				fd = File.createTempFile("merge.", ".tmp", path);
				fd.deleteOnExit();
			} catch(IOException e) {
				Util.abort("Unable to create temp file in '"+pathName+"'.");
			}
		} else {
		    this.fileName = fileName;
		    fd = new File(fullPath(fileName));
		}
	    try {
			setModes();
			reOpen(mode);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return;
	}

	/**
	 * Increases the record length of a FIXED-LEN file to this new length. All records
	 * already written are re-written to the new length.
	 * @param length The new record length.
	 */
	public void increaseRecordLength(int length, String recordSepString) {
		byte [] rs = recordSepString.getBytes();
		if(!attr(FIXED_LENGTH)) return;
		if(recordLength >= length) return;
		if(recordLength < 0) return;
		if(!isOpen()) return;
//		MssFile tmpfile = new MssFile();
		try {

			String fname = fullPath(fileName);
			String tmpname = fname + ".tmp";
			File tfd = fd;
			ioStream.close();
			tfd.renameTo(new File(tmpname));
			tfd = new File(tmpname);
			tfd.deleteOnExit();
			RandomAccessFile tstream = new RandomAccessFile(tfd, "r");
			
			fd = new File(fname);
			ioStream = new RandomAccessFile(fd, "rw");
			byte [] rbuf = new byte[recordLength + statusLen];
			byte [] wbuf = new byte[length + statusLen];
			for(int i=0;i<wbuf.length;i++) wbuf[i] = ' ';
			offsetAddress = 0;
			if(rs.length > 0) {
				System.arraycopy(rs, 0, wbuf, length - rs.length, rs.length);
			}
			while(tstream.read(rbuf) > 0) {
				System.arraycopy(rbuf, 0, wbuf, 0, rbuf.length - rs.length);
				ioStream.write(wbuf);
				offsetAddress += length;
			}
			tstream.close();
			tfd.delete();
		} catch (IOException e) {
			e.printStackTrace();
		}
		recordLength = length;
	}

	/**
	 * Please see {@link File#deleteOnExit()}
	 */
	public void deleteOnExit() {
		if(fd != null) fd.deleteOnExit();
	}
	
	/**
	 * Create a new MssFile object but don't open the file yet.
	 * @param fileName The default name of the file to open - Look for an
	 * environment variable of the same name - if it exists, then use the 
	 * env name for the actual name of the file.
	 */
	public MssFile(String fileName) {
		this();
		quiet = true;
		titleChange = true;
		titleIdentifier = fileName;
		try {
			setDefaultModes();
			this.fileName = fileEquate(fileName);
			setModes();
		} catch (IOException e) {
			e.printStackTrace();
		}
		quiet = false;
	}
	
	/**
	 * Create a new MssFile object but don't open the file.
	 * @param fileVar A Var or Group object which will contain the file name.
	 * When the file is opened, use this name.
	 */
	public MssFile(Var fileVar) {
		this();
		quiet = true;
		titleChange = true;
		titleIdentifier = fileVar.toString();
		this.fileVar = fileVar;
		if(this.fileVar == null) throw new IllegalArgumentException("fileVar is null");
		attr &= ~(FILE_EQUATED);
		try {
			setDefaultModes();
			setModes();
		} catch (IOException e) {
			e.printStackTrace();
		}
		quiet = false;
		attr |= FILE_VARNAME;
	}
	
	private MssFile() {
		initialiseStatics();
		this.attr = 0;
		this.byteBuffer = null;
		this.fd = null;
		this.fileChange = true;
		this.fileIsOpen = false;
		this.fileName = null;
		this.fileVar = null;
		this.firstWrite = true;
		this.indexActive = false;
		this.indexVariable = null;
		this.ioStream = null;
		this.lastLength = 0;
		this.lastModified = 0;
		this.offsetAddress = 0;
		this.offsetAccessLast = 0;
		this.quiet = true;
		this.readBuffer = null;
		this.recordCount = -1;
		this.recordLength = 0;
		this.statusLen = 0;
		this.recordSeparator = null;
		this.relIndex = null;
		this.rnAccessLast = 0;
		this.rnAccessNext = 0;
		this.rnRequested = 0;
		this.statusSeeking = false;
	}
	
	/**
	 * Clone the MssFile object - everything is copied but the file itself is closed.
	 */
	public MssFile clone() {
		MssFile f = new MssFile();
		f.attr = this.attr & ~(READ|WRITE|APPEND|TRUNCATE);
		f.byteBuffer = null;
		f.fd = this.fd;
		f.fileChange = this.fileChange;
		f.fileIsOpen = false;
		f.fileName = this.fileName;
		f.fileVar = this.fileVar;
		f.firstWrite = true;
		f.indexActive = this.indexActive;
		f.indexVariable = this.indexVariable;
		f.ioStream = null;
		f.lastLength = 0;
		f.recordCount = this.recordCount;
		f.recordLength = this.recordLength;
		f.statusLen = this.statusLen;
		f.recordSeparator = this.recordSeparator;
		//f.relIndex = this.relIndex.clone();
		return this;
	}
	
	
	/**
	 * Open the MssFile with the particular mode settings
	 * Generally used in conjunction with the following 2 constructors.
	 * {@link #MssFile(String)} & {@link #MssFile(Var)} 
	 * @param mode
	 * @para fileStatus file status var
	 * @return true if success, false if failure to open.
	 * @throws IOException
	 */
	public boolean open(String mode) throws IOException {
		if(attr(FILE_VARNAME)) {
			String fn = fileVar.getTrim();
			if(fileName == null || fn.compareTo(fileName) != 0) {
				fileName = fn;
				setModes();
			}
		}
		if(fileName.equals("") && fileStatus != null) {
			fileStatus.set(notFound);
			return false;
		}
//		out.write("File Open: filename="+fileName+"\n");
		//System.out.println("open-" + fileName + "-" + fullPath(fileName));
		fd = new File(fullPath(fileName));
		while(!fd.exists() &&  mode != null && mode.equals("r")) {
			//if(attr(NO_WAIT)) {
				if(log.isDebugEnabled())
					log.debug("File "+fileName+" not found. Ignoring.");
				//System.out.println("File "+fileName+" not found.");
				if(fileStatus != null) fileStatus.set(notFound);
				return false;
			//}
			//Good for kir not for others
			//need to set an attribute String alternate = Util.waitForFile(fullPath(fileName));
			//if(alternate != null) {
				//fileName = alternate;
				//fd = new File(fileName);
			//}
		}
		if(fileStatus != null) fileStatus.set(success);
//		if(indexVariable != null) indexVariable.set(1);
		
		if(titleChange) {
			titleChange = false;
			lastModified = 0;
			if(indexActive) {
				relIndex.close();
				relIndex = null;
				indexActive = false;
			}
		}
		if(encoding == null) {
			if(attr(EBCDIC)) encoding = ebcdicEncoding;
			else encoding = asciiEncoding;
		}
		reOpen(mode);
		firstWrite = true;
		return fileIsOpen;
	}
	
	/**
	 * Re-open a MssFile in a particular mode.
	 * @param mode
	 * @throws IOException
	 */
	public void reOpen(String mode) throws IOException {
		if(fd == null || titleChange) {
			open(mode);
			return;
		}
		if(isOpen()) {
			lastModified = fd.lastModified();
		}
		if(lastModified != fd.lastModified()) {
			recordCount = -1;
			rnAccessLast = 0;
			rnAccessNext = 0;
			lastLength = -1;
		}
		
		if(mode == null || mode.length() == 0) mode = "r";
		attr &= ~(READ|WRITE|TRUNCATE|APPEND);
		if(mode.charAt(0) == 'r') attr |= READ;
		else if(mode.charAt(0) == 'w') attr |= WRITE|TRUNCATE;
		else if(mode.charAt(0) == 'a') attr |= WRITE|APPEND;
		if(mode.length() > 1 && mode.charAt(1) == '+') attr |= READ|WRITE;
		
		if(attr(READ)) {
			recordCount = 0;
			rnAccessNext = 0;
			rnAccessLast = 0;
			offsetAccessLast = 0;
			offsetAddress = 0;
			bufPtr = 0;
			endPtr = 0;
		}
		if(attr(TRUNCATE)) {
			if(fd != null) fd.delete();
			relIndex = new MssRelativeIndex(this);
			relIndex.close();
			relIndex = null;
			if(indexActive) relIndex = new MssRelativeIndex(this);
			recordCount = 0;
			rnAccessNext = 0;
			rnAccessLast = 0;
			offsetAccessLast = 0;
			offsetAddress = 0;
		}
		String rmode = "r";
		if(attr(WRITE)) {
			rmode += "w";
			//Create dirs...
			String path = fd.getParent();
			if(path != null && path.length() > 0) {
				File dir = new File(path);
				dir.mkdirs();
			}
		}
		if(attr(VARIABLE_LENGTH)) {
			reclenBuffer = new byte[4];
			byteBuffer = new byte[0];
		} else if(attr(FIXED_LENGTH)) {
			byteBuffer = new byte[recordLength + statusLen];
			reclenBuffer = null;
		} else if(attr(RECORD_SEPARATOR)) {
			byteBuffer = new byte[0];
			reclenBuffer = null;
		}
		try {
			if(ioStream != null) {
				flush();
				ioStream.close();
			}
		} catch (IOException e) { /* do nothing */ }
		try {
			ioStream = new RandomAccessFile(fd, rmode);
			if(attr(APPEND)) {
				offsetAddress = ioStream.length();
				ioStream.seek(offsetAddress);
				offsetAccessLast = offsetAddress;
				if(recordCount != -1) rnAccessNext = recordCount;
			} else {
				offsetAddress = 0;
				rnAccessNext = 0;
			}
			fileIsOpen = true;
		} catch(IOException e) {
			fileIsOpen = false;
			if(log.isDebugEnabled()) {
				if(attr(WRITE)) log.error("Cannot create file \""+fileName+"\" with mode "+rmode+".");
				else if(log.isDebugEnabled()) log.debug("Cannot open file \""+fileName+"\" with mode "+rmode+".");
			}
			attr &= ~(WRITE|READ|TRUNCATE);
			fd = null;
			return;
		}
		
		//If a valid index exists, use it.
		if(!indexActive) {
			if(Timer.isActive()) Timer.begin(titleIdentifier + ": CreateIndexFile()");
			relIndex = new MssRelativeIndex(this);
			if(relIndex.isValidIndex()) {
				indexActive = true;
				recordCount = relIndex.recordCount();
			} else {
				relIndex.close();
				relIndex = null;
				indexActive = false;
				if(ioStream.length() == 0) recordCount = 0;
				else if(attr(FIXED_LENGTH)) {
					if(recordLength > 0)
						recordCount = (int)(ioStream.length() / (recordLength + statusLen));
					else recordCount = -1;
				}
			}
			Timer.end();
		}

		rnRequested = -1;
		
		if(indexVariable != null && recordCount == -1 && attr(RELATIVE)) 
			countLines();
		
		if(log.isDebugEnabled())
			log.debug("MssFile.open("+fileName+",\""+mode+"\") file-attributes="+printAttributes()+" count="+recordCount
				+" indexActive="+indexActive);
	}
	
	private int calculateSize(byte [] buf) throws IOException {
		if(buf == null || buf.length < 4) return 0;
		else if(attr(FIXED_LENGTH)) return recordLength;
		else if(attr(VARIABLE_LENGTH) && attr(SIZEVISIBLE)) {
			String s = new String(buf,0,4, encoding);
			return Util.parseInt(s);
		} else return buf.length;
	}

	/**
	 * Write out the String and advance to the next line.
	 * @param buf the String to write
	 * @throws IOException
	 */
	public void writeLine(String buf) throws IOException {
		if(buf == null) buf = "";
		write(buf);
	}

	 /* Overloaded method see {@link #write(String)}
	 * 
	 * @param v
	 * @return
	 * @throws IOException
	 */
	public boolean write(Var v) throws IOException {
		if(v instanceof Group) {
			byte [] b = ((Group)v).getRawBytesArray();
			int pos = ((Group)v).getMyOffset();
			int len = ((Group)v).size();
			write(b, pos, len);
			return true;
		}
		write(v.getString());
		return true;
	}
	
	/**
	 * Overloaded method using Var see {@link #reWrite(String)}
	 * @param v a Var containing the string.
	 * @return
	 * @throws IOException
	 */
	public boolean reWrite(Var v) throws IOException {
		return reWrite(v.getString());
	}

	/**
	 * The String is converted into a byte array for the specified encoding and then
	 * delegated to {@link #reWrite(byte[])} which in turn delegates to {@link #reWrite(byte[], int)}
	 * @param buf
	 * @return
	 * @throws IOException
	 */
	public boolean reWrite(String buf) throws IOException {
		byte [] b;
		b = buf.getBytes(encoding);
		if(attr(EBCDIC)) for(int i=0;i<b.length;i++) if(b[i] == 0x15) b[i] = 0x25;
		return reWrite(b);
	}
	
	/**
	 * Overloaded see {@link #reWrite(byte[], int)}
	 * @param buf
	 * @return
	 * @throws IOException
	 */
	public boolean reWrite(byte[] buf) throws IOException {
		return reWrite(buf, buf.length);
	}
	
	//Adjust the first 4 bytes of buf to the ascii/ebcdic length indicated by len
	private void adjustVisibleLength(byte [] buf, int len) throws UnsupportedEncodingException {
		String s = String.format("%04d", len);
		byte [] b = s.getBytes(encoding);
		/*b = Util.getBytes(s);
		if(attr(EBCDIC) && attr(NO_TRANSLATE))
			Util.ascii2ebcdic(b, b.length);*/
		System.arraycopy(b, 0, buf, 0, 4);
	}

	/**
	 * Overwrites the previous write. If the new data > len of previous write it is 
	 * truncated to the previous writes data size.
	 *   
	 * @param buf array to write
	 * @param len length of array to write out.
	 * @return
	 * @throws IOException
	 */
	public boolean reWrite(byte [] buf, int len) throws IOException {//todo filestatus
		int oldlen; //length of old record
		thisIsReWrite = true;

		if(Timer.isActive())
			Timer.begin(titleIdentifier + ": reWrite()");

		//Position file beginning of previous read or write.
		if(attr(RELATIVE) && indexVariable != null) {
			seekRecord(indexVariable.getInt() - 1);
		} else if(rnRequested == -1) seekRecord(rnAccessLast);
		
		//Ensure we know the size of the record we're overwriting.
		if(attr(FIXED_LENGTH)) {
			oldlen = recordLength;
		} else if(attr(VARIABLE_LENGTH) && attr(SIZEVISIBLE)) {
			//DEBUG START prc
			/*byte [] b4 = new byte[4];
			System.arraycopy(buf, 0, b4, 0, 4);
			StringBuilder b4raw = new StringBuilder();
			for(int i=0;i<4;i++) b4raw.append(String.format("%02x", b4[i]));
			if(attr(EBCDIC) && attr(NO_TRANSLATE))
				Util.ebcdic2ascii(b4, 4);
			StringBuilder b4a = new StringBuilder();
			for(int i=0;i<4;i++) b4a.append(String.format("%02x", b4[i]));
			log.debug(fileName+": rewrite: buf.length="+buf.length+" raw=0x"+
					b4raw.toString()+" ascii="+b4a.toString());
			*/
			//DEBUG END prc
			
			oldlen = lastLength;
			len = calculateSize(buf);
			if(buf.length < len) 
				throw new IOException(fileName+": Length bytes indicate record of "+len+" bytes but buffer array is only "+buf.length+" bytes.");
			if(oldlen > 0 && oldlen != len) {
				adjustVisibleLength(buf, oldlen);
				
			}
		} else if(attr(VARIABLE_LENGTH) && attr(SIZEINVISIBLE)) {
			oldlen = lastLength - 4;
		} else if(attr(RECORD_SEPARATOR)) {
			oldlen = lastLength;
		} else {
			oldlen = lastLength;
		}
		if(oldlen < 0) {
			//We don't know the last length - better find out!
			if(attr(VARIABLE_LENGTH)) {
				byte [] b = new byte[4];
				ioStream.read(b);
				lastLength = Util.parseInt(new String(b, encoding));
				if(attr(SIZEINVISIBLE)) oldlen = lastLength - 4;
				else {
					oldlen = lastLength;
					if(oldlen != len)
						adjustVisibleLength(buf, oldlen);
				}
			} else if(attr(RECORD_SEPARATOR)) {
				long save = offsetAddress;
				byte [] b = readUntilSeparator();
				lastLength = b.length;
				oldlen = lastLength;
				offsetAddress = save;
			}
			ioStream.seek(offsetAddress);
		}
		
		
		if(oldlen != len) {
			log.warn(fileName+": reWrite called with different record size. "+
					"Original size="+oldlen+" new size="+ len);
			if(len > oldlen) len = oldlen;
			else {
				byte [] b = new byte[oldlen];
				System.arraycopy(buf, 0, b, 0, len);
				for(int i=len;i<oldlen;i++) b[i] = ' ';
				buf = b;
				len = oldlen;
			}
		}
		//if(lineCount != -1) lineCount--;
		thisIsReWrite = true;
		if(rnRequested != -1 && !(attr(RANDOM)))
			rnRequested = rnAccessLast;
		
		//if(currentRecordNumber != -1 && !attr(RANDOM)) {
		//	nextRecordNumber = currentRecordNumber;
		//}
		boolean rslt = write(buf, 0, len);
		Timer.end();
		return rslt;
	}
	
	public void deleteRecord() throws IOException, Exception {
		if(attr(RELATIVE)) {
			int recnum; //0 relative
			if(attr(RANDOM)) {
				recnum = indexVariable.getInt();
				if(recnum > countLines()) {
					throw new IOException("Request to delete record "+recnum+" greater than records in file");
				}
				recnum--;
			} else {
				recnum = rnAccessLast;
			}
			int offset = recnum * (recordLength + statusLen) + recordLength;
			byte [] b = new byte[1];
			b[0] = 0;
			ioStream.seek(offset);
			ioStream.write(b);
			return;
		}
		
		if(ioStream == null) 
			throw new IOException(fileName+": Cannot delete on unopened file.");
		throw new Exception("delete not yet implemented");
	}

	
	/**
	 * Seek to a particular record (0 relative). The next read or write will operate from
	 * this location.
	 * @param record The zero relative record number.
	 * @throws IOException
	 */
	public boolean seekRecord(int record) throws IOException {
		if(ioStream == null) 
			throw new IOException(fileName+": Cannot seek on unopened file.");
		
		if(record < 0) 
			if(fileStatus != null)
				return false;
			else			
				throw new IOException(fileName+": Requested record cannot be negative: "+record);
//		System.out.println("readBytes: seekRecord="+record);

		rnRequested = record;
		
		//If the requested record is the same as the one about to be read or written,
		//do nothing.
//		if(rnRequested == rnAccessNext) {
//			if(rnAccessLast != rnAccessNext) lastLength = -1;
//			return true;
//		}
		long offset;
		if(attr(FIXED_LENGTH)) {
			if(recordLength < 1) 
				throw new IOException(fileName+":  Record length unknown.");
			int numLines = countLines();
			if(record > numLines) 
				//throw new IOException(fileName+": Requested record out of range: "+record);
				return false;
			rnRequested = record;
			offset = rnRequested * (recordLength + statusLen);
			rnAccessNext = rnRequested;
			ioStream.seek(offset);
			offsetAddress = offset;
		} else {
			if(indexActive) {
				if(record > countLines()) 
					throw new IOException(fileName+": Requested record out of range: "+record);
				offset = relIndex.getOffset(rnRequested);
				lastLength = relIndex.getLength(rnRequested);
				rnAccessNext = rnRequested;
			} else {
				if(rnRequested == 0) {
					rnAccessNext = 0;
					//if(rnAccessLast != 0) lastLength = -1;
					offset = 0;
				} else if(rnRequested == rnAccessLast) {
					offset = offsetAccessLast;
					rnAccessNext = rnRequested;
				} else if(rnRequested == (rnAccessNext + 1)) {
					int saveAttr = attr;
					attr &= ~(RELATIVE|RANDOM);
					statusSeeking = true;
					read();
					statusSeeking = false;
					rnRequested = record;
					attr = saveAttr;
					lastLength = -1;
					offset = offsetAddress;
					if(rnRequested != rnAccessNext)
						throw new IOException(fileName+": Could not reposition to record "+rnRequested);
				} else if(recordCount != -1 && rnRequested == recordCount) {
					//No need for index file as we know where we are.
					offset = ioStream.length();
					rnAccessNext = recordCount;
				} else { // now we need to create the indexing facility.
					if(relIndex == null) {
						relIndex = new MssRelativeIndex(this);
						indexActive = true;
					}
					if(record > countLines()) 
						throw new IOException(fileName+": Requested record out of range: "+record);
					lastLength = relIndex.autoSeek(rnRequested);
					offset = relIndex.getOffset(rnRequested);
					rnAccessNext = rnRequested;
				}
			}
		}
		if(offset != offsetAddress) {
			ioStream.seek(offset);
			offsetAddress = offset;
		}
		if(indexVariable != null && !attr(RANDOM)) indexVariable.set(rnAccessNext+1);
		return true;
	}
	
	/**
	 * Sets 1 relative record number for next read.
	 * @param record
	 * @throws IOException
	 */
	public void setRecord(int record) throws IOException {
		seekRecord(record - 1);
	}

	/**
	 * Write a String to the MssFile, will be written as bytes in the specified encoding.
	 * @param buf to write to file
	 * @return true if successful write, false otherwise.
	 * @throws IOException
	 */
	public boolean write(String buf) throws IOException {
		if(buf == null) buf = "";
		byte [] b = buf.getBytes(encoding);
		if(attr(EBCDIC)) for(int i=0;i<b.length;i++) if(b[i] == 0x15) b[i] = 0x25;
		return(write(b, 0, b.length));
		//if(fileStatus != null && fileStatus.ne(0)) return false;
		//return true;
	}

	/**
	 * 
	 * @param buf
	 * @return
	 * @throws IOException
	 */
	public boolean writeNonl(String buf) throws IOException {
		if(!attr(WRITE) || ioStream == null) {
			throw new IOException("Attempt to write to Unopened File.");
		}
		if(buf == null) buf = "";
		
		if(attr(VARIABLE_LENGTH))
			throw new IOException("Function cannot be used in VARIABLE_LENGTH files.");
		if(attr(FIXED_LENGTH))
			throw new IOException("Function cannot be used in FIXED_LENGTH files.");
		ioStreamWrite(buf.getBytes());
//		ioStream.writeBytes(buf);
		offsetAddress += buf.length();
		firstWrite = false;
		return true;
	}
	
    /**
     * Write a byte array to the file.
     * @param buf the byte array.
     * @throws IOException
     */
    public boolean write(byte [] buf) throws IOException {
        return(write(buf, 0, buf.length));
    }
    /**
     * Write a byte array to the LS file.
     * @param buf the byte array.
     * @throws IOException
     */
    public boolean writeLs(Group line) throws IOException {
        return(write(line.getRawBytesArray(), line.getMyOffset(), line.rtrim().length()));
    }
    
	private void ioStreamWrite(byte [] buf) throws IOException {
		ioStreamWrite(buf, 0, buf.length);
	}

	private void ioStreamWrite(byte [] buf, int pos, int len) throws IOException {
		if(noCache || attr(RELATIVE|RANDOM)) {
			ioStream.write(buf, pos, len);
			return;
		}
		while((iosPosition + len) >= iosSize) {
			if(iosPosition < (iosSize - 1)) {
				int l = iosSize - iosPosition;
				System.arraycopy(buf, pos, iosCache, iosPosition, l);
				iosPosition += l;
				pos += l;
				len -= l;
			}
			flush();
		}
		System.arraycopy(buf, pos, iosCache, iosPosition, len);
		iosPosition += len;
	}

	/**
	 * Flushes the iosCache to disk
	 * @throws IOException 
	 */
	public void flush() throws IOException {
		if(attr(RELATIVE|RANDOM)) {
			return;
		}
		if(ioStream == null || iosPosition == 0)
			return;
		ioStream.write(iosCache, 0, iosPosition);
		iosPosition = 0;
	}

	/**
	 * Write a specified length of the array to the file. 
	 * @param buf The buffer to write to disk
	 * @param pos Start position in buffer
	 * @param len Number of bytes
	 * @throws IOException
	 */
	public boolean write(byte [] buf, int len) throws IOException {
		return write(buf, 0, len);
	}
	
	/**
	 * Write a specified length of the array to the file. 
	 * @param buf The buffer to write to disk
	 * @param pos Start position in buffer
	 * @param len Number of bytes
	 * @throws IOException
	 */
	public boolean write(byte [] buf, int pos, int len) throws IOException {
		String operation;
		String wmsg = null;
		boolean debugEnabled = log.isDebugEnabled();
		int buflen = buf.length - pos;
		if(!attr(WRITE) || ioStream == null) {
			if(fileStatus == null)
				throw new IOException("Attempt to write to Unopened File.");
			else
				fileStatus.set(notOpen);
				return false;
		}
		if(fileStatus != null) fileStatus.set(success);
		
		//if(attr(EBCDIC) && attr(TRANSLATE)) {
		//	String x = new String(buf, asciiEncoding); //ascii to UTF-8
		//	buf = x.getBytes(ebcdicEncoding); //UTF-8 to ebcdic
		//}
		
		long offset;
//		if(!thisIsReWrite && Timer.isActive())
//			Timer.begin(titleIdentifier + ": write()");
		
//		FileChannel channel = ioStream.getChannel();
//		FileLock lock = channel.lock();

		if(attr(RELATIVE) && indexVariable != null) {
			if(!thisIsReWrite && attr(RANDOM)) {
				int recNum = indexVariable.getInt() - 1;
				createMissingRecords(recNum);
				seekRecord(recNum);
			} else if(!thisIsReWrite && attr(SEQUENTIAL)) {
				int recNum = countLines();
				seekRecord(recNum);
				if(indexVariable != null) {
					indexVariable.set(recNum + 1);
				}
			}
		} else if(attr(RANDOM) && rnRequested == -1) { //force re-write of the same record.
			rnAccessNext = rnAccessLast;
			offsetAddress = offsetAccessLast;
			flush();
			ioStream.seek(offsetAddress);
			noCache = true;
		}
		if(thisIsReWrite) {
			operation = "rewrite";
			offset = offsetAddress;
			/*if(indexActive) {
				offset = relIndex.getOffset(rnAccessNext);
				ioStream.seek(offset);
			} else if(rnAccesLast == rnAccessNext) {
				ioStream.seek(offsetAccessLast);
				offset = offsetAccessLast;
			} else if(attr(FIXED_LENGTH)) {
				offset = rnAccessNext * recordLength;
				offsetAccessLast = offset;
				ioStream.seek(offset);
			} else
				throw new IOException(fileName+": Cannot re-write record " + rnAccessNext);
				*/
		} else {
			operation = "write";
			if(attr(RANDOM)) {
				if(!attr(RELATIVE) && rnAccessNext != countLines()) {
//					Timer.end();
//					lock.release();
					if(fileStatus != null) {
						fileStatus.set(alreadyExists);
						return false;
					} else
						throw new IOException(fileName +
								": Cannot write() exisiting record for RELATIVE-RANDOM file (record="+
								rnAccessNext+", 0 relative.)");
				}
				if(attr(SEQUENTIAL)) {
					flush();
					offset = ioStream.length();
				} else {
					offset = offsetAddress;
				}
			} else if(attr(APPEND)) {
				flush();
				offset = ioStream.length();
				if(offset != offsetAddress)
					ioStream.seek(offset);
				noCache = true;
				if(recordCount != -1) rnAccessNext = countLines();
			} else {
				offset = offsetAddress;
			}
		}
		rnRequested = -1;
		offsetAccessLast = offset;
		offsetAddress = offset;
		
		if(debugEnabled)
			log.debug(fileName+": "+operation+" "+len+" bytes (requested). file-attributes="+printAttributes());

		if(attr(FIXED_LENGTH)) {
			if(recordLength <= 0) setRecordLength(len);
			else len = recordLength;
		} else if(attr(RAWWRITE)) {
		} else if(attr(VARIABLE_LENGTH)) {
			if(attr(SIZEVISIBLE)) {
				String lstr;
				if(buflen < 4 || len < 4) {
//					lock.release();
//					Timer.end();
					throw new IOException(fileName+": Required length bytes missing for variable length 'size visible' file.");
				}
				lstr = new String(buf,0,4,encoding);
				len = Util.parseInt(lstr);
				if(len < 4) {
//					lock.release();
//					Timer.end();
					throw new IOException(fileName + ": Variable length 'size visible' file expects 4 character length indicator. Got '"+lstr+"'");
				}
			}
		}
		
		//Ensure buf is at least len bytes. Pad with spaces.
		if(buflen < (len + statusLen)) {
			byte [] tb = new byte[len + statusLen];
			byte space = " ".getBytes(encoding)[0];
			for(int i=buf.length;i<len;i++) tb[i] = space;
			System.arraycopy(buf, pos, tb, 0, buflen);
			pos = 0;
			buflen = tb.length;
			buf = tb;
		}
		
		if(attr(RELATIVE) && statusLen > 0) {
			buf[pos + len] = REC_EXISTS;
		}
		
//		long actualOffset = ioStream.getFilePointer();
		firstWrite = false;
		//Write the length indicator, if required.
		if(attr(RAWWRITE)) {
			if(debugEnabled) wmsg = " " + len + " bytes ";
			lastLength = len;
		} else if(attr(VARIABLE_LENGTH) && attr(SIZEINVISIBLE)) {
			if(!thisIsReWrite && indexActive) relIndex.addRecord(offset, len+4);
			String lstr = String.format("%04d", len+4);
			ioStreamWrite(lstr.getBytes(encoding));
			offsetAddress += lstr.length();
			if(debugEnabled) wmsg = " "+(len+4)+" bytes ";
			lastLength = len + 4;
		} else if(attr(RECORD_SEPARATOR)) {
			if(!thisIsReWrite && indexActive) relIndex.addRecord(offset, len);
			if(debugEnabled) {
				int l;
				setSystemSeparator();
				l = recordSeparator.length;
				if(debugEnabled) wmsg = " "+len+" bytes + "+l+" bytes ";
			}
			lastLength = len;
		} else {
			if(!thisIsReWrite && indexActive) relIndex.addRecord(offset, len);
			if(debugEnabled) wmsg = " "+len+" bytes ";
			lastLength = len;
		}
		
		if(attr(RECORD_SEPARATOR) && !attr(RAWWRITE)) {
			byte [] b;
			if(recordSeparator == null) {
				setSystemSeparator();
			}
			if(recordSeparator.length > 0) {
				b = new byte[len + statusLen + recordSeparator.length];
				System.arraycopy(buf, pos, b, 0, len + statusLen);
				System.arraycopy(recordSeparator, 0, b, len + statusLen, recordSeparator.length);
				ioStreamWrite(b, 0, len + statusLen + recordSeparator.length);
				offsetAddress += len + statusLen + recordSeparator.length;
			}
		} else {
			ioStreamWrite(buf, pos, len + statusLen);
			offsetAddress += len + statusLen;
		}
//		lock.release();
		if(debugEnabled) {
			log.debug(fileName+": "+operation+wmsg+"(actual). crn="+rnAccessLast+
				" nrn="+rnAccessNext+" lineCount="+recordCount+
				" lastAccessOffset="+offsetAccessLast+
				" actualOffset="+0);
		}
//		if(!attr(RELATIVE) && actualOffset != offsetAccessLast) {
//			Timer.end();
//			throw new IOException("lastaccessoffset != actualoffset");
//		}
		rnAccessLast = rnAccessNext;
		
		rnAccessNext++;
		if(!thisIsReWrite) {
//			Timer.end();
			recordCount++;
		}
		thisIsReWrite = false;
		if(indexVariable != null && !attr(RANDOM)) indexVariable.set(rnAccessNext);
		return true;
	}
	
	/**
	 * Create any missing records up to, but not including recNum.
	 * This works only for RELATIVE|RANDOM
	 * @param recNum
	 * @throws IOException 
	 */
	private void createMissingRecords(int recNum) throws IOException {
		if(!(attr(RELATIVE) && attr(RANDOM))) {
			throw new IllegalStateException("File is not RELATIVE|RANDOM");
		}
		int reclen = recordLength + statusLen;
		int recCount = countLines();
		if(recCount < recNum) {
			byte [] b = new byte[reclen];
			Arrays.fill(b, (byte)' ');
			b[b.length - 1] = REC_DELETED;
			for(int i=recCount; i<recNum;i++) {
				ioStream.seek(reclen * i);
				ioStream.write(b);
			}
		}
	}

	/**
	 * Write out the provided String before advancing the specified number of lines 
	 * @param lines the number of lines to advance
	 * @param buf the String to write
	 * @throws IOException failure to access underlying file.
	 */
	public void writeBeforeAdvancing(int lines, String buf)  throws IOException {
		write(buf);
		for(int i=1; i<lines; i++) write("");
	}
	
	/**
	 * Overloaded version of {@link #writeBeforeAdvancing(int, String)} except we use a Var 
	 * and {@link Var#getString()} to obtain the String
	 * @param lines
	 * @param buf
	 * @throws IOException
	 */
	public void writeBeforeAdvancing(int lines, Var buf)  throws IOException {
		write(buf);
		for(int i=1; i<lines; i++) write("");
	}
	
	/**
	 * Overloaded version of {@link #writeBeforeAdvancing(int, String)} except we use a Var 
	 * to represent the number of lines see {@link Var#getInt()}  
	 * @param lines
	 * @param buf
	 * @throws IOException
	 */
	public void writeBeforeAdvancing(Var lines, String buf)  throws IOException {
		write(buf);
		for(int i=1; i<lines.getInt(); i++) write("");
	}
	
	/**
	 * Overloaded version of {@link #writeBeforeAdvancing(int, String)} except we use a Var to reprsent both the
	 * number of lines and the string to be written. 
	 * @param lines
	 * @param buf
	 * @throws IOException
	 */
	public void writeBeforeAdvancing(Var lines, Var buf)  throws IOException {
		write(buf);
		for(int i=1; i<lines.getInt(); i++) write("");
	}
	
	/**
	 * Overloaded, however with the potential to specify a command/message which will 
	 * translate into a an advance in the file.
	 * Presently the only recognised command/message is "PAGE" i.e. a form advance.
	 * 
	 * Please note that if no prior write has taken place i.e. we are doing the firstwrite 
	 * the String buf will not be written out.
	 * 
	 * @param cmd if null ignored, only present usable option "PAGE"
	 * @param buf the string to write
	 * @throws IOException
	 */
	public void writeBeforeAdvancing(String cmd, String buf)  throws IOException {
		if (!firstWrite) write(buf + "");
		if(cmd != null) {
			if(cmd.equals("PAGE")) {
				ioStream.writeBytes("\f");
				offsetAddress++;
			}
		}
		firstWrite=false;
	}
	
	/**
	 * Overloaded variant of {@link #writeBeforeAdvancing(String, String)}
	 * The only difference here is that the string to write out is a Var see {@link Var#getString()}
	 * @param cmd
	 * @param buf
	 * @throws IOException
	 */
	public void writeBeforeAdvancing(String cmd, Var buf)  throws IOException {
		if (!firstWrite) write(buf);
		if(cmd != null) {
			if(cmd.equals("PAGE")) {
				ioStream.writeBytes("\f");
				offsetAddress++;
			}
		}
		firstWrite=false;
	}
	
	/**
	 * Write out the provided String after advancing the specified number of lines 
	 * @param lines the number of lines to advance
	 * @param buf the String to write
	 * @throws IOException failure to access underlying file.
	 */
	public void writeAfterAdvancing(int lines, String buf)  throws IOException {
		for(int i=1; i<lines; i++) write("");
		write(buf);
	}
	
	
	/**
	 * Overloaded version of {@link #writeAfterAdvancing(int, String)} except we use a Var 
	 * and {@link Var#getString()} to obtain the String
	 * @param lines
	 * @param buf
	 * @throws IOException
	 */
	public void writeAfterAdvancing(int lines, Var buf)  throws IOException {
		for(int i=1; i<lines; i++) write("");
		write(buf);
	}
	
	/**
	 * Overloaded version of {@link #writeAfterAdvancing(int, String)} except we use a Var 
	 * to represent the number of lines see {@link Var#getInt()}  
	 * @param lines
	 * @param buf
	 * @throws IOException
	 */
	public void writeAfterAdvancing(Var lines, String buf)  throws IOException {
		int limit = lines.getInt();
		for(int i=1; i<limit; i++) write("");
		write(buf);
	}
	
	
	/**
	 * Overloaded version of {@link #writeAfterAdvancing(int, String)} except we use a Var to reprsent both the
	 * number of lines and the string to be written. 
	 * @param lines
	 * @param buf
	 * @throws IOException
	 */
	public void writeAfterAdvancing(Var lines, Var buf)  throws IOException {
		int limit = lines.getInt();
		for(int i=1; i<limit; i++) write("");
		write(buf);
	}

	/**
	 * Overloaded, however with the potential to specify a command/message which will 
	 * translate into a an advance in the file.
	 * Presently the only recognised command/message is "PAGE" i.e. a form advance.
	 * 
	 * @param cmd if null ignored, only present usable option "PAGE"
	 * @param buf the string to write
	 * @throws IOException
	 */
	public void writeAfterAdvancing(String cmd, String buf)  throws IOException {
		if(cmd != null) {
			if(cmd.equals("PAGE")) {
				ioStream.writeBytes("\f");
				offsetAddress++;
			}
		}
		write(buf);
		firstWrite=false;
	}
	
	/**
	 * Overloaded variant of {@link #writeAfterAdvancing(String, String)}
	 * The only difference here is that the string to write out is a Var see {@link Var#getString()}
	 * @param cmd
	 * @param buf
	 * @throws IOException
	 */
	public void writeAfterAdvancing(String cmd, Var buf)  throws IOException {
		if(cmd != null) {
			if(cmd.equals("PAGE")) {
				ioStream.writeBytes("\f");
				offsetAddress++;
			}
		}
		write(buf);
		firstWrite=false;
	}
	
	/**
	 * Reads the current record and returns it as a String to the calle3r.
	 * @return the record as a string.
	 * @throws IOException
	 */
	public String readLine() throws IOException {
		return read();
	}
	
	/**
	 * Reads the current record into the provided byte array.
	 * If the provided byte array length < than the record size an exception is thrown. 
	 * @param buf
	 * @return number of bytes read.
	 * @throws IOException if buf.length < record.length
	 */
	public int read(byte [] buf) throws IOException {
		byte [] b = readBytes();
		if(b == null || b.length == 0) return 0;
		
		if(buf.length < b.length) 
			throw new IOException(fileName+": Buffer overflow: required length="+b.length+ 
					" available length="+buf.length);
		System.arraycopy(b, 0, buf, 0, b.length);
		return b.length;
	}
	
	private boolean statusSeeking = false;
//	private static long rsTime;
//	private static long rByteTime;
//	private static int countBytesRead;
//	private static int countLoads;
//	private static int countRecordSeparator;
//	private static int countBytesLoaded;

	private static final byte [] nullBytes = new byte [] {0,0,0,0};
	
	/**
	 * Reads a record from a file. The size of the record is dependent on whether the file has
	 * been declares as variable length, fixed length or with record separators. No ascii/ebcdic
	 * translation is performed.
	 * @return A byte array holding exactly one record, or null on end-of-file.
	 * @throws IOException
	 */
	public byte [] readBytes() throws IOException {
		byte [] buffer;
		if(!attr(READ) || !fileIsOpen || ioStream == null) {
			String s;
			if(fd == null) s = fileName;
			else s = fd.getAbsolutePath();
			if(fileStatus == null)
				throw new IOException(s+": File is not open. Cannot read.");
			else {
				fileStatus.set(notOpen);
				return null;
			}
		}
//		long rbTime = System.currentTimeMillis();

		if(fileStatus != null) fileStatus.set(success);
		if(Timer.isActive())
			Timer.begin(titleIdentifier + ": read()");
		//Position for read
		if(!statusSeeking) {
			if(attr(RELATIVE) && indexVariable != null) {
//				System.out.println("readBytes: seekRecord()="+indexVariable.getInt());
				if(attr(RANDOM)) {
					if (!seekRecord(indexVariable.getInt() - 1)) {
						if(fileStatus != null)
							fileStatus.set(noRecordFound);
						return null;
					}
				} else { //Sequential
					if (!seekRecord(rnAccessNext)) {
						if(fileStatus != null)
							fileStatus.set(noNext);
						return null;
					}
					indexVariable.set(rnAccessNext);
				}
			} else if(attr(RANDOM)) {
				if(rnRequested == -1) {
					rnAccessNext = rnAccessLast;
					offsetAddress = offsetAccessLast;
					ioStream.seek(offsetAddress);
//					System.out.println("readBytes: seek="+offsetAddress);
				}
			}
		}
		long offset;
		if(indexActive) {
			 offset = relIndex.getOffset(rnAccessNext);
		} else if(rnAccessNext == 0) {
			offset = 0;
		} else if(rnAccessNext == rnAccessLast) {
			offset = offsetAccessLast;
		} else if(rnAccessNext == rnAccessLast + 1) {
			offset = offsetAddress;
		} else {
			if(attr(FIXED_LENGTH)) {
				offset = rnAccessNext * (recordLength + statusLen);
			} else if(indexActive) {
				offset = relIndex.getOffset(rnAccessNext);
			} else {
				Timer.end();
				throw new IOException(fileName+": oops! Unexpected state. indexActive="+indexActive+" nextRecordNumber="+rnAccessNext+" lastRecordNumber="+rnAccessLast);
			}
		}
		if(offset != offsetAddress) {
//			System.out.println("readBytes: seek="+offset+" offsetAddress="+offsetAddress);
			ioStream.seek(offset);
			offsetAddress = offset;
		}
		rnRequested = -1;
		offsetAccessLast = offset;

//		rByteTime += System.currentTimeMillis() - rbTime;
		try {
			if(attr(VARIABLE_LENGTH)) {
//				countLoads++;
//							System.out.println("readBytes(): VARIABLE_LEN");
				int rlen = ioStream.read(reclenBuffer);
				offsetAddress += rlen;
				if(rlen < 1) {
					if(fileStatus != null)
						fileStatus.set(noNext);
					return null; //EOF
				}
				if(rlen < 4) {
					throw new IOException(fileName+": Could not read record length bytes.");
				}

				if(Arrays.equals(reclenBuffer, nullBytes)) {
					if(fileStatus != null)
						fileStatus.set(noNext);
					return null; //EOF
				}

				//			int l2 = Util.asciiBytesToInt(reclenBuffer, 0, 4);
				int l = 0;
				int mult = 1;
				for(int i=3;i>=0;i--) {
					l += (reclenBuffer[i] - '0') * mult;
					mult *= 10;
				}

				if(l < 4) {
					throw new IOException(fileName+": Length indicator="+l+" ("
							+Util.formatHex(reclenBuffer)+") returns integer value of "+l);
				}
				l -= 4;
				if(attr(SIZEVISIBLE)) {
					buffer = new byte[l+4];
					System.arraycopy(reclenBuffer, 0, buffer, 0, 4);
					rlen = ioStream.read(buffer, 4, l);
				} else {
					buffer = new byte[l];
					rlen = ioStream.read(buffer, 0, l);
				}
				offsetAddress += rlen;
				lastLength = l + 4;
				if(rlen < l) {
					throw new IOException(fileName+": Premature end-of-file. Cannot read "+l+" bytes.");
				}
			} else if(attr(FIXED_LENGTH)) {
//							System.out.println("readBytes(): FIXED_LENGTH");
				if(recordLength < 1) {
					if(attr(AUTO_LENGTH)) { //Used for extract files. Assume separator is available.
						ioStream.seek(0);
						System.out.println("readBytes: seek="+offsetAddress);

						byte [] bb = new byte[4096];
						int ll = ioStream.read(bb);
						if(ll <= 0) {
							return null;
						}
						for(int i=0;i<ll;i++) {
							if(bb[i] == '\r') {
								if(i < (ll-1) && bb[i+1] == '\n') recordLength = i + 2;
								else recordLength = i + 1;
								break;
							} else if(bb[i] == '\n') {
								recordLength = i + 1;
								break;
							}
						}
					}
					if(recordLength < 1) {
						throw new IOException(fileName+": Record length has not been set. Cannot read fixed-length record.");
					}
					ioStream.seek(offsetAddress);
					System.out.println("readBytes: seek="+offsetAddress);

				}
				buffer = new byte[recordLength + statusLen];
				int rlen = ioStream.read(buffer, 0, recordLength + statusLen);
				if(rlen > 0) offsetAddress += rlen;
				else {
					if(fileStatus != null)
						fileStatus.set(noNext);
					return null; //EOF
				}
				if(attr(RELATIVE)) {
					if(attr(RANDOM)) {
						if(buffer[recordLength] == REC_DELETED) {
							if(fileStatus != null)
								fileStatus.set(noRecordFound);
							return null;
						}
					} else if(attr(SEQUENTIAL)) {
						while(buffer[recordLength] == REC_DELETED) {
							rlen = ioStream.read(buffer, 0, recordLength + statusLen);
							if(rlen > 0) {
								offsetAddress += rlen;
								rnAccessLast = rnAccessNext;
								rnAccessNext++;
							} else {
								if(fileStatus != null)
									fileStatus.set(noNext);
								return null;
							}
						}
					}
					byte [] tb = new byte[recordLength];
					System.arraycopy(buffer, 0, tb, 0, recordLength);
					buffer = tb;
				}
			} else if(attr(RECORD_SEPARATOR)) {

//				countRecordSeparator++;
//							System.out.println("readBytes(): RECORD_SEPARATOR");
				if(indexActive) {
//					System.out.println("readBytes(): indexActive");
//					countLoads++;
					lastLength = relIndex.autoSeek(rnAccessNext);
					buffer = new byte[lastLength];
					int rlen = ioStream.read(buffer);
					if(rlen > 0) offsetAddress += rlen;
					else {
						if(fileStatus != null)
							fileStatus.set(noNext);
						return null;
					}
					if(rlen < lastLength) {
						throw new IOException(fileName+": Premature end-of-file. Cannot read "+lastLength+" bytes.");
					}
				} else {
//					System.out.println("readBytes(): !indexActive");
					long stime = System.currentTimeMillis();
					buffer = readUntilSeparator();
					stime = System.currentTimeMillis() - stime;
//					rsTime += stime;
					if(buffer != null) {
						lastLength = buffer.length;
//						countBytesRead += lastLength;
//						out.write(new String(buffer, encoding)+"\n");
					} else {
						if(fileStatus != null)
							fileStatus.set(noNext);
					}

				}
			} else {
				throw new IOException(String.format("Unknown mode: 0%o.", attr));
			}
			int alen;
			if(buffer == null) alen = 0;
			else alen = buffer.length;
			if(log.isDebugEnabled()) {
				long actualOffset = ioStream.getFilePointer();
				log.debug(fileName+": read len="+alen+" bytes crn="+rnAccessLast+
						" nrn="+rnAccessNext+" count="+recordCount+
						" lastAccessOffset="+offsetAccessLast+" actualOffset="+actualOffset);
			}
			rnAccessLast = rnAccessNext;
			rnAccessNext++;
			if(indexVariable != null) {
				indexVariable.set(rnAccessNext);
			}
			//if(buffer!=null) log.debug(fileName+": read "+buffer.length+" bytes. file-attributes="+printAttributes());
			//if(attr(EBCDIC) && attr(TRANSLATE)) {
			//	String x = new String(buffer, ebcdicEncoding);
			//	buffer = x.getBytes(asciiEncoding);
			//}
			return buffer;
		} finally {
			Timer.end();
//			System.out.println("Counts: bytesRead="+countBytesRead+" recordSep="+countRecordSeparator+" countLoads="+countLoads+" bytesLoaded="+countBytesLoaded+ " Time in readUntilSep()="+rsTime+" readBytesTime="+rByteTime);
		}
	}
	
	/**
	 * Reads len bytes from file and returns a String
	 * @param len The maximum number of bytes to read
	 * @return The String representation of the data read. The length of the string is
	 * the number of bytes actually read. Returns null for EOF
	 * @throws IOException
	 */
	public String read(int len) throws IOException {
		byte [] buf = new byte[len];
		int rlen = read(buf, len);
		if(rlen == 0) return null;
		String s = new String(buf, encoding);
		return s;
	}
	
	/**
	 * Reads len bytes of data from a file into buf.
	 * @param buf The byte buffer to accept file data
	 * @param len The maximum number of bytes to read
	 * @return The number of bytes read, or 0 for EOF
	 * @throws IOException
	 */
	public int read(byte [] buf, int len) throws IOException {
		if(buf == null || buf.length < len) 
			throw new IOException(fileName+": buf is too small for requested length");
		if(Timer.isActive())
			Timer.begin(titleIdentifier + ": read()");
		int rlen = ioStream.read(buf, 0, len);
		if(rlen <= 0) return 0;
		offsetAddress += rlen;
		Timer.end();
		return len;
	}
	
	/**
	 * Reads the current record and sets the value into the proided Var
	 * @param v
	 * @return true if read, false if any exception occurred.
	 * @throws IOException
	 */
	public boolean read(Group g) throws IOException {
		byte [] b = readBytes();
		if(b == null) {
			return false;
		}
		if(fileStatus != null) fileStatus.set(success);
		g.set(b);
		return true;
	}
	
	/**
	 * Reads the current record and sets the value into the proided Var
	 * @param v
	 * @return true if read, false if any exception occurred.
	 * @throws IOException
	 */
	public boolean read(Var v) throws IOException {
		if(v instanceof Group)
			return read((Group)v);
		
		String s = read();
		if(s == null) {
			return false;
		}
		if(fileStatus != null) fileStatus.set(success);
		v.set(s);
		return true;
	}
	
	/**
	 * Reads a record from the file and returns a string.
	 * The bytes are decoded into the specified encoding.
	 * @return
	 * @throws IOException
	 */
	public String read() throws IOException {//TODO filestatus
		byte [] b = readBytes();
		if(b == null) {
			if(fileStatus != null) fileStatus.set(notOpen);
			return null;
		}
		if(fileStatus != null) fileStatus.set(success);
		String s = new String(b, 0, b.length, encoding);
		return s;
	}
	
	private byte [] readBuffer = null;
	//private int maxLength = 4096;
	private static final int maxLength = 5120;
	private int bufPtr = 0;
	private int endPtr = 0;
	private boolean loadBuffers() throws IOException {
		if(readBuffer == null) readBuffer = new byte[maxLength+1];
		if(bufPtr > 0 && bufPtr < endPtr) {
			System.arraycopy(readBuffer, bufPtr, readBuffer, 0, endPtr - bufPtr);
			endPtr = endPtr - bufPtr;
			bufPtr = 0;
		} else {
			bufPtr = 0;
			endPtr = 0;
		}
		int rlen = ioStream.read(readBuffer, endPtr, maxLength - endPtr);
		if(rlen <= 0) {
//			System.out.println("MssFile.loadBuffers() - read EOF");
			return false;
		}
//		countLoads++;
//		countBytesLoaded += rlen;
//		System.out.println("MssFile.loadBuffers() - read " + rlen + " bytes");
		endPtr += rlen;
		
		offsetAddress += rlen;
		return true;
	}
	
	private byte [] readUntilSeparator() throws IOException {
		if(readBuffer == null) readBuffer = new byte[maxLength+1];
		//Look for the record separator

		int ptr;
		int sizeSep = 0;
		boolean found = false;
		while(true) {
			
			if(bufPtr >= endPtr) {
				if(!loadBuffers()) {
					return null;
				}
			}
			
			for(ptr = bufPtr; ptr < endPtr; ptr++) {
				if(recordSeparator == null) { //look for '\n', '\r' or '\r\n'
					if(readBuffer[ptr] == '\n') { 
						sizeSep = 1;
						found = true;
						recordSeparator = new byte [] { '\n' };
						break;
					} else if(readBuffer[ptr] == '\r') {
						sizeSep = 1;
						found = true;
						if(readBuffer[ptr+1] == '\n') {
							sizeSep++;
						}
						recordSeparator = new byte [] { '\r', '\n' };
						break;
					}
				} else if(readBuffer[ptr] == recordSeparator[0] ) { //must match all characters in separator
					boolean match = true;
					for(int j=1; j<recordSeparator.length;j++) {
						if(readBuffer[ptr+j] != recordSeparator[j]) {
							match = false;
							break;
						}
					}
					if(match) {
						found = true;
						sizeSep = recordSeparator.length;
						break;
					}
				}
			}

			if(found) {
				byte [] record = new byte[ptr - bufPtr];
				System.arraycopy(readBuffer, bufPtr, record, 0, ptr - bufPtr);
				bufPtr = ptr + sizeSep;
//				System.out.println("MssFile.readUntilSeparator() - returned "+record.length+" bytes");
				return record;
			} else {
				if(!loadBuffers()) {
					byte [] record = new byte[endPtr - bufPtr];
					System.arraycopy(readBuffer, bufPtr, record, 0, endPtr - bufPtr);
					bufPtr = ptr + sizeSep;
//					System.out.println("MssFile.readUntilSeparator() - returned "+record.length+" bytes");
					return record;
				}
			}
		}
	}
	
	/**
	 * Close and dispose of resources w.r.t. the file and the stream writing data to it.
	 * See {@link java.io.
	 */
	public void close() {
		try {
			flush();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	    if(ioStream != null) {
			try {
				ioStream.close();
				if(indexActive) relIndex.close();
			} catch(IOException e) {
			}
			ioStream = null;
	    } else {
	    	if(fileStatus != null) fileStatus.set(notOpen);
	    	return;
	    }
	    if(fileStatus != null) fileStatus.set(success);
		attr &= ~(READ|WRITE|APPEND|TRUNCATE);
		firstWrite = true;
		fileIsOpen = false;
		relIndex = null;
		indexActive = false;
		if(fd != null) lastModified = fd.lastModified();
		else lastModified = 0;
		if(log.isDebugEnabled())
			log.debug(fileName+": close count="+recordCount);
	}
	
	/**
	 * Delete all resources associated with MssFile including the
	 * {@link MssRelativeIndex} if the index is active.
	 * @throws IOException
	 */
	public void delete() throws IOException {
		File fds = this.fd;
		if(fds == null) {
			if(fileName == null || fileName.equals("")) { 
				if(fileStatus != null) {
					fileStatus.set(noRecordFound);
				}
				return;
			}
			fds = new File(fullPath(fileName));
		}
		fds.delete();
		if(indexActive) {
			relIndex.delete();
			relIndex.close();
			relIndex = null;
			indexActive = false;
		}
		recordCount = -1;
	}
	
	/**
	 * Delete file only
	 * @throws IOException
	 */
	public void deleteFile() throws IOException {
		if(attr(FILE_VARNAME)) {
			fileName = fileVar.getTrim();
		}
		if(fileName.equals("") || isOpen()) {
			return;
		}
		File fds = new File(fullPath(fileName));
		fds.delete();
	}
	/**
	 * Close/Dispose and then remove all file resources used.
	 * Combines {@link #close()} & {@link #delete()}
	 * 
	 * @throws IOException
	 */
	public void closeRemove() throws IOException {
		close();
		delete();
	}
	
	/**
	 * Return the filename underpining this instance of the MssFile.
	 * @return
	 */
	public String getFileName() {
		return fileName;
	}
	
	
	/**
	 * Returns the year with the day of the year as an int e.g. 2009120 
	 * means the file was last modofied on the 120th day of 2009.
	 * @return int representation of year + day of year e.g. 2009235
	 */
	public int getModified() {
		File tfd = this.fd;
		if(tfd == null) tfd = new File(fullPath(fileName));
		Date date = new Date(tfd.lastModified());
		SimpleDateFormat df = new SimpleDateFormat("yyyyDDD");  
		
		return Integer.parseInt(df.format(date));
	}
	
	public long getFileSize() throws IOException {
		if(!fileIsOpen)
			throw new IOException("File is not open. Cannot get line count.");
		if(fd == null) return -1;
		return ioStream.length();
	}
	
	
	/**
	 * How many lines in this file? It is calculated for fixed length file format.
	 * @return number of lines.
	 * @throws IOException
	 */
	public int countLines() throws IOException
	{
		long time = System.currentTimeMillis();
//		System.out.println("readBytes: countLines()");
		if(!fileIsOpen)
			throw new IOException("File is not open. Cannot get line count.");
		if(fd == null) return -1;
		if(ioStream.length() == 0) {
			recordCount = 0;
			return (int)recordCount;
		}
		if(Timer.isActive())
			Timer.begin(titleIdentifier + ": countLines()");
		int count;
		if(attr(FIXED_LENGTH)) {
			if(recordLength <= 0)
				throw new IOException("Cannot count FIXED LENGTH records when we don't know the record size.");
			long fileSize = fd.length();
			count = (int)(fileSize / (recordLength+statusLen));
		} else {
			if(recordCount >= 0) return (int)recordCount;
			if(!indexActive) {
				relIndex = new MssRelativeIndex(this);
				indexActive = true;
			}
			count = relIndex.recordCount();
		}
	    recordCount = count;
	    Timer.end();
	    time = System.currentTimeMillis() - time;
	    log.debug("countLines()={} time={}",count,time); 
	    return count;
	}
	
	/**
	 * Returns the number of the last record in the file, zero relative.
	 * @return Last line number
	 * @throws IOException 
	 */
	public int lastRecordNumber() throws IOException {
		return countLines() - 1;
	}
	
	/**
	 * Have we counted the number of records present in the file 
	 * @return true if it has been counted, false otherwise.
	 */
	public boolean counted() {
		if(recordCount == -1) return false;
		return true;
	}
	
	/**
	 * Not implemented as yet
	 * @return always returns 0 presently.
	 */
	public int getSerialno() {
		if(fd == null) return 0;
        return 0;	
	}
	
	/** 
	 * Set a title or new title
	 * @param title
	 * @return
	 * @throws IOException
	 */
	public int setTitle(String title) throws IOException {
		fileName = Util.unixFilename(title);
		titleChange = true;
		setModes();
		if(log.isDebugEnabled())
			log.debug(fileName+": setTitle("+title+") mode="+printAttributes());
       return 0;	
	}
	
	/**
	 * Overlaoad of {@link #setTitle(String)} except uses a Var for
	 * the Title name.
	 * @param title
	 * @return
	 * @throws IOException
	 */
	public int setTitle(Var title) throws IOException {
		setTitle(title.toString().trim());
        return 0;	
	}
	
	/**
	 * If file exists return 1 else -1 if does not uses {@link #exists()}
	 * @return 1 exists aka resident, -1 not resident...
	 * @throws IOException
	 */
	public int resident() throws IOException {
		if(exists()) return 1;
		return -1;
	}
	
	/**
	 * See {@link File#exists()}
	 * @return
	 * @throws IOException
	 */
	public boolean exists() throws IOException {
		if(fd == null) {
			if(attr(FILE_VARNAME)) {
				String fn = fileVar.getTrim();
				if(fn.compareTo(fileName) != 0) fileName = fn;
			}
			fd = new File(fullPath(fileName));
		}
		return fd.exists();
	}
	

	/**
	 * Return the size of the file in bytes
	 * @return
	 * @throws IOException 
	 */
	public int size() throws IOException {
		if(!exists())
			return 0;
		return (int) fd.length();
	}

	/**
	 * Rename the underlying file
	 * @param newname the new name for the file
	 * @return true if renamed, false otherwise
	 * @throws IOException
	 */
	public boolean renameTo(String newname) throws IOException {
		if(fd == null) return false;
		if(newname == null || newname.length() == 0) return false;
		if(indexActive) relIndex.renameTo(newname);
		close();
		File newfd = new File(fullPath(newname));
		return fd.renameTo(newfd);
	}
	
	/**
	 * Please see {@link File#length()}
	 * @return
	 */
	public long fileLength() {
		if(fd == null) return 0;
        return fd.length();	
	}

	public String getEncoding() {
		return encoding;
	}
	
	public String getNote() {
		return note;
	}
	public void setNote(String lNote) {
		note=lNote;
	}
	public void setNote(Var lNote) {
		note=lNote.getString();
	}
	public void setBlockSize(int blockSize) {
		return;
	}
	public void setBlockSize(Var blockSize) {
		return;
	}
	public void setMaxrecsize(int maxrecsize) {
		return;
	}
	public void setMaxrecsize(Var maxrecsize) {
		return;
	}
	public int getNextRecord() {
		return rnAccessNext;
	}
	public boolean start(String condition) throws IOException, Exception {
		if(attr(RELATIVE)) {
			int recNum;
			if(indexVariable != null)
				recNum = indexVariable.getInt() - 1;
			else
				recNum = 0;
			rnAccessNext = recNum;
			seekRecord(recNum);
			return true;
		}
		if(ioStream == null) 
			throw new IOException(fileName+": Cannot start on unopened file.");
		throw new Exception("start not yet implemented");
	}

	public String getEbcdicEncoding() {
		return ebcdicEncoding;
	}
}


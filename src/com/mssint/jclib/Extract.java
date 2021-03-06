package com.mssint.jclib;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Provides a generalised mechanism for extracting data from a Report file.
 * Data can be retrieved by record identifier. Specific records can be targeted.  
 * Presently fixed length files with a record separator are supported. 
 * Please note that data can also be written.
 * 
 * Additionally sorting is provided.
 * 
 * @author MssInt
 *
 */
public class Extract extends Report {
	private static final Logger log = LoggerFactory.getLogger(Extract.class);

	public static final int TEMPORARY = 0001;
	public static final int PERMANENT = 0002;
	public static final int EXISTING = 0004;

	private boolean fileClass(int c) {
		return ((fileClass & c) != 0);
	}

	private static boolean initialised = false;
	private static ArrayList<Extract> extractList;
	private static String extractPath; // Path to extract directory
	// private static String extractNameRule; //Rule for naming extract files

	private String buffer; // Holds data just read/written
	private String lastBuffer; //Holds data read on last read
	private String saveBuffer; //Holds new buffer (for onChange)
	private boolean inSavedState;
	private int recordNumber;
	private int seekToRecord;
	private String rs;
	@SuppressWarnings("unused")
	private FrameState extractedAs;
//	private boolean firstAccess;

	private int fileClass; // TEMPORARY, PERMANENT or EXISTING

	// TODO: Record navigation
	// We are making the assumption that extract files are fixed-length records
	// with
	// a record separator. This assumption does need to be challenged or tested
	// to the full.
	private int recordLength = 0;

	private Glb glb;
	private boolean readARecord;
	
	public static void releaseAll() {
		if(extractList == null)
			return;
		for(Extract ex : extractList)
			ex.release();
	}

	/**
	 * Constructor taking only the filename of the extract file.
	 * No Report Identifier so this is will work on a temporary file.
	 * No Global Variable instance provided, it is set to null.
	 * @param fileName extract file name.
	 * @throws ClassNotFoundException 
	 */
	public Extract(String fileName) throws ClassNotFoundException {
		this(null, null, fileName);
	}

	/**
	 * Overloaded Constructor for Extract. 
	 * No Global Variable instance provided, it is set to null.
	 * @param repid the Report Id 
	 * @param fileName extract file name.
	 * @throws ClassNotFoundException 
	 */
	public Extract(String repid, String fileName) throws ClassNotFoundException {
		this(null, repid, fileName);
	}

	/**
	 * Fully qualified Constructor.
	 * @param glb global variable instance
	 * @param repid the Report Id 
	 * @param fileName extract file name.
	 */
	public Extract(Glb glb, String repId, String fileName) throws ClassNotFoundException {
		// repId is the first character (A=1, etc)
		init();
		title = null;
		this.fileName = null;
		if(repId != null && repId.length() > 0)
			this.repId = (char) (repId.charAt(0) - 'A' + 1);
		else this.repId = 0;
		this.glb = glb;
		recordNumber = 0;
		seekToRecord = -1;
		if(repId != null && repId.length() > 1) {
			char c = repId.charAt(0);
			if (c >= 'A' && c <= 'Z')
				this.repId = (char) (c - 'A' + 1);
			else if (c >= 'a' && c <= 'z')
				this.repId = (char) (c - 'a' + 1);
			fileClass = PERMANENT;
		}				
		
		if (fileName != null && fileName.length() > 0) {
			attr |= FIRST_ACCESS|NO_EMPTY_CREATE;
			this.title = fileName;
		}
		if(fileClass == PERMANENT)
			try {
				createFile(false);
			} catch (IOException e) {
			}
		extractList.add(this);
		readARecord = false;
		buffer = null;
		saveBuffer = null;
		lastBuffer = null;
		inSavedState = false;
	}

	private void init() {
		if (initialised)
			return;
		permanent = false;
//		firstAccess = false;
		extractPath = System.getenv("EXTRACT_DIR");
		if (extractPath == null)
			extractPath = Config.getProperty("jclib.extract.dir");
		if (extractPath == null)
			extractPath = ".";
		// extractNameRule = System.getProperty("extract.name");
		extractList = new ArrayList<Extract>();
		initialised = true;
		titleSet = false;
		fileClass = 0;
		return;
	}
	
	
	private void createFile(boolean rename) throws IOException {
		//create new empty file. Existing file overwritten, unless "EXISTING" is set.
		this.fileName = newFileName();
		if(fd != null) fd.close();
		String f = Util.getFileName(extractPath, fileName);
		Util.mkdir(Util.dirName(f));
		
		if(fd != null && rename) fd.renameTo(f);
		
		if(!rename && fileClass != EXISTING) {
			try {
				if(fd == null) fd = new MssFile(f);
				fd.delete();
				fd.open("w");
				if(fd.isOpen()) fd.close();
			} catch (IOException e) {
				log.error("File create failed: file='"+f+"' error="+e.getMessage());
			}
		}
	}

	/**
	 * Overridden toString() method.
	 * Return whatever is currently in the buffer as a result of a {@link #fetch()} {@link #print(FrameState)}.
	 */
	public String toString() {
		return buffer;
	}

	/**
	 * Gets the record size see {@link MssFile#getRecordLength()
	 * @return the record length of records in the file.
	 */
	public int getRecordLength() {
		if (fd == null)
			return 0;
		return fd.getRecordLength();
	}

	private void writeLine(String buf) throws IOException {
		if(!attr(FD_WRITE)) {
			if (attr(FD_READ)) {
				fd.close();
				attr &= ~(FD_READ);
			}
			fileName = newFileName();
			String f = Util.getFileName(extractPath, fileName);
			Util.mkdir(Util.dirName(f));
			fd = new MssFile(f);
			try {
				if (fd.isModeSet(MssFile.MODE_CONFIG)) {
					recordLength = fd.getRecordLength();
				} else {
					String s = Config.getProperty("file.attribute.default");
					if(s != null) {
						fd.setMode(s);
					}
				}
				byte [] fdrs = fd.getRecordSeparator();
				if(fdrs != null && fdrs.length > 0) {
					rs = new String(fdrs);
					fd.setRecordSeparator(null);
				}
				if(rs == null || rs.length() == 0)
					rs = "\n";
				recordLength = buf.length() + rs.length();
				fd.setMode(MssFile.FIXED_LENGTH);
				if(attr(FIRST_ACCESS)) fd.open("w");
				else fd.open("a+");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(!permanent && (fileClass(TEMPORARY)))
				fd.deleteOnExit();
			recordNumber = 0;
			seekToRecord = -1;
			if (fd != null)
				attr |= FD_WRITE;
		} else if (!fd.isOpen()) {
			fd.reOpen("a+");
		}
		buffer = buf;

		//Add the record separator to buf
        if(buf.length() < (recordLength-rs.length()))
            buf = Util.pad(buf, (recordLength-rs.length())) + rs;
        else
            buf += rs;
        recordLength = buf.length();

        //Check if record-length has increased
		if(fd.recordLength > 0 && fd.recordLength < buf.length()) {
			fd.increaseRecordLength(buf.length(), rs);
		} else if(fd.recordLength == 0) 
			fd.setRecordLength(recordLength);
		
		fd.writeLine(buf);
		attr |= WRITTEN;
		recordNumber++;
		attr &= ~(FIRST_ACCESS);
	}

	/**
	 * Overloaded option see {@link #setRecord(int)}
	 * @param Var representing an integer 
	 */
	public void setRecord(Var record) {
		setRecord(record.getInt());
	}

	/**
	 * Set the record we are going to use.
	 * @param record an integer between 1 and 32565 
	 */
	public void setRecord(int record) {
		seekToRecord = record;
	}

	/**
	 * Retrieve the current record index.
	 * @return 
	 */
	public int getRecord() {
		if (seekToRecord != -1)
			return seekToRecord;
		return recordNumber;
	}

	/*
	 * private String newFileName() { String e; e =
	 * System.getenv("EXTRACT_NAME"); if(e == null) e =
	 * Config.getProperty("jclib.extract.name"); String fname; if(e == null) {
	 * if(title != null && title.length() > 1) { fname = title; } else { fname =
	 * reportName; char letter = (char)((repId-1) + 'A'); if(repId > 0) fname =
	 * fname + "." + letter; } } else { fname =
	 * applicationState.envParser.parseProperty(e, this); } fname =
	 * fname.replaceAll("/", "_"); return fname; }
	 */

	public int getRecordNumber() {
		return recordNumber;
	}

	public String getBuffer() {
		return buffer;
	}
	public void setBuffer(String buf) {
		buffer = buf;
	}

	/**
	 * Always returns true
	 * @param true
	 */
	public boolean isExtract() {
		return true;
	}

	/**
	 * Always returns false
	 * @param false
	 */
	public boolean isReport() {
		return false;
	}

	/**
	 * Overloaded please see {@link #print(FrameState)}
	 * @param obj must be a FramState
	 * @throws IOException
	 */
	public void print(Object obj) throws IOException {
		print(obj, 0);
	}
	
	/**
	 * Prints out the extract data contained in the FramState instance see {@link FrameState#getExtractString()} 
	 * @param a FrameState instance
	 */
	public void print(FrameState frame) throws IOException {
		print(frame, 0);
	}

	/**
	 * Prints out the extract data contained in the FramState instance see {@link FrameState#getExtractString()} 
	 * @param a FrameState instance
	 * @param line is ignored
	 */
	public void print(FrameState frame, int line) throws IOException {
		if (attr(RELEASE))
			release();
		if (frame == null)
			return;
		String s = frame.getExtractString();
		writeLine(s);
		// frame.clear();
	}

	/**
	 * Please see {@link #print(FrameState, int)}
	 * @param obj
	 * @param line
	 * @throws IOException
	 */
	public void print(Object obj, int line) throws IOException {
		if (obj instanceof CursorState) {
			CursorState c = (CursorState) obj;
			if (attr(RELEASE))
				release();
			String s = c.getExtractString();
			writeLine(s);
		} else {
			// Class<?> objClass = obj.getClass();
			// String cName = objClass.getName();
			// ArrayList<Var> vars = new ArrayList<Var>();
			// Field[] fields = getClass().getDeclaredFields();
			/*
			 * for (int i = 0; i < fields.length; i++) { Object o; try { o =
			 * fields[i].get(obj); } catch(IllegalAccessException e) { continue;
			 * } }
			 */
		}
	}

	/**
	 *  Open for extract file for reading
	 */
	public void open() {
		open(fileName);
	}
	
	/**
	 * Migration of EXTRACTED.AS; - the effect of this is that if the previous operation on this
	 * extract item was setTitle(), and the file is currently PERMANENT, it's state will be changed
	 * to EXISTING. This means that the existing data will be retained.
	 * @param extractedAs
	 */
	public void open(FrameState extractedAs) {
		this.extractedAs = extractedAs;
		open(fileName);
	}

	public void open(String name) {
		if(attr(FIRST_ACCESS) && fileClass == PERMANENT) {
			fileClass = EXISTING;
		}
		if(attr(FD_READ)) {
			return;
		}
		attr &= ~(FIRST_ACCESS);
		close();
		if (titleSet) {
			recordLength = -1;
		} else if (name != null)
			title = name;
		fileName = newFileName();
		if(log.isDebugEnabled())
			log.debug("ex" + (char) (repId + '@') + " open() sets filename="
				+ fileName);

		String f = Util.getFileName(extractPath, fileName);
		fd = new MssFile(f);
		try {
			if(!fd.exists() && fileClass != EXISTING) return;
			if (fd.isModeSet(MssFile.MODE_CONFIG)) {
				recordLength = fd.getRecordLength();
				rs = null;
			} else {
				String s = Config.getProperty("file.attribute.default");
				if(s == null) {
					fd.setMode(MssFile.FIXED_LENGTH | MssFile.AUTO_LENGTH);
					if (recordLength > 0)
						fd.setRecordLength(recordLength);
					rs = "\n";
				} else
					fd.setMode(s);
			}
			if(attr(FIRST_ACCESS)) {
				if(!fileClass(EXISTING)) {
					fd.delete();
					fd.open("w");
					fd.close();
				}
				attr &= ~(FIRST_ACCESS);
			}
			fd.open("r");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (!permanent && (fileClass(TEMPORARY)))
			fd.deleteOnExit();
		if (fd.isOpen())
			attr |= FD_READ;
		recordNumber = 0;
		if(seekToRecord == 0)
			seekToRecord = -1;
		readARecord = false;
	}
	
	public void release() {
		release_extract();
		super.release();
	}

	/**
	 * Used in case file has never been accessed.
	 */
	private void release_extract() {
		if(!attr(FD_WRITE) && attr(FIRST_ACCESS)) {
			if(attr(FD_READ)) {
				fd.close();
				attr &= ~(FD_READ);
			}
			if(fd == null) {
				fileName = newFileName();
				String f = Util.getFileName(extractPath, fileName);
				Util.mkdir(Util.dirName(f));
				fd = new MssFile(f);
			}
			try {
				if(attr(FIRST_ACCESS) && !attr(NO_EMPTY_CREATE) && fileClass != EXISTING) fd.open("w");
			} catch (IOException e) {
				e.printStackTrace();
			}
			if(!permanent && (fileClass(TEMPORARY)))
				fd.deleteOnExit();
			recordNumber = 0;
		}
	}

	public boolean fetch() throws IOException {
		if(!attr(FD_READ)) {
			open(fileName);
			if (!fd.isOpen()) {
				if (glb != null)
					glb.setStatus("*****");
				return false;
			}
		}
		if(inSavedState) {
			recoverLastBuffer();
		}
		byte [] rsep = fd.getRecordSeparator();
		if(rsep == null || rsep.length == 0)
			try {
				fd.setRecordSeparator("0A");
			} catch (IOException e) {
			}
		if(seekToRecord != -1) {
			if (recordLength <= 0) { // need to do at least one read first.
				byte[] b = fd.readBytes();
				if (b != null)
					recordLength = b.length;
			}
			try {
				fd.seekRecord(seekToRecord); // we actually read the next record.
				recordNumber = seekToRecord - 1;
			} catch(IOException e) {
				if(glb != null) {
					if(log.isDebugEnabled())
						log.debug("Fetch record no " + seekToRecord + " invalid");
					glb.setStatus("*****");
				}
				try {
					if(recordNumber < 0)
						recordNumber = 0;
					fd.seekRecord(recordNumber);
				} catch(IOException x) {}
			}
			seekToRecord = -1;
		}
		recordNumber++;
		saveBuffer = buffer;
		buffer = fd.readLine();
		if(buffer == null) {
			if(glb != null) {
				if(readARecord)
					glb.setStatus("     ");
				else
					glb.setStatus("*****");
			}
			buffer = saveBuffer;
			return false;
		}
		if(recordLength <= 0)
			recordLength = buffer.length();
		if(glb != null)
			glb.setStatus("     ");
		readARecord = true;
		return true;
	}
	
	public void initialiseLastBuffer() {
		if(lastBuffer != null) return;
		lastBuffer = buffer;
	}
	/**
	 * Used by OnChange
	 */
	public void setLastBuffer() {
		if(lastBuffer == null) return;
		if(inSavedState) return;
		saveBuffer = buffer;
		buffer = lastBuffer;
		inSavedState = true;
	}
	
	/**
	 * Used by OnChange
	 */
	public void recoverLastBuffer() {
		if(!inSavedState) return;
		lastBuffer = saveBuffer;
		buffer = saveBuffer;
		inSavedState = false;
	}

	public void restart() {
		close();
		attr |= FIRST_ACCESS;
	}

	public void close() {
		if (fd != null && attr(FD_READ | FD_WRITE))
			fd.close();
		attr &= ~(FD_READ | FD_WRITE);
		recordNumber = 0;
		readARecord = false;
	}

	public void sort(CursorState cstate, String[] keys) {
		FrameVar[] fv = new FrameVar[keys.length];
		for (int i = 0; i < keys.length; i++) {
			fv[i] = cstate.getFrameVar(keys[i]);
			if (fv[i] == null)
				Util.abort("Extract.sort() could not resolve field '" + keys[i]
						+ "'.");
		}
		sort(fv);
	}

	public void sort(FrameVar[] keys) {
		/*
		 * for(int i=0; i < keys.length; i++) { FrameVar v = keys[i];
		 * Util.debug("key "+i+" is "+ v.getName() +
		 * " offset="+v.getOffset()+" len="+v.getExlen()); }
		 */
		if(fileName == null) fileName = newFileName();
		String fn = Util.getFileName(extractPath, fileName);

		if(fileClass == 0)
			fileClass = TEMPORARY;
		// Check if file exists. If not, don't bother.
		File f = new File(fn);
		if(!f.exists()) {
			if(fileClass(PERMANENT) || fileClass(EXISTING))
				try {
					f.createNewFile();
				} catch (IOException e) {}
			attr &= ~(FIRST_ACCESS);
			return;
		} else {
			if(attr(FIRST_ACCESS) && !fileClass(EXISTING)) {
				//any existing file should be cleared.
				try {
					f.delete();
					f.createNewFile();
				} catch (IOException e) {}
				attr &= ~(FIRST_ACCESS);
				return;
			}
		}
		attr &= ~(FIRST_ACCESS);
		Sort s = new Sort(fn, keys);
		int records;
		try {
			records = s.sort(fd);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			records = 0;
		}
		Util.info(records + " records sorted.");
	}

	/*
	 * public ArrayList<FrameVar> order(FrameVar v, ArrayList<FrameVar> keys) {
	 * if(keys == null) keys = new ArrayList<FrameVar>(); keys.add(v); return
	 * keys; }
	 */

	public Var getVar(FrameVar v) {
		if (buffer == null)
			buffer = "";
		return v.newClone(buffer);
	}

	public Var getVar(Object mainObject, Object name) {
		if (buffer == null)
			buffer = "";
		if (mainObject != null && name != null
				&& mainObject instanceof CursorState) {
			FrameVar v;
			if (name instanceof String)
				v = ((CursorState) mainObject).getFrameVar((String) name);
			else if (name instanceof Var)
				v = ((CursorState) mainObject).getFrameVar((Var) name);
			else
				v = null;
			return getVar(v);
		}
		return new Var(buffer);
		// return v.newClone(buffer);
	}
	
	public void setVar(FrameVar v, Var value) {
		setVar(v, value.getString());
	}
	public void setVar(FrameVar v, String value) {
		int offs = v.getOffset();
		int len = v.getExlen();
		if(value == null) value = Var.spaces.substring(0, len);
		else if(value.length() < len) value = value + Var.spaces.substring(0, len - value.length());
		else if(value.length() > len) value = value.substring(0, len);
		
		StringBuilder sb = new StringBuilder();
		if(buffer == null) sb.append(Var.spaces.substring(0, offs + len));
		else sb.append(buffer);
		if(sb.length() < (offs + len)) sb.append(Var.spaces.substring(0, (offs+len)-sb.length()));
		sb.replace(offs, offs+len, value);
		buffer = sb.toString();
	}
	
	public String getEncoding() {
		if(fd != null)
			return fd.getEncoding();
		else return null;
	}

	public void setTitle(String title) {
		setTitle(title, false);
	}

	public void setTitle(String title, boolean existing) {
//		boolean rename = false;
		attr |= FIRST_ACCESS;
		if(title != null && title.equals("DUMMY")) {
			close();
			return;
		}
		if(fd != null) { //check the previous file
			if(fileClass == TEMPORARY) {
				try {
					fd.closeRemove();
					fd = null;
				} catch (IOException e) {}
			} else if(fileClass == PERMANENT) {
					try {
						if(!attr(WRITTEN)) {
							fd.closeRemove();
							fd = null;
						} else {
							fd.close();
						}
					} catch (IOException e) {}
			} else if(fileClass == EXISTING) {
				if(attr(WRITTEN)) {
					//If it's been accessed it gets renamed to the new file.
//					rename = false;
				} else {
					fd.close();
				}
			}
			close();
		}
		if (title == null)
			title = "null";
		int i = title.indexOf(' ');
		if (i > 0)
			title = title.substring(0, i);
		this.title = title;
		if(existing) fileClass = EXISTING;
		else fileClass = PERMANENT;
		String f = Util.getFileName(extractPath, newFileName());
		fd = new MssFile(f);
		try {
			if(!fd.exists()) {
				fd.open("w");
				fd.close();
			}
		} catch (IOException e) {}
		
		permanent = true;
		titleSet = true;
	}
	public void setExtractPath(String path) {
		if(path == null)
			extractPath=".";
		else
			extractPath=path;
	}
	public void setExtractPath(Var path) {
		setExtractPath(path.getString());
	}
}

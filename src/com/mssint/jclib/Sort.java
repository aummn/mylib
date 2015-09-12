package com.mssint.jclib;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Provides the ability to sort the following types
 * String[]
 * 
 * 
 * @author MssInt
 *
 */
public class Sort {
	private static final Logger log = LoggerFactory.getLogger(Sort.class);
	//For performance reasons, calculations of values are performed locally
	//rather than relying on dereferences to Var types.
	
	//--- the keys for sorting ---//	
	private FrameVar[] keyVars;
	private Group[] groupVars;
	
	//--- file handling variables ---//
	private MssFile infile;
	private MssFile outfile;
	private String sortFile;

	//--- Output file handling ---//
	Var outfileIdx = null;
	
	//--- variables used in managing the sort ---//
	protected String data[];
	protected int maxRecords = 400000;
	protected int sortSize;
	
	
	protected class MergeItem {
		protected String dataItem;
		private MssFile fd;
		protected int sequence;
		
		public MergeItem(int sequence, MssFile fd) {
			this.fd = fd;
			this.sequence = sequence;
			try {
				fd.reOpen("r");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			loadNextRecord();
		}
		
		public boolean loadNextRecord() {
			try {
				dataItem = fd.readLine();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(dataItem == null) return false;
			return true;
		}
		public String getData() { return dataItem; }
		public void delete() throws IOException {
			fd.close();
			fd.delete();
			dataItem = null;
			fd = null;
		}
	}

	protected class tCompare implements Comparator<MergeItem> {
		private Compare superCompare;
		
		public tCompare(Compare superCompare) {
			this.superCompare = superCompare;
		}
		
		@Override
	    public int compare(MergeItem m1, MergeItem m2) {
			int diff = superCompare.compare(m1.dataItem, m2.dataItem);
			if(diff == 0) return m1.sequence - m2.sequence;
			return diff;
		}
	}
	
	/**
	 * Provides the Comparator mechanism for sorting.
	 * 
	 * @author MssInt
	 *
	 */
	protected class Compare implements Comparator<String> {
	    private int offsets[];
	    private int ends[];
	    private boolean isChar[];
	    private boolean[] descending;
	    private String collationEncoding;

		public void loadKeys(FrameVar[] keys) {
		    if(keys.length == 0) {
		        offsets = null;
		    } else {
		        offsets = new int[keys.length];
		        ends = new int[keys.length];
		        isChar = new boolean[keys.length];
		        descending = new boolean[keys.length];
		        for(int i=0; i<keys.length; i++) {
		            offsets[i] = keys[i].getOffset();
		            ends[i] = offsets[i] + keys[i].getExlen();
		            descending[i] = keys[i].isDescending();
		            isChar[i] = keys[i].isCharType();
		        }
		    }
		}
		
		/**
		 * 
		 * @param groups
		 */
		public void loadGroups(Group[] groups) {
			if(groups.length == 0) {
				offsets = null;
			} else {
				offsets = new int[groups.length];
				ends = new int[groups.length];
				isChar = new boolean[groups.length];
				descending = new boolean[groups.length];
				for(int i=0; i<groups.length; i++) {
					offsets[i] = groups[i].getMyOffset();
					ends[i] = offsets[i] + groups[i].getLen();
					descending[i] = groups[i].isDescending();
					isChar[i] = groups[i].testAttr(Var.CHAR) ? true : false;
				}
			}
		}
		
		@Override
		public int compare(String c1, String c2) {
			for(int i=0; i < offsets.length; i++) {
			    int c;
			    int start, end;
			    start = offsets[i];
			    end = ends[i];
			    if(c1.length() < end) end = c1.length();
			    if(end < start) start = end;
				String s1 = c1.substring(start, end);
			    start = offsets[i];
			    end = ends[i];
			    if(c2.length() < end) end = c2.length();
			    if(end < start) start = end;
			    String s2 = c2.substring(start, end);
				if(isChar[i]) {
					if(collationEncoding != null) {
						try {
							byte [] b1 = s1.getBytes(collationEncoding);
							byte [] b2 = s2.getBytes(collationEncoding);
							c = byteCompare(b1, b2);
						} catch (UnsupportedEncodingException e) {
							e.printStackTrace();
							c = 0;
						}
					} else {
						c = s1.compareTo(s2);
					}
				} else {
					long l1, l2;
					try { l1 = Long.parseLong(s1);
					} catch(NumberFormatException e) { l1 = 0; }
					try { l2 = Long.parseLong(s2);
					} catch(NumberFormatException e) { l2 = 0; }
					if(l1 < l2) c = -1;
					else if(l1 > l2) c = 1;
					else c = 0;
				}
			    if(c != 0) {
					if(descending[i]) return -c;
					return c;
				}
			}
			return 0;
		}

		private int byteCompare(byte[] b1, byte[] b2) {
			for(int i=0; i<b1.length; i++) {
				if(i > b2.length) {
					return -1;
				}
				int c = (b1[i] & 0xff) - (b2[i] & 0xff);
				if(c != 0) {
					return c;
				}
			}
			return 0;
		}

		public void setCollation(String collationEncoding) {
			this.collationEncoding = collationEncoding;
		}
	}

	/**
	 * 
	 * Non default constructor that allows the definition of a file containing the 
	 * sort definition in a file and the FrameVar(s) to use as the sort keys in the file.
	 * 
	 * @param file the MssFile containing the keys 
	 * @param keys defining the keys
	 */
	public Sort(String file, FrameVar[] keys) {
		if(file == null || keys == null) {
			Util.abort("Attempt to sort file '" + 
				file == null ? "null" : file + 
				keys == null ? "' with undefined keys." : "'");;
		}
		infile = null;
		outfile = null;
		sortFile = file;
		keyVars = keys;
	    sortSize = 0;
	    data = new String[maxRecords];
	}
	
	/**
	 * Default constructor will need to be used in conjunction with
	 * {@link #sort(MssFile, Group...)} or
	 * {@link #sort(MssFile, MssFile, Group...)} or
	 * {@link #sort(Object, Object, Group...)} or
	 * {@link #Sort(String, FrameVar[])} 
	 */
	public Sort() {
		sortFile = null;
		infile = null;
		outfile = null;
		keyVars = null;
		groupVars = null;
		sortSize = 0;
		data = new String[maxRecords];
	}
	
	/**
	 * Sorts the provided MssFile in-situ.
	 * @param infile the MssFile to sort
	 * @param keys the keys on which to sort.
	 * @return the number of records sorted
	 */
	public int sort(MssFile infile, Group ... keys) {
		return sort(infile, null, keys);
	}
	
	/**
	 * Sorts the provided MssFile in-situ.
	 * @param infile the MssFile to sort
	 * @param outfile the MssFile to place the sorted values into
	 * @param keys the keys on which to sort.
	 * @return the number of records sorted
	 */
	public int sort(MssFile infile, MssFile outfile, Group ... keys) {
		if(infile != null) 
			infile.close();
		if(outfile != null) 
			outfile.close();
		this.infile = infile;
		this.outfile = outfile;
		groupVars = keys;
		try {
			return sort();
		} catch (IOException e) {
			return 0;
		}
	}

	/**
	 * This is the generic sort routine that allows any combination of the following as input and output : - 
	 * JbolApplication, JbolSection, Paragraph & MssFile
	 * If both input and output are MssFile then we are simulating {@link #sort(MssFile, MssFile, Group...)}
	 * In the case where the input or output is one of JbolApplication, JbolSection, Paragraph the internal temporary  
	 * file within the objects is used for input and the results are stored into a newly created temporary file.
	 * 
	 * Additionally it allows for the definition of Object[2] for both input and output values. The sort routine will
	 * do a perform Object[0] to Object[1] on behalf of the caller then perform the sort and then if out is similarly  
	 * defined it will do a perform on these before creating the temporary file into which it will write the sorted data.
	 * This does not hold true for MssFile as it is not an excutable/performable item..
	 * 
	 * @param inp either JbolApplication, JbolSection, Paragraph & MssFile
	 * @param out either JbolApplication, JbolSection, Paragraph & MssFile
	 * @param keys the keys to sort on.
	 * @return number of records sorted.
	 * @throws Exception - Unknown/unimplemented type.
	 */
	public int sort(Object inp, Object out, Group ... keys) throws Exception {
		JbolApplication app = null;
		boolean inpIsFile;
		boolean outIsMethod;
		outfile = null;
		if(inp instanceof JbolSection) {
			app = ((JbolSection)inp).getJbolApplication();
			Timer.begin("sort: perform inp");
			//Timer.suspend();
			app.perform((JbolSection)inp);
			//Timer.resume();
			Timer.end();
			infile = app.getTempFile();
			inpIsFile = false;
		} else if(inp instanceof Paragraph) {
			app = ((Paragraph)inp).getJbolApplication();
			app.perform((Paragraph)inp);
			infile = app.getTempFile();
			inpIsFile = false;
		} else if(inp instanceof Object[]) {
			Object[] iobj = (Object[])inp;
			if(iobj.length != 2) throw new IllegalArgumentException("Expected array of 2 JbolSection / Paragraph objects.");
			if(iobj[0] instanceof Paragraph)
				app = ((Paragraph)iobj[0]).getJbolApplication();
			else if(iobj[0] instanceof JbolSection)
				app = ((JbolSection)iobj[0]).getJbolApplication();
			else throw new IllegalArgumentException("Parameter 'inp' of unknown type.");
			app.perform(iobj[0], iobj[1]);
			infile = app.getTempFile();
			inpIsFile = false;
		} else if(inp instanceof MssFile) {
			infile = (MssFile)inp;
			inpIsFile = true;
		} else throw new IllegalArgumentException("Parameter 'inp' of unknown type.");

		if(out instanceof JbolSection) {
			app = ((JbolSection)out).getJbolApplication();
			outIsMethod = true;
		} else if(out instanceof Paragraph) {
			app = ((Paragraph)out).getJbolApplication();
			outIsMethod = true;
		} else if(out instanceof Object[]) {
			Object[] obj = (Object[])out;
			if(obj.length != 2) throw new IllegalArgumentException("Expected array of 2 JbolSection / Paragraph objects.");
			if(obj[0] instanceof Paragraph)
				app = ((Paragraph)obj[0]).getJbolApplication();
			else if(obj[0] instanceof JbolSection)
				app = ((JbolSection)obj[0]).getJbolApplication();
			else throw new IllegalArgumentException("Parameter 'inp' of unknown type.");
			outIsMethod = true;
		} else if(out instanceof MssFile) {
			outfile = (MssFile)out;
			outIsMethod = false;
		} else throw new IllegalArgumentException("Parameter 'out' of unknown type.");

		if(infile == null) {
			if(inpIsFile) throw new IllegalStateException("Input Sort file is null.");
			if(log.isDebugEnabled())
				log.debug("Temporary sort file is empty - no records were written.");
			//Ensure that the output file (if not null) exists and is empty.
			if(outfile != null) {
				outfile.reOpen("w");
				outfile.close();
			}
			if(!outIsMethod) return -1;
			infile = new MssFile("./", "wt");
		}
		infile.reOpen("r");
		groupVars = keys;
		
		//If the output is the tempfile but the input was a real file, we
		//must prepare the tempfile as the sort output.
		if(outfile == null) {
			if(outIsMethod) {
				if(app == null) throw new IOException("JbolApplication instance is null.");
				app.clearTempFile();
				app.createTempFile();
				outfile = app.getTempFile();
				if(outfile == null) outfile = infile;
			} else outfile = infile;
		}
		
		//Do the actual sort
		Timer.begin("sort: Read input file");
		//Timer.suspend();
		int rval = sort();
		//Timer.resume();
		Timer.end();
		
		if(out instanceof JbolSection) {
			if(app == null) throw new IOException("JbolApplication instance is null.");
			Timer.begin("sort: perform output section");
			//Timer.suspend();
			app.perform((JbolSection)out);
			//Timer.resume();
			Timer.end();
		} else if(out instanceof Paragraph) {
			if(app == null) throw new IOException("JbolApplication instance is null.");
			app.perform((Paragraph)out);
		} else if(out instanceof Object[]) {
			Object [] obj = (Object[])out;
			if(app == null) throw new IOException("JbolApplication instance is null.");
			app.perform(obj[0], obj[1]);
		}
		return rval;
	}
	private int wrnum;
	
	/**
	 * Sorts an MssFile and places the results into itself this will do String based sorting.
	 * @param sortFile the MssFile to sort
	 * @return the number of records sorted
	 * @throws IOException file does not exist or locked etc...
	 */
	public int sort(MssFile sortFile) throws IOException {
		infile = sortFile;
		return sort();
	}
	
	/**
	 * This method is called internally by all the other sort functions.
	 * If called independently from an external it will work on the previously defined
	 * input/output file and keys.
	 *  
	 * @return
	 * @throws IOException
	 */
	public int sort() throws IOException {
		if(keyVars == null && groupVars == null) return 0;
		//ArrayList<MssFile> mergeList = null;
		TreeSet<MergeItem> mergeList = null;
	    
		Compare compare = new Compare();
		//Prepare keys for sorting.
		if(keyVars != null) {
			compare.loadKeys(keyVars);
		} else {
			compare.loadGroups(groupVars);
		}
		
	    if(infile == null) {
	    	infile = new MssFile(sortFile, "r");
	    } else infile.reOpen("r");
	    
	    if(infile.isModeSet(MssFile.SORT_EBCDIC)) {
	    	compare.setCollation(infile.getEbcdicEncoding());
	    }
	    
		if(log.isDebugEnabled())
			log.debug("input file="+infile.getFileName());
	    if(outfile == null) outfile = infile;
	    else if(log.isDebugEnabled())
			log.debug("output file="+outfile.getFileName());

		int record = 0;
		int recordCount = 0;
	    int mergeFiles = 0;
	    boolean setRecordNumberVar;
	    boolean setRecordNumber;
	    Var idxVar = null;
	    if(infile.attr(MssFile.RELATIVE)) {
	    	if(infile.getIndexVariable() != null) {
	    		setRecordNumberVar = true;
	    		setRecordNumber = false;
	    		idxVar = infile.getIndexVariable();
	    	} else if(infile.attr(MssFile.RANDOM)) {
	    		setRecordNumber = true;
	    		setRecordNumberVar = false;
	    	} else { //SEQUENTIAL, no var
	    		setRecordNumber = false;
	    		setRecordNumberVar = false;
	    	}
	    } else {
	    	setRecordNumber = false;
	    	setRecordNumberVar = false;
	    }

	    int count = 0;
	    if(setRecordNumberVar) idxVar.set(1 + count++);
	    else if(setRecordNumber) infile.seekRecord(count++);
		while((data[record] = infile.read()) != null) {
			record++;
			if(record >= maxRecords) {
				if(mergeList == null) {
				    tCompare tcompare = new tCompare(compare);
					mergeList = new TreeSet<MergeItem>(tcompare);
				}
				if(log.isDebugEnabled())
					log.debug("Creating merge set " + (mergeList.size() + 1));
			    Arrays.sort(data, 0, record, compare);
				MssFile mfd = new MssFile(sortFile, "wt");
			    for(int i=0; i<record; i++) {
					mfd.write(data[i]);
					data[i] = null;
				}
				mfd.close();
				recordCount += record;
				record = 0;
				MergeItem mi = new MergeItem(mergeFiles++, mfd);
				mergeList.add(mi);
			}
			if(setRecordNumberVar) idxVar.set(1 + count++);
		    else if(setRecordNumber) infile.seekRecord(count++);
		}
	    infile.close();
	    
		//Temporary debug:
		//String ifile = infile.getFileName();
		//String ofile = outfile.getFileName();
		//String cmd = "cp "+ifile+" "+ifile+".before.sort";
		//log.debug("before sort: cmd="+cmd);
		//Runtime.getRuntime().exec(cmd);
		
		
		outfile.close();
	    outfile.reOpen("w");

	    if(outfile.attr(MssFile.RELATIVE)) {
	    	if(outfile.getIndexVariable() != null) {
	    		setRecordNumberVar = true;
	    		setRecordNumber = false;
	    		outfileIdx = outfile.getIndexVariable();
	    	} else if(outfile.attr(MssFile.RANDOM)) {
	    		setRecordNumber = true;
	    		setRecordNumberVar = false;
	    	} else { //SEQUENTIAL, no var
	    		setRecordNumber = false;
	    		setRecordNumberVar = false;
	    	}
	    } else {
	    	setRecordNumber = false;
	    	setRecordNumberVar = false;
	    }
	    
	    if(setRecordNumberVar) outfileIdx.set(1);
	    wrnum = 0;
    
		if(mergeList == null) { //No merge took place
			if(record == 0) return 0;
			//Timer.resume();
			Timer.begin("Array.sort");
			Arrays.sort(data, 0, record, compare);
			Timer.end();
			//Timer.suspend();
			for(int i=0; i<record; i++) {
		        writeOutput(data[i],setRecordNumber, setRecordNumberVar);
		        data[i] = null;
		    }
		    recordCount = record;
		} else {
			if(record > 0) { //records left in buffer
				MssFile mfd = new MssFile(sortFile, "wt");
			    Util.info("sort(): Creating merge set " + (mergeList.size() + 1));
				Arrays.sort(data, 0, record, compare);
				for(int i=0; i<record; i++) {
					mfd.write(data[i]);
					data[i] = null;
				}
				mfd.close();
				recordCount += record;
				record = 0;
				mergeList.add(new MergeItem(mergeFiles++, mfd));
			}
			if(log.isDebugEnabled())
				log.debug("Merging " + mergeList.size() + " sorted sets.");
		    while(!mergeList.isEmpty()) {
				MergeItem mfd = mergeList.first();
				mergeList.remove(mfd);
				writeOutput(mfd.getData(), setRecordNumber, setRecordNumberVar);
				if(mfd.loadNextRecord()) mergeList.add(mfd);
				else mfd.delete();
			}
			mergeList = null;
		}
		
		//for(int i=0; i<keys.length; i++) keys[i].clearDescending();
		
		outfile.flush();
		outfile.close();
		
		//Debug - save after-sort file
		//cmd = "cp "+ofile+" "+ofile+".after.sort";
		//log.debug("after sort: "+cmd);
		//Runtime.getRuntime().exec(cmd);
		
		
		outfile = null;
		infile = null;
	    log.info("sort(): Sort complete. "+recordCount+" records sorted.");
		return recordCount;
	}
	
	/**
	 * Store the sort results.
	 * @param record
	 * @param rn
	 * @param v
	 * @throws IOException
	 */
	private void writeOutput(String record, boolean rn, boolean v) throws IOException {
		if(v) outfileIdx.set(1 + wrnum++);
		else if(rn) outfile.seekRecord(wrnum++);
		outfile.write(record);
	}
}

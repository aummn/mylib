package com.mssint.jclib;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MssRelativeIndex {
	private static final Logger log = LoggerFactory.getLogger(MssRelativeIndex.class);
	MssFile primary;
	private File fdIdx;
	private Var indexVar = null;
	
	private RandomAccessFile iosIdx = null;
	private long autoIndex;
	private long autoOffset;
	private int autoLength;
	private boolean validIndex = false;
	private long recordCount;
	private static final String encoding = Config.getProperty("jclib.ebcdic.encoding");
	private boolean creatingIndex;

	/**
	 * Create new instance and initialise. Do not create indexes, but open
	 * if one exists.
	 * @param file
	 * @throws IOException 
	 */
	public MssRelativeIndex(MssFile file) throws IOException {
		primary = file;
		recordCount = 0;
		autoIndex = 0;
		creatingIndex = false;
		openIndexFile();
	}
	
	public MssRelativeIndex() throws IOException {
		throw new IOException("Cannot call default constructor.");
	}
	
	protected void setIndexVar(Var v) { indexVar = v; }
	protected Var getIndexVar() { return indexVar; }
	
	public boolean isValidIndex() { return validIndex; }
	
	private void openIndexFile() throws IOException {
		if(iosIdx != null) iosIdx.close();
		File fd = primary.getFileInstance();
		if(fdIdx == null) {
			String path = fd.getParent();
			String fname = "." + Util.baseName(fd.getName()) + ".idx";
			if(path == null) path = ".";
			fdIdx = new File(path + "/" + fname);
		}
		boolean valid;
		//Check if file is valid. Delete if not.
		if(!fdIdx.exists()) valid = false;
		else if(fd.length() == 0 && fdIdx.length() != 0) valid = false;
		else if(fd.length() != 0 && fdIdx.length() == 0) valid = false;
		else if(fdIdx.lastModified() < fd.lastModified()) valid = false;
		else valid = true;
		if(valid) {
			iosIdx = new RandomAccessFile(fdIdx, "rw");
			validIndex = true;
			recordCount = iosIdx.length() / 12;
			if(log.isDebugEnabled())
				log.debug(primary.getFileName()+": openIndexFile index=valid count="+recordCount);
		} else {
			if(fdIdx.exists()) fdIdx.delete();
			//log.debug(primary.getFileName()+": openIndexFile index=invalid");
			iosIdx = null;
			validIndex = false;
		}
		autoIndex = 0;
		autoOffset = 0;
		autoLength = -1;
	}
	
	protected void open() throws IOException {
		openIndexFile();
		if(validIndex) return;
		
		//Not available, therefore need to create a file.
		creatingIndex = true;
		iosIdx = new RandomAccessFile(fdIdx, "rw");
		if(primary.attr(MssFile.VARIABLE_LENGTH)) {
			addAllIndexes_VariableLen();
		} else if(primary.attr(MssFile.FIXED_LENGTH)) {
			addAllIndexes_FixedLen();
		} else addAllIndexes_RecordSep();
		iosIdx.close();
		iosIdx = new RandomAccessFile(fdIdx, "rw");
		validIndex = true;
		creatingIndex = false;
		if(recordCount > 0)
			if(log.isDebugEnabled())
				log.debug(primary.getFileName()+": Created index file. Record count="+recordCount);
	}
	
	protected void close() throws IOException {
		if(iosIdx == null) return;
		iosIdx.close();
		iosIdx = null;
		validIndex = false;
	}
	
	protected void delete() throws IOException {
		close();
		if(fdIdx.exists()) fdIdx.delete();
	}
	
	public int recordCount() throws IOException {
		if(!validIndex) open();
		return (int)(iosIdx.length() / 12);
	}
	
	public boolean renameTo(String newname) throws IOException {
		if(fdIdx == null) return false;
		if(newname == null || newname.length() == 0) return false;
		
		close();
		String path = fdIdx.getParent();
		String fname = "." + Util.baseName(newname) + ".idx";
		if(path == null) path = ".";
		File newfd = new File(path + "/" + fname);
		return fdIdx.renameTo(newfd);
	}

	private void addAllIndexes_VariableLen() throws IOException {
		long offset = 0;
		byte [] bsize = new byte[4];
		byte [] buf = new byte[0];
		RandomAccessFile fs = new RandomAccessFile(primary.getFileInstance(), "r");
		while(true) {
			offset = fs.getFilePointer();
			int len = fs.read(bsize);
			if(len <= 0) break;
			if(len < 4) {
				fs.close();
				throw new IOException(primary.getFileName()+
					": Premature end-of-file. Unable to read 4 bytes for size indicator.");
			}
			if(bsize[0]==0 && bsize[1]==0 && bsize[2]==0 && bsize[3]==0) break;
			String s;
			if(primary.attr(MssFile.EBCDIC))
				s = new String(bsize, encoding);
			else s = new String(bsize);
			try {
				len = Integer.parseInt(s) - 4;
			} catch (NumberFormatException e) { len = -1; }
			if(len < 0) {
				fs.close();
				throw new IOException(primary.getFileName()+
					": Bad record length indicator ("+Util.formatHex(bsize)+
					")at offset "+offset);
			}
			if(len > buf.length) buf = new byte[len];
			int xlen = fs.read(buf, 0, len);
			if(xlen < len) {
				fs.close();
				throw new IOException(primary.getFileName()+
					": Premature EOF at offset "+offset+". Unable to read record of "+len+" bytes.");
			}
			addRecord(offset, len+4);
		}
		fs.close();
	}
	
	private void addAllIndexes_FixedLen() throws IOException {
		long offset = 0;
		int reclen = primary.getRecordLength();
		byte [] buf = new byte[reclen];
		if(reclen < 1) throw new IOException(primary.getFileName()+
				": Unknown record length.");
		RandomAccessFile fs = new RandomAccessFile(primary.getFileInstance(), "r");
		while(true) {
			int xlen = fs.read(buf);
			if(xlen <= 0) break;
			if(xlen != reclen) {
				fs.close();
				throw new IOException(primary.getFileName()+
					": Short read for last record. Expected "+reclen+" got "+xlen+".");
			}
			addRecord(offset, reclen);
			offset += reclen;
		}
		fs.close();
	}
	
	public static final int READBUFSZ = 16384;
	private void addAllIndexes_RecordSep() throws IOException {
		byte [] sep = primary.getRecordSeparator();
		RandomAccessFile fs = new RandomAccessFile(primary.getFileInstance(), "r");
		int len;
		int reclen = 0;
		byte [] buf = new byte[READBUFSZ];
		
		if(sep == null || sep.length == 0) {
			sep = new byte[1];
			sep[0] = '\n';
		}
		
		int xstart = 0;
		int start = 0;
		long offset = 0;
		boolean match = false;
		while(true) {
			len = fs.read(buf, xstart, READBUFSZ - xstart);
			if(len < 1) break;
			start = 0;
			for(int i=xstart;i<(xstart+len);i++) {
				//Find next record separator
				if(buf[i] == sep[0]) {
					match = true;
					for(int j=1;j<sep.length && (i+j) < (xstart+len);j++) {
						if(sep[j] != buf[i+j])
							match = false;
					}
				}
				if(match) {
					addRecord(offset, reclen);
					offset += reclen + sep.length;
					start = i + sep.length;
					reclen = 0;
					match = false;
				} else reclen++;
			}
			
			xstart = READBUFSZ - start;
		}
		fs.close();
	}
	
	/**
	 * Append a new record to the end of the index file. This becomes the current record.
	 * @param offset
	 * @param len
	 * @throws IOException
	 */
	protected void addRecord(long offset, int len) throws IOException {
		if(iosIdx == null) open();
		long flen = iosIdx.length();
		iosIdx.seek(flen);
		iosIdx.writeLong(offset);
		iosIdx.writeInt(len);
		recordCount++;
		if(!creatingIndex && log.isDebugEnabled())
			log.debug(primary.getFileName()+": add count="+recordCount+" recnum="+(flen/12)+" offset="+offset+" len="+len);
	}

	/**
	 * Return's the offset for record. The record is set as the current record.
	 * @param record The record number
	 * @return The offset into the MssFile object associated with this index.
	 * @throws IOException
	 */
	protected long getOffset(long record) throws IOException {
		if(record != autoIndex || autoLength == -1)
			setCurrentRecord(record);
		return autoOffset;
	}
	/**
	 * Return's the record length for this record. The record is set as the current record
	 * @param record The record number
	 * @return The length of the record at this index 
	 * @throws IOException
	 */
	protected int getLength(long record) throws IOException {
		if(record != autoIndex || autoLength == -1)
			setCurrentRecord(record);
		return autoLength;
	}
	
	protected void setCurrentRecord(long record) throws IOException {
		if(iosIdx == null) open();
		long ofs = record * 12;
		iosIdx.seek(ofs);
		System.out.println("seek: setCurrentRecord:"+ofs);
		if(record >= recordCount) {
			autoOffset = primary.getFileInstance().length();
			autoLength = 0;
			return;
		}
		autoOffset = iosIdx.readLong();
		autoLength = iosIdx.readInt();
		autoIndex = record;
		if(log.isDebugEnabled())
			log.debug(primary.getFileName()+": get count="+recordCount+" recnum="+record+" offset="+autoOffset+" len="+autoLength);
	}
	
	/**
	 * Attempts to position the file pointer of the MssFile object to the correct position for
	 * the next read or write operation. The behaviour of this method depends on what type of
	 * file we are dealing with.
	 * For RELATIVE SEQUENTIAL:
	 * @return The length of the current record. 
	 * @throws IOException
	 */
	protected int autoSeek() throws IOException {
		setCurrentRecord(autoIndex);
		return autoLength;
	}
	
	protected int autoSeek(int recnum) throws IOException {
		if(!validIndex) open();
		setCurrentRecord(recnum);
		return autoLength;
	}
/*
	private void seekToCurrentRecord() throws IOException {
		if(!attr(RELATIVE)) return;
		if(indexVariable != null) autoIndex = indexVariable.getLong() - 1;
		if(autoIndex < 0 || autoIndex > lineCount)
			throw new IOException(fileName+": Attempt to access file beyond limits. (index="+(autoIndex+1)+").");
		if(autoIndex == lineCount) currentOffset = ioStream.length();
		else if(attr(FIXED_LENGTH)) currentOffset = recordLength * autoIndex;
		else if(attr(VARIABLE_LENGTH)) currentOffset = getIndexOffset(autoIndex);
		else throw new IOException(fileName+": Unknown mode "+
				String.format("0%o", attr)+" for RELATIVE file access.");
		ioStream.seek(currentOffset);
		currentRecordNumber = autoIndex;
	}
	
	private void seekToEnd() throws IOException {
		if(!attr(RELATIVE)) return;
		autoIndex = lineCount;
		currentOffset = ioStream.length();
		if(indexVariable != null) indexVariable.set(autoIndex + 1);
		ioStream.seek(currentOffset);
		currentRecordNumber = autoIndex;
	}
	
	// zero relative
	public void seekToRecord(int recnum) throws IOException {
		if(!fileIsOpen) throw new IOException(fileName+": File is not open.");
		if(attr(FIXED_LENGTH)) {
			long offset = recnum * recordLength;
			lastReadOffset = offset;
			currentRecordNumber = recnum;
		} else throw new IOException("non-fixed-length not yet coded.");
	}
	*/

}

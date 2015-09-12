package com.mssint.jclib;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import junit.framework.TestCase;

public class ExtractFileTest extends TestCase {
	

	public class Frame01 extends FrameState {
		public FrameVar NUM = new FrameVar(1, 1, "unumber",3);
		public FrameVar TEXT = new FrameVar(1, 10, "char",60);
	}
	Frame01 f01 = new Frame01();
	
	//This function MUST be first - sets up config.
	public void testMakeProperties() {
		try {
			@SuppressWarnings("unused")
			Config config = new Config();
		} catch (FileNotFoundException e) {}
		
		Config.setProperty("jclib.extract.dir", "/tmp/");
		Config.setProperty("jclib.report.dir", "/tmp/");
		Config.setProperty("jclib.default.filepath", "/tmp/");
	}
	
	private String FILENAME = "TESTFILE";
	
	//Make a file for use in other extract files
	private void makeFile(int records) throws ClassNotFoundException, IOException {
		Extract x = new Extract("X", null);
		x.setTitle(FILENAME, true);
		runExtract(x, records);
		x.close();
		x = null;
	}
	
	private void runExtract(Extract ex, int records) throws IOException {
		for(int i = 0; i < records; i++) {
			f01.NUM.set(i+1);
			f01.TEXT.set("MAKE RECORD " + (i+1) + ": DATA RECORD");
			ex.print(f01);
		}
	}
	
	
	private void deleteFile() {
		File fd = new File("/tmp/"+FILENAME);
		fd.delete();
	}
	
	public boolean checkFileExists() {
		File fd = new File("/tmp/"+FILENAME);
		return fd.exists();
	}
	public boolean checkFileSize(int records) {
		File fd = new File("/tmp/"+FILENAME);
		int expectedLen = records * 64;
		return fd.length() == expectedLen;
	}
	
	public void testFirstAccess() throws ClassNotFoundException, IOException {
		Extract exA;
		makeFile(3);
		
		assertEquals(true, checkFileExists());
		assertEquals(true, checkFileSize(3));
		
		//First access is a read of a EXISTING file - verify it has 3 records.
		exA = new Extract("A", null);
		exA.setTitle(FILENAME, true);
		int count = 0;
		exA.open(); while(exA.fetch()) count++; exA.close();
		assertEquals(true, checkFileExists());
		assertEquals(true, checkFileSize(3));
		assertEquals(3, count);
		
		//PERMANENT file
		exA = new Extract("A", null);
		exA.setTitle(FILENAME);
		count = 0;
		exA.open(); while(exA.fetch()) count++; exA.close();
		assertEquals(true, checkFileExists());
		assertEquals(true, checkFileSize(0));
		assertEquals(0, count);
		
		//EXISTING and sort as first access
		makeFile(3);
		exA = new Extract("A", null);
		exA.setTitle(FILENAME, true);
		exA.sort(new FrameVar [] {f01.NUM});
		count = 0;
		exA.open(); while(exA.fetch()) count++; exA.close();
		assertEquals(3, count);
		assertEquals(true, checkFileExists());
		assertEquals(true, checkFileSize(3));

		//PERMANENT and sort as first access
		exA = new Extract("A", null);
		exA.setTitle(FILENAME);
		exA.sort(new FrameVar [] {f01.NUM});
		count = 0;
		exA.open(); while(exA.fetch()) count++; exA.close();
		assertEquals(0, count);
		assertEquals(true, checkFileExists());
		assertEquals(true, checkFileSize(0));
		
		//PERMANENT and EXTRACTED AS; as first access
		makeFile(3);
		exA = new Extract("A", null);
		exA.setTitle(FILENAME);
		count = 0; exA.open(f01); while(exA.fetch()) count++; exA.close();
		assertEquals(3, count);
		assertEquals(true, checkFileExists());
		assertEquals(true, checkFileSize(3));
		
		deleteFile();
		assertEquals(false, checkFileExists());

		//Ensure that settitle create a new, empty file, if not present.
		exA = new Extract("A", null);
		exA.setTitle(FILENAME);
		assertEquals(true, checkFileExists());
		assertEquals(true, checkFileSize(0));

		deleteFile();
		exA = new Extract("A", null);
		exA.setTitle(FILENAME, true);
		assertEquals(true, checkFileExists());
		assertEquals(true, checkFileSize(0));

		deleteFile();
	}
	
	public void testExtracts() throws ClassNotFoundException, IOException {
		Extract exA;
		deleteFile();
		int count;
		
		//Test extracting to new temp file
		exA = new Extract("A", null);
		runExtract(exA, 5);
		exA.sort(new FrameVar [] {f01.NUM});
		count = 0; exA.open(); while(exA.fetch()) count++; exA.close();
		assertEquals(5, count);
		assertEquals(false, checkFileExists());
		
		//Test on non-existing PERMANENT file
		deleteFile();
		exA = new Extract("A", null);
		exA.setTitle(FILENAME);
		runExtract(exA, 5);
		count = 0; exA.open(); while(exA.fetch()) count++; exA.close();
		assertEquals(5, count);
		assertEquals(true, checkFileExists());

		//Test on pre-existing PERMANENT file (truncate's)
		exA = new Extract("A", null);
		exA.setTitle(FILENAME);
		runExtract(exA, 5);
		count = 0; exA.open(); while(exA.fetch()) count++; exA.close();
		assertEquals(5, count);
		assertEquals(true, checkFileExists());

		//Test on pre-existing PERMANENT file (truncate's)
		exA = new Extract("A", null);
		exA.setTitle(FILENAME);
		count = 0; exA.open(); while(exA.fetch()) count++; exA.close(); //first access should truncate
		runExtract(exA, 5);
		count = 0; exA.open(); while(exA.fetch()) count++; exA.close();
		assertEquals(5, count);
		assertEquals(true, checkFileExists());

		//Test on pre-existing PERMANENT file (append's)
		exA = new Extract("A", null);
		exA.setTitle(FILENAME);
		count = 0; exA.open(f01); while(exA.fetch()) count++; exA.close(); //first access should truncate
		runExtract(exA, 7);
		count = 0; exA.open(); while(exA.fetch()) count++; exA.close();
		assertEquals(12, count);
		assertEquals(true, checkFileExists());

		//Test on non-existing EXISTING file
		deleteFile();
		exA = new Extract("A", null);
		exA.setTitle(FILENAME, true);
		runExtract(exA, 5);
		count = 0; exA.open(); while(exA.fetch()) count++; exA.close();
		assertEquals(5, count);
		assertEquals(true, checkFileExists());

		//Test on pre-existing EXISTING file
		exA = new Extract("A", null);
		exA.setTitle(FILENAME, true);
		runExtract(exA, 5);
		count = 0; exA.open(); while(exA.fetch()) count++; exA.close();
		assertEquals(5, count);
		assertEquals(true, checkFileExists());
		
		//Test for appending of EXISTING file
		exA = new Extract("A", null);
		exA.setTitle(FILENAME, true);
		count = 0; exA.open(); while(exA.fetch()) count++; exA.close();
		assertEquals(5, count);
		runExtract(exA, 7);
		count = 0; exA.open(); while(exA.fetch()) count++; exA.close();
		assertEquals(12, count);
		assertEquals(true, checkFileExists());
		
	}
	
}

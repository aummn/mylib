package com.mssint.jclib;

//import com.mssint.jclibtest.ApplicationClass;

import junit.framework.TestCase;

public class FilenameTest extends TestCase  {

	public FilenameTest() throws Exception {}
	
	public void test_1() throws ClassNotFoundException {
	    Report rep = new Report();
	    Report repA = new Report("A");
	    Report.setReportName("FileNameTest");
	 
	    assertEquals("FileNameTest", rep.newFileName());
	    assertEquals("FileNameTest.A", repA.newFileName());
	    System.setProperty("jclib.PRINT", "MyFile");
	    System.setProperty("jclib.PRINTA", "MyBigFile");
	    assertEquals("MyFile", rep.newFileName());
	    assertEquals("MyBigFile", repA.newFileName());
	}
	
	public void test_2() throws ClassNotFoundException {
		Extract exA = new Extract("Apple");
		Extract exB = new Extract("B", "Afile");
		Extract exC = new Extract("C", "");
	    Report.setReportName("FileNameTest");
		
		assertEquals("Apple", exA.newFileName());
		assertEquals("Afile", exB.newFileName());
		assertEquals("FileNameTest.C", exC.newFileName());
		System.setProperty("jclib.EXTRACTB", "GoodFellow");
		System.setProperty("jclib.EXTRACTC", "BadFellow");
	}
	
	public void test_3() throws ClassNotFoundException {
		Var XFILNAME = new Var(Var.CHAR, 40);
		Extract exX = new Extract("X",  "X");
		XFILNAME.set(new Var("CHRS_HRDATA"));
		exX.setTitle(XFILNAME.getString());
		
		exX.open();
		exX.close();
	}
}
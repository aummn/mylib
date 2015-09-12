package com.mssint.jclib;

import junit.framework.TestCase;

public class FrameTest extends TestCase {

	public class Frame01 extends FrameState {
		public FrameVar t001 = new FrameVar(1,20,"PAGE HEADERS           PAGE ");
		public FrameVar page = new FrameVar(1,50, "unumeric, zerofill", 3);
		public FrameVar t002 = new FrameVar(2,20, "===============================");
	}

	public void test_1() throws Exception {
		Frame01 f01 = new Frame01();
		assertEquals("PAGE HEADERS           PAGE ", f01.t001.getString());
		assertEquals("000", f01.page.getString());
		f01.page.set(f01.page.getInt() + 1);
		assertEquals("001", f01.page.getString());
		assertEquals("PAGE HEADERS           PAGE 001===============================", f01.getExtractString());
		assertEquals("                   PAGE HEADERS           PAGE   001", f01.getPrintString(1));
		assertEquals("                   ===============================", f01.getPrintString(2));
	}
}

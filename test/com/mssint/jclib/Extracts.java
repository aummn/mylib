package com.mssint.jclib;

import java.io.FileNotFoundException;
import java.io.IOException;

import junit.framework.TestCase;

public class Extracts extends TestCase {
	public Extracts() throws ClassNotFoundException {	}
	
	//Sample cursor
	public class cCtfin extends CursorState {
		private static final long serialVersionUID = -7290090920244930474L;
		public Var VAL1             = new Var(Var.NUMERIC,12,2);
		public Var VAL2             = new Var(Var.NUMERIC,12,2);
		public Var NUM1             = new Var(Var.NUMERIC,10);
		public Var TXT10           	= new Var(Var.CHAR,10);
		public Var TXT20          	= new Var(Var.CHAR,20);
	}
	public cCtfin ctfin = new cCtfin();

	Extract exA = new Extract("X", null);
	Extract exB = new Extract("X", null);

	//Frame01 references some database variables
	public class Frame01 extends FrameState {
		public FrameVar NUM = new FrameVar(1, 1, "unumber",3);
		public FrameVar CTFIN_NUM1 = new FrameVar(1, 10, ctfin.NUM1);
		public FrameVar CTFIN_VAL1 = new FrameVar(1, 10, ctfin.VAL1);
		public FrameVar VAL1 = new FrameVar(1, 1, "number",12,2);
		public FrameVar CTFIN_VAL2 = new FrameVar(1, 10, ctfin.VAL2);
		public FrameVar CTFIN_TXT20 = new FrameVar(1, 10, ctfin.TXT20);
		public FrameVar TXT = new FrameVar(1, 1, "char",10);
	}
	Frame01 f01 = new Frame01();
	
	//Frame02 references frame01 variables
	public class Frame02 extends FrameState {
		public FrameVar F01_NUM = new FrameVar(1,1, exA, f01.NUM);
		public FrameVar CTFIN_NUM1 = new FrameVar(1, 10, exA, f01.CTFIN_NUM1);
		public FrameVar CTFIN_VAL1 = new FrameVar(1,1,exA, f01.CTFIN_VAL1);
		public FrameVar F01_VAL1 = new FrameVar(1,1,exA, f01.VAL1);
		public FrameVar CTFIN_TXT20 = new FrameVar(1, 10, exA, f01.CTFIN_TXT20);
		public FrameVar F01_TXT = new FrameVar(1, 1, exA, f01.TXT);
		public FrameVar CTFIN_VAL2 = new FrameVar(1, 10, exA, f01.CTFIN_VAL2);
	}
	Frame02 f02 = new Frame02();
	
	
	private String FILENAME = "TESTFILE";
    public Var SN_DO014_PC = new Var(Var.UNUMERIC,9);// PZ

	//This function MUST be first - sets up config.
	public void testExtractFile() throws ClassNotFoundException, IOException {
		//Initialise all
		try {
			@SuppressWarnings("unused")
			Config config = new Config();
		} catch (FileNotFoundException e) {}
		
		Config.setProperty("jclib.extract.dir", "/tmp/");
		Config.setProperty("jclib.report.dir", "/tmp/");
		Config.setProperty("jclib.default.filepath", "/tmp/");
		f01.init();
		f02.init();
		SN_DO014_PC.set("HELLO");
        Config.log.info(SN_DO014_PC.getMessageString() + " > " + "ADDRESS RECORDS WITHOUT POSTAL CODE");

	
		//Create the initial extract file (partially from database)
		Extract ex = new Extract("X", null);
		ex.setTitle(FILENAME + "_1");
		for(int i = 0; i < 10; i++) {
			ctfin.NUM1.set(i*11);
			ctfin.VAL1.set((double)i * 37.0 / 3.8);
			ctfin.VAL2.set((((double)i * 72) + 34827) / 2.178);
			ctfin.TXT10.set("REC-"+i);
			ctfin.TXT20.set("RNUMBER-"+(i*24));
			f01.NUM.set(i*11);
			f01.TXT.set(ctfin.TXT10);
			ex.print(f01);
		}
		int count = 0; ex.open(); while(ex.fetch()) count++ ; ex.close();
		assertEquals(10, count);
		ex.sort(new FrameVar [] {f01.NUM.descending()});
		boolean p1 = false;
		boolean p2 = false;
		boolean cmp = true;
		//Now make a second extract from the first
		exA.setTitle(FILENAME + "_1", true);
		exB.setTitle(FILENAME + "_2");
		exA.open();
		count = 10;
		while(exA.fetch()) {
			count--;
			//Check order of records
			assertEquals(count * 11, exA.getVar(f01.NUM).getInt());
			if(p1) {
				System.out.print("["+exA.getVar(f01.NUM) + "]");
				System.out.print("["+exA.getVar(f01.CTFIN_NUM1) + "]");
				System.out.print("["+exA.getVar(f01.CTFIN_VAL1) + "]");
				System.out.print("["+exA.getVar(f01.CTFIN_TXT20) + "]");
				System.out.print("["+exA.getVar(f01.CTFIN_VAL2) + "]");
				System.out.print("["+exA.getVar(f01.TXT) + "]");
				System.out.print("["+exA.getVar(f01.VAL1) + "]");
				System.out.println();
			}
			if(p2) {
				System.out.print("["+f02.F01_NUM + "]");
				System.out.print("["+f02.CTFIN_NUM1 + "]");
				System.out.print("["+f02.CTFIN_VAL1 + "]");
				System.out.print("["+f02.CTFIN_TXT20 + "]");
				System.out.print("["+f02.CTFIN_VAL2 + "]");
				System.out.print("["+f02.F01_TXT + "]");
				System.out.print("["+f02.F01_VAL1 + "]");
				System.out.println();
			}
			if(cmp) {
				assertEquals(exA.getVar(f01.NUM).getString(),f02.F01_NUM.getString());
				assertEquals(exA.getVar(f01.CTFIN_NUM1).getString(),f02.CTFIN_NUM1.getString());
				assertEquals(exA.getVar(f01.CTFIN_VAL1).getString(),f02.CTFIN_VAL1.getString());
				assertEquals(exA.getVar(f01.CTFIN_TXT20).getString(),f02.CTFIN_TXT20.getString());
				assertEquals(exA.getVar(f01.CTFIN_VAL2).getString(),f02.CTFIN_VAL2.getString());
				assertEquals(exA.getVar(f01.TXT).getString(),f02.F01_TXT.getString());
				assertEquals(exA.getVar(f01.VAL1).getString(),f02.F01_VAL1.getString());
			}
			if(cmp) {
				assertEquals(exA.getVar(f01.NUM).toString(),f02.F01_NUM.toString());
				assertEquals(exA.getVar(f01.CTFIN_NUM1).toString(),f02.CTFIN_NUM1.toString());
				assertEquals(exA.getVar(f01.CTFIN_VAL1).toString(),f02.CTFIN_VAL1.toString());
				assertEquals(exA.getVar(f01.CTFIN_TXT20).toString(),f02.CTFIN_TXT20.toString());
				assertEquals(exA.getVar(f01.CTFIN_VAL2).toString(),f02.CTFIN_VAL2.toString());
				assertEquals(exA.getVar(f01.TXT).toString(),f02.F01_TXT.toString());
				assertEquals(exA.getVar(f01.VAL1).toString(),f02.F01_VAL1.toString());
			}
			if(cmp) {
				assertEquals(exA.getVar(f01.NUM).getInt(),f02.F01_NUM.getInt());
				assertEquals(exA.getVar(f01.CTFIN_NUM1).getInt(),f02.CTFIN_NUM1.getInt());
				assertEquals(exA.getVar(f01.CTFIN_VAL1).getInt(),f02.CTFIN_VAL1.getInt());
				assertEquals(exA.getVar(f01.CTFIN_TXT20).getInt(),f02.CTFIN_TXT20.getInt());
				assertEquals(exA.getVar(f01.CTFIN_VAL2).getInt(),f02.CTFIN_VAL2.getInt());
				assertEquals(exA.getVar(f01.TXT).getInt(),f02.F01_TXT.getInt());
				assertEquals(exA.getVar(f01.VAL1).getInt(),f02.F01_VAL1.getInt());
			}
			if(cmp) {
				assertEquals(exA.getVar(f01.NUM).getLong(),f02.F01_NUM.getLong());
				assertEquals(exA.getVar(f01.CTFIN_NUM1).getLong(),f02.CTFIN_NUM1.getLong());
				assertEquals(exA.getVar(f01.CTFIN_VAL1).getLong(),f02.CTFIN_VAL1.getLong());
				assertEquals(exA.getVar(f01.CTFIN_TXT20).getLong(),f02.CTFIN_TXT20.getLong());
				assertEquals(exA.getVar(f01.CTFIN_VAL2).getLong(),f02.CTFIN_VAL2.getLong());
				assertEquals(exA.getVar(f01.TXT).getLong(),f02.F01_TXT.getLong());
				assertEquals(exA.getVar(f01.VAL1).getLong(),f02.F01_VAL1.getLong());
			}
			if(cmp) {
				assertEquals(exA.getVar(f01.NUM).getDouble(),f02.F01_NUM.getDouble());
				assertEquals(exA.getVar(f01.CTFIN_NUM1).getDouble(),f02.CTFIN_NUM1.getDouble());
				assertEquals(exA.getVar(f01.CTFIN_VAL1).getDouble(),f02.CTFIN_VAL1.getDouble());
				assertEquals(exA.getVar(f01.CTFIN_TXT20).getDouble(),f02.CTFIN_TXT20.getDouble());
				assertEquals(exA.getVar(f01.CTFIN_VAL2).getDouble(),f02.CTFIN_VAL2.getDouble());
				assertEquals(exA.getVar(f01.TXT).getDouble(),f02.F01_TXT.getDouble());
				assertEquals(exA.getVar(f01.VAL1).getDouble(),f02.F01_VAL1.getDouble());
			}
			if(cmp) {
				assertEquals(exA.getVar(f01.NUM).getLincString(),f02.F01_NUM.getExtractString());
				assertEquals(exA.getVar(f01.CTFIN_NUM1).getLincString(),f02.CTFIN_NUM1.getExtractString());
				assertEquals(exA.getVar(f01.CTFIN_VAL1).getLincString(),f02.CTFIN_VAL1.getExtractString());
				assertEquals(exA.getVar(f01.CTFIN_TXT20).getLincString(),f02.CTFIN_TXT20.getExtractString());
				assertEquals(exA.getVar(f01.CTFIN_VAL2).getLincString(),f02.CTFIN_VAL2.getExtractString());
				assertEquals(exA.getVar(f01.TXT).getLincString(),f02.F01_TXT.getExtractString());
				assertEquals(exA.getVar(f01.VAL1).getLincString(),f02.F01_VAL1.getExtractString());
			}
			if(cmp) {
				assertEquals(exA.getVar(f01.NUM).getString(),f02.F01_NUM.getString());
				assertEquals(exA.getVar(f01.CTFIN_NUM1).getString(),f02.CTFIN_NUM1.getString());
				assertEquals(exA.getVar(f01.CTFIN_VAL1).getString(),f02.CTFIN_VAL1.getString());
				assertEquals(exA.getVar(f01.CTFIN_TXT20).getString(),f02.CTFIN_TXT20.getString());
				assertEquals(exA.getVar(f01.CTFIN_VAL2).getString(),f02.CTFIN_VAL2.getString());
				assertEquals(exA.getVar(f01.TXT).getString(),f02.F01_TXT.getString());
				assertEquals(exA.getVar(f01.VAL1).getString(),f02.F01_VAL1.getString());
			}
			if(cmp) {
				assertEquals(exA.getVar(f01.NUM).getString(),f02.F01_NUM.getString());
				assertEquals(exA.getVar(f01.CTFIN_NUM1).getString(),f02.CTFIN_NUM1.getString());
				assertEquals(exA.getVar(f01.CTFIN_VAL1).getString(),f02.CTFIN_VAL1.getString());
				assertEquals(exA.getVar(f01.CTFIN_TXT20).getString(),f02.CTFIN_TXT20.getString());
				assertEquals(exA.getVar(f01.CTFIN_VAL2).getString(),f02.CTFIN_VAL2.getString());
				assertEquals(exA.getVar(f01.TXT).getString(),f02.F01_TXT.getString());
				assertEquals(exA.getVar(f01.VAL1).getString(),f02.F01_VAL1.getString());
			}
			if(cmp) {
//				assertEquals(exA.getBuffer(), f02.getExtractString());
			}

			exB.print(f02);
		}
		exA.close();
	}
}

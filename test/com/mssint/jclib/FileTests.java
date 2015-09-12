package com.mssint.jclib;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

public class FileTests extends TestCase {
	public void testRelativeRandom() throws Exception {
		/*
		MssFile pcwork3;
	       Group pcworkRecord3 = new Group(50);

	       Group pcworkId3 = new Group(Var.CHAR, 200);
	       Group wsVariables = new Group();
	       Group pcworkKey3 = wsVariables.addMember(Var.UNUMERIC|Var.COMP,6).set(1);

	       pcwork3 = new MssFile(pcworkId3);
	       pcwork3.setMode("RELATIVE|RANDOM");
	       pcwork3.setIndexVariable(pcworkKey3);
	       pcwork3.setRecordLength(50);

	       pcworkId3.set("/tmp/PCWORKSYS");
	       pcwork3.open("w");
	       pcwork3.close();
	       pcwork3.open("r+");

	       pcworkKey3.set(200);
	       pcworkRecord3.set("xxxxxxx");
	       pcwork3.write(pcworkRecord3);
	       pcworkRecord3.set("");
	       pcworkKey3.set(200);
	       pcwork3.read(pcworkRecord3);
	       System.out.println("pcworkRecord3(200)=" + pcworkRecord3.getString());
	       pcworkKey3.set(1);
	       pcwork3.read(pcworkRecord3);
	       System.out.println("pcworkRecord3(1)=" + pcworkRecord3.getString());
	       */
	}
	
	public void testRelative() throws Exception {
		Group fileName = new Group(Var.CHAR, 120);
		Group index = new Group(Var.UNUMERIC, 5);
		
		Group record = new Group();
		Group indexNum = record.addMember(Var.UNUMERIC, 5);
		Group name = record.addMember(Var.CHAR, 30);
		Group age = record.addMember(Var.UNUMERIC, 3);
		Group score = record.addMember(Var.NUMBER, 6, 2);
		Group filler = record.addMember(Var.CHAR, 5);
		
		MssFile file = new MssFile(fileName);
		fileName.set("tmp/relative");
		
		//Ensure directory exists
		File f = new File("tmp");
		f.mkdirs();
		//Make sure file is deleted.
		f = new File(fileName.toString());
		f.delete();
		
		
		file.setMode("RELATIVE|RANDOM");
		file.setIndexVariable(index);
		file.setRecordLength(record.size());
		
		//At this point the file does not exist. Start by writing the 10th
		//record - the previous 9 should then be created but marked as deleted.
		file.open("w"); //create the file
		file.close();
		file.open("r+");
		
		index.set(5);
		indexNum.set(index);
		name.set("Steve Rainbird");
		age.set(51);
		score.set(12.76);
		file.write(record);
		
		//Verify the file size
		int size = file.size();
		assertEquals(5 * (record.size() + 1), size);
		
		//Write a second record, as the 10th.
		index.set(10);
		indexNum.set(index);
		name.set("Peter Colman");
		age.set(51);
		score.set(20.36);
		file.write(record);
		
		//Verify the file size
		size = file.size();
		assertEquals(10 * (record.size() + 1), size);
		
		//Read the 3th record and verify not existent
		index.set(3);
		boolean status = file.read(record);
		assertEquals(false, status);
		
		index.set(5);
		status = file.read(record);
		assertEquals(true, status);
		assertEquals(5, index.getInt());
		assertEquals("Steve Rainbird", name.toString());
		assertEquals(12.76, score.getDouble());
		
		//Write record number 7
		index.set(7);
		indexNum.set(index);
		name.set("Nick Wnekowski");
		age.set(61);
		score.set(15.21);
		file.write(record);
		
		//Verify record 10
		index.set(10);
		status = file.read(record);
		assertEquals(true, status);
		assertEquals(10, index.getInt());
		assertEquals("Peter Colman", name.toString());
		assertEquals(20.36, score.getDouble());

		//Write record number 1
		index.set(1);
		indexNum.set(1);
		name.set("Maurice Shnaps");
		age.set(89);
		score.set(11.77);
		file.write(record);

		//Verify record 7
		index.set(7);
		status = file.read(record);
		assertEquals(true, status);
		assertEquals(7, index.getInt());
		assertEquals("Nick Wnekowski", name.toString());
		assertEquals(15.21, score.getDouble());

		//Verify record 1
		index.set(1);
		status = file.read(record);
		assertEquals(true, status);
		assertEquals(1, index.getInt());
		assertEquals("Maurice Shnaps", name.toString());
		assertEquals(11.77, score.getDouble());
		
		//Delete record 7
		index.set(7);
		file.deleteRecord();
		
		//Verify delete
		status = file.read(record);
		assertEquals(false, status);
		
		//Change mode to SEQUENTIAL
		file.setMode("SEQUENTIAL");
		
		//Add a new record
		name.set("Bill Pope");
		indexNum.set(11);
		age.set(58);
		score.set(5.21);
		filler.set("XXXXX");
		file.write(record);
		
		assertEquals(11 * (record.size() + 1), file.size());
		
		//Read the records sequentially
		index.set(1);
		file.start(null);

		status = file.read(record);
		assertEquals(true, status);
		assertEquals(1, indexNum.getInt());
		status = file.read(record);
		assertEquals(true, status);
		assertEquals(5, indexNum.getInt());
		status = file.read(record);
		assertEquals(true, status);
		assertEquals(10, indexNum.getInt());
		status = file.read(record);
		assertEquals(true, status);
		assertEquals(11, indexNum.getInt());
		status = file.read(record);
		assertEquals(false, status);

		index.set(5);
		file.start(null);
		String s = file.read();
		assertEquals(true, s.startsWith("00005"));
		file.deleteRecord();
		
		index.set(5);
		file.start(null);
		s = file.read();
		assertEquals(true, s.startsWith("00010"));

		status = file.read(record);
		assertEquals(true, status);
		assertEquals(11, indexNum.getInt());
		status = file.read(record);
		assertEquals(false, status);
		
		file.close();
		
		index.set(10);
		file.setMode(MssFile.RANDOM|MssFile.RELATIVE);
		file.open("r");
		s = file.read();
		assertEquals(true, s.startsWith("00010"));
	}
	
	public void testRelative2() throws Exception {
		//File Descriptors
		MssFile testfileS = null;
		MssFile testfileR = null;

		//File layout groups
		Group testRec = new Group(20);
		Group testRecR = new Group(20);

		//Variable Declarations
		Group testfileSKey = new Group(Var.UNUMERIC, 9).set(0);
		Group testfileRKey = new Group(Var.UNUMERIC, 9).set(0);

		//----- Group FILE-STATUS: Defined in testfile.cbl line 26
		Group fileStatus = new Group();
		Group fileStatus1 = fileStatus.addMember(Var.UNUMERIC, 1);
		Group fileStatus2 = fileStatus.addMember(Var.UNUMERIC|Var.COMP, 2);

		if (testfileS == null) {
			testfileS = new MssFile("tmp/scrap");
			testfileS.setMode("RELATIVE");
			testfileS.setIndexVariable(testfileSKey);
			testfileS.setRecordLength(Util.getMax(testRec.size()));
		}
		if (testfileR == null) {
			testfileR = new MssFile("tmp/scrap");
			testfileR.setMode("RELATIVE|RANDOM");
			testfileR.setIndexVariable(testfileRKey);
			testfileR.setRecordLength(Util.getMax(testRecR.size()));
			testfileR.setFileStatusVar(fileStatus);
		}
		testfileS.open("w");
		testRec.fill("1");
		testfileS.write(testRec.getBytes());
		testRec.fill("2");
		testfileS.write(testRec.getBytes());
		testRec.fill("3");
		testfileS.write(testRec.getBytes());
		testRec.fill("4");
		testfileS.write(testRec.getBytes());
		testRec.fill("5");
		testfileS.write(testRec.getBytes());
		testRec.fill("6");
		testfileS.write(testRec.getBytes());
		testRec.fill("7");
		testfileS.write(testRec.getBytes());
		testfileS.close();
		testfileS.open("r");
		testfileS.read(testRec);
		assertEquals("11111111111111111111", testRec.getString());
		Config.log.info(testfileSKey.getString());
		assertEquals("000000001", testfileSKey.getString());
		testfileS.read(testRec);
		assertEquals("22222222222222222222", testRec.getString());
		Config.log.info(testfileSKey.getString());
		assertEquals("000000002", testfileSKey.getString());
		testfileSKey.set(4);
		testfileS.start("START TESTFILE-S ");
		testfileS.read(testRec);
		assertEquals("44444444444444444444", testRec.getString());
		Config.log.info(testfileSKey.getString());
		assertEquals("000000004", testfileSKey.getString());
		testfileS.close();
		testfileS.open("r+");
		testfileS.read(testRec);
		assertEquals("11111111111111111111", testRec.getString());
		Config.log.info(testfileSKey.getString());
		assertEquals("000000001", testfileSKey.getString());
		testfileS.read(testRec);
		assertEquals("22222222222222222222", testRec.getString());
		Config.log.info(testfileSKey.getString());
		assertEquals("000000002", testfileSKey.getString());
		testfileS.deleteRecord();
		testfileSKey.set(1);
		testfileS.start("START TESTFILE-S ");
		testfileS.read(testRec);
		assertEquals("11111111111111111111", testRec.getString());
		Config.log.info(testfileSKey.getString());
		assertEquals("000000001", testfileSKey.getString());
		testfileS.read(testRec);
		assertEquals("33333333333333333333", testRec.getString());
		Config.log.info(testfileSKey.getString());
		assertEquals("000000003", testfileSKey.getString());
		testfileS.close();
		testfileR.open("r+");
		testfileRKey.set(1);
		testfileR.read(testRecR);
		assertEquals("11111111111111111111", testRecR.getString());
		Config.log.info(fileStatus.getString());
		assertEquals("00", fileStatus.getString());
		testfileRKey.set(2);
		testfileR.read(testRecR);
		assertEquals("11111111111111111111", testRecR.getString());
		Config.log.info(fileStatus.getString());
		assertEquals("23", fileStatus.getString());
		testRecR.fill("8");
		testfileR.write(testRecR.getBytes());
		testfileRKey.set(2);
		testfileR.read(testRecR);
		assertEquals("88888888888888888888", testRecR.getString());
		testfileRKey.set(2);
		testfileR.read(testRecR);
		testfileR.deleteRecord();
		testfileRKey.set(4);
		testfileR.read(testRecR);
		testRecR.fill("9");
		testfileR.reWrite(testRecR.getBytes());
		testfileR.close();
		testfileS.open("r");
		testfileS.read(testRec);
		assertEquals("11111111111111111111", testRec.getString());
		Config.log.info(testfileSKey.getString());
		assertEquals("000000001", testfileSKey.getString());
		testfileS.read(testRec);
		assertEquals("33333333333333333333", testRec.getString());
		Config.log.info(testfileSKey.getString());
		assertEquals("000000003", testfileSKey.getString());
		testfileS.read(testRec);
		assertEquals("99999999999999999999", testRec.getString());
		Config.log.info(testfileSKey.getString());
		assertEquals("000000004", testfileSKey.getString());
		testfileS.read(testRec);
		assertEquals("55555555555555555555", testRec.getString());
		Config.log.info(testfileSKey.getString());
		assertEquals("000000005", testfileSKey.getString());
		testfileS.read(testRec);
		assertEquals("66666666666666666666", testRec.getString());
		Config.log.info(testfileSKey.getString());
		assertEquals("000000006", testfileSKey.getString());
		testfileS.read(testRec);
		assertEquals("77777777777777777777", testRec.getString());
		Config.log.info(testfileSKey.getString());
		assertEquals("000000007", testfileSKey.getString());
		testfileS.close();
	}
}

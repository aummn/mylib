package com.mssint.jclib;

import java.text.DecimalFormat;

//import com.mssint.jclib.Group;
//import com.mssint.jclib.Var;
//import com.mssint.jclib.*;








import java.util.ArrayList;
import java.util.List;

import com.mssint.jclib.Group.GroupMode;

import junit.framework.TestCase;

public class GroupTest extends TestCase {
	
//	GroupByteArray  = new GroupByteArray(3884);
	
	@SuppressWarnings("unused")
	public void testOccursGroup() throws Exception {
		Group svRunFlatRec = new Group().occurs(10);
			Group svMastLineNo = svRunFlatRec.addMember(Var.UNUMERIC, 4);
			Group svRunRunNo = svRunFlatRec.addMember(Var.CHAR, 16);
			Group svRunQty = svRunFlatRec.addMember(Var.NUMERIC, 9, 2);
		
		svMastLineNo.index(1).set(1234);
		svMastLineNo.index(5).set(4231);
		svRunRunNo.index(1).set("abcdefghijklmnop");
		svRunRunNo.index(5).set("ABCDEFGHIHJKLMNOP");
		svRunQty.index(1).set("12345.67");
		svRunQty.index(5).set("76543.21");
		
		String s1 = svRunFlatRec.getHexString();
		assertEquals("1234abcdefghijklmnop001234567", svRunFlatRec.index(1).getString());
		assertEquals("4231ABCDEFGHIHJKLMNO007654321", svRunFlatRec.index(5).getString());
		String s2 = svRunFlatRec.getHexString();
		assertEquals(s1, s2);
		
		Group wsMisc = new Group();
    	Group wsPrices = wsMisc.addMember(Var.UNUMERIC, 12, 5).format("Z(5)9.9(5)").set(0).occurs(15);
        
        Group wsPasswd_20 = new Group(Var.CHAR, 20);
        	Group wsPasswd_20_x = wsPasswd_20.redefine(Var.CHAR, 1).occurs(20);
        	Group wsPasswd_20_n = wsPasswd_20.redefine(Var.UNUMERIC|Var.COMP, 2).occurs(20);
        	Group hexWork = new Group(Var.UNUMERIC|Var.COMP, 4);


        wsPasswd_20_n.index(20).set(1);
        wsPasswd_20_n.index(15).set(27);
        wsPasswd_20_n.index(8).set(76);
        assertEquals(1, wsPasswd_20_n.index(20).getInt());
        assertEquals(27, wsPasswd_20_n.index(15).getInt());
        assertEquals(76, wsPasswd_20_n.index(8).getInt());

		Group tech21 = new Group();
			Group wsProgName = tech21.addMember(Var.CHAR, 75); 
			Group wsProgChar = wsProgName.redefine();
				Group wsPChar = wsProgChar.addMember(Var.CHAR, 1).occurs(75);

		wsProgName.set("ABCDE");
		
        assertEquals("ABCDE", wsProgName.toString());
        assertEquals("A", wsPChar.index(1).getString());
        assertEquals("B", wsPChar.index(2).getString());
        assertEquals("C", wsPChar.index(3).getString());
        assertEquals("D", wsPChar.index(4).getString());
        assertEquals("E", wsPChar.index(5).getString());
        assertEquals(" ", wsPChar.index(6).getString());
        
        
		
		Group gA = new Group();
    	Group fA = gA.addMember(Var.UNUMERIC, 12, 5).format("Z(5)9.9(5)");
		Group gB = new Group();
    	Group fB = gB.addMember(Var.UNUMERIC, 12, 5).format("Z(5)9.9(5)");
    	
    	//Verify that the picture is used on copy
    	assertEquals("000000000000", fA.getString());
    	assertEquals("000000000000", fB.getString());
        fA.set(fB);
    	assertEquals("     0.00000", fA.getString());
    	assertEquals("000000000000", fB.getString());
        fB.set(fA);
    	assertEquals("     0.00000", fA.getString());
    	assertEquals("     0.00000", fB.getString());

	}
	
	public void testOccurs() throws Exception {
	    int coPmIx = 0;

	    //----- Group CO-PM-TABLE: Defined in source/PX241.cbl line 203
	    Group coPmTable = new Group();
	        //   THIS TABLE RE-INITIALIZED PER EMPLOYEE, NOT USED FOR ENTIRE
	        //    CHECK RUN.  THIS AS COMPROMISE BETWEEN ELIMINATION OF THE TAB
	        //    AND INABILITY TO SIZE LARGE ENOUGH INTERNALLY TO HANDLE FULL
	        //    COMPANY RUN DUE TO LIMITS IN @MOVEB.     (RWK  2-27-87)
	        //    'CO' PREFIX KEPT TO ELIMINATE EXTENSIVE CODE CHANGES
	        //        CONTAINS USED PAY MODIFIERS, INITIALIZED AS NEEDED, WHEN
	        //        FULL MOVE DIRECTLY TO CO-PM-TEMP NOT UPDATED
	        Group coPmLen = coPmTable.addMember(Var.UNUMERIC|Var.COMP, 4).set(148);
	        Group coPmTemp = coPmTable.addGroup();
	            Group coPmKey = coPmTemp.addGroup();
	                Group coPmUnion = coPmKey.addMember(Var.UNUMERIC|Var.COMP, 5);
	                Group coPmType = coPmKey.addMember(Var.CHAR, 1);
	                //                A-PAY, B-BEN, D-DED, X-PIECE RATE
	                Group coPmId = coPmKey.addMember(Var.CHAR, 4);
	            Group coPmStat = coPmTemp.addMember(Var.CHAR, 1);
	            Group coPmPer = coPmTemp.addMember(Var.CHAR, 1);
	            //            " " - ALLOW DEFINITION AT EMPLOYEE LEVEL
	            //              Y - TAKE THIS PERIOD
	            //              N - DON'T TAKE THIS PERIOD
	            Group coPmCalc = coPmTemp.addMember(Var.CHAR, 1);
	            //            A - FLAT
	            //            P - PERCENT OF EARNINGS
	            //            R - HOURS WORKED,
	            //            H - HOURS PAID
	            Group coPmAmtPct = coPmTemp.addMember(Var.UNUMERIC|Var.COMP, 9, 4);
	            Group coPmRate = coPmAmtPct.redefine().addMember(Var.UNUMERIC|Var.COMP, 9, 6);
	            Group coPmLimit = coPmTemp.addMember(Var.UNUMERIC|Var.COMP, 9, 2);
	            Group coPmExGlDept = coPmTemp.addMember(Var.CHAR, 1);
	            Group coPmInfo = coPmTemp.addGroup();
	                Group coPmFed = coPmInfo.addMember(Var.CHAR, 1);
	                Group coPmFica = coPmInfo.addMember(Var.CHAR, 1);
	                Group coPmUn = coPmInfo.addMember(Var.CHAR, 1);
	                Group coPm_401K = coPmInfo.addMember(Var.CHAR, 1);
	                Group coPmDirBurden = coPmInfo.addMember(Var.CHAR, 1);
	                //                    N-NOT DIRECT, Y-DIRECT
	                Group coPmStatesIt = coPmInfo.addGroup();
	                    Group coPmIt = coPmStatesIt.addMember(Var.CHAR, 1).occurs(60);
	                //                    Y - TAXABLE, N - NOT TAXABLE
	                //                    S - STATE TAXABLE, C - CITY TAXABLE
	                Group coPmStatesUnemp = coPmInfo.addGroup();
	                    Group coPmUnemp = coPmStatesUnemp.addMember(Var.CHAR, 1).occurs(60);
	            //                    Y - TAXABLE, N - NOT TAXABLE
	            Group coPmIncWc = coPmTemp.addMember(Var.CHAR, 1);
	            Group coPmIncCalc = coPmTemp.addMember(Var.CHAR, 1);
	            Group coPmGross = coPmTemp.addMember(Var.CHAR, 1);
	        Group coPmTabArea = coPmTable.addGroup();
	            Group coPmTab = coPmTabArea.addGroup().occurs(25);
	                Group coPmTabKey = coPmTab.addMember(Var.CHAR, 8);
	                Group filler_044 = coPmTab.addMember(Var.CHAR, 140);
	    

		
        for(coPmIx=1;coPmIx <= 25;coPmIx++) {
            if(coPmTabKey.index(coPmIx).eq(coPmKey)) {
                coPmTemp.set(coPmTab.index(coPmIx));
                break;
            }
            if(coPmIx == 25) {
                break;
            }
        }

	}
	
	public void testSomeOtherStuff() throws Exception {
		Group SG_DATE = new Group();
			Group SD_YEAR = SG_DATE.addMember(Var.UNUMERIC, 4);
			Group SD_MD = SG_DATE.addMember(Var.UNUMERIC, 4);
		Var PHDATE8                 = new Var(Var.NUMERIC,8);
		
		SD_YEAR.set("2015");
		assertEquals("20150000", SG_DATE.getString());
		SG_DATE.set("2015");
		assertEquals("2015    ", SG_DATE.getString());
		
		PHDATE8.set(SG_DATE);
		assertEquals("20150000", PHDATE8.getString());
		assertEquals(20150000, PHDATE8.getLong());

		Group SG_HOLDKEY2 = new Group();
			Group SD_KEY2 = SG_HOLDKEY2.addMember(Var.UNUMERIC, 8);
		SD_KEY2.set(SG_DATE);

		assertEquals(20150000, SD_KEY2.getLong());
		assertEquals("20150000", SD_KEY2.getString());
	}
	
	public void testGroupArrayInitialise() throws Exception {
		Group.setGroupMode(GroupMode.LINC);
		Group wmrTermRcv = new Group();
        Group secModule = wmrTermRcv.addGroup().occurs(40);
        Group secFunction = secModule.addMember(Var.UNUMERIC|Var.COMP, 1).occurs(30);
        Group tdrNo = wmrTermRcv.addMember(Var.UNUMERIC, 2).bwz();
        Group tdxNo = tdrNo.redefine().addMember(Var.CHAR, 2);
        Group tdrBankName = wmrTermRcv.addMember(Var.CHAR, 30);
        Group tdrTenderType = wmrTermRcv.addMember(Var.UNUMERIC, 2).occurs(15);
        Group tdxTenderType = tdrTenderType.redefine().addMember(Var.CHAR, 2).occurs(15);
        Group tdrTypeDesc = wmrTermRcv.addMember(Var.CHAR, 20).occurs(15);
        Group tdrGlNo = wmrTermRcv.addMember(Var.UNUMERIC, 16).format("Z(9).ZZZZZZ").bwz().occurs(15);
        Group tdxGlNo = tdrGlNo.redefine().addMember(Var.CHAR, 16).occurs(15);
        Group tdrDept = wmrTermRcv.addMember(Var.CHAR, 1).occurs(15);
        Group tdrCreditCard = wmrTermRcv.addMember(Var.CHAR, 1).occurs(15);
        Group tdrGlTitle = wmrTermRcv.addMember(Var.CHAR, 32).occurs(15);
        Group tdrCcBox = wmrTermRcv.addMember(Var.CHAR, 1);
 
        secFunction.index(1,1).set(7);
        secFunction.index(40,30).set(9);
        tdrTenderType.index(1).set(11);
        tdrTenderType.index(2).set(22);
        tdrTenderType.index(15).set(22);
        
		String s1 = wmrTermRcv.getHexString();


        assertEquals("7", secFunction.index(1,1).getString());
        assertEquals("9", secFunction.index(40,30).getString());
        assertEquals("11", tdrTenderType.index(1).getString());
        assertEquals("22", tdrTenderType.index(2).getString());
        assertEquals("22", tdrTenderType.index(15).toString());
        
        assertEquals(
        		"070000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000009303020202020202020202020202020202020202020202020202020202020202031313232303030303030303030303030303030303030303030303030323220202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020",
        		wmrTermRcv.getHexString());
		String s2 = wmrTermRcv.getHexString();
		assertEquals(s1, s2);

        wmrTermRcv.initialise();
        assertEquals("0", secFunction.index(1,1).getString());
        assertEquals("0", secFunction.index(40,30).getString());
        assertEquals("00", tdrTenderType.index(1).getString());
        assertEquals("00", tdrTenderType.index(2).getString());
        assertEquals("00", tdrTenderType.index(15).getString());
        assertEquals(
        		"000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000202020202020202020202020202020202020202020202020202020202020202030303030303030303030303030303030303030303030303030303030303020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020",
        		wmrTermRcv.getHexString());

		Group.setGroupMode(GroupMode.UNISYS);
        wmrTermRcv.initialise();
        System.out.println("["+wmrTermRcv.getHexString()+"]");
//        assertEquals(
//        		"000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000202020202020202020202020202020202020202020202020202020202020202030303030303030303030303030303030303030303030303030303030303020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020202020",
//        		wmrTermRcv.getHexString());


        Group gx1 = new Group ();
        Group gx2 = gx1.addMember(Var.UNUMERIC, 8).format("Z(9)9");
        Group x = new Group(Var.CHAR,8);

        x.set(gx2);
        gx1.set("");
        assertEquals(0, x.toString().length());
        x.set(gx2);
        assertEquals(0, x.toString().length());
        gx1.set("xxxxxxxx");
        x.set(gx2);
        assertEquals("xxxxxxxx", x.toString());
        gx1.set("00000000");
        x.set(gx2);
        assertEquals("00000000", x.toString());
	}
	
	
	public void testGroupObjects() {
		Group xM = new Group();
			Group text1 = xM.addMember(Var.CHAR, 30);
			Group num1 = xM.addMember(Var.NUMBER, 10);
			Group xSub = xM.addGroup();
				Group type = xSub.addMember(Var.CHAR, 1);
				Group typeName = xSub.addMember(Var.CHAR, 15);
			Group enc = xM.addMember(Var.CHAR, 12);
			
		class gM_c extends Group {
			public Group text1 = this.addMember(Var.CHAR, 30);
			public Group num1 = this.addMember(Var.NUMBER, 10);
			class gSub_c extends Group {
				public gSub_c(gM_c gM_i) {
					super((Group)gM_i);
				}
				public Group type = this.addMember(Var.CHAR, 1);
				public Group typeName = this.addMember(Var.CHAR, 15);
			}
			public gSub_c gSub = new gSub_c(this);
			
			public Group enc = this.addMember(Var.CHAR, 12);
		}
		gM_c gM = new gM_c();
		
		
		
		
		
		
		gM.text1.set("This is the name");
		gM.num1.set(123456);
		gM.gSub.type.set("N");
		gM.gSub.typeName.set("No Params");
		gM.enc.set("ENCLOSE");
	
		xM.set(gM);
		System.out.println("typeName="+typeName);
	}


	
	
	public class CheckMyGroup {
		Group fileDescriptor = new Group();
        Group fileName = fileDescriptor.addMember(Var.CHAR, 30);
        Group fileHandle = fileDescriptor.addMember(Var.CHAR, 2);
        //    03  FILE-HANDLE-CONV        PIC 9999 COMP.
        Group fileHandleConv = fileDescriptor.addMember(Var.CHAR, 2);
        Group fileUr = fileDescriptor.addMember(Var.UNUMERIC|Var.COMP, 8);
        Group fileRecSize = fileDescriptor.addMember(Var.UNUMERIC|Var.COMP, 4);
        Group fileNoIndicies = fileDescriptor.addMember(Var.UNUMERIC|Var.COMP, 4);
        Group fileUpdPos = fileDescriptor.addMember(Var.UNUMERIC|Var.COMP, 4);
        Group fileLastUpd = fileDescriptor.addMember(Var.UNUMERIC|Var.COMP, 2);
        Group indexBlkDup2 = fileDescriptor.addGroup();
            //        05  FILLER OCCURS 10.
            Group filler011 = indexBlkDup2.addGroup().occurs(20);
                Group fKeyOffs = filler011.addMember(Var.UNUMERIC|Var.COMP, 4);
                Group fKeySz = filler011.addMember(Var.UNUMERIC|Var.COMP, 4);
                Group fKeyTyp = filler011.addMember(Var.UNUMERIC|Var.COMP, 4);
                Group fAsc = filler011.addMember(Var.CHAR, 2);
                Group fNulls = filler011.addMember(Var.CHAR, 2);
                Group fDups = filler011.addMember(Var.CHAR, 2);
                Group filler012 = filler011.addMember(Var.CHAR, 8);
       
        public void set(Group x) {
        	fileDescriptor.set(x);
        	System.out.println("fileRecSize=" + fileRecSize.getString());
        }
 
	}
	
	public void testAGroup() throws Exception {
		CheckMyGroup check = new CheckMyGroup();
		Group icwhseDescriptor = new Group();
        Group fileNameDup38 = icwhseDescriptor.addMember(Var.CHAR, 30).set("ICWHSE");
        Group fileHandleDup38 = icwhseDescriptor.addMember(Var.CHAR, 2).setHex("FFFF");
        Group fileHandleConvDup38 = icwhseDescriptor.addMember(Var.CHAR, 2).setHex("FFFF");
        Group fileUrDup38 = icwhseDescriptor.addMember(Var.UNUMERIC|Var.COMP, 8);
        Group fileRecSizeDup38 = icwhseDescriptor.addMember(Var.UNUMERIC|Var.COMP, 4).set(200);
        Group fileNoIndiciesDup38 = icwhseDescriptor.addMember(Var.UNUMERIC|Var.COMP, 4).set(1);
        Group fileUpdPosDup38 = icwhseDescriptor.addMember(Var.UNUMERIC|Var.COMP, 4).set(0);
        Group fileLastUpdDup38 = icwhseDescriptor.addMember(Var.UNUMERIC|Var.COMP, 2);
        Group indexBlkDup38 = icwhseDescriptor.addGroup();
            Group iKeyOffs1_dup38 = indexBlkDup38.addMember(Var.UNUMERIC|Var.COMP, 4).set(0);
            Group iKeySz1_dup38 = indexBlkDup38.addMember(Var.UNUMERIC|Var.COMP, 4).set(43);
            Group iKeyTyp1_dup38 = indexBlkDup38.addMember(Var.UNUMERIC|Var.COMP, 4).set(1);
            Group iAsc1_dup38 = indexBlkDup38.addMember(Var.CHAR, 2).setHex("FFFF");
            Group iNulls1_dup38 = indexBlkDup38.addMember(Var.CHAR, 2).setHex("FFFF");
            Group iDups1_dup38 = indexBlkDup38.addMember(Var.CHAR, 2).set("LOW-VALUES");
            Group filler060 = indexBlkDup38.addMember(Var.CHAR, 8);

       System.out.println("a:fileRecSizeDup38=" + fileRecSizeDup38);
       check.set(icwhseDescriptor);
       System.out.println("b:fileRecSizeDup38=" + fileRecSizeDup38);
       
	}
	
    public void testStringGrouping() throws Exception {
    	Group.setGroupMode(GroupMode.LINC);
       Group gm = new Group();
        Group ga = gm.addMember(Var.CHAR, 9);
        Group gb = gm.addMember(Var.CHAR, 5);
        Group gc = gm.addMember(Var.CHAR, 2);
        Group gd = gm.addMember(Var.CHAR, 4);
        Group ge = gm.addMember(Var.CHAR, 30);

        // too long
        ga.set("abcdefghijklmnopqrstuvxyz");
        assertEquals("abcdefghi                                         ", gm.getString());
        // too short
        gb.set("ABC");
        assertEquals("abcdefghiABC                                      ", gm.getString());
        // empty
        gc.set("");
        // right size
        gd.set("xyzw");

        assertEquals("abcdefghiABC    xyzw                              ", gm.getString());
        assertEquals("abcdefghi", ga.getString());
        assertEquals("ABC  ", gb.getString());
        assertEquals("  ", gc.getString());
        assertEquals("xyzw", gd.getString());

        ga.set(3);
        assertEquals("3        ", ga.getString());
        ga.set(-3);
        assertEquals("-3       ", ga.getString());
        ge.set(123456789123L);
        assertEquals("123456789123                  ", ge.getString());
        ge.set(-123456789123L);
        assertEquals("-123456789123                 ", ge.getString());
    }
    
    @SuppressWarnings("unused")
	public void testNumericGrouping() throws Exception {

    	Group gm = new Group();
    	Group ga = gm.addMember(Var.NUMERIC, 5);
    	Group gb = gm.addMember(Var.NUMERIC, 10, 4);
    	Group gc = gm.addMember(Var.NUMERIC, 2);
    	Group gd = gm.addMember(Var.NUMERIC, 4);
    	
    	Var v1 = new Var(Var.NUMBER, 10, 4);
    	Var v2 = new Var(Var.NUMBER, 10);

    	// too short
    	gb.set(-12.3);
    	// too long
    	ga.set(-1243234.3541234);
    	// empty
    	gc.set(0);
    	// right size
    	gd.set(1234);

    	// pqrstuvwxy
    	// 0123456789
    	// 4323t 000012300p 00 1234
    	assertEquals("4323t000012300p001234", gm.getString());
    	assertEquals("-43234", ga.getString());
//    	assertEquals("000012300p", gb.getString());
    	assertEquals(-12.3000, gb.getDouble());
    	assertEquals("00", gc.getString());
    	assertEquals("1234", gd.getString());
    	//abcde fghijklmno pq rstu vxyz0123456789
    	gm.set("abcdefghijklmnopqrstuvxyz0123456789");
    	assertEquals("abcdefghijklmnopqrstu", gm.getString());
    	assertEquals("abcde", ga.getString());
//    	assertEquals("fghijklmno", gb.getString());
    	assertEquals("pq", gc.getString());
    	assertEquals("rstu", gd.getString());

    	Group n = new Group();
    	Group n10 = n.addMember(Var.NUMBER, 10);
        n10.set(123);
        //System.out.println("n10="+n10.getString(false));
        //System.out.println("n10="+n10.getString(true));
        
        assertEquals("0000000123", n10.toString());
        assertEquals("0000000123", n10.getString());
        assertEquals("0000000123", n10.getString(true));
        assertEquals("123", n10.getString(false));
 
        n10.set(0);
        assertEquals("0000000000", n10.toString());
        assertEquals("0000000000", n10.getString());
        assertEquals("0000000000", n10.getString(true));
        assertEquals("0", n10.getString(false));
        
        gd.set(123.4567);
        assertEquals(123.0, gd.getDouble());
        gb.set(123.4567);
        assertEquals(123.4567, gb.getDouble());
        
    }
    //junit.framework.AssertionFailedError: expected:<0000000123> but was:<0000000123>


    public void testLength() throws Exception {
    	Group gm = new Group();
    		/*Group ga =*/ gm.addMember(Var.CHAR, 5);
    		/*Group gb =*/ gm.addMember(Var.NUMERIC, 10);
    		Group gs = gm.addGroup();
    			/*Group gsa =*/ gs.addMember(Var.CHAR, 10);
    			/*Group gsn =*/ gs.addMember(Var.NUMERIC, 10);
    		Group gr = gm.addGroup();
    			/*Group gra =*/ gr.addMember(Var.NUMERIC, 10);
    			/*Group grn =*/ gr.addMember(Var.NUMERIC, 10);
    		/*Group gt =*/ gm.addMember(Var.NUMERIC, 5);
    		

    	Group gz = new Group(132);
    	
        assertEquals(132, gz.getString().length());
        assertEquals(60, gm.length());
        assertEquals(20, gs.length());
        assertEquals(20, gr.length());
    }
    
    public void testSubgroups() throws Exception {

        Group gm = new Group();
            Group ga = gm.addMember(Var.CHAR, 5);
            Group gs = gm.addGroup();
                Group gsa = gs.addMember(Var.CHAR, 6);
                Group gsn = gs.addMember(Var.NUMERIC, 6);
            Group gr = gm.addGroup();
                Group gra = gr.addMember(Var.CHAR, 6);
                Group grn = gr.addMember(Var.NUMERIC, 6);
        Group gt = gm.addMember(Var.CHAR, 5);

        gm.set("abcdefghijklmnopqrstuvwxyzABCDEFGH");

        assertEquals(34, gm.getString().length());
        assertEquals("abcdefghijklmnopqrstuvwxyzABCDEFGH", gm.getString());
//        assertEquals("abcde", ga.getString());
        assertEquals("fghijklmnopq", gs.getString());
        assertEquals("fghijk", gsa.getString());
        assertEquals("lmnopq", gsn.getString());
        assertEquals("rstuvwxyzABC", gr.getString());
        assertEquals("rstuvw", gra.getString());
        assertEquals("xyzABC", grn.getString());
        assertEquals("DEFGH", gt.getString());

    }

    @SuppressWarnings("unused")
	public void testPeterPrepared() throws Exception {
    	Group.setGroupMode(GroupMode.LINC);

        Group gm = new Group();
        Group ga = gm.addMember(Var.CHAR, 10);
        Group gn = gm.addMember(Var.NUMERIC, 8);
        Group gd = gm.addMember(Var.NUMERIC, 8, 2);
        Group gs = gm.addGroup();
        /*Group gsa =*/ gs.addMember(Var.CHAR, 6);
        Group gsn = gs.addMember(Var.UNUMERIC, 6);
        /*Group gf =*/ gm.addMember(Var.CHAR, 2);

        Var n = new Var(Var.NUMERIC, 10);
        /*
         * Notes: gm should be a 40 characters in length. The sub-group gs is 12
         * characters. ga is a member of the group gm. gsa is a member of the
         * group gs, which is a member of gm.
         */

                    //aaaaaaaaaa9999999911111111aaaaaa999999xx
        assertEquals("          0000000000000000      000000  ", gm.getString());

        gn.set(-21);
        assertEquals("          0000002q00000000      000000  ", gm.getString());
        assertEquals("-00000021", gn.getString());

        ga.set("abcdefghihjlmnopqrstuvwxyz");
        assertEquals("abcdefghih0000002q00000000      000000  ", gm.getString());
        assertEquals("abcdefghih", ga.getString());

        gd.set(-123456789.1234);
        assertEquals("abcdefghih0000002q4567891r      000000  ", gm.getString());
//        assertEquals("4567891r", gd.getString());

        gs.set("abcdefghihjlmnopqrstuvwxyz");
        assertEquals("abcdefghih0000002q4567891rabcdefghihjl  ", gm.getString());
        assertEquals("abcdefghihjl", gs.getString());
        assertEquals("ghihjl", gsn.getString());

        //n.set(gsn);
        //Why??assertEquals("0000789800", n.getString());

        gsn.set(-123456789);
        assertEquals("abcdefghih0000002q4567891rabcdef456789  ", gm.getString());
        assertEquals("abcdef456789", gs.getString());
        assertEquals("456789", gsn.getString());
    }
    
    public void testGroupNumbers()  throws Exception {
        Group g = new Group();
        Group ga = g.addMember(Var.CHAR, 10);
        Group gb = g.addMember(Var.NUMERIC, 8);
        Group gc = g.addMember(Var.NUMERIC, 8, 2);
        Group gs = g.addGroup();
        /*Group ge =*/ gs.addMember(Var.UNUMERIC, 8);
        Group gf = gs.addMember(Var.UNUMERIC, 8, 2);
        
        g.set("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOP");
        assertEquals(0.0, ga.getDouble());
        assertEquals(-12, gb.getInt());
        assertEquals(-1234567890000007L, gs.getLong());
  //           ..........++++++++........++++++++........
        g.set("  -1234.2700939852012515121253652{2532698S");
//        assertEquals(-1234.27, ga.getDouble());
        assertEquals(939852, gb.getInt());
        //assertEquals("12515.12", gc.getDouble());
        assertEquals(12515, gc.getLong());

        
        // Test assignments from differing datatypes
        g.set(" ");
        gb.set("12-123.459");
        gc.set("12-123.459");
        gf.set("12-123.459");
        System.out.println("Hey - g=["+g.getString()+"]");
        assertEquals("-00000123", gb.getString());
       //TODO assertEquals("0001234u", gc.getString());
       //TODO  assertEquals("00012345", gf.getString());

        gb.set("-123.459");
        gc.set("-123.459");
        gf.set("-123.459");
//        assertEquals("0000012s", gb.getString());
 //       assertEquals("0001234u", gc.getString());
 //       assertEquals("00012345", gf.getString());
 //       gc.set("-123.459");
 //       assertEquals("0001234u", gc.getString());
        assertEquals("          0000012s0001234u        00012345", g.getString());

        assertEquals(-123, gb.getInt());
        assertEquals(-123, gb.getLong());
        assertEquals(-123.0, gb.getDouble());

        assertEquals(-123, gc.getInt());
        assertEquals(-123, gc.getLong());
        assertEquals(-123.45, gc.getDouble());

        assertEquals(123, gf.getInt());
        assertEquals(123, gf.getLong());
        assertEquals(123.45, gf.getDouble());

        gb.set("123.45y");
        gc.set("123.45y");
        gf.set("123.45y");
  //      assertEquals("0000012s", gb.getString());
   //     assertEquals("0001234u", gc.getString());
  //      assertEquals("00012345", gf.getString());

  //      gb.set("1234u");
  //      gc.set("1234u");
  //      gf.set("1234u");
  //      assertEquals("0001234u", gb.getString());
  //      assertEquals("0001234u", gc.getString());
  //      assertEquals("00012345", gf.getString());

        Var v = new Var(Var.NUMERIC,8,2);
        v.set(-123.459);
        gb.set(v);
        gb.set(v);
        gc.set(v);
        gf.set(v);

        Group.setGroupMode(GroupMode.MICROFOCUS);
        Group miscWs = new Group();
        Group convDisc = miscWs.addMember(Var.NUMERIC, 10, 4);
        Group  cplineDiscAmt = miscWs.addMember(Var.NUMERIC|Var.COMP, 11, 5);

        cplineDiscAmt.set(convDisc);
        System.out.println("convDisc=" + convDisc.getString());
        System.out.println("cplineDiscAmt=" + cplineDiscAmt.getString());
        
    }
    public void testPeterPreparedForSubgrouping() throws Exception {

    	Group.setGroupMode(GroupMode.LINC);
       Group gm = new Group();
        /*Group ga =*/ gm.addMember(Var.CHAR, 10);
        Group gn = gm.addMember(Var.NUMERIC, 8);
        Group gd = gm.addMember(Var.NUMERIC, 8, 2);
        Group gs1 = gm.addGroup();
        /*Group gs1a =*/ gs1.addMember(Var.CHAR, 6);
        Group gs1n = gs1.addMember(Var.UNUMERIC, 6);
        Group gs2 = gm.addGroup();
        /*Group gs2a =*/ gs2.addMember(Var.CHAR, 6);
        /*Group gs2n =*/ gs2.addMember(Var.UNUMERIC, 6);
        Group gs2_sub = gs2.addGroup();
        Group gs2_sub1 = gs2_sub.addMember(Var.CHAR, 1);
        Group gs2_sub2 = gs2_sub.addMember(Var.CHAR, 10);
        Group gs2_sub3 = gs2_sub.addMember(Var.CHAR, 1);
        gs2_sub3.set("N");

        // Initial value
        assertEquals(
                "          0000000000000000      000000      000000           N",
                gm.getString());
        assertEquals("           N", gs2_sub.getString());

        gs1.set("abcdefghihjlmnopqrstuvwxyz");
        assertEquals("abcdefghihjl", gs1.getString());

        gs2_sub1.set("N");
        gs2_sub2.set("----------------");
        assertEquals(
                "          0000000000000000abcdefghihjl      000000N----------N",
                gm.getString());
        assertEquals("N----------N", gs2_sub.getString());

        gs2_sub2.set("-");
        assertEquals("N-         N", gs2_sub.getString());

        gn.set(-21);
        assertEquals("-00000021", gn.getString());

        gd.set(-123456789.1234);
//        assertEquals("4567891r", gd.getString());

        gd.set(1);
//        assertEquals("00000100", gd.getString());

        gs1n.set(-123456789);
        assertEquals("abcdef456789", gs1.getString());
        assertEquals("456789", gs1n.getString());
        assertEquals("          0000002q00000100abcdef456789      000000N-         N", gm.getString());

        gm.set("A");
        assertEquals("A                                                             ", gm.getString());

    }
    
	@SuppressWarnings("unused")
    public void testGlb() throws Exception {
		Group.setGroupMode(GroupMode.LINC);
    	Glb GLB = new Glb();
        Group SG_OMSGF = new Group();
        Group SD_REGCEN = SG_OMSGF.addMember(Var.UNUMERIC, 2);
        Group SD_FLETYP = SG_OMSGF.addMember(Var.UNUMERIC, 1);
        Group SD_SERNBR = SG_OMSGF.addMember(Var.UNUMERIC, 2);
        Group SD_LSTRNR = SG_OMSGF.addMember(Var.UNUMERIC, 7);
        Group SD_RELVL = SG_OMSGF.addMember(Var.UNUMERIC, 7);
        Group SD_OMSRELIN = SG_OMSGF.addMember(Var.UNUMERIC, 4);
        
        int len = GLB.SPACES.size();
        StringBuilder spaces = new StringBuilder(len);
        for(int i=0; i<len; i++) spaces.append(' ');
        assertEquals(spaces.toString(), GLB.SPACES.getString());
        
        assertEquals(false,SG_OMSGF.eq(GLB.SPACES));
        assertEquals(true,SG_OMSGF.eq(GLB.ZEROS));
        assertEquals("00000000000000000000000", SG_OMSGF.getString());
        SG_OMSGF.set("01101000001000350000070");
        assertEquals(false,SG_OMSGF.eq(GLB.SPACES));
        assertEquals(false,SG_OMSGF.eq(GLB.ZEROS));
    }
    
    
    public void testAdd() throws Exception {
        Var S_LINE_CNT = new Var(Var.UNUMERIC,1);
        Var ZEROS = new Var(Var.NUMERIC, 1); // numeric(0)
        ZEROS.set(0);
        S_LINE_CNT.set(ZEROS);
        S_LINE_CNT = S_LINE_CNT.add(1);
        assertEquals(1,S_LINE_CNT.getInt());
        S_LINE_CNT = S_LINE_CNT.add(1);
        assertEquals(2,S_LINE_CNT.getInt());
    }

    public void testEq() throws Exception {
        Group GS_P01_A = new Group();
        /*Group GS_P01_MEN_A =*/ GS_P01_A.addMember(Var.CHAR, 5);
        /*Group GS_P01_OPT_N =*/ GS_P01_A.addMember(Var.UNUMERIC, 2);
        /*Group GS_P01_CN_N =*/ GS_P01_A.addMember(Var.UNUMERIC, 8);
        /*Group GS_P01_GSD_N =*/ GS_P01_A.addMember(Var.UNUMERIC, 5);
        /*Group GS_P01_GAC_N =*/ GS_P01_A.addMember(Var.UNUMERIC, 2);
        /*Group GS_P01_GAP_N =*/ GS_P01_A.addMember(Var.UNUMERIC, 6);
        Group GS_P01_MESS_A = GS_P01_A.addMember(Var.CHAR, 50);

        Var SPACES = new Var(Var.CHAR, 2028);
        SPACES.set(" ");
        GS_P01_A.set(SPACES);
        try  {
            GS_P01_MESS_A.ne(SPACES);
        } catch(Exception ex)  {
            fail(ex.getMessage());
        }
    }
    public void testDates() throws Exception {
        //Initialisation
        Glb GLB = new Glb();
        GLB.initAll();

        Group GS = new Group();
        /*Group GSD =*/ GS.addMember(Var.CHAR, 2);
        /*Group GSM =*/ GS.addMember(Var.CHAR, 3);
        /*Group GSY =*/ GS.addMember(Var.UNUMERIC, 2);
        Var VS = new Var(Var.CHAR,7);

        Group GA = new Group();
        Group GAD = GA.addMember(Var.NUMERIC, 2);
        Group GAM = GA.addMember(Var.CHAR, 2);
        Group GAY = GA.addMember(Var.UNUMERIC, 2);



        //========== International
        GLB.intl = DC.I;
        GS.set("93OCT01");
        GLB.BASE.set(1011957);
        GS.toDayNumber(GLB);
        assertEquals(13422, GLB.TOTAL.getInt());
        assertEquals("     ", GLB.STATUS.getString());
        VS.set(GS);

        GS.set("01OCT99");
        GS.toDayNumber(GLB);
        assertEquals("*****", GLB.STATUS.getString());

        VS.toDayNumber(GLB);
        assertEquals(13422, GLB.TOTAL.getInt());
        assertEquals("     ", GLB.STATUS.getString());

        //========== UK Dates
        GLB.intl = DC.UK;
        GS.set("01OCT93");
        GS.toDayNumber(GLB);
        assertEquals(13422, GLB.TOTAL.getInt());
        assertEquals("     ", GLB.STATUS.getString());
        VS.set(GS);

        GS.set("93OCT01");
        GS.toDayNumber(GLB);
        assertEquals("*****", GLB.STATUS.getString());

        VS.toDayNumber(GLB);
        assertEquals(13422, GLB.TOTAL.getInt());
        assertEquals("     ", GLB.STATUS.getString());

        //========== US Dates
        GLB.intl = DC.US;
        GAD.set(11); //month
        GAM.set(30); //day
        GAY.set(93);
        GA.toDayNumber(GLB);
        assertEquals(13482, GLB.TOTAL.getInt());
        assertEquals("     ", GLB.STATUS.getString());
        assertEquals(GLB.STATUS.eq(""), true);
        VS.set(GA);

        GS.set("01OCT99");
        GS.toDayNumber(GLB);
        assertEquals("*****", GLB.STATUS.getString());

        VS.toDayNumber(GLB);
        assertEquals(13482, GLB.TOTAL.getInt());
        assertEquals("     ", GLB.STATUS.getString());

        GAD.set(2);
        GA.toDayNumber(GLB);
        assertEquals(0, GLB.TOTAL.getInt());
        assertEquals("*****", GLB.STATUS.getString());
    }
    
    public void testForSteve() throws Exception {
        Group SP_TYD = new Group();
           Group SS_HH = SP_TYD.addMember(Var.UNUMERIC, 2);
           Group SS_MM = SP_TYD.addMember(Var.UNUMERIC, 2);
           /*Group SS_REST =*/ SP_TYD.addMember(Var.UNUMERIC, 4);

        Group GW_WORK = new Group();
          Group GW_TIJD = GW_WORK.addMember(Var.UNUMERIC, 8);

        GW_WORK.set("12232184");
        assertEquals(12232184, GW_TIJD.getInt());
        assertEquals("12232184", GW_TIJD.getString());

        SP_TYD.set(GW_TIJD);
        assertEquals("12232184", SP_TYD.getString());
        assertEquals(12, SS_HH.getInt());
        assertEquals(23, SS_MM.getInt());

        Var GRDE2_ID1 = new Var(Var.CHAR,8);
        Group GS_DMY_2_A = new Group();
            Group GS_DMY_2_D_N = GS_DMY_2_A.addMember(Var.UNUMERIC, 2);
            /*Group GS_DMY_2_M_N =*/ GS_DMY_2_A.addMember(Var.UNUMERIC, 2);
            /*Group GS_DMY_2_Y_N =*/ GS_DMY_2_A.addMember(Var.UNUMERIC, 2);

        GS_DMY_2_A.set("151293");
        GRDE2_ID1.set(GS_DMY_2_D_N);

        assertEquals("15      ",GRDE2_ID1.getString());

        Group GS_GRDRS_A = new Group();
         Group GS_GRDRS_1_A = GS_GRDRS_A.addMember(Var.CHAR, 59);
         Group GS_GRDRS_2_A = GS_GRDRS_A.addMember(Var.CHAR, 54);

        Group GS_GRD01_1_A = new Group();
          /*Group GS_GRD01_1_D_N =*/ GS_GRD01_1_A.addMember(Var.UNUMERIC, 5);
          /*Group GS_GRD01_1_TSS_N =*/ GS_GRD01_1_A.addMember(Var.NUMERIC, 12, 2);
          /*Group GS_GRD01_1_PAT_N =*/ GS_GRD01_1_A.addMember(Var.NUMERIC, 10, 2);
          /*Group GS_GRD01_1_ZSS_N =*/ GS_GRD01_1_A.addMember(Var.NUMERIC, 12, 2);
          /*Group GS_GRD01_1_OAT_N =*/ GS_GRD01_1_A.addMember(Var.NUMERIC, 10, 2);
          /*Group GS_GRD01_1_DAT_N =*/ GS_GRD01_1_A.addMember(Var.NUMERIC, 10, 2);
          /*Group GS_GRD01_1_TSR_N =*/ GS_GRD01_1_A.addMember(Var.NUMERIC, 12, 2);
          Group GS_GRD01_1_RAT_N = GS_GRD01_1_A.addMember(Var.NUMERIC, 10, 2);
          /*Group GS_GRD01_1_POT_N =*/ GS_GRD01_1_A.addMember(Var.NUMERIC, 11, 2);
          /*Group GS_GRD01_1_WST_N =*/ GS_GRD01_1_A.addMember(Var.NUMERIC, 10, 2);
          /*Group GS_GRD01_1_POTC_N =*/ GS_GRD01_1_A.addMember(Var.NUMERIC, 11, 2);

        GS_GRDRS_1_A.set("1348200618282979E000000000{00000000000{000236957I000000000{");
        GS_GRDRS_2_A.set("00458028729C013955552H0004087432I000000000{0004087432H");

        GS_GRD01_1_A.set(GS_GRDRS_A);

        String fmt = "0000000.00";

        DecimalFormat df = new DecimalFormat(fmt);
        String str = df.format(GS_GRD01_1_RAT_N.getDouble());

        assertEquals("1395555.28",str);
        
        Group.setGroupMode(GroupMode.MICROFOCUS);
        Group pcworkRecord4 = new Group(3);
        Group pcworkKey4 = pcworkRecord4.redefine().addMember(Var.UNUMERIC|Var.COMP, 6);
        
        pcworkRecord4.setHex("000000000000000000000000");
        
        assertEquals("000000", pcworkKey4.getHexString());
        assertEquals(true, pcworkKey4.eqHex("000000"));
    }

    public void testLargeNumber() throws Exception {
        Var aNumber = new Var(Var.NUMERIC, 18);
        Group GS_TLK_4_A = new Group();
         /*Group GS_TLK_4_TOTC_N =*/ GS_TLK_4_A.addMember(Var.UNUMERIC, 1);
         /*Group GS_TLK_4_TITC_N =*/ GS_TLK_4_A.addMember(Var.UNUMERIC, 1);
         /*Group GS_TLK_4_OSUN_N =*/ GS_TLK_4_A.addMember(Var.UNUMERIC, 4);
         /*Group GS_TLK_4_BNO_N =*/ GS_TLK_4_A.addMember(Var.UNUMERIC, 4);
         /*Group GS_TLK_4_YMD_N =*/ GS_TLK_4_A.addMember(Var.UNUMERIC, 6);
         Group GS_TLK_4_RRN_N = GS_TLK_4_A.addMember(Var.UNUMERIC, 2);

            aNumber.set(128311328893042931L);
            GS_TLK_4_A.set(aNumber);

            assertEquals(31,GS_TLK_4_RRN_N.getInt());
            
            
   		Group bodyField = new Group();
   			/*Group bodyTag =*/ bodyField.addMember(Var.UNUMERIC, 2);
   			/*Group filler061 =*/ bodyField.addMember(Var.CHAR, 1);
   			Group bodyRest = bodyField.addMember(Var.CHAR, 300);
    	Group idrorgbrn52_x = new Group();
    		Group idrorgbrn52 = idrorgbrn52_x.addMember(Var.UNUMERIC, 8);
    	Var v = new Var(Var.CHAR, 100).set("81267098213546987852565689");
		bodyField.set("52:77712013");
		idrorgbrn52_x.set(bodyRest);
		assertEquals("77712013", idrorgbrn52_x.getString());
		idrorgbrn52.set(bodyRest);
		assertEquals("00000000", idrorgbrn52.getString());
		idrorgbrn52_x.set(v);
		assertEquals("81267098", idrorgbrn52_x.getString());
		idrorgbrn52.set(v);
		assertEquals("00000000", idrorgbrn52.getString());
		idrorgbrn52_x.set("25976502365645");
		assertEquals("25976502", idrorgbrn52_x.getString());
		idrorgbrn52.set("25976502365645");
		assertEquals("02365645", idrorgbrn52.getString());
     }

    public void test_1() throws Exception {
    	Group sr30_sr40_workArea = new Group();
 	   	Group srDaysInMonthTable = sr30_sr40_workArea.addGroup();
 	  //Group srDaysInMonthTable = new Group();
        /*Group filler005 =*/ srDaysInMonthTable.addMember(Var.UNUMERIC, 2).set(31);
        /*Group filler006 =*/ srDaysInMonthTable.addMember(Var.UNUMERIC, 2).set(28);
        /*Group filler007 =*/ srDaysInMonthTable.addMember(Var.UNUMERIC, 2).set(31);
        /*Group filler008 =*/ srDaysInMonthTable.addMember(Var.UNUMERIC, 2).set(30);
        /*Group filler009 =*/ srDaysInMonthTable.addMember(Var.UNUMERIC, 2).set(31);
        /*Group filler010 =*/ srDaysInMonthTable.addMember(Var.UNUMERIC, 2).set(30);
        /*Group filler011 =*/ srDaysInMonthTable.addMember(Var.UNUMERIC, 2).set(31);
        /*Group filler012 =*/ srDaysInMonthTable.addMember(Var.UNUMERIC, 2).set(31);
        /*Group filler013 =*/ srDaysInMonthTable.addMember(Var.UNUMERIC, 2).set(30);
        /*Group filler014 =*/ srDaysInMonthTable.addMember(Var.UNUMERIC, 2).set(31);
        /*Group filler015 =*/ srDaysInMonthTable.addMember(Var.UNUMERIC, 2).set(30);
        /*Group filler016 =*/ srDaysInMonthTable.addMember(Var.UNUMERIC, 2).set(31);
        Group srDaysTable = srDaysInMonthTable.redefine();
        	Group srDaysInMonth = srDaysTable.addMember(Var.UNUMERIC, 2);
        	srDaysInMonth.occurs(12); 
        	
        String s1 = srDaysInMonth.getHexString();
        String x = srDaysInMonth.index(1).getString();
        String s2 = srDaysInMonth.getHexString();
        System.out.println("s1="+s1);
        System.out.println("s2="+s2);
        assertEquals(s1, s2);
            
            
	    assertEquals("312831303130313130313031", srDaysTable.getString()); 
	    assertEquals("312831303130313130313031", sr30_sr40_workArea.getString()); 
	    assertEquals(31, srDaysInMonth.index(1).getInt());
	    assertEquals(28, srDaysInMonth.index(2).getInt());
	    assertEquals(31, srDaysInMonth.index(3).getInt());
	    assertEquals(30, srDaysInMonth.index(4).getInt());
	    assertEquals(31, srDaysInMonth.index(5).getInt());
	    assertEquals(31, srDaysInMonth.index(12).getInt());
    }
    
    @SuppressWarnings("unused")
	public void test_2() throws Exception {
    	Group.setGroupMode(GroupMode.LINC);
    	Group tableOfCounters = new Group();
    	Group tocData = tableOfCounters.addGroup();
        	Group tocAccWcaseOk = tocData.addMember(Var.UNUMERIC, 9);
        	System.out.println("tocData="+tocData.getString());
        	Group tocBytypestats = tocData.addGroup().occurs(2);
            	Group tocCasesTtl = tocBytypestats.addMember(Var.CHAR, 8).setAll("Agitated");
            	Group tocCasesDel = tocBytypestats.addMember(Var.CHAR, 5).setAll("Chain");
            	Group tocCasesTtlok = tocBytypestats.addMember(Var.CHAR, 7).setAll("Fragile");
            	Group tocCasesByloc = tocBytypestats.addMember(Var.CHAR, 3).setAll("Mac");
            	tocCasesByloc = tocCasesByloc.occurs(3);
            	Group tocOffDel = tocBytypestats.addMember(Var.CHAR, 5).setAll("Sight");
        assertEquals("000000000AgitatedChainFragileMacMacMacSightAgitatedChainFragileMacMacMacSight",
        		tableOfCounters.getString());
        assertEquals("AgitatedChainFragileMacMacMacSight",
        		tocBytypestats.index(1).getString());
 
        tocAccWcaseOk.set(123456789);
        String [] word8 = {"","Acedemic","Zeppelin"};
        String [] word7 = {"","Birdman","Fusspot"};
        String [] word5 = {"","Forty","Morse","Boast","Cocky"};
        String [] word3 = {"","Fit","Peg","Got","Tin","Ran","Tap"};
        
        int i, j;
        for(i=1;i<=2;i++) {
        	tocCasesTtl.index(i).set(word8[i]);
        	tocCasesDel.index(i).set(word5[i]);
        	tocCasesTtlok.index(i).set(word7[i]);
        	for(j=1;j<=3;j++) {
        		tocCasesByloc.index(i,j).set(word3[i*j]);
        	}
        	tocOffDel.index(i).set(word5[i+2]);
        }
//        System.out.println("tocBytypestats[1]="+tocBytypestats.index(1).getString());
        assertEquals("AcedemicFortyBirdmanFitPegGotBoast",
        		tocBytypestats.index(1).getString());
//        System.out.println("tableOfCounters="+tableOfCounters.getString());
        assertEquals("123456789AcedemicFortyBirdmanFitPegGotBoastZeppelinMorseFusspotPegTinTapCocky",
        		tableOfCounters.getString());
    }
    
    public void test_3() throws Exception {
    	Group sr30_sr40_workArea = new Group();
    		Group srWrkDate = sr30_sr40_workArea.addMember(Var.UNUMERIC, 8);
    	Group srWrkDateR = srWrkDate.redefine();
        	Group srWrkYyyy = srWrkDateR.addMember(Var.UNUMERIC, 4);
        	Group srWrkMm = srWrkDateR.addMember(Var.UNUMERIC, 2);
        	Group srWrkDd = srWrkDateR.addMember(Var.UNUMERIC, 2);
        Group srWrkYear = srWrkYyyy.redefine();
    		Group srWrkCC = srWrkYear.addMember(Var.UNUMERIC, 2);
    		Group srWrkYY = srWrkYear.addMember(Var.UNUMERIC, 2);
   
    	Group primary = new Group(100);
    	Group p0 = primary.redefine();
		Group p1 = p0.addMember(Var.CHAR, 20);
		Group p2 = p0.addMember(Var.CHAR, 20);
		Group p3 = p0.addMember(Var.CHAR, 20);
		Group p4 = p0.addMember(Var.CHAR, 20);
		Group p5 = p0.addMember(Var.CHAR, 20);
//		read(primary);
    	primary.set("Hello World".getBytes());
//    	p3.set("A fond farewell");
    		
        srWrkDate.set(20070729);
    		
    	String s1 = Util.formatHex(primary.getBytes());
    	String x = primary.getString();
    	String s2 = Util.formatHex(primary.getBytes());
    	System.out.println("s1="+s1);
    	System.out.println("s2="+s2);
    	assertEquals(s1, s2);

    	assertEquals(8, sr30_sr40_workArea.getString().length());
        assertEquals(20070729,srWrkDate.getInt());
        assertEquals(20070729,sr30_sr40_workArea.getInt());
        assertEquals(7,srWrkMm.getInt());
        assertEquals(29,srWrkDd.getInt());
        assertEquals(2007,srWrkYyyy.getInt());
        assertEquals(20,srWrkCC.getInt());
        assertEquals(7,srWrkYY.getInt());
        
        Group termRcv = new Group();
        Group porFromWhseX = termRcv.addMember(Var.CHAR|Var.EXPORT, 6);
        Group porFromWhse = porFromWhseX.redefine().addMember(Var.UNUMERIC, 6).format("Z(5)9");

        porFromWhseX.set(1);
        

        
        

        System.out.println("porFromWhseX=" + porFromWhseX.getString());
        System.out.println("porFromWhse=" + porFromWhse.getString());
        System.out.println("porFromWhseX=" + porFromWhseX.getString());
    }
    
    public void test_4() throws Exception {
    	Group errorMessages = new Group();
    		 errorMessages.addMember(Var.CHAR, 34)
    			.set("01AGENCY XXXX NOT MTP             ");
    		 errorMessages.addMember(Var.CHAR, 34)
    			.set("02INVALID COURT LOCATION          ");
    		 errorMessages.addMember(Var.CHAR, 34)
    			.set("03COURT LOCATION NOT FOUND        ");

    	Group errorMsgR = errorMessages.redefine();
    		Group errMsgDtls = errorMsgR.addGroup().occurs(3);
    			Group errNo = errMsgDtls.addMember(Var.UNUMERIC, 2);
    			Group errMsg = errMsgDtls.addMember(Var.CHAR, 32);
    	
    	assertEquals(102, errorMessages.getString().length());
    	assertEquals(2, errNo.index(2).getInt());
    	assertEquals("INVALID COURT LOCATION          ", errMsg.index(2).getString());
    	assertEquals("COURT LOCATION NOT FOUND        ", errMsg.index(3).getString());
    }
    public void testforstevetps() throws Exception{
    	Group.setGroupMode(GroupMode.LINC);
        Group SDCHKCASENO = new Group();
    		/*Group SDCHK1 =*/ SDCHKCASENO.addMember(Var.CHAR, 12);
    		Group SDCHK2 = SDCHKCASENO.addMember(Var.UNUMERIC, 8);
    		/*Group SDCHK3 =*/ SDCHKCASENO.addMember(Var.CHAR, 3);
    	SDCHKCASENO.set("            AA091629");
    	assertEquals(11091629,SDCHK2.getLong());
    	Group sr30_sr40_workArea = new Group();
    	    Group srWrkDate = sr30_sr40_workArea.addMember(Var.UNUMERIC, 8);
    	    Group srWrkDateR = srWrkDate.redefine();
    	    Group srWrkYyyy = srWrkDateR.addMember(Var.UNUMERIC, 4);
    	    Group srWrkMm = srWrkDateR.addMember(Var.UNUMERIC, 2);
    	    Group srWrkDd = srWrkDateR.addMember(Var.UNUMERIC, 2);
    	       
    	 srWrkDate.set(20070729);
    	 assertEquals(20070729,srWrkDate.getInt());
    	 assertEquals(2007,srWrkYyyy.getInt());
    	 assertEquals(07,srWrkMm.getInt());
    	 assertEquals(29,srWrkDd.getInt());
    	 
        Group gfield = new Group();
           /*Group fld1 =*/ gfield.addMember(Var.CHAR,10);
    	   Group fld2 = gfield.addMember(Var.CHAR,10);
    	 gfield.set("");
    	 fld2.set("xxxxxxx");
    	 assertEquals("          xxxxxxx   ",gfield.getString());
    	        
        Var yy = new Var(Var.CHAR,20).fill("x");
        assertEquals("xxxxxxxxxxxxxxxxxxxx",yy.getString());
    	        
        Group gfld = new Group();
   	        /*Group fld3 =*/ gfld.addMember(Var.CHAR,10);
   	        /*Group fld4 =*/ gfld.addMember(Var.CHAR,10).fill("x");
   	     assertEquals("          xxxxxxxxxx",gfld.getString());
   	    
         Glb GLB = new Glb();
         Var testDate = new Var(Var.UNUMERIC,8);
         testDate.set(20070101);
         testDate.dateConvert(DC.CCYYMMDD, 0, GLB);
         assertEquals("01/01/07",GLB.DC_DD_MM_YY.getString());
         assertEquals("     ",GLB.STATUS.getString());
         
         testDate.set(20070199);
         testDate.dateConvert(DC.CCYYMMDD, 0, GLB);
//I suspect this depends on what version of linc you are using
//Comment in dateCovert says manual asys todays date is used
         //assertEquals("01/01/07",GLB.DC_DD_MM_YY.getString());
         assertEquals(GLB.TODAY_DDMMYY.getString(),GLB.DC_DDMMYY.getString());
         assertEquals("*****",GLB.STATUS.getString());

         testDate.set(20070123);
         testDate.dateEdit(DC.CCYYMMDD, GLB);
         //assertEquals("01/01/07",GLB.DC_DD_MM_YY.getString());
         assertEquals(GLB.TODAY_DDMMYY.getString(),GLB.DC_DDMMYY.getString());
         assertEquals("     ",GLB.STATUS.getString());
         
         testDate.set(20070144);
         testDate.dateEdit(DC.CCYYMMDD, GLB);
         //assertEquals("01/01/07",GLB.DC_DD_MM_YY.getString());
         assertEquals(GLB.TODAY_DDMMYY.getString(),GLB.DC_DDMMYY.getString());
         assertEquals("*****",GLB.STATUS.getString());
         
         Group srDaysInMonthTable = sr30_sr40_workArea.addGroup();
         /*Group filler005 =*/ srDaysInMonthTable.addMember(Var.UNUMERIC, 2).set(31);
         /*Group filler006 =*/ srDaysInMonthTable.addMember(Var.UNUMERIC, 2).set(0);
         /*Group filler007 =*/ srDaysInMonthTable.addMember(Var.UNUMERIC, 2).set(31);
         /*Group filler008 =*/ srDaysInMonthTable.addMember(Var.UNUMERIC, 2).set(30);
         /*Group filler009 =*/ srDaysInMonthTable.addMember(Var.UNUMERIC, 2).set(31);
         /*Group filler010 =*/ srDaysInMonthTable.addMember(Var.UNUMERIC, 2).set(30);
         /*Group filler011 =*/ srDaysInMonthTable.addMember(Var.UNUMERIC, 2).set(31);
         /*Group filler012 =*/ srDaysInMonthTable.addMember(Var.UNUMERIC, 2).set(31);
         /*Group filler013 =*/ srDaysInMonthTable.addMember(Var.UNUMERIC, 2).set(30);
         /*Group filler014 =*/ srDaysInMonthTable.addMember(Var.UNUMERIC, 2).set(31);
         /*Group filler015 =*/ srDaysInMonthTable.addMember(Var.UNUMERIC, 2).set(30);
         /*Group filler016 =*/ srDaysInMonthTable.addMember(Var.UNUMERIC, 2).set(31);
     Group srDaysTable = srDaysInMonthTable.redefine();
         Group srDaysInMonth = srDaysTable.addMember(Var.UNUMERIC, 2).occurs(12);
         
         assertEquals(31,srDaysInMonth.index(1).getInt());
         assertEquals(0,srDaysInMonth.index(2).getInt());
         assertEquals(30,srDaysInMonth.index(6).getInt());
         assertEquals(30,srDaysInMonth.index(11).getInt());
         assertEquals(31,srDaysInMonth.index(12).getInt());
         
      Group wsDtsUnavail = new Group();
         /*Group wsUnavMrn1 =*/ wsDtsUnavail.addMember(Var.CHAR, 1);
         /*Group wsUnavMrn2 =*/ wsDtsUnavail.addMember(Var.CHAR, 1);
         /*Group wsUnavAft1 =*/ wsDtsUnavail.addMember(Var.CHAR, 1);
         /*Group wsUnavAft2 =*/ wsDtsUnavail.addMember(Var.CHAR, 1);
         /*Group wsUnavEve =*/ wsDtsUnavail.addMember(Var.CHAR, 1);

         wsDtsUnavail.set("ATM P");
         wsDtsUnavail.replaceAll("A", "");
         assertEquals(" TM P",wsDtsUnavail.getString());
         wsDtsUnavail.replaceAll("T", "");
         assertEquals("  M P",wsDtsUnavail.getString());
         wsDtsUnavail.replaceAll("M", "");
         assertEquals("    P",wsDtsUnavail.getString());
         wsDtsUnavail.replaceAll("P", "");
        
      
         Group chrsRcd = new Group();
         	Group cEmplid = chrsRcd.addGroup();
         	/*Group cEmpno =*/ cEmplid.addMember(Var.UNUMERIC, 5);
         	Group cSurname = chrsRcd.addMember(Var.CHAR, 30);
         	/*Group cGivenname =*/ chrsRcd.addMember(Var.CHAR, 30);
     
         cSurname.set("Rainbird    xxxxx");
         long sub = cSurname.countLeading("");
         assertEquals(0,sub);                      
     
         cSurname.set(" Rainbird    xxxxx");
         sub = cSurname.countLeading("");
         //assertEquals(1,sub);
         assertEquals(1,sub);
         Var xxx = new Var(Var.CHAR,20);
         xxx.set("abcdef");
         String ss = xxx.toUpper().getString();
         assertEquals("ABCDEF              ",ss);
         xxx.set(cSurname.toUpper().getString().substring(0, 20));
         assertEquals(" RAINBIRD    XXXXX  ",xxx.getString());
    }
 
    public void test_5() throws Exception {
      Group chrsRcd = new Group();
      	Group cEmplid = chrsRcd.addGroup();
      		/*Group cEmpno =*/ cEmplid.addMember(Var.UNUMERIC, 5);
      	Group cSurname = chrsRcd.addMember(Var.CHAR, 30);
      	/*Group cGivenname =*/ chrsRcd.addMember(Var.CHAR, 30);
      	
      	cSurname.set(" Rainbird    xxxxx");
      	assertEquals(" RAINBIRD    XXXXX            ", cSurname.toUpper().getString());
    }
    
    public void test_6() throws Exception {
    	Group GDGENGRP = new Group();
    		Group GDGENAVDTS = GDGENGRP.addMember(Var.UNUMERIC, 3);
    	GDGENGRP.set(" ");
    	assertEquals("000", GDGENAVDTS.getString());
    }
    
    public void test_7() throws Exception {
    	Group.setGroupMode(GroupMode.LINC);
        Group wsQcReport = new Group();
        	Group wsQcReportDetail = wsQcReport.addGroup();
        		Group wsQcDetail = wsQcReportDetail.addGroup();
        			/*Group filler025 =*/ wsQcDetail.addMember(Var.CHAR, 42).set("");
        			Group wsQcDetMsg = wsQcDetail.addGroup();
        				/*Group wsQcDetDesc =*/ wsQcDetMsg.addMember(Var.CHAR, 36);
        				Group wsQcDetCnt = wsQcDetMsg.addMember(Var.UNUMERIC, 10);
        				//Group xx = wsQcDetCnt.redefine().format("9999/99/99");
        					//Group wsQcDetDate = xx.addMember(Var.CHAR, 10);
        				Group wsQcDetDate = wsQcDetCnt.redefine().addMember(Var.CHAR, 10).format("9999/99/99");
            		/*Group filler026 =*/ wsQcDetail.addMember(Var.CHAR, 44).set("");
        
        Group wsRunTime = new Group(Var.UNUMERIC, 8); 

        assertEquals(132, wsQcReport.getString().length());
        wsQcDetCnt.set(1234567891);
        assertEquals("1234567891", wsQcDetCnt.getString());
        assertEquals("1234567891", wsQcDetDate.getString());
        assertEquals("                                                                              1234567891                                            ", wsQcDetail.getString());
        assertEquals("00000000", wsRunTime.getString());
    }
    
    public void testRedefine() throws Exception {
    	Group shParameter = new Group();
        	Group shParamAction = shParameter.addMember(Var.CHAR, 32)
                 .set("sh /isweb/prog/updateLookups.sh ");
        	Group shParam = shParameter.addMember(Var.CHAR, 12).set("");
        	Group filler_212 = shParameter.addMember(Var.CHAR, 1).set("");
        	Group shParam2 = shParameter.addMember(Var.CHAR, 20).set("");
        	Group shParam3 = shParameter.addMember(Var.CHAR, 2).set(" &");
    
        Group shCall = shParameter.redefine(Var.CHAR, 68);
       
        assertEquals("sh /isweb/prog/updateLookups.sh                                   &",shParameter.getString());
        assertEquals("sh /isweb/prog/updateLookups.sh ",shParamAction.getString());
        assertEquals("sh /isweb/prog/updateLookups.sh                                   & ",shCall.getString());

    }
    
    public void test_8() throws Exception {
    	Group.setGroupMode(GroupMode.LINC);
    	 Group sptPatternTable = new Group();
         	Group sptPtrnTable = sptPatternTable.addGroup().occurs(5);
             	Group wsSptPtrn = sptPtrnTable.addMember(Var.CHAR, 5);
             	Group wsSptPattern = wsSptPtrn.redefine();
                 	Group wsSptPatDay = wsSptPattern.addMember(Var.CHAR, 1).occurs(5);
        
    
        sptPtrnTable.index(1).set("12345");
        wsSptPtrn.index(2).set("23456");
        wsSptPtrn.index(3).set("34567");
        sptPtrnTable.index(5).set("45678");
        
        
        assertEquals("123452345634567     45678", sptPatternTable.getString());
        assertEquals("34567", sptPtrnTable.index(3).getString());
        assertEquals("34567", wsSptPtrn.index(3).getString());
        assertEquals("34567", wsSptPattern.index(3).getString());
        /*
        wsSptPatDay.index(5,3).set("x");
        assertEquals("123452345634567     45x78", sptPatternTable.getString());
        assertEquals("45x78", wsSptPattern.index(5).getString());
        */
        
        Group bmrpt = new Group();
        Group ptotMat = bmrpt.addMember(Var.NUMERIC|Var.COMP, 11, 2).set(230959.83);
        Group tempw = bmrpt.addMember(Var.NUMERIC|Var.COMP, 17, 7).set(61122);

        double d1 = ptotMat.getDouble();
        double d2 = tempw.getDouble();
        d1 = d1 + d2;
        ptotMat.set(d1);
        assertEquals("0000292081.83", ptotMat.getString());
        
        
    }
    
// Same as 8   public void test_9() throws Exception {
//        Group sptPatternTable = new Group();
//        Group sptPtrnTable = sptPatternTable.addGroup().occurs(5);
//            Group wsSptPtrn = sptPtrnTable.addMember(Var.CHAR, 5);
//            Group wsSptPattern = wsSptPtrn.redefine();
//                /*Group wsSptPatDay =*/ wsSptPattern.addMember(Var.CHAR, 1).occurs(5);
//    
//        sptPtrnTable.index(1).set("12345");
//        wsSptPtrn.index(2).set("23456");
//        wsSptPtrn.index(3).set("34567");
//        sptPtrnTable.index(5).set("45678");
//        assertEquals("123452345634567     45678", sptPatternTable.getString());
//        
//        //assertEquals("34567", wsSptPattern.index(3));
//    }
    
//    public void test_10() throws Exception {
//    	Group datesTable = new Group();
//    		Group datesEntry = datesTable.addGroup().occurs(24);
//    			Group datesData = datesEntry.addMember(Var.UNUMERIC, 6);
//    			Group datesDataR = datesData.redefine();
//    				/*Group datesYy =*/ datesDataR.addMember(Var.UNUMERIC, 4);
//    				/*Group datesMm =*/ datesDataR.addMember(Var.UNUMERIC, 2);
//    }
    
    public void test_11() throws Exception {
    	Group wsWorkDate = new Group(Var.UNUMERIC, 8);
    	Group wsWorkDateR = wsWorkDate.redefine();
        	/*Group filler001 =*/ wsWorkDateR.addMember(Var.UNUMERIC, 2);
        	Group wsWorkYymmdd = wsWorkDateR.addMember(Var.UNUMERIC, 6);
        
        wsWorkDate.set(20080101);
        assertEquals(80101,wsWorkYymmdd.getLong());
    }
    
    public void test_12() throws Exception {
    	
        Group reptStats = new Group();
             Group rsEntry = reptStats.addGroup().occurs(17);
                 Group rsCount = rsEntry.addMember(Var.UNUMERIC, 7).format("Z(07)");
                 rsCount.index(1).set(1234);
                 rsCount.index(14).set(234);
                 rsCount.index(17).set(567);

//              assertEquals("0001234", rsCount.index(1).getString());
//              assertEquals("0000234", rsCount.index(14).getString());
//              assertEquals("0000567", rsCount.index(17).getString());
              assertEquals("   1234", rsCount.index(1).getString());
              assertEquals("    234", rsCount.index(14).getString());
              assertEquals("    567", rsCount.index(17).getString());

    	/*
    	
        Group datesTableX = new Group();
        Group datesTable = datesTableX.addGroup();
        Group datesEntry = datesTable.addGroup().occurs(24);
            Group datesData = datesEntry.addMember(Var.UNUMERIC, 6);
            Group datesDataR = datesData.redefine();
                Group datesYy = datesDataR.addMember(Var.UNUMERIC, 4);
                Group datesMm = datesDataR.addMember(Var.UNUMERIC, 2);
        datesYy.index(1).set(1990);
        datesYy.index(3).set(2099);
        datesMm.index(1).set(1);
        datesMm.index(3).set(2);
        
//....--....--....--....--....--....--....--....--....--....--....--....--
        
        assertEquals("199001      209902",datesTable.getString());
    */
    }

    public void test_13() throws Exception {
    Group stwrkBadgeData = new Group();
       Group stwrkDtls = stwrkBadgeData.addGroup().occurs(4);
          /*Group stwrkHst =*/ stwrkDtls.addMember(Var.UNUMERIC, 4);
          /*Group stwrkCnt =*/ stwrkDtls.addMember(Var.UNUMERIC, 4);
          Group stwrkAve = stwrkDtls.addMember(Var.UNUMERIC, 6, 2);
          stwrkBadgeData.set("01420004000710080000000000000006000200006001000002001000");
          
          assertEquals("0007.10", stwrkAve.index(1).getString());
          assertEquals(7.10, stwrkAve.index(1).getDouble());
    }
    
    public void test_14() throws Exception {
    	Glb GLB = new Glb();
    	Group GG_SCDTE = new Group();// Gp-DTETODB
    	Group GD_SCEE = GG_SCDTE.addMember(Var.UNUMERIC, 2);
    	/*Group GD_SCYYMMDD =*/ GG_SCDTE.addMember(Var.UNUMERIC, 6);
    	GG_SCDTE.set(20080814);
    	GLB.CENTURY.set(GD_SCEE); 
    	assertEquals(20, GLB.CENTURY.getInt());
    }
    public void test_15() throws Exception {
    	Group SG_OMSGR = new Group();
    		/*Group SD_PARTYP =*/ SG_OMSGR.addMember(Var.UNUMERIC, 2);
    		Group SD_CLRDAY = SG_OMSGR.addMember(Var.UNUMERIC, 6);
    		/*Group SD_CLRRUN =*/ SG_OMSGR.addMember(Var.UNUMERIC, 3);
    		/*Group SD_FNMOMSGF =*/ SG_OMSGR.addMember(Var.CHAR, 17);
    		/*Group SD_OMSGF =*/ SG_OMSGR.addMember(Var.CHAR, 2277);
    	
    	SD_CLRDAY.set(123456);
    	Var num5 = new Var(Var.NUMBER,5);
    	Var CDAY = new Var(Var.UNUMERIC, 6);
    	CDAY.set(123456);

    	num5.set(SD_CLRDAY);
    	assertEquals(23456, num5.getLong());
    	num5.set(CDAY);
    	assertEquals(23456, num5.getInt());
    	
    	
    	Group xxwTermRcv = new Group();
        Group iqrSocSec = xxwTermRcv.addMember(Var.CHAR, 11).format("XXX/XX/XXXX");
        Group pxmastSocSec = xxwTermRcv.addMember(Var.CHAR, 9).set("123456789");
        Group pxmastSocSec2 = xxwTermRcv.addMember(Var.NUMBER, 9).set(123456789);

        iqrSocSec.set("123456789");
    	assertEquals("123/45/6789", iqrSocSec.getString());
        iqrSocSec.set(123456789);
    	assertEquals("123/45/6789", iqrSocSec.getString());
        iqrSocSec.set(123456789L);
    	assertEquals("123/45/6789", iqrSocSec.getString());
        iqrSocSec.set(123456789.0);
    	assertEquals("123/45/6789", iqrSocSec.getString());
        iqrSocSec.set(pxmastSocSec);
    	assertEquals("123/45/6789", iqrSocSec.getString());
        iqrSocSec.set(pxmastSocSec2);
    	assertEquals("123/45/6789", iqrSocSec.getString());
    }
    
	@SuppressWarnings("unused")
    public void test_16() throws Exception {
        int i;
        Group A = new Group();
        	/*Group A1 =*/ A.addMember(Var.UNUMERIC, 4).set(1234);
        	/*Group A2 =*/ A.addMember(Var.UNUMERIC, 2).set(89);
        	Group B = A.addGroup().occurs(5);
        		Group B1 = B.addMember(Var.UNUMERIC, 4);
                B1.index(1).set(11);
                B1.index(2).set(22);
                B1.index(3).set(33);
                B1.index(4).set(44);
                B1.index(5).set(55);
        		Group B2 = B.addMember(Var.CHAR, 4);
        	
        B1.index(1).set(11);
        B2.index(1).set("aaAA");
        B1.index(2).set(22);
        B2.index(2).set("bbBB");
        B1.index(3).set(33);
        B2.index(3).set("ccCC");
        B1.index(4).set(44);
        B2.index(4).set("ddDD");
        B1.index(5).set(55);
        B2.index(5).set("eeEE");
        
        System.out.println("A="+ A.getString());
        
        assertEquals("1234890011aaAA0022bbBB0033ccCC0044ddDD0055eeEE", A.getString());	        		
        	Group C = B.redefine();
        		/*Group C1 =*/ C.addMember(Var.CHAR, 24);
        		/*Group C2 =*/ C.addMember(Var.CHAR, 16);

        	Group D = B.redefine().occurs(5);
        		Group D1 = D.addMember(Var.UNUMERIC, 4);
        		Group D2 = D.addMember(Var.CHAR, 4);
        		
        	Group E = D.redefine().occurs(10);
        		Group E1 = E.addMember(Var.CHAR, 4);
        		
        assertEquals("1234890011aaAA0022bbBB0033ccCC0044ddDD0055eeEE", A.getString());	        		
        for(i=1; i<=5;i++) {
        	B1.index(i).set(i*3 + 989);
        	B2.index(i).set(String.format("%02d%c%c", i*7, i+'A', i+'a'));
        }
        assertEquals("123489099207Bb099514Cc099821Dd100128Ee100435Ff", A.getString());	        		
        for(i=1; i<=5;i++) {
        	D1.index(i).set(i*3 + 989);
        	D2.index(i).set(String.format("%02d%c%c", i*9, i+'C', i+'g'));
        }
        assertEquals("123489099209Dh099518Ei099827Fj100136Gk100445Hl", A.getString());	        		
        assertEquals("099518Ei", B.index(2).getString());
        assertEquals("0995", D1.index(2).getString());
        
        int j;
        StringBuilder y = new StringBuilder();
        for(j=1, i=1;i<=10;i++) {
        	String s;
        	if(i%2 != 0) {
            	s = String.format("%04d", j*3 + 989);
            	y.append(s);
        	} else {
            	s = String.format("%02d%c%c", j*9, j+'C', j+'g');
            	y.append(s);
        		j++;
        	}
//        	System.out.printf("idx=%s j=%d i=%d s=%s y=%s\n", E1.index(i).getString(),j,i,s,y);
        	assertEquals(s, E1.index(i).getString());
        }
        
    }
	
	@SuppressWarnings("unused")
    public void test_17() throws Exception {
		Group y2000_ws = new Group();
        Group y2Convdatetonum = y2000_ws.addMember(Var.CHAR, 13).set("convdatetonum");
        Group y2Convnumtodate = y2000_ws.addMember(Var.CHAR, 13).set("convnumtodate");
        Group y2Convpertonum = y2000_ws.addMember(Var.CHAR, 12).set("convpertonum");
        Group y2Convnumtoper = y2000_ws.addMember(Var.CHAR, 12).set("convnumtoper");
        Group y2_date = y2000_ws.addMember(Var.UNUMERIC|Var.COMP, 8);
        Group y2_dnum = y2000_ws.addMember(Var.UNUMERIC|Var.COMP, 8);
        Group y2_compPeriod = y2000_ws.addMember(Var.UNUMERIC|Var.COMP, 4);
        Group y2_pnum = y2000_ws.addMember(Var.UNUMERIC|Var.COMP, 4);
        Group y2_period = y2000_ws.addMember(Var.UNUMERIC, 4);
        Group xfiller_007 = y2_period.redefine();
             Group y2_mm = xfiller_007.addMember(Var.UNUMERIC, 2);
             Group y2_yy = xfiller_007.addMember(Var.UNUMERIC, 2);
        Group y2_13_perGl = y2000_ws.addMember(Var.CHAR, 1).set("");
       //used as a temp storage if comparing two dates or periods
        Group y2_wksvDnum = y2000_ws.addMember(Var.UNUMERIC|Var.COMP, 8);
        Group y2_wksvPnum = y2000_ws.addMember(Var.UNUMERIC|Var.COMP, 4);
        
          Group miscVariables = new Group();
          Group testforfile = miscVariables.addMember(Var.CHAR, 11).set("testforfile");
          Group settransactionparams = miscVariables.addMember(Var.CHAR, 20)
                .set("settransactionparams");
          Group delayer = miscVariables.addMember(Var.CHAR, 4).set("dlay");
          Group wsLevel = miscVariables.addMember(Var.UNUMERIC, 2).set(1);
        //    03  CHECK6                  PIC X(10) VALUE "[SYS]<SYS>".
          Group xtemp = miscVariables.addMember(Var.NUMERIC|Var.COMP, 11, 2);
          Group lngth = miscVariables.addMember(Var.UNUMERIC|Var.COMP, 4);
          Group lngth2 = miscVariables.addMember(Var.UNUMERIC|Var.COMP, 4);
          Group formpw = miscVariables.addMember(Var.CHAR, 1).set("");
          Group moderead = miscVariables.addMember(Var.CHAR, 2).set("mr");
        //    03  CHECK4                  PIC X(4) VALUE "MAST".
          Group tru = miscVariables.addMember(Var.CHAR, 1).setHex("FF");
          Group flse = miscVariables.addMember(Var.CHAR, 1).setHex("00");
          Group ix = miscVariables.addMember(Var.UNUMERIC|Var.COMP, 4).set(0);
          Group xix2 = miscVariables.addMember(Var.UNUMERIC|Var.COMP, 4).set(0);
          Group l6 = miscVariables.addMember(Var.UNUMERIC|Var.COMP, 4).set(6);
          Group l7 = miscVariables.addMember(Var.UNUMERIC|Var.COMP, 4).set(7);
          Group fileLength = miscVariables.addMember(Var.UNUMERIC|Var.COMP, 8);
          Group progFh = miscVariables.addMember(Var.UNUMERIC|Var.COMP, 4);
        //    03  CHECK5                  PIC X(6) VALUE "ER.CNF".
          Group xefl = miscVariables.addMember(Var.UNUMERIC, 1).set(0);
          Group xfiller_006 = miscVariables.addMember(Var.CHAR, 10).set("TICKSWAIT:");
        //        SET ISAM TIMEOUT TO 5 MINUTES
          Group wtickswaitX = miscVariables.addMember(Var.UNUMERIC, 4).set(3000);
        //    03  SW0                     PIC 99 COMP.
          Group isam_8 = miscVariables.addMember(Var.UNUMERIC, 1).set(0);
          Group wtickswait = miscVariables.addMember(Var.UNUMERIC|Var.COMP, 4);
        //    03  PROG-NAME               PIC X(32).
        //    03  CHECK-FIELD REDEFINES PROG-NAME.
        //        05  CHECK1              PIC X(10).
        //        05  CHECK2              PIC X(4).
        //        05  CHECK3              PIC X(6).
        //        05  FILLER              PIC X(12).
          Group wsName = miscVariables.addMember(Var.CHAR, 30);
          Group wsCh = wsName.redefine().addMember(Var.UNUMERIC, 1).occurs(30);
          Group dates = miscVariables.addGroup();
              Group wmm = dates.addMember(Var.UNUMERIC, 2);
              Group wdd = dates.addMember(Var.UNUMERIC, 2);
              Group wyy = dates.addMember(Var.UNUMERIC, 2);
          Group workDate = dates.redefine().addMember(Var.UNUMERIC, 6);

       y2_date.set(12516);
       
       assertEquals("00012516", y2_date.getString());
       workDate.set(y2_date);
       assertEquals("012516", workDate.getString());
       assertEquals("01", wmm.getString());
       assertEquals("25", wdd.getString());
       assertEquals("16", wyy.getString());
       
       
       

       Group SG_HOLDKEY = new Group();
              Group SD_KEY = SG_HOLDKEY.addMember(Var.UNUMERIC, 3);
              Group SD_RCDTYP = SG_HOLDKEY.addMember(Var.CHAR, 1);
             
       SD_KEY.set(3);
       Glb GLB=new Glb();
       assertEquals(false, SD_KEY.eq(GLB.ZEROS));
              
	}
	
	public void testPictures() throws Exception {
		Group icuirAvail = new Group(Var.NUMERIC, 13, 2).format("Z(9).99-");
        Group icuixAvail = icuirAvail.redefine().addMember(Var.CHAR, 13);
        Group icuiyAvail = icuirAvail.redefine().addMember(Var.UNUMERIC, 13,2);

        icuirAvail.set(-12345.67);
        assertEquals("    12345.67-",icuirAvail.getString());
        assertEquals("    12345.67-",icuixAvail.getString());

        Var tempv = new Var(Var.NUMERIC, 11, 2).set(-12345.67);
        assertEquals("-000012345.67", tempv.getString());
        Group temp = new Group(Var.NUMERIC, 11, 2);
        temp.set(-12345.67);
        assertEquals("-000012345.67", temp.getString());
        icuirAvail.set(temp);
        assertEquals("    12345.67-",icuirAvail.getString());
        assertEquals("    12345.67-",icuixAvail.getString());
        
        System.out.println("icuiyAvail=" + icuiyAvail.getString());
        assertEquals("000012345.67", icuiyAvail.getString());
        

	}
	
    public void test_bwz() throws Exception {
    	Group g = new Group();
    	Group v1 = g.addMember(Var.NUMBER, 5).format("z9999").bwz();
    	Group v2 = g.addMember(Var.NUMBER, 5).bwz();
    	Group v3 = g.addMember(Var.NUMBER, 6).format("ZZ,Z99").bwz();
    	
    	v1.set("00000");
    	assertEquals("     ", v1.getString());
    	v1.replaceAll("0", "7");
    	assertEquals("     ", v1.getString());
    	v1.replaceAll(" ", "A");
    	assertEquals("AAAAA", v1.getString());
    	v1.set("     ");
    	assertEquals("     ", v1.getString());
    	v1.set(1);
    	assertEquals(" 0001", v1.getString());
    	
    	v2.set("00000");
    	assertEquals("     ", v2.getString());
    	v2.replaceAll("0", "7");
    	assertEquals("     ", v2.getString());
    	v2.set("     ");
    	assertEquals("     ", v2.getString());
    	v2.set(1);
    	assertEquals("00001", v2.getString());

    	v3.set("00000");
    	assertEquals("      ", v3.getString());
    	v3.replaceAll("0", "7");
    	assertEquals("      ", v3.getString());
    	v3.replaceAll(" ", "7");
    	assertEquals("777777", v3.getString());
    	v3.set("      ");
    	assertEquals("      ", v3.getString());
    	v3.set(1);
    	assertEquals("    01", v3.getString());
    	
    }
    
    public void test_picture() throws Exception {
    	Group g = new Group();
    	Group.setGroupMode(GroupMode.UNISYS);
    	Group v1 = g.addMember(Var.NUMBER, 6, 2).format("---9.99");
    	Group v2 = g.addMember(Var.NUMBER, 7).format("-999.99");
    	Group v3 = g.addMember(Var.NUMBER, 7).format("-ZZ9.99");
    	Group v4 = g.addMember(Var.NUMBER, 8).format("9999/99/99");
    	Group v5 = g.addMember(Var.NUMBER, 11).format("bz(05)9bbbb");
    	Group v6 = g.addMember(Var.NUMBER, 6).format("ZZ,ZZ9");
    	Group v7 = g.addMember(Var.NUMBER, 9).format("Z,ZZZ,ZZZ");
    	Group v8 = g.addMember(Var.NUMBER, 4).format("-999");
    	Group v9 = g.addMember(Var.NUMBER, 4, 2).format("ZZ.ZZ");
    	Group v10 = g.addMember(Var.NUMBER, 9, 2).format("$$,$$$.99");
    	
    	v1.set(0);
    	v2.set(0);
    	v3.set(0);
    	v4.set(0);
    	v5.set(0);
    	v6.set(0);
    	v7.set(0);
    	v8.set(0);
    	v9.set(0);
    	v10.set(0);
    	//            ---9.99-999.99-ZZ9.999999/99/99bzzzzz9bbbbZZ,ZZ9Z,ZZZ,ZZZ-999ZZ.ZZ$$,$$$.99
    	assertEquals("   0.00 000.00   0.000000/00/00      0         0          000          $.00", g.getString());
    	
    	g.initialise();

    	v1.set("-2.02");
     	assertEquals("  -2.02", v1.getString());
    	v1.set("-1204.2");
    	assertEquals("-204.20", v1.getString());
    	v1.set("1204.239");
    	assertEquals(" 204.23", v1.getString());
    	v1.set(0);
     	assertEquals("   0.00", v1.getString());

    	v2.set("-2.02");
    	assertEquals("-002.02", v2.getString());
    	v2.set("-1204.2");
    	assertEquals("-204.20", v2.getString());
    	v2.set(1204.239);
    	assertEquals(" 204.23", v2.getString());
    	v2.set(0);
    	assertEquals(" 000.00", v2.getString());
    	
    	v3.set("-2.02");
     	assertEquals("-  2.02", v3.getString());
    	v3.set("-1204.2");
    	assertEquals("-204.20", v3.getString());
    	v3.set("1204.239");
     	assertEquals(" 204.23", v3.getString());
    	v3.set(0);
     	assertEquals("   0.00", v3.getString());
    	
    	v4.set("70924");
    	assertEquals("0007/09/24", v4.getString());
    	v4.set(0);
    	assertEquals("0000/00/00", v4.getString());
    	v4.set(20081127);
    	assertEquals("2008/11/27", v4.getString());
    	v4.set(-19840601);
    	assertEquals("1984/06/01", v4.getString());
    	
    	v5.set(0);
    	assertEquals("      0    ", v5.getString());
    	v5.set("0000");
    	assertEquals("      0    ", v5.getString());
    	v5.set("      ");
    	assertEquals("      0    ", v5.getString());
    	v5.set(12345);
    	assertEquals("  12345    ", v5.getString());
    	v5.set(12340678);
    	assertEquals(" 340678    ", v5.getString());
    	v5.set("  -0009");
    	assertEquals("      9    ", v5.getString());
    	
    	v6.set(0);
    	assertEquals("     0", v6.getString());
    	v6.set("  123");
    	assertEquals("   123", v6.getString());
    	v6.set("12045");
    	assertEquals("12,045", v6.getString());
    	v6.set(123456);
    	assertEquals("23,456", v6.getString());
    	
    	v7.set(0);
    	assertEquals("         ", v7.getString());
    	v7.set(23456);
    	assertEquals("   23,456", v7.getString());
    	v7.set(123456789);
    	assertEquals("3,456,789", v7.getString());
    	
    	v8.set(0);
    	assertEquals(" 000", v8.getString());
    	v8.set(345);
    	assertEquals(" 345", v8.getString());
    	v8.set(-24);
    	assertEquals("-024", v8.getString());

    	v9.set("0");
    	assertEquals("     ", v9.getString());
    	v9.set("0.10");
    	assertEquals("  .10", v9.getString());
    	v9.set("0.01");
    	assertEquals("  .01", v9.getString());
    	v9.set(123.056);
    	assertEquals("23.05", v9.getString());
    	
    	v10.set(0);
    	assertEquals("     $.00", v10.getString());
    	v10.set(1218);
    	assertEquals("$1,218.00", v10.getString());
    	v10.set(18);
    	assertEquals("   $18.00", v10.getString());
    	v10.set(218);
    	assertEquals("  $218.00", v10.getString());
    	
        Group gp = new Group();
        Group oe1ROrdTotX = gp.addMember(Var.CHAR, 11);
        Group oe1ROrdTot = oe1ROrdTotX.redefine().addMember(Var.NUMERIC, 11, 2).format("Z(7).99-");
        
        oe1ROrdTot.set(.4);
        assertEquals("       .40 ", oe1ROrdTotX.getString());
        assertEquals("       .40 ", oe1ROrdTot.getString());
        oe1ROrdTot.set(-0.4);
        assertEquals("       .40-", oe1ROrdTotX.getString());
        assertEquals("       .40-", oe1ROrdTot.getString());
    	
    }
    
	@SuppressWarnings("unused")
    public void test_morestuff() throws Exception {
    	Group imsgeRec = new Group();
    		Group imsgeRecnbr = imsgeRec.addMember(Var.UNUMERIC, 7);
    		Group imsgeMsgbdy = imsgeRec.addMember(Var.CHAR, 1857);
    		
    	assertEquals(true, imsgeRecnbr.eq(0));
    	imsgeRec.set("0000002\r:21:01\r:31:20090122\r:32:151790\r:50:11114010810000580482001004");
    	assertEquals(true, imsgeRecnbr.eq(2));
    
    	Group CP_GROUP = new Group();
        	Group SG_KAL = CP_GROUP.addGroup();
        	Group K_YEAR = SG_KAL.addMember(Var.UNUMERIC, 4);
        	Group K_MONTH = SG_KAL.addMember(Var.UNUMERIC, 2);
        	Group K_STSDAY = SG_KAL.addMember(Var.CHAR, 31);
        Group SG_SKA = CP_GROUP.addGroup();
        	Group SK_IPN = SG_SKA.addMember(Var.CHAR, 3);
        	Group SK_ACTDTE = SG_SKA.addMember(Var.UNUMERIC, 8);
        	Group SK_EXPDTE = SG_SKA.addMember(Var.UNUMERIC, 8);
        	Group SK_BRNIDT = SG_SKA.addMember(Var.UNUMERIC, 8);
        	Group SK_BRNIDTORG = SG_SKA.addMember(Var.UNUMERIC, 8);
        	Group SK_NAM1 = SG_SKA.addMember(Var.CHAR, 35);
        	Group SK_NRB = SG_SKA.addMember(Var.CHAR, 26);
        Group SG_SKK = CP_GROUP.addGroup();
        	Group SO_IPN = SG_SKK.addMember(Var.CHAR, 3);
        	Group SO_MSGSUBGRP = SG_SKK.addMember(Var.UNUMERIC, 2);
        	Group SO_DOKTYP = SG_SKK.addMember(Var.CHAR, 2);
        	Group SO_ACTDTE = SG_SKK.addMember(Var.UNUMERIC, 8);
        	Group SO_EXPDTE = SG_SKK.addMember(Var.UNUMERIC, 8);

    SG_SKA.setSubstr("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",1);
//    System.out.println("SG_SKA=" + SG_SKA);
//    System.out.println("SG_SKK=" + SG_SKK);
    	
    }
	
	public void testComp2() throws Exception {
		Group gd = new Group();
		Group g1 = gd.addMember(Var.NUMBER|Var.COMP, 5,2);
		Group gdm = gd.addMember(Var.UNUMERIC|Var.COMP, 9);
		Group gdx = gd.addMember(Var.NUMERIC, 9);
		Group gdy = gd.addMember(Var.NUMERIC|Var.COMP, 9);

		gdm.set(0);
		gdm.set(10893);
		assertEquals("000010893",gdm.getString());
		gdm.set(-10894.0);
		System.out.println("gdm="+gdm.getString());
		assertEquals("000010894",gdm.getString());
		gdx.set(-10894);
		assertEquals("-000010894",gdx.getString());
		gdy.set(-10894);
		assertEquals("-000010894",gdy.getString());
	
		gd.setHex("1ff92def2a");
		System.out.println("g1="+g1.getDouble());
		System.out.println("g1="+g1.getString());
		
		Group dg = new Group(Var.NUMBER, 6,2);
		Group lg = new Group(Var.NUMBER, 6);
		Var dv = new Var(Var.NUMBER, 6,2);
		Var lv = new Var(Var.NUMBER, 6);
		dg.set(9999.99);
		lg.set(9999);
		dv.set(9999.99);
		lv.set(9999);
		assertEquals(true, g1.gt(9999));
		assertEquals(true, g1.gt(999.99));
		assertEquals(true, g1.gt(dg));
		assertEquals(true, g1.gt(lg));
		assertEquals(true, g1.gt(dv));
		assertEquals(true, g1.gt(lv));
		
		
        Group wkNetRemaining = new Group(Var.UNUMERIC|Var.COMP, 9, 2).set(0);
        Group pxcalcCtNet = new Group(Var.NUMERIC|Var.COMP, 9, 2);

        pxcalcCtNet.set(1424.49);
        wkNetRemaining.set(pxcalcCtNet);

        assertEquals("00001424.49", pxcalcCtNet.getString());
        assertEquals("0001424.49", wkNetRemaining.getString());
	}

	public void test_truncation() throws Exception {
		Group ntotMat = new Group(Var.NUMERIC|Var.COMP, 11, 2).set(0);
        ntotMat.set(29203.57875);
        assertEquals("0000029203.57",ntotMat.getString());

        ntotMat.set(2.4);
        assertEquals("0000000002.40",ntotMat.getString());

        
        Group jccalcWs = new Group();
        Group jcTemp = jccalcWs.addMember(Var.NUMERIC|Var.COMP, 10);
        Group jcEst = jccalcWs.addMember(Var.NUMERIC|Var.COMP, 13, 2).set(72.00);
        Group jcAct = jccalcWs.addMember(Var.NUMERIC|Var.COMP, 13, 2).set(8746.50);
        Group jcPct = jccalcWs.addMember(Var.NUMERIC|Var.COMP, 10, 4);
        Group jchistCompPct = jccalcWs.addMember(Var.UNUMERIC|Var.COMP, 3);
        Group rpPhasePct = jccalcWs.addMember(Var.UNUMERIC, 7, 2).format("Z(4).99");

        jcTemp.set(new Var((jcAct.getDouble() / jcEst.getDouble()) * 100.0).round());
        
        assertEquals("00000012148",jcTemp.getString());
        
        jchistCompPct.set(jcTemp); 
        assertEquals("148",jchistCompPct.getString());
        rpPhasePct.set(jchistCompPct);
        assertEquals(" 148.00",rpPhasePct.getString());
        
        Group wsTotals = new Group();
        Group wsTotal = wsTotals.addGroup().occurs(2);
        Group wsTotSales = wsTotal.addMember(Var.NUMERIC|Var.COMP, 11, 2);
        Group wsTotCost = wsTotal.addMember(Var.NUMERIC|Var.COMP, 11, 2);
       
        Group prAmt = new Group(Var.NUMERIC, 13, 2).format("Z(9).99-");

        wsTotSales.index(1).set(18.32);
        wsTotCost.index(1).set(20);
        double d1 = wsTotSales.index(1).getDouble();
        double d2 = wsTotCost.index(1).getDouble();
        double d = d1 - d2;
        prAmt.set(d);
        assertEquals("        1.68-", prAmt.getString());
	}
	
	public void testOverflow() throws Exception {
		Var pct1 = new Var (Var.UNUMERIC, 3);
        Group pct2 = new Group (Var.UNUMERIC, 3).format("ZZ9");
        Group pct3 = new Group (Var.UNUMERIC, 3);
        Group pct4 = new Group (Var.NUMERIC, 5,2);

        
        pct1.set(1234);
        pct2.set(1234);
        pct3.set(1234);
        pct4.set(1234.456);

        assertEquals(234, pct1.getInt());
        assertEquals(true, pct2.overflow());
        assertEquals(234, pct2.getInt());
        assertEquals(true, pct3.overflow());
        assertEquals(234, pct3.getInt());
        assertEquals(true, pct4.overflow());
        assertEquals("234.45", pct4.getString());
        assertEquals(234.45, pct4.getDouble());
        
        pct1.set(123);
        pct2.set(123);
        pct3.set(123);
        pct4.set(123.456);
        assertEquals(false, pct1.overflow());
        assertEquals(false, pct2.overflow());
        assertEquals(false, pct3.overflow());
        assertEquals(false, pct4.overflow());
        assertEquals("123.45", pct4.getString());

        pct1.set("1234");
        pct2.set("1234");
        pct3.set("1234");
        pct4.set("1234.56");
        assertEquals(true, pct1.overflow());
        assertEquals(true, pct2.overflow());
        assertEquals(true, pct3.overflow());
        assertEquals(true, pct4.overflow());
//        assertEquals("234.45", pct4.getString());

        pct1.set("123");
        pct2.set("123");
        pct3.set("123");
        assertEquals(false, pct1.overflow());
        assertEquals(false, pct2.overflow());
        assertEquals(false, pct3.overflow());
        
        
        Group jcmastCosts = new Group();
        Group jcmastRev = jcmastCosts.addMember(Var.NUMERIC|Var.COMP, 11, 2);
        Group jcmastAct = jcmastCosts.addMember(Var.NUMERIC|Var.COMP, 11, 2);
        Group jcmastCommFunds = jcmastCosts.addMember(Var.NUMERIC|Var.COMP, 11, 2);
        Group brl2_pct1 = jcmastCosts.addMember(Var.UNUMERIC, 3).format("ZZ9");
        jcmastAct.set(509.90);
        jcmastCommFunds.set(150);
        jcmastRev.set(241);
        
        double d1 = jcmastAct.getDouble();
        double d2 = jcmastCommFunds.getDouble();
        double d3 = jcmastRev.getDouble();

        double d = ((jcmastAct.getDouble() + jcmastCommFunds.getDouble()) / jcmastRev.getDouble()) * 100.0;
        Var x = new Var(d);
        Var y = x.round();
        assertEquals(3, y.size());
        brl2_pct1.set(y);
        assertEquals(false, brl2_pct1.overflow());
	}
	
    @SuppressWarnings("unused")
	public void testComp() throws Exception {
    	Group dfmastRec = new Group();		//co=0 go=0
         Group dfmastKey = dfmastRec.addGroup(); //co=0 go=0
             Group dfmastCoNo = dfmastKey.addMember(Var.UNUMERIC|Var.COMP, 2);  //co=0 go=0
             Group dfFiller = dfmastKey.addGroup();  //co=1 go=1
                 Group dfRecType = dfFiller.addMember(Var.CHAR, 1);  //co=1 go=0
                 Group dfKeyx = dfFiller.addGroup(); //co=2 go=1
                     Group dfDummy = dfKeyx.addMember(Var.UNUMERIC|Var.COMP, 6); //co=2 go=0
                 Group dfKey1 = dfKeyx.redefine();  //co=2  go=0
                     Group dfStCty = dfKey1.addGroup(); //co=2  go=0
                         Group dfSt = dfStCty.addMember(Var.UNUMERIC|Var.COMP, 2); //co=2  go=0
                         Group dfCty = dfStCty.addMember(Var.UNUMERIC|Var.COMP, 2); //co=3  go=1
                     Group filler633 = dfKey1.addMember(Var.CHAR, 1);
                 Group filler634 = dfKeyx.redefine();
                     Group dfDept = filler634.addMember(Var.UNUMERIC|Var.COMP, 6);
                 Group filler635 = dfKeyx.redefine();
                     Group dfDed = filler635.addMember(Var.UNUMERIC|Var.COMP, 3);
                     Group filler636 = filler635.addMember(Var.CHAR, 1);
                 Group filler637 = dfKeyx.redefine();
                     Group dfLoc = filler637.addMember(Var.UNUMERIC|Var.COMP, 6);
                 Group filler638 = dfKeyx.redefine();
                     Group dfPcat = filler638.addMember(Var.UNUMERIC|Var.COMP, 6);
                 Group filler639 = dfKeyx.redefine();
                     Group dfSalesmanNo = filler639.addMember(Var.UNUMERIC|Var.COMP, 6);
             Group dfCode = dfFiller.redefine().addMember(Var.CHAR, 4);
             Group dfCodeN = dfFiller.redefine().addMember(Var.UNUMERIC, 4);
         
         dfmastCoNo.set(01);
         dfRecType.set("X");
         
         dfSt.set(-27);
         assertEquals(27, dfSt.getInt());
         
         dfSt.set(127);
         assertEquals(127, dfSt.getInt());
         
         dfSt.set(200);
         assertEquals(200, dfSt.getInt());

         dfSt.set(255);
         assertEquals(255, dfSt.getInt());

         dfSt.set(128);
         assertEquals(128, dfSt.getInt());
         
         dfSt.set(12);
         dfCty.set(34);
         filler633.set("Y");
         
         assertEquals("01",dfmastCoNo.getString());
         assertEquals("X",dfRecType.getString());
         assertEquals("12",dfSt.getString());
         assertEquals("34",dfCty.getString());
         assertEquals("Y",filler633.getString());
         
         //System.out.println("dfmastRec=" + dfmastRec.getString());
         //System.out.println("dfmastRec=" + Util.formatHex(dfmastRec.getBytes()));
         
         //assertEquals("000158000C002259",Util.formatHex(dfmastRec.getBytes()));
         assertEquals("01580C2259",Util.formatHex(dfmastRec.getBytes()));
         
         Var v = new Var(Var.CHAR,20).set(dfmastRec);
         
         assertEquals("01580C2259",Util.formatHex(dfmastRec.getBytes()));
         
         dfmastRec.set(v);
         assertEquals("01580C2259",Util.formatHex(dfmastRec.getBytes()));
         assertEquals("01",dfmastCoNo.getString());
         assertEquals("X",dfRecType.getString());
         assertEquals("12",dfSt.getString());
         assertEquals("34",dfCty.getString());
         assertEquals("Y",filler633.getString());
         
         
         Group gcomp = new Group();
         	Group  f1x = gcomp.addMember(Var.CHAR, 1); //pic x.
         	Group  f1p94c = gcomp.addMember(Var.NUMERIC|Var.COMP, 4); //format("s9(4)");
         	Group  f1m94c = gcomp.addMember(Var.NUMERIC|Var.COMP, 4); //format("s9(4)");
         	Group  f194c = gcomp.addMember(Var.UNUMERIC|Var.COMP, 4); //format("9(4)");
         	Group  f194 = gcomp.addMember(Var.UNUMERIC, 4).format("9(4)");
         	Group  f1p94v99c = gcomp.addMember(Var.NUMERIC|Var.COMP, 6,2); //format("s9(4)V99");
         	Group  f1m94v99c = gcomp.addMember(Var.NUMERIC|Var.COMP, 6,2); //format("s9(4)V99");
         	Group  f19v994c = gcomp.addMember(Var.UNUMERIC|Var.COMP, 6,2); //format("9(4)V99");
         	Group  f19v994 = gcomp.addMember(Var.UNUMERIC, 6,2); //format("9(4)V99");
         
         gcomp.set(" ");
         assertEquals("                          ", gcomp.getString());
         f1x.set("X");
		 f1p94c.set(1234);
		 f1m94c.set(-1234);
		 f194c.set(5678);
		 f194.set(1357);
		 f1p94v99c.set(1234.56);
		 f1m94v99c.set(1234.56);
		 f19v994c.set(9876.54);
		 f19v994.set(1234.56);
		 
		 assertEquals(f1x.getString(),"X");
		 assertEquals(f1p94c.getInt(),1234);
		 assertEquals(f1m94c.getInt(),-1234);
		 assertEquals(f194c.getInt(),5678);
		 assertEquals(f194.getInt(),1357);
		 assertEquals(f1p94v99c.getDouble(),1234.56);
		 assertEquals(f1m94v99c.getDouble(),1234.56);
		 assertEquals(f19v994c.getDouble(),9876.54);
		 assertEquals(f19v994.getDouble(),1234.56);

		 //assertEquals("5804D2FB2E162E313335370001E2400001E240000F1206313233343536", Util.formatHex(gcomp.getBytes()));
		 assertEquals("5804D2FB2E162E3133353701E24001E2400F1206313233343536", Util.formatHex(gcomp.getBytes()));
         
		 
		 Group X = new Group();
		 	Group u2 = X.addGroup(Var.UNUMERIC|Var.COMP, 4);
		 	Group u4 = X.addGroup(Var.UNUMERIC|Var.COMP, 8);
		 	Group u8 = X.addGroup(Var.UNUMERIC|Var.COMP, 18);
		 
		 u2.set(10000);
		 assertEquals(10000, u2.getInt());
		 u2.set(20000);
		 assertEquals(20000, u2.getInt());
		 u2.set(32768);
		 assertEquals(32768, u2.getInt());
		 u2.set(65535);
		 assertEquals(65535, u2.getInt());

		 u4.set(65536);
		 assertEquals(65536, u4.getInt());
		 u4.set(2147483647);
		 assertEquals(2147483647, u4.getInt()); //max signed int
		 u4.set(4294967295L);  //max unsigned int
		 assertEquals(4294967295L, u4.getLong());
		 
		 u8.set(4294967295L);  //max unsigned int
		 assertEquals(4294967295L, u8.getLong());

		 u8.set(429496729500L);  
		 assertEquals(429496729500L, u8.getLong());
		 
		 u8.set(9223372036854775807L);  //Max signed long
		 assertEquals(9223372036854775807L, u8.getLong());
		 
		 
    }
    
    @SuppressWarnings("unused")
	public void testeqemptysring() throws Exception {
    	Group commonSystemArea = new Group();
        Group sylSyVersion = commonSystemArea.addMember(Var.UNUMERIC, 1);
        Group sylModule = commonSystemArea.addMember(Var.CHAR, 2);
        Group sylProg = commonSystemArea.addMember(Var.CHAR, 15);
        Group controlId = commonSystemArea.addMember(Var.CHAR, 20);
        Group sylProgs = commonSystemArea.addGroup();
            Group sylTemp = sylProgs.addMember(Var.CHAR, 75);
            Group systemRoutine = sylProgs.addMember(Var.CHAR, 75);
            Group sylForm = sylProgs.addMember(Var.CHAR, 75);
            Group sylIsam = sylProgs.addMember(Var.CHAR, 75);
            Group sylPrint = sylProgs.addMember(Var.CHAR, 75);
            Group sylSort = sylProgs.addMember(Var.CHAR, 75);
            Group sylMenu = sylProgs.addMember(Var.CHAR, 75);
            Group sylSecure = sylProgs.addMember(Var.CHAR, 75);


        assertEquals(false,controlId.ne(""));
        assertEquals(true,controlId.eq(""));
        
        controlId.set("/isweb/admin/");
        assertEquals(true,controlId.ne(""));
        assertEquals(false,controlId.eq(""));
        
        assertEquals(false,controlId.eq("/"));
        assertEquals(true,controlId.ne("/"));
        
    }

    @SuppressWarnings("unused")
	public void testMoves() throws Exception {
        Group termRcv2 = new Group();
        Group rPostingDateX = termRcv2.addMember(Var.CHAR, 6);
        Group rPostingDate = rPostingDateX.redefine().addMember(Var.UNUMERIC, 6).format("Z(6)");
        Group wrkDate = termRcv2.addGroup();
               Group mm = wrkDate.addMember(Var.UNUMERIC, 2);
               Group dd = wrkDate.addMember(Var.UNUMERIC, 2);
               Group yy = wrkDate.addMember(Var.UNUMERIC, 2);

     
     mm.set(1);
     dd.set(1);
     yy.set(1);
     rPostingDate.set(wrkDate);
     assertEquals("01", mm.getString());
     assertEquals("010101", wrkDate.getString());
     assertEquals("010101", rPostingDate.getString());
     assertEquals("010101", rPostingDateX.getString());
     rPostingDate.set("010101");
     assertEquals(" 10101", rPostingDate.getString());
     assertEquals(" 10101", rPostingDateX.getString());
     
     Group date_gr = new Group();
     	Group y2_date2 = date_gr.addMember(Var.UNUMERIC|Var.COMP, 8);
     Group num = new Group(Var.NUMERIC, 15, 3);
     Group numg = new Group();
     	Group num1 = numg.addMember(Var.NUMBER, 15, 3);
     	
     num.set(53015);
     y2_date2.set(num);
     assertEquals("00053015",y2_date2.getString() );
     
     Group wsVariables = new Group();
     Group currTime = wsVariables.addMember(Var.UNUMERIC|Var.COMP, 8);
     Group currTimeLong = wsVariables.addMember(Var.UNUMERIC|Var.COMP, 12);
     
     currTime.set("23100101");
     assertEquals("23100101", currTime.getString());
     currTimeLong.set("231001011234");
     assertEquals("231001011234", currTimeLong.getString());


     Group formWs = new Group();
     Group xnum = formWs.addMember(Var.NUMERIC, 15, 3).set("111133");

     Group armastRecord = new Group(Var.CHAR, 680);
     	Group armastRec2 = armastRecord.redefine();
     		Group armastKey2 = armastRec2.addGroup();
     			Group armastCoNo2 = armastKey2.addMember(Var.UNUMERIC|Var.COMP, 2);
     			Group armastAcctNo2 = armastKey2.addMember(Var.NUMERIC|Var.COMP, 10);


     armastAcctNo2.set(xnum);

     assertEquals("000000111133.000", xnum.getString());
     assertEquals("00000111133", armastAcctNo2.getString());

    }

    @SuppressWarnings("unused")
    public void testoccursdef() throws Exception {
    	 Group pcworkRecord = new Group(300);
    	Group pcworkRec = pcworkRecord.redefine();
    	Group pcworkKey = pcworkRec.addMember(Var.UNUMERIC|Var.COMP, 6);
    	Group pcworkSales = pcworkRec.addMember(Var.NUMERIC|Var.COMP, 12, 2);
    	Group pcworkCost = pcworkRec.addMember(Var.NUMERIC|Var.COMP, 12, 2);
    	Group pcworkLnDisc = pcworkRec.addMember(Var.NUMERIC|Var.COMP, 12, 2);
    	Group pcworkCash = pcworkRec.addMember(Var.NUMERIC|Var.COMP, 12, 2);
    	Group pcworkInv = pcworkRec.addMember(Var.NUMERIC|Var.COMP, 12, 2);
    	Group invgEntryTotals = pcworkRec.addGroup();
    	Group ietgGlCnt = invgEntryTotals.addMember(Var.NUMERIC|Var.COMP, 8);
    	Group ietgGlTxbl = invgEntryTotals.addMember(Var.NUMERIC|Var.COMP, 10, 2);
    	Group ietgGlNtax = invgEntryTotals.addMember(Var.NUMERIC|Var.COMP, 10, 2);
    	Group ietgGlGross = invgEntryTotals.addMember(Var.NUMERIC|Var.COMP, 10, 2);
    	Group ietgGlInvoiceAmt = invgEntryTotals.addMember(Var.NUMERIC|Var.COMP, 10, 2);
    	Group ietgGlCash = invgEntryTotals.addMember(Var.NUMERIC|Var.COMP, 10, 2);
    	Group ietgGlDownPymt = invgEntryTotals.addMember(Var.NUMERIC|Var.COMP, 10, 2);
    	Group ietgGlRebateAmt = invgEntryTotals.addMember(Var.NUMERIC|Var.COMP, 10, 2);
    	Group ietgGlCredCard = invgEntryTotals.addMember(Var.NUMERIC|Var.COMP, 10, 2);
    	Group ietgGlLnDisc = invgEntryTotals.addMember(Var.NUMERIC|Var.COMP, 10, 2);
    	Group ietgGlOrdDisc = invgEntryTotals.addMember(Var.NUMERIC|Var.COMP, 10, 2);
    	Group ietgGlCashDisc = invgEntryTotals.addMember(Var.NUMERIC|Var.COMP, 10, 2);
    	Group ietgGlBank = invgEntryTotals.addMember(Var.NUMERIC|Var.COMP, 10, 2);
    	Group ietgGlCost = invgEntryTotals.addMember(Var.NUMERIC|Var.COMP, 10, 2);
    	Group ietgGlStateTax = invgEntryTotals.addMember(Var.NUMERIC|Var.COMP, 10, 2);
    	Group ietgGlCountyAmt = invgEntryTotals.addMember(Var.NUMERIC|Var.COMP, 10, 2);
    	Group ietgGlCityAmt = invgEntryTotals.addMember(Var.NUMERIC|Var.COMP, 10, 2);
    	Group ietgGlAo = invgEntryTotals.addMember(Var.NUMERIC|Var.COMP, 10, 2).occurs(5);
    	Group ietgGlCodRecv = invgEntryTotals.addMember(Var.NUMERIC|Var.COMP, 10, 2);
    	Group ietgDownPymt = invgEntryTotals.addMember(Var.NUMERIC|Var.COMP, 10, 2);
    	Group ietgRebateAmt = invgEntryTotals.addMember(Var.NUMERIC|Var.COMP, 10, 2);
    	Group ietgCredCard = invgEntryTotals.addMember(Var.NUMERIC|Var.COMP, 10, 2);
    	Group ietgOrdDisc = invgEntryTotals.addMember(Var.NUMERIC|Var.COMP, 10, 2);
    	Group ietgBank = invgEntryTotals.addMember(Var.NUMERIC|Var.COMP, 10, 2);
    	Group ietgGlInvEnt = invgEntryTotals.addMember(Var.NUMERIC|Var.COMP, 10, 2);
    	Group ietgQoConverted = invgEntryTotals.addMember(Var.NUMERIC|Var.COMP, 5);
    	Group ietgQoEntered = invgEntryTotals.addMember(Var.NUMERIC|Var.COMP, 5);
    	Group ietgLbEntered = invgEntryTotals.addMember(Var.NUMERIC|Var.COMP, 5);
    	Group ietgTots = invgEntryTotals.addMember(Var.NUMERIC|Var.COMP, 12, 2).occurs(21);
    }
    
    private String printArray(String [] array) {
    	StringBuilder sb = new StringBuilder();
    	sb.append("[");
    	for(int i=0; i<array.length; i++) {
    		if(i > 0) sb.append(",");
    		sb.append(array[i]);
    	}
    	sb.append("]");
    	
    	return sb.toString();
    }

    private String printArray(String [][] array) {
    	StringBuilder sb = new StringBuilder();
    	sb.append("[");
    	for(int i=0; i<array.length; i++) {
    		if(i > 0) sb.append(",");
    		sb.append(printArray(array[i]));
    	}
    	sb.append("]");
    	
    	return sb.toString();
    }
    
    
    public void testUnString() throws Exception {
    	String [][] unstringCounts;
    	List<String> delims = new ArrayList<String>();
    	
    	Group s = new Group();
    	Group s1 = s.addMember(Var.CHAR, 200);
    	Group s2 = s.addMember(Var.CHAR, 200);
    	Group s3 = s.addMember(Var.CHAR, 200);
    	Group s4 = s.addMember(Var.CHAR, 200);
    	Group s5 = s.addMember(Var.CHAR, 200);
    	Group s6 = s.addMember(Var.CHAR, 200);
    	Group s7 = s.addMember(Var.CHAR, 200);
    	
    	
    	s1.set("Apples/Pears/Oranges");
    	unstringCounts = s1.unstringDelim(":", s2);
    	assertEquals("Apples/Pears/Oranges", s2.toString());
    	assertEquals("[[200],[],[1]]", printArray(unstringCounts));

    	s1.set("Apples/Pears/Oranges");
    	unstringCounts = s1.unstringDelim(":", s2, s3);
    	assertEquals("Apples/Pears/Oranges", s2.toString());
    	assertEquals("", s3.toString());
    	assertEquals("[[200,0],[,],[1,null]]", printArray(unstringCounts));

    	s1.set("Apples/Pears/Oranges");
    	unstringCounts = s1.unstringDelim("/", s2);
    	assertEquals("Apples", s2.toString());
    	assertEquals("[[6],[/],[1]]", printArray(unstringCounts));
    	
    	s1.set("Peter/Jane/Luke/Kate/Matthew");
    	unstringCounts = s1.unstringDelim("/", s2, s3, s4, s5, s6, s7);
    	assertEquals("Peter", s2.toString());
    	assertEquals("Jane", s3.toString());
    	assertEquals("Luke", s4.toString());
    	assertEquals("Kate", s5.toString());
    	assertEquals("Matthew", s6.toString());
    	assertEquals("", s7.toString());
    	assertEquals("[[5,4,4,4,179,0],[/,/,/,/,,],[5,null,null,null,null,null]]", printArray(unstringCounts));

    	s1.set("Peter|Jane/Luke^|Kate|Matthew");
    	unstringCounts = s1.unstringDelim(new String [] {"/","^|","|"}, s2, s3, s4, s5, s6, s7);
    	assertEquals("Peter", s2.toString());
    	assertEquals("Jane", s3.toString());
    	assertEquals("Luke", s4.toString());
    	assertEquals("Kate", s5.toString());
    	assertEquals("Matthew", s6.toString());
    	assertEquals("", s7.toString());
    	assertEquals("[[5,4,4,4,178,0],[|,/,^|,|,,],[5,null,null,null,null,null]]", printArray(unstringCounts));

//    	System.out.println("s2="+s2.toString());
//    	System.out.println("s3="+s3.toString());
//    	System.out.println("s4="+s4.toString());
//    	System.out.println("s5="+s5.toString());
//    	System.out.println("s6="+s6.toString());
//    	System.out.println("s7="+s7.toString());
//    	System.out.println("unstringCounts="+printArray(unstringCounts));
    	
    	Group wsPipeDollar = new Group();
        	Group filler_048 = wsPipeDollar.addMember(Var.CHAR, 1).set("|");
        	Group filler_049 = wsPipeDollar.addMember(Var.CHAR, 1).set("$");

        Group ustr=new Group(Var.CHAR,100).set("1|$Y|$|$10.00");
        Group ustr1=new Group(Var.CHAR,10);
        Group ustr2=new Group(Var.CHAR,10);
        Group ustr3=new Group(Var.CHAR,10);
        Group ustr4=new Group(Var.CHAR,10);
        Group ustr5=new Group(Var.CHAR,10);
        
        unstringCounts=ustr.unstringDelim(wsPipeDollar.getString(),ustr1,ustr2,ustr3,ustr4,ustr5);

        assertEquals("1", ustr1.toString());
        assertEquals("Y", ustr2.toString());
        assertEquals("", ustr3.toString());
        assertEquals("10.00", ustr4.toString());
        assertEquals("", ustr5.toString());
    }
    
    @SuppressWarnings("unused")
	public void testMoreUnString() throws Exception {
    	Group prntrec = new Group(Var.CHAR,100);
        Group wsDelim1 = new Group(Var.CHAR,10);
        Group wsDelim2 = new Group(Var.CHAR,10);
        Group wsField1 = new Group(Var.CHAR, 132);
        Group wsField2 = new Group(Var.CHAR, 132);
       
        ArrayList<String> unstringdelims = new ArrayList<String>();
        String [][] unstringcounts;
        unstringdelims.clear();
        unstringdelims.add("OP: ");
        prntrec.set("abcdefghij OP: 1234567890");
        unstringcounts = prntrec.unstringDelim(unstringdelims, wsField1, wsField2);

        
        assertEquals("abcdefghij", wsField1.toString());
        assertEquals("1234567890", wsField2.toString());
        assertEquals("11", unstringcounts[0][0]);
        assertEquals("OP: ", unstringcounts[1][0]);
        assertEquals("85", unstringcounts[0][1]);
        assertEquals("", unstringcounts[1][1]);
        assertEquals("2", unstringcounts[2][0]);
  
        wsField1.clear();
        wsField2.clear();
        unstringcounts = prntrec.unstringDelim("OP: ", wsField1, wsField2);

        assertEquals("abcdefghij", wsField1.toString());
        assertEquals("1234567890", wsField2.toString());
        assertEquals("11", unstringcounts[0][0]);
        assertEquals("OP: ", unstringcounts[1][0]);
        assertEquals("85", unstringcounts[0][1]);
        assertEquals("", unstringcounts[1][1]);
        assertEquals("2", unstringcounts[2][0]);
        
        Var v100 = new Var(Var.CHAR,100);

        v100.set("abcde fghij   klm");
        assertEquals("abcde fghij",v100.getDelim("  "));

        Group g100 = new Group(Var.CHAR,100);
        g100.set("abcde fghij   klm");
        assertEquals("abcde fghij",g100.getDelim("  "));
        
        
        Group pomarEndOrd = new Group(Var.CHAR, 10).set(1.00);
        Group wsVariables2 = new Group();
          Group wsOrdNo = wsVariables2.addMember(Var.CHAR, 7);
          Group wsRelNo = wsVariables2.addMember(Var.CHAR, 2);
        Group svBegOrd = wsVariables2.addMember(Var.CHAR, 7);

        svBegOrd.set(1234567);
        assertEquals("1234567", svBegOrd.getString());
        String [][] unstringCounts = pomarEndOrd.unstringDelim("-", wsOrdNo, wsRelNo);
        //Tests that the unstring does not corrupt data in the next field.
        assertEquals("1234567", svBegOrd.getString());
        
    }
    
    @SuppressWarnings("unused")
	public void testRedfeineExtending() throws Exception {
    	Group reportInfo = new Group();
    	Group rX = reportInfo.addMember(Var.CHAR, 100);
    	Group report1_ns = reportInfo.addMember(Var.CHAR, 254);
        Group report1 = report1_ns.redefine();
    	Group xs1 = report1.addMember(Var.CHAR, 154);
    	Group xs2 = report1.addMember(Var.CHAR, 100);
    	Group xs3 = report1.addMember(Var.CHAR, 100); //This one now extends report1 beyond the redfined length
            
    	
    	assertEquals(454, reportInfo.size());
    	assertEquals(254, report1_ns.size());
    	assertEquals(354, report1.size());

    }
    
    public void testOutOfBounds() throws Exception {
    	Group gp22 = new Group();
    	Group gp922 = gp22.addMember(Var.NUMERIC|Var.COMP, 9, 2);
    	Group gp1122 = gp22.addMember(Var.NUMERIC|Var.COMP, 11, 2);
//    	Group gp92o2 = gp22.addMember(Var.NUMERIC|Var.COMP, 9, 2).occurs(2);

    	gp1122.set(895.00);
    	System.out.println("gp1122=" + gp1122.getString() + "<");
    	System.out.println("gp1122=" + gp1122.getDouble() + "<");

    	gp922.set(895.00);
    	System.out.println("gp922=" + gp922.getString() + "<");
    	System.out.println("gp922=" + gp922.getDouble() + "<");

    	gp1122.set(gp922);
    }
    
    @SuppressWarnings("unused")
	public void testMoreFormats() throws Exception {
        Group tpGroup = new Group();
        Group tpRemain = tpGroup.addMember(Var.NUMERIC, 11, 2).format("Z(7).99-");
        Group tpmrRemain = tpGroup.addMember(Var.NUMERIC, 11, 2).format("Z(7).99-");
        Group tpmxRemain = tpmrRemain.redefine().addMember(Var.CHAR, 11);
       
        tpRemain.set(-100);
        tpmrRemain.set(tpRemain);
        
        assertEquals("    100.00-", tpRemain.getString());
        assertEquals("    100.00-", tpmrRemain.getString());
        assertEquals("    100.00-", tpmxRemain.getString());
        
        Group wsTempSurplus = new Group(Var.NUMERIC|Var.COMP, 13, 3).set(125);
        Group sur2_surplusPoint = new Group(Var.UNUMERIC, 9).format("Z(8)9");

        sur2_surplusPoint.set(wsTempSurplus);
        
        assertEquals("00000000125.000", wsTempSurplus.getString());
        assertEquals("      125", sur2_surplusPoint.getString());
        
    }    
    
    @SuppressWarnings("unused")
	public void testCompute() throws Exception {
    	Group.setGroupMode(GroupMode.MICROFOCUS);
    	Group tcg = new Group();
           Group wsIcVal = tcg.addMember(Var.NUMERIC|Var.COMP, 12, 2).set(-1281238136.23);
           Group wsAvgVal = tcg.addMember(Var.NUMERIC|Var.COMP, 14, 2).set(-56710106904.55);
           Group wsAvgCnt = tcg.addMember(Var.UNUMERIC|Var.COMP, 9).set(34);
           
        wsIcVal.compute(wsIcVal.getDouble() + wsAvgVal.getDouble()); //Compute
        wsAvgCnt.compute(wsAvgCnt.getDouble() + 1.0); //Compute
       
        wsIcVal.compute(wsIcVal.getDouble() / wsAvgCnt.getDouble()); //Compute 
        
        assertEquals(-1656895572.59, wsIcVal.getDouble());
        
        Group stNum2 = tcg.addMember(Var.NUMERIC, 17, 2).format("Z,ZZZ,ZZZ,ZZZ.Z(2)-");
        
        Var stVar = new Var(Var.NUMERIC, 14, 2);
        stVar.set(wsIcVal.getDouble());
        assertEquals(-1656895572.59, stVar.getDouble());

        stNum2.set(stVar);
        assertEquals("1,656,895,572.59-", stNum2.getString());
        
        stNum2.set(wsIcVal);
        assertEquals("1,656,895,572.59-", stNum2.getString());
        
         wsIcVal.set(-1239697627.76);
         wsAvgVal.set(-168986167132.04);
         wsAvgCnt.set(13);
        
         wsIcVal.compute(wsIcVal.getDouble() + wsAvgVal.getDouble()); //Compute
         wsAvgCnt.compute(wsAvgCnt.getDouble() + 1.0); //Compute
    
         wsIcVal.compute(wsIcVal.getDouble() / wsAvgCnt.getDouble()); //Compute 
     
         assertEquals(-12158990339.98, wsIcVal.getDouble());
     
         stVar.set(wsIcVal.getDouble());
         assertEquals(-12158990339.98, stVar.getDouble());

         stNum2.set(stVar);
         assertEquals("2,158,990,339.98-", stNum2.getString());
     
         stNum2.set(wsIcVal);
         assertEquals("2,158,990,339.98-", stNum2.getString());
        
        Group oePurFlg = new Group(Var.UNUMERIC|Var.COMP, 1);
        Group oePurFlg2 = new Group(Var.UNUMERIC, 1);
        
        assertFalse(oePurFlg.eq(0));
        assertEquals("2", oePurFlg.getString());     //this is what is DISPLAYed maybe getString() isnt the right method.  Maybe we need display()or perhaps toString() can do it.
        oePurFlg.compute(oePurFlg.getDouble() + 1.0);    //Compute
        assertEquals("3", oePurFlg.getString());
        assertTrue(oePurFlg.eq(33));
        
//        assertFalse(oePurFlg2.eq(0));
//        assertEquals("2", oePurFlg2.getString());
        oePurFlg.compute(oePurFlg2.getDouble() + 1.0);    //Compute
//        assertEquals("3", oePurFlg2.getString());
        assertFalse(oePurFlg2.eq(33));
        assertTrue(oePurFlg2.eq(" "));
    }
    
    public void testDoubles() throws Exception {
    	Group base = new Group();
    	Group dv1 = base.addMember(Var.NUMBER, 12, 3);
    	Group dv2 = base.addMember(Var.NUMBER, 12, 4);
    	Group dv3 = base.addMember(Var.NUMBER, 4, 1);
    	
    	Group wsVariables = new Group();
        Group svCost = wsVariables.addMember(Var.NUMERIC|Var.COMP, 14, 5);
        Group svCostEach = wsVariables.addMember(Var.NUMERIC|Var.COMP, 14, 8);
        svCost.set(779.68);
        svCostEach.set(svCost);
        
        System.out.println("svCost=" + svCost.getString());
        System.out.println("svCostEach=" + svCostEach.getString());
    }
    
    public void testCompCache() {
    	Group pga = new Group();
			Group sga = pga.addGroup();
				Group a1 = sga.addMember(Var.NUMBER|Var.COMP, 9);
				Group a2 = sga.addMember(Var.NUMBER|Var.COMP, 12);
			Group a3 = sga.addMember(Var.NUMBER|Var.COMP, 9);

		Group pgb = new Group();
			Group sgb = pgb.addGroup();
				Group b1 = sgb.addMember(Var.NUMBER|Var.COMP, 9);
				Group b2 = sgb.addMember(Var.NUMBER|Var.COMP, 12);
			Group b3 = sgb.addMember(Var.NUMBER|Var.COMP, 9);
			
		Group pgc = pgb.redefine();
			Group sgc = pgc.addGroup();
				Group c1 = sgc.addMember(Var.NUMBER|Var.COMP, 9);
				Group c2 = sgc.addMember(Var.NUMBER|Var.COMP, 12);
			Group c3 = sgc.addMember(Var.NUMBER|Var.COMP, 9);
			
		a1.set(23);
		a2.set(492);
		a3.set(1783);
		
		pgb.set(pga);
		assertEquals(23, b1.getInt());
		assertEquals(492, b2.getInt());
		assertEquals(1783, b3.getInt());
		
		assertEquals(23, c1.getInt());
		assertEquals(492, c2.getInt());
		assertEquals(1783, c3.getInt());
		
		
    }

}



package com.mssint.jclib;

import com.mssint.jclib.Var;

import junit.framework.TestCase;

public class VarTest extends TestCase {
	public void testInitialiseNines() throws Exception {
	    Var intVar = new Var(-999999999);
	    Var intUnsignedVar = new Var(999999999);
	    Var dblVar = new Var(-99999999999.99);
	    Var dblUnsignedVar = new Var(99999999999.99);
	    Var longVar = new Var(-999999999999999999L);
	    Var longUnsignedVar = new Var(999999999999999999L);
		
	    assertEquals(999999999, intUnsignedVar.getInt());
	    assertEquals("999999999", intUnsignedVar.getString());
	    assertEquals(999999999.0, intUnsignedVar.getDouble());
	    assertEquals(999999999L, intUnsignedVar.getLong());

	    assertEquals(-999999999, intVar.getInt());
	    assertEquals("-999999999", intVar.getString());
	    assertEquals(-999999999.0, intVar.getDouble());
	    assertEquals(-999999999L, intVar.getLong());
		
	    assertEquals(99999999999.99, dblUnsignedVar.getDouble());
	    assertEquals("99999999999.99", dblUnsignedVar.getString());
	    assertEquals(99999999999L, dblUnsignedVar.getLong());
	    assertEquals(999999999, dblUnsignedVar.getInt());
	    
	    assertEquals(-99999999999.99, dblVar.getDouble());
	    assertEquals("-99999999999.99", dblVar.getString());
	    assertEquals(-99999999999L, dblVar.getLong());
	    assertEquals(-999999999, dblVar.getInt());
		
	    assertEquals(999999999, longUnsignedVar.getInt());
	    assertEquals("999999999999999999", longUnsignedVar.getString());
	    assertEquals(999999999999999.0, longUnsignedVar.getDouble());
	    assertEquals(999999999999999999L, longUnsignedVar.getLong());
		
	    assertEquals(-999999999, longVar.getInt());
	    assertEquals("-999999999999999999", longVar.getString());
	    assertEquals(-999999999999999.0, longVar.getDouble());
	    assertEquals(-999999999999999999L, longVar.getLong());
	}

	public void testInitialiseStringNines() throws Exception {
		Var intVar = new Var("-999999999");
		Var intUnsignedVar = new Var("999999999");
		Var dblVar = new Var("-99999999999.99");
		Var dblUnsignedVar = new Var("99999999999.99");
		Var longVar = new Var("-999999999999999999");
		Var longUnsignedVar = new Var("999999999999999999");
	    Var vLongVar = new Var("-999999999999999999999999999999");
	    Var vLongUnsignedVar = new Var("999999999999999999999999999999");
		
		assertEquals(999999999, intUnsignedVar.getInt());
		assertEquals("999999999", intUnsignedVar.getString());
		assertEquals(999999999.0, intUnsignedVar.getDouble());
		assertEquals(999999999L, intUnsignedVar.getLong());

		assertEquals(-999999999, intVar.getInt());
		assertEquals("-999999999", intVar.getString());
		assertEquals(-999999999.0, intVar.getDouble());
		assertEquals(-999999999L, intVar.getLong());
		
		assertEquals(99999999999.99, dblUnsignedVar.getDouble());
		assertEquals("99999999999.99", dblUnsignedVar.getString());
		assertEquals(99999999999L, dblUnsignedVar.getLong());
		assertEquals(999999999, dblUnsignedVar.getInt());
		
		assertEquals(-99999999999.99, dblVar.getDouble());
		assertEquals("-99999999999.99", dblVar.getString());
		assertEquals(-99999999999L, dblVar.getLong());
		assertEquals(-999999999, dblVar.getInt());
		
		assertEquals(999999999, longUnsignedVar.getInt());
		assertEquals("999999999999999999", longUnsignedVar.getString());
		assertEquals(999999999999999.0, longUnsignedVar.getDouble());
		assertEquals(999999999999999999L, longUnsignedVar.getLong());
		
		assertEquals(-999999999, longVar.getInt());
		assertEquals("-999999999999999999", longVar.getString());
		assertEquals(-999999999999999.0, longVar.getDouble());
		assertEquals(-999999999999999999L, longVar.getLong());

	    assertEquals(-999999999, vLongVar.getInt());
	    assertEquals("-999999999999999999999999999999", vLongVar.getString());
	    assertEquals(-999999999999999.0, vLongVar.getDouble());
	    assertEquals(-999999999999999999L, vLongVar.getLong());

	    assertEquals(999999999, vLongUnsignedVar.getInt());
	    assertEquals("999999999999999999999999999999", vLongUnsignedVar.getString());
	    assertEquals(999999999999999.0, vLongUnsignedVar.getDouble());
	    assertEquals(999999999999999999L, vLongUnsignedVar.getLong());
		
		vLongVar.set(".99999999999999999999");
	    assertEquals(0.999999999999999, vLongVar.getDouble());
	    vLongVar.set("9.99999999999999999999");
	    assertEquals(9.99999999999999, vLongVar.getDouble());
	    vLongVar.set("99999999999.99999999999999999999999");
	    assertEquals(99999999999.9999, vLongVar.getDouble());
	    vLongVar.set("99999999999999.99999999999999999999");
	    assertEquals( 99999999999999.9, vLongVar.getDouble());
	    vLongVar.set("999999999999999.9999999999999999999");
	    assertEquals( 999999999999999.0, vLongVar.getDouble());
	}
	
	public void testSetNines() throws Exception {
		Var intVar = new Var(Var.NUMERIC, 9).set(-999999999);
		Var intUnsignedVar = new Var(Var.UNUMERIC, 9);
		Var dblVar = new Var(Var.NUMERIC, 15,2);
		Var dblUnsignedVar = new Var(Var.UNUMERIC, 15,2);
		Var longVar = new Var(Var.NUMERIC, 18);
		Var longUnsignedVar = new Var(Var.UNUMERIC, 18);
		Var vLongVar = new Var(Var.NUMERIC, 30);
		Var vLongUnsignedVar = new Var(Var.UNUMERIC, 30);


		//intVar.set(-999999999);
		intUnsignedVar.set(999999999);
		dblVar.set(-9999999999999.99);
		dblUnsignedVar.set(9999999999999.99);
		longVar.set(-999999999999999999L);
		longUnsignedVar.set(999999999999999999L);
		vLongVar.set(-999999999999999999L);
		vLongUnsignedVar.set(999999999999999999L);
		
		assertEquals(999999999, intUnsignedVar.getInt());
		assertEquals("999999999", intUnsignedVar.getString());
		assertEquals(999999999.0, intUnsignedVar.getDouble());
		assertEquals(999999999L, intUnsignedVar.getLong());

		assertEquals(-999999999, intVar.getInt());
		assertEquals("-999999999", intVar.getString());
		assertEquals(-999999999.0, intVar.getDouble());
		assertEquals(-999999999L, intVar.getLong());
		
		assertEquals(9999999999999.99, dblUnsignedVar.getDouble());
		assertEquals("9999999999999.99", dblUnsignedVar.getString());
		assertEquals(9999999999999L, dblUnsignedVar.getLong());
		assertEquals(999999999, dblUnsignedVar.getInt());
		
		assertEquals(-9999999999999.99, dblVar.getDouble());
		assertEquals("-9999999999999.99", dblVar.getString());
		assertEquals(-9999999999999L, dblVar.getLong());
		assertEquals(-999999999, dblVar.getInt());
		
		assertEquals(999999999, longUnsignedVar.getInt());
		assertEquals("999999999999999999", longUnsignedVar.getString());
		assertEquals(999999999999999.0, longUnsignedVar.getDouble());
		assertEquals(999999999999999999L, longUnsignedVar.getLong());
		
		assertEquals(-999999999, longVar.getInt());
		assertEquals("-999999999999999999", longVar.getString());
		assertEquals(-999999999999999.0, longVar.getDouble());
		assertEquals(-999999999999999999L, longVar.getLong());
	}

	public void testSetStringNines() throws Exception {
	    Var intVar = new Var(Var.CHAR, 10);
	    Var intUnsignedVar = new Var(Var.CHAR, 10);
	    Var dblVar = new Var(Var.CHAR, 15);
	    Var dblUnsignedVar = new Var(Var.CHAR, 15);
	    Var longVar = new Var(Var.CHAR, 19);
	    Var longUnsignedVar = new Var(Var.CHAR, 19);
	    Var vLongVar = new Var(Var.CHAR, 31);
	    Var vLongUnsignedVar = new Var(Var.CHAR, 31);


	    intVar.set("-999999999");
	    intUnsignedVar.set("999999999");
	    dblVar.set("-99999999999.99");
	    dblUnsignedVar.set("99999999999.99");
	    longVar.set("-999999999999999999");
	    longUnsignedVar.set("999999999999999999");
	    vLongVar.set("-999999999999999999999999999999");
	    vLongUnsignedVar.set("999999999999999999999999999999");
	    
	    assertEquals(999999999, intUnsignedVar.getInt());
	    assertEquals("999999999 ", intUnsignedVar.getString());
	    assertEquals(999999999.0, intUnsignedVar.getDouble());
	    assertEquals(999999999L, intUnsignedVar.getLong());

	    assertEquals(-999999999, intVar.getInt());
	    assertEquals("-999999999", intVar.getString());
	    assertEquals(-999999999.0, intVar.getDouble());
	    assertEquals(-999999999L, intVar.getLong());
	    
	    assertEquals(99999999999.99, dblUnsignedVar.getDouble());
	    assertEquals("99999999999.99 ", dblUnsignedVar.getString());
	    assertEquals(99999999999L, dblUnsignedVar.getLong());
	    assertEquals(999999999, dblUnsignedVar.getInt());
	    
	    assertEquals(-99999999999.99, dblVar.getDouble());
	    assertEquals("-99999999999.99", dblVar.getString());
	    assertEquals(-99999999999L, dblVar.getLong());
	    assertEquals(-999999999, dblVar.getInt());
	    
	    assertEquals(999999999, longUnsignedVar.getInt());
	    assertEquals("999999999999999999 ", longUnsignedVar.getString());
	    assertEquals(999999999999999.0, longUnsignedVar.getDouble());
	    assertEquals(999999999999999999L, longUnsignedVar.getLong());
	    
	    assertEquals(-999999999, longVar.getInt());
	    assertEquals("-999999999999999999", longVar.getString());
	    assertEquals(-999999999999999.0, longVar.getDouble());
	    assertEquals(-999999999999999999L, longVar.getLong());

	    assertEquals(-999999999, vLongVar.getInt());
	    assertEquals("-999999999999999999999999999999", vLongVar.getString());
	    assertEquals(-999999999999999.0, vLongVar.getDouble());
	    assertEquals(-999999999999999999L, vLongVar.getLong());

	    assertEquals(999999999, vLongUnsignedVar.getInt());
	    assertEquals("999999999999999999999999999999 ", vLongUnsignedVar.getString());
	    assertEquals(999999999999999.0, vLongUnsignedVar.getDouble());
	    assertEquals(999999999999999999L, vLongUnsignedVar.getLong());
	}
	
	public void testTruncation() throws Exception {
		Var smallNumber = new Var(Var.NUMERIC, 4);
		Var largeNumber = new Var(Var.NUMERIC, 12);
		Var smallDouble = new Var(Var.NUMERIC, 6,2);
		Var largeDouble = new Var(Var.NUMERIC, 14,3);
		Var smallString = new Var(Var.CHAR, 5);
		Var largeString = new Var(Var.CHAR, 20);
		
		assertEquals(0, smallNumber.getInt());
		
		largeNumber.set(32123456789L);
		smallNumber.set(largeNumber);
	    assertEquals(6789, smallNumber.getInt());
		smallNumber.set(12345678);
	    assertEquals(5678, smallNumber.getInt());
		smallString.set(largeNumber);
	
	    assertEquals("03212", smallString.getString());
		smallDouble.set(largeNumber);
	    assertEquals(6789.0, smallDouble.getDouble());
	    largeDouble.set(largeNumber);
	    assertEquals(32123456789.0, largeDouble.getDouble());
		largeString.set(largeDouble);
		assertEquals("32123456789000      ", largeString.getString());
		
		largeDouble.set(123.456);
	    largeString.set(largeDouble);
	    assertEquals("00000000123456", largeString.getTrim());
		
		smallDouble.set(123456789.12899);
	    assertEquals(6789.12, smallDouble.getDouble());
		largeDouble.set(123456789.12599);
	    assertEquals(123456789.125, largeDouble.getDouble());
		
		largeDouble.set(-123456789.12899);
	    assertEquals(-123456789.128, largeDouble.getDouble());
	    assertEquals("-123456789.128", largeDouble.getString(false));
	    assertEquals("-00123456789.128", largeDouble.getString());
	    largeString.set(largeDouble);
	    assertEquals("00123456789128      ", largeString.getString());
		
		smallDouble.set(largeDouble);
	    assertEquals(-6789.12, smallDouble.getDouble());
	    largeString.set(smallDouble);
	    assertEquals("678912              ", largeString.getString());
		
		largeNumber.set(largeDouble);
	    assertEquals(-123456789, largeNumber.getLong());
	    assertEquals("-123456789", largeNumber.getString(false));
	    assertEquals("-000123456789", largeNumber.getString());
		smallNumber.set(largeNumber);
	    assertEquals(-6789, smallNumber.getInt());
	    assertEquals("-6789", smallNumber.getString());
		largeString.set(smallNumber);
	    assertEquals("6789                ", largeString.getString());
	}

	public void testAssignment() throws Exception {
	    Var smallNumber = new Var(Var.NUMERIC, 4);
	    Var largeNumber = new Var(Var.NUMERIC, 12);
	    Var smallDouble = new Var(Var.NUMERIC, 6,2);
	    Var largeDouble = new Var(Var.NUMERIC, 12,3);
	    Group G = new Group();
	    Group GsmallNumber = G.addMember(Var.NUMERIC, 4);
	    Group GlargeNumber = G.addMember(Var.NUMERIC, 12);
	    Group GsmallDouble = G.addMember(Var.NUMERIC, 6,2);
//	    Group GlargeDouble = G.addMember(Var.NUMERIC, 12,3);
	    
	    Var char2 = new Var(Var.CHAR, 2);
	    Var char4 = new Var(Var.CHAR, 4);
	    
	    char2.set("12");
	    char4.set("12  ");

	    GsmallNumber.set("12  ");
	    assertEquals("1200", GsmallNumber.getString());
	    assertEquals(1200, GsmallNumber.getInt());
	    GsmallNumber.set(char4);
	    assertEquals("1200", GsmallNumber.getString());
	    assertEquals(1200, GsmallNumber.getInt());
	    GlargeNumber.set(GsmallNumber);
	    assertEquals("000000001200", GlargeNumber.getString());
	    assertEquals(1200, GlargeNumber.getLong());

	    char4.set(GsmallNumber);
	    assertEquals("1200", char4.getString());
	    
	    GsmallNumber.set(char2);
	    assertEquals("0012", GsmallNumber.getString());
	    assertEquals(12, GsmallNumber.getInt());
	    GsmallNumber.set("12");
	    assertEquals("0012", GsmallNumber.getString());
	    assertEquals(12, GsmallNumber.getInt());

	    char4.set("  12");
	    GsmallNumber.set(char4);
	    assertEquals("0012", GsmallNumber.getString());
	    assertEquals(12, GsmallNumber.getInt());
	    GsmallNumber.set("  12");
	    assertEquals("0012", GsmallNumber.getString());
	    assertEquals(12, GsmallNumber.getInt());
	    
	    
	    GsmallNumber.set("12");
	    assertEquals("0012", GsmallNumber.getString());
	    assertEquals(12, GsmallNumber.getInt());
	    
	    
	    largeNumber.set(654321.09);
	    assertEquals("000000654321", largeNumber.getString());
	    GlargeNumber.set(654321.09);
	    assertEquals("000000654321", GlargeNumber.getString());

		smallNumber.set(123.12);
	    assertEquals(123, smallNumber.getInt());
	    smallNumber.set(123);
	    assertEquals("0123", smallNumber.getString());
	    smallNumber.set(1234567890123456L);
	    assertEquals(3456, smallNumber.getInt());
	    smallNumber.set("123.12");
	    assertEquals(123, smallNumber.getInt());
	    smallNumber.set("-123.12");
	    assertEquals(-123, smallNumber.getInt());
	    smallNumber.set("12s");
	    assertEquals(-123, smallNumber.getInt());
	    smallNumber.set("123.1r");
	    assertEquals(-123, smallNumber.getInt());

	    GsmallNumber.set(123.12);
	    assertEquals(123, GsmallNumber.getInt());
	    GsmallNumber.set(123);
	    assertEquals("0123", GsmallNumber.getString());
	    GsmallNumber.set(1234567890123456L);
	    assertEquals(3456, GsmallNumber.getInt());
	    GsmallNumber.set("123.12");
	    assertEquals(3, GsmallNumber.getInt());
	    GsmallNumber.set("-123.12");
	    assertEquals(3, GsmallNumber.getInt());
	    GsmallNumber.set("12s");
	    assertEquals(-123, GsmallNumber.getInt());
	    GsmallNumber.set("123.1r");
	    assertEquals(-3, GsmallNumber.getInt());
		
		largeNumber.set(123456789.987);
	    assertEquals(123456789L, largeNumber.getLong());
		largeNumber.set(123456789012345678L);
	    assertEquals(789012345678L, largeNumber.getLong());
		largeNumber.set("123456789.987");
	    assertEquals(123456789L, largeNumber.getLong());
		largeNumber.set("-123456789.987");
	    assertEquals(-123456789L, largeNumber.getLong());
		largeNumber.set("123456789.98w");
	    assertEquals(-123456789L, largeNumber.getLong());
		largeNumber.set("12345678y");
	    assertEquals(-123456789L, largeNumber.getLong());
		
		GlargeNumber.set(123456789.987);
	    assertEquals(123456789L, GlargeNumber.getLong());
		GlargeNumber.set(123456789012345678L);
	    assertEquals(789012345678L, GlargeNumber.getLong());
		GlargeNumber.set("123456789.987");
	    assertEquals(23456789L, GlargeNumber.getLong());
		GlargeNumber.set("-123456789.987");
	    assertEquals(23456789L, GlargeNumber.getLong());
		GlargeNumber.set("123456789.98w");
	    assertEquals(-23456789L, GlargeNumber.getLong());
		GlargeNumber.set("12345678y");
	    assertEquals(-123456789L, GlargeNumber.getLong());
		
		smallDouble.set(123456.12789);
	    assertEquals(3456.12, smallDouble.getDouble());
	    smallDouble.set(-123456.12789);
	    assertEquals(-3456.12, smallDouble.getDouble());
		smallDouble.set(123456789123456L);
	    assertEquals(3456.0, smallDouble.getDouble());
	    smallDouble.set(-123456789123456L);
	    assertEquals(-3456.0, smallDouble.getDouble());
		smallDouble.set(123);
	    assertEquals(123.0, smallDouble.getDouble());
		smallDouble.set("-1.2");
	    assertEquals(-1.2, smallDouble.getDouble());
		smallDouble.set("1.r");
	    assertEquals(-1.2, smallDouble.getDouble());
		
		GsmallDouble.set(123456.12789);
	    assertEquals(3456.12, GsmallDouble.getDouble());
	    GsmallDouble.set(-123456.12789);
	    assertEquals(-3456.12, GsmallDouble.getDouble());
		GsmallDouble.set(123456789123456L);
	    assertEquals(3456.0, GsmallDouble.getDouble());
	    GsmallDouble.set(-123456789123456L);
	    assertEquals(-3456.0, GsmallDouble.getDouble());
		GsmallDouble.set(123);
	    assertEquals(123.0, GsmallDouble.getDouble());
		GsmallDouble.set("-1.2");
	    assertEquals(-1.2, GsmallDouble.getDouble());
	    //TODO - following is undecided
//		GsmallDouble.set("1.r");
//	    assertEquals(-1.2, GsmallDouble.getDouble());
		
	    smallDouble.set("123456.12");
	    assertEquals(3456.12, smallDouble.getDouble());
	    smallDouble.set("123456.1r");
	    assertEquals(-3456.12, smallDouble.getDouble());
	    smallDouble.set("123456.12s");
	    assertEquals(-3456.12, smallDouble.getDouble());
	    smallDouble.set("123456789p");
	    assertEquals(-7890.0, smallDouble.getDouble());
		smallDouble.set("-1234567890");
	    assertEquals(-7890.0, smallDouble.getDouble());
		
	    GsmallDouble.set("123456.12");
	    assertEquals(456.12, GsmallDouble.getDouble());
	    GsmallDouble.set("123456.1r");
	    assertEquals(-456.12, GsmallDouble.getDouble());
	    GsmallDouble.set("123456.12s");
	    assertEquals(-56.12, GsmallDouble.getDouble());
	    //TODO uncertain
//	    GsmallDouble.set("123456789p");
//	    assertEquals(-7890.0, GsmallDouble.getDouble());
//		GsmallDouble.set("-1234567890");
//	    assertEquals(-7890.0, GsmallDouble.getDouble());
		
		largeDouble.set(12345678901.458);
	    assertEquals(345678901.458, largeDouble.getDouble());
		largeDouble.set("12345678901.4588");
	    assertEquals(345678901.458, largeDouble.getDouble());
	    largeDouble.set(12345678901234567L);
	    System.out.println("largeDouble.getDouble()=" + largeDouble.getString());
	    assertEquals(901234567.000, largeDouble.getDouble());
	    largeDouble.set(-12345678901234567L);
	    assertEquals(-901234567.000, largeDouble.getDouble());
		largeDouble.set("123456789p");
	    assertEquals(-234567890.0, largeDouble.getDouble());
		largeDouble.set("-123456789");
	    assertEquals(-123456789.0, largeDouble.getDouble());
	    largeDouble.set("123456789.1599p");
	    assertEquals(-123456789.159, largeDouble.getDouble());
		largeDouble.set("-123456789.1599");
	    assertEquals(-123456789.159, largeDouble.getDouble());
	  	largeNumber.set("AA091629");
    	assertEquals(11091629,largeNumber.getLong());
    	
    	Var [] SA_PAY_GST = Var.createArray(6, Var.UNUMERIC, 10, 2);
        Group SP_GWSAV = new Group();
        Group SE_PAY_GST = SP_GWSAV.addMember(Var.CHAR, 60);
        
        SA_PAY_GST[1].set(12345678.91);
        SA_PAY_GST[2].set(22345678.91);
        SA_PAY_GST[3].set(32345678.91);
        SA_PAY_GST[4].set(42345678.91);
        SA_PAY_GST[5].set(52345678.91);
        SA_PAY_GST[6].set(62345678.91);

        for(int i=0;i<6;i++) {
        	SE_PAY_GST.setSubstr(SA_PAY_GST[i+1],(i * 10) + 1);
        }
        assertEquals("123456789122345678913234567891423456789152345678916234567891", SE_PAY_GST.getString());
        
        Var [] SA_PAY_GST_2 = Var.createArray(6, Var.UNUMERIC, 10);
        SA_PAY_GST_2[1].set(1234567891);
        SA_PAY_GST_2[2].set(2234567891L);
        SA_PAY_GST_2[3].set(3234567891L);
        SA_PAY_GST_2[4].set(4234567891L);
        SA_PAY_GST_2[5].set(5234567891L);
        SA_PAY_GST_2[6].set(6234567891L);

        for(int i=0;i<6;i++) {
        	SE_PAY_GST.setSubstr(SA_PAY_GST_2[i+1],(i * 10) + 1);
        }
        assertEquals("123456789122345678913234567891423456789152345678916234567891", SE_PAY_GST.getString());
        
        Var [] SA_PAY_GST_3 = Var.createArray(6, Var.UNUMERIC, 10);
        SA_PAY_GST_3[1].set("1234567891");
        SA_PAY_GST_3[2].set("2234567891");
        SA_PAY_GST_3[3].set("3234567891");
        SA_PAY_GST_3[4].set("4234567891");
        SA_PAY_GST_3[5].set("5234567891");
        SA_PAY_GST_3[6].set("6234567891");

        for(int i=0;i<6;i++) {
        	SE_PAY_GST.setSubstr(SA_PAY_GST_3[i+1],(i * 10) + 1);
        }
        assertEquals("123456789122345678913234567891423456789152345678916234567891", SE_PAY_GST.getString());
        
        SE_PAY_GST.setFromArray(SA_PAY_GST,6);
        assertEquals("123456789122345678913234567891423456789152345678916234567891", SE_PAY_GST.getString());
        
        for(int i=0;i<6;i++) {
            SA_PAY_GST[i+1].setMva(SE_PAY_GST, i);
        }
        assertEquals(12345678.91,SA_PAY_GST[1].getDouble());    
        assertEquals(22345678.91,SA_PAY_GST[2].getDouble());
        assertEquals(32345678.91,SA_PAY_GST[3].getDouble());
        assertEquals(42345678.91,SA_PAY_GST[4].getDouble());
        assertEquals(52345678.91,SA_PAY_GST[5].getDouble());
        assertEquals(62345678.91,SA_PAY_GST[6].getDouble());
        
        SE_PAY_GST.setFromArray(SA_PAY_GST_2,6);
        assertEquals("123456789122345678913234567891423456789152345678916234567891", SE_PAY_GST.getString());
        
        for(int i=0;i<6;i++) {
            SA_PAY_GST_2[i+1].setMva(SE_PAY_GST, i);
        }
        assertEquals(1234567891,SA_PAY_GST_2[1].getLong());    
        assertEquals(2234567891L,SA_PAY_GST_2[2].getLong());
        assertEquals(3234567891L,SA_PAY_GST_2[3].getLong());
        assertEquals(4234567891L,SA_PAY_GST_2[4].getLong());
        assertEquals(5234567891L,SA_PAY_GST_2[5].getLong());
        assertEquals(6234567891L,SA_PAY_GST_2[6].getLong());
        
        SE_PAY_GST.setFromArray(SA_PAY_GST_3,6);
        assertEquals("123456789122345678913234567891423456789152345678916234567891", SE_PAY_GST.getString());
        
        for(int i=0;i<6;i++) {
            SA_PAY_GST_3[i+1].setMva(SE_PAY_GST, i);
        }
        assertEquals("1234567891",SA_PAY_GST_3[1].getString());    
        assertEquals("2234567891",SA_PAY_GST_3[2].getString());
        assertEquals("3234567891",SA_PAY_GST_3[3].getString());
        assertEquals("4234567891",SA_PAY_GST_3[4].getString());
        assertEquals("5234567891",SA_PAY_GST_3[5].getString());
        assertEquals("6234567891",SA_PAY_GST_3[6].getString());
        
        SE_PAY_GST.set("");
        for(int i=0;i<6;i++) {
            SA_PAY_GST[i+1].setMva(SE_PAY_GST, i);
        }
        
        assertEquals(0.0,SA_PAY_GST[1].getDouble());    
        assertEquals(0.0,SA_PAY_GST[2].getDouble());
        assertEquals(0.0,SA_PAY_GST[3].getDouble());
        assertEquals(0.0,SA_PAY_GST[4].getDouble());
        assertEquals(0.0,SA_PAY_GST[5].getDouble());
        assertEquals(0.0,SA_PAY_GST[6].getDouble());
        
        Var ZERO = new Var(Var.NUMERIC|Var.AUTOVAR, 0); // numeric(0)
        Var ZEROS = ZERO;
        assertEquals("000000000",ZEROS.substr(0, 9));
    	//TODO uncertain
    	/*
		GlargeDouble.set(12345678901.458);
	    assertEquals(345678901.458, GlargeDouble.getDouble());
		GlargeDouble.set("12345678901.4588");
	    assertEquals(345678901.458, GlargeDouble.getDouble());
	    GlargeDouble.set(12345678901234567L);
	    assertEquals(901234567.0, GlargeDouble.getDouble());
	    GlargeDouble.set(-12345678901234567L);
	    assertEquals(-901234567.0, GlargeDouble.getDouble());
		GlargeDouble.set("123456789p");
	    assertEquals(-234567890.0, GlargeDouble.getDouble());
		GlargeDouble.set("-123456789");
	    assertEquals(-123456789.0, GlargeDouble.getDouble());
	    GlargeDouble.set("123456789.1599p");
	    assertEquals(-123456789.159, GlargeDouble.getDouble());
		GlargeDouble.set("-123456789.1599");
	    assertEquals(-123456789.159, GlargeDouble.getDouble());
	  	GlargeNumber.set("AA091629");
    	assertEquals(11091629,GlargeNumber.getLong());
    	*/
        Group SD_VALOR = new Group();
	    	Group SD_VL1 = SD_VALOR.addMember(Var.CHAR, 9);
	    	Group SD_VG = SD_VALOR.addMember(Var.CHAR, 1);// -
	    	Group SD_DEC = SD_VALOR.addMember(Var.CHAR, 2);
	    Var SD_TOTAIS = new Var(Var.UNUMERIC,12,2).set(123.45);
	    SD_VL1.setSubstr(SD_TOTAIS.substr(2 - 1, 9), 1);
        SD_DEC.setSubstr(SD_TOTAIS.substr(11 - 1, 2), 1);
        SD_VG.set(",");
        assertEquals("000000123,45",SD_VALOR.getString());
	}
	
	@SuppressWarnings("unused")
	public void testRounding() throws Exception {
		Var v = new Var(Var.NUMBER, 10,6);
		Var r = new Var(Var.NUMBER, 12, 6);
		v.set(1.00933);
		r.set(v.power(47).getDouble() - 1);
		assertEquals("000000.547246", r.getString());

		Var SD_INT = new Var(Var.NUMERIC,17,2);
		Var SD_INT1 = new Var(Var.NUMERIC,18,3);
		
		SD_INT1.set(0);
	
		SD_INT1.set(1025.975);
		SD_INT.set(new Var(1 * SD_INT1.getDouble()).roundDouble(2));// AH-F5191
		assertEquals("000000000001025.98", SD_INT.getString());
		
		SD_INT1.set(1025.976);
		SD_INT.set(new Var(1 * SD_INT1.getDouble()).roundDouble(2));// AH-F5191
		assertEquals("000000000001025.98", SD_INT.getString());

		Var A = new Var(Var.NUMBER, 15,2);
		Var B = new Var(Var.NUMBER, 15,2);
		Var X = new Var(Var.NUMBER, 15,6);
		Var C = new Var(Var.NUMBER, 15,2);
		Group G = new Group();
			Group GA = G.addMember(Var.NUMBER, 15,2);
			Group GB = G.addMember(Var.NUMBER, 15,2);
			Group GC = G.addMember(Var.NUMBER, 15,2);
		
		B.set(3608.25);
		C.set(3608.17);
		A.set(B.getDouble() - C.getDouble());
		assertEquals("0000000000000.08", A.getString());
		GB.set(3608.25);
		GC.set(3608.17);
		GA.set(GB.getDouble() - GC.getDouble());
		assertEquals("000000000000008", GA.getString());
		
		B.set(221337.07);
		C.set(222127.50);
		A.set(B.getDouble() - C.getDouble());
		assertEquals("-0000000000790.43", A.getString());
		GB.set(221337.07);
		GC.set(222127.50);
		GA.set(GB.getDouble() - GC.getDouble());
		assertEquals("00000000007904s", GA.getString());
		
		B.set(2882592.65);
		C.set(-2882210.33);
		A.set(B.getDouble() + C.getDouble());
		assertEquals("0000000000382.32", A.getString());
		GB.set(2882592.65);
		GC.set(-2882210.33);
		GA.set(GB.getDouble() + GC.getDouble());
		assertEquals("000000000038232", GA.getString());
		
		B.set(167699.39);
		C.set(-167752.41);
		A.set(B.getDouble() + C.getDouble());
		assertEquals("-0000000000053.02", A.getString());
		GB.set(167699.39);
		GC.set(-167752.41);
		GA.set(GB.getDouble() + GC.getDouble());
		assertEquals("00000000000530r", GA.getString());
		
		X.set(7.193455);
		C.set(-437000.0);
		A.set(X.getDouble() * C.getDouble());
		assertEquals("-0000003143539.83", A.getString());
		Var XY = new Var(X.getDouble() * C.getDouble());
		A.set(new Var(X.getDouble() * C.getDouble()).roundDouble(2));
		assertEquals("-0000003143539.84", A.getString());
		
		Var SD_IZNROUND = new Var(Var.UNUMERIC,17).set(0);
        Var CST_STO = new Var(Var.UNUMERIC,3).set(100);
        Var IZNPLA = new Var(Var.NUMERIC,17,2).set(1393000000);
        
        SD_IZNROUND.set(new Var(CST_STO.getDouble() * IZNPLA.getDouble() / CST_STO.getDouble()).round());
        assertEquals(1393000000, SD_IZNROUND.getLong());
       
       
        

	}

	public void testInitialValues() throws Exception {
		Var aNumber = new Var(Var.NUMERIC, 12);
		Var aDouble = new Var(Var.NUMERIC, 12,2);
		Var aString = new Var(Var.CHAR, 10);

	    assertEquals(0.0, aNumber.getDouble());
	    assertEquals(0.0, aDouble.getDouble());
	    assertEquals(0.0, aString.getDouble());

	    assertEquals(0, aNumber.getLong());
	    assertEquals(0, aDouble.getLong());
	    assertEquals(0, aString.getLong());
	
	    assertEquals(0, aNumber.getInt());
	    assertEquals(0, aDouble.getInt());
	    assertEquals(0, aString.getInt());
		
	    assertEquals(0.0F, aNumber.getFloat());
	    assertEquals(0.0F, aDouble.getFloat());
	    assertEquals(0.0F, aString.getFloat());
		
	    assertEquals("000000000000", aNumber.getLincString());
	    assertEquals("000000000000", aDouble.getLincString());
	    assertEquals("          ", aString.getLincString());
		
	    assertEquals("0", aNumber.getTrim());
	    assertEquals("0.0", aDouble.getTrim());
	    assertEquals("", aString.getTrim());
		
	    assertEquals("0000", aNumber.substr(2,4));
	    assertEquals("0000", aDouble.substr(2,4));
	    assertEquals("    ", aString.substr(2,4));
		
	    assertEquals("000000000000", aNumber.getString());
	    assertEquals("0000000000.00", aDouble.getString());
	    assertEquals("          ", aString.getString());
	}
    public void testLargeNumber() throws Exception {
            Var aNumber = new Var(Var.NUMERIC, 18);
            Var aString = new Var(Var.CHAR,18);
            
            aNumber.set(128311328893042931L);
            aString.set(aNumber);
            
            assertEquals(128311328893042931L,aNumber.getLong());
            assertEquals("128311328893042931",aString.getString());
    }
    
    public void testForSteve() throws Exception {
    	Var xx = new Var(Var.CHAR,20);
    	Var yy = new Var(Var.CHAR,20);
        xx.set("THURSDAY");
        Var wsTally = new Var(Var.UNUMBER,6);
        wsTally.set(0);        
        wsTally.set(xx.getString().indexOf(" ")); 
        assertEquals(8,wsTally.getInt());
        wsTally.set(wsTally.getLong() + 3);
        yy.set("2007 11 25");
        wsTally.set(xx.setPart(wsTally, yy.getString()));
        assertEquals("THURSDAY  2007 11 25",xx.getString());
        
        Var n10 = new Var(Var.NUMBER, 10);
        n10.set(123);
        
        assertEquals("0000000123", n10.toString());
        assertEquals("0000000123", n10.getString());
        assertEquals("0000000123", n10.getString(true));
        assertEquals("123", n10.getString(false));

        n10.set(0);
        assertEquals("0000000000", n10.toString());
        assertEquals("0000000000", n10.getString());
        assertEquals("0000000000", n10.getString(true));
        assertEquals("0", n10.getString(false));
        
    }
    
    public void testPicture() throws Exception {
    	Var v1 = new Var(Var.NUMBER, 6, 2).format("---9.99");
    	v1.set("-2.02");
    	assertEquals("  -2.02", v1.getString());
    	v1.set("-1204.2");
    	assertEquals("-204.20", v1.getString());
    	v1.set("1204.239");
    	assertEquals(" 204.23", v1.getString());
    	v1.set(0);
    	assertEquals("   0.00", v1.getString());
 
    	

    	Var v2 = new Var(Var.NUMBER, 7,2).format("-999.99");
    	v2.set("-2.02");
    	assertEquals("-002.02", v2.getString());
    	v2.set("-1204.2");
    	assertEquals("-204.20", v2.getString());
    	v2.set("1204.239");
    	assertEquals(" 204.23", v2.getString());
    	v2.set(0);
    	assertEquals(" 000.00", v2.getString());
    	
    	Var v3 = new Var(Var.NUMBER, 7,2).format("-ZZ9.99");
    	v3.set("-2.02");
    	assertEquals("-  2.02", v3.getString());
    	v3.set("-1204.2");
    	assertEquals("-204.20", v3.getString());
    	v3.set("1204.239");
    	assertEquals(" 204.23", v3.getString());
    	v3.set(0);
    	assertEquals("   0.00", v3.getString());
    	
    	Var v4 = new Var(Var.NUMBER, 10).format("9999/99/99");
    	v4.set("70924");
    	assertEquals("0007/09/24", v4.getString());
    	v4.set(0);
    	assertEquals("0000/00/00", v4.getString());
    	v4.set(20081127);
    	assertEquals("2008/11/27", v4.getString());
    	v4.set(-19840601);
    	assertEquals("1984/06/01", v4.getString());
    	
    	Var v5 = new Var(Var.NUMBER, 11).format("bz(05)9bbbb");
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
    	
    	
    	
    	Var v6 = new Var(Var.NUMBER, 6).format("ZZ,ZZ9");
    	v6.set(0);
    	assertEquals("     0", v6.getString());
    	v6.set("  123");
    	assertEquals("   123", v6.getString());
    	v6.set("12045");
    	assertEquals("12,045", v6.getString());
    	v6.set(123456);
    	assertEquals("23,456", v6.getString());
    	
    	Var v7 = new Var(Var.NUMBER, 9).format("Z,ZZZ,ZZZ");
    	v7.set(0);
    	assertEquals("         ", v7.getString());
    	v7.set(23456);
    	assertEquals("   23,456", v7.getString());
    	v7.set(123456789);
    	assertEquals("3,456,789", v7.getString());
    	
    	Var v8 = new Var(Var.NUMBER, 3).format("-999");
    	v8.set(0);
    	assertEquals(" 000", v8.getString());
    	v8.set(345);
    	assertEquals(" 345", v8.getString());
    	v8.set(-24);
    	assertEquals("-024", v8.getString());
    	
       	Var v9 = new Var(Var.NUMBER, 10).format("z9/99/9999");
    	v9.set(0);
    	assertEquals(" 0/00/0000", v9.getString());
    	v9.set(1012008);
    	assertEquals(" 1/01/2008", v9.getString());
    	v9.set(25111960);
    	assertEquals("25/11/1960", v9.getString());
    	
      	Var v10 = new Var(Var.UNUMERIC,5).format("Z9:99");
    	v10.set(0);
    	assertEquals(" 0:00", v10.getString());
    	v10.set(123);
    	assertEquals(" 1:23", v10.getString());
    	v10.set(1234);
    	assertEquals("12:34", v10.getString());
    	
      	Var v11 = new Var(Var.UNUMERIC,9,2).format("ZZZZZ9.99");
    	v11.set(0);
    	assertEquals("     0.00", v11.getString());
    	v11.set(123);
    	assertEquals("   123.00", v11.getString());
    	v11.set(1234.99);
    	assertEquals("  1234.99", v11.getString());
    	
    	Var v12 = new Var(Var.UNUMERIC,6).format("Z(6)").occurs(10);
    	v12.index(1).set(0);
    	assertEquals("      ", v12.index(1).getString());
    	v12.index(1).set(123);
    	assertEquals("   123", v12.index(1).getString());
    	v12.index(1).set(1234.99);
    	assertEquals("  1234", v12.index(1).getString());
    	v12.index(9).set(0);
    	assertEquals("      ", v12.index(9).getString());
    	v12.index(9).set(123);
    	assertEquals("   123", v12.index(9).getString());
    	v12.index(9).set(1234.99);
    	assertEquals("  1234", v12.index(9).getString());
    	assertEquals("      ", v12.index(2).getString());
    	assertEquals("      ", v12.index(3).getString());
    	assertEquals("      ", v12.index(4).getString());
    	assertEquals("      ", v12.index(5).getString());
    	
    }
    
    public void test_bwz() throws Exception {
    	Var v1 = new Var(Var.NUMBER, 5).format("z9999").bwz();
    	Var v2 = new Var(Var.NUMBER, 5).bwz();
    	Var v3 = new Var(Var.NUMBER, 6).format("ZZ,Z99").bwz();
    	
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
    
    public void testReplace() {
    	Var v1 = new Var(Var.CHAR, 20);
    	Group g = new Group();
    	Group g1 = g.addMember(Var.CHAR, 20);
    	
    	v1.replace(3, 2, "EEE");
    	assertEquals("   EE               ", v1.getString());
    	v1.replace(4, 1, "1");
    	assertEquals("   E1               ", v1.getString());
    	v1.replace(5, 5, "D");
    	assertEquals("   E1DDDDD          ", v1.getString());

    	g1.replace(3, 2, "EEE");
    	assertEquals("   EE               ", g1.getString());
    	g1.replace(4, 1, "1");
    	assertEquals("   E1               ", g1.getString());
    	g1.replace(5, 5, "D");
    	assertEquals("   E1DDDDD          ", g1.getString());
    }
    
    public void testDetach() {
		Glb glb = new Glb();
    	Var w = new Var(Var.CHAR, 20);
    	Group g = new Group();
    	Group x = g.addMember(Var.CHAR, 20);
    	
    	w.set("apple pear");
    	x.set("apple pear");
    	assertEquals("apple", w.detach(null, 1, " "));
    	assertEquals("apple", x.detach(null, 1, " "));

    	w.set("apple\rpear");
    	x.set("apple\rpear");
    	assertEquals("apple", w.detach(null, 1, "\r"));
    	assertEquals("apple", x.detach(null, 1, "\r"));

    	w.set("apple+-pear");
    	x.set("apple+-pear");
    	assertEquals("apple", w.detach(null, 1, "+-"));
    	assertEquals("apple", x.detach(null, 1, "+-"));
  
		String d = "\r\n-";
		Var v = new Var(Var.CHAR, 100);
		v.set("apple"+d+"pear"+d+d+"peach");
		assertEquals("apple", v.detach(glb, 1, d));
		assertEquals(true, glb.STATUS.eq(" "));
		assertEquals(9, glb.detachOffset);
		assertEquals(5, glb.LENGTH.getInt());
		
		assertEquals("pear", v.detach(glb, 9, d));
		assertEquals(true, glb.STATUS.eq(" "));
		assertEquals(19, glb.detachOffset);
		assertEquals(4, glb.LENGTH.getInt());

    }
    
   @SuppressWarnings("unused")
   public void testComparison() {
    	Group g = new Group();
    		Group gD = g.addMember(Var.NUMERIC, 12,2);
    		Group gL = g.addMember(Var.NUMBER, 10);
    		Group gS = g.addMember(Var.CHAR, 10);
    	Var vD = new Var(Var.NUMBER, 12,2);
		Var vL = new Var(Var.NUMBER, 10);
		Var vS = new Var(Var.CHAR, 10);
		double dv;
		long lv;
		String sv;
		
		//String comparisons
		gS.set("12    ");
		vS.set("142    ");
		assertEquals(true, gS.eq("12                          "));
		assertEquals(true, vS.eq("142                         "));
		assertEquals(false, gS.eq(" 12                        "));
		assertEquals(false, vS.eq(" 142                       "));
		assertEquals(true, gS.eq("12"));
		assertEquals(true, vS.eq("142"));
		assertEquals(false, gS.eq(" 12"));
		assertEquals(false, vS.eq(" 142"));
		assertEquals(false, vS.eq(gS));
		assertEquals(false, gS.eq(vS));
		gS.set(142);
		assertEquals(true, vS.eq(gS));
		assertEquals(true, gS.eq(vS));
		gS.set("0000000142");
		vS.set("0000000142");
		assertEquals(false, vS.eq(142));
		assertEquals(false, gS.eq(142));
		assertEquals(false, vS.eq(142.0));
		assertEquals(false, gS.eq(142.0));

		gS.set("142");
		vS.set("142");
		assertEquals(true, vS.eq(142));
		assertEquals(true, gS.eq(142));
		assertEquals(false, vS.eq(142.0));
		assertEquals(false, gS.eq(142.0));

		gS.set("142.0");
		vS.set("142.0");
		assertEquals(false, vS.eq(142));
		assertEquals(false, gS.eq(142));
		assertEquals(true, vS.eq(142.0));
		assertEquals(true, gS.eq(142.0));

		
		gS.set("Banana");
		vS.set("Grape");
		assertEquals(true, gS.compareTo("Apple") > 0);
		assertEquals(true, gS.compareTo("Banana") == 0);
		assertEquals(true, gS.compareTo("Pear") < 0);
		assertEquals(true, vS.compareTo("Apple") > 0);
		assertEquals(true, vS.compareTo("Grape") == 0);
		assertEquals(true, vS.compareTo("Pear") < 0);

		gL.set(11);
		vL.set(100);
		assertEquals(true, gL.compareTo("0000000012") < 0);
		assertEquals(true, gL.compareTo("0000000010") > 0);
		assertEquals(true, vL.compareTo("0000000112") < 0);
		assertEquals(true, vL.compareTo("0000000010") > 0);

		assertEquals(true, gL.compareTo(12) < 0);
		assertEquals(true, gL.compareTo(10) > 0);
		assertEquals(true, vL.compareTo(112) < 0);
		assertEquals(true, vL.compareTo(10) > 0);
		
		gD.set(11.32);
		vD.set(100.17);
		/* This is not a series of tests relevant to LINC
		assertEquals(true, gD.compareTo("000000001133") < 0);
		assertEquals(true, gD.compareTo("000000001131") > 0);
		assertEquals(true, vD.compareTo("0000000100.18") < 0);
		assertEquals(true, vD.compareTo("0000000100.16") > 0);
		*/
		assertEquals(true, gD.compareTo(11.32) == 0);
		assertEquals(true, gD.compareTo(11.33) < 0);
		assertEquals(true, gD.compareTo(11.31) > 0);
		assertEquals(true, gD.compareTo(12) < 0);
		assertEquals(true, gD.compareTo(10) > 0);
		assertEquals(true, vD.compareTo(100.17) == 0);
		assertEquals(true, vD.compareTo(112) < 0);
		assertEquals(true, vD.compareTo(10) > 0);
		
		gD.set(100.17);
		assertEquals(0, gD.compareTo(vD));
		assertEquals(0, vD.compareTo(gD));
		assertEquals(true, vD.ge(gD));
		assertEquals(true, vD.le(gD));
		assertEquals(false, vD.gt(gD));
		assertEquals(false, vD.lt(gD));
		assertEquals(false, vD.ne(gD));
		assertEquals(true, vD.eq(gD));

		gD.set(100.18);
		assertEquals(false, vD.ge(gD));
		assertEquals(true, vD.le(gD));
		assertEquals(false, vD.gt(gD));
		assertEquals(true, vD.lt(gD));
		assertEquals(true, vD.ne(gD));
		assertEquals(false, vD.eq(gD));
		
		
		gD.set(123.45);
		vD.set(123.45);
		assertEquals(true, gD.eq(vD));
		assertEquals(true, vD.eq(gD));
		vD.set(123.46);
		assertEquals(false, gD.eq(vD));
		assertEquals(false, vD.eq(gD));
		assertEquals(true, gD.lt(vD));
		assertEquals(true, vD.gt(gD));

		assertEquals(true, gD.eq(123.45));
		assertEquals(true, vD.eq(123.46));
		assertEquals(false, gD.eq(123.47));
		assertEquals(false, vD.eq(123.47));
		assertEquals(true, gD.lt(123.47));
		assertEquals(true, vD.gt(123.45));
		assertEquals(true, gD.lt(124));
		assertEquals(true, vD.gt(123));
		
		vL.set(0);
		vS.set(" ");
		gL.set(0);
		gS.set(" ");
		//Comparing space vs 0 
		//(default is that they are equal)
		Config.COMPARE_SPACEZERO_NOT_EQUAL = false;
		//Compare Numeric Var to Char Var
		assertEquals(true, vL.eq(vS));
		assertEquals(true, vS.eq(vL));
		assertEquals(false, vS.ne(vL));
		assertEquals(false, vL.ne(vS));
		
		//Numeric Var and Char Group
		assertEquals(true, gS.eq(vL));
		assertEquals(true, vL.eq(gS));
		assertEquals(false, gS.ne(vL));
		assertEquals(false, vL.ne(gS));
		
		//Numeric Group and Var Char
		assertEquals(true, vS.eq(gL));
		assertEquals(true, gL.eq(vS));
		assertEquals(false, vS.ne(gL));
		assertEquals(false, gL.ne(vS));
		
		//Numeric Group and Group Char
		assertEquals(true, gS.eq(gL));
		assertEquals(true, gL.eq(gS));
		assertEquals(false, gS.ne(gL));
		assertEquals(false, gL.ne(gS));

		Config.COMPARE_SPACEZERO_NOT_EQUAL = true;
		//Compare Numeric Var to Char Var
		assertEquals(false, vL.eq(vS));
		assertEquals(false, vS.eq(vL));
		assertEquals(true, vS.ne(vL));
		assertEquals(true, vL.ne(vS));
		
		//Numeric Var and Char Group
		assertEquals(false, gS.eq(vL));
		assertEquals(false, vL.eq(gS));
		assertEquals(true, gS.ne(vL));
		assertEquals(true, vL.ne(gS));
		
		//Numeric Group and Var Char
		assertEquals(false, vS.eq(gL));
		assertEquals(false, gL.eq(vS));
		assertEquals(true, vS.ne(gL));
		assertEquals(true, gL.ne(vS));
		
		//Numeric Group and Group Char
		assertEquals(false, gS.eq(gL));
		assertEquals(false, gL.eq(gS));
		assertEquals(true, gS.ne(gL));
		assertEquals(true, gL.ne(gS));
    }

    @SuppressWarnings("unused")
    public void test_unstring() throws Exception {
   	    Group ibdskMessage = new Group();
		Group filler009 = ibdskMessage.addMember(Var.UNUMERIC, 4);
        Group filler010 = ibdskMessage.addMember(Var.UNUMERIC, 3);
        Group ibdskBrnidtorgXDup2 = ibdskMessage.addGroup();
            Group ibdskBrnidtorgDup2 = ibdskBrnidtorgXDup2.addMember(Var.UNUMERIC, 8);
        Group ibdskDteiniXDup2 = ibdskMessage.addGroup();
            Group ibdskDteiniDup2 = ibdskDteiniXDup2.addMember(Var.UNUMERIC, 8);
        Group ibdskSeqnbrinpX = ibdskMessage.addGroup();
            Group ibdskSeqnbrinp = ibdskSeqnbrinpX.addMember(Var.UNUMERIC, 7);
        Group ibdskBrnidtrcvX = ibdskMessage.addGroup();
            Group ibdskBrnidtrcv = ibdskBrnidtrcvX.addMember(Var.UNUMERIC, 8);
        Group ibdskMsgbdy = ibdskMessage.addMember(Var.CHAR, 1857);

        Group wsUnstringGroup = new Group();
        Group wsDelimiters = wsUnstringGroup.addGroup();
            Group slash = wsDelimiters.addMember(Var.CHAR, 1).set("/");
            Group crlf = wsDelimiters.addMember(Var.CHAR, 2).setHex("0D0A");
            Group hex00 = wsDelimiters.addMember(Var.CHAR, 1).setHex("00");
            Group fs1 = wsDelimiters.addMember(Var.CHAR, 3).setHex("0D0A3A");
            Group fs2 = wsDelimiters.addMember(Var.CHAR, 3).setHex("0D0A2D");
            Group fs3 = wsDelimiters.addMember(Var.CHAR, 3).setHex("0D0A0D");
            Group fs4 = wsDelimiters.addMember(Var.CHAR, 3).setHex("0D0A0A");
            Group fs5 = wsDelimiters.addMember(Var.CHAR, 1).setHex("0A");
            Group bodyFieldFull = wsUnstringGroup.addMember(Var.CHAR, 1000);
        Group bodyField = new Group();
            Group bodyTag = bodyField.addMember(Var.UNUMERIC, 2);
            Group filler061 = bodyField.addMember(Var.CHAR, 1);
            Group bodyRest = bodyField.addMember(Var.CHAR, 300);
        ibdskMsgbdy.set("abcde" + fs1.getString() + "fghi" + fs2.getString() + ",nmo" + fs1.getString() + "/xyz" + fs1.getString() + "aaaaa" + fs1.getString() + ";bbbbbccccc" + fs1.getString() + "dddd" + fs1.getString() + "tttttt" + fs1.getString() + "fgsgsfgsfg;sfgsfgsdfg;sdfgsfggsf" + fs1.getString() + "");
		String [][] counts = new String[2][200];
		Var ptr = new Var(Var.NUMERIC,10).set(2);
   	    bodyRest.set("");
   	    ptr.set(2);
		counts=ibdskMsgbdy.unstringDelimFrom(ptr,fs1.getString() + "|" + fs2.getString(),bodyRest);
		assertEquals("4", counts[0][0]);
		//assertEquals(" ", counts[1][0]);
		//System.out.println(">" + bodyRest.getTrim() + "<");
		assertEquals("bcde", bodyRest.getTrim());
		assertEquals(9, ptr.getInt());
			
       	ptr.set(2);
		counts=ibdskMsgbdy.unstringDelimFrom(ptr,"$",bodyRest);
		assertEquals("1856", counts[0][0]);
		assertEquals("", counts[1][0]);
		//assertEquals("bcde", bodyRest.getString());
		assertEquals(1858, ptr.getInt());
        ibdskMsgbdy.set("abcde" + fs2.getString());
        ptr.set(1);
		counts=ibdskMsgbdy.unstringDelimFrom(ptr,fs2,bodyRest);
		assertEquals("5", counts[0][0]);
		//assertEquals(" ", counts[1][0]);
		assertEquals("abcde", bodyRest.rtrim());
		assertEquals(9, ptr.getInt());
        ibdskMsgbdy.set("abcde" + fs2.getString() + "xxx");
        ptr.set(1);
		counts=ibdskMsgbdy.unstringDelimFrom(ptr,fs2,bodyRest);
		assertEquals("5", counts[0][0]);
		//assertEquals(" ", counts[1][0]);
		assertEquals("abcde", bodyRest.rtrim());
		assertEquals(9, ptr.getInt());
    }
    
    public void testDivide() {
        
        Var i = new Var(Var.NUMBER,10,2).set(400.00);
        Var j = new Var(Var.NUMBER,10,2).set(649.45);
        Var k = new Var(Var.NUMBER,10,2).set(16457.40);
        
        Var exp = new Var(Var.NUMBER,11,3).set(i.getDouble()*j.getDouble()/k.getDouble());
        assertEquals("00000015.784", exp.getString());
        
        Var x = new Var(400.00);
        Var y = new Var(649.45);
        Var z = new Var(16457.40);

        Var expr = new Var(x.getDouble()*y.getDouble()/z.getDouble());
        Var c = new Var(Var.NUMERIC,11,3);
        c.set(expr);
        assertEquals("00000015.784", c.getString());
        
        Var int1 = new Var(Var.NUMERIC,2).set(2);
    	Var int2 = new Var(Var.NUMERIC,2).set(2);
   	
    	int1.setDivide(int1,12,int2);
   	
    	assertEquals("00", int1.getString());
    	assertEquals("02", int2.getString());
   	
    	Var double1 = new Var(Var.NUMERIC,2,1).set(2);
    	Var double2 = new Var(Var.NUMERIC,2,1).set(2);
    	double1.setDivide(double1,12.0,double2);
    	
    	assertEquals("0.1", double1.getString());
    	assertEquals("0.8", double2.getString());
   	
   	
    }
    public void testTimes() {
        Var iL = new Var(Var.NUMBER,5);
        Var jL = new Var(Var.NUMBER,3);
        Var kL = new Var(Var.NUMBER,10);
        Var iD = new Var(Var.NUMBER,8,2);
        Var jD = new Var(Var.NUMBER,8,2);
        Var kD = new Var(Var.NUMBER,10,2);
        
        iL.set(3312);
        jL.set(100);
        kL.set(jL.times(iL));
        assertEquals(331200,kL.getLong());
        kL.set(iL.times(jL));
        assertEquals(331200,kL.getLong());

        kL.set(iL.times(100.001));
        assertEquals(331203,kL.getLong());
        
        iD.set(1324.56);
        jD.set(120.12);
        kD.set(iD.times(jD));
        assertEquals(159106.14,kD.getDouble());
        kD.set(jD.times(iD));
        assertEquals(159106.14,kD.getDouble());
        kD.set(iD.times(120.12));
        assertEquals(159106.14,kD.getDouble());
        
        
        Var sd1 = new Var(Var.NUMERIC,17,2);
        sd1.set (614321.09);
        sd1.set(sd1.times(-1));
        assertEquals(-614321.09,sd1.getDouble());
    }
        
 
    public void testComplexMove() {
    	Var dest = new Var(Var.CHAR,20).set("aaaaaaa fffffffff"); 
	    Var src = new Var(Var.CHAR,20).set("1234567890 fffffffff");
	    int srcPos=1;
	    int srcLen=3;
	    int destPos=1;
	    
	    dest.complexMove(src, srcPos, srcLen, destPos);
	    assertEquals("123aaaa fffffffff   ",dest.getString());
	    
	    dest.complexMove("123",10);
	    assertEquals("123aaaa f123fffff   ",dest.getString());
	    
	    dest.set("aaaaaaa fffffffff");
	    src.set("1234567890 fffffffff");
	    
	    Var vSrcPos=new Var(Var.NUMBER,10).set(1);
	    Var vSrcLen=new Var(Var.NUMBER,10).set(3);
	    Var vDestPos=new Var(Var.NUMBER,10).set(1);
	    
	    dest.complexMove(src, vSrcPos, vSrcLen, vDestPos);
	    assertEquals("123aaaa fffffffff   ",dest.getString());
	    
	    dest.complexMove("123",10);
	    assertEquals("123aaaa f123fffff   ",dest.getString());
	    
	    dest.complexMove("123",0);
	    assertEquals("123aaaa f123fffff   ",dest.getString());
    }
    
    @SuppressWarnings("unused")
	public void testArrays() {
    	Var [] a1 = Var.createArray(16,Var.CHAR|Var.INQUIRY,78);
    	String x = "                                                                                ";
    	//initialise array
//    	for(int i=0;i<a1.length;i++) {
//    		a1[i].set("String " + i + " iter.");
//    	}
    	//Verify array contents
    	//    	for(int i=0;i<a1.length;i++)
    	//    		assertEquals("String " + i + " iter.", a1[i].toString());
    	//Initialise array using Util.setArray
    	for(int i=1;i<a1.length;i++) {
    		Var v = new Var(Var.CHAR, 80);
    		v.set("String " + i + " iter.");
    		a1[i].clear();
    		Util.setArray(v, a1, i);
    	}
    	//    	for(int i=1;i<a1.length;i++)
    	//    		System.out.println("a1["+i+"]=["+a1[i].toString()+"]");	

    	//Verify array contents
    	for(int i=1;i<a1.length;i++)
    		assertEquals("String " + i + " iter.", Util.getArray(a1, i).toString());
    	//Clear array
    	//Move array to a string
    	//		Util.setArray(gsd.GD_MENU_LINE,screen.MENU_LINE,GLB.COPY);

    	Var v = Util.getArray(a1, 5);
		assertEquals("String 5 iter.", v.toString());
    	v = Util.getArray(a1, 76);
		assertEquals("String 5 iter.", v.toString());
    	v = Util.getArray(a1, 0);
		assertEquals("String 5 iter.", v.toString());
    	
		
		Var [][] a2d = Var.createArray2D(10, 11, Var.UNUMERIC, 2);
		Var [][][] a3d = Var.createArray3D(10, 11, 3, Var.UNUMERIC, 2);

		Util.setArray(new Var(10),a2d,1,1);

		assertEquals(10, Util.getArray(a2d,1,1).getInt());
		assertEquals(10, Util.getArray(a2d,11,1).getInt());
		assertEquals(10, Util.getArray(a2d,1,12).getInt());
		
		Util.setArray(new Var(10),a3d,1,1,1);

		assertEquals(10, Util.getArray(a3d,1,1,1).getInt());
		assertEquals(10, Util.getArray(a3d,1,1,4).getInt());
		assertEquals(10, Util.getArray(a3d,1,12,1).getInt());
		assertEquals(10, Util.getArray(a3d,12,1,1).getInt());

		Util.setArray(new Var(10),a2d,1,1);

		assertEquals(10, Util.getArray(a2d,1,1).getInt());
		assertEquals(10, Util.getArray(a2d,11,1).getInt());
		assertEquals(10, Util.getArray(a2d,1,12).getInt());
		Util.setArray(new Var(99),a2d,1,1);

		assertEquals(99, Util.getArray(a2d,11,1).getInt());
		assertEquals(99, Util.getArray(a2d,1,12).getInt());

		Var [][] SA_DISPLAY = Var.createArray2D(2, 14, Var.CHAR, 34);

		Util.setArray("xxxxxxxxxx",SA_DISPLAY,1,2);
		Util.setArray("xxxxxxxxyx",SA_DISPLAY,1,3);
		assertEquals("xxxxxxxxxx", SA_DISPLAY[1][2].toString());
		assertEquals("xxxxxxxxyx", SA_DISPLAY[1][3].toString());

    }
    
    @SuppressWarnings("unused")
	public void testZeros() {
    	FrameVar F90_OPT2 = new FrameVar(18, 49, "char",25);
    	Group SP_REPDTLS = new Group();
        Group SE_IN_BRANCH = SP_REPDTLS.addMember(Var.UNUMERIC, 2);
        Group SE_IN_MM = SP_REPDTLS.addMember(Var.UNUMERIC, 2);
        Group SE_IN_AA = SP_REPDTLS.addMember(Var.CHAR, 2);
        Group SE_IN_M1 = SP_REPDTLS.addMember(Var.UNUMERIC, 2);
        
        F90_OPT2.set(SE_IN_MM);
        assertEquals("0000  00", SP_REPDTLS.getString());
        assertEquals("00                       ", F90_OPT2.getString());
        
        SP_REPDTLS.set(" ");
        assertEquals("        ", SP_REPDTLS.getString());
        assertEquals("00", SE_IN_MM.getString());
    }
}
	
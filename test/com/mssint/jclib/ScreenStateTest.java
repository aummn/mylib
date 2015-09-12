package com.mssint.jclib;

import junit.framework.TestCase;

public class ScreenStateTest extends TestCase {

	public class Screen extends ScreenState {
		private static final long serialVersionUID = 1L;
		public Var ISPEC = new Var(Var.CHAR, 5);
		public Var TRANNO = new Var(Var.UNUMERIC, 6);
		public Var UNSET = new Var(Var.UNUMERIC, 6);
		public Var [] ARRAY = Var.createArray(9, Var.CHAR, 15);
		public String ILLEGAL_PARAMETER = "illegal";
	}
	
	public class ScreenSimplified extends ScreenState {
		private static final long serialVersionUID = 1L;
		public Var ISPEC = new Var(Var.CHAR, 5);
	}
	
	public void testSetGet() throws Exception {
		Screen screen = new Screen();
		screen.init();
		screen.TRANNO.set(123);
		screen.ISPEC.set("GEM");
		screen.ARRAY[0].set("STRING");
		screen.ARRAY[1].set("   DIFFERENT STRING ");

		String s = screen.get("ISPEC");
		System.out.println("s="+s);
		
		s = screen.get("TRANNO");
		
		//Var v = screen.getVar("ISPEC");
	}

	public void testSimple() throws Exception {
		// some initialization
		Screen screen1 = new Screen();
		screen1.init();
		screen1.TRANNO.set(123);
		screen1.ISPEC.set("GEM");
		screen1.ARRAY[0].set("STRING");
		screen1.ARRAY[1].set("   DIFFERENT STRING ");

		// so what's up?
		assertEquals(123, screen1.TRANNO.getInt());
	    ParameterWrapper wrapper = screen1.parameters.get("TRANNO");
		assertEquals("000123", wrapper.getString());
		
		wrapper = screen1.parameters.get("ISPEC");
		assertEquals("GEM", wrapper.getString());
		
		wrapper = screen1.parameters.get("UNSET");
		assertEquals("000000", wrapper.getString());
		
		wrapper = screen1.parameters.get("ARRAY");
		assertEquals("STRING", wrapper.getString(0));
		assertEquals("   DIFFERENT ST", wrapper.getString(1));
				
		wrapper = screen1.parameters.get("ILLEGAL_PARAMETER");
		assertNull(wrapper);
	}
	
	public void testCopy() throws Exception {
		// some initialization
		Screen screen1 = new Screen();
		screen1.init();
		screen1.TRANNO.set(123);
		screen1.ISPEC.set("GEM");
		screen1.ARRAY[0].set("STRING");
		screen1.ARRAY[1].set("   DIFFERENT STRING ");
		
		Screen screen2 = new Screen();
		screen2.init();
		Screen.copy(screen1, screen2);
		
		ParameterWrapper wrapper = screen2.parameters.get("TRANNO");
		assertEquals("000123", wrapper.getString());
		wrapper = screen2.parameters.get("ISPEC");
		assertEquals("GEM", wrapper.getString());
		wrapper = screen2.parameters.get("UNSET");
		assertEquals("000000", wrapper.getString());
		wrapper = screen2.parameters.get("ARRAY");
		assertEquals("STRING", wrapper.getString(0));
		assertEquals("   DIFFERENT ST", wrapper.getString(1));
		
		// should be a deep copy
		screen2.ARRAY[3].set("text");
		assertEquals("text", wrapper.getString(3));
		wrapper = screen1.parameters.get("ARRAY");
		assertEquals("", wrapper.getString(3));
		
		screen2.TRANNO.set(321);
		wrapper = screen1.parameters.get("TRANNO");
		assertEquals("000123", wrapper.getString());
		wrapper = screen2.parameters.get("TRANNO");
		assertEquals("000321", wrapper.getString());
		
	}
	
	public void testSetWhenSameTypes() throws Exception {
		// some initialization
		Screen screen1 = new Screen();
		screen1.init();
		screen1.TRANNO.set(123);
		screen1.ISPEC.set("GEM");
		screen1.ARRAY[0].set("STRING");
		screen1.ARRAY[1].set("   DIFFERENT STRING ");
		
		Screen screen2 = new Screen();
		screen2.init();
		screen2.set(screen1);
		
		ParameterWrapper wrapper = screen2.parameters.get("TRANNO");
		assertEquals("000123", wrapper.getString());
		wrapper = screen2.parameters.get("ISPEC");
		assertEquals("GEM", wrapper.getString());
		wrapper = screen2.parameters.get("UNSET");
		assertEquals("000000", wrapper.getString());
		wrapper = screen2.parameters.get("ARRAY");
		assertEquals("STRING", wrapper.getString(0));
		assertEquals("   DIFFERENT ST", wrapper.getString(1));
		
		// should be a deep copy
		screen2.ARRAY[3].set("text");
		assertEquals("text", wrapper.getString(3));
		wrapper = screen1.parameters.get("ARRAY");
		assertEquals("", wrapper.getString(3));
		
		screen2.TRANNO.set(321);
		wrapper = screen1.parameters.get("TRANNO");
		assertEquals("000123", wrapper.getString());
		wrapper = screen2.parameters.get("TRANNO");
		assertEquals("000321", wrapper.getString());
		
	}
	
	public void testSetWhenDifferentTypes1() throws Exception {
		// some initialization
		Screen screen1 = new Screen();
		screen1.init();
		screen1.TRANNO.set(123);
		screen1.ISPEC.set("GEM");
		screen1.ARRAY[0].set("STRING");
		screen1.ARRAY[1].set("   DIFFERENT STRING ");
		
		ScreenSimplified screen2 = new ScreenSimplified();;
		screen2.set(screen1);
		
		ParameterWrapper wrapper = screen2.parameters.get("TRANNO");
		assertEquals("000123", wrapper.getString());
		wrapper = screen2.parameters.get("ISPEC");
		assertEquals("GEM", wrapper.getString());
		wrapper = screen2.parameters.get("UNSET");
		assertEquals("000000", wrapper.getString());
		wrapper = screen2.parameters.get("ARRAY");
		assertEquals("STRING", wrapper.getString(0));
		assertEquals("   DIFFERENT ST", wrapper.getString(1));
		
		// ISPEC's value shoud be set
		assertEquals("GEM", screen2.ISPEC.toString().trim());
		
		// should be a deep copy
		screen2.ISPEC.set("text");
		wrapper = screen2.parameters.get("ISPEC");
		assertEquals("text", wrapper.getString());
		wrapper = screen1.parameters.get("ISPEC");
		assertEquals("GEM", wrapper.getString(3));
	
	}
	
	public void testSetWhenDifferentTypes2() throws Exception {
		// some initialization
		ScreenSimplified screen1 = new ScreenSimplified();
		screen1.parameters.put("TRANNO", 
				new ParameterWrapper(new Var(4321)));
		
		Screen screen2 = new Screen();
		screen2.set(screen1);
		
		assertEquals("004321", screen2.TRANNO.toString());
	
	}
	
	public void testLikeInCommit() throws Exception {
		// some initialization
		ScreenState screen1 = new ScreenState();
		screen1.parameters.put("TRANNO", 
				new ParameterWrapper(new Var(4321)));
		
		Screen screen2 = new Screen();
		screen2.init();
		Screen screen3 = new Screen();
		screen3.init();
		
		screen2.set(screen1);
		screen3.set(screen2);
		
		//System.out.println(screen2.TRANNO);
		//System.out.println(screen3.TRANNO);
		
		screen1.parameters.put("ISPEC", 
				new ParameterWrapper(new Var("ispec")));
		screen2.set(screen1);
		screen3.set(screen2);
		
		//System.out.println(screen2.ISPEC);
		//System.out.println(screen3.ISPEC);
		
		//assertEquals("004321", screen2.TRANNO.toString());
	
	}
	
	public void testLikeInCommit2() throws Exception {
		Screen screen2 = new Screen();
		screen2.init();
		screen2.ISPEC.set("value");
		
		// some initialization
		ScreenState screen1 = screen2.snapshot();
		screen1.parameters.put("no sense", 
				new ParameterWrapper(new Var(4321)));
		screen1.parameters.put("TRANNO", 
				new ParameterWrapper(new Var(4321)));
		
		//System.out.println(screen1.parameters.get("ISPEC"));
		
		Screen screen3 = new Screen();
		screen3.init();
		
		screen2.set(screen1);
		screen3.set(screen2);
		
		//System.out.println(screen2.TRANNO);
		//System.out.println(screen3.TRANNO);
		
		screen1.parameters.put("ISPEC", 
				new ParameterWrapper(new Var("ispec")));
		screen2.set(screen1);
		screen3.set(screen2);
		
		//System.out.println(screen2.ISPEC);
		//System.out.println(screen3.ISPEC);
	
	}
	
	public void testParameterWrapper() throws Exception {
		// good
		new ParameterWrapper(new Var(0));
		// good
		new ParameterWrapper(new Var[]  {new Var(0), new Var(1)});
		// good
		new ParameterWrapper(new Var[0]);
		// no good
		try  {
			new ParameterWrapper(null);
			fail();
		} catch(IllegalArgumentException ex)  {
		}
		// no good
		try  {
			new ParameterWrapper("illegal");
			fail();
		} catch(IllegalArgumentException ex)  {
		}
	}

}

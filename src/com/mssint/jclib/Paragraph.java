package com.mssint.jclib;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Simulates a Cobol Paragraph i.e. a set of Statements that need to be executed.
 * This may be of the form of "normal" statements i.e. file manipulation, reports etc
 * 
 * When a Cobol program is ported each Cobol paragraph as appropriate is converted into 
 * a method representing that paragraph is created in the @see JbolSection. The created method 
 * returns a Paragraph and consequently allows for the execution of a set of paragraphs in turn.     
 * 
 * Each method is "known" via the non null default constructor @{link} #Paragraph i.e. the 
 * String method parameter.
 * 
 * Paragraphs exist within a @see JbolSection class and "know their" containing JbolSection instance and
 * @see JbolApplication via the non default constructor.
 * 
 * @author MssInt
 *
 */
public class Paragraph {
	private static final Logger log = LoggerFactory.getLogger(Paragraph.class);
	Method method;
	JbolSection instanceClass;
	JbolSection section;
	JbolApplication app;
	private Class<? extends Object> clazz;
	private String methodName;
	private int sequence;
	
	
	/**
	 * When this Paragraph was registered with the contain JbolApplication which index on joining did it receive.
	 * @return an int representing it's when registered via @link #JbolApplication.addParagraph 
	 */
	public int getSequence() { return sequence; }
	
	/**
	 * Default constructor do not use. 
	 * @throws Exception if invoked.
	 */
	public Paragraph() throws Exception {
		throw new java.lang.IllegalArgumentException("Cannot call default constructor.");
	}
	
	/**
	 * Constructor for a paragraph.
	 * @param app the ultimate containing class/object @see JbolApplication class in which this paragraph is housed. 
	 * @param section the @see JbolSection that this paragraph is defined within the parameter in production code is always "this".
	 * @param method the primary execution method that all the statements are generated in. 
	 * @throws Exception either an IllegalArgumentException when the contain JbolApplication is null or the string method name provided
	 * does not equate to a defined method for the paragraph class. 
	 * 
	 */
	public Paragraph(JbolApplication app, JbolSection section, String method) 
		throws Exception {
		Class<?> params[] = {};
		clazz = section.getClass();
		this.instanceClass = section;
		
		if(app == null) throw new IllegalArgumentException("(JbolApplication)app is null");
		this.app = app;
		try {
			this.method = clazz.getDeclaredMethod(method, params);
			methodName = this.method.getName();
		} catch(NoSuchMethodException e) {
			throw new NoSuchMethodException("FATAL: Method '"+method+"' not found in class '"+clazz.getName()+"'.");
		}
		
		//This could be a replacement paragraph, in which case we need to replace it in the section
		//which the old paragraph was in.
		sequence = app.getSequence(method);
		if(sequence == -1) {
			sequence = app.addParagraph(method, this);
			this.section = section;
			section.addParagraph(this);
			if(log.isDebugEnabled())
				log.debug(sequence+": class="+clazz.getName()+" method="+methodName);
		} else {
			Paragraph para = app.getParagraph(sequence);
			this.section = para.section;
			app.replaceParagraph(sequence, this);
			para.section.replaceParagraph(para, this);
		}
		
		
	}
	
	/**
	 * Executes the defined method as per the constructor.
	 * Although available perform execution is generally handled in migrated code via the
	 * containing JbolApplication instance. 
	 * @return whatever return is defined in the method generated during migration.
	 * @throws Exception if the method connot be invoked in the containing JbolSection instance.
	 */
	public Paragraph perform() throws Exception {
		Paragraph para;
		final Object [] params = {};
		try {
//			if(section.jbolApplication.trackParagraphs)
//				if(log.isDebugEnabled())
//					log.debug("Start Paragraph "+methodName);
			para = (Paragraph)method.invoke(instanceClass, params);
//			if(section.jbolApplication.trackParagraphs)
//				if(log.isDebugEnabled())
//					log.debug("End Paragraph "+methodName);
		} catch (EndJbolProgramException e) {
			throw new EndJbolProgramException(e);
		} catch(IllegalAccessException e) {
			Throwable ne = e.getCause();
			throw (Exception)ne;
		} catch(InvocationTargetException e) {
//			System.out.println(e.toString());
//			System.out.println(e.getMessage());
//			System.out.println(e.getCause());
//			System.out.println(e.getTargetException());
			String s = e.getTargetException().toString();
			if(app.isEndProgram() || s.indexOf("EndJbolProgramException") != -1)
				throw new EndJbolProgramException(e.getTargetException().getMessage());
			Throwable ne = e.getCause();
			throw new RuntimeException(ne);
//			throw (Exception);
		}
		return para;
	}

	/**
	 * What is the name of the method (created via the code generator) that contains the
	 * operational execution logic of the Paragragh 
	 * @return
	 */
	public String getMethodName() { return methodName; }
	
	/**
	 * What is the containing JbolSection name
	 * @return the name of the section in which the migrated code Paragraph "lives"
	 */
	public String getSectionName() {
		return instanceClass.getClass().getSimpleName();
	}
	
	/**
	 * Gets the instance of the containing JbolSection
	 * @return the containing JbolSection instance.
	 */
	public JbolSection getSection() { return instanceClass; }
	
	
	/**
	 * Gets the instance of the JbolApplication i.e. the primary execution container for this migrated Cobol program.
	 * @return the containing JbolApplication 
	 */
	public JbolApplication getJbolApplication() { return section.getJbolApplication(); }
}

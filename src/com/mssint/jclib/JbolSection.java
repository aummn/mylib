package com.mssint.jclib;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simulates a Cobol Section
 * Effectively contains a series of registered Paragraph instances and their associated methods
 * along with who the containing JbolApplication is.
 * 
 * @author MssInt
 *
 */
public class JbolSection {
	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(JbolSection.class);
	public JbolApplication jbolApplication = null;
	private ArrayList<Paragraph> paragraphs = new ArrayList<Paragraph>();

	/**
	 * Default constructor to be used in conjunction with the {@link #init(JbolApplication)}
	 * @throws Exception
	 */
	public JbolSection() throws Exception {
		//log.debug("JbolSection()");
	}
	
	/**
	 * If the default constructor has been used this method needs to be called to register the 
	 * section with a containing JbolApplication instance. 
	 * @param app
	 * @return the registered Section instance.
	 */
	public JbolSection init(JbolApplication app) {
		//log.debug("JbolSection.setApp(this)");
	    jbolApplication = app;
	    return this;
	}
	
	/**
	 * Constructor taking a JbolApplication instance
	 * i.e. the section's containing application. 
	 * @param app as Object to be internally cast to JbolApplication 
	 */
	public JbolSection(JbolApplication app) {
	    //log.debug("JbolSection(JbolApplication app)");
	    jbolApplication = app;
	}
	
	/**
	 * Constructor taking the JbolApplication instance as an Object type 
	 * i.e. the section's containing application. 
	 * @param app as Object to be internally cast to JbolApplication 
	 */
	public JbolSection(Object app) {
	    //log.debug("JbolSection(Object app)");
		jbolApplication = (JbolApplication)app;
	}

	/**
	 * Add/register a Paragraph 
	 * @param para the Paragraph to register
	 * @return it's position/registration index
	 */
	public int addParagraph(Paragraph para) {
		//log.debug("Calling add");
		paragraphs.add(para);
		return paragraphs.size();
		//return jbolApplication.addParagraph(para);
	}
	
	protected void replaceParagraph(Paragraph oldPara, Paragraph newPara) {
		int idx = paragraphs.indexOf(oldPara);
		if(idx != -1) {
			paragraphs.set(idx, newPara);
		}
	}
	
	/**
	 * Get the first paragraph registered in the JbolSection
	 * @return the first paragraph instance
	 */
	public Paragraph getFirstParagraph() {
		if(paragraphs.size() > 0) return paragraphs.get(0);
		return null;
	}
	
	/**
	 * Get the last paragraph registered in the JbolSection
	 * @return the last paragraph instance
	 */
	public Paragraph getLastParagraph() {
		if(paragraphs.size() > 0) return paragraphs.get(paragraphs.size() - 1);
		return null;
	}
	
	/**
	 * Get the count of the number of contained/registered Paragraphs
	 * @return the total
	 */
	public int getParagraphCount() { return paragraphs.size(); }
	
	/**
	 * Get the instance of the containing JbolApplication instance;
	 * @return
	 */
	public JbolApplication getJbolApplication() { return jbolApplication; }
}

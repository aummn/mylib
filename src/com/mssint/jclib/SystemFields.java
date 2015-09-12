package com.mssint.jclib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SystemFields defines the system level data items which are transferred 
 * between an online application bean and webManager. It includes methods
 * for managing these fields from either end. This is intended to be subclassed
 * by <b>com.mssint.jclib.ScreenClass</b> 
 * and <b>com.mssint.linc.webManager.StateManager</b>.
 * <p>jlinc last rebuilt %DATE; </p>
 * @version %BUILD;
 */
public class SystemFields {
	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(SystemFields.class);
	
	public SystemFields() {
		rcType = 0;
		messCount = 0;
		cuRec = 0;
		rcIspec = "";
		mssReport = "";
		cuField = "";
		cuBlock = "";
		sessionID = "";
		stationName = "";
		userCode = "";
	}
	
	//-- Field definitions --
	/**
	 * Defines the maximum number of messages allowed on "page 2" popup.
	 */
	 public static final int MESSLINE_SIZE = 24;
	
	/**
	 * <p>Defines the action to be performed.</p>
	 * 0 = No recall done. i.e. go back thru your own startup logic<br> 
	 * 1 = Recall of yourself done i.e do not do preScreen logic <br>
	 * 2 = Recall done from elsewhere i.e do PreScreen logic <br>
	 * <br><b>Direction: From EJB to Client</b>
	 */
	public int rcType;
	/**
	 * The name of the ISPEC or Bean to call.
	 * If {@link ScreenClass#rcType rcType} is non-zero, then the webManager
	 * will load the stated XML form which will, in turn, reference the 
	 * particular bean.
	 * <br><b>Direction: From EJB to Client</b>
	 */
	public String rcIspec;
	/**
	  * <p>Indicates the number of messages returned by the Bean to the webManager.</p>
	  * A value of 0 means no messages were returned.<br>
	  * A value of 1 means that a single message was returned in 
	  * {@link ScreenClass#message message}<br>
	  * Other values indicate the number of message lines to be found in
	  * the array {@link ScreenClass#messLine messLine}.
	 * <br><b>Direction: From EJB to Client</b>
	  */
	public int messCount;
	/**
	 * The array of message lines to be returned to webManager. 
	 * The lines are displayed in the popup "page 2" window.
	 * <br><b>Direction: From EJB to Client</b>
	 */
	public String[] messLine = new String[MESSLINE_SIZE];
	/**
	 * A Single line message returned to webManager. This message is 
	 * displayed at the bottom of the form.
	 * <br><b>Direction: From EJB to Client</b>
	 */
	public String message;
	/**
	 * The name of a requested report. If the Online Bean requests a report to 
	 * be run, the name is transmitted to the form.
	 * <br><b>Direction: From EJB to Client</b>
	 */
	public String mssReport;
	/**
	 * If a CURSOR("field_name") is done we need to know which record number
	 * within the copy block (screen array) to place the cursor. The record number
	 * is indicated in cuRec as a 1-relative index.
	 * <br><b>Direction: From EJB to Client</b>
	 */
	public int cuRec;
	/**
	 * Inidcates the field into which to place the cursor.
	 * <br><b>Direction: From EJB to Client</b>
	 */
	public String cuField;
	/**
	 * Either contains "BLOCK" or "COPYBLOCK" depending on whether the cursor
	 * is to be placed into a normal screen item (BLOCK) or an array item
	 * (COPYBLOCK)
	 * <br><b>Direction: From EJB to Client</b>
	 */
	public String cuBlock;
	/**
	 * A string which is traditionally used by LINC programs as a session
	 * identifier. This will be maintained for compatibility although the 
	 * actual session identifier will be stored in {@link #stationName stationname}.<br>
	 * This field is never cleared, even by {@link #clearSystemFields clearSystemFields()}
	 * <br><b>Direction: From client to EJB</b>
	 */
	public String stationName;
	/**
	 * Each online program (or bean) must save state information. The sessionID
	 * is an index into that state.
	 * This field is never cleared, even by {@link #clearSystemFields clearSystemFields()}
	 * <br><b>Direction: From client to EJB</b>
	 */
	public String userCode;
	/**
	 * Provides storage for transmitting the usercode to EJB's
	 * <br><b>Direction: From client to EJB</b>
	 */
	public String sessionID;

	//Provide a few methods for dealing with the data.
	/**
	 * Clears all the system fields (except {@link #stationName screen.stationName}
	 * and {@link #sessionID screen.sessionID}) but leaves the fields defined in
	 * the subclassed ScreenClass object alone.
	 */
	public void clearSystemFields() {
	    rcType = 0;
	    messCount = 0;
	    cuRec = 0;
	    rcIspec = "";
	    mssReport = "";
	    cuField = "";
	    cuBlock = "";
		for(int i=0;i<MESSLINE_SIZE;i++) messLine[i] = "";
	}
	/**
	 * Package the fields destined for the client into an XML string.
	 * @return System fields wrapped in &lt;Sys>&lt;/sys> tags.
	 */
	public String wrapFieldsForClient() {
		StringBuilder buf = new StringBuilder();
		buf.append("<sys>");
		if(rcType != 0)
			buf.append("<item id=\"rcType\"><value>"+rcType+"</value></item>");
		if(messCount != 0)
			buf.append("<item id=\"messCount\"><value>"+messCount+"</value></item>");
		if(cuRec != 0)
			buf.append("<item id=\"cuRec\"><value>"+cuRec+"</value></item>");
		if(rcIspec.trim().length() != 0)
			buf.append("<item id=\"rcIspec\"><value>"+rcIspec.trim()+"</value></item>");
		if(messCount == 1)
			buf.append("<item id=\"message\"><value><![CDATA["+message.trim()+"]]></value></item>");
		if(mssReport.trim().length() != 0)
			buf.append("<item id=\"mssReport\"><value><![CDATA["+mssReport.trim()+"]]></value></item>");
		if(cuField.trim().length() != 0)
			buf.append("<item id=\"cuField\"><value><![CDATA["+cuField.trim()+"]]></value></item>");
		if(cuBlock.trim().length() != 0)
			buf.append("<item id=\"cuBlock\"><value><![CDATA["+cuBlock.trim()+"]]></value></item>");
		
	    if(messCount > 1) {
			buf.append("<item id=\"messLine\">");
			for(int i = 0; i < messCount; i++) { //LINC references from 1
				if(messLine[i] == null) messLine[i] = "";
				buf.append("<value><![CDATA["+messLine[i].trim()+"]]></value>");
			}
			buf.append("</item>");
		}
	    buf.append("</sys>");
		return buf.toString();
	}

	/**
	 * Package the system fields destined for the EJB into an XML string. 
	 * Essentially thes are just the {@link #sessionID sessionID} and 
	 * {@link #stationName stationName} fields.
	 * @return System fields wrapped in &lt;Sys>&lt;/sys> tags.
	 */
	public String wrapFieldsForEJB() {
		return "<sys>" + 
			"<item id=\"sessionID\"><value>" + sessionID + "</value></item>" +
			"<item id=\"stationName\"><value>" + stationName + "</value></item>" +
			"<item id=\"rcIspec\"><value>" + rcIspec + "</value></item>" +
			"</sys>";
	}

	/**
	 * Method sets the field value using strings. Needed for setting values
	 * from XML strings. Programs can normally just access the fields directly.
	 * Although the EJB's access the messLine values as 1 relative, webManager
	 * accesses this array as 0 relative. Since this method will only be used
	 * by webManager, the access is 0 relative.
	 * @param name The name of the field.
	 * @param index The index for array types. Ignored for others.
	 * @param value The data used to update the field value.
	 */
	public void setFieldValueForClient(String name, int index, String value) {
	    if(name == null || name.length() == 0) return;
	    if(value == null) value = "";

	    if(name.equals("rcType"))
	        try{rcType = Integer.parseInt(value);} catch (Exception e) {rcType = 0;}
	    else if(name.equals("messCount"))
	        try{messCount = Integer.parseInt(value);} catch (Exception e) {messCount = 0;}
	    else if(name.equals("cuRec"))
	        try{cuRec = Integer.parseInt(value);} catch (Exception e) {cuRec = 0;}
	    else if(name.equals("rcIspec")) rcIspec = value;
	    else if(name.equals("mssReport")) mssReport = value;
	    else if(name.equals("mssReport")) mssReport = value;
	    else if(name.equals("message")) message = value;
	    else if(name.equals("cuBlock")) cuBlock = value;
	    else if(name.equals("stationName")) stationName = value;
	    else if(name.equals("userCode")) userCode = value;
	    else if(name.equals("messLine") && index >= 0 && index < MESSLINE_SIZE)
	        messLine[index] = value;
	}
	 
	public void setFieldValueForEJB(String name, int index, String value) {
		if(name == null || name.length() == 0) return;
		if(value == null) value = "";

		if(name.equals("rcType"))
			try{rcType = Integer.parseInt(value);} catch (Exception e) {rcType = 0;}
		else if(name.equals("messCount"))
			try{messCount = Integer.parseInt(value);} catch (Exception e) {messCount = 0;}
		else if(name.equals("cuRec"))
			try{cuRec = Integer.parseInt(value);} catch (Exception e) {cuRec = 0;}
		else if(name.equals("rcIspec")) rcIspec = value;
		else if(name.equals("mssReport")) mssReport = value;
		else if(name.equals("cuField")) cuField = value;
		else if(name.equals("cuBlock")) cuBlock = value;
		else if(name.equals("stationName")) stationName = value;
		else if(name.equals("userCode")) userCode = value;
		else if(name.equals("messLine") && index > 0 && index < MESSLINE_SIZE)
			messLine[index] = value;
	}
}

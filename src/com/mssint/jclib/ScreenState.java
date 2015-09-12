package com.mssint.jclib;


import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScreenState implements java.io.Serializable {
	private static final long serialVersionUID = 5920346635916879612L;
	private static final Logger log = LoggerFactory.getLogger(ScreenState.class);

	public static final int MESSLINE_SIZE = 24;
	//For managing persistence:
	private GlobalPersistence persist;
	private boolean persitenceInitialised;
	
	public int rcType;
	public String rcIspec;
	public String ispec;
	public int messCount = 9;
	public String[] messLine = new String[MESSLINE_SIZE];
	public String message;
	public String mssReport;
	public int cuRec;
	public String cuField;
	public String cuBlock;
	public String stationName;
	public String url;
	public String userCode;
	public String sessionID;
	public String glbError;
	public String glbLanguage;
	public String glbLanguageCode;
	public String glbStyle;
	private String action = null;
	private String actionValue = null;
	public boolean teachScreen = false;
	private boolean supportsNumericKeyPadDecimal = false;
	private Map<String, CursorState> storedCursorList;
	public boolean nofMode;
	
	// flag informing if init() has been called - not to call it 
	// multiple times
	protected boolean isInitialised = false;
	
	// map to hold parameters
	public Map<String, ParameterWrapper> parameters;
	private ListRepository listRepository;	
	
	public ScreenState() {
		clearSystemFields();
		createParameters();
		sessionID = "";
		stationName = "";
		userCode = "";
		persist = null;
		persitenceInitialised = false;
		rcType = 0;
		storedCursorList = null;
		glbLanguage = null;
		glbStyle = null;
		nofMode = false;
	}
	
	private void initialisePersistence(Class<?>persistClass) {
		try {
			persist = (GlobalPersistence) persistClass.newInstance();
			persist.initialise();
		} catch (InstantiationException e) {
			persist = null;
		} catch (IllegalAccessException e) {
			persist = null;
		}
		persitenceInitialised = true;
		return; 
	}
	
	/**
	 * This function will recover all variables saved in this screenState object
	 * in the GlobalPersistence class 'persist' into variables in GLB which have
	 * the same name.
	 * @param GLB The Glb data set
	 * @param persist The GlobalPersistence set
	 */
	public void recoverPersistentGlobals(Object gsd) {
		if(persist == null) return;
		persist.recover(gsd);
	}
	
	/**
	 * This function will store all variables in 'persist' into GLB variables
	 * of the same name.
	 * @param GLB The Glb data set
	 * @param persist The GlobalPersistence set
	 */
	public void storePersistentGlobals(Object gsd, Class<?> persistClass) {
		if(!persitenceInitialised) initialisePersistence(persistClass);
		if(persist == null) return;
		persist.store(gsd);
	}
	
	/**
	 * Recover stored cursors into the supplied CursorState objects. Each cursor
	 * in the list is compared to a CursorState object in the supplied list. If a match
	 * is found, the values are recovered and the associated object is removed from the
	 * stored cursor list. Whether or not successful recovery is made, the list is destroyed.
	 * @param cursors
	 */
	public void recoverCursors(CursorState ... cursors) {
		if(storedCursorList == null) return;
		for(CursorState curs : cursors) {
			if(curs == null) continue;
			Class<? extends CursorState> clazz = curs.getClass();
			String name = clazz.getName();
			CursorState sc = storedCursorList.get(name);
			if(sc == null) continue;
			curs.set(sc);
		}
		storedCursorList = null;
	}
	
	/**
	 * Store a list of CursorState objects for recovery by the next called EJB. This
	 * method will always create a new list - any existing list will be destroyed.
	 * @param cursors
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public void storeCursors(CursorState ... cursors) throws InstantiationException, IllegalAccessException  {
		storedCursorList = new HashMap<String, CursorState>();
		for(CursorState curs : cursors) {
			
			if(curs == null)
				continue;
			Class<? extends CursorState> cursorClass = curs.getClass();
			String name = cursorClass.getName();
			CursorState nc = cursorClass.newInstance();
			nc.set(curs);
			storedCursorList.put(name, nc);
		}
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getAction() {
		return action;
	}

	public void setActionValue(String actionValue) {
		this.actionValue = actionValue;
	}

	public boolean getSupportsNumericKeyPadDecimal() {
		return supportsNumericKeyPadDecimal;
	}

	public void setSupportsNumericKeyPadDecimal(boolean supportsNumericKeyPadDecimal) {
		this.supportsNumericKeyPadDecimal = supportsNumericKeyPadDecimal;
	}

	public String getActionValue() {
		return actionValue;
	}

	
	public String get(String var) {
		ParameterWrapper pw = parameters.get(var);
		if(pw == null) return null;
		Object obj = pw.getParameter();
		if(obj instanceof Var) return ((Var)obj).getString();
		else if(obj instanceof String) return (String)obj;
		else if(obj instanceof Integer) return ((Integer)obj).toString();
		else throw new IllegalStateException("Item '"+var+"' not a supported type.");
	}

	public void createParameters() {
		parameters = new HashMap<String, ParameterWrapper>();
	}
	
	public String spaceToNbsp(String s) {
		StringBuilder sb = new StringBuilder();
		boolean span = false;
		for(int i=0; i< s.length();i++) {
			if(s.charAt(i) == ' ') sb.append("&nbsp;");
			else if(s.charAt(i) < ' ') {
				String c;
				if(s.charAt(i) == '\u0018') c = "blink";
				else if(s.charAt(i) == '\u001a') c = "bright";
				else if(s.charAt(i) == '\u000e') c = "reverse";
				else if(s.charAt(i) == '\u000f') c = "underline";
				else if(s.charAt(i) == '\u000b') c = "error";
				else {
					if(span) sb.append("</span>");
					sb.append("&nbsp;");
					continue;
				}
				sb.append("&nbsp;");
				if(span) sb.append("</span>");
				sb.append("<span class=\"c_" + c + "\">");
				span = true;
			} else if(s.charAt(i) == '"') {
				sb.append("&quot;");
			} else if(s.charAt(i) == '\'') {
				sb.append("&prime;");
			} else {
				sb.append(s.charAt(i));
			}
		}
		if(span) sb.append("</span>");
		return sb.toString();
	}
	
	public void clearSystemFields() {
		rcType = 0;
		messCount = 0;
		cuRec = 0;
		rcIspec = "";
		mssReport = "";
		cuField = "";
		cuBlock = "";
		teachScreen = false;
		for (int i = 0; i < MESSLINE_SIZE; i++)
			messLine[i] = "";
	}

	public ScreenState snapshot() {
		init();
		ScreenState state = new ScreenState();
		copy(this, state);
		return state; 
	}
	
	public static void copy(ScreenState from, ScreenState to)  {
		to.rcType = from.rcType;
		to.rcIspec = from.rcIspec;
		to.messCount = from.messCount;
		to.messLine = from.messLine;
		to.message = from.message;
		to.mssReport = from.mssReport;
		to.cuRec = from.cuRec;
		to.cuField = from.cuField;
		to.cuBlock = from.cuBlock;
		to.stationName = from.stationName;
		to.url = from.url;
		to.userCode = from.userCode;
		to.sessionID = from.sessionID;
		to.persist = from.persist;
		to.persitenceInitialised = from.persitenceInitialised;
		to.storedCursorList = from.storedCursorList;
		to.glbLanguage = from.glbLanguage;
		to.glbStyle = from.glbStyle;
		to.glbLanguageCode = from.glbLanguageCode;
		to.teachScreen = from.teachScreen;
		to.listRepository = from.listRepository;
		to.nofMode = from.nofMode;
		// initialize parameters for both
		from.init();
		to.init();
		for(Iterator<String> it = from.parameters.keySet().iterator(); it.hasNext(); )  {
			String key = it.next();
			// if it doesn't exist as a parameter, add its copy
			if(!to.parameters.containsKey(key))  {
				//log.debug("not exist: " + key);
				to.parameters.put(key, from.parameters.get(key).copy());
			}
			// if exist, set the value
			else  {
				//log.debug("exist: " + key);
//				if(!from.parameters.get(key).isListType()) {
					to.parameters.get(key).set(from.parameters.get(key));
//				} else {
//					to.parameters.get(key).set(from.parameters.get(key).copy());
//				}
			}
		}
	}

	public void init() {
		if(isInitialised)
			return;
		// loop through all the declared fields
		Field[] fields = getClass().getDeclaredFields();
		for (int i = 0; i < fields.length; i++) {
			// take object's value...
			Object object = null;
			try {
				object = fields[i].get(this);
			} catch (IllegalAccessException ex) {
				continue;
			}

			// ... and add to the parameters, wrapping with
			// ParameterWrapper not to worry about the type
			if (object != null && fields[i] != null) {
				try  {
					String n = fields[i].getName();
					if(n != null)
						parameters.put(fields[i].getName(),
								new ParameterWrapper(object,  true));
				} catch(IllegalArgumentException ex)  {
					// don't care
				}
			}
		}
		// set the flag
		isInitialised = true;
	}

	/**
	 * Copy the data in all of the elements in 'screen' into this instance.
	 * Essentially, the <b>set()</b> method is used to copy all of the data
	 * from one <b>ScreenClass</b> object ('screen') to this one.<br>
	 * All items, including System items, are copied. If
	 * {@link #stationName screen.stationName} and
	 * {@link #sessionID screen.sessionID} contain data then those fields in
	 * this instance will be overwritten with the values from the 'screen'
	 * instance. If, however, these values have not been set in 'screen' then
	 * the values in this object will not be overwritten.
	 * 
	 * @param screen
	 *            The source of the <b>ScreenState</b> object data.
	 */
	public void set(ScreenState screen) {
		// save fields
		String tempStationName = screen.stationName;
		String tempSessionID = screen.sessionID;
		String tempUserCode = screen.userCode;
		String tmpUrl = screen.url;
		// set vanilla State
		clearAll();
		copy(screen, this);
		// if needed - restore fields
		if(stationName == null || stationName.length() == 0)
			stationName = tempStationName;
		if(sessionID == null || sessionID.length() == 0)
			sessionID = tempSessionID;
		if(userCode == null || userCode.length() == 0)
			userCode = tempUserCode;
		if(url == null || url.length() == 0)
			url = tmpUrl;
	}
	
	/**
	 * Copy the data items from 'screen' to this.
	 * @param screen
	 */
	public void copyFields(ScreenState screen) {
		for(Iterator<String> it = screen.parameters.keySet().iterator(); it.hasNext(); )  {
			String key = it.next();
			// if it doesn't exist as parameter, add its copy
			if(!this.parameters.containsKey(key))  {
				//log.debug("not exist: " + key);
				this.parameters.put(key, screen.parameters.get(key).copy());
			}
			// if exist, set the value
			else  {
				//log.debug("exist: " + key);
				if(!screen.parameters.get(key).isListType()) {
					this.parameters.get(key).set(screen.parameters.get(key));
				} else {
					this.parameters.get(key).set(screen.parameters.get(key).copy());
				}
			}
		}

	}

	/**
	 * Same as above, but not affecting messaging related objects
	 * 
	 * @param screen
	 */
	public void setLeavingMessages(ScreenState screen) {
		// save fields related to messages
		int tempMessCount = messCount;
		String[] tempMessLine = messLine;
		String tempMessage = message;
		String saveCuField = cuField;
		int saveCuRec = cuRec;
		if(messCount > 0) {
			messLine = new String[MESSLINE_SIZE];
		}
		// reset
		clearAll();
		copy(screen, this);
		// if needed - restore fields related to messages
		if (messCount == 0) {
			messCount = tempMessCount;
			messLine = tempMessLine;
			message = tempMessage;
			cuField = saveCuField;
			cuRec = saveCuRec;
		}
	}

	/**
	 * Clears all the fields defined in the subclassed ScreenClass object. Does
	 * not clear system fields.
	 */
	public void clear() {
		for (Iterator<ParameterWrapper> it = 
			parameters.values().iterator(); it.hasNext();)
			it.next().clear();
	}

	/**
	 * Clears all the fields defined in the subclassed ScreenClass object,
	 * including system fields, except for {@link #sessionID screen.sessionID}),
	 * {@link #stationName screen.stationName} and {@link #userCodeName screen.userCode}
	 */
	public void clearAll() {
		// save fields
		String tempStationName = stationName;
		String tempSessionID = sessionID;
		String tempUserCode = userCode;
		String tmpUrl = url;
		// reset
		clearSystemFields();
		for (Iterator<ParameterWrapper> it = 
			parameters.values().iterator(); it.hasNext();)
			it.next().clear();
		// restore fields
		stationName = tempStationName;
		sessionID = tempSessionID;
		userCode = tempUserCode;
		url = tmpUrl;
	}

	public void clearInquiryFields() {
		for (Iterator<ParameterWrapper> it = 
			parameters.values().iterator(); it.hasNext();)
			it.next().clearIfInquiry();
	}
	
	/**
	 * Clears Check Boxes by setting the values to the off/unchecked value
	 * 
	 */
	public void clearCheckBoxes() {
		for (Iterator<ParameterWrapper> it = 
			parameters.values().iterator(); it.hasNext();)  {
			ParameterWrapper wrapper = it.next();
			if(wrapper.getParameterType() == ParameterWrapper.TYPE_CHECKITEM)
				wrapper.setChecksToOffValue();
		}
	}
	
	/**
	 * Clears all the screen fields inside the copyblock
	 */
	public void clearCopyBlock() {
		clearParametersOfType(ParameterWrapper.TYPE_COPYFROM);
	}
	
	public void clearParametersOfType(int parameterType)  {
		for (Iterator<ParameterWrapper> it = 
			parameters.values().iterator(); it.hasNext();)  {
			ParameterWrapper wrapper = it.next();
			if(wrapper.getParameterType() == parameterType || wrapper.getParameterType() == 0)
				wrapper.clear();
		}
	}

	/**
	 * Sets ispec and rcType which defines where we go next
	 * 
	 * @param pIspec
	 *            The name of the ispec to recall
	 * @return GLB.STATUS
	 */
	public String recallIspec(String pIspec) {
		if(pIspec == null) return "*****";
		
		if (pIspec.trim().equals("")) {
			rcIspec = ispec.replace('-','_');
			rcType = 1;
			return "*****";
		}
		rcIspec = pIspec.replace('-','_');
		 if(ispec == null) ispec = ".";
		if (pIspec.trim().equals(ispec.trim()))
			rcType = 1;
		else 
			rcType = 2;
   
		return "*****";
	}
	
	/**
	 * Compares the new ispec with one of a list of names.
	 * @param names
	 * @return
	 */
	public boolean newIspecOneOf(String ... names) {
		if(rcIspec == null || rcIspec.length() == 0) return false;
		for(String n : names) {
			n = n.replace('-', '_').trim();
			if(rcIspec.trim().equals(n)) return true;
		}
		return false;
	}

	/**
	 * Sets ispec and rcType which defines where we go next
	 * 
	 * @param ispec
	 *            The name of the ispec to recall
	 * @return GLB.STATUS
	 */
	public String recallIspec(Var ispec) {
		return (recallIspec(ispec.getString()));
	}

	/**
	 * Sets ispec and rcType which defines where we go next
	 * 
	 * @param ispec
	 *            The name of the ispec to recall
	 * @return GLB.STATUS
	 */
	public String recallIspec(Group ispec) {
		return (recallIspec(ispec.getString()));
	}

	/**
	 * Display teach screen for specified ispec
	 * 
	 * @param ispec
	 *            The name of the ispec
	 * @param teachScreen
	 *            The name of the teach screen to display
	 * @return GLB.STATUS
	 */
	public String recallTeach(String ispec, String teachScreenName,String locale)
			throws SQLException {
		String l_ispec;
		String l_teach;
		int i;

		l_ispec = ispec.toUpperCase(); // .substring(1,5).replace('-','_');
		l_teach = teachScreenName.toUpperCase(); // .substring(1,5).replace('-','_');
		for (i = 0; i < MESSLINE_SIZE; i++)
			messLine[i] = "";
		String Sql = "select coalesce(text,' ') from teach_screens " + "where ispec = ? and "
				+ "teach_screen_name = ?";
		if(!locale.trim().equals(""))
			Sql+=" and locale = ?";
		Sql+=" order by line desc";
		Connection conn = DBConnectionFactory.getConnection();
		java.sql.CallableStatement stmnt = conn.prepareCall(Sql);
		stmnt.setString(1, Strings.rtrim(l_ispec));
		stmnt.setString(2, Strings.rtrim(l_teach));
		if(!locale.trim().equals(""))
			stmnt.setString(3, Strings.rtrim(locale.toLowerCase()));
		java.sql.ResultSet rs = stmnt.executeQuery();
		i = 0;
		if (rs != null) {
			while (rs.next()) {
				messLine[i++] = spaceToNbsp(rs.getString(1));
				if(i >= MESSLINE_SIZE) break;
			}
			rs.close();
		}
		stmnt.close();
		conn.commit();
		conn.close();
		messCount = i;
		if(i == 0) {
			message("TRY AGAIN - TEACH NOT KNOWN(" + l_teach + ")");
		} else {
			recallIspec(ispec);
			teachScreen=true;
		}
		return ("*****");
	}

	/**
	 * Display teach screen for specified ispec
	 * 
	 * @param ispec
	 *            The name of the ispec
	 * @param teachScreen
	 *            The name of the teach screen to display
	 * @return GLB.STATUS
	 * @throws SQLException
	 */
	public String recallTeach(Var ispec, Var teachScreen, Var locale)
			throws SQLException {
		return (recallTeach(ispec.getString(), teachScreen.getString(), locale.getString()));
	}

	/**
	 * Display teach screen for specified ispec
	 * 
	 * @param ispec
	 *            The name of the ispec
	 * @param teachScreen
	 *            The name of the teach screen to display
	 * @return GLB.STATUS
	 * @throws SQLException
	 */
	public String recallTeach(Var ispec, String teachScreen, Var locale)
			throws SQLException {
		return (recallTeach(ispec.getString(), teachScreen,locale.getString()));
	}
	
	/**
	 * Display teach screen for specified ispec
	 * 
	 * @param ispec
	 *            The name of the ispec
	 * @param teachScreen
	 *            The name of the teach screen to display
	 * @return GLB.STATUS
	 * @throws SQLException
	 */
	public String recallTeach(String ispec, String teachScreen, Var locale)
			throws SQLException {
		return (recallTeach(ispec, teachScreen, locale.getString() ));
	}

	/**
	 * Display teach screen for specified ispec
	 * 
	 * @param ispec
	 *            The name of the ispec
	 * @param teachScreen
	 *            The name of the teach screen to display
	 * @return GLB.STATUS
	 * @throws SQLException
	 */
	public String recallTeach(String ispec, Var teachScreen, Var locale)
			throws SQLException {
		return (recallTeach(ispec, teachScreen.getString(), locale.getString() ));
	}

	/**
	 * Display teach screen for specified ispec
	 * 
	 * @param ispec
	 *            The name of the ispec
	 * @param teachScreen
	 *            The name of the teach screen to display
	 * @return GLB.STATUS
	 */
	public String recallTeach(String ispec, String teachScreen)
			throws SQLException {
		return(recallTeach(ispec, teachScreen, ""));
	}
	
	/**
	 * Display teach screen for specified ispec
	 * 
	 * @param ispec
	 *            The name of the ispec
	 * @param teachScreen
	 *            The name of the teach screen to display
	 * @return GLB.STATUS
	 * @throws SQLException
	 */
	public String recallTeach(Var ispec, Var teachScreen)
			throws SQLException {
		return (recallTeach(ispec.getString(), teachScreen.getString(), ""));
	}

	/**
	 * Display teach screen for specified ispec
	 * 
	 * @param ispec
	 *            The name of the ispec
	 * @param teachScreen
	 *            The name of the teach screen to display
	 * @return GLB.STATUS
	 * @throws SQLException
	 */
	public String recallTeach(Var ispec, String teachScreen)
			throws SQLException {
		return (recallTeach(ispec.getString(), teachScreen, ""));
	}

	/**
	 * Display teach screen for specified ispec
	 * 
	 * @param ispec
	 *            The name of the ispec
	 * @param teachScreen
	 *            The name of the teach screen to display
	 * @return GLB.STATUS
	 * @throws SQLException
	 */
	public String recallTeach(String ispec, Var teachScreen)
			throws SQLException {
		return (recallTeach(ispec, teachScreen.getString(), ""));
	}

	/**
	 * Set up message to display on the screen
	 * 
	 * @param pMessage
	 *            message to display
	 * @return GLB.STATUS
	 */
	public String message(String pMessage) {
		return message(0, pMessage);
	}
	
	public String message(int linenum, String pMessage) {
		// if there is already maximum number of messages, return
		if (messCount >= MESSLINE_SIZE)
			return ("*****");
//		message = pMessage;
		// prepare the message string
		
		int pos = pMessage.indexOf('>');
		if (pos != -1) {
			String firstPart = Strings.rpad(pMessage.substring(0, pos - 1), 20);
			String middle;
			if(linenum > 9) middle = "" + linenum;
			else if(linenum > 0) middle = "0" + linenum;
			else middle = "";
			String secondPart = pMessage.substring(pos, pMessage.length());
//			if (secondPart.length() > 80)
//				secondPart = secondPart.substring(0, 80);
			message = firstPart + middle + secondPart;
		} else
			message = pMessage;
		if(!nofMode) {
			message = spaceToNbsp(message);
			message = message.replaceAll("\"", "").replaceAll("'", ""); //Should really use something like commons-net escapeEcmaScript
		}
		// add it to the array - it will be ignored if one
		// message only one message is added.
		
		messLine[messCount++] = message;
		return ("*****");
	}

	/**
	 * Set up message to display on the screen
	 * 
	 * @param pMessage
	 *            message to display
	 * @return GLB.STATUS
	 */
	public String message(Var pMessage) {
		return (message(pMessage.getString()));
	}

	/**
	 * Set up message to display on the screen
	 * 
	 * @param pMessage
	 *            message to display
	 * @return GLB.STATUS
	 */
	public String message(Group pMessage) {
		return (message(pMessage.getString()));
	}
	/**
	 * Set up message to display on the screen
	 * @param  pMessage message to display
	 * @param Glb GLB class
	 */
	public void message(String pMessage, Glb glb) {
		message(pMessage);
   		glb.ERROR.set("*****");
	}

	/**
	 * Set up message to display on the screen
	 * @param  pMessage message to display
	 * @param Glb GLB class
	 */
	public void message(Var pMessage, Glb glb) {
		message(pMessage);
   		glb.ERROR.set("*****");
	}
	/**
	 * Set up message to display on the screen
	 * @param  pMessage message to display
	 * @param Glb GLB class
	 */
	public void message(Group pMessage, Glb glb) {
		message(pMessage);
   		glb.ERROR.set("*****");
	}
	/**
	 * LINC places messages into the list in reverse order (yes I know!) so before
	 * displaying them we need to reverse the order.
	 */
	public void reverseMessageOrder() {
		if(messCount > 1) {
			String [] tstr = new String[MESSLINE_SIZE];
			int i;
			for(i=0; i<messCount;i++) {
				tstr[i] = messLine[messCount - i - 1];
			}
			for(; i < MESSLINE_SIZE; i++) tstr[i] = "";
			messLine = tstr;
		}
	}

	/**
	 * When a recall is done place cursor in this field
	 * 
	 * @param fld Name of field
	 * @param rec Line number if in Copy block
	 * @param block name of block (BLOCK or COPYBLOCK)
	 */
	public void putCursor(String fld, int rec, String block) {
		cuField = fld;
		cuRec = rec;
		cuBlock = block;
		return;
	}

	/**
	 * When a recall is done place cursor in this field
	 * 
	 * @param fld Name of field
	 */
	public void putCursor(String fld) {
		putCursor(fld, 1, "BLOCK");
		return;
	}

	/**
	 * When a recall is done place cursor in this field
	 * 
	 * @param fld Name of field
	 * @param rec Line number if in Copy block
	 * @param block name of block (BLOCK or COPYBLOCK)
	 */
	public void putCursor(Var fld, int rec, String block) {
		putCursor(fld.getString(), rec, block);
		return;
	}

	/**
	 * When a recall is done place cursor in this field
	 * 
	 * @param fld Name of field
	 */
	public void putCursor(Var fld) {
		putCursor(fld.getString(), 1, "BLOCK");
		return;
	}
	
	public String toString()  {
		StringBuffer buff = new StringBuffer();
		buff.append("{rcType=" + rcType + ", rcIspec=" + rcIspec +
				", messCount=" + messCount);
		if(message != null)
			buff.append(", message='" + message + "'");
		// TODO: play with messLine - too late for this
		buff.append(", url="+url);
		buff.append(", mssReport=" + mssReport + ", cuRec=" + cuRec + 
				", glbLanguage=" + glbLanguage + ", glbLanguageCode=" + glbLanguageCode +
				", cuBlock=" + cuBlock + ", stationName=" +
				stationName + ", userCode=" + userCode + ", sessionID=" + sessionID + 
				", parameters=" + parameters + "}");
		return buff.toString();
	}

    public int getRcType() {
        return rcType;
    }

    public String getRcIspec() {
        return rcIspec;
    }

    public int getMessCount() {
        return messCount;
    }

    public String[] getMessLine() {
        return messLine;
    }

    public String getMessage() {
        return message;
    }

    public String getMssReport() {
        return mssReport;
    }

    public int getCuRec() {
        return cuRec;
    }

    public String getCuField() {
        return cuField;
    }

    public String getCuBlock() {
        return cuBlock;
    }

    public String getStationName() {
        return stationName;
    }

    public String getUserCode() {
        return userCode;
    }

    public String getSessionID() {
        return sessionID;
    }

    public String getGlbError() {
        return glbError;
    }

    public Map<String, ParameterWrapper> getParameters() {
        return parameters;
    }

    //TODO remove
	public ListRepository getRepository() {
		return listRepository;
	}    
	
	public void storeRepository(ListRepository repository) {
		if(log.isDebugEnabled())
			log.debug("ScreenState: storeRepository: " + repository);
		listRepository = repository;
	}

	public String getDataAsTPString() {
		StringBuilder sb = new StringBuilder();
		
		//Loop through the declared fields in order.
		/*
		Field[] fields = getClass().getDeclaredFields();
		for (int i = 0; i < fields.length; i++) {
			// take object's value...
			Object object = null;
			try {
				object = fields[i].get(this);
			} catch (IllegalAccessException ex) {
				continue;
			}

			if (object != null && fields[i] != null) {
				try  {
					String n = fields[i].getName();
					sb.append(n);
//					if(n != null)
//						parameters.put(fields[i].getName(),
//								new ParameterWrapper(object));
				} catch(IllegalArgumentException ex)  {
					// don't care
				}
			}
		}*/
		
		Iterator<Entry<String, ParameterWrapper>> it = parameters.entrySet().iterator();
		while(it.hasNext()) {
			Entry<String, ParameterWrapper> entry = it.next();
			String name = entry.getKey();
			ParameterWrapper p = entry.getValue();
			Var x = p.getVar();
			sb.append(name);
			sb.append('=');
			if(x == null) {
				Var [] va = p.getVarArray();
				if(va == null) 
					sb.append("unknown-parameter-type");
				else {
					sb.append("[");
					boolean comma = false;
					for(Var v : va) {
						if(comma) sb.append(",");
						else comma = true;
						sb.append(v.getString());
					}
					sb.append("]");
				}
						
			} else
				sb.append(p.getVar().getString());
			sb.append("|");
		}
		return sb.toString();
	}    
}

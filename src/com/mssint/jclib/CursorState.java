package com.mssint.jclib;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CursorState implements Serializable {
	private static final Logger log = LoggerFactory.getLogger(CursorState.class);
	private static final long serialVersionUID = 2L;

	public ArrayList<Object> varList = null;
	public Map<String, Object> varMap = null;

	// These vars are for Cursor V2
	private String tableName;
	private String colPrefix = "";
	protected String cs_rowid;
	protected long cs_ident;
	protected boolean sqlServer = true;
	protected boolean cs_rowidValid = false;
	protected boolean cs_cursorOpen = false;
	protected boolean cs_bypass = false;
	protected int cs_counter = 0;
	protected boolean cs_mssRestartCursor = false;
	
	// Local (static) database variables. */
	//protected String cs_saveMssRowid;
	protected String cs_reloadRowid;
	protected SelectItem lastActiveSelectItem;
	
	//For management of caching
	private boolean cacheEnabled = false;
	private CursorCache cursorCache = null; 
	
	//GLB.DTIME 
	private boolean glbDtimeNeeded = false;
	
	//Ident col for SQL/Server
	String identCol="unique_id";
	
	private ApplicationState app;
	
	/**
	 * Uses reflection to add all Var type fields to an array. Columns with the same name
	 * MUST exist in the database table.
	 * @param app 
	 */
	protected void initialiseCursorState(ApplicationState app) {
		this.app = app;
	    varList = new ArrayList<Object>();
		varMap = new HashMap<String, Object>();
	    int offset = 0;
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

	        if (object != null) {
	            if(object instanceof Var) {
	                try {
	                    //Util.debug("ADDING "+ fields[i].getName());
	                    FrameVar v = new FrameVar(i, 1, object);
	                    v.setName(fields[i].getName());
	                    int exlen = v.getExtractString().length();
	                    v.setExlen(exlen);
	                    v.setOffset(offset);
	                    offset += exlen;
	                    varList.add(v);
						varMap.put(v.getName(), v);
	                } catch(IllegalArgumentException ex)  {
	                    // don't care
	                }
	            } 
				/* For the moment lets not worry about arrays.
				else if(object.getClass().isArray()) {
					if(((Object[])object)[0] instanceof Var)
					    Util.debug("ADDING 1D ARRAY "   + fields[i].getName() + ": " + object);
				    else if(((Object[])object)[0].getClass().isArray() && ((Object[][])object)[0][0] instanceof Var)
				        Util.debug("ADDING 2D ARRAY "   + fields[i].getName() + ": " + object);
				}
				*/
	        }
	    }
	    
	    //Check caching. First check for report-name caching as in
	    //    jclib.cache.<reportname> = tablename, tablename
	    //If not found, check
	    //    jclib.cache.<tablename> = true/false
	    //Lastly, check "jclib.cache" for table names.
	    boolean checked = false;
	    boolean cache = false;
	    
	    //Check for specific report entry as in "jclib.cache.<reportname> = list-of-table-names"
	    if(app != null) {
	    	String p = "jclib.cache." + app.REPORTNAME;
	    	String s = Config.getProperty(p.toLowerCase());
	    	if(s != null) {
	    		cache = checkInList(s, tableName);
	    		if(cache) checked = true;
	    	}

	    	//Check for specific entry as in "jclib.cache.<tablename> = true/false"
	    	if(!checked) {
	    		p = "jclib.cache." + tableName;
	    		s = Config.getProperty(p.toLowerCase());
	    		if(s != null) {
	    			checked = true;
	    			if(s.equalsIgnoreCase("true")) cache = true;
	    		}
	    	}

	    	//Check for general entry, as in "jclib.cache = table list"
	    	if(!checked) {
	    		p = "jclib.cache";
	    		s = Config.getProperty(p);
	    		if(s != null) {
	    			cache = checkInList(s, tableName);
	    		}
	    	}
	    	if(cache) {
	    		log.info("Enable cache for table " + tableName);
	    		enableCache();
	    	}
	    }
	    String p = "jclib.glbdtime";
	    String s = Config.getProperty(p);
	    if(s != null && s.equalsIgnoreCase("true")) {
	    	glbDtimeNeeded=true;
	    }
	    p = "jclib.database";
	    s = Config.getProperty(p);
 
	    if(s != null && s.equalsIgnoreCase("sqlserver")) {
	    	sqlServer=true;
	    }
	    
	}
	
	private boolean checkInList(String list, String find) {
		if(list == null || find == null) return false;
		String [] arr = list.split("[,:; ]");
		for(String x : arr) {
			if(x.equalsIgnoreCase(find)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Enable caching on this cursor
	 */
	public void enableCache() {
		cacheEnabled = true;
	}
	
	/**
	 * Disable caching on this cursor
	 */
	public void disableCache() {
		cacheEnabled = false;
	}

	/**
	 * @return true if caching is enabled on this cursor, otherwise false.
	 */
	public boolean cacheEnabled() {
		return cacheEnabled;
	}

	/**
	 * If caching is enabled, and if it has not already been loaded, load the cache.
	 * @throws SQLException 
	 */
	public void loadCache(SelectItem select) throws SQLException {
		if(!cacheEnabled) return;
		if(cursorCache != null) return;
		cursorCache = new CursorCache(this, select);
	}

	public CursorCache getCache() {
		return cursorCache;
	}
	
	public void print(String s) {
		System.out.println("CONTENTS OF TABLE "+tableName+": "+s+"\n");
		for(Object obj : varList) {
			if(!(obj instanceof FrameVar))
				continue;
			((FrameVar)obj).print();
		}
	}
	
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public String getTableName() {
		return tableName;
	}
	
	public void setColPrefix(String colPrefix) {
		this.colPrefix = colPrefix;
	}

	public String getColPrefix() {
		return colPrefix;
	}
	public void setUseGlbDtime() {
		this.glbDtimeNeeded = true;
	}

	public boolean getUseGlbDtime() {
		return glbDtimeNeeded;
	}
	
	public void printIndexValues() {
		if(lastActiveSelectItem == null)
			return;
		lastActiveSelectItem.printIndexValues();
	}
	/**
	 * Return a String which consists of a comma separated list of field names. This list
	 * may be submitted to sql statements such as select and insert.
	 */
	private String thisFieldList = null;
	int fieldCount = -1;
	protected String getFieldList() {
		if(thisFieldList != null)
			return thisFieldList;
		fieldCount = 0;
		StringBuilder sb = new StringBuilder();
		for(Object obj : varList) {
			if(fieldCount > 0)
				sb.append(", ");
			if(!(obj instanceof FrameVar))
				continue;
			if(!sqlServer || getColPrefix() != "") {
				sb.append(getColPrefix());
				sb.append(((FrameVar)obj).getName());
			} else sb.append(Util.fixSqlServerReserved(((FrameVar)obj).getName()));
			fieldCount++;
		}
		thisFieldList = sb.toString();
		return thisFieldList;
	}
	
	private String thisFieldUpdateList = null;
	protected String getUpdateFieldList() {
		if(thisFieldUpdateList != null)
			return thisFieldUpdateList;

		StringBuilder sb = new StringBuilder();
		fieldCount=0;
		for(Object obj : varList) {
			if(!(obj instanceof FrameVar))
				continue;
			if(sb.length() > 0)
				sb.append(",\n");
			if(!sqlServer || getColPrefix() != "") {
				sb.append(getColPrefix());
				sb.append(((FrameVar)obj).getName());
			} else
				sb.append(Util.fixSqlServerReserved(((FrameVar)obj).getName()));
			sb.append(" = ?");
			fieldCount++;
		}
		thisFieldUpdateList = sb.toString();
		return thisFieldUpdateList;
	}
	
	/**
	 * Return the number of fields which occur in the cursor.
	 * @return count of the number of fields in table
	 */
	protected int getParameterCount() {
		if(fieldCount == -1)
			getFieldList();
		return fieldCount;
	}
	
	private String questionMarkList = null;
	/**
	 * Return a list of comma separated question marks which match the fields defined for
	 * this cursor.
	 * @return String of the form "?,?,?,?" with one ? for each field.
	 */
	protected String getQuestionMarkList() {
		if(questionMarkList != null)
			return questionMarkList;
		if(fieldCount == -1)
			getFieldList();
		StringBuilder sb = new StringBuilder();
		for(int i=0; i < fieldCount; i++) {
			if(i > 0) sb.append(',');
			sb.append('?');
		}
		questionMarkList = sb.toString();
		return questionMarkList;
	}
	
	protected void storeValues(CallableStatement stmnt) throws SQLException {
		if(fieldCount == -1)
			getFieldList();
		for(int i=0; i < fieldCount; i++) {
			FrameVar fv = (FrameVar) varList.get(i);
			Var v = fv.getVar();
			if(v.testAttr(Var.CHAR) || v.testAttr(Var.PICTURE)) {
				String s = v.rtrim();
				stmnt.setString(i+1 ,s.length()!=0 ? s : " ");
			} else if(v.testAttr(Var.NUMBER) && v.scale == 0) {
				stmnt.setLong(i+1 ,v.getLong());
			} else {
				stmnt.setDouble(i+1, v.getDouble());
			}
		}
	}

	protected void storeValues(PreparedStatement stmnt) throws SQLException {
		if(fieldCount == -1)
			getFieldList();

		for(int i=0; i < fieldCount; i++) {
			FrameVar fv = (FrameVar) varList.get(i);
			Var v = fv.getVar();
			if(v.testAttr(Var.CHAR) || v.testAttr(Var.PICTURE)) {
				String s = v.rtrim();
				stmnt.setString(i+1 ,s.length()!=0 ? s : " ");
			} else if(v.testAttr(Var.NUMBER) && v.scale == 0) {
				stmnt.setLong(i+1 ,v.getLong());
			} else {
				stmnt.setDouble(i+1, v.getDouble());
			}
		}
	}

	protected void recoverValues(ResultSet rs) throws SQLException {
		if(fieldCount == -1)
			getFieldList();

		for(int i=0; i < fieldCount; i++) {
			FrameVar fv = (FrameVar) varList.get(i);
			Var v = fv.getVar();
			if(v.testAttr(Var.CHAR) || v.testAttr(Var.PICTURE)) {
				v.set(rs.getString(i+1));
			} else if(v.testAttr(Var.NUMBER) && v.scale == 0) {
				v.set(rs.getLong(i+1));
			} else {
				v.set(rs.getDouble(i+1));
			}
		}
	}


	public void recoverValues(Object[] vars) {
		if(fieldCount == -1)
			getFieldList();

		for(int i=0; i < fieldCount; i++) {
			FrameVar fv = (FrameVar) varList.get(i);
			Var v = fv.getVar();
			if(vars[i] instanceof Var) {
				v.set((Var)vars[i]);
			}
		}
	}

	/**
	 * Copy the data and status from one cursor to another. The assumption is that BOTH cursors
	 * are the same class.
	 * @param source The source cursor, which must be the same as the target.
	 */
	public void set(CursorState source) {
		if(fieldCount == -1)
			getFieldList();

		for(int i=0; i < fieldCount; i++) {
			FrameVar tfv = (FrameVar) varList.get(i);
			Var tv = tfv.getVar();
			FrameVar sfv = (FrameVar) source.varList.get(i);
			Var sv = sfv.getVar();
			tv.set(sv);
		}
		this.cs_rowid=source.cs_rowid;
		this.cs_ident=source.cs_ident;
		this.cs_rowidValid=source.cs_rowidValid;
	}

	/**
	 * Clears each Var item in the cursor.
	 */
	public void clear() {
		if(fieldCount == -1)
			getFieldList();

		for(int i=0; i < fieldCount; i++) {
			FrameVar tfv = (FrameVar) varList.get(i);
			Var tv = tfv.getVar();
			tv.clear();
		}
	}

	
	/**
	 * Insert the values of this cursor into the database as a new record.
	 * If GLB.ERROR is set to "*****" the no insert will be attempted.
	 * @return 0 for successful insert, else -1. Also, GLB.STATUS set to "*****"
	 * @throws SQLException
	 */
	public int insert() throws SQLException {
		if(app.GLB.ERROR.eq("*****")) return 0;
		String sql;
		PreparedStatement stmnt;
		ResultSet rs;
		int glbunique=0;
		
		Sql.sqlBt(tableName + ".insert()");
		
		//sql = "begin insert into " +
		if(sqlServer) {
			sql = "select next value for glbunique";
			stmnt = app.connection.prepareStatement(sql);
			rs=stmnt.executeQuery();
			rs.next();
			glbunique = rs.getInt(1);
		}
		sql = "insert into " +
			tableName + 
			" (" +
			getFieldList();
		if(glbDtimeNeeded) {
			sql += ",glb_dtime";
		}
		sql += ") values (" +
			getQuestionMarkList();
		if(glbDtimeNeeded) {
            if(!sqlServer)
				sql += ",glbunique.nextval";
			else
				sql += ",?";
		}
		//sql += ") returning rowid into ?; end;";
		sql += ")";

		if(sqlServer)
			sql = sql.toUpperCase();
		stmnt = app.connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
		
		//stmnt.registerOutParameter(getParameterCount() + 1,java.sql.Types.CHAR);
		
		storeValues(stmnt);
		if(sqlServer)
			stmnt.setInt(getParameterCount()+1,glbunique);

		try {
			//rs=stmnt.executeQuery();
			stmnt.executeUpdate();
		} catch(SQLException sqlEx) {
Config.log.info("new sqlEx.getErrorCode()=" + sqlEx.getErrorCode());
			if (!sqlServer && sqlEx.getErrorCode() == 1)  {
				//app.GLB.setStatus("*****");
				log.error("Duplicate database entry - aborting");
				//System.err.println(sqlEx);
				app.connection.rollback();
				cs_rowidValid = false;
				cs_rowid = null;
				stmnt.close();
				stmnt = null;
				Sql.sqlEt();
				return -1;
			} else if (sqlServer && sqlEx.getErrorCode() == 2601)  {
				app.GLB.setStatus("*****");
				cs_rowidValid = false;
				cs_rowid = null;
				stmnt.close();
				stmnt = null;
				Sql.sqlEt();
				return 0;
			} else
				throw sqlEx;
		}
		//cs_rowid = stmnt.getString(getParameterCount() + 1);
		rs = stmnt.getGeneratedKeys();
		rs.next();
		if (sqlServer) {
			cs_ident = rs.getLong(1);
		} else {
			cs_rowid = rs.getRowId(1).toString();
		}
		
		cs_rowidValid = true;
		app.GLB.setStatus(0);
		rs.close();
		rs = null;
		stmnt.close();
		stmnt = null;
		Sql.sqlEt();
		return 0;
	}
	
	/**
	 * Delete's the currently selected record from the database.
	 * @return
	 * @throws SQLException
	 */
	public int delete() throws SQLException {
		if(!cs_rowidValid) return 0;
		Sql.sqlBt(tableName + ".delete()");
		String sql;
		if (sqlServer)
			sql="delete from " + tableName.toUpperCase() + " where " + identCol + " = ?";
		else
			sql="delete from " + tableName + " where rowid = ?";
		PreparedStatement stmnt;
		stmnt = app.connection.prepareCall(sql);
		if (sqlServer)
			stmnt.setLong(1, cs_ident);
		else
			stmnt.setString(1, cs_rowid);
			
		stmnt.execute();
		stmnt.close();
		stmnt = null;
		//rowidValid = false;
		app.GLB.setStatus(0);
		Sql.sqlEt();
		return 0;
	}
	
    public void lockRow() throws SQLException {
        lockRow(true);
    }

	private void lockRow(boolean lock) throws SQLException {
		if(!cs_rowidValid) return;
		Sql.sqlBt(tableName + ".lockRow()");
		String sql="";
		if(sqlServer)
			sql = "select ";
		else
			sql = "select /*+ ROWID("+tableName+") */ ";
		sql += getFieldList() +
		" from " + tableName;
		if(sqlServer && lock)
            sql += " with (UPDLOCK, ROWLOCK) ";
		if(sqlServer) {
			sql = sql.toUpperCase();
			sql += " where " + identCol + " = ?";
		} else
			sql += " where rowid = ?";
		if(!sqlServer && lock)
            sql += " for update";
		PreparedStatement stmnt = app.connection.prepareCall(sql);
		if (sqlServer)
			stmnt.setLong(1, cs_ident);
		else
			stmnt.setString(1, cs_rowid);
		ResultSet rs=stmnt.executeQuery();
		rs.next();
		recoverValues(rs);
		rs.close();
		rs = null;
		stmnt.close();
		stmnt = null;
		cs_rowidValid = true;
		app.GLB.setStatus(0);
		Sql.sqlEt();
		return;
	}
	
	public boolean fetchItem(SelectItem select) throws SQLException {
		Sql.sqlBt(tableName + ".fetch_" + select.getCursorName());
		if(select.fetch()) {
			cs_rowidValid = true;
			if(sqlServer)
				cs_ident = select.getIdent();
			else
				cs_rowid = select.getRowid();
				
			if(select.getRowCount() == 1) 
				app.GLB.setStatus(0);
			else
				app.GLB.resetGlbStatus();
		} else {
			if(select.getRowCount() == 0) 
				app.GLB.setStatus("*****");
			else
				app.GLB.setStatus(0);
			select.setRowCount(1); //stop setting GLB.STATUS if called again
			Sql.sqlEt();
			return false;
		};
		//cs_saveMssRowid = cs_rowid;
		if(Sql.sqlIntegrity(tableName))
			reload();
		Sql.sqlEt();
		return true;
	}
	
	public int reload() throws SQLException {
        lockRow(false);
		return 0;
	}
	
	public int update() throws SQLException {
		return this.update(false);
	}
	
	public int update(boolean handleCw) throws SQLException {
		if(!cs_rowidValid) {
			app.GLB.setStatus("*****");return -1;
		}
		Sql.sqlBt(tableName + ".update()");
		String sql = "update " + tableName + " set " + getUpdateFieldList() + " where ";
		if (sqlServer) {
			sql = sql.toUpperCase();
			sql += identCol + " = ?";
		} else
			sql += "rowid = ?";
		PreparedStatement stmnt = app.connection.prepareCall(sql);
	
		storeValues(stmnt);
		if(sqlServer)
			stmnt.setLong(getParameterCount() + 1, cs_ident);
		else
			stmnt.setString(getParameterCount() + 1, cs_rowid);

		stmnt.execute();
		stmnt.close();
		stmnt = null;
		app.GLB.setStatus(0);
		Sql.sqlEt();
		return 0;
	}
	

	public String getExtractString() {
		if(varList == null) initialiseCursorState(null);
		StringBuilder sb = new StringBuilder();
		for(int i=0; i < varList.size(); i++) {
			FrameVar v = (FrameVar)varList.get(i);
			sb.append(v.getExtractString());
		}
		return sb.toString();
	}
	
	/**
	 * Return a FrameVar initialised as a sort key, typically of a CursorState object.
	 * @param name The name of the key. To specify a descending key, add a ".descend"
	 * suffix to the key.
	 * @return The FrameVar key object
	 */
	public FrameVar getFrameVar(String name) {
	    if(varMap == null) initialiseCursorState(null);
    	int i = name.indexOf(":descend");
    	if(i != -1) name = name.substring(0, i);
    	Object obj = varMap.get(name);
    	if(!(obj instanceof FrameVar)) return(FrameVar)obj;
    	FrameVar v = (FrameVar)obj;
    	if(i == -1) return v;
    	else return v.descend();
	}
	
	public FrameVar getFrameVar(Var v) {
	    if(varList == null) initialiseCursorState(null);
		for(int i=0; i<varList.size();i++) {
			FrameVar fv = (FrameVar)varList.get(i);
			if(fv.getVar() == v) {
				return fv;
			}
		}
	    return null;
	}
	
	/**
	 * Copies all defined Var type variables in the ScreenState object to Var
	 * type variables defined in this CursorState object which have the same name.
	 * @param screen The ScreenState object from which to source the data.
	 */
	public void set(ScreenState screen) {
		//Iterate through all screen vars and check if one with the same 
		//name exists in my own map.
	    Field[] fields = getClass().getDeclaredFields();
	    for (int i = 0; i < fields.length; i++) {
	        Object object = null;
	        try {
	            object = fields[i].get(this);
	        } catch (IllegalAccessException ex) {
	            continue;
	        }
	        if(object instanceof Var) {
	        	String name = fields[i].getName();
				if(screen.parameters.containsKey(name))  {
					Var dest = (Var)object;
					Var source = screen.parameters.get(name).getVar();
					if(!source.testAttr(Var.INPUT) && !source.testAttr(Var.INQUIRY))
						dest.set(source);
				}
	        	
	        }
	    }
	}
	
	/**
	 * Copy all matching fields from copy to me.
	 * @param copy
	 */
	public void copy(CursorState copy) {
		Field[] myFields = getClass().getDeclaredFields();
		Field[] cpFields = copy.getClass().getDeclaredFields();

		for(int i=0; i<myFields.length && i<cpFields.length;i++) {
			Object myObject, cpObject;
			try {
				myObject = myFields[i].get(this);
				cpObject = cpFields[i].get(copy);
			} catch (IllegalAccessException ex) {
				continue;
			}
			if(myObject == null || cpObject == null) continue;
			if(myFields[i].getName().equals(cpFields[i].getName())) {
				if(myObject instanceof Var && cpObject instanceof Var) {
					if(Config.DEBUG) System.out.println("copy(): Copying field " + myFields[i].getName());
					((Var)myObject).set((Var)cpObject);
				}
			}
		}
	}
	public Connection getConnection() {
		return app.connection;
	}

	/**
	 * Create a new Object array containing copies of the Var objects of this cursor
	 * @return
	 */
	public Object [] getDataSetVars() {
		Object [] olist = new Object[getParameterCount()];
		int idx = 0;
		for(Object o : varList) {
			Var v;
			if(o instanceof FrameVar) {
				v = new Var(((FrameVar)o).getVar());
			} else continue;
			olist[idx++] = v;
		}
		return olist;
	}
	public String getRowid() {
		return cs_rowid;
	}
	public void setIdentCol(String identCol) {
		this.identCol = identCol;
	}

	public String getIdentCol() {
		return identCol;
	}
}

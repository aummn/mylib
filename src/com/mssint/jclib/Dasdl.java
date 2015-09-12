package com.mssint.jclib;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides DASDL like access to a migrated DMS (II) source to a relational schema.
 * 
 * Specific implementations extend this class, and define all the fields of interest (columns).
 * 
 * It provides the key functionality for iterating/saving & deleting data on an underlying
 * base table. All specific data types will extend Dasdl i.e. Address, Employee, ... 
 * 
 * @author MssInt
 */
public class Dasdl {
	private static final Logger log = LoggerFactory.getLogger(Dasdl.class);
	private boolean isInitialised = false;
	protected JbolApplication jbolApp;
	private Map<String, Var> columns;
	private Vector<Var> columnList;
	//private Vector<Var> tempColumnList;
	private String _tableName = null;
	private String _dataSource = null;
	private String _selectList = null;
	protected Connection connection = null;
	private String _saveWhere = null; //used to check whether a new cursor is required.
	private String _saveOrder = null;
	private String _lastNext = null;
	private String _lastBack = null;
	private boolean _lastFirstNotFound = false;
	private boolean _backOpen = false;
	private int currentRecord = -1;
	private DasdlSet dSet = null;
	private int uniqueId = 0;
	private boolean _setToBeginning;
	private boolean _setToEnd;
	private int _setToUniqueId;

	
	private ResultSet cursor = null;
	private Statement stmnt = null;
	private SQLException sqlEx = null;
	
	/**
	 * Initialise the Dasdl object.
	 * Initialise any variables defined in the inheriting implementation.
	 * 
	 * All instance variables are assumed to be columns unless they are one of the following:-
	 * A String with the name tablename defining the table used for querying 
	 * A String with the name dataSource defining the data source 
	 * Any Dasdl fields 
	 * Any DasdlSet fields 
	 * 
	 * @param app the JbolApplication (extending instance) whose is going to be updated with DNS status'
	 * @param c cannot be null
	 * @return initialised Dasl Object
	 */
	public Dasdl init(JbolApplication app, Connection c) {
		if(isInitialised) return this;
		jbolApp = app;
		if(c == null) throw new IllegalArgumentException("Connection parameter is null");
		connection = c;
		
		buildFieldList();
		
		if(_tableName == null) {
			//Set to the name of the class
			_tableName = this.getClass().getSimpleName().toUpperCase();
		}
		//log.debug("TABLENAME=" + _tableName);
		
		// set the flag
		isInitialised = true;
		return this;
	}

	public Dasdl init(Dasdl parent) {
		if(isInitialised) return this;
		jbolApp = parent.jbolApp;
		connection = parent.connection;
		
		buildFieldList();
		
		if(_tableName == null) {
			_tableName = parent.getClass().getSimpleName().toUpperCase();
		}
		isInitialised = true;
		return this;
	}

	private void buildFieldList() {
		columns = new HashMap<String, Var>();
		columnList = new Vector<Var>();
		StringBuilder sb = new StringBuilder();
		//tempColumnList = new Vector<Var>();
		Field[] fields = getClass().getDeclaredFields();
		boolean first = true;
		for (int i = 0; i < fields.length; i++) {
			// take object's value...
			Object object = null;
			try {
				object = fields[i].get(this);
			} catch (IllegalAccessException ex) {
				continue;
			}

			if (object != null) {
				if(object instanceof String) {
					if(fields[i].getName().compareTo("tableName") == 0) {
						_tableName = (String)object;
					} else if(fields[i].getName().compareTo("dataSource") == 0) {
						_dataSource = (String)object;
					}
				} else if(object instanceof Dasdl) {
					Map<String, Var> cols = ((Dasdl)object).getColumnMap();
					Vector<Var> colList = ((Dasdl)object).getColumnList();
					columns.putAll(cols);
					columnList.addAll(colList);
					if(!first) sb.append(", ");
					else first = false;
					sb.append(((Dasdl)object).getSelectList());
				} else if(object instanceof DasdlSet) {
				} else {
					try  {
						if(!first) sb.append(", ");
						else first = false;
						sb.append(fields[i].getName());
						columns.put(fields[i].getName(), (Var)object);
						columnList.add((Var)object);
						//log.debug("Added field " + fields[i].getName());
					} catch(IllegalArgumentException ex)  {
						//log.debug("Not Added field " + fields[i].getName());
						throw ex;
					}
				}
			}
		}
		_selectList = sb.toString();
		
	}
	
	public String getString() {
		StringBuilder sb = new StringBuilder();
		for(Var v : columnList) {
			sb.append(v.getLincString());
		}
		return sb.toString();
	}
	
	private String spaces = null;
	public void set(String s) {
		if(s == null) s = "";
		int l = recordLength();
		if(s.length() < l) {
			if(spaces == null) {
				StringBuilder sb = new StringBuilder();
				for(int i=0;i<l;i++) sb.append(' ');
				spaces = sb.toString();
			}
			s += spaces.substring(0, l - s.length());
		}
		int offset = 0;
		for(Var v : columnList) {
			v.set(s.substring(offset, offset + v.displayLength));
			offset += v.displayLength;
		}
	}
	
	public int recordLength() {
		int len = 0;
		for(Var v : columnList) {
			len += v.displayLength;
		}
		return len;
	}
	
	private Vector<Var> getColumnList() {
		return columnList;
	}


	private Map<String, Var> getColumnMap() {
		return columns;
	}

	
	private ResultSet findCursor(Object [] obj) 
		throws SQLException, IndexOutOfBoundsException {
		boolean rebuild;
		if(!isInitialised) 
			throw new IllegalStateException("Class not initialised.");
		String where = null;
		String dWhere = null;
		String orderBy = null;
		int nextObj = 0;
		
		if(obj == null) 
			throw new IllegalArgumentException("Null parameter not allowed.");
		
		if(obj.length > nextObj) { //obj[0] must be null or a DasdlSet
			if(obj[nextObj] != null) {
				if(obj[nextObj] instanceof DasdlSet) {
					dSet = (DasdlSet)obj[nextObj++];
				}
			}
		}
		if(dSet != null) {
			dWhere = dSet.getWhere();
			if(_lastFirstNotFound || _backOpen)
				orderBy = dSet.getBackOrderBy();
			else
				orderBy = dSet.getOrderBy();
		}
		
		if(obj.length > nextObj) { //obj[1] is a where clause (as string)
			if(obj[nextObj] instanceof String) where = (String)obj[nextObj++];
			else throw new IllegalArgumentException("Parameters are [DasdlSet,] [String].");
		}
		if(cursor == null) rebuild = true;
		else rebuild = false;
		//There must now be a single parameter for each ? in the where clause,
		//if present.
		if(where != null) {
			int pos = 0;
			int x;
			StringBuilder w = new StringBuilder();
			
			while(pos < where.length()) {
				x = where.substring(pos).indexOf('?');
				if(x < 0) {
					w.append(where.substring(pos));
					break;
				} 
				w.append(where.substring(pos, pos + x));
				pos += x + 1;
				if(nextObj >= obj.length) 
					throw new IndexOutOfBoundsException("Insufficient arguments for '?' in where clause");
				Object o = obj[nextObj++];
				if(o instanceof String) w.append("'" + (String)o + "'");
				else if(o instanceof Integer) w.append(o);
				else if(o instanceof Long) w.append(o);
				else if(o instanceof Double) w.append(o);
				else if(o instanceof Var) {
					Var v = (Var)o;
					if(v.testAttr(Var.STRING)) w.append("'"+v.getString()+"'");
					else w.append(v.getString());
				} else throw new IllegalArgumentException("Type for parameter "+(nextObj-1)+" not handled.");
			}
			where = w.toString();
		}
		if(where == null && dWhere != null) where = dWhere;
		else if(where != null && dWhere != null) 
			where = "("+dWhere + ") and (" +where +")";  
		
		if(where != null) {
			if(!rebuild) { //Check if the where clause has changed
				if(_saveWhere == null || !_saveWhere.equals(where)) {
					rebuild = true;
					_saveWhere = where;
				}
			} else _saveWhere = where;
		} else if(_saveWhere != null) {
			rebuild = true;
			_saveWhere = null;
		}
		if(orderBy != null) {
			if(!rebuild) {
				if(_saveOrder == null || !_saveOrder.equals(orderBy)) {
					rebuild = true;
					_saveOrder = orderBy;
				}
			} else _saveOrder = orderBy;
		} else if(_saveOrder != null) {
			rebuild = true;
			_saveOrder = null;
		}
		
		//Rebuild a cursor. True if either the order by or where clause have
		//changed, or else it's the first time.
		if(rebuild) {
			if(log.isDebugEnabled())
				log.debug("REBUILDING CURSOR");
			if(cursor != null) cursor.close();
			StringBuilder select = new StringBuilder();
			select.append("select " + getSelectList() + " from " + getTableName());
			if(where != null) select.append(" where " + where);
			if(orderBy != null) select.append(" order by " + orderBy);
			if(log.isDebugEnabled())
				log.debug(select.toString());
			if(stmnt != null) stmnt.close();
			stmnt = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			cursor = stmnt.executeQuery(select.toString());
		}
		return cursor;
	}
	
	
	private void loadData(ResultSet rs) throws SQLException {
		int colCount = rs.getMetaData().getColumnCount();
		currentRecord = rs.getRow();
		for(int j=0; j < colCount; j++) {
			columnList.get(j).set(rs.getString(j+1));
		}
		uniqueId=columns.get("UNIQUE_ID").getInt();
		if(dSet != null) dSet.setUniqueId(uniqueId); 
		dSet = null;
	}
	public void clear()  {
		for(int j=0; j < columnList.size(); j++) {
			columnList.get(j).clear();
		}
	}
	private void clearDmstatus (){
	    jbolApp.DMERROR = false; 
	    jbolApp.DMSTATUS[JbolApplication.DMCATEGORY] = 0; 
	    jbolApp.DMSTATUS[JbolApplication.DMERRORTYPE] = 0; 
	    jbolApp.DMSTATUS[JbolApplication.DMSTRUCTURE] = 0;
	}
	private void setDmstatus(int structure,int category,int errorType) {
	    jbolApp.DMERROR = true; 
	    jbolApp.DMSTATUS[JbolApplication.DMCATEGORY] = category; 
	    jbolApp.DMSTATUS[JbolApplication.DMERRORTYPE] = errorType; 
	    jbolApp.DMSTATUS[JbolApplication.DMSTRUCTURE] = structure;
	}
	private void setDmstatus(int category,int errorType) {
		setDmstatus(0, category, errorType);
	}
	
	/**
	 * Finds the first occurrence of a data set. In particular the parameters
	 * passed in define what needs to be selected.
	 * @param obj is of 2 forms viz: - 
	 * <br>
	 * An explicit where clause with fully qualified data
	 * <br>
	 * A parameterised query with the first object being the parametrised where clause portion
	 * and all subsequent values being the parameter values to the parameterised query
	 * @return true if we are at the start, false otherwise
	 * @throws SQLException
	 */
	public boolean findFirst(Object ... obj) throws SQLException {
		DasdlSet dSetOrig = dSet;
		cursor = null;
		Sql.sqlBt(_tableName+".findFirst()");
		ResultSet rs = findCursor(obj);
		_lastNext = null;
		_lastBack = null;
		_lastFirstNotFound=false;
		_backOpen=false;
		_setToBeginning=false;
		_setToEnd=false;
		clearDmstatus();
		if(rs.first()) {
			loadData(rs);
			Sql.sqlEt();
			return true;
		} 
		if((obj != null && obj.length > 0) && dSetOrig != null){
			_lastFirstNotFound=true;
			Object[] object = new String [1];
			dSet=dSetOrig;
			_lastNext = dSet.getPrevRow(obj);
			object[0] = _lastNext;
			_lastFirstNotFound=true;
			if(log.isDebugEnabled())
				log.debug("getPrevRow");
			rs = findCursor(object);
			setDmstatus(1,1);
			Sql.sqlEt();
			return false;
		} 
		setDmstatus(1,1);
		Sql.sqlEt();
		return false;
	}

	/**
	 * Load the first data retrieved that matches the object parameters 
	 * @param obj is of 2 forms viz: - 
	 * <br>
	 * An explicit where clause with fully qualified data
	 * <br>
	 * A parameterised query with the first object being the parametrised where clause portion
	 * and all subsequent values being the parameter values to the parameterised query
	 * @return true if we have managed to find & load data, false otherwise.
	 * @throws SQLException
	 */
	public boolean findExact(Object ... obj) throws SQLException {
		cursor = null;
		ResultSet rs = findCursor(obj);
		_lastNext = null;
		_lastBack = null;
		_lastFirstNotFound=false;
		_backOpen=false;
		_setToBeginning=false;
		_setToEnd=false;
		clearDmstatus();
		if(rs.first()) {
			loadData(rs);
			return true;
		}
		setDmstatus(1,1);
		return false;
	}

	/**
	 * Find and load the the next row into the dasdl set.
	 * @param obj is of 2 forms viz: - 
	 * <br>
	 * An explicit where clause with fully qualified data
	 * <br>
	 * A parameterised query with the first object being the parametrised where clause portion
	 * and all subsequent values being the parameter values to the parameterised query
	 * @return true if we have managed to find & load data, false otherwise.
	 * @throws SQLException
	 */
	@SuppressWarnings("resource")
	public boolean findNext(Object ... obj) throws SQLException {
		Sql.sqlBt(_tableName+".findNext()");
		DasdlSet dSetOrig = dSet;
		boolean skipToRec = false;
		_backOpen=false;
		_lastBack = null;
		clearDmstatus();
		ResultSet rs;
		//if(_setToBeginning && (obj == null || obj.length == 0)) {
		if(_setToBeginning) {	
			_setToBeginning = false;
			cursor=null;
			rs = findCursor(null);
			if(rs.next()) {
				loadData(rs);
				Sql.sqlEt();
				return true;
			}
			setDmstatus(1,2);
			Sql.sqlEt();
			return false;
		}
		_setToBeginning = false;
		if(_setToEnd){
			setDmstatus(1,2);
			Sql.sqlEt();
			return false;
		}
		if(_setToUniqueId != 0){
			String where;
			if(dSet == null){
				rs = findCursor(null);
				while(rs.next()){
					loadData(rs);
					if(_setToUniqueId == columns.get("UNIQUE_ID").getInt()) return true;
				}
			}else{
				where = "unique_id=" + _setToUniqueId;
				Object[] object = new String [1];
				object[0] = where;
				rs = findCursor(object);
				if(!rs.next()){
					if(log.isDebugEnabled())
						log.debug("no next");
					Sql.sqlEt();
					return false;
				}
				loadData(rs);
				dSet = dSetOrig;
				_lastNext = dSet.getNextWhere();
				object[0] = _lastNext;
				rs = findCursor(object);
				while(rs.next()){
					loadData(rs);
					if(_setToUniqueId == columns.get("UNIQUE_ID").getInt()) break;
				}
			}
			if(log.isDebugEnabled())
				log.debug("UniqueId=" + columns.get("UNIQUE_ID").getInt());
			_setToUniqueId=0;
			if(rs.next()) {
				loadData(rs);
				Sql.sqlEt();
				return true;
			}
			setDmstatus(1,2);
			Sql.sqlEt();
			return false;
		}
		_setToUniqueId=0;
		Object[] object = new String [1];
		if((obj == null || obj.length == 0) && dSet != null){
			if(_lastFirstNotFound){
				object[0] = _lastNext;
				if(log.isDebugEnabled()) {
					log.debug("lastFirstNotFound");
					log.debug(_lastNext);
				}
				rs = findCursor(object);
				if(rs.next()){
					loadData(rs);
					skipToRec=true;
				}else{
					_lastFirstNotFound=false;
					rs = findCursor(null);
					if(rs.next()) {
						loadData(rs);
						_lastNext=null;
						Sql.sqlEt();
						return true;
					}
					setDmstatus(1,2);
					Sql.sqlEt();
					return false;
				}
				_lastNext=null;
			}
			_lastFirstNotFound=false;
			if (_lastNext == null){
				
				dSet = dSetOrig;
				_lastNext = dSet.getNextWhere();
				object[0] = _lastNext;
				if(log.isDebugEnabled())
					log.debug("lastNext-1");
				rs = findCursor(object);
				rs.next();
				if(skipToRec){
					skipToRec=false;
					loadData(rs);
					dSetOrig.savePrevKey();
					while(dSetOrig.sameAsPrev()){
						if(rs.next())
							loadData(rs);
						else{
							setDmstatus(1,2);
							Sql.sqlEt();
							return false;
						}
					}
					rs.previous();
				}	
			}else {
				object[0] = _lastNext;
				if(log.isDebugEnabled())
					log.debug("lastNext-2");
				rs = findCursor(object);
			}
		}else{
			if(log.isDebugEnabled())
				log.debug("get next");
			rs = findCursor(obj);
		}
		if(rs.next()) {
			loadData(rs);
			Sql.sqlEt();
			return true;
		}
		setDmstatus(1,2);
		Sql.sqlEt();
		return false;
	}

	/**
	 * Find and load the preceding row/result
	 * 
	 * @param obj is of 2 forms viz: - 
	 * <br>
	 * An explicit where clause with fully qualified data
	 * <br>
	 * A parameterised query with the first object being the parametrised where clause portion
	 * and all subsequent values being the parameter values to the parameterised query
	 * @return true if we have managed to find & load data, false otherwise.
	 * @throws SQLException
	 * @throws SQLException
	 */
	@SuppressWarnings("resource")
	public boolean findPrior(Object ... obj) throws SQLException {
		Sql.sqlBt(_tableName+".findPrior()");
		DasdlSet dSetOrig = dSet;
		ResultSet rs;
		clearDmstatus();
		_lastFirstNotFound=false;
		if(_setToEnd) {
			_setToEnd = false;
			cursor = null;
			rs = findCursor(null);
			if(rs.previous()) {
				loadData(rs);
				Sql.sqlEt();
				return true;
			}
			setDmstatus(1,2);
			Sql.sqlEt();
			return false;
		}
		_setToEnd = false;
		if(_setToBeginning){
			setDmstatus(1,2);
			Sql.sqlEt();
			return false;
		}
		_setToBeginning=false;
		if(_setToUniqueId != 0){
			String where;
			if(dSet == null){
				rs = findCursor(null);
				while(rs.next()){
					loadData(rs);
					if(_setToUniqueId == columns.get("UNIQUE_ID").getInt()) return true;
				}
			}else{
				where = "unique_id=" + _setToUniqueId;
				Object[] object = new String [1];
				object[0] = where;
				rs = findCursor(object);
				if(!rs.next()){
					if(log.isDebugEnabled())
						log.debug("no next");
					Sql.sqlEt();
					return false;
				}
				loadData(rs);
				dSet = dSetOrig;
				_lastNext = dSet.getNextWhere();
				object[0] = _lastNext;
				rs = findCursor(object);
				while(rs.next()){
					loadData(rs);
					if(_setToUniqueId == columns.get("UNIQUE_ID").getInt()) break;
				}
			}
			if(log.isDebugEnabled())
				log.debug("UniqueId=" + columns.get("UNIQUE_ID").getInt());
			_setToUniqueId=0;
			if(rs.previous()) {
				loadData(rs);
				Sql.sqlEt();
				return true;
			}
			setDmstatus(1,2);
			Sql.sqlEt();
			return false;
		}
		_setToUniqueId=0;
		Object[] object = new String [1];
		if(_backOpen){
			if(cursor.next()) {
				loadData(cursor);
				Sql.sqlEt();
				return true;
			}
			setDmstatus(1,3);
			Sql.sqlEt();
			return false;
		} 
		
		if((obj == null || obj.length == 0) && dSet != null){
			if (_lastBack == null){
				_backOpen=true;
				_lastBack = dSet.getBackWhere();
				object[0] = _lastBack;
				if(log.isDebugEnabled())
					log.debug("lastBack-1");
				rs = findCursor(object);
				rs.next();
				if(rs.next()) {
					loadData(rs);
					Sql.sqlEt();
					return true;
				}
				setDmstatus(1,3);
				Sql.sqlEt();
				return false;
			}
			object[0] = _lastBack;
			if(log.isDebugEnabled())
				log.debug("lastBack-2");
			rs = findCursor(object);
			if(rs.previous()) {
				if(log.isDebugEnabled())
					log.debug("previous found");
				Sql.sqlEt();
				return true;
			}
			_backOpen=true;
			dSet=dSetOrig;
			_lastBack = dSet.getBackWhere();
			object[0] = _lastBack;
			if(log.isDebugEnabled())
				log.debug("lastBack-3");
			rs = findCursor(object);
			if(rs.next()) {
				loadData(rs);
				Sql.sqlEt();
				return true;
			}
			setDmstatus(1,3);
			Sql.sqlEt();
			return false;

		} 
		_backOpen=false;
		_lastBack=null;
		rs = findCursor(obj);
		if(rs.previous()) {
			loadData(rs);
			Sql.sqlEt();
			return true;
		}
		Sql.sqlEt();
		return false;
	}

	/**
	 * Loads the last result/row
	 * @param obj is of 2 forms viz: - 
	 * <br>
	 * An explicit where clause with fully qualified data
	 * <br>
	 * A parameterised query with the first object being the parametrised where clause portion
	 * and all subsequent values being the parameter values to the parameterised query
	 * @return true if we have managed to find & load data, false otherwise.
	 * @throws SQLException
	 */
	public boolean findLast(Object ... obj) throws SQLException {
		Sql.sqlBt(_tableName+".findLast()");
		_lastNext = null;
		_lastBack = null;
		_lastFirstNotFound=false;
		_backOpen=false;
		_setToBeginning=false;
		_setToEnd=false;
		clearDmstatus();
		cursor = null;
		ResultSet rs = findCursor(obj);
		if(rs.last()) {
			loadData(rs);
			Sql.sqlEt();
			return true;
		}
		setDmstatus(1,1);
		Sql.sqlEt();
		return false;
	}
	
	/**
	 * Set all the internal state values to beginning/initial.
	 * State equivalent to initialisation. 
	 * @return true
	 */
	public boolean setToBeginning() {
		_lastNext = null;
		_lastBack = null;
		_lastFirstNotFound=false;
		_backOpen=false;
		clearDmstatus();
		_setToBeginning = true;
		return true;
	}
	
	/**
	 * Set all the internal state values to end.
	 * State equivalent to when record set has been exhausted. 
	 * @return true
	 */
	public boolean setToEnd() {
		_lastNext = null;
		_lastBack = null;
		_lastFirstNotFound=false;
		_backOpen=false;
		clearDmstatus();
		return true;
	}
	
	private boolean setTo(int uniqueId) {
		_lastNext = null;
		_lastBack = null;
		_lastFirstNotFound=false;
		_backOpen=false;
		clearDmstatus();
		_setToUniqueId=uniqueId;
		return false;
	}
	
	/**
	 * Sets the unique index to the DasdlSet unique Id
	 * @param ds
	 * @return
	 */
	public boolean setTo(DasdlSet ds) {
		return(setTo(ds.getUniqueId()));
	}
	
	/**
	 * Sets the unique index to the Dasdl unique Id
	 * @param ds
	 * @return
	 */
	public boolean setTo(Dasdl ds) {
		return(setTo(ds.getUniqueId()));
	}
	
	/**
	 *  
	 * @return
	 */
	public boolean create() {
		clearDmstatus();
		clear();
		uniqueId=0;
		return true;
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean reCreate() {
		clearDmstatus();
		uniqueId=0;
		return true;
	}
	
	/**
	 * Inserts/updates data to the current table.
	 * @return
	 * @throws SQLException
	 */
	public boolean store() throws SQLException {
		Sql.sqlBt(_tableName+".store()");
		clearDmstatus();
		Field[] fields = getClass().getDeclaredFields();
		StringBuilder select = new StringBuilder();
		boolean first = true;
		Statement st;
		String s;
		
		if(uniqueId == 0) {
			select.append("insert into " + getTableName() + " (");
			for(int j=0; j < fields.length; j++) {
				Object object = null;
				try {
					object = fields[j].get(this);
				} catch (IllegalAccessException ex) {
					continue;
				}
				if (object != null) {
					if(object instanceof String) {
						if(fields[j].getName().compareTo("tableName") == 0) {
							continue;
						} else if(fields[j].getName().compareTo("dataSource") == 0) {
							continue;
						}
					} else if(object instanceof Dasdl) continue;
					else if(object instanceof DasdlSet) continue;
				}
				if(fields[j].getName().compareTo("UNIQUE_ID") == 0) continue;
				if(!first) select.append(",");
				select.append(fields[j].getName().toString());
				first = false;
			}
			select.append(") values (");
			first = true;
			for(int j=0; j < columnList.size(); j++) {
				if(fields[j].getName().compareTo("UNIQUE_ID") == 0) continue;
				if(!first) select.append(",");
				if(columnList.get(j).testAttr(Var.STRING)){
					s = columnList.get(j).rtrim().replace("'","''");
					if(s.equals("")) s = " ";
					select.append("'" + s + "'");
				} else 
					select.append(columnList.get(j).getString());
				first = false;
			}
			select.append(")");
			if(log.isDebugEnabled()) {
				log.debug("STORE - INSERT");
				log.debug(select.toString());
			}
			st = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			try {
				st.executeUpdate(select.toString(),Statement.RETURN_GENERATED_KEYS);
			}
			catch( SQLException e ) {
				sqlEx = e;
				setDmstatus(2,1);
				st.close();
				Sql.sqlEt();
				return false;
			}
			if(uniqueId == 0){
				ResultSet rs = st.getGeneratedKeys();
				rs.next();
				uniqueId = rs.getInt(1);
				rs.close();
				if(log.isDebugEnabled())
					log.debug("uniqueid=" + uniqueId);
			}
		} else {
			select.append("update " + getTableName() + " set ");
			if(log.isDebugEnabled())
				log.debug("fields.length=" + fields.length);
			for(int j=0; j < columnList.size(); j++) {
				if(fields[j].getName().compareTo("UNIQUE_ID") == 0) continue;
				if(j != 0) select.append(",");
				select.append(fields[j].getName() + " = ");
				if(columnList.get(j).testAttr(Var.STRING)){
					s = columnList.get(j).rtrim().replace("'","''");
					if(s.equals("")) s = " ";
					select.append("'" + s + "'");
				}else 
					select.append(columnList.get(j).getString());
			}
			select.append(" where unique_id = " + uniqueId);
			if(log.isDebugEnabled()) {
				log.debug("STORE- UPDATE uniqueId=" + uniqueId);
				log.debug(select.toString());
			}
			st = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			try {
				st.executeUpdate(select.toString());
			}
			catch( SQLException e ) {
				setDmstatus(13,1);
				sqlEx = e;
				Sql.sqlEt();
				return false;
			}
		}
		st.close();
		Sql.sqlEt();
		return true;
	}
	
	/**
	 * Delete data using a where clause.
	 * @param where the sql where clause.
	 * @return true if no exception encountered, false exception is raised.
	 * @throws SQLException no SQLException is raised it is implicit in a return value of false
	 */
	public boolean delete(String where)throws SQLException {
		Sql.sqlBt(_tableName+".findFirst()");
		clearDmstatus();
		StringBuilder select = new StringBuilder();
		select.append("delete from " + getTableName() + " where " + where);
		if(log.isDebugEnabled()) {
			log.debug("DELETE");
			log.debug(select.toString());
		}
		Statement st = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		try {
			 st.executeUpdate(select.toString());
			 st.close();
		}
		catch( SQLException e ) {
			setDmstatus(1,1);
			st.close();
			return false;
		}

		return true;
	}
	
	/**
	 * Delete the data at the current index delegates to {@see #delete(String)} with the 
	 * internal/current unique id.
	 * @return true if delete succeeded, false if   
	 * @throws SQLException this is masked and will not be thrown false will always be returned.
	 */
	public boolean delete()throws SQLException {
		clearDmstatus();
		if(uniqueId == 0){
			setDmstatus(13,2);
			return false;
		}
		return(delete("unique_id=" + uniqueId));
	}
		
	/**
	 * What is the current unique identifier to the row 
	 * we are using/pointing too either using the DasdlSet or
	 * the Dasdl class itself if no dasdl set in play.
	 * @return the Id of the DB row
	 */
	public int getUniqueId() {return uniqueId;}
	
	/**
	 * Set a new Dasdl set to be primary for operations.
	 * @param ds the dadlset to be used for operations.
	 */
	public void setDasdlSet(DasdlSet ds) { dSet = ds; }
	
	/**
	 * Which table are we using for data storage/retrieval 
	 * @return
	 */
	public String getTableName() { return _tableName; }
	
	/**
	 * What is the name of the data source
	 * @return the data source name
	 */
	public String getDataSource() { return _dataSource; }
	
	/**
	 * What columns are in play/being selected from.
	 * @return Comma separated list of columns which are active.
	 */
	public String getSelectList() { return _selectList; }
	
	/**
	 * Return a reference to the DB connection instance being used.
	 * @return
	 */
	public Connection getConnection() { return connection; }
	
	/**
	 * Get the index of the current row loaded.
	 * @return current row or -1 if not before or after
	 */
	public int getCurrentRow() { return currentRecord; }
	
	/**
	 * Named column lookup. Indexer into the column data
	 * @param name column name whose value we need to lookup
	 * @return
	 */
	public Var getColumnValue(String name) {return columns.get(name);}
	
	/**
	 * print the last sql exception to std error
	 */
	public void printStackTrace () {
		if(sqlEx != null)
			sqlEx.printStackTrace();
		else
			log.error("No error stack to display");
	}
	
}

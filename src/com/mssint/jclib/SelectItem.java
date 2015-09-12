package com.mssint.jclib;

import java.sql.PreparedStatement;	
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NavigableMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SelectItem {
	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(SelectItem.class);

	CursorState cursor;
	private String tableName;
	private String cursorName;
	private String hint;
	private String sql;
	private String where;
	private String fixedCondition;
	private String condition;
	private boolean addedCondition;
	private String orderBy;
	private boolean view;
	private ArrayList<Var>orderByList;
	private boolean [] orderDescend;
	private Var [] whereVars;
	private Var [] untilVars;
	private String [] eventIspecs;

	//private java.sql.Connection connection;
	
	private PreparedStatement stmnt;
	private ResultSet rs;
	private String rowid;
	private int ident;
	private int mode;
	private String identCol;
	
	private int rowCount;
	
	private boolean useUnions = false;
	private int hiOrder;
	private boolean back;
	private String signature;
	
	//After an open, this will contain a view of the selected records.
	private CursorCache.Profile profile = null;
	private NavigableMap<String, Integer> cachedMap;
	private Iterator<Integer> iterator;
	private CursorCache.RecordCache recordCache;
	
	public static final int EVERY 	= 0000000000001;
	public static final int FROM 	= 0000000000002;
	public static final int BACK 	= 0000000000004;
	public static final int GROUP 	= 0000000000010;
	public static final int UNTIL 	= 0000000000020;
	
	
	public SelectItem(CursorState cursor, String cursorName) {
		this.cursor = cursor;
		this.cursorName = cursorName;
		tableName = cursor.getTableName();
		identCol = cursor.getIdentCol();
		rs = null;
		stmnt = null;
		setRowCount(0);
		hint = null;
		sql = null;
		where = null;
		orderBy = null;
		fixedCondition = null;
		condition = null;
		addedCondition=false;
		eventIspecs = null;
		mode = 0;
		hiOrder = -999;
		back = false;
		recordCache = null;
		view = false;
	}
	
	public void setCache(CursorCache.RecordCache cache) {
		this.recordCache = cache;
	}
	public CursorCache.RecordCache getCache() {
		return recordCache;
	}
	
	public void setMode(int mode) {
		this.mode = mode;
	}
	public int getMode() {
		return mode;
	}
	
	public void setHint(String hint) {
		this.hint = hint;
	}
	public void setView() {
		this.view = true;
	}
	public void setCondition(String condition) {
		this.condition = "(" + condition + ")";
		this.fixedCondition = this.condition;
	}
	public void addCondition(String condition) {
		if(condition.trim().length() != 0) {
			if(this.fixedCondition != null)
				this.condition = fixedCondition + " and (" + condition + ")";
			else
				this.condition = "(" + condition + ")";
			addedCondition=true;
			sql=null; //To force redoing sql
		}
	}
	public void setOrder(Var ... var) {
		setOrder(false, var);
	}
	
	public void setOrder(boolean invert, Var ... var) {
		if(var == null || var.length == 0) {
			orderBy = null;
			return;
		}
		back = invert;
		StringBuilder sb = new StringBuilder();
		StringBuilder sig = new StringBuilder();
		orderByList = new ArrayList<Var>();
		orderDescend = new boolean[var.length];
		int i = 0;
		for(Var v : var) {
			orderByList.add(v);
			FrameVar fv = cursor.getFrameVar(v);
			String name = fv.getName();
			if(sb.length() > 0) sb.append(", ");
			if(!cursor.sqlServer)
				sb.append(name);
			else
				sb.append(Util.fixSqlServerReserved(name));	
			boolean desc;
			if(v.testAttr(Var.ORDER_DESC)) desc = true;
			else desc = false;
			if(invert) desc = !desc;
			if(desc) {
				sb.append(" desc");
				v.clearAttr(Var.ORDER_DESC);
				orderDescend[i] = true;
//				sig.append("(");
				sig.append(name);
//				sig.append(")");
			} else {
				orderDescend[i] = false;
				sig.append(name);
			}
			sig.append("|");
			i++;
		}
		if(sb.length() > 0) {
			orderBy = sb.toString();
			signature = sig.toString();
		} else {
			signature = null;
			orderBy = null;
		}
	}

	/**
	 * Returns the signature of this index. Essentially it is a concatenation (in order)
	 * of all the index field names. Descending order fields are returned in parenthesis.
	 * @return
	 */
	public String getIndexSignature() {
		if(condition == null) return signature;
		if(signature == null) return null;
		return signature + condition;
	}

	public void setKeyMatch(int keyCount) {
		sql = null;
		if(keyCount < 0)
			hiOrder = -keyCount;
		else
			hiOrder = keyCount;
	}
	
	public void setWhereVars(Var ... vars) throws SQLException {
		whereVars = vars;
		if(whereVars == null || whereVars.length != orderByList.size())
			throw new SQLException("Parameters for where list does not match orderby list.");
	}
	public void setUntilVars(Var ... vars) throws SQLException {
		untilVars = vars;
		if(untilVars == null || untilVars.length != orderByList.size())
			throw new SQLException("Parameters for until list does not match orderby list.");
	}
	
	public void setEventIspecs(String ... ispecs) throws SQLException {
		eventIspecs = ispecs;
	}
	
	private String generateEventWhere() {
		String where="(";
		for(int i=0;i< eventIspecs.length;i++) {
			where+="ISPEC='" + eventIspecs[i] + "'";
			if (i != eventIspecs.length - 1){
				where+=" or ";
			}
		}
		where+=")";
		return where;
	}
	
	private void setParameter(int idx, Var val, Var field) throws SQLException {
		Var lfield = new Var(field).set(val);
		if(field.testAttr(Var.CHAR) || field.testAttr(Var.PICTURE)) {
			String s = lfield.rtrim();
			stmnt.setString(idx ,s.length()!=0 ? s : " ");
		} else if(field.testAttr(Var.NUMBER) && field.scale == 0) {
			stmnt.setLong(idx ,lfield.getLong());
		} else {
			stmnt.setDouble(idx, lfield.getDouble());
		}
	}
	
	public void instantiateWhereVars() throws SQLException {
		if(whereVars == null || orderByList == null || stmnt == null)
			return;
		int idx = 1;
		switch(mode) {
		case EVERY:
			for(int i=0;i< whereVars.length;i++) {
				setParameter(idx++,whereVars[i], orderByList.get(i));
			}
			break;
		case FROM:
		case BACK:
			for(int i = orderByList.size(); i > 0; i--) {
				for(int j=0; j < i; j++) {
					setParameter(idx++,whereVars[j], orderByList.get(j));
				}
			}
			break;
		case GROUP:
			for(int i=0;i< hiOrder;i++) {
				setParameter(idx++,whereVars[i], orderByList.get(i));
			}
			for(int i = orderByList.size(); i >= hiOrder; i--) {
				for(int j=hiOrder; j < i; j++) {
					setParameter(idx++,whereVars[j], orderByList.get(j));
				}
			}
			break;
		case UNTIL:
			for(int i=0;i< hiOrder;i++) {
				setParameter(idx++,whereVars[i], orderByList.get(i));
			}
			for(int i = orderByList.size(); i >= hiOrder; i--) {
				for(int j=hiOrder; j < i; j++) {
					setParameter(idx++,whereVars[j], orderByList.get(j));
				}
			}
			for(int i = orderByList.size(); i >= hiOrder; i--) {
				for(int j=hiOrder; j < i; j++) {
					setParameter(idx++,untilVars[j], orderByList.get(j));
				}
			}
			break;
		}
	}
	
	public void printIndexValues() {
		if(orderByList == null)
			return;
		for(int i=0; i < orderByList.size();i++) {
			if(i > 0) System.out.print(":");
			System.out.print(orderByList.get(i).getString());
		}
		//System.out.println();
	}
	
	/**
	 * Create a string representation of all the key variables
	 * @return
	 */
	private String makeKey() {
		if(whereVars == null || orderByList == null)
			return null;
		StringBuilder sb = new StringBuilder();
		//The passed parameters (in whereVars) might not be the same length as the 
		//actual variables (in orderByList) so have to be manipulated.
		for(int i=0;i<whereVars.length;i++) {
			Var o = new Var(orderByList.get(i));
			o.set(whereVars[i]);
			sb.append(o.getString());
		}
		return sb.toString();
	}
	
	private String makeToKey() {
		if(whereVars == null || orderByList == null)
			return null;
		StringBuilder sb = new StringBuilder();
		//The passed parameters (in whereVars) might not be the same length as the 
		//actual variables (in orderByList) so have to be manipulated.
		for(int i=0;i<hiOrder;i++) {
			Var o = new Var(orderByList.get(i));
			o.set(whereVars[i]);
			sb.append(o.getString());
		}
		for(int i=hiOrder;i<whereVars.length;i++) {
			Var o = new Var(orderByList.get(i));
			if(back)
				o.setMinValue();
			else
				o.setMaxValue();
			sb.append(o.getString());
		}
		return sb.toString();
	}
	
	private String makeUntilKey() {
		if(whereVars == null || orderByList == null || untilVars == null)
			return null;
		StringBuilder sb = new StringBuilder();
		//The passed parameters (in whereVars) might not be the same length as the 
		//actual variables (in orderByList) so have to be manipulated.
		for(int i=0;i<hiOrder;i++) {
			Var o = new Var(orderByList.get(i));
			o.set(whereVars[i]);
			sb.append(o.getString());
		}
		for(int i=hiOrder;i<untilVars.length;i++) {
			Var o = new Var(orderByList.get(i));
			o.set(untilVars[i]);
			sb.append(o.getString());
		}
		return sb.toString();
	}
	
	private void generateAllEqualWhere() {
		if(whereVars == null)
			return;
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<orderByList.size(); i++) {
			Var o = orderByList.get(i);
			FrameVar fv = cursor.getFrameVar(o);
			if(i > 0) sb.append(" and\n");
			if(cursor.sqlServer)
				sb.append(Util.fixSqlServerReserved(fv.name));
			else
				sb.append(fv.name);
			sb.append(" = ?");
		}
		where = sb.toString();
	}
	
	private String generateFromWhere(int max) {
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<max; i++) {
			Var o = orderByList.get(i);
			FrameVar fv = cursor.getFrameVar(o);
			if(i > 0) sb.append(" and\n");
			if(cursor.sqlServer)
				sb.append(Util.fixSqlServerReserved(fv.name));
			else
				sb.append(fv.name);
			if(i < (max - 1))
				sb.append(" = ?");
			else {
				if(orderDescend[i])
					sb.append(" <");
				else
					sb.append(" >");
				if(max == orderByList.size())
					sb.append("= ?\n");
				else sb.append(" ?\n");
			}
		}
		return sb.toString();
	}
	
	private void generateGroupWhere()
	{
		int count = orderByList.size();
		StringBuilder sb = new StringBuilder();
		/* Work out the GROUP (or equal) portion */
		for(int i=0; i<hiOrder; i++) {
			Var o = orderByList.get(i);
			FrameVar fv = cursor.getFrameVar(o);
			if(i > 0) sb.append(" and\n");
			if(cursor.sqlServer)
				sb.append(Util.fixSqlServerReserved(fv.name));
			else
				sb.append(fv.name);
			sb.append(" = ?");
		}
		if(hiOrder > 0)
			sb.append("\n");
	
		/* Now do the `FROM' part */
		if(hiOrder < count) {
			if(hiOrder != 0) sb.append("and (\n");
			for(int i=hiOrder; i<count; i++) {
				if(i > hiOrder) sb.append(" or ");
				sb.append("(\n");
				for(int j=i; j<count; j++) {
					Var o = orderByList.get(j-i+hiOrder);
					FrameVar fv = cursor.getFrameVar(o);
					if(j > i) sb.append(" and\n");
					if(cursor.sqlServer)
						sb.append(Util.fixSqlServerReserved(fv.name));
					else
						sb.append(fv.name);
					if(j == (count - 1)) {
						if(orderDescend[j-i+hiOrder]) sb.append(" <");
						else sb.append(" >");
						if(i == hiOrder) sb.append("=");
					} else sb.append(" =");
					sb.append(" ?");
				}
				sb.append("\n)");
			}
			if(hiOrder > 0)	sb.append("\n)");
		}
		where = sb.toString();
	}
	
	private void generateUntilWhere()
	{
		int count = orderByList.size();
		StringBuilder sb = new StringBuilder();
		/* Work out the GROUP (or equal) portion */
		for(int i=0; i<hiOrder; i++) {
			Var o = orderByList.get(i);
			FrameVar fv = cursor.getFrameVar(o);
			if(i > 0) sb.append(" and\n");
			if(cursor.sqlServer)
				sb.append(Util.fixSqlServerReserved(fv.name));
			else
				sb.append(fv.name);
			sb.append(" = ?");
		}
		if(hiOrder > 0)
			sb.append("\n");
	
		/* Now do the `FROM' part */
		if(hiOrder < count) {
			if(hiOrder != 0) sb.append("and (\n");
			else sb.append("(\n");
			for(int i=hiOrder; i<count; i++) {
				if(i > hiOrder) sb.append(" or ");
				sb.append("(\n");
				for(int j=i; j<count; j++) {
					Var o = orderByList.get(j-i+hiOrder);
					FrameVar fv = cursor.getFrameVar(o);
					if(j > i) sb.append(" and\n");
					if(cursor.sqlServer)
						sb.append(Util.fixSqlServerReserved(fv.name));
					else
						sb.append(fv.name);
					if(j == (count - 1)) {
						if(orderDescend[j-i+hiOrder]) sb.append(" <");
						else sb.append(" >");
						if(i == hiOrder) sb.append("=");
					} else sb.append(" =");
					sb.append(" ?");
				}
				sb.append("\n)");
			}
			sb.append("\n)");
		}
		/* Now do the `UNTIL' part */
		if(hiOrder < count) {
			sb.append("and (\n");
			for(int i=hiOrder; i<count; i++) {
				if(i > hiOrder) sb.append(" or ");
				sb.append("(\n");
				for(int j=i; j<count; j++) {
					Var o = orderByList.get(j-i+hiOrder);
					FrameVar fv = cursor.getFrameVar(o);
					if(j > i) sb.append(" and\n");
					if(cursor.sqlServer)
						sb.append(Util.fixSqlServerReserved(fv.name));
					else
						sb.append(fv.name);
					if(j == (count - 1)) {
						if(orderDescend[j-i+hiOrder]) sb.append(" >");
						else sb.append(" <");
						if(i == hiOrder) sb.append("=");
					} else sb.append(" =");
					sb.append(" ?");
				}
				sb.append("\n)");
			}
			sb.append("\n)");
		}
		where = sb.toString();
	}
	
		
	public void generateFromBackSql() {
		StringBuilder sb = new StringBuilder();
		if(orderByList == null || orderByList.size() == 0) {
			generateSql();
			return;
		}
		if(useUnions) {
			for(int i=0; i<orderByList.size();i++) {
				if(i > 0) {
					sb.append("union all ");
				}
				sb.append("select * from ( \n");
				sb.append("select ");
				if(!cursor.sqlServer && hint != null)
					sb.append(hint);
				sb.append("\n");
				sb.append(cursor.getFieldList());
				if(!view) {
					if(cursor.sqlServer) 
						sb.append(", " + identCol + "\n");
					else
						sb.append(", rowid\n");
						
				}
				sb.append(" from ");
				sb.append(tableName);
				sb.append("\n");
				if(cursor.sqlServer && hint != null)
					sb.append(hint + "\n");
				sb.append("where\n(\n");
				if(eventIspecs != null && eventIspecs.length != 0) {
					sb.append(generateEventWhere());
					sb.append(" and \n");
				}
				if(condition != null) {
					sb.append(condition);
					sb.append(" and \n");
				}
				sb.append(generateFromWhere(orderByList.size() - i));
				if(!cursor.sqlServer) {
					sb.append(")\norder by\n");
					sb.append(orderBy);
				} else
					sb.append(")\n");

				sb.append("\n) ");
				sb.append(Character.toString((char)(i + 'A')));
				sb.append("\n");
				
			}
			if(cursor.sqlServer) {
				sb.append("order by\n");
				sb.append(orderBy);
				//sb.append("\nOPTION (FAST 1)");
			}
		} else {
			sb.append("select ");
			if(!cursor.sqlServer && hint != null)
				sb.append(hint);
			sb.append("\n");
			sb.append(cursor.getFieldList());
			if(!view) {
				if(cursor.sqlServer)
					sb.append(", " + identCol + "\n");
				else
					sb.append(", rowid\n");
					
			}
			sb.append(" from ");
			sb.append(tableName);
			sb.append("\n");
			if(cursor.sqlServer)
				sb.append(" with (NOLOCK) \n");
			if(cursor.sqlServer && hint != null) 
				sb.append(hint + "\n");
			sb.append("where\n");
			if(eventIspecs != null && eventIspecs.length != 0) {
				sb.append(generateEventWhere());
				sb.append(" and \n");
			}
			if(condition != null) {
				sb.append(condition);
				sb.append(" and \n");
			}
			sb.append("(");
			for(int i=0; i<orderByList.size();i++) {
				if (i > 0) sb.append("\nOR\n");
				sb.append("(" + generateFromWhere(orderByList.size() - i) + ")");
			}
			sb.append(")\n");
			sb.append("order by\n");
			sb.append(orderBy);
			if(cursor.sqlServer) {
				sb.append(" OPTION (FAST 100)");
			}
		}
		sql = sb.toString();
		if(cursor.sqlServer)
			sql = sql.toUpperCase().replaceAll(identCol.toUpperCase(), identCol);
	}
	
	public void generateSql() {
		StringBuilder sb = new StringBuilder();
		sb.append("select ");
		if(hint != null)
			sb.append(hint);
		sb.append("\n");
		sb.append(cursor.getFieldList());
		if(!view)
			if(cursor.sqlServer)
				sb.append(", " + identCol + " \n");
			else
				sb.append(", rowid \n");
		sb.append(" from ");
		sb.append(tableName);
		sb.append("\n");
		if(cursor.sqlServer)
			sb.append(" with (NOLOCK) \n");
		if(where != null || condition != null || (eventIspecs != null && eventIspecs.length != 0)) {
			sb.append("where\n");
		}
		if(eventIspecs != null && eventIspecs.length != 0) {
			sb.append(generateEventWhere());
		}
		if(condition != null) {
			if(eventIspecs != null && eventIspecs.length != 0) {
				sb.append(" and \n");
			}
			sb.append(condition);
			sb.append("\n");
		}
		if(where != null) {
			if(condition != null || (eventIspecs != null && eventIspecs.length != 0)) {
				sb.append(" and(\n");
			} else sb.append(" (\n");
			sb.append(where);
			sb.append(")\n");
		}
		if(orderBy != null) {
			sb.append("order by\n");
			sb.append(orderBy);
			sb.append("\n");
		}
		sql = sb.toString();
		if(cursor.sqlServer) {
			sql = sql.toUpperCase().replaceAll(identCol.toUpperCase(), identCol);
			sql+=" OPTION(FAST 100)";
		}
	}
	
	public void close() {
		Sql.sqlBt(tableName + ".close_" + cursorName);
		closeSelectItem();
		Sql.sqlEt();
	}
	
	public void closeSelectItem() {
		
		if(stmnt != null) {
			try {
				stmnt.cancel();
				stmnt.close();
				stmnt = null;
			} catch (SQLException ex) {
			}
		}
		if(rs != null) {
			try {
				rs.close();
				rs = null;
			} catch (SQLException ex) {
			}
		}
		cursor.lastActiveSelectItem = null;
		return;
	}

	public void open() throws SQLException {
		Sql.sqlBt(tableName + ".open_" + cursorName);
		if(cursor.cacheEnabled()) {
			cursor.loadCache(this);
			CursorCache cache = cursor.getCache();
			profile = cache.getProfile(this);
			String key, tokey;
			//			System.out.println("key=["+key+"]");
			switch(mode) {
			case EVERY:
				key = makeKey();
				cachedMap = profile.getEvery(key);
				break;
			case FROM:
				key = makeKey();
				cachedMap = profile.getFrom(key);
				break;
			case BACK:
				key = makeKey();
				cachedMap = profile.getBack(key);
				break;
			case GROUP:
				key = makeKey();
				tokey = makeToKey();
				cachedMap = profile.getGroup(back, key, tokey);
				break;
			case UNTIL:
				key = makeKey();
				tokey = makeUntilKey();
				cachedMap = profile.getGroup(back, key, tokey);
				break;
			default:
				if(profile == null) {
					recordCache.resetIndex();
				} else {
					key = makeKey();
					cachedMap = profile.getAll();
				}
			}
			if(cachedMap != null) {
				iterator = cachedMap.values().iterator();
			} else iterator = null;
		} else {
			switch(mode) {
			case EVERY: //Generate where clause once
				if(sql == null) {
					generateAllEqualWhere();
					generateSql();
				}
				break;
			case FROM:
			case BACK:
				if(sql == null) {
					generateFromBackSql();
				}
				break;
			case GROUP:
				if(sql == null) {
					generateGroupWhere();
					generateSql();
				}
				break;
			case UNTIL:
				if(sql == null) {
					generateUntilWhere();
					generateSql();
				}
				break;
			default:
				where = null;
				if(sql == null) {
					generateSql();
				}
				break;
			}
			closeSelectItem();
//System.out.println("sql=" + sql);
			//Config.log.info("sql=" + sql);
			if(cursor.sqlServer) {
				//stmnt = cursor.getConnection().prepareStatement(sql, ResultSet.CONCUR_READ_ONLY, ResultSet.TYPE_FORWARD_ONLY);
				sql = sql.toUpperCase().replaceAll(identCol.toUpperCase(), identCol);
				stmnt = cursor.getConnection().prepareStatement(sql, ResultSet.CONCUR_READ_ONLY);
			} else
				stmnt = cursor.getConnection().prepareStatement(sql);
			instantiateWhereVars();
			rs = stmnt.executeQuery();
		}
		if (addedCondition) {
			this.condition = this.fixedCondition;
			addedCondition=false;
			sql=null;
		}
		Sql.sqlEt();
		setRowCount(0);
		cursor.lastActiveSelectItem = this;
	}

	public boolean status() {
		if(rs == null)
			return false;
		return true;
	}

	public boolean fetch() throws SQLException {
		if(cursor.cacheEnabled()) {
			if(iterator == null) {
				Integer idx = recordCache.next();
				if(idx < 0) return false;
				CursorCache.Record record = recordCache.get(idx);
				rowid = record.rowid;
				cursor.recoverValues(record.vars);
				setRowCount(getRowCount() + 1);
				return true;
			} else if(iterator.hasNext()) {
				Integer idx = iterator.next();
				CursorCache.Record record = recordCache.get(idx);
				rowid = record.rowid;
				cursor.recoverValues(record.vars);
				setRowCount(getRowCount() + 1);
				return true;
			} else return false;
		} else {
			if(rs == null) return false;
			cursor.lastActiveSelectItem = this;
			if(rs.next()) {
				cursor.recoverValues(rs);
				if(!view)
					if(!cursor.sqlServer)
						rowid = rs.getString(cursor.getParameterCount()+1);
					else
						ident = rs.getInt(cursor.getParameterCount()+1);
				else {
					rowid=null;
					ident=0;
				}
				setRowCount(getRowCount() + 1);
				return true;
			} else {
				return false;
			}
		}
	}
	
	/**
	 * Get a data string representing the concatenated values of the index variables.
	 * @return The index to be used in a map
	 */
	public String getIndexString() {
		if(orderByList == null || orderByList.size() == 0)
			return null;
		StringBuilder sb = new StringBuilder();
			for(Var v : orderByList) {
				sb.append(v.getString());
			}
		return sb.toString();
	}

	public String getRowid() {
		return rowid;
	}
	
	public int getIdent() {
		return ident;
	}

	public String getCursorName() {
		return cursorName;
	}
	public void setRowCount(int rowCount) {
		this.rowCount = rowCount;
	}

	public int getRowCount() {
		return rowCount;
	}


	public boolean changed(int hiOrder, boolean back) {
		if(this.hiOrder != hiOrder || this.back != back)
			return true;
		return false;
	}
	public void printSql() {
		System.out.println("cursorName=" + cursorName + "\nsql=" + sql);
	}

	public String getCondition() {
		return condition;
	}

}

package com.mssint.jclib;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Mimics the behaviour of DasdlSet. 
 *  
 * In particular a full or sub set of columns are defined in the extending DasdlSet class.
 * These correspond to a set of fields that can be consider as an indexed view or key set index overlying the base table/type 
 * and are used retrieve a specific subset of from the underlying table defined in the class extending Dasdl in which 
 * the DasdlSet instance will exist.
 * 
 * It requires a Dasdl instance in order to operate, which provides the primary methods for operation and complete
 * definition of the table and its columns.
 * 
 * In particular DasdlSet delegates retrieval calls to Dasdl in which it is generally "housed".
 * 
 * @author MssInt
 *
 */
public class DasdlSet {
	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(DasdlSet.class);
	private boolean isInitialised = false;
	//private List<String> indexes;
	private Vector<String> indexes;
	private Vector<String> revIndexes;
	private Vector<String> inds;
	private String where = null;
	private Dasdl dasdl = null;
	private String orderBy = null;
	private String backOrderBy = null;
	private String lincWhere = null;
	private int uniqueId;
	private Var[] prevKeys;
	
	/**
	 * Initialise the DasdlSet, in particular it's columns and their sort ordering if.
	 * In particular any predefined ordering and a where clause for the Set.
	 * 
	 * The usage of an init method like this allows for in-line instantiation within the containing Dasl class e.g.
	 * public PAUDIT paudit = (PAUDIT)new PAUDIT().init(this);
	 * 
	 * @param dsdl The underlying Dasl instance.  
	 * @return An instantiated and initialised DasdlSet
	 */
	public DasdlSet init(Dasdl dsdl) {
		if(isInitialised) return this;
		this.dasdl = dsdl;
		// loop through all the declared fields
		indexes = new Vector<String>();
		inds = new Vector<String>();
		revIndexes = new Vector<String>();
		Field[] fields = getClass().getDeclaredFields();
		for (int i = 0; i < fields.length; i++) {
			// take object's value...
			Object object = null;
			try {
				object = fields[i].get(this);
			} catch (IllegalAccessException ex) {
				continue;
			}

			// ... and add to the parameters
			if (object != null) {
				try  {
					if(object instanceof String) {
						if(fields[i].getName().compareTo("whereClause") == 0) {
							where = (String)object;
							//log.debug("Added whereclause: \""+where+"\"");
						} else {
							//take the string value rather than the name.
							String s = (String)object;
							inds.add(fields[i].getName());
							if(s.equalsIgnoreCase("desc") || s.equalsIgnoreCase("descending")) {
								s = fields[i].getName() + " desc";
							}
							indexes.add(s);
							revIndexes.add(s.replaceFirst(" desc", " asc"));
							//log.debug("1: Added field " + (String)object);
						}
					} else {
						indexes.add(fields[i].getName());
						inds.add(fields[i].getName());
						revIndexes.add(fields[i].getName() + " desc");
						//log.debug("2: Added field " + fields[i].getName());
					}
				} catch(IllegalArgumentException ex)  {
					throw ex;
				}
			} else {
				String name = fields[i].getName();
				indexes.add(name);
				inds.add(name);
				revIndexes.add(name + " desc");
				//log.debug("3: Added field " + name);
			}
		}

		// set the flag
		isInitialised = true;
		return this;
	}
	
	/**
	 * Get the currently defined where clause {@link #init(Dasdl)}
	 * @return the where clause as a String
	 */
	public String getWhere() { return where; }
	
	/**
	 * Returns the column order of the member variables defined in the DasdlSet 
	 * @return a comma separated list of columns in order of compile time definition.
	 */
	public String getOrderBy() {
		if(orderBy == null) {
			StringBuilder sb = new StringBuilder();
			for(int i=0; i<indexes.size();i++) {
				if(i > 0) sb.append(", ");
				sb.append(indexes.get(i));
			}
			orderBy = sb.toString();
		}
		return orderBy;
	}
	
	/**
	 * Reverses the column order of the member variables defined in the DasdlSet 
	 * @return a comma separated list of columns in reverse order of compile time definition.
	 */
	public String getBackOrderBy() {
		if(backOrderBy == null) {
			StringBuilder sb = new StringBuilder();
			for(int i=0; i<revIndexes.size();i++) {
				if(i > 0) sb.append(", ");
				sb.append(revIndexes.get(i));
			}
			backOrderBy = sb.toString();
		}
		return backOrderBy;
	}
	
	private String getLincWhere(boolean forward){
		
		StringBuilder sb = new StringBuilder("(");
		for(int i=inds.size() - 1; i >= 0;i--) {
			if(i!=inds.size() - 1) sb.append(" OR ");
			sb.append("(");
			for(int j=0; j<=i;j++) {
				if(j!=0) sb.append(" AND ");
				sb.append(inds.get(j));
				if(forward){
					if(i == j) {
						if(i==inds.size() - 1) sb.append(" >= ");
						else sb.append(" > ");
					} else sb.append(" = ");
				}else{
					if(i == j) {
						if(i==inds.size()- 1) sb.append(" <= ");
						else sb.append(" < ");
					} else sb.append(" = ");
				}
				Var v = dasdl.getColumnValue(inds.get(j));
				if(v.testAttr(Var.STRING)) sb.append("'"+v.getString()+"'");
				else sb.append(v.getString());
			}
			sb.append(")");
		}
		sb.append(")");
		lincWhere = sb.toString();
		return lincWhere;
	}
	
	/**
	 * Build and return a string representing a where clause that would retrieve data further ahead of the current position.
	 * @return a string representing the where clause. 
	 */
	public String getNextWhere() {
		return getLincWhere(true);
	}

	/**
	 * Build and return a string representing a where clause that would retrieve data before the current position.
	 * @return a string representing the where clause. 
	 */
	public String getBackWhere() {
		return getLincWhere(false);
	}
	
	/**
	 * 
	 * @param obj
	 * @return
	 */
	public String getPrevRow(Object ... obj){
		boolean forward=false;
		StringBuilder sb = new StringBuilder("(");
		for(int i=inds.size() - 1; i >= 0;i--) {
			int nextObj = 1;
			if(i!=inds.size() - 1) sb.append(" OR ");
			sb.append("(");
			for(int j=0; j<=i;j++) {
				if(j!=0) sb.append(" AND ");
				sb.append(inds.get(j));
				if(forward){
					if(i == j) {
						if(i==inds.size() - 1) sb.append(" >= ");
						else sb.append(" > ");
					} else sb.append(" = ");
				}else{
					if(i == j) {
						if(i==inds.size() - 1) sb.append(" <= ");
						else sb.append(" < ");
					} else sb.append(" = ");
				}
				if(nextObj < obj.length){
					Object o = obj[nextObj++];
					if(o instanceof String) sb.append("'" + (String)o + "'");
					else if(o instanceof Integer) sb.append(o);
					else if(o instanceof Long) sb.append(o);
					else if(o instanceof Double) sb.append(o);
					else if(o instanceof Var) {
						Var v = (Var)o;
						if(v.testAttr(Var.STRING)) sb.append("'"+v.getString()+"'");
						else sb.append(v.getString());
					} else throw new IllegalArgumentException("Type for parameter "+(nextObj-1)+" not handled.");
				}else{
					Var v = dasdl.getColumnValue(inds.get(j));
					if(v.testAttr(Var.STRING)) sb.append("' '");
					else sb.append("0");
					
				}
			}
			sb.append(")");
		}
		sb.append(")");
		lincWhere = sb.toString();
		return lincWhere;
	}
	
	/**
	 * Store the current key values for future reference.
	 */
	public void savePrevKey(){
		prevKeys = new Var[inds.size()];
		for(int i=0; i < inds.size();i++) {
			Var v = dasdl.getColumnValue(inds.get(i));
			prevKeys[i] = new Var(v);
			prevKeys[i].set(v);
		}
		return;
	}
	
	/**
	 * Are the current key values the same as the stored/previous key values?
	 * @return true if all are the same false otherwise
	 */
	public boolean sameAsPrev(){
		for(int i=0; i < inds.size();i++) {
			Var v = dasdl.getColumnValue(inds.get(i));
			if(prevKeys[i].ne(v)) return false;
		}
		return true;
	}
	
	/**
	 * Please see {@link Dasdl#findFirst(Object...)}
	 * @param obj
	 * @return
	 * @throws SQLException
	 */
	public boolean findFirst(Object ... obj) throws SQLException {
		dasdl.setDasdlSet(this);
		return dasdl.findFirst(obj);
	}

	/**
	 * Please see {@link Dasdl#findExact(Object...)}
	 * @param obj
	 * @return
	 * @throws SQLException
	 */
	public boolean findExact(Object ... obj) throws SQLException {
		dasdl.setDasdlSet(this);
		return dasdl.findExact(obj);
	}

	/**
	 * Please see {@link Dasdl#findNext(Object...)}
	 * @param obj
	 * @return
	 * @throws SQLException
	 */
	public boolean findNext(Object ... obj) throws SQLException {
		dasdl.setDasdlSet(this);
		return dasdl.findNext(obj);
	}

	/**
	 * Please see {@link Dasdl#findPrior(Object...)}
	 * @param obj
	 * @return
	 * @throws SQLException
	 */
	public boolean findPrior(Object ... obj) throws SQLException {
		dasdl.setDasdlSet(this);
		return dasdl.findPrior(obj);
	}

	/**
	 * Please see {@link Dasdl#findLast(Object...)}
	 * @param obj
	 * @return
	 * @throws SQLException
	 */
	public boolean findLast(Object ... obj) throws SQLException {
		dasdl.setDasdlSet(this);
		return dasdl.findLast(obj);
	}
	
	/**
	 * Please see {@link Dasdl#setToBeginning()}
	 * @return
	 */
	public boolean setToBeginning() {
		dasdl.setDasdlSet(this);
		return dasdl.setToBeginning();
	}

	/**
	 * Please see {@link Dasdl#setToBeginning()()}
	 * @return
	 */
	public boolean setToEnd() {
		dasdl.setDasdlSet(this);
		return dasdl.setToEnd();
	}

	/**
	 * Please see {@link Dasdl#setTo(DasdlSet)}
	 * @param ds
	 * @return
	 */
	public boolean setTo(DasdlSet ds) {
		dasdl.setDasdlSet(this);
		return dasdl.setTo(ds);
	}
	
	/**
	 * Please see {@link Dasdl#setTo(Dasdl)}
	 * @param ds
	 * @return
	 */
	public boolean setTo(Dasdl ds) {
		dasdl.setDasdlSet(this);
		return dasdl.setTo(ds);
	}
	
	/**
	 * Please see {@link Dasdl#getTableName()}
	 * @return
	 */
	public String getTableName() { return dasdl.getTableName(); }
	/**
	 * Please see {@link Dasdl#getDataSource()}
	 * @return
	 */
	public String getDataSource() { return dasdl.getDataSource(); }
	/**
	 * Please see {@link Dasdl#getSelectList()}
	 * @return
	 */
	public String getSelectList() { return dasdl.getSelectList(); }
	/**
	 * Set the unique identifier for the data.
	 * @param id
	 */
	public void setUniqueId(int id) {uniqueId=id;}
	/**
	 * Get the unique data identifier
	 * @return
	 */
	public int getUniqueId() {return uniqueId;}

}

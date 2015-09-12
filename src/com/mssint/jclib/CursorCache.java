package com.mssint.jclib;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CursorCache {
	private static final Logger log = LoggerFactory.getLogger(CursorCache.class);
	CursorState cursorState;
	
	//Innerclass for dealing with saved records.
	//If rowid is null this is a newly inserted record.
	class Record {
		public Object [] vars;
		public String rowid;
		public boolean updated;
		public Record(String rowid, Object[] dataSetVars) {
			this.rowid = rowid;
			this.vars = dataSetVars;
			updated = false;
		}
		public String getIndexString() {
			StringBuilder sb = new StringBuilder();
			for(Object v : vars) {
				sb.append(((Var)v).getString());
			}
			return sb.toString();
		}
	}
	
	//Cache for all records.
	class RecordCache {
		private ArrayList<Record>cache;
		private int nextIndex;
		private String condition;
		
		public int size() {
			if(cache == null) return 0;
			return cache.size();
		}

		public RecordCache(SelectItem selectItem, Profile pf) throws SQLException {
			nextIndex = -1;
			cache = new ArrayList<CursorCache.Record>();
//			log.info("Table " + cursorState.getTableName() + ": Caching activated.");
			long time = SystemDate.getDate().getTime();
			StringBuilder sql = new StringBuilder();
			sql.append("select ");
			sql.append(cursorState.getFieldList());
			sql.append(", rowid");
			sql.append(" from ");
			sql.append(cursorState.getTableName());
			condition = selectItem.getCondition();
			if(condition != null) {
				sql.append(" where ");
				sql.append(condition);
			} else nullConditionCache = this;
			
			PreparedStatement stmnt = cursorState.getConnection().prepareCall(sql.toString());
			ResultSet rs = stmnt.executeQuery();
			int count = 0;
			while(rs.next()) {
				cursorState.recoverValues(rs);
				String rowid = rs.getString(cursorState.getParameterCount()+1);
				String index;
				if(pf != null && selectItem != null) {
					index = selectItem.getIndexString();
					if(index != null) {
						pf.addKey(index, count);
					}
				}
				Record record = new Record(rowid, cursorState.getDataSetVars());
				cache.add(record);
				count++;
			}
			time = SystemDate.getDate().getTime() - time;
			
			String msg;
			if(condition == null) msg = "";
			else msg = " with " + condition;
			log.info("Cache table " + cursorState.getTableName() + msg + ": Loaded " + count +
					" records in " + (time/1000) + "." + (time%1000) + " seconds");
		}

		public void resetIndex() {
			if(cache.size() > 0)
				nextIndex = 0;
			else nextIndex = -1;
		}
		public int next() {
			if(nextIndex < 0) return -1;
			if(nextIndex < cache.size())
				return nextIndex++;
			else 
				nextIndex = -1;
			return nextIndex;
		}

		public Record get(int idx) {
			if(cache == null) return null;
			return cache.get(idx);
		}

	}
	private RecordCache nullConditionCache; //Cache for null condition
	private TreeMap<String, RecordCache>cacheMap;
	
	//Innerclass for handling index maps
	class Profile {
		public TreeMap<String, Integer> map; //map of keys to index in main cache
		public String signature;
		public Profile(String signature) {
			map = new TreeMap<String, Integer>();
		}
		/**
		 * Add a new index item.
		 * @param key
		 */
		public void addKey(String key, int idx) {
			map.put(key, new Integer(idx));
		}
		
		public NavigableMap<String, Integer> getFrom(String key) {
			return map.tailMap(key, true);
		}

		public NavigableMap<String, Integer> getBack(String key) {
			return map.headMap(key, true).descendingMap();
		}

		public NavigableMap<String, Integer> getGroup(boolean back, String from, String to) {
			if(back)
				return map.subMap(to, true, from, true).descendingMap();
			return map.subMap(from, true, to, true);
		}

		public NavigableMap<String, Integer> getEvery(String key) {
			return map.subMap(key, true, key, true);
		}

		public NavigableMap<String, Integer> getAll() {
			return map.tailMap(" ", true);
		}
	}
	private TreeMap<String, Profile>profiles;
	
	/**
	 * Creates a new cache for a database table and loads the data.
	 * The SelectItem object, if not null, will be used to create an index to the cache.
	 * @param cursorState The cursor instance to cache.
	 * @throws SQLException 
	 */
	public CursorCache(CursorState cursorState, SelectItem selectItem) throws SQLException {
		this.cursorState = cursorState;
		profiles = null;
		loadCache(selectItem);
	}
	
	protected void loadCache(SelectItem selectItem) throws SQLException {
		if(cacheMap != null)
			throw new SQLException("Cache already exists. Attempt to create second instance.");
		cacheMap = new TreeMap<String, CursorCache.RecordCache>();

		Profile pf = null;
		String sig;
		if(selectItem != null) {
			sig = selectItem.getIndexSignature();
			//Check if we have a profile.
			if(profiles == null)
				profiles = new TreeMap<String, CursorCache.Profile>();
			if(sig != null && profiles.get(sig) == null) {
				pf = new Profile(sig);
				profiles.put(sig, pf);
			}
		} else sig = null;

		RecordCache rcache = new RecordCache(selectItem, pf);
		if(selectItem != null) {
			if(selectItem.getCondition() != null) {
				cacheMap.put(selectItem.getCondition(), rcache);
			} else nullConditionCache = rcache;
			selectItem.setCache(rcache);
		} else nullConditionCache = rcache;
	}
	
	/**
	 * Create a new profile.
	 * @param select
	 * @throws SQLException 
	 */
	public Profile getProfile(SelectItem select) throws SQLException {
		String sig = select.getIndexSignature();
		Profile pf;
		if(sig != null) {
			pf = profiles.get(sig);
			if(pf == null) {
				pf = new Profile(sig);
				profiles.put(sig, pf);
			}
		} else pf = null;
		
		//Retrieve existing cache or create new one.
		RecordCache cache;
		if(select.getCondition() == null) {
			cache = nullConditionCache;
		} else {
			cache = cacheMap.get(select.getCondition());
		}
		if(cache == null) {
			cache = new RecordCache(select, pf);
			if(select.getCondition() == null)
				nullConditionCache = cache;
			else
				cacheMap.put(select.getCondition(), cache);
			select.setCache(cache);
			return pf;
		} else {
			if(select.getCache() == null)
				select.setCache(cache);
		}
		
		if(pf == null) return null;
		if(pf.map.size() < cache.size()) {
			for(int idx=0; idx < cache.size(); idx++) {
				Record rec = cache.get(idx);
				select.cursor.recoverValues(rec.vars);
				String index = select.getIndexString();
				pf.addKey(index, idx);
			}
		}
		return pf;
	}
}

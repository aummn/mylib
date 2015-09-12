package com.mssint.jclib;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GlobalPersistence implements Serializable {
	private static final long serialVersionUID = 1L;
	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(GlobalPersistence.class);
	//Map pointers to the GLB vars
	private Map<String, Integer>glbMap = null;
	//Map pointers to the local vars
	private Map<String, Object>store;
	
	public GlobalPersistence() {
		glbMap = null;
		store = null;
	}
	
	public void initialise() {
		Field[] fields = getClass().getDeclaredFields();
		for(int i = 0; i < fields.length; i++) {
			// take object's value...
			Object object;
			try {
				object = fields[i].get(this);
				if(object == null) continue;
				if(store == null) store = new HashMap<String, Object>();
				if(object instanceof Var || object instanceof Group)
					store.put(fields[i].getName(), object);
				else if(object instanceof Var []) {
					store.put(fields[i].getName(), object);
				}
			} catch (IllegalAccessException ex) {
				continue;
			}
		}
	}
	
	private void makeGlbMap(Object gsd) {
			//Find object of same name in GLB
		if(glbMap != null) return;
		glbMap = new HashMap<String, Integer>();
		Field [] glbFields = gsd.getClass().getDeclaredFields();
		for(int i=0; i<glbFields.length; i++) {
			Object object;
			try  {
				object = glbFields[i].get(gsd);
				if(object == null) continue;
				String n = glbFields[i].getName();
				if(n != null && (object instanceof Var || object instanceof Group)) {
					if(store.containsKey(n)) {
						if(Config.DEBUG) System.out.println("Adding peristence Var " + n);
						Integer v = i;
						glbMap.put(n, v);
					}
				} else if(n != null && (object instanceof Var [])) {
					if(store.containsKey(n)) {
						if(Config.DEBUG) System.out.println("Adding peristence Var " + n + "[]");
						Integer v = i;
						glbMap.put(n, v);
					}
				}
			} catch(IllegalArgumentException ex)  {
			} catch (IllegalAccessException e) {
			}
		}
	}

	public void store(Object gsd) {
		if(store == null || gsd == null) return;
		if(glbMap == null) makeGlbMap(gsd);
		Field [] glbFields = gsd.getClass().getDeclaredFields();
		for(Iterator<String> it = store.keySet().iterator(); it.hasNext(); )  {
			String key = it.next();
			int idx;
			if(glbMap.containsKey(key))
				idx = glbMap.get(key);
			else continue;
			Object obj = store.get(key);
			try {
				if(obj instanceof Var) {
					Var stv = (Var)obj;
					Var glb = (Var) glbFields[idx].get(gsd);
					if(stv == null || glb == null) continue;
					stv.set(glb);
				} else if(obj instanceof Var []) {
					Var [] stv = (Var [])obj;
					Var [] glb = (Var []) glbFields[idx].get(gsd);
					if(stv == null || glb == null) continue;
					int len = stv.length < glb.length ? stv.length : glb.length;
					for(int i=0; i<len; i++) {
						stv[i].set(glb[i]);
					}
				}
			} catch (IllegalArgumentException e) {
			} catch (IllegalAccessException e) {
			}
		}
	}
	
	public void recover(Object gsd) {
		if(glbMap == null || store == null) return;
		Field [] glbFields = gsd.getClass().getDeclaredFields();
		for(Iterator<String> it = store.keySet().iterator(); it.hasNext(); )  {
			String key = it.next();
			Object obj = store.get(key);
			int idx;
			if(glbMap.containsKey(key))
				idx = glbMap.get(key);
			else continue;
			try {
				if(obj instanceof Var) {
					Var stv = (Var)obj;
					Var glb = (Var) glbFields[idx].get(gsd);
					if(stv == null || glb == null) continue;
					glb.set(stv);
				} else if(obj instanceof Var []) {
					Var [] stv = (Var [])obj;
					Var [] glb = (Var []) glbFields[idx].get(gsd);
					if(stv == null || glb == null) continue;
					int len = stv.length < glb.length ? stv.length : glb.length;
					for(int i=0; i<len; i++) {
						glb[i].set(stv[i]);
					}
				}
			} catch (IllegalArgumentException e) {
			} catch (IllegalAccessException e) {
			}
		}
		
	}
}

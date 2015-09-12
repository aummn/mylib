package com.mssint.jclib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This is a class to add indexing array capability to a Var or Group. It is 
 * used by Var and Group classes for storing an array.
 * @author pete
 *
 */
public class VarArray {
	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(VarArray.class);
	private int idxSize;
	private Var[] vars;
	
	VarArray(Var proto, int size) {
		idxSize = size;
		vars = new Var[idxSize];
		for(int i=0; i < idxSize; i++) {
			vars[i] = new Var(proto.attr, proto.displayLength, proto.scale, proto.picture);
		}
	}
	
	public Var index(int idx) {
		return vars[idx];
	}
	
	public int size() { return idxSize; }
}

package com.mssint.jclib;

import java.io.IOException;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Match {
	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(Match.class);
	private Glb GLB;
	private Extract ex1;
	private Extract ex2;
	private boolean ex1HasMore;
	private boolean ex2HasMore;
	private boolean read1;
	private boolean read2;
	private ArrayList<KeySet> keyList;
	private String buf1;
	private String buf2;
	
	
	protected class KeySet {
		int [] offsets = new int[2];
		int [] ends = new int[2];
		boolean [] chars = new boolean[2];
		public KeySet(FrameVar k1, FrameVar k2) {
			offsets[0] = k1.getOffset();
			ends[0] = offsets[0] + k1.getExlen();
			chars[0] = k1.isCharType();
			offsets[1] = k2.getOffset();
			ends[1] = offsets[1] + k2.getExlen();
			chars[1] = k2.isCharType();
		}
		public int compare(String s1, String s2) {
			String p1, p2;
			if(s1.length() >= ends[0]) p1 = s1.substring(offsets[0], ends[0]);
			else if(s1.length() <= offsets[0]) p1 = "";
			else p1 = s1.substring(offsets[0]);
			if(s2.length() >= ends[1]) p2 = s1.substring(offsets[1], ends[1]);
			else if(s2.length() <= offsets[1]) p2 = "";
			else p2 = s2.substring(offsets[1]);
			return p1.compareTo(p2);
		}
	}
	
	public Match(Glb GLB, Extract ex1, Extract ex2) {
		if(ex1 == null || ex2 == null)
			throw new IllegalArgumentException("Cannot specify null extract file arguments");
		this.GLB = GLB;
		this.ex1 = ex1;
		this.ex2 = ex2;
		ex1.open();
		ex2.open();
		GLB.MATCH.set("");
		read1 = true;
		read2 = true;
	}
	
	public void close() {
		ex1.close();
		ex2.close();
	}
	
	public void addKey(FrameVar k1, FrameVar k2) {
		if(keyList == null) keyList = new ArrayList<KeySet>();
		keyList.add(new KeySet(k1, k2));
	}
	
	private int compare(String s1, String s2) {
		for(KeySet key : keyList) {
			int cmp = key.compare(s1, s2);
			if(cmp != 0) return cmp;
		}
		return 0;
	}
	
	public boolean fetch() throws IOException {
		if(!ex1HasMore && !ex2HasMore) return false;
		
		if(read1) {
			ex1HasMore = ex1.fetch();
			if(ex1HasMore) buf1 = ex1.getBuffer();
			else ex1.setBuffer(buf1);
		}
		if(read2) {
			ex2HasMore = ex2.fetch();
			if(ex2HasMore) buf2 = ex2.getBuffer();
			else ex2.setBuffer(buf2);
		}
		
		if(keyList == null) {
			if(ex1HasMore && !ex2HasMore) {
				GLB.MATCH.set(ex1.getShadowName());
				read2 = false;
			} else if(!ex1HasMore && ex2HasMore) {
				GLB.MATCH.set(ex2.getShadowName());
				read1 = false;
			} else GLB.MATCH.set("");
		} else {
			int cmp = compare(buf1, buf2);
			if(cmp < 0) {
				GLB.MATCH.set(ex1.getShadowName());
				read1 = true;
				read2 = false;
			} else if(cmp > 0) {
				GLB.MATCH.set(ex2.getShadowName());
				read1 = false;
				read2 = true;
			} else {
				GLB.MATCH.set("");
				read1 = true;
				read2 = true;
			}
		}
		return ex1HasMore || ex2HasMore;
	}
}

package com.mssint.jclib;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Date;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Timer {
	private static final Logger log = LoggerFactory.getLogger(Timer.class);
	
	protected static class StatRecord {
		@SuppressWarnings("unused")
		private static final Logger log = LoggerFactory.getLogger(StatRecord.class);
        int    level;
        int    sline;
        int    eline;
        long starttime;
        long elapsed;
        String id; //limit to 52 bytes
        private static final String b52 = "                                                    ";
        
        public void write(RandomAccessFile fd) {
        	try {
				fd.writeInt(level);
				fd.writeInt(sline);
				fd.writeInt(eline);
				fd.writeLong(starttime);
				fd.writeLong(elapsed);
				if(id.length() > 52) fd.writeBytes(id.substring(0,52));
				else if(id.length() < 52)
					fd.writeBytes(id + b52.substring(0, 52 - id.length()));
				else fd.writeBytes(id);
				
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
	}
	
	private static boolean active = false;
	private static boolean holdable = false;

	private static String mainClassName = null;
//	private static String repName = null;
//	private static Object mainObject;
	private static File timingFile;
	private static RandomAccessFile outFd = null;
	
	private static Stack<StatRecord> stack;
	public static boolean isActive() { return active; }
	
	public static void init(Object app) {
		if(app == null) return;
		if(active) return;
		mainClassName = app.getClass().getSimpleName();
//		try {
//			Field fld = app.getClass().getDeclaredField("REPORTNAME");
//		    if(fld == null) 
//		    	repName = mainClassName;
//		    else 
//		    	repName = (String)fld.get(app);
//		} catch (Exception e) {}
		
		String path = Config.getProperty("jclib.timing.dir");
		String s = Config.getProperty("jclib.timing.run");
		if(s == null || mainClassName == null) return;
		if(s.toLowerCase().equals("yes") || s.equals("1")) active = true;
		else return;

		holdable = true;
		
		String tf = path + "/" + mainClassName + ".x." + Util.getPid();
		timingFile = new File(tf);
		try {
			outFd = new RandomAccessFile(timingFile, "rw");
			log.info("Timing routines enabled. File="+tf);
		} catch (FileNotFoundException e) {
			log.warn("Cannot write to file '"+tf+"'. Timing routines disabled.");
			active = false;
			holdable = false;
			return;
		}
		stack = new Stack<StatRecord>();
	}
	
	public static void begin(String id) {
		if(!active) return;
		StatRecord stat = new StatRecord();
		stat.sline = 0;
		stat.level = stack.size();
		stat.starttime = new Date().getTime();
		stat.id = id;
		stack.push(stat);
		return;
	}
	
	public static void suspend() { if(holdable) active = false; }
	public static void resume() { if(holdable) active = true; }

	public static void end() {
		if(!active) return;
		StatRecord stat;
		stat = stack.pop();
		if(stat == null) return;
		stat.eline = 0;
		stat.elapsed = new Date().getTime() - stat.starttime;
		stat.write(outFd);
		return;
	}
}

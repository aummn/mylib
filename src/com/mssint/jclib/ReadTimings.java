package com.mssint.jclib;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Read back timings saved in a file. 
 * 
 * @author MssInt
 *
 */
public class ReadTimings {
	private static final Logger log = LoggerFactory.getLogger(ReadTimings.class);

	public class StatRecord {
        int count;
        int max;
        int min;
        long total;
	}

	private double cutoff;
	
	
	private void setCutoff(double cutoff) {
		this.cutoff = cutoff;
	}


	/**
	 * Reads the next 
	 * @param fd
	 * @param sr
	 * @return
	 */
	public String read(RandomAccessFile fd, StatRecord sr) {
        String id; //limit to 52 bytes
        byte [] b = new byte[52];
    	char [] c = new char[52];
        try {
    		int level = fd.readInt(); //level
    		fd.readInt(); //sline
    		fd.readInt(); //eline
    		fd.readLong(); //starttime
    		sr.total = fd.readLong();
    		fd.read(b, 0, 52);
    		for(int i=0;i<52;i++) c[i] = (char)b[i];
    		id = String.valueOf(c);
    		return level + ":" + id;
		} catch (EOFException e) {
			return null;
		} catch (IOException e) {
			return null;
		}
    }

	/**
	 * Load the timings file and write to stdout.
	 * @param fname the name of the file to load.
	 */
	public void loadStats(String fname) {
		
		//First try from current directory
		RandomAccessFile fd;
		String fileName = fname;
		try {
			fd = new RandomAccessFile(fileName, "r");
		} catch (FileNotFoundException e) {
			try {
				String path = "/usr/local/timing";
				if(fname.charAt(0) != '/')
					fileName = path + "/" + fname;
				else fileName = fname;
				fd = new RandomAccessFile(fileName, "r");
			} catch (FileNotFoundException e1) {
				log.info("Cannot open file '" + fileName +"'");
				return;
			}
		}
		Map<String, StatRecord> tmap = new HashMap<String, StatRecord>();

		while(true) {
			StatRecord sr = new StatRecord();
			StatRecord x;
			String id = read(fd, sr);
			if(id == null) break;
			
			//See if a record already exists:
			x = tmap.get(id);
			if(x == null) {
				//System.out.print("Adding key=");
				sr.count = 1;
				sr.max = (int)sr.total;
				sr.min = (int)sr.total;
				tmap.put(id, sr);
				//System.out.println(id + "count="+sr.count+" time=" + sr.total);
			} else {
				//System.out.print("Retrieved key=");
				x.count++;
				x.total += sr.total;
				if((int)sr.total < x.min) x.min = (int)sr.total;
				else if((int)sr.total > x.max) x.max = (int)sr.total;
				//System.out.println(id + "count="+x.count+" time=" + x.total);
			}
		}
		System.out.printf("%-40s %5s %12s %12s %12s %12s\n",
				"ACCESS ROUTINE", "COUNT", "MIN", "MAX", "AVERAGE", "TOTAL");
		Iterator<String>it = tmap.keySet().iterator();
		while(it.hasNext()) {
			String key = it.next();
			StatRecord sr = tmap.get(key);
			double min = sr.min / 1000.0;
			double max = sr.max / 1000.0;
			double ave = (double)sr.total / (double)sr.count;
			ave /= 1000;
			double total = (double)sr.total / 1000;
			if(total >= cutoff)
				System.out.printf("%.40s %5d %12.3f %12.3f %12.3f %12.3f\n",
					key, sr.count, min, max, ave, total);
		}
	}
	
	/**
	 * Run from command line.
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length <  1) {
			System.err.println("Enter filename");
			System.exit(2);
		}
		ReadTimings rt = new ReadTimings();
		String filename;
		if(args.length > 1) {
			double cutoff = 0;
			try {
				cutoff = Double.parseDouble(args[0]);
			} catch(NumberFormatException e) {
				cutoff = 0;
			}
			rt.setCutoff(cutoff);
			filename = args[1];
		} else {
			filename = args[0];
		}
		rt.loadStats(filename);
	}

}

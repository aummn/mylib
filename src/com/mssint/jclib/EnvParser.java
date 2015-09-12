package com.mssint.jclib;

import java.text.SimpleDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Retrieves environment parameters from a given string.
 * 
 * @author MssInt
 *
 */
public class EnvParser {
	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(EnvParser.class);
	ApplicationState app;
	
	public EnvParser(ApplicationState app) {
		this.app = app;
	}
	
	/**
	 * Parses properties out of a String using as necessary the ApplicationState 
	 * and Report objects.
	 * @param p
	 * @param rep
	 * @return
	 */
	public String parseProperty(String p, Report rep) {
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<p.length(); ) {
			if(p.charAt(i) == '%') {
				if((i+1) < p.length()) {
				    if(p.charAt(i+1) == '%') {
						sb.append('%');
						i += 2;
				    } else if(p.charAt(i+1) == '{') {
					    String s = subSegment(p.substring(i), '{', '}');
						i += s.length();
						s = getValue(s, rep);
						sb.append(s);
					} else {
						Util.abort("Cannot parse property value '"+p.substring(i)+"'.");
					}
				
				}
			} else if(p.charAt(i) == '$') {
			} else {
				sb.append(p.charAt(i++));
			}
		}
		return sb.toString();
	}
	
	/**
	 * Finds a matching end tag/delimiter and returns all the data within the 
	 * tag boundaries.
	 * 
	 * If no matching end tag or an unmatched tag is found in the String 
	 * passed in the system will log and exit.
	 * 
	 * @param p String from which we are searching for the matching end tag/delimiter
	 * @param open The start tag/delimiter
	 * @param close The end tag/delimiter
	 * @return the substring to be found within tags/delimeter
	 */
	private String subSegment(String p, char open, char close) {
		int level = 0;
		int end;
		for(end=1; end<p.length(); end++) {
			if(p.charAt(end) == open) level++;
			else if(p.charAt(end) == close) level--;
			if(level == 0) break;
		}
		end++;
		if(level > 0) Util.abort("Missing closing '"+close+"' in string '"+p+"'.");
	    else if(level < 0) Util.abort("Too many closing '"+close+"' in string '"+p+"'.");
		return p.substring(0, end);
	}
	
	/**
	 * Get the value from the environment (Report values, Date, Time, Process ID.
	 * The following values are obtainable : -
	 * REPORTNAME 	- From ApplicationState.
	 * PID 			- Process Identifier
	 * SEQ 			- Report Sequence Number 
	 * SHADOW 		- Shadow Name of Report
	 * TITLE		- Report Title
	 * DATE
	 * TIME
	 * 
	 * @param p the sub-segment needing a value lookup
	 * @param rep the report being used if null returned values 
	 * @return the value
	 */
	private String getValue(String p, Report rep) {
		if(p.charAt(0) == '%' && p.charAt(1) == '{' && p.charAt(p.length()-1) == '}') {
			p = p.substring(2, p.length()-1);
			if(p.indexOf("%") != -1) p = parseProperty(p, rep);
			if(p.compareToIgnoreCase("REPORTNAME") == 0) return app.REPORTNAME;
			else if(p.compareToIgnoreCase("PID") == 0) return Util.getPid();
		    else if(p.compareToIgnoreCase("SEQ") == 0 && rep != null) 
				return rep.getSequenceString();
		    else if(p.compareToIgnoreCase("SHADOW") == 0 && rep != null) 
		        return rep.getShadowName();
		    else if(p.compareToIgnoreCase("TITLE") == 0 && rep != null) 
		        return rep.getTitle();
		    else if(p.compareToIgnoreCase("DATE") == 0)
		        return new SimpleDateFormat("yyyyMMdd").format(SystemDate.getDate()).toUpperCase();
		    else if(p.compareToIgnoreCase("TIME") == 0)
		        return new SimpleDateFormat("HHmm").format(SystemDate.getDate());
		}
		return p;
	}
	
}

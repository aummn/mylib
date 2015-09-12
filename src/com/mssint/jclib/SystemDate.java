package com.mssint.jclib;

import java.util.*;
import java.text.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemDate {
	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(SystemDate.class);
	private static long offset   = 0;
	
	public static Date getDate () {
		Date date = new Date();
		return new Date (date.getTime()-offset);
	}
	
	public static Calendar getCalendar () {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime (getDate());
		return calendar;
	}
	
	public static void setSystemDate (Date newDate) {
		offset = new Date().getTime() - newDate.getTime();
	}
	
	public static boolean setSystemDate (String dateString) {
		SimpleDateFormat formatter = new SimpleDateFormat ("dd-MM-yyyy H:mm:ss");
		ParsePosition    pos       = new ParsePosition (0);
		
		Date date = formatter.parse(dateString,pos);
		if (date != null) {
			setSystemDate (date);
			return true;
		}
		else {
			return false;
		}	
	}
}

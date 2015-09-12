package com.mssint.jclib;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A general utility class offering some extended String processing functions that are required.
 * It is a stateless implementation and all methods are static.
 * 
 * <p>last rebuilt %DATE; </p>
 * 
 * @version %BUILD;
 * @author Steve Rainbird
*/
public class Strings {
@SuppressWarnings("unused")
private static final Logger log = LoggerFactory.getLogger(Strings.class);

    /** 
     * Remove leading whitespace from a String 
     * Uses regular expression match "^ +" to achieve this.
	 * @param source String, should not be null.
	 * @return String with all leading spaces removed.
	*/
    public static String ltrim(String source) {
        //return source.replaceAll("^\\s+", "");
        return source.replaceAll("^ +", "");
    }

    /** 
     * Remove trailing whitespace from a String 
     * Uses regular expression match " +$" to achieve this.
	 * @param source String, should not be null.
	 * @return String with all trailing spaces removed.
	*/
    public static String rtrim(String source) {
        //return source.replaceAll("\\s+$", "");
        return source.replaceAll(" +$", "");
    }

    /** 
     * Replace multiple whitespaces between words with single space 
     * Uses "\\b\\s{2,}\\b" to achieve this.  
	 * @param source string to clean up.
	 * @return String with multiple whitespaces removed.
	*/
    public static String itrim(String source) {
        return source.replaceAll("\\b\\s{2,}\\b", " ");
    }


	/**
	 * Remove leading and trailing white space from a String.
	 * Combines ltrim and rtrim functions from the class.
	 * {@see #ltrim}
	 * {@see #rtrim}
	 * 
	 * @param source String.
	 * @return String with leading and trailing spaces removed.
	*/
    public static String lrtrim(String source){
        return ltrim(rtrim(source));
    }

	/** 
 	* Pads a string to the right with padChar and returns a final 
  	* string of at least length characters. 
  	* 
	* @param source String to right pad, should not be null.
	* @param length of return string, if less than source String returns the source String contents.
	* @param padChar the padding character.
	* @return the padded String or source string contents if length <= source String length.
   	*/ 
	public static String rpad(String source, int length, char padChar){ 
    	if (source.length() >= length) return source; 
    	char[] rightPart = new char[length  - source.length() ];
    	Arrays.fill(rightPart, padChar); 
    	return source + new String(rightPart); 
	}			 
	
	/** 
 	* Pads a string to the right with spaces and returns a final 
  	* string of at least length characters. 
  	* Uses {@see #rpad} 
	* @param source String to right pad, should not be null.
	* @param length of return string, if less than source String returns the source String contents.
	* @return the padded String or source string contents if length <= source String length.
   	*/ 
	public static String rpad(String source, int length){ 
		return(rpad(source,length,' '));
	}
	
	/** 
 	* Pads a string to the left with padChar and returns a final 
  	* string of at least length characters. 
  	* 
	* @param source String to right pad, should not be null.
	* @param length of return string, if less than source String returns the source String contents.
	* @param padChar the padding character.
	* @return the left padded String or source string contents if length <= source String length.
   	*/ 
	public static String lpad(String source, int length, char padChar){ 
    	if (source.length() >= length) return source; 
    	char[] leftPart = new char[length  - source.length() ];
    	Arrays.fill(leftPart, padChar); 
    	return new String(leftPart) + source;
	}			 

	/** 
 	* Pads a string to the left with spaces and returns a final 
  	* string of at least length characters. 
  	* Uses {@see #lpad} 
	* @param source String to right pad, should not be null.
	* @param length of return string, if less than source String returns the source String contents.
	* @return the padded String or source string contents if length <= source String length.
   	*/ 
	public static String lpad(String source, int length){ 
		return(lpad(source,length,' '));
	}
	
	/** 
 	* Formats a double into a string with leading and trailing 0's removing the sign.
 	* e.g. if the arguments were 12.23,8,4 the output would be the string "0012.230"
 	* 
	* @param source double to be formatted.
	* @param total length of the return string.
	* @param total size of the fractional part.
	* @return the formatted String.
   	*/ 
	public static String format(double source, int length, int dec) {
		String intPart, decPart;
		int i = (int)Math.abs(source);
		double d = source - i;
		intPart  = Integer.toString(i);
		decPart = Double.toString(d);
		return lpad(intPart,length - dec,'0') + rpad(decPart,dec,'0');
	}
}

package com.mssint.jclib;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Emulates the COBOL PIC/PICTURE variable.
 * 
 * Allows for formatting 
 * 
 * @author MssInt
 *
 */
public class Picture implements Serializable {
	private static final long serialVersionUID = 1L;
	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(Picture.class);
	private String picture;
	private short numlen = 0;
	private short declen = 0;
	private short dec = -1;
	private boolean isSign;
	private boolean isTrailingSign;
	private boolean noLeadingZeros;
	boolean isFormatted; 
	
	private static final String spaces = "                                                ";

	//Create a new Picture for remembering the lost decimals from a source assignment string.
	public Picture(int decimals, String str) {
		if(decimals < str.length()) {
			if(decimals >= 0) 
				str = str.substring(decimals);
		}
		dec = (short)decimals; //The number of decimals for the Var
		picture = str;	//The decimal string portion to save
		declen = (short)str.length(); //The length of the decimal strings
	}
	
	//For a complex move, get the extra number of decimals supplied by a Dtring assignement.
	int getExtraDecimals() {
		return declen - dec;
	}
	String getExtraDecimalString() {
		return picture;
	}
	
	/**
	 * 
	 * Constructor taking a Picture definition in as a String.
	 * Validates the definition against permitted/expected structure 
	 * Throws an IllegalArgumentException if not in correct format null or empty. 
	 * 
	 * @param pic String the defintion of the Picture structure 
	 * @throws IllegalArgumentException when null, empty or in an unrecognised format.
	 */
	public Picture(String pic) throws IllegalArgumentException {
		if(pic == null) throw new IllegalArgumentException("Picture clause cannot be null.");
		if(pic.length() < 1) throw new IllegalArgumentException("Picture string is zero length.");
		boolean nineSeen = false;
		picture = "";
		
		int x;
		for(x=0; x < pic.length(); ) {
			switch(pic.charAt(x)) {
			case '9':
				picture = picture + "9";
				if(dec == -1) numlen++;
				else declen++;
				nineSeen = true;
				break;
			case 'Z':
			case 'z':
			case '*':
				if(nineSeen) 
					throw new IllegalArgumentException("Picture clause cannot have 'Z' after a '9'");
				picture = picture + "Z";
				if(dec == -1) numlen++;
				else declen++;
//				noLeadingZeros = true;
				break;
			case 'B':
			case 'b':
				picture = picture + "B";
				noLeadingZeros = true;
				break;
			case '-':
				picture = picture + "-";
				if(isSign && picture.charAt(picture.length()-1) != '-') 
					throw new IllegalArgumentException("Only one non-contiguous minus sign allowed.");
				/*if(dec == -1) {
					if(isSign) numlen++;
					isSign = true;
					noLeadingZeros = true;
				} else {
					if(isSign) 
						throw new IllegalArgumentException("Leading and trailing sign???");
					isTrailingSign = true;
				}*/
				if(x == (pic.length() - 1) && picture.charAt(picture.length()-2) != '-') { //trailing sign and not all minuses
					if(isSign) 
						throw new IllegalArgumentException("Leading and trailing sign???");
					isTrailingSign = true;
				} else {
					if(isSign) numlen++;
					isSign = true;
					noLeadingZeros = true;
				}
				
				
				break;
			case '$':
				if(nineSeen) 
					throw new IllegalArgumentException("Picture clause cannot have '$' after a '9'");
				picture = picture + "$";
				if(dec == -1) {
					//Whyif(isSign) numlen++;
					numlen++;
				} else throw new IllegalArgumentException("Dollar sign illegal after dec point.");
				//WhyisSign = true;
				noLeadingZeros = true;
				break;
			case '.':
				if(dec != -1) 
					throw new IllegalArgumentException("Maximum 1 decimal point.");
				dec = (short)picture.length();
				picture = picture + '.';
				break;
			
			case '(': //(nn) - repeat the last character nn times.
				if(picture.length() < 1) 
					throw new IllegalArgumentException("Cannot repeat nothing!");
				x++;
				int i = pic.indexOf(')', x);
				if(i == -1) 
					throw new IllegalArgumentException("Cannot locate closing bracket.");
				String nn = pic.substring(x, i);
				char lc = picture.charAt(picture.length() - 1);
				int len = 0;
				len = Integer.parseInt(nn) - 1;
				if(lc == 'Z' || lc == '9' || lc == '-' || lc == '+' || lc == '$') {
					if(dec == -1) numlen += len;
					else declen += len;
				}
				x = i;
				if(len > 32) 
					throw new IllegalArgumentException("Numeric value ib brackets too large.");
				for(i=0; i < len; i++) picture = picture + lc;
				break;
			default:
				picture = picture + pic.charAt(x);
				noLeadingZeros = true;
				break;
			}
			x++;
		}
	}
	
	public boolean hasNoLeadingZero() {
		return noLeadingZeros;
	}
	
	/**
	 * Property returning the total number of characters making up the Picture 
	 * that is internally stored as a string  
	 * 
	 * @return int length of internal string used to store the Picture
	 */
	public int length() { return picture.length(); }
	
	/**
	 * Property returning the number of decimals represented within the Picture.
	 * 
	 * @return int number of decimal places defined in the Picture
	 */
	public int decimals() { return declen; }

	/**
	 * Given a definition of a Picture as created via the constructor of this 
	 * class. 
	 * Retrieve it's representation in the given format.
	 * Note no zero blanking will take place.
	 *
	 * @param val long defining the format as a long e.g. 999999
	 * @return String of the Picture as represted in this format. 
	 */
	public String format(long val) {
		String v = Long.toString(val);
		return format(v);
	}
	
	/**
	 * Given a definition of a Picture as created via the constructor of this 
	 * class. 
	 * Retrieve it's representation in the given format.
	 * Note no zero blanking will take place.
	 * 
	 * @param val long defining the format as a long e.g. ########
	 * @return String of the Picture as represted by the inbound format. 
	 * 
	 */
	public String format(String val) {
		return format(val, false);
	}
	
	/**
	 * Given a definition of a Picture as created via the constructor of this 
	 * class. 
	 * Retrieve it's representation in the given format.
	 * 
	 * @param val long defining the format as a long e.g. ########
	 * @param zeroblank boolean indicating if 0's are required to fit the format request size.
	 * @return String of the Picture as represted by the inbound format. 
	 */
	public String format(String valStr, boolean zeroblank) {
		if(valStr == null) valStr = "";
		StringBuilder val = new StringBuilder(valStr.trim());
		StringBuilder sb = new StringBuilder();
		boolean sign;
//		boolean trailingSign;
		boolean dollarNeeded = false;
		isFormatted = true;

		
		int i = val.indexOf("-");
		if(i == -1) {
			sign = false;
//			trailingSign = false;
		} else {
			if(isSign) sign = true;
			else sign = false;
			if(isTrailingSign) sign = true;

//			if(isTrailingSign) sign = trailingSign=true;
//			else trailingSign = false;
			val.deleteCharAt(i);
		}
		
		int decV = val.indexOf(".");
		String mantissa = "";
		String fraction = "";
		if(declen > 0) {
			if(decV != -1) fraction = val.substring(decV + 1);
			for(i=fraction.length();i<declen;i++) fraction += '0';
			if(fraction.length() > declen) fraction = fraction.substring(0, declen);
		}
		if(numlen > 0) {
			if(decV != -1) mantissa = val.substring(0, decV);
			else mantissa = val.toString();
			if(mantissa.length() < numlen) {
				mantissa = spaces.substring(0, numlen - mantissa.length()) + mantissa;
			} else if(mantissa.length() > numlen) 
				mantissa = mantissa.substring(mantissa.length() - numlen);
		}
		if(isSign) mantissa = " " + mantissa;
		if(isTrailingSign) mantissa += " ";
		
		int pF;
		int pV;
		int rlen;
		if(dec == -1) rlen = picture.length();
		else rlen = dec;
		boolean numStart = false;
		boolean allzeros = true;

		for(pF=0, pV=0; pF < rlen; pF++) {
			if(picture.charAt(pF) == 'B') sb.append(' ');
			else if(picture.charAt(pF) == '9') {
				//Why (doesn't work for 99/99/99) if(mantissa.length() > pF)
				if(mantissa.charAt(pV) == ' ') {
					sb.append('0');
					pV++;
				} else {
					if(mantissa.charAt(pV) != '0') allzeros = false;
					sb.append(mantissa.charAt(pV++));
				}
				numStart = true;
			} else if(picture.charAt(pF) == '-') {
				if(isTrailingSign) {
					if(sign) {
						sb.append("-");
					} else {
						sb.append(" ");
					}
					continue;
				}
				if(mantissa.charAt(pV) == '0' || mantissa.charAt(pV) == ' ') {
					if(numStart) sb.append('0');
					else sb.append(' ');
					pV++;
				} else {
					allzeros = false;
					sb.append(mantissa.charAt(pV++));
					numStart = true;
				}
				if(sign) {
					if(numStart) {
						if(pV > 1) sb.replace(pV-2, pV-1, "-");
						else sb.replace(pV-1, pV, "-");
						sign = false;
					} else {
						if(picture.charAt(pF+1) != '-') {
							sb.replace(pV-1, pV, "-");
							sign = false;
						}
					}
				}
			} else if(picture.charAt(pF) == '$') {
				dollarNeeded = true;
				if(mantissa.charAt(pV) == '0' || mantissa.charAt(pV) == ' ') {
					if(numStart) sb.append('0');
					else sb.append(' ');
					pV++;
				} else {
					allzeros = false;
					sb.append(mantissa.charAt(pV++));
					numStart = true;
				}
			} else if(picture.charAt(pF) == 'Z') {
				if(mantissa.charAt(pV) == '0' || mantissa.charAt(pV) == ' ') {
					if(numStart) sb.append('0');
					else sb.append(' ');
					pV++;
				} else {
					allzeros = false;
					sb.append(mantissa.charAt(pV++));
					numStart = true;
				}
			} else if(picture.charAt(pF) == '*') {
				if(mantissa.charAt(pV) == '0' || mantissa.charAt(pV) == '*') {
					if(numStart) sb.append('0');
					else sb.append('*');
					pV++;
				} else {
					allzeros = false;
					sb.append(mantissa.charAt(pV++));
					numStart = true;
				}
			} else {
				if(numStart) sb.append(picture.charAt(pF));
				else sb.append(' ');
			}
			
		}
		if(dollarNeeded) {
			for(i=0; i<sb.length();i++) {
				if(sb.charAt(i) == ' ') continue;
				if(i > 0) sb.replace(i-1, i, "$");
				else sb.replace(0, 1, "$");
				dollarNeeded = false;
				break;
			}
			if(dollarNeeded) {
				sb.replace(sb.length()-1, sb.length(), "$");
				numStart = true;
			}
		}
		if(dec != -1) {
			int dpos = sb.length();
			if(numStart) sb.append('.');
			else sb.append(' ');
			pV = 0;
			for(pF = dec+1; pF < picture.length(); pF++) {
				if(picture.charAt(pF) == 'B') sb.append(' ');
				else if(picture.charAt(pF) == '9') {
					if(fraction.charAt(pV) != '0') allzeros = false;
					sb.append(fraction.charAt(pV++));
					if(!numStart) sb.replace(dec, dec+1, ".");
					numStart = true;
				} else if(picture.charAt(pF) == 'Z') {
					if(fraction.charAt(pV) != '0') allzeros = false;
					if(numStart) sb.append(fraction.charAt(pV++));
					else {
						if(fraction.charAt(pV) == '0') {
							sb.append(' ');
							pV++;
						} else {
							sb.replace(dpos, dpos+1, ".");
							for(i=dpos+1; i<sb.length();i++)
								sb.replace(i, i+1, "0");
							sb.append(fraction.charAt(pV++));
							numStart = true;
						}
					}
				} else if(picture.charAt(pF) == '-') {
					if(sign) 
						sb.append("-");
					else
						sb.append(" ");
					sign = false;
				}
				
			}
		}
		if(zeroblank && allzeros) {
			for(i=0; i<sb.length(); i++) {
				sb.replace(i, i+1, " ");
			}
		}
		
		return sb.toString();
	}
	
	/**
	 * Given a definition of a Picture as created via the constructor of this 
	 * class. 
	 * Retrieve it's representation in the given format.
	 * 
	 * @param val long defining the format as a long e.g. ########
	 * @param zeroblank boolean indicating if 0's are required to fit the format request size.
	 * @return String of the Picture as represented by the inbound format. 
	 */
	public String formatx(String val) {
		isFormatted = true;
		if(val == null) val = "";
		val=val + spaces.substring(0, picture.length());
		StringBuilder sb = new StringBuilder();
		
		int pF;
		int pV;
		int rlen = picture.length();

		for(pF=0, pV=0; pF < rlen; pF++) {
			if(picture.charAt(pF) == 'B') sb.append(' ');
			else if(picture.charAt(pF) == 'X') {
				sb.append(val.charAt(pV++));
			} else if(picture.charAt(pF) == '/') sb.append('/');
		}
		
		return sb.toString();
	}
}

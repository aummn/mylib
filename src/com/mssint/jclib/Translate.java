package com.mssint.jclib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Translate {
	private static final Logger log = LoggerFactory.getLogger(Translate.class);
	
	private short [] e2a;
	private short [] a2e;
	
	public Translate() {
		String t = Config.getProperty("jclib.translate.table");
		String tt;
		if(t == null) tt = null;
		else tt = t.toUpperCase();
		
		if(tt == null) {
			e2a = ebcdic2asciiWestern;
			a2e = ascii2ebcdicWestern;
		} else if(tt.compareTo("LATIN2") == 0) {
			e2a = ebcdic2asciiLatin2;
			a2e = ascii2ebcdicLatin2;
		} else
			throw new IllegalArgumentException("Unsupported translation table: jclib.translate.table="+t);
		if(log.isDebugEnabled())
			log.debug("Loaded translation tables for "+ (tt==null? "Default" : tt));
	}
	
	public short [] getEbcdic2Ascii() { return e2a; }
	public short [] getAscii2Ebcdic() { return a2e; }
	
	//These are the standard Western tables.
	private short [] ascii2ebcdicWestern = {
		/*         0    1    2    3    4    5    6    7    8    9    A    B    C    D    E    F */
		/*0*/	0x00,0x01,0x02,0x03,0x37,0x2D,0x06,0x2F,0x16,0x05,0x25,0x0B,0x0C,0x0D,0x0E,0x0F,
		/*1*/	0x10,0x11,0x12,0x13,0x14,0x3D,0x32,0x26,0x18,0x19,0x3F,0x27,0x1C,0x1D,0x1E,0x1F,
		/*2*/	0x40,0x5A,0x7F,0x7B,0x5B,0x6C,0x50,0x7D,0x4D,0x5D,0x5C,0x4E,0x6B,0x60,0x4B,0x61,
		/*3*/	0xF0,0xF1,0xF2,0xF3,0xF4,0xF5,0xF6,0xF7,0xF8,0xF9,0x7A,0x5E,0x4C,0x7E,0x6E,0x6F,
		/*4*/	0x7C,0xC1,0xC2,0xC3,0xC4,0xC5,0xC6,0xC7,0xC8,0xC9,0xD1,0xD2,0xD3,0xD4,0xD5,0xD6,
		/*5*/	0xD7,0xD8,0xD9,0xE2,0xE3,0xE4,0xE5,0xE6,0xE7,0xE8,0xE9,0xAD,0xE0,0xBD,0x5F,0x6D,
		/*6*/	0x79,0x81,0x82,0x83,0x84,0x85,0x86,0x87,0x88,0x89,0x91,0x92,0x93,0x94,0x95,0x96,
		/*7*/	0x97,0x98,0x99,0xA2,0xA3,0xA4,0xA5,0xA6,0xA7,0xA8,0xA9,0xC0,0x4F,0xD0,0xA1,0x07,
		/*8*/	0x80,0x81,0x82,0x83,0x84,0x85,0x86,0x87,0x88,0x89,0x8A,0x8B,0x8C,0x8D,0x8E,0x8F,
		/*9*/	0x90,0x91,0x92,0x93,0x94,0x95,0x96,0x97,0x98,0x99,0x9A,0x9B,0x9C,0x9D,0x9E,0x9F,
		/*A*/	0x20,0xAA,0x4A,0xB1,0x9F,0xB2,0x6A,0xB5,0xBB,0xB4,0x9A,0x8A,0xB0,0x60,0xAF,0xBC,
		/*B*/	0x90,0x8F,0xEA,0xFA,0x7D,0xA0,0xB6,0xB3,0x9D,0xDA,0x9B,0x8B,0xB7,0xB8,0xB9,0xAB,
		/*C*/	0x64,0x65,0x62,0x66,0x63,0x67,0x9E,0x68,0x74,0x71,0x72,0x73,0x78,0x75,0x76,0x77,
		/*D*/	0xAC,0x69,0xED,0xEE,0xEB,0xEF,0xEC,0xBF,0x80,0xFD,0xFE,0xFB,0xFC,0xBA,0x8E,0x59,
		/*E*/	0x44,0x45,0x42,0x46,0x43,0x47,0x9C,0x48,0x54,0x51,0x52,0x53,0x58,0x55,0x56,0x57,
		/*F*/	0x8C,0x49,0xCD,0xCE,0xCB,0xCF,0xCC,0xE1,0x70,0xDD,0xDE,0xDB,0xDC,0x8D,0xAE,0xDF
	};
		
	private short [] ebcdic2asciiWestern = { 
		/*         0    1    2    3    4    5    6    7    8    9    A    B    C    D    E    F */
		/*0*/	0x00,0x01,0x02,0x03,0x04,0x09,0x06,0x7F,0x08,0x09,0x0A,0x0B,0x0C,0x0D,0x0E,0x0F,
		/*1*/	0x10,0x11,0x12,0x13,0x14,0x0A,0x08,0x17,0x18,0x19,0x1A,0x1B,0x1C,0x1D,0x1E,0x1F,
		/*2*/	0x20,0x21,0x1C,0x23,0x24,0x0A,0x17,0x1B,0x28,0x29,0x2A,0x2B,0x2C,0x05,0x06,0x07,
		/*3*/	0x30,0x31,0x16,0x0D,0x34,0x35,0x36,0x04,0x38,0x09,0x0C,0x3B,0x14,0x15,0x3E,0x1A,
		/*4*/	0x20,0xA0,0xE2,0xE4,0xE0,0xE1,0xE3,0xE5,0xE7,0xF1,0xA2,0x2E,0x3C,0x28,0x2B,0x7C,
		/*5*/	0x26,0xE9,0xEA,0xEB,0xE8,0xED,0xEE,0xEF,0xEC,0xDF,0x21,0x24,0x2A,0x29,0x3B,0x5E,
		/*6*/	0x2D,0x2F,0xC2,0xC4,0xC0,0xC1,0xC3,0xC5,0xC7,0xD1,0xA6,0x2C,0x25,0x5F,0x3E,0x3F,
		/*7*/	0xF8,0xC9,0xCA,0xCB,0xC8,0xCD,0xCE,0xCF,0xCC,0x60,0x3A,0x23,0x40,0x27,0x3D,0x22,
		/*8*/	0xD8,0x61,0x62,0x63,0x64,0x65,0x66,0x67,0x68,0x69,0xAB,0xBB,0xF0,0xFD,0xFE,0xB1,
		/*9*/	0xB0,0x6A,0x6B,0x6C,0x6D,0x6E,0x6F,0x70,0x71,0x72,0xAA,0xBA,0xE6,0xB8,0xC6,0xA4,
		/*A*/	0xB5,0x7E,0x73,0x74,0x75,0x76,0x77,0x78,0x79,0x7A,0xA1,0xBF,0xD0,0x5B,0xDE,0xAE,
		/*B*/	0xAC,0xA3,0xA5,0xB7,0xA9,0xA7,0xB6,0xBC,0xBD,0xBE,0xDD,0xA8,0xAF,0x5D,0xB4,0xD7,
		/*C*/	0x7B,0x41,0x42,0x43,0x44,0x45,0x46,0x47,0x48,0x49,0xAD,0xF4,0xF6,0xF2,0xF3,0xF5,
		/*D*/	0x7D,0x4A,0x4B,0x4C,0x4D,0x4E,0x4F,0x50,0x51,0x52,0xB9,0xFB,0xFC,0xF9,0xFA,0xFF,
		/*E*/	0x5C,0xF7,0x53,0x54,0x55,0x56,0x57,0x58,0x59,0x5A,0xB2,0xD4,0xD6,0xD2,0xD3,0xD5,
		/*F*/	0x30,0x31,0x32,0x33,0x34,0x35,0x36,0x37,0x38,0x39,0xB3,0xDB,0xDC,0xD9,0xDA,0xFF
	};
	
	//Latin2 ISO/Latin2 EBCDIC tables
	private short [] ascii2ebcdicLatin2 = {
		/*         0    1    2    3    4    5    6    7    8    9    A    B    C    D    E    F */
		/*0*/	0x00,0x01,0x02,0x03,0x37,0x2D,0x2E,0x2F,0x16,0x05,0x25,0x0B,0x0C,0x0D,0x0E,0x0F,
		/*1*/	0x10,0x11,0x12,0x13,0x3C,0x3D,0x32,0x26,0x18,0x19,0x3F,0x27,0x1C,0x1D,0x1E,0x1F,
		/*2*/	0x40,0x4F,0x7F,0x7B,0x5B,0x6C,0x50,0x7D,0x4D,0x5D,0x5C,0x4E,0x6B,0x60,0x4B,0x61,
		/*3*/	0xF0,0xF1,0xF2,0xF3,0xF4,0xF5,0xF6,0xF7,0xF8,0xF9,0x7A,0x5E,0x4C,0x7E,0x6E,0x6F,
		/*4*/	0x7C,0xC1,0xC2,0xC3,0xC4,0xC5,0xC6,0xC7,0xC8,0xC9,0xD1,0xD2,0xD3,0xD4,0xD5,0xD6,
		/*5*/	0xD7,0xD8,0xD9,0xE2,0xE3,0xE4,0xE5,0xE6,0xE7,0xE8,0xE9,0x4A,0xE0,0x5A,0x5F,0x6D,
		/*6*/	0x79,0x81,0x82,0x83,0x84,0x85,0x86,0x87,0x88,0x89,0x91,0x92,0x93,0x94,0x95,0x96,
		/*7*/	0x97,0x98,0x99,0xA2,0xA3,0xA4,0xA5,0xA6,0xA7,0xA8,0xA9,0xC0,0x6A,0xD0,0xA1,0xFF,
		/*8*/	0x20,0x21,0x22,0x23,0x24,0x15,0x06,0x17,0x28,0x29,0x2A,0x2B,0x2C,0x09,0x0A,0x1B,
		/*9*/	0x30,0x31,0x1A,0x33,0x34,0x35,0x36,0x08,0x38,0x39,0x3A,0x3B,0x04,0x14,0x3E,0x07,
		/*A*/	0x41,0xB1,0x80,0xBA,0x9F,0x77,0xAA,0xB5,0xBD,0xBC,0xAF,0xFD,0xB9,0xCA,0xB8,0xB4,
		/*B*/	0x90,0xA0,0x9E,0x9A,0xBE,0x57,0x8A,0x70,0x9D,0x9C,0x8F,0xDD,0xB7,0x64,0xB6,0xB2,
		/*C*/	0xED,0x65,0x62,0x66,0x63,0x78,0x69,0x68,0x67,0x71,0x72,0x73,0xDA,0x75,0x76,0xFA,
		/*D*/	0xAC,0xBB,0xAB,0xEE,0xEB,0xEF,0xEC,0xBF,0xAE,0x74,0xFE,0xFB,0xFC,0xAD,0xB3,0x59,
		/*E*/	0xCD,0x45,0x42,0x46,0x43,0x58,0x49,0x48,0x47,0x51,0x52,0x53,0xDF,0x55,0x56,0xEA,
		/*F*/	0x8C,0x9B,0x8B,0xCE,0xCB,0xCF,0xCC,0xE1,0x8E,0x54,0xDE,0xDB,0xDC,0x8D,0x44,0xB0
	};
		
	private short [] ebcdic2asciiLatin2 = { 
		/*         0    1    2    3    4    5    6    7    8    9    A    B    C    D    E    F */
		/*0*/	0x00,0x01,0x02,0x03,0x9C,0x09,0x86,0x9F,0x97,0x8D,0x8E,0x0B,0x0C,0x0D,0x0E,0x0F,
		/*1*/	0x10,0x11,0x12,0x13,0x9D,0x85,0x08,0x87,0x18,0x19,0x92,0x8F,0x1C,0x1D,0x1E,0x1F,
		/*2*/	0x80,0x81,0x82,0x83,0x84,0x0A,0x17,0x1B,0x88,0x89,0x8A,0x8B,0x8C,0x05,0x06,0x07,
		/*3*/	0x90,0x91,0x16,0x93,0x94,0x95,0x96,0x04,0x98,0x99,0x9A,0x9B,0x14,0x15,0x9E,0x1A,
		/*4*/	0x20,0xA0,0xE2,0xE4,0xFE,0xE1,0xE3,0xE8,0xE7,0xE6,0x5B,0x2E,0x3C,0x28,0x2B,0x21,
		/*5*/	0x26,0xE9,0xEA,0xEB,0xF9,0xED,0xEE,0xB5,0xE5,0xDF,0x5D,0x24,0x2A,0x29,0x3B,0x5E,
		/*6*/	0x2D,0x2F,0xC2,0xC4,0xBD,0xC1,0xC3,0xC8,0xC7,0xC6,0x7C,0x2C,0x25,0x5F,0x3E,0x3F,
		/*7*/	0xB7,0xC9,0xCA,0xCB,0xD9,0xCD,0xCE,0xA5,0xC5,0x60,0x3A,0x23,0x40,0x27,0x3D,0x22,
		/*8*/	0xA2,0x61,0x62,0x63,0x64,0x65,0x66,0x67,0x68,0x69,0xB6,0xF2,0xF0,0xFD,0xF8,0xBA,
		/*9*/	0xB0,0x6A,0x6B,0x6C,0x6D,0x6E,0x6F,0x70,0x71,0x72,0xB3,0xF1,0xB9,0xB8,0xB2,0xA4,
		/*A*/	0xB1,0x7E,0x73,0x74,0x75,0x76,0x77,0x78,0x79,0x7A,0xA6,0xD2,0xD0,0xDD,0xD8,0xAA,
		/*B*/	0xFF,0xA1,0xBF,0xDE,0xAF,0xA7,0xBE,0xBC,0xAE,0xAC,0xA3,0xD1,0xA9,0xA8,0xB4,0xD7,
		/*C*/	0x7B,0x41,0x42,0x43,0x44,0x45,0x46,0x47,0x48,0x49,0xAD,0xF4,0xF6,0xE0,0xF3,0xF5,
		/*D*/	0x7D,0x4A,0x4B,0x4C,0x4D,0x4E,0x4F,0x50,0x51,0x52,0xCC,0xFB,0xFC,0xBB,0xFA,0xEC,
		/*E*/	0x5C,0xF7,0x53,0x54,0x55,0x56,0x57,0x58,0x59,0x5A,0xEF,0xD4,0xD6,0xC0,0xD3,0xD5,
		/*F*/	0x30,0x31,0x32,0x33,0x34,0x35,0x36,0x37,0x38,0x39,0xCF,0xDB,0xDC,0xAB,0xDA,0x7F
	};
		
}

package com.mssint.jclib;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @author Peter Colman (p.r.colman@gmail.com)
 *
 * GroupData holds the byte array data for a group.
 *
 */
public class GroupData implements Serializable {
	
//	private byte [] data;
	protected GroupByteArray byteArray;
	protected GroupByteArray swappedByteArray;
//	protected int gbaOffset;
	private int gbaSize;
	private final Group pGroup;
//	private final SortedSet<Group>registeredGroups = new TreeSet<Group>(new GroupComparator());
//	public static int markChanged;
//	public static int hasChanged;
//	public static int hasNotChanged;

	@SuppressWarnings("unused")
	private GroupData() {
		pGroup = null;
	}
	
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		byte [] data = byteArray.data;
		for(int i=0;i < gbaSize;i++) {
			if(i > 0) sb.append('.');
			sb.append(String.format("%02X", data[i]&0xff));
		}
		return sb.toString();
	}
	
	/**
	 * Construct a new GroupData instance, setting the size of the data
	 * store and the fill character.
	 * @param length The length of the data store
	 * @param fill The fill character
	 */
	public GroupData(Group primaryGroup, int length, byte fill) {
		byteArray = new GroupByteArray(false, length);
		gbaSize = length;
		byteArray.fill(0, gbaSize, fill);
//		registeredGroups.add(primaryGroup);
		pGroup = primaryGroup;
	}
	
	/**
	 * Construct a new GroupData instance, setting the size and using a global GroupByteArray instance.
	 * @param array
	 * @param length
	 * @param fill
	 */
	/*
	public GroupData(GroupByteArray gba, int length, byte fill) {
		if(gba == null) {
			gba = new GroupByteArray(false, length);
		}
		gbaOffset = gba.getOffset();
		byteArray = gba;
		byteArray.allocate(gbaOffset, length);
		gbaSize = length;
		byteArray.fill(gbaOffset, gbaSize, fill);
	}
*/
	/**
	 * Construct a new GroupData instance with zero length
	 */
	public GroupData(Group primaryGroup) {
		byteArray = new GroupByteArray(false, 0);
		gbaSize = 0;
		pGroup = primaryGroup;
		
//		registeredGroups.add(primaryGroup);
	}
	/*
	public GroupData(GroupByteArray gba) {
		if(gba == null) {
			gba = new GroupByteArray(false, 0);
		}
		gbaOffset = gba.getOffset();
		byteArray = gba;
		byteArray.allocate(gbaOffset, 0);
		gbaSize = 0;
	}
*/
	/**
	 * Register primary groups and redefines which use this GroupData structure.
	 * @param g
	 */ /*
	protected void registerAdditionalGroups(Group g) {
		registeredGroups.add(g);
	}
	

	protected void evaluateCacheImpact(Group g) {
		for(Group x : registeredGroups) {
			x.evaluateCacheImpact(g);
		}
	}
*/
	
	/**
	 * get the length of the storage.
	 * @return
	 */
	public int length() {
		return gbaSize;
	}

	/**
	 * Extend the length of the data storage to the new value. If the new
	 * length is less than or equal to the current length then do nothing.
	 * @param len The new length
	 * @param fill The fill character
	 */
	public void extendTo(int len, byte fill) {
		if(len <= gbaSize)
			return;
		
		byteArray.extendTo(len);
		byteArray.fill(gbaSize, len-gbaSize, fill);
		gbaSize = len;
	}

	/**
	 * Insert the byte array b at pos. The length of GroupData will be increased
	 * by the length of b. If pos is greater than the existing length, then the
	 * length of this item will be increased to pos plus the length of b
	 * @param b
	 * @param pos
	 */
	public void insert(byte[] b, int pos) {
		if(b == null || b.length == 0)
			return;
		
		byteArray.insert(b, pos);
		gbaSize += b.length;
	}
	
	/**
	 * Replace the segment in the array starting at pos for length len
	 * @param b The replacement bytes to insert into the array
	 * @param pos The start position for replacing
	 * @param len The length of the segment to be replaced
	 */
	public void replace(byte [] b, int pos, int len) {
		
		if(b == null || b.length == 0)
			return;
		
		byteArray.replace(b, b.length, pos, len);
		gbaSize += b.length - len;
	}

	/**
	 * Set the value of this GroupData item, starting at pos, to the value
	 * of the byte array b. If the existing length is too short, it will be
	 * increased to accomodate.
	 * @param b
	 * @param pos
	 */
	public void setSubString(byte[] b, int pos) {
		replace(b, pos, b.length);
	}

	/**
	 * Set the value of this GroupData item, starting at pos, to the value
	 * of the byte array b. If the existing length is too short, it will be
	 * increased to accomodate. If b is too short, the remaining space will
	 * be filled with spaces.
	 * @param b
	 * @param start
	 * @param end
	 */
	private void _setSubString(byte[] b, int start, int end) {
		if(b == null)
			b = new byte[0];
		int len = end - start;
		if(len <= 0)
			return;

//		byteArray.replace(b, gbaOffset + start, b.length);
		byteArray.replace(b, len, start, len);
//		if(b.length < len) {
//			byteArray.fill(gbaOffset + b.length, len - b.length, (byte)' ');
//		}
//		if(gbaSize < end)
//			gbaSize = end;
	}
	
	/**
	 * Copy the byte array into this groupData item. 
	 * @param b The byte array to copy
	 * @param start The start position in GroupData array
	 * @param end The end position in GroupData array
	 */
	public void setSubString(byte[] b, int start, int end) {
		if(b == null)
			b = new byte[0];
		int len = end - start;
		if(len <= 0)
			return;
		
		if(b.length >= len) {
			System.arraycopy(b, 0, byteArray.data, start, len);
		} else {
			System.arraycopy(b, 0, byteArray.data, start, b.length);
			Arrays.fill(byteArray.data, start + b.length, start + len, (byte)' ');
		}
	}

	/**
	 * Copy a portion of the src byte array to the specified offset in this
	 * GroupData item's byte array
	 * @param src
	 * @param srcPos
	 * @param destPos
	 * @param len
	 */
    public void setSubString(byte[] b, int srcPos, int destPos, int len) {
        if(b == null)
            b = new byte[len];

        if(b.length >= (len + srcPos)) {
        	if(len > (byteArray.data.length - destPos))
        		len = byteArray.data.length - destPos;
        	if(len > 0)
        		System.arraycopy(b, srcPos, byteArray.data, destPos, len);
        } else {
            System.arraycopy(b, srcPos, byteArray.data, destPos, b.length - srcPos);
            Arrays.fill(byteArray.data, destPos + b.length - srcPos, destPos + len, (byte)' ');
        }
    }

//	public static void printChangeStats() {
//		System.out.printf("markCHange=%d, hasChanged=%d, hasNotChanged=%d\n", markChanged, hasChanged, hasNotChanged);
//	}

	
	/**
	 * Return a new byte array which is a substring of the existing byte array.
	 * If the start or end are greater than the actual length of the data, then
	 * new bytes will be padded with spaces.
	 * @param start
	 * @param end
	 * @return
	 */
	public byte[] subString(int start, int end) {
		byte [] b = new byte[end - start];
		
		byteArray.fillArray(b, start, end);
		
		return b;
	}

	public void storeComp(int pos, int len, long val) {
//		for(int i = len - 1; i >= 0; i--) {
//			byteArray.data[i + pos] = (byte)(val >> ((len - i - 1) * 8));
//		}
		
		switch(len) {
		case 1:
			byteArray.data[pos] = (byte)(val);
			break;
		case 2:
			byteArray.data[1 + pos] = (byte)(val);
			byteArray.data[pos] = (byte)(val >> 8);
			break;
		case 3:
			byteArray.data[2 + pos] = (byte)(val);
			byteArray.data[1 + pos] = (byte)(val >> 8);
			byteArray.data[pos] = (byte)(val >> 16);
			break;
		case 4:
			byteArray.data[3 + pos] = (byte)(val);
			byteArray.data[2 + pos] = (byte)(val >> 8);
			byteArray.data[1 + pos] = (byte)(val >> 16);
			byteArray.data[pos] = (byte)(val >> 24);
			break;
		case 5:
			byteArray.data[4 + pos] = (byte)(val);
			byteArray.data[3 + pos] = (byte)(val >> 8);
			byteArray.data[2 + pos] = (byte)(val >> 16);
			byteArray.data[1 + pos] = (byte)(val >> 24);
			byteArray.data[pos] = (byte)(val >> 32);
			break;
		case 6:
			byteArray.data[5 + pos] = (byte)(val);
			byteArray.data[4 + pos] = (byte)(val >> 8);
			byteArray.data[3 + pos] = (byte)(val >> 16);
			byteArray.data[2 + pos] = (byte)(val >> 24);
			byteArray.data[1 + pos] = (byte)(val >> 32);
			byteArray.data[pos] = (byte)(val >> 40);
			break;
		case 7:
			byteArray.data[6 + pos] = (byte)(val);
			byteArray.data[5 + pos] = (byte)(val >> 8);
			byteArray.data[4 + pos] = (byte)(val >> 16);
			byteArray.data[3 + pos] = (byte)(val >> 24);
			byteArray.data[2 + pos] = (byte)(val >> 32);
			byteArray.data[1 + pos] = (byte)(val >> 40);
			byteArray.data[pos] = (byte)(val >> 48);
			break;
		default:
			byteArray.data[7 + pos] = (byte)(val);
			byteArray.data[6 + pos] = (byte)(val >> 8);
			byteArray.data[5 + pos] = (byte)(val >> 16);
			byteArray.data[4 + pos] = (byte)(val >> 24);
			byteArray.data[3 + pos] = (byte)(val >> 32);
			byteArray.data[2 + pos] = (byte)(val >> 40);
			byteArray.data[1 + pos] = (byte)(val >> 48);
			byteArray.data[pos] = (byte)(val >> 56);
			break;
		}

	}

	public void storeComp(int pos, int len, int val) {
//		for(int i = len - 1; i >= 0; i--) {
//			byteArray.data[i + pos] = (byte)(val >> ((len - i - 1) * 8));
//		}
		if(len > 4) {
			storeComp(pos, len, (long)val);
			return;
		}

		switch(len) {
		case 1:
			byteArray.data[pos] = (byte)(val);
			break;
		case 2:
			byteArray.data[1 + pos] = (byte)(val);
			byteArray.data[pos] = (byte)(val >> 8);
			break;
		case 3:
			byteArray.data[2 + pos] = (byte)(val);
			byteArray.data[1 + pos] = (byte)(val >> 8);
			byteArray.data[pos] = (byte)(val >> 16);
			break;
		default:
			byteArray.data[3 + pos] = (byte)(val);
			byteArray.data[2 + pos] = (byte)(val >> 8);
			byteArray.data[1 + pos] = (byte)(val >> 16);
			byteArray.data[pos] = (byte)(val >> 24);
			break;
		}
	}

	public long getCompAsLong(boolean unsigned, int pos, int len) {
		
		long val;
		if(!unsigned && (byteArray.data[pos] & 0x80) != 0 && len < 8)
			val = -1L << (len * 8);
		else 
			val = 0;
		
		
//		for(int i=0; i < len; i++) {
//			xval |= (long)(byteArray.data[i+pos] & 0xff) << ((len - i - 1) * 8);
//		}
//		if(true) return xval;
		
		
		switch(len) {
		case 1:
			return val |
					(long)(byteArray.data[pos] & 0xff);
		case 2:
			return val |
					(long)(byteArray.data[pos] & 0xff) << (8) |
					(long)(byteArray.data[1+pos] & 0xff);
		case 3:
			return val |
					(long)(byteArray.data[pos] & 0xff) << (16) |
					(long)(byteArray.data[1+pos] & 0xff) << (8) |
					(long)(byteArray.data[2+pos] & 0xff);
		case 4:
			return val |
					(long)(byteArray.data[pos] & 0xff) << (24) |
					(long)(byteArray.data[1+pos] & 0xff) << (16) |
					(long)(byteArray.data[2+pos] & 0xff) << (8) |
					(long)(byteArray.data[3+pos] & 0xff);
		case 5:
			return val |
					(long)(byteArray.data[pos] & 0xff) << (32) |
					(long)(byteArray.data[1+pos] & 0xff) << (24) |
					(long)(byteArray.data[2+pos] & 0xff) << (16) |
					(long)(byteArray.data[3+pos] & 0xff) << (8) |
					(long)(byteArray.data[4+pos] & 0xff);
		case 6:
			return val |
					(long)(byteArray.data[pos] & 0xff) << (40) |
					(long)(byteArray.data[1+pos] & 0xff) << (32) |
					(long)(byteArray.data[2+pos] & 0xff) << (24) |
					(long)(byteArray.data[3+pos] & 0xff) << (16) |
					(long)(byteArray.data[4+pos] & 0xff) << (8) |
					(long)(byteArray.data[5+pos] & 0xff);
		case 7:
			return val |
					(long)(byteArray.data[pos] & 0xff) << (48) |
					(long)(byteArray.data[1+pos] & 0xff) << (40) |
					(long)(byteArray.data[2+pos] & 0xff) << (32) |
					(long)(byteArray.data[3+pos] & 0xff) << (24) |
					(long)(byteArray.data[4+pos] & 0xff) << (16) |
					(long)(byteArray.data[5+pos] & 0xff) << (8) |
					(long)(byteArray.data[6+pos] & 0xff);
		default:
			return val |
					(long)(byteArray.data[pos] & 0xff) << (56) |
					(long)(byteArray.data[1+pos] & 0xff) << (48) |
					(long)(byteArray.data[2+pos] & 0xff) << (40) |
					(long)(byteArray.data[3+pos] & 0xff) << (32) |
					(long)(byteArray.data[4+pos] & 0xff) << (24) |
					(long)(byteArray.data[5+pos] & 0xff) << (16) |
					(long)(byteArray.data[6+pos] & 0xff) << (8) |
					(long)(byteArray.data[7+pos] & 0xff);
		}

//		return val;
	}
	
	public int getCompAsInt(boolean unsigned, int pos, int len) {
		/*
		int val;
		if(!unsigned && (byteArray.data[pos] & 0x80) != 0 && len < 4)
			val = -1 << (len * 8);
		else 
			val = 0;
		
		for(int i=0; i < len; i++) {
			val |= (byteArray.data[i+pos] & 0xff) << ((len - i - 1) * 8);
		}
		*/
		switch(len) {
		case 1:
			return ((!unsigned && (byteArray.data[pos] & 0x80) != 0) ? -1 << 8 : 0) |
					(byteArray.data[pos] & 0xff); 
		case 2:
			return ((!unsigned && (byteArray.data[pos] & 0x80) != 0) ? -1 << 16 : 0) |
					(byteArray.data[pos] & 0xff) << (8) |
					(byteArray.data[1+pos] & 0xff);
		case 3:
			return ((!unsigned && (byteArray.data[pos] & 0x80) != 0) ? -1 << 24 : 0) |
					(byteArray.data[pos] & 0xff) << (16) |
					(byteArray.data[1+pos] & 0xff) << (8) |
					(byteArray.data[2+pos] & 0xff);
		default:
			return 
					(byteArray.data[pos] & 0xff) << (24) |
					(byteArray.data[1+pos] & 0xff) << (16) |
					(byteArray.data[2+pos] & 0xff) << (8) |
					(byteArray.data[3+pos] & 0xff);
		}
	}

	public double getCompAsDouble(boolean unsigned, int pos, int len, int scale) {
		long v = getCompAsLong(unsigned, pos, len);
		if(scale == 0)
			return (double)v;
		return (double) v / Var.powerFloat[scale];
	}

	public byte[] getBytes() {
		return byteArray.data;
	}

	public void set(byte[] newData) {
		byteArray.set(newData, 0, gbaSize);
	}

	public void clear() {
		for(int i=0;i<(gbaSize);i++)
			byteArray.data[i] = (byte)0;
	}

	/**
	 *  Fill the data byte array starting at start and ending at (to - 1)
	 * 
	 * @param b
	 * @param start
	 * @param to
	 */
	public void fill(byte[] b, int start, int to) {
		if(to > gbaSize)
			to = gbaSize;
		for(int i=start; i<to; i += b.length) {
			for(int j=0; j<b.length && (i+j) < to; j++)
				byteArray.data[i+j] = b[j];
		}
	}

	/**
	 * Duplicate the GroupData object.
	 * @return a new GroupData object
	 */
	public GroupData duplicate(Group primaryGroup) {
		GroupData d = new GroupData(primaryGroup);
		d.byteArray = new GroupByteArray(false, gbaSize);
		d.byteArray.assignedLen = byteArray.assignedLen;
		d.byteArray.preAllocated = byteArray.preAllocated;
		d.gbaSize = gbaSize;
		System.arraycopy(byteArray.data, 0, d.byteArray.data, 0, d.byteArray.data.length);
		return d;
	}

	public void upshift(int start, int end) {
		for(int i = start; i < end; i++) {
			byteArray.data[i] = (byte) Character.toUpperCase(byteArray.data[i]);
		}
	}

	public void downshift(int start, int end) {
		for(int i = start; i < end; i++) {
			byteArray.data[i] = (byte) Character.toLowerCase(byteArray.data[i]);
		}
	}

	/**
	 * Convert the required segment to a string
	 * @param offs
	 * @param length
	 * @param charset
	 * @return
	 */
	public String getString(int offs, int length, String charset) {
		return byteArray.getString(offs, length, charset);
	}

	/**
	 * Return the bytes from offset for length len as a sequence of hex characters
	 * @param offset
	 * @param byteLength
	 * @return
	 */
	public String getHexString(int offset, int len) {
		String x = byteArray.getHexString(offset, len);
		return x;
	}

	/**
	 * Return pointer to raw byte array
	 * @return
	 */
	public byte[] getByteArray() {
		return byteArray.data;
	}

	/**
	 * Convert the byte array, from offset for len bytes, to a long value.
	 * Take overstamps into account and ignore values beyond scale.
	 * @param offset
	 * @param len
	 * @param scale
	 * @return
	 */
	public long convertBytesToLong(int offs, int byteLen, int maxLen, short scale, int dtype) {
		if(byteLen == 0)
			return 0;
		int start = offs;
		int end = start + byteLen - 1;
		byte [] b = byteArray.data;
		long lv = 0;
		boolean sign = false;
		boolean digitOnly = false;
		
		byte c = b[end];
		if(c >= 'p' && c <= 'y') {c = (byte) (c - 'p'); sign = true; }
		else if(c >= 'A' && c <= 'I') {c = (byte) (c - 'A' + 1); }
		else if(c >= 'J' && c <= 'R') {c = (byte) (c - 'J' + 1); sign = true; }
		else if(c >= 'S' && c <= 'Z') {c = (byte) (c - 'S' + 2); }
		else if(c == '}') {c = 0; sign = true; }
		else if(c == '{') {c = 0;}
		else if(c >= '0' && c <= '9') c -= '0';
		else c = 0;

		if(scale > 0) {
			end -= scale;
			byteLen -= scale;
			c = (byte) (b[end] - '0');
		}
		
		if(byteLen > maxLen) {
			if((dtype & Var.LONG) != 0 || (dtype & Var.DOUBLE) != 0) {
				//Truncate from left
				start += byteLen - maxLen;
			} else {
				//Truncate from right
				end -= byteLen - maxLen;
			}
		}
		
		for(int i=start; i<end; i++) {
			if(!digitOnly) {
				if(b[i] == ' ')
					continue;
				if(b[i] == '-') {
					sign = true;
					continue;
				} else if(b[i] == '+') {
					sign = false;
					continue;
				} else {
					digitOnly = true;
				}
			}
			int digit = (int)b[i] - (int)'0';
			if(digit < 0 || digit > 9) {
				if(b[i] == ' ') {
					digit = 0;
				} else if(b[i] == '.') {
					if(sign)
						return -lv;
					return lv;
				} else {
					throw new NumberFormatException();
				}
			}
			lv *= 10;
			lv += digit;
		}
		lv *= 10;
		lv += c;
		if(sign)
			return -lv;
		return lv;
	}
	
	public double convertBytesToDouble(int offs, int byteLen, int maxLen, short scale, int dtype) {
		int start = offs;
		int end = start + byteLen - 1;
		byte [] b = byteArray.data;
		double lv = 0;
		boolean sign = false;
		boolean digitOnly = false;
		
		byte c = b[end];
		if(c >= 'p' && c <= 'y') {c = (byte) (c - 'p'); sign = true; }
		else if(c >= 'A' && c <= 'I') {c = (byte) (c - 'A' + 1); }
		else if(c >= 'J' && c <= 'R') {c = (byte) (c - 'J' + 1); sign = true; }
		else if(c >= 'S' && c <= 'Z') {c = (byte) (c - 'S' + 2); }
		else if(c == '}') {c = 0; sign = true; }
		else if(c == '{') {c = 0;}
		else if(c == ' ') {c = 0;}
		else c -= '0';
		
		if(byteLen > maxLen) {
			if((dtype & Var.LONG) != 0 || (dtype & Var.DOUBLE) != 0) {
				//Truncate from left
				start += byteLen - maxLen;
			} else {
				//Truncate from right
				end -= byteLen - maxLen;
			}
		}
		
		boolean extraDigit = true;
		int scaling = 0;
		for(int i=start; i<end; i++) {
			if(!digitOnly) {
				if(b[i] == ' ')
					continue;
				if(b[i] == '-') {
					sign = true;
					continue;
				} else if(b[i] == '+') {
					sign = false;
					continue;
				} else {
					digitOnly = true;
				}
			}
			int digit = (int)b[i] - (int)'0';
			if(b[i] == ' ') digit = 0;
			if(digit < 0 || digit > 9) {
				if(b[i] == ' ') {
					extraDigit = false;
					break;
				} else if(b[i] == '.') {
					scaling++;
					if(scaling > scale) {
						extraDigit = false;
						break;
					}
					continue;
				}
				return 0.0;
			}
			if(scaling > 0) {
				scaling++;
			}
			lv *= 10;
			lv += digit;
		}
		if(extraDigit) {
			lv *= 10;
			lv += c;
		}
		if(scaling > 0)
			scale = (short) scaling;
		double dv;
		if(scale > 0) {
			dv = (double)lv / Math.pow(10, scale);
		} else {
			dv = (double)lv;
		}
		if(sign)
			return -dv;
		return dv;
	}

	protected void checkPrimaryGroupLength() {
		pGroup.checkByteLength();
//		registeredGroups.first().checkByteLength();
	}
}

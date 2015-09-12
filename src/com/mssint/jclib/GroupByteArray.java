package com.mssint.jclib;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * @author Peter Colman (pete@mssint.com)
 * 
 * Holds a large byte array which accounts for all GroupData space
 * 
 */
public class GroupByteArray {
	protected byte [] data;
	protected int assignedLen;
	protected boolean preAllocated;
	private boolean snapshotRegistered;
	private boolean savedWithCompression;
	public static int bsize = 0;

	public GroupByteArray(boolean warn, int size) {
		data = new byte[size];
		bsize += size;
		assignedLen = size;
		preAllocated = warn;
	}

	public GroupByteArray(int size) {
		data = new byte[size];
		bsize += size;
		assignedLen = size;
		preAllocated = true;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(int i=0;i < data.length;i++) {
			if(i > 0) sb.append('.');
			sb.append(String.format("%02X", data[i]&0xff));
		}
		return sb.toString();
	}


	/**
	 * Fill a segment of the byte array with the fill character.
	 * @param gbaOffset The start position in the array
	 * @param gbaSize The number of bytes to fill
	 * @param fill The fill character
	 */
	public void fill(int gbaOffset, int len, byte fill) {
		Arrays.fill(data, gbaOffset, gbaOffset + len, fill);
	}

	public void fill(byte fill) {
		Arrays.fill(data, fill);
	}
	/**
	 * Get the offset at which my data begins.
	 * @return
	 */
	public int getOffset() {
		return assignedLen;
	}

	/**
	 * Allocate length bytes for use
	 * @param length Number of bytes to allocate
	 */
	public int allocate(int length) {
		int newLen = assignedLen + length;
		if(data.length < newLen) {
			resizeArray(newLen);
		}
		if(assignedLen < newLen)
			assignedLen = newLen;
		return length;
	}
	
	protected int extendTo(int newlen) {
		resizeArray(newlen);
		return data.length;
	}
	
	private void resizeArray(int newSize) {
		if(newSize <= data.length)
			return;
		if(preAllocated) {
			System.out.println("WARNING: Pre-allocated GroupByteArray resized from "+data.length+" to "+newSize);
		}
		byte [] newData = new byte[newSize];
		bsize += newSize - data.length;

		if(data.length > 0) {
			System.arraycopy(data, 0, newData, 0, data.length);
		}
		data = newData;
	}

	/** 
	 * Insert these bytes at the specified location. Values from pos to the current length are moved.
	 * @param b
	 * @param pos
	 */
	public void insert(byte[] b, int pos) {
		if(b == null || b.length == 0)
			return;
		
		if(pos > assignedLen) {
			assignedLen = pos;
		}
		
		if((b.length + assignedLen) > data.length)
			resizeArray(b.length + assignedLen);
		
		if(assignedLen > pos) {
			System.arraycopy(data, pos, data, pos + b.length, assignedLen - pos);
		}
		System.arraycopy(b, 0, data, pos, b.length);
		assignedLen += b.length;
	}

	/**
	 * Replace the bytes in the array beginning at pos for len bytes, with the array b. 
	 * If b is longer than len, then additional space is created,, with bytes being
	 * moved to the right to accommodate. If b is shorter than len, then space is compacted.
	 * 
	 * @param b The bytes to replace
	 * @param pos
	 * @param len
	 */
	public void replace(byte[] b, int newLen, int pos, int oldLen) {
		if(b == null)
			return;
		
		if(pos > assignedLen) {
			assignedLen = pos;
			resizeArray(assignedLen);
		}
		if(newLen > oldLen) {
			int size = assignedLen + newLen - oldLen;
			resizeArray(size);
			if((pos + oldLen) < assignedLen) {
				System.arraycopy(data, pos + oldLen, data, pos + newLen, assignedLen - pos - oldLen);
			}
			assignedLen = size;
		} else if(newLen < oldLen) {
			if((pos + newLen) < assignedLen) {
				System.arraycopy(data, pos + oldLen, data, pos + newLen, assignedLen - pos - oldLen);
			}
			assignedLen -= oldLen - newLen;
		}
		
		if(b.length >= newLen) {
			System.arraycopy(b, 0, data, pos, newLen);
		} else {
			System.arraycopy(b, 0, data, pos, b.length);
			Arrays.fill(data, pos + b.length, pos + newLen, (byte)' ');
		}
	}

	/**
	 * Replace a segment of the array with a segment of the source array.
	 * @param offset Offset in dest array
	 * @param src source array
	 * @param srcPos position in source array
	 * @param destPos position in destination array
	 * @param len bytes to copy
	 */
	public void replaceSubstr(int offset, byte[] src, int srcPos,
			int destPos, int len) {
		if(assignedLen < (offset + destPos + len)) {
			assignedLen = offset + destPos + len;
			if(data.length < assignedLen) {
				resizeArray(assignedLen);
			}
		}
		destPos += offset;
		if(src.length < (srcPos + len)) {
			System.arraycopy(src, srcPos, data, destPos, src.length - srcPos);
			Arrays.fill(data, destPos + src.length - srcPos, destPos + len, (byte)' ');
		} else {
			System.arraycopy(src, srcPos, data, destPos, len);
		}
	}

	/**
	 * Fill the array b with data from start to end. If the array is too long for the data, fill it
	 * with spaces.
	 */
	public void fillArray(byte[] b, int start, int end) {
		if(start >= assignedLen) {
			Arrays.fill(b, (byte)' ');
		} else if(end > assignedLen) {
			System.arraycopy(data, start, b, 0, assignedLen - start);
			Arrays.fill(b, assignedLen - start, b.length, (byte)' ');
		} else {
			System.arraycopy(data, start, b, 0, b.length);
		}
	}

	public byte[] getBytes(int offset, int len) {
		byte [] b = new byte[len];
		System.arraycopy(data, offset, b, 0, len);
		return b;
	}

	/**
	 * Set this segment of the array to the new value. Lengthen if necessary.
	 * @param b
	 * @param offset
	 * @param len
	 */
	public void set(byte[] b, int offset, int len) {
		if(b == null || b.length == 0)
			return;
		if((offset + b.length) > assignedLen) {
			assignedLen = offset + b.length;
			resizeArray(assignedLen);
		}
		if(b.length < len) {
			System.arraycopy(b, 0, data, offset, b.length);
			Arrays.fill(data, offset + b.length, offset + len, (byte)' ');
		} else {
			System.arraycopy(b, 0, data, offset, len);
		}
	}

	/**
	 * Generate a new string
	 * @param gbaOffset
	 * @param length
	 * @param charset 
	 * @return
	 */
	public String getString(int offset, int length, String charset) {
		if((offset + length) > assignedLen) {
			assignedLen = offset + length;
			resizeArray(assignedLen);
		}
		String s;
		try {
			s = new String(data, offset, length, charset);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			s = new String(data, offset, length);
		}
		return s;
	}
	
	final static protected char [] hexCharacters = "0123456789ABCDEF".toCharArray();
	/**
	 * Return the bytes from offset for length len as a sequence of hex characters
	 * @param offset
	 * @param byteLength
	 * @return
	 */
	public String getHexString(int offset, int len) {
/*		if((offset+len) > assignedLen) {
			assignedLen = offset + len;
			resizeArray(assignedLen);
		}*/
		StringBuilder sb = new StringBuilder(len * 2);
		len += offset;
		for(int i=offset;i<len;i++) {
			sb.append(hexCharacters[((data[i] >> 4) & 0x0f)]);
			sb.append(hexCharacters[(data[i] & 0x0f)]);
		}
		
		return sb.toString();
	}

	/**
	 * Return storage size
	 * @return
	 */
	public int size() {
		return data.length;
	}

	/**
	 * Indicates whether this instance has already been registered for snapshots.
	 * @return
	 */
	public boolean isRegistered() {
		return snapshotRegistered;
	}
	
	public void register() {
		snapshotRegistered = true;
	}
	
	/**
	 * Create a snapshot of the value of the byte array. Uses simple RLE compression to 
	 * reduce space without impacting performance.
	 * @return
	 */
	public byte[] createSnapshot() {
		int len = data.length;
		if(len == 0)
			return null;
		
		//Basic copy-store
		
		byte [] b = new byte[len];
		System.arraycopy(data, 0, b, 0, len);
		savedWithCompression = false;
		return b;
		
		
		
		
		//TODO: Implement a simple compression algorithm.
		/*
		byte [] idx = new byte[len];
		byte [] d = new byte[len];
		int ip = 0; //Pointer into index array
		int dp = 0; //Pointer into data array

		boolean countMode = true;
		int count = 1;
		int runlength = 1;
		d[0] = data[0];
		for(int p=1; p<data.length; p++) {
			if(data[p] == d[dp]) {
				count++;
				if(runlength > 1 && count > 2) {
					if(runlength == 2) {
						idx[ip++] = 1;
					} else {
						d[dp++] = 0;
						idx[ip++] = 0;
						d[dp] = data[p];
						runlength = 1;
					}
				}
			} else {
				if(count > 2) {
					idx[ip++] = (byte)count;
					d[++dp] = data[p];
					count = 1;
					runlength = 1;
				} else if(runlength > 0) {
					if(count > 1) {
						d[dp+1] = d[dp++];
					}
					d[++dp] = data[p];
					runlength++;
					count = 1;
				}
			}
			if(dp + ip >= data.length) {
				//No point using compression, just return the byte array
				byte [] b = new byte[len];
				System.arraycopy(data, 0, b, 0, len);
				savedWithCompression = false;
				return b;
			}
		}
		if(count > 1) {
			idx[ip++] = (byte) count;
		} else if(runlength > 1) {
			idx[ip++] = 0;
		}
		
		byte [] b = new byte[4 + ip + dp];
		int offset = 0;
		
		//Store the length of the index segment
		storeInteger(b, offset, ip);
		offset += 4;
		
		//Copy in the index
		System.arraycopy(idx, 0, b, offset, ip);
		offset += ip;
		
		//Copy the data.
		System.arraycopy(d, 0, b, offset, dp);
		
		savedWithCompression = true;
		
		return b;
		*/
	}

	public void loadSnapshot(byte[] snapshot) {
		if(!savedWithCompression) {
			if(snapshot != null)
				System.arraycopy(snapshot, 0, data, 0, snapshot.length);
			return;
		}
		/*
		int dpPos = getInteger(snapshot, 0) + 4;
		int ip = 4;
		int dp = dpPos;
		int p = 0;
		
		while(ip < dpPos && dp < snapshot.length) {
			if(snapshot[ip] == 0) {
				for(; dp < snapshot.length && snapshot[dp] != 0; dp++) {
					data[p++] = snapshot[dp];
				}
				dp++;
			} else {
				for(int i=snapshot[ip]; i > 0; i--) {
					data[p++] = snapshot[dp];
				}
				dp++;
			}
		}
		*/
	}
	
/*
 
 	private void storeInteger(byte [] data, int pos, int val) {
		for(int i = 4 - 1; i >= 0; i--) {
			data[i + pos] = (byte)(val >> ((4 - i - 1) * 8));
		}
	}

	private int getInteger(byte [] data, int pos) {
		int val = 0;

		for(int i=0; i < 4; i++) {
			val |= (data[i+pos] & 0xff) << ((4 - i - 1) * 8);
		}
		return val;
	}
 
	public void test(String testString) {
		byte [] orig = testString.getBytes();
		data = new byte[orig.length];
		System.arraycopy(orig, 0, data, 0, data.length);
		System.out.println(prettyPrint(data));
		byte [] x = createSnapshot();
		int p = getInteger(x, 0);
		byte [] idx = new byte[p];
		System.arraycopy(x, 4, idx, 0, p);
		byte [] dat = new byte[x.length - (p+4)];
		System.arraycopy(x, 4+p, dat, 0, dat.length);
		System.out.println("indexlen="+p+" ["+decimalPrint(idx)+"]");
		System.out.println(prettyPrint(dat));
		loadSnapshot(x);
		if(Arrays.equals(orig, data)) {
			System.out.println("SUCCESS: Results match");
		} else {
			System.out.println("FAIL: Results match");
		}
	}
	
	public String prettyPrint(byte [] b) {
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<b.length; i++) {
			char c = (char)b[i];
			if(Character.isLetterOrDigit(c))
				sb.append(c);
			else if(Character.isISOControl(c)) {
				sb.append('^');
				if(b[i] == 0)
					sb.append("@");
				else
					sb.append(c);
			} else if(b[i] == ' ') {
				sb.append(".");
			} else {
				sb.append(String.format("\\x%02X", b[i]));
			}
		}
		
		return sb.toString();
	}
	
	public String decimalPrint(byte [] b) {
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<b.length;i++) {
			if(i > 0) sb.append(".");
			sb.append((int)b[i]);
		}
		return sb.toString();
	}
	
	public void runTests() {
		fill((byte)-1);
		String s = "    0000000000ABCD        CDDCCXYY";
		test(s);
	}
	
	public static void main(String [] args) {
		GroupByteArray gba = new GroupByteArray(100);
		gba.runTests();
		
	}
*/
}

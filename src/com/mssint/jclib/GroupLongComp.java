package com.mssint.jclib;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

@SuppressWarnings("serial")
public class GroupLongComp extends Group {

	private long cacheVal;
	
	@SuppressWarnings("unused")
	private GroupLongComp() {
	}
	
	protected GroupLongComp(Group g) {
		attr = g.attr;
		displayLength =  g.displayLength;
		byteLength = g.byteLength;
		scale = 0;
		parent = g.parent;
		groupOffset = g.groupOffset;
		indexLevel = g.indexLevel;
		members = g.members;
		value = g.value;
		myCachedOffset = g.myCachedOffset;
		exp = g.exp;
		indexLevel = g.indexLevel;
		occurs = g.occurs;
		updateCache();
	}
	
	private void updateCache() {
		cacheVal = ((GroupData)value).getCompAsLong((attr & UNSIGNED) != 0, myCachedOffset, byteLength);
	}

	@Override
	public Group set(byte [] src) {
		super.set(src);
		updateCache();
		return this;
	}

	@Override
	public Group set(byte [] src, int from, int len) {
		super.set(src, from, len);
		updateCache();
		return this;
	}
	
	@Override
	public Group set(double val) {
		if((attr & (UNSIGNED)) != 0)
			val = Math.abs(val);
	    val = (int) f_chop(val, displayLength, 0);
	    
		BigDecimal bd = new BigDecimal(val, mathContext);
		long lv = bd.longValue();
		if(exp == 0) exp = (byte) displayLength;
		long v = lv % powerLong[exp];
		if(v != lv) {
			attr |= OVERFLOW;
		}
//		if(v != cacheVal) {
			((GroupData)value).storeComp(myCachedOffset, byteLength, v);
			cacheVal = v;
//		}
		return this;
	}

	@Override
	public Group set(Group g) {
//		if(true) return super.set(g);
		long iv;
		attr &= ~(OVERFLOW);

		iv = g.getLong();
		if((attr & (UNSIGNED)) != 0 && iv < 0) {
			iv = -iv;
		}
		long v = iv % powerLong[displayLength];
		if(v != iv) {
			attr |= OVERFLOW;
		}
//		if(v != cacheVal) {
			((GroupData)value).storeComp(myCachedOffset, byteLength, v);
			cacheVal = v;
//		}
		return this;
	}
	
	@Override
	public Group set(int val) {
//		if(true) return super.set(val);
		attr &= ~(OVERFLOW);
		if((attr & (UNSIGNED)) != 0 && val < 0) {
			val = -val;
		}
//		if((long)val != cacheVal) {
			((GroupData)value).storeComp(myCachedOffset, byteLength, (long)val);
			cacheVal = val;
//		}
		((GroupData)value).storeComp(myCachedOffset, byteLength, (long)val);
		return this;
	}
	
	@Override
	public Group set(long val) {
//		if(true) return super.set(val);
		attr &= ~(OVERFLOW);
		if((attr & (UNSIGNED)) != 0 && val < 0) {
			val = -val;
		}
//		if(val != cacheVal) {
			((GroupData)value).storeComp(myCachedOffset, byteLength, val);
			cacheVal = val;
//		}
		return this;
	}
	
	@Override
	public Group set(String s) {
		super.set(s);
		return this;
	}
	
	@Override
	public Group set(Var v) {
		long val = v.getLong();
//		if(val != cacheVal) {
			((GroupData)value).storeComp(myCachedOffset, byteLength, val);
			cacheVal = val;
//		}
		return this;
	}
	
	@Override
	public Group increment(long amount) {
		cacheVal = getLong();
		cacheVal += (int)amount;
		((GroupData)value).storeComp(myCachedOffset, byteLength, cacheVal);
		return this;
	}

	public Group increment(int amount) {
		cacheVal = getLong();
		cacheVal += amount;
		((GroupData)value).storeComp(myCachedOffset, byteLength, cacheVal);
		return this;
	}
	

	
	@Override 
	public int getInt() {
		return (int)getLong();
	}

	@Override 
	public long getLong() {
		cacheVal = ((GroupData)value).getCompAsLong((attr & UNSIGNED) != 0, myCachedOffset, byteLength);
		return cacheVal;
	}
	
	public double getDouble() {
		return (double)getLong();
	}

	public boolean eq(int l) {
		return l == getLong();
	}
	
	public boolean eq(long l) {
		return l == getLong();
	}

	public boolean eq(Var g) {
		if((g.attr & NUMBER) != 0) {
			if(g.scale == 0) {
				return getLong() == g.getLong();
			} else {
				return getLong() == g.getDouble();
			}
		}
		
		return g.compareTo(Long.toString(getLong())) == 0;
	}

}

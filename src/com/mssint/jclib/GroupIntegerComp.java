package com.mssint.jclib;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.util.Arrays;

@SuppressWarnings("serial")
public class GroupIntegerComp extends Group {

	@SuppressWarnings("unused")
	private GroupIntegerComp() {
	}

	protected GroupIntegerComp(Group g) {
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
	}

	@Override
	public String toString() {
		return Integer.toString(getInt());
	}

	@Override
	public Group set(byte [] src) {
		super.set(src);
		return this;
	}

	@Override
	public Group set(byte [] src, int from, int len) {
		super.set(src, from, len);
		return this;
	}

//	private static int integerStored, integerNotStored, integerCached, integerNotCached;

	public static void printStats() {
//		System.out.printf("IntStore: %d/%d  IntRead: %d/%d\n", integerNotStored,integerStored+integerNotStored,integerCached,integerCached+integerNotCached);
	}

	@Override
	public Group set(double val) {
		if((attr & (UNSIGNED)) != 0)
			val = Math.abs(val);
		val = (int) f_chop(val, displayLength, 0);

		BigDecimal bd = new BigDecimal(val, mathContext);
		int lv = bd.intValue();
		if(exp == 0) exp = (byte) displayLength;
		int v = lv % powerInt[exp];
		if(v != lv) {
			attr |= OVERFLOW;
		}
		((GroupData)value).storeComp(myCachedOffset, byteLength, v);
//		integerStored++;
		return this;
	}

	@Override
	public Group set(Group g) {
		//		if(true) return super.set(g);
		int iv;
		attr &= ~(OVERFLOW);

		iv = g.getInt();
		if((attr & (UNSIGNED)) != 0 && iv < 0) {
			iv = -iv;
		}
		int v = iv % powerInt[displayLength];
		if(v != iv) {
			attr |= OVERFLOW;
		}
		((GroupData)value).storeComp(myCachedOffset, byteLength, v);
//		integerStored++;
//		integerNotStored++;
		return this;
	}

	@Override
	public Group set(int val) {
		//		if(true) return super.set(val);
		attr &= ~(OVERFLOW);
		if((attr & (UNSIGNED)) != 0 && val < 0) {
			val = -val;
		}
		((GroupData)value).storeComp(myCachedOffset, byteLength, val);
//		integerStored++;
		return this;
	}

	@Override
	public Group set(long val) {
		//		if(true) return super.set(val);
		attr &= ~(OVERFLOW);
		if((attr & (UNSIGNED)) != 0 && val < 0) {
			val = -val;
		}
		((GroupData)value).storeComp(myCachedOffset, byteLength, (int)val);
//		integerStored++;
		return this;
	}

	@Override
	public Group set(String s) {
		super.set(s);
		return this;
	}

	@Override
	public Group set(Var v) {
		int val = v.getInt();
		((GroupData)value).storeComp(myCachedOffset, byteLength, val);
//		integerStored++;
		return this;
	}

	@Override
	public Group increment(long amount) {
		((GroupData)value).storeComp(myCachedOffset, byteLength, getInt() + (int)amount);
		return this;
	}

	public Group increment(int amount) {
		((GroupData)value).storeComp(myCachedOffset, byteLength, getInt() + amount);
		return this;
	}

	@Override 
	public int getInt() {
//		integerCached++;
		return ((GroupData)value).getCompAsInt((attr & UNSIGNED) != 0, myCachedOffset, byteLength);
	}

	@Override 
	public long getLong() {
		return getInt();
	}

	@Override
	public double getDouble() {
		return (double)getInt();
	}

	@Override
	public boolean eq(int l) {
		return getInt() == l;
	}

	@Override
	public boolean eq(long l) {
		return getInt() == l;
	}

	@Override
	public boolean eq(Var g) {
		if((g.attr & NUMBER) != 0) {
			if(g.scale == 0) {
				if(g.displayLength < 10) {
					return getInt() == g.getInt();
				} else {
					return getInt() == g.getLong();
				}
			} else {
				return getInt() == g.getDouble();
			}
		}

		return g.compareTo(Integer.toString(getInt())) == 0;
	}

	public Group compute(double v) {
		super.compute(v);
		return this;
	}

	public Group compute(long v) {
		super.compute(v);
		return this;
	}


}

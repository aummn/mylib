package com.mssint.jclib;

import java.util.Comparator;

/**
 * Used to compare Group hascodes
 * @author Peter Colman (pete@mssint.com)
 *
 */
public class GroupComparator implements Comparator<Group> {

	@Override
	public int compare(Group g1, Group g2) {
		return g1.hashCode() - g2.hashCode();
	}
	

}

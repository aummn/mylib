package com.mssint.jclib;

public interface ReportExtension {

	/**
	 * Whenever a new print file is created by the Report class, load() will
	 * be called, passing the Report object itself as a parameter.
	 * @param rep
	 */
	public void load(Report rep);
	
	/**
	 * The release() method is called by the Report object when a report is 
	 * released for printing. Report will assume that the extension will take
	 * care of managing the print, so it will not perform any further functions.
	 */
	public void release();
}

package com.mssint.jclib;

/**
 * @author Peter Colman (pete@mssint.com)
 *
 * These are the names of environment variables passed to batch programs 
 * when called by the online.
 */
public enum Batch {
	BC_SESSIONID,
	BC_WMUSER,
	BC_STATIONNAME,
	BC_LOOKUPSTATION,
	BC_URL,
	BC_REPORTNAME,
	BC_PID,
	BC_REMOTEUSER,
	FUNCTION,
	DISPLAYTEXT,
	SESSIONID,
	PROMPT,
	F_BOJ,
	F_EOJ,
	F_DISPLAY,
	F_ACCEPT,
	Q_MESSAGECOUNT,
	Q_GET_READ_MESSAGES,
	Q_GET_NEW_MESSAGES,
	Q_DELETE_MESSAGES,
	Q_ACCEPT,
	Q_ACCEPT_STRING,
	Q_KILLREP,
	Q_WHO, //List of logged in users
	Q_REP  //List of running reports
}

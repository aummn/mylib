package com.mssint.jclib;


import java.rmi.RemoteException;

import javax.ejb.EJBObject;

/** 
 * This is the <i>remote</i> interface for online applications. 
 * <p>last rebuilt %DATE; </p>
 * @version %BUILD;
 */ 
public interface OnlineWrapper extends EJBObject { 
	ScreenState startup(ScreenState initialState) throws Exception, RemoteException; 
	ScreenState commit(ScreenState commitedState) throws Exception, RemoteException; 
} 

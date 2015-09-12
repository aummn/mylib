package com.mssint.jclib;


import java.rmi.RemoteException;

import javax.ejb.Remote;



/** 
 * This is the <i>remote</i> interface for online applications. 
 * <p>last rebuilt %DATE; </p>
 * @version %BUILD;
 */ 
@Remote
public interface LincRemote { 
	public abstract ScreenState startup(ScreenState initialState) throws Exception, RemoteException; 
	public abstract ScreenState commit(ScreenState commitedState) throws Exception, RemoteException; 
} 

package com.mssint.jclib;

import java.rmi.RemoteException; 

import javax.ejb.CreateException; 
import javax.ejb.EJBHome; 

/**
 * <p>last rebuilt %DATE; </p>
 * @version %BUILD;
*/
public interface OnlineWrapperHome extends EJBHome { 
	OnlineWrapper create() throws RemoteException, CreateException, Exception; 
} 

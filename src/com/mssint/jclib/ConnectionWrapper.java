package com.mssint.jclib;

import java.sql.Connection;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is to be used with startup/commit methods for EJBs.<br>
 * It allows to use the same instance of Connection object for all cursors 
 * called within each of these methods. If no cursor is being used, the 
 * connection will not be unnecessarily instantiated.
 * 
 * It is effect a wrapper/delegate of the DBConnectionFactory. 
 * 
 * A database name cannot be provided {@link DBConnectionFactory}
 * 
 * @author Peter Martyniak
 */

public class ConnectionWrapper {
	
	private static final Logger log = LoggerFactory.getLogger(ConnectionWrapper.class);
	
	// connection instance
	private Connection connection;
	
	/**
	 * Provides a Connection to the caller.
	 * 
	 * @return java.sql.Connection from the DBConnectionFactory i.e. from a JNDI DataSource or straight jdbc connection.
	 * @throes SQLException 
	 */
	public Connection getConnection() throws SQLException  {
		// if connection has not yet been instantiated, do it 
		if(connection == null)
			connection = DBConnectionFactory.getConnection();
		// connection should not be closed explicitly - warn for development purposes
		if(connection.isClosed())  {
			log.warn("Connection should not be closed explicitly.");
			connection = DBConnectionFactory.getConnection();
		}
		// return the connection object
		return connection;
	}
	
	/**
	 * Allows for the managed disposal/closing of a Connection.
	 * 
	 * @throws SQLException 
	 */
	public void releaseConnection() throws SQLException  {
		// if connection is null - it's ok; was unnecessary
		if(connection == null)
			return;
		// connection should not be closed explicitly - warn for development purposes
		if(connection.isClosed())  {
			log.warn("Connection should not be closed explicitly.");
			connection = DBConnectionFactory.getConnection();
		}
		// close connection
		connection.close();
	}
	
}

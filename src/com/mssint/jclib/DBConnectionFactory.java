package com.mssint.jclib;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;

//import java.util.List;

//import javax.naming.Binding;
//import javax.naming.Context;
import javax.naming.InitialContext;
//import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages database connectivity for JLinc components.
 * Several options for setting the parameters and estabilshing a Connection as available: -
 * 
 * <ul>
 * <li>
 * OPTION 1 
 * In the first instance if the caller has used the {@see #setSuggestedDataSource}
 * the system will attempt to provide a Connection via a lookup into JNDI and use the 
 * {@link javax.sql.DataSource} to establish a connection using the value as set in the
 * setSuggestedDataSource call but only if the provided DataSource uri is non null and a null dbname is used, 
 * which is most easily done by calling the parameterless overloaded method getConnection() or providing null to the 
 * other overloaded getConnection(null) method;  
 *   
 * Several attempts at normalising the datasource name are made in order to connect if the datasource 
 * uri is malformed/incomplete.
 * </li>
 * <li>
 * OPTION 2
 * A Connection will attempt to be established using the JNDI Context using the property key/name 'jlinc.jdbc.datasource' entry.
 * If a value is found it will follow the same mechanism as above i.e. get a datasource object looked up by this name in JNDI 
 * and establish a Connection.
 * Note: -
 * Let's say we have a named database foo the property key/name looked up for obtaining the JNDI Context and the resultant Connection 
 * will be of the form jlinc.jdbc.foo.datasource
 * If the named database is null or the empty string then the property key/name used will be 'jlinc.jdbc.datasource'    
 * </li>
 * <li>
 * OPTION 3
 * If none of the above conditions are met the system will attempt to establish a simple jdbc connection using: -
 * 'jlinc.jdbc.driver',
 * 'jlinc.jdbc.url', and for most cases 'jlinc.jdbc.user' and
 * 'jlinc.jdbc.password'.
 * </li>
 * </ul>
 * <br>
 * 
 * @author Peter Martyniak
 * @author Peter Colman
 */

public class DBConnectionFactory {
	private static final Logger log = LoggerFactory.getLogger(DBConnectionFactory.class);

	// possible states
	private static final int STATE_NOT_INITIALIZED = 0;

	private static final int STATE_INITIALIZED_AND_READY = 1;

	private static final int STATE_INITIALIZED_AND_ERROR = 2;

	// singleton instance of this class
	private static DBConnectionFactory factory;

	// state of the factory
	private static int factoryState = STATE_NOT_INITIALIZED;

	// suggested datasource to use
	private static String suggestedDataSourceName;

	// flag showing which connection method to use
	private boolean isDataSourceUsed;

	// datasource
	private String dataSourceName;
	private DataSource dataSource;

	// connection parameters
	private String jdbcUrl;

	private String userName;

	private String password;

	/**
	 * Parameterless static method to be used for getting a Connection instance.
	 * In effect the database name is null, please refer to details in the class 
	 * definition above describing the resultant behaviour.
	 *  
	 * @return java.SQL.Connection
	 * @throws SQLException connection failed to be established.
	 */
	public static Connection getConnection() throws SQLException {
		// if it's the first time - initialize the factory
		if(factoryState != STATE_INITIALIZED_AND_READY)
			factory = new DBConnectionFactory(null);
		// if there are no entries in properties file about DB connectivity,
		// or creating Connection is impossible for other reasons, throw
		// exception
		if (factoryState == STATE_INITIALIZED_AND_ERROR)
			throw new SQLException(
					"No database connection could be established. "
					+ "Check error log for details.");
		// otherwise - return connection
		if(suggestedDataSourceName != null)
			factory.makeSureItIsUsed(suggestedDataSourceName);
		Connection conn = factory.getConnectionInstance();
        conn.setAutoCommit(false);
        return conn;
	}
	
	/**
	 * Get a DB Connection using a database name. 
	 * Any non-null database name provided will cause the usage of a runtime supplied DataSource 
	 * {@see setSuggestedDataSource} to not be used. 
	 * 
	 * @param dbName named instance of the database to be used with OPTION 2
	 * 
	 */
	public static Connection getConnection(String dbname) throws SQLException {
		factory = new DBConnectionFactory(dbname);
		return factory.getConnectionInstance();
		
	}

	/**
	 * Private constructor
	 * @throws SQLException 
	 */
	private DBConnectionFactory(String dbname) throws SQLException {
		if(log.isDebugEnabled())
			log.debug("initializing connection factory.");
		if (suggestedDataSourceName != null && dbname == null)  {
			if(log.isDebugEnabled())
				log.debug("using datasource recommended by webmanager...");
			useDataSource(suggestedDataSourceName);
		} else {
			// first - try to find 'jlinc.jdbc.datasource' property
			String key;
			if(dbname == null) key = "jclib.jdbc.";
			else key = "jclib.jdbc." + dbname + ".";
			if(log.isDebugEnabled())
				log.debug("Looking up '"+key+"*' in config file "+Config.PROPERTIES_FILE_NAME);
			if(Config.getProperties().containsKey(key + "datasource")
					&& !Config.getProperty(key + "datasource")
							.trim().equals(""))
				// if found, datasource will be used
				useDataSource(Config.getProperty(key + "datasource"));
			else {
				// second - try to find entries for straight database connection
				if (Config.getProperties().containsKey(key + "driver")
						&& Config.getProperties().containsKey(key + "url")) {
					// driver will be needed just for one operation...
					String driver = Config.getProperty(key + "driver");
					// ... while other parameters will be used to connect
					jdbcUrl = Config.getProperty(key + "url");
					userName = Config.getProperty(key + "user");
					password = Config.getProperty(key + "password");
					// use straight connection
					useStraightConnection(driver.trim());
				} else {
					factoryState = STATE_INITIALIZED_AND_ERROR;
					throw new SQLException("Cannot locate jdbc configuration entry with key="+key);
				}
			}
		}
	}
	
	
	/**
	 * Attempt to get a DataContext from the JNDI service.
	 */
	private static DataSource lookupDataSource(InitialContext ic, String dsName) {
		try {
			return (DataSource)ic.lookup(dsName);
		} catch (Exception ex) {
			return null;
		}
	}

	/**
	 * Looks up dataSource in JNDI, intially the attempt is made on the provided data and on failure
	 * further attempts to normalise the dataSourceName in various different ways and re-attempt.
	 * 
	 * @param dataSource
	 */
	private void useDataSource(String dataSourceName) {
		if(log.isDebugEnabled())
			log.debug("using data source " + dataSourceName);
		this.dataSourceName = dataSourceName; 

		InitialContext ic;
		try {
			ic = new InitialContext();
		} catch(NamingException e) {
			log.error("Cannot locate Initial Context.");
			factoryState = STATE_INITIALIZED_AND_ERROR;
			return;
		}

		//First, try the string "as given"
		dataSource = lookupDataSource(ic, dataSourceName);
		if(dataSource != null) {
			factoryState = STATE_INITIALIZED_AND_READY;
			isDataSourceUsed = true;
			return;
	    }
		//Normalise the name
		if(dataSourceName.startsWith("java:"))
			dataSourceName = dataSourceName.substring(5);
		if(dataSourceName.startsWith("/"))
			dataSourceName = dataSourceName.substring(1);
		if(dataSourceName.startsWith("comp/env/"))
			dataSourceName = dataSourceName.substring(9);

		// Try the various combinations.
		dataSource = lookupDataSource(ic, dataSourceName);
		if(dataSource == null) 
			dataSource = lookupDataSource(ic, "java:" + dataSourceName);
	    if(dataSource == null) 
			dataSource = lookupDataSource(ic, "java:comp/env" + dataSourceName);
	    if(dataSource == null) 
			dataSource = lookupDataSource(ic, "comp/env" + dataSourceName);

		if(dataSource == null) {
			log.error("Problems while doing JNDI lookup for: "+ dataSourceName);
			factoryState = STATE_INITIALIZED_AND_ERROR;
		} else {
			factoryState = STATE_INITIALIZED_AND_READY;
			isDataSourceUsed = true;
		}
		return;
	}

	/**
	 * Registers the jdbc driver
	 * 
	 * @param driver
	 */
	private void useStraightConnection(String driverClass) {
		if(log.isDebugEnabled())
			log.debug("using DB straight connection...");
		try {
			// create instance of the driver
			Driver driverInstance = (Driver) Class.forName(driverClass).newInstance();
			DriverManager.registerDriver(driverInstance);
			// set parameters
			log.info("Connected to Database: " + userName + "@" + jdbcUrl);
			factoryState = STATE_INITIALIZED_AND_READY;
			isDataSourceUsed = false;
		} catch (InstantiationException e) {
			log.error("Problems instantiating class: " + driverClass);
			factoryState = STATE_INITIALIZED_AND_ERROR;
		} catch (IllegalAccessException e) {
			log.error("Problems instantiating class: " + driverClass);
			factoryState = STATE_INITIALIZED_AND_ERROR;
		} catch (ClassNotFoundException e) {
			log.error("Class is not available in the classpath: " + driverClass);
			factoryState = STATE_INITIALIZED_AND_ERROR;
		} catch (SQLException e) {
			log.error("Problems registering the driver: " + driverClass);
			factoryState = STATE_INITIALIZED_AND_ERROR;
		}
	}

	/**
	 * Depending on the connection method, return Connection instance
	 * 
	 * @return
	 * @throws SQLException
	 */
	private Connection getConnectionInstance() throws SQLException {
		if (isDataSourceUsed)
			return dataSource.getConnection();

		java.util.Properties info = new java.util.Properties();
		info.put("user", userName.trim());
		info.put("password", password.trim());
		info.put("defaultRowPrefetch","1");

		Connection c = DriverManager.getConnection(jdbcUrl.trim(), info);
		//Connection c = DriverManager.getConnection(jdbcUrl.trim(), userName.trim(), password.trim());
		//log.info("Connected to Database: " + userName + "@" + jdbcUrl);
		return c;
	}

	/**
	 * 
	 * Used for setting the JNDI DataSource lookup value.
	 * 
	 * @param dataSourceName JNDI DataSource uri. A null value will disallow any runtime lookup.
	 * 
	 */
	public static void setSuggestedDataSource(String dataSourceName) {
		suggestedDataSourceName = dataSourceName;
	}
	

	private void makeSureItIsUsed(String suggestedDataSource) {
		if(dataSourceName == null || !dataSourceName.equals(suggestedDataSource))
			useDataSource(suggestedDataSource);
	}
}

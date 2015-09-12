package com.mssint.jclib;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import com.sun.corba.se.impl.encoding.OSFCodeSetRegistry.Entry;

/**
 * A wrapper class around the java properties file for the jvm instance.
 * This will read the default config file and also allow for the runtime
 * property assignment and discovery.
 *  
 * Additionally it provides a wrapper for instantiating one or more OdtClient(s)
 * primarily as the Server socket definition is held within this class.
 * {@link OdtClient}
 * <ul> 
 * <li>
 * In the case where instance of the jvm is working in a single threaded manner the 
 * odtConnect static method {@link #odtConnect} can be called to setup a single 
 * statically instanced odtClient.
 * </li>
 * <li>
 * In the case where the system is multi-threaded an additional method {@link #getOdtClient}
 * is provided for each thread to establish an OdtClient as required.
 * </li>
 * </ul>
 *  
 * @author MssInt
 *
 */
public class Config {

	
	/**
	 * Decimal separator default value
	 */
	public static String DEFAULT_DECIMAL_SEPARATOR = ".";

	
	/**
	 * Single instance of property file
	 */
	public static volatile String PROPERTIES_FILE_NAME = "jclib.properties";
	
	/**
	 * Single instance of the java properties object.
	 */
	private static volatile Properties properties = null;
	
	/**
	 * Single instance of the Log4j Logger
	 */
	public static final Logger log = LoggerFactory.getLogger("com.mssint.jclib");
//	private static final Logger log = Logger.getLogger(Config.class);	
	/**
	 * The program name definition, should be explicitly set by caller and used
	 * with OdtClient
	 *  
	 */
	public static String programName = null;
	
	/**
	 * Single instance of the OdtClient used in single Threaded scenario.
	 * 
	 */
	public static OdtClient odtClient = null;
	
	private static int odtUse = -1;
	
	public static char DECIMALCHAR = '.';
	
	public static boolean DEBUG = true;


	public static boolean COMPARE_SPACEZERO_NOT_EQUAL;
	/**
	 * Constructor accepting a fully qualified file name for the properties file. 
	 * 
	 * @param filename the fully path and file name quaified properties file.
	 * @throws FileNotFoundException invalid property file check location/existance.
	 */
	public Config(String filename) {
		PROPERTIES_FILE_NAME = filename;
		init();
	}
	
	/**
	 * Replace any existing properties with a new Properties object.
	 * @param props
	 */
	public static void updateProperties(Properties props) {
		if(props == null) {
			System.out.println("ATTEMPT TO SET NULL PROPERTIES. Leaving old properties as is: ");
			return;
		}
		properties = props;
	}

	/**
	 * Add the contents of this Properties object to the existing properties.
	 * If the existing properties are null, create new.
	 * @param <E>
	 * @param props
	 */
	public static void addProperties(Properties props) {
		if(properties == null)
			properties = new Properties();
		if(props == null)
			return;
		properties.putAll(props);
	}
	
	/**
	 * Verify connection and or attempt to Connect to the single instanced
	 * odtmon 
	 * In the first instance either one of the following properties need to have been set/configured. 
	 * <ul>
	 * <li>
	 * jclib.odtmon.server.
	 * </li>
	 * <li>
	 * odtmon.host and odtmon.port
	 * </li>
	 * </ul>
	 * Note that this allows only one connection per virtual machine.
	 * For usage in multi-threaded environments, use getOdtClient()
	 * 
	 * @return true if a connection is available, otherwise false.
	 * 
	 */
	public static boolean odtConnect() {
		//if(odtUse == 0) return false;
		if(odtUse == 1) return true;
		if(odtClient == null) {
			String odtHost = Config.getProperty("odtmon.host");
			int odtPort = 0;
			if(odtHost != null) {
				String s = Config.getProperty("odtmon.port");
				if(s != null) odtPort = Integer.parseInt(s);
				else odtHost = null;
			}
			if(odtHost == null) {
				String odt = Config.getProperty("jclib.odtmon.server");
				if(odt != null) {
					String [] hp = odt.split(":");
					if(hp.length == 2) {
						odtHost = hp[0];
						odtPort = Integer.parseInt(hp[1]);
					}
				}
			}
			if(odtHost == null) {
				odtUse = 0;
				return false;
			}
			odtClient = new OdtClient(odtHost, odtPort, programName);
			if(!odtClient.isConnected()) {
				odtClient = null;
				return false;
			}
			odtUse = 1;
			return true;
		}
		return false;
	}
	
	/**
	 * Close down the single instanced odtClient it will be disposed and set to null.
	 * 
	 */
	public static void odtClose() {
		odtUse = -1;
		if(odtClient != null) {
			odtClient.close();
			odtClient = null;
		}
	}
	
	/**
	 * Default parameterless constructor.
	 * 
	 * PROPERTIES_FILE_NAME will have needed to be set prior to calling
	 * this. 
	 * 
	 * @throws FileNotFoundException properties file has not been or is invalid. 
	 */
	public Config() throws FileNotFoundException {
		init();
	}
	
	/**
	 * This is a "factory class" which creates a new OdtClient instance, connects it
	 * to the required odtmon server and returns the object. The caller must call close()
	 * on this object once complete it is also suggested that the reference is set to null.
	 * 
	 * To test for a successful connection call isConnected()
	 * @return a new OdtClient object.
	 */
	public static OdtClient getOdtClient(String program) {
		String host = Config.getProperty("odtmon.host");
		String s = Config.getProperty("odtmon.port");
		int port;
		if(s != null) port = Integer.parseInt(s);
		else port = 0;
		OdtClient odt = new OdtClient(host, port, program);
		return odt;
	}
	
	/**
	 * Called from constructor and sets up the following
	 * Read properties, set environment vars for Command, do so initial logging 
	 * and set the timer.  
	 */
	private void init() {
		if(properties == null) {
			properties = new Properties();
			try {
				InputStream inputStream = Glb.class.getClassLoader()
				.getResourceAsStream(PROPERTIES_FILE_NAME);
				properties.load(inputStream);
			} catch (NullPointerException e) {
				log.error("Properties file not found: "
						+ PROPERTIES_FILE_NAME, new FileNotFoundException(e.toString()));
//				throw new FileNotFoundException("Properties file '"+PROPERTIES_FILE_NAME+"' not found.");
			} catch (IOException e) {
				log.error("Properties file not found: "
						+ PROPERTIES_FILE_NAME, new FileNotFoundException(e.toString()));
//				throw new FileNotFoundException("Properties file '"+PROPERTIES_FILE_NAME+" not found.");
			}
			log.info("Loaded properties file '"+PROPERTIES_FILE_NAME+"'");
		}
        //Initialise Paths for command execution
        Command.setPath(properties.getProperty("execution.path"));
        Command.setHomeDirectory(properties.getProperty("execution.homedir"));
        Command.setStdOut(properties.getProperty("execution.stdout"));
        String s = properties.getProperty("jclib.decimal");
        if(s == null || s.length() == 0) DECIMALCHAR = '.';
        else DECIMALCHAR = s.charAt(0);
        s = properties.getProperty("jclib.space.zero.equal");
        if(s == null || s.length() == 0) COMPARE_SPACEZERO_NOT_EQUAL = false;
        else if(s.equalsIgnoreCase("true")) COMPARE_SPACEZERO_NOT_EQUAL = false;
        else COMPARE_SPACEZERO_NOT_EQUAL = true;
        
        s=properties.getProperty("numeric.decimal.separator");
        if(s!=null && s.length() > 0) DECIMALCHAR = s.charAt(0);
        s=properties.getProperty("jclib.debug");
        if(s!=null && s.equalsIgnoreCase("true")) DEBUG = true;
        else DEBUG = false;
	}
	

	/**
	 * Get an instance of the Logger (Log4j)
	 * A log4j.xml file is expected with a com.mssint.jclib logger defined.
	 * However due to the nature of Log4j if not present the net effect is no logging
	 * will occur.
	 * 
	 * @return Logger the current system wide Logger (Log4j)
	 */
	public static Logger getLogger() { return log; }
	
	public static String getProperty(String p) { 
		if(properties == null) return null;
		return properties.getProperty(p);
	}
	
	public static Properties getProperties() {
		return properties;
	}
	public static String getPropertiesName() {
		return PROPERTIES_FILE_NAME;
	}
	
	/**
	 * Returns an array of properties based on a match of the first part
	 * of the property name.
	 * @param search
	 * @return A list of matching property objects
	 */
	public static Object [] getPropertyKeys(String search) {
		ArrayList<String> set = new ArrayList<String>();
		if(properties == null) 
			throw new NullPointerException("Config class has not been initialised.");
		for(Iterator<Object>it=properties.keySet().iterator();it.hasNext();) {
			String s = it.next().toString();
			if(s.length() < search.length()) continue;
			if(s.substring(0, search.length()).compareTo(search) == 0)
				set.add(s);
		}
		Object [] list = set.toArray();
		Arrays.sort(list);
		return list;
	}
	
	/**
	 * Set a property at runtime
	 * 
	 * @param name the key of the property
	 * @param value the value associated with the key
	 * 
	 */
	public static void setProperty(String name, String value) {
		if(name == null) return;
		if(properties == null)
			properties = new Properties();
		properties.setProperty(name, value);
	}

	
	/**
	 * Runtime discovery of the setting of a boolean property.
	 * 
	 * Any undefined property will recieve a false value. 
	 * 
	 * @param name the key of the property
	 * @return boolean indicating the value of the property as true or false.
	 */
	public static boolean getBooleanProperty(String name) {
		if(properties == null) return false;
		String value = properties.getProperty(name);
		if(value == null) return false;
		if(value.equalsIgnoreCase("true")) return true;
	    if(value.equals("1")) return true;
		return false;
	}

	/**
	 * 
	 * Simple helper method to allow a developer to get a sample lo4j xml config file
	 * printed to stdout.
	 * 
	 */
	public static void printLogConfigFile() {
		System.out.print(
		"<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
		"<!DOCTYPE log4j:configuration SYSTEM \"log4j.dtd\">\n" +
		"<!-- This is a sample log4j configuration file. It should be written to\n" +
	    "     a file called \"log4j.xml\" and must appear in the classpath.\n" +
	    " -->\n" +
		"\n" +
		"<log4j:configuration xmlns:log4j=\"http://jakarta.apache.org/log4j/\">\n" +
		"\n" +
		"    <!-- Output messages to the console -->\n" +
		"    <appender name=\"console\" class=\"org.apache.log4j.ConsoleAppender\">\n" +
		"        <layout class=\"org.apache.log4j.PatternLayout\">\n" +
		"            <param name=\"ConversionPattern\" value=\"%-5p %C{1}() - %m%n\"/>\n" +
		"        </layout>\n" +
		"    </appender>\n" +
		"\n" +
		"    <!-- Output messages to a log file -->\n" +
		"    <appender name=\"jcliblog\" class=\"org.apache.log4j.FileAppender\">\n" +
		"        <param name=\"File\" value=\"/usr/local/log/jclibtest.log\" />\n" +
		"        <param name=\"Append\" value=\"true\" />\n" +
		"        <layout class=\"org.apache.log4j.PatternLayout\">\n" +
		"            <param name=\"ConversionPattern\"\n" +
		"                value=\"%d{yyyy-MM-DD HH:mm:ss.SSS} %t %-5p %c{2} - %m%n\"/>\n" +
		"        </layout>\n" +
		"    </appender>\n" +
		"\n" +
		"<!-- Set the level appropriately:\n" +
		"     levels are:  debug, info, warn, error, fatal\n" +
		" -->\n" +
		"    <logger name=\"com.mssint.jclib\">\n" +
	    "        <level value=\"debug\" />\n" +
	    "        <appender-ref ref=\"console\"/>\n" +
	    "        <appender-ref ref=\"jcliblog\"/>\n" +
	    "    </logger>\n" +
		"\n" +
	    "</log4j:configuration>\n"
		);

	}

	/**
	 * Force the load of a specific property
	 * 
	 * Any predefined key/value pairs will be replaced.
	 * 
	 */
	public static void load(String properties) throws FileNotFoundException {
		new Config(properties);
	}
}

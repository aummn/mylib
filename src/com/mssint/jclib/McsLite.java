package com.mssint.jclib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Java client for connecting to a MCS Server for programme execution/message processing. 
 * 
 * This is achieved via the establishing of a socket connection with the MCS and sending and receiving 
 * messages on behalf of a requesting client i.e. as a client or clients in "server/multi-mode" scenario i.e.
 * housed in a container e.g JBOSS etc...
 * 
 * @author MssInt
 * 
 */
public class McsLite {
	private static final Logger log = LoggerFactory.getLogger(McsLite.class);
	//FLags - keep in sync with mcs_api.h
	public static final int NODELAY			= 000000000001;
	public static final int CONFIRM			= 000000000002;
	public static final int NOTUSED			= 000000000004;
	public static final int ISCLIENT		= 000000000040;
	public static final int ISSERVER		= 000000000100;
	public static final int ISCONTROL		= 000000000200;
	public static final int IST27			= 000000000400;
	public static final int LOGINREQ		= 000000001000;
	public static final int RETURN			= 000000002000;
	public static final int MALLOCHOST		= 000000004000;
	public static final int MESSAGES		= 000000010000;
	public static final int AUTH_OS			= 000000020000;
	public static final int AUTH_MCS		= 000000040000;
	public static final int AUTH_USER		= 000000100000;
	public static final int AUTH_NONE		= 000000200000;
	public static final int REQUIRE_RESPONSE= 000001000000;
	public static final int GLOB_RESPONSE	= 000002000000;
	public static final int MULTISEND		= 000004000000;
	public static final int AUTH = (AUTH_OS|AUTH_MCS|AUTH_USER|AUTH_NONE);
	
	
	public static final int GOTO_EOJ		= 000010000000;
	public static final int PULLMODE		= 000020000000;
	public static final int TIMEOUT			= 000040000000;
	public static final int READY			= 000100000000;
	public static final int BROADCAST		= 000200000000;
	public static final int MSSMCS			= 000400000000;
	public static final int EMBEDDED_HEADER	= 001000000000;
	public static final int NOREPLY			= 002000000000;
	public static final int MSG_AVAILABLE	= 004000000000;
	public static final int POLL_REQUESTED	= 010000000000;

	//Plugin management
	public static final int INPUT_PHASE		= 000000000010;
	public static final int OUTPUT_PHASE	= 000000000020;
	public static final int ISPLUGIN		= 000000400000;
	
	
	public static final int MCS_SESSIONID_LEN = 36;
	public static final int MCS_NAMELEN = 26;
	
	//Error values
	public static final int E_SYSERR	=	-1001;
	public static final int E_VERSION	=	-2001;
	public static final int E_SOCKET	=	-2003;
	public static final int E_MAXCOPIES	=	-2004;

	//Non fatal errors
	public static final int E_FULL		=	 -2;
	public static final int E_EMPTY		=	 -2;
	public static final int E_TIMEOUT	=	 -3;
	public static final int E_NOINIT	=	 -5;
	public static final int E_BADPARAM	=	 -6;
	public static final int E_NOQUEUE	=	 -7;
	public static final int E_NOPROGRAM	=	 -8;
	public static final int E_NODATA	=	 -9;
	public static final int E_TOOBIG	=	-15;
	public static final int E_INTR		=	-23;

	public static final int E_BADHOST	=	-51;
	public static final int E_NOMCS		=	-52;
	public static final int E_NOAUTH	=	-53;
	public static final int E_BADNAME	=	-54;
	public static final int E_DISABLED	=	-61;
	public static final int E_EOJ		=	-99;
	public static final String MCS_ERROR = "MCS:ERROR: ";
	
	private String MCS_VERSION = "1.3.0-0";

	
	private static String defaultHost = null;
	private static int defaultPort = 0;
//	private static int defaultT27Port = 0;
	private static long defaultReadTime = 0;
	private static boolean exitOnError;
	
	private static boolean DEBUG = false;
	private static String debugFile = "/tmp/McsLite.dbg";
	
	private String serviceName;
	private String sessionId;
	private String userName;
	private int attr;
	private int sequence; //Increment for each message sent
	
	private String host;
	private int port;
	private boolean connected;
	private int errno;
	private String messages;
	private boolean moreToCome;
	PrintWriter out;
	
	public int errno() { return errno; }
	private String charset;
	private McsHeader hdr;
	private int timeOut;
	
	private final Properties properties;
	
	private Socket socket;
	
	
//	private SocketChannel socketChannel;
//	private ByteBuffer headerBuffer;
//	private byte[] headerBytes;
//	private ByteBuffer readBuffer;
	/**
	 * Constructor - initialised with properties file for connection details
	 * @param properties java properties with specified MCS connection options
	 * e.g. host port timeout etc....
	 */
	public McsLite(Properties properties) {
		Properties props = Config.getProperties();
		if(props == null && properties == null)
			props = new Properties();
		else if(props == null)
			props = properties;
		if(properties != null)
			props.putAll(properties);
		this.properties = props;
		
		sessionId = null;
		attr = 0;
		host = null;
		port = 0;
		exitOnError = true;
		socket = null;
		connected = false;
		hdr = null;
		userName = null;
		moreToCome = false;
		charset = properties.getProperty("character.set");
		if(charset == null) charset = "iso-8859-1";
		if(DEBUG) {
			try {
				out = new PrintWriter(new File(debugFile));
			} catch (IOException e) {
				DEBUG = false;
				log.info("Could not open '"+debugFile+"' - debug mode disabled.");
			}
		}
		if(DEBUG) { out.println("CONSTRUCTOR CALLED."); out.flush(); }
	}
	
	/**
	 * Set the host to which the client socket will connect
	 * @param host IP resolvable name
	 */
	public void setHost(String host) { this.host = host; }
	
	/**
	 * Set the port number to which the client socket will connect
	 * @param host IP resolvable name
	 */
	public void setPort(int port) { this.port = port; }
	
	/**
	 * Set the timeout
	 * @param timeout in hundreths second (600=60 seconds)
	 */
	public void setTimeOut(int timeOut) { this.timeOut = timeOut * 1000; }//ms 
	
	/**
	 * get the timeout
	 * @return timeout in ms (60000=60 seconds)
	 */
	public int getTimeOut() { return timeOut; }//ms 
	/**
	 * Get the host this client is/will connect too.
	 * @return the host
	 */
	public String getHost() { return host; }
	
	/**
	 * Get the port number this client is/will connect too.
	 * @return the port number
	 */
	public int getPort() { return port; }
	
	/**
	 * Is the client connected to the MCS server
	 * @return true - yes false - no
	 */
	public boolean isConnected() { return connected; }

	/**
	 * Does the client allow any form of delay? 
	 * In particular when the message is received the full header
	 * must be immediately readable/available or it is discarded i.e. 
	 * null is returned to the caller.
	 * @param on true means no delay, false means delayable.
	 */
	public void setNoDelay(boolean on) {
		if(on) attr |= NODELAY;
		else attr &= ~(NODELAY); 
	}

	/**
	 * Get the name of the program which sent the last message
	 * @return the program name
	 */
	public String programName() {
		if(hdr == null) return null;
		return hdr.msgSource;
	}
	
	/**
	 * Is an attribute/flag set 
	 * @param a the flag/attribute to check
	 * @return true is set, false not set.
	 */
	public boolean attr(int a) { return (attr & a) != 0; }
	
	/**
	 * Is there additional data/message available
	 * @return true the message is still incomplete, false everything has been received
	 */
	public boolean isMoreToCome() { 
		if(DEBUG) System.out.println("isMoreToCome() returning " + moreToCome);
		return moreToCome; 
	}
	
	/**
	 * Read the MCS configuration file to get the default settings
	 * Re-read this file if it has not been read in 30 seconds.
	 * 
	 * Please refer to MCS setup documentation for mssmcs.cfg file,
	 * environment and properties file configuration and deployment.
	 * 
	 */
	private void setDefaults() {
		long time = new Date().getTime();
		if((time - defaultReadTime) < 30) return;
		
		//Figure out which file to read.
		String fileName = "/usr/local/mcs/mssmcs.cfg"; //The default
		String s = System.getenv("MCS_CONFIG_FILE");
		if(s == null) s = properties.getProperty("mcs.config.file");
		if(s != null) {
			if(s.charAt(0) != '/') {
				String x = System.getenv("MCS_PATH_PREFIX");
				if(x == null) x = properties.getProperty("mcs.path.prefix");
				if(x == null) x = "/usr/local/mcs";
				s = x + "/" + s;
			}
			fileName = s;
		}
		if(log.isDebugEnabled())
			log.debug("Loading MCS config file "+fileName);
		setDefaultsStatic(fileName);
		if(defaultHost == null || defaultPort == 0) {
			defaultHost = "localhost";
			defaultPort = 15200;
		}
	}
	
	/**
	 * Re-initialises any configuration data in this module.
	 * @param fileName
	 */
	private static synchronized void setDefaultsStatic(String fileName) {
		File mcsFile = new File(fileName);
		BufferedReader input = null;
		try {
			input = new BufferedReader(new FileReader(mcsFile));
			String buf;
			while((buf = input.readLine()) != null) {
				buf = buf.trim();
				if(buf.length() == 0) continue;
				if(buf.charAt(0) == '#') continue;
				String [] p = buf.split("=");
				if(p.length != 2) continue;
				p[0] = p[0].trim().replaceAll("  *",	" ");
				p[1] = p[1].trim().replaceAll("^\"", "").replaceAll("\"$", "");
				if(p[0].equalsIgnoreCase("mcs host")) {
					if(p[1].length() > 0) defaultHost = p[1];
				} else if(p[0].equalsIgnoreCase("listener port")) {
					if(p[1].length() > 0) defaultPort = Integer.parseInt(p[1]);
				} else if(p[0].equalsIgnoreCase("t27 port")) {
//					if(p[1].length() > 0)  defaultT27Port = Integer.parseInt(p[1]);
				} else if(p[0].equalsIgnoreCase("mcs exitonerror")) {
					if(p[1].equalsIgnoreCase("true") || p[1].equals("1"))
						exitOnError = true;
					else exitOnError = false;
				}
			}
		} catch (FileNotFoundException e) {
			//We really don't care if the file is missing - we will use defaults
		} catch (IOException e) {
			log.error("An error occurred while reading MCS config file "+fileName);
		} finally {
			if(input != null)
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		defaultReadTime = new Date().getTime();
	}

	/**
	 * Set host and port-name in a single call. 
	 * These could have been set by setHost() and setPort()
	 * This option allows for a single host:port setting of the values.
	 * If no value is specified or the value cannot be parsed the system will set the values from the environment.
	 * @param id the host:port value, null, "" 
	 * @throws IOException
	 */
	private void setHostAndPort(String id) throws IOException {
		setDefaults(); //Read info from the mcs config file

		if(id == null || id.length() == 0 || id.charAt(0) == ' ') {
			//If BOTH host and port are already set we can ignore properties or env
			if(host != null && port > 0) return;
			//Check environment then properties then try mcs config file.
			String s = System.getenv("MCS_CONNECT");
			if(s != null) {
				String [] sp = s.split(":");
				if(sp.length != 2)
					throw new IOException("Environment variable MCS_CONNECT=\""+s+"\" should be set to \"host:port\"");
				host = sp[0];
				port = Integer.parseInt(sp[1]);
			} else {
				host = properties.getProperty("mssmcs.host");
				if(host != null) {
					s = properties.getProperty("mssmcs.port");
					if(s != null) port = Integer.parseInt(s);
				}
			}
		} else {
			String [] sp = id.split(":");
			if(sp.length == 2)
				port = Integer.parseInt(sp[1]);
			host = sp[0];
		}
		if(host == null || port == 0) {
			host = defaultHost;
			port = defaultPort;
		}
	}
	
	/**
	 * Will return the Session Identifier if the system is behaving as a client
	 * null otherwise.
	 * @return
	 */
	public String getSessionId() {
		if(!attr(ISCLIENT)) return null;
		return sessionId;
	}
	
	/**
	 * Presently behaves exactly like {@link #getSessionId()}
	 * @return
	 */
	public String getMyName() {
		if(attr(ISCLIENT)) return sessionId;
		return serviceName;
	}
	
	/**
	 * Enables a client to initialise and establish a socket connection to the MCS
	 * @param mcsId host:port value
	 * @param sessionId station identifier or generally named identifier in multiple client mode
	 * @return true if initialised & connected
	 * @throws IOException no Mcs Server present, configuration error, host unavailable.
	 */
	public boolean clientInit(String mcsId, String sessionId) throws IOException {
		
		if(sessionId != null && sessionId.length() > 0)
			this.sessionId = sessionId;
		
		if(connected) close();
		
		attr |= ISCLIENT;
		setHostAndPort(mcsId);
		sequence = 1;
		
		//Set timeout (default=60s)
		String s = properties.getProperty("mssmcs.timeout");
		if(s == null) timeOut = 0;
		else timeOut = Integer.parseInt(s);
		timeOut *= 1000; //Convert to ms
		
//		selector = SelectorProvider.provider().openSelector();
		if(!setupConnection()) {
			return false;
		}
		
		connected = true;
		if(DEBUG) {
			out.println("clientInit: id="+this.sessionId); out.flush();
		}
		return true;
	}
	
	public boolean serverInit(String mcsId, String serviceName) throws IOException {
		if(serviceName == null || serviceName.length() == 0)
			throw new IllegalArgumentException("serviceName is required");
		
		this.serviceName = serviceName;

		if(connected) close();
		
		attr |= ISSERVER;
		setHostAndPort(mcsId);
		sequence = 1;
		
		//Get the process ID (returns "pid@host")
		String pidhost = ManagementFactory.getRuntimeMXBean().getName();
		int idx = pidhost.indexOf('@');
		int pid;
		if(idx != -1) {
			String pidStr = pidhost.substring(0, idx);
			pid = Integer.parseInt(pidStr);
		} else {
			pid = 1;
		}
		
		//TODO: setUserId(0);
		
		if(!setupConnection()) {
			return false;
		}
		
		//Send a login message
		hdr = new McsHeader();
		attr |= ISSERVER;
		attr |= PULLMODE;
		hdr.flags = attr | LOGINREQ;
		
		LoginMessage login = new LoginMessage();
		login.programname.set(serviceName);
		login.version.set(MCS_VERSION);
		login.pid.set(pid);
		login.userid.set(System.getProperty("user.name"));
		
		hdr.msgLength = login.size();
		hdr.msgSource = serviceName;
		hdr.setMessage(login.message.getBytes(), login.message.size());
		
		//Send the message
		OutputStream os = socket.getOutputStream();
		byte [] h = hdr.getMessageBytes();
//		System.out.println("McsLite.serverInit - wrote "+h.length+" bytes.");
		os.write(h);

		hdr.flags &= ~(LOGINREQ);
		connected = true;
		if(DEBUG) { 
			out.println("serverInit: id="+this.serviceName); out.flush();
		}
		return true;
	}
	
	private boolean setupConnection() throws IOException {
		
		boolean error = false;
		try {
			socket = new Socket(host, port);
			socket.setSoTimeout(timeOut);
		} catch (IOException e) {
			error = true;
			log.error("Error creating socket for "+host+":"+port);
			if(exitOnError) throw new RuntimeException(e);
			socket = null;
			e.printStackTrace();
			connected = false;
			return false;
		} finally {
			if(error && socket != null)
				close();
		}
		return true;
	}

	/* For socketChannel
	private boolean setupConnection() throws IOException {
		
		boolean error = false;
		try {
			socketChannel = SocketChannel.open();
			socketChannel.configureBlocking(true);
			InetSocketAddress sockAddress = new InetSocketAddress(host, port);
			socketChannel.connect(sockAddress);
			socketChannel.socket().setSoTimeout(timeOut);
		} catch (IOException e) {
			error = true;
			log.error("Error creating socket for "+host+":"+port);
			if(exitOnError) throw new RuntimeException(e);
			socketChannel = null;
			e.printStackTrace();
			connected = false;
			return false;
		} finally {
			if(error && socketChannel != null)
				close();
		}
		//Required for non-blocking channels
//		socketChannel.finishConnect();
		return true;
	}
	*/
	
	/**
	 * Set the user name to be used in client communications, if set the instance is set for
	 * authentication. 
	 * @param userName
	 */
	public void setUserId(String userName) {
		attr |= AUTH_MCS;
		this.userName = userName;
	}

	/**
	 * What is the user name that we are using to connect to the MCS with
	 * @return the user name if defined null otherwise.
	 * 
	 */
	public String getUserId() {
		return userName;
	}

	/**
	 * Close the current MCS server connection and connect to the defined host:port combination.
	 * @param mcsId host:port combination
	 * @return if successful reconnection -> true else false;
	 * @throws IOException 
	 */
	public boolean reconnect(String mcsId) throws IOException {
		if(!connected) return false;
		if(mcsId == null) return false;
		mcsId = mcsId.trim();
		if(mcsId.length() == 0) return false;
		String host;
		int port;
		
		int i = mcsId.indexOf(':');
		if(i != -1) {
			host = mcsId.substring(0, i);
			port = Integer.parseInt(mcsId.substring(i+1));
		} else {
			if(!sendMessage(null, "?WHOIS " + mcsId)) 
				return false;
			String msg = recvMessageString();
			if(msg == null || msg.length() == 0) return false;
			//expect "MCSADDRESS mcsid host:port"
			String [] m = msg.split(" ");
			i = m[2].indexOf(':');
			if(i == -1) return false;
			host = m[2].substring(0, i);
			port = Integer.parseInt(m[2].substring(i+1));
		}
		sequence = 1;

		close();
		boolean error = false;
		try {
			socket = new Socket(host, port);
			socket.setSoTimeout(timeOut);
		} catch (IOException e) {
			error = true;
			log.error("Error creating socket for "+host+":"+port);
			if(exitOnError) throw e;
			socket = null;
			e.printStackTrace();
			connected = false;
			return false;
		} finally {
			if(error && socket != null)
				try {
					socket.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
		}
		connected = true;
		return true;
	}
	
	/**
	 * The client wants the MCS to handle the message pass it through to a destination program.
	 * If destination is null the message being sent to the MCS is of a control type i.e. not targeting a program 
	 * but control/informational from the MCS itself.
	 * @param dest the target program being called, or null if control/informational message 
	 * @param message the message/data being sent.
	 * @return true if message successfully sent, false otherwise.
	 * @throws IOException
	 */
	public boolean sendMessage(String dest, String message) throws IOException {
		return sendMessage(dest, message, message.length());
	}
	
	/**
	 * Overloaded method using a string as message of a specified length {@see #sendMessage(String, String)} 
	 * @param dest
	 * @param message
	 * @param len
	 * @return
	 * @throws IOException
	 */
	public boolean sendMessage(String dest, String message, int len) throws IOException {
		byte [] buf;
		if(message == null || len == 0) buf = new byte[0];
		else buf = message.getBytes(charset);
//		else buf = Util.getBytes(message);
		return sendMessage(dest, buf, buf.length);
	}
	
	/**
	 * Overloaded method using a byte array no specified length {@see #sendMessage(String, String)} 
	 * @param dest
	 * @param message
	 * @return
	 * @throws IOException
	 */
	public boolean sendMessage(String dest, byte [] message) throws IOException {
		return sendMessage(dest, message, message.length);
	}
	
	/**
	 * Overloaded method using a byte array of a specified length {@see #sendMessage(String, String)}
	 * This method holds the implementation specifics all others defer here. 
	 * @param dest
	 * @param message
	 * @param len
	 * @return
	 * @throws IOException
	 */
	public boolean sendMessage(String dest, byte [] message, int len) throws IOException {
		if(!connected) return false;
		boolean voidMessage;
		
		if(DEBUG) { out.println("SEND MESSAGE TO MCS [dest="+dest+"]:\n" + new String(message,0,len,"cp1250"));  out.flush(); }
		if(dest != null && dest.length() == 0) dest = null;

		if(attr(ISCLIENT)) {
			hdr = new McsHeader();
		} else {
			if(hdr == null)
				hdr = new McsHeader();
			if(dest == null || dest.trim().length() == 0) {
				hdr.msgDest = dest;
			} else {
				hdr.msgDest = null;
				hdr.flags |= RETURN;
			}
		}
		if(message == null || len <= 0) {
			if(dest == null || !attr(CONFIRM)) {
				errno = E_NODATA;
				return false;
			}
			voidMessage = true;
		} else voidMessage = false;
		
		if(!voidMessage && message[0] == '?') {
			String msg = new String(message);
			String [] m = msg.split("[ \t]{1,}");
			if(m.length > 1 && m[0].toUpperCase().equals("?ON")) {
				if(reconnect(m[1])) {
					messages = "CONNECTED TO " + m[1];
				} else {
					messages = "CONNECTION TO " + m[1] + " FAILED";
				}
				attr |= MESSAGES;
				return true;
			}
		}
		
		hdr.setMessage(message, len);
		hdr.sequence = sequence++;
		hdr.flags = attr;
		if(attr(ISCLIENT)) {
			hdr.sid = sessionId;
			hdr.userName = userName;
			hdr.msgSource = null;
		} else {
			hdr.msgSource = serviceName;
		}
		
		if(dest != null) hdr.msgDest = dest;
		//TODO - some flag management
		
		//Send the message to the MCS
		byte [] b = hdr.getMessageBytes();
		OutputStream out = socket.getOutputStream();
//		System.out.println("McsLite.sendMessage - wrote "+b.length+" ("+hdr.totalLength()+") bytes.");
		out.write(b, 0, hdr.totalLength());
		out.flush();
		return true;
	}
	
	/**
	 * Get the response message from MCS as a string.
	 * @return String response message
	 * @throws IOException
	 */
	public String recvMessageString() throws IOException {
		byte [] buf = recvMessage();
		if(buf == null) return null;
		return new String(buf, charset);
//		return Util.newString(buf);
	}
	
	/**
	 * Get the response message from MCS as a byte array.
	 * @return byte array response message
	 * @throws IOException
	 */
	public byte [] recvMessage() throws IOException  {
		if(!connected) return null;

		if(socket == null) {
			log.error("McsLite.recvMessage(): socket is null yet connected=true. session-id="+sessionId);
			close();
			return null;
		}
		
		//Indicate to mcs that we are willing to receive a message.
		if(attr(ISSERVER) && attr(PULLMODE)) {
			McsHeader h = new McsHeader();
			h.msgSource = serviceName;
			h.flags = READY | ISSERVER | ISCONTROL | PULLMODE;
			h.setMessage(null, 0);
			byte [] b = h.getMessageBytes();
//			System.out.println("McsLite.recvMessage - wrote "+b.length+" ("+h.totalLength()+") bytes.");
			OutputStream out = socket.getOutputStream();
			out.write(h.getMessageBytes(), 0, h.totalLength());
			out.flush();
		}
		
		InputStream in = socket.getInputStream();
//		If NODELAY is set, don't bother unless we can read a full header.
		if(attr(NODELAY)) {
			if(in.available() < hdr.headerSize) return null;
		}
		
		if(attr(MESSAGES)) {
			attr &= ~(MESSAGES);
			return messages.getBytes();
		}

		if(hdr == null) hdr = new McsHeader();
		
		attr &= ~(MSG_AVAILABLE|POLL_REQUESTED);
		
		
		//Read the header
		byte [] buf = new byte[hdr.headerSize];
		int totlen = hdr.headerSize;
		int len = 0;
		while(len < totlen) {
			int l;
			try {
//				System.out.println("McsLite.recv 1 - expect "+(totlen-len)+" bytes.");
				l = in.read(buf, len, totlen - len);
//				System.out.println("McsLite.recv - read "+(l)+" bytes.");
			} catch (SocketTimeoutException e) {
				log.error("Timeout while reading from MCS "+ e.getMessage());
				close();
				throw new RuntimeException(e);
			} catch (IOException e) {
				if(exitOnError) throw new RuntimeException(e);
				if(DEBUG) e.printStackTrace();
				close();
				return null;
			}
			if(l < 0) {
				close();
				log.error("MCS Header read failure.");
				if(exitOnError) 
					throw new IOException("Communication failure: Could not read full MCS header");
				return null;
			}
			len += l;
		}
		
		//We have now read a full header.
		hdr.loadHeader(buf);
		if(log.isDebugEnabled())
			log.debug(String.format("MCS HEADER FLAGS=0%o message length=%d", hdr.flags, hdr.msgLength));
		if(attr(ISCLIENT) && (hdr.flags & MULTISEND) != 0)
			moreToCome = true;
		else
			moreToCome = false;
		
		if(DEBUG) System.out.println("Read MCS header. Still to read "+hdr.msgLength+" bytes. moreToCome="+moreToCome);
		
		//Ensure SID is set
		if(attr(ISCLIENT)) {
			if(sessionId == null && hdr.sid != null) sessionId = hdr.sid;
		} else if(attr(ISSERVER)) {
			//Set pullmode according to the MCS demands
			if(hdr.flags(PULLMODE)) {
				attr |= PULLMODE;
			} else {
				attr &= ~(PULLMODE);
			}
			if(hdr.flags(MULTISEND)) {
				attr |= MULTISEND|PULLMODE;
			} else {
				attr &= ~(MULTISEND);
			}
		}
		
		len = 0;
		totlen = hdr.msgLength;
		if(totlen <= 0) return new byte[0];
		buf = new byte[totlen];
		while(len < totlen) {
			int l;
			try {
//				System.out.println("McsLite.recv 2 - expect "+(totlen-len)+" bytes.");
				l = in.read(buf, len, totlen - len);
//				System.out.println("McsLite.recv - read "+(l)+" bytes.");
			} catch (SocketTimeoutException e) {
//				log.error("Error while reading from MCS "+ e.getMessage());
				throw new RuntimeException(e);
			} catch (IOException e) {
//				log.error("Error while reading from MCS "+ e.getMessage());
				if(exitOnError) throw new RuntimeException(e);
				if(DEBUG) e.printStackTrace();
				close();
				return null;
			}
			if(l < 0) {
				close();
				log.error("MCS message read failure.");
				if(exitOnError) 
					throw new IOException("Could not read full MCS message");
				return null;
			}
			len += l;
		}
		if(DEBUG) { out.println("RECV MESSAGE FROM MCS:\n" + new String(buf, "cp1250"));  out.flush(); }
//		System.out.println("recvMessage() returning 160 + "+totlen + " bytes. ["+
//				new String(buf, 0, totlen > 30 ? 30 : totlen, "cp1250"));
		return buf;
	}

	/**
	 * Check to see if a message is available when we return from a poll from the client i.e.
	 * has sufficient processing occurred that we have one or more messages pending processing? 
	 * @return true if a message is available to read, otherwise false.
	 * @throws IOException 
	 */
	public boolean isMessageAvailable() throws IOException {
		if(!connected) return false;

		
		InputStream in = socket.getInputStream();
		int size = in.available();
		if(DEBUG) System.out.println("isMessageAvailable(): " + size + " bytes");
		if(size >= hdr.headerSize) return true;
		return false;
	}
	
	/**
	 * Close/dispose of the connection to the MCS
	 */
	public void close() {
		connected = false;
		if(socket == null) return;
		try {
			if(socket.getOutputStream() != null)
				socket.getOutputStream().close();
		} catch (IOException e) {}
		try {
			if(socket.getInputStream() != null)
				socket.getInputStream().close();
		} catch (IOException e) {}
		try {
			socket.close();
		} catch (IOException e) {} //Don't care if error occurs.
		socket = null;
		attr = 0;
	}
	
	/**
	 * Inner class to handle message area and MCS_HEADER
	 * 
	 * Specific to the MCS and it's communications message definition.
	 * 
	 * @author MssInt
	 */
	protected class McsHeader {
		private int magic = 0x2003dfab; //offset 0
		int flags = 0;					//offset 4
		short version = 3;				//offset 8
		short headerSize = 160;			//offset 10
		short srcPort = 0;				//offset 12
		short destPort = 0;				//offset 14
		int sequence = 0;				//offset 16
		int special = 0;				//offset 20
		String sid = null;				//offset 24
		String msgSource = null;		//offset 61
		String msgDest = null;			//offset 88
		String userName = null;			//offset 115
		int msgLength = 0;				//offset 144
		int timestamp = 0;				//offset 148
		int sec = 0;					//offset 152
		int usec = 0;					//offset 156
		
		byte [] message = null;
		
		public String toString() {
			String s = String.format("%08x:0%10o:%03d:%d:%d:%d:%s:%s:%s:%s:%d", 
					magic,flags,version,headerSize,srcPort,destPort,sequence,
					sid,msgSource,msgDest,userName,
					msgLength);
			return s;
		}
		
		public boolean flags(int flag) {
			if((flag & flags) == 0)
				return false;
			return true;
		}

		protected McsHeader() { }
		protected int totalLength() {
			return headerSize + msgLength;
		}
		private void toByteArray(byte [] b, short val, int off) {
			b[off] = (byte)(val >>> 8);
			b[off+1] = (byte)(val);
		}
		private void toByteArray(byte [] b, int val, int off) {
			b[off] = (byte)(val >>> 24);
			b[off+1] = (byte)(val >>> 16);
			b[off+2] = (byte)(val >>> 8);
			b[off+3] = (byte)(val);
		}
		private void toByteArray(byte [] b, String val, int off, int len) {
			int i = 0;
			if(val != null) for(; i<val.length() && i < len; i++)
				b[off+i] = (byte)val.charAt(i);
			if(i < len) for(; i<len; i++) b[off+i] = 0;
			else b[off + len - 1] = 0;
		}
		private void fillHdrBytes(byte [] buf, int offset) {
			toByteArray(buf, magic, 0);
			toByteArray(buf, flags, 4);
			toByteArray(buf, version, 8);
			toByteArray(buf, headerSize, 10);
			toByteArray(buf, srcPort, 12);
			toByteArray(buf, destPort, 14);
			toByteArray(buf, sequence, 16);
			toByteArray(buf, special, 20);
			toByteArray(buf, sid, 24, MCS_SESSIONID_LEN + 1);
			toByteArray(buf, msgSource, 61, MCS_NAMELEN + 1);
			toByteArray(buf, msgDest, 88, MCS_NAMELEN + 1);
			toByteArray(buf, userName, 115, MCS_NAMELEN + 1);
			toByteArray(buf, msgLength, 144);
			toByteArray(buf, timestamp, 148);
			toByteArray(buf, sec, 152);
			toByteArray(buf, usec, 156);
		}
		private int toInt(byte [] b, int off) {
			return (b[off] << 24)
			+ ((b[off+1] & 0xff) << 16)
			+ ((b[off+2] & 0xff) << 8)
			+ (b[off+3] & 0xff);

		}
		private short toShort(byte [] b, int off) {
			return (short)((b[off] << 8) + (b[off+1] & 0xff));
		}
		private String bToString(byte [] b, int off, int len) {
			StringBuilder s = new StringBuilder();
			for(int i=0;i<len;i++) {
				if(b[off+i] == 0) break;
				s.append((char)b[off+i]);
			}
			return s.toString();
		}
		protected void loadHeader(byte [] hdr) throws IOException {
			//Validate header
			if(magic != toInt(hdr, 0))
				throw new IOException("MCS Header is corrupt - bad magic number." + 
						"This session has lost sync with the MCS and should be restarted.");
			if(version != toShort(hdr, 8))
				throw new IOException("MCS Header is version "+toShort(hdr, 8)+". This API can only handle version "+version);
			if(headerSize != toShort(hdr, 10))
				throw new IOException("MCS Header is corrupt - Header size="+toShort(hdr, 10)+" but expected "+headerSize);
			
			flags = toInt(hdr, 4);
			srcPort = toShort(hdr, 12);
			destPort = toShort(hdr, 14);
			sequence = toInt(hdr, 16);
			special = toInt(hdr, 20);
			sid = bToString(hdr, 24, MCS_SESSIONID_LEN + 1);
			msgSource = bToString(hdr, 61, MCS_NAMELEN + 1);
			msgDest = bToString(hdr, 88, MCS_NAMELEN + 1);
			userName = bToString(hdr, 115, MCS_NAMELEN + 1);
			msgLength = toInt(hdr, 144);
			timestamp = toInt(hdr, 148);
			sec = toInt(hdr, 152);
			usec = toInt(hdr, 156);
		}
		
		protected void setMessage(byte [] mesg, int len) {
			if(mesg == null || len == 0) msgLength = 0;
			else msgLength = len;
			message = new byte[headerSize + msgLength];
			if(msgLength > 0 && mesg != null)
				System.arraycopy(mesg, 0, message, headerSize, msgLength);
		}
		
		protected byte [] getMessageBytes() {
			fillHdrBytes(message, 0);
			return message;
		}
	}
	
	protected class LoginMessage {
		Group message = new Group();
//		Group magic = message.addGroup(Var.UNUMBER|Var.COMP, 4); //2 bytes
//		Group loginVersion = message.addGroup(Var.UNUMBER|Var.COMP, 4); //2 bytes
		Group pid = message.addGroup(Var.NUMBER|Var.COMP, 8); //4 bytes
		Group uid = message.addGroup(Var.NUMBER|Var.COMP, 8);
//		Group requestAttr = message.addGroup(Var.UNUMBER|Var.COMP, 8);
//		Group reserved = message.addGroup(Var.UNUMBER|Var.COMP, 8);
		Group userid = message.addGroup(Var.CHAR, MCS_NAMELEN + 1);
		Group version = message.addGroup(Var.CHAR, 16);
		Group passwd = message.addGroup(Var.CHAR, MCS_NAMELEN - 16 + 1);
		Group programname = message.addGroup(Var.CHAR, MCS_NAMELEN + 1);
		Group filler = message.addGroup(Var.CHAR, 3);
		
		public LoginMessage() {
			message.fillHex("00");
//			magic.setHex("B9C2");
//			loginVersion.set(2);
		}
		/**
		 * Returns the size of the LoginMessage message, in bytes.
		 * @return
		 */
		public int size() {
			return message.size();
		}
		
	}

	public void clearMessageQueue() {
		try {
			sendMessage(null, "?CLEAR_MESSAGE_QUEUE");
		} catch (IOException e) {
			//Not interested in error's or exceptions.
		}
	}

	/**
	 * Format a message indicating an error
	 * The message is a String which can contain parameter substitutions where each {} is 
	 * substituted by the next Object in the list.
	 * @param string The message string.
	 * @return
	 */
	public static byte[] errorMesg(String message, Object ... params) {
		StringBuilder sb = new StringBuilder();
		sb.append(MCS_ERROR);
		if(message == null) {
			sb.append("<null>");
		} else {
			String [] mesg = message.split("\\{\\}");
			for(int i=0; i<mesg.length; i++) {
				sb.append(mesg[i]);
				if((i+1) < mesg.length ) {
					if(i >= params.length) {
						sb.append("<null>");
					} else {
						Object o = params[i];
						if(o instanceof String) {
							sb.append((String)o);
						} else {
							sb.append(o);
						}
					}
				}
			}
		
		}
		
		return sb.toString().getBytes();
	}
}

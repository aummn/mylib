package com.mssint.jclib;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Arrays;
import java.util.HashSet;

import com.mssint.jclib.OdtClient.MsgType;

import java.util.Random;

import javafx.scene.control.Separator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Util {
	private static final Logger log = LoggerFactory.getLogger(Util.class);

	private static String PID;
	
	private static short [] ascii2ebcdic = null;
	private static short [] ebcdic2ascii = null;
	
	static {
		RuntimeMXBean rtb = ManagementFactory.getRuntimeMXBean();   
		PID = rtb.getName();
		int i = PID.indexOf("@");
		if(i > 0) PID = PID.substring(0, i);
		
		//Setup translation tables.
		Translate trans = new Translate();
		ascii2ebcdic = trans.getAscii2Ebcdic();
		ebcdic2ascii = trans.getEbcdic2Ascii();
	}
	public static String getPid() { return PID; }

	public static int stringToInt(String s) {
		if(s == null) return(0);
		try {
			return Integer.parseInt(s);
		} catch (Exception e) {
			return 0;
		}
	}
	
	public static String format(String fmt, int val) { return format(fmt, (double)val); }   
	public static String format(String fmt, long val) { return format(fmt, (double)val); }   
	public static String format(String fmt, double val) {
		if(fmt == null || fmt.length() == 0) fmt = "0";
		NumberFormat formatter = new DecimalFormat(fmt);
		return formatter.format(val);
	}
	
	public static String parentOf(String path) {
		File f = new File(path);
		String s = f.getParent();
		if(s == null)
			return "." + File.separator;
		return s;
	}

	/**
	 * Returns a String representation of the first character of @entry, escaped if 
	 * necessary, in a form which can be used as a regex.
	 * @param entry The String to get the first character from.
	 * @return A regex compliant String
	 */
	public static String getDelimiter(String entry) {
		if(entry == null || entry.length() < 1)
			return "\\^";
		char delChar = entry.charAt(0);
		String delimiter;
		if(delChar != 0) {
			switch(delChar) {
			case '\\': delimiter = "\\\\"; break;
			case '^': delimiter = "\\^"; break;
			case '[': delimiter = "\\["; break;
			case ']': delimiter = "\\]"; break;
			case '(': delimiter = "\\("; break;
			case ')': delimiter = "\\)"; break;
			case '$': delimiter = "\\$"; break;
			default: delimiter = Character.toString(delChar);
			}
		} else return "\\^";
		return delimiter;
	}


	public static String dirName(String path) {
		File f = new File(path);
		String s = f.getParent();
		if(s == null)
			return "." + File.separator;
		return s;
	}
	
	public static String formatHex(byte [] s) {
		if(s == null) return "";
		return formatHex(s, s.length);
	}
	
	public static String formatHex(byte [] s, int len) {
		final char [] hexCharacters = GroupByteArray.hexCharacters;
		if(s == null || len < 1) return "";
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < len; i++) {
			sb.append(hexCharacters[((s[i] >> 4) & 0x0f)]);
			sb.append(hexCharacters[(s[i] & 0x0f)]);
		}
		return sb.toString();
	}
	
	public static String baseName(String path) {
		File f = new File(path);
		return f.getName();
	}
	
	public static void mkdir(String path) {
		File f = new File(path);
		f.mkdirs();
	}
	
	public static String logIndex() {
	    return new SimpleDateFormat("yyyy/MM/dd HH:mm ").format(SystemDate.getDate()) +
			PID + ": ";
	}

	public static void log(String msg) {
		if(msg == null) msg = "";
	    log.info(logIndex() + msg);
	}
	public static void info(String msg) {
		if(msg == null) msg = "";
		if(log.isDebugEnabled())
			log.debug(logIndex() + "INFO: " + msg);
	}

	public static void error(String msg) {
		if(msg == null) return;
		log.error(logIndex() + "ERROR: " + msg);
	}
	
	public static void warning(String msg) {
		if(msg == null) msg = "";
		log.info("WARNING: " + msg);
	}

	public static void debug(String msg) {
		if(msg == null) msg = "";
		if(log.isDebugEnabled())
			log.debug(logIndex() + "DEBUG: " + msg);
	}

	public static void message(String msg) {
		if(msg == null) msg = "";
		log.info(logIndex() + msg);
	}

	/**
	 * Copies at most len characters from src to sb. The copy ends when 'len' characters have been
	 * copied or else if the character indicated in 'tchar' is reached. The tchar character is
	 * not copied.
	 * @param sb The destination for the characters copied. Characters are appended  to sb.
	 * @param src The string to copy from.
	 * @param fromIndex The starting position (zero relative) in the src string.
	 * @param len The maximum number of characters to copy.
	 * @param tchar The terminating character.
	 * @return The number of characters copied.
	 */
	public static int replace(StringBuilder sb, String src, int fromIndex, int len, char tchar) {
		sb.delete(0, sb.length());
	    if(src.length() < fromIndex) return 0;
	    if(len > (src.length() - fromIndex)) len = src.length() - fromIndex;
	    
		int tlen = src.substring(fromIndex, fromIndex+len).indexOf(tchar);
	    if(tlen == -1) tlen = len;
	    sb.append(src.substring(fromIndex, fromIndex + tlen));
	    return tlen;
	}

	/**
	 * Copies at most len characters from src to sb. The copy ends when 'len' characters have been
	 * copied or else if the character indicated in 'tchar' is reached. The tchar character is
	 * not copied.
	 * @param sb The destination for the characters copied. Characters are appended  to sb.
	 * @param src The string to copy from.
	 * @param fromIndex The starting position (zero relative) in the src string.
	 * @param len The maximum number of characters to copy.
	 * @param tchar The terminating character.
	 * @param appendChar Appended to end of destination string.
	 * @return The number of characters copied (tchar is counted but not copied).
	 */
	public static int replace(StringBuilder sb, String src, int fromIndex, int len, 
			char tchar, char appendChar) {
		sb.delete(0, sb.length());
	    if(src.length() < fromIndex) return 0;
	    if(len > (src.length() - fromIndex)) len = src.length() - fromIndex;
	    int extraChar = 0;
		int tlen = src.substring(fromIndex, fromIndex+len).indexOf(tchar);
	    if(tlen == -1) tlen = len;
	    else extraChar = 1;
	    sb.append(src.substring(fromIndex, fromIndex + tlen));
	    sb.append(appendChar);
	    return tlen + extraChar;
	}
	
	public static String replace(String destString, int destPos, String srcStr,int len) {
		String fmt = "%-" + destPos + "." + destPos + "s%-" + len + "." + len + "s%s";
		String estr = (destString.length() > (destPos + len)) ? 
				destString.substring(destPos+len) : "";
		return String.format(fmt, destString, srcStr, estr);
	}

	public static String replace(String destString, int destPos, Var srcStr,int len) {
		String fmt = "%-" + destPos + "." + destPos + "s%-" + len + "." + len + "s%s";
		String estr = (destString.length() > (destPos + len)) ? 
				destString.substring(destPos+len) : "";
		return String.format(fmt, destString, srcStr.toString(), estr);
	}
	
	public static void abort(String msg) {
		if(msg == null) 
			log.error(logIndex() + "ABORT: Unspecified abort." );
		else
			log.error(logIndex() + "ABORT: " + msg);
	    log.error(logIndex() + "The program has aborted.");
		Thread.dumpStack();
		
		System.exit(27);;
	}
	/**
	* Reallocates an array with a new size, and copies the contents
	* of the old array to the new array.
	* @param oldArray  the old array, to be reallocated.
	* @param newSize   the new array size.
	* @return          A new array with the same contents.
	*/
	public static Object resizeArray (Object oldArray, int newSize) {
	   int oldSize = java.lang.reflect.Array.getLength(oldArray);
	   Class<?> elementType = oldArray.getClass().getComponentType();
	   Object newArray = java.lang.reflect.Array.newInstance(
	         elementType,newSize);
	   int preserveLength = Math.min(oldSize,newSize);
	   if (preserveLength > 0)
	      System.arraycopy (oldArray,0,newArray,0,preserveLength);
	   return newArray;
	}
	
	/**
	 * Displays a prompt on stdout or the odtmon monitor and waits for keyboard input or else a
	 * response from the monitor.
	 * Relies on configuration to decide which. If the property "jclib.odtmon.server" is set then
	 * that will be used. If the monitor is not running then an exception will be thrown.
	 * @param msg The text to use as a prompt. This is suffixed by "AX> " on the next line. If
	 * msg is null, the string "AX> " will appear by itself.
	 * @return The String typed by the user.
	 * @throws IOException
	 */
	public static String prompt(String msg) throws IOException {
		String response;
		if(Config.odtConnect()) {
			Config.odtClient.sendMessage(MsgType.WAITING_ENTRY, msg);
			response = Config.odtClient.recvMessage();
		} else {
			if(msg == null) System.out.print("AX> ");
			else System.out.print(msg + "\nAX> ");

			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			try {
				response = br.readLine();
				System.out.println("Entered> " + response);
			} catch(IOException ioe) {
				log.error("IO error trying to ACCEPT!",ioe);
				throw ioe;
			}
		}
		return response;
	}
	
	public static String waitForFile(String file) throws IOException {
		String newFile;
		File fd = new File(file);
		while(!fd.exists()) {
			if(Config.odtConnect()) {
				Config.odtClient.sendMessage(MsgType.WAITING_ENTRY, "Missing file: " + file);
				newFile = Config.odtClient.recvMessage();
			} else {
				System.out.print("Missing file: " + file + "\nAX> ");

				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				try {
					newFile = br.readLine();
					System.out.println("Entered> " + newFile);
				} catch(IOException ioe) {
					log.error("IO error trying to ACCEPT!", ioe);
					throw ioe;
				}
			}
			if(newFile != null && newFile.trim().length() > 0) {
				file = newFile;
				fd = new File(file);
			}
		}
		return file;
	}
	
	public static void display(String msg) throws IOException {
		if(Config.odtConnect()) {
			Config.odtClient.sendMessage(MsgType.DISPLAY, msg);
		} else {
			if(msg == null) System.out.print("MESSAGE> ");
			else System.out.print("MESSAGE> " + msg);
		}
	}
	public static void accept(Var dest) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		try {
			System.out.print("AX>");
			dest.set(br.readLine());
			System.out.println("Entered>" + dest.getString());;
		} catch(IOException ioe) {
			log.error("IO error", ioe);
			throw ioe;
		}
		return;
	}
	
	private static boolean staticInitialised = false;
	private static String  defaultFilepath = null;

	private static void initialiseStatics() {
		if(staticInitialised) return;
		defaultFilepath = Config.getProperty("jclib.default.filepath");
		if(defaultFilepath != null) { 
			if(defaultFilepath.charAt(defaultFilepath.length() - 1) != '/')
				defaultFilepath += "/";
		}

		staticInitialised = true;
	}
	
	public static String defaultPathname() {
		initialiseStatics();
		return defaultFilepath;
	}
	
	public static String pathName(String path) {
		initialiseStatics();
		if(defaultFilepath == null) return path;
		if(path.charAt(0) == '/' || path.charAt(0) == File.separatorChar || 
				(path.length() > 1 && path.charAt(1) == ':')) return path;
		return defaultFilepath + path;
	}
	
	public static String [][] listDir(String path,String format, boolean recurse) {
		String full = pathName(path.trim());
		int cutfrom = full.length() - path.length();
		//log.debug("path='"+path+" full="+full+ " cutfrom="+cutfrom);
		File dir = new File(Util.pathName(path.trim()));
		if(!dir.exists()) {
			String [][] dirList = new String[0][2];
			return dirList;
		}
		String[] children;
		ArrayList<String> files = new ArrayList<String>();
		if(recurse) {
			recurseDirectories(files, dir);
		} else {
			if(dir.isDirectory()) {
				children = dir.list();
	            for (int i=0; i<children.length; i++) {
	            	File f = new File(Util.pathName(children[i]));
	            	if(!f.isDirectory()) files.add(children[i]);
	            }
			} else files.add(Util.pathName(path.trim()));
		}
		if(!recurse) cutfrom = 0;
		String [][] dirList = new String[files.size()][2];
		try {
			for(int i = 0; i < files.size(); i++) {
				String file = files.get(i);
				File fil = new File(file);
				dirList[i][0] = file.substring(cutfrom);
				SimpleDateFormat sdf = new SimpleDateFormat(format);
				Date d = new Date(fil.lastModified());
				dirList[i][1] = sdf.format(d);
				//System.out.println(dirList[i][0]);
			}
		} catch (NullPointerException e){
		}
		return dirList;
	}
	
    
    private static void recurseDirectories(ArrayList<String>files, File dir) {
    	if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i=0; i<children.length; i++) {
            	recurseDirectories(files, new File(dir, children[i]));
            }
        } else {
            files.add(dir.getPath());
        }
    }
    
    public static String trimLeft(String str) {
    	for(int i=0; i<str.length();i++) {
    		if(!Character.isWhitespace(str.charAt(i))) {
    			return str.substring(i);
    		}
    	}
    	return "";
    }
    public static String trimRight(String str) {
    	for(int i=str.length() - 1;i >= 0;i--) {
    		if(!Character.isWhitespace(str.charAt(i))) {
    			return str.substring(0, i+1);
    		}
    	}
    	return "";
    }

	/**
	 * Translate a Unisys MCP style filename to Unix.
	 * @param mcp The Unisys filename in form "(USER)DIR/NAME ON DISK"
	 * @return The Unix name in form "DIR/NAME"
	 */
	public static String unixFilename(String mcp) {
		String s = mcp.toString();
		int i = s.indexOf(')');
		if(i != -1) s = s.substring(i+1);
		i = s.indexOf(' ');
		if(i != -1) s = s.substring(0, i);
		
		if(File.separatorChar == '\\') {
			s.replaceAll("/", "\\\\");
		}
		
		return s;
	}
	/**
	 * Translate a Unisys MCP style filename to Unix.
	 * @param mcp The Unisys filename in form "(USER)DIR/NAME ON DISK"
	 * @return The Unix name in form "DIR/NAME"
	 */
	public static String unixFilename(Var mcp) {
		return unixFilename(mcp.getString().trim());
	}
	
	public static int parseInt(String s, int start, int len) {
		if(s == null || start < 0 || start >= s.length() || len <= start) return 0;
		if((start+len) > s.length()) len = s.length() - start;
		s = s.substring(start, start + len);
		return parseInt(s);
	}
	public static int parseInt(String s, int len) {
		if(s == null || len < 0) return 0;
		if(len > s.length()) len = s.length();
		s = s.substring(0, len);
		return parseInt(s);
	}
	public static int parseInt(String s) {
		try {
			return Integer.parseInt(s);
		} catch(Exception e) {
			return 0;
		}
	}
	
	public static int asciiBytesToInt(byte [] b, int start, int end) {
		int val = 0;
		int mult = 1;
		for(int i=end-1;i>=start;i--) {
			val += (b[i] - '0') * mult;
			mult += 10;
		}
		return val;
	}
	
	
	/*static String url_ = "localhost:1099";
	//static String name_ = "queue/example";
	static QueueConnection conn = null;
	static QueueSession session = null;
	static Queue queue = null;

	public static void wakeUp (String name) throws Exception {
		Properties props = new Properties();
		props.setProperty("java.naming.factory.initial","org.jnp.interfaces.NamingContextFactory");
		props.setProperty("java.naming.factory.url.pkgs", "org.jboss.naming");
		props.setProperty("java.naming.provider.url", url_);

		Context context = new InitialContext(props);

		QueueConnectionFactory tcf = (QueueConnectionFactory) context.lookup("ConnectionFactory");
		conn = tcf.createQueueConnection();
		//queue = (Queue) context.lookup(name_); //static queue
		System.out.println("1");
		session = conn.createQueueSession(false, QueueSession.AUTO_ACKNOWLEDGE);
		System.out.println("wu name=" + name);
		queue = session.createQueue(name); //Dynamic queue
		System.out.println("2");
		conn.start();
		System.out.println("3");
	    QueueSender send = session.createSender(queue);
	    TextMessage tm = session.createTextMessage("xxxx");
	    send.send(tm);
	    send.close();
	    
		if(conn != null) {
			conn.stop();
		}

		if(session != null) {
			session.close();
		}

		if(conn != null) {
			conn.close();
		}
	}
	public static class Listener implements MessageListener {

		String url_;
		String name_;
		QueueConnection conn = null;
	    	QueueSession session = null;
	    	Queue queue = null;

	    	public Listener(String url, String name) {
	    		super();

	    		url_ = url;
	    		name_ = name;

	    		try {
	    			this.initializeListener();
	    		} catch (Exception e) {
	    			System.out.println("Error creating listener: " + e);
				e.printStackTrace();
			}

		}

		public void onMessage(Message msg) {

			TextMessage tm = (TextMessage) msg;

			try {
				System.out.println("Incoming message: " + tm.getText());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		private void initializeListener() throws JMSException, NamingException {

			Properties props = new Properties();
			props.setProperty("java.naming.factory.initial","org.jnp.interfaces.NamingContextFactory");
			props.setProperty("java.naming.factory.url.pkgs", "org.jboss.naming");
			props.setProperty("java.naming.provider.url", url_);

			Context context = new InitialContext(props);
			System.out.println("performing lookup...");

			Object tmp = context.lookup("ConnectionFactory");
			System.out.println("lookup completed, making queue");

			QueueConnectionFactory tcf = (QueueConnectionFactory) tmp;
			conn = tcf.createQueueConnection();
			//queue = (Queue) context.lookup(name_); //static queue
			System.out.println("After start");
			session = conn.createQueueSession(false, QueueSession.AUTO_ACKNOWLEDGE);
			System.out.println("1-" + name_);
			queue = session.createTemporaryQueue(name_); //Dynamic queue
			System.out.println("2");
			conn.start();
			System.out.println("3");

			QueueReceiver recv = session.createReceiver(queue);
			recv.setMessageListener(this);
		}


		public void disconnect() throws JMSException {
			if(conn != null) {
				conn.stop();
			}

			if(session != null) {
				session.close();
			}

			if(conn != null) {
				conn.close();
			}
		}
	}

	

	public static void sleepUntilWoken (String name) throws Exception {
			Listener listener = new Listener(url_,name);
			try {
				Thread.sleep(60000);
			} catch(Exception e) {
				System.out.println("Error sleeping: " + e);
				e.printStackTrace();
			}

			listener.disconnect();
	}*/
	private static StringBuilder spaceArray = new StringBuilder(); 
	public static String spaces(int len) {
		if(spaceArray.length() < len) {
			int newlen = ((len / 1024)*1024)+1024;
			spaceArray.append(String.format("%"+newlen+"s", " "));
		}
		return spaceArray.substring(0, len);
	}

	private static StringBuilder zeroArray = new StringBuilder(); 
	public static String zeroes(int len) {
		if(zeroArray.length() < len) {
			int newlen = ((len / 1024)*1024)+1024;
			for(int i=zeroArray.length(); i<newlen; i += 16)
				zeroArray.append("0000000000000000");
		}
		return zeroArray.substring(0, len);
	}
	
	/**
	 * Return cpu time an integer, in 60th's of a second (ala Unisys MCP)
	 * @return
	 */
	public static int mcpProcessorTime() {
		return 0; //TODO - get actual CPU time
	}
	
	/**
	 * Displays a prompt "AX> " on stdout and waits for keyboard input.
	 * @return The String typed by the user.
	 * @throws IOException
	 */
	public static String prompt() throws IOException {
		return prompt(null);
	}
	/**
	 * Compares two strings, limited by starting position and length, and returns -1 is s1 is
	 * less than s2, 0 if equal otherwise 1.
	 * @param s1 The first string for comparison.
	 * @param start1 The starting position in the first string.
	 * @param len1 The length of the string s1 for comparison purposes.
	 * @param s2 The secondstring for comparison.
	 * @param start2 The starting position in the second string.
	 * @param len2 The length of the string s2 for comparison purposes.
	 * @return -1, 0 or 1 if s1 < s2, s1 == s2 or s1 > s2
	 */
	public static int compare(String s1, int start1, int len1, String s2, int start2, int len2) {
		if(s1 == null && s2 == null) return 0;
		else if(s1 == null) return 1;
		else if(s2 == null) return -1;
		
		String str1, str2;
		if(s1.length() < start1) str1 = "";
		else str1 = s1.substring(start1).trim();
		if(str1.length() > len1) str1 = str1.substring(0, len1);
		
		if(s2.length() < start2) str2 = "";
		else str2 = s2.substring(start2).trim();
		if(str2.length() > len2) str2 = str2.substring(0, len2);
		
		return str1.compareTo(str2);
	}
	
	/**
	 * Compares two strings, ignoring trailing spaces, and for a maximum length.
	 * @param s1 The first string
	 * @param s2 The second string
	 * @param maxlen The maximum length for comparison
	 * @return -1, 0 or 1 if s1 < s2, s1 == s2 or s1 > s2
	 */
	public static int compare(String s1, String s2, int maxlen) {
		if(s1 == null && s2 == null) return 0;
		else if(s1 == null) return 1;
		else if(s2 == null) return -1;
		
		if(s1.length() > maxlen) s1 = s1.substring(0, maxlen);
		if(s2.length() > maxlen) s2 = s2.substring(0, maxlen);
		return s1.trim().compareTo(s2.trim());
	}
	
	/**
	 * Returns a string guaranteed to be the length given by width. If str is too
	 * short or null it will be padded with spaces, or truncated if too long.
	 * @param str The string to format.
	 * @param width The length of the returned string.
	 * @return The string set to the correct length.
	 */
	public static String pad(String str, int width) {
		return String.format("%-"+width+"."+width+"s", str == null ? " " : str);
	}
	

	public static String ascii2ebcdic(String buf) {
		StringBuilder s = new StringBuilder();
		for(int i=0; i < buf.length(); i++) {
			int c = buf.charAt(i);
			s.append((char)ascii2ebcdic[c & 0xff]);
		}
		return s.toString();
	}

	public static String ebcdic2ascii(String buf) {
		if(buf == null) return null;
		StringBuilder s = new StringBuilder();
		for(int i=0; i < buf.length(); i++) {
			s.append((char)ebcdic2ascii[buf.charAt(i)&0xff]);
		}
		return s.toString();
	}
	public static void ascii2ebcdic(byte [] buf, int len) {
		if(len > buf.length) len = buf.length;
		for(int i=0; i < len; i++) {
			buf[i] = (byte)ascii2ebcdic[buf[i]&0xff];
		}
	}

	public static void ebcdic2ascii(byte [] buf, int len) {
		if(len > buf.length) len = buf.length;
		for(int i=0; i < len; i++) {
			buf[i] = (byte)ebcdic2ascii[buf[i] & 0xff];
		}
	}
	public static char ebcdic2ascii(byte b) {
		return (char)ebcdic2ascii[b & 0xff];
	}
	
	public static String newString(byte [] b) { return newString(b, b.length); }
	public static String newString(byte [] b, int len) {
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<len; i++)
			sb.append((char)(b[i] & 0xff));
		return sb.toString();
	}
	
	public static byte [] getBytes(String s) {
		byte [] b = new byte[s.length()];
		for(int i=0; i<s.length(); i++)
			b[i] = (byte)s.charAt(i);
		return b;
	}
	
	static Random randomGenerator = null;

	public static double getRandom (long seed){
		randomGenerator = new Random(seed);
		return  getRandom();
	}
	public static double getRandom (){
		if (randomGenerator == null) {
			randomGenerator = new Random();
		}
		return randomGenerator.nextDouble();
	}
	public static String getBaseName() {
		Throwable t = new Throwable();
	    StackTraceElement ste = t.getStackTrace()[t.getStackTrace().length - 1];
	    return(ste.getFileName());
	}
	
	public static Var currentDate() {
		Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSZ");
        String date=dateFormat.format(calendar.getTime());
        return(new Var(date.substring(0, 16) + date.substring(17)));
	}
	public static int getLineNumber() {
		int lineNumber=new Exception().getStackTrace()[1].getLineNumber();
		return lineNumber;
	}
	
	public static int compileDate() {
		return 0;
	}
	/**
	 * Gets the Var (or Group) object of an N-dimensional array.
	 * @param array The array, which must be of type Var or Group
	 * @param index One or more indexes into the array.
	 */
	public static Var getArray(Object array,int ... index) {
		int dim = 0;
	    Class<?> cls = array.getClass();
	    Object deref = array;
	    Object obj0 = array;
	    boolean boundsError = false;

	    while (cls.isArray()) {
	    	int idx;
	    	if(dim < index.length) idx = index[dim];
	    	else idx = 0;
	    	//if index is out of bounds, check for previous value
	    	if(idx < 1 || idx >= Array.getLength(deref)) {
	    		boundsError = true;
	    		idx = 0;
	    	}
	    	obj0 = Array.get(obj0, 0);
	    	deref = Array.get(deref, idx);
	        dim++;
	        cls = cls.getComponentType();
	    }
        if(index.length < dim)
        	throw new IllegalArgumentException("Only "+index.length+" indeces specified. The array requires "+dim+" indeces." );
	    if(deref instanceof Var) {
	    	if(boundsError) {
	    		return (Var)obj0;
	    	} else {
	    		((Var)obj0).set((Var)deref);
	    		return (Var)deref;
	    	}
	    } else
	    	throw new IllegalArgumentException("'array' must be an array of type Var or Group");
	}
	
	/**
	 * Sets the 'src' value into the correct element of an N-dimensional array.
	 * @param src The data item to set
	 * @param array The array, which must be of type Var or Group
	 * @param index One or more indexes into the array.
	 */
    public static void setArray(Var src, Object array, int ... index) {
		int dim = 0;
	    Class<?> cls = array.getClass();
	    Object deref = array;
	    Object obj0 = array;
	    while (cls.isArray()) {
	    	if(index[dim] < 1 || index[dim] >= Array.getLength(deref))
	    		return;
	    	if(dim < index.length)
	    		deref = Array.get(deref, index[dim]);
	    	else return; //not enough indexes
	        dim++;
	        obj0 = Array.get(obj0, 0);
	        cls = cls.getComponentType();
	    }
	    if(deref instanceof Var) {
	    	((Var)deref).set(src);
	    	((Var)obj0).set((Var)deref);
    	} else
	    	throw new IllegalArgumentException("'array' must be an array of type Var or Group");
	}
    
    public static void setArray(long src, Object array, int ... index) {
    	setArray(new Var(src), array, index);
    }
    public static void setArray(String src, Object array, int ... index) {
    	setArray(new Var(src), array, index);
    }
    public static void setArray(double src, Object array, int ... index) {
    	setArray(new Var(src), array, index);
    }
    
    /**
	 * clears the array.
	 */
    public static void clearArray(Object array) {
		int dim = 0;
		int [] maxIndex = new int [3];
	    Class<?> cls = array.getClass();
	    Object deref = array;
	    while (cls.isArray()) {
	    	maxIndex[dim]=Array.getLength(deref);
	    	deref = Array.get(deref, 0);
	        dim++;
	        cls = cls.getComponentType();
	    }
	    if(deref instanceof Var) {
	    	Object row = array;
	    	Object dim2 = array;
	    	switch (dim) {
	    	case 1:
	    		for(int i = 0 ;i < maxIndex[0]; i++) {
	    			deref = Array.get(array, i);
	    			((Var)deref).clear();
	    		}
	    		break;
	    	case 2:
	    		for(int i = 0 ;i < maxIndex[0]; i++) {
	    			row = Array.get(array, i);
	    			for(int j = 0 ;j < maxIndex[1]; j++) {
	    				deref = Array.get(row, j);
	    				((Var)deref).clear();
	    			}
	    		}
	    		break;
	    	case 3:
	    		for(int i = 0 ;i < maxIndex[0]; i++) {
	    			dim2 = Array.get(array, i);
	    			for(int j = 0 ;j < maxIndex[1]; j++) {
	    				row = Array.get(dim2, j);
	    				for(int k = 0 ;k < maxIndex[2]; k++) {
	    					deref = Array.get(row, k);
	    					((Var)deref).clear();
	    				}
	    			}
	    		}
	    		break;
	    	default:
	    		throw new IllegalArgumentException("clearArray only caters for max 3 dimensions");
	    	}
    	} else
	    	throw new IllegalArgumentException("'array' must be an array of type Var or Group");
	}
    /**
	 * Writes to a file
	 * @param fileName The name of the file
	 * @param line The line to write
	 */
    public static int writeFile(String fileName, String line) {
    	String extractPath = System.getenv("EXTRACT_DIR");
		if (extractPath == null)
			extractPath = Config.getProperty("jclib.extract.dir");
		if (extractPath == null)
			extractPath = ".";
    	try{
    		FileWriter fstream = new FileWriter(extractPath + File.separator + fileName,true);
    		BufferedWriter out = new BufferedWriter(fstream);
    		out.write(line);
    		out.close();
    	}catch (Exception e){//Catch exception if any
    		log.error("Error: " + e.getMessage());
    		return -1;
    	}
    	return 0;
    }
    /**
	 * Writes to a file
	 * @param fileName The name of the file
	 * @param line The line to write
	 */
    public static int writeFile(String fileName, Var line) {
        return writeFile(fileName, line.getString());
    }
    /**
	 * Writes to a file
	 * @param fileName The name of the file
	 * @param line The line to write
	 */
    public static int writeFile(Var fileName, String line) {
        return writeFile(fileName.getString(), line);
    }
    /**
	 * Writes to a file
	 * @param fileName The name of the file
	 * @param line The line to write
	 */
    public static int writeFile(Var fileName, Var line) {
        return writeFile(fileName.getString(), line.getString());
    }
    
    public static void callLib(Var lib, Var... args)
    {  
    	lib.set(Thread.currentThread().getStackTrace()[2].getClassName().substring(0,Thread.currentThread().getStackTrace()[2].getClassName().lastIndexOf('.') + 1) + lib.toString().substring(0, 1).toUpperCase() + lib.toString().substring(1).toLowerCase());
    	//lib.set(lib.toString().substring(0, 1).toUpperCase() + lib.toString().substring(1).toLowerCase());
    	System.out.println("Calling:" + lib + "<");
    	ClassLoader cl = ClassLoader.getSystemClassLoader();
    	 
        URL[] urls = ((URLClassLoader)cl).getURLs();
 
        for(URL url: urls){
        	System.out.println(url.getFile());
        }
    	Class c = null;
    	try
        {
        	c = Class.forName("Sylink");
        	System.out.println("after Sylink");
        }catch(Exception e) {
            System.out.println(e.getMessage());  
            e.printStackTrace();  
            //System.exit(1);
        }
    	
        try
        {
        	c = Class.forName(lib.toString());
        }catch(Exception e) {
            System.out.println(e.getMessage());  
            e.printStackTrace();  
            System.exit(1);
        }
        String[] paramData = new String[args.length];  
        for (int i=0; i<args.length; i++) {
        	paramData[i] = new String(args[i].getString());
        }
        Object[] pargs = new Object[1];
        pargs[0] = paramData;
        try  
        {  
            Method m = c.getMethod("callCob", String[].class);  
            m.invoke(null, pargs);  
        }catch(Exception e) {  
            System.out.println(e.getMessage());  
            e.printStackTrace();  
        }  
    }
    
    /**
	 * Renames a file
	 * @param oldFile The old name of the file
	 * @param newFile The new name of the file
	 * @return 0 = Success -1 = Fail
	 */
    public static int renameFile (String oldFile, String newFile) {
    	File oldfile =new File(oldFile.trim());
		File newfile =new File(newFile.trim());
 
		if(oldfile.renameTo(newfile)){
			return 0;
		}else{
			return -1;
		}
    }
    /**
	 * Renames a file
	 * @param oldFile The old name of the file
	 * @param newFile The new name of the file
	 * @return 0 = Success -1 = Fail
	 */
    public static int renameFile (Group oldFile, Group newFile) {
    	return renameFile(oldFile.getString(), newFile.getString());
    }
    /**
	 * Renames a file
	 * @param oldFile The old name of the file
	 * @param newFile The new name of the file
	 * @return 0 = Success -1 = Fail
	 */
    public static int renameFile (Var oldFile, String newFile) {
    	return renameFile(oldFile.getString(), newFile);
    }
    /**
	 * Renames a file
	 * @param oldFile The old name of the file
	 * @param newFile The new name of the file
	 * @return 0 = Success -1 = Fail
	 */
    public static int renameFile (String oldFile, Var newFile) {
    	return renameFile(oldFile, newFile.getString());
    }
    

    /**
     * Create a byte array which is a substring of the input. The new array
     * will contain the characters starting from 'from' and ending with the
     * byte before 'to'.
     * @param val
     * @param from
     * @param to
     * @return
     */
    public static byte [] byteSubstring(byte [] val, int from, int to) {
    	byte [] b = new byte [to - from];
    	System.arraycopy(val, from, b, 0, to - from);
    	return b;
    }

    public static byte [] byteSubstring(byte [] val, int from) {
    	byte [] b = new byte [val.length - from];
    	System.arraycopy(val, from, b, 0, val.length - from);
    	return b;
    }

    public static int byteIndexOf(byte[] b, char c) {
    	for(int i=0; i<b.length; i++) {
    		if(b[i] == c)
    			return i;
    	}
    	return -1;
    }
    /**
	 * javaise a class name
	 * @param psrc class name (case irrelevant)
	 * @return javaised class name
	 */
    public static String javaise (Var psrc) {
    	return javaise(psrc.getString());
    }
    /**
	 * javaise a class name
	 * @param psrc class name (case irrelevant)
	 * @return javaised class name
	 */
    public static String javaise (String psrc) {
    	StringBuilder src = new StringBuilder(psrc);
    	int i;
    	for(i=0;i<src.length();i++)  {
    		if(src.charAt(i) == '_' || src.charAt(i) == '-' || src.charAt(i) == '.') {
    			src.deleteCharAt(i);
    			if(i < src.length()) src.setCharAt(i, Character.toUpperCase(src.charAt(i)));
    		} else if(Character.isDigit(src.charAt(i))) {
    			while(i < src.length() && Character.isDigit(src.charAt(i))) i++;
    			if(i < src.length()) src.setCharAt(i, Character.toUpperCase(src.charAt(i)));
    		} else src.setCharAt(i, Character.toLowerCase(src.charAt(i)));
    	}
    	src.setCharAt(0, Character.toUpperCase(src.charAt(0)));
    	return src.toString();
    }
    
    /**
	 * Get the max of a number of ints 
	 * @param ints
	 * @return max
	 */
    
    public static int getMax(int... values)
    {
       int largest = Integer.MIN_VALUE;
       for (int v : values) if (v > largest) largest = v;
       return largest;
    }

	public static boolean isNumeric(String val) {
		if(val == null || val.length() == 0) {
			return false;
		}
		for(int i=0; i<val.length();i++) {
			if(!Character.isDigit(val.charAt(i))) {
				return false;
			}
		}
		return true;
	}
	public static String[] reservedWordsA={"BASIS",
			                 "TIME",
			                 "PRINT",
			                 "BASIS",
			                 "DATA",
			                 "NAME",
			                 "MANUAL",
			                 "REFERENCE",
			                 "TRANNO",
			                 "LAST_LINE"};
	
	public static HashSet<String> reservedWords = new HashSet(Arrays.asList(reservedWordsA));
	public static String fixSqlServerReserved(String name) {
		
		if(reservedWords.contains(name))
			name = "X" + name;
		return "[" + name + "]";
	}
	
	
	public static String getFileName(String ... parts) {
		StringBuilder sb = new StringBuilder();
		for(String p : parts) {
			p = p.trim();
			if(File.separatorChar == '\\') {
				p = p.replaceAll("/", "\\\\");
			} else {
				p = p.replaceAll("\\\\", "/");
			}
			if(sb.length() == 0) {
				sb.append(p);
			} else if(new File(p).isAbsolute() ||
					(File.separatorChar == '\\' && p.length() > 1 && p.charAt(1) == ':')) {
				sb.delete(0, sb.length());
				sb.append(p);
			} else {
				if(sb.charAt(sb.length()-1) != File.separatorChar) {
					sb.append(File.separator);
				}
				if(p.charAt(0) == File.separatorChar) {
					sb.append(p.substring(1));
				} else {
					sb.append(p);
				}
			}
		}
		
		return sb.toString();
	}
	
	
	public static void main(String [] args) {
		String [] ax = {
				".",
				"/",
				"c:\\usr\\bin",
				"c:\\usr\\bin\\",
				"c:/usr/bin",
				"\\\\azuer\\usr\\bin"
		};
		String [] ay = {
				"apple",
				"/usr/bin/apple",
				"C:/usr/bin/apple",
				"c:\\usr\\bin",
				"c:",
				"\\\\drive\\us\\bin"
		};

		for(String x : ax) {
			for(String y : ay) {
				String s = getFileName(x, y);
				String parent = parentOf(s);
				String base = baseName(s);
				System.out.printf("%s  %s: [%s] parent=%s base=%s\n", x, y, s, parent, base);
			}
		}
	}

	/**
	 * Returns time as a date string of the form "yyyy-mm-dd hh:mm:ss"
	 * @param time
	 * @return
	 */
	public static String getLogDate(long time) {
		final Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(time);
		
		return String.format("%04d-%02d-%02d %02d:%02d:%02d",
				calendar.get(Calendar.YEAR),
				calendar.get(Calendar.MONTH) + 1,
				calendar.get(Calendar.DAY_OF_MONTH),
				calendar.get(Calendar.HOUR_OF_DAY),
				calendar.get(Calendar.MINUTE),
				calendar.get(Calendar.SECOND)
		);
	}

	/**
	 * Returns current time as a date string of the form "yyyy-mm-dd hh:mm:ss"
	 * @param time
	 * @return
	 */
	public static String getLogDate() {
		return getLogDate(System.currentTimeMillis());
	}
	
	/**
	 * Returns time as a date string of the form "hh:mm:ss.nnn"
	 * @param time
	 * @return
	 */
	public static String getLogTime(long time) {
		final Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(time);
		
		return String.format("%02d:%02d:%02d.%03d",
				calendar.get(Calendar.HOUR_OF_DAY),
				calendar.get(Calendar.MINUTE),
				calendar.get(Calendar.SECOND),
				calendar.get(Calendar.MILLISECOND)
		);
	}

	/**
	 * Returns current time as a date string of the form "hh:mm:ss.nnn"
	 * @return
	 */
	public static String getLogTime() {
		return getLogTime(System.currentTimeMillis());
	}
	
	/**
	 * Returns time as a date string of the form "yyyymmdd.hhmmss.nnn"
	 * @param time
	 * @return
	 */
	public static String getTimeStamp(long time) {
		final Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(time);
		
		return String.format("%04d%02d%02d.%02d%02d%02d.%03d",
				calendar.get(Calendar.YEAR),
				calendar.get(Calendar.MONTH) + 1,
				calendar.get(Calendar.DAY_OF_MONTH),
				calendar.get(Calendar.HOUR_OF_DAY),
				calendar.get(Calendar.MINUTE),
				calendar.get(Calendar.SECOND),
				calendar.get(Calendar.MILLISECOND)
		);
	}
	/**
	 * Returns current time as a date string of the form "yyyymmdd.hhmmss.nnn"
	 * @return
	 */
	public static String getTimeStamp() {
		return getTimeStamp(System.currentTimeMillis());
	}
	/*
	 * Sets the machine name for gpc
	 */
	public static String getGpcHost () {
		String computerName=System.getenv("COMPUTERNAME");
        if (computerName == null)
            computerName="unknown";
        switch (computerName.toLowerCase()) {
        case "gldaplincdev":
            return("DEVDB");
        case "gldaplinctest":
        	return("TESTDB");
        case "gldaplinc01":
        	return("GPCDB");
        default:
        	return("unknown");
        }
	}

}

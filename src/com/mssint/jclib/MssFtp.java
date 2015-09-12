package com.mssint.jclib;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketException;

import org.apache.commons.net.ftp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a wrapper with several convenience methods around the 
 * apache FTPClient.
 * 
 * @author MssInt
 *
 */
public class MssFtp {
	private static final Logger log = LoggerFactory.getLogger(MssFtp.class);
	private FTPClient ftp;
	private String server;
	private boolean binaryTransfer;
	private boolean connected;
	private boolean loggedIn;
	private String workingDir;
	private String targetPath;
	private String homePath;
	private String loginID;
	
	/**
	 * Default constructor establishes working directory from properties if available.
	 * Needs to be used in conjunction with @link {@link #connect(String)}
	 * 
	 */
	public MssFtp() {
		ftp = new FTPClient();
		binaryTransfer = true;
		connected = false;
		loggedIn = false;
		server = null;
		homePath = null;
		workingDir = Config.getProperty("jclib.ftp.dir");
		if(workingDir == null) workingDir = Config.getProperty("jclib.default.filepath");
		if(workingDir == null) workingDir = Config.getProperty("jclib.extract.dir");
		if(workingDir == null) workingDir = "";
		if(log.isDebugEnabled())
			log.debug("Working directory set to \""+workingDir+"\"");
	}
	
	/**
	 * Constructor that allows for the specification of the FTP server i.e. host.
	 * @param host the FTP server name/IP
	 * @throws IOException
	 */
	public MssFtp(String host) throws IOException {
		this();
		this.server = host;
		if(!connect(host)) 
			throw new IOException("Connection failed.");
	}
	
	/**
	 * Indicates status of connection.
	 * @return true if connected false otherwise
	 */
	public boolean isConnected() { return connected; }
	
	/**
	 * Indicates whether or not we are logged in
	 * @return true if logged in false otherwise
	 */
	public boolean isLoggedIn() { return loggedIn; }
	/**
	 * Create's an ftp session to a host. By default, the transfer mode is set to binary.
	 * @param host Hostname or IP number of the FTP server.
	 * @return true if the connection was successful, otherwise false.
	 * @throws SocketException
	 * @throws IOException
	 */
	public boolean connect(String host) throws IOException {
		try {
			ftp.connect(host);
		} catch (Exception e) {
			String m = "Connection to host '"+host+"' failed. ";
			log.error(m);
			throw new IOException(m);
		}
		log.info("Connected to " + host + ".");
		if(log.isDebugEnabled())
			log.debug(ftp.getReplyString());
		this.server = host;
		// After connection attempt, you should check the reply 
		// code to verify success.
		int reply = ftp.getReplyCode();

		if(!FTPReply.isPositiveCompletion(reply)) {
			ftp.disconnect();
			log.error("FTP server refused connection.");
			connected = false;
			return false;
		}
		if(binaryTransfer) ftp.setFileTransferMode(FTPClient.BINARY_FILE_TYPE);
		else ftp.setFileTransferMode(FTPClient.ASCII_FILE_TYPE);
		
		//Setup default target path for this server.
		String prop = "jclib.ftp." + host + ".dir";
		targetPath = Config.getProperty(prop);
		
		connected = true;
		return true;
	}
	
	/**
	 * Disconnect the FTP session i.e. logout and disconnect.
	 */
	public void disconnect() {
		if(loggedIn) logout();
		if(connected) {
			connected = false;
			try {
				ftp.disconnect();
			} catch (IOException e) {
			}
		}
	}
	
	/**
	 * Login to the FTP server using the supplied credentials.
	 * @param user The username for the host supplied in the connect() method. connect() must 
	 * have been called first.
	 * @param passwd Password
	 * @return true if successful, otherwise false.
	 * @throws IOException
	 */
	public boolean login(String user, String passwd) throws IOException {
		if(!connected) {
			loggedIn = false;
			throw new IOException("Cannot login - must connect first.");
		}
		if(!ftp.login(user, passwd)) {
			if(log.isDebugEnabled())
				log.debug("login: "+ftp.getReplyCode() + ": "+ftp.getReplyString());
			log.error("Login to ftp server '"+server+"' failed.");
			loggedIn = false;
			return false;
		}
		loggedIn = true;
		
		homePath = ftp.printWorkingDirectory();
		if(targetPath == null) targetPath = homePath;
		else if(targetPath.charAt(0) != '/')
			targetPath = homePath + "/" + targetPath;
		
		String p;
		if(targetPath == null) p = "/";
		else if(targetPath.charAt(0) == '/') p = targetPath;
		else p = "/" + targetPath;
		loginID = "ftp://" + user + "@" + server + p;
		if(targetPath != null) {
			if(!ftp.changeWorkingDirectory(targetPath)) {
				logout();
				disconnect();
				String msg = "url="+loginID+" - Cannot chdir to "+targetPath;
				log.error(msg);
				throw new IOException(msg);
			}
		}
		return true;
	}
	
	/**
	 * Connect and login to the FTP server using the supplied credentials.
	 * @param host The hostname or IP number of the server.
	 * @param user The username for the host supplied in the connect() method. connect() must 
	 * have been called first.
	 * @param passwd Password
	 * @return true if successful, otherwise false.
	 * @throws IOException
	 */
	public boolean login(String host, String user, String passwd) throws IOException {
		if(!connect(host)) return false;
		return login(user, passwd);
	}
	
	/**
	 * Logout from the ftp server.
	 */
	public void logout() {
		if(!connected || !loggedIn) return;
		try {
			loggedIn = false;
			ftp.logout();
		} catch (IOException e) {}
	}
	
	/**
	 * Set binary transfer mode On or Off
	 * @param mode true -> binary false -> ascii
	 */
	public void setBinaryMode(boolean mode) {
		binaryTransfer = mode;
	}
	
	/**
	 * Create a path on the FTP server
	 * @param path the path to create.
	 * @return true if path created false otherwise
	 */
	public boolean createPath(String path) {
		if(path.compareTo("/") == 0 || path.compareTo(".") == 0) return true;
		String parent = Util.parentOf(path);
		if(!createPath(parent)) return false;
		try {
			if(!ftp.changeWorkingDirectory(path)) {
				int r = ftp.mkd(path);
				if(r != 257) {
					String msg = ftp.getReplyString();
					log.error(loginID+": Cannot create directory \""+path+"\" - reply code="+msg);
					return false;
				}
			}
		} catch (IOException e) {
			log.error(loginID+": Cannot create directory "+path);
			return false;
		}
		return true;
	}
	
	/**
	 * Put a file onto the FTP server
	 * @param file the local file to put the remote file will have the same name.
	 * @return success of put action
	 * @throws IOException
	 */
	public boolean put(String file) throws IOException {
		return put(file, file);
	}
	
	/**
	 * Put a file onto the FTP server with a possibly an alyternate name.
	 * @param localFile name of local file
	 * @param remoteFile name of remote file
	 * @return
	 * @throws IOException
	 */
	public boolean put(String localFile, String remoteFile) throws IOException {
		if(!connected || !loggedIn)
			throw new IOException("Cannot transfer '"+localFile+" to "+server+" Connection is not enabled.");
		if(localFile.charAt(0) != '/') 
			if(workingDir.charAt(workingDir.length()-1) == '/')
				localFile = workingDir + localFile;
			else
				localFile = workingDir + "/" + localFile;
		
		log.info(loginID+": put(" + (binaryTransfer ? "binary" : "ascii") +
				") localFile="+localFile+" remoteFile="+remoteFile);
		if(binaryTransfer) ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
		else ftp.setFileType(FTPClient.ASCII_FILE_TYPE);
		
		//Open source file
		FileInputStream in;
		try {
			File file = new File(localFile);
			in = new FileInputStream(file);
		} catch (IOException e) {
			log.error(loginID+": Cannot open source file \""+localFile+"\".");
			return false;
		}
		
		//Ensure target directory exists.
		String path = Util.parentOf(remoteFile);
		String baseName = Util.baseName(remoteFile);
		if(path.charAt(0) == '/') ;
		else if(path.compareTo(".") == 0) path = targetPath;
		else if(targetPath != null) {
			if(targetPath.charAt(targetPath.length()-1) == '/')
				path = targetPath + path;
			else
				path = targetPath + "/" + path;
		}
		if(path != null && !ftp.changeWorkingDirectory(path)) {
			if(log.isDebugEnabled())
				log.debug(loginID+": Target path non-existent. Creating \""+path+"\".");
			if(!createPath(path)) {
				in.close();
				return false;
			}
			if(!ftp.changeWorkingDirectory(path)) {
				log.error(loginID+": Cannot transfer to destination \""+remoteFile+"\".");
				in.close();
				return false;
			}
		}
		boolean success = ftp.storeFile(baseName, in);
		if(targetPath == null) ftp.changeWorkingDirectory("/");
		else ftp.changeWorkingDirectory(targetPath);
		return success;
	}
	
	/**
	 * Retrieve a file from the FTP server.
	 * @param file the file to retrieve
	 * @return a boolean indicating the success of transfer.
	 * @throws IOException
	 */
	public boolean get(String file) throws IOException {
		return get(file, file);
	}
	
	/**
	 * Get file from FTP server and specify local name
	 * @param remoteFile the remote file name
	 * @param localFile the local file name
	 * @return true if transfer succeeded false otherwise.
	 * @throws IOException
	 */
	public boolean get(String remoteFile, String localFile) throws IOException {
		if(!connected || !loggedIn)
			throw new IOException("Cannot transfer '"+localFile+" to "+server+" Connection is not enabled.");
		if(localFile.charAt(0) != '/') {
			if(localFile.charAt(localFile.length()-1) == '/')
				localFile = workingDir + localFile;
			else
				localFile = workingDir + "/" + localFile;
		}
		if(binaryTransfer) ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
		log.info(loginID+": get(" + (binaryTransfer ? "binary" : "ascii") +
				") remoteFile="+remoteFile+" localFile="+localFile);
		
		String path = Util.parentOf(localFile);
		if(path.charAt(0) == '/') ;
		else if(path.compareTo(".") == 0) path = targetPath;
		else if(targetPath != null) {
			if(path.charAt(path.length()-1) == '/')
				path = targetPath + path;
			else
				path = targetPath + "/" + path;
		}
		Util.mkdir(path);
		
		//Create target file
		FileOutputStream out;
		try {
			File file = new File(localFile);
			out = new FileOutputStream(file);
		} catch (IOException e) {
			log.error(loginID+": Cannot open target file \""+localFile+"\".");
			return false;
		}
		
		//Normalise remoteFile name
		if(remoteFile.charAt(0) != '/' && targetPath != null) {
			if(remoteFile.charAt(remoteFile.length()-1) == '/')
				remoteFile = targetPath + "/" + remoteFile;
			else
				remoteFile = targetPath + "/" + remoteFile;
		}
		
		boolean success = ftp.retrieveFile(remoteFile, out);
		return success;
	}
	
	/**
	 * Uses the ftp protocol to put a file onto an ftp server. If the file is a 
	 * pathname, subdirectories will be created on the server if necessary. This
	 * is not the most efficient use of ftp because it will open an ftp connection,
	 * send the file and then close the connection. For transferring multiple files,
	 * use the non static methods: open(), put(), get() and close(). 
	 * @param host The name of the host to connect to.
	 * @param user Username to connect as. If set to null, "anonymous" will be used.
	 * @param passwd The password for 'user'.
	 * @param file The name of the file to transfer.
	 * @param destfile Name of file on destination server
	 * @return true if successful, otherwise false.
	 */
	public static boolean putFile(String host, String user, String passwd, 
			String sourcefile, String destfile) {
		MssFtp mftp = new MssFtp();
		/*
		String [] segments = sourcefile.split("/");
		String directory;
		if(sourcefile.charAt(0) == '/') directory = "/";
		else directory = "";
		if(segments.length > 1) {
			for(int i=0;i<(segments.length-1);i++) {
				directory += segments[i] + "/";
			}
		}
		*/
		try {
			if(!mftp.connect(host)) return false;
			if(!mftp.login(user, passwd)) {
				return false;
			}
			if(!mftp.put(sourcefile, destfile)) return false;
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			mftp.logout();
			mftp.disconnect();
		}
		
		return true;
	}
	
	/**
	 * Places a file onto a FTP server host, essentially does the same as {@link #putFile(String, String, String, String, String)}
	 * except that the remote file will be named the same as the local file. Will connect login and transfer with the given credentials.
	 * @param host The name of the host to connect to.
	 * @param user Username to connect as. If set to null, "anonymous" will be used.
	 * @param passwd The password for 'user'.
	 * @param file The name of the file to transfer.
	 * @return true if successful, otherwise false.
	 */
	public static boolean putFile(String host, String user, String passwd, String file) {
		return putFile(host, user, passwd, file, file);
	}

	/**
	 * Gets a named file from the FTP server host, this is effectively the same as 
	 * {@link #getFile(String, String, String, String, String)} except that the local filename is 
	 * not defined and will be the same as the remote file name
	 * @param host The name of the host to connect to.
	 * @param user Username to connect as. If set to null, "anonymous" will be used.
	 * @param passwd The password for 'user'.
	 * @param file The name of the file to transfer on the FTP server.
	 * @return true if succeeded false otherwise
	 */
	public static boolean getFile(String host, String user, String passwd, String file) {
		return getFile(host, user, passwd, file, file);
	}

	/**
	 * Gets a named file from the FTP server host and places it locally with the specified local name. 
	 * Will connect login and transfer in-bound with the given credentials.
	 * @param host The name of the host to connect to.
	 * @param user Username to connect as. If set to null, "anonymous" will be used.
	 * @param passwd The password for 'user'.
	 * @param sourcefile The name of the file to transfer on the FTP server.
	 * @param destfile Name of file on local machine
	 * @return true if succeeded false otherwise
	 */
	public static boolean getFile(String host, String user, String passwd, 
			String sourcefile, String destfile) {
		MssFtp mftp = new MssFtp();
		/*
		String [] segments = sourcefile.split("/");
		String directory;
		if(sourcefile.charAt(0) == '/') directory = "/";
		else directory = "";
		if(segments.length > 1) {
			for(int i=0;i<(segments.length-1);i++) {
				directory += segments[i] + "/";
			}
		}
		*/
		try {
			if(!mftp.connect(host)) return false;
			if(!mftp.login(user, passwd)) {
				return false;
			}
			if(!mftp.get(sourcefile, destfile)) return false;
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			mftp.logout();
			mftp.disconnect();
		}
		
		return true;
	}
}

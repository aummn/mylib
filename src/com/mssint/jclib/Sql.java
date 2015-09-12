package com.mssint.jclib;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;



//Import the JMS API classes.
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.MessageProducer;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Message;
import javax.jms.TextMessage;
//Import the classes to use JNDI.
import javax.naming.*;
/**
 * <p>Title: JavaLincLib</p>
 * <p>Description: Java LINC libraries</p>
 * <p>last rebuilt %DATE; </p>
 * @author Peter Colman
 * @version %BUILD;
 */

public class Sql {
	private static final Logger log = LoggerFactory.getLogger(Sql.class);
	/** DB constant */
	public static int FETCHED_COL_IS_NULL = 1405;
	/** DB constant */
	public static int ORA_SEQUENCE_ERROR = 1002;
	/** DB constant */
	public static int SQL_UNIQUE_CONSTRAINT = 1;
	/** DB constant */
	public static int SQL_NOTFOUND = 100;
	/** DB constant */
	public static int ORA_NOTFOUND = 1403;
	/** DB constant */
	
    static final String b52 = "                                                    ";
	
	/**
	 * Internal Class of {@see #Sql} which defines the file structure of written timing
	 * data.
	 * 
	 * 
	 */
	protected static class StatRecord {
        int    level;
        int    sline;
        int    eline;
        long starttime;
        long elapsed;
        String id; //limit to 52 bytes
        public void write(RandomAccessFile fd) {
        	try {
				fd.writeInt(level);
				fd.writeInt(sline);
				fd.writeInt(eline);
				fd.writeLong(starttime);
				fd.writeLong(elapsed);
				if(id.length() > 52) fd.writeBytes(id.substring(0,52));
				else if(id.length() < 52)
					fd.writeBytes(id + b52.substring(0, 52 - id.length()));
				else fd.writeBytes(id);
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	
        	
        }
	}
	
	private static String mainClassName = null;
	private static boolean timingOn = false;
	private static File timingFile;
	private static RandomAccessFile outFd = null;
	private static StatRecord statRec;
	public static String repName = null;

	/**
	 * Sets the primary/main class name and report name 
	 * if defined otherwise the report name defaults to the application name.
	 * 
	 * Needs to be called prior to {@see #initTimer} as the timing routines 
	 * require a main clss name.
	 * 
	 */
	public static void init(Object app)  {
		if(app == null) return;
		mainClassName = app.getClass().getSimpleName();
		try{
			Field fld = app.getClass().getDeclaredField("REPORTNAME");
		    if(fld == null) 
		    	repName = mainClassName;
		    else 
		    	repName = (String)fld.get(app);
		} catch (Exception e) {}
	}	
	/**
	 * Initialise the timing routines.
	 * The properties jclib.timing.dir or jclib.timing.run must be set in order for 
	 * the timing routine to be initialised and timing records to be written.
	 * 
	 * {@see #init} should be called prior to the initialising the timer as the app for which
	 * timing is being recording needs to be registered.
	 * 
	 */
	public static void initTimer() {
		String path = Config.getProperty("jclib.timing.dir");
		String s = Config.getProperty("jclib.timing.run");
		if(path == null || s == null || mainClassName == null) return;
		if(s.equals("yes") || s.equals("1")) timingOn = true;
		else return;

		String tf = path + "/" + mainClassName + "." + Util.getPid();
		timingFile = new File(tf);
		try {
			outFd = new RandomAccessFile(timingFile, "rw");
			log.info("Timing routines enabled. File="+tf);
		} catch (FileNotFoundException e) {
			log.warn("Cannot write to file '"+tf+"'. Timing routines disabled.");
			timingOn = false;
			return;
		}
		statRec = new StatRecord();
	}

	
	/**
	 * Toggle the timing off or on. 
	 * 
	 * @param onOff boolean true => on fgalse ==> off.
	 */
	public static void setTimer(boolean onOff) {
		timingOn=onOff;
	}
	
	/**
	 * Force a timing write 
	 * 
	 */
	public static void sqlEt()
	{
		if(!timingOn) return;
		statRec.eline = 0;
		statRec.elapsed = SystemDate.getDate().getTime() - statRec.starttime;
		statRec.write(outFd);
		return;
	}
	
	/**
	 * Set a timing start point.
	 * 
	 * @id a unique identifier for this timing point.
	 * 
	 */
	public static void sqlBt(String id)
	{
		if(!timingOn) return;
		statRec.sline = 0;
		statRec.level = 0;
		statRec.starttime = SystemDate.getDate().getTime();
		statRec.id = id;
		return;
	}

	/**
	 * Standard method for displaying Sql errors.
	 * @param id text to add to error message
	 * @param x SQLException that has occurred
	 * @return a status code 0 is OK, -1 is UNIQUE_CONSTRAINT, 2 is fatal
	*/
	public static int sqlError(String id, SQLException x)
	{
		int errorCode = x.getErrorCode();
		Util.debug("errorCode = " + errorCode);

		if(errorCode == SQL_UNIQUE_CONSTRAINT) {
			// GLB.STATUS = "*****";		for the moment
			return -1;
		} else if(errorCode == FETCHED_COL_IS_NULL ||
		   errorCode == ORA_SEQUENCE_ERROR) {
			return 0;
		} else {
			if(id != null) Util.abort(id + x.getMessage());
			else Util.error(x.getMessage());
		}
		return 0;
	}

	
	/**
	 * Not implemeted presently.
	 * 
	*/
	public static void rollBack()
	{
		return;
	}
	
	/**
	 * Not implemeneted presently always yields false.
	 * @param id does nothing with this
	 * @return false all the time
	*/
	public static boolean sqlIntegrity(String id)
	{
		return false;
	}
	
	/* not needed now
		public static void setGlbstatus(int zero)
		{
			GLB.STATUS="     ";  
			return;
		}
		public static void setGlbstatus(String val)
		{
			GLB.STATUS=val;
			return;
		}
	*/

	/**
	 * used for appending where clauses to sql statements.
	 * @param stmnt the first part of the sql statement
	 * @param idxcnt number of fields to add to where clause
	 * @param grpcnt
	 * @param fld the field names
	 * @param desc
	*/
	public static String appendWhere(String stmnt,int idxcnt,int grpcnt,String[] fld,int[] desc)
	{
		int i,j;
		String compare;
		String cmpop;
	
		if(grpcnt > idxcnt) grpcnt = idxcnt;
	
		/* Work out the GROUP (or equal) portion */
		for(i=0; i<grpcnt; i++) {
			if(i!=0) stmnt=stmnt + "and ";
			stmnt = stmnt + fld[i] + "  = ? ";
		}
	
		/* Now do the `FROM' part */
		if(grpcnt < idxcnt) {
			if(grpcnt!=0) stmnt=stmnt + "and ";
			stmnt=stmnt + "(\n";
			for(i=grpcnt; i<idxcnt; i++) {
				if(i>grpcnt) stmnt=stmnt + " or ";
				stmnt=stmnt + "(";
				for(j=i; j<idxcnt; j++) {
					cmpop = desc[j-i+grpcnt]!=0? "<" : ">";
					if(j>i) stmnt=stmnt + "and ";
					if(j == (idxcnt-1)) {
						if(i == grpcnt) compare= cmpop + "=";
						else compare=cmpop;
					} else compare="=";
					stmnt=stmnt + fld[j-i+grpcnt] + compare + "? ";
				}
				stmnt=stmnt + ")";
			}
			stmnt=stmnt + ")";
		}
		return stmnt;
	}

	/**
	 * Calls commit on an open java.sql.Connection 
	 * any Exception is trapped and discarded. 
	 * 
	 * @param conn the java.sql.Connection on which a commit is to be called.
	 * @return true or false (an error occured 
	 */
	public static boolean commit(Connection conn) {
		try {
			if(conn != null) {
				conn.commit();
			}
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	/**
	 * Calls rollback on an open java.sql.Connection 
	 * any Exception is trapped and discarded. 
	 * 
	 * @param conn the java.sql.Connection on which a rollback is to be called.
	 * @return true or false (an error occured 
	 */
	public static boolean rollback(Connection conn) {
		try {
			if(conn != null) conn.rollback();
		} catch(Exception e) {return false;}
		return true;
	}
	
	/**
	 * Not implemented. 
	 * 
	 * @param p1 
	 * @param p2 
	 */
	public static void wait(int p1, int p2) {
	    Util.warning("wait() not implemented.");
	}
	
	/**
	 * Logs the message to the Logger.
	 * 
	 * @param self presently unused
	 * @param message the message to be logged. 
	 */
	public static void traceString(String self,String message)
	{
		log.debug(message);
		return;
	}
	
	/**
	 * 
	 * Removes all entries in the commit_point table with the report name as established in the {@see #init} method
	 * and a process id obtained from the java.lang.management.RuntimeMXBean via the {@see #Util} class.
	 * 
	 * The transaction is not implicitly commited within this method.
	 * 
	 * @param conn database connection
	 */
	public static int deleteCriticalPoint(Connection conn) throws Exception {
		if (conn == null) return -1;
		PreparedStatement stmnt = conn.prepareStatement("delete from commit_point where repname = ? and pid = ?");
		stmnt.setString(1,repName);
		stmnt.setInt(2, Integer.parseInt(Util.getPid()));
		stmnt.executeUpdate();
	    stmnt.close();
	    return 0;
	}
	
	/**
	 * Insert an entry with the given sequence number into the commit_point table  
	 * 
	 * The report name is as established in the {@see #init} method
	 * and a process id obtained from the java.lang.management.RuntimeMXBean via the {@see #Util} class.
	 * 
	 * The transaction is not implicitly commited within this method.
	 * 
	 * @param conn database connection
	 * @param sd a Var which contains critical point data.
	 * @param conn database connection
	 */
	public static int insertCriticalPoint(Connection conn, Var sd, int seq) throws Exception {
		if (conn == null) return -1;
		
		PreparedStatement stmnt = conn.prepareStatement("insert into commit_point (repname, pid, ctime, ppid, cp_seq, cp_data)"
				+ " values (?, ?, sysdate, 0, ?, ?)");
		stmnt.setString(1,repName);
		stmnt.setInt(2, Integer.parseInt(Util.getPid()));
		stmnt.setInt(3,seq);
		stmnt.setString(4,sd.getString());
		stmnt.executeUpdate();
		stmnt.close();
		stmnt = null;
		return 0;
	}
	
	/**
	 * Returns the current sequence number from the commit_point table where
	 * the report name is as established in the {@see #init} method
	 * and a process id obtained from the java.lang.management.RuntimeMXBean via the {@see #Util} class. 
	 * 
	 */
	public static int getCriticalPoint(Connection conn, int pid, Var cpData ) throws Exception {
		
		if (conn == null) return 0;
		
		PreparedStatement stmnt = conn.prepareStatement("select cp_data, cp_seq from commit_point where repname = ? and pid = ?");
		stmnt.setString(1,repName);
		stmnt.setInt(2, pid);
		ResultSet rs = stmnt.executeQuery();
		rs.next();
		cpData.set(rs.getString(1));
		int cpSeq = rs.getInt(2);
	    stmnt.close();
	    rs.close();
		return cpSeq;
	}
	/**
	 * Inserts a critical point record, commits and resets GLB.DATE and GLB.TIME
	 * @param conn the database connection
	 * @param sd The data to update the critical point record with
	 * @param seq The Sequence number of the critical point
	 * @param GLB the GLB object
	*/
	public static void criticalPoint (Connection conn, Var sd, int seq, Glb GLB) throws Exception {
		criticalPoint (conn, sd, seq);
		GLB.setGlbDate();
	}
	/**
	 * Inserts a critical point record after removing any prior critical points and commits the work.
	 * @param conn the database connection
	 * @param sd The data to update the critical point record with
	 * @param seq The Sequence number of the critical point
	*/
	public static void criticalPoint (Connection conn, Var sd, int seq) throws Exception {

		if (conn == null);

		deleteCriticalPoint(conn);
	    	
		insertCriticalPoint(conn, sd, seq);
		
		//cpUsed=true;
		
		commit(conn);
	}
	
	private static int portNo = 0;
	
	
	/**
	 * Allows for backing off for a specified time or indefinitely (see time param) waiting for a wake-up signal 
	 * to proceed. A wake up signal may not be recieved before the timeout is met (see return values).
	 * 
	 * @param time the number of seconds to wait for a socket connection before timing out. 
	 * If 0 then will wait indefinitely. 
	 * @return 1, 0, or -1 
	 * where 1 indicates a SocketTimeoutException has ocurred
	 * 0 indictes succesful execution
	 * -1 no port has been defined/found check properties
	 */
	public static int waitForWakeUpViaSocket (int time) throws Exception {
		String port = Config.getProperty(repName.trim() + ".wakeup.port");
		if (port == null)
			port = Config.getProperty("default.wakeup.port");
		if (port == null) return -1;
		portNo = Integer.parseInt(port);
		ServerSocket sock = null;
		Socket wakeUp = null;
		try {
			sock = new ServerSocket(portNo);
			sock.setSoTimeout(time * 1000);
			//System.out.println("Wait port=" + portNo + " time=" + time);
			wakeUp = sock.accept();
		} catch (SocketTimeoutException e) {
			return 1;
		} finally {
			if(wakeUp != null)
				wakeUp.close();
			if(sock != null)
				sock.close();
		}
		return 0;
	}
	/**
	 * Wait for Wakup signal via oracle pipe
	 * 
	 * @param time the number of seconds to wait for a socket connection before timing out. 
	 * If 0 then will wait indefinitely. 
	 * @return GLB.PARAM 
	 */
	private static int reason = 0;
	public static String waitForWakeUpOracle (Connection conn, long time, Glb glb) throws Exception {
                //For sqlserver 
                if(true) {
			glb.PARAMFLAG.set("N");
			return " ";
		}
		CallableStatement receive_pipe = null;
		CallableStatement unpack_pipe = null;
		String param = " ";
		try {
			if (time != 0) {
				receive_pipe = conn.prepareCall("{? = call DBMS_PIPE.RECEIVE_MESSAGE(?,?)}");
				receive_pipe.setLong (3, time);
			} else {
				receive_pipe = conn.prepareCall("{? = call DBMS_PIPE.RECEIVE_MESSAGE(?)}");
			}
			receive_pipe.registerOutParameter(1, java.sql.Types.INTEGER);
			receive_pipe.setString (2, repName);
			log.debug("waiting on pipe:" + repName + "<");
			receive_pipe.execute();
			reason = receive_pipe.getInt(1);
			if(reason == 0) {
				unpack_pipe = conn.prepareCall("{call DBMS_PIPE.UNPACK_MESSAGE(?)}");
				unpack_pipe.registerOutParameter(1, java.sql.Types.VARCHAR);
				unpack_pipe.execute();
				param = unpack_pipe.getString(1);
				glb.PARAMFLAG.set("Y");
			log.debug("received>" + param + "<");
			} else {
				glb.PARAMFLAG.set("N");
				log.debug("reason=" + reason + " timeout");
			}
			return param;
			
		} catch (SQLException e) {
			e.printStackTrace();
			return "";
		} finally {
			if(receive_pipe != null)
				receive_pipe.close();
			if(unpack_pipe != null)
				unpack_pipe.close();
		}
	}
	
	
	/**
	 * commits and resets GLB.DATE and GLB.TIME
	 * @param conn the database connection
	 * @param GLB the GLB object
	*/
	public static String sleep(Connection conn, Glb GLB) throws Exception { 
		GLB.setGlbDate();
		return sleep(conn, 0, GLB);
	}
	/**
	 * commits
	 * @param conn the database connection
	  
	public static String sleep(Connection conn) throws Exception {
		return sleep(0);
	}*/
	
	/**
	 * commits sleeps time seconds and resets GLB.DATE and GLB.TIME
	 * @param conn the database connection
	 * @param time the time to sleep
	 * @param GLB the GLB object
	
	public static String sleep(int time, Glb GLB) throws Exception {
		GLB.setGlbDate();
		return(sleep(time));
	}*/
	
	/**
	 * commits sleeps time seconds
	 * @param conn the database connection
	 * @param time the time to sleep
	*/
	public static String sleep(Connection conn, long time, Glb GLB) throws Exception { 
		commit(conn);
		if(GLB.CLOSE.eq("CLOSE")) {
			GLB.CLOSE.set("CLOSING");
			return " ";
		}
		GLB.CLOSE.set("");
		String ret=" ";
		reason=0;
		if (time != 0) {
			if(repName != "BMBATCHMGR" && repName != "BMTIMER")
				Thread.sleep(time * 1000);
			else
				ret=waitForWakeUp (time, GLB);
			if(ret.equals("CLOSEDOWN")) GLB.CLOSE.set("CLOSE");
		    GLB.PARAMFLAG.set("N");
		} else GLB.PARAMFLAG.set("N");
		GLB.setGlbDate();
		return ret;
	}
	
	public static String sleep(Connection conn, int time, Glb GLB) throws Exception { 
		return sleep(conn, (long) time, GLB);
	}
	
	/**
	 * Commits any pending transactions and sleeps/backs-off indefinitely and wakes up when signaled.
	 * 
	 * @param conn the database connection
	 * @param GLB global variable class into which the date and time is set
	 */
	public static String sleepUntilWoken (Connection conn, Glb GLB) throws Exception {
		return sleepUntilWoken (conn, GLB, 0);
	}
	
	/**
	 * Commits any pending transactions and sleeps/backs-off for a specified number of seconds or until a signal 
	 * is received that is less than the back-off time.
	 * 
	 * @param conn the database connection
	 * @param GLB global variable class into which the date and time is set 
	 * @param time the time to sleep
	 */
	public static String sleepUntilWoken (Connection conn, Glb GLB, int time) throws Exception {
		commit(conn);
		if(GLB.CLOSE.eq("CLOSE")) {
			GLB.CLOSE.set("CLOSING");
			return " ";
		}
		reason=0;
		String ret=waitForWakeUp (time, GLB);
		if(ret.equals("CLOSEDOWN")) GLB.CLOSE.set("CLOSE");
		if(time != 0 && reason == 1)
			GLB.CLOSE.set("CLOSE");
		GLB.setGlbDate();
		return ret;
	}

	/**
	 * 
	 * Send the "wake up" signal to the listening/waiting process on the socket/port.
	 * 
	 * @param report the name of the report which is used to see if a report specific 
	 * socket port is being used.
	 * 
	 */
	public static int wakeUpViaSocket (String report) {
		String port = Config.getProperty(report.trim() + ".wakeup.port");
		if (port == null)
			port = Config.getProperty("default.wakeup.port");
		if (port == null) return -1;
		portNo = Integer.parseInt(port);
		//System.out.println("port=" + portNo);
		try {
			Socket sock = new Socket("localhost",portNo);
			sock.close();
		}catch (java.net.ConnectException ce) {
			
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
		
		return 0;
	}
	
	
	/**
	 * 
	 * Send the "wake up" signal to the listening/waiting process via the Oracle
	 * sql pipe.
	 * 
	 * @param report
	 *            the name of the report which to be woken
	 * @param param
	 *            parameter to be sent to the report
	 * 
	 */
	public static int wakeUp(String report, String param) {
		Context context = null;
		javax.jms.ConnectionFactory jmsConnectionFactory= null;
		Session session = null;
		javax.jms.Connection jmsConnection = null;
		try {
			if(repName != null) {
				Properties jndiProps = new Properties();
				jndiProps.put(Context.INITIAL_CONTEXT_FACTORY,         
			    	"org.jboss.naming.remote.client.InitialContextFactory");
				jndiProps.put(Context.PROVIDER_URL,"http-remoting://127.0.0.1:8080");
			   	jndiProps.put(Context.SECURITY_PRINCIPAL, "jinzhm");
			   	jndiProps.put(Context.SECURITY_CREDENTIALS, "jzm30jzm");
				jndiProps.put("jboss.naming.client.ejb.context", true);
				context= new InitialContext(jndiProps);
				jmsConnectionFactory = (javax.jms.ConnectionFactory) context.lookup("jms/RemoteConnectionFactory");
			} else {
				context= new InitialContext();
				jmsConnectionFactory = (javax.jms.ConnectionFactory) context.lookup("java:/ConnectionFactory");
			}
			jmsConnection = jmsConnectionFactory.createConnection("jinzhm", "jzm30jzm");
			session = jmsConnection.createSession(false,Session.AUTO_ACKNOWLEDGE);
			Destination destination = session.createQueue(report);
			MessageProducer producer = session.createProducer(destination);
			TextMessage message = session.createTextMessage();
System.out.println("Sending:" + report + ":" + param);
			message.setText(param);
			producer.send(message);
			session.close();
			jmsConnection.close();
			
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			//if(context!=null) try { context.close(); } catch (Exception ex) { /* No Op */ }
			if(session!=null) try { session.close(); } catch (Exception ex) { /* No Op */ }
			if(jmsConnection!=null) try { jmsConnection.close(); } catch (Exception ex) { /* No Op */ }
		}
		return 0;
	}
	public static String waitForWakeUp(long time , Glb glb) {
		return waitForWakeUp(repName, time , glb);
	}
	public static String waitForWakeUp(String report, long time , Glb glb) {
		String param="";
		reason=0;
		Session session = null;
		Context context = null;
		ConnectionFactory jmsConnectionFactory = null;
		javax.jms.Connection jmsConnection = null;
		try {
			Properties jndiProps = new Properties();
			jndiProps.put(Context.INITIAL_CONTEXT_FACTORY,         
			    "org.jboss.naming.remote.client.InitialContextFactory");
			jndiProps.put(Context.PROVIDER_URL,"http-remoting://127.0.0.1:8080");
			jndiProps.put(Context.SECURITY_PRINCIPAL, "jinzhm");
			jndiProps.put(Context.SECURITY_CREDENTIALS, "jzm30jzm");
			jndiProps.put("jboss.naming.client.ejb.context", true);
			context= new InitialContext(jndiProps);
			jmsConnectionFactory = (ConnectionFactory) context.lookup("jms/RemoteConnectionFactory");
			Destination destination;
			jmsConnection = jmsConnectionFactory.createConnection("jinzhm", "jzm30jzm");
			session = jmsConnection.createSession(false,Session.AUTO_ACKNOWLEDGE);
			destination = session.createQueue(report);
			MessageConsumer consumer = session.createConsumer(destination);
			jmsConnection.start();
			Message message=consumer.receive(time*1000);
			if(message == null) {
				param="";
System.out.println("timed out");
				glb.PARAMFLAG.set("N");
				reason=1;
			} else {
				param=((TextMessage)message).getText();
System.out.println("received:" + report + ":" + param);
				glb.PARAMFLAG.set("Y");
			}
			
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			//if(context!=null) try { context.close(); } catch (Exception ex) { /* No Op */ }
			if(session!=null) try { session.close(); } catch (Exception ex) { /* No Op */ }
			if(jmsConnection!=null) try { jmsConnection.close(); } catch (Exception ex) { /* No Op */ }
		}
		return param;
	}


	//public static int wakeUp(String report) {
		//return wakeUpViaSocket(report);
	//}
	/**
	 * 
	 * Send the "wake up" signal to the listening/waiting process via the Oracle sql pipe.
	 * 
	 * @param report the name of the report which to be woken
	 * @param param parameter to be sent to the report
	 * 
	 */
	public static int wakeUpOraclePipe (Connection conn, String report, String param) {
                //For sqlserver 
                if(true) return 0;
		CallableStatement send_pipe = null;
		CallableStatement pack_pipe = null;
		try {
			//Why purge?
			//CallableStatement purge_pipe = conn.prepareCall
				//("{call DBMS_PIPE.PURGE(?)}");
			//purge_pipe.setString(1, report.trim());
			//purge_pipe.execute();
			pack_pipe = conn.prepareCall
				("{call DBMS_PIPE.PACK_MESSAGE(?)}");
			pack_pipe.setString(1, param);
			pack_pipe.execute();
			send_pipe = conn.prepareCall
				("{? = call DBMS_PIPE.SEND_MESSAGE(?)}");
			send_pipe.registerOutParameter(1, java.sql.Types.INTEGER);
			send_pipe.setString (2, report.trim());
			log.debug("wakeUp>" + report.trim() + "<>" + param + "<");
			send_pipe.execute();
		} catch (SQLException e) {
			e.printStackTrace();
			return -1;
		} finally {
			try {
				if(send_pipe != null)
					send_pipe.close();
				if(pack_pipe != null)
					pack_pipe.close();
			} catch (SQLException e) {
				//No interest in error here.
			}
		}
		return 0;
	}
	/**
	 * 
	 * Send the "wake up" signal to the listening/waiting process via the Oracle sql pipe.
	 * 
	 * @param report the name of the report which to be woken
	 *  
	 */
	public static int wakeUp (String report) {
		return wakeUp (report, "");
	}
	/**
	 * 
	 * Send the "wake up" signal to the listening/waiting process via the Oracle sql pipe.
	 * 
	 * @param report the name of the report which to be woken
	 *  
	 */
	public static int wakeUp (Var report) {
		return wakeUp (report.getString(), "");
	}
	/**
	 * 
	 * Send the "wake up" signal to the listening/waiting process via the Oracle sql pipe.
	 * 
	 * @param report the name of the report which to be woken
	 * @param param parameter to be sent to the report
	 * 
	 */
	public static int wakeUp (Var report, String param) {
		return wakeUp (report.getString(), param);
	}
	/**
	 * 
	 * Send the "wake up" signal to the listening/waiting process via the Oracle sql pipe.
	 * 
	 * @param report the name of the report which to be woken
	 * @param param parameter to be sent to the report
	 * 
	 */
	public static int wakeUp (String report, Var param) {
		return wakeUp (report, param.getString());
	}
	/**
	 * 
	 * Send the "wake up" signal to the listening/waiting process via the Oracle sql pipe.
	 * 
	 * @param conn the database connection
	 * @param report the name of the report which to be woken
	 * @param param parameter to be sent to the report
	 * 
	 */
	public static int wakeUp (Var report, Var param) {
		return wakeUp (report.getString(), param.getString());
	}
	
	/**
	 * 
	 * Return the nextval for a sequence.
	 * 
	 * @param conn the database connection
	 * @param sequencename the name of the sequence
	 * @return the nextval or -1 if error
	 * 
	 */
	
	public static long getNextSequence (Connection conn, String sequenceName) {
		long l_unique;
		try {
			String Sql = "select "+ sequenceName.trim() +".nextval from dual";
			CallableStatement stmnt = conn.prepareCall(Sql);
			ResultSet rs = stmnt.executeQuery();
			if(rs.next()) {
				l_unique = rs.getLong(1);
				rs.close();
			} else l_unique = -1;
			stmnt.close();
		}catch (SQLException e) {
			e.printStackTrace();
			return -1;
		}
		return l_unique;
	}
	/**
	 * 
	 * Return the nextval for a sequence.
	 * 
	 * @param conn the database connection
	 * @param sequencename the name of the sequence
	 * @return the nextval or -1 if error
	 * 
	 */
	public static long getNextSequence (Connection conn, Var sequenceName) {
		return getNextSequence (conn,sequenceName.getString());
	}
	
	/**
	 * 
	 * Return the currval for a sequence.
	 * 
	 * @param conn the database connection
	 * @param sequencename the name of the sequence
	 * @return the currval or -1 if error
	 * 
	 */
	public static long getCurrSequence (Connection conn, String sequenceName) {
		long l_unique;
		try {
			String Sql = "select "+ sequenceName.trim() +".currval from dual";
			CallableStatement stmnt = conn.prepareCall(Sql);
			ResultSet rs = stmnt.executeQuery();
			if(rs.next()) {
				l_unique = rs.getLong(1);
				rs.close();
			} else l_unique = -1;
			stmnt.close();
		}catch (SQLException e) {
			e.printStackTrace();
			return -1;
		}
		return l_unique;
	}
	/**
	 * 
	 * Return the currval for a sequence.
	 * 
	 * @param conn the database connection
	 * @param sequencename the name of the sequence
	 * @return the currval or -1 if error
	 * 
	 */
	public static long getCurrSequence (Connection conn, Var sequenceName) {
		return getCurrSequence (conn,sequenceName.getString());
	}
	/**
	 * 
	 * Creates a new sequence.
	 * 
	 * @param conn the database connection
	 * @param sequencename the name of the sequence
	 * @param start start value
	 * @param inc inccrement
	 * @return the 0 or -1 if error
	 * 
	 */
	public static int createSequence (Connection conn, String sequenceName,long start, long inc) {
		try {
			Statement stmt = conn.createStatement();
		    stmt.executeUpdate("CREATE SEQUENCE " + sequenceName.trim() + " START WITH " + start +
		    	        						" INCREMENT BY " + inc );
		    stmt.close();
		}catch (SQLException e) {
			e.printStackTrace();
			return -1;
		}
		return 0;
	}
	/**
	 * 
	 * Creates a new sequence.
	 * 
	 * @param conn the database connection
	 * @param sequencename the name of the sequence
	 * @param start start value
	 * @param inc inccrement
	 * @return the 0 or -1 if error
	 * 
	 */
	public static int createSequence (Connection conn, Var sequenceName,long start, long inc) {
		return createSequence (conn,sequenceName.getString(), start, inc);
	}
	/**
	 * 
	 * Creates a new sequence.
	 * 
	 * @param conn the database connection
	 * @param sequencename the name of the sequence
	 * @param start start value
	 * @param inc inccrement
	 * @return the 0 or -1 if error
	 * 
	 */
	public static int createSequence (Connection conn, Var sequenceName,Var start, long inc) {
		return createSequence (conn,sequenceName.getString(), start.getLong(), inc);
	}
	/**
	 * 
	 * Creates a new sequence.
	 * 
	 * @param conn the database connection
	 * @param sequencename the name of the sequence
	 * @param start start value
	 * @param inc inccrement
	 * @return the 0 or -1 if error
	 * 
	 */
	public static int createSequence (Connection conn, Var sequenceName,Var start, Var inc) {
		return createSequence (conn,sequenceName.getString(), start.getLong(), inc.getLong());
	}
	/**
	 * 
	 * Creates a new sequence.
	 * 
	 * @param conn the database connection
	 * @param sequencename the name of the sequence
	 * @param start start value
	 * @param inc inccrement
	 * @return the 0 or -1 if error
	 * 
	 */
	public static int createSequence (Connection conn, Var sequenceName,long start, Var inc) {
		return createSequence (conn,sequenceName.getString(), start, inc.getLong());
	}
	/**
	 * 
	 * Drops a sequence.
	 * 
	 * @param conn the database connection
	 * @param sequencename the name of the sequence
	 * @return the 0 or -1 if error
	 * 
	 */
	public static int dropSequence (Connection conn, String sequenceName) {
		try {
			Statement stmt = conn.createStatement();
		    stmt.executeUpdate("DROP SEQUENCE " + sequenceName.trim()); 
		    stmt.close();
		}catch (SQLException e) {
			e.printStackTrace();
			return -1;
		}
		return 0;
	}
	/**
	 * 
	 * Drops a sequence.
	 * 
	 * @param conn the database connection
	 * @param sequencename the name of the sequence
	 * @return the 0 or -1 if error
	 * 
	 */
	public static int dropSequence (Connection conn, Var sequenceName) {
		return dropSequence (conn, sequenceName.getString());
	}
}

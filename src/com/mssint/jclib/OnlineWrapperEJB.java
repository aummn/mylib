package com.mssint.jclib;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OnlineWrapperEJB implements the sessionBean for the LINC online applications.
 * <p>
 * last rebuilt %DATE;
 * </p>
 * 
 * This supports EJB2.x - for EJB3.x please see LincRemote interface
 * 
 * @version %BUILD;
 */
public class OnlineWrapperEJB implements SessionBean {
	private static final long serialVersionUID = 5680720094059808283L;
	private static final Logger log = LoggerFactory.getLogger(OnlineWrapperEJB.class);
	protected boolean newInstance = true;
	protected boolean isStartup;
	//private SessionContext m_ctx = null;


	public void ejbCreate() throws Exception, CreateException {
	}

	/**
	 * Sets the session context. Required by EJB spec.
	 * 
	 * @param context
	 * @throws EJBException
	 *            A SessionContext object.
	 */
	public void setSessionContext(SessionContext context) throws EJBException {
		//m_ctx = context;
	}

	/**
	 * Removes the bean. Required by EJB spec.
	 * 
	 * @throws EJBException
	 */
	public void ejbRemove() throws EJBException {
		Util.info("ejbRemove() on obj " + this);
	}

	/**
	 * Loads the state of the bean from secondary storage. Required by EJB spec.
	 * 
	 * @throws EJBException
	 */
	public void ejbActivate() throws EJBException {
		Util.info("ejbActivate() on obj " + this);
	}

	/**
	 * Serializes the state of the bean to secondary storage. Required by EJB
	 * spec.
	 * 
	 * @throws EJBException
	 */
	public void ejbPassivate() throws EJBException {
		Util.info("ejbPassivate() on obj " + this);
	}

	public ScreenState startup(ScreenState initialState)
			throws Exception {
		newInstance = true;
		isStartup = true;
		return null;
	}

	public ScreenState commit(ScreenState commitedState) throws
			Exception {
		isStartup = false;
//		if(repository != null)
//			repository.servicedClearDownRepositories.clear();
		return null;
	}


	
	/**
	 * Runs a command line program, passing param to the script and returning the result
	 * both into param and returned as a String. If the 'script' parameter is null, then
	 * assume that the command to be executed is embedded in the param paremeter. In this
	 * case, do not pass param as arguments to the command.
	 * @param script The script to call. The script should return a single line only.
	 *               If script is null then execute the command embedded in param.
	 * @param param The arguments to pass to the script and a repository for the result.
	 * @return The standard out from the called script.
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public String callPipe(String script, Var param) throws InterruptedException {
		String s = callPipe(script, param.toString().trim());
		param.set(s);
		return s;
	}
	
	public String callPipe(Var param) throws InterruptedException {
		return callPipe(null, param);
	}
	
	public String callPipe(String script, String param) throws InterruptedException {
		StringBuilder sb = new StringBuilder();
		if(param == null) param = "";
		String cmd;
		if(script == null) cmd = param.trim();
		else cmd = script + " " + param.trim();
		
		Process process;
		int exitValue;
		try {
			process = Runtime.getRuntime().exec(cmd);
			process.waitFor();
			exitValue = process.exitValue();
			
			BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String s;
			
			while((s = br.readLine()) != null) {
				if(sb.length() > 0) sb.append(' ');
				sb.append(s);
			}
			BufferedReader ebr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			boolean first = true;
			while((s = ebr.readLine()) != null) {
				if(first) {
					log.warn("Script '"+script+"' returned data on stderr:");
					first = false;
				}
				log.warn("   >"+s);
			}
			br.close();
			ebr.close();
		} catch (IOException e) {
			exitValue = 2;
			sb.append("NOTOK");
			log.error("callPipe: cmd=\""+cmd+"\" failed. " + e.getMessage());
		}
		if(exitValue != 0)
			log.warn("Called script '"+cmd+"' returned exit code="+exitValue);
		else
			log.info("Script '"+cmd+"' returned an exit status of "+exitValue);
		return sb.toString();
	}
}


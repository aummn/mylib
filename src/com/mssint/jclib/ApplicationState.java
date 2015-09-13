package com.mssint.jclib;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationState  {
	private static final long serialVersionUID = 2L;
	private static final Logger log = LoggerFactory.getLogger(ApplicationState.class);
	public java.sql.Connection connection;
	public Glb GLB;
	public ScreenState screenRef;
	protected boolean copyFromIspec;

	
	public String REPORTNAME;
	public String VERSION;
	//public String GENDATE;
	//public Date genDate;
	
	//This repository is temporary. It is passed to the Webmanager front end and
	//saved in the ScreenState object.
	private ListRepository repository;
	
	private boolean initialised = false;
	public EnvParser envParser;
	
	BufferedReader bufferedReader = null;
	private ReportControl reportControl;

	{
		Sql.init(this);
		Sql.initTimer();
	}

	protected void init(String repName) {
		if(initialised) return;
		copyFromIspec = false;
		REPORTNAME = repName;
		Config.programName = repName;
	    //genDate = DC.parseDate(GENDATE);
	    Report.setParent(this, REPORTNAME);
		initialised = true;
		//String jVersion = System.getProperty("java.version");
	    envParser = new EnvParser(this);
	    
	    //Note BOJ in database (commit_point table, if present)
	    try {
	    	Sql.insertCriticalPoint(connection, new Var("BOJ"), 0);
	    } catch (Exception e) {}
	    
	}
	
	/**
	 * Perform any initialisation which may be required for the startup phase
	 * @param state The ScreenState object passed to the EJB
	 */
	public void startupInit(ScreenState state) {
//		startupPhase = true;
		repository = null;
	}
	
	/**
	 * Perform any initialisation which may be required for the commit phase
	 * @param state The ScreenState object passed to the EJB
	 */
	public void commitInit(ScreenState state) {
//		startupPhase = false;
		repository = null;
	}
	
	public void boj() {
		String user = System.getenv("BC_USERNAME");
		if(user == null)
			user = System.getProperty("user.name");
		if(user == null)
			user = "Unknown";
		log.info("BOJ: REPORT=" + REPORTNAME + 
				" PID=" + Util.getPid() +
				" User="+ user +
				" Java Version="+System.getProperty("java.version"));
		
		reportControl = new ReportControl(this, REPORTNAME);
		if(reportControl.isControlled()) {
			log.debug("isControlled");
			if(reportControl.needsUrlLookup()) {
				String u = getUrlFromGlbWork(reportControl.getStation());
				log.debug("BOJ: u={}",u);
				reportControl.setUrl(u + "/batch/functions");
			}
			log.debug("BOJ: url={}",reportControl.getUrl());
			reportControl.notifyBoj();
		} else {
			log.debug("!isControlled");
			String urlstr = Config.getProperty("webmanager.urls");
			log.debug("BOJ: urlstr={}", urlstr);
			reportControl.setIsControlled(true);
			if(urlstr != null) {
				String [] urls = urlstr.split("[, ]");
				for(String u : urls) {
					u = u.trim();
					if(u.length() == 0)
						continue;
					reportControl.setUrl(u + "/batch/functions");
					reportControl.notifyBoj();
				}
			}
			reportControl.setUrl(null);
			reportControl.setIsControlled(false);
		}
	}
	
	public void eoj() {
		log.info("EOJ: REPORT=" + REPORTNAME + 
				" PID=" + Util.getPid() +
				" User="+ System.getProperty("user.name")+
				" Java Version="+System.getProperty("java.version"));
		if(reportControl != null && reportControl.isControlled()) {
			log.debug("EOJ: reportControl != null,  url={}", reportControl.getUrl());
			reportControl.notifyEoj();
		} else {
			String urlstr = Config.getProperty("webmanager.urls");
			log.debug("EOJ: urlstr={}", urlstr);
			reportControl.setIsControlled(true);
			if(urlstr != null) {
				String [] urls = urlstr.split("[, ]");
				for(String u : urls) {
					u = u.trim();
					if(u.length() == 0)
						continue;
					reportControl.setUrl(u + "/batch/functions");
					log.debug("EOJ: u={}", u);
					reportControl.notifyEoj();
				}
			}
			reportControl.setUrl(null);
			reportControl.setIsControlled(false);
		}
	}
	
	public boolean display(Var station, Var mesg) {
		return display(station.toString(), mesg.getString());
	}
	
	public boolean display(Var station, String mesg) {
		return display(station.toString(), mesg);
	}

	public boolean display(String station, Var mesg) {
		return display(station, mesg.toString());
	}
	
	public boolean display(String station, String mesg) {
		if(reportControl == null) {
			reportControl = new ReportControl(this, REPORTNAME);
		}
		if(station.toString().trim().length() > 0) {
			reportControl.setStation(station.toString());
			if(reportControl.getUrl() == null) {
				reportControl.setUrl(getUrlFromGlbWork(station));
			}
		}
		log.debug("display({}, {} - batchControlled={} station={} url={}", station.toString(), mesg, reportControl.isControlled(), reportControl.getStation(), reportControl.getUrl());
		return display(mesg, true);
	}

	/** 
	 * Called by batch program to get URL of controlling webmanager instance
	 * @param station
	 * @return
	 */
	private String getUrlFromGlbWork(String station) {
		if(connection == null || station == null || station.trim().length() == 0)
			return null;
		
		String Sql = "select url from glb_audit10 where station_au = '" + station + "'";
		CallableStatement stmnt = null;
		ResultSet rs = null;
		String myUrl = null;
		try {
			stmnt = connection.prepareCall(Sql);
			rs = stmnt.executeQuery();
			if(rs != null && rs.next()) {
				myUrl = rs.getString(1);
			}		
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if(rs != null) {
				try { rs.close(); } catch (SQLException e) { }
			}
			if(stmnt != null) {
				try { stmnt.close(); } catch (SQLException e) { }
			}
		}
		return myUrl;
	}

	public boolean display(Var mesg) {
		return display(mesg.getString());
	}
	
	/**
	 * Display text to user - if run from Webmanager, then send the display to the users console,
	 * else send to stdout/
	 * @param text
	 * @return
	 */
	public boolean display(String mesg) {
		return display(mesg, false);
	}
	/**
	 * Display text to user - if run from Webmanager, then send the display to the users console,
	 * else send to stdout/
	 * @param text
	 * @param stationSpecified
	 * @return
	 */
	public boolean display(String mesg, boolean stationSpecified) {
		if(reportControl != null && reportControl.isControlled()) {
			if(!stationSpecified)
				reportControl.setStation(reportControl.getStation());
			reportControl.display(mesg);
			return true;
		}
		System.out.println(mesg);
		return false;
	}
	
	public boolean isInitialised() { return initialised; }
	public boolean isInitialized() { return initialised; }

	public void cfMessage(Var msg, int loopno) { cfMessage(msg.toString(), loopno); }
	public void cfMessage(String msg, int loopNo) { 
		if(loopNo == 2)
			message(msg,false); 
		else
			message(msg,true);
	}
	public void message(Var msg) { message(msg.toString()); }
	public void message(Var msg,boolean error) { message(msg.toString(),error); }
	public void message(String msg) { message(msg,true); }
	public void message(String msg, int copy, boolean error) {
		message(msg + " " + copy,true); 
	}
    public void message(String msg, boolean error) {
    	
		if(screenRef != null) {
			if(copyFromIspec && GLB != null)
				screenRef.message(GLB.COPY,  msg);
			else
				screenRef.message(msg);
			if (error)
	    		GLB.ERROR.set("*****");
		} else {
			Util.message(msg);
		}
	}

	public String accept() throws Exception {
		if(reportControl != null && reportControl.isControlled()) {
			String text = reportControl.accept(null);
			return text;
		}

		if(bufferedReader == null)
			bufferedReader = new BufferedReader(new InputStreamReader(System.in));
		String s = bufferedReader.readLine();
        return s;
    }

    public String accept(Var v) throws Exception {
        String s = accept();
        if(v != null) v.set(s);
        return s;
    }
    
	public void xrecoverRepository(ListRepository repository) {
//		System.out.println("OnlineWrapper: recoverRepository: " + repository);
		this.repository = repository;
	}
	
	public ListRepository getRepository() {
//		System.out.println("OnlineWrapper: getRepository: " + repository);
		return repository;
	}
	
	@Deprecated
    public void sendlistadd(Var repositoryName, Var value) {
    	if(repository == null) repository = new ListRepository();
    	repository.sendListAdd(repositoryName, value);
    }

    
	@Deprecated
    public void sendlistadd(String repositoryName, String value) {
    	if(repository == null) repository = new ListRepository();
    	repository.sendListAdd(repositoryName, value);
    }

    
	@Deprecated
    public void sendlistadd(String repositoryName, Var value) {
    	if(repository == null) repository = new ListRepository();
    	repository.sendListAdd(repositoryName, value);
    }
    
    
	@Deprecated
    public void sendlistclear(String repositoryName) {
    	if(repository == null) return;
    	repository.sendListClear(repositoryName);
    }
    
	@Deprecated
    public void sendlistclear(Var repositoryName) {
    	if(repository == null) return;
    	repository.sendListClear(repositoryName);
    }

    /**
	 * Create a list which can be used in select and list boxes. These lists are
	 * dynamic and have single user scope.
	 * @param repositoryName The name of the list repository
	 * @param value The value to add. Must be in the form ^xxx^xxxxxxxxxxx^ where ^ is any
	 * character which will be regarded as the separato character.
	 */
    public void sendListAdd(Var repositoryName, Var value) {
    	if(repository == null) repository = new ListRepository();
    	repository.sendListAdd(repositoryName, value);
    }

    
	/**
	 * Create a list which can be used in select and list boxes. These lists are
	 * dynamic and have single user scope.
	 * @param repositoryName The name of the list repository
	 * @param value The value to add. Must be in the form ^xxx^xxxxxxxxxxx^ where ^ is any
	 * character which will be regarded as the separato character.
	 */
    public void sendListAdd(String repositoryName, String value) {
    	if(repository == null) repository = new ListRepository();
    	repository.sendListAdd(repositoryName, value);
    }

    
	/**
	 * Create a list which can be used in select and list boxes. These lists are
	 * dynamic and have single user scope.
	 * @param repositoryName The name of the list repository
	 * @param value The value to add. Must be in the form ^xxx^xxxxxxxxxxx^ where ^ is any
	 * character which will be regarded as the separato character.
	 */
    public void sendListAdd(String repositoryName, Var value) {
    	System.out.printf("%s [%s]\n", repositoryName, value.toString());
    	if(repository == null) repository = new ListRepository();
    	repository.sendListAdd(repositoryName, value);
    }

    public void sendListClearPrior(String repositoryName) {
    	if(repository == null) repository = new ListRepository();
    	repository.sendListClearPrior(repositoryName);
    }

    public void sendListClearPrior(Var repositoryName) {
    	sendListClearPrior(repositoryName.toString());
    }
    
    
	/**
	 * Clear's a dynamic list repository.
	 * @param repositoryName The name of the list repository
	 */
    public void sendListClear(String repositoryName) {
    	sendListClearPrior(repositoryName);
    	repository.sendListClear(repositoryName);
    }

	/**
	 * Clear's a dynamic list repository.
	 * @param repositoryName The name of the list repository
	 */
    public void sendListClear(Var repositoryName) {
    	sendListClearPrior(repositoryName.toString());
    	repository.sendListClear(repositoryName);
    }
    
    /**
     * Package up a call to the configured webserver(s) to add a static list.
     * Webserver url's are identified by the "jclib.sendlist.url" property
     * @param repositoryName The name of the repository to add
     * @param list The values to add.
     * @throws UnsupportedEncodingException 
     */
	private void staticListHttpCall(String repositoryName, ArrayList<String> list) throws IOException {
//		StringBuilder sb = new StringBuilder();
		String urlPart = "/nof/repository";
		//TODO Handle security, login's and sessions. For the moment, just insecure.

		Object [] urlKeys = Config.getPropertyKeys("jclib.repository.url.");
		if(urlKeys == null)
			return;
		for(Object key : urlKeys) {
			String url = Config.getProperty(key.toString());
			if(url == null || url.length() == 0)
				continue;
			if(url.endsWith("/"))
				url = url.substring(0, url.length()-1);
			url = url + urlPart;

			HttpClient client = new DefaultHttpClient();
			HttpPost post = new HttpPost(url);
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
			nameValuePairs.add(new BasicNameValuePair("function", "create"));
			nameValuePairs.add(new BasicNameValuePair("name", repositoryName));
			for(String s : list) {
				nameValuePairs.add(new BasicNameValuePair("o", s));
			}

			if(log.isDebugEnabled())
				log.debug("Sending create repository '"+repositoryName+"' request to "+url);
			post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

			HttpResponse response = client.execute(post);
			BufferedReader rd = new BufferedReader(new InputStreamReader(
					response.getEntity().getContent()));
			
			String line;
			while ((line = rd.readLine()) != null) {
				if(log.isDebugEnabled())
					log.debug(line);
			}
		}
	}

	/**
	 * Create a list which can be used in select and list boxes. These lists are
	 * static and have global scope.
	 * @param repositoryName The name of the list repository
	 * @param exFile An extract file of values, each line in the form
	 *  ^xxx^xxxxxxxxxxx^ where ^ is any character which will be regarded
	 *   as the separator character. The entire file is loaded.
	 * @throws IOException 
	 */
    public void staticListAdd(Var repositoryName, Extract exFile) throws IOException {
    	staticListAdd(repositoryName.toString(), exFile);
    }
	/**
	 * Create a list which can be used in select and list boxes. These lists are
	 * static and have global scope. Once the list is created, it is sent to all
	 * configured webmanager instances.
	 * @param repositoryName The name of the list repository
	 * @param exFile An extract file of values, each line in the form
	 *  ^xxx^xxxxxxxxxxx^ where ^ is any character which will be regarded
	 *   as the separator character. The entire file is loaded.
	 * @throws IOException 
	 */
    public void staticListAdd(String repositoryName, Extract exFile) throws IOException {
    	if(repositoryName == null || repositoryName.length() == 0)
    		return;
    	if(exFile == null) {
    		log.error("Add static list for repository="+repositoryName+" - exFile is null");
    		return;
    	}
    	
    	ArrayList<String>list = new ArrayList<String>();
    	exFile.open();
    	while(exFile.fetch()) {
    		String s = exFile.getBuffer().trim();
    		list.add(s);
    	}
    	staticListHttpCall(repositoryName, list);
    }
    
	/**
	 * Create a list which can be used in select and list boxes. These lists are
	 * static and have global scope.
	 * @param repositoryName The name of the list repository
	 * @param fileName The name of a file containing values, each line in the form
	 *  ^xxx^xxxxxxxxxxx^ where ^ is any character which will be regarded
	 *   as the separator character. The entire file is loaded.
	 * @throws IOException 
	 */
    public void staticListAdd(String repositoryName, String fileName) throws IOException {
    	if(repositoryName == null || repositoryName.length() == 0)
    		return;
    	if(fileName == null || fileName.length() == 0) {
    		log.error("Add static list for repository="+repositoryName+" - fileName is null");
    		return;
    	}
    	
    	ArrayList<String>list = new ArrayList<String>();
    	MssFile f = new MssFile(fileName);
    	f.open("r");
    	String s;
    	while((s = f.read()) != null) {
    		list.add(s);
    	}
    	staticListHttpCall(repositoryName, list);
    }
	/**
	 * Create a list which can be used in select and list boxes. These lists are
	 * static and have global scope.
	 * @param repositoryName The name of the list repository
	 * @param fileName The name of a file containing values, each line in the form
	 *  ^xxx^xxxxxxxxxxx^ where ^ is any character which will be regarded
	 *   as the separator character. The entire file is loaded.
	 * @throws IOException 
	 */
    public void staticListAdd(Var repositoryName, String fileName) throws IOException {
    	staticListAdd(repositoryName.toString(), fileName);
    }
	/**
	 * Create a list which can be used in select and list boxes. These lists are
	 * static and have global scope.
	 * @param repositoryName The name of the list repository
	 * @param fileName The name of a file containing values, each line in the form
	 *  ^xxx^xxxxxxxxxxx^ where ^ is any character which will be regarded
	 *   as the separator character. The entire file is loaded.
	 * @throws IOException 
	 */
    public void staticListAdd(String repositoryName, Var fileName) throws IOException {
    	staticListAdd(repositoryName, fileName.toString());
    }
	/**
	 * Create a list which can be used in select and list boxes. These lists are
	 * static and have global scope.
	 * @param repositoryName The name of the list repository
	 * @param fileName The name of a file containing values, each line in the form
	 *  ^xxx^xxxxxxxxxxx^ where ^ is any character which will be regarded
	 *   as the separator character. The entire file is loaded.
	 * @throws IOException 
	 */
    public void staticListAdd(Var repositoryName, Var fileName) throws IOException {
    	staticListAdd(repositoryName.toString(), fileName.toString());
    }

    @Deprecated
    public void xclearRepositories() {
    	if(repository == null) return;
    	repository.clearRepositories();
    }

    @Deprecated
    public void xinjectRepositoryItems(Map<String, ParameterWrapper> parameters) {
    	if(repository == null) return;
    	repository.injectRepositoryItems(parameters);
    }

}

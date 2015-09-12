package com.mssint.jclib;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportControl {
	private static final Logger log = LoggerFactory.getLogger(ReportControl.class);

	ApplicationState state;
	
	boolean batchControlled;
	
	//Environment variables set by WebManager
	private String sessionId;
	private String userName;
	private String stationName;
	private String url;
	private String reportName;
	private String lookupStation;

	private BasicCookieStore cookieStore;

	private BasicHttpContext httpContext;

	private DefaultHttpClient httpClient;
	
	
	public ReportControl(ApplicationState applicationState, String repName) {
		state = applicationState;
		sessionId = System.getenv(Batch.BC_SESSIONID.toString());
		userName = System.getenv(Batch.BC_WMUSER.toString());
		stationName = System.getenv(Batch.BC_STATIONNAME.toString());
		url = System.getenv(Batch.BC_URL.toString());
		reportName = System.getenv(Batch.BC_REPORTNAME.toString());
		lookupStation = System.getenv(Batch.BC_LOOKUPSTATION.toString());
		if(reportName == null || !reportName.equals(repName))
			reportName = repName;
		if(sessionId != null || state == null) {
			batchControlled = true;
		} else {
			batchControlled = false;
		}
		
		log.debug("New ReportControl: report={} userName={} station={} url={} sessionId={}, batchControlled={}", reportName, userName, stationName, url, sessionId, batchControlled);
		//If lookupStation is not null then this program was initiated from another 
		//batch program and we do not know the sessionId. 
		if(!batchControlled && lookupStation != null && lookupStation.trim().length() > 0) {
			sessionId = null;
			stationName = null;
			batchControlled = true;
		}
		
		
	}
	
	/**
	 * Check if this report was initiated by Webmanager's batch controller
	 * @return
	 */
	public boolean isControlled() {
		return batchControlled;
	}
	
	public void setIsControlled(boolean yes) {
		batchControlled = yes;
	}
	
	private List<NameValuePair>getStandardPairs() {
		List<NameValuePair> pairs = new ArrayList<NameValuePair>();
		pairs.add(new BasicNameValuePair(Batch.BC_SESSIONID.toString(), sessionId));
		pairs.add(new BasicNameValuePair(Batch.BC_WMUSER.toString(), userName));
		pairs.add(new BasicNameValuePair(Batch.BC_STATIONNAME.toString(), stationName));
		//TODO - Check that reportname is correct.
		pairs.add(new BasicNameValuePair(Batch.BC_REPORTNAME.toString(), reportName));
		pairs.add(new BasicNameValuePair(Batch.BC_PID.toString(), Util.getPid()));
		pairs.add(new BasicNameValuePair(Batch.BC_REMOTEUSER.toString(), System.getProperty("user.name")));
		return pairs;
	}

	/**
	 * Notify Webmanager that the report is starting.
	 */
	public void notifyBoj() {
		if(!batchControlled) {
			return;
		}
		List<NameValuePair>pairs = getStandardPairs();
		pairs.add(new BasicNameValuePair(Batch.FUNCTION.toString(), Batch.F_BOJ.toString()));
		try {
			sendHttpCall(pairs);
		} catch (IOException e) {
			log.error("Unable to send BOJ notification to Webmanager.", e);
		}
	}

	public void notifyEoj() {
		if(!batchControlled) {
			return;
		}
		List<NameValuePair>pairs = getStandardPairs();
		pairs.add(new BasicNameValuePair(Batch.FUNCTION.toString(), Batch.F_EOJ.toString()));
		log.debug("pair function = {}  F_EOJ={}", Batch.FUNCTION.toString(), Batch.F_EOJ.toString());
		try {
			sendHttpCall(pairs);
		} catch (IOException e) {
			log.error("Unable to send EOJ notification to Webmanager.", e);
		}
	}

	private String sendHttpCall(List<NameValuePair> pairs) throws ClientProtocolException, IOException {
		if(httpClient == null) {
			httpClient = new DefaultHttpClient();
			cookieStore = new BasicCookieStore();
			httpContext = new BasicHttpContext();
			httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
		}
		if(url == null) {
			log.info("URL is null - reverting to stdout");
			batchControlled = false;
			return null;
		}
		HttpPost post = new HttpPost(url);
		post.setHeader("User-Agent", "JLinc");
		post.setEntity(new UrlEncodedFormEntity(pairs));

		HttpResponse response = httpClient.execute(post);
		BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
		
		String line;
		StringBuilder sb = new StringBuilder();
		while ((line = rd.readLine()) != null) {
			sb.append(line);
			sb.append("\n");
			if(log.isDebugEnabled())
				log.debug(line);
		}
		return sb.toString();
	}

	public void display(String text) {
		if(!batchControlled) {
			return;
		}
		log.debug("display({}); - station={} url={}", text, stationName, url);
		List<NameValuePair>pairs = getStandardPairs();
		pairs.add(new BasicNameValuePair(Batch.FUNCTION.toString(), Batch.F_DISPLAY.toString()));
		pairs.add(new BasicNameValuePair(Batch.DISPLAYTEXT.toString(), text));
		try {
			sendHttpCall(pairs);
		} catch (IOException e) {
			log.error("Unable to send EOJ notification to Webmanager.", e);
		}
	}

	public String accept(String prompt) {
		if(!batchControlled) {
			return "";
		}
		List<NameValuePair>pairs = getStandardPairs();
		pairs.add(new BasicNameValuePair(Batch.FUNCTION.toString(), Batch.F_ACCEPT.toString()));
		if(prompt == null)
			prompt = "";
		pairs.add(new BasicNameValuePair(Batch.PROMPT.toString(), prompt));
		try {
			return sendHttpCall(pairs);
		} catch (IOException e) {
			log.error("Unable to send EOJ notification to Webmanager.", e);
			return "";
		}
	}
	
	public void startHtmlProgram(String program) {
		url="http://iolite.mss:8080/gpc/batch/runReport";
		List<NameValuePair>pairs = new ArrayList<NameValuePair>();
		pairs.add(new BasicNameValuePair("name",program));
		pairs.add(new BasicNameValuePair("user","BATCH"));
		pairs.add(new BasicNameValuePair("station","BATCH"));
		try {
			sendHttpCall(pairs);
		} catch (IOException e) {
			log.error("Unable to send runReport to Webmanager.", e);
		}
	}

	/**
	 * Set a temporary station as the target for a message
	 * @param station
	 */
	public void setStation(String station) {
		this.stationName = station;
		batchControlled = true;
	}

	/** 
	 * Return the URL set for this instance.
	 * @return
	 */
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		if(url != null && url.length() > 0) {
			this.url = url;
		} else {
			url = null;
		}
	}

	public String getStation() {
		if(lookupStation != null && lookupStation.length() > 0)
			return lookupStation;
		return stationName;
	}

	public boolean needsUrlLookup() {
		log.debug("url={}, lookupStation={}", url, lookupStation);
		if((url == null || url.length() == 0) && lookupStation != null)
			return true;
		return false;
	}

}

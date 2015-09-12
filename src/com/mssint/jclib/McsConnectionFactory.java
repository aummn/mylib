package com.mssint.jclib;

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class McsConnectionFactory {
	private static final Logger log = LoggerFactory.getLogger(McsConnectionFactory.class);
	private static McsConnectionFactory factory;
	private static boolean initialised = false;
	ConcurrentHashMap<String, McsLite> mcsMap;
	
	private McsConnectionFactory() {
		mcsMap = new ConcurrentHashMap<String, McsLite>(1000);
		initialised = true;
	}
	
	private static synchronized void createFactory() {
		if(initialised) return;
		factory = new McsConnectionFactory();
	}
	
	public static McsLite getMcsConnection(String sessionId) {
		if(!initialised) McsConnectionFactory.createFactory();
		return factory.getMcs(sessionId);
	}

	public static void release(String sessionId) {
		if(!initialised) return;
		McsLite mcs = factory.mcsMap.get(sessionId);
		if(mcs == null) return;
		mcs.close();
		factory.remove(sessionId);
	}

	private void remove(String sessionId) {
		mcsMap.remove(sessionId);
	}
	private int maxCount = 0;
	private McsLite getMcs(String sessionId) {
		if(sessionId == null) return null;
		//Try to find an existing entry:
		McsLite mcs = mcsMap.get(sessionId);
		if(mcs != null) return mcs;
		if(Config.getProperties() == null) return null;
		mcs = new McsLite(Config.getProperties());
		mcsMap.put(sessionId, mcs);
		
		int count = mcsMap.size();
		if(count > maxCount) {
			maxCount = count;
			if((maxCount % 20) == 0) {
				if(log.isDebugEnabled())
					log.debug("McsConnectionFactory: Open sockets="+count);
			}
		}
		
		return mcs;
	}

}

package com.mssint.jclib;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class SendlistRepository implements java.io.Serializable {
	private static final long serialVersionUID = -5563326590557140004L;
	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(SendlistRepository.class);
	
	//map to hold repository items
	protected HashMap<String, GUIItemList<String>> repositoryList = new HashMap<String, GUIItemList<String>>(); 
	protected HashMap<String, GUIItemList<String>> backingRepositoryList = new HashMap<String, GUIItemList<String>>(); 
	protected ArrayList<String> amendedRepositories = new ArrayList<String>();
	protected ArrayList<String> servicedClearDownRepositories = new ArrayList<String>(); 

    public void sendlistadd(Var repositoryName, Var value){
    	sendlistadd(repositoryName.getString().replaceAll("\\s+$", "").replaceAll("^\\s+", ""), value);
    }

    
    public void sendlistadd(String repositoryName, Var value) {
    	GUIItemList<String> list;
    	repositoryName = repositoryName.replaceAll("\\s+$", "").replaceAll("^\\s+", "").replaceAll("-", "_");
    	amendedRepositories.add(repositoryName);
    	if(repositoryList.containsKey(repositoryName)){
    		if(value.getString().length() < 3) { //not enough space for a name value pair with 2 delimeters.
    			sendlistclear(repositoryName);
        		list = new GUIItemList<String>();
    		} else {
	    		list = repositoryList.get(repositoryName.toString());
    		}
    	} else {
    		list = new GUIItemList<String>();
		}
		if(value != null && value.getString().length() >= 2 && !list.contains(value.getString())){
			if(list.getIndex(value.getString()) != null){
				if(!list.getIndex(value.getString()).matches("^\\s+$") && list.containsIndexValue(value.getString())){
					list.remove(value.getString());
				}
			}
			list.add(value.getString());
			repositoryList.put(repositoryName, list);
		}
    }
    
    
    public void sendlistclear(String repositoryName){
    	repositoryName = repositoryName.replaceAll("\\s+$", "");
    	if(repositoryList.containsKey(repositoryName)) {
    		repositoryList.get(repositoryName).clear();
    	}
    }
    
    public void sendlistclear(Var repositoryName){
    	String repName = repositoryName.getString().replaceAll("\\s+$", "").replaceAll("^\\s+", "");    	
    	
    	if(repositoryList.containsKey(repName)){
    		repositoryList.get(repositoryName).clear();
    	}
    }
    
    @SuppressWarnings("unchecked")
	public void clearRepositories(){
    	backingRepositoryList.putAll((HashMap<String, GUIItemList<String>>) repositoryList.clone()); 
    	repositoryList.clear();
    	repositoryList = new HashMap<String, GUIItemList<String>>();
    	amendedRepositories.clear();
    	amendedRepositories = new ArrayList<String>();
    }

    public void injectRepositoryItems(Map<String, ParameterWrapper> parameters){
    	String repositoryKey;
		for(Iterator<String> it = repositoryList.keySet().iterator(); it.hasNext();) {
			repositoryKey = it.next();
			if(parameters.containsKey(repositoryKey) && amendedRepositories.contains(repositoryKey)){
				parameters.remove(repositoryKey);
			}
			parameters.put(repositoryKey, new ParameterWrapper(repositoryList.get(repositoryKey)));
			parameters.get(repositoryKey).setParameterType(ParameterWrapper.TYPE_LISTBOXITEM);
			backingRepositoryList.remove(repositoryKey);
    	}
		for(Iterator<String> it = backingRepositoryList.keySet().iterator(); it.hasNext();) {
			repositoryKey = it.next();
			if(parameters.containsKey(repositoryKey)){
				parameters.remove(repositoryKey);
			}
			parameters.put(repositoryKey, new ParameterWrapper(backingRepositoryList.get(repositoryKey)));
			parameters.get(repositoryKey).setParameterType(ParameterWrapper.TYPE_LISTBOXITEM);
    	}
    }

	
}

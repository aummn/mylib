package com.mssint.jclib;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListRepository implements java.io.Serializable {
	private static final long serialVersionUID = -5563326590557140004L;
	private static final Logger log = LoggerFactory.getLogger(ListRepository.class);
	
	//map to hold dynamic (local) repository items
	protected HashMap<String, GUIItemList<String>> repositoryList = new HashMap<String, GUIItemList<String>>(); 

	//map to hold static (global) repository items
	protected static HashMap<String, GUIItemList<String>> staticRepoList = new HashMap<String, GUIItemList<String>>(); 
	
	private String cleanRepositoryName(String repositoryName) {
    	repositoryName = repositoryName.replaceAll("\\s+$", "").replaceAll("^\\s+", "");
		return repositoryName;
	}
	
    /**
	 * Create a list which can be used in select and list boxes. These lists are
	 * dynamic and have single user scope.
	 * @param repositoryName The name of the list repository
	 * @param value The value to add. Must be in the form ^xxx^xxxxxxxxxxx^ where ^ is any
	 * character which will be regarded as the separato character.
	 */
    public void sendListAdd(Var repositoryName, Var value) {
    	sendListAdd(repositoryName.getString(), value);
    }
    
    /**
	 * Create a list which can be used in select and list boxes. These lists are
	 * dynamic and have single user scope.
	 * @param repositoryName The name of the list repository
	 * @param value The value to add. Must be in the form ^xxx^xxxxxxxxxxx^ where ^ is any
	 * character which will be regarded as the separato character.
	 */
    public void sendListAdd(String repositoryName, Var value) {
    	sendListAdd(repositoryName, value.getString());
    }
    
    /**
	 * Create a list which can be used in select and list boxes. These lists are
	 * dynamic and have single user scope.
	 * @param repositoryName The name of the list repository
	 * @param value The value to add. Must be in the form ^xxx^xxxxxxxxxxx^ where ^ is any
	 * character which will be regarded as the separator character.
	 */
    public void sendListAdd(String repositoryName, String value) {
    	GUIItemList<String> list = null;
    	repositoryName = cleanRepositoryName(repositoryName);
    	if(repositoryName.length() == 0)
    		return;
    	if(repositoryList.containsKey(repositoryName)) {
    		if(value.length() < 3) { //not enough space for a name value pair with 2 delimeters.
	    		list = repositoryList.get(repositoryName);
	    		if(list != null) {
	    			list.clear();
	    		}
    		} else {
	    		list = repositoryList.get(repositoryName);
    		}
    		if(list == null)
    			repositoryList.remove(repositoryName);
    	}
		if(list == null) {
    		list = new GUIItemList<String>();
    		list.isStatic(false);
			repositoryList.put(repositoryName, list);
		}

		if(value != null && value.length() >= 2 /* && !list.contains(value) */) {
			/*
			// Note that LINC does not seem to remove duplicate keys, so disabling this check.
			if(list.getIndex(value) != null) { //Remove duplicates
				if(!list.getIndex(value).matches("^\\s+$") && list.containsIndexValue(value)){
					list.remove(value);
				}
			}
			*/
			list.add(value);
		}
    }
    
    /**
	 * Create a list which can be used in select and list boxes. These lists are
	 * static and have global scope. Typically, this method will be called by
	 * RepositoryServlet in response to an http session call to "/repository"
	 * @param repositoryName The name of the list repository
	 * @param vals The list of values to add. Each entry in the array must
	 *  be in the form ^xxx^xxxxxxxxxxx^ where ^ is any character which will
	 *  be regarded as the separator character.
	 *  @return Count of the total number of items in the list
	 */
    public static synchronized int staticListAdd(String repositoryName, String [] vals) {
    	GUIItemList<String> list = null;
//    	repositoryName = cleanRepositoryName(repositoryName);
    	if(repositoryName.length() == 0)
    		return -1;
    	if(staticRepoList.containsKey(repositoryName)) {
	    	list = staticRepoList.get(repositoryName);
    	} else {
    		list = new GUIItemList<String>();
    		list.isStatic(true);
			staticRepoList.put(repositoryName, list);
		}

    	for(String value : vals) {
    		if(value != null && value.length() >= 2 && !list.contains(value)) {
    			if(list.getIndex(value) != null) {
    				if(!list.getIndex(value).matches("^\\s+$") && list.containsIndexValue(value)){
    					list.remove(value);
    				}
    			}
    			list.add(value);
    		}
    	}
    	return list.size();
    }
    
    public static GUIItemList<String> staticListFind(String repositoryName) {
    	if(repositoryName == null || repositoryName.trim().length() == 0)
    		return null;
    	return staticRepoList.get(repositoryName);
    }
    
    /**
     * Clear the named static list.
     * @param name The name of the list to clear.
     */
	public static synchronized void staticListClear(String repositoryName) {
		if(repositoryName == null || repositoryName.length() == 0)
			return;
    	if(staticRepoList.containsKey(repositoryName)) {
	    	GUIItemList<String>list = staticRepoList.get(repositoryName);
	    	staticRepoList.remove(repositoryName);
	    	list.clear();
    	}
	}

    /**
     * Update this repository with the contents of 'repository'
     * @param startupMode 
     * @param repository - the repository to update from.
     */
	public void update(boolean startupMode, ListRepository repository) {
		if(repository.repositoryList == null)
			return;
		//Iterate through the supplied repository.
		Iterator<Entry<String, GUIItemList<String>>> it = 
			repository.repositoryList.entrySet().iterator();
		while(it.hasNext()) {
			Entry<String, GUIItemList<String>>e = it.next();
			GUIItemList<String>list;
//			boolean created;
			
			//Check if our repository has the same entry.
			if(repositoryList.containsKey(e.getKey())) {
				//List exists - use existing pointer
				list = repositoryList.get(e.getKey());
				//LINC appears to clear down the lists during the preScreen phase.
				if(startupMode)
					list.clear();
				if(e.getValue().isClearFirst())
					list.clear();
//				created = false;
			} else { //Does not exist. Create a new one and insert into repository.
				list = new GUIItemList<String>();
				repositoryList.put(new String(e.getKey()), list);
//				created = true;
			}
			//Add the contents of the new list to list
			for(String s : e.getValue()) {
				/*
				String [] as = s.split(Util.getDelimiter(s));
				if(as.length < 3)
					continue;
				//If the option description is blank, so should the option value.
				String a,b,c,d;
				a = as[0].trim();
				if(as.length > 1) b = as[1] ; else b = "--";
				if(as.length > 2) c = as[2] ; else c = "--";
				if(as.length > 3) d = as[3] ; else d = "--";
//				if(as.length < 3 || as[2].trim().length() == 0)
//					list.clear();
				 */
				list.add(s);
			}
			if(log.isInfoEnabled())
				log.debug("Added new repository '"+e.getKey()+"' with "+list.size()+" items.");
			if(log.isDebugEnabled()) {
				int count = 0;
				for(String s : list) {
					if(log.isDebugEnabled())
						log.debug("      "+count+": "+s);
					count++;
				}
			}
			e.getValue().clear();
		}
		repository.clearRepositories();
	}
    
	public GUIItemList<String> getGUIItemList(String repository) {
		GUIItemList<String>list;
		list = repositoryList.get(repository);
		if(list == null) 
			list = staticRepoList.get(repository);
		if(list == null && !repository.startsWith("*.")) {
			int i = repository.indexOf(".");
			if(i != -1) {
				repository = "*." + repository.substring(i+1);
				list = repositoryList.get(repository);
				if(list == null) 
					list = staticRepoList.get(repository);
			}
		}
		return list;
	}

    public void sendListClear(String repositoryName) {
    	repositoryName = cleanRepositoryName(repositoryName);
    	if(repositoryList.containsKey(repositoryName)){
    		GUIItemList<String> list = repositoryList.get(repositoryName);
    		list.clear();
    		list.clearFirst();
    	}
    }

    public void sendListClearPrior(String repositoryName) {
       	GUIItemList<String> list = null;
    	repositoryName = cleanRepositoryName(repositoryName);
    	if(repositoryName.length() == 0)
    		return;
    	if(repositoryList.containsKey(repositoryName)) {
	    	list = repositoryList.get(repositoryName);
    	}
		if(list == null) {
    		list = new GUIItemList<String>();
    		list.isStatic(false);
			repositoryList.put(repositoryName, list);
		}
		list.clearFirst();
    }
    
    public void sendListClear(Var repositoryName){
    	String repName = cleanRepositoryName(repositoryName.getString());
    	
    	if(repositoryList.containsKey(repName)){
    		GUIItemList<String> list = repositoryList.get(repositoryName);
    		list.clear();
    		list.clearFirst();
    	}
    }
    
    @SuppressWarnings("unchecked")
    /**
     * Clears all repositories
     */
	public void clearRepositories() {
    	repositoryList.clear();
    	repositoryList = new HashMap<String, GUIItemList<String>>();
    }
    
    /**
     * Clear all static lists from the repository
     */
    public void clearStaticLists() {
		Iterator<Entry<String, GUIItemList<String>>> it = 
			repositoryList.entrySet().iterator();
		while(it.hasNext()) {
			Entry<String, GUIItemList<String>>e = it.next();
			String key = e.getKey();
			GUIItemList<String>list = repositoryList.get(key);
			if(list.isStatic()) {
				it.remove();
			}
		}
    }

    public void injectRepositoryItems(Map<String, ParameterWrapper> parameters) {
    	//TODO: Fix this
    	
    	System.out.println("injectRepositoryItems");
    	
    	/*
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
    	*/
    }

}

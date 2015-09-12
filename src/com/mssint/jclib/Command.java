package com.mssint.jclib;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mssint.jclib.Command.CommandSet;


/**
 * 
 * Internal class used by the {@link Command} class for providing a threaded 
 * InputStream to OuputStream redirect capability  
 * 
 * @author MssInt
 *
 */
class StreamGobbler extends Thread
{
    InputStream is;
    String type;
    String os;
    boolean append;
    
    
    /**
     *
     * This constructor will does not define an OutputStream and in effect when the
     * thread is started the output is in effect redirected nowhere /dev/null unix equivalent.
     * 
     * @param is InputStream the source Stream 
     * @param type of redirection qualified name unused and not a processing directive presently.
     * 
     */
    StreamGobbler(InputStream is, String type)
    {
        this(is, type, null, false);
    }
    StreamGobbler(InputStream is, String redirect, String type)
    {
        this(is, type, redirect, false);
    }

    /**
    *
    * This constructor allows for the definition of an OutputStream and if non null will when the thread is 
    * started write the results out to the specific OutputStream.
    * 
    * @param is InputStream the source Stream 
    * @param type of redirection qualified name unused and not a processing directive presently.
    * @param redirect The outputstream to which the inputstream is being written/redirected.
    */
    StreamGobbler(InputStream is, String type, String redirect, boolean append)
    {
        this.is = is;
        this.type = type;
        this.os = redirect;
        this.append = append;
    }

    /**
    *
    * This constructor gets its streams from commandSet
    * started write the results out to the specific OutputStream.
    * 
    * @param is InputStream the source Stream 
    * @param type of redirection qualified name unused and not a processing directive presently.
    * @param redirect The outputstream to which the inputstream is being written/redirected.
    */
    StreamGobbler(InputStream is, String type, CommandSet commandSet)
    {
        this.is = is;
        this.type = type;
        this.os = commandSet.stdOutStr;
        this.append = commandSet.stdOutAppend;
    }
    
    /**
     * Thread start routine to perform the InputStream to OutputStream redirect.
     * 
     */
    public void run()
    {
        PrintWriter pw = null;
        try
        {
            if (os != null) {
            	pw = new PrintWriter(new FileWriter(os, append));
            }
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line=null;
            while ( (line = br.readLine()) != null)
            {
                if (pw != null)
                    pw.println(line);
            }
            
        } catch (IOException ioe) {
            ioe.printStackTrace();  
		} finally {
            if(pw != null) {
            	pw.flush();
            	pw.close();
            }
		}
    }
}

/**
 * Provides the mechanism for executing external processes required to 
 * support the system. 
 * 
 * In particular this class allows for the execution of external programs through a
 * dense set of overloaded execute methods. 
 * 
 * It also allows for the definition of argumaents to a known external program that 
 * simulates the UNISYS WFL construct.
 * 
 * Internally the class uses the java.lang.ProcessBuilder class as the execution mechanism.
 * 
 * Additionally the class allows for the definition environment variables from which the
 * Path, Home Directory, Shell & stdOut definition can be defined these are set 
 * via the Config class into here during it's construction {@link Config} 
 * 
 * The system will attempt to search the entire path definition (the path proprerty can contain multiple directorys via a ; or : delimeter) 
 * until it finds the appropriate executable and returns a status code of 0 immediatley not 
 * waiting for the process to complete. 
 * A status code of -1 is returned whenever the external process is not found on the path.
 * 
 * This class does not report the return value of the executed process directly as it is in effect
 * threaded and does not wait for completion and return the specific return code of the executed 
 * process i.e. the status code of 0 means the process was started a status code of -1 
 * indicates the external process could not be found.
 * 
 * When calling execute the first argument is always the command any further arguments are 
 * arguments.
 * 
 * @author MssInt
 *
 */
public class Command {
	private static final Logger log = LoggerFactory.getLogger(Command.class);
	// singleton instance of this class
    private static Command command;
	
	//List of paths and home directory
	private String[] pathList;
	private String homeDir;
	private String outputFile;
	
	//For remote or local shell
	//private String shell;
	
	// state of the factory
    private static boolean initialised = false;
	private static String remoteStation;

	//Runtime instance
	//private Runtime runtime;
	//private ProcessBuilder proc;
	//private boolean notFound;
	
	//Constructor for private use only.
	private Command() {
		pathList = null;
		//shell = null;
		homeDir = null;
		outputFile = "/dev/null";
	}
	
	static class CommandSet {
		List<String> cmdList = new ArrayList<String>();
		String stdOutStr;
		String stdErrStr;
		String stdInStr;
		String command;
		boolean stdOutAppend;
		boolean stdErrAppend;
		boolean combineOutput;
		private List<String> envp;

		public String toString() {
			StringBuilder sb = new StringBuilder();
			stdOutAppend=true; // Steve temporary
			for(String s : cmdList) {
				sb.append(s);
				sb.append(" ");
			}
			if(stdOutStr != null) {
				if(stdOutAppend) sb.append(">> ");
				else sb.append("> ");
				sb.append(stdOutStr);
				sb.append(" ");
			}
			if(stdInStr != null) {
				sb.append("< ");
				sb.append(stdInStr);
				sb.append(" ");
			}
			if(combineOutput) {
				sb.append("2>&1");
			} else if(stdErrStr != null) {
				if(stdErrAppend) sb.append("2>> ");
				else sb.append("2> ");
				sb.append(stdErrStr);
				sb.append(" ");
			}
			
			return sb.toString();
		}
		
		public CommandSet(String cmd) {
			processCommands(cmd);
		}

		public CommandSet(String cmd, String arg) {
			processCommands(cmd);
			processCommands(arg);
		}
		
		public CommandSet(String[] args) {
			for(String arg : args) {
				processCommands(arg);
			}
		}
		
		public CommandSet(String cmd, String[] args) {
			processCommands(cmd);
			for(String arg : args) {
				processCommands(arg);
			}
		}
		
//		private static String regex = "(>>|>)|(2>>|2>)|\"([^\"]*)\"|(\\S+)|>|>>";
		private static String regex = "(<)|(>>)|(>)|(2>>)|(2>)|\"([^\"].*)\"|([^<> ]*)";
		
		private void processCommands(String cmd) {
			//System.out.println(cmd);
			Matcher m = Pattern.compile(regex).matcher(cmd);
			boolean stdOut = false;
			boolean stdErr = false;
			boolean append = false;
			boolean stdIn = false;
			
			while(m.find()) {
			//	System.out.print("["+m.group()+"]");
				if(m.group(1) != null) { // >
					stdIn = true;
				} else if(m.group(2) != null) { // >>
					stdOut = true;
					append = true;
				} else if(m.group(3) != null) { // >
					stdOut = true;
				} else if(m.group(4) != null) { // 2>>
					stdErr = true;
					append = true;
				} else if(m.group(5) != null) { // 2>
					stdErr = true;
				} else if(m.group(6) != null) { //Quoted string
					String s = m.group();
					if(s.length() > 0) {
						applyToList(s, stdIn, stdOut, stdErr, append);
						stdOut = false;
						stdErr = false;
						append = false;
					}
				} else if(m.group(7) != null) { // String
					String s = m.group(7);
					if(s.length() > 0) {
						applyToList(s, stdIn, stdOut, stdErr, append);
						stdOut = false;
						stdErr = false;
						stdIn = false;
						append = false;
					}
				}
			}

		}

		private void applyToList(String s, boolean stdIn, boolean stdOut, boolean stdErr, boolean append) {
			if(stdIn) {
				stdInStr = s;
			} else if(stdOut) {
				stdOutStr = s;
				stdOutAppend = append;
			} else if(stdErr) {
				if(s.equals("&1")) {
					combineOutput = true;
				} else {
					stdErrStr = s;
					stdErrAppend = append;
				}
			} else {
				if(cmdList.size() == 0) {
					command = s;
				}
				cmdList.add(s);
			}
		}

		public void setRedirects(ProcessBuilder pb, String homeDir, String defaultOut) {
			String out = stdOutStr;
			if(out == null) {
				out = defaultOut;
			}
				
			if(out.charAt(0) != '/' && homeDir != null) {
				out = homeDir + "/" + out;
			}
			if(homeDir != null)
				pb.directory(new File(homeDir));
			
			stdOutStr = out;
			//For the moment, we ignore stderr and combine outputs
			//TODO Operate separate streams for stderr and stdout
			pb.redirectErrorStream(true);
			combineOutput = true;
			
			if(stdInStr != null) {
				pb.redirectInput(new File(stdInStr));
			}
			
		}

		public void setCommand(String path) {
			String cmd = path + "/" + command;
			cmdList.remove(0);
			cmdList.add(0, cmd);
		}

		/**
		 * Add an an array of environment settings to the command.
		 * These should be an array of Strings, each one of the form NAME=VALUE
		 * @param envp
		 */
		public void setEnvironment(String[] envp) {
			if(envp == null)
				return;
			this.envp = new ArrayList<String>();
			for(String s : envp) {
				this.envp.add(s);
			}
		}
		
		/**
		 * Add a new variable of the form "KEY=VALUE"
		 * @param env
		 */
		public void addToEnvironment(String env) {
			if(env == null)
				return;
			if(this.envp == null)
				this.envp = new ArrayList<String>();
			envp.add(env);
		}
		
	}
	
	/**
	 * 
	 * Path(s) value set by the Config class can contain multiple paths delimteted by : or ;
	 * 
	 */
	public synchronized static void setPath(String path) {
		if(path == null || path.length() == 0) {
			initialised = false;
			command = null;
			return;
		}
		if(!initialised) {
			command = new Command();
			initialised = true;
		}
		if(path.indexOf(";") != -1) command.pathList = path.split(";");
		else if(path.indexOf(":") != -1) command.pathList = path.split(":");
		else command.pathList = new String[] { path };
		
		//for(int i=0; i < command.pathList.length ; i++) {
		//	log.debug("pathList["+i+"]="+command.pathList[i]);
		//}
	}
	
	/**
	 * 
	 * Executing Shell value set by the Config class
	 * 
	 * Note this value is used in ProcessBuilder which is not synchronized 
	 * consequently this methods is.
	 * 
	 * @param s shell to execute under
	 * 
	 */
	 public synchronized static void setShell(String s) {
		if(!initialised) {
	        command = new Command();
			initialised = true;
		}       
		if(s == null || s.length() == 0) {
	        //command.shell = null;
			return;
		} //else command.shell = s;
	}
	
	/**
	 * 
	 * Home directory Value set by the Config class
	 * If null the output of the process will be directed to the specified stdout file.
	 * If set the output will be directed to the stdout file location located below the 
	 * homedirectory e.g. Home_Dir/tt1/foo.out
	 * If not set (null) the above would translate to an output location of /tt1/foo.out
	 *  
	 * Note this value is used in ProcessBuilder which is not synchronized 
	 * consequently this methods is.

	 * @param s home directory location
	 * 
	 */
	public synchronized static void setHomeDirectory(String s) {
		if(!initialised) {
			command = new Command();
			initialised = true;
		}       
		if(s == null || s.length() == 0) {
			command.homeDir = null;
			return;
		}
		command.homeDir = s;
	}
	
	
	/**
	 * 
	 * Stdout value set by the Config class
	 * 
	 * Note this value is used in ProcessBuilder which is not synchronized 
	 * consequently this methods is.
	 * 
	 * @param s stdout definition /dev/null or /tt1/foo.out
	 * 
	 */	
	public synchronized static void setStdOut(String s) {
		if(!initialised) {
			command = new Command();
			initialised = true;
		}       
		if(s == null || s.length() == 0) {
			command.outputFile = "/dev/null";
			return;
		}
		command.outputFile = s;
	}

	private static List<String> makeList(String s1, String s2) {
		ArrayList<String> list = new ArrayList<String>();
		list.add(s1);
		if(s2 != null && s2.length() > 0) list.add(s2);
		return list;
	}
	
	/**
	 * execute an external command/process
	 * 
	 * @param c the command to execute
	 */
	public static int execute(String c) {
		CommandSet commandSet = new CommandSet(c);
		return execute(commandSet);
	}	
	
	/**
	 * Using the jclib VAR class to define the executable name
	 * 
	 * @param c the command to execute
	 */
	public static int execute(Var c) {
		CommandSet commandSet = new CommandSet(c.toString());
		return execute(commandSet);
    }

	/**
	 * Using the jclib VAR class to define the executable name and argument
	 * 
	 * @param c the command to execute
	 * @param p the argument to the command (the first one)
	 */
	public static int execute(Var c,Var p) {
		CommandSet commandSet = new CommandSet(c.toString(), p.toString());
		return execute(commandSet);
    }

	/**
	 * Using the jclib {@link VAR} class to define the executable name a string for the argument
	 * 
	 * @param c the command to execute
	 * @param p the argument to the command (the first one)
	 */
	public static int execute(Var c, String p) {
		CommandSet commandSet = new CommandSet(c.toString(), p);
		return execute(commandSet);
    }
	
	/**
	 * Using a String to define the executable name the jclib {@link VAR} class to define for the argument
	 * 
	 * @param c the command to execute
	 * @param p the argument to the command (the first one)
	 */
    public static int execute(String c, Var p) {
		CommandSet commandSet = new CommandSet(c, p.toString());
		return execute(commandSet);
    }
    
	/**
	 * Using a String to define the executable and argument 
	 * 
	 * @param c the command to execute
	 * @param p the argument to the command (the first one)
	 */    
	public static int execute(String c, String p) {
		CommandSet commandSet = new CommandSet(c, p);
		return execute(commandSet);
	}
	
	/**
	 * 
	 * Provide an Array of String 0th index is the command index 1 - n are the arguments.
	 * 
	 * @param args the command and arguments to execute
	 */
	public static int execute(String ...  args) {
		CommandSet commandSet = new CommandSet(args);
		return execute(commandSet);
	}
	
	/**
	 * 
	 * Provide an explicit String for the command and an Array of String for the arguments.
	 * 
	 * @param c the command
	 * @param argumentList the arguments as an array
	 */
	public static int execute(String c, String [] argumentList) {
		CommandSet commandSet = new CommandSet(c, argumentList);
		return execute(commandSet);
	}

	/**
	 * 
	 * Provide an List of String 0th index is the command index 1 - n are the arguments.
	 * In effect this maps 1 to 1 to ProcessBuilder.
	 * 
	 * @param args the command and arguments to execute
	 */
	public static int execute(CommandSet commandSet) {
		if(initialised) return command.executeNoWait(commandSet, true);
		//System.out.println("not initialised");
		return -1;
	}

	public static int execute(boolean wait, CommandSet commandSet) {
		if(initialised) return command.executeNoWait(commandSet, wait);
		//System.out.println("not initialised");
		return -1;
	}

	/**
	 * execute an external command/process
	 * 
	 * @param GLB capture the success/failure of process initiation.
	 * @param c the command to execute
	 */
	public static int execute(Glb GLB, String c) {
		return execute(GLB, c, "");
	}

	/**
	 * Using the jclib VAR class to define the executable name
	 * 
	 * @param GLB capture the success/failure of process initiation.
	 * @param c the command to execute
	 */
	public static int execute(Glb GLB, Var c) {
		return execute(GLB, c.getString(), "");
	}
	
	
	/**
	 * Using the jclib {@link VAR} class to define the executable name a string for the argument
	 * 
	 * @param GLB capture the success/failure of process initiation.
	 * @param c the command to execute
	 * @param p the argument to the command (the first one)
	 */
	public static int execute(Glb GLB, Var c, String p) {
		return execute(GLB, c.getString(), p);
	}

	/**
	 * Using a String to define the executable name the jclib {@link VAR} class to define for the argument
	 * 
	 * @param GLB capture the success/failure of process initiation.
	 * @param c the command to execute
	 * @param p the argument to the command (the first one)
	 */
	public static int execute(Glb GLB, String c, Var p) {
		return execute(GLB, c, p.getString());
	}
	/**
	 * Using the jclib VAR class to define the executable name and argument
	 * 
	 * @param GLB capture the success/failure of process initiation.
	 * @param c the command to execute
	 * @param p the argument to the command (the first one)
	 */
	public static int execute(Glb GLB, Var c, Var p) {
		return execute(GLB, c.getString(), p.getString());
	}

	/**
	 * Using a String to define the executable and argument 
	 * 
	 * @param GLB capture the success/failure of process initiation.
	 * @param c the command to execute
	 * @param p the argument to the command (the first one)
	 */    
	public static int execute(Glb GLB, String c, String p) {
		remoteStation = GLB.REMOTESTATION.toString();
		if(remoteStation == null || remoteStation.length() == 0)
			remoteStation = GLB.STN.toString();
		GLB.REMOTESTATION.set("");
		int rval = execute(c, p);
		if(rval < 0) GLB.STATUS.set("INVAL");
		else GLB.STATUS.set(" ");
		return rval;
	}
	
	/**
	 * Execute the WFL shell (runwfl.sh) with GLB status and command 
	 * 
	 * @param GLB capture the success/failure of process initiation.
	 * @param c the command to execute in the custom runwfl.sh
	 */    
	public static int startWfl(Glb GLB, String c) {
		return startWfl(GLB, c, "");
	}

	/**
	 * Execute the WFL shell (runwfl.sh) with GLB status and command 
	 * 
	 * @param GLB capture the success/failure of process initiation.
	 * @param c the command to execute in the custom runwfl.sh
	 */    
	public static int startWfl(Glb GLB, Var c) {
		return startWfl(GLB, c.getString(), "");
	}
	
	/**
	 * Execute the WFL shell (runwfl.sh) with GLB status and command 
	 * 
	 * @param GLB capture the success/failure of process initiation.
	 * @param c the command to execute in the custom runwfl.sh
	 * @param p the argumant for the command in the custom wfl shell
	 */    
	public static int startWfl(Glb GLB, Var c, String p) {
		return startWfl(GLB, c.getString(), p);
	}

	/**
	 * Execute the WFL shell (runwfl.sh) with GLB status and command 
	 * 
	 * @param GLB capture the success/failure of process initiation.
	 * @param c the command to execute in the custom runwfl.sh
	 * @param p the argument for the command in the custom wfl shell
	 */    
	public static int startWfl(Glb GLB, String c, Var p) {
		return startWfl(GLB, c, p.getString());
	}
	
	/**
	 * Execute the WFL shell (runwfl.sh) with GLB status and command 
	 * 
	 * @param GLB capture the success/failure of process initiation.
	 * @param c the command to execute in the custom runwfl.sh
	 * @param p the argument for the command in the custom wfl shell
	 */    
	public static int startWfl(Glb GLB, Var c, Var p) {
		return startWfl(GLB, c.getString(), p.getString());
	}

	/**
	 * Execute the WFL shell (runwfl.sh) with GLB status and command 
	 * 
	 * @param GLB capture the success/failure of process initiation.
	 * @param c the command to execute in the custom runwfl.sh
	 * @param p the argument for the command in the custom wfl shell
	 */    
	public static int startWfl(Glb GLB, String c, String p) {
		remoteStation = GLB.REMOTESTATION.toString();
		if(remoteStation == null || remoteStation.length() == 0)
			remoteStation = GLB.STN.toString();
		GLB.REMOTESTATION.set("");
		int rval = execute("runwfl.bat",c, p);
		if(rval < 0) GLB.STATUS.set("INVAL");
		else GLB.STATUS.set(" ");
		return rval;
	}
	
	/**
	 * Execute the WFL shell (runwfl.sh) a command 
	 * 
	 * @param c the command to execute in the custom runwfl.sh
	 */    
	public static int startWfl(String c) {
		return startWfl(c, "");
	}

	/**
	 * Execute the WFL shell (runwfl.sh) a command 
	 * 
	 * @param c the command to execute in the custom runwfl.sh
	 */    
	public static int startWfl(Var c) {
		return startWfl(c.getString(), "");
	}
	
	/**
	 * Execute the WFL shell (runwfl.sh) a command 
	 * 
	 * @param c the command to execute in the custom runwfl.sh
	 * @param p the argument for the command in the custom wfl shell
	 */    
	public static int startWfl(Var c, String p) {
		return startWfl(c.getString(), p);
	}

	/**
	 * Execute the WFL shell (runwfl.sh) a command 
	 * 
	 * @param c the command to execute in the custom runwfl.sh
	 * @param p the argument for the command in the custom wfl shell
	 */    
	public static int startWfl(String c, Var p) {
		return startWfl(c, p.getString());
	}
	
	/**
	 * Execute the WFL shell (runwfl.sh) a command 
	 * 
	 * @param c the command to execute in the custom runwfl.sh
	 * @param p the argument for the command in the custom wfl shell
	 */    
	public static int startWfl(Var c, Var p) {
		return startWfl(c.getString(), p.getString());
	}
	
	/**
	 * Execute the WFL shell (runwfl.sh) a command 
	 * 
	 * @param c the command to execute in the custom runwfl.sh
	 * @param p the argument for the command in the custom wfl shell
	 */    
	public static int startWfl(String c, String p) {
		int rval = execute("runwfl.bat",c, p);
		return rval;
	}
	/**
	 * Execute the UNIX shell (runReport.sh) passing report name and device type
	 * 
	 * @param GLB GLB to capture the success/failure of process initiation.
	 * @param report The report to execute via the custom runReport.sh
	 */    
	public static int runReport(Glb GLB, String report) {
		return runReport(GLB, report, "","");
	}

	/**
	 * Execute the UNIX shell (runReport.sh) passing report name and device type
	 * 
	 * @param GLB GLB to capture the success/failure of process initiation.
	 * @param report The report to execute via the custom runReport.sh
	 */    
	public static int runReport(Glb GLB, Var report) {
		return runReport(GLB, report.getString(), "", "");
	}
	
	/**
	 * Execute the UNIX shell (runReport.sh) passing report name and device type
	 * 
	 * @param GLB GLB to capture the success/failure of process initiation.
	 * @param report The report to execute via the custom runReport.sh
	 * @param device Device type (LP,RP,VD)
	 */    
	public static int runReport(Glb GLB, Var report, String device) {
		return runReport(GLB, report.getString(), device,"");
	}

	/**
	 * Execute the UNIX shell (runReport.sh) passing report name and device type
	 * 
	 * @param GLB GLB to capture the success/failure of process initiation.
	 * @param report The report to execute via the custom runReport.sh
	 * @param device Device type (LP,RP,VD)
	 */    
	public static int runReport(Glb GLB, String report, Var device) {
		return runReport(GLB, report, device.getString(),"");
	}
	/**
	 * Execute the UNIX shell (runReport.sh) passing report name and device type
	 * 
	 * @param GLB GLB to capture the success/failure of process initiation.
	 * @param report The report to execute via the custom runReport.sh
	 * @param device Device type (LP,RP,VD)
	 */    
	public static int runReport(Glb GLB, Var report, Var device) {
		return runReport(GLB, report.getString(), device.getString(),"");
	}
	/**
	 * Execute the UNIX shell (runReport.sh) passing report name and device type
	 * 
	 * @param GLB GLB to capture the success/failure of process initiation.
	 * @param report The report to execute via the custom runReport.sh
	 * @param device Device type (LP,RP,VD)
	 */    
	public static int runReport(Glb GLB, String report, String device) {
		return runReport(GLB, report, device,"");
		
	}
	

	/**
	 * This method is called from ReportRunner for running online reports.
	 * @param repName
	 * @param envp  Array of XX=YY items to place into environment.
	 * @return an error message, else null.
	 */
	public static String runReport(String repName, String[] envp) {
		CommandSet commandSet = new CommandSet("runReport.bat", repName);
		commandSet.setEnvironment(envp);
		execute(commandSet);

		return null;
	}
	


	/**
	 * Execute the UNIX shell (runReport.sh) passing report name and device type
	 * 
	 * @param GLB GLB to capture the success/failure of process initiation.
	 * @param report The report to execute via the custom runReport.sh
	 * @param device Device type (LP,RP,VD)
	 * @param param Report parameters
	 */    
	public static int runReport(Glb GLB, String report, String device, String param) {
		//int rval = execute("runReport.sh",report, device, GLB.STN.getString(), param);
		remoteStation = GLB.REMOTESTATION.toString();
		if(remoteStation == null || remoteStation.length() == 0)
			remoteStation = GLB.STN.toString();
		GLB.REMOTESTATION.set("");
		int rval = execute("runReport.bat",report, device, GLB.STN.getString(), param);
		if(rval < 0) GLB.STATUS.set("INVAL");
		else GLB.STATUS.set(" ");
		return rval;
	}
	/**
	 * Execute the UNIX shell (runReport.sh) passing report name and device type
	 * 
	 * @param GLB GLB to capture the success/failure of process initiation.
	 * @param report The report to execute via the custom runReport.sh
	 * @param device Device type (LP,RP,VD)
	 * @param param Report parameters
	 */    
	public static int runReport(Glb GLB, String report, String device, Var param) {
		return runReport(GLB, report, device, param.getString());
		
	}
	/**
	 * Execute the UNIX shell (runReport.sh) passing report name and device type
	 * 
	 * @param report The report to execute via the custom runReport.sh
	 */    
	public static int runReport(String report) {
		return runReport(report, "");
	}

	/**
	 * Execute the UNIX shell (runReport.sh) passing report name and device type
	 * 
	 * @param report The report to execute via the custom runReport.sh
	 */     
	public static int runReport(Var report) {
		return runReport(report.getString(), "");
	}
	
	/**
	 * Execute the UNIX shell (runReport.sh) passing report name and device type
	 * 
	 * @param report The report to execute via the custom runReport.sh
	 * @param device Device type (LP,RP,VD)
	 */    
	public static int runReport(Var report, String device) {
		return runReport(report.getString(), device);
	}

	/**
	 * Execute the UNIX shell (runReport.sh) passing report name and device type
	 * 
	 * @param report The report to execute via the custom runReport.sh
	 * @param device Device type (LP,RP,VD)
	 */    
	public static int runReport(String report, Var device) {
		return runReport(report, device.getString());
	}
	
	/**
	 * Execute the UNIX shell (runReport.sh) passing report name and device type
	 * 
	 * @param report The report to execute via the custom runReport.sh
	 * @param device Device type (LP,RP,VD)
	 */    
	public static int runReport(Var report, Var device) {
		return runReport(report.getString(), device.getString());
	}
	
	/**
	 * Execute the UNIX shell (runReport.sh) passing report name and device type
	 * 
	 * @param report The report to execute via the custom runReport.sh
	 * @param device Device type (LP,RP,VD)
	 */    
	public static int runReport(String report, String device) {
		int rval = execute("runReport.sh",report,device);
		return rval;
	}
	

	/**
	 * 
	 * Key execution function relying on java.lang.ProcessBuilder and java.lang.Process 
	 * to initiate the requests made for execution within the custom Unix shell or not.
	 * 
	 * The process iterates the paths looking for the command and initiates it.
	 * The generated InputStream is then pumped to the defined stdout.
	 * stderr is also directed to stdout.
	 * 
	 * @param command and arguments or in the case of the custom wfl the shell, command within shell and then arguments.
	 * @return 0 if process found and started -1 if not found.
	 */
	private int executeNoWait(CommandSet commandSet, boolean wait) {
		if(commandSet == null)
			return -1;
		
		ProcessBuilder pb = new ProcessBuilder(commandSet.cmdList);
		commandSet.setRedirects(pb, homeDir, outputFile);
		pb.command(commandSet.cmdList);
		
		if(remoteStation != null && remoteStation.trim().length() > 0 && 
           !remoteStation.equals("BATCH")) {
			log.debug("Setting {} to {}",Batch.BC_LOOKUPSTATION.toString(),remoteStation.trim());
			commandSet.addToEnvironment(Batch.BC_LOOKUPSTATION.toString() + "=" + remoteStation.trim());
			remoteStation = null;
		}
		
		if(commandSet.envp != null) {
			Map<String, String>envMap = pb.environment();
			for(String s : commandSet.envp) {
				String [] nv = s.split("=");
				if(nv.length != 2)
					continue;
				envMap.put(nv[0], nv[1]);
			}
		}
		
		if(log.isInfoEnabled())
			log.info("Command={}", commandSet.toString());
		if(pathList == null || pathList.length == 0) {
			IllegalStateException e = new IllegalStateException("pathList cannot be null or empty");
			log.error("Null pathlist", e);
			return -1;
		}
		IOException ex = null;
		for(int i=0; pathList != null && i < pathList.length; i++) {
			try {
				commandSet.setCommand(pathList[i]);
				if(log.isDebugEnabled())
					log.debug("attempt: {}", commandSet.toString());
				Process process = pb.start();
				StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), "OUTPUT", commandSet);
				outputGobbler.start();
				if(wait) {
					process.waitFor();
				}
				ex = null;
				break;
			} catch(IOException e) {
				ex = e;
				continue;
			}
			catch (InterruptedException e) {
				log.error("Error occured while executing Linux command \"{}\"\nError Description: {}",
					commandSet.command, e.getMessage());
				continue;
			}
		}
		if(ex != null) {
			log.error("Command {} could not be executed.", commandSet.command, ex);
			return -1;
		}
		/*
		try{
			Thread.sleep(10);
		} catch (InterruptedException e) {
		}*/
		return 0;
	}
}

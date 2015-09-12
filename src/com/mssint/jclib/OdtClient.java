package com.mssint.jclib;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * 
 * 
 * @author MssInt
 *
 */
public class OdtClient {
	private static final Logger log = LoggerFactory.getLogger(OdtClient.class);
	private static boolean connected = false;
//	private String host;
//	private int port;
	private Socket socket;
	
	/**
	 * 
	 * Enumeration of message types that are known to client and server.
	 * 
	 * IDENTITY along with program name is used when first establishing 
	 * the connection
	 * BAD_TYPE is used when toMsgType is used and an unknown identifier is provided. 
	 * 
	 * @author MssInt
	 *
	 */
	public static enum MsgType {
		BAD_TYPE,
		WAITING_ENTRY,
		COMMAND,
		DISPLAY,
		IDENTITY,
		RESPONSE
	};
	
	/**
	 * Get short form name of MsgType enumeration
	 * 
	 * @param type {@see #MsgType}
	 * @return Single uppercase String to be used in sockect msgs.
	 */
	public String toString(MsgType type) {
		switch(type) {
		case WAITING_ENTRY: return "W";
		case COMMAND: return "C";
		case DISPLAY: return "D";
		case IDENTITY: return "I";
		case RESPONSE: return "R";
		case BAD_TYPE: return null;
		}
		return null;
	}
	
	/**
	 * Get the MsgType enumeration value given it's short socket form.
	 * If the type is unknown BAD_TYPE is returned.
	 * 
	 * @param c the character representing the MsgType short form.
	 * @return the enumerated value.
	 */
	public MsgType toMsgType(byte c) {
		switch(c) {
		case 'W': return MsgType.WAITING_ENTRY;
		case 'C': return MsgType.COMMAND;
		case 'D': return MsgType.DISPLAY;
		case 'I': return MsgType.IDENTITY;
		case 'R': return MsgType.RESPONSE;
		}
		return MsgType.BAD_TYPE;
	}
	
	/**
	 * Constructor for the OdtClient 
	 * 
	 * @param host the Server that the OdtMonitor will connect via a socket to.
	 * @param port the port to connect on.
	 * @param program the name of the program i.e. it's identity.
	 * 
	 */
	public OdtClient(String host, int port, String program) {
		if(host == null || port == 0) {
			connected = false;
		} else {
			try {
				socket = new Socket(host, port);
				String p;
				if(program == null) p = "unknown";
				else p = program;
				connected = true;
				sendMessage(MsgType.IDENTITY, "type=program;pid="+Util.getPid()+";program="+p);
			} catch (IOException e) {
				log.error("Error connecting to ODT Monitor at "+host+":"+port);
				socket = null;
				connected = false;
			}
		}
	}
	
	/**
	 * Close the connection to the ServerSocket
	 * 
	 */
	public void close() {
		try {
			if(connected) socket.close();
		} catch (IOException e) {
		}
		connected = false;
	}
	
	/**
	 * Send a command to the ODT monitor and wait for a response
	 * @param cmd The command to send
	 * @return Response as a string
	 * @throws IOException 
	 */
	public String sendCommand(String cmd) throws IOException {
		if(!connected) return null;
		sendMessage(MsgType.COMMAND, cmd);
		
		String rv = recvMessage();
		return rv;
	}
	
	/**
	 * A customised reply message sent to the server which is og MsgType.Response.
	 *  
	 * @param ?????????
	 * @param ?????????
	 * @param ?????????
	 * @param ?????????
	 */
	public void sendReply(int socket, String odtAction, int seq, String msg) throws IOException {
//		 * sequence=nnn;pid=nnn;socket=nnn;program=xxx;message=zzzzz
		String m = "action="+odtAction+";sequence="+seq+";socket="+socket+";message="+msg;
		sendMessage(MsgType.RESPONSE, m);

	}

	/**
	 * Sends a message to the OdtMonitor
	 * 
	 * @param type the MsgType being sent
	 * @param msg the MsgType being sent
	 */
	public void sendMessage(MsgType type, String msg) throws IOException {
		if(!connected) return;
		String m = String.format("%05d%s%s", msg.length() + 6, toString(type), msg);

		OutputStream out = socket.getOutputStream();
		out.write(m.getBytes());
		out.flush();
	}
	
	/**
	 * Recieve mesage from the OdtMonitor
	 * 
	 * @return message as string but only if the MsgType has a value of RESPONSE (R) 
	 */
	public String recvMessage() throws IOException {
		if(!connected) return null;
		
		InputStream in = socket.getInputStream();
		int len;
		byte [] buf;
		buf = new byte[6];
		int l = in.read(buf, 0, 6);
		if(l < 0) return null;
		if(l < 6) throw new IOException("Unable to read 6 byte header.");
		String s = new String(buf, 0, 5);
		len = Integer.parseInt(s);
		len -= 6;
		if(len < 0) throw new IOException("Length indicator in string '"+s+"' is too short.");
		MsgType type = toMsgType(buf[5]);
		
		int totlen = len;
		len = 0;
		buf = new byte[totlen];
		while(len < totlen) {
			l = in.read(buf, len, totlen - len);
			if(l < 0) throw new IOException("Premature EOF on socket read.");
			len += l;
		}
		
		if(type == MsgType.RESPONSE)
			return new String(buf, 0, len);
		throw new IOException("Unhandled message type: " + toString(type));
	}
	
	/**
	 * Property for determining the OdtClient has a Socket connection to the OdtMonitor.
	 * 
	 * @return true if connected false if not. 
	 * 
	 */
	public boolean isConnected() { return connected; }
}


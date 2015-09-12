package com.mssint.jclib;
/**
 * @author Peter Colman (pete@mssint.com)
 * 
 * Unchecked exception
 * 
 * When a Jbol program encounters an endProgram() it is meant, in Cobol, to 
 * completely end the program. However, we cannot call System.exit() because
 * this program may be running as a thread inside a container - it might
 * also be many levels of "perform" in, and may even have executed another 
 * sub-program. So we throw an unchecked exception so that, if the calling
 * routine traps it, we can effectively exit from any level to the controlling
 * level.
 *
 */

public class EndJbolProgramException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	String message;
	public EndJbolProgramException() {
		super();
	}
	
	public EndJbolProgramException(String message) {
		super(message);
		this.message = message;
	}
	
	public EndJbolProgramException(String message, Throwable cause) {
		super(message, cause);
	}

	public EndJbolProgramException(Throwable cause) {
		super(cause);
	}
	
	@Override
	public String toString() {
		return super.toString();
	}
	
	@Override
	public String getMessage() {
		if(message == null) return super.getMessage();
		return message;
	}
}

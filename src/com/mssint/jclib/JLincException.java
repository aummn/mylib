package com.mssint.jclib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Custom jlinc application specific exception extends exception. 
 * 
 * @author MssInt
 *
 */
public class JLincException  extends Exception {
	private static final long serialVersionUID = 5680720094059808284L;
	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(JLincException.class);

	/**
	 * Default ctor
	 */
	public JLincException() {
	}

	/**
	 * Overriden ctor
	 * @param message the error message
	 */
	public JLincException(String message) {
		super(message);
	}

	/**
	 * Overriden ctor
	 * @param cause the Throwable instance
	 */
	public JLincException(Throwable cause) {
		super(cause);
	}

	/**
	 * Overriden ctor
	 * @param message the error message
	 * @param cause the Throwable instance
	 */
	public JLincException(String message, Throwable cause) {
		super(message, cause);
	}
}

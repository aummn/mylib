package com.mssint.jclib;

import java.io.UnsupportedEncodingException;

import org.jdom.Document;

/**
 * Define the primary/core functionality required by a Terminal Emulator.
 *  
 * @author Peter Colman
 *
 */
public interface TerminalEmulatorInterface {
	/**
	 * Perform necessary initialisation and set terminal characteristics.
	 */
	public void init();
	
	/**
	 * Processes the message received from the TP monitor. The document doc will be 
	 * modified according to the message. screen item fields and text fields may be 
	 * added or removed as the message is processed. On completion, the document is
	 * expected to be in a state which can be passed directly to the xslt processor
	 * for rendering into HTML or whatever.
	 * @param doc The JDom Document to be modified
	 * @param mesg he raw byte array message from the MCS or TP monitor
	 * @param len The length of the message
	 * @return true for successful processing, otherwise false.
	 */
	public boolean processMcsMessage(ScreenState state, Document doc, byte [] mesg, int len);
	
	/**
	 * Creates a representation of a screen, with an array of rows and columns.
	 * @param mesg The message to process into the terminal emulation.
	 * @return A 2 dimensional array representation of a screen.
	 * @throws UnsupportedEncodingException
	 */
	public byte [][] createScreenImage(String mesg) throws UnsupportedEncodingException;
	
	/**
	 * Update the form field values in the virtual screen map with values typed by user.
	 * @param values
	 * @throws UnsupportedEncodingException
	 */
	public void updateFields(String values) throws UnsupportedEncodingException;
	
	/**
	 * Gets the number of rows the terminal can display
	 * @return An integer indicating the number of rows
	 */
	public int getRows();
	
	/**
	 * Gets the number of columns the terminal can display
	 * @return An integer indicating the number of columns
	 */
	public int getColumns();
	
	/**
	 * Some terminals show a status row indicating various aspects regarding the operation
	 * of the terminal. This should return a string of exactly getColumns() characters, 
	 * which presents a status line.
	 * @return The status string, or else null.
	 */
	public String getStatusRow();
	
	/**
	 * Returns the current cursor position as (row+1)*(col+1)-1 
	 * @return 0 relative cursor position counted from top-left of screen.
	 */
	public int getCursorPosition();
	
	/**
	 * Set the cursor to the specified location on the screen
	 * @param row the row
	 * @param col the column
	 */
	public void setCursorPosition(int row, int col);
	
	/**
	 * Returns true or false depending on whether a terminal uses a visible space for
	 * screen attributes.
	 * @return true for a screen which uses a visible space otherwise false.
	 */
	public boolean attributeSpace();
	
	/**
	 * Clear the internal screen map
	 */
	public void clearScreen();
	
	/**
	 * Return a string in the form of a formatted message which can be displayed
	 * on a blank form.
	 * @param msg The application error message
	 * @return A formatted string.
	 */
	public String errorScreen(String msg);

	/**
	 * Set the screen's character encoding plan. The default is taken from the main
	 * properties file as "character.set"
	 * @param encoding The character set encoding plan. e.g. iso8859_1
	 */
	public void setEncoding(String encoding);

	/**
	 * Return the byte array representing the screen
	 * @return
	 */
	public byte[][] getScreen();

}

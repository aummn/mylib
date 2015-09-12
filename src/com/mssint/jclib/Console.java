package com.mssint.jclib;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.fusesource.jansi.AnsiConsole;

/**
 * @author Peter Colman (pete@mssint.com)
 * 
 * Uses JAnsi to display and accept text at specific locations on a 
 * console screen.
 */
public class Console {
	//Initialisation
	static {
		AnsiConsole.systemInstall();
	}
	
	
	//Ansi control sequences
	
	public static final String ESC = "\u001b";
	public static final String CSI = "\u001b[";
	public static final String CLS = CSI + "2J";
	
	// Cursor movements
	public static final String HOME = CSI + "H";
	public static final String CUU = CSI + "1A";		/// cursor up
	public static final String CUD = CSI + "1B";		/// cursor down
	public static final String CUF = CSI + "1C";		/// cursor right (forward)
	public static final String CUB = CSI + "1D";		/// cursor left (back)
	public static final String CNL = CSI + "1E";		/// cursor newline (beginning of next line)
	public static final String CPL = CSI + "1F";		/// cursor to beginning of Previous Line
	public static final String CUP = CSI + "y;xH";		/// cursor position, row y column x
	public static final String E2EOS = CSI + "0J";		/// erase from cursor to end-of-screen
	public static final String E2BOS = CSI + "1J";		/// erase from cursor to beginning of screen
	public static final String E2EOL = CSI + "0K";		/// erase from cursor to end of line
	public static final String E2BOL = CSI + "1K";		/// erase from cursor to beginning of line
	public static final String SU	  = CSI + "1S";		/// scroll up one line
	public static final String SD    = CSI + "1T";		/// scroll down one line
	public static final String SCP   = CSI + "s"; 		/// save cursor position
	public static final String RCP   = CSI + "u"; 		/// restore cursor position
	
	
	public static final String BOLD = CSI + "1m";
	public static final String AT55 = CSI + "10;10H";
	public static final String REVERSEON = CSI + "7m";
	public static final String NORMAL = CSI + "0m";
	public static final String WHITEONBLUE = CSI + "37;44m";

	
	
	private int crow; //Current row
	private int ccol; //current column
	private int rows; //Number of rows on screen
	private int cols; //Number of columns on screen
	
	/**
	 * Create a new Console object. This creates a new console with defaults
	 * as shown:
	 * Screen size is set to 25 rows and 80 columns.
	 * Cursor is positioned at row 1, column 1.
	 */
	public Console() {
		crow = 1;
		ccol = 1;
		rows = 25;
		cols = 80;
	}
	
	public void clearScreen() {
		AnsiConsole.out.print(CLS + HOME);
		AnsiConsole.out.flush();
	}
	
	/**
	 * Display text at the specified location. After completion, row is set to @row 
	 * and column is set to @col plus the length of the @text string. If the string
	 * is too long to fit on the screen, it is truncated.
	 * @param row The row to begin display.
	 * @param col The column to start the display
	 * @param text The text string to display.
	 */
	public void displayAt(int row, int col, String text) {
		setCursor(row, col, false);
		AnsiConsole.out.print(text);
		AnsiConsole.out.flush();
	}
	
	/**
	 * Display @text on the screen, starting at the current row and column.
	 * @param text
	 */
	public void display(String text) {
		AnsiConsole.out.print(text);
		AnsiConsole.out.flush();
	}
	
	public String accept() throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String s = in.readLine();
		if(s == null)
			s = "";
		return s;
	}
	
	public String acceptAt(int row, int col) throws IOException {
		setCursor(row, col, true);
		String s = accept();
		return s;
	}
	
	public String accept(Var var) throws IOException {
		String s = accept();
		var.set(s);
		return s;
	}
	
	public String acceptAt(int row, int col, Var var) throws IOException {
		String s = acceptAt(row, col);
		var.set(s);
		return s;
	}
	
	public void setRow(int row) {
		crow = row;
		if(crow > rows) {
			crow = rows;
		}
	}
	
	public void setCol(int col) {
		ccol = col;;
		if(ccol > cols) {
			ccol = cols;
		}
	}
	
	public void setCursor(int row, int col) {
		setCursor(row, col, false);
	}
	
	public void setCursor(int row, int col, boolean flush) {
		setRow(row);
		setCol(col);
		StringBuilder sb = new StringBuilder();
		sb.append(CSI);
		sb.append(row);
		sb.append(";");
		sb.append(col);
		sb.append("H");
		AnsiConsole.out.print(sb.toString());
		if(flush) {
			AnsiConsole.out.flush();
		}
	}
	
	public int getRow() {
		return crow;
	}
	
	public int getCol() {
		return ccol;
	}
	
	public void test() throws IOException {
		System.out.println("Testing console: If you can see this, it's not working!");
		clearScreen();
		display("Console Test:");
		display(" This is still on line 1");
		display("\nThis is line 2\n");
		display("This is a line that is longer than eighty characters. We're just going to keep on typing until it overflows and then we will see what happens.");
		displayAt(10, 30, "This is at row 10, column 30");
		displayAt(12,10, "Name ..............: ");
		displayAt(13,10, "Age ...............: ");
		displayAt(14,10, "Address ...........: ");
		String name = acceptAt(12, 31);
		String age = acceptAt(13, 31);
		String address = acceptAt(14, 31);
		
		displayAt(20, 10, name + " is " + age + " old and lives at " + address);
		//Move cursor home
		display(HOME);
	}
	
	public static void main(String [] args) throws IOException {
		Console c = new Console();
		c.test();
	}
}

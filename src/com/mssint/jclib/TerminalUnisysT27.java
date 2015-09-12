package com.mssint.jclib;

import java.io.UnsupportedEncodingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jdom.Document;
import org.jdom.Element;


/**
 * Provides an representation/implementation in java of a Unisys T27 terminal.
 * 
 * Data received is processed according to T27 control/data character sequence(s) rules.
 * 
 * @author Peter Colman
 *
 */
public class TerminalUnisysT27 implements TerminalEmulatorInterface	{
	private static final Logger log = LoggerFactory.getLogger(TerminalUnisysT27.class);

	final static byte NUL  = 0x00;  
	final static byte SOH  = 0x01; //A 
	final static byte STX  = 0x02; //B 
	final static byte ETX  = 0x03; //C End of transmission. All following characters ignored.
	final static byte EOT  = 0x04; //D 
	final static byte ENQ  = 0x05; //E 
	final static byte ACK  = 0x06; //F 
	final static byte CUR  = 0x07; //G Focus field
	final static byte BS   = 0x08; //H Back space
	final static byte HT   = 0x09; //I Tab
	final static byte LF   = 0x0A; //J Line feed
	final static byte VT   = 0x0B; //K Vertical tab
	final static byte FF   = 0x0C; //L Clear screen
	final static byte CR   = 0x0D; //M Carriage return
	final static byte SO   = 0x0E; //N Reverse Video On
	final static byte SI   = 0x0F; //O Underline On
	final static byte DLE  = 0x10; //P
	final static byte DC1  = 0x11; //Q If first char is DC1, no editing is done.
	final static byte DC2  = 0x12; //R (? ENTER FORMS MODE)
	final static byte DC3  = 0x13; //S Previous line  //(? EXIT FORMS MODE)
	final static byte DC4  = 0x14; //T Home cursor
	final static byte NAK  = 0x15; //U
	final static byte SYN  = 0x16; //V
	final static byte ETB  = 0x17; //W End of Message
	final static byte CAN  = 0x18; //X Blink On
	final static byte EM   = 0x19; //Y Secure (i.e. password fields)
	final static byte SUB  = 0x1A; //Z Bright On
	final static byte ESC  = 0x1B;  
	final static byte FS   = 0x1C; //^_  Transmittable Protected Field Start
	final static byte GS   = 0x1D; //^] Right Justified Unprotected Field Start
	final static byte RS   = 0x1E; //^^  Unprotected Field End
	final static byte US   = 0x1F; //Left Justified Unprotected Field Start  
	final static byte SP   = 0x20;
	final static byte DEL  = 0x7F;

	private int ROWS; //maximum rows on screen
	private int COLS; //maximum columns
	private int row;  //current row (0 relative)
	private int col;  //current column (0 relative)
	private int STATUSROW; 
	private byte [][] screen;
	private boolean protectedMode;
	@SuppressWarnings("unused")
	private boolean uppercaseMode;
	@SuppressWarnings("unused")
	private int TABS;
	private int nameSequence;
	private int posCol;
	private int posRow;

	final static int STYLE_NORMAL = 	00000001;
	final static int STYLE_REVERSE = 	00000002;
	final static int STYLE_UNDERLINE = 	00000004;
	final static int STYLE_BRIGHT = 	00000010;
	final static int STYLE_BLINK = 		00000020;
	final static int STYLE_PASSWD = 	00000040;
	final static int STYLE_PROTECTED = 	00000100;

	// Alignment constants
	final static int ALIGN_LEFT = 		00001000;
	final static int ALIGN_RIGHT = 		00002000;
	final static int ALIGN_CENTRE = 	00004000;

	// Field Types
	final static int FIELD_TEXT = 		00010000;
	final static int FIELD_ITEM = 		00020000;
	final static int FIELD_CONTROL = 	00100000;

	private String encoding;

	@Override
	public void init() {
		ROWS = 24;
		COLS = 80;
		TABS = 8;
		STATUSROW = 24;
		screen = new byte[ROWS+1][COLS];
		protectedMode = false;
		uppercaseMode = false;
		clearScreen();
		for(int i=0;i<COLS;i++) screen[STATUSROW][i] = ' ';
		nameSequence = 0;
		encoding = Config.getProperty("character.set");
		if(encoding == null) encoding = "cp1252";
	}

	@Override
	public boolean attributeSpace() {
		return true;
	}
	
	@Override
	public byte [][] getScreen() {
		return screen;
	}

	@Override
	public int getColumns() {
		return COLS;
	}

	@Override
	public int getRows() {
		return ROWS;
	}

	@Override
	public int getCursorPosition() {
		return (row+1)*(col+1)-1;
	}

	@Override
	public void setCursorPosition(int row, int col) {
		this.row = row;
		this.col = col;
	}

	@Override
	public String getStatusRow() {
		return null;
	}

	/**
	 * Clear the screen of any previous data.
	 * 
	 * If the screen is running in protected mode only data parts of the 
	 * screen are cleared.
	 * 
	 */
	public void clearScreen() {
		if(protectedMode) { //clear all editable fields
			int r = 0;
			int c = 0;
			boolean first = true;
			for(row=0;row<ROWS;row++) {
				for(col=0;col<COLS;col++) {
					if(screen[row][col] == US || screen[row][col] == GS || screen[row][col] == FS) {
						if(first) {
							r = row;
							c = col+1;
							if(c >= COLS) {
								r++;
								c = 0;
							}
							first = false;
						}
						col++;
						if(col >= COLS) {
							row++;
							col = 0;
						}
						while(col < COLS) {
							if(screen[row][col] != US && screen[row][col] != GS && screen[row][col] != FS) {
								if(screen[row][col] < ' ') break;
								screen[row][col] = ' ';
							}
							col++;
						}
					}
				}
			}
			row = r;
			col = c;
		} else {
			for(int i=0;i<ROWS;i++)
				for(int j=0;j<COLS;j++) 
					screen[i][j] = ' ';
			row = 0;
			col = 0;
		}
	}

	/**
	 * Clear to end of line on current row
	 */
	private void clearToEOL() {
		for(int i=col;i<COLS;i++)
			screen[row][i] = ' ';
	}

	/**
	 * Clear to end of screen from current row
	 */
	private void clearToEOS() {
		for(int i=row;i<ROWS;i++)
			for(int j=col;j<COLS;j++) 
				screen[i][j] = ' ';
	}

	/**
	 * Increment col move to next row if col exceeds COL
	 */
	private void incrementCol() {
		col++;
		if(col >= COLS) {
			row++;
			if(row >= ROWS) row = 0;
			col = 0;
		}
	}

	/**
	 * Insert a blank line at cursor position. All rows moved down.
	 */
	private void insertLine() {
		byte [] lastRow = screen[ROWS-1];
		for(int i=ROWS-1;i>row;i--) 
			screen[i] = screen[i-1];
		screen[row] = lastRow;
		for(int i=0;i<COLS;i++) screen[row][i] = ' ';
	}

	/**
	 * Delete line at cursor pos. Following lines moved up, blank line placed at end.
	 */
	private void deleteLine() {
		byte [] currentLine = screen[row];
		for(int i=row;i<(ROWS-1);i++)
			screen[i] = screen[i+1];
		for(int i=0;i<COLS;i++) currentLine[i] = ' ';
		screen[ROWS - 1] = currentLine;
	}

	//Sets cursor to start of first editable field (if protectedMode is true) else
	//set it to first non-editable character.
	private void homeCursor() {
		if(protectedMode) {
			nextEditableField(0, 0);
			return;
		}
		boolean inField = false;
		for(row=0;row<ROWS;row++) {
			for(col=0;col<COLS;col++) {
				if(inField && screen[row][col] == RS) {
					inField = false;
				} else if(screen[row][col] == US || screen[row][col] == GS || screen[row][col] == FS ) {
					inField = true;
				} else return;
			}
			//Field can't extend to next line
			inField = false;
		}
	}

	//Locate next editable field from current position.
	private void nextEditableField(int r, int c) {
		for(row=r;row<ROWS;row++) {
			for(col=c;col<COLS;col++) {
				if(screen[row][col] == US || screen[row][col] == GS || screen[row][col] == FS ) {
					incrementCol();
					return;
				}
			}
			c = 0;
		}
	}


	@Override
	public byte[][] createScreenImage(String mesg) throws UnsupportedEncodingException {
		byte [] bMesg = mesg.getBytes(encoding);
		protectedMode = false;
		buildVirtualScreen(bMesg, mesg.length());
		return screen;
	}

	//The main processing entry point.
	@Override
	public boolean processMcsMessage(ScreenState state, Document dom, byte[] mesg, int len) {

		protectedMode = false;
		buildVirtualScreen(mesg, len);
		nameSequence = 0;

		//Having processed the message and built or modified the virtual screen image,
		//we need to build the DOM and populate state variables.
		Element screenElement = dom.getRootElement().getChild("screen");
		screenElement.removeChildren("item");
		screenElement.removeChildren("textitem");
		Element item = new Element("item");
		item.setAttribute("id", "wm_concatValues");
		item.addContent(new Element("hidden").setText("true"));
		item.addContent(new Element("row").setText("0"));
		item.addContent(new Element("col").setText("0"));
		screenElement.addContent(item);
		state.cuField = null;
		buildDOM(state, screenElement);
		return true;
	}

	/**
	 * Update the form field values in the virtual screen map with values typed by user.
	 * @param values
	 * @throws UnsupportedEncodingException
	 */
	@Override
	public void updateFields(String values) throws UnsupportedEncodingException {
		if(values == null || values.length() == 0) return;
		boolean pmSave = protectedMode;
		if(log.isDebugEnabled())
			log.debug("update fields = [" + values + "]");
		protectedMode = true;
		byte [] b = values.getBytes(encoding);
		buildVirtualScreen(b, b.length);
		protectedMode = pmSave;
	}

	//TODO The last cursor position sequence - or wherever the cursor end up - if it's
	//in an editable field, that field must be the focus field for the screen.


	//Read the message from the MCS and build or modify the virtual screen buffers.
	private void buildVirtualScreen(byte [] mesg, int len) {
		byte [] saveLine;
		int i;
		//TODO protectedMode can be optionally cleared on xmit
		boolean fieldStart = false;
		//If there are no escape sequences then this is a simple message and
		//the screen must be cleared.
		for(i = 0; i < len; i++) {
			if((int)(mesg[i] & 0xff) <= ESC) break;
		}
		if(i >= len) clearScreen();

		//debug
		/*
		String file;
		int num = 1;
		while(true) {
			file = "/home/pete/tmp/t27msg." + num;
			File f = new File(file);
			if(!f.exists()) break;
			num++;
		}
		MssFile x = new MssFile(file);
		try {
			x.open("w");
			x.write(mesg, len);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		x.close();
		*/
	
		for(i=0; i<len; i++) {
			if(mesg[i] == 0) continue;

			switch(mesg[i]) {
			case ESC: //Various terminal modes.
				switch(mesg[++i]) {
				case '"': //set cursor position
					col = (int)(mesg[++i] & 0xff) - 32;
					row = (int)(mesg[++i] & 0xff) - 32;
					if(col >= COLS || row >= ROWS) {
						if(log.isDebugEnabled())
							log.debug("WARNING: Illegal cursor position request: <esc>\""+(char)(col-32)+(char)(row-32) );
						if(row >= ROWS) row = ROWS - 1;
						if(col >= COLS) col = COLS - 1;
					}
					break;
				case 'W': //enter protected mode
					protectedMode = true;
					homeCursor();
					break;
				case 'X': //exit protected mode
					protectedMode = false;
					homeCursor();
					break;
				case 'R': //Looking for RS
					if(mesg[i+1] != 'S') break;
					for(int j=0;j<COLS;j++) screen[STATUSROW][j] = ' ';
					i += 2;
					//The next 2 characters are hex digits:
					String hex = new String(mesg, i, 2);
					int l = Integer.parseInt(hex, 16);
					i += 2;
					if((i+l) > len) l = len - i;
					for(int j = 0; j < l; j++) {
						if(mesg[i+j] >= 0 && mesg[i+j] < ' '){ // control characters
							if(mesg[i+j] == ETX) { //Premature end of transmission
								l = j;
								break;
							} else if(j == 0 && mesg[i+j] != SUB){ // otherwise lets have the text bright 
								mesg[i+j] = '.';
							}
							else
								mesg[i+j] = '.';
						}
						if(j < COLS)
							screen[STATUSROW][j] = mesg[i+j];
					}
					i += l - 1;
					row = col = 0;
					break;
				case '!': //Insert character at cursor position
					insertInLine();
					break;
				case '%': //Insert character at cursor position
					deleteInLine();
					break;
				case '@': //Insert character at cursor position
					insertInPage();
					break;
				case 'P': //Insert character at cursor position
					deleteInPage();
					break;
				case 'J': //Erase to End of Screen
					clearToEOS();
					break;
				case 'K': //Erase to End of Line
					clearToEOL();
					break;
				case 'L': //Insert line
					if(!protectedMode) insertLine();
					break;
				case 'M': //Delete current line
					if(!protectedMode) deleteLine();
					break;
				case '>': //Swap current line with previous line
					//No action if in forms mode
					log.warn("Ignoring move line up");
					break;
				case '<': //Swap current line with next line
					//No action if in forms mode
					log.warn("Ignoring move line down");
					break;
				case 'S': //Roll all lines up
					if(protectedMode) break; //Illegal in protected mode
					//Top line is moved to bottom, all others moved up 1 line
					saveLine = screen[0];
					for(int k=0;k<(ROWS-1);k++) screen[k] = screen[k+1];
					screen[ROWS-1] = saveLine;
					break;
				case 'T': //Roll all lines down
					if(protectedMode) break; //Illegal in protected mode
					//Bottom line is moved to top, all others moved down 1 line
					saveLine = screen[ROWS-1];
					for(int k=ROWS-1;k>0;k--) screen[k] = screen[k-1];
					screen[0] = saveLine;
					break;
				case 'C': //Move cursor right
					incrementCol();
					break;
				case 'Y': //Set UPPERCASE for entire terminal
					uppercaseMode = true;
					break;
				case 'Z': //Set normal case mode for entire terminal
					uppercaseMode = false;
					break;
				case 'N': //Negative Video Enable - ignored
				case 'O': //Negative Video Disable - ignored
					log.warn("Ignoring Negative Video mode");
					break;
				case 'E': //Search Mode Enable - ignored
				case 'F': //Search Mode Disable - ignored
					log.warn("Ignoring Search mode");
					break;
				case '-': //Set Search Character - ignored
					i++; //ignore the actual search character
					log.warn("Ignoring Set Search Character");
					break;
				case '&':
					log.warn("Ignoring Data Communication Area Paging codes");
					break;
				case '$': //Select page <ESC>$<#>
					log.warn("Ignoring Select Page");
					i++;
					break;
				case '#': //Clear all variable tab stops
					log.warn("Ignoring clear variable tab stops");
					break;
				case ';': //Print from HOME to cursor position
					log.warn("Ignoring print screen");
					break;
				case ':': //Print from unprotected data
					log.warn("Ignoring print unprotected data");
					break;
				case '.': //Clear all variable tab stops
					log.warn("Ignoring set/reset variable tab stops");
					break;
				case '?': //Bell - ignore
					break;
				}
				break;
			case FF: //Clear screen
				clearScreen();
				break;
			case DC1:
			case STX:
			case ACK:
				break; //Ignore these
			case DC4: //Home cursor
				homeCursor();
				//row = col = 0;
				break;
			case CR: //Go to start of line (or 1st field in protected mode)
				if(!protectedMode) {
					col = 0;
					row++;
					if(row >= ROWS) row = 0;
				} else {
					if(row >= ROWS) row = 0;
					for(col=0;col<COLS;col++) {
						if(screen[row][col] == US || screen[row][col] == GS || screen[row][col] == FS ) {
							col++;
							break;
						}
					}
					if(col >= COLS) {
						col = 0;
						row++;
					}
				}
				break;
			case LF: //Move to next line and wrap at end
				row++;
				if(row >= ROWS) row = 0;
				break;
			case BS: //Move cursor left
				col--;
				if(col < 0) {
					col = COLS - 1;
					row--;
					if(row < 0) row = ROWS - 1;
				}
				break;
			case DC2:
				protectedMode = true;
				homeCursor();
				break;
			case DC3: //Previous line
				protectedMode = false;
				//row--;
				//if(row < 0) row = ROWS - 1;
				break;
			case HT: //Horizontal tab
				if(protectedMode) nextEditableField(row, col);
				else {
					col = ((col+8)/8) * 8;
					if(col >= COLS) {
						col = 0;
						row++;
						if(row >= ROWS) row = 0;
					}
				}
				break;
			case VT: //Vertical tab
				log.warn("Ignoring Vertical Tab");
				break;
			case ETX:
				len = i; //no more data
				break;
			default: //Add character to virtual screen and increment cursor.
				if(row >= ROWS) break;
				if(col >= COLS) col = 0;
				if(protectedMode) {
					byte b = screen[row][col];
					if(b == RS) //end of edit field
						nextEditableField(row, col);
					else if(b == US || b == GS || b == FS) {//Start of next edit field
						incrementCol();
					}
				} else {
					if(i < len && (mesg[i] == US || mesg[i] == GS || mesg[i] == FS)) {
						if(fieldStart) { //Duplicate field start characters
							//Convert the preceding start into an end
							if(col > 0) screen[row][col-1] = RS;
						}
						fieldStart = true;
					} else fieldStart = false;
				}
				if(i >= len || row >= ROWS) break;
				if(mesg[i] == 0) screen[row][col] = ' ';
				else if(mesg[i] == SUB) screen[row][col] = ' '; //Ignore BRIGHT
				else screen[row][col] = mesg[i];
				incrementCol();
				break;
			}

		}
		posRow = row;
		posCol = col;
	}

	private int reduceLength = 0;

	//Run through the virtual screen array and build the DOM
	private void buildDOM(ScreenState state, Element form) {
		int attr = 0;
		int startAttr = 0;
		boolean inField = false;
		int startCol = 0;
		boolean mark = false;
		boolean gotSpace = false;
		int pendingAttr = 0;

		for(row=0;row < (ROWS+1); row++) {
			for(col=0;col<COLS;col++) {
				switch(screen[row][col]) {
				case CAN: //BLINK
					attr = STYLE_BLINK;
					gotSpace = false;
					mark = true;
					break;
				case SO: //REVERSE
					attr = STYLE_REVERSE;
					gotSpace = false;
					mark = true;
					break;
				case SUB: //Bright
					attr = STYLE_BRIGHT;
					gotSpace = false;
					mark = true;
					break;
				case SI: //Underline
					attr = STYLE_UNDERLINE;
					gotSpace = false;
					mark = true;
					break;
				case EM: //Secure (i.e. password field
					pendingAttr = STYLE_PASSWD;
					reduceLength++; //Reduce length of previous field
					break;
				case US: //Start edit - left justified
					attr = FIELD_ITEM | ALIGN_LEFT | pendingAttr;
					pendingAttr = 0;
					gotSpace = false;
					mark = true;
					//System.out.println("cur("+row+","+col+") start edit");
					break;
				case GS: //Start edit - right justified
					attr = FIELD_ITEM | ALIGN_RIGHT | pendingAttr;
					pendingAttr = 0;
					//System.out.println("cur("+row+","+col+") start edit right justified");
					gotSpace = false;
					mark = true;
					break;
				case FS: //Start transmittable protected field
					attr = FIELD_ITEM | STYLE_PROTECTED | ALIGN_LEFT;
					pendingAttr = 0;
					mark = true;
					gotSpace = false;
					//System.out.println("cur("+row+","+col+") start edit protected transmittable");
					break;
				case RS: //End field
					attr = 0;
					pendingAttr = 0;
					mark = true;
					break;
				case ' ':
					if(inField && attr == FIELD_TEXT) {
						if(gotSpace) mark = true;
						else gotSpace = true;
					}
					break;
				default:
					if(screen[row][col] != ' ') {
						if(inField) {
							if(gotSpace) gotSpace = false;
						} else {
							startAttr = attr = FIELD_TEXT;
							startCol = col;
							inField = true;
						}
					}
					break;
				}
				if(mark) {
					if(inField) {
						addItem(state, form, startCol, startAttr);
						if(gotSpace) attr = 0;
					} else reduceLength = 0;
					startCol = col + 1;
					startAttr = attr;
					if(attr == 0) inField = false;
					else inField = true;
					mark = false;
					gotSpace = false;
				}
			}
			//End of line. Any field in process should be completed.
			if(inField) {
				if(col == COLS && startCol == COLS) {
					//The start-of-field attribute occurred on the last position of the line.
					//The actual field therefore starts at the beginning of the next line.
					startCol = 0;
				} else {
					addItem(state, form, startCol, startAttr);
					inField = false;
				}
			}
		}
		if(log.isDebugEnabled())
			log.debug("End of BuildDom");
	}

	@SuppressWarnings("unused")
	private String zeros = "00000000000000000000000000000000000000000000000000000000000000000000";
	@SuppressWarnings("unused")
	private String spaces = "                                                                   ";
	private void addItem(ScreenState state, Element form, int startCol, int attr) {
		int len = col - startCol;

		//The next field requires extra room so this one must exit early.
		len -= reduceLength;
		reduceLength = 0;

		for(int i=startCol; i < (startCol + len); i++)
			if(screen[row][i] < ' ' && screen[row][i] > 0) screen[row][i] = ' ';
		String value;
		try {
			value = new String(screen[row], startCol, len, encoding);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			value = "ENCODING ERROR ("+encoding+")";
		}


		Element item;
		if((attr & FIELD_ITEM) != 0) {
			String eName = String.format("AutoId%03d", nameSequence++);

			if(state.cuField == null) state.cuField = eName;
			//System.out.println("posRow="+posRow+"("+row+") posCol="+posCol+"("+startCol+") "+eName);

			if(posRow == row && posCol == startCol) {
				state.cuField = eName;
				//System.out.println("Set cuField="+eName);
			}
			item = new Element("item");
			item.setAttribute("id", eName);
			item.setAttribute("xmit", Integer.toString(nameSequence));
			item.addContent(new Element("length").setText(Integer.toString(len)));
			if((attr & STYLE_PASSWD) != 0)
				item.addContent(new Element("password").setText("true"));
			if((attr & ALIGN_RIGHT) != 0) {
				item.addContent(new Element("align").setText("right"));
				item.addContent(new Element("datatype").setText("string"));
				value = value.trim();
				//if(value.length() < len)
				//	value = spaces.substring(0, len - value.length()) + value;
			} else item.addContent(new Element("datatype").setText("string"));

			if((attr & STYLE_PROTECTED) != 0) {
				item.addContent(new Element("protected").setText("true"));
				if(state.cuField.equals(eName)) state.cuField = null;
			}
			state.parameters.put(eName, new ParameterWrapper(new Var(value)));
		} else {
			item = new Element("textitem");
			item.addContent(new Element("text").setText(value));
		}

		if((attr & STYLE_BLINK) != 0)
			item.addContent(new Element("blink").setText("true"));
		if((attr & STYLE_BRIGHT) != 0)
			item.addContent(new Element("bright").setText("true"));
		if((attr & STYLE_REVERSE) != 0)
			item.addContent(new Element("reverse").setText("true"));
		if((attr & STYLE_UNDERLINE) != 0) {
			item.addContent(new Element("underline").setText("true"));
			Element ul = new Element("textitem");
			ul.addContent(new Element("row").setText(Integer.toString(row+1)));
			ul.addContent(new Element("col").setText(Integer.toString(startCol+1)));
			String us = "_________________________________________________________________________________";
			ul.addContent(new Element("text").setText(us.substring(0, value.length() + 1)));
			form.addContent(ul);
		}

		if(row == STATUSROW) 
			item.addContent(new Element("class").setText("c_statusline"));

		item.addContent(new Element("row").setText(Integer.toString(row+1)));
		item.addContent(new Element("col").setText(Integer.toString(startCol+2)));
		form.addContent(item);
	}

	/**
	 * Inserts a space at cursor position. All characters on line are moved right. In
	 * protectedMode, only characters in an editable fields are moved. If performed in
	 * non-editable field, no action is taken.
	 */
	public void insertInLine() {
		for(int i=COLS-1;i>col;i--) screen[row][i] = screen[row][i-1];
		screen[row][col] = ' ';
	}

	/** 
	 * Deletes a character at cursor position. All characters on line are moved left. In
	 * protectedMode, only characters in an editable fields are moved. If performed in
	 * non-editable field, no action is taken.
	 */
	public void deleteInLine() {
		for(int i=col;i<(COLS-1);i++) screen[row][i] = screen[row][i+1];
		screen[row][COLS-1] = ' ';
	}

	/**
	 * Inserts a space at cursor position. All characters on screen are moved right. In
	 * protectedMode, only characters in an editable fields are moved. If performed in
	 * non-editable field, no action is taken.
	 */
	public void insertInPage() {
		for(int i=ROWS-1;i>row;i--) {
			for(int j=COLS-1;j>0;j--) screen[i][j] = screen[i][j-1];
			screen[i][0] = screen[i-1][COLS-1];
		}
		for(int i=COLS-1;i>col;i--) screen[row][i] = screen[row][i-1];
		screen[row][col] = ' ';
	}

	/**
	 * Deletes a character at cursor position. All characters on screen are moved left. In 
	 * protectedMode, only characters in an editable fields are moved. If performed in
	 * non-editable field, no action is taken.
	 */
	public void deleteInPage() {
		for(int i=col;i<(COLS-1);i++) screen[row][i] = screen[row][i+1];
		for(int i=row+1;i<ROWS;i++) {
			screen[i-1][COLS-1] = screen[i][0];
			for(int j=0;j<(COLS-1);j++) screen[i][j] = screen[i][j+1];
		}
		screen[ROWS-1][COLS-1] = ' ';
	}

	private String cPosString(int row, int col) {
		StringBuilder sb = new StringBuilder();
		sb.append((char)ESC + "\""); //cursor position
		sb.append((char)(col + 31)); //column
		sb.append((char)(row+31)); //row
		return sb.toString();
	}

	/**
	 * Format error message to screen type.
	 * @param msg the error message to format
	 * @return a string representing an errorScreen for the Terminal.
	 */
	public String errorScreen(String msg) {
		StringBuilder sb = new StringBuilder();


		sb.append((char)FF); //clear screen
		sb.append(cPosString(1, 1));
		sb.append("Action ");
		sb.append((char)US + "       " + (char)RS + "       SYSTEM ERROR");
		sb.append(cPosString(3, 1));
		sb.append((char)SI + " ");
		sb.append(cPosString(7, 2));
		sb.append((char)SUB + msg);
		return sb.toString();

	}

	@Override
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

}

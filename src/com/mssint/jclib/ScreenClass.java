package com.mssint.jclib;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import java.io.UnsupportedEncodingException;

import java.lang.reflect.*;

import java.net.URLDecoder;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;



/**
 * 
 * <p><b>ScreenClass</b> is intended to be used within the migrated LINC online application 
 * programs to provide an easy to use interface between the web frontend and the
 * screen variables in the program. Most standard variables and the jlinc <b>Var</b> class
 * are supported. 1 dimensional arrays are also supported.</p>
 * 
 *<p> Arrays and variables which require initialisation must be instantiated in the
 * class definition, or before the init() method is called. Unsupported types,
 * multi-dimensional arrays and uninitialised variables and arrays will be
 * silently ignored. It is not necessary to initialise basic data types and
 * wrappers (such as Integer, int, double, etc).</p>
 * 
 * <p>The program should define a class which extends <b>ScreenClass.</b>
 * This class should contain the variables which need to be communicated
 * between the <b>webManager</b> front-end and the application. Some
 * additional standard items are added by default. It is recommended,
 * though not necessary, to create the new class as an "inner" class.
 * This example assumes just that.</p>
 * 
 * <pre>
 * class ApplicationProgram {
 *     public class Screen extends ScreenClass {
 *         public String screenItem1;
 *         public Var v;
 *         public int[] balances = new int[5];
 *     }
 *     Screen screen = new Screen();
 * }
 * </pre>
 * <p>The <b>ScreenClass</b> declaration must be either public or
 * protected. If it's declared as private, the <b>ScreenClass</b> methods
 * will be unable to read any fields.  All of the fields declared within
 * the class are <i>required</i> to be public.  Fields declared with
 * any other modifiers (including no modifier) will simply be ignored
 * and the data represented by those fields will not be communicated
 * to the <b>webManager,</b> nor will the fields be updated. The only indication
 * (other than the fact that the program won't work properly) will be
 * a message in the system log file, prefixed by ScreenClass.</p>
 *
 * <p>The 'screen' object must be initialised before it can be used for
 * communication.  Any screen variables should be instantiated before
 * the call to <b>init().</b></p>
 * <pre>
 * screen.v = new Var(Var.STRING, 10);
 * screen.init();
 * </pre>
 * <p>The {@link ScreenClass#toXml toXml()} and {@link ScreenClass#loadXml loadXml()}
 * methods convert between an XML string and the internal
 * storage of the individual data items. (e.g. screen.balances[2] = 500;)</p>
 * 
 * <p>Individual data items are accessed directly and not through getter/setter methods.</p>
 *
 * <p>The following data types are supported by <b>ScreenClass</b>:
 * <br><b>int, long, float, double, Integer, Long, Float, Double, Var, Group</b>
 * <p>jlinc last rebuilt %DATE; </p>
 * @version %BUILD;
 */
public class ScreenClass extends SystemFields {
	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(ScreenClass.class);

	private static final int T_int		= 1;
	private static final int T_Integer	= 2;
	private static final int T_long		= 3;
	private static final int T_Long		= 4;
	private static final int T_double	= 5;
	private static final int T_Double	= 6;
	private static final int T_float	= 7;
	private static final int T_Float	= 8;
	private static final int T_Var		= 9;
	private static final int T_Group	=10;
	private static final int T_String	=11;
	private static final int T_StringBuilder	=12;
	private static final int T_StringBuffer		=13;
	private static final int T_TypeMask = 037;  //Mask for above types
	private static final int T_System = 0100; //cleared by ClearAll or ClearSystemFields
	private static final int T_Safe = 0200; //Never clear this value
	
	private static final int T_Clear =  1;
	private static final int T_ClearAll =  2;
	private static final int T_ClearSystem =  3;
	private static final int T_ClearCopyFrom =  4;
	
	public Var ISPEC = new Var(Var.CHAR,5);
	public Var TRANNO = new Var(Var.UNUMERIC,6);
	public Var INPUT_DATE = new Var(Var.CHAR,7);
	public Var ACTMTH = new Var(Var.UNUMERIC,4);

	
	public String wrapFieldsForClient() {
		// cut last </sys> field, create StringBuffer for it
		String completeString = super.wrapFieldsForClient(); 
		String partialString = completeString.substring(0, 
				completeString.length() - 6);
		StringBuffer buff = new StringBuffer(partialString);
		// append fields
		buff.append("</sys>");
		buff.append("<item id=\"ISPEC\"><value>" + rcIspec + "</value></item>");
		buff.append("<item id=\"TRANNO\"><value>" + TRANNO + "</value></item>");
		buff.append("<item id=\"INPUT_DATE\"><value>" + INPUT_DATE + "</value></item>");
		buff.append("<item id=\"ACTMTH\"><value>" + ACTMTH + "</value></item>");
		return buff.toString();
	}
	
	//Private inner class for holding information about Screen variables.
	private class Varlist {
		//A structure for storing the data
		protected class Fields {
			int type;
			int arrayLen;
			Field field;
		}
		HashMap<String, Fields> hm; 	//A hashmap of the screen variables
		Object object;	//The actual instance
		@SuppressWarnings("unused")
		Class<?> clazz; 		//The base class

		//The constructor. Create's the hashmap & saves class and object instance.
		Varlist(Class<?> clazz, Object object) {
			this.clazz = clazz;
			this.object = object;
			hm = new HashMap<String, Fields>();
		}
		
		//Adds a new Variables object field to the hashmap
		protected void addField(int attr, String name, int type, Field field, int arrayLen) {
			Fields var = new Fields();
			var.type = type | attr;
			var.field = field;
			var.arrayLen = arrayLen;
			hm.put(name, var);
			//Util.debug("addField("+name+","+type+",*,"+arrayLen+")");
		}

		//Converts all non-ignored variables to an XML string.
		protected String toXml() {
			StringBuilder buf = new StringBuilder();
			Iterator<String> it = hm.keySet().iterator();
			while(it.hasNext()) {
				String name = it.next();
				String s = getXmlString(name);
				buf.append(s);
			}
			return buf.toString();
		}
		
		//Clears (resets) variables in this object, according to the following rules:
		//	what=T_Clear - clear non "system" variables.
		//	what=T_ClearSystem - clear only system variables
		//	what=T_ClearAll - clears all variables
		//	what=T_ClearCopyFrom - clears all variables inside copy from area
		protected void clear(int what) {
			Iterator<String> it = hm.keySet().iterator();
			while(it.hasNext()) {
				String vname = it.next();
				Fields v = hm.get(vname);
				if(what==T_Clear && (v.type & T_System) != 0) continue;
				else if(what==T_ClearSystem && (v.type & T_System) == 0) continue;
				else if(what==T_ClearCopyFrom && (v.arrayLen == 0)) continue;
				if((v.type & T_Safe) != 0) continue;
				if(v.arrayLen > 0) for(int i=0;i<v.arrayLen;i++) {
					setValueString(vname,i,"");
				} else {
					setValueString(vname,0,"");
				}
			}
		}
		
		/*
		 * Returns an XML string returning the value of "vname" in the form
		 * <item id=vname><value>xxx</value></item>. For an array, the value
		 * entries are repeated. If no values are found (all spaces or null
		 * lengths) then an empty string is returned.
		 * @param vname The name of the screen variable to get.
		 * @return An XML string representing the variable and data.
		 */
		protected String getXmlString(String vname) {
			if(vname == null) return "";
			StringBuilder buf = new StringBuilder();
			buf.append("<item id=\""+vname+"\">");
			boolean valueFound = false;
			Fields v = hm.get(vname);
			if(v == null) return "";
			if(v.arrayLen > 0) {
				for(int i=0; i < v.arrayLen; i++) {
					String s = getValueString(v, i);
				    if(s.length() > 0)
						valueFound = true;
					buf.append("<value><![CDATA["+s+"]]></value>");
				}
			} else {
				String s = getValueString(v, 0);
				if(s.length() > 0) 
					valueFound = true;
				buf.append("<value><![CDATA["+s+"]]></value>");
			}
			buf.append("</item>");
			if(valueFound) return buf.toString();
			return "";
		}

		/* Sets a variable to a new value, stored in 'data' as a string. This
		   is converted to the correct type as required.
		   vname - the name of the variable
		   idx - an index if vname is an array. Ignored for non-array items.
		   data - the data value
		*/
		protected void setValueString(String vname, int idx, String data) {
			Integer ival;
			Double dval;
			Float fval;
			Long lval;
			if(vname == null) return;

			Fields v = hm.get(vname);
			if(v == null || idx < 0) return;

			if(data == null) data = "";
			try {
				switch(v.type & T_TypeMask) {
					case T_int:
						try {ival = Integer.parseInt(data);} catch(Exception e) {ival = 0;}
						if(v.arrayLen == 0) v.field.setInt(object, ival);
						else Array.set(v.field.get(object), idx, ival);
						break;
					case T_Integer:
						try {ival = Integer.parseInt(data);} catch(Exception e) {ival = 0;}
						if(v.arrayLen == 0) v.field.set(object, ival);
						else Array.set(v.field.get(object), idx, ival);
						break;
					case T_long:
						try {lval = Long.parseLong(data);} catch(Exception e) {lval = 0L;}
						if(v.arrayLen == 0) v.field.setLong(object, lval);
						else Array.set(v.field.get(object), idx, lval);
						break;
					case T_Long:
						try {lval = Long.parseLong(data);} catch(Exception e) {lval = 0L;}
						if(v.arrayLen == 0) v.field.set(object, lval);
						else Array.set(v.field.get(object), idx, lval);
						break;
					case T_double:
						try {dval = Double.parseDouble(data);} catch(Exception e) {dval = 0.0;}
						if(v.arrayLen == 0) v.field.setDouble(object, dval);
						else Array.set(v.field.get(object), idx, dval);
						break;
					case T_Double:
						try {dval = Double.parseDouble(data);} catch(Exception e) {dval = 0.0;}
						if(v.arrayLen == 0) v.field.set(object, dval);
						else Array.set(v.field.get(object), idx, dval);
						break;
					case T_float:
						try {fval = Float.parseFloat(data);} catch(Exception e) {fval = 0F;}
						if(v.arrayLen == 0) v.field.setFloat(object, fval);
						else Array.set(v.field.get(object), idx, fval);
						break;
					case T_Float:
						try {fval = Float.parseFloat(data);} catch(Exception e) {fval = 0F;}
						if(v.arrayLen == 0) v.field.set(object, fval);
						else Array.set(v.field.get(object), idx, fval);
						break;
					case T_Var:
						Var var;
						if(v.arrayLen == 0) var = (Var)(v.field.get(object));
						else var = (Var)Array.get(v.field.get(object), idx);
						if(var != null) {
							if(data.length() == 0) var.clear();
							else var.set(data);
						}
						break;
					case T_Group:
						Group gvar;
						if(v.arrayLen == 0) gvar = (Group)(v.field.get(object));
						else gvar = (Group)Array.get(v.field.get(object), idx);
						gvar.set(data);
						break;
					case T_String:
						if(v.arrayLen == 0) v.field.set(object, data);
						else Array.set(v.field.get(object), idx, data);
						break;
					case T_StringBuilder:
						StringBuilder sb;
						if(v.arrayLen == 0) sb = (StringBuilder)(v.field.get(object));
						else sb = (StringBuilder)Array.get(v.field.get(object), idx);
						sb.delete(0, sb.length());
						sb.append(data);
						break;
					case T_StringBuffer:
						StringBuffer sb2;
						if(v.arrayLen == 0) sb2 = (StringBuffer)(v.field.get(object));
						else sb2 = (StringBuffer)Array.get(v.field.get(object), idx);
						sb2.delete(0, sb2.length());
						sb2.append(data);
						break;
					default:
						break;
				}
			} catch (IllegalAccessException e) {
				Util.error("ScreenClass: Illegal access Violation - ignored");
			}

		}

		/* Returns a string representation of a variable, indicated by the Variable
		   type 'v'. If an array type, 'idx' is used as an index.
		   This method will never return null.
		*/
		private String getValueString(Fields v, int idx) {
			if(v == null || idx < 0) return "";
			Integer ival;
			Double dval;
			Float fval;
			String str = "";
			Long lval;

			try {
				switch(v.type & T_TypeMask) {
					case T_int:
						if(v.arrayLen == 0) ival = v.field.getInt(object);
						else ival = (Integer)Array.get(v.field.get(object), idx);
						str = (ival == null || ival == 0) ? "" : ival.toString();
						break;
					case T_Integer:
						if(v.arrayLen == 0) ival = (Integer)v.field.get(object);
						else ival = (Integer)Array.get(v.field.get(object), idx);
						str = (ival == null || ival == 0) ? "" : ival.toString();
						break;
					case T_long:
						if(v.arrayLen == 0) lval = v.field.getLong(object);
						else lval = (Long)Array.get(v.field.get(object), idx);
						str = (lval == null || lval == 0) ? "" : lval.toString();
						break;
					case T_Long:
						if(v.arrayLen == 0) lval = (Long)v.field.get(object);
						else lval = (Long)Array.get(v.field.get(object), idx);
						str = (lval == null || lval == 0.0) ? "" : lval.toString();
						break;
					case T_double:
						if(v.arrayLen == 0) dval = v.field.getDouble(object);
						else dval = (Double)Array.get(v.field.get(object), idx);
						str = (dval == null || dval == 0.0) ? "" : dval.toString();
						break;
					case T_Double:
						if(v.arrayLen == 0) dval = (Double)v.field.get(object);
						else dval = (Double)Array.get(v.field.get(object), idx);
						str = (dval == null || dval == 0.0) ? "" : dval.toString();
						break;
					case T_float:
						if(v.arrayLen == 0) fval = v.field.getFloat(object);
						else fval = (Float)Array.get(v.field.get(object), idx);
						str = (fval == null || fval == 0.0) ? "" : fval.toString();
						break;
					case T_Float:
						if(v.arrayLen == 0) fval = (Float)v.field.get(object);
						else fval = (Float)Array.get(v.field.get(object), idx);
						str = (fval == null || fval == 0.0) ? "" : fval.toString();
						break;
					case T_Var:
						Var var;
						if(v.arrayLen == 0) var = (Var)(v.field.get(object));
						else var = (Var)Array.get(v.field.get(object), idx);
						str = var == null ? "" : var.toString();
						break;
					case T_Group:
						Group gvar;
						if(v.arrayLen == 0) gvar = (Group)(v.field.get(object));
						else gvar = (Group)Array.get(v.field.get(object), idx);
						str = gvar == null ? "" : gvar.toString();
						break;
					case T_String:
						if(v.arrayLen == 0) str = (String)(v.field.get(object));
						else str = (String)Array.get(v.field.get(object), idx);
						if(str == null) str = "";
						break;
					case T_StringBuilder:
						StringBuilder sb;
						if(v.arrayLen == 0) sb = (StringBuilder)(v.field.get(object));
						else sb = (StringBuilder)Array.get(v.field.get(object), idx);
						str = sb  == null ? "" : sb.toString();
						break;
					case T_StringBuffer:
						StringBuffer sb2;
						if(v.arrayLen == 0) sb2 = (StringBuffer)(v.field.get(object));
						else sb2 = (StringBuffer)Array.get(v.field.get(object), idx);
						str = sb2  == null ? "" : sb2.toString();
						break;
					default:
						str = "";
						break;
				}
			} catch (IllegalAccessException e) {
				Util.error("ScreenClass: Illegal access Violation - ignored");
			}
			if(str == null) return "";
			return str;
		}
	} //End of inner class 'Varlist'
	//The instance of the Varlist inner-class.
	private Varlist varlist = null;

	//Adds the system fields.
	/*private void addSystemFields() {
		try {
			addNewField(T_System, varlist.object, varlist.clazz.getField("rcType"));
			addNewField(T_System, varlist.object, varlist.clazz.getField("ispec"));
			addNewField(T_System, varlist.object, varlist.clazz.getField("messCount"));
			addNewField(T_System, varlist.object, varlist.clazz.getField("messLine"));
			addNewField(T_System, varlist.object, varlist.clazz.getField("message"));
			addNewField(T_System, varlist.object, varlist.clazz.getField("mssReport"));
			addNewField(T_System, varlist.object, varlist.clazz.getField("cuRec"));
			addNewField(T_System, varlist.object, varlist.clazz.getField("cuField"));
			addNewField(T_System, varlist.object, varlist.clazz.getField("cuBlock"));
			addNewField(T_Safe, varlist.object, varlist.clazz.getField("stationName"));
			addNewField(T_Safe, varlist.object, varlist.clazz.getField("sessionID"));
		} catch (NoSuchFieldException e) {
			Util.error("ScreenClass: Internal Error - No such field");
		}
	}*/


	/**
	 * <p>Inspects the this object, assumed to be subclassed from <b>ScreenClass</b>
	 * in order to get references to fields declared in the class.</p>
	 * Builds a map of fields & objects. Intended for loading screen variables
	 * from applications so that vars->xml and xml->vars transformations can take
	 * place in a standard fashion.</p>
	 * <p><b>init()</b> needs to be called at least once and before any xml 
	 * transformations take place. It should be called <i>after</i> all required
	 * field and array instantiatons have taken place. Any fields or arrays which
	 * have not been instantiated when <b>init()</b> is run will be ignored and a message
	 * to that effect will be written to the system log file. <b>init()</b> may be
	 * called more than once. All existing references will be dropped, but field
	 * data will be unaffected.
	 */
	public void init() {
		Object object = this;
		Class<?> clazz = object.getClass();
		varlist = new Varlist(clazz, object);
		Field field[] = clazz.getDeclaredFields();
		//addSystemFields();
		for(int i=0;i<field.length;i++) {
			addNewField(0, object, field[i]);
		}
	}

	//Adds a new field (duh!) to varlist. Internal use only.
	private void addNewField(int attr, Object object, Field field) {
		int type = -1;
		int arrayLen;

		if(field == null) return;
		//Silently Ignore anything which is not public.
		//Util.debug("PRE: "+ field.getType().getName()+" : "+ field.getName());
		if(!Modifier.isPublic(field.getModifiers())) return;
		type = -1;
		arrayLen = 0;

		Class<?> c = field.getType();
		String dt = c.getName();
		//dt is one of the following forms:
		//long, int, double, etc for base types
		//java.lang.Double, etc for standard wrapper types
		//com.mssint., etc for Var types
		//If an array, a '[' for every dimension. followed by a letter
		//indicating class followed by name of class, unless the class is
		//basic - then the type is indicated by a single letter.
		try {
			if(c.isArray()) {
				if(dt.substring(0,2).equals("[[")) {
					Util.error("ScreenClass: Only 1 dimensional arrays supported." +
						" '"+field.getName()+"' will be ignored.");
					return;
				}
				if(field.get(object) == null) {
					Util.error("ScreenClass: The uninitialiased array '"+
						field.getName()+"' will be ignored.");
					return;
				}
				arrayLen = Array.getLength(field.get(object));
				if(dt.length() == 2) {
					char ch = dt.charAt(1);
					switch(ch) {
						case 'D': dt = "double"; break;
						case 'F': dt = "float"; break;
						case 'I': dt = "int"; break;
						case 'J': dt = "long"; break;
						case 'Z': dt = "boolean"; break;
						default: dt = "Unknown("+dt+");"; break;
					}
				} else {
					dt = dt.substring(2);
					int idx = dt.indexOf(';');
					if(idx != -1) dt = dt.substring(0,idx);
				}
			} //else if(dt.length() > 6) dt = dt.substring(6);
			if(field.get(object) == null) {
				if(!dt.substring(0,10).equals("java.lang.")) {
					Util.error("ScreenClass: The uninitialiased variable '"+
						dt+" "+field.getName()+"' will be ignored.");
					return;
				}
			}
		} catch (IllegalAccessException e) {
			Util.error("ScreenClass: Illegal access Violation - ignored");
		}
		if(dt.equals("com.mssint.jclib.Var")) {
			type = T_Var;
		} else if(dt.equals("java.lang.Integer")) {
			type = T_Integer;
		} else if(dt.equals("java.lang.String")) {
			type = T_String;
		} else if(dt.equals("int")) {
			type = T_int;
		} else if(dt.equals("java.lang.Double")) {
			type = T_Double;
		} else if(dt.equals("java.lang.Long")) {
			type = T_Long;
		} else if(dt.equals("long")) {
			type = T_long;
		} else if(dt.equals("double")) {
			type = T_double;
		} else if(dt.equals("java.lang.Float")) {
			type = T_Float;
		} else if(dt.equals("float")) {
			type = T_float;
		} else if(dt.equals("java.lang.StringBuffer")) {
			type = T_StringBuffer;
		} else if(dt.equals("java.lang.StringBuilder")) {
			type = T_StringBuilder;
		} else {
			Util.error("ScreenClass: The variable '"+dt+" "
				+field.getName()+"' is not supported and will be ignored.");
		}
		if(type > 0) varlist.addField(attr, field.getName(), type, field, arrayLen);
	}

	//Method to process an incoming XML stream (for EJB ONLY) and save the data in the 
	//various screen fields as appropriate. Uses the inner class "valueLoader"
	//to do the actual loading of data.
	public void processInputXmlToEJB(String xmlString) {
	
		XMLReader parser;
		try {
			parser = XMLReaderFactory.createXMLReader();
			parser.setContentHandler(new valueLoader());
			parser.parse(new InputSource(new ByteArrayInputStream(xmlString.getBytes())));
		} catch(SAXException e) {
			return; //We don't really care about errors here.
		} catch(IOException e) {
			return;
		}
	}


	//Inner class called by processInputXmlToEJB()
	//We're only interested stuff inside parameters tags.
	protected class valueLoader extends DefaultHandler {
		boolean parameters = false;
		boolean sys = false;
		boolean item = false;
		boolean value = false;
		String id;
		int index;

		public void startElement(String nsURI, String localName, String qName,
							Attributes attributes) {
			if(parameters && qName.equals("item")) {
				item = true;
				if(attributes.getLength() > 0) {
					if(attributes.getLocalName(0).equals("id")) {
						index = 0;
						id = attributes.getValue(0);
					}
				}
			} else if(parameters && item && qName.equals("value")) value = true;
			else if(parameters && qName.equals("sys")) sys = true;
			else if(qName.equals("parameters")) parameters = true;
		}
		public void endElement(String nsURI, String localName, String qName) {
			if(item && qName.equals("item")) item = false;
			else if(value && qName.equals("value")) {index++;value = false;}
			else if(sys && qName.equals("sys")) sys = false;
			else if(qName.equals("parameters")) parameters = false;
		}
		public void characters(char [] ch, int start, int len) {
			if(!value) return;
			String s = new String(ch, start, len);
			try {
				s = URLDecoder.decode(s,"UTF-8");
			} catch(UnsupportedEncodingException e) {
				// TODO
			}
			if(s.indexOf('&') != -1) {
			    s = s.replace("&amp;", "&");
			    s = s.replace("&nbsp;", " ");
			}
			if(sys) setFieldValueForEJB(id, index, s);
			else setValue(id, index, s);
		}
	} //End of the inner class valueLoader


	/**
	 * Copy the data in all of the elements in 'screen' into this instance. 
	 * Essentially, the <b>set()</b> method is used to copy all of the data
	 * from one <b>ScreenClass</b> object ('screen') to this one.<br>
	 * All items, including System items, are copied. 
	 * If {@link #stationName screen.stationName}
	 * and {@link #sessionID screen.sessionID} contain data then those fields
	 * in this instance will be overwritten with the values from the 'screen'
	 * instance. If, however, these values have not been set in 'screen' then
	 * the values in this object will not be overwritten.
	 * @param screen The source of the <b>ScreenClass</b> object data. 
	 */
	public void set(ScreenClass screen) {
		clearAll();
		loadXml(screen.toXml());
	}
	
	public void setLeavingMessages(ScreenClass screen) {
		// save fields related to messages
		int tempMessCount = messCount;
		String[] tempMessLine = messLine;
		String tempMessage = message;
		// reset
		clearAll();
		loadXml(screen.toXml());
		// set fields related to messages
		messCount = tempMessCount;
		messLine = tempMessLine;
		message = tempMessage;
	}
	
	//Merely an error message to be displayed in teh system log if init() has
	//not been called.
	private String nullVarListError() {
		String s = "ScreenClass: Method ScreenClass.init() has not been called on this instance.";
		Util.error(s);
		return s;
	}
	
	/**
	 * Clears all the fields defined in the subclassed ScreenClass object. Does
	 * not clear system fields.
	 */
	public void clear() {varlist.clear(T_Clear);}
	/**
	 * Clears all the fields defined in the subclassed ScreenClass object,
	 * including system fields, except for {@link #sessionID screen.sessionID})
	 * and {@link #stationName screen.stationName}
	 */
	public void clearAll() {clearSystemFields(); varlist.clear(T_ClearAll);}
	/**
	 * Clears all the screen fields inside the copyblock
	 */
	public void clearCopyBlock() {varlist.clear(T_ClearCopyFrom);}

	/**
	 * Set's the value of a particular screen field by name. Normally this 
	 * method will not be used since fields are accessed directly. If applied 
	 * to an array item, the assumed value of the index will be 0.
	 * @param vname The name of the field to update.
	 * @param value The value used for the update. Applies to numeric items as 
	 * well.
	 */
	public void setValue(String vname, String value) {
	  varlist.setValueString(vname, 0, value);
	}
	/**
	 * Set's the value of a particular screen field by name. Normally this 
	 * method will not be used since fields are accessed directly. Can be used
	 * update array items as well as non-array items.
	 * @param vname The name of the field to update.
	 * @param index A zero relative index into the array.
	 * @param value The value used for the update. Applies to numeric items as 
	 * well.
	 */
	public void setValue(String vname, int index, String value) {
	  varlist.setValueString(vname, index, value);
	}

	/**
	 * Returns an XML representation of a particular field item. If the field
	 * is an array then the entire list will be output. The returned string
	 * may be empty if the item held no assigned value.<br>
	 * The XML string, assuming a field name "SALES" and a value 220 will look
	 * like this: <br>
	 * <pre>
	 * &lt;item id="SALES">&lt;value>200&lt;/value>&lt;/item></pre>
	 * An array would repeat the value's, in index order. For example,
	 * <pre>&lt;item id="SALES_ITEM">&lt;value>Trainers&lt;/value>
	 * &lt;value>Tennis Shoes&lt;/value>&lt;value>Stinger Missile Kits&lt;/value>&lt;/item></pre>
	 * The returned string will have no line breaks.
	 * @param name The name of the field item.
	 * @return An XML string encoding the data item and value.
	 */
	public String getXmlString(String name) {
		if(varlist == null) return nullVarListError();
		return varlist.getXmlString(name);
	}

	/**
	 * Returns an XML representation of all the registered field items, including
	 * system items. Null, empty or zero'd fields are not included in the string.
	 * This is useful in limitting the amount of data traffic between the client
	 * and server programs. <br>
	 * When returned to webManager, the XML will be used to populate the HTML
	 * form with data. Fields which were null or empty (or zero), and are not
	 * included in the string, will be assumed by webManager to be empty 
	 * and will be cleared.
	 * @return Each item is represented as shown for {@link #getXmlString getXmlString()}
	 * but with the additional
	 * <pre>&lt;parameters>&lt;/parameters></pre>
	 * tags enclosing the entire string.
	 */
	public String toXml() {
		if(varlist == null) return nullVarListError();
	    return "<parameters>" + wrapFieldsForClient() + varlist.toXml()
			+ "</parameters>";
	}
	
	/**
	 * Reads the incoming XML string, assumed to be from <b>webManager,</b> and populates
	 * the field in the <b>ScreenClass</b> object with data from the XML string. Items
	 * missing from the incoming string will be cleared in the <b>ScreenClass</b> object.
	 * @param xmlString The incoming string representing the viewers HTML screen.
	 */
	public void loadXml(String xmlString) {
	    if(varlist == null) nullVarListError();
	    clearSystemFields();
		varlist.clear(T_ClearAll);
		processInputXmlToEJB(xmlString);
	}
	/**
	 * Sets ispec and rcType which defines where we go next
	 * @param ispec The name of the ispec to recall
	 * @return GLB.STATUS
	 */
	public String recallIspec(String ispec) {
//		if(ispec.trim().length() == 0)
//			ispec = Strings.rtrim(ispec);
//		else
//			ispec=Strings.rtrim(ispec).replace('-','_');
//		if(ispec==ispec) rcType=2;
//		else rcType=2;
//		
//		if(ispec.equals("GEMF"))
//			rcType=2;
//		if(ispec.equals("GEM"))
//			rcType=1;
		if(ispec == null || ispec.trim().equals(""))
			ispec = rcIspec;
		
		if(ispec.trim().equals(rcIspec.trim()))
			rcType=1;
		else  {
			rcIspec = ispec;
			rcType=2;
		}
		
		return("*****");
	}
	/**
	 * Sets ispec and rcType which defines where we go next
	 * @param ispec The name of the ispec to recall
	 * @return GLB.STATUS
	 */
	public String recallIspec(Var ispec) {
		return(recallIspec(ispec.getString()));
	}
	/**
	 * Sets ispec and rcType which defines where we go next
	 * @param ispec The name of the ispec to recall
	 * @return GLB.STATUS
	 */
	public String recallIspec(Group ispec) {
		return(recallIspec(ispec.getString()));
	}
	/**
	 * Display teach screen for specified ispec
	 * @param  ispec The name of the ispec
	 * @param  teachScreen The name of the teach screen to display
	 * @return GLB.STATUS
	 */
	public String recallTeach(String ispec,String teachScreen) 
				throws SQLException {
		String l_ispec;
		String l_teach;
		int i;

   		l_ispec = ispec; //.substring(1,5).replace('-','_');
   		l_teach = teachScreen; //.substring(1,5).replace('-','_');
		for(i=0;i<MESSLINE_SIZE;i++) messLine[i] = "";
		String Sql = "select text from teach_screens " +
                    	"where ispec = ? and " +
                    	"teach_screen_name = ? " +
                    	"order by line desc";
		Connection conn = DBConnectionFactory.getConnection();
		java.sql.CallableStatement stmnt = conn.prepareCall(Sql);
		stmnt.setString(1,Strings.rtrim(l_ispec));
		stmnt.setString(2,Strings.rtrim(l_teach));
		java.sql.ResultSet rs=stmnt.executeQuery();
		i=0;
		while(rs.next()){
			i++;
			messLine[i]=rs.getString(1);
		}
		stmnt.close();
		rs.close();
		conn.commit();
		conn.close();
   		messCount=i;
		return("*****");
	}
	/**
	 * Display teach screen for specified ispec
	 * @param  ispec The name of the ispec
	 * @param  teachScreen The name of the teach screen to display
	 * @return GLB.STATUS
	 * @throws SQLException
	 */
	public String recallTeach(Var ispec,Var teachScreen)
				throws SQLException {
		return(recallTeach(ispec.getString(),teachScreen.getString()));
	}
	/**
	 * Display teach screen for specified ispec
	 * @param  ispec The name of the ispec
	 * @param  teachScreen The name of the teach screen to display
	 * @return GLB.STATUS
	 * @throws SQLException
	 */
	public String recallTeach(Var ispec,String teachScreen)
				throws SQLException {
		return(recallTeach(ispec.getString(),teachScreen));
	}
	/**
	 * Display teach screen for specified ispec
	 * @param  ispec The name of the ispec
	 * @param  teachScreen The name of the teach screen to display
	 * @return GLB.STATUS
	 * @throws SQLException
	 */
	public String recallTeach(Var ispec,Group teachScreen)
				throws SQLException {
		return(recallTeach(ispec.getString(),teachScreen.getString()));
	}
	/**
	 * Display teach screen for specified ispec
	 * @param  ispec The name of the ispec
	 * @param  teachScreen The name of the teach screen to display
	 * @return GLB.STATUS
	 * @throws SQLException
	 */
	public String recallTeach(String ispec,Var teachScreen)
				throws SQLException {
		return(recallTeach(ispec,teachScreen.getString()));
	}
	/**
	 * Display teach screen for specified ispec
	 * @param  ispec The name of the ispec
	 * @param  teachScreen The name of the teach screen to display
	 * @return GLB.STATUS
	 * @throws SQLException
	 */
	public String recallTeach(String ispec,Group teachScreen)
				throws SQLException {
		return(recallTeach(ispec,teachScreen.getString()));
	}
	/**
	 * Display teach screen for specified ispec
	 * @param  ispec The name of the ispec
	 * @param  teachScreen The name of the teach screen to display
	 * @return GLB.STATUS
	 * @throws SQLException
	 */
	public String recallTeach(Group ispec,Var teachScreen)
				throws SQLException {
		return(recallTeach(ispec.getString(),teachScreen.getString()));
	}
	/**
	 * Display teach screen for specified ispec
	 * @param  ispec The name of the ispec
	 * @param  teachScreen The name of the teach screen to display
	 * @return GLB.STATUS
	 * @throws SQLException
	 */
	public String recallTeach(Group ispec,String teachScreen)
				throws SQLException {
		return(recallTeach(ispec.getString(),teachScreen));
	}
	/**
	 * Display teach screen for specified ispec
	 * @param  ispec The name of the ispec
	 * @param  teachScreen The name of the teach screen to display
	 * @return GLB.STATUS
	 * @throws SQLException
	 */
	public String recallTeach(Group ispec,Group teachScreen)
				throws SQLException {
		return(recallTeach(ispec.getString(),teachScreen.getString()));
	}

	/**
	 * Set up message to display on the screen
	 * @param  pMessage message to display
	 * @return GLB.ERROR
	 */
	public String message(String pMessage) {
		// if there is already maximum number of messages, return
		if(messCount == MESSLINE_SIZE)
			return("*****");
		// prepare the message string
		int pos = pMessage.indexOf('>');
   		if (pos != -1)  {
   			String firstPart = Strings.rpad(pMessage.substring(0, pos - 1), 20);
   			String secondPart = pMessage.substring(pos, pMessage.length());
   			if(secondPart.length() > 80)
   				secondPart = secondPart.substring(0, 80);
			message = firstPart + secondPart;
   		}
   		else
      		message = pMessage;
   		// add it to the array - it will be ignored if one 
   		// message will be available only
   		messLine[messCount++] = message;
   		return("*****");
	}

	/**
	 * Set up message to display on the screen
	 * @param  pMessage message to display
	 * @return GLB.ERROR
	 */
	public String message(Var pMessage) {
		return(message(pMessage.getString()));
	}
	/**
	 * Set up message to display on the screen
	 * @param  pMessage message to display
	 * @return GLB.ERROR
	 */
	public String message(Group pMessage) {
		return(message(pMessage.getString()));
	}
	/**
	 * Set up message to display on the screen
	 * @param  pMessage message to display
	 * @param Glb GLB class
	 */
	public void message(String pMessage, Glb glb) {
		message(pMessage);
   		glb.ERROR.set("*****");
	}

	/**
	 * Set up message to display on the screen
	 * @param  pMessage message to display
	 * @param Glb GLB class
	 */
	public void message(Var pMessage, Glb glb) {
		message(pMessage);
   		glb.ERROR.set("*****");
	}
	/**
	 * Set up message to display on the screen
	 * @param  pMessage message to display
	 * @param Glb GLB class
	 */
	public void message(Group pMessage, Glb glb) {
		message(pMessage);
   		glb.ERROR.set("*****");
	}
	/**
	 * When a recall is done place cursor in this field
	 * @param fld  Name of field
	 * @param rec  Line number if in Copy block
	 * @param block  name of block (BLOCK or COPYBLOCK)
	 */
	public void putCursor(String fld,int rec,String block) {
		cuField=fld;
		cuRec=rec;
		cuBlock=block;
		return;
	}
	/**
	 * When a recall is done place cursor in this field
	 * @param fld  Name of field
	 */
	public void putCursor(String fld) {
		putCursor(fld,1,"BLOCK");
		return;
	}
	/**
	 * When a recall is done place cursor in this field
	 * @param fld  Name of field
	 * @param rec  Line number if in Copy block
	 * @param block  name of block (BLOCK or COPYBLOCK)
	 */
	public void putCursor(Var fld,int rec,String block) {
		putCursor(fld.getString(),rec,block);
		return;
	}
	/**
	 * When a recall is done place cursor in this field
	 * @param fld  Name of field
	 */
	public void putCursor(Var fld) {
		putCursor(fld.getString(),1,"BLOCK");
		return;
	}

		
}

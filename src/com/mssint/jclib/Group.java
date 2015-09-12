package com.mssint.jclib;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.javafx.collections.MappingChange.Map;

/**
 * <p>
 * Title: Group
 * </p>
 * <p>
 * Description: Java LINC libraries
 * </p>
 * This class implements Groups to facilitate LINC Groups. Data of Group types
 * are stored in a Vector list. Once the Group is defined "members" are added to
 * the group using the addMember method (each of these are also a Group). Each
 * Group has an attribute describing whether it's a Group or Group Member. The
 * data items are stored within Group Members as Var types.
 * <p>
 * last rebuilt %DATE;
 * </p>
 * 
 * @author Peter Colman
 * @version %BUILD;
 */

public class Group extends Var implements java.io.Serializable {
	public enum GroupMode {
		UNISYS,
		MICROFOCUS,
		LINC
	}
	private static final Logger log = LoggerFactory.getLogger(Group.class);
	private static final long serialVersionUID = -5014525601214874325L;
	
	private static GroupMode groupMode;

	protected Group parent;
	protected ArrayList<Group> members;
	protected int groupOffset;
	
	protected int occurs;  //Array size for this element
	protected byte indexLevel;  //Count of expected number of indexes for this item.
	protected int byteLength; //The length of this group's byte storage
	protected int redefinedLen;
	protected int myCachedOffset = -1;
//	private SortedSet<Group>cachedGroupRegister = new TreeSet<Group>(new GroupComparator());

	
//	private boolean redefined;

	static {
		String s = Config.getProperty("jclib.group.mode");
		if(s != null) {
			s = s.trim();
			if(s.equalsIgnoreCase("UNISYS")) {
				groupMode = GroupMode.UNISYS;
			} else if(s.equalsIgnoreCase("MICROFOCUS")) {
				groupMode = GroupMode.MICROFOCUS;
			} else if(s.equalsIgnoreCase("LINC")) {
				groupMode = GroupMode.LINC;
			} else {
				groupMode = GroupMode.LINC; //default
			}
		} else {
			groupMode = GroupMode.LINC;
		}
	}
	
	
	public static void setGroupMode(GroupMode mode) {
		if(mode == null) {
			groupMode = GroupMode.LINC;
		} else {
			groupMode = mode;
		}
	}
	
	public static GroupMode getGroupMode() {
		return groupMode;
	}
	
	private static final short [] unsignedByteLengths = {
		0,1,1,2,2,3,3,3,4,4,5,5,5,6,6,7,7,8,8,8,9,9,
		10,10,10,11,11,12,12,13,13,13,14,14,15,15,15,16,16
	};
	private static final short [] signedByteLengths = {
		0,1,1,2,2,3,3,4,4,4,5,5,6,6,6,7,7,8,8,9,9,9,
		10,10,11,11,11,12,12,13,13,13,14,14,15,15,16,16,16
	};
	
    public Group() {
		attr = CHAR | GROUP;
		displayLength = 0;
		scale = 0;
		parent = null;
		groupOffset = 0;
		indexLevel = 0;
		members = null;
		groupConstructorInitialise();
		cacheMyGroupData();
    }
/*
    public Group(GroupByteArray gba) {
		attr = CHAR | GROUP;
		displayLength = 0;
		scale = 0;
		parent = null;
		groupOffset = 0;
		indexLevel = 0;
		members = null;
		groupConstructorInitialise(gba);
		cacheMyGroupData();
    }
  */  
    public Group(int len) {
    	if(len != -1) {
    		attr = CHAR | GROUP;
    		this.displayLength =  len;
    		scale = 0;
    		parent = null;
    		this.displayLength =  len;
    		groupOffset = 0;
    		indexLevel = 0;
    		members = null;
    		groupConstructorInitialise();
    		cacheMyGroupData();
    	}
    }
/*
    public Group(GroupByteArray gba, int len) {
    	if(len != -1) {
    		attr = CHAR | GROUP;
    		this.displayLength =  len;
    		scale = 0;
    		parent = null;
    		this.displayLength =  len;
    		groupOffset = 0;
    		indexLevel = 0;
    		members = null;
    		groupConstructorInitialise(gba);
    		cacheMyGroupData();
    	}
    }
  */  
    public Group(int attr, int len) {
    	parent = null;
		this.displayLength =  len;
		groupOffset = 0;
		indexLevel = 0;
		this.attr = GROUP | attr;
		if((attr & (DOUBLE|LONG)) != 0) attr |= NUMERIC;
		else if((attr & (NUMERIC)) != 0) 
			this.attr |= LONG;
		this.scale = 0;
		members = null;
		groupConstructorInitialise();
		cacheMyGroupData();
    }
    
    public Group(int attr, int len, int dec) {
    	parent = null;
		this.displayLength =  len;
		groupOffset = 0;
		indexLevel = 0;
		this.attr = GROUP | attr;
		if((attr & (DOUBLE|LONG)) != 0) this.attr |= NUMERIC;
		else if((attr & (NUMERIC)) != 0) {
			if(dec > 0) this.attr |= DOUBLE;
			else this.attr |= LONG;
		}
		this.scale = (short)dec;
		members = null;
		groupConstructorInitialise();
		cacheMyGroupData();
    }


	/**
	 * Returns true if this is the primary group. If this is a 
	 * member of another group, return false
	 * @return
	 */
	public boolean isPrimary() {
		if(parent == null)
			return true;
		return false;
	}
	
	private byte initialPadChar() {
		switch(groupMode) {
		case MICROFOCUS:
			return (byte)' ';
		case UNISYS:
			return (byte)0;
		case LINC: //fall through to default
		default:
			if((attr & COMP) != 0)
				return (byte)0;
			return (byte)((attr & NUMERIC) != 0 ? '0' : ' ');
		}
	}

    private void groupConstructorInitialise() {
    	if(value != null)
    		return;
    	if(charset == null)
    		charset = "iso-8859-1";
    	GroupData data;
    	
		if(displayLength > 0) {
			if((attr & (COMP)) != 0) {
				if((attr & (UNSIGNED)) != 0)
					byteLength = unsignedByteLengths[displayLength];
				else
					byteLength = signedByteLengths[displayLength];
			} else
				byteLength = displayLength;
			
			data = new GroupData(this, byteLength, initialPadChar());
		} else data = new GroupData(this);
		value = data;
    }
    
    /**
     * Used when adding new group members to a group
     * @param g
     * @param attr
     * @param len
     * @param dec
     */
    private Group(Group g, int attr, int len, int dec)  {
		//var = null;
		parent = g;
		indexLevel = (byte) parent.getIndexLevel();
		this.attr = attr;
		if((attr & (DOUBLE|LONG)) != 0) this.attr |= NUMERIC;
		else if((attr & (NUMERIC)) != 0) {
			if(dec > 0) this.attr |= DOUBLE;
			else this.attr |= LONG;
		}
		if(g.occurs > 0)
			groupOffset = g.byteLength / g.occurs;
		else 
			groupOffset = g.byteLength;
		this.displayLength =  len;
		this.scale = (short)dec;
		cacheMyGroupData();

		if(len > 0) {
			if((attr & (COMP)) != 0) {
				if((attr & (UNSIGNED)) != 0)
					byteLength = unsignedByteLengths[len];
				else
					byteLength = signedByteLengths[len];
			} else {
				byteLength =  len;
			}
			
			byte [] newBytes = new byte[byteLength];
			Arrays.fill(newBytes, initialPadChar());
	    	g.increaseLength(groupOffset,0,byteLength, newBytes);
		}
    }
    
    /**
     * Construct a new group which is a redefine of another. Generally, the parent
     * of the group we're redefining becomes our group. If, however, g is the primary
     * group (and therefore the holder of the data) then g does not have a parent yet
     * we must still be able to access the same data area.
     *
     * @param g is the group we're redefining.
     */
    private Group(Group g) {
    	
		displayLength = 0;
		byteLength = 0;
		attr = GROUP|CHAR;
		members = null;
		scale = 0;
    	
		redefinedLen = g.byteLength;

		if(g.parent == null) {
			value = g.value;
			groupOffset = g.myCachedOffset;
			myCachedOffset = g.myCachedOffset;
			parent = null;
		} else {
			indexLevel = g.indexLevel;
			if(indexLevel > 0) {
				parent = g.parent;
				groupOffset = g.groupOffset;
				myCachedOffset = g.myCachedOffset;
			} else {
				parent = null;
				groupOffset = g.myCachedOffset;
				myCachedOffset = g.myCachedOffset;
			}
			value = g.value;
			if(g.occurs > 0)
				redefinedLen /= g.occurs;
		}
//		cacheMyGroupData();
    }
    
    
	@Override
	public Group format(String fmt) {
		if((attr & (COMP)) != 0)
			throw new IllegalArgumentException("COMP item cannot have a format.");
		
		picture = new Picture(fmt);
		scale = (short) picture.decimals();
		attr |= PICTURE;
		if(byteLength < picture.length()) {
			Group g;
			if(parent != null) g = parent;
			else g = this;
			if((attr & (NUMERIC)) != 0) {
				g.increaseLength(0,0,picture.length() - byteLength, adjustByteLengthLeft(null, byteLength, initialPadChar()));
			} else {
				g.increaseLength(0,0,picture.length() - byteLength, adjustByteLengthRight(null, byteLength));
			}
		    byteLength =  picture.length();
		    displayLength = byteLength;
		}
		return this;
	}
	
	/* 
	 * Initialise the group including any with initial values!
	*/
    public void initialize() {
    	initialise();
    }
    /* 
	 * Initialise the group including any with initial values!
	*/
    public void initialise() {
    	if(members != null && members.size() > 0) { //This has members
    		for(Group g : members)
    			g.initialise();
    	} else {
    		int [] bounds;
    		//super.initialise();
    		switch(getIndexLevel()) {
    		case 0:
    			if((attr & (LONG|DOUBLE)) != 0) set(0);
    			else set("");
    			break;
       		case 1:
       			bounds = getIndexBounds();
    			for(int i = 1; i <= bounds[0] ;i++) {
    				if((attr & (LONG|DOUBLE)) != 0) index(i).set(0);
    				else index(i).set("");
    			}
    			break;
       		case 2:
       			bounds = getIndexBounds();
    			for(int i = 1; i <= bounds[0] ;i++) {
    				for(int j=1; j<=bounds[1]; j++) {
    					if((attr & (LONG|DOUBLE)) != 0) index(i,j).set(0);
    					else index(i,j).set("");
    				}
    			}
    			break;
       		case 3:
       			bounds = getIndexBounds();
    			for(int i = 1; i <= bounds[0] ;i++) {
    				for(int j=1; j<=bounds[1]; j++) {
        				for(int k=1; k<=bounds[2]; k++) {
        					if((attr & (LONG|DOUBLE)) != 0) index(i,j,k).set(0);
        					else index(i,j,k).set("");
        				}
    				}
    			}
    			break;
    		default:
    			throw new IllegalArgumentException("Initialise on indexes with "+
    					getIndexLevel()+" indexes has not been coded.");
    		}
    	}
    		
    }

    /* 
	 * Initialise the group except any with initial values!
	*/
    public void initialiseGroupSd() {
    	if(members != null && members.size() > 0) { //This has members
    		for(Group g : members)
    			g.initialiseGroupSd();
    	} else {
    		if((attr & (NOINIT)) == 0)
    			super.initialise();
    	}
    }
    
    int getTotalOccurs(int v) {
    	if(v < 1) v = 1;
    	if(occurs > 0) v *= occurs;
    	if(indexLevel == 0 || parent == null) return v;
		return parent.getTotalOccurs(v);
    }
    
    //used for redefine from a Var
    protected Group(Var v) {
    	//parent = null;
    	//var = v;
    	parent = (Group)v;
    	//attr = GROUP|CHAR|VARPARENT;
    	attr = GROUP|CHAR;
    	displayLength = 0;
    	byteLength = 0;
    	groupOffset = 0;
    	indexLevel = 0;
    	scale = 0;
		cacheMyGroupData();
    }
    
    //Used for sort:
    public Group descending() { attr |= ORDER_DESC; return this;}
    public Group ascending() { attr &= ~(ORDER_DESC); return this;}
    public boolean isDescending() { return ((attr & ORDER_DESC) == ORDER_DESC); }
    public boolean isAscending() { return ((attr & ORDER_DESC) != ORDER_DESC); }
    
    //This constructor creates a temporary group based on the current group.
    //It instantiates the offset to the correct offset for this item.
    private Group(Group g, GroupData storage, int offt) {
    	parent = null;
    	groupOffset = offt;
    	myCachedOffset = offt;
    	occurs = 0;
    	if(g.occurs > 0) {
    		byteLength = g.byteLength / g.occurs;
    		displayLength = g.displayLength / g.occurs;
    	} else {
    		byteLength = g.byteLength;
    		displayLength = g.displayLength;
    	}
    	scale = g.scale;
    	attr = g.attr;
    	attr |= ISASSIGNED;
    	indexLevel = 0;
    	picture = g.picture;
    	value = storage;
    }

    /**
     * 
     * @param relOffset
     * @param oldLen
     * @param newLen
     * @param b
     */
    private void increaseLength(int relOffset, int oldLen, int newLen, byte [] b) {
    	int newByteLength;
		if(occurs > 0) {
	    	if(b.length != newLen)
	    		throw new IllegalAccessError("Segment length must match newLen");

			int oldSegmentLength = byteLength / occurs;
			int newSegmentLength = oldSegmentLength + newLen - oldLen;
			newByteLength = newSegmentLength * occurs;
			
			//This is a redefined group.
			if(redefinedLen > 0) {
				byteLength = newByteLength;
				displayLength = byteLength;
				//Check if the newly increased length fits within the redefined length
				if(newByteLength <= redefinedLen) {
					return;
				}
				//Add length to the supporting byte array
				int dataLen = ((GroupData)value).length();
				int extraLen = (myCachedOffset + newByteLength) - dataLen;
				if(extraLen > 0) {
					byte [] newBytes = new byte[extraLen];
					Arrays.fill(newBytes, initialPadChar());
					((GroupData)value).insert(newBytes, dataLen);
				}
				((GroupData)value).checkPrimaryGroupLength();
				return;
			}
			
			//Create a new full segment to replace the old one.
			byte [] newSegment = new byte[newByteLength];
			//Get my old segment, if it exists
			if(oldSegmentLength > 0) {
				byte [] oldData = ((GroupData)value).getBytes();
				//Populate the new data segment
				for(int i=0; i<occurs; i++) {
					int spos = myCachedOffset + (oldSegmentLength * i);
					int dpos = newSegmentLength * i;
					System.arraycopy(oldData, spos, newSegment, dpos, oldSegmentLength);
					System.arraycopy(b, 0, newSegment, dpos + oldSegmentLength, newSegmentLength - oldSegmentLength);
				}
			} else {
				for(int i=0; i<occurs; i++) {
					int dpos = newSegmentLength * i;
					System.arraycopy(b, 0, newSegment, dpos, newSegmentLength);
				}
			}
			if(parent == null) {
				insertMainSegment(groupOffset, byteLength, newByteLength, newSegment);
			} else {
				parent.increaseLength(groupOffset, byteLength, newByteLength, newSegment);
			}
			byteLength = newByteLength;
			displayLength = byteLength;

		} else if(redefinedLen > 0) {
			if((byteLength + newLen - oldLen) <= redefinedLen) {
				byteLength += newLen - oldLen;
				displayLength += newLen - oldLen;
				return;
			} 
			
			//Add length to the supporting byte array
			int dataLen = ((GroupData)value).length();
			int extraLen = (myCachedOffset + byteLength + newLen - oldLen) - dataLen;
			if(extraLen > 0) {
				byte [] newBytes = new byte[extraLen];
			
				Arrays.fill(newBytes, initialPadChar());
				((GroupData)value).insert(newBytes, dataLen);
				((GroupData)value).checkPrimaryGroupLength();
			}
			byteLength += newLen - oldLen;
			displayLength += newLen - oldLen;
		} else {
			if(parent != null) {
				parent.increaseLength(groupOffset + relOffset, oldLen, newLen, b);
				byteLength += newLen - oldLen;
				displayLength += newLen - oldLen;
			} else {
				insertMainSegment(groupOffset + relOffset, oldLen, newLen, b);
			}
		}
	}

	//This method is only used during creation. 
    private void insertMainSegment(int offt, int oldLen, int newLen, byte [] b) {
    	if(value == null)
    		throw new IllegalAccessError("Cannot call increaseMainLength on non-primary group");

		GroupData data = (GroupData)value;
		
		if(this.byteLength < offt) {
			this.byteLength =  offt;
			this.displayLength =  offt;
		}

		//Ensure text of segment to insert is exactly the right length. Because
		//this comes from Group it's unlikely to be different so don't worry about
		//efficiency.
		if(b == null) {
			b = new byte[newLen];
			Arrays.fill(b, (byte)' ');
		} else if(b.length < newLen) {
			byte [] x = new byte [newLen];
			System.arraycopy(b, 0, x, 0, b.length);
			Arrays.fill(x, b.length, x.length, (byte)' ');
			b = x;
		} else if(b.length > newLen) {
			b = Arrays.copyOf(b, newLen);
		}
		
		//Insert segment b into array ba at required location offt
		if(oldLen == 0) {
			data.insert(b, offt);
		} else {
			data.replace(b, offt, oldLen);
		}
		
		this.byteLength += (newLen - oldLen);
		this.displayLength += (newLen - oldLen);
	}
    
	//The following 2 methods manage the byte array object for Group's
	private void setRealSubstring(byte [] b, int offset, int len) {
//		if((offset + len) > this.byteLength) return; //throw new Exception("String out of bounds");
		
		if((attr & (NUMERIC)) != 0) {
			byte [] x = b;
			b = adjustByteLengthLeft(b, len, initialPadChar());
			if(b.length < x.length) {
				attr |= OVERFLOW;
			}
		} else {
			b = adjustByteLengthRight(b, len);
		}
		GroupData data = (GroupData)value;
		data.setSubString(b, offset);
	}

	public Group addMember(int attribute, int len, int scale)  {
		//Group g = new Group(this, attribute|GROUP|GROUP_MEMBER, len, scale);
		Group g = new Group(this, attribute|GROUP, len, scale);
//		g.redefined = redefined;
		if((g.attr & COMP) != 0) {
			if(scale == 0) {
				if(byteLength < 5) {
					g = new GroupIntegerComp(g);
				} else {
					g = new GroupLongComp(g);
				}
			} 
		}

		if(members == null) members = new ArrayList<Group>();
		members.add(g);
		return g;
	}
	public Group addMember(int attribute, int len) {
		//Group g = new Group(this, attribute|GROUP|GROUP_MEMBER, len, 0);
		Group g = new Group(this, attribute|GROUP, len, 0);
//		g.redefined = redefined;
		if((g.attr & COMP) != 0) {
			if(g.byteLength < 5) {
				g = new GroupIntegerComp(g);
			} else {
				g = new GroupLongComp(g);
			}
		}

		if(members == null) members = new ArrayList<Group>();
		members.add(g);
		return g;
	}
	public Group addGroup()  {
		//Group g = new Group(this, GROUP|CHAR|GROUP_MAIN, 0, 0);
		Group g = new Group(this, GROUP|CHAR, 0, 0);
//		g.redefined = redefined;
		if(members == null) members = new ArrayList<Group>();
		members.add(g);
		return g;
	}
	public Group addGroup(int attribute, int len)  {
		//Group g = new Group(this, attribute|GROUP|GROUP_MEMBER, len, 0);
		Group g = new Group(this, attribute|GROUP, len, 0);
//		g.redefined = redefined;

		if(members == null) members = new ArrayList<Group>();
		members.add(g);
		return g;
	}

	public Group addGroup(Group group)  {
		if(members == null) members = new ArrayList<Group>();

		group.parent = this;
//		group.redefined = redefined;

		members.add(group);
		group.indexLevel = (byte) getIndexLevel();

		group.groupOffset = byteLength;
		
		if(group.byteLength > 0) {
			byte [] newBytes = adjustByteLengthRight(null, group.byteLength);
	    	increaseLength(0, 0, byteLength, newBytes);
		}
		group.cacheMyGroupData();
		return group;
	}
    
    public Group redefine() {
    	Group g = new Group(this);
//		g.redefined = true;
//    	g.cachedGroupRegister = cachedGroupRegister;
//    	((GroupData)value).registerAdditionalGroups(g);
		return g;
    }

    /**
     * Create a new group which redefines this one.
     * @param len The length of the redefined group.
     * @return
     */
    public Group redefine(int len) {
    	Group g = redefine();
    	
    	if(len > 0) {
    		g.byteLength =  len;
    		g.displayLength =  len;
    		g.ensureSpace(len);
    	}
    	return g;
    }
    
    /**
     * Return true if this is a redefine 
     * @return
     */
    public boolean isRedefined() {
    	if(redefinedLen > 0)
    		return true;
    	if(parent == null)
    		return false;
    	return parent.isRedefined();
    }
    
    public boolean isExported() {
    	if((attr & Var.EXPORT) != 0)
    		return true;
    	return false;
    }
    
    public Group redefine(int attr, int len) {
    	return redefine(attr, len, 0);
    }
    
    public Group redefine(int attr, int len, int dec) {
    	Group g = redefine();
    	
    	if(len > 0) {
    		g.displayLength =  len;
    		if((attr & COMP) != 0) {
    			if((attr & UNSIGNED) != 0) {
    				g.byteLength = unsignedByteLengths[len];
    			} else {
    				g.byteLength = signedByteLengths[len];
    			}
    		} else {
    			g.byteLength =  len;
    		}
    		g.ensureSpace(g.byteLength);
    	}
    	if(attr != 0) {
    		g.attr &= ~(Var.CHAR);
    		g.attr |= attr;
    		if((attr & (DOUBLE|LONG)) != 0) attr |= NUMERIC;
    	}
    	if(dec != 0) {
    		g.scale = (short)dec;
    		if((g.attr & CHAR) != 0)
    			throw new IllegalArgumentException("Cannot apply scale to CHAR type");
    		g.attr |= DOUBLE|NUMERIC;
    		g.attr &= ~(LONG);
    	}
    	return g;
    }
    
    /**
     * Ensure sufficient space for this group. 
     * Only used during group re-define
     * @param len
     */
    private void ensureSpace(int len) {
    	if(value != null) {
    		GroupData data = (GroupData)value;
    		if(data.length() < len)
    			data.extendTo(len, initialPadChar());
    	} else {
    		parent.ensureSpace(len);
    	}
    }
    
	public Group occurs(int size)  throws IllegalStateException {
		if(occurs > 0) 
			throw new IllegalStateException("Cannot call occurs more than once on a Group");
		if(value == null) {
			throw new IllegalStateException("Group.value is null");
		}
//		indexLevel++;
		occurs =  size;
		
		if(occurs > 0 && byteLength > 0 && redefinedLen == 0) {
			if(parent != null) {
				byte [] part = parent.getGroupSubstring(groupOffset, byteLength);
				//We copy "occurs - 1" because the first segment already exists.
				byte [] segment = new byte[byteLength * (occurs - 1)];
				for(int i=1; i<occurs; i++)
					System.arraycopy(part, 0, segment, byteLength * (i-1), byteLength);
//				parent.insertSegment(groupOffset + byteLength, segment.length, segment);
				parent.increaseLength(groupOffset + byteLength, 0, segment.length, segment);
				byteLength *= occurs;
				displayLength *= occurs;
			} else {
				//The occurs is being applied to the base group.
				byte [] part = ((GroupData)value).getBytes();
				byte [] newArray = new byte[byteLength * occurs];
				for(int i=0; i<occurs; i++) {
					System.arraycopy(part, 0, newArray, i * byteLength, byteLength);
				}
				((GroupData)value).set(newArray);
				byteLength *= occurs;
				displayLength *= occurs;
			}
		} else if(occurs > 0) {
			byteLength *= occurs;
			displayLength *= occurs;
		}
		return this;
	}
	
	public int getIndexLevel() { return occurs == 0 ? indexLevel : indexLevel + 1; }
	
	public int [] getIndexBounds() {
		int [] bounds;
		if(indexLevel > 0)
			bounds = parent.getIndexBounds();
		else if(occurs > 0) {
			bounds = new int[1];
			bounds[0] = occurs;
			return bounds;
		} else {
			return new int[0];
		}

		if(occurs > 0) {
			int [] nbounds = new int[bounds.length + 1];
			int i;
			for(i=0; i<bounds.length; i++) 
				nbounds[i] = bounds[i];
			nbounds[i] = occurs;
			bounds = nbounds;
		}
		return bounds;
	}
	
	
	//This is needed to overide the Var.index(int) call.
	@Override
	public Group index(int idx)  
		throws IndexOutOfBoundsException, IllegalArgumentException {

		int expectedIndexCount = occurs == 0 ? indexLevel : indexLevel + 1;
		
		if(expectedIndexCount != 1) {
			log.error("Passed "+1+" indexes. Expected "+expectedIndexCount+".");
			return null;
		}
		//Optimise for single index - no recursion...
		if(expectedIndexCount == 1) {
			if(occurs != 0) {
				//Indexed at this level
				if(idx > occurs || idx < 1) {
					throw new IndexOutOfBoundsException("Index of "+idx+" is out of range (1 to "+occurs+")");
				}
				int offt = myCachedOffset + (idx - 1) * (byteLength / occurs);
				return new Group(this, ((GroupData)value), offt);
			} else if(parent.occurs > 0) {
				if(idx > parent.occurs || idx < 1) {
					throw new IndexOutOfBoundsException("Index of "+idx+" is out of range (1 to "+parent.occurs+")");
				}
				int offt = parent.myCachedOffset + ((idx - 1) * (parent.byteLength / parent.occurs)) + groupOffset;
				return new Group(this, ((GroupData)value), offt);
			}
		}
		
		return index(new int [] { idx });
	}
	
	@Override
	public Group index(int idx1, int idx2) 
			throws IndexOutOfBoundsException, IllegalArgumentException {
		return index(new int [] { idx1, idx2 });
	}
	public Group index(Var idx) {
		return index(idx.getInt());
	}
	
	//Create's a temporary group which points to the right part of the primary
	//var for this indexed item.
	public Group index(int ... indeces)
		throws IndexOutOfBoundsException, IllegalArgumentException {
		
		int expectedIndexCount = occurs == 0 ? indexLevel : indexLevel + 1;
		
		if(expectedIndexCount != indeces.length) {
			log.error("Passed "+indeces.length+" indexes. Expected "+expectedIndexCount+".");
			return null;
		}
		//Optimise for single index - no recursion...
		if(expectedIndexCount == 1) {
			if(occurs != 0) {
				//Indexed at this level
				int offt = myCachedOffset + ((indeces[0] - 1) * (byteLength / occurs));
				return new Group(this, ((GroupData)value), offt);
			} else if(parent.occurs > 0) {
				int offt = parent.myCachedOffset + ((indeces[0] - 1) * (parent.byteLength / parent.occurs)) + groupOffset;
				return new Group(this, ((GroupData)value), offt);
			}
		}

		int off = getRealOffset(0, indeces.length, indeces);
		return new Group(this, ((GroupData)value), off);
	}
	
	public Group index(Var ... obj) {
		
		int expectedIndexCount = occurs == 0 ? indexLevel : indexLevel + 1;
		
		if(expectedIndexCount != obj.length) {
			log.error("Passed "+obj.length+" indexes. Expected "+expectedIndexCount+".");
			return null;
		}
		//Optimise for single index - no recursion...
		if(expectedIndexCount == 1) {
			int idx;
			if(obj[0] instanceof Group) {
				idx = ((Group)obj[0]).getInt();
			} else if(obj[0] instanceof Var) {
				idx = ((Var)obj[0]).getInt();
			} else {
				idx = 0;
			}
			if(occurs != 0) {
				//Indexed at this level
				int offt = myCachedOffset + ((idx - 1) * (byteLength / occurs));
				return new Group(this, ((GroupData)value), offt);
			} else if(parent.occurs > 0) {
				int offt = parent.myCachedOffset + ((idx - 1) * (parent.byteLength / parent.occurs)) + groupOffset;
				return new Group(this, ((GroupData)value), offt);
			}
		}
		
		int [] arr = new int [obj.length];
		for(int i=0; i<arr.length;i++) {
			if(obj[i] instanceof Group) {
				Group g = (Group)obj[i];
				arr[i] = g.getInt();
			} else if(obj[i] instanceof Var) {
				Var v = (Var)obj[i];
				arr[i] = v.getInt();
			}
			else {
				log.error("Only Var, Group and int handled for index");
				return null;
			}
		}
		return index(arr);
	}
	
	public int getRealOffset(int ... indeces) throws IndexOutOfBoundsException {
		return getRealOffset(0, indeces.length, indeces);
	}
	
	// The array of indexes is 1 relative
	private int getRealOffset(int offset, int count, int [] indeces)
		throws IndexOutOfBoundsException {
		int offt;
		if(occurs > 0) {
			if(count > 0) {
				count--;
				if(indeces[count] < 1 || indeces[count] > occurs)
					throw new IndexOutOfBoundsException("Index out of range: "+(indeces[count]));
				offt = this.groupOffset + ((byteLength / occurs) * (indeces[count]-1));
			} else offt = this.groupOffset;
		} else offt = this.groupOffset;
		
		if(parent == null) return offt + offset;
		return parent.getRealOffset(offset + offt, count, indeces);
	}

	
	/** The following group of functions are used internally to get and set 
	 * the group portions of the primary group's var.
	 * Note that the portion of the group being replaced will be space 
	 * padded if the String s is too short. If the string needs to be right-
	 * justified, ensure that it is correct before calling these setters.
	 * These functions are recursive, and all of them end up setting or getting
	 * a segment in the Var which is instantiated by the primary group.
	 */
	private void setGroupSubstring(byte [] b) {
		if(byteLength == 0) {
			if(b.length > 0) throw new IllegalStateException("Group item has no members");
			return;
		}
//		if(var != null) var.setRealSubstring(s, offset, len);
//		else parent.setGroupSubstring(s, offset, len);
		if(parent == null) setRealSubstring(b, groupOffset, byteLength);
		else parent.setGroupSubstring(b, groupOffset, byteLength);
	}
	
	private void setGroupSubstring(byte [] b, int offset, int len) {
		if(len == 0) {
			if(b.length > 0) throw new IllegalStateException("Group item has no members");
			return;
		}
//		if(var != null) var.setRealSubstring(s, offset, len);
//		else parent.setGroupSubstring(s, this.offset + offset, len);
		if(parent == null) setRealSubstring(b, this.groupOffset + offset, len);
		else parent.setGroupSubstring(b, this.groupOffset + offset, len);
	}
	
	private byte [] getGroupSubstring() {
		if(byteLength == 0) return new byte[0];
	    if(parent == null) {
			GroupData data = (GroupData)value;
			return data.subString(groupOffset, groupOffset + byteLength);
	    
	    }
	    byte [] b = parent.getGroupSubstring(groupOffset, byteLength);
	    return b;
	}
	
	private String getGroupSubstringAsString() {
		try {
			byte [] b = getGroupSubstring();
			String s = new String(b, charset);
			return s;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return "";
		}
	}
	
	
	private byte [] getGroupSubstring(int offset, int len) {
		if(len == 0) return new byte[0];
		if(parent == null) {
//			if(offset > this.byteLength) return new byte[0];
//			else if((offset + len) > this.byteLength) len = this.byteLength - offset;
			GroupData data = (GroupData)value;
			byte [] b = data.subString(offset, offset + len);
			return b;
		}
		return parent.getGroupSubstring(this.groupOffset + offset, len);
	}
	/*******************************************************************/
	
	@Override
	public String getString() throws IllegalArgumentException{
		if(indexLevel > 0 || occurs > 0) throw new IllegalArgumentException("Group requires an index.");
		String s;
//		if(parent != null) s = parent.getString().substring(offset, offset + len);
//		else s = var.getRawString().substring(offset, offset + len);
		
		int offs = myCachedOffset;
		
		if((attr & (COMP)) != 0) {
			BigDecimal bd;
			if(byteLength < 5) {
				int val = ((GroupData)value).getCompAsInt((attr & UNSIGNED) != 0, offs, byteLength);
				bd = new BigDecimal(val/Math.pow(10,scale)).setScale(scale, BigDecimal.ROUND_HALF_DOWN);
			} else {
				long val = ((GroupData)value).getCompAsLong((attr & UNSIGNED) != 0, offs, byteLength);
				bd = new BigDecimal(val/Math.pow(10,scale)).setScale(scale, BigDecimal.ROUND_HALF_DOWN);
			}
		    s = bd.toPlainString();
		    int len=displayLength;
		    if((attr & EXTRACT) == 0) {
		    	if(scale > 0) len++;
		    	if((attr & UNSIGNED) == 0) len++;
		    }
		    //s = adjustLengthLeft(s, displayLength, ' ');
		    if(s.length() < len) {
		    	s = adjustLengthLeft(s, len, '0', false);
		    } else if(s.length() > len) {
		    	s = s.substring(s.length()-len);
		    }
		} else if((attr & NUMBER) != 0) {
			s = ((GroupData)value).getString(offs, byteLength, charset);
			//Check if there are non-numeric values. If there are, just return the string.
			for(int i=0; i<(s.length()-1); i++) {
				char c = s.charAt(i);
				if((c >= '0' && c <= '9') || c == ' ' || c == '.' || c == '-') {
					continue;
				}
				return s;
			}
			if(!((attr & (PICTURE)) != 0) && (attr & NUMBER) != 0) {
				StringBuilder sb = new StringBuilder(s.replace(' ', '0'));
				boolean sign;
				boolean replace;
				byte c = (byte) sb.charAt(sb.length() - 1);
				
				if(c == '-') {
					sign = true;
					sb.deleteCharAt(sb.length()-1);
				} else {
					if(c >= 'p' && c <= 'y') {c = (byte) (c - 'p'); sign = true; replace = true; }
					else if(c >= 'A' && c <= 'I') {c = (byte) (c - 'A' + 1); sign = false; replace = true; }
					else if(c >= 'J' && c <= 'R') {c = (byte) (c - 'J' + 1); sign = true; replace = true; }
					else if(c >= 'S' && c <= 'Z') {c = (byte) (c - 'S' + 2); sign = false; replace = true; }
					else if(c == '}') {c = 0; sign = true; replace = true; }
					else if(c == '{') {c = 0; sign = false; replace = true; }
					else {c -= '0'; sign = false; replace = false; }
					if(replace) {
						sb.deleteCharAt(sb.length()-1);
						sb.append(c);
					}
				}
				if(scale > 0 && scale <= displayLength) {
					int dpx = sb.indexOf(".");
					if(dpx == -1) {
						sb.insert(displayLength-scale, '.');
					} else {
						if(dpx < (sb.length() - scale - 1)) {
							sb.delete(dpx, dpx + scale + 1);
						} else if(dpx > (sb.length() - scale - 1)) {
							int l = sb.length() - dpx + 1;
							for(int i=0; i<l; i++) {
								sb.append("0");
								sb.deleteCharAt(0);
							}
						}
					}
				}
				if(sign) {
					if((attr & UNSIGNED) == 0)
						sb.insert(0, '-');
				}
				s = sb.toString();
			}
		} else {
			s = ((GroupData)value).getString(offs, byteLength, charset);
		}
		
		if((attr & (Var.NUMBER)) != 0 && (attr & (ZEROBLANK)) == 0) {
			if(picture == null) {
				int idx = s.indexOf('-');
				if(idx == -1) {
					s = s.replaceAll(" ", "0");
				} else if(idx > 0) {
					s = "-" + s.substring(1, idx).replaceAll(" ", "0") +
							"0" + s.substring(idx+1);
				}
			}
		}
		
		/*
		if(parent != null) s = parent.getString().substring(groupOffset, groupOffset + len);
		else {
	    	if(value == null) value = new StringBuilder(spaces.substring(0, groupOffset+len));
	    	else if(((StringBuilder)value).length() < (groupOffset+len))
	    		((StringBuilder)value).append(spaces.substring(0, (groupOffset+len)-((StringBuilder)value).length()));
			s = ((StringBuilder)value).substring(groupOffset, groupOffset + len);
	    	//s = getRawString().substring(groupOffset, groupOffset + len);
		}
		*/
		
//		if(!(attr(PICTURE)) && attr(NUMBER)) s = s.replace(' ', '0');
		/*if(dec > 0 && dec <= len) {
			StringBuilder xs = new StringBuilder(s);
			xs.insert(len-dec, '.');
			return xs.toString();
		}*/
		return s;
	}
	
	@Override
	public String getString(boolean rz) {
		if((attr & (COMP)) != 0) {
			long lv = getCompAsLong();
			if(scale > 0) {
				BigDecimal bd = new BigDecimal(lv).setScale(scale);
				return bd.toPlainString();
			} else return Long.toString(lv);
		}
		
		String r;
		try {
			r = new String(getBytes(), charset);
		} catch (UnsupportedEncodingException e) {
			r = null;
			e.printStackTrace();
		}
		
		if((attr & (Var.NUMBER)) != 0) {
			r = r.replaceAll(" ", "0");
		}
		if(rz) return r;
		r = r.replaceAll("^0*", "");
		if(r.length() == 0) r = "0";
		
		return r;
	}
	
	/**
	 * When a non-elementary item is going to receive data from another item.
	 * Negative values are absoluted.
	 * Numbers are prefixed with 0 to the required length
	 * @return
	 */
	public String getRawString() {
		String r;
		if((attr & (COMP)) != 0) {
			long lv = Math.abs(getCompAsLong());
			if(scale > 0) {
				double dv = lv;
				BigDecimal bd = new BigDecimal(dv).setScale(0, BigDecimal.ROUND_HALF_DOWN);
				r = bd.toPlainString();
			} else r = Long.toString(lv);
			if(r.length() < displayLength) {
				r = zeros.substring(0, displayLength - r.length()) + r;
			} else if(r.length() > displayLength) {
				r = r.substring(0, displayLength);
			}

		} else {
			if((attr & (Var.NUMBER)) != 0) {
				byte [] b = getBytes();
				//Remove overstamp
				b[b.length-1] &= 0x3F;
				if((attr & ZEROBLANK) == 0 && picture == null) {
					if(b.length == 0)
						r = "0";
					else
						r = new String(b).replaceAll(" ", "0");
				} else {
					r = new String(b);
				}
			} else {
				try {
					r = new String(getBytes(), charset);
				} catch (UnsupportedEncodingException e) {
					r = "";
					e.printStackTrace();
				}
			}
		}
		return r;
	}
	
	private final GroupData cacheMyGroupData() {
		if(value == null)
			value = parent.cacheMyGroupData();
		myCachedOffset = getRealOffset(0);
		return (GroupData)value;
		
//		if(parent == null)
//			return (GroupData)value;
//		return parent.getMyGroupData();
	}
	
	
	public int getMyOffset() {
		if(myCachedOffset == -1)
			myCachedOffset = getRealOffset(0);
		return myCachedOffset;
	}


	
	@Override
	public final byte [] getBytes() {
		int offs = myCachedOffset;
		return ((GroupData)value).subString(offs, offs + byteLength);
	}
	
	/**
	 * Returns the underlying byte array supporting this group.
	 * @return
	 */
	public byte [] getRawBytesArray() {
		return ((GroupData)value).getByteArray();
	}
	
	@Override
	public int getLen() {
		return displayLength;
	}
	
	public String toString() {
		try {
			if(indexLevel > 0) {
				int offs = getRealOffset(groupOffset);
				return new String(((GroupData)value).getBytes(), offs, byteLength);
			}
			return getString().trim();
		} catch (Exception e) {}
		return "";
	}
	
	/*
	@Override
	public Group set(Var v) {
		if (this.overflow()) this.attr &= ~(OVERFLOW);
		if(v == null) return this;
		if(v.overflow()) this.attr |= OVERFLOW;

		if(v.testAttr(AUTOVAR)) {
			try {
				set(v.getString(this.attr,displayLength, scale));
			} catch(Exception e) { ; }
		} else if((attr & (NUMBER)) != 0 && v.testAttr(NUMBER)) {
			if((attr & (LONG)) != 0) set(v.getLong(displayLength));
			//else if((attr & (DOUBLE)) != 0) set(v.getDouble(displayLength, scale));
			else if((attr & (DOUBLE)) != 0) set(v.getDouble());
			else set(v.getString());
		} else if((v.attr & GROUP) != 0 && (attr & v.attr & CHAR) != 0) {
				set("");
				setSubstr(((Group)v).getBytes(), 0);
		} else if(v.testAttr(GROUP) && ((attr & (LONG)) != 0 || (attr & DOUBLE) != 0) && v.displayLength < displayLength) {
			//Special case where we are numeric, sending field is group and our length is longer
			Var v2 = new Var(zeros.substring(0, displayLength - v.displayLength) + v.getString());
			if((attr & (LONG)) != 0) set(v2.getLong(displayLength));
			else if((attr & (DOUBLE)) != 0) set(v2.getDouble(displayLength, scale));
		} else if(v.testAttr(GROUP) && v.testAttr(CHAR) && ((attr & (NUMBER)) != 0) && ((attr & (PICTURE)) != 0)) {
			//If we are assigning a pic x field to a field with a picture the picture is ignored 
			attr &= ~(PICTURE);
			set(v.getString());
			attr |= PICTURE;
		} else {
			set(v.getString());
		}
		return this;
	}
	*/
	public static int countCharChar;
	public void gotCharChar() {
		countCharChar++;
	}
	public static int countCharNumber;
	public void gotCharNumber() {
		countCharNumber++;
	}
	public static int countNumberNumber;
	public void gotNumberNumber() {
		countNumberNumber++;
	}
	public static int countCharDouble;
	public void gotCharDouble() {
		countCharDouble++;
	}
	public static int countDoubleDouble;
	public void gotDoubleDouble() {
		countDoubleDouble++;
	}
	public static int countSomethingElse;
	public void gotSomethingElse() {
		countSomethingElse++;
	}
	
	@Override
	public Group set(Var v) {
		if(v == null) return this;
		if((v.attr & OVERFLOW) != 0) attr |= OVERFLOW;
		else attr &= ~(OVERFLOW);
		
		if((attr & PICTURE) != 0) {
			return set(v.getString());
		}

		if(v.testAttr(AUTOVAR)) {
			try {
				set(v.getString(this.attr,displayLength, scale));
			} catch(Exception e) { ; }
		} else if(members != null) {
			byte [] dst = ((GroupData)value).getByteArray();
			byte [] src = v.getBytes();
 			int offset = getMyByteArrayOffset();
			if(src.length >= byteLength) {
				System.arraycopy(src, 0, dst, offset, byteLength);
			} else {
				System.arraycopy(src, 0, dst, offset, src.length);
				int fillFrom = offset + src.length;
				int fillTo = offset + byteLength;
//				System.out.println("Arrays.fill: array.length="+dst.length+" from="+fillFrom+" to="+fillTo);
				Arrays.fill(dst, fillFrom, fillTo, (byte)' ');
			}
//			notifyCachingGroups();
		} else if((attr & (CHAR)) != 0) {
			//Receiving item is a non-elementary group with members.
			byte [] b;
			int srcOffset;
			int srcLen;
			if((v.attr & NUMBER) != 0) {
				//Avoid slower charset conversion - numbers only
				b = getBytesFast(v.getRawString());
			} else {
				try {
					b = v.getRawString().getBytes(charset);
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
					b = v.getRawString().getBytes();
				}
			}
			srcOffset = 0;
			srcLen = b.length;

			byte [] dst = ((GroupData)value).getByteArray();
			int offset = getMyByteArrayOffset();
			if(srcLen >= byteLength) {
				System.arraycopy(b, srcOffset, dst, offset, byteLength);
			} else {
				System.arraycopy(b, srcOffset, dst, offset, srcLen);
				Arrays.fill(dst, offset + srcLen , offset + byteLength, (byte)' ');
			}
//			notifyCachingGroups();
		} else if(v.testAttr(COMP)) {
			//set(v.getLong()); //v1
			//set(v.getDouble()); //v2
			if((attr & (NUMBER)) != 0) set(v.getDouble()); //v3
			else set(v.getString());
		} else if((attr & (NUMBER)) != 0 && v.testAttr(NUMBER)) {
			if((attr & (LONG)) != 0) set(v.getLong(displayLength));
			//else if((attr & (DOUBLE)) != 0) set(v.getDouble(displayLength, scale));
			else if((attr & (DOUBLE)) != 0) set(v.getDouble());
			else set(v.getString());
		//Temporary fix fir edited pic x fields
		} else if((attr & (CHAR)) != 0 && v.testAttr(CHAR) && ((attr & (PICTURE)) != 0))  {
			set(v.getString());
		} else if((v.attr & GROUP) != 0 && (attr & v.attr & CHAR) != 0) {
			byte [] bytes = ((Group)v).getBytes();
			set("");
			setSubstr(bytes, 0);
//			notifyCachingGroups();
		} else if(v.testAttr(GROUP) && ((attr & (LONG)) != 0 || (attr & DOUBLE) != 0) && v.displayLength < displayLength) {
			//Special case where we are numeric, sending field is group and our length is longer
			Var v2 = new Var(zeros.substring(0, displayLength - v.displayLength) + v.getString());
			if((attr & (LONG)) != 0) set(v2.getLong(displayLength));
			else if((attr & (DOUBLE)) != 0) set(v2.getDouble(displayLength, scale));
		} else if(v.testAttr(CHAR) && ((attr & (NUMBER)) != 0) && ((attr & (PICTURE)) != 0)) {
			//If we are assigning a pic x field to a field with a picture the picture is ignored 
			attr &= ~(PICTURE);
			set(v.getString());
			attr |= PICTURE;
		} else {
			set(v.getString());
		}
		return this;
	}
	
	public Group set(Group g) {
		if(g == null) return this;
		
		//Optimise for moving data between identical groups.
		if(byteLength == g.byteLength && (attr & ~(ISASSIGNED|OVERFLOW)) == (g.attr & ~(ISASSIGNED|OVERFLOW)) &&
				(g.picture == null || (g.picture != null && g.picture.isFormatted))) {
			//Identical types, just copy data
			if(scale != g.scale) {
				if((attr & COMP) != 0 && (g.attr & COMP) != 0) {
					int diff = scale - g.scale;
					long v = ((GroupData)g.value).getCompAsLong((g.attr & UNSIGNED) != 0, g.myCachedOffset, g.byteLength);
					if(diff < 0) {
						v /= powerLong[-diff];
					} else {
						v *= powerLong[diff];
					}
					storeComp(v);
				} else {
					set(g.getDouble());
				}
			} else {
				System.arraycopy(((GroupData)g.value).byteArray.data, g.myCachedOffset, ((GroupData)value).byteArray.data, myCachedOffset, byteLength);
				if(picture != null) {
					attr = g.attr;
					picture.isFormatted = g.picture.isFormatted;
				}
			}
//			notifyCachingGroups();
			return this;
		}
		
		if((g.attr & OVERFLOW) != 0) attr |= OVERFLOW;
		else attr &= ~(OVERFLOW);

		if(((attr & CHAR) != 0 && (g.attr & CHAR) != 0 && (attr & PICTURE) == 0) || g.members != null) {
			int srcOffset = g.myCachedOffset;
			int srcLen = g.byteLength;
			int dstOffset = myCachedOffset;
			int dstLen = byteLength;
			if(dstLen > srcLen) {
				System.arraycopy(((GroupData)g.value).byteArray.data, srcOffset, ((GroupData)value).byteArray.data, dstOffset, srcLen);
				Arrays.fill(((GroupData)value).byteArray.data, dstOffset+srcLen, dstOffset+dstLen, (byte)' ');
			} else {
				System.arraycopy(((GroupData)g.value).byteArray.data, srcOffset, ((GroupData)value).byteArray.data, dstOffset, dstLen);
			}
//			notifyCachingGroups();
			return this;
		} else if((attr & COMP) != 0 && (g.attr & COMP) != 0) {
			if(byteLength < 5 && g.byteLength < 5) {
				int v = ((GroupData)g.value).getCompAsInt((g.attr & UNSIGNED) != 0, g.myCachedOffset, g.byteLength);
				storeScaledCompValue(v, g.scale);
			} else {
				long v = ((GroupData)g.value).getCompAsLong((g.attr & UNSIGNED) != 0, g.myCachedOffset, g.byteLength);
				storeScaledCompValue(v, g.scale);
			}
//			notifyCachingGroups();
			return this;
		} else if((g.attr & COMP) != 0 && (attr & PICTURE) != 0) {
			long lv = g.getLong();
			
			String s;
			if(g.scale > 0) {
				BigDecimal bd = new BigDecimal(lv);
				if(g.scale > 0) {
					bd = bd.movePointLeft(g.scale).setScale(g.scale);
				}
				s = bd.toPlainString();
			} else {
				s = Long.toString(lv);
			}
			return set(s);
		}

		if((attr & PICTURE) != 0) {
			return set(g.getString());
		}

		if(g.testAttr(AUTOVAR)) {
			try {
				set(g.getString(this.attr,displayLength, scale));
			} catch(Exception e) { ; }
		} else if(members != null) {
			byte [] dst = ((GroupData)value).getByteArray();
			byte [] src = g.getBytes();
			if(src.length >= byteLength) {
				System.arraycopy(src, 0, dst, myCachedOffset, byteLength);
			} else {
				System.arraycopy(src, 0, dst, myCachedOffset, src.length);
				int fillFrom = myCachedOffset + src.length;
				int fillTo = myCachedOffset + byteLength;
//				System.out.println("Arrays.fill: array.length="+dst.length+" from="+fillFrom+" to="+fillTo);
				Arrays.fill(dst, fillFrom, fillTo, (byte)' ');
			}
			
		} else if((attr & (CHAR)) != 0) {
			//Receiving item is a non-elementary group with members.
			byte [] b;
			int srcOffset;
			int srcLen;
			if(g instanceof Group) {
				if(((Group) g).members != null || (g.attr & CHAR) != 0) {
					//Avoid byte[] string conversion or unnecessary array copying
					b = ((GroupData)((Group)g).value).getByteArray();
					srcOffset = ((Group)g).getMyByteArrayOffset();
					srcLen = ((Group) g).byteLength;
				} else {
					//Avoid slower charset conversion - numbers only
					b = getBytesFast(g.getRawString());
					srcOffset = 0;
					srcLen = b.length;
				}
			} else {
				if((g.attr & NUMBER) != 0) {
					//Avoid slower charset conversion - numbers only
					b = getBytesFast(g.getRawString());
				} else {
					try {
						b = g.getRawString().getBytes(charset);
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
						b = g.getRawString().getBytes();
					}
				}
				srcOffset = 0;
				srcLen = b.length;
			}
			
			byte [] dst = ((GroupData)value).getByteArray();
			int offset = getMyByteArrayOffset();
			if(srcLen >= byteLength) {
				System.arraycopy(b, srcOffset, dst, offset, byteLength);
			} else {
				System.arraycopy(b, srcOffset, dst, offset, srcLen);
				Arrays.fill(dst, offset + srcLen , offset + byteLength, (byte)' ');
			}
//			notifyCachingGroups();
		} else if(g.testAttr(COMP)) {
			if(g.scale == 0) {
				if(g.byteLength < 5) set(g.getInt());
				else set(g.getLong());
			} else if((attr & (NUMBER)) != 0) {
				set(g.getDouble()); //v3
			} else {
				set(g.getString());
			}
		} else if((attr & (NUMBER)) != 0 && g.testAttr(NUMBER)) {
			if((attr & (LONG)) != 0) set(g.getLong(displayLength));
			//else if((attr & (DOUBLE)) != 0) set(v.getDouble(displayLength, scale));
			else if((attr & (DOUBLE)) != 0) set(g.getDouble());
			else set(g.getString());
		//Temporary fix fir edited pic x fields
		} else if((attr & (CHAR)) != 0 && g.testAttr(CHAR) && ((attr & (PICTURE)) != 0))  {
			set(g.getString());
		} else if((g.attr & GROUP) != 0 && (attr & g.attr & CHAR) != 0) {
			byte [] bytes = ((Group)g).getBytes();
			set("");
			setSubstr(bytes, 0);
//			notifyCachingGroups();
		} else if(g.testAttr(GROUP) && ((attr & (LONG)) != 0 || (attr & DOUBLE) != 0) && g.displayLength < displayLength) {
			//Special case where we are numeric, sending field is group and our length is longer
			Var v2 = new Var(zeros.substring(0, displayLength - g.displayLength) + g.getString());
			if((attr & (LONG)) != 0) set(v2.getLong(displayLength));
			else if((attr & (DOUBLE)) != 0) set(v2.getDouble(displayLength, scale));
		} else if(g.testAttr(CHAR) && ((attr & (NUMBER)) != 0) && ((attr & (PICTURE)) != 0)) {
			//If we are assigning a pic x field to a field with a picture the picture is ignored 
			attr &= ~(PICTURE);
			set(g.getString());
			attr |= PICTURE;
		} else {
			set(g.getString());
		}
		return this;
	}
	

	/**
	 * Sets a floating point value as a long with a scale
	 * @param val The Integer value with implied decimal
	 * @param vScale The position (scale) of decimal point
	 */
	private void storeScaledCompValue(long val, short vScale) {
		if(scale < vScale) {
			for(; vScale > scale; vScale--) {
				val /= 10;
			}
		} else if(scale > vScale) {
			for(; vScale < scale; vScale++) {
				val *= 10;
			}
		}
		long v = val % powerLong[displayLength];
		if(v != val) {
			attr |= OVERFLOW;
		}
		((GroupData)value).storeComp(myCachedOffset, byteLength, v);
	}

	/**
	 * Sets a floating point value as a long with a scale
	 * @param val The Integer value with implied decimal
	 * @param vScale The position (scale) of decimal point
	 */
	private void storeScaledCompValue(int val, short vScale) {
		if(scale < vScale) {
			for(; vScale > scale; vScale--) {
				val /= 10;
			}
		} else if(scale > vScale) {
			for(; vScale < scale; vScale++) {
				val *= 10;
			}
		}
		int v = val % powerInt[displayLength];
		if(v != val) {
			attr |= OVERFLOW;
		}
		((GroupData)value).storeComp(myCachedOffset, byteLength, v);
	}
	
	/**
	 * Assign a byte array to this group
	 */
	public Group set(byte [] src) {
		int offset = myCachedOffset;
		((GroupData)value).setSubString(src, offset, offset + byteLength);
//		notifyCachingGroups();
		return this;
	}
	
	/**
	 * Assign a portion of a byte array to the group.
	 * @param src The byte array to copy from
	 * @param from The position in src from where to start the copy
	 * @param len The length of the byte array to copy
	 * @return This group
	 */
	@Override
	public Group set(byte [] src, int from, int len) {
		GroupData data = ((GroupData)value);
		int offset = myCachedOffset;
//		if((attr & (CHAR)) != 0) {
//			for(int i = from; i < (from + len); i++) {
//				if(src[i] == (byte)0)
//					src[i] = ' ';
//			}
//		}
		//System.out.println("before: group="+this.getString());
		//System.out.println("offset="+from+" len="+len);
		//System.out.println("src="+new String(src, from, len));
		if(len > byteLength) {
			len = byteLength;
		}
		data.setSubString(src, from, offset, len);
		if(len < byteLength) {
			byte [] b = new byte[1];
			b[0] = ' ';
			data.fill(b, offset + len, offset + byteLength);
		}
		//System.out.println("after: group="+this.getString());
//		notifyCachingGroups();
		return this;
	}
	
	
	private Group _setComp(String s) {
		attr &= ~(OVERFLOW);

		if((attr & (LONG)) != 0) {
			long v = lincStrToLong(s, displayLength, CHAR);
			if((attr & (UNSIGNED)) != 0)
				v = Math.abs(v);
			storeComp(v);
		} else if((attr & (DOUBLE)) != 0) {
			double d = lincStrToDouble(s);
			if((attr & (UNSIGNED)) != 0)
				d = Math.abs(d);
			set(d);
		}
		attr |= ISASSIGNED;
		return this;
	}
	
	private Group _setNumeric(String s) {
		String sx = s;
		attr &= ~(OVERFLOW);

		if((attr & (PICTURE)) != 0) {
			s = picture.format(s, (attr & ZEROBLANK) != 0);
//			if(s.length() > len) s = s.substring(s.length() - len);
		} 

		if(s.length() > displayLength)
			s = s.substring(s.length() - displayLength);
	
		if((attr & (PICTURE)) == 0) {
			if((attr & (LONG)) != 0) {
				long v = lincStrToLong(s, displayLength, CHAR);
				s = longToLincString(v, false);
			} else if((attr & (DOUBLE)) != 0) {
				double d = lincStrToDouble(s);
				s = doubleToLincString(d, false);
			}
		}
		if(s.length() < displayLength) { //Add zeros to left
			boolean sign;
			if(s.length() > 0 && s.charAt(0) == '-') {
				sign = true;
				s = s.substring(1);
			} else sign = false;
			if(sign)
				s = "-" + zeros.substring(0, displayLength - s.length() - 1) + s;
			else 
				s = zeros.substring(0, displayLength - s.length()) + s;
		}

		byte[] b;
		//			b = s.getBytes(charset);
		b = getBytesFast(s);
		if((attr & (NUMERIC)) != 0) {
			if(b.length != byteLength) {
				b = adjustByteLengthLeft(b, byteLength, initialPadChar());
			}
			if(b.length < sx.length()) {
				//Check that the digits we're discarding are actual values before setting flag.
				for(int i=0; i< (sx.length()-b.length);i++) {
					char c = sx.charAt(i);
					if(c != ' ' && c != '0') {
						attr |= OVERFLOW;
						break;
					}
				}
			}
		} else if(b.length != byteLength) {
			b = adjustByteLengthRight(b, byteLength);
		}

		((GroupData)value).setSubString(b, myCachedOffset);
		attr |= ISASSIGNED;
//		notifyCachingGroups();

		return this;
	}
	

	@Override
	public Group set(String s) {
		if(s == null || s.length() == 0) {
			if((attr & (COMP)) != 0) {
				attr &= ~(OVERFLOW);
				storeComp(0);
//				notifyCachingGroups();
				return this;
			} else if((attr & (NUMERIC)) != 0) {
				if((attr & (PICTURE)) != 0) {
					return _setNumeric(s);
				} 
				return set(0);
			}
			Arrays.fill(getRawBytesArray(), myCachedOffset, myCachedOffset + byteLength, (byte)' ');
//			notifyCachingGroups();
			return this;
		}
		if((attr & (COMP)) != 0) {
//			notifyCachingGroups();
			return _setComp(s);
		} else if((attr & (NUMERIC)) != 0) {
			return _setNumeric(s);
		}
		return _setCharType(s);
	}
	
	@Override
	public Group increment(long amount) {
		if((attr & (COMP)) != 0) {
			long i = getCompAsLong();
			i += amount;
			storeComp(i);
		} else if((attr & (LONG)) != 0) {
			long i = lincStrToLong(getGroupSubstringAsString()); 
			i += amount;
			set(i);
		} else if((attr & (DOUBLE)) != 0) {
			double d = lincStrToDouble(getGroupSubstringAsString());
			d += amount;
			set(d);
		} else {
			set(getInt() + amount);
		}
		return this;
	}

	/**
	 * This is much faster than String.getBytes() but will only work
	 * with iso8859-1 or iso8859-2
	 * @param str
	 * @return
	 */
    private static byte[] getBytesFast(String str) {
        final int length = str.length();
        final char buffer[] = new char[length];
        str.getChars(0, length, buffer, 0);
        final byte b[] = new byte[length];
        for (int j = 0; j < length; j++)
            b[j] = (byte) buffer[j];
        return b;
    }

	
	private Group _setCharType(String s) {
		byte[] b;
		/*try {
			b = s.getBytes(charset);
		} catch (UnsupportedEncodingException e) {
			b = null;
			e.printStackTrace();
			return this;
		}*/
		//Temporary fix untl Pete does it properly
		if((attr & (PICTURE)) != 0) {
			s = picture.formatx(s);
		}
		b = getBytesFast(s);
		
		((GroupData)value).setSubString(b, myCachedOffset, myCachedOffset + byteLength);
		attr |= ISASSIGNED;
//		notifyCachingGroups();
		return this;
	}
	
	public byte[] stringToByte (String s) {
		byte[] ba = new byte[s.length()/2];
		s+="  ";
    	for(int i=0;i<ba.length * 2;i+=2) {
    		ba[i/2]=(byte)Integer.parseInt(s.substring(i, i+2), 16);
    	}
    	return ba;
	}
	/*
	 * Assigns a hex String to a Group. 
	 * 
	 * @param val
	 *            the string to assign
	 */
	@Override
    public Group setHex(String v) {
    	set(stringToByte(v));
    	return this;
   	}
    
	
	public boolean neHex(String s) {
		return !eqHex(s);
	}
	public boolean eqHex(String s) {
		byte[] sba = stringToByte(s);
		int offs = myCachedOffset;
		byte [] mba = ((GroupData)value).subString(offs, offs + byteLength);
		return Arrays.equals(sba,mba);
	}

    /**
	 * Fills a Var with the same hex characters repeated. 
	 * 
	 * @param val
	 *            the string to assign
	 */
    public Group fillHex(String v) {
    	byte [] b;
    	if(v == null || v.length() == 0) {
    		b = new byte[1];
    		b[0] = (byte)' ';
    	} else 
    		b = stringToByte(v);
    	
    	int offs = myCachedOffset;
    	((GroupData)value).fill(b, offs, offs + byteLength);
    	return this;
   	}
	/**
	 * Gets the real offset of this group.
	 */
	private int getRealOffset(int offset) {
		if(parent == null) return offset + this.groupOffset;
		return parent.getRealOffset(offset + this.groupOffset);
	}
	
	public int getMyByteArrayOffset(int offset) {
		if(parent == null) {
			GroupData dat = (GroupData) (GroupData)value;
			return offset + this.groupOffset;
		}
		return parent.getMyByteArrayOffset(offset + this.groupOffset);
	}
	
	public int getMyByteArrayOffset() {
		int offs = getMyByteArrayOffset(0);
		return offs;
	}

	protected Group getRealGroup() {
		if(parent == null) return this;
		return parent.getRealGroup();
	}
	
	public void upshift() {
		if((attr & (NUMERIC)) != 0) return;
		int start = getRealOffset(0);
		((GroupData)value).upshift(start, start + byteLength);
	}

	public void upshift(int start, int len) {
		if((attr & (NUMERIC)) != 0) return;
		start = getRealOffset(0) + start;
		((GroupData)value).upshift(start, start + len);
	}
	

	public void downshift() { //Overridden in Group
		if((attr & (NUMERIC)) != 0) return;
		int start = getRealOffset(0);
		((GroupData)value).downshift(start, start + byteLength);
	}

	public void downshift(int start, int len) {
		if((attr & (NUMERIC)) != 0) return;
		start = getRealOffset(0) + start;
		((GroupData)value).downshift(start, start + len);
	}

	protected static final MathContext mathContext = new MathContext(0, RoundingMode.DOWN);
	@Override
	public Group set(double val) {
		String s;
		attr &= ~(OVERFLOW);
		
		if(Double.isNaN(val) || Double.isInfinite(val)){
			attr |= OVERFLOW;
			return this; //to catch set(0/0) mf cobol seems to leave unchanged  
		}
		if((attr & (COMP)) != 0) {
			if((attr & (UNSIGNED)) != 0)
				val = Math.abs(val);
		    val = val * Math.pow(10, scale);
		    val = f_chop(val, displayLength, 0);
//			bd = new BigDecimal(val).setScale(0, BigDecimal.ROUND_HALF_DOWN);
//			bd = new BigDecimal(val).setScale(0, BigDecimal.ROUND_FLOOR);
			BigDecimal bd = new BigDecimal(val, mathContext);//.setScale(0, BigDecimal.ROUND_DOWN);
			long lv = bd.longValue();
			if(exp == 0) exp = (byte) displayLength;
			long v = lv % (long)Math.pow(10, exp);
			if(v != lv) 
				attr |= OVERFLOW;
			storeComp(v);
//			notifyCachingGroups();
			return this;
		} else if((attr & (PICTURE)) != 0) {
			if((attr & NUMERIC) != 0) {
			    val = f_chop(val, displayLength, scale);
				return _setNumeric(BigDecimal.valueOf(val).toPlainString());
			}
			return _setCharType(BigDecimal.valueOf(val).toPlainString());
		} else if((attr & (NUMERIC)) != 0) {
			s = doubleToLincString(val, true);
		} else {
			s=BigDecimal.valueOf(val).toPlainString();
		}
		setRealSubstring(getBytesFast(s), myCachedOffset, byteLength);
		return this;
	}

	@Override
	public Group set(long val) {
		attr &= ~(OVERFLOW);

		String s;
		if((attr & (COMP)) != 0) {
			if((attr & (UNSIGNED)) != 0)
				val = Math.abs(val);
			
			if(scale > 0) {
				val = (long)(val * Math.pow(10,scale));
			}
			storeComp(val);
			//storeComp(val);
//			notifyCachingGroups();
			return this;
		} else if((attr & (PICTURE)) != 0) {
			if((attr & NUMERIC) != 0)
				return _setNumeric(Long.toString(val));
			return _setCharType(Long.toString(val));
		} else if((attr & (NUMERIC)) != 0) {
			if((attr & (DOUBLE)) != 0) {
				s = doubleToLincString(val, true);  //sjr ADDED
			}
			else {
				if(exp == 0) exp = (byte) displayLength;
				long lv = val % (long)Math.pow(10, exp);
				if (lv != val)
					attr |= OVERFLOW;
				s = longToLincString(lv, true);
			}
		} else {
			s = Long.toString(val);
		}
//		notifyCachingGroups();
		setRealSubstring(getBytesFast(s), myCachedOffset, byteLength);
//		setGroupSubstring(getBytesFast(s));
		return this;
	}
	
	
	@Override
	public Group compute(double val) {
		if(groupMode != GroupMode.MICROFOCUS || (attr & (COMP)) == 0) {
			return set(val);
		}
		
		attr &= ~(OVERFLOW);
		if(Double.isNaN(val) || Double.isInfinite(val)){
			attr |= OVERFLOW;
			return this; //to catch set(0/0) mf cobol seems to leave unchanged  
		}

		if((attr & (UNSIGNED)) != 0)
			val = Math.abs(val);
		val = val * Math.pow(10, scale);
		val = f_chop(val);
		BigDecimal bd = new BigDecimal(val, mathContext);//.setScale(0, BigDecimal.ROUND_DOWN);
		long lv = bd.longValue();
		storeComp(lv);
		return this;
	}

	@Override
	public Group compute(long val) {
		attr &= ~(OVERFLOW);

		String s;
		if((attr & (COMP)) != 0) {
			if((attr & (UNSIGNED)) != 0)
				val = Math.abs(val);
			long lv = (long)(val * Math.pow(10,scale));
			storeComp(lv);
			//storeComp(val);
			return this;
		} else if((attr & (PICTURE)) != 0) {
			if((attr & NUMERIC) != 0)
				return _setNumeric(Long.toString(val));
			return _setCharType(Long.toString(val));
		} else if((attr & (NUMERIC)) != 0) {
			if((attr & (DOUBLE)) != 0) {
				s = doubleToLincString(val, true);  //sjr ADDED
			}
			else {
				if(exp == 0) exp = (byte) displayLength;
				long lv = val % (long)Math.pow(10, exp);
				if (lv != val)
					attr |= OVERFLOW;
				s = longToLincString(lv, true);
			}
		} else {
			s = Long.toString(val);
		}
		setGroupSubstring(getBytesFast(s));
		return this;
	}

	
	protected void storeComp(long val) {
		((GroupData)value).storeComp(myCachedOffset, byteLength, val);
	}

	protected void storeComp(int val) {
		((GroupData)value).storeComp(myCachedOffset, byteLength, val);
	}

	protected long getCompAsLong() {
		return ((GroupData)value).getCompAsLong((attr & UNSIGNED) != 0, myCachedOffset, byteLength);
	}

	protected int getCompAsInt() {
		return ((GroupData)value).getCompAsInt((attr & UNSIGNED) != 0, myCachedOffset, byteLength);
	}

	protected double getCompAsDouble() {
		return ((GroupData)value).getCompAsDouble((attr & UNSIGNED) != 0, myCachedOffset, byteLength, scale);
	}


	/**
	 * Checks for equality between the this and the parameter.
	 * Note that for comparison between CHAR and NUMBER types, 0 is regarded as 
	 * equal to spaces. To change this behaviour, set "jclib.space.zero.equal = false"
	 * in the properties file.
	 * 
	 * @param v The Var type to compare to
	 * @return true if they are equal otherwise false.
	 */
	@Override
	public boolean eq(Var g) {
		//Optimize for integer comps
		if((attr & COMP) != 0 && (g.attr & COMP) != 0 && scale == 0 && g.scale == 0) {
			if(byteLength < 5 && ((Group)g).byteLength < 5 && scale == 0 && g.scale == 0) {
				return ((GroupData)value).getCompAsInt((attr & UNSIGNED) != 0, myCachedOffset, byteLength) ==
						((GroupData)g.value).getCompAsInt((attr & UNSIGNED) != 0, ((Group)g).myCachedOffset, ((Group)g).byteLength);			
			} else {
				return ((GroupData)value).getCompAsLong((attr & UNSIGNED) != 0, myCachedOffset, byteLength) ==
						((GroupData)g.value).getCompAsLong((attr & UNSIGNED) != 0, ((Group)g).myCachedOffset, ((Group)g).byteLength);			
			}
		}
		
		//Optimize for most common - numeric vs numeric, no doubles.
		if((attr & NUMERIC) != 0 && (g.attr & NUMERIC) != 0 && scale == 0 && g.scale == 0) {
			if(displayLength < 10 && g.displayLength < 10) {
				int a, b;
				if((attr & COMP) != 0) {
					a = ((GroupData)value).getCompAsInt((attr & UNSIGNED) != 0, myCachedOffset, byteLength);
				} else {
					a = getInt();	
				}
				if((g.attr & COMP) != 0) {
					b = ((GroupData)g.value).getCompAsInt((attr & UNSIGNED) != 0, ((Group)g).myCachedOffset, ((Group)g).byteLength);
				} else {
					b = g.getInt();	
				}
				return a == b;
			} else {
				return getLong() == g.getLong();
			}
		}

		return compareTo(g) == 0;
	}
	
	/**
	 * Checks for equality between the Var and the parameter.
	 * Note that for comparison between CHAR and NUMBER types, 0 is regarded as 
	 * equal to spaces. To change this behaviour, set "jclib.space.zero.equal = false"
	 * in the properties file.
	 * 
	 * @param l
	 * @return true if they are same otherwise false.
	 */
	public boolean eq(long l) {
		//Optimize for most common - numeric vs numeric, no doubles.
		if(scale == 0) {
			if((attr & COMP) != 0) {
				if(byteLength < 5) { //32 bit or less
					return l == ((GroupData)value).getCompAsInt((attr & UNSIGNED) != 0, myCachedOffset, byteLength);
				} else {
					return l == ((GroupData)value).getCompAsLong((attr & UNSIGNED) != 0, myCachedOffset, byteLength);
				}
			}
			if(displayLength < 10) {
				return l == getInt();
			} else {
				return l == getLong();
			}
		}
		return eq((double)l);
	}

	/**
	 * Checks for equality between the Var and the parameter.
	 * Note that for comparison between CHAR and NUMBER types, 0 is regarded as 
	 * equal to spaces. To change this behaviour, set "jclib.space.zero.equal = false"
	 * in the properties file.
	 * 
	 * @param l
	 * @return true if they are same otherwise false.
	 */
	public boolean eq(int l) {
		//Optimize for most common - numeric vs numeric, no doubles.
		if(byteLength == 1 && (attr & COMP) != 0) {
			return ((GroupData)value).byteArray.data[myCachedOffset] == l;
		}
		if(scale == 0) {
			if((attr & COMP) != 0) {
				if(byteLength < 5) { //32 bit or less
					return l == ((GroupData)value).getCompAsInt((attr & UNSIGNED) != 0, myCachedOffset, byteLength);
				} else {
					return l == ((GroupData)value).getCompAsLong((attr & UNSIGNED) != 0, myCachedOffset, byteLength);
				}
			}
			if(displayLength < 10) {
				return l == getInt();
			} else {
				return l == getLong();
			}
		}
		return eq((double)l);
	}

	/**
	 * Checks for equality between the Var and the parameter. Trailing spaces
	 * are removed before the comparison is made.
	 * Note that for comparison between CHAR and NUMBER types, 0 is regarded as 
	 * equal to spaces. To change this behaviour, set "jclib.space.zero.equal = false"
	 * in the properties file.
	 * 
	 * @param s
	 * @return true if they are same otherwise false.
	 */
	@Override
	public boolean eq(String s) {
		if(byteLength == 1 && s.length() == 1) {
			return ((GroupData)value).byteArray.data[myCachedOffset] == s.charAt(0);
			
		}
		return compareTo(s) == 0;
	}

	@Override
	public int compareTo(String c) {
		int rv;
		if(c == null) c = "";

		int offs = myCachedOffset;
		try {
			String s = new String(((GroupData)value).subString(offs, offs + displayLength), charset);
			if(displayLength < c.length()) 
				s+=spaces.substring(0, c.length() - displayLength);
			else if(displayLength > c.length())
				c+=spaces.substring(0, displayLength - c.length());
			rv = s.compareTo(c);
		} catch (UnsupportedEncodingException e) {
			rv = -1;
			e.printStackTrace();
		}
		return rv;
	}
	
	@Override
	public int compareTo(Var g) {
		//Optimize for most common - numeric vs numeric, no doubles.
		/*if((attr & NUMERIC) != 0 && (g.attr & NUMERIC) != 0 && scale == 0 && g.scale == 0) {
			long l1 = getLong();
			long l2 = g.getLong();
			if(l1 < l2) return -1;
			if(l2 > l1) return 1;
			return 0;
		}*/
		
		long base;
		if((attr & (PICTURE|CHAR)) != 0 || (g.attr & (PICTURE)) != 0) base = CHAR;
		else if((attr & (DOUBLE)) != 0 || (g.attr & (DOUBLE)) != 0 || ((attr & (NUMERIC)) != 0 && scale > 0) || ((g.attr & (NUMERIC)) != 0 && g.scale > 0) ) base = DOUBLE;
		else if((attr & (LONG|NUMERIC)) != 0 || (g.attr & (LONG|NUMERIC)) != 0) base = LONG;
		else base = CHAR;
		
		if(base == DOUBLE) {
			double dd = getDouble();
			double vv;
			if(Config.COMPARE_SPACEZERO_NOT_EQUAL && (g.attr & (CHAR)) != 0) {
				String x = g.getString().trim();
				if(x.length() == 0 && eq(0.0)) return 1;
			}
			if((g.attr & (AUTOVAR)) != 0) vv = g.getDouble();
			else vv = g.getDouble();
			if(dd < vv) return -1;
			else if(dd > vv) return 1;
			else return 0;
		} else if(base == LONG) {
			long ll = getLong();
			long vv;
			if((g.attr & (AUTOVAR)) != 0) vv = g.getLong();
			else vv = g.getLong();
			if(Config.COMPARE_SPACEZERO_NOT_EQUAL && (g.attr & (CHAR)) != 0) {
				String x = g.getString().trim();
				if(x.length() == 0 && vv == 0) return 1;
			}
			if(ll < vv) return -1;
			else if(ll > vv) return 1;
			else return 0;
		} else {
			if((g.attr & (AUTOVAR)) != 0) return compareTo(g.getString(CHAR, displayLength, 0));
			else if(!Config.COMPARE_SPACEZERO_NOT_EQUAL && (g.attr & (NUMERIC)) != 0) {
				String x = getString().trim();
				if(x.length() == 0) {
					if(g.eq(0)) return 0;
				}
			} 
			return compareTo(g.getString());
		} 
	}

	
	/*
	@Override
	public int compareTo(long v) {
		if((attr & COMP) != 0) {
			long l = ((GroupData)value).getCompAsLong((attr & UNSIGNED) != 0, myCachedOffset, byteLength);
			if(scale > 0) {
				double d = (double) l / Math.pow(10, scale);
				if(d < (double)v) return -1;
				else if((double)d > v) return 1;
				else return 0;
			}
			if(l < v) return -1;
			else if(l > v) return 1;
			else return 0;
		}
		
		if(scale > 0) {
			
		} else {
			long l = ((GroupData)value).convertBytesToLong(myCachedOffset, byteLength, byteLength, scale, attr);
			if(l < v) return -1;
			else if(l > v) return 1;
			else return 0;
		}
		
		
		if((attr & (DOUBLE)) != 0 || scale > 0) {
			double d = getDouble(displayLength, scale);
			if(d < (double)v) return -1;
			else if((double)d > v) return 1;
			else return 0;
		} else if((attr & (CHAR)) != 0) {
			return compareTo(Long.toString(v));
		}
		long l = getLong(displayLength);
		if(l < v) return -1;
		else if(l > v) return 1;
		else return 0;
	}
	*/
	
	/*
	@Override
	public int compareTo(double v) {
		if((attr & (LONG)) != 0) {
			double d = getDouble(displayLength, 0);
			if(d < v) return -1;
			else if(d > v) return 1;
			else return 0;
		} else if((attr & (CHAR)) != 0) {
			return compareTo(Double.toString(v));
		}
		double dd = getDouble(displayLength, scale);
		double vv = f_chop(v, displayLength, scale);
		if(dd < vv) return -1;
		else if(dd > vv) return 1;
		else return 0;
	}

*/
	
	@Override
	public Group set(int val) {
		attr &= ~(OVERFLOW);

		String s;
		if((attr & (COMP)) != 0) {
			if((attr & (UNSIGNED)) != 0)
				val = Math.abs(val);
			
			if(scale > 0) {
				long v = val * powerLong[scale];
				storeComp(v);
			} else {
				storeComp(val);
			}
			return this;
		} else if((attr & (PICTURE)) != 0) {
			if((attr & NUMERIC) != 0)
				return _setNumeric(Integer.toString(val));
			return _setCharType(Integer.toString(val));
		} else if((attr & (NUMERIC)) != 0) {
			if((attr & (DOUBLE)) != 0) {
				s = doubleToLincString(val, true);  //sjr ADDED
			}
			else {
				if(exp == 0) exp = (byte) displayLength;
				long lv = val % (long)Math.pow(10, exp);
				if (lv != val)
					attr |= OVERFLOW;
				s = longToLincString(lv, true);
			}
		} else {
			s = Integer.toString(val);
		}
		setRealSubstring(getBytesFast(s), myCachedOffset, byteLength);
		return this;
	}
/*	public Group set(Integer val) {
		if((attr & (NUMERIC)) != 0) {
			setGroupSubstring(longToLincString(val.longValue(), true));
		} else
			setGroupSubstring(Integer.toString(val));
		return this;
	}*/
	
	
	/**
	 * Convert my bytes to a long value. This will not work on COMP fields.
	 * @return
	 */
	public long convertMyBytesToLong() {
		long l = ((GroupData)value).convertBytesToLong(myCachedOffset, byteLength, byteLength, scale, 0);
		return l;
	}
	
	/**
	 * Convert my bytes to a long value. If the field length is longer than len, truncate.
	 * If dtype is numeric, truncate from left else truncate from right. 
	 * This will not work on COMP fields.
	 * @return
	 */
	public long convertMyBytesToLong(int maxLen, int dtype) {
		long l = ((GroupData)value).convertBytesToLong(myCachedOffset, byteLength, maxLen, scale, dtype);
		return l;
	}
	
	@Override
	public double getDouble() {
		if((attr & (COMP)) != 0) 
			return getCompAsDouble();
		
//		double d = lincStrToDouble(getGroupSubstring(), true);
		double d = ((GroupData)value).convertBytesToDouble(myCachedOffset, byteLength, displayLength, scale, scale);
		return d;
	}
	@Override
	public double getDouble(int len, int decimals) { 
		String s;
		if((attr & (COMP)) != 0) {
			long lv = getCompAsLong();
			lv %= Math.pow(10, len);
			double d = lv / Math.pow(10, decimals);
			return d;
		}
		try {
			s = new String(getGroupSubstring(), charset);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return 0.0;
		}
		double d = lincStrToDouble(s, len, decimals, true);
		return d;
	}
	
	public float getFloat() { 
		if((attr & (COMP)) != 0) {
			return (float)getDouble();
		}
		try {
			return (float)lincStrToDouble(new String(getGroupSubstring(), charset));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return 0;
		}
	}
	
	
	
	public float getFloat(int len, int decimals) {
		if((attr & (COMP)) != 0) 
			return (float)getDouble(len, decimals);
		
		String s =  getGroupSubstringAsString();
		return (float)lincStrToDouble(s, len, decimals, true);
	}

	@Override
	public long getLong() { 
		if((attr & (COMP)) != 0) {
			return ((GroupData)value).getCompAsLong((attr & UNSIGNED) != 0, myCachedOffset, byteLength);
		}
		long lv;
		try {
			lv = ((GroupData)value).convertBytesToLong(myCachedOffset, byteLength, byteLength, scale, attr);
		} catch(NumberFormatException e) {
			lv = lincStrToLong(getGroupSubstringAsString());
		}
		return lv;
	} 
	
	@Override
	public long getLong(int len) {
		if((attr & (COMP)) != 0) {
			return ((GroupData)value).getCompAsLong((attr & UNSIGNED) != 0, myCachedOffset, byteLength);
		}
		long lv;
		try {
			lv = ((GroupData)value).convertBytesToLong(myCachedOffset, byteLength, len, scale, attr);
		} catch(NumberFormatException e) {
			lv = lincStrToLong(getGroupSubstringAsString(), len, attr);
		}
		return lv;
	}
	
	@Override
	public int getInt() { 
		if((attr & (COMP)) != 0) {
//			return getCompAsInt();
			if(byteLength > 4) {
				long l =  ((GroupData)value).getCompAsLong((attr & UNSIGNED) != 0, myCachedOffset, byteLength);
				if(scale > 0) {
					l /= powerLong[scale];
				}
				return (int)l;
			} else {
				int i = ((GroupData)value).getCompAsInt((attr & UNSIGNED) != 0, myCachedOffset, byteLength);
				if(scale > 0) {
					i /= powerInt[scale];
				}
				return i;
			}
		}
		long lv;
		try {
			lv = convertMyBytesToLong();
		} catch(NumberFormatException e) {
			lv = lincStrToLong(getGroupSubstringAsString());
		}
		return (int)lv;
	} 
	
	public int getInt(int len) {
		if((attr & (COMP)) != 0) {
//			int lv = getCompAsInt();
			int lv = ((GroupData)value).getCompAsInt((attr & UNSIGNED) != 0, myCachedOffset, byteLength);
//			lv %= Math.pow(10, len);
			return lv;
		}
		long lv;
		try {
			lv = convertMyBytesToLong(len, attr);
		} catch(NumberFormatException e) {
			lv = lincStrToLong(getGroupSubstringAsString(), len, attr);
		}
		return (int)lv;
	}
	
	/**
	 * This is used to convert a Group
	 * item which is NOT a COMP to an integer as if it were a COMP.
	 */
	public int getIntAsComp() {
		int l;
		if(byteLength > 4) l = 4;
		else l = byteLength;
		return ((GroupData)value).getCompAsInt((attr & UNSIGNED) != 0, myCachedOffset, l);

	}

	/**
	 * Get part of a Var 
	 * 
	 * @param delim
	 *            delimited
	 * @param lit
	 *            String to be counted
	 * @return Var up to delimiter
	 */
	@Override
	public String getDelim(String delim) {
//		System.out.println("Group getDelim(String delim): ["+delim+"]");
		byte [] b;
		if(delim.length() == 0) {
			b = new byte[1];
			b[0] = (byte)' ';
		} else {
//			b = (byte)delim.charAt(0);
			b = getBytesFast(delim);
		}
		return getDelim(b);
	}
	
	/**
	 * Get part of a Var 
	 * 
	 * @param delim
	 *            delimited
	 * @param lit
	 *            String to be counted
	 * @return Var up to delimiter
	 */
	@Override
	public String getDelim(Var delim) {
//		System.out.println("Group getDelim(Var delim): ["+delim.getString()+"]");
		byte b = delim.getFirstByte();
		return(getDelim(b));
	}

	public String getDelim(byte delim) {
		byte [] d = ((GroupData)value).getByteArray();
		int offset = getMyByteArrayOffset();
		int p = byteLength;
		for(int i=0; i<byteLength; i++) {
			if(d[offset + i] == delim) {
				p = i;
				break;
			}
		}
		String s = new String(d, offset, p);
		return s;
	}

	public String getDelim(byte [] delim) {
		byte [] d = ((GroupData)value).getByteArray();
		int offset = getMyByteArrayOffset();
		
		loop:
		for(int i=0; i<byteLength; i++) {
			if(d[offset + i] != delim[0]) {
				continue;
			}
			int pos = i;
			for(int j=1; j<delim.length;j++) {
				if(d[offset + i + j] != delim[j]) {
					continue loop;
				}
			}
			return new String(d, offset, pos);
		}
		return new String(d, offset, byteLength);
	}
	
	@Override
	public void setSubstr(String src, int start) {
		try {
			setSubstr(src.getBytes(charset), start);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Changes the sub string from start with src.
	 * 
	 * @param src
	 *            the byte array to use as the replacement
	 * @param start
	 *            the location at which to replace it. start is 1 relative.
	 *            If start < 1, start will be set to 1.
	 */
	public void setSubstr(byte [] src, int start) { 
		if(src.length < 1 || start > byteLength)
			return;

		attr |= ISASSIGNED;
		
		start--;
		if(start < 0) start = 0;
		if((src.length + start) > byteLength) {
			int offset = getRealOffset(0);
			((GroupData)value).setSubString(src, offset + start, offset + byteLength);
		} else {
			((GroupData)value).setSubString(src, getRealOffset(0) + start);
		}
	}
	
	@Override
	public int size() { return byteLength; }
	
	public void printstuff() {
		if(log.isDebugEnabled()) {
			log.debug("offset="+groupOffset+" len="+displayLength);
			log.debug("    ["+ this.getString()+ "]");
		}
	}
	/**
	 * Fills a Var with the same characters repeated.
	 * 
	 * @param s
	 *            the fill string
	 */
	public Group fill(String s) { 
		StringBuilder tmp = new StringBuilder();
		if(s == "") s = " ";
		while(tmp.length() <= displayLength) 
			tmp.append(s);
		return(set(tmp.substring(0,displayLength)));
	}

	public Group bwz() {
		if((attr & (PICTURE)) == 0 && (attr & (NUMERIC)) == 0) {
			log.error("bwz() can only be applied to numeric or picture Group.", new Exception("bwz()"));
			return this;
		}
		if((attr & (PICTURE)) == 0) {
			String nines = "99999999999999999999999999999999";
			String m;
			String d;
			if(scale > 0) {
				m = nines.substring(0,displayLength-scale) + ".";
				d = nines.substring(0, scale);
			} else {
				m = nines.substring(0, displayLength);
				d = "";
			}
			format(m + d);
		}
		attr |= ZEROBLANK;
		return this;
	}
	
	public void replaceAll(String from, String to) {
		int pointer = 0;
		StringBuilder orig = new StringBuilder(getString());
		StringBuilder result = new StringBuilder();
		if(from.equals("")) from =  " ";
		if(to.equals("")) to =  Strings.rpad(" ",from.length(),' ');
		if(from.length() != to.length()) return;
		pointer = orig.indexOf(from);
		while(pointer >= 0) {
			result.append(orig.substring(0,pointer));
			result.append(to);
			result.append(orig.substring(pointer + to.length()));
			//log.debug("result=" + result.toString());
			pointer = orig.indexOf(from,pointer + to.length());
			orig.delete(0, orig.length());
			orig.append(result);
			result.delete(0, result.length());
		}
		if((attr & (PICTURE)) != 0) {
			setGroupSubstring(orig.toString().getBytes());
		} else set(orig.toString());
	}
	
	public String getStringLeftAlign(boolean rz) {
		String res;
		if((attr & (COMP)) != 0)
			return getString();
		
		if(scale > 0) {
			res = "" + getDouble();
			//int lenRes=tmpRes.length();
			//res=tmpRes.substring(0,lenRes - dec) + "." + tmpRes.substring(lenRes - dec);
			if(getDouble() < 0 && getDouble() > -1)
				res="-" + res.substring(2);
			res = res.replaceAll("^0*", "");
			
		} else
			res = getString(false);
		if (!rz && res.equals("0")) return "";
		else return formatThousands(res.trim(),null,true);
	}
	
	//TODO move corresponding
	public void setCorr(Var source) {System.out.println("Warning MOVE CORR used");}
	
	public Group getMember (int index) {
		return members.get(index);
	}
	public int getMemberSize() {
		return members == null ? 0 : members.size();
	}
	public void setMember (int index, long var) {
		members.get(index).set(var);
	}
	public void setMember (int index, int var) {
		members.get(index).set(var);
	}
	public void setMember (int index, String var) {
		members.get(index).set(var);
	}
	public void setMember (int index, Var var) {
		members.get(index).set(var);
	}
	

	/***
	 * Return a byte array, adjusted to the required length.
	 * If the in array is too long, truncate from rhs.
	 * If the in array is too short, pad to the right with spaces.
	 * @param in
	 * @param len
	 * @return
	 */
	private byte [] adjustByteLengthRight(byte [] in, int len) {
		byte [] b = new byte[len];
		int copyLen;
		if(in == null || in.length == 0) {
			copyLen = 0;
		} else {
			copyLen = in.length < len ? in.length : len;
			System.arraycopy(in, 0, b, 0, copyLen);
		}
		
		if(copyLen < len) {
			final byte pad = initialPadChar();
			for(int i=copyLen; i < len; i++) {
				b[i] = pad;
			}
		}
		return b;
	}
	
	
	//If the string is too long, truncate from rhs for CHAR and LHS for number.
	//If the string is too short, pad the left with the pad character.

	/***
	 * Return a byte array, adjusted to the required length.
	 * If the in array is too long, truncate from rhs for char and lhs for number.
	 * If the in array is too short, pad to the left with the pad character.
	 * @param in
	 * @param len
	 * @return
	 */
	private byte [] adjustByteLengthLeft(byte [] in, int len, byte pad) {
		if(in == null)
			in = new byte [0];
		if(in.length == len)
			return in;
		
		if(in.length > len) {
			if((attr & (NUMERIC)) != 0)
				return Util.byteSubstring(in, in.length - len);
			return Util.byteSubstring(in, 0, len);
		} else { //in.length < len
			byte [] b = new byte[len];
			for(int i=0; i<(len - in.length);i++)
				b[i] = pad;
			System.arraycopy(in, 0, b, len - in.length, in.length);
			if(pad == 0) {
				int idx = Util.byteIndexOf(b, '-');
				if(idx != -1) {
					b[idx] = pad;
					overstamp(b);
				}
			}
			return b;
		}
	}

	private void overstamp(byte [] b) {
		b[b.length - 1] = (byte) (b[b.length - 1] - '0' + 'p');
	}
	
	public int getOccurs() {return occurs;}

	public Group getParent() {
		return parent;
	}

	/**
	 * Create a string which can be used to re-create this group.
	 * @param sb
	 */
	public String serialise() {
		StringBuilder sb = new StringBuilder();
		sb.append(attr);
		sb.append(":");
		sb.append(groupOffset);
		sb.append(":");
		if(occurs > 0)
			sb.append(displayLength / occurs);
		else
			sb.append(displayLength);
		sb.append(":");
		sb.append(scale);
		sb.append(":");
		sb.append(occurs);
		sb.append(":");
		sb.append(redefinedLen);
		sb.append(":");
		sb.append(getMemberSize());
		
		return sb.toString();
	}
	
	/**
	 * Create a new group based on serialised data and attach it
	 * to the parent, if any.
	 * @param parent
	 * @param requiredOffset If this offset is greater than the actual offset, create a dummy group to fill.
	 * @param code
	 * @return
	 */
	public static Group deserialise(Group parent, String code) {
		String [] s = code.split(":");
		int at = Integer.parseInt(s[0]);
		int offs = Integer.parseInt(s[1]);
		int dlen = Integer.parseInt(s[2]);
		int sc = Integer.parseInt(s[3]);
		int occ = Integer.parseInt(s[4]);
		int red = Integer.parseInt(s[5]);
		int memb = Integer.parseInt(s[6]);
		
		Group g;
		if(parent == null) {
			if(memb > 0)
				g = new Group();
			else 
				g = new Group(dlen);
			g.attr = at;
		} else {
			if(red > 0) {
				g = parent.redefine();
			} else {
				if(offs > parent.byteLength) {
					//Create a dummy member to fill the vacant space
					parent.addMember(Var.CHAR, offs - parent.byteLength);
				}
				if(memb > 0)
					g = parent.addGroup();
				else
					g = parent.addMember(at, dlen, sc);
			}
			g.attr = at;
		}
		if(occ > 0)
			g.occurs(occ);
		return g;
	}

	/**
	 * Print a 1 line summary of this group information
	 * @return
	 */
	public String printSummary() {
		StringBuilder sb = new StringBuilder();
		sb.append("offset=");
		sb.append(myCachedOffset);
		sb.append(" group-offset=");
		sb.append(groupOffset);
		sb.append(" length(");
		sb.append(displayLength);
		sb.append(",");
		sb.append(byteLength);
		if(scale > 0)
			sb.append("."+scale);
		sb.append(")"); 
		
		sb.append("  - data: len=");
		sb.append(((GroupData)value).getByteArray().length);

		return sb.toString();
	}

	/**
	 * Duplicate all aspects of this group. The new parent of the 
	 * dupped Group must be provided.
	 * @return
	 */
	public Group duplicate(Group parent) {
		Group g = new Group(-1);
		g.attr = attr;
		g.displayLength = displayLength;
		g.byteLength = byteLength;
		g.exp = exp;
		g.groupOffset = groupOffset;
		g.indexLevel = indexLevel;
		g.occurs = occurs;
		g.picture = picture;
//		g.redefined = redefined;
		g.redefinedLen = redefinedLen;
		g.scale = scale;
		
		if(parent == null) {
			g.value = ((GroupData)value).duplicate(g);
		} else {
			//If I have a parent, add me.
			g.parent = parent;
			if(parent.members == null)
				parent.members = new ArrayList<Group>();
			parent.members.add(g);
			g.cacheMyGroupData();
		}
		return g;
	}

	public void setExported(boolean exported) {
		if(exported)
			attr |= Var.EXPORT;
		else
			attr &= ~(Var.EXPORT);
	}
	/**
	 * This creates a Group with the attributes of another.
	 * 
	 */
	public Group clone() {
		return new Group(this.getAttr(), this.getLen(), this.getDec()).set(this);
	}
	public int getAttr() {
		return attr;
	}

	public int getGroupOffset() {
		return groupOffset;
	}

	public Group setAll (Object val) {
		int idxLevel = getIndexLevel();
		if (idxLevel > 0) {
			int [] indexBounds = getIndexBounds();
			int [] idx = new int[idxLevel];
			for(int i=0; i<idx.length; i++) {
				idx[i] = 1;
			}
			int level = indexBounds.length - 1;
			while(idx[0] <= indexBounds[0]) {
				/*
				StringBuilder sb = new StringBuilder();
				sb.append("index(");
				for(int i=0; i<idx.length; i++) {
					if(i > 0) sb.append(", ");
					sb.append(idx[i]);
				}
				sb.append(")");
				System.out.println(sb);
				*/
				if(val instanceof String) 
					index(idx).set((String)val);
				else if(val instanceof Integer) 
					index(idx).set((Integer)val);
				else if(val instanceof Long) 
					index(idx).set((Long)val);
				else if(val instanceof Var) 
					index(idx).set((Var)val);
				else if(val instanceof Double) 
					index(idx).set((Double)val);
				
				for(int i=level; i>=0; i--) {
					idx[i]++;
					if(idx[i] <= indexBounds[i] || i == 0) {
						break;
					}
					idx[i] = 1;
				}
			}
		} else {
			if(val instanceof String) 
				set((String)val);
			else if(val instanceof Integer) 
				set((Integer)val);
			else if(val instanceof Long) 
				set((Long)val);
			else if(val instanceof Var) 
				set((Var)val);
			else if(val instanceof Double) 
				set((Double)val);
		}
		return this;
	}

	/**
	 * Return the value of this group as a hex-encoded string.
	 * @return
	 */
	public String getHexString() {
		String x = ((GroupData)value).getHexString(myCachedOffset, byteLength);
		return x;
	}
	
	@Override
	public byte getFirstByte() {
		return ((GroupData)value).getByteArray()[myCachedOffset];
	}
	
	
	@Override
	public String [][] unstringDelim(String delim, Var ... vlist) {
		String [][] counts = new String[3][vlist.length];
		byte [][] d = new byte[1][0];
		d[0] = getBytesFast(delim);
		unstringDelimFrom(1, counts, d, vlist);
		return counts;
	}
	
	@Override
	public String [][] unstringDelim(String [] delim, Var ... vlist) {
		String [][] counts = new String[3][vlist.length];
		byte [][] d = new byte[delim.length][0];
		for(int i=0; i<delim.length; i++) {
			d[i] = getBytesFast(delim[i]);
		}
		unstringDelimFrom(1, counts, d, vlist);
		return counts;
	}
	
	
	/**
	 * Unstring a var from a pointer 
	 * 
	 * @param ptr
	 *            Position to start from, 1 relative.
	 * @param counts
	 * 			  Array of arrays of strings, used to fill counts. Array must be size [3][vlist.length]
	 * @param delimiter
	 *            Array of delimiters
	 * @param vlist
	 *            Array of targets.
	 * @return .
	 */
	public int unstringDelimFrom(int ptr, String [][] counts, byte [][] delims, Var ... vlist) {
		if(counts.length != 3)
			throw new IllegalArgumentException("'counts' array first dimension must be 3");
		if(counts[0].length != vlist.length)
			throw new IllegalArgumentException("'counts' array second dimension must match vlist.length("+vlist.length+")");
		
		if(delims == null || delims.length < 1) {
			delims = new byte[1][1];
			delims[0][0] = ' ';
		} else {
			for(int i=0; i<delims.length; i++) {
				if(delims[i].length < 1) {
					delims[i] = new byte[1];
					delims[i][0] = ' ';
				}
			}
		}
		
		byte [] data = ((GroupData)value).getByteArray();
		int offset = myCachedOffset;
		int endPos = offset + byteLength;
		
		int pos = offset + ptr - 1;
		int sPos = pos; //Start of the string we're looking at
		int cEntry = 0; //Current entry, wrt clist and counts.
		int matches = 0;
		for(; pos < endPos && cEntry < vlist.length; pos++) {
			for(int i=0; i<delims.length ; i++) {
				if(delims[i][0] != data[pos]) {
					continue; //optimise for most common condition.
				}
				byte [] d = delims[i];
				int foundPos = pos; //Start of matching delimeter
				boolean found = true;
				for(int j=1; j<d.length && (pos+j) < endPos; j++) {
					if(data[pos+j] != d[j]) {
						found = false;
						break;
					}
				}
				if(found) { //We have a match
					//sPos points to start of word
					//foundPos points to start of matching delimiter
					pos += d.length;
					//pos points to first byte after matching delimiter
					matches++;
//					String matchedString = new String(data, sPos, foundPos - sPos);
					String matchedDelim = new String(d);
					vlist[cEntry].set(data, sPos, foundPos - sPos);
					counts[0][cEntry] = Integer.toString(foundPos - sPos);
					counts[1][cEntry] = matchedDelim;
					cEntry++;
					sPos = pos--;
					break;
				}
			}
		}
		//Chuck in any remaining text into any remaining vlist vars, if any.
		if(sPos <= endPos && cEntry < vlist.length) {
			matches++;
			vlist[cEntry].set(data, sPos, endPos - sPos);
			counts[0][cEntry] = Integer.toString(endPos - sPos);
			counts[1][cEntry] = "";
			pos = endPos;
			cEntry++;
		}
		
		for(; cEntry < vlist.length; cEntry++) {
			counts[0][cEntry] = "0";
			counts[1][cEntry] = "";
		}
		
		
		counts[2][0] = Integer.toString(matches);
		return pos + 1;
	}

	/**
	 * Get the GroupByteArray object undelying this group.
	 * If the Group is not the primary group, return null;
	 * @return Underlying GroupByteArray object, or null if not primary group.
	 */
	public GroupByteArray getGroupByteArray() {
		if(value == null)
			return null;
		return ((GroupData)value).byteArray;
	} 

	public Group numval() {
		try {
			if( eq(" "))
				return new Group(0);
			else {
				String s = lrtrim();
				char last=s.charAt(s.length() - 1) ;
				if(last == '-' || last == '+' ) {
					s=last + s.substring(0, s.length() - 1);
				}
				int len = s.length();
				int decs = 0;
				int dot = s.indexOf('.');
				if(dot != -1) {
					if(dot == 0) {
						decs = len - dot - 1;
					} else {
						len--;
						decs=len - dot;
					}
					Group ret = new Group (Var.NUMERIC,len,decs);
					ret.set(Double.valueOf(s));
					return ret;
				} else {
					Group ret = new Group (Var.NUMERIC,len);
					ret.set(Long.valueOf(s));
					return ret;
				}
			}
		} catch (NumberFormatException e) {
			return new Group(0);
		}
	}

	/**
	 * This method is called whenever the underliying GroupData object is uncreased.
	 * Only the primary group is called.
	 */
	protected void checkByteLength() {
		if(parent == null) {
			GroupData gd = (GroupData)value;
			if(gd.length() > byteLength) {
				byteLength = gd.length();
				displayLength = byteLength;
			}
		} else {
			throw new IllegalStateException("checkByteLength called on non-primary Group.");
		}
	}

	/**
	 * Sets the byte array of this group to the one used by 'group'
	 * @param group
	 * @return Returns the current GroupData item.
	 */
	public GroupByteArray swapByteArray(Group grp, boolean swapBytes) {
//		set(grp); if(true) return null;
		
//		swapBytes = false;
		GroupData gd = (GroupData) value;
		GroupData ngd = (GroupData) grp.value;
		
		if(ngd.byteArray == null) {
			//param already got swapped out
			//The target variable must still get the data so take it from the saved backing store.
			System.out.println("Array already swapped");
			if(((GroupData)grp.value).swappedByteArray != null) { 
				if(byteLength > grp.byteLength) {
					System.arraycopy(((GroupData)grp.value).swappedByteArray.data, grp.myCachedOffset, ((GroupData)value).byteArray.data, myCachedOffset, grp.byteLength);
					Arrays.fill(((GroupData)value).byteArray.data, myCachedOffset+grp.byteLength, myCachedOffset+byteLength, (byte)' ');
				} else {
					System.arraycopy(((GroupData)grp.value).swappedByteArray.data, grp.myCachedOffset, ((GroupData)value).byteArray.data, myCachedOffset, byteLength);
				}
			}
			return null;
		}
		if(swapBytes && 
				(attr & COMP) == 0 && 
				(grp.attr & COMP) == 0 &&
				byteLength == grp.byteLength && 
				myCachedOffset == grp.myCachedOffset && 
				gd.byteArray.data.length == ngd.byteArray.data.length) {
			ngd.swappedByteArray = gd.byteArray;
			gd.byteArray = ngd.byteArray;
			ngd.byteArray = null;
			return ngd.swappedByteArray;
		}
		if(byteLength > grp.byteLength) {
			System.arraycopy(((GroupData)grp.value).byteArray.data, grp.myCachedOffset, ((GroupData)value).byteArray.data, myCachedOffset, grp.byteLength);
			Arrays.fill(((GroupData)value).byteArray.data, myCachedOffset+grp.byteLength, myCachedOffset+byteLength, (byte)' ');
		} else {
			System.arraycopy(((GroupData)grp.value).byteArray.data, grp.myCachedOffset, ((GroupData)value).byteArray.data, myCachedOffset, byteLength);
		}
		return null;
	
	}

	/**
	 * Sets the byte array of this group to 'groupData'
	 * @param group
	 * @return Returns the current GroupData item.
	 */
	public GroupByteArray swapByteArray(GroupByteArray gba, Group grp) {
//		grp.set(this); if(true) return null;		
		if(gba != null) {
			//Set this array to gba
			GroupData gd = (GroupData) value;
			GroupByteArray rval = gd.byteArray;
			gd.byteArray = gba;
			((GroupData)grp.value).swappedByteArray = null;
			((GroupData)grp.value).byteArray = rval;
			return rval;
		}
		
		if(((GroupData)grp.value).byteArray == null) {
			//This value was swapped out by another. Ignore.
			return null;
		}
		
		//Or else set grp to the value in this
		if(grp.byteLength > byteLength) {
			System.arraycopy(((GroupData)this.value).byteArray.data, this.myCachedOffset, ((GroupData)grp.value).byteArray.data, grp.myCachedOffset, this.byteLength);
			Arrays.fill(((GroupData)value).byteArray.data, myCachedOffset+this.byteLength, myCachedOffset+byteLength, (byte)' ');
		} else {
			System.arraycopy(((GroupData)this.value).byteArray.data, this.myCachedOffset, ((GroupData)grp.value).byteArray.data, grp.myCachedOffset, grp.byteLength);
		}
//		grp.notifyCachingGroups();
		return null;
	}
	
	public GroupByteArray setRawBytes(Group grp) {
		if(byteLength > grp.byteLength) {
			System.arraycopy(((GroupData)grp.value).byteArray.data, grp.myCachedOffset, ((GroupData)value).byteArray.data, myCachedOffset, grp.byteLength);
			Arrays.fill(((GroupData)value).byteArray.data, myCachedOffset+grp.byteLength, myCachedOffset+byteLength, (byte)' ');
		} else {
			System.arraycopy(((GroupData)grp.value).byteArray.data, grp.myCachedOffset, ((GroupData)value).byteArray.data, myCachedOffset, byteLength);
		}

		return null;
		
//		if(dst.length > src.length) {
//			System.arraycopy(src, 0, dst, 0, src.length);
//			Arrays.fill(dst, src.length, dst.length, (byte)' ');
//		} else {
//			System.arraycopy(src, 0, dst, 0, dst.length);
//		}
	}

	/**
	 * Recursively setup all value pointers of all sub-groups to the new value.
	 * @param value
	 */
	/*
	private void setupByteArray(GroupData groupData) {
		value = groupData;
		if(members != null) {
			for(Group g : members) {
				g.setupByteArray(groupData);
			}
		}
	}
*/
	/**
	 * Register the group as being of a caching nature.
	 * @param g
	 *//*
	protected void registerCachingGroups(Group g) {
//		Ensure our parents know of us
		if(cachedGroupRegister == null) {
			cachedGroupRegister = new TreeSet<Group>(new GroupComparator());
		}
		//Don't cache myself
		if(this.hashCode() != g.hashCode()) {
			cachedGroupRegister.add(g);
		}
		if(parent != null) {
			parent.registerCachingGroups(g);
		} else {
			//find all groups (including redfines) which could affect the value of this group (g)
			((GroupData)value).evaluateCacheImpact(g);
		}
		
	}*/

	/**
	 * Check if this Group, including it's children, can affect g's value
	 * @param g
	 */
	/*
	protected void XevaluateCacheImpact(Group g) {
		if(myCachedOffset < (g.myCachedOffset + g.byteLength) && (myCachedOffset + byteLength) > g.myCachedOffset) {
			//Changing this Group will affect the value of g
			cachedGroupRegister.add(g);
			if(members != null) {
				for(Group x : members) {
					x.XevaluateCacheImpact(g);
				}
			}
		}
	}
	
	protected void XnotifyCachingGroups() {
		if(cachedGroupRegister == null) {
			return;
		}
		for(Group g : cachedGroupRegister) {
			g.XsetCacheDirty();
		}
	}

	//For non caching groups, do nothing.
	protected void XsetCacheDirty() {
	}
	*/
/*
	public Group watchMe() {
		return this;
	}
	*/
}

package com.mssint.jclib;

import java.io.File;

public class TestPaths {
	
	public static String parentOf(String path) {
		File f = new File(path);
		String s = f.getParent();
		if(s == null)
			return "." + File.separator;
		return s;
	}

	public static String baseName(String path) {
		File f = new File(path);
		return f.getName();
	}
	
	public static String getFileName(String ... parts) {
		StringBuilder sb = new StringBuilder();
		for(String p : parts) {
			p = p.trim();
			if(File.separatorChar == '\\') {
				p = p.replaceAll("/", "\\\\");
			} else {
				p = p.replaceAll("\\\\", "/");
			}
			if(sb.length() == 0) {
				sb.append(p);
			} else if(new File(p).isAbsolute() ||
					(File.separatorChar == '\\' && p.length() > 1 && p.charAt(1) == ':')) {
				sb.delete(0, sb.length());
				sb.append(p);
			} else {
				if(sb.charAt(sb.length()-1) != File.separatorChar) {
					sb.append(File.separator);
				}
				if(p.charAt(0) == File.separatorChar) {
					sb.append(p.substring(1));
				} else {
					sb.append(p);
				}
			}
		}
		
		return sb.toString();
	}
	

	public static void main(String [] args) {
		String [] ax = {
				".",
				"/",
				"c:\\usr\\bin",
				"c:\\usr\\bin\\",
				"c:/usr/bin",
				"\\\\azuer\\usr\\bin"
		};
		String [] ay = {
				"apple",
				"/usr/bin/apple",
				"C:/usr/bin/apple",
				"c:\\usr\\bin",
				"c:",
				"\\\\drive\\us\\bin"
		};

		for(String x : ax) {
			for(String y : ay) {
				String s = getFileName(x, y);
				String parent = parentOf(s);
				String base = baseName(s);
				System.out.printf("%s  %s: [%s] parent=%s base=%s\n", x, y, s, parent, base);
			}
		}
	}

}

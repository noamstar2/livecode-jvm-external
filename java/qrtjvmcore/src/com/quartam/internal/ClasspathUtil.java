/* Copyright (C) 2008-2013 Quartam Software / Jan Schenkel.
 
 This file is part of the [qrtjvm] project, also known as
 Quartam JavaVirtualMachine External for LiveCode.
 
 [qrtjvm] is free software; you can redistribute it and/or modify it under
 the terms of the GNU General Public License v3 as published by the Free
 Software Foundation.
 
 [qrtjvm] is distributed in the hope that it will be useful, but WITHOUT ANY
 WARRANTY; without even the implied warranty of MERCHANTABILITY or
 FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 for more details.
 
 You should have received a copy of the GNU General Public License
 along with [qrtjvm].  If not see <http://www.gnu.org/licenses/>.  */

package com.quartam.internal;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class ClasspathUtil {

	private static final Class<?>[] PARAMETERS = new Class[]{URL.class};

	public static void addFile(String s) throws IOException {
		File f = new File(s);
		addFile(f);
	}
	public static void addFile(File f) throws IOException {
		URL toURL = f.toURL();
		addURL(toURL);
	}
	public static void addURL(URL u) throws IOException {
		URLClassLoader sysloader = (URLClassLoader)ClassLoader.getSystemClassLoader();
		for(URL su:sysloader.getURLs()) {
			if (su.equals(u)) {
				throw new IOException("Error, URL already added to system classloader");
			}
		}
		Class<?> sysclass = URLClassLoader.class;
		try {
			Method method = sysclass.getDeclaredMethod("addURL", PARAMETERS);
			method.setAccessible(true);
			method.invoke(sysloader, new Object[]{u});
		} catch (Throwable t) {
			t.printStackTrace();
			throw new IOException("Error, could not add URL to system classloader");
		}
		
	}
	public static boolean isFileLoaded(String s) {
		File f = new File(s);
		return isFileLoaded(f);
	}
	public static boolean isFileLoaded(File f) {
		URL toURL = null;
		try {
			toURL = f.toURL();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		if (toURL != null) {
			return isURLLoaded(toURL);
		} else {
			return false;
		}
	}
	public static boolean isURLLoaded(URL u) {
		URLClassLoader sysloader = (URLClassLoader)ClassLoader.getSystemClassLoader();
		for (URL su:sysloader.getURLs()) {
			if (su.equals(u)) {
				return true;
			}
		}
		return false;
	}
}

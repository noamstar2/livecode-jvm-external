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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.quartam.external.ExternalAlias;
import com.quartam.external.ExternalCommand;
import com.quartam.external.ExternalException;
import com.quartam.external.ExternalFunction;
import com.quartam.external.ExternalInterface;
import com.quartam.external.ExternalPackage;
import com.quartam.external.ExternalPackageDispose;
import com.quartam.external.ExternalPackageInit;


/**
 * The abstract class <code>ExternalLoader</code> encapsulates all the underlying
 * logic to load the external libraries.
 * <p>
 * Use the subclass <code>TestHost</code> for the purpose of testing your externals.
 * <p>
 * Do <b>not</b> try to use the <code>ExternalHost</code> subclass as it uses native
 * JNI methods to interact with the Revolution external interface, which will not be
 * available when testing inside your favorite Java IDE. 
 * 
 * @author  Jan Schenkel
 * @version v1.0.0
 * @since   1.0
 *
 */
public abstract class ExternalLoader {

	// STATIC METHODS
	
	/**
	 * Returns the stack trace of an exception as a String
	 * 
	 * @param t the <code>Throwable</code> whose stack trace to obtain
	 * @return a <code>String</code> containing the text of the exception
	 */
	public static String getExceptionText(Throwable t) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		return sw.toString();
	}

	// PRIVATE FIELDS AND CONSTRUCTOR
	
	private List<XLibrary> xLibraryList;
	private Map<String,XLibrary> xLibraryFiles;
	private Map<String,XLibrary> xLibraryMap;
	private Map<String,XPackage> xPackageMap;
	private Map<String,XCommand> xCommandMap;
	private Map<String,XFunction> xFunctionMap;
	
	/**
	 * Public constructor to setup the instance variables
	 */
	public ExternalLoader() {
		super();
		this.xLibraryList = new ArrayList<XLibrary>();
		this.xLibraryFiles = new HashMap<String,XLibrary>();
		this.xLibraryMap = new HashMap<String,XLibrary>();
		this.xPackageMap = new HashMap<String,XPackage>();
		this.xCommandMap = new HashMap<String,XCommand>();
		this.xFunctionMap = new HashMap<String,XFunction>();
	}
	
	// INSTANCE METHODS
	
	/**
	 * Loads a .jar file as an external library
	 * 
	 * @param jarPath the path to the .jar library to load
	 * @throws IllegalArgumentException if the library file is not a valid .jar file with xlibrary.xml descriptor entry 
	 */
	public void loadExternalLibrary(String jarPath) {
		
		if (xLibraryFiles.containsKey(jarPath)) {
			// ignore
			return;
		}
		
		File jarFile = new File(jarPath);
		if (!jarFile.exists()) {
			throw new IllegalArgumentException("The <library path> must refer to an existing .jar file");
		}
		
		String jarFileName = jarFile.getName();
		if (xLibraryMap.containsKey(jarFileName)) {
			throw new IllegalArgumentException("A library with the same name was already loaded from a different path");
		}
		
		ZipFile jarZipFile = null;
		try {
			jarZipFile = new ZipFile(jarFile);
		} catch (ZipException e) {
			// ignore
		} catch (IOException e) {
			// ignore
		}
		if (jarZipFile == null) {
			throw new IllegalArgumentException("The <library path> must refer to a valid .jar file");
		}
		
		try {
			XLibrary xLibrary = loadXLibrary(jarFileName, jarFile.getAbsolutePath(), jarZipFile);
			xLibraryList.add(xLibrary);
			xLibraryFiles.put(xLibrary.getJarPath(), xLibrary);
			xLibraryMap.put(xLibrary.getLibraryName(), xLibrary);
			rebuildXMaps();
		} catch (Exception e) {
			throw new IllegalArgumentException(
					"The <library path> must refer to a valid .jar file with xlibrary descriptor" +
					"\n" + e.getMessage());
		}
		
	}
	
	/**
	 * Unloads an external library
	 * 
	 * @param libraryNameOrJarPath the name or the path of the .jar library to unload
	 * @throws IllegalArgumentException if the specified library was not loaded
	 */
	public void unloadExternalLibrary(String libraryNameOrJarPath) {
		XLibrary xLibrary = null;
		xLibrary = xLibraryFiles.get(libraryNameOrJarPath);
		if (xLibrary == null) {
			xLibrary = xLibraryMap.get(libraryNameOrJarPath);
		}
		if (xLibrary == null) {
			throw new IllegalArgumentException("There is no library '" + libraryNameOrJarPath + "' loaded");
		}
		xLibraryList.remove(xLibrary);
		xLibraryMap.remove(xLibrary.getLibraryName());
		xLibraryFiles.remove(xLibrary.getJarPath());
		xLibrary.dispose();
		rebuildXMaps();
	}
	
	private XLibrary loadXLibrary(String libraryName, String jarPath, ZipFile jarZipFile) throws ExternalException {
		ZipEntry jarZipEntry = jarZipFile.getEntry(Constants.DESCRIPTOR_ENTRY_PATH);
		if (jarZipEntry == null) {
			throw new ExternalException("External Library descriptor could not be found.");
		}
		
		Pattern classNamePattern = Pattern.compile(Constants.JAVACLASSNAME_REGEX);
		List<String> packages = new LinkedList<String>();
		Exception errorCause = null;
		String errorDescription = null;
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(jarZipFile.getInputStream(jarZipEntry));
			NodeList xpackageNodes = document.getElementsByTagName("xpackage");
			xpackageNodesLoop:
			for (int i = 0, n = xpackageNodes.getLength(); i < n; i++) {
				Node xpackageNode = xpackageNodes.item(i);
				String packageClassName = xpackageNode.getTextContent();
				if (!classNamePattern.matcher(packageClassName).matches()) {
					errorDescription = "'" + packageClassName + "' is not a valid Java class name";
					break xpackageNodesLoop;
				}
				packages.add(packageClassName);
			}
		} catch (ParserConfigurationException e) {
			errorDescription = "The XML parser could not be configured";
			errorCause = e;
		} catch (SAXException e) {
			errorDescription = "The descriptor file could not be parsed";
			errorCause = e;
		} catch (IOException e) {
			errorDescription = "The descriptor file could not be read";
			errorCause = e;
		}
		if (errorDescription != null) {
			throw new ExternalException(
					"External Library descriptor was invalid" +
					"\n" + errorDescription, errorCause);
		}
		if (packages.size() == 0) {
			throw new ExternalException("External Library contains no external packages", null);
		}
		
		URL jarFileURL = null;
		try {
			jarFileURL = new URL("jar:file:" + jarPath + "!/");
		} catch (MalformedURLException e) {
			// Should not happen (I know... Famous last words...)
		}
		URL[] classLoaderURLs = {jarFileURL};
		ClassLoader loader = URLClassLoader.newInstance(classLoaderURLs);
		
		XLibrary xLibrary = new XLibrary(libraryName, jarPath, loader);
		for (String packageClassName:packages) {
			XPackage xPackage = null;
			errorDescription = null;
			try {
				Class<?> packageClass = loader.loadClass(packageClassName);
				if (packageClass.isAnnotationPresent(ExternalPackage.class)) {
					Object packageObject = packageClass.newInstance();
					xPackage = new XPackage(packageClassName, packageObject);
					Method[] methods = packageClass.getMethods();
					methodsLoop:
					for (Method method:methods) {
						Class<?> returnType = method.getReturnType();
						Class<?>[] parameterTypes = method.getParameterTypes();
						if (method.isAnnotationPresent(ExternalPackageInit.class)) {
							// there can be only one ExternalPackageInit method
							if (xPackage.isInitMethodSet()) {
								errorDescription = "ExternalPackage is not allowed to annotate more than one method as ExternalPackageInit";
								break methodsLoop;
							}
							// the ExternalPackageInit method cannot return anything
							if (!returnType.equals(Void.TYPE)) {
								errorDescription = "ExternalPackageInit method '" + method.getName() + "' has a return type other than 'void'";
								break methodsLoop;
							}
							// the ExternalPackageInit method cannot have parameters
							if ((parameterTypes.length != 1) || ((!parameterTypes[0].equals(ExternalInterface.class)))) {
								errorDescription = "ExternalPackageInit method '" + method.getName() + "' must expect 1 parameter of type ExternalInterface";
								break methodsLoop;
							}
							xPackage.setInitMethod(method);
						} else if (method.isAnnotationPresent(ExternalPackageDispose.class)) {
							// there can be only one ExternalPackageDispose method
							if (xPackage.isDisposeMethodSet()) {
								errorDescription = "ExternalPackage is not allowed to annotate more than one method as ExternalPackageDispose";
								break methodsLoop;
							}
							// the ExternalPackageDispose method cannot return anything
							if (!returnType.equals(Void.TYPE)) {
								errorDescription = "ExternalPackageDispose method '" + method.getName() + "' has a return type other than 'void'";
								break methodsLoop;
							}
							// the ExternalPackageDispose method cannot have parameters
							if (parameterTypes.length > 0) {
								errorDescription = "ExternalPackageDispose method '" + method.getName() + "' is not allowed to expect parameters";
								break methodsLoop;
							}
							xPackage.setDisposeMethod(method);
						} else if (method.isAnnotationPresent(ExternalCommand.class)) {
							String commandName = null;
							if (method.isAnnotationPresent(ExternalAlias.class)) {
								commandName = method.getAnnotation(ExternalAlias.class).value();
							} else {
								commandName = method.getName();
							}
							// an XCMD can return either nothing or a String
							if ((!returnType.equals(Void.TYPE)) && (!returnType.equals(String.class))) {
								errorDescription = "ExternalCommand '" + commandName + "' has a return type other than 'void' or 'String'";
								break methodsLoop;
							}
							ExternalReturnType xReturnType = returnType.equals(Void.TYPE) ? ExternalReturnType.VOID : ExternalReturnType.STRING;
							// an XCMD has either no parameters or a single String[] array
							ExternalParameterType xParameterType = null;
							if (parameterTypes.length == 0) {
								xParameterType = ExternalParameterType.NONE;
							} else if ((parameterTypes.length == 1) && (parameterTypes[0].equals(String[].class))) {
								xParameterType = ExternalParameterType.ARRAY;
							} else {
								errorDescription = "ExternalCommand '" + commandName + "' has invalid parameters";
								break methodsLoop;
							}
							XCommand xCommand = new XCommand(commandName, packageObject, method, xParameterType, xReturnType);
							xPackage.addXCommand(xCommand);
						} else if (method.isAnnotationPresent(ExternalFunction.class)) {
							String functionName = null;
							if (method.isAnnotationPresent(ExternalAlias.class)) {
								functionName = method.getAnnotation(ExternalAlias.class).value();
							} else {
								functionName = method.getName();
							}
							// an XFCN must return a single String
							if (!returnType.equals(String.class)) {
								errorDescription = "ExternalFunction '" + functionName + "' has a return type other than 'String'";
								break methodsLoop;
							}
							// an XFCN has either no parameters or a single String[] array
							ExternalParameterType xParameterType = null;
							if (parameterTypes.length == 0) {
								xParameterType = ExternalParameterType.NONE;
							} else if ((parameterTypes.length == 1) && (parameterTypes[0].equals(String[].class))) {
								xParameterType = ExternalParameterType.ARRAY;
							} else {
								errorDescription = "ExternalFunction '" + functionName + "' has invalid parameters";
								break methodsLoop;
							}
							XFunction xFunction = new XFunction(functionName, packageObject, method, xParameterType);
							xPackage.addXFunction(xFunction);
						}
					}
					// Try and initialise the xPackage
					xPackage.init(getExternalInterface());
				} else {
					errorDescription =  "class '" + packageClassName + "' is not annotated as an ExternalPackage";
				}
			} catch (ClassNotFoundException e) {
				errorDescription =  "class '" + packageClassName + "' was not found";
				errorCause = e;
			} catch (InstantiationException e) {
				errorDescription =  "class '" + packageClassName + "' could not be instantiated";
				errorCause = e;
			} catch (IllegalAccessException e) {
				errorDescription =  "class '" + packageClassName + "' could not be accessed";
				errorCause = e;
			} catch (IllegalArgumentException e) {
				errorDescription =  "class '" + packageClassName + "' could not be initialised";
				errorCause = e;
			} catch (InvocationTargetException e) {
				errorDescription =  "class '" + packageClassName + "' could not be initialised";
				errorCause = e;
			}
			if (errorDescription != null) {
				throw new ExternalException(
						"External Package '" + packageClassName + "' could not be loaded" + 
						"\n" + errorDescription, errorCause);
			} else if (xPackage != null){
				xLibrary.addXPackage(xPackage);
			}
		}
		return xLibrary;
		
	}

	protected abstract ExternalInterface getExternalInterface();
	
	/**
	 * This internal method rebuilds the XMaps used for quick dispatch of XCalls.
	 * Note that these maps effectively ensure that when two external commands or
	 * functions are loaded with the same name, the last loaded is the one that
	 * is made available to the developer.  
	 */
	private void rebuildXMaps() {
		xPackageMap = new HashMap<String,XPackage>();
		xCommandMap = new HashMap<String,XCommand>();
		xFunctionMap = new HashMap<String,XFunction>();
		for (XLibrary xLibrary:xLibraryList) {
			for (XPackage xPackage:xLibrary.getXPackages()) {
				xPackageMap.put(xPackage.getPackageName(), xPackage);
				for (XCommand xCommand:xPackage.getXCommands()) {
					xCommandMap.put(xCommand.getCommandName(), xCommand);
				}
				for (XFunction xFunction:xPackage.getXFunctions()) {
					xFunctionMap.put(xFunction.getFunctionName(), xFunction);
				}
			}
		}
	}

	
	/**
	 * Returns the list of loaded external libraries
	 * 
	 * This is called from the JVM External through JNI.
	 * 
	 * @return the list of external libraries
	 */
	public String getExternalLibraries() {
		StringBuilder result = new StringBuilder();
		for (String xLibraryName:xLibraryMap.keySet()) {
			result.append(xLibraryName);
			result.append('\n');
		}
		if (result.length() > 0) {
			result.deleteCharAt(result.length() - 1);
		}
		return result.toString();
	}
	
	/**
	 * Returns the list of available external packages
	 * 
	 * This is called from the JVM External through JNI.
	 * 
	 * @return the list of external packages
	 */
	public String getExternalPackages() {
		StringBuilder result = new StringBuilder();
		for (String xPackageName:xPackageMap.keySet()) {
			result.append(xPackageName);
			result.append('\n');
		}
		if (result.length() > 0) {
			result.deleteCharAt(result.length() - 1);
		}
		return result.toString();
	}
	
	/**
	 * Returns the list of available external commands
	 * 
	 * This is called from the JVM External through JNI.
	 * 
	 * @return the list of external commands
	 */
	public String getExternalCommands() {
		StringBuilder result = new StringBuilder();
		for (String xCommandName:xCommandMap.keySet()) {
			result.append(xCommandName);
			result.append('\n');
		}
		if (result.length() > 0) {
			result.deleteCharAt(result.length() - 1);
		}
		return result.toString();
	}
	
	/**
	 * Returns the list of available external functions
	 * 
	 * This is called from the JVM External through JNI.
	 * 
	 * @return the list of external functions
	 */
	public String getExternalFunctions() {
		StringBuilder result = new StringBuilder();
		for (String xFunctionName:xFunctionMap.keySet()) {
			result.append(xFunctionName);
			result.append('\n');
		}
		if (result.length() > 0) {
			result.deleteCharAt(result.length() - 1);
		}
		return result.toString();
	}
	
	// EXTERNAL COMMAND/FUNCTION INVOCATION
	
	public String callExternalCommand(String commandName, String[] parameters) throws ExternalException {
		String result = "";
		XCommand xCommand = xCommandMap.get(commandName);
		if (xCommand == null) {
			throw new ExternalException("ExternalCommand '" + commandName + "' is unknown");
		}
		result = xCommand.invoke(parameters);
		return result;
	}
	
	public String callExternalFunction(String functionName, String[] parameters) throws ExternalException {
		String result = "";
		XFunction xFunction = xFunctionMap.get(functionName);
		if (xFunction == null) {
			throw new ExternalException("ExternalFunction '" + functionName + "' is unknown");
		}
		result = xFunction.invoke(parameters);
		return result;
	}
	// PRIVATE CLASSES
	
	private static class XLibrary {
		private String libraryName;
		private String jarPath;
		private ClassLoader classLoader;
		private List<XPackage> xPackages;
		public XLibrary(String libraryName, String jarPath, ClassLoader classLoader) {
			super();
			this.libraryName = libraryName;
			this.jarPath = jarPath;
			this.classLoader = classLoader;
			xPackages = new LinkedList<XPackage>();
		}
		public void dispose() {
			for(XPackage xPackage:xPackages) {
				try {
					xPackage.dispose();
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			// Help the garbage collector along
			xPackages.clear();
			libraryName = null;
			jarPath = null;
			classLoader = null;
			xPackages = null;
		}
		public String getLibraryName() {
			return this.libraryName;
		}
		public String getJarPath() {
			return this.jarPath;
		}
		public ClassLoader getClassLoader() {
			return this.classLoader;
		}
		public List<XPackage> getXPackages() {
			return Collections.unmodifiableList(this.xPackages);
		}
		public void addXPackage(XPackage xPackage) {
			xPackages.add(xPackage);
		}
	}
	
	private static class XPackage {
		private String packageName;
		private Object xObject;
		private Method initMethod;
		private Method disposeMethod;
		private List<XCommand> xCommands;
		private List<XFunction> xFunctions;
		public XPackage(String packageName, Object object) {
			super();
			this.packageName = packageName;
			this.xObject = object;
			xCommands = new ArrayList<XCommand>();
			xFunctions = new ArrayList<XFunction>();
			initMethod = null;
			disposeMethod = null;
		}
		public String getPackageName() {
			return this.packageName;
		}
		public Object getXObject() {
			return this.xObject;
		}
		public List<XCommand> getXCommands() {
			return Collections.unmodifiableList(this.xCommands);
		}
		public List<XFunction> getXFunctions() {
			return Collections.unmodifiableList(this.xFunctions);
		}
		public void setInitMethod(Method initMethod) {
			this.initMethod = initMethod;
		}
		public void setDisposeMethod(Method disposeMethod) {
			this.disposeMethod = disposeMethod;
		}
		public void addXCommand(XCommand xCommand){
			xCommands.add(xCommand);
		}
		public void addXFunction(XFunction xFunction){
			xFunctions.add(xFunction);
		}
		public boolean isInitMethodSet() {
			return (initMethod != null);
		}
		public boolean isDisposeMethodSet() {
			return (disposeMethod != null);
		}
		public void init(ExternalInterface xInterface) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
			if (initMethod != null) {
				initMethod.invoke(xObject, xInterface);
			}
		}
		public void dispose() throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
			if (disposeMethod != null) {
				disposeMethod.invoke(xObject);
			}
			for (XCommand xCommand:xCommands) {
				xCommand.dispose();
			}
			for (XFunction xFunction:xFunctions) {
				xFunction.dispose();
			}
			// Help the garbage collector along
			xCommands.clear();
			xFunctions.clear();
			packageName = null;
			xObject = null;
			xCommands = null;
			xFunctions = null;
			initMethod = null;
			disposeMethod = null;
		}
	}
	
	private static class XCommand {
		private String commandName;
		private Object commandObject;
		private Method commandMethod;
		private ExternalParameterType parameterType;
		private ExternalReturnType returnType;
		public XCommand(String commandName, Object commandObject, Method commandMethod, ExternalParameterType parameterType, ExternalReturnType returnType) {
			super();
			this.commandName = commandName;
			this.commandObject = commandObject;
			this.commandMethod = commandMethod;
			this.parameterType = parameterType;
			this.returnType = returnType;
		}
		public String getCommandName() {
			return this.commandName;
		}
		public Object getCommandObject() {
			return this.commandObject;
		}
		public Method getCommandMethod() {
			return this.commandMethod;
		}
		public ExternalParameterType getParameterType() {
			return this.parameterType;
		}
		public String invoke(String[] arguments) throws ExternalException {
			String result = "";  // default result is an empty string
			String errorDescription = null;
			try {
				switch (returnType) {
				case VOID:
					switch (parameterType) {
					case NONE:
						commandMethod.invoke(commandObject);
						break;
					case ARRAY:
						Class<?> cls = Class.forName("java.lang.String");
						Object arr = Array.newInstance(cls, arguments.length);
						for(int index = 0, count = arguments.length; index < count; index++) {
							Array.set(arr, index, arguments[index]);
						}
						commandMethod.invoke(commandObject, arr);
					}
					break;
				case STRING:
					switch (parameterType) {
					case NONE:
						result = (String) commandMethod.invoke(commandObject);
						break;
					case ARRAY:
						Class<?> cls = Class.forName("java.lang.String");
						Object arr = Array.newInstance(cls, arguments.length);
						for(int index = 0, count = arguments.length; index < count; index++) {
							Array.set(arr, index, arguments[index]);
						}
						result = (String) commandMethod.invoke(commandObject, arr);
					}
					break;
				}
			} catch (IllegalArgumentException e) {
				errorDescription = getExceptionText(e);
			} catch (IllegalAccessException e) {
				errorDescription = getExceptionText(e);
			} catch (InvocationTargetException e) {
				errorDescription = getExceptionText(e);
			} catch (ClassNotFoundException e) {
				errorDescription = getExceptionText(e);
			} finally {
				if (errorDescription != null) {
					throw new ExternalException(
							"ExternalCommand '" + commandName + "' encountered an exception" +
							"\n" + errorDescription);
				}
			}
			return result;
		}
		public void dispose() {
			commandName = null;
			commandObject = null;
			commandMethod = null;
			parameterType = null;
			returnType = null;
		}
	}
	
	private static class XFunction {
		private String functionName;
		private Object functionObject;
		private Method functionMethod;
		private ExternalParameterType parameterType;
		public XFunction(String functionName, Object functionObject, Method functionMethod, ExternalParameterType parameterType) {
			super();
			this.functionName = functionName;
			this.functionObject = functionObject;
			this.functionMethod = functionMethod;
			this.parameterType = parameterType;
		}
		public String getFunctionName() {
			return this.functionName;
		}
		public Object getFunctionObject() {
			return this.functionObject;
		}
		public Method getFunctionMethod() {
			return this.functionMethod;
		}
		public ExternalParameterType getParameterType() {
			return this.parameterType;
		}
		public String invoke(String[] arguments) throws ExternalException {
			String result = "";  // default result is an empty string
			String errorDescription = null;
			try {
				switch (parameterType) {
				case NONE:
					result = (String) functionMethod.invoke(functionObject);
					break;
				case ARRAY:
					Class<?> cls = Class.forName("java.lang.String");
					Object arr = Array.newInstance(cls, arguments.length);
					for(int index = 0, count = arguments.length; index < count; index++) {
						Array.set(arr, index, arguments[index]);
					}
					result = (String) functionMethod.invoke(functionObject, arr);
				}
			} catch (IllegalArgumentException e) {
				errorDescription = getExceptionText(e);
			} catch (IllegalAccessException e) {
				errorDescription = getExceptionText(e);
			} catch (InvocationTargetException e) {
				errorDescription = getExceptionText(e);
			} catch (ClassNotFoundException e) {
				errorDescription = getExceptionText(e);
			} finally {
				if (errorDescription != null) {
					throw new ExternalException(
							"ExternalFunction '" + functionName + "' encountered an exception" +
							"\n" + errorDescription);
				}
			}
			return result;
		}
		public void dispose() {
			functionName = null;
			functionObject = null;
			functionMethod = null;
			parameterType = null;
		}
	}
	
	private enum ExternalReturnType {
		VOID, STRING
	}
	private enum ExternalParameterType {
		NONE, ARRAY
	}
	

	private static class Constants {
		public static final String DESCRIPTOR_ENTRY_PATH = "META-INF/xlibrary.xml";
		public static final String JAVACLASSNAME_REGEX = "^[a-zA-Z]([a-zA-Z0-9])*([\\.][a-zA-Z]([a-zA-Z0-9])*)*$";
	}
	

}

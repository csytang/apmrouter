/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2007, Helios Development Group and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org. 
 *
 */
package org.helios.apmrouter.byteman.sockets.impl;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketImpl;
import java.net.SocketImplFactory;
import java.net.SocketOptions;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javassist.ByteArrayClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.Modifier;
import javassist.NotFoundException;

import org.helios.apmrouter.jmx.ConfigurationHelper;
import org.helios.apmrouter.util.SimpleLogger;

/**
 * <p>Title: SocketImplTransformer</p>
 * <p>Description: Transforms {@link SocketImpl} classes</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.byteman.sockets.impl.SocketImplTransformer</code></p>
 * TODO:  Sysprops and impls for:
 * 		logging
 *      class writes
 */

public class SocketImplTransformer implements ClassFileTransformer {
	/** The class name of {@link SocketImpl} */
	public static final String SOCK_NAME = "java.net.SocketImpl";
	/** The class name of {@link Socket} */
	public static final String SOCKET_NAME = "java.net.Socket";
	/** The binary class name of {@link Socket} */
	public static final String SOCKET_BIN_NAME = "java/net/Socket";
	/** The class name of {@link ServerSocket} */
	public static final String SERVER_SOCKET_NAME = "java.net.ServerSocket";
	/** The binary class name of {@link ServerSocket} */
	public static final String SERVER_SOCKET_BIN_NAME = "java/net/ServerSocket";
	
	/** The class name of SocketOutputStream */
	public static final String SOCKET_OS_NAME = "java.net.SocketOutputStream";
	/** The binary class name of SocketOutputStream */
	public static final String SOCKET_OS_BIN_NAME = "java/net/SocketOutputStream";
	/** The class name of SocketInputStream */
	public static final String SOCKET_IS_NAME = "java.net.SocketInputStream";
	/** The binary class name of SocketInputStream */
	public static final String SOCKET_IS_BIN_NAME = "java/net/SocketInputStream";
	

	
	/** The binary class name of {@link SocketImpl} */
	public static final String SOCK_BIN_NAME = "java/net/SocketImpl";
	/** The class name of {@link SocketImplFactory} */
	public static final String SOCK_FACTORY_NAME = "java.net.SocketImplFactory";
	/** The binary class name of {@link SocketImplFactory} */
	public static final String SOCK_FACTORY_BIN_NAME = "java/net/SocketImplFactory";
	
	/** The system property or env variable to enable verbose logging */
	public static final String VERBOSE_PROP = "org.helios.sockets.verbose";
	/** The system property or env variable to enable instrumented class files writes (the name of the directory) */
	public static final String CLASSDIR_PROP = "org.helios.sockets.classdir";
	/** The verbose logging default value */
	public static final boolean DEFAULT_VERBOSE = true;
	/** The name of the directory to write class files to */
	public static final String DEFAULT_CLASSDIR = System.getProperty("java.io.tmpdir") + "/org/helios";
	
	
//	/** The wrapping socket impl factory */
//	public static final Class<TrackingSocketImplFactory> TrackingSocketImplFactoryClass = TrackingSocketImplFactory.class;
	/** The wrapping socket impl */
	public static final Class<TrackingSocketImpl> TrackingSocketImplClass = TrackingSocketImpl.class; 
	
	/** The publicizing interface to be applied to socketimpls  */
	public static final Class<ISocketImpl> ISocketImplClass = ISocketImpl.class; 

	/** The class name of {@link SocketOptions} */
	public static final String SOCK_OPT_NAME = "java.net.SocketOptions";
	
	static {
//		try {
//			Socket.setSocketImplFactory(new TrackingSocketImplFactory());
//			TrackingSocketImpl.setInstalled();
//		} catch (Exception e) {
//			/* Noop */
//		}
	}
	
	/** The recursion level for the current thread */
	protected static final ThreadLocal<AtomicInteger> recursionLevel = new ThreadLocal<AtomicInteger>() {
		@Override
		protected AtomicInteger initialValue() {			
			return new AtomicInteger();
		}
	};
	/** A thread local containing the transformed method signatures of the current class */
	protected static final ThreadLocal<Set<String>> currentSignatures = new ThreadLocal<Set<String>>();
	
	/** The javassist classpool used by this transformer */
	protected final ClassPool classPool;
	/** The javassist ctclass representation of {@link SocketImpl} */
	protected final CtClass socketImplCtClass;
	/** The javassist ctclass representation of {@link Socket} */
	protected final CtClass socketCtClass;
	/** The javassist ctclass representation of {@link ServerSocket} */
	protected final CtClass serverSocketCtClass;
	
	/** The javassist ctclass representation of {@link SocketImplFactory} */
	protected final CtClass socketImplFactoryCtClass;
	/** The javassist method representation of {@link SocketImplFactory#createSocketImpl} */
	protected final CtMethod createSocketImplMethod;
//	/** The javassist ctclass representation of {@link TrackingSocketImpl} */
//	protected final CtClass trackingSocketImplCtClass;
//	/** The javassist ctclass representation of {@link TrackingSocketImplFactory} */
//	protected final CtClass trackingSocketImplFactoryCtClass;
	
	/** The javassist ctclass representation of {@link ISocketImpl} */
	protected final CtClass iSocketImplCtClass;
	
	
	/** The javassist ctclass representation of {@link SocketOptions} */
	protected final CtClass socketOptionsCtClass;
	/** The javassist ctclass representation of {@link Object} */
	protected final CtClass objectCtClass;
	
	/** The configured verbosity */
	protected final boolean verbose;
	/** The configured class write directory */
	protected final File classDir;
	
	/** A map of post-istrumentation bytecode instrumented classes keyed by the binary class name. */
	protected final Map<String, byte[]> instrumented = new ConcurrentHashMap<String, byte[]>();
	/** A map of pre-istrumentation bytecode instrumented classes keyed by the binary class name. */
	protected final Map<String, byte[]> preInstrumented = new ConcurrentHashMap<String, byte[]>();
	
	/** A map of socket impl class names keyed by the signature of the instrumented method */
	protected final Map<String, String> socketImplSigs = new ConcurrentHashMap<String, String>(); 
	
	
	public void flushInstr() {
		instrumented.clear();
		socketImplSigs.clear();
	}
	/**
	 * Creates a new SocketImplTransformer
	 */
	public SocketImplTransformer() {
		try {
			verbose = ConfigurationHelper.getBooleanSystemThenEnvProperty(VERBOSE_PROP, DEFAULT_VERBOSE);
			String dir = ConfigurationHelper.getSystemThenEnvProperty(CLASSDIR_PROP, DEFAULT_CLASSDIR);
			if(dir==null || dir.trim().isEmpty()) {
				classDir = null;
			} else {
				File tmp = new File(dir.trim());
				if(!tmp.exists()) {
					if(tmp.mkdirs()) {
						classDir = tmp;
					} else {
						classDir = null;
					}
				} else {
					if(tmp.isDirectory()) {
						classDir = tmp;
					} else {
						classDir = null;
					}
				}
			}
			if(classDir!=null) {
				SimpleLogger.info("Generated class files will be written to [" , classDir , "]");
			}
			classPool = new ClassPool(true);
//			classPool.appendClassPath(new ClassClassPath(TrackingSocketImplFactoryClass));
//			classPool.importPackage(TrackingSocketImplFactoryClass.getPackage().getName());
//			trackingSocketImplCtClass = classPool.get(TrackingSocketImpl.class.getName());
//			trackingSocketImplFactoryCtClass = classPool.get(TrackingSocketImplFactoryClass.getName());
			serverSocketCtClass = classPool.get(SERVER_SOCKET_BIN_NAME);
			socketCtClass = classPool.get(SOCKET_BIN_NAME);
			iSocketImplCtClass = classPool.get(ISocketImplClass.getName());
			socketImplCtClass = classPool.get(SOCK_NAME);
			socketOptionsCtClass = classPool.get(SOCK_OPT_NAME);
			socketImplFactoryCtClass = classPool.get(SOCK_FACTORY_NAME);
			createSocketImplMethod = socketImplFactoryCtClass.getDeclaredMethod("createSocketImpl");
			objectCtClass = classPool.get("java.lang.Object");
			SimpleLogger.info("Created SocketImplTransformer");
		} catch (Exception ex) {
			SimpleLogger.error("Failed to initialize SocketImpleTransformer", ex);
			throw new RuntimeException("Failed to initialize SocketImpleTransformer", ex);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.instrument.ClassFileTransformer#transform(java.lang.ClassLoader, java.lang.String, java.lang.Class, java.security.ProtectionDomain, byte[])
	 */
	@Override
	public byte[] transform(ClassLoader loader, String className,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			final byte[] classfileBuffer) throws IllegalClassFormatException {
		LoaderClassPath lcp = null;
		final ByteArrayClassPath bcp = new ByteArrayClassPath(convertFromBinaryName(className), classfileBuffer);
		final int recLevel = recursionLevel.get().incrementAndGet();
		
		final boolean createdSigs = currentSignatures.get()==null;
		if(createdSigs) currentSignatures.set(new HashSet<String>());
		if(instrumented.containsKey(className)) return instrumented.get(className);
		try {			
			if(loader!=null) {
				lcp = new LoaderClassPath(loader);
				classPool.appendClassPath(lcp);
			}
			CtClass clazz = null;
			try {
				clazz = classPool.get(convertFromBinaryName(className));
			} catch (Exception ex) {
				return classfileBuffer;
			}
//			if(SOCKET_BIN_NAME.equals(className)) {
//				byte[] byteCode = transformSocket(clazz);
//				preInstrumented.put(className, classfileBuffer);
//				instrumented.put(className, byteCode);
//				if(loader!=null) {
//					loader.loadClass(convertFromBinaryName(className));
//				} else {
//					Class.forName(convertFromBinaryName(className));
//				}				
//				return byteCode;
//			}
			if(SOCKET_OS_BIN_NAME.equals(className)) {
				return transformSocketOutputStream(clazz);
			}
			if(SOCKET_IS_BIN_NAME.equals(className)) {
				return transformSocketInputStream(clazz);
			}
			
			if(!clazz.isInterface()) {
				if(isSocketImpl(clazz)) {
					SimpleLogger.info("[", Thread.currentThread(), "] SocketImplTransformer Recursion Level:", recLevel);
					byte[] byteCode =  transformSocketImpl(clazz);
					instrumented.put(className, byteCode);
					return byteCode;
//					if(loader!=null) {
//						loader.loadClass(convertFromBinaryName(className));
//						SimpleLogger.info("LOADED SOCKIMPL:[" + className + "] from [", loader, "]");
//					} else {
//						Class.forName(convertFromBinaryName(className));
//						SimpleLogger.info("LOADED SOCKIMPL:[" + className + "] from Bootloader");
//					}				
				}					
			}
//			if(clazz.isInterface()) {
//				if(implementsInterface(clazz, socketImplFactoryCtClass)) {
//					if(verbose) SimpleLogger.info("[", Thread.currentThread(), "] SocketImplTransformer Recursion Level:", recLevel);
//					byte[] byteCode = transformSocketImplFactory(clazz);
//					preInstrumented.put(className, classfileBuffer);
//					instrumented.put(className, byteCode);
//					return byteCode;
//				}
//			} else {
//				if(isSocketImpl(clazz)) {
//					if(verbose) SimpleLogger.info("[", Thread.currentThread(), "] SocketImplTransformer Recursion Level:", recLevel);
//					return transformSocketImpl(clazz);
//				}					
//			}
			return classfileBuffer;
		} catch (Throwable t) {
			t.printStackTrace(System.err);
			return classfileBuffer;
		} finally{
			if(recursionLevel.get().decrementAndGet()==0) {
				recursionLevel.remove();
			}			
			if(createdSigs) {
				currentSignatures.remove();
			}
			classPool.removeClassPath(bcp);
			if(lcp!=null) classPool.removeClassPath(lcp);
		}
	}
	
	/**
	 * Determines if the passed class has {@link SocketImpl} as a direct or indirect superclass and does not already implement {@link ISocketImpl}
	 * @param clazz The class to test  
	 * @return true if the passed class has {@link SocketImpl} as a direct or indirect superclass and does not already implement {@link ISocketImpl}
	 * @throws NotFoundException thrown when the superclass cannot be loaded
	 */
	protected boolean isSocketImpl(CtClass clazz) throws NotFoundException {
		if(clazz.isInterface()) return false;		
		CtClass parent = clazz.getSuperclass();
		while(parent!=null && !parent.getName().equals(objectCtClass.getName())) {
			if(parent.getName().equals(SOCK_NAME)) return true;
			parent = parent.getSuperclass();
		}
		return false;
	}
	
	/**
	 * Determines if the passed class <b><code>clazz</code></b> is a descendent of <b><code>superClazz</code></b>.  
	 * @param clazz The class to test 
	 * @param superClazz The super class
	 * @return true if <b><code>clazz</code></b> is a descendent of <b><code>superClazz</code></b>
	 * @throws NotFoundException thrown on failure to get a super class
	 */
	protected boolean isDescendentOf(CtClass clazz, CtClass superClazz) throws NotFoundException {
		if(clazz.isInterface()) return false;		
		String superName = superClazz.getName();
		CtClass parent = clazz.getSuperclass();
		while(parent!=null && !parent.getName().equals(objectCtClass.getName())) {
			if(parent.getName().equals(superName)) return true;
			parent = parent.getSuperclass();
		}
		return false;
	}
	
	
	/**
	 * Determines if the passed class implements the passed interface and is not abstract
	 * @param clazz The class to test
	 * @param iface The interface to test for
	 * @return true if the passed class implements the passed interface, false otherwise
	 * @throws NotFoundException thrown on error getting the class interfaces
	 */
	protected boolean implementsInterface(CtClass clazz, CtClass iface) throws NotFoundException {
		if(Modifier.isAbstract(clazz.getModifiers())) return false;
		boolean isIface = false;
		for(CtClass i: clazz.getInterfaces()) {
			if(i.getName().equals(iface.getName())) {
				isIface = true;
				break;
			}
		}
		return isIface;
	}
	
//	/**
//	 * Instruments the passed {@link SocketImplFactory}
//	 * @param socketImplFactoryImpl the javassist representation of a {@link SocketImplFactory} 
//	 * @return the class byte code
//	 */
//	protected byte[] transformSocketImplFactory(CtClass socketImplFactoryImpl) {
//		try {
//			SimpleLogger.info("Transforming SocketImplFactory [", socketImplFactoryImpl.getName(), "]");
//			CtMethod originalMethod = socketImplFactoryImpl.getMethod(createSocketImplMethod.getName(), createSocketImplMethod.getSignature());
//			originalMethod.setName("_" + createSocketImplMethod.getName());
//			CtMethod newMethod = new CtMethod(socketImplCtClass, createSocketImplMethod.getName(), new CtClass[0], socketImplFactoryImpl);
//			newMethod.setBody("return new TrackingSocketImplFactory(" + originalMethod.getName() + ");");
//			if(classDir!=null) {
//				SimpleLogger.info("Writing SocketImplFactory [", socketImplFactoryImpl.getName(), "]");
//				socketImplFactoryImpl.writeFile(classDir.getAbsolutePath());
//			}
//			return socketImplFactoryImpl.toBytecode();
//		} catch (Exception ex) {
//			ex.printStackTrace(System.err);
//			throw new RuntimeException("Failed to instrument [" + socketImplFactoryImpl.getName() + "]", ex);
//		}		
//	}
	
//	protected byte[] transformSocket(CtClass socket) {
//		SimpleLogger.info("Transforming Socket");
//		try {
//			socket.defrost();
//			CtMethod setMethod = socket.getDeclaredMethod("setSocketImplFactory");
//			setMethod.setName("_setSocketImplFactory");
//			CtMethod newMethod = new CtMethod(setMethod, socket, null);
//			newMethod.setName("setSocketImplFactory");
//				//new CtMethod(CtClass.voidType, "setSocketImplFactory", new CtClass[]{socketImplFactoryCtClass}, socket);
//			newMethod.setBody(new StringBuilder("{")				
//				.append("if (factory != null && (factory instanceof TrackingSocketImplFactory)) {")
//				.append("System.out.println(\"Current Factory:\" + factory.getClass().getName());")
//				.append("factory = new TrackingSocketImplFactory($1);")
//				.append("} else { _setSocketImplFactory($1); if($1 instanceof TrackingSocketImplFactory) {}")
//				.append("System.out.println(\"Installed Factory:\" + factory.getClass().getName());")
//				.append("}")
//				.append("}")
//				.toString()
//			);
//			newMethod.setModifiers(newMethod.getModifiers() | Modifier.STATIC);
//			newMethod.setModifiers(newMethod.getModifiers() | Modifier.PUBLIC);
//			socket.addMethod(newMethod);
//			if(classDir!=null) {
//				SimpleLogger.info("Writing Socket [", socket.getName(), "]");
//				socket.writeFile(classDir.getAbsolutePath());
//			}			
//			return socket.toBytecode();
//		} catch (Exception ex) {
//			ex.printStackTrace(System.err);
//			throw new RuntimeException("Failed to instrument socket", ex);
//		}		
//	}
	
	/**
	 * Transforms a {@link SocketImpl} class
	 * @param socketImplImpl the javassist representation of a {@link SocketImpl} 
	 * @return the class byte code
	 * TODO: publify fields
	 * TODO: add socket tracker adapter calls  (CtMethod.insertAfter or CodeConverter.insertAfterMethod) 
	 * @throws NotFoundException thrown on failure to render class hierarchy
	 */
	protected byte[] transformSocketImpl(final CtClass socketImplImpl) throws NotFoundException {
		SimpleLogger.info("Transforming SocketImpl [", socketImplImpl.getName(), "]");
		final Set<String> sigs = currentSignatures.get();
		try {
			socketImplImpl.defrost();
			socketImplImpl.setModifiers(socketImplImpl.getModifiers() | Modifier.PUBLIC);
			CtConstructor ctor = socketImplImpl.getDeclaredConstructor(new CtClass[0]);
			ctor.setModifiers(ctor.getModifiers() | Modifier.PUBLIC);
			socketImplImpl.addInterface(iSocketImplCtClass);
			CtMethod getSockMethod = new CtMethod(socketCtClass, "getSocket", new CtClass[0], socketImplImpl);
			getSockMethod.setModifiers(getSockMethod.getModifiers() | Modifier.PUBLIC);
			getSockMethod.setBody("return socket;");
			socketImplImpl.addMethod(getSockMethod);
			CtMethod getServerSockMethod = new CtMethod(serverSocketCtClass, "getServerSocket", new CtClass[0], socketImplImpl);
			getServerSockMethod.setModifiers(getServerSockMethod.getModifiers() | Modifier.PUBLIC);
			getServerSockMethod.setBody("return serverSocket;");
			socketImplImpl.addMethod(getServerSockMethod);
			
			for(CtMethod method: socketImplCtClass.getDeclaredMethods()) {
				CtMethod toPub = socketImplImpl.getMethod(method.getName(), method.getSignature());
				String key = toPub.getName() + "." + toPub.getSignature();
				int modifiers = toPub.getModifiers();
				if(Modifier.isNative(modifiers) || sigs.contains(key) || socketImplSigs.containsKey(toPub.getName() + toPub.getSignature())) continue;
				sigs.add(key);
				socketImplSigs.put(toPub.getName() + toPub.getSignature(), socketImplImpl.getName());
//				SimpleLogger.info("ADD KEY [", key, "]");
				//socketImplImpl.removeMethod(toPub);
				toPub.setModifiers(toPub.getModifiers() & ~Modifier.PROTECTED);
				toPub.setModifiers(toPub.getModifiers() | Modifier.PUBLIC);
				
				String transform = SocketTrackingAdapter.SOCKET_IMPL_ADAPTERS.get(key);
				if(transform!=null) {
//					CtMethod afterMethod = new CtMethod(toPub.getReturnType(), "_after" + toPub.getName(), toPub.getParameterTypes(), socketImplImpl);
//					SimpleLogger.info("Adding AfterMethod:[", toPub.getLongName(), "  /  ", afterMethod.getLongName(), "]\n\t[", transform, "]" );
//					afterMethod.setBody(transform);
//					socketImplImpl.addMethod(afterMethod);
//					CodeConverter cc = new CodeConverter();
//					cc.insertAfterMethod(toPub, afterMethod);
//					toPub.instrument(cc);
					transform = transform.replace("##className##", toPub.getDeclaringClass().getSimpleName());
//					SimpleLogger.info("Adding insertAfter on:[", toPub.getDeclaringClass().getSimpleName(), " | ", toPub.getSignature(), "]\n\t[", transform, "]" );
					toPub.insertAfter(transform);
				}
				//socketImplImpl.addMethod(toPub);
			}
			if(classDir!=null) {
//				SimpleLogger.info("Writing SocketImpl [", socketImplImpl.getName(), "]");
				socketImplImpl.writeFile(classDir.getAbsolutePath());
			}						
			return socketImplImpl.toBytecode(); 
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
			throw new RuntimeException("Failed to instrument [" + socketImplImpl.getName() + "]", ex);
		}
	}
	
	/**
	 * Instruments the SocketOutputStream class
	 * @param socketOutput the javassist representation of the SocketOutputStream class
	 * @return the bytecode of the instrumented class 
	 * @throws Exception thrown on any error instrumenting the class
	 */
	protected byte[] transformSocketOutputStream(CtClass socketOutput) throws Exception {
		for(CtMethod method: socketOutput.getDeclaredMethods()) {
			String key = method.getName() + "." + method.getSignature();
			String transform = SocketTrackingAdapter.SOCKET_OS_ADAPTERS.get(key);
			if(transform!=null) {
				method.insertAfter(transform);
			}
		}
		return socketOutput.toBytecode();
	}
	
	/**
	 * Instruments the SocketInputStream class
	 * @param socketInput the javassist representation of the SocketInputStream class
	 * @return the bytecode of the instrumented class 
	 * @throws Exception thrown on any error instrumenting the class
	 */
	protected byte[] transformSocketInputStream(CtClass socketInput) throws Exception {
		for(CtMethod method: socketInput.getDeclaredMethods()) {
			String key = method.getName() + "." + method.getSignature();
			String transform = SocketTrackingAdapter.SOCKET_IS_ADAPTERS.get(key);
			if(transform!=null) {
				method.insertAfter(transform);
			}
		}
		return socketInput.toBytecode();
	}
	
	
	/**
	 * Returns a formatted string of the socket impl instrumented methods
	 * @return a formatted string of the socket impl instrumented methods
	 */
	public String getSocketImplSigs() {
		StringBuilder b = new StringBuilder();
		for(Map.Entry<String, String> entry: socketImplSigs.entrySet()) {
			b.append("\n\t").append(entry.getValue()).append(" : ").append(entry.getKey());
		}
		return b.toString();
	}
	
	/**
	 * Builds an inherritance hierarchy of the passed class <b><code>impl</code></b>
	 * where all classes extend <b><code>parent</code></b> up to the top class that is <b><code>parent</code></b>  
	 * @param impl The class to start from
	 * @param parent the top class to go to
	 * @return a list of classes starting with <b><code>impl</code></b>  and ending with <b><code>parent</code></b>.
	 * @throws NotFoundException thrown on any failure to get super classes
	 */
	protected List<CtClass> getClassHierarchy(CtClass impl, CtClass parent) throws NotFoundException {
		List<CtClass> hier = new ArrayList<CtClass>();
		if(isDescendentOf(impl, parent)) {
			hier.add(impl);
			CtClass sc = impl.getSuperclass();
			while(sc!=null && !sc.getName().equals(objectCtClass.getName()) && !sc.getName().equals(parent.getName())) {
				hier.add(sc);
				sc = sc.getSuperclass();
			}
			hier.add(parent);
		}
		return hier;
	}
	
	
	/**
	 * Converts a class name to the binary name used by the class file transformer, or returns the passed name if it is already a binary name
	 * @param name The class name to convert
	 * @return the binary name
	 */
	protected String convertToBinaryName(String name) {
		int index = name.indexOf('.');
		if(index!=-1) {
			return name.replace('.', '/');
		}
		return name;
	}
	
	/**
	 * Converts a class name from the binary name used by the class file transformer to the standard dot notated form, or returns the passed name if it is already a binary name
	 * @param name The class name to convert
	 * @return the standard dot notated class name
	 */
	protected String convertFromBinaryName(String name) {
		int index = name.indexOf('/');
		if(index!=-1) {
			return name.replace('/', '.');
		}
		return name;
	}
		

}

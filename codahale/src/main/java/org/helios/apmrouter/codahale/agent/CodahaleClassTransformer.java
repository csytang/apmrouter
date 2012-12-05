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
package org.helios.apmrouter.codahale.agent;

import java.lang.annotation.Annotation;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Proxy;
import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javassist.ByteArrayClassPath;
import javassist.CannotCompileException;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.annotation.AnnotationImpl;
import javassist.bytecode.annotation.NoSuchClassError;

import org.helios.apmrouter.codahale.annotation.Timed;

import com.yammer.metrics.core.Timer;



/**
 * <p>Title: CodahaleClassTransformer</p>
 * <p>Description: Java Agent class transformer for <a href="http://metrics.codahale.com">Codahale</a> instrumentation</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.codahale.agent.CodahaleClassTransformer</code></p>
 */

public class CodahaleClassTransformer implements ClassFileTransformer {
	/** A set of the package names (in binary format) to be inspected for instrumentation */
	protected final Set<String> targetPackages = new CopyOnWriteArraySet<String>();
	/** A set of the package names (in binary format) that should not be instrumented */
	protected final Set<String> prohibitedPackages = new CopyOnWriteArraySet<String>();
	/** A set of instrumented class info */
	protected final Set<InstrumentedClassInfo> classInfo = new CopyOnWriteArraySet<InstrumentedClassInfo>();
	/** A javassist classpool for the current thread */
	protected final ThreadLocal<ClassPool> classPool = new ThreadLocal<ClassPool>() {
		@Override
		protected ClassPool initialValue() {
			return new ClassPool();
		}
	};
	
	/**
	 * Creates a new CodahaleClassTransformer
	 * @param packageNames An array of package names to instrument
	 */
	public CodahaleClassTransformer(String...packageNames) {
		for(String s: packageNames) {
			if(s.indexOf('.')!=-1) {
				targetPackages.add(s.replace('.', '/').trim());
				continue;
			}
			if(s.indexOf('/')==-1) {
				targetPackages.add(s.trim());
			}			
		}
	}
	
	/**
	 * Creates a new CodahaleClassTransformer
	 * @param packageNames A collection of package names to instrument
	 */
	public CodahaleClassTransformer(Collection<String> packageNames) {
		this(packageNames.toArray(new String[0]));
	}
	
	/**
	 * Converts from a standard java class name to the binary class name
	 * @param className The dot separated java class name
	 * @return The / separated class name
	 */
	public static String toBinaryName(String className) {
		return className.replace('.', '/');
	}
	
	/**
	 * Converts from a binary java class name to the java internal class name
	 * @param binClassName The / separated java class name
	 * @return The . separated class name
	 */
	public static String toName(String binClassName) {
		return binClassName.replace('/', '.');
	}
	
	/**
	 * Returns the binary package name for the passed binary class name
	 * @param binClassName The / separated class name
	 * @return the / separated package name
	 */
	public static String getBinaryPackage(String binClassName) {
		int index = binClassName.lastIndexOf('/');
		if(index==0) return binClassName;
		return binClassName.substring(0, index);
	}
	

	
	/**
	 * {@inheritDoc}
	 * @see java.lang.instrument.ClassFileTransformer#transform(java.lang.ClassLoader, java.lang.String, java.lang.Class, java.security.ProtectionDomain, byte[])
	 */
	@Override
	public byte[] transform(ClassLoader classLoader, String className, Class<?> classToBeTransformed,
			ProtectionDomain protectionDomain, byte[] classFileBuffer) throws IllegalClassFormatException {
		final byte[] original = classFileBuffer;		
		CtClass clazz = null;
		
		String packageName = getBinaryPackage(className);
		if(!targetPackages.contains(packageName) || prohibitedPackages.contains(packageName)) {
			//log("Skipped [" + className + "]");
			return original;
		}
		log("Examining [" + className + "]");
		//cp.appendClassPath(new LoaderClassPath(classLoader));
		ClassPool cp = classPool.get();
		try {
			cp.appendClassPath(new LoaderClassPath(Thread.currentThread().getContextClassLoader()));
			cp.appendClassPath(new LoaderClassPath(Timer.class.getClassLoader()));
			cp.appendClassPath(new ByteArrayClassPath(toName(className), original));
			cp.appendClassPath(new ClassClassPath(org.helios.apmrouter.codahale.annotation.Timed.class));
			
			clazz = cp.get(toName(className));
			log("Loaded CtClass [" + clazz.getName() + "]");
			int totalInstrumentations = 0;
			for(CtMethod method: clazz.getDeclaredMethods()) {
				Object[] annotations = method.getAvailableAnnotations();
				if(annotations.length>0) {
					cp.importPackage("com.yammer.metrics.core");
					log("Found [" + annotations.length + "] annotations on [" + clazz.toString() + "]");
					totalInstrumentations += instrumentMethod(method, annotations);
				}
			}
			if(totalInstrumentations>0) {
				log("Completed [" + totalInstrumentations + "] joinpoint instrumentations on  [" + clazz.toString() + "]");
				byte[] instrumentedByteCode = clazz.toBytecode();
				classInfo.add(new InstrumentedClassInfo(className, classLoader, protectionDomain, original, instrumentedByteCode));
				return instrumentedByteCode;
			}		
			return original;			
		} catch (Exception ex) {
			log("Failed to instrument class [" + className + "]:" + ex + ". Stack trace follows.");
			ex.printStackTrace(System.err);
			return original;
		} finally {
			if(clazz!=null) try { clazz.detach(); } catch (Exception ex) {/* No Op */}
			classPool.remove();
		}
	}
	
	/**
	 * Instruments the passed method for each of the passed annotations
	 * @param method The method to instrument
	 * @param annotations The annotations describing the type of instrumentation to apply
	 * @return the number of successfully applied annotations
	 * @throws NoSuchClassError thrown if supporting classes cannot be found
	 * @throws ClassNotFoundException thrown if supporting classes cannot be found
	 */
	protected int instrumentMethod(CtMethod method, Object...annotations) throws ClassNotFoundException, NoSuchClassError {
		int cnt = 0;
		for(Object annotation: annotations) {
			AnnotationImpl ai = (AnnotationImpl)Proxy.getInvocationHandler(annotation);			
			AnnotationType annotationType = AnnotationType.CLASS2TYPE.get(ai.getTypeName());
			if(annotationType==null) continue;
			//Object rebuiltAnnotation = AnnotationImpl.make(annotationType.getAnnotationClazz().getClassLoader(), annotationType.getAnnotationClazz(), classPool.get(), ai.getAnnotation());
			Annotation typedAnnotation = (Annotation) Proxy.newProxyInstance(annotationType.getAnnotationClazz().getClassLoader(), new Class[]{annotationType.getAnnotationClazz()}, ai);	
			try {
				instrumentMethod(method, typedAnnotation, annotationType);
				cnt++;
			} catch (Exception ex) {
				log("Failed to instrument method [" + method.getName() + "] with annotation [" + annotationType.name() + "]. Stack trace follows...");
				ex.printStackTrace(System.err);
			}
			
		}
		return cnt;
	}
	
	/**
	 * Basically a switch block to direct the method to the correct instrumentation method according to the type of annotation
	 * @param method The method to instrument
	 * @param typedAnnotation The rebuilt typed annotation 
	 * @param annotationType The annotation type to switch on
	 * @throws CannotCompileException possibly thrown by the delegated instrumentation methods
	 * @throws NotFoundException thrown if supporting classes cannot be found
	 */
	protected void instrumentMethod(CtMethod method, Annotation typedAnnotation, AnnotationType annotationType) throws CannotCompileException, NotFoundException {
		switch (annotationType) {
			case TIMED:
				instrumentMethodTimed(method, (Timed)typedAnnotation);
				break;
			default:
				break;
		}
	}
	
	/** Scoped CtMethod template for a codahale timer */
	public static final String SCOPED_TIMER_TEMPLATE = "com.yammer.metrics.Metrics.defaultRegistry().newTimer(%s.class, \"%s\", \"%s\", java.util.concurrent.TimeUnit.%s, java.util.concurrent.TimeUnit.%s);";
	/** No Scope CtMethod template for a codahale timer */
	public static final String TIMER_TEMPLATE = "com.yammer.metrics.Metrics.defaultRegistry().newTimer(%s.class, \"%s\",  java.util.concurrent.TimeUnit.%s, java.util.concurrent.TimeUnit.%s);";
	
	/**
	 * Instruments a method with a {@link Timed} annotation
	 * @param method The method to instrument
	 * @param timed The @Timed annotation instance
	 * @throws CannotCompileException thrown on javassist compilation errors
	 * @throws NotFoundException thrown if supporting classes cannot be found
	 */
	protected void instrumentMethodTimed(CtMethod method, Timed timed) throws CannotCompileException, NotFoundException {
		final boolean staticMethod = Modifier.isStatic(method.getModifiers());
		CtClass clazz = method.getDeclaringClass();
		String fieldName = (staticMethod ? "static" : "") + "Timer_" + clazz.makeUniqueName(method.getName());
		boolean scoped = !"".equals(timed.scope());
		String initer = scoped ? 
//				String.format(SCOPED_TIMER_TEMPLATE, fieldName, clazz.getName(), timed.name(), timed.scope(), timed.durationUnit(), timed.rateUnit()) :				
//				String.format(TIMER_TEMPLATE, fieldName, clazz.getName(), timed.name(), timed.durationUnit(), timed.rateUnit())
				String.format(SCOPED_TIMER_TEMPLATE, clazz.getName(), timed.name(), timed.scope(), timed.durationUnit(), timed.rateUnit()) :				
				String.format(TIMER_TEMPLATE, clazz.getName(), timed.name(), timed.durationUnit(), timed.rateUnit())
								
		;
		log("Timer init for [" + method.toString() + "]\n[" + initer + "]");
		CtField timerField = new CtField(classPool.get().get("com.yammer.metrics.core.Timer"), fieldName, clazz);
		timerField.setModifiers(timerField.getModifiers() | Modifier.PRIVATE | Modifier.FINAL );
		if(staticMethod) timerField.setModifiers(timerField.getModifiers() | Modifier.STATIC );		
		clazz.addField(timerField, CtField.Initializer.byExpr(initer));
		
		method.instrument(new TimerExpressionEditor(fieldName));		
	}
		
//	protected void instrumentMethodTimed(CtMethod method, Map<String, ?> av) throws CannotCompileException {
//		final boolean staticMethod = Modifier.isStatic(method.getModifiers());
//		CtClass clazz = method.getDeclaringClass();
//		String fieldName = (staticMethod ? "static" : "") + "Timer_" + clazz.makeUniqueName(method.getName());
//		
//		//CtClass timerClass = gc("com.yammer.metrics.core.Timer");
//		boolean scoped = !"".equals(av.get("scope"));
//		String initer = scoped ? 
//				String.format(SCOPED_TIMER_TEMPLATE, fieldName, clazz.getName(), av.get("name"), av.get("scope"), av.get("durationUnit"), av.get("rateUnit"))
//				:
//					String.format(TIMER_TEMPLATE, fieldName, clazz.getName(), av.get("name"), av.get("durationUnit"), av.get("rateUnit"))
//		;
//				log("Timer init for [" + method.toString() + "]\n[" + initer + "]");
//		CtField timerField = CtField.make(initer, clazz);
//		timerField.setModifiers(timerField.getModifiers() | Modifier.PRIVATE | Modifier.FINAL );
//		if(staticMethod) timerField.setModifiers(timerField.getModifiers() | Modifier.STATIC );		
//		clazz.addField(timerField);
//		method.instrument(new TimerExpressionEditor(fieldName));		
//	}
	
	
	
	/**
	 * Looks up the named class in the current thread's classpool
	 * @param className The class name to get the class for
	 * @return the named class
	 */
	protected CtClass gc(String className) {
		try {
			return classPool.get().get(className);
		} catch (NotFoundException nfe) {
			throw new RuntimeException("The class [" + className + "] was not found", nfe);
		}
	}
	
	
	/**
	 * Returns an array of annotation instances found for the passed CtMethod.
	 * @param method The CtMethod to look for annotations on
	 * @return A possible empty array of annotations
	 */
	protected Object[] getAnnotations(CtMethod method) {
		Set<Annotation> annotations = new HashSet<Annotation>();
		try {
			annotations.add((Annotation) method.getAnnotation(Timed.class));
		} catch (ClassNotFoundException e) { /* No Op */ }
		
		return annotations.toArray(new Annotation[annotations.size()]);
	}
	
	/**
	 * Simple out logger
	 * @param msg the message
	 */
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	/**
	 * Simple err logger
	 * @param msg the message
	 */
	public static void elog(Object msg) {
		System.err.println(msg);
	}
	
	/**
	 * Error logger
	 * @param msg The error message
	 * @param t The throwable to print the stack trace for
	 */
	public static void loge(Object msg, Throwable t) {
		System.err.println(msg);
		t.printStackTrace(System.err);
	}	
		

}

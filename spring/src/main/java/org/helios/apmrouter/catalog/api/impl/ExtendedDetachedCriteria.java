/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2012, Helios Development Group and individual contributors
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
package org.helios.apmrouter.catalog.api.impl;

import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.ProjectionList;

/**
 * <p>Title: ExtendedDetachedCriteria</p>
 * <p>Description: An extended implementation of {@link DetachedCriteria} that facades some of the functionality only found in the attached {@link Criteria} class.
 * Extended functionality is applied when {@link DetachedCriteria#getExecutableCriteria(org.hibernate.Session)} is called.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.catalog.api.impl.ExtendedDetachedCriteria</code></p>
 */

public class ExtendedDetachedCriteria extends DetachedCriteria implements Parsed<ExtendedDetachedCriteria>, ExecutableQuery  {

	/**  */
	private static final long serialVersionUID = -1135481743060334307L;
	
	/** The op code to set the query fetch size  */
	public static final String EXT_FETCHSIZE = "fs";
	/** The op code to set the query timeout in seconds */
	public static final String EXT_TIMEOUT = "t";
	/** The op code to enable query caching for this query  */
	public static final String EXT_CACHEABLE = "c";
	/** The op code to set override the flush mode for this query */
	public static final String EXT_FLUSHMODE = "fm";
	/** The op code to set override the cache mode for this query */
	public static final String EXT_CACHEMODE = "cm";
	/** The op code to set the maximum number of results  */
	public static final String EXT_MAXRESULTS = "mr";
	/** The op code to set first result returned for this query */
	public static final String EXT_FIRSTRESULT = "fr";
	/** The op code to set the cache region name to be used for query caching for this query */
	public static final String EXT_CACHEREGION = "cr";
	/** The op code to set the fetch mode for this query */
	public static final String EXT_FETCHMODE = "fmd";
	/** The op code to to start setting projections on the query */
	public static final String EXT_PROJS = "projs";
	/** The op code to to add a projection to the projection list of this query */
	public static final String EXT_PROJ = "proj";
	/** The op code to to add a criterion to this query */
	public static final String EXT_CRIT = "cond";
	
	
	/** The recognized op codes */
	public static final Set<String> ATTR_OP_CODES;
	
	/** The available fetch modes (since the class does not have a static parse) */
	private static final Map<String, FetchMode> fetchModes = new HashMap<String, FetchMode>(5);
	
	/** Method handles for the extended Criteria methods, keyed by the JSON op code */
	private static final Map<String, MethodHandle> myMethodHandles = new HashMap<String, MethodHandle>();
	/** Method handles for the attached Criteria methods, keyed by the JSON op code */
	private static final Map<String, MethodHandle> criteriaMethodHandles = new HashMap<String, MethodHandle>();
	
	/** Saved extended Criteria method values, keyed by the JSON op code */
	private final Map<String, Serializable> extendedCriteria = new HashMap<String, Serializable>();
	/** a map of {@link Criteria} methods keyed by method name */
	private static final Map<String, Method> criteriaMethods = new HashMap<String, Method>();
	
	/** The running projection list for this query */
	protected ProjectionListAccumulator projectionList = null;
	
	static {
		Set<String> tmp = new HashSet<String>();
		for(Field field: ExtendedDetachedCriteria.class.getDeclaredFields()) {
			if(Modifier.isStatic(field.getModifiers()) &&  field.getName().startsWith("EXT_")) {
				try {
					tmp.add((String)field.get(null));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}				
			}
		}		
		ATTR_OP_CODES = Collections.unmodifiableSet(tmp);
		System.out.println("" + ATTR_OP_CODES.size() +  " Op Codes:" + ATTR_OP_CODES);
		for(Field field: FetchMode.class.getDeclaredFields()) {
			if(Modifier.isStatic(field.getModifiers()) && field.getType().equals(FetchMode.class)) {
				try {
					fetchModes.put(field.getName(), (FetchMode)field.get(null));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}
		for(Method m: Criteria.class.getDeclaredMethods()) {
			if(m.getName().startsWith("set")) {
				criteriaMethods.put(m.getName(), m);
			}
		}
		for(Method m: ExtendedDetachedCriteria.class.getDeclaredMethods()) {
			if(!Modifier.isStatic(m.getModifiers()) && m.getName().startsWith("set")) {
				saveHandle(m);
			}
		}
		saveHandle(EXT_PROJS, ExtendedDetachedCriteria.class, "setProjection", Object[].class);
		saveHandle(EXT_PROJ, ExtendedDetachedCriteria.class, "setProjection", Object[].class);
		saveHandle(EXT_CRIT, ExtendedDetachedCriteria.class, "createCriteria", Object.class);
//		System.out.println("Criteria Codes:" + criteriaMethodHandles.keySet());
//		System.out.println("My Codes:" + myMethodHandles.keySet());
	}
	
	private static void saveHandle(String key, Class<?> clazz, String name, Class<?>...pattern){
		try {
			Method m = null;
			try {
				m = clazz.getDeclaredMethod(name, pattern);
			} catch (NoSuchMethodException nex) {
				m = clazz.getMethod(name, pattern);
			}
			saveHandle(key, m);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	private static void saveHandle(Method method){
		saveHandle(null, method);
	}
	
	private static void saveHandle(String dkey, Method method){
		try {
			MethodType desc = MethodType.methodType(method.getReturnType(), method.getParameterTypes());
			MethodHandle mh = MethodHandles.lookup().findVirtual(method.getDeclaringClass(), method.getName(), desc);
			String key = dkey!=null ? dkey : (method.getName().equals("setFetchMode")) ? "fmd" : getKey(method.getName());
			myMethodHandles.put(key, mh);
			if(method.getDeclaringClass()!=ExtendedDetachedCriteria.class) {
				System.out.println(method.toGenericString());
			}
			if(method.getDeclaringClass()==Criteria.class) {
				Method cmethod = criteriaMethods.get(method.getName());
				desc = MethodType.methodType(Criteria.class, cmethod.getParameterTypes());
				mh = MethodHandles.lookup().findVirtual(Criteria.class, method.getName(), desc);
				criteriaMethodHandles.put(key, mh);
			}
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
			throw new RuntimeException(ex);
		}
	}
	
	
	/** The first int of upper case letters */
	private static final int LOWER = 'A';
	/** The last int of upper case letters */
	private static final int UPPER = 'Z';
	
	@Override
	public Parsed<ExtendedDetachedCriteria> applyPrimitive(String op, Object value) {
		Object nestedReturn = null;
		try {
			if(value.getClass().isArray()) {
				Object[] args = new Object[Array.getLength(value)+1];
				args[0] = this;
				System.arraycopy(value, 0, args, 1, args.length-1);
				nestedReturn = myMethodHandles.get(op).invokeWithArguments(args);
			} else {
				nestedReturn = myMethodHandles.get(op).invoke(this, value);
			}
			if(nestedReturn!=null && nestedReturn!=this && nestedReturn instanceof Parsed) {
				return (Parsed<ExtendedDetachedCriteria>)nestedReturn;
			}
			return this;
		} catch (Throwable t)  {
			throw new RuntimeException(t);
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.catalog.api.impl.ExecutableQuery#execute(org.hibernate.Session)
	 */
	@Override
	public List<?> execute(Session session) {
		Criteria criteria = getExecutableCriteria(session);
		return criteria.list();
	}	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.catalog.api.impl.ExecutableQuery#unique(org.hibernate.Session)
	 */
	@Override
	public Object unique(Session session) {
		Criteria criteria = getExecutableCriteria(session);
		return criteria.uniqueResult();		
	}
	
	private static String getKey(String methodName) {
		StringBuilder b = new StringBuilder();
		for(int c: methodName.replace("set", "").toCharArray()) {
			
			if(c >= LOWER && c <= UPPER) {
				b.append((char)c);
			}
		}
		return b.toString().toLowerCase();
	}

	/**
	 * Creates a new ExtendedDetachedCriteria
	 * @param entityName the entity name this query is for
	 */
	public ExtendedDetachedCriteria(String entityName) {
		super(entityName);
	}

	/**
	 * Creates a new ExtendedDetachedCriteria
	 * @param entityName the entity name this query is for
	 * @param alias the entity alias
	 */
	public ExtendedDetachedCriteria(String entityName, String alias) {
		super(entityName, alias);
	}

	/**
	 * Sets or adds a projection
	 * @param args The projection op arguments
	 * @return the projection list parsed.
	 */
	public Parsed<ProjectionList> setProjection(Object...args) {
		if(projectionList==null) {
			projectionList = new ProjectionListAccumulator();
		}
		Object[] pargs = null;
		if(args.length>1) {
			pargs = new Object[args.length-1];
			System.arraycopy(args, 1, pargs, 0, pargs.length);
		} 
		return projectionList.applyPrimitive((String)args[0], pargs); 
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.hibernate.criterion.DetachedCriteria#getExecutableCriteria(org.hibernate.Session)
	 */
	@Override
	public Criteria getExecutableCriteria(Session session) {
		if(projectionList!=null) this.setProjection(projectionList.get());
		Criteria criteria = super.getExecutableCriteria(session);
		for(Map.Entry<String, Serializable> ext: extendedCriteria.entrySet()) {
			try {
				criteriaMethodHandles.get(ext.getKey()).invokeWithArguments(criteria, extendedCriteria.get(ext.getKey()));
			} catch (Throwable e) {
				throw new RuntimeException("Failed to invoke extended criteria application for [" + ext.getKey() + "]", e);
			}
		}
		return criteria;
	}
	
	public static void main(String[] args) {}
	
	/**
	 * Returns a new nested detached criteria
	 * @param value The value to create the with  
	 * @return a new ExtendedDetachedCriteria
	 */
	public ExtendedDetachedCriteria createCriteria(Object value) {
		try {
			if(value.getClass().isArray()) {
				
				Object[] args = new Object[Array.getLength(value)];
				System.arraycopy(value, 0, args, 0, args.length);
				return new ExtendedDetachedCriteria((String)args[0], (String)args[1]);
			}
			return new ExtendedDetachedCriteria((String)value);
		} catch (Throwable t)  {
			throw new RuntimeException(t);
		}
	}
	
	/**
	 * Specify an association fetching strategy for an association or a
	 * collection of values.
	 *
	 * @param associationPath a dot seperated property path
	 * @param mode The fetch mode for the referenced association
	 * @return this (for method chaining)
	 */
	public ExtendedDetachedCriteria setFetchMode(String associationPath, String mode) {
		setFetchMode(associationPath, fetchModes.get(mode.trim().toUpperCase()));
		return this;
	}

	/**
	 * Set a limit upon the number of objects to be retrieved.
	 *
	 * @param maxResults the maximum number of results
	 * @return this (for method chaining)
	 */
	public ExtendedDetachedCriteria setMaxResults(Number maxResults) {
		extendedCriteria.put(EXT_MAXRESULTS, maxResults.intValue());
		return this;
	}

	/**
	 * Set the first result to be retrieved.
	 *
	 * @param firstResult the first result to retrieve, numbered from <tt>0</tt>
	 * @return this (for method chaining)
	 */
	public ExtendedDetachedCriteria setFirstResult(Number firstResult) {
		extendedCriteria.put(EXT_FIRSTRESULT, firstResult.intValue());
		return this;
	}

	/**
	 * Set a fetch size for the underlying JDBC query.
	 *
	 * @param fetchSize the fetch size
	 * @return this (for method chaining)
	 *
	 * @see java.sql.Statement#setFetchSize
	 */	
	public ExtendedDetachedCriteria setFetchSize(Number fetchSize) {
		extendedCriteria.put(EXT_FETCHSIZE, fetchSize.intValue());
		return this;
	}

	/**
	 * Set a timeout for the underlying JDBC query.
	 *
	 * @param timeout The timeout value to apply.
	 * @return this (for method chaining)
	 *
	 * @see java.sql.Statement#setQueryTimeout
	 */
	public ExtendedDetachedCriteria setTimeout(Number timeout) {
		extendedCriteria.put(EXT_TIMEOUT, timeout.intValue());
		return this;
	}

	/**
	 * Enable caching of this query result, provided query caching is enabled
	 * for the underlying session factory.
	 *
	 * @param cacheable Should the result be considered cacheable; default is
	 * to not cache (false).
	 * @return this (for method chaining)
	 */
	public ExtendedDetachedCriteria setCacheable(boolean cacheable) {
		extendedCriteria.put(EXT_CACHEABLE, cacheable);
		return this;
	}

	/**
	 * Set the name of the cache region to use for query result caching.
	 *
	 * @param cacheRegion the name of a query cache region, or <tt>null</tt>
	 * for the default query cache
	 * @return this (for method chaining)
	 *
	 * @see #setCacheable
	 */
	public ExtendedDetachedCriteria setCacheRegion(String cacheRegion) {
		extendedCriteria.put(EXT_CACHEREGION, cacheRegion);
		return this;
	}

	/**
	 * Override the flush mode for this particular query.
	 *
	 * @param flushMode The flush mode to use.
	 * @return this (for method chaining)
	 */
	public ExtendedDetachedCriteria setFlushMode(String flushMode) {
		extendedCriteria.put(EXT_FLUSHMODE, FlushMode.parse(flushMode.trim().toUpperCase()));
		return this;
	}

	/**
	 * Override the cache mode for this particular query.
	 *
	 * @param cacheMode The cache mode to use.
	 * @return this (for method chaining)
	 */
	public ExtendedDetachedCriteria setCacheMode(String cacheMode) {
		extendedCriteria.put(EXT_CACHEMODE, CacheMode.parse(cacheMode.trim().toUpperCase()));
		return this;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.catalog.api.impl.Parsed#get()
	 */
	@Override
	public ExtendedDetachedCriteria get() {		
		return this;
	}



	

}

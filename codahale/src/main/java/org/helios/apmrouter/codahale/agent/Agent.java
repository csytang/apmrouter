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
package org.helios.apmrouter.codahale.agent;

import java.lang.instrument.Instrumentation;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

import org.helios.apmrouter.codahale.helios.HeliosReporter;
import org.helios.apmrouter.jmx.XMLHelper;
import org.w3c.dom.Node;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.MetricPredicate;



/**
 * <p>Title: Agent</p>
 * <p>Description: Java Agent to bootstrap the codahale AOP</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.codahale.agent.Agent</code></p>
 */

public class Agent {
	/** The provided instrumentation instance */
	protected static Instrumentation instrumentation = null;
	/** The provided agent argument string */
	protected static String agentArgs = null;
	/** The configured package names that should be considered for AOP */
	protected static Set<String> packageNames = new CopyOnWriteArraySet<String>();
	/** The codahale class transformer */
	protected static CodahaleClassTransformer codahaleTransformer = null;
	
	

	/**
	 * The pre-main entry point
	 * @param agentArgs The agent bootstrap arguments
	 * @param inst The Instrumentation instance
	 */
	public static void premain(String agentArgs, Instrumentation inst) {
		Agent.agentArgs = agentArgs;
		Agent.instrumentation = inst;		
	}
	
	/**
	 * The agent-main entry point
	 * @param agentArgs The agent bootstrap arguments
	 * @param inst The Instrumentation instance
	 */
	public static void agentmain(String agentArgs, Instrumentation inst) {
		premain(agentArgs, inst);
	}
	
	/**
	 * The helios jagent entry point
	 * @param agentArgs The agent bootstrap arguments
	 * @param inst The Instrumentation instance
	 * @param codahaleNode The configuration node for codahale
	 */
	public static void heliosBoot(String agentArgs, Instrumentation inst, Node codahaleNode) {
		premain(agentArgs, inst);
		//packageNames
		Node packageNode = XMLHelper.getChildNodeByName(codahaleNode, "packages", false);
		if(packageNode!=null) {
			String ps = XMLHelper.getNodeTextValue(packageNode);
			for(String s: ps.split(",")) {
				if(s.trim().isEmpty()) continue;
				packageNames.add(s.trim());
			}
		}
		codahaleTransformer = new CodahaleClassTransformer(packageNames);
		if(XMLHelper.getChildNodeByName(codahaleNode, "annotations", false)!=null) {
			codahaleTransformer = new CodahaleClassTransformer(packageNames);
			instrumentation.addTransformer(codahaleTransformer, instrumentation.isRetransformClassesSupported());
			log("CodahaleClassTransformer Installed");
		}
		
		//HeliosReporter reporter = new HeliosReporter(Metrics.defaultRegistry(), MetricPredicate.ALL, "helios");
		HeliosReporter.enable(Metrics.defaultRegistry(), 15000, TimeUnit.MILLISECONDS, MetricPredicate.ALL); 
		//Metrics.defaultRegistry().addListener(reporter);
		
		
		
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

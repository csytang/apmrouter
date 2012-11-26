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
package org.helios.apmrouter;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.helios.apmrouter.spring.ctx.ApplicationContextService;
import org.helios.apmrouter.util.io.ConfigurableFileExtensionFilter;
import org.helios.apmrouter.util.io.RecursiveDirectorySearch;
import org.springframework.context.support.GenericXmlApplicationContext;

/**
 * <p>Title: APMRouter</p>
 * <p>Description: The main server bootstrap class.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.APMRouter</code></p>
 */

public class APMRouter {
	/** Static class logger */
	private static final Logger LOG = Logger.getLogger(APMRouter.class);

	/** The extension pattern for apmrouter config xml files */
	public static final String APM_FILE_FILTER = ".apmrouter.xml";
	/** The root application context display name */
	public static final String ROOT_DISPLAY_NAME = "APMRouterRootAppCtx";
	
	/** The booted spring context */
	private static GenericXmlApplicationContext appContext = null;
	/** The main thread, when interrupted shuts down the server */
	private static Thread MAIN_THREAD = null;
	
	/**
	 * Boots the APMRouter server
	 * @param args The spring xml configuration file directories, space separated
	 */
	public static void main(String[] args) {
		LOG.info("\n\t\t*************************\n\t\tAPMRouter v. " + APMRouter.class.getPackage().getImplementationVersion() + "\n\t\t*************************\n");
		try {
			URL log4jUrl = APMRouter.class.getClassLoader().getResource("log4j.xml");
			if(log4jUrl!=null) {
				System.out.println("Loading log4j config from [" + log4jUrl + "]");
				DOMConfigurator.configureAndWatch(log4jUrl.getFile(), 5000);
			} else {
				File log4jFile = new File("./log4j.xml");
				if(log4jFile.canRead()) {
					log4jUrl = log4jFile.toURI().toURL(); 
					System.out.println("Loading log4j config from [" + log4jFile.getAbsolutePath() + "]");					
				} else {
					System.out.println("Log4j Config Not Found. Yer on yer own");
				}
			}
			String confDir = null;
			Set<String> confDirs = new HashSet<String>();
			if(args.length<1) {
				confDir = "./src/test/resources/server";
				File dir = new File(confDir);
				if(!dir.exists() || !dir.isDirectory()) {
					LOG.error("No conf directory specified and DEV mode directory [" + confDir + "] does not exist. Exiting...");
					return;
				}
			} else {				
				Set<String> badDirs = new HashSet<String>();
				for(String d: args) {
					File dir = new File(d);
					if(!dir.exists() || !dir.isDirectory()) {
						badDirs.add(d);
					} else {
						confDirs.add(d);
					}
				}
				if(!badDirs.isEmpty()) {
					LOG.warn("These directories were not found " + badDirs);
				}
				if(confDirs.isEmpty()) {
					LOG.error("No conf directories found. Exiting..." + badDirs);
					return;
				}
						
			}
			String[] configFiles = findConfig(confDirs.toArray(new String[confDirs.size()]));
			if(configFiles==null || configFiles.length<1) {
				configFiles = findConfig(args);
			}
			if(configFiles==null || configFiles.length<1) {
				LOG.warn("Found no files matching [" + APM_FILE_FILTER + "]. Cannot start APMRouter. Bye.");
				return;
			}
			LOG.info("Located [" + configFiles.length + "] Configuration Files");
			if(LOG.isDebugEnabled()) {
				StringBuilder b = new StringBuilder();
				for(String s: configFiles) {
					b.append("\n\t").append(s);
				}
				LOG.debug("Config Files:" + b.toString());
			}
			LOG.info("Starting...");
			if(log4jUrl!=null) {
				System.out.println("Loading log4j config from [" + log4jUrl + "]");
			}
			appContext = new GenericXmlApplicationContext();
			appContext.setDisplayName(ROOT_DISPLAY_NAME);
			appContext.load(configFiles);
			ApplicationContextService.register(appContext);
			appContext.refresh();			
			LOG.info("Started");
			MAIN_THREAD = Thread.currentThread();
			
			Runtime.getRuntime().addShutdownHook(new Thread(){
				public void run() {
					MAIN_THREAD.interrupt();
				}
			});
			try {
				//Thread.currentThread().join();
				BufferedReader d = new BufferedReader(new InputStreamReader(System.in));
				while(true) {
					String line = d.readLine();
					if(line!=null) {
						if("exit".equals(line.trim().toLowerCase())) {
							break;
						}
					}
				}
				LOG.info("Stopping...");
				appContext.stop();
				LOG.info("APMRouter Stopped. Bye.");
				System.exit(-1);				
			} catch (Exception e) {
				LOG.info("Stopping...");
				appContext.stop();
				LOG.info("APMRouter Stopped. Bye.");
				System.exit(-1);
			}
		} catch (Exception e) {
			LOG.error("Sorry, APMRouter could not be started", e);
			System.exit(-1);
		}

	}
	
	/**
	 * Searches for apm config files in the specified location
	 * @return all located config files
	 */
	private static String[] findConfig(String...directories) {
		Set<String> files = new HashSet<String>();
		for(String dir: directories) {
			if(dir==null || dir.trim().isEmpty()) continue;
			File d = new File(dir.trim());
			if(d.exists() && d.isDirectory()) {
				Collections.addAll(files, RecursiveDirectorySearch.searchDirectories(new ConfigurableFileExtensionFilter(false, APM_FILE_FILTER), d.getAbsolutePath()));
			}
		}
		return files.toArray(new String[files.size()]);
	}

}

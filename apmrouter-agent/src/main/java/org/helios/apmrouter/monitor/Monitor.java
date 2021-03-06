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
package org.helios.apmrouter.monitor;

import java.util.Properties;

/**
 * <p>Title: Monitor</p>
 * <p>Description: Defines the base spec for a scheduled monitor</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.monitor.Monitor</code></p>
 */

public interface Monitor {
	/**
	 * Directs a monitor to execute it's collection and trace
	 */
	public void collect();
	
	/**
	 * Returns the collection period in ms.
	 * @return the collection period in ms.
	 */
	public long getCollectPeriod();
	
	/**
	 * Sets the configuration properties on this monitor
	 * @param p The configuration properties
	 */
	public void setProperties(Properties p);	
	
	/**
	 * Sets the collection period in ms.
	 * @param period the collection period in ms.
	 */
	public void setCollectPeriod(long period);
	
	
	/**
	 * Starts scheduled executions for this monitor
	 */
	public void startMonitor();
	
	/**
	 * Schedules a delayed start for scheduled executions for this monitor
	 * @param seconds The number of seconds to delay the start for
	 */
	public void startMonitor(long seconds);
	
	
	/**
	 * Stops scheduled executions for this monitor
	 */
	public void stopMonitor();
}

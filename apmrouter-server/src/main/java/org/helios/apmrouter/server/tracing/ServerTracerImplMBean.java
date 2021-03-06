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
package org.helios.apmrouter.server.tracing;

import java.util.Date;

import org.helios.apmrouter.trace.MetricSubmitter;

/**
 * <p>Title: ServerTracerImplMBean</p>
 * <p>Description: JMX interface for exposing virtual agent management interfaces</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.tracing.ServerTracerImplMBean</code></p>
 */

public interface ServerTracerImplMBean extends MetricSubmitter {
	/**
	 * Returns the virtual agent's host
	 * @return the virtual agent's host
	 */
	public String getHost();
	/**
	 * Returns the virtual agent's agent name
	 * @return the virtual agent's agent name
	 */
	public String getAgent();
	
	/**
	 * Touches the virtual agent's keep-alive timestamp
	 */
	public void touch();
	
	/**
	 * Expires this virtual agent
	 */
	public void expire();
	
	/**
	 * Returns the assigned virtual agent serial
	 * @return the assigned virtual agent serial
	 */
	public long getSerial();
	
	/**
	 * Returns the number of ms. until expiry
	 * @return the imminent expiry time unless touched in ms.
	 */
	public long getTimeToExpiry();
	
	/**
	 * Returns the last touch timestamp for this virtual agent
	 * @return the last touch timestamp for this virtual agent
	 */
	public long getLastTouchTimestamp();
	
	/**
	 * Returns the last touch date for this virtual agent
	 * @return the last touch date for this virtual agent
	 */
	public Date getLastTouchDate();
}

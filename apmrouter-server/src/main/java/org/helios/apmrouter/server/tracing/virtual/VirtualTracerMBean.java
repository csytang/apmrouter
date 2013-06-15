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
package org.helios.apmrouter.server.tracing.virtual;

import java.util.Date;

/**
 * <p>Title: VirtualTracerMBean</p>
 * <p>Description: MBean interface for {@link VirtualTracer}s</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.tracing.VirtualTracerMBean</code></p>
 */

public interface VirtualTracerMBean  {
	
	/** The virtual tracer availability metric name */
	public static final CharSequence AVAIL_METRIC_NAME = "Availability";
	/** The virtual tracer namespace prefix */
	public static final CharSequence TRACER_NAMESPACE = "virtualtracers";


	/**
	 * Returns the activity highwater timestamp
	 * @return the activity highwater timestamp
	 */
	public long getLastTouchTimestamp();
	
	/**
	 * Returns the number of sent metrics
	 * @return the number of sent metrics
	 */
	public long getSentMetrics();
	

	/**
	 * Returns the activity highwater date
	 * @return the activity highwater date
	 */
	public Date getLastTouchDate();

	/**
	 * Returns this tracer's serial number
	 * @return this tracer's serial number
	 */
	public long getSerial();

	/**
	 * Touches the activity highwater timestamp
	 */
	public void touch();

	/**
	 * Expires this virtual tracer.
	 */
	public void expire();
	
	/**
	 * Invalidates this virtual tracer
	 */
	public void invalidate();	

	/**
	 * Returns the time until this virtual tracer transitions to {@link VirtualState#SOFTDOWN}, unless there is additional activity
	 * @return the time until this virtual tracer tracer transitions to {@link VirtualState#SOFTDOWN}, or -1 if it is already DOWN.
	 */
	public long getTimeToSoftDown();
	
	/**
	 * Returns the time until this virtual tracer transitions to {@link VirtualState#HARDDOWN}, unless there is additional activity
	 * @return the time until this virtual tracer tracer transitions to {@link VirtualState#HARDDOWN}, or -1 if it is already DOWN.
	 */
	public long getTimeToHardDown();
	

	/**
	 * Returns the tracer's current state name
	 * @return the tracer's current state name
	 */
	public String getStateName();
	
	/**
	 * Returns the host for the tracer's parent virtual agent
	 * @return the host for the tracer's parent virtual agent
	 */
	public String getHost();
	
	/**
	 * Returns the agent name for the tracer's parent virtual agent
	 * @return the agent name for the tracer's parent virtual agent
	 */
	public String getAgent();
	
	/**
	 * Returns the tracer name
	 * @return the tracer name
	 */
	public String getName();
	
	/**
	 * Returns the unique key for this virtual tracer within the parent agent
	 * @return the unique key for this virtual tracer within the parent agent
	 */
	public String getKey();
	
	/**
	 * Returns the designated soft down period, 
	 * meaning if there has been no activity for this period, the tracer is marked soft down
	 * @return the softDownPeriod the designated soft down period 
	 */
	public long getSoftDownPeriod();

	/**
	 * Sets the designated soft down period,
	 * @param softDownPeriod the softDownPeriod to set
	 */
	public void setSoftDownPeriod(long softDownPeriod);

	/**
	 * Returns the designated hard down period, 
	 * meaning if there has been no activity for this period, the tracer is marked hard down
	 * @return the hardDownPeriod
	 */
	public long getHardDownPeriod();

	/**
	 * Sets the designated hard down period
	 * @param hardDownPeriod the hardDownPeriod to set
	 */
	public void setHardDownPeriod(long hardDownPeriod);

}
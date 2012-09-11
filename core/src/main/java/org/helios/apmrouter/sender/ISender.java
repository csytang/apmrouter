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
package org.helios.apmrouter.sender;

import java.net.URI;
import java.util.Collection;

import org.helios.apmrouter.metric.IMetric;

/**
 * <p>Title: ISender</p>
 * <p>Description: Defines a metric sender that dispatches closed metrics to the apm-router</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.sender.ISender</code></p>
 */

public interface ISender {
	/**
	 * Queues the passed metrics for transmission to the configured endpoint
	 * @param metrics the metrics to queue
	 */
	public void send(IMetric...metrics);

	/**
	 * Directly transmits the passed metrics to the configured endpoint
	 * @param metrics the metrics to send
	 */
	public void sendDirect(IMetric...metrics);
	
	/**
	 * Directly transmits the passed metrics to the configured endpoint
	 * @param metrics the metrics to send
	 */
	public void sendDirect(Collection<IMetric[]> metrics);
	
	/**
	 * Returns the total number of sent metrics on this sender
	 * @return the total number of sent metrics on this sender
	 */
	public long getSentMetrics();

	/**
	 * Returns the total number of dropped metrics on this sender
	 * @return the total number of dropped metrics on this sender
	 */	
	public long getDroppedMetrics();

	
	/**
	 * Returns the URI that this sender is sending to
	 * @return the URI that this sender is sending to
	 */
	public URI getURI();
}

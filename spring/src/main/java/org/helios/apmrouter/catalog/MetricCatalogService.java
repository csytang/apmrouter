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
package org.helios.apmrouter.catalog;

import java.util.Map;

import org.helios.apmrouter.metric.catalog.IDelegateMetric;

/**
 * <p>Title: MetricCatalogService</p>
 * <p>Description: Defines a metric catalog service, the exclusive and canonical repository for metrics.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.catalog.MetricCatalogService</code></p>
 */

public interface MetricCatalogService {
	/**
	 * Returns the unique identifier for a metric
	 * @param token The metric ID which may be -1 meaning the metric does not exist yet
	 * @param host The host name
	 * @param agent The agent name
	 * @param typeId The metric type
	 * @param namespace The metric namespace
	 * @param name The metric name
	 * @return the assigned ID
	 */
	public long getID(long token, String host, String agent, int typeId, String namespace, String name);
	
	/**
	 * Called when an agent connects or disconnects (or times out)
	 * @param connected true for a connect, false for a disconnect
	 * @param host The host name
	 * @param ip The host IP address
	 * @param agent The agent name
	 * @param agentURI The agent's listening URI
	 * @return the number of agents connected from the passed host after this operation completes
	 */
	public int hostAgentState(boolean connected, String host, String ip, String agent, String agentURI);
	
	/**
	 * Returns the delegate metric for the passed token
	 * @param token the token
	 * @return a delegate metric ID
	 */
	public IDelegateMetric getMetricID(long token);
	
	/**
	 * Indicates if the metric catalog is real time 
	 * @return true if the metric catalog is real time , false otherwise
	 */
	public boolean isRealtime();

	/**
	 * Sets the realtime attribute of the metric catalog
	 * @param realtime true for a realtime metric catalog, false otherwise
	 */
	public void setRealtime(boolean realtime); 	
	
	/**
	 * Lists registered hosts
	 * @param onlineOnly If true, only lists online hosts
	 * @return A map of host names keyed by host ID
	 */
	public Map<Integer, String> listHosts(boolean onlineOnly);
}

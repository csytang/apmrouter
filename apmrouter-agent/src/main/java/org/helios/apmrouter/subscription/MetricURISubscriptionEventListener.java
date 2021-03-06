/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2013, Helios Development Group and individual contributors
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
package org.helios.apmrouter.subscription;

/**
 * <p>Title: MetricURISubscriptionEventListener</p>
 * <p>Description: Defines a listener that can be registered with the agent to receive events emitted from metric URI subscriptions</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.subscription.MetricURISubscriptionEventListener</code></p>
 */

public interface MetricURISubscriptionEventListener {
	/**
	 * Callback when the agent receives a new metric event
	 * @param newMetric The new metric event
	 */
	public void onNewMetric(Object newMetric);
	
	/**
	 * Callback when the agent is notified of a metric state change that triggered an addition to this subscriber's subscription 
	 * @param metric The metric that entered the subscription
	 */
	public void onMetricStateChangeEntry(Object metric);
	
	/**
	 * Callback when the agent is notified of a metric state change on a metric already in this subscriber's subscription 
	 * @param metric The metric changed state in the subscription
	 */
	public void onMetricStateChange(Object metric);
	
	
	/**
	 * Callback when the agent is notified of a metric state change that triggered a removal from this subscriber's subscription 
	 * @param metric The metric that exited the subscription
	 */
	public void onMetricStateChangeExit(Object metric);
	
	/**
	 * A published metric data event for a metric this subscription is subscribed to
	 * @param metricData The incoming metric data
	 */
	public void onMetricData(Object metricData);
	
}

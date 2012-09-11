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
package org.helios.apmrouter.trace;

import java.io.Serializable;

import org.helios.apmrouter.metric.ICEMetric;
import org.helios.apmrouter.metric.MetricType;

/**
 * <p>Title: ITracer</p>
 * <p>Description: Defines a class that create an {@link ICEMetric} and sends it to the configured endpoint</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.trace.ITracer</code></p>
 */

public interface ITracer {
	/**
	 * Creates and sends an {@link ICEMetric} 
	 * @param value The value of the metric
	 * @param name The name of the metric
	 * @param type The type of the metric
	 * @param namespace The optional namespace of the metric
	 * @return the created {@link ICEMetric} 
	 */
	public ICEMetric trace(Object value, CharSequence name, MetricType type, CharSequence...namespace);
	
//	/**
//	 * Creates and sends an {@link ICEMetric} 
//	 * @param value The value of the metric
//	 * @param name The name of the metric
//	 * @param type The type of the metric
//	 * @param namespace The optional namespace of the metric
//	 * @return the created {@link ICEMetric} 
//	 */
//	public ICEMetric trace(Number value, CharSequence name, MetricType type, CharSequence...namespace);
//	
//	/**
//	 * Creates and sends an {@link ICEMetric} 
//	 * @param value The value of the metric
//	 * @param name The name of the metric
//	 * @param type The type of the metric
//	 * @param namespace The optional namespace of the metric
//	 * @return the created {@link ICEMetric} 
//	 */
//	public ICEMetric trace(long value, CharSequence name, MetricType type, CharSequence...namespace);
	
	/**
	 * Traces a long type
	 * @param value The long value
	 * @param name The name of the metric
	 * @param namespace The optional namespace of the metric
	 * @return the created {@link ICEMetric}
	 */
	public ICEMetric traceLong(long value, CharSequence name, CharSequence...namespace);
	
	/**
	 * Traces a delta long type
	 * @param value The long value
	 * @param name The name of the metric
	 * @param namespace The optional namespace of the metric
	 * @return the created {@link ICEMetric}
	 */
	public ICEMetric traceDelta(long value, CharSequence name, CharSequence...namespace);
	
	/**
	 * Traces a string type
	 * @param value The string value
	 * @param name The name of the metric
	 * @param namespace The optional namespace of the metric
	 * @return the created {@link ICEMetric}
	 */
	public ICEMetric traceString(CharSequence value, CharSequence name, CharSequence...namespace);
	
	/**
	 * Traces an error long type
	 * @param value The throwable value
	 * @param name The name of the metric
	 * @param namespace The optional namespace of the metric
	 * @return the created {@link ICEMetric}
	 */
	public ICEMetric traceError(Throwable value, CharSequence name, CharSequence...namespace);
	
	/**
	 * Traces a blob long type
	 * @param value The serializable value
	 * @param name The name of the metric
	 * @param namespace The optional namespace of the metric
	 * @return the created {@link ICEMetric}
	 */
	public ICEMetric traceBlob(Serializable value, CharSequence name, CharSequence...namespace);
	
	/**
	 * Returns the originating host of the metrics created by this tracer
	 * @return the originating host of the metrics created by this tracer
	 */
	public String getHost();

	/**
	 * Returns the originating agent of the metrics created by this tracer
	 * @return the originating agent of the metrics created by this tracer
	 */
	public String getAgent();
	
	/**
	 * Returns the total number of sent metrics on this tracer's sender
	 * @return the total number of sent metrics on this tracer's sender
	 */
	public long getSentMetrics();

	/**
	 * Returns the total number of dropped metrics on this tracer's sender
	 * @return the total number of dropped metrics on this tracer's sender
	 */	
	public long getDroppedMetrics();
	
	
}

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
package org.helios.apmrouter.metric;

import static org.helios.apmrouter.util.Methods.nvl;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.helios.apmrouter.metric.catalog.ICEMetricCatalog;
import org.helios.apmrouter.metric.catalog.IDelegateMetric;
import org.helios.apmrouter.util.StringHelper;


/**
 * <p>Title: ICEMetric</p>
 * <p>Description: The public metricId implementation. ICEMetric wraps one {@link IDelegateMetric} and one {link ICEMetricValue}. One instance is created per trace operation.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.metric.ICEMetric</code></p>
 */

public class ICEMetric implements IMetric {
	/** The value for this metricId */
	protected final ICEMetricValue value;
	/** The metricId name that this instance represents*/
	protected final IDelegateMetric metricId;
	

	
	/**
	 * Creates a new ICEMetric
	 * @param value The value for this metricId
	 * @param metricId The metricId identifier
	 */
	private ICEMetric(ICEMetricValue value, IDelegateMetric metric) {
		this.value = value;
		this.metricId = metric;
	}
	
	/**
	 * Creates a new ICEMetric
	 * @param value The metric value
	 * @param host The host name
	 * @param agent The agent name 
	 * @param name The metric name
	 * @param type The metric type
	 * @param namespace An optional array of namespace entries
	 * @return an ICEMetric
	 * @throws RuntimeException Thrown if any of the initial metricId parameters are invalid
	 */
	public static ICEMetric trace(Object value, String host, String agent, CharSequence name, MetricType type, CharSequence...namespace) {
		try {
			return new ICEMetric(
				type.write(value), 
				ICEMetricCatalog.getInstance().get(host, agent, name, type, namespace)
			);
		} catch (Exception e) {
			throw new RuntimeException("Failed to create ICEMetricBuilder", e);
		}
	}
	
	/**
	 * Creates a new ICEMetric
	 * @param value The metric value
	 * @param host The host name
	 * @param agent The agent name 
	 * @param name The metric name
	 * @param type The metric type
	 * @param namespace An optional array of namespace entries
	 * @return an ICEMetric
	 * @throws RuntimeException Thrown if any of the initial metricId parameters are invalid
	 */
	public static ICEMetric trace(Number value, String host, String agent, CharSequence name, MetricType type, CharSequence...namespace) {
		try {
			return new ICEMetric(
				new ICEMetricValue(type, value.longValue()),
				ICEMetricCatalog.getInstance().get(host, agent, name, type, namespace)
			);
		} catch (Exception e) {
			throw new RuntimeException("Failed to create ICEMetricBuilder", e);
		}
	}
	
	/**
	 * Creates a new ICEMetric
	 * @param value The metric value
	 * @param host The host name
	 * @param agent The agent name 
	 * @param name The metric name
	 * @param type The metric type
	 * @param namespace An optional array of namespace entries
	 * @return an ICEMetric
	 * @throws RuntimeException Thrown if any of the initial metricId parameters are invalid
	 */
	public static ICEMetric trace(long value, String host, String agent, CharSequence name, MetricType type, CharSequence...namespace) {
		try {
			return new ICEMetric(
				new ICEMetricValue(type, value),
				ICEMetricCatalog.getInstance().get(host, agent, name, type, namespace)
			);
		} catch (Exception e) {
			throw new RuntimeException("Failed to create ICEMetricBuilder", e);
		}
	}
	


	/**
	 * Returns the value object for this metricId
	 * @return the value
	 */
	ICEMetricValue getICEMetricValue() {
		return value;
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.IMetric#getHost()
	 */
	@Override
	public String getHost() {
		return metricId.getHost();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.IMetric#getAgent()
	 */
	@Override
	public String getAgent() {
		return metricId.getAgent();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.IMetric#isFlat()
	 */
	@Override
	public boolean isFlat() {
		return metricId.isFlat();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.IMetric#isMapped()
	 */
	@Override
	public boolean isMapped() {
		return metricId.isMapped();
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.IMetric#getNamespace()
	 */
	@Override
	public String[] getNamespace() {
		return metricId.getNamespace();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.IMetric#getName()
	 */
	@Override
	public String getName() {
		return metricId.getName();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.IMetric#getTime()
	 */
	@Override
	public long getTime() {
		return value.getTime();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.IMetric#getType()
	 */
	@Override
	public MetricType getType() {
		return metricId.getType();
	}

	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.IMetric#getFQN()
	 */
	@Override
	public String getFQN() {
		return String.format(FQN_FORMAT, getHost(), getAgent(), getNamespaceF(), getName());
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.IMetric#getNamespaceF()
	 */
	@Override
	public String getNamespaceF() {
		String[] ns = getNamespace();
		if(ns.length==0) return "";
		return StringHelper.fastConcatAndDelim(NSDELIM, ns);

	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.IMetric#getNamespace(int)
	 */
	@Override
	public String getNamespace(int index) {
		return getNamespace()[index];
	}
	
	/**
	 * Returns the namespace element at the provided index.
	 * Throws a RuntimeException if the metric is not mapped
	 * @param index The namespace index
	 * @return a namespace element
	 */
	public String getNamespace(CharSequence index) {
		if(metricId.isFlat()) throw new RuntimeException("Requesting named index namespace on non-mapped metric [" + getFQN() + "]", new Throwable());
		String[] ns = metricId.getNamespace();
		if(ns==null || ns.length<1) throw new RuntimeException("Requesting named index namespace on zero sized namespace in metric [" + getFQN() + "]", new Throwable());
		String key = nvl(index, "Mapped Namespace Index").toString().trim();
		
		for(String s: ns) {
			int eq = s.indexOf('=');
			if(key.equals(s.substring(0, eq))) {
				return s.substring(eq+1);
			}
		}
		return null;		
	}
	
	/**
	 * Returns the namespace as a map.
	 * Throws a RuntimeException if the metric is not mapped
	 * @return a map representing the mapped namespace of this metric
	 */
	public Map<String, String> getNamespaceMap() {
		if(metricId.isFlat()) throw new RuntimeException("Requesting named index namespace on non-mapped metric [" + getFQN() + "]", new Throwable());
		String[] ns = metricId.getNamespace();
		if(ns==null || ns.length<1) return Collections.emptyMap();
		Map<String, String> map = new LinkedHashMap<String, String>(ns.length);
		for(String s: ns) {
			int eq = s.indexOf('=');
			map.put(s.substring(0, eq), s.substring(eq+1));
		}		
		return map;
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.IMetric#getNamespaceSize()
	 */
	@Override
	public int getNamespaceSize() {
		return getNamespace().length;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.IMetric#getDate()
	 */
	@Override
	public Date getDate() {
		return value.getDate();
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.IMetric#getValue()
	 */
	@Override
	public Object getValue() {
		if(metricId.getType().isLong()) {
			return getLongValue();
		}
		return value.getValue();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.IMetric#getLongValue()
	 */
	@Override
	public long getLongValue() {
		if(!metricId.getType().isLong()) {
			throw new RuntimeException("The metric [" + getFQN() + "] is not a long type: [" + metricId.getType() + "]", new Throwable());
		}
		return value.getLongValue();
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ICEMetric [>");
		builder.append(getType());
		builder.append("<");
		builder.append(getFQN());
		builder.append("[");
		builder.append(getDate());
		builder.append("]:");
		builder.append(getValue());
		return builder.toString();
	}

	
}

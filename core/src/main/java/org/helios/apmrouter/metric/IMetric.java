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

import java.util.Date;
import java.util.Map;

/**
 * <p>Title: IMetric</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.metric.IMetric</code></p>
 */

public interface IMetric {
	
	/** The namespace delimiter */
	public static final String NSDELIM = "/";
	/** The name delimiter */
	public static final String NADELIM = ":";
	/** The timestamp start delimiter */
	public static final String TS_S_DELIM = "[";
	/** The timestamp end delimiter */
	public static final String TS_E_DELIM = "]";
	/** The value delimiter */
	public static final String VDELIM = "/";
	/** The mapped namespace pair delimiter */
	public static final String MDELIM = "=";

	/** The format for rendering a transmittable metricId */
	static final String TX_FORMAT = TS_S_DELIM + "%s" + TS_E_DELIM + "%s" + NSDELIM + "%s%s" + VDELIM + "%s" ;
	/** The format for rendering the fully qualified metricId name */
	static final String FQN_FORMAT = "%s" + NSDELIM + "%s%s" + NADELIM + "%s" ;
	

	/**
	 * Returns the host name that this metricId originated from
	 * @return the host name that this metricId originated from
	 */
	public abstract String getHost();

	/**
	 * The name of the agent that this metricId originated from
	 * @return the name of the agent that this metricId originated from
	 */
	public abstract String getAgent();

	/**
	 * Indicates if the metricId namespace is flat or mapped
	 * @return true if the metricId namespace is flat, false if it is mapped
	 */
	public abstract boolean isFlat();

	/**
	 * Indicates if the metricId namespace is flat or mapped
	 * @return true if the metricId namespace is mapped, false if it is flat
	 */
	public abstract boolean isMapped();

	/**
	 * The namespace of the metricId
	 * @return the namespace
	 */
	public abstract String[] getNamespace();

	/**
	 * Returns the metricId name
	 * @return the name
	 */
	public abstract String getName();

	/**
	 * Returns the metricId timestamp or -1 if no timestamp has been set
	 * @return the time
	 */
	public abstract long getTime();

	/**
	 * Returns the metricId type
	 * @return the type
	 */
	public abstract MetricType getType();

	/**
	 * Returns the fully qualified metricId name
	 * @return the fully qualified metricId name
	 */
	public abstract String getFQN();

	/**
	 * Returns the concatenated namespace
	 * @return the concatenated namespace
	 */
	public abstract String getNamespaceF();

	/**
	 * Returns the namespace element at the provided index
	 * @param index The namespace index
	 * @return a namespace element
	 */
	public abstract String getNamespace(int index);
	
	/**
	 * Returns the namespace element at the provided index.
	 * Throws a RuntimeException if the metric is not mapped
	 * @param index The namespace index
	 * @return a namespace element
	 */
	public abstract String getNamespace(CharSequence index);
	
	/**
	 * Returns the namespace as a map.
	 * Throws a RuntimeException if the metric is not mapped
	 * @return a map representing the mapped namespace of this metric
	 */
	public abstract Map<String, String> getNamespaceMap();

	/**
	 * Returns the number of elements in the namespace
	 * @return the number of elements in the namespace
	 */
	public abstract int getNamespaceSize();

	/**
	 * The timestamp of this metricId as a java date
	 * @return the date
	 */
	public abstract Date getDate();

	/**
	 * Returns the value of this metricId
	 * @return the value
	 */
	public abstract Object getValue();

	/**
	 * Returns the value as a long, or throws a RuntimeException if the type is not long based
	 * @return the long value
	 */
	public abstract long getLongValue();

}
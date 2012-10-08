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
package org.helios.apmrouter.catalog.jdbc.h2;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.HashSet;
import java.util.Set;

import javax.sql.DataSource;

import org.helios.apmrouter.catalog.MetricCatalogService;
import org.helios.apmrouter.collections.ConcurrentLongSlidingWindow;
import org.helios.apmrouter.collections.LongSlidingWindow;
import org.helios.apmrouter.metric.MetricType;
import org.helios.apmrouter.server.ServerComponentBean;
import org.helios.apmrouter.util.SystemClock;
import org.helios.apmrouter.util.SystemClock.ElapsedTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;




/**
 * <p>Title: H2JDBCMetricCatalog</p>
 * <p>Description: The H2 implementation of the {@link MetricCatalogService}. When realtime is set to true
 * host, agent and metric timestamps are kept realtime with respect to their <i>last seen</i> timestamp.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.catalog.jdbc.H2JDBCMetricCatalog</code></p>
 */

public class H2JDBCMetricCatalog extends ServerComponentBean implements MetricCatalogService {
	/** The h2 datasource */
	protected DataSource ds = null;
	/** Indicates if the metric catalog should be kept real time */
	protected boolean realtime = false;
	
	/** Sliding windows of catalog call elapsed times in ns. */
	protected final LongSlidingWindow elapsedTimesNs = new ConcurrentLongSlidingWindow(15);
	/** Sliding windows of catalog call elapsed times in ms. */
	protected final LongSlidingWindow elapsedTimesMs = new ConcurrentLongSlidingWindow(15); 
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#doStart()
	 */
	@Override
	public void doStart() throws Exception {
		Connection conn = null;
		PreparedStatement ps = null;
		try {
			conn = ds.getConnection();
			//MERGE INTO TEST KEY(ID) VALUES(2, 'World')
			ps = conn.prepareStatement("MERGE INTO TRACE_TYPE KEY(TYPE_ID) VALUES(?,?)");
			for(MetricType mt: MetricType.values()) {
				ps.setInt(1, mt.ordinal());
				ps.setString(2, mt.name());
				ps.addBatch();
			}
			ps.executeBatch();	
			ps.close();
			
		} catch (Exception e) {
			throw new RuntimeException("Failed to add metric types", e);
		} finally {
			try { ps.close(); } catch (Exception e) {}
			try { conn.close(); } catch (Exception e) {}
		}
		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.catalog.MetricCatalogService#getID(long, java.lang.String, java.lang.String, int, java.lang.String, java.lang.String)
	 */
	@Override
	public long getID(long token, String host, String agent, int typeId, String namespace, String name) {
		if(token!=-1 && !realtime) return 0;
		SystemClock.startTimer();
		incr("CallCount");		
		Connection conn = null;
		CallableStatement cs = null;
		try {
			conn = ds.getConnection();
			cs = realtime ? conn.prepareCall("? = CALL TOUCH(?,?,?,?,?,?)") : conn.prepareCall("? = CALL GET_ID(?,?,?,?,?,?)");
			cs.registerOutParameter(1, Types.NUMERIC);
			cs.setNull(1, Types.NULL);			
			cs.setLong(2, token);
			cs.setString(3, host);
			cs.setString(4, agent);
			cs.setInt(5, typeId);
			cs.setString(6, namespace);
			cs.setString(7, name);
			cs.execute();
			long id = cs.getLong(1);
			if(id!=0) {
				incr("AssignedMetricIDs");
			}
			ElapsedTime et = SystemClock.endTimer();
			elapsedTimesNs.insert(et.elapsedNs);
			elapsedTimesMs.insert(et.elapsedMs);			
			return id;
		} catch (Exception e) {
			error("Failed to get ID for [" , String.format("%s/%s%s:%s", host, agent, namespace, name) , "]", e);
			Throwable cause = e.getCause();
			if(cause!=null) cause.printStackTrace(System.err);
			//throw new RuntimeException("Failed to get ID", e);
			return 0;
		} finally {
			try { cs.close(); } catch (Exception e) {}
			try { conn.close(); } catch (Exception e) {}
		}
	}
	/**
	 * Sets the h2 datasource
	 * @param ds the h2 datasource
	 */
	@Autowired(required=true)
	@Qualifier("H2DataSource")
	public void setDs(DataSource ds) {
		this.ds = ds;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponent#getSupportedMetricNames()
	 */
	@Override
	public Set<String> getSupportedMetricNames() {
		Set<String> metrics = new HashSet<String>(super.getSupportedMetricNames());
		metrics.add("AssignedMetricIDs");
		metrics.add("CallCount");		
		return metrics;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponent#resetMetrics()
	 */
	@Override
	public void resetMetrics() {
		super.resetMetrics();
		elapsedTimesNs.clear();
		elapsedTimesMs.clear();
	}
	
	/**
	 * Returns the number of assigned metric IDs
	 * @return the number of assigned metric IDs
	 */
	@ManagedMetric(category="MetricCatalogService", metricType=org.springframework.jmx.support.MetricType.COUNTER, description="The number of assigned metric IDs")
	public long getAssignedMetricIDs() {
		return getMetricValue("AssignedMetricIDs");
	}	

	/**
	 * Returns the cumulative number of catalog calls
	 * @return the cumulative number of catalog calls
	 */
	@ManagedMetric(category="MetricCatalogService", metricType=org.springframework.jmx.support.MetricType.COUNTER, description="The cumulative number of catalog calls")
	public long getCallCount() {
		return getMetricValue("CallCount");
	}	
	
	/**
	 * Returns the sliding average elapsed time in ns. of the last 15 catalog calls
	 * @return the sliding average elapsed time in ns. of the last 15 catalog calls
	 */
	@ManagedMetric(category="MetricCatalogService", metricType=org.springframework.jmx.support.MetricType.GAUGE, description="The sliding average elapsed time in ns. of the last 15 catalog calls")
	public long getAverageCallTimeNs() {
		return elapsedTimesNs.avg();
	}
	
	/**
	 * Returns the sliding average elapsed time in ms. of the last 15 catalog calls
	 * @return the sliding average elapsed time in ms. of the last 15 catalog calls
	 */
	@ManagedMetric(category="MetricCatalogService", metricType=org.springframework.jmx.support.MetricType.GAUGE, description="The sliding average elapsed time in ms. of the last 15 catalog calls")
	public long getAverageCallTimeMs() {
		return elapsedTimesMs.avg();
	}	
	

	/**
	 * Indicates if the metric catalog is real time 
	 * @return true if the metric catalog is real time , false otherwise
	 */
	@Override
	@ManagedAttribute(description="Indicates if the metric catalog is real time ")
	public boolean isRealtime() {
		return realtime;
	}

	/**
	 * Sets the realtime attribute of the metric catalog
	 * @param realtime true for a realtime metric catalog, false otherwise
	 */
	@Override
	@ManagedAttribute(description="Indicates if the metric catalog is real time ")
	public void setRealtime(boolean realtime) {
		this.realtime = realtime;
	}
}

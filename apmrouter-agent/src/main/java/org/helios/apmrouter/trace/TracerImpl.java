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

import static org.helios.apmrouter.util.Methods.nvl;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.helios.apmrouter.metric.ICEMetric;
import org.helios.apmrouter.metric.MetricType;
import org.helios.apmrouter.metric.catalog.ICEMetricCatalog;
import org.snmp4j.PDU;

/**
 * <p>Title: TracerImpl</p>
 * <p>Description: The main public tracing interface</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.trace.TracerImpl</code></p>
 */

public class TracerImpl implements ITracer {
	/** The originating host of the metrics created by this tracer */
	protected final String host;
	/** The originating agent of the metrics created by this tracer */
	protected final String agent;
	/** The collection submitter */
	protected final MetricSubmitter submitter; 
	
	
	

	/**
	 * Creates a new TracerImpl
	 * @param host The originating host of the metrics created by this tracer
	 * @param agent The originating agent of the metrics created by this tracer
	 * @param submitter the collection submitter the tracer writes metrics to
	 */
	public TracerImpl(String host, String agent, MetricSubmitter submitter) {
		this.host = nvl(host, "HostName");
		this.agent = nvl(agent, "Agent Name");
		this.submitter = submitter;
	}
	
	/**
	 * Coerces a long from an object
	 * @param value The value to coerce
	 * @return The coerced long
	 */
	protected static long coerce(Object value) {
		long actual = 0;
		if(value==null) {
			actual = 0;
		} else if(value instanceof Long) {
			actual = (Long)value;
		} else if(value instanceof Number) {
			actual = ((Number)value).longValue();
		} else {
			try { actual = new Double(value.toString().trim()).longValue(); } catch (Exception e) {
				actual = 0;
			}
		}
		return actual;
	}

	/**
	 * Trace operation
	 * @param value The value of the metric
	 * @param name The name of the metric
	 * @param type The type of the metric
	 * @param namespace The optional namespace of the metric
	 * @return the created {@link ICEMetric} 
	 */
	@Override
	public ICEMetric trace(Object value, CharSequence name, MetricType type, CharSequence... namespace) {
		ICEMetric metric = null;
		try {
			if(type.isLong()) {
				if(type.isDelta()) {
					Long delta = ICEMetricCatalog.getInstance().getDelta(coerce(value), host, agent, name, namespace);
					if(delta==null) return null;
					metric = ICEMetric.trace(delta.longValue(), host, agent, name, type, namespace);
				} else {
					metric = ICEMetric.trace(coerce(value), host, agent, name, type, namespace);
				}
			} else {
				metric = ICEMetric.trace(value, host, agent, name, type, namespace);
			}
			if(TXContext.hasContext()) {
				metric.attachTXContext(TXContext.rollContext());
			}
			submitter.submit(metric);
			return metric;
		} catch (Throwable t) {
			t.printStackTrace(System.err);
			return null;
		}
	}
	
	/**
	 * Confirmed Trace operation
	 * @param timeout The timeout period to wait for a confirm
	 * @param unit The unit of the timeout 
	 * @param value The value of the metric
	 * @param name The name of the metric
	 * @param type The type of the metric
	 * @param namespace The optional namespace of the metric
	 * @return the created {@link ICEMetric} 
	 */
	protected ICEMetric _trace(long timeout, TimeUnit unit, Object value, CharSequence name, MetricType type, CharSequence... namespace) {
		ICEMetric metric = null;
		try {
			if(type.isLong()) {
				if(type.isDelta()) {
					Long delta = ICEMetricCatalog.getInstance().getDelta(coerce(value), host, agent, name, namespace);
					if(delta==null) return null;
					metric = ICEMetric.trace(delta.longValue(), host, agent, name, type, namespace);
				} else {
					metric = ICEMetric.trace(coerce(value), host, agent, name, type, namespace);
				}
			} else {
				metric = ICEMetric.trace(value, host, agent, name, type, namespace);
			}
			if(TXContext.hasContext()) {
				metric.attachTXContext(TXContext.rollContext());
			}			
			submitter.submitDirect(metric, TimeUnit.MILLISECONDS.convert(timeout, unit));
			return metric;
		} catch (Throwable t) {
			if(t.getClass().equals(TimeoutException.class)) {
				throw new RuntimeException("Confirmed Trace Timed Out", t);
			}
			t.printStackTrace(System.err);
			return null;
		}
	}

	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#traceDirect(java.lang.Object, java.lang.CharSequence, org.helios.apmrouter.metric.MetricType, java.lang.CharSequence[])
	 */
	@Override
	public ICEMetric traceDirect(long timeout, TimeUnit unit, Object value, CharSequence name, MetricType type, CharSequence...namespace) {
		return _trace(timeout, unit,  value, name, type, namespace);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#traceDirect(java.lang.Object, java.lang.CharSequence, org.helios.apmrouter.metric.MetricType, java.lang.CharSequence[])
	 */
	@Override
	public ICEMetric traceDirect(Object value, CharSequence name, MetricType type, CharSequence...namespace) {
		return traceDirect(TracerFactory.DIRECT_TIMEOUT, TimeUnit.MILLISECONDS, value, name, type, namespace);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#tracePDUDirect(org.snmp4j.PDU, java.lang.CharSequence, java.lang.CharSequence[])
	 */
	public ICEMetric tracePDUDirect(PDU pdu, CharSequence name, CharSequence...namespace) {
		return traceDirect(TracerFactory.DIRECT_TIMEOUT, TimeUnit.MILLISECONDS, pdu, name, MetricType.PDU, namespace);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#traceBlobDirect(java.io.Serializable, java.lang.CharSequence, java.lang.CharSequence[])
	 */
	public ICEMetric traceBlobDirect(Serializable value, CharSequence name, CharSequence...namespace) {
		return traceDirect(TracerFactory.DIRECT_TIMEOUT, TimeUnit.MILLISECONDS, value, name, MetricType.BLOB, namespace);
	}

//	/**
//	 * {@inheritDoc}
//	 * @see org.helios.apmrouter.trace.ITracer#trace(java.lang.Number, java.lang.CharSequence, org.helios.apmrouter.metric.MetricType, java.lang.CharSequence[])
//	 */
//	@Override
//	public ICEMetric trace(Number value, CharSequence name, MetricType type, CharSequence... namespace) {
//		if(value==null) return null;
//		try {
//			if(type.equals(MetricType.DELTA)) {
//				Long delta = ICEMetricCatalog.getInstance().getDelta(value.longValue(), host, agent, name, namespace);
//				if(delta==null) return null;
//				return ICEMetric.trace(value, host, agent, name, type, namespace);
//			}
//			return ICEMetric.trace(value.longValue(), host, agent, name, type, namespace);			
//		} catch (Throwable t) {
//			return null;
//		}		
//	}

//	/**
//	 * {@inheritDoc}
//	 * @see org.helios.apmrouter.trace.ITracer#trace(long, java.lang.CharSequence, org.helios.apmrouter.metric.MetricType, java.lang.CharSequence[])
//	 */
//	@Override
//	public ICEMetric trace(long value, CharSequence name, MetricType type,CharSequence... namespace) {
//		try {
//			if(type.equals(MetricType.DELTA)) {
//				Long delta = ICEMetricCatalog.getInstance().getDelta(value, host, agent, name, namespace);
//				if(delta==null) return null;
//				return ICEMetric.trace(value, host, agent, name, type, namespace);
//			}
//			return ICEMetric.trace(value, host, agent, name, type, namespace);			
//		} catch (Throwable t) {
//			return null;
//		}		
//	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#traceCounter(long, java.lang.CharSequence, java.lang.CharSequence[])
	 */
	@Override
	public ICEMetric traceCounter(long value, CharSequence name, CharSequence... namespace) {
		try {				
			ICEMetric metric = ICEMetric.trace(value, host, agent, name, MetricType.LONG_COUNTER, namespace);
			submitter.submit(metric);
			return metric;
		} catch (Throwable t) {
			t.printStackTrace(System.err);
			return null;
		}		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#traceGauge(long, java.lang.CharSequence, java.lang.CharSequence[])
	 */
	@Override
	public ICEMetric traceGauge(long value, CharSequence name, CharSequence... namespace) {
		try {				
			ICEMetric metric = ICEMetric.trace(value, host, agent, name, MetricType.LONG_GAUGE, namespace);
			submitter.submit(metric);
			return metric;
		} catch (Throwable t) {
			t.printStackTrace(System.err);
			return null;
		}		
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#traceDeltaCounter(long, java.lang.CharSequence, java.lang.CharSequence[])
	 */
	@Override
	public ICEMetric traceDeltaCounter(long value, CharSequence name, CharSequence... namespace) {
		try {
			Long delta = ICEMetricCatalog.getInstance().getDelta(value, host, agent, name, namespace);
			if(delta==null) return null;			
			ICEMetric metric =  ICEMetric.trace(delta.longValue(), host, agent, name, MetricType.DELTA_COUNTER, namespace);
			submitter.submit(metric);
			return metric;
		} catch (Throwable t) {
			t.printStackTrace(System.err);
			return null;
		}		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#traceDeltaGauge(long, java.lang.CharSequence, java.lang.CharSequence[])
	 */
	@Override
	public ICEMetric traceDeltaGauge(long value, CharSequence name, CharSequence... namespace) {
		try {
			Long delta = ICEMetricCatalog.getInstance().getDelta(value, host, agent, name, namespace);
			if(delta==null) return null;			
			ICEMetric metric =  ICEMetric.trace(delta.longValue(), host, agent, name, MetricType.DELTA_GAUGE, namespace);
			submitter.submit(metric);
			return metric;
		} catch (Throwable t) {
			t.printStackTrace(System.err);
			return null;
		}		
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#traceString(java.lang.CharSequence, java.lang.CharSequence, java.lang.CharSequence[])
	 */
	@Override
	public ICEMetric traceString(CharSequence value, CharSequence name, CharSequence... namespace) {
		try {				
			ICEMetric metric = ICEMetric.trace(value, host, agent, name, MetricType.STRING, namespace);
			submitter.submit(metric);
			return metric;			
		} catch (Throwable t) {
			t.printStackTrace(System.err);
			return null;
		}		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#traceError(java.lang.Throwable, java.lang.CharSequence, java.lang.CharSequence[])
	 */
	@Override
	public ICEMetric traceError(Throwable value, CharSequence name, CharSequence... namespace) {
		try {				
			ICEMetric metric =  ICEMetric.trace(value, host, agent, name, MetricType.ERROR, namespace);
			submitter.submit(metric);
			return metric;						
		} catch (Throwable t) {
			t.printStackTrace(System.err);
			return null;
		}		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#traceBlob(java.io.Serializable, java.lang.CharSequence, java.lang.CharSequence[])
	 */
	@Override
	public ICEMetric traceBlob(Serializable value, CharSequence name, CharSequence... namespace) {
		try {				
			ICEMetric metric =   ICEMetric.trace(value, host, agent, name, MetricType.BLOB, namespace);
			submitter.submit(metric);
			return metric;									
		} catch (Throwable t) {
			t.printStackTrace(System.err);
			return null;
		}		
	}
	
	@Override
	public ICEMetric tracePDU(PDU pdu, CharSequence name, CharSequence...namespace) {
		try {				
			ICEMetric metric =   ICEMetric.trace(pdu, host, agent, name, MetricType.PDU, namespace);
			submitter.submit(metric);
			return metric;									
		} catch (Throwable t) {
			t.printStackTrace(System.err);
			return null;
		}				
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#getHost()
	 */
	@Override
	public String getHost() {
		return host;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#getAgent()
	 */
	@Override
	public String getAgent() {
		return agent;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#getSentMetrics()
	 */
	@Override
	public long getSentMetrics() {
		return submitter.getSentMetrics();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#getDroppedMetrics()
	 */
	@Override
	public long getDroppedMetrics() {
		return submitter.getDroppedMetrics();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#resetStats()
	 */
	@Override
	public void resetStats() {
		submitter.resetStats();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#getQueuedMetrics()
	 */
	@Override
	public long getQueuedMetrics() {
		return submitter.getQueuedMetrics();
	}

	/**
	 * Returns the collection submitter 
	 * @return the submitter
	 */
	public MetricSubmitter getSubmitter() { 
		return submitter;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#traceIncrement(long, java.lang.CharSequence, java.lang.CharSequence[])
	 */
	@Override
	public ICEMetric traceIncrement(long value, CharSequence name, CharSequence... namespace) {
		try {				
			ICEMetric metric = ICEMetric.trace(value, host, agent, name, MetricType.INCREMENTOR, namespace);
			submitter.submit(metric);
			return metric;
		} catch (Throwable t) {
			t.printStackTrace(System.err);
			return null;
		}				
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#traceIncrement(java.lang.CharSequence, java.lang.CharSequence[])
	 */
	@Override
	public ICEMetric traceIncrement(CharSequence name, CharSequence... namespace) {
		return traceIncrement(1L, name, namespace);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#traceIntervalIncrement(long, java.lang.CharSequence, java.lang.CharSequence[])
	 */
	@Override
	public ICEMetric traceIntervalIncrement(long value, CharSequence name, CharSequence... namespace) {
		try {				
			ICEMetric metric = ICEMetric.trace(value, host, agent, name, MetricType.INTERVAL_INCREMENTOR, namespace);
			submitter.submit(metric);
			return metric;
		} catch (Throwable t) {
			t.printStackTrace(System.err);
			return null;
		}				
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#traceIntervalIncrement(java.lang.CharSequence, java.lang.CharSequence[])
	 */
	@Override
	public ICEMetric traceIntervalIncrement(CharSequence name, CharSequence... namespace) {
		return traceIntervalIncrement(1L, name, namespace);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#trace(java.lang.Object, java.lang.CharSequence, java.lang.Object, java.lang.CharSequence[])
	 */
	@Override
	public ICEMetric trace(Object value, CharSequence name, Object type, CharSequence... namespace) {
		try { return trace(value, name, MetricType.valueOf(type), namespace); } catch (Exception ex) { return null; }
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#traceDirect(java.lang.Object, java.lang.CharSequence, java.lang.Object, java.lang.CharSequence[])
	 */
	@Override
	public ICEMetric traceDirect(Object value, CharSequence name, Object type, CharSequence... namespace) {
		return traceDirect(value, name, MetricType.valueOf(type), namespace);
	}

}

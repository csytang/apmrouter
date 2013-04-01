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

import static org.helios.apmrouter.server.tracing.virtual.VirtualState.INIT;

import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.helios.apmrouter.collections.delay.DelayChangeReceiver;
import org.helios.apmrouter.collections.delay.NotifyingDelay;
import org.helios.apmrouter.jmx.JMXHelper;

/**
 * <p>Title: VirtualAgent</p>
 * <p>Description: Represents a notional agent for which no agent actually exists but which is logically 
 * represented by apm-collectors using {@link VirtualTracer}s tagged to this agent.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.tracing.VirtualAgent</code></p>
 */

public class VirtualAgent implements VirtualAgentMXBean, NotifyingDelay, DelayChangeReceiver<VirtualTracer>, Runnable, Iterable<VirtualTracer> {
	/** The virtual tracer's state */
	private final AtomicReference<VirtualState> agentState = new AtomicReference<VirtualState>(INIT);
	/** The receiver to notify when this agent's delay changes */
	protected DelayChangeReceiver<NotifyingDelay> receiver = null;
	/** A dynamically sorting set of virtual tracerExpiryQueue, sorted by the expiry time of the tracerExpiryQueue */
	protected final ConcurrentSkipListSet<VirtualTracer> tracerExpiryQueue = new ConcurrentSkipListSet<VirtualTracer>();
	/** A map of virtual tracerExpiryQueue keyed by the tracer name */
	protected final Map<String, VirtualTracer> tracers = new ConcurrentHashMap<String, VirtualTracer>(); 
	/** The agent's host */
	protected final String host;
	/** The agent's name */
	protected final String agent;
	/** The agent's local URI */
	protected final String localURI;
	/** The JMX ObjectName for this virtual agent */
	protected final ObjectName objectName;
	/** The schedule handle for this agent's pending invalidation */
	protected final AtomicReference<ScheduledFuture<?>> pendingInvalidation = new AtomicReference<ScheduledFuture<?>>(null); 
	/** A reference to the Virtual Agent Manager */
	protected final VirtualAgentManager vaManager;
	/** Instance logger */
	protected final Logger log;
	
	/**
	 * Creates a new VirtualAgent
	 * @param host The agent's host
	 * @param agent The agent name
	 * @param localURI The designated local URI for this virtual agent
	 * @param vaManager A reference to the Virtual Agent Manager
	 */
	VirtualAgent(String host, String agent, String localURI, VirtualAgentManager vaManager) {
		this.host = host;
		this.agent = agent;
		this.localURI = localURI;
		this.vaManager = vaManager;
		log =  Logger.getLogger(String.format("", getClass().getName() ,".", host, ":", agent));
		objectName = JMXHelper.objectName(String.format(VA_OBJ_NAME, host, agent));
		JMXHelper.registerMBean(objectName, this);		
	}

	/**
	 * Returns the named virtual tracer
	 * @param name The name of the virtual tracer to retrieve
	 * @return the named virtual tracer
	 */
	public VirtualTracer getVirtualTracer(String name) {
		return tracers.get(name);
	}
	
	/**
	 * Returns the reference key for this virtual agent
	 * @return the reference key
	 */
	public String getKey() {
		return String.format("%s:%s", host, agent);
	}
	
	/**
	 * Adds a virtual tracer to this virtual agent
	 * @param vt The virtual tracer to add
	 */
	public void addVirtualTracer(VirtualTracer vt) {
		if(vt!=null) {
			if(!host.equals(vt.getHost())) throw new IllegalArgumentException("The virtual tracer for host [" + vt.getHost() + "] does not belong in this agent for host [" + host + "]", new Throwable());
			if(!agent.equals(vt.getAgent())) throw new IllegalArgumentException("The virtual tracer for agent [" + vt.getAgent() + "] does not belong in this agent for agent [" + agent + "]", new Throwable());
			if(!tracers.containsKey(vt.getName())) {
				synchronized(tracers) {
					if(!tracers.containsKey(vt.getName())) {
						tracers.put(vt.getName(), vt);
						tracerExpiryQueue.add(vt);

					}
				}
			}
		}		
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.virtual.VirtualAgentMXBean#getState()
	 */
	@Override
	public String getState() {
		return agentState.get().name();
	}

	
	
	/**
	 * <p>Delegates to the tracer with the longest time to expiry.</p>
	 * {@inheritDoc}
	 * @see java.util.concurrent.Delayed#getDelay(java.util.concurrent.TimeUnit)
	 */
	@Override
	public long getDelay(TimeUnit unit) {
		try {						
			while(true) {
				VirtualTracer vt = tracerExpiryQueue.last();
				long delay = vt.getDelay(unit);
				if(delay<1) {
					vt.clearDelayChangeReceiver();
					tracerExpiryQueue.remove(vt);
				} else {
					return delay;
				}
			}
		} catch (NoSuchElementException nse) {
			return 0;
		}		
	}
	
	/**
	 * Returns the time to expiry of the tracer with the longest expiry time in ms.
	 * @return the time to expiry of the tracer with the longest expiry time
	 */
	@Override
	public long getTimeToExpiry() {
		try {			
			return tracerExpiryQueue.last().getTimeToExpiry();
		} catch (NoSuchElementException nse) {
			return 0;
		}				
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Delayed o) {
		try {			
			return tracerExpiryQueue.first().compareTo(o);
		} catch (NoSuchElementException nse) {
			return 0;
		}		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.delay.NotifyingDelay#setDelayChangeReceiver(org.helios.apmrouter.collections.delay.DelayChangeReceiver)
	 */
	@Override
	public void setDelayChangeReceiver(DelayChangeReceiver<NotifyingDelay> receiver) {
		this.receiver = receiver;
		
	}

	/**
	 * <p>Called when this virtual agent expires</p>
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.delay.NotifyingDelay#clearDelayChangeReceiver()
	 */
	@Override
	public void clearDelayChangeReceiver() {
		this.receiver = null;
		for(VirtualTracer vt: tracerExpiryQueue) {
			vt.clearDelayChangeReceiver();
		}
		tracerExpiryQueue.clear();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.delay.DelayChangeReceiver#onDelayChange(org.helios.apmrouter.collections.delay.NotifyingDelay)
	 */
	@Override
	public void onDelayChange(VirtualTracer virtualTracer) {		
		if(tracerExpiryQueue.remove(virtualTracer)) {
			tracerExpiryQueue.add(virtualTracer);			
			receiver.onDelayChange(this);
		}
		VirtualState priorState = agentState.getAndSet(VirtualState.UP);
		if(priorState==VirtualState.SOFTDOWN) {
			vaManager.setAgentState(this, true);
			tracerExpiryQueue.add(virtualTracer);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.virtual.VirtualAgentMXBean#getHost()
	 */
	@Override
	public String getHost() {
		return host;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.virtual.VirtualAgentMXBean#getAgent()
	 */
	@Override
	public String getAgent() {
		return agent;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.virtual.VirtualAgentMXBean#expire()
	 */
	@Override
	public void expire() {
		for(Iterator<VirtualTracer> iter = tracerExpiryQueue.iterator(); iter.hasNext();) {
			iter.next().expire();
			iter.remove();
		}		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.virtual.VirtualAgentMXBean#invalidate()
	 */
	public void invalidate() {
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.virtual.VirtualAgentMXBean#getLastTouchTimestamp()
	 */
	@Override
	public long getLastTouchTimestamp() {
		try {			
			return tracerExpiryQueue.first().getLastTouchTimestamp();
		} catch (NoSuchElementException nse) {
			return 0;
		}		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.virtual.VirtualAgentMXBean#getLastTouchDate()
	 */
	@Override
	public Date getLastTouchDate() {
		try {			
			return tracerExpiryQueue.first().getLastTouchDate();
		} catch (NoSuchElementException nse) {
			return null;
		}		
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((agent == null) ? 0 : agent.hashCode());
		result = prime * result + ((host == null) ? 0 : host.hashCode());
		return result;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		VirtualAgent other = (VirtualAgent) obj;
		if (agent == null) {
			if (other.agent != null)
				return false;
		} else if (!agent.equals(other.agent))
			return false;
		if (host == null) {
			if (other.host != null)
				return false;
		} else if (!host.equals(other.host))
			return false;
		return true;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("VirtualAgent [host:%s, agent:%s, uri:%s]", host, agent, localURI);
	}

	/**
	 * Returns the local URI for this virtual agent
	 * @return the localURI
	 */
	public String getLocalURI() {
		return localURI;
	}
	
	/**
	 * <p>Called when this virtual agent is invalidated</p>
	 * {@inheritDoc}
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		log.info("Invalidating " + this);
		for(VirtualTracer t: this) {
			t.invalidate();
		}
		tracers.clear();
		tracerExpiryQueue.clear();
		pendingInvalidation.set(null);
	}
	
	/**
	 * Cancels a pending invalidation
	 */
	void cancelInvalidation() {
		ScheduledFuture<?> handle = pendingInvalidation.getAndSet(null);
		if(handle!=null) {
			handle.cancel(false);
		}
	}

	/**
	 * Returns the schedule handle for this agent's pending invalidation
	 * @return the pendingInvalidation the invalidation handle
	 */
	ScheduledFuture<?> getPendingInvalidation() {
		return pendingInvalidation.get();
	}
	
	/**
	 * Sets the invalidation handle for this agent
	 * @param scheduleHandle the invalidation handle
	 */
	void setPendingInvalidation(ScheduledFuture<?> scheduleHandle) {
		pendingInvalidation.set(scheduleHandle);
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<VirtualTracer> iterator() {
		return Collections.unmodifiableCollection(tracers.values()).iterator();
	}
	

}

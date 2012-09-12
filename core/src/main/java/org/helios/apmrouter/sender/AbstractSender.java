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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.helios.apmrouter.metric.IMetric;
import org.helios.apmrouter.sender.netty.codec.IMetricEncoder;


/**
 * <p>Title: AbstractSender</p>
 * <p>Description: Abstract base class for sender implementations</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.sender.AbstractSender</code></p>
 */

public abstract class AbstractSender implements ISender {
	/** A map of created senders keyed by the URI */
	protected static final Map<URI, ISender> senders = new ConcurrentHashMap<URI, ISender>(); 
	
	/** The metric encoder */
	protected static final IMetricEncoder metricEncoder = new IMetricEncoder();
	
	protected final AtomicLong sent = new AtomicLong(0);
	protected final AtomicLong dropped = new AtomicLong(0);
	protected final URI serverURI;
	
	protected AbstractSender(URI serverURI) {
		this.serverURI = serverURI;
	}
	
	public long getSentMetrics() {
		return sent.get();
	}
	
	public long getDroppedMetrics() {
		return dropped.get();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.sender.ISender#getURI()
	 */
	@Override
	public URI getURI() {
		return serverURI;
	}

}

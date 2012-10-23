/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2012, Helios Development Group and individual contributors
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
package org.helios.apmrouter.subscription.session;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;

import org.helios.apmrouter.server.ServerComponentBean;
import org.helios.apmrouter.subscription.criteria.FailedCriteriaResolutionException;
import org.helios.apmrouter.subscription.criteria.RecoverableFailedCriteriaResolutionException;
import org.helios.apmrouter.subscription.criteria.SubscriptionCriteria;
import org.helios.apmrouter.subscription.criteria.SubscriptionCriteriaInstance;

/**
 * <p>Title: DefaultSubscriptionSessionImpl</p>
 * <p>Description: The default {@link SubscriptionSession} implementation.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.subscription.session.DefaultSubscriptionSessionImpl</code></p>
 */

public class DefaultSubscriptionSessionImpl extends ServerComponentBean implements SubscriptionSession {
	/** The session's subscriber channel */
	protected final SubscriberChannel subscriberChannel;
	/** The registered criteria */
	protected final Set<SubscriptionCriteria<?,?,?>> criteria = new CopyOnWriteArraySet<SubscriptionCriteria<?,?,?>>();
	/** The resolved criteria keyed by the criteria Id.*/
	protected final Map<Long, SubscriptionCriteriaInstance<?>> resolvedCriteria = new ConcurrentHashMap<Long, SubscriptionCriteriaInstance<?>>();
//	/** The unique ID for this session */
//	public final long sessionId; 
//	/** Serial number generator for subscriptions  */
//	protected static final AtomicLong serial = new AtomicLong(0L);
	
	
	
	/**
	 * Creates a new DefaultSubscriptionSessionImpl
	 * @param subscriberChannel The session's subscriber channel
	 */
	public DefaultSubscriptionSessionImpl(SubscriberChannel subscriberChannel) {
		super();
		this.subscriberChannel = subscriberChannel;		
	}



	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.subscription.session.SubscriptionSession#terminate()
	 */
	@Override
	public void terminate() {
		for(SubscriptionCriteriaInstance<?> sci: resolvedCriteria.values()) {
			sci.terminate();
		}
		resolvedCriteria.clear();
		criteria.clear();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.subscription.session.SubscriptionSession#addCriteria(org.helios.apmrouter.subscription.criteria.SubscriptionCriteria, org.helios.apmrouter.subscription.session.SubscriptionSession)
	 */
	@Override
	public long addCriteria(SubscriptionCriteria<?,?,?> criteria, SubscriptionSession session) throws FailedCriteriaResolutionException {
		if(criteria==null) throw new IllegalArgumentException("The passed criteria was null", new Throwable());
		this.criteria.add(criteria);
		try {
			SubscriptionCriteriaInstance<?> sci = criteria.instantiate();
			sci.resolve(session);
			resolvedCriteria.put(sci.getCriteriaId(), sci);
			return sci.getCriteriaId();
		} catch (FailedCriteriaResolutionException fce) {
			if(!(fce instanceof RecoverableFailedCriteriaResolutionException)) {
				this.criteria.remove(criteria);
			}
			throw fce;
		}
	}
	
	/**
	 * Cancels the subscription criteria with the passed id.
	 * @param criteriaId The id of the c=subscription criteria to cancel
	 */
	public void cancelCriteria(long criteriaId) {
		SubscriptionCriteriaInstance<?> sci = resolvedCriteria.remove(criteriaId);
		if(sci!=null) {
			sci.terminate();
			criteria.remove(sci.getSubscriptionCriteria());
		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.subscription.session.SubscriptionSession#getSubscriptionSessionId()
	 */
	@Override
	public long getSubscriptionSessionId() {
		return subscriberChannel.getSubscriberId();
	}

}

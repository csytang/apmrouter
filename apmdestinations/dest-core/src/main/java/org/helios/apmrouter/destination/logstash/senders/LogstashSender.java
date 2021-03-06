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
package org.helios.apmrouter.destination.logstash.senders;

/**
 * <p>Title: LogstashSender</p>
 * <p>Description: Defines the basics of a logstash sender which accepts a logging event and forwards it to logstash</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.logstash.LogstashSender</code></p>
 * @param <T> The logging event type
 */

public interface LogstashSender<T> {
	/**
	 * Dispatches the passed stashee to logstash
	 * @param stashee The object to be 'stashed
	 */
	public void stash(T stashee);
	
	/**
	 * Returns the base type of the type of log events stashed by this sender
	 * @return the base type of the type of log events 
	 */
	public Class<T> getAcceptedType();
}

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
package org.helios.apmrouter.server.services.session;

/**
 * <p>Title: ChannelSessionListener</p>
 * <p>Description: Defines a class that listens on channels entering (connecting) and leaving (closing) the {@link SharedChannelGroup}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.services.session.ChannelSessionListener</code></p>
 */

public interface ChannelSessionListener {
	
	/**
	 * Fired when a new channel is added to the {@link SharedChannelGroup}
	 * @param channel the new channel that was added
	 */
	public void onConnectedChannel(DecoratedChannel channel);
	
	/**
	 * Fired when a connected channel is closed and removed from the {@link SharedChannelGroup}
	 * @param channel The closed channel
	 * @return The number of agents still connected from the host this channel was closed from
	 */
	public int onClosedChannel(DecoratedChannel channel);
	
	/**
	 * Fired when a connected channel is identified
	 * @param channel The identified channel
	 * @return The number of agents now connected from the host this channel was closed from
	 */
	public int onIdentifiedChannel(DecoratedChannel channel);
	
}

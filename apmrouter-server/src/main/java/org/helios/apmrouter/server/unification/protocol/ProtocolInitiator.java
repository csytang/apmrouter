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
package org.helios.apmrouter.server.unification.protocol;

import org.helios.apmrouter.server.unification.pipeline.PipelineModifier;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;

/**
 * <p>Title: ProtocolInitiator</p>
 * <p>Description: Defines a class that can identify a specific protocol from an initiated connection through the unification server.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.unification.protocol.ProtocolInitiator</code></p>
 */

public interface ProtocolInitiator {
	/**
	 * Tests this initiator to see if the initiating connection is a protocol match.
	 * @param magic1 The first unsigned byte in the channel buffer
	 * @param magic2 The second unsigned byte in the channel buffer
	 * @return true for match, false otherwise
	 */
	public boolean match(int magic1, int magic2);
	
	/**
	 * Tests this initiator to see if the initiating connection is a protocol match.
	 * @param buff The initial channel buffer passed on connect
	 * @return true for match, false otherwise
	 */
	public boolean match(ChannelBuffer buff);
	

	/**
	 * Modifies the passed pipeline to provide specific functionality after a successful protocol match
	 * @param ctx The channel handler context
	 * @param channel The current channel
	 * @param buffer  The initiating buffer
	 */
	public void modifyPipeline(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer);
	
	
	/**
	 * Returns the bean name
	 * @return the bean name
	 */
	public String getBeanName();
	
	/**
	 * Returns the protocol recognition magic int 1
	 * @return the protocol recognition magic int 1
	 */
	public int getMyMagic1();
	
	
	/**
	 * Returns the protocol recognition magic int 2
	 * @return the protocol recognition magic int 2
	 */
	public int getMyMagic2();
	
	/**
	 * Returns the protocol implemented by this initiator.
	 * This protocol should match up with a {@link PipelineModifier}.
	 * @return the protocol name
	 */
	public String getProtocol();
	

	
}

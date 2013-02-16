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
package org.helios.apmrouter.server.net.listener.netty.handlers;

import java.net.SocketAddress;

import org.helios.apmrouter.OpCode;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;

/**
 * <p>Title: AgentRequestHandler</p>
 * <p>Description: Defines a service that handles agent requests encoded in channel buffers</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.net.listener.netty.handlers.AgentRequestHandler</code></p>
 */

public interface AgentRequestHandler {
	/**
	 * Processes a channel buffer containing the encoded agent request
	 * @param opCode The agent request op code
	 * @param buff The channel buffer containing the encoded agent request
	 * @param remoteAddress The remote address of the agent that sent the request
	 * @param channel The channel the request was received on
	 */
	public void processAgentRequest(OpCode opCode, ChannelBuffer buff, SocketAddress remoteAddress, Channel channel);
	
	/**
	 * Returns the opcodes handled by this handler
	 * @return the opcodes handled by this handler
	 */
	public OpCode[] getHandledOpCodes();
}

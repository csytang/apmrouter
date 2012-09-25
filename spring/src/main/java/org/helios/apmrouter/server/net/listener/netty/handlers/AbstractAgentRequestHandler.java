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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.helios.apmrouter.OpCode;
import org.helios.apmrouter.metric.AgentIdentity;
import org.helios.apmrouter.server.ServerComponentBean;
import org.helios.apmrouter.server.net.listener.netty.ChannelGroupAware;
import org.helios.apmrouter.server.net.listener.netty.group.ManagedChannelGroup;
import org.helios.apmrouter.util.TimeoutQueueMap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.logging.InternalLogLevel;

/**
 * <p>Title: AbstractAgentRequestHandler</p>
 * <p>Description: Base class </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.net.listener.netty.handlers.AbstractAgentRequestHandler</code></p>
 */

public abstract class AbstractAgentRequestHandler extends ServerComponentBean implements AgentRequestHandler, ChannelGroupAware {
	/** The channel group */
	protected ManagedChannelGroup channelGroup = null;	
	/** The synchronous request timeout map */
	protected static final TimeoutQueueMap<String, CountDownLatch> timeoutMap = new TimeoutQueueMap<String, CountDownLatch>(2000);
	/** The logical session timeout map */
	protected static final TimeoutQueueMap<String, SocketAddress> sessionTimeoutMap = new TimeoutQueueMap<String, SocketAddress>(15000);	
	
	/** Logging handler */
	private static final LoggingHandler clientConnLogHandler = new LoggingHandler("org.helios.AgentMetricHandler", InternalLogLevel.DEBUG, true);
	
	/**
	 * Sets the channel group
	 * @param channelGroup the injected channel group
	 */
	@Override
	public void setChannelGroup(ManagedChannelGroup channelGroup) {
		this.channelGroup = channelGroup;
	}
	/**
	 * Acquires a channel connected to the provided remote address
	 * @param incoming The incoming channel to acquire a new channel from, if required
	 * @param remoteAddress The remote address to connect to
	 * @return a channel connected to the remote address
	 * FIXME: Need configurable timeout on remote connect
	 */
	protected Channel getChannelForRemote(final Channel incoming, final SocketAddress remoteAddress) {
		Channel channel = channelGroup.findRemote(remoteAddress);
		if(channel==null) {
			synchronized(channelGroup) {
				channel = channelGroup.findRemote(remoteAddress);
				if(channel==null) {
					channel = incoming.getFactory().newChannel(Channels.pipeline(clientConnLogHandler));
					channelGroup.add(channel, "AgentConnection/" + remoteAddress);
					try {
						if(!channel.connect(remoteAddress).await(1000)) throw new Exception();
					} catch (Exception  e) {
						throw new RuntimeException("Failed to acquire remote connection to [" + remoteAddress + "]", e);
					}
					final ChannelGroup cg = channelGroup;
					channel.getCloseFuture().addListener(new ChannelFutureListener() {
						@Override
						public void operationComplete(ChannelFuture future) throws Exception {
							System.err.println("Client Channel Closed. Did Agent Go Away ?");
							cg.remove(future.getChannel());
						}
					});
					channelGroup.add(channel);
				}
			}
		}
		return channel;
	}
	
	
	
	/**
	 * Sends a ping request to the passed address
	 * @param channel The channel to the client to ping
	 * @param timeout the timeout in ms.
	 * @return true if ping was confirmed within the timeout, false otherwise
	 */
	public boolean ping(Channel channel, long timeout) {
		try {
			StringBuilder key = new StringBuilder();
			ChannelBuffer ping = encodePing(key);
			channel.write(ping,channel.getRemoteAddress());
			CountDownLatch latch = new CountDownLatch(1);
			timeoutMap.put(key.toString(), latch, timeout);
			return latch.await(timeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			return false;
		}		
	}
	
	
	
	/**
	 * Creates a ping channel buffer and appends the key to the passed buffer 
	 * @param key The buffer to place the key in
	 * @return the ping ChannelBuffer
	 */
	protected ChannelBuffer encodePing(final StringBuilder key) {
		String _key = new StringBuilder(AgentIdentity.ID.getHostName()).append("-").append(AgentIdentity.ID.getAgentName()).append(System.nanoTime()).toString();
		key.append(_key);
		byte[] bytes = _key.getBytes();
		ChannelBuffer ping = ChannelBuffers.buffer(1+4+bytes.length);
		ping.writeByte(OpCode.PING.op());
		ping.writeInt(bytes.length);
		ping.writeBytes(bytes);
		return ping;
	}
	
	/**
	 * Decodes a ping from the passed channel buffer, and if the resulting key locates a latch in the timeout map, counts it down.
	 * @param cb The ChannelBuffer to read the ping from
	 */
	protected void decodePing(ChannelBuffer cb) {
		int byteCount = cb.readInt();
		byte[] bytes = new byte[byteCount];
		cb.readBytes(bytes);
		String key = new String(bytes);
		CountDownLatch latch = timeoutMap.remove(key);
		if(latch!=null) latch.countDown();
	}

}

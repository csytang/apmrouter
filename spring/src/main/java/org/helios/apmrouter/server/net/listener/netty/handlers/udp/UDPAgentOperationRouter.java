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
package org.helios.apmrouter.server.net.listener.netty.handlers.udp;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.helios.apmrouter.OpCode;
import org.helios.apmrouter.server.net.listener.netty.ChannelGroupAware;
import org.helios.apmrouter.server.net.listener.netty.group.ManagedChannelGroup;
import org.helios.apmrouter.server.net.listener.netty.group.ManagedChannelGroupMXBean;
import org.helios.apmrouter.server.net.listener.netty.handlers.AbstractAgentRequestHandler;
import org.helios.apmrouter.server.net.listener.netty.handlers.AgentRequestHandler;
import org.helios.apmrouter.server.services.session.ChannelType;
import org.helios.apmrouter.server.services.session.SharedChannelGroup;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.logging.InternalLogLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.support.MetricType;

/**
 * <p>Title: UDPAgentOperationRouter</p>
 * <p>Description: Routes agent requests to the correct service in accordance with the op-code in the request message</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.net.listener.netty.handlers.udp.UDPAgentOperationRouter</code></p>
 */

public class UDPAgentOperationRouter extends AbstractAgentRequestHandler implements ChannelUpstreamHandler, ChannelGroupAware {
	/** The channel group */
	protected ManagedChannelGroupMXBean channelGroup = null;
	
	/** A set of socket addresses to which {@link OpCode#WHO} requests have been sent to but for which a response has not been received */
	protected final Set<SocketAddress> pendingWhos = new CopyOnWriteArraySet<SocketAddress>();

	/** A map of agent request handlers keyed by the opcode */
	protected final EnumMap<OpCode, AgentRequestHandler> handlers = new EnumMap<OpCode, AgentRequestHandler>(OpCode.class);
	
	
	
	/**
	 * Sets the agent request handlers
	 * @param agentRequestHandlers a collection of agent request handlers
	 */
	@Autowired(required=true)
	public void setAgentRequestHandlers(Collection<AgentRequestHandler> agentRequestHandlers) {
		for(AgentRequestHandler arh: agentRequestHandlers) {
			for(OpCode soc: arh.getHandledOpCodes()) {
				handlers.put(soc, arh);
				info("Added AgentRequestHandler [", arh.getClass().getSimpleName(), "] for Op [", soc , "]");
			}
		}
	}
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelUpstreamHandler#handleUpstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
	 */
	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		if(e instanceof MessageEvent) {
			Object msg = ((MessageEvent)e).getMessage();
			if(msg instanceof ChannelBuffer) {
				SocketAddress remoteAddress = ((MessageEvent)e).getRemoteAddress();
				if(!pendingWhos.contains(remoteAddress)) {
					Channel remoteChannel = SharedChannelGroup.getInstance().getByRemote(remoteAddress);
					if(remoteChannel==null) {
						try {
							remoteChannel = getChannelForRemote(e.getChannel(), remoteAddress);						
						} catch (Exception ex) {
							ex.printStackTrace(System.err);
						}
					}
				}
				
				ChannelBuffer buff = (ChannelBuffer)msg;
				incr("RequestsReceived");
				OpCode opCode = OpCode.valueOf(buff);
				try {
					handlers.get(opCode).processAgentRequest(opCode, buff, ((MessageEvent) e).getRemoteAddress(), e.getChannel());
					incr("RequestsCompleted");
				} catch (Throwable t) {
					incr("RequestsFailed");
					error("Failed to handle [", opCode, "]", t );
				}
				return;
			}
		}
		ctx.sendUpstream(e);
	}
	
	/** Logging handler */
	private static final LoggingHandler clientConnLogHandler = new LoggingHandler("org.helios.UDPAgentOperationRouter", InternalLogLevel.INFO, true);

	
	/**
	 * Acquires a channel connected to the provided remote address
	 * @param incoming The incoming channel to acquire a new channel from, if required
	 * @param remoteAddress The remote address to connect to
	 * @return a channel connected to the remote address
	 * FIXME: Need configurable timeout on remote connect
	 */
	protected Channel getChannelForRemote(final Channel incoming, final SocketAddress remoteAddress) {
		Channel channel = SharedChannelGroup.getInstance().getByRemote(remoteAddress);
		if(channel==null) {
			synchronized(SharedChannelGroup.getInstance()) {
				channel = SharedChannelGroup.getInstance().getByRemote(remoteAddress);
				if(channel==null) {
					channel = incoming.getFactory().newChannel(Channels.pipeline(clientConnLogHandler));					
					try {
						if(!channel.connect(remoteAddress).await(1000)) throw new Exception();
						if(!pendingWhos.contains(remoteAddress)) {
							sendWho(channel, remoteAddress);
						}
//						SharedChannelGroup.getInstance().add(channel, ChannelType.UDP_AGENT, "UDPAgent");
					} catch (Exception  e) {
						throw new RuntimeException("Failed to acquire remote connection to [" + remoteAddress + "]", e);
					}
				}
			}
		}
		return channel;
	}
	
	/**
	 * Sends a {@link OpCode#WHO} request to a newly connected channel
	 * @param channel The newly connected channel
	 * @param remoteAddress Thre remote address of the newly connected channel
	 */
	protected void sendWho(Channel channel, final SocketAddress remoteAddress) {
		byte[] bytes = remoteAddress.toString().getBytes();
		ChannelBuffer cb = ChannelBuffers.directBuffer(bytes.length+5);
		cb.writeByte(OpCode.WHO.op());
		cb.writeInt(bytes.length);
		cb.writeBytes(bytes);
		info("Sending Who Request to [", remoteAddress, "]");
		pendingWhos.add(remoteAddress);
		channel.write(cb, remoteAddress).addListener(new ChannelFutureListener() {
			public void operationComplete(ChannelFuture f) throws Exception {
				if(f.isSuccess()) {					
					info("Confirmed Send Of Who Request to [", remoteAddress, "]");
				} else {
					error("Failed to send Who request to [", remoteAddress, "]", f.getCause());
					pendingWhos.remove(remoteAddress);
				}
				
			}
		});			
	}
	
	
	/**
	 * Returns the total number of agent operations received
	 * @return the total number of agent operations received
	 */
	@ManagedMetric(category="UDPOpRequests", metricType=MetricType.COUNTER, description="total number of agent operations received")
	public long getRequestsReceived() {
		return getMetricValue("RequestsReceived");
	}
	
	/**
	 * Returns the total number of agent operations completed
	 * @return the total number of agent operations completed
	 */
	@ManagedMetric(category="UDPOpRequests", metricType=MetricType.COUNTER, description="total number of agent operations completed")
	public long getRequestsCompleted() {
		return getMetricValue("RequestsCompleted");
	}
	
	/**
	 * Returns the total number of agent operations failed
	 * @return the total number of agent operations failed
	 */
	@ManagedMetric(category="UDPOpRequests", metricType=MetricType.COUNTER, description="total number of agent operations failed")
	public long getRequestsFailed() {
		return getMetricValue("RequestsFailed");
	}
	
	/**
	 * Returns the total number of pending {@link OpCode#WHO} requests in Whoville
	 * @return the total number of pending {@link OpCode#WHO} requests 
	 */
	@ManagedMetric(category="UDPOpRequests", metricType=MetricType.COUNTER, description="total number of pending who requests")
	public int getPendingWhos() {
		return pendingWhos.size();
	}
	
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponent#getSupportedMetricNames()
	 */
	@Override
	public Set<String> getSupportedMetricNames() {
		Set<String> metrics = new HashSet<String>(super.getSupportedMetricNames());
		metrics.add("RequestsReceived");
		metrics.add("RequestsCompleted");
		metrics.add("RequestsFailed");
		return metrics;
	}	
	
	
	
	/**
	 * Sets the channel group
	 * @param channelGroup the injected channel group
	 */
	public void setChannelGroup(ManagedChannelGroup channelGroup) {
		this.channelGroup = channelGroup;
		for(AgentRequestHandler arh: handlers.values()) {
			if(arh instanceof ChannelGroupAware) {
				((ChannelGroupAware)arh).setChannelGroup(channelGroup);
			}
		}
	}
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.net.listener.netty.handlers.AgentRequestHandler#processAgentRequest(org.helios.apmrouter.OpCode, org.jboss.netty.buffer.ChannelBuffer, java.net.SocketAddress, org.jboss.netty.channel.Channel)
	 */
	@Override
	public void processAgentRequest(OpCode opCode, ChannelBuffer buff, SocketAddress remoteAddress, Channel channel) {
		if(opCode==OpCode.WHO_RESPONSE) {
			pendingWhos.remove(remoteAddress);
			buff.readByte();
			int hostLength = buff.readInt();
			byte[] hostBytes = new byte[hostLength];
			buff.readBytes(hostBytes);
			int agentLength = buff.readInt();
			byte[] agentBytes = new byte[agentLength];
			buff.readBytes(agentBytes);
			String host = new String(hostBytes);
			String agent = new String(agentBytes);
			SharedChannelGroup.getInstance().add(channel, ChannelType.UDP_AGENT, "UDPAgent/" + host + "/" + agent);
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.net.listener.netty.handlers.AgentRequestHandler#getHandledOpCodes()
	 */
	@Override
	public OpCode[] getHandledOpCodes() {
		return new OpCode[]{OpCode.WHO_RESPONSE};
	}


}

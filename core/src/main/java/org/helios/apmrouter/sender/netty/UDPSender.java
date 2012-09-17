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
package org.helios.apmrouter.sender.netty;

import static org.helios.apmrouter.util.Methods.nvl;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.BasicConfigurator;
import org.helios.apmrouter.ReceiverOpCode;
import org.helios.apmrouter.SenderOpCode;
import org.helios.apmrouter.jmx.ThreadPoolFactory;
import org.helios.apmrouter.metric.catalog.ICEMetricCatalog;
import org.helios.apmrouter.metric.catalog.IMetricCatalog;
import org.helios.apmrouter.sender.AbstractSender;
import org.helios.apmrouter.trace.DirectMetricCollection;
import org.helios.apmrouter.trace.DirectMetricCollection.SplitDMC;
import org.helios.apmrouter.trace.DirectMetricCollection.SplitReader;
import org.helios.apmrouter.util.TimeoutQueueMap;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.DirectChannelBufferFactory;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.FixedReceiveBufferSizePredictorFactory;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.logging.InternalLogLevel;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.Log4JLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: UDPSender</p>
 * <p>Description: A Netty unicast UDP sender implementation</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.sender.netty.UDPSender</code></p>
 */

public class UDPSender extends AbstractSender implements ChannelPipelineFactory {
	/** Static class logger */
	protected static final Logger LOG = LoggerFactory.getLogger(UDPSender.class);	
	/** The netty server worker pool */
	protected final Executor workerPool;
	/** The netty bootstrap */
	protected final ConnectionlessBootstrap bstrap;
	/** The netty channel factory */
	protected final ChannelFactory channelFactory;
	/** The connected channel */
	protected final DatagramChannel channel;
	/** The server socket to send to */
	protected final InetSocketAddress socketAddress;
	/** The metric catalog for token updates */
	protected final IMetricCatalog metricCatalog;
	/** The logging handler for debug */
	private LoggingHandler loggingHandler;
	/** A discard handler used for discarding self-sent messages */
	protected final SimpleChannelUpstreamHandler discard = new SimpleChannelUpstreamHandler() {
		@Override
		public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
			if(e.getChannel().getLocalAddress().equals(e.getRemoteAddress())) {
				LOG.info("Drop");
			} else {
				Object msg = e.getMessage();
				if(msg instanceof ChannelBuffer) {
					ChannelBuffer buff = (ChannelBuffer)msg;
					ReceiverOpCode opCode = ReceiverOpCode.valueOf(buff.readByte());					
					switch (opCode) {
						case CONFIRM_METRIC:							
							int keyLength = buff.readInt();
							byte[] keyBytes = new byte[keyLength];
							buff.readBytes(keyBytes);
							String key = new String(keyBytes);
							CountDownLatch latch = timeoutMap.get(key);
							if(latch!=null) {
								long c = latch.getCount(); 
								latch.countDown();
							}
							break;
						case SEND_METRIC_TOKEN:
							int fqnLength = buff.readInt();
							byte[] bytes = new byte[fqnLength];
							buff.readBytes(bytes);
							String fqn = new String(bytes);
							long token = buff.readLong();
							metricCatalog.setToken(fqn, token);						
							break;
						default:
							break;
					}
				}
			}
		}
	};
	
	/**
	 * Out log
	 * @param msg the message to log
	 */
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	/**
	 * Returns a built instance of a UDPSender for the passed URI
	 * @param serverURI The host/port to send to in the form of a URI. e.g. <b><code>udp://myhostname:2094</code></b>.
	 * @return a UDPSender
	 */
	public static UDPSender getInstance(URI serverURI) {
		UDPSender sender = (UDPSender) senders.get(nvl(serverURI, "Server URI"));
		if(sender==null) {
			synchronized(senders) {
				sender = (UDPSender) senders.get(serverURI);
				if(sender==null) {
					sender = new UDPSender(serverURI);
					senders.put(serverURI, sender);
				}
			}
		}
		return sender;
	}
	
	/**
	 * Creates a new UDPSender
	 * @param serverURI The host/port to send to
	 */
	private UDPSender(URI serverURI) {
		super(serverURI);
		BasicConfigurator.configure();
		InternalLoggerFactory.setDefaultFactory(new Log4JLoggerFactory());
		metricCatalog = ICEMetricCatalog.getInstance();
		loggingHandler = new LoggingHandler(InternalLogLevel.INFO, true);
		workerPool =  ThreadPoolFactory.newCachedThreadPool(getClass().getPackage().getName(), "UDPSenderWorker/" + serverURI.getHost() + "/" + serverURI.getPort());
		channelFactory = new NioDatagramChannelFactory(workerPool);
		bstrap = new ConnectionlessBootstrap(channelFactory);
		bstrap.setPipelineFactory(this);
		bstrap.setOption("broadcast", true);
		bstrap.setOption("receiveBufferSizePredictorFactory", new FixedReceiveBufferSizePredictorFactory(1024));
		socketAddress = new InetSocketAddress(serverURI.getHost(), serverURI.getPort());
		//channel = (DatagramChannel) bstrap.connect(socketAddress).awaitUninterruptibly().getChannel();
		channel = (DatagramChannel) bstrap.bind(new InetSocketAddress("localhost", 0));
		channel.getConfig().setBufferFactory(new DirectChannelBufferFactory());
		channel.connect(socketAddress);
		
		//socketAddress = new InetSocketAddress("239.192.74.66", 25826);
	}
	


	

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelPipelineFactory#getPipeline()
	 */
	@Override
	public ChannelPipeline getPipeline()  {
		ChannelPipeline pipeline = Channels.pipeline();
		
		//pipeline.addLast("logging", loggingHandler);
		pipeline.addLast("discard", discard);
		pipeline.addLast("metric-encoder", metricEncoder);
		return pipeline;
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.sender.ISender#send(org.helios.apmrouter.trace.DirectMetricCollection)
	 */
	@Override
	public void send(final DirectMetricCollection dcm) {
//		System.out.println("Received [" + dcm.getMetricCount() + "]");
//		SplitReader sr = dcm.newSplitReader(1024);
//		int cnt = 0;
//		for(DirectMetricCollection d: sr) {
//			cnt += d.getMetricCount();
//			d.destroy();
//		}
//		dcm.destroy();
//		System.out.println("Sending [" + cnt + "] Dropped:" + sr.getDrops());
		
		if(dcm.getSize()<1024) {
			final int mcount = dcm.getMetricCount();
			ChannelFuture channelFuture = channel.write(dcm);
			//System.out.println("Sent to [" + socketAddress + "]");
			channelFuture.addListener(new ChannelFutureListener() {
				public void operationComplete(ChannelFuture future) throws Exception {					
					if(future.isSuccess()) {
						sent.addAndGet(mcount);
					} else {
						long d = failed.addAndGet(mcount);
						System.err.println("Sender Fails:" + d );
						if(future.getCause()!=null) future.getCause().printStackTrace(System.err);
					}					
				}
			});
			return;
		}
		
		
		
		SplitDMC sr = dcm.newSplitReader(1024);
		for(final DirectMetricCollection d: sr) {
			final boolean last = !sr.hasNext();
			final int mcount = d.getMetricCount();
			ChannelFuture channelFuture = channel.write(d);
			channelFuture.addListener(new ChannelFutureListener() {
				public void operationComplete(ChannelFuture future) throws Exception {					
					if(future.isSuccess()) {
						sent.addAndGet(mcount);
					} else {
						long d = failed.addAndGet(mcount);
						System.err.println("Sender Fails:" + d );
						if(future.getCause()!=null) future.getCause().printStackTrace(System.err);
					}
				}
			});
			if(last) channelFuture.addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					//ch.close();
				}
			});
		}
		dcm.destroy();
		dropped.addAndGet(((SplitReader)sr).getDrops());
		
		
		
		
	}

}

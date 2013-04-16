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
package org.helios.apmrouter.wsclient;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.helios.apmrouter.jmx.ThreadPoolFactory;
import org.helios.apmrouter.jmx.threadinfo.ExtendedThreadManager;
import org.helios.apmrouter.sender.SynchOpSupport;
import org.helios.apmrouter.util.SimpleLogger;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpRequestEncoder;
import org.jboss.netty.handler.codec.http.HttpResponseDecoder;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketVersion;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.json.JSONObject;

/**
 * <p>Title: WebSocketClient</p>
 * <p>Description: WebSocket client interface to server for browser/js emulation and non-UDP comms.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.wsclient.WebSocketClient</code></p>
 */

public class WebSocketClient extends OneToOneDecoder implements ChannelPipelineFactory {
	/** The URI of the APMRouter server to connect to */
	protected final URI wsuri;
	/** The client websocket handshaker */
	protected final WebSocketClientHandshaker handshaker;
	/** The client websocket channel handler */
	protected final WebSocketClientHandler wsClientHandler;
	/** The client synchronous invocation response handler */
	protected final SynchInvocationHandler synchHandler = new SynchInvocationHandler();
	/** The synchronous invocation handler pipeline key */
	protected final String synchHandlerKey;
	/** The client instance channel */
	protected final Channel channel;
	/** The session id assigned to a web-sock connection by the server */
	protected String sessionId=null;
	/** The client close future */
	protected final ChannelFuture closeFuture;
	/** The client bootstrap */
	protected final ClientBootstrap bootstrap;
	/** Indicates if this is a shared client, or an exclusive one */
	protected final boolean shared;
	
	/** The configured synch request timeout in ms. */
	protected long synchRequestTimeout = DEFAULT_SYNCH_TIMEOUT;
	
			
	/** The default synchronous request timeout in ms. */
	public static final long DEFAULT_SYNCH_TIMEOUT = 2000;
	
	
	/** The websocket client boss pool */
	protected static final Executor bossPool = ThreadPoolFactory.newCachedThreadPool("org.helios.apmrouter.client.websocket", "BossPool");
	/** The websocket client worker pool */
	protected static final Executor workerPool = ThreadPoolFactory.newCachedThreadPool("org.helios.apmrouter.client.websocket", "WokerPool");
	/** The websocket client application thread pool */
	protected static final Executor applicationPool = ThreadPoolFactory.newCachedThreadPool("org.helios.apmrouter.client.websocket", "ApplicationPool");
	
	/** The client channel factory */
	protected static final NioClientSocketChannelFactory channelFactory  = new NioClientSocketChannelFactory(bossPool, workerPool);
	/** The client channel channel group */
	protected static final ChannelGroup channelGroup = new DefaultChannelGroup("WebSocketClients");
	/** The client websocket client handshaker factory */
	protected static final WebSocketClientHandshakerFactory handshakerFactory = new WebSocketClientHandshakerFactory();
	/** A map of WebSocketClient keyed by the URI */
	protected static final Map<URI, WebSocketClient> clients = new ConcurrentHashMap<URI, WebSocketClient>();
	/** Empty header map const */
	public static final Map<String, String> WS_HEADER_MAP = Collections.unmodifiableMap(Collections.singletonMap("wc-client", "java-se"));
	/** Shared HttpResponse decoder handler */
	protected static final HttpResponseDecoder httpResponseDecoder = new HttpResponseDecoder();
	/** Shared HttpRequest encoder handler */
	protected static final HttpRequestEncoder httpRequestEncoder= new HttpRequestEncoder();
	/** Shared json codec */
	protected static final JsonCodec jsonHandler = new JsonCodec(applicationPool);
	
	/** The leading string in the session id message by the server when the agent first connects */
	public static final String SESSION_SIGNATURE = "{\"sessionid\":";

	
	static {
		InternalLoggerFactory.setDefaultFactory(new SimpleLoggerFactory());
		if(!ExtendedThreadManager.isInstalled()) {
			ExtendedThreadManager.install();
		}
	}

	/**
	 * Synchronously acquires a shared WebSocketClient instance for the passed URI
	 * @param wsuri The URI of the APMRouter server to connect to
	 * @return a WebSocketClient instance
	 */
	public static WebSocketClient getInstance(final URI wsuri) {
		if(wsuri==null) throw new IllegalArgumentException("The passed URI was null", new Throwable());
		if(!"ws".equals(wsuri.getScheme().toLowerCase())) throw new IllegalArgumentException("The passed URI had an invalid scheme [" + wsuri.getScheme() + "]", new Throwable());
		WebSocketClient client = clients.get(wsuri);
		if(client==null) {
			synchronized(clients) {
				client = clients.get(wsuri);
				if(client==null) {
					client = new WebSocketClient(true, wsuri);
					client.closeFuture.addListener(new ChannelFutureListener() {
						@Override
						public void operationComplete(ChannelFuture future) throws Exception {
							clients.remove(wsuri);
						}
					});
					clients.put(wsuri, client);
				}
			}
		}
		return client;
	}
	
	/**
	 * Synchronously acquires an exclusive WebSocketClient instance for the passed URI
	 * @param wsuri The URI of the APMRouter server to connect to
	 * @param listeners Optional listeners to register
	 * @return a WebSocketClient instance
	 */
	public static WebSocketClient getNewInstance(final URI wsuri, WebSocketEventListener...listeners) {
		if(wsuri==null) throw new IllegalArgumentException("The passed URI was null", new Throwable());
		if(!"ws".equals(wsuri.getScheme().toLowerCase())) throw new IllegalArgumentException("The passed URI had an invalid scheme [" + wsuri.getScheme() + "]", new Throwable());
		return new WebSocketClient(false, wsuri, listeners);
	}
	
	
	
	/**
	 * Creates a new WebSocketClient
	 * @param shared true for a shared client, false for an exclusive one
	 * @param wsuri The URI of the APMRouter server to connect to
	 * @param listeners Optional listeners to register
	 */
	protected WebSocketClient(boolean shared, URI wsuri, WebSocketEventListener...listeners) {
		this.wsuri = wsuri;
		this.shared = shared;
		bootstrap = new ClientBootstrap(channelFactory);
		bootstrap.setPipelineFactory(this);
		if(listeners!=null && listeners.length>0) {
			for(WebSocketEventListener listener: listeners) {
				if(listener!=null) {
					addWebSocketEventListener(listener);
				}
			}
		}
		handshaker = handshakerFactory.newHandshaker(wsuri, WebSocketVersion.V13, null, false, WS_HEADER_MAP);
		wsClientHandler = new WebSocketClientHandler(handshaker);
		ChannelFuture connectFuture = bootstrap.connect(new InetSocketAddress(wsuri.getHost(), wsuri.getPort()));
		connectFuture.syncUninterruptibly();
		channel = connectFuture.getChannel();
		synchHandlerKey = "Synch" + channel.getId();
		closeFuture = channel.getCloseFuture();
		channelGroup.add(channel);
		try {
			handshaker.handshake(channel).syncUninterruptibly();
		} catch (Exception ex) {
			throw new RuntimeException("WSClient [" + wsuri + "] failed to handshake", ex);
		}
	}
	
	/** Serial factory for request ids  */
	protected final AtomicInteger requestSerial = new AtomicInteger();

	/**
	 * Starts a client websocket daemon
	 * @param args As follows:<ul></ul>
	 */
	public static void main(String[] args) {
		log("WebSocketClient Test");
		WebSocketEventListener listener = new EmptyWebSocketResponseListener() {
			@Override
			public void onConnect(SocketAddress remoteAddress) {
				log("Connected to [" + remoteAddress + "]");
			}
			@Override
			public void onClose(SocketAddress remoteAddress) {
				log("Disconnected from [" + remoteAddress + "]");
			}
			@Override
			public void onError(SocketAddress remoteAddress, Throwable t) {
				log("Error on channel to [" + remoteAddress + "]");
				t.printStackTrace(System.err);
			}
			@Override
			public void onMessage(SocketAddress remoteAddress, JSONObject message) {
				try {
					log("Message Received from [" + remoteAddress + "]-->[\n" + message.toString(2) + "\n]");
				} catch (Exception ex) {
					ex.printStackTrace(System.err);
				}
			}
		};
		try {
			//jsonHandler.addWebSocketEventListener(listener);
			WebSocketClient client = new WebSocketClient(false, new URI("ws://localhost:8087/ws"));
			log("Client Connected:" + client.channel.getRemoteAddress());
			JSONObject request = new JSONObject();
			int reqId = client.requestSerial.incrementAndGet();
			request.put("rid", reqId);
			request.put("t", "req");
			request.put("svc", "sub");
			request.put("op", "start");
			JSONObject ags = new JSONObject();
			ags.put("es", "jmx");
			ags.put("esn", "service:jmx:local://DefaultDomain");
			ags.put("f", "org.helios.apmrouter.session:service=SharedChannelGroup");
			request.put("args", ags);
			client.channel.write(request);
			Thread.currentThread().join(60000);
			
			// function(callback, op, type, esn, filter, ex) {
			//"start", "jmx", "service:jmx:local://DefaultDomain", 
			//"org.helios.apmrouter.session:service=SharedChannelGroup");
			/*
			 * var req = {'t': 'req', 'svc' : 'sub', 'op' : op};
			 * 		var args = {'es' : type, 'esn': esn, 'f' : filter};
		if(ex!=null) {
			if($.isArray(ex)) {
				args['stf'] = ex;
			} else {
				args['exf'] = ex;
			}
			
		}
		req['args'] = args;
		var cb = callback;		
				
		var rid = $.apmr.send(req, function(data){
			var unsubKey = $.subscribe(topic, cb);			
			sub['ts'] = new Date().getTime();		
			sub['subId'] = data.msg;
		});
		$.apmr.config.subsByReqId[rid] = sub;
		sub['rid'] = rid;
		var topic = '/' + 'req' + '/' + rid;
		sub['topic'] = topic;

			 */
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
			System.exit(-1);
		} finally {
			log("Closing channels");
			try { channelGroup.close().awaitUninterruptibly(); } catch (Exception ex) {}
			log("Closing channel factory");
			try { channelFactory.releaseExternalResources(); } catch (Exception ex) {}
			
		}

	}
	
	/**
	 * Adds a web socket event listener
	 * @param listener the listener to add
	 */
	public void addWebSocketEventListener(WebSocketEventListener listener) {
		jsonHandler.addWebSocketEventListener(listener);
	}
	
	/**
	 * Removes a web socket event listener
	 * @param listener the listener to remove
	 */
	public void removeWebSocketEventListener(WebSocketEventListener listener) {
		jsonHandler.removeWebSocketEventListener(listener);
	}
	
	/**
	 * Adds the synchronous handler to the pipeline if not already installed
	 * @param rid The request of the pending response
	 * @param latch  The latch to countdown on response receipt
	 */
	protected void addSynchHandler(long rid, CountDownLatch latch) {
		if(channel.getPipeline().get(synchHandlerKey)==null) {
			channel.getPipeline().addLast(synchHandlerKey, synchHandler);
			synchHandler.prepSynchRequest(rid, latch);
		} else {
			throw new RuntimeException("Cannot wait on multiple synch requests. (rid=" + rid + ")", new Throwable());
		}
	}
	
	/**
	 * Indicates if this client is connected
	 * @return true if connected, false otherwise
	 */
	public boolean isConnected() {
		return (channel!=null && channel.isConnected());
	}
	
	/**
	 * Closes the client if it is exclusive
	 */
	public void close() {
		if(!shared) {
			channel.close();
		}
	}
	
	/**
	 * Removes the synchronous handler from the pipeline
	 */
	protected void clearSynchHandler() {
		channel.getPipeline().remove(synchHandlerKey);
	}
	
	
	/**
	 * Sends a JSON request to the server
	 * @param asynch true for asynch, false for synch
	 * @param request The JSONObject request
	 * @return The response to the send if synchronous, otherwise null
	 */
	public JSONObject sendRequest(final boolean asynch, final JSONObject request) {
		try {
			if(request==null) throw new IllegalArgumentException("The passed request was null", new Throwable());
			final long rid;
			final CountDownLatch latch;
			try {
				rid = request.getLong("rid");
			} catch (Exception ex) {
				throw new RuntimeException("No request id found in request", new Throwable());
			}	
			if(!asynch) {
				latch = SynchOpSupport.registerSynchOp(rid, synchRequestTimeout);
				addSynchHandler(rid, latch);									
			} else {
				latch = null;
			}
			ChannelFuture cf = channel.write(request);
			if(asynch) {
				cf.addListener(new ChannelFutureListener() {
					@Override
					public void operationComplete(ChannelFuture future) throws Exception {
						if(!future.isSuccess()) SimpleLogger.error("Asynch request failed [" + request.toString() + "]", future.getCause());
					}
				});
				return null;
			}
			return synchHandler.getSynchResponse(rid, synchRequestTimeout);
		} finally {
			clearSynchHandler();
		}
	}
	
	/**
	 * Sends a string request to the server asynchronously
	 * @param request The string request
	 */
	public void sendRequest(final CharSequence request) {
		if(request==null) throw new IllegalArgumentException("The passed request was null", new Throwable());
		channel.write(request).addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if(!future.isSuccess()) {
					SimpleLogger.error("Failed to send request to [", wsuri, "] ", future.getCause());
				}
			}
		});
	}
	


	
	
	public static void log(Object msg) {
		System.out.println(msg);
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelPipelineFactory#getPipeline()
	 */
	@Override
	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline pipeline = Channels.pipeline();
		pipeline.addLast("decoder", httpResponseDecoder);
		pipeline.addLast("encoder", httpRequestEncoder);
		pipeline.addLast("ws-handler", wsClientHandler);
		pipeline.addLast("json-handler", jsonHandler);
		pipeline.addLast("session-handler", this);
		return pipeline;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.handler.codec.oneone.OneToOneDecoder#decode(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.Channel, java.lang.Object)
	 */
	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
		SimpleLogger.info("Processing SessionID handshake [", msg, "]");
		ctx.getPipeline().remove(this);
		sessionId = (String)msg;		
		return null;
	}
	


	/**
	 * Returns 
	 * @return the synchRequestTimeout
	 */
	public long getSynchRequestTimeout() {
		return synchRequestTimeout;
	}


	/**
	 * Sets 
	 * @param synchRequestTimeout the synchRequestTimeout to set
	 */
	public void setSynchRequestTimeout(long synchRequestTimeout) {
		this.synchRequestTimeout = synchRequestTimeout;
	}

	/**
	 * Closes this client
	 */
	protected void _close() {
		this.channel.close().awaitUninterruptibly(500);
	}
	
	/**
	 * Stops the websocket client closing all connections and releasing the channel factory
	 */
	protected void shutdown() {
		channelGroup.close().awaitUninterruptibly(500);
		channelFactory.releaseExternalResources();
	}

	/**
	 * Returns the websocket client's WS URI
	 * @return the websocket client's WS URI
	 */
	public URI getWebSocketURI() {
		return wsuri;
	}


	/**
	 * Returns the session id assigned to a web-sock connection by the server
	 * @return the session id assigned to a web-sock connection by the server
	 */
	public String getSessionId() {
		return sessionId;
	}



}

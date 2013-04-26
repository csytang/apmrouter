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

import java.net.SocketAddress;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;

import org.helios.apmrouter.metric.AgentIdentity;
import org.helios.apmrouter.metric.MetricURIBuilder;
import org.helios.apmrouter.metric.MetricURIBuilder.SubscriptionType;
import org.helios.apmrouter.subscription.EmptyMetricURISubscriptionEventListener;
import org.helios.apmrouter.subscription.MetricURIEvent;
import org.helios.apmrouter.subscription.MetricURISubscriptionEventListener;
import org.helios.apmrouter.trace.ITracer;
import org.helios.apmrouter.trace.TracerFactory;
import org.helios.apmrouter.util.SimpleLogger;
import org.helios.apmrouter.util.URLHelper;
import org.json.JSONObject;

/**
 * <p>Title: WebSocketAgent</p>
 * <p>Description: Agent implementation using websockets.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.wsclient.WebSocketAgent</code></p>
 */

public class WebSocketAgent implements WebSocketEventListener {
	/** The WebSocketClient this agent will use to comm with the server */
	protected final WebSocketClient wsClient;
	/** The web sock listeners registered by this agent */
	protected final Set<WebSocketEventListener> webSockListeners = new CopyOnWriteArraySet<WebSocketEventListener>();
	
	/** A set of subscribed global MetricURISubscriptionEventListeners */
	protected final Set<MetricURISubscriptionEventListener> subListeners = new CopyOnWriteArraySet<MetricURISubscriptionEventListener>();
	/** A map of arrays of MetricURISubscriptionEventListeners keyed by the request id */
	protected final Map<Long, MetricURISubscriptionEventListener[]> reqListeners = new ConcurrentHashMap<Long, MetricURISubscriptionEventListener[]>();
	/** A metricURI to request id mapping */
	protected final Map<URI, Long> metricURIRequestIds = new ConcurrentHashMap<URI, Long>();
	
	/** Empty listener array const */
	protected static final MetricURISubscriptionEventListener[] EMPTY_LISTENER_ARR = {};
	
	
	public static void main(String[] args) {
		log("WebSocketAgent Test");
		WebSocketAgent agent = null;
		try {
			agent = WebSocketAgent.newInstance("ws://localhost:8087/ws");
			log("Connected to [" + agent.getWebSocketURI() + "]");
			agent.wsClient.setSynchRequestTimeout(5000);
//			int loop = 10000;
//			int dot = loop/100;
//			for(int i = 0; i < loop; i++) {
//				agent.getMetrics("DefaultDomain/njw810/APMRouterServer/platform=JVM/category=cpu");
//				System.out.print(".");
//				if(i%dot==0) System.out.println("--[ " + i);
//			}
//			log("Warmup Complete");
//			SystemClock.startTimer();
//			for(int i = 0; i < loop; i++) {
//				agent.getMetrics("DefaultDomain/njw810/APMRouterServer/platform=JVM/category=cpu");
//				System.out.print(".");
//				if(i%dot==0) System.out.println("--[ " + i);				
//			}
//			ElapsedTime et = SystemClock.endTimer();
//			log(et + "  Avg " + et.avgMs(loop) + " ms. ") ;
			String host = AgentIdentity.ID.getHost();
			String agentName = AgentIdentity.ID.getAgentName();
			log("AGENT: -----[ " + agentName + "@" + host + "]");
			ITracer tracer = TracerFactory.getTracer();
			URI metricURI = MetricURIBuilder.newBuilder("*", host, agentName).subType(SubscriptionType.NEW_METRIC).namespace("foo", "bar").build();
			final Set<Object> results = new CopyOnWriteArraySet<Object>();
			final AtomicLong st = new AtomicLong();
			agent.subscribeMetricURISynch(metricURI, new EmptyMetricURISubscriptionEventListener(){
				@Override
				public void onNewMetric(Object newMetric) {
					long elapsed = System.currentTimeMillis()-st.get();
					log("Received New Metric after [" + elapsed + "] ms. :" + newMetric);
					results.add(newMetric);
				}
			});
			//Thread.sleep(3000);
			log("Listener Registered");
			
			for(int x = 0; x < 100; x++) {
				st.set(System.currentTimeMillis());
				String mname = "C" + x;
				tracer.traceCounter(0, mname, "foo", "bar");
				log("Traced [" + mname + "]");
				Thread.sleep(5000);
			}			
			log("Done");
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		} finally {
			if(agent!=null) {
				try { agent.wsClient.shutdown(); } catch (Exception ex) {}
			}
		}
//		agent.subscribeMetricURIAsynch(URLHelper.toURI("DefaultDomain/njw810/APMRouterServer/platform=JVM/category=cpu"), new EmptyMetricURISubscriptionEventListener(){
//			final AtomicLong dataCount = new AtomicLong();
//			@Override
//			public void onMetricData(Object metricData) {
//				long cnt = dataCount.incrementAndGet();
//				if(cnt%5==0) {
//					log("Metrics Received:" + cnt);
//				}				
//			}});
//		final Thread t = Thread.currentThread();
//		agent.wsClient.channel.getCloseFuture().addListener(new ChannelFutureListener() {
//			@Override
//			public void operationComplete(ChannelFuture future) throws Exception {
//				t.interrupt();
//			}
//		});
//		try { Thread.currentThread().join(); } catch (Exception ex) {}
//		log("Exiting.....");
//		//agent.close();
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	/**
	 * Closes this agent's WebSocket connection
	 */
	public void close() {
		wsClient.close();
	}
	
	/**
	 * Returns the agent's WebSocket URI
	 * @return the agent's WebSocket URI
	 */
	public URI getWebSocketURI() {
		return wsClient.getWebSocketURI();
	}
	
	
	/**
	 * Registers a {@link MetricURISubscriptionEventListener}
	 * @param listener the listener to register
	 */
	public void registerMetricURISubscriptionEventListener(MetricURISubscriptionEventListener listener) {
		if(listener!=null) {
			subListeners.add(listener);
		}
	}
	
	/**
	 * Unregisters a {@link MetricURISubscriptionEventListener}
	 * @param listener the listener to unregister
	 */
	public void unRegisterMetricURISubscriptionEventListener(MetricURISubscriptionEventListener listener) {
		if(listener!=null) {
			subListeners.remove(listener);
		}
	}
	
	
	/**
	 * Returns a WebSocketAgent for the passed web socket URI
	 * @param wsUri the web socket URI to connect to
	 * @return a WebSocketAgent connected to the passed web socket URI
	 */
	public static WebSocketAgent newInstance(URI wsUri) {
		return new WebSocketAgent(WebSocketClient.getInstance(wsUri));
	}
	
	/**
	 * Returns a WebSocketAgent for the passed web socket URI
	 * @param wsUri the web socket URI to connect to
	 * @return a WebSocketAgent connected to the passed web socket URI
	 */
	public static WebSocketAgent newInstance(CharSequence wsUri) {
		return new WebSocketAgent(WebSocketClient.getInstance(URLHelper.toURI(wsUri)));
	}
	
	
	/**
	 * Adds a web socket event listener
	 * @param listener the listener to add
	 */
	public void addWebSocketEventListener(WebSocketEventListener listener) {
		if(listener!=null) {
			wsClient.addWebSocketEventListener(listener);
			webSockListeners.add(listener);
		}
	}
	
	/**
	 * Removes a web socket event listener
	 * @param listener the listener to remove
	 */
	public void removeWebSocketEventListener(WebSocketEventListener listener) {
		if(listener!=null) {
			wsClient.removeWebSocketEventListener(listener);
			webSockListeners.remove(listener);
		}
	}
	
	/**
	 * Removes all the web sock listeners registered from this agent
	 */
	protected void removeAllWebSocketEventListeners() {
		for(WebSocketEventListener listener: webSockListeners) {
			wsClient.removeWebSocketEventListener(listener);
		}
		webSockListeners.clear();
	}
	
	
	//212 748 4138

	/**
	 * Creates a new WebSocketAgent
	 * @param wsClient The WebSocketClient this agent will use to comm with the server
	 */
	protected WebSocketAgent(WebSocketClient wsClient) {
		super();
		this.wsClient = wsClient;
		this.wsClient.addWebSocketEventListener(this);
	}
	
	/**
	 * Returns the session id assigned to a web-sock connection by the server
	 * @return the session id assigned to a web-sock connection by the server
	 */
	public String getSessionId() {
		return wsClient.getSessionId();
	}
	
	
	/**
	 * Sends a MetricURI subscription request
	 * @param asynch true for an asynchronous call, false for a synchronous call
	 * @param metricURI The MetricURI to subscribe to
	 * @param listeners The listeners that will be invoked on with incoming data for this subscription. 
	 * If empty, the responses will be routed to the agent's global event listeners
	 * @return the subscription response if synch, otherwise, if asynch, returns null
	 */
	public JSONObject subscribeMetricURI(final boolean asynch, URI metricURI, MetricURISubscriptionEventListener...listeners) {
		JsonRequest request = JsonRequestBuilder.newBuilder()
				.put("svc", "catalog")
				.put("op", "submetricuri")
				.putMapPair("args", "uri", metricURI.toASCIIString())
				.build();
		if(asynch) {
			registerURISub(request.getRequestId(), metricURI, listeners);
		}
		registerURISub(request.getRequestId(), metricURI, listeners);
		JSONObject response = wsClient.sendRequest(asynch, request);
		if(!asynch) {
			registerURISub(request.getRequestId(), metricURI, listeners);
			return response;
		}
		return null;
	}
	
	/**
	 * Cancels a MetricURI subscription
	 * @param asynch true for an asynchronous call, false for a synchronous call
	 * @param metricURI The MetricURI to subscribe to
	 */
	public void unsubscribeMetricURI(final boolean asynch, URI metricURI) {
		JsonRequest request = JsonRequestBuilder.newBuilder()
				.put("svc", "catalog")
				.put("op", "unsubmetricuri")
				.putMapPair("args", "uri", metricURI.toASCIIString())
				.build();
		if(asynch) {
			registerURISub(request.getRequestId(), metricURI);
		}
		wsClient.sendRequest(asynch, request);
		if(!asynch) {
			registerURISub(request.getRequestId(), metricURI);
		}		
	}
	
	/**
	 * Asynchronously cancels a MetricURI subscription
	 * @param metricURI The MetricURI to subscribe to
	 */
	public void unsubscribeMetricURI(URI metricURI) {
		unsubscribeMetricURI(true, metricURI);
	}
	
	
	
	/**
	 * Subscribes to the passed MetricURI asynchronously
	 * @param metricURI The MetricURI to subscribe to
	 * @param listeners The listeners that will be invoked on with incoming data for this subscription. 
	 * If empty, the responses will be routed to the agent's global event listeners
	 */
	public void subscribeMetricURIAsynch(URI metricURI, MetricURISubscriptionEventListener...listeners) {
		subscribeMetricURI(true, metricURI, listeners);
	}
	
	/**
	 * Returns the metrics for the passed URI
	 * @param metricURI The metric uri
	 * @return A json object containing the metrics
	 */
	public JSONObject getMetrics(CharSequence metricURI) {
		if(metricURI==null) throw new IllegalArgumentException("The passed metricURI was null", new Throwable());
		return getMetrics(URLHelper.toURI(metricURI));
	}
	
	
	/**
	 * Returns the metrics for the passed URI
	 * @param metricURI The metric uri
	 * @return A json object containing the metrics
	 */
	public JSONObject getMetrics(URI metricURI) {
		if(metricURI==null) throw new IllegalArgumentException("The passed metricURI was null", new Throwable());
		JsonRequest request = JsonRequestBuilder.newBuilder()
				.put("svc", "catalog")
				.put("op", "resolvemetricuri")
				.putMapPair("args", "uri", metricURI.toASCIIString())
				.build();
		return wsClient.sendRequest(false, request);

	}

	/**
	 * Subscribes to the passed MetricURI synchronously
	 * @param metricURI The MetricURI to subscribe to
	 * @param listeners The listeners that will be invoked on with incoming data for this subscription. 
	 * If empty, the responses will be routed to the agent's global event listeners
	 * @return the subscription response 
	 */
	public JSONObject subscribeMetricURISynch(URI metricURI, MetricURISubscriptionEventListener...listeners) {
		return subscribeMetricURI(false, metricURI, listeners);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.wsclient.WebSocketEventListener#onConnect(java.net.SocketAddress)
	 */
	@Override
	public void onConnect(SocketAddress remoteAddress) {
		SimpleLogger.info("Connected to [", remoteAddress, "]");
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.wsclient.WebSocketEventListener#onClose(java.net.SocketAddress)
	 */
	@Override
	public void onClose(SocketAddress remoteAddress) {
		SimpleLogger.info("Closed [", remoteAddress, "]");
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.wsclient.WebSocketEventListener#onError(java.net.SocketAddress, java.lang.Throwable)
	 */
	@Override
	public void onError(SocketAddress remoteAddress, Throwable t) {
		SimpleLogger.error("WS error from [", remoteAddress, "]", t);
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.wsclient.WebSocketEventListener#onMessage(java.net.SocketAddress, org.json.JSONObject)
	 */
	@Override
	public void onMessage(SocketAddress remoteAddress, JSONObject message) {
		try {
			//SimpleLogger.info(message.toString(2));
			long rerid = message.getLong("rerid");
			MetricURISubscriptionEventListener[] listeners = getListenersForRerid(rerid);			
			MetricURIEvent event = MetricURIEvent.forEvent(message.getString("t"));
			switch(event) {
				
			case DATA:
				for(MetricURISubscriptionEventListener listener: listeners) {
					listener.onMetricData(message);
				}								
				break;
			case NEW_METRIC:
				for(MetricURISubscriptionEventListener listener: listeners) {
					listener.onNewMetric(message);
				}				
				break;
			case STATE_CHANGE_ENTRY:
				for(MetricURISubscriptionEventListener listener: listeners) {
					listener.onMetricStateChangeEntry(message);
				}				
				break;
			case STATE_CHANGE_EXIT:
				for(MetricURISubscriptionEventListener listener: listeners) {
					listener.onMetricStateChangeExit(message);
				}				
				break;
			case STATE_CHANGE:
				for(MetricURISubscriptionEventListener listener: listeners) {
					listener.onMetricStateChange(message);
				}				
				break;				
			default:
				break;
				
			}
		} catch (Exception ex) {
			SimpleLogger.error("Failed to process WebSocket JSON Response [", message, "]", ex);
		}
		
	}
	
	/**
	 * Returns the listener array for the passed rerid
	 * @param rerid The in-reference-to request id
	 * @return an array of listeners which will be empty if the rerid was not found
	 */
	protected MetricURISubscriptionEventListener[] getListenersForRerid(long rerid) {
		MetricURISubscriptionEventListener[] listeners = reqListeners.get(rerid);
		return listeners==null ? subListeners.toArray(new MetricURISubscriptionEventListener[0]) : listeners;
	}
	
	/**
	 * Registers a metricURI subscription
	 * @param rid The request id
	 * @param metricUri The metric URI subscribed to
	 * @param listeners The listeners to be registered
	 */
	protected void registerURISub(long rid, URI metricUri, MetricURISubscriptionEventListener...listeners) {
		metricURIRequestIds.put(metricUri, rid);
		final MetricURISubscriptionEventListener[] _listeners = (listeners==null || listeners.length==0) ? EMPTY_LISTENER_ARR : listeners; 
		reqListeners.put(rid, _listeners);		
	}
	
	/**
	 * Cleans up a terminated metric URI subscription
	 * @param metricUri The metric URI subscription to clean up
	 */
	protected void cleanupURISub(URI metricUri) {
		Long rid = metricURIRequestIds.remove(metricUri);
		if(rid!=null) {
			reqListeners.remove(rid);
		}
	}
	
	
	
	/*
	 * 
	 * 	@JSONRequestHandler(name="submetricuri")
	public void subscribeMetricURI(JsonRequest request, Channel channel) {
		String muri = request.getArgument("uri");
		try {

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

	 */
	
}

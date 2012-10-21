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
package org.helios.apmrouter.server.unification.pipeline.http;

import static org.jboss.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static org.jboss.netty.handler.codec.http.HttpHeaders.setContentLength;
import static org.jboss.netty.handler.codec.http.HttpMethod.GET;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.helios.apmrouter.dataservice.json.JSONRequestRouter;
import org.helios.apmrouter.server.services.session.ChannelType;
import org.helios.apmrouter.server.services.session.SharedChannelGroup;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import org.jboss.netty.util.CharsetUtil;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>Title: WebSocketRequestHandler</p>
 * <p>Description: HTTP categorized web socket request handler</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.unification.pipeline.http.WebSocketRequestHandler</code></p>
 */

public class WebSocketRequestHandler extends AbstractHttpRequestHandler implements ChannelUpstreamHandler {

	
	
	/** The websocket handshaker */
	private WebSocketServerHandshaker handshaker;
	/** The JSON Request Router */
	protected JSONRequestRouter router = null;
	/** The websocket channel handler to be installed once a handshake is complete */
	protected final WebSocketChannelHandler wsChannelHandler = new WebSocketChannelHandler();

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.unification.pipeline.http.HttpRequestHandler#handle(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent, org.jboss.netty.handler.codec.http.HttpRequest, java.lang.String)
	 */
	@Override
	public void handle(ChannelHandlerContext ctx, MessageEvent e, HttpRequest request, String path) throws Exception {
        if (request.getMethod() != GET) {
            sendHttpResponse(ctx, request, new DefaultHttpResponse(HTTP_1_1, FORBIDDEN));
            return;
        }
        
        // Handshake
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(getWebSocketLocation(request), null, false);
        this.handshaker = wsFactory.newHandshaker(request);
        if (this.handshaker == null) {
            wsFactory.sendUnsupportedWebSocketVersionResponse(ctx.getChannel());
        } else {
        	ChannelFuture cf = handshaker.handshake(ctx.getChannel(), request); 
        	ctx.getPipeline().addLast("ws", wsChannelHandler);
        	ctx.getPipeline().addLast("wsRequestHandler", this);
            cf.addListener(WebSocketServerHandshaker.HANDSHAKE_LISTENER);
            cf.addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture f) throws Exception {
					if(f.isSuccess()) {
						Channel wsChannel = f.getChannel();
						SharedChannelGroup.getInstance().add(f.getChannel(), ChannelType.WEBSOCKET_REMOTE, "WebSocketClient-" + f.getChannel().getId(), "", "");
						wsChannel.write(new JSONObject(Collections.singletonMap("sessionid", wsChannel.getId())));
						//wsChannel.getPipeline().remove(DefaultChannelHandler.NAME);
					}
				}
			});
        }
	}

	
	
    /**
     * Sends an HTTP error response
     * @param ctx The channel handler context
     * @param req The HTTP request
     * @param res The HTTP response
     */
    private void sendHttpResponse(ChannelHandlerContext ctx, HttpRequest req, HttpResponse res) {
        // Generate an error page if response status code is not OK (200).
        if (res.getStatus().getCode() != 200) {
            res.setContent(ChannelBuffers.copiedBuffer(res.getStatus().toString(), CharsetUtil.UTF_8));
            setContentLength(res, res.getContent().readableBytes());
        }

        // Send the response and close the connection if necessary.
        ChannelFuture f = ctx.getChannel().write(res);
        if (!isKeepAlive(req) || res.getStatus().getCode() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }
	
	
	/**
	 * Processes a websocket request
	 * @param ctx The channel handler context
	 * @param frame The websocket frame request to process
	 */
	public void handleRequest(ChannelHandlerContext ctx, WebSocketFrame frame) {
        // Check for closing frame
        if (frame instanceof CloseWebSocketFrame) {
            this.handshaker.close(ctx.getChannel(), (CloseWebSocketFrame) frame);
            return;
        } else if (frame instanceof PingWebSocketFrame) {
            ctx.getChannel().write(new PongWebSocketFrame(frame.getBinaryData()));
            return;
        } else if (!(frame instanceof TextWebSocketFrame)) {
            throw new UnsupportedOperationException(String.format("%s frame types not supported", frame.getClass()
                    .getName()));
        }

        
        String request = ((TextWebSocketFrame) frame).getText();
        JSONObject wsRequest = null;
        try {
        	wsRequest = new JSONObject(request);
        	if("who".equals(wsRequest.get("t"))) {
        		SocketAddress sa = ctx.getChannel().getRemoteAddress();
        		String host = "unknown";
        		String agent = "unknown";
        		if(sa!=null) {
        			host = ((InetSocketAddress)sa).getHostName();        			
        		}
        		if(wsRequest.get("agent")!=null) {
        			agent = wsRequest.get("agent").toString();
        		}
        		SharedChannelGroup.getInstance().add(ctx.getChannel(), ChannelType.WEBSOCKET_REMOTE, "ClientWebSocket", host, agent);
        	} else {
        		router.invoke(wsRequest, ctx.getChannel());
        	}
        	
        		
        } catch (Exception ex) {
        	log.error("Failed to parse request [" + request + "]", ex);
        	// FIXME:  Need to send the browser an error here.
        }
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.unification.pipeline.http.HttpRequestHandler#getUriPatterns()
	 */
	@Override
	public Set<String> getUriPatterns() {		
		return new HashSet<String>(Arrays.asList("/ws"));
	}
	

    /**
     * Generates a websocket URL
     * @param req The http request
     * @return The websocket URL
     */
    private String getWebSocketLocation(HttpRequest req) {
        return "ws://" + req.getHeader(HttpHeaders.Names.HOST) + "/ws";
    }

	/**
	 * Returns the websocket handshaker
	 * @return the handshaker
	 */
	public WebSocketServerHandshaker getHandshaker() {
		return handshaker;
	}

	/**
	 * Sets the json request router
	 * @param router the router to set
	 */
	@Autowired(required=true)
	public void setRouter(JSONRequestRouter router) {
		this.router = router;
	}



	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelUpstreamHandler#handleUpstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
	 */
	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		if(e instanceof MessageEvent) {
			MessageEvent me = (MessageEvent)e;
			Object message = me.getMessage();
			if(message instanceof WebSocketFrame) {
				handleRequest(ctx, (WebSocketFrame)message);
			} else {
				ctx.sendUpstream(e);
			}			
		} else {
			ctx.sendUpstream(e);
		}
		
		
	}
	
	

}

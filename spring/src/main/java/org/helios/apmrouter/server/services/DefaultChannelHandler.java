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
package org.helios.apmrouter.server.services;

import java.util.Map;

import org.apache.log4j.Logger;
import org.helios.apmrouter.server.services.handlergroups.websockets.WebSocketServerHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame;

/**
 * <p>Title: DefaultChannelHandler</p>
 * <p>Description: The initial and default channel handler inserted into all pipelines. This handler is intended to
 * examine the request URI and reconfigure the pipeline to handle the next request.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.services.DefaultChannelHandler</code></p>
 */

public class DefaultChannelHandler extends SimpleChannelUpstreamHandler {
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());

	/** The name of this handler in the pipeline */
	public static final String NAME = "router";

	/** A map of pipeline modifiers keyed by the URI that they are attached to */
	protected final Map<String, PipelineModifier> modifierMap;
	/**
	 * Creates a new DefaultChannelHandler
	 * @param modifierMap The map of modifiers, keyed by the URI they accept.
	 */
	public DefaultChannelHandler(final Map<String, PipelineModifier> modifierMap) {
		this.modifierMap = modifierMap;		
	}
	
    /**
     * {@inheritDoc}
     * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#messageReceived(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent)
     */
	@Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {   
    	Object message = e.getMessage();
    	if(message instanceof HttpRequest) { 
	        HttpRequest request = (HttpRequest)message;
	        PipelineModifier modifier = getModifier(request.getUri());
	        if(!modifier.getName().equals(ctx.getAttachment())) {
	        	clearLastHandler(ctx.getPipeline());
	        	modifier.modifyPipeline(ctx.getPipeline());
	        	ctx.setAttachment(modifier.getName());
	        }
    	} else if (message instanceof CloseWebSocketFrame) {
    		handleWebSocketFrame(ctx, (CloseWebSocketFrame) message);
    		return;
    	} else {
    		log.info("\n\t=====================\n\tNon HTTP Message Received\n\t" + message.getClass().getName() + "\n\t=====================\n");
    		ctx.sendDownstream(e);
    	}
        ctx.sendUpstream(e);
    }
    
    /**
     * Handles a websocket close request
	 * @param ctx The channel handler context
	 * @param closeFrame The websocket close frame
	 */
	protected void handleWebSocketFrame(ChannelHandlerContext ctx, CloseWebSocketFrame closeFrame) {		
		log.info("Closing WebSocket On Channel [" + ctx.getChannel().getId() + "]");
		((WebSocketServerHandler)ctx.getPipeline().toMap().get("ws")).getHandshaker().close(ctx.getChannel(), closeFrame);		
	}

	/**
     * Removes the last handler from the pipeline unless the last handler is this handler.
     * @param pipeline The pipeline to operate on
     */
    protected void clearLastHandler(ChannelPipeline pipeline) {
    	if(this!=pipeline.getLast()) {
    		pipeline.removeLast();
    	}
    }
    
    /**
     * Returns the pipeline modifier for the passed URI
     * @param uri The URI of the incoming request to get the pipeline modifier for
     * @return the pipeline modifier
     */
    protected PipelineModifier getModifier(String uri) {
    	String[] frags = uri.trim().split("\\/");
    	for(String frag: frags) {
    		if(frag.trim().isEmpty()) continue;
    		PipelineModifier modifier = modifierMap.get(frag.trim());
    		if(modifier!=null) {
    			return modifier;
    		}
    	}
    	return modifierMap.get("");
    }

}

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
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.helios.apmrouter.server.ServerComponentBean;
import org.helios.apmrouter.server.services.handlergroups.URIHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.handler.stream.ChunkedWriteHandler;
import org.jboss.netty.logging.InternalLogLevel;
import org.springframework.jmx.export.annotation.ManagedAttribute;

/**
 * <p>Title: ServerPipelineFactory</p>
 * <p>Description: The factory that creates pipelines for each connecting client. The handlers that are inserted into the pipeline
 * will be specific to the type of Ajax push that the client requests.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.services.ServerPipelineFactory</code></p>
 */

public class ServerPipelineFactory extends ServerComponentBean implements ChannelPipelineFactory {
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());
	/** The modifier map */
	protected final Map<String, PipelineModifier> modifierMap = new ConcurrentHashMap<String, PipelineModifier>();
	/** The modifier URI map */
	protected final Map<String, PipelineModifier> uriMap = new ConcurrentHashMap<String, PipelineModifier>();
	
	/** The logging handler logger */
	protected final Logger logHandlerLogger = Logger.getLogger(LoggingHandler.class);
	
	/**
	 * Creates a new ServerPipelineFactory
	 */
	public ServerPipelineFactory() {

	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#doStart()
	 */
	@Override
	public void doStart() throws Exception {
		Map<String, PipelineModifier> modifiers = applicationContext.getBeansOfType(PipelineModifier.class);
		for(Map.Entry<String, PipelineModifier> entry: modifiers.entrySet()) {
			if(!modifierMap.containsKey(entry.getKey())) {
				modifierMap.put(entry.getKey(), entry.getValue());
			}
			for(String uri: entry.getValue().getUriPatterns()) {
				if(uriMap.containsKey(uri)) {
					warn("PipelineModifier URI overlap. The modifier [", uriMap.get(uri).getName(), "] was already registered so [", entry.getValue().getName(), "] will not be registered for URI [", uri, "]" );
				} else {
					uriMap.put(uri, entry.getValue());
				}
			}
		}
		info("DataService ServerPipelineFactory started with [", modifierMap.size(), "] pipeline modifiers");
	}
	
	/**
	 * Adds a modifier to the factory
	 * @param name The URI that the modifier responds to
	 * @param modifier The modifier to add
	 */
	public void addModifier(String name, PipelineModifier modifier) {
		modifierMap.put(name, modifier);
	}
	
	/** The port unification pipeline switch */
	private final ProtocolSwitch ps = new ProtocolSwitch();
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelPipelineFactory#getPipeline()
	 */
	@Override
	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline pipeline = Channels.pipeline();
		pipeline.addLast("protocolSwitch", ps);
		pipeline.addLast("decoder", new HttpRequestDecoder());
		pipeline.addLast("aggregator", new HttpChunkAggregator(65536));
		pipeline.addLast("encoder", new HttpResponseEncoder());
		pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());
		if(logHandlerLogger.isDebugEnabled()) {
			pipeline.addLast("logger", new LoggingHandler(InternalLogLevel.INFO));
		}		
		pipeline.addLast(DefaultChannelHandler.NAME, new DefaultChannelHandler(uriMap)); 
		if(log.isDebugEnabled()) log.debug("Created Pipeline [" + pipeline + "]");
		return pipeline;
	}

	
	/**
	 * Returns the number of pipeline modifiers registered
	 * @return the number of pipeline modifiers registered
	 */
	@ManagedAttribute(description="The number of pipeline modifiers registered")
	public int getModifierCount() {
		return modifierMap.size();
	}
	
	/**
	 * Returns the names of the registered pipeline modifiers
	 * @return the names of the registered pipeline modifiers
	 */
	@ManagedAttribute(description="The names of the registered pipeline modifiers")
	public String[] getModifierNames() {
		return modifierMap.keySet().toArray(new String[modifierMap.size()]);
	}
	
}

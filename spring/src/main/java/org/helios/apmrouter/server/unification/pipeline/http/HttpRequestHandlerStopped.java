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

import org.springframework.context.ApplicationEvent;

/**
 * <p>Title: HttpRequestHandlerStopped</p>
 * <p>Description: A Spring app event published when an {@link HttpRequestHandler} starts</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.unification.pipeline.http.HttpRequestHandlerStopped</code></p>
 */
public class HttpRequestHandlerStopped extends ApplicationEvent {
	/**  */
	private static final long serialVersionUID = -724220750156079572L;
	/** The name of the bean */
	protected final String beanName;
	
	/**
	 * Creates a new HttpRequestHandlerStopped
	 * @param source The {@link HttpRequestHandler} that stopped
	 * @param beanName The name of the bean
	 */
	public HttpRequestHandlerStopped(HttpRequestHandler source, String beanName) {
		super(source);
		this.beanName = beanName;
	}
	
	/**
	 * Returns the bean name of the event originator 
	 * @return the bean name of the event originator
	 */
	public String getBeanName() {
		return beanName;
	}
	
	/**
	 * Returns the associated {@link HttpRequestHandler}
	 * @return the associated {@link HttpRequestHandler}
	 */
	public HttpRequestHandler getHandler() {
		return (HttpRequestHandler)getSource();
	}
	
	

}

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
package org.helios.apmrouter.instrumentation;

import java.lang.annotation.*;

/**
 * <p>Title: Trace</p>
 * <p>Description: AOP instrumentation directive</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.instrumentation.Trace</code></p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Trace {
	/**
	 * Specifies a TXContext operation
	 */
	public TXDirective txcontext() default TXDirective.NOOP; 
	/**
	 * Specifies the metric name
	 */
	public String name() default "";
	/**
	 * Specifies the metric name
	 */
	public String[] namespace() default {};
	
	/**
	 * Specifies the runtime performance data points that will be measured on an intercepted method
	 */
	public TraceCollection[] collections() default {};
	
	/**
	 * Specifies the dynamic script that an object reference will be passed to for trace processing.
	 */
	public String script() default "";
	
	/**
	 * Defines an expression that navigates to an {@link AccessibleObjectReference} and extracts it, passing it on for trace processing
	 */
	public String accessible() default "";
	
}

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
package org.helios.collector.jmx.identifiers;

import javax.management.*;
import java.io.IOException;


/**
 * <p>Title: IMBeanServerIdentifier</p>
 * <p>Description: Defines a class that retrieves a host name and VM identifier from an MBeanServer connection.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.collectors.jmx.identifiers.IMBeanServerIdentifier</code></p>
 */

public interface IMBeanServerIdentifier {
	/**
	 * Returns a string array containing the host and vm identifiers
	 * @param connection The MBeanServerConnection to get the identifiers from
	 * @return A String array with the host id in [0], the VM id in [1] and the JMX Default Domain in [2]
	 * @throws IOException 
	 * @throws ReflectionException 
	 * @throws MBeanException 
	 * @throws InstanceNotFoundException 
	 * @throws AttributeNotFoundException 
	 */
	public String[] getHostVMId(MBeanServerConnection connection) throws AttributeNotFoundException, InstanceNotFoundException, MBeanException, ReflectionException, IOException;
}

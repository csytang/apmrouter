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
package org.helios.apmrouter.jmx.connector.protocol.mxrp;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorProvider;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;

/**
 * <p>Title: ClientProvider</p>
 * <p>Description: JMX remoting client provider for acquiring {@link MBeanServerConnection}s to agents from external JVMs proxying through the APMRouter server.</p>
 * <p>E.g.  <code>service:jmx:mxrp://localhost:8002/service:jmx:udp://com.cpex.ne-wk-nwhi-01/marginservice</code></p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.jmx.connector.mxl.ClientProvider</code></p>
 */

public class ClientProvider implements JMXConnectorProvider {

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnectorProvider#newJMXConnector(javax.management.remote.JMXServiceURL, java.util.Map)
	 */
	@Override
	public JMXConnector newJMXConnector(JMXServiceURL serviceURL,
			Map<String, ?> environment) throws IOException {
		if (!serviceURL.getProtocol().equals("mxrp")) {
            throw new MalformedURLException("Protocol not mxrp: " +
                                            serviceURL.getProtocol());
        }
		return null;
	}

}

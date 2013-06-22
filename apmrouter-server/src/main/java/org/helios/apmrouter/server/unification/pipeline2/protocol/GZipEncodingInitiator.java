/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2013, Helios Development Group and individual contributors
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
package org.helios.apmrouter.server.unification.pipeline2.protocol;

import org.helios.apmrouter.server.unification.pipeline2.AbstractInitiator;
import org.helios.apmrouter.server.unification.pipeline2.ProtocolSwitchContext;
import org.helios.apmrouter.server.unification.pipeline2.SwitchPhase;
import org.jboss.netty.buffer.ChannelBuffer;

/**
 * <p>Title: GZipEncodingInitiator</p>
 * <p>Description: An initiator that detects a GZip encoded stream of data</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.unification.pipeline2.encoding.GZipEncodingInitiator</code></p>
 */

public class GZipEncodingInitiator extends AbstractInitiator  {

	/**
	 * Creates a new GZipEncodingInitiator
	 */
	public GZipEncodingInitiator() {
		super(2, "gzip");
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.unification.pipeline2.Initiator#match(org.jboss.netty.buffer.ChannelBuffer)
	 */
	@Override
	public Object match(ChannelBuffer buff) {
		return isGzip(buff) ? true : null;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.unification.pipeline2.Initiator#process(org.helios.apmrouter.server.unification.pipeline2.ProtocolSwitchContext, java.lang.Object)
	 */
	@Override
	public SwitchPhase process(ProtocolSwitchContext ctx, Object matchKey) {
		return SwitchPhase.CONTENTDETECT;
	}

	/**
	 * Determines if the two passed bytes represent a gzipped stream of data
	 * @param magic1 The first byte of the incoming request
	 * @param magic2 The second byte of the incoming request
	 * @return true if the incoming payload is gzipped, false otherwise
	 */
	public static boolean isGzip(int magic1, int magic2) {
		return magic1 == 31 && magic2 == 139;	
	}	
	
	
	/**
	 * Determines if the channel is carrying a gzipped payload
	 * @param buffer The channel buffer to check
	 * @return true if the incoming payload is gzipped, false otherwise
	 */
	public static boolean isGzip(ChannelBuffer buffer) {
		return isGzip(buffer.getUnsignedByte(0), buffer.getUnsignedByte(1));
	}	

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.unification.pipeline2.encoding.EncodingInitiator#requiresFullPayload()
	 */
	@Override
	public boolean requiresFullPayload() {
		// TODO Auto-generated method stub
		return false;
	}

}

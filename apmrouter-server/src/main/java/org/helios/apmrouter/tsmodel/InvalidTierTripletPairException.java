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
package org.helios.apmrouter.tsmodel;

/**
 * <p>Title: InvalidTierTripletPairException</p>
 * <p>Description: Tier exception thrown when the tier parser detects an invalid triplet pair</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.tsmodel.InvalidTierTripletPairException</code></p>
 */

public class InvalidTierTripletPairException extends InvalidTierModelException {

	/**  */
	private static final long serialVersionUID = 3120978420655148026L;

	/**
	 * Creates a new InvalidTierTripletPairException
	 * @param message  A message describing why the triplet pair is invalid
	 */
	public InvalidTierTripletPairException(String message) {
		super(message);		
	}

}

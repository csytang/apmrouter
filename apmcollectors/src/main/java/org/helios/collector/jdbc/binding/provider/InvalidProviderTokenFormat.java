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
package org.helios.collector.jdbc.binding.provider;

/**
 * <p>Title: InvalidProviderTokenFormat</p>
 * <p>Description: Exception thrown when parsing an invalid provider token.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class InvalidProviderTokenFormat extends Exception {

	/**  */
	private static final long serialVersionUID = -2896393084454687126L;

	/**
	 * 
	 */
	public InvalidProviderTokenFormat() {
	}

	/**
	 * @param arg0
	 */
	public InvalidProviderTokenFormat(String arg0) {
		super(arg0);
	}

	/**
	 * @param arg0
	 */
	public InvalidProviderTokenFormat(Throwable arg0) {
		super(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public InvalidProviderTokenFormat(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

}

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
package org.helios.apmrouter.destination.chronicletimeseries;

import java.util.Date;

/**
 * <p>Title: ChronicleTierMXBean</p>
 * <p>Description: MXBean interface for ChronicleTier JMX management interface.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.chronicletimeseries.ChronicleTierMXBean</code></p>
 */

public interface ChronicleTierMXBean {
	/**
	 * Returns the name of this chronicle tier
	 * @return the name of this chronicle tier
	 */
	public String getName();
	
	/**
	 * Returns the size of this chronicle tier
	 * @return the size of this chronicle tier
	 */
	public long getSize();
	
	/**
	 * Clears the chronicle
	 */
	public void clear();

	/**
	 * Closes the chronicle
	 */
	public void close();
	
	/**
	 * Returns the chronicle path 
	 * @return the chroniclePath
	 */
	public String getChroniclePath();	
	
	/**
	 * Returns the earliest period end timestamp in this tier
	 * @return the earliest period end timestamp in this tier
	 */
	public Date getStartPeriod();

	/**
	 * Returns the latest period end timestamp in this tier
	 * @return the latest period end timestamp in this tier
	 */
	public Date getEndPeriod();
	
	/**
	 * Returns the size of the index data file
	 * @return the size of the index data file
	 */
	public long getIndexSize();
	
	/**
	 * Returns the size of the data file
	 * @return the size of the data file
	 */
	public long getDataSize();
	
	/**
	 * Dumps a formatted output of the excerpt at the passed index
	 * @param index The index to dump
	 * @return A formatted string
	 */
	public String dump(long index);
	
	
}

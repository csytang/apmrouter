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
package org.helios.apmrouter.server.services.mtxml;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.cliffc.high_scale_lib.Counter;
import org.cliffc.high_scale_lib.NonBlockingHashMap;

/**
 * <p>Title: SanStatsParsingContext</p>
 * <p>Description: A context shared across parsing operations for a single san stat xml document</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.services.mtxml.SanStatsParsingContext</code></p>
 */

public class SanStatsParsingContext {
/*
the tags that are important are the vv_name, host_name, port_node
port_node is not as important as vv_name and host
typically we would want to see the metrics broken down like this
sum across the array
sum by host
sum by vv_name
sum by port_node
on a deep dive trying to find issues on a host we might break it down by: host, vv_name
host, port
host, vv_name, port
the top two would be sum by host and sum by vv_name

 */
	/** An unknown value */
	public static final String UNKNOWN = "<unknown>";
	
	/** The system serial number */
	private String serialNumber = UNKNOWN;
	/** The system name */
	private String systemName = UNKNOWN;
	/** The system cpu speed */
	private int cpuMhz = -1;
	/** The ip name */
	private String ipName = UNKNOWN;
	/** The system os version */
	private String osVersion = UNKNOWN;
	/** The system model name */
	private String modelName = UNKNOWN;
	/** The cache size in mb */
	private int cacheSize = -1;
	
	/** The tag name for the current timestamp */
	public static final String NOW = "now";	
	/** The tag name for the queue length */
	public static final String QUEUE_LENGTH = "qlen";
	/** The tag name for the busy time */
	public static final String BUSY_TIME = "busy";
	/** The tag name for the read count */
	public static final String READ_COUNT = "rcount";
	/** The tag name for the bytes read count */
	public static final String READ_BYTES = "rbytes";
	/** The tag name for the read errors */
	public static final String READ_ERRORS = "rerror";
	/** The tag name for the read drops */
	public static final String READ_DROPS= "rdrops";
	/** The tag name for the read ticks */
	public static final String READ_TICKS = "rticks";
	/** The tag name for the write count */
	public static final String WRITE_COUNT = "wcount";
	/** The tag name for the bytes write count */
	public static final String WRITE_BYTES = "wbytes";
	/** The tag name for the write errors */
	public static final String WRITE_ERRORS = "werror";
	/** The tag name for the write drops */
	public static final String WRITE_DROPS= "wdrops";
	/** The tag name for the write ticks */
	public static final String WRITE_TICKS = "wticks";
	
	
	/** The tag name for the virtual vlun name */
	public static final String VVNAME = "vv_name";
	/** The tag name for the virtual vlun host name */
	public static final String VVHOSTNAME = "host_name";
	/** The tag name for the virtual vlun port node */
	public static final String PORTNODE = "port_node";
	
	

	
	
	/** The number of successfully parsed lun fragments */
	private final AtomicInteger lunsParsed = new AtomicInteger(0);
	/** The completion queue */
	private final BlockingQueue<Map<String, String>> completionQueue = new ArrayBlockingQueue<Map<String, String>>(3000, false);
	
	
	// ================================================================================================
	//   Aggregations
	// ================================================================================================
	/** The total values across the array */
	protected final NonBlockingHashMap<String, Counter> arrayTotals = new NonBlockingHashMap<String, Counter>(); 
	/** The total values across each host in the array */
	protected final NonBlockingHashMap<String, NonBlockingHashMap<String, Counter>> hostTotals = new NonBlockingHashMap<String, NonBlockingHashMap<String, Counter>>(); 
	/** The total values across each vv in the array */
	protected final NonBlockingHashMap<String, NonBlockingHashMap<String, Counter>> vvTotals = new NonBlockingHashMap<String, NonBlockingHashMap<String, Counter>>(); 
	/** The total values across each port node in the array */
	protected final NonBlockingHashMap<String, NonBlockingHashMap<String, Counter>> pnTotals = new NonBlockingHashMap<String, NonBlockingHashMap<String, Counter>>(); 

	public int getTotalHosts() {
		return hostTotals.size();
	}
	public int getTotalVVHosts() {
		return vvTotals.size();
	}
	public int getTotalPortNodes() {
		return pnTotals.size();
	}

	
	
	/**
	 * Intializes the passed counter map with a target key and counter for each key
	 * @param counterMap The map to insert into
	 * @param keys The keys to insert
	 */
	protected void initCounters(NonBlockingHashMap<String, Counter> counterMap, String...keys) {
		for(String s: keys) {
			counterMap.put(s, new Counter());
		}
	}
	
	/**
	 * Creates a new SanStatsParsingContext and initializes the aggregate counters
	 */
	public SanStatsParsingContext() {
		initCounters(arrayTotals, QUEUE_LENGTH, BUSY_TIME, READ_COUNT, READ_BYTES, READ_ERRORS, READ_DROPS, READ_TICKS, WRITE_COUNT, WRITE_BYTES, WRITE_ERRORS, WRITE_DROPS, WRITE_TICKS);
	}
	
	
	
	/**
	 * Sets the system info
	 * @param sysInfo a map of the system info values keyed by the xml tag name
	 */
	public void setSysInfo(Map<String, String> sysInfo) {
		serialNumber = sysInfo.get("serial_number");
		systemName = sysInfo.get("system_name");
		ipName = sysInfo.get("ip_name");
		osVersion = sysInfo.get("os_version");
		modelName = sysInfo.get("model_name");
		cacheSize = Integer.parseInt(sysInfo.get("ch_size_mb"));
		cpuMhz = Integer.parseInt(sysInfo.get("cpu_mhz"));
		
	}
	
	/**
	 * Increments the counter in the passed aggregate by the passed amount
	 * @param counterMap The map to increment
	 * @param key The target key
	 * @param value The value to increment by
	 */
	protected void increment(NonBlockingHashMap<String, Counter> counterMap, String key, long value) {
		if(counterMap!=null) {
			Counter ctr = counterMap.get(key);
			if(ctr!=null) ctr.add(value);			
		}
	}
	
	/**
	 * Returns the value for the passed key from the array aggregate
	 * @param key The key to retrieve the value for
	 * @return the long value
	 */
	public long getArrayValue(String key) {		
		return arrayTotals.get(key).get();
	}
	
	/**
	 * @param counterMap
	 * @param key
	 * @return
	 */
	protected NonBlockingHashMap<String, Counter> getNestedCounterMap(NonBlockingHashMap<String, NonBlockingHashMap<String, Counter>> counterMap, String key) {
		NonBlockingHashMap<String, Counter> ctr = counterMap.get(key);
		if(ctr==null) {
			synchronized(counterMap) {
				ctr = counterMap.get(key);
				if(ctr==null) {
					ctr = new NonBlockingHashMap<String, Counter>();
					initCounters(ctr, QUEUE_LENGTH, BUSY_TIME, READ_COUNT, READ_BYTES, READ_ERRORS, READ_DROPS, READ_TICKS, WRITE_COUNT, WRITE_BYTES, WRITE_ERRORS, WRITE_DROPS, WRITE_TICKS);
					counterMap.put(key, ctr);
				}
			}
		}
		return ctr;
	}
	
	/**
	 * Adds a vlunstat sampling
	 * @param vlunstats a vlunstat sampling
 
	 */
	public void addVLun(Map<String, String> vlunstats)  {
		
		long[] lvalues = new long[13];
		int seq = 0;
		
		lvalues[seq++] = Long.parseLong(vlunstats.get(NOW));		
		lvalues[seq++] = Long.parseLong(vlunstats.get(QUEUE_LENGTH));			
		lvalues[seq++] = Long.parseLong(vlunstats.get(BUSY_TIME));
		
		lvalues[seq++] = Long.parseLong(vlunstats.get(READ_COUNT));
		lvalues[seq++] = Long.parseLong(vlunstats.get(READ_BYTES));
		lvalues[seq++] = Long.parseLong(vlunstats.get(READ_ERRORS));
		lvalues[seq++] = Long.parseLong(vlunstats.get(READ_DROPS));
		lvalues[seq++] = Long.parseLong(vlunstats.get(READ_TICKS));
		
		lvalues[seq++] = Long.parseLong(vlunstats.get(WRITE_COUNT));
		lvalues[seq++] = Long.parseLong(vlunstats.get(WRITE_BYTES));
		lvalues[seq++] = Long.parseLong(vlunstats.get(WRITE_ERRORS));
		lvalues[seq++] = Long.parseLong(vlunstats.get(WRITE_DROPS));
		lvalues[seq++]= Long.parseLong(vlunstats.get(WRITE_TICKS));
		
		// ========================
		//   array totals
		// ========================
		sumTotals(arrayTotals, lvalues);
		
		// ========================
		//   host totals
		// ========================
		sumTotals(getNestedCounterMap(hostTotals, vlunstats.get(VVHOSTNAME)), lvalues); 
		
		// ========================
		//   vv totals
		// ========================
		sumTotals(getNestedCounterMap(vvTotals, vlunstats.get(VVNAME)), lvalues); 

		// ========================
		//   port node totals
		// ========================
		sumTotals(getNestedCounterMap(pnTotals, vlunstats.get(VVHOSTNAME) + "/" + vlunstats.get(PORTNODE)), lvalues); 

		
		
		
		this.lunsParsed.incrementAndGet();

		try {
			completionQueue.put(vlunstats);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Applies the raw values to the passed aggregate counter map
	 * @param counterMap The counter map to increment
	 * @param rawValues The raw values read from a statvlun
	 */
	protected void sumTotals(NonBlockingHashMap<String, Counter> counterMap, long[] rawValues)  {
		int seq = 1;
		increment(counterMap, QUEUE_LENGTH, rawValues[seq++]);
		increment(counterMap, BUSY_TIME, rawValues[seq++]);
		increment(counterMap, READ_COUNT, rawValues[seq++]);
		increment(counterMap, READ_BYTES, rawValues[seq++]);
		increment(counterMap, READ_ERRORS, rawValues[seq++]);
		increment(counterMap, READ_DROPS, rawValues[seq++]);
		increment(counterMap, READ_TICKS, rawValues[seq++]);

		increment(counterMap, WRITE_COUNT, rawValues[seq++]);
		increment(counterMap, WRITE_BYTES, rawValues[seq++]);
		increment(counterMap, WRITE_ERRORS, rawValues[seq++]);
		increment(counterMap, WRITE_DROPS, rawValues[seq++]);
		increment(counterMap, WRITE_TICKS, rawValues[seq++]);
	}
	
	/**
	 * Returns a formatted string reporting the array total values
	 * @return the array total values
	 */
	public String printArrayTotals() {
		StringBuilder b = new StringBuilder("\n\t==============================\n\tArray Totals [").append(systemName).append("]\n\t==============================");
		b.append("\n\tQueue Length:").append(getArrayValue(QUEUE_LENGTH));
		b.append("\n\tBusy Time:").append(getArrayValue(BUSY_TIME));
		b.append("\n\tRead Count:").append(getArrayValue(READ_COUNT));
		b.append("\n\tRead Bytes:").append(getArrayValue(READ_BYTES));
		b.append("\n\tRead Errors:").append(getArrayValue(READ_ERRORS));
		b.append("\n\tRead Drops:").append(getArrayValue(READ_DROPS));
		b.append("\n\tRead Ticks:").append(getArrayValue(READ_TICKS));
		b.append("\n\tWrite Count:").append(getArrayValue(WRITE_COUNT));
		b.append("\n\tWrite Bytes:").append(getArrayValue(WRITE_BYTES));
		b.append("\n\tWrite Errors:").append(getArrayValue(WRITE_ERRORS));
		b.append("\n\tWrite Drops:").append(getArrayValue(WRITE_DROPS));
		b.append("\n\tWrite Ticks:").append(getArrayValue(WRITE_TICKS));
		
		return b.toString();
	}
	
	/**
	 * Returns a formatted string reporting the host total values
	 * @return the host total values
	 */
	public String printHostAggregates() {
		StringBuilder b = new StringBuilder("\n\t==============================\n\tVirtual Host Aggregates\n\t==============================");
		for(Map.Entry<String, NonBlockingHashMap<String, Counter>> entry: hostTotals.entrySet()) {
			b.append("\n\tHost:").append(entry.getKey());
			NonBlockingHashMap<String, Counter> counterMap = entry.getValue();
			b.append("\n\t\tQueue Length:").append(counterMap.get(QUEUE_LENGTH).get());
			b.append("\n\t\tBusy Time:").append(counterMap.get(BUSY_TIME).get());
			b.append("\n\t\tRead Count:").append(counterMap.get(READ_COUNT).get());
			b.append("\n\t\tRead Bytes:").append(counterMap.get(READ_BYTES).get());
			b.append("\n\t\tRead Errors:").append(counterMap.get(READ_ERRORS).get());
			b.append("\n\t\tRead Drops:").append(counterMap.get(READ_DROPS).get());
			b.append("\n\t\tRead Ticks:").append(counterMap.get(READ_TICKS).get());
			b.append("\n\t\tWrite Count:").append(counterMap.get(WRITE_COUNT).get());
			b.append("\n\t\tWrite Bytes:").append(counterMap.get(WRITE_BYTES).get());
			b.append("\n\t\tWrite Errors:").append(counterMap.get(WRITE_ERRORS).get());
			b.append("\n\t\tWrite Drops:").append(counterMap.get(WRITE_DROPS).get());
			b.append("\n\t\tWrite Ticks:").append(counterMap.get(WRITE_TICKS).get());			
		}
		
		return b.toString();
	}
	
	/**
	 * Returns a formatted string reporting the vv total values
	 * @return the vv total values
	 */
	public String printVVAggregates() {
		StringBuilder b = new StringBuilder("\n\t==============================\n\tVV Aggregates\n\t==============================");
		for(Map.Entry<String, NonBlockingHashMap<String, Counter>> entry: vvTotals.entrySet()) {
			b.append("\n\tvv Name:").append(entry.getKey());
			NonBlockingHashMap<String, Counter> counterMap = entry.getValue();
			b.append("\n\t\tQueue Length:").append(counterMap.get(QUEUE_LENGTH).get());
			b.append("\n\t\tBusy Time:").append(counterMap.get(BUSY_TIME).get());
			b.append("\n\t\tRead Count:").append(counterMap.get(READ_COUNT).get());
			b.append("\n\t\tRead Bytes:").append(counterMap.get(READ_BYTES).get());
			b.append("\n\t\tRead Errors:").append(counterMap.get(READ_ERRORS).get());
			b.append("\n\t\tRead Drops:").append(counterMap.get(READ_DROPS).get());
			b.append("\n\t\tRead Ticks:").append(counterMap.get(READ_TICKS).get());
			b.append("\n\t\tWrite Count:").append(counterMap.get(WRITE_COUNT).get());
			b.append("\n\t\tWrite Bytes:").append(counterMap.get(WRITE_BYTES).get());
			b.append("\n\t\tWrite Errors:").append(counterMap.get(WRITE_ERRORS).get());
			b.append("\n\t\tWrite Drops:").append(counterMap.get(WRITE_DROPS).get());
			b.append("\n\t\tWrite Ticks:").append(counterMap.get(WRITE_TICKS).get());			
		}
		
		
		return b.toString();
	}
	
	
	
	public void countdown() {
		try {
			completionQueue.take();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Returns the SAN serial number 
	 * @return the serialNumber
	 */
	public String getSerialNumber() {
		return serialNumber;
	}

	/**
	 * Returns the SAN system name
	 * @return the systemName
	 */
	public String getSystemName() {
		return systemName;
	}

	/**
	 * Returns the SAN cpu speed in Mhz
	 * @return the cpuMhz
	 */
	public int getCpuMhz() {
		return cpuMhz;
	}

	/**
	 * Returns the SAN ip name
	 * @return the ipName
	 */
	public String getIpName() {
		return ipName;
	}

	/**
	 * Returns the SAN OS version
	 * @return the osVersion
	 */
	public String getOsVersion() {
		return osVersion;
	}

	/**
	 * Returns the SAN model name
	 * @return the modelName
	 */
	public String getModelName() {
		return modelName;
	}

	/**
	 * Returns the SAN cache size in MB
	 * @return the cacheSize
	 */
	public int getCacheSize() {
		return cacheSize;
	}

	/**
	 * Returns the number of vlunstats parsed for this file
	 * @return the lunsParsed
	 */
	public int getLunsParsed() {
		return lunsParsed.get();
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return new StringBuilder("SANStatsParsingContext[").append(this.systemName).append("] vluns parsed:").append(lunsParsed.get()).toString();
	}

}

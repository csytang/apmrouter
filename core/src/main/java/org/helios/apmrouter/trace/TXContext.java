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
package org.helios.apmrouter.trace;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicLong;

import org.helios.apmrouter.util.SystemClock;

/**
 * <p>Title: TXContext</p>
 * <p>Description: Tracks a distributed and incrementing context across multiple tracing points and JVMs 
 * so that trace points can be correlated on the server side and their sequence correctly interpreted.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.trace.TXContext</code></p>
 */

public class TXContext {
	/** The TXContext ID for this TXContext */
	private final long txId;
	/** The TXContext qualifier for this instance */
	private final int txQualifier;
	/** The TXContext thread qualifier for this instance */
	private final int txThreadId;
	
	/** The size of a TXContext in bytes */
	public static final int TXCONTEXT_SIZE = 8 + 4 + 4;
    /** Zero byte literal */
    public static final byte BYTE_ZERO = 0;
    /** One byte literal */
    public static final byte BYTE_ONE = 1;
	
	
	
	
	/** The default NULL context generated by the thread local */
	private static final TXContext NULLContext = new TXContext(-1, -1, -1);
	/** The TX ID Generator for originating TXContexts */
	private static final AtomicLong TXSERIAL = new AtomicLong(0L);

	
	/** The TXContext for the current thread */
	private static final InheritableThreadLocal<TXContext> currentContext = new InheritableThreadLocal<TXContext>(){
		@Override
		protected TXContext initialValue() {
			return NULLContext;
		}
		@Override
		protected TXContext childValue(TXContext parentValue) {
			return parentValue==NULLContext ? NULLContext : parentValue.rollThread();
		}
	};
	
	/**
	 * Determines if there is a real TXContext associated with the current thread
	 * @return true if there is a real TXContext associated with the current thread, false otherwise
	 */
	public static boolean hasContext() {
		TXContext txc = currentContext.get();
		if(txc==NULLContext) {
			currentContext.remove();
			return false;
		}
		return true;
	}
	
	/**
	 * Increments or initiates the current context
	 * @return the new TXContext
	 */
	public static TXContext rollContext() {
		TXContext txc = currentContext.get();
		if(txc==NULLContext) return txc.startContext();
		return txc.rollQualifier();
	}
	
	/**
	 * Clears the context for the current thread.
	 */
	public static void clearContext() {
		currentContext.remove();
	}
	
	/**
	 * Retrieves the context for the current thread if one exists.
	 * @return the context for the current thread or null if one does not exist
	 */
	public static TXContext getContext() {
		TXContext txc = currentContext.get();
		if(txc==NULLContext) {
			currentContext.remove();
			return null;
		}
		return txc;		
	}
	
	//public static TXContext

	/**
	 * Creates a new TXContext
	 * @param txId The TXContext ID for this TXContext
	 * @param txQualifier The TXContext qualifier for this instance
	 * @param txThreadId The TXContext thread qualifier for this instance 
	 */
	TXContext(long txId, int txQualifier, int txThreadId) {
		this.txId = txId;
		this.txQualifier = txQualifier;
		this.txThreadId = txThreadId;
	}

	/**
	 * Returns the TXContext ID for this TXContext
	 * @return the TXContext ID for this TXContext
	 */
	public long getTxId() {
		return txId;
	}

	/**
	 * Returns the TXContext qualifier for this instance
	 * @return the TXContext qualifier for this instance
	 */
	public int getTxQualifier() {
		return txQualifier;
	}

	/**
	 * Returns the TXContext thread qualifier for this instance 
	 * @return the TXContext thread qualifier for this instance 
	 */
	public int getTxThreadId() {
		return txThreadId;
	}
	
	/**
	 * Starts a new originating context
	 * @return an originating context
	 */
	private TXContext startContext() {
		TXContext txc = new TXContext(TXSERIAL.incrementAndGet(), 0, 0);
		currentContext.set(txc);
		return txc;
	}
	
	/**
	 * Generates the next sequential TXContext within one thread
	 * @return the next sequential TXContext
	 */
	private TXContext rollQualifier() {
		TXContext txc = new TXContext(txId, txQualifier+1, txThreadId);
		currentContext.set(txc);
		return txc;
	}
	
	/**
	 * Generates the next sequential TXContext within one thread
	 * @return the next sequential TXContext
	 */
	private TXContext rollThread() {
		TXContext txc = new TXContext(txId,  txQualifier+1, txThreadId+1);
		currentContext.set(txc);
		return txc;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (txId ^ (txId >>> 32));
		result = prime * result + txQualifier;
		result = prime * result + txThreadId;
		return result;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		TXContext other = (TXContext) obj;
		if (txId != other.txId) {
			return false;
		}
		if (txQualifier != other.txQualifier) {
			return false;
		}
		if (txThreadId != other.txThreadId) {
			return false;
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("[%s:%s:%s]", txId, txQualifier, txThreadId);
	}
	
	/**
	 * Encodes this TXContext to a byte array in the native byte order
	 * @return a byte array containing the encoded TXContext
	 */
	public byte[] encode() {
		return encode(this);
	}
	
	/**
	 * Encodes this TXContext to a byte array in the specified byte order
	 * @param byteOrder The byte order to encode in
	 * @return a byte array containing the encoded TXContext
	 */
	public byte[] encode(ByteOrder byteOrder) {
		return encode(this, byteOrder);
	}
	
	/**
	 * Returns the passed TXContext in the form of a byte array in the specified byte order 
	 * @param context The context to encode
	 * @param byteOrder The byte order to encode in
	 * @return The encoded TXContext
	 */
	public static byte[] encode(TXContext context, ByteOrder byteOrder) {
		return ByteBuffer.allocate(TXCONTEXT_SIZE).order(byteOrder)
				.put(byteOrder==ByteOrder.LITTLE_ENDIAN ? BYTE_ZERO : BYTE_ONE)
				.putLong(context.txId)
				.putInt(context.txQualifier)
				.putInt(context.txThreadId)
				.array();
	}
	
	/**
	 * Returns the passed TXContext in the form of a byte array in the native byte order 
	 * @param context The context to encode
	 * @return The encoded TXContext
	 */
	public static byte[] encode(TXContext context) {
		return encode(context, ByteOrder.nativeOrder());
	}	
	
	/**
	 * Decodes the passed bytes to a TXContext
	 * @param bytes The bytes to decode
	 * @return the decoded TXContext
	 */
	public static TXContext decode(byte[] bytes) {
		return decode(bytes, false);
	}
	
	/**
	 * Decodes the passed bytes to a TXContext
	 * @param bytes The bytes to decode
	 * @param reverse true to reverse the byte code from the one encoded
	 * @return the decoded TXContext
	 */
	public static TXContext decode(byte[] bytes, boolean reverse) {
		if(bytes==null || bytes.length!=TXCONTEXT_SIZE) throw new IllegalArgumentException("Byte size not valid for an encoded TXContext [" + (bytes==null ? "<null>" : bytes.length) + "]");
		ByteBuffer bb = null;
		if(reverse) {
			
			bb= ByteBuffer.wrap(bytes).order(bytes[0]==BYTE_ZERO ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
		} else {
			bb = ByteBuffer.wrap(bytes).order(bytes[0]==BYTE_ZERO ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
			
		}		
		bb.get();
		return new TXContext(bb.getLong(), bb.getInt(), bb.getInt());
	}	
	
	
	
	public static void main(String[] args) {
		log("TXContext Test");
		log("Has:" + hasContext());
		log("Actual:" + rollContext());
		log("Has:" + hasContext());
		for(int i = 0; i < 10; i++) {
			rollContext();
		}
		log("Current:" + getContext());
		clearContext();
		log("Has:" + hasContext());
		log("Actual:" + rollContext());
		log("========================================");
		byte[] encoded = getContext().encode();
		log("Decoded:" + decode(encoded));
		new Thread() {
			public void run() {
				log("Has:" + hasContext());
				log("Current:" + getContext());
				log("Rolled:" + rollContext());
			}
		}.start();
		clearContext();
		SystemClock.sleep(1000);
		new Thread() {
			public void run() {
				log("Has:" + hasContext());
				log("Current:" + getContext());
				log("Rolled:" + rollContext());
				new Thread() {
					public void run() {
						log("Has:" + hasContext());
						log("Current:" + getContext());
						log("Rolled:" + rollContext());
					}
				}.start();
			}
		}.start();		
		SystemClock.sleep(5000);
	}
	
	public static void log(Object msg) {
		System.out.println("[" + Thread.currentThread().getName() + "]" + msg);
	}
	
	
	
}

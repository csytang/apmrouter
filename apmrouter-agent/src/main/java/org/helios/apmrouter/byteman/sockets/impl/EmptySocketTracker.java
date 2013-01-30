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
package org.helios.apmrouter.byteman.sockets.impl;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.cliffc.high_scale_lib.NonBlockingHashSet;
import org.helios.apmrouter.jmx.ConfigurationHelper;
import org.helios.apmrouter.nativex.APMSigar;
import org.helios.apmrouter.util.SimpleLogger;
import org.helios.apmrouter.util.SimpleLogger.Level;
import org.hyperic.sigar.NetStat;

/**
 * <p>Title: EmptySocketTracker</p>
 * <p>Description: An empty {@link ISocketTracker} for extending.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.byteman.sockets.impl.EmptySocketTracker</code></p>
 */

public class EmptySocketTracker implements ISocketTracker, ThreadFactory {
	/** A hashset of server side sockets */
	protected final NonBlockingHashSet<ISocketImpl> serverSideSockets = new NonBlockingHashSet<ISocketImpl>();
	/** The harvester thread */
	protected Thread harvesterThread;
	/** The name of the active tracker */
	protected String activeTracker = null;
	/** The keep running flag */
	protected final AtomicBoolean keepRunning = new AtomicBoolean(false);
	/** Serial number generator for harvester threads */
	protected static final AtomicLong serial = new AtomicLong(0L);
	/** Harvester thread group */
	protected static final ThreadGroup harvesterThreadGroup = new ThreadGroup("SocketTrackingHarvesters");
	
	/** The harvester sleep period in ms. */
	protected final AtomicLong harvesterSleep = new AtomicLong(-1L);
	
	/** The logging level for the tracker */
	protected Level loggingLevel = Level.INFO;

	
	/** Native OS API  */
	protected static final APMSigar sigar = APMSigar.getInstance();
	
	/**
	 * Creates a new EmptySocketTracker
	 */
	public EmptySocketTracker() {
		harvesterSleep.set(ConfigurationHelper.getLongSystemThenEnvProperty(SOCKET_HARVESTER_PERIOD_PROP, DEFAULT_SOCKET_HARVESTER_PERIOD));
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
	 */
	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(harvesterThreadGroup, this, getClass().getSimpleName() + "-HarvesterThread#" + serial.incrementAndGet());
		t.setDaemon(true);
		t.setPriority(Thread.NORM_PRIORITY-1);
		return t;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#setActiveTracker(java.lang.String)
	 */
	@Override
	public void setActiveTracker(String simpleName) {
		activeTracker = simpleName;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#getActiveTracker()
	 */
	@Override
	public String getActiveTracker() {
		return activeTracker;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onSetEOF(java.io.InputStream, java.lang.Object, boolean)
	 */
	@Override
	public void onSetEOF(InputStream is, Object socket, boolean eof) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onSkip(java.io.InputStream, long, java.lang.Object, long)
	 */
	@Override
	public void onSkip(InputStream is, long skipped, Object socket, long skip) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onSocketWrite(java.io.OutputStream, java.lang.Object, byte[], int, int)
	 */
	@Override
	public void onSocketWrite(OutputStream os, Object socket, byte[] b,
			int off, int len) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onRead(java.io.InputStream, int, java.lang.Object, byte[])
	 */
	@Override
	public void onRead(InputStream is, int actualBytesRead, Object socket,
			byte[] buffer) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onRead(java.io.InputStream, int, java.lang.Object, byte[], int, int)
	 */
	@Override
	public void onRead(InputStream is, int actualBytesRead, Object socket,
			byte[] buffer, int off, int length) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onRead(java.io.InputStream, int, java.lang.Object)
	 */
	@Override
	public void onRead(InputStream is, int value, Object socket) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onConnect(org.helios.apmrouter.byteman.sockets.impl.ISocketImpl, java.net.SocketAddress, int)
	 */
	@Override
	public void onConnect(ISocketImpl socketImpl, SocketAddress address,
			int timeout) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onConnect(org.helios.apmrouter.byteman.sockets.impl.ISocketImpl, java.net.InetAddress, int)
	 */
	@Override
	public void onConnect(ISocketImpl socketImpl, InetAddress address,
			int timeout) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onConnect(org.helios.apmrouter.byteman.sockets.impl.ISocketImpl, java.lang.String, int)
	 */
	@Override
	public void onConnect(ISocketImpl socketImpl, String host, int port) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onBind(org.helios.apmrouter.byteman.sockets.impl.ISocketImpl, java.net.InetAddress, int)
	 */
	@Override
	public void onBind(ISocketImpl socketImpl, InetAddress host, int port) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onListen(org.helios.apmrouter.byteman.sockets.impl.ISocketImpl, int)
	 */
	@Override
	public void onListen(ISocketImpl socketImpl, int backlog) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onAccept(org.helios.apmrouter.byteman.sockets.impl.ISocketImpl, org.helios.apmrouter.byteman.sockets.impl.ISocketImpl)
	 */
	@Override
	public void onAccept(ISocketImpl socketImpl, ISocketImpl acceptedSocketImpl) {

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onGetInputStream(org.helios.apmrouter.byteman.sockets.impl.ISocketImpl, java.io.InputStream)
	 */
	@Override
	public void onGetInputStream(ISocketImpl socketImpl, InputStream inputStream) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onGetOutputStream(org.helios.apmrouter.byteman.sockets.impl.ISocketImpl, java.io.OutputStream)
	 */
	@Override
	public void onGetOutputStream(ISocketImpl socketImpl,
			OutputStream outputStream) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onAvailable(org.helios.apmrouter.byteman.sockets.impl.ISocketImpl, int)
	 */
	@Override
	public void onAvailable(ISocketImpl socketImpl, int available) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onClose(org.helios.apmrouter.byteman.sockets.impl.ISocketImpl)
	 */
	@Override
	public void onClose(ISocketImpl socketImpl) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onShutdownInput(org.helios.apmrouter.byteman.sockets.impl.ISocketImpl)
	 */
	@Override
	public void onShutdownInput(ISocketImpl socketImpl) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onShutdownOutput(org.helios.apmrouter.byteman.sockets.impl.ISocketImpl)
	 */
	@Override
	public void onShutdownOutput(ISocketImpl socketImpl) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onSendUrgentData(org.helios.apmrouter.byteman.sockets.impl.ISocketImpl, int)
	 */
	@Override
	public void onSendUrgentData(ISocketImpl socketImpl, int data) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onSetSocket(org.helios.apmrouter.byteman.sockets.impl.ISocketImpl, java.lang.Object)
	 */
	@Override
	public void onSetSocket(ISocketImpl socketImpl, Object socket) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onSetServerSocket(org.helios.apmrouter.byteman.sockets.impl.ISocketImpl, java.lang.Object)
	 */
	@Override
	public void onSetServerSocket(ISocketImpl socketImpl, Object serverSocket) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onReset(org.helios.apmrouter.byteman.sockets.impl.ISocketImpl)
	 */
	@Override
	public void onReset(ISocketImpl socketImpl) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onSetPerformancePreferences(org.helios.apmrouter.byteman.sockets.impl.ISocketImpl, int, int, int)
	 */
	@Override
	public void onSetPerformancePreferences(ISocketImpl socketImpl,
			int connectionTime, int latency, int bandwidth) {
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#requiresHarvester()
	 */
	@Override
	public boolean requiresHarvester() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#stop()
	 */
	@Override
	public synchronized void stop() {
		if(!requiresHarvester()) return;
		if(keepRunning.get()) {
			keepRunning.set(false);
		}
		if(harvesterThread!=null && harvesterThread.isAlive()) {
			harvesterThread.interrupt();
		}
		harvesterThread = null;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#start()
	 */
	@Override
	public synchronized void start() {
		if(!requiresHarvester()) return;
		if(!keepRunning.get()) {
			keepRunning.set(true);
		}
		if(harvesterThread!=null && harvesterThread.isAlive()) {
			harvesterThread.interrupt();
			harvesterThread = null;
		}
		harvesterThread = newThread(this);		
		harvesterThread.start();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#isStarted()
	 */
	@Override
	public boolean isStarted() {
		return keepRunning.get();
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		while(keepRunning.get()) {
			try {
				harvest();
			} catch (Throwable t) {
				SimpleLogger.error("Harvester Thread Error", t);
			} finally {
				try {
					Thread.currentThread().join(harvesterSleep.get());
				} catch (InterruptedException iex) {
					Thread.interrupted();
				}
			}
		}
	}

	/**
	 * The harvest task implementation
	 */
	protected void harvest() {
		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#hasJMXInterface()
	 */
	@Override
	public boolean hasJMXInterface() {
		return false;
	}
	
	/**
	 * Tests a socket's input and output streams to see if the socket is active.
	 * @param so The socket to test
	 * @return true if the socket is active, false otherwise
	 */
	protected boolean testSocketStreams(Socket so) {
		if(so==null) return false;
		try {			
			if(!so.isConnected() || so.isClosed()) return false;
			boolean ok = testSocketInput(so) && testSocketOutput(so);
			if(!ok) return false;
			//so.sendUrgentData(0);
//			NetStat ns = sigar.getNetStat(so.getLocalAddress().getAddress(), so.getLocalPort());
//			SimpleLogger.info(new NetStatPrinter(ns));
//			if(ns.getTcpCloseWait()>0) {
//				return false;
//			}
			return ok;
		}  catch (Exception ex) {
			if(so.isConnected()) {
				try { so.close(); } catch (Exception e) {}
			}
			return false;
		}
	}
	
	/**
	 * Tests the socket's input stream to determine if input is closed 
	 * @param so the socket to test
	 * @return true if the input is still active, false otherwise
	 */
	protected boolean testSocketInput(Socket so) {
		if(so==null) return false;
		try {			
			if(so.isInputShutdown()) return false;
			so.getInputStream().available();			
			return true;
		}  catch (Exception ex) {
			return false;
		}
	}
	
	/** An empty byte array buffer constant */
	public static final byte[] EMPTY_BYTE_ARR = {};
	/**
	 * Tests the socket's output stream to determine if output is closed 
	 * @param so the socket to test
	 * @return true if the output is still active, false otherwise
	 */
	protected boolean testSocketOutput(Socket so) {
		if(so==null) return false;
		try {
			if(so.isOutputShutdown()) return false;
			OutputStream os = so.getOutputStream();			
			os.write(EMPTY_BYTE_ARR, 0, 0);			
			return true;
		}  catch (Exception ex) {
			return false;
		}
	}
	
	
	public static class NetStatPrinter {
		/** the netstat to print */
		private final NetStat netStat;

		/**
		 * Creates a new NetStatPrinter
		 * @param netStat the netstat to print
		 */
		public NetStatPrinter(NetStat netStat) {
			super();
			this.netStat = netStat;
		}
		
		

		public int getAllInboundTotal() {
			return netStat.getAllInboundTotal();
		}

		public int getAllOutboundTotal() {
			return netStat.getAllOutboundTotal();
		}

		public int getTcpBound() {
			return netStat.getTcpBound();
		}

		public int getTcpClose() {
			return netStat.getTcpClose();
		}

		public int getTcpCloseWait() {
			return netStat.getTcpCloseWait();
		}

		public int getTcpClosing() {
			return netStat.getTcpClosing();
		}

		public int getTcpEstablished() {
			return netStat.getTcpEstablished();
		}

		public int getTcpFinWait1() {
			return netStat.getTcpFinWait1();
		}

		public int getTcpFinWait2() {
			return netStat.getTcpFinWait2();
		}

		public int getTcpIdle() {
			return netStat.getTcpIdle();
		}

		public int getTcpInboundTotal() {
			return netStat.getTcpInboundTotal();
		}

		public int getTcpLastAck() {
			return netStat.getTcpLastAck();
		}

		public int getTcpListen() {
			return netStat.getTcpListen();
		}

		public int getTcpOutboundTotal() {
			return netStat.getTcpOutboundTotal();
		}

		public int[] getTcpStates() {
			return netStat.getTcpStates();
		}

		public int getTcpSynRecv() {
			return netStat.getTcpSynRecv();
		}

		public int getTcpSynSent() {
			return netStat.getTcpSynSent();
		}

		public int getTcpTimeWait() {
			return netStat.getTcpTimeWait();
		}



		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("NetStatPrinter [\n\tgetAllInboundTotal()=")
					.append(getAllInboundTotal())
					.append("\n\tgetAllOutboundTotal()=")
					.append(getAllOutboundTotal()).append("\n\tgetTcpBound()=")
					.append(getTcpBound()).append("\n\tgetTcpClose()=")
					.append(getTcpClose()).append("\n\tgetTcpCloseWait()=")
					.append(getTcpCloseWait()).append("\n\tgetTcpClosing()=")
					.append(getTcpClosing()).append("\n\tgetTcpEstablished()=")
					.append(getTcpEstablished()).append("\n\tgetTcpFinWait1()=")
					.append(getTcpFinWait1()).append("\n\tgetTcpFinWait2()=")
					.append(getTcpFinWait2()).append("\n\tgetTcpIdle()=")
					.append(getTcpIdle()).append("\n\tgetTcpInboundTotal()=")
					.append(getTcpInboundTotal()).append("\n\tgetTcpLastAck()=")
					.append(getTcpLastAck()).append("\n\tgetTcpListen()=")
					.append(getTcpListen()).append("\n\tgetTcpOutboundTotal()=")
					.append(getTcpOutboundTotal())
					.append("\n\tgetTcpSynRecv()=").append(getTcpSynRecv())
					.append("\n\tgetTcpSynSent()=").append(getTcpSynSent())
					.append("\n\tgetTcpTimeWait()=").append(getTcpTimeWait())
					.append("]");
			return builder.toString();
		}
		
		
	}


	/**
	 * Returns the name of the current logging level
	 * @return the current logging level name
	 */
	public String getLoggingLevel() {
		return loggingLevel.name();
	}

	/**
	 * Sets the logging level to the passed level name
	 * @param loggingLevel the level name
	 */
	public void setLoggingLevel(String loggingLevel) {
		this.loggingLevel = Level.forName(loggingLevel);
	}

}

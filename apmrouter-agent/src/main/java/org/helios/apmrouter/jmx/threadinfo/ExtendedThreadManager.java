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
package org.helios.apmrouter.jmx.threadinfo;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.MBeanNotificationInfo;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;

import org.helios.apmrouter.jmx.JMXHelper;
import org.helios.apmrouter.util.SystemClock;

/**
 * <p>Title: ExtendedThreadManager</p>
 * <p>Description: A drop in replacement for the standard {@link ThreadMXBean} that adds additional functionality and notifications.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.jmx.threadinfo.ExtendedThreadManager</code></p>
 */

public class ExtendedThreadManager extends NotificationBroadcasterSupport implements ExtendedThreadManagerMXBean {
	private static final MBeanNotificationInfo[] notificationInfo = createMBeanInfo();
	/** The delegate ThreadMXBean */
	protected final ThreadMXBean delegate;
	/** Indicates if the delegate is installed */
	protected static final AtomicBoolean installed = new AtomicBoolean(false);
	/** The platform MBeanServer */
	protected static final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
	/** The ThreadMXBean object name */
	protected static final ObjectName THREAD_MX_NAME = JMXHelper.objectName(ManagementFactory.THREAD_MXBEAN_NAME);
	/** The JMX notification type emitted when Thread Contention Monitoring is enabled */
	public static final String NOTIF_TCM_ENABLED = "threadmxbean.tcm.enabled";
	/** The JMX notification type emitted when Thread Contention Monitoring is disabled */
	public static final String NOTIF_TCM_DISABLED = "threadmxbean.tcm.disabled";
	/** The JMX notification type emitted when Thread Timing is enabled */
	public static final String NOTIF_TCT_ENABLED = "threadmxbean.tct.enabled";
	/** The JMX notification type emitted when Thread Timing is disabled */
	public static final String NOTIF_TCT_DISABLED = "threadmxbean.tct.disabled";
	/** The extended thread manager instance */
	private static ExtendedThreadManager mxb = null;
	/** JMX notification serial number generator */
	private static final AtomicLong serial = new AtomicLong(0L);
	/** The original ThreadMXBean */
	public static final ThreadMXBean original = ManagementFactory.getThreadMXBean();
	/** Indicates if thread contention monitoring is supported */
	public static final boolean TCM_SUPPORTED = original.isThreadContentionMonitoringSupported();
	/** Indicates if thread cpu timing monitoring is supported */
	public static final boolean TCT_SUPPORTED = original.isThreadCpuTimeSupported();
	
	/** The default max depth to get thread Infos with */
	private int maxDepth = Integer.MAX_VALUE;
	
	// record initial tct and tcm states, store in statics
	
	public static void main(String[] args) {
		log("Installing Notifier");
		install();
		try { Thread.currentThread().join(); } catch (Exception ex) {};
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	/**
	 * Installs and return the ExtendedThreadManager
	 * @return the ExtendedThreadManager instance
	 */
	public static ExtendedThreadManager install() {
		if(!installed.get()) {
			mxb = new ExtendedThreadManager(ManagementFactory.getThreadMXBean());
			try {
				server.unregisterMBean(THREAD_MX_NAME);
				server.registerMBean(mxb, THREAD_MX_NAME);
				installed.set(true);
			} catch (Exception ex) {
				ex.printStackTrace(System.err);
				throw new RuntimeException("Failed to install ExtendedThreadManager", ex);
			}
		}
		return mxb;
	}
	public static void remove() {
		if(installed.get()) {
			
		}
	}
	public static boolean isInstalled() {
		return installed.get();
	}
	
	/**
	 * Creates a new ExtendedThreadManager
	 * @param delegate the ThreadMXBean delegate
	 */
	private ExtendedThreadManager(ThreadMXBean delegate) {
		super(Executors.newFixedThreadPool(1, new ThreadFactory(){
			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r, "ThreadMXBeanNotifier");
				t.setDaemon(true);
				return t;
			}
		}), notificationInfo);
		this.delegate = delegate;
	}
	
	@Override
	public ObjectName getObjectName() {
		return delegate.getObjectName();
	}
	@Override
	public int getThreadCount() {
		return delegate.getThreadCount();
	}
	@Override
	public int getPeakThreadCount() {
		return delegate.getPeakThreadCount();
	}
	@Override
	public long getTotalStartedThreadCount() {
		return delegate.getTotalStartedThreadCount();
	}
	@Override
	public int getDaemonThreadCount() {
		return delegate.getDaemonThreadCount();
	}
	@Override
	public int getNonDaemonThreadCount() {
		return delegate.getThreadCount() - delegate.getDaemonThreadCount();
	}
	
	@Override
	public long[] getAllThreadIds() {
		return delegate.getAllThreadIds();
	}
	@Override
	public ThreadInfo getThreadInfo(long id) {
		return delegate.getThreadInfo(id);
	}
	@Override
	public ThreadInfo[] getThreadInfo(long[] ids) {
		return delegate.getThreadInfo(ids);
	}
	@Override
	public ThreadInfo getThreadInfo(long id, int maxDepth) {
		return delegate.getThreadInfo(id, maxDepth);
	}
	@Override
	public ThreadInfo[] getThreadInfo(long[] ids, int maxDepth) {
		return delegate.getThreadInfo(ids, maxDepth);
	}
	@Override
	public boolean isThreadContentionMonitoringSupported() {
		return delegate.isThreadContentionMonitoringSupported();
	}
	@Override
	public boolean isThreadContentionMonitoringEnabled() {
		return delegate.isThreadContentionMonitoringEnabled();
	}
	@Override
	public void setThreadContentionMonitoringEnabled(boolean enable) {
		delegate.setThreadContentionMonitoringEnabled(enable);
		if(enable) {
			sendNotification(new Notification(NOTIF_TCM_ENABLED, THREAD_MX_NAME, serial.incrementAndGet(), SystemClock.time(), "Thread Contention Monitoring Enabled"));
		} else {
			sendNotification(new Notification(NOTIF_TCM_DISABLED, THREAD_MX_NAME, serial.incrementAndGet(), SystemClock.time(), "Thread Contention Monitoring Disabled"));
		}
	}
	@Override
	public long getCurrentThreadCpuTime() {
		return delegate.getCurrentThreadCpuTime();
	}
	@Override
	public long getCurrentThreadUserTime() {
		return delegate.getCurrentThreadUserTime();
	}
	@Override
	public long getThreadCpuTime(long id) {
		return delegate.getThreadCpuTime(id);
	}
	@Override
	public long getThreadUserTime(long id) {
		return delegate.getThreadUserTime(id);
	}
	@Override
	public boolean isThreadCpuTimeSupported() {
		return delegate.isThreadCpuTimeSupported();
	}
	@Override
	public boolean isCurrentThreadCpuTimeSupported() {
		return delegate.isCurrentThreadCpuTimeSupported();
	}
	@Override
	public boolean isThreadCpuTimeEnabled() {
		return delegate.isThreadCpuTimeEnabled();
	}
	@Override
	public void setThreadCpuTimeEnabled(boolean enable) {
		delegate.setThreadCpuTimeEnabled(enable);
		if(enable) {
			sendNotification(new Notification(NOTIF_TCT_ENABLED, THREAD_MX_NAME, serial.incrementAndGet(), SystemClock.time(), "Thread CPU Time Monitoring Enabled"));
		} else {
			sendNotification(new Notification(NOTIF_TCT_DISABLED, THREAD_MX_NAME, serial.incrementAndGet(), SystemClock.time(), "Thread CPU Time Monitoring Disabled"));
		}		
	}
	@Override
	public long[] findMonitorDeadlockedThreads() {
		return delegate.findMonitorDeadlockedThreads();
	}
	@Override
	public void resetPeakThreadCount() {
		delegate.resetPeakThreadCount();
	}
	@Override
	public long[] findDeadlockedThreads() {
		return delegate.findDeadlockedThreads();
	}
	@Override
	public boolean isObjectMonitorUsageSupported() {
		return delegate.isObjectMonitorUsageSupported();
	}
	@Override
	public boolean isSynchronizerUsageSupported() {
		return delegate.isSynchronizerUsageSupported();
	}
	@Override
	public ThreadInfo[] getThreadInfo(long[] ids, boolean lockedMonitors,
			boolean lockedSynchronizers) {
		return delegate.getThreadInfo(ids, lockedMonitors, lockedSynchronizers);
	}
	/**
	 * {@inheritDoc}
	 * @see java.lang.management.ThreadMXBean#dumpAllThreads(boolean, boolean)
	 */
	@Override
	public ThreadInfo[] dumpAllThreads(boolean lockedMonitors,
			boolean lockedSynchronizers) {
		return delegate.dumpAllThreads(lockedMonitors, lockedSynchronizers);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jmx.threadinfo.ExtendedThreadManagerMXBean#getNonDaemonThreadNames()
	 */
	@Override
	public String[] getNonDaemonThreadNames() {
		try {
			Thread[] allThreads = new Thread[getThreadCount()];
			Thread.enumerate(allThreads);
			Set<String> threadNames=  new HashSet<String>();
			for(Thread t: allThreads) {
				if(t==null) continue;
				if(!t.isDaemon()) {
					threadNames.add(t.toString());
				}
			}
			return threadNames.toArray(new String[threadNames.size()]);
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
			throw new RuntimeException("Failed to list non-daemon threads:" + ex);
		}
	}
	
/*
Thread[AWT-Shutdown,5,main]
Thread[AWT-EventQueue-0,6,main]
Thread[DestroyJavaVM,5,main]
Thread[Thread-10,6,main]
 */
	
	private static MBeanNotificationInfo[] createMBeanInfo() {
		return new MBeanNotificationInfo[]{
			new MBeanNotificationInfo(new String[]{NOTIF_TCM_ENABLED, NOTIF_TCM_DISABLED}, Notification.class.getName(), "Notification indicating if ThreadContentionMonitoring (tcm) enablement has changed"),
			new MBeanNotificationInfo(new String[]{NOTIF_TCT_ENABLED, NOTIF_TCT_DISABLED}, Notification.class.getName(), "Notification indicating if ThreadContentionMonitoring (tcm) enablement has changed"),
		};		
	}
	
	

	/**
	 * Returns the max depth used for getting thread infos
	 * @return the max depth used for getting thread infos
	 */
	@Override
	public int getMaxDepth() {
		return maxDepth;
	}

	/**
	 * Sets the max depth used for getting thread infos
	 * @param maxDepth the max depth used for getting thread infos
	 */
	@Override
	public void setMaxDepth(int maxDepth) {
		this.maxDepth = maxDepth;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jmx.threadinfo.ExtendedThreadManagerMXBean#getThreadInfo()
	 */
	@Override
	public ExtendedThreadInfo[] getThreadInfo() {
		return ExtendedThreadInfo.wrapThreadInfos(delegate.getThreadInfo(delegate.getAllThreadIds(), maxDepth));
	}
}

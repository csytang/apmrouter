package org.helios.apmrouter.catalog.domain;

// Generated Oct 25, 2012 9:05:53 PM by Hibernate Tools 3.6.0

import java.util.HashSet;
import java.util.Set;

/**
 * TraceType generated by hbm2java
 */
public class TraceType implements java.io.Serializable {

	private short typeId;
	private String typeName;
	private Set metrics = new HashSet(0);

	public TraceType() {
	}

	public TraceType(short typeId) {
		this.typeId = typeId;
	}

	public TraceType(short typeId, String typeName, Set metrics) {
		this.typeId = typeId;
		this.typeName = typeName;
		this.metrics = metrics;
	}

	public short getTypeId() {
		return this.typeId;
	}

	public void setTypeId(short typeId) {
		this.typeId = typeId;
	}

	public String getTypeName() {
		return this.typeName;
	}

	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}

	public Set getMetrics() {
		return this.metrics;
	}

	public void setMetrics(Set metrics) {
		this.metrics = metrics;
	}

}
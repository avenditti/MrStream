package org.zapto.mike.mrstreamserver;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Packet implements Serializable{

	protected String type;
	protected Object[] data;

	public Packet(String type, Object... data) {
		this.type = type;
		this.data = data;
	}

	public String getType() {
		return type;
	}

	public Object[] getData() {
		return data;
	}



}

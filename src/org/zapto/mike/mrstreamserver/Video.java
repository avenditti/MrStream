package org.zapto.mike.mrstreamserver;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Video implements Serializable{

	private final String owner;
	private final String url;
	private final String name;

	public Video(String url, String owner, String name) {
		this.url = url;
		this.owner = owner;
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public String getUrl() {
		return url;
	}

	public String getOwner() {
		return owner;
	}
}
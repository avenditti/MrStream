package org.zapto.mike.mrstreamserver;

import java.io.Serializable;

@SuppressWarnings("serial")
public class VideoClient implements Serializable{

	private transient ClientHandler c;
	private boolean canPlay;
	private String name;
	private Double time;

	public VideoClient(ClientHandler c) {
		this.canPlay = false;
		this.name = c.getName();
		this.c = c;
		this.time = 0.0;
	}

	public Double getTime() {
		return time;
	}

	public void updateTime(Double time) {
		this.time = time;
	}

	public boolean canPlay() {
		return canPlay;
	}

	public void setCanPlay(boolean canPlay) {
		this.canPlay = canPlay;
	}

	public String getName() {
		return name;
	}

	ClientHandler getClient() {
		return c;
	}
}
package org.zapto.mike.mrstreamserver;

import java.util.HashMap;

public class VideoHandler implements Runnable{


	private HashMap<ClientHandler,VideoClient> videoClients;
	private boolean stopping;
	private double globalTime; // Global time
	private final double minTimeDiff = 1; // Minimum time difference between clients
	private String currentVideo;
	private Channel parentChannel;
	private boolean canPlay;
	private boolean isPlaying;

	public VideoHandler(Channel pc) {
		parentChannel = pc;
		videoClients = new HashMap<ClientHandler, VideoClient>();
		globalTime = 0.0;
		new Thread(this).start();
	}

	@Override
	public void run() {
		while(!stopping) {
			try {
				while(isPlaying) {
					try {
						sendTimeUpdate();
						requestTimeUpdate();
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				Thread.sleep(100);
			} catch(InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	void handleVideoPacket(Packet p, ClientHandler c) {
		switch((String)p.getData()[0]) {
		case "currentVideo":
			c.sendPacket(new Packet("video", "videoList", currentVideo));
			requestTimeUpdate();
			sendTimeUpdate();
			break;
		case "seekTo":
//			pauseVideo();
			sendVideoPacket(new Packet("video", "seek", (Double)p.getData()[1]));
			break;
		case "addRequest":
			setVideo((String)p.getData()[1]);
			c.sendServerMessage("Video add accepted");
			break;
		case "pause":
			pauseVideo();
			break;
		case "play":
			if(canPlay) {
				sendVideoPacket(new Packet("video", "play"));
				isPlaying = true;
			}
			break;
		case "buffering":
			pauseVideo();
			setCanPlay(c, false);
			setControls(parentChannel.getOwner(), false);
			updateClientCanPlay(c, false);
			break;
		case "canPlayThrough":
			setCanPlay(c, true);
			updateClientCanPlay(c, true);
			canGlobalPlay();
			break;
		case "cantPlayThrough":
			setCanPlay(c, false);
			updateClientCanPlay(c, false);
			canGlobalPlay();
			break;
		case "timeUpdate":
			Double time = (Double)p.getData()[1];
			/*
			 * Check if the clients current time is outside of acceptable boundaries
			 * If so then pause everyone until this client can catch up
			 */
			if(c.getName().equals(parentChannel.getOwner().getName())) {
				this.globalTime = time;
			} else if(time + minTimeDiff < globalTime || time - minTimeDiff > globalTime) {
				pauseVideo();
//				setControls(parentChannel.getOwner(), false);
				c.sendPacket(new Packet("video", "seek", globalTime));
			}
			try {
				videoClients.get(c).updateTime(time);
			} catch(NullPointerException e) {
				//This is for if it tries to update a client who has disconnected
			}
			break;
		case "wantsVideoUpdates":
			addClient(c);
			break;
		case "shutdown":
			removeClient(c);
			break;
		}
	}

	void setCanPlay(ClientHandler c, boolean can) {
		if(!(c == null)) {
			videoClients.get(c).setCanPlay(can);
		}
	}

	void updateClientCanPlay(ClientHandler c, boolean canPlay) {
		if(!(c == null)) {
			sendVideoPacket(new Packet("video", "canPlay", c.getName(), canPlay));
		}
	}

	synchronized void canGlobalPlay() {
		synchronized(videoClients) {
			boolean last = true;
			for(VideoClient c : videoClients.values()) {
				if(!c.canPlay()) {
					last = false;
					break;
				}
			}
			if(last) {
				setControls(parentChannel.getOwner(), true);
			}
			this.canPlay = last;
		}
	}

	void setVideo(String url) {
		currentVideo = url;
		sendVideoPacket(new Packet("video", "loadURL", currentVideo));
	}

	void pauseVideo() {
		sendVideoPacket(new Packet("video", "pause"));
		isPlaying = false;
	}

	private void setControls(ClientHandler c, boolean toggle) {
		if(!toggle) {
			c.sendPacket(new Packet("video", "disableControls"));
		} else {
			c.sendPacket(new Packet("video", "enableControls"));
		}
	}

	void sendTimeUpdate() {
		synchronized(videoClients) {
			Object[] times = new Object[videoClients.size()*2];
			int i = 0;
			for(VideoClient c : videoClients.values()) {
				times[i] = c.getName();
				times[i+1] = c.getTime();
				i += 2;
			}
			sendVideoPacket(new Packet("video", "timeUpdate", times));
		}
	}

	void requestTimeUpdate() {
		synchronized(videoClients) {
			for(VideoClient c : videoClients.values()) {
				if(c.canPlay()) {
					c.getClient().sendPacket(new Packet("video", "requestTimeUpdate"));
				}
			}
		}
	}

	void addClient(ClientHandler c) {
		VideoClient vc = new VideoClient(c);
		c.sendPacket(getStartupPacket());
		synchronized(videoClients) {
			videoClients.put(c, vc);
		}
		sendVideoPacket(new Packet("video", "newClient", vc, parentChannel.getOwner().getName()));
	}

	private Packet getStartupPacket() {
		Object[] clientList = new Object[videoClients.size()];
		int i = 0;
		for(VideoClient v : videoClients.values()) {
			clientList[i] = v;
			i++;
		}
		return new Packet("video", "startUp", clientList, parentChannel.getOwner().getName(), currentVideo, globalTime);
	}

	void removeClient(ClientHandler c) {
		VideoClient temp = videoClients.get(c);
		videoClients.remove(c);
		sendVideoPacket(new Packet("video", "removeClient", temp, parentChannel.getOwner().getName()));
	}

	private void sendVideoPacket(Packet p) {
		synchronized(videoClients) {
			for(VideoClient c : videoClients.values()) {
				p.type = "video";
				c.getClient().sendPacket(p);
			}
		}
	}

	public void shutdown() {
		this.stopping = true;
		this.isPlaying = false;
	}
}

package org.zapto.mike.mrstreamserver;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

class Channel{

	protected ObservableList<ClientHandler> clients;
	private ClientHandler channelOwner;
	private String channelName;
	private VideoHandler videoHandler;
	private Server server;


	public Channel(ClientHandler owner, String channelName, Server server) {
		this.channelOwner = owner;
		this.channelName= channelName;
		this.server = server;
		this.videoHandler = new VideoHandler(this);
		clients = FXCollections.observableArrayList();
	}

	void closeChannel() {
		videoHandler.shutdown();
		server.out.println("Closing channel " + channelName);
		synchronized(clients) {
			for(ClientHandler c : clients) {
				c.setChannel(server.rootChannel);
			}
		}
		server.removeChannel(this);
	}

	void addClient(ClientHandler client) {
		clients.add(client);
		/*
		 * Update clientList
		 */
		client.sendMessage("Owner of channel - " + channelOwner);
	}

	void removeClient(ClientHandler client) {
		if(client.getName().equals(channelOwner) && clients.size() > 1) {
			clients.remove(client);
			channelOwner = clients.get(0);
			videoHandler.removeClient(client);
		} else {
			clients.remove(client);
			videoHandler.removeClient(client);
		}
		if(clients.size() <= 0) {
			closeChannel();
			return;
		}
		/*
		 * Update clientList
		 */
		notifyChannel(client.getName() + " left the channel");
		client.sendMessage("You have left the channel " + channelName);
	}

	void notifyChannel(String message) {
		synchronized(clients) {
			for(ClientHandler c : clients) {
				c.sendMessage(message);
			}
		}
	}

	void notifyServerChat(String message) {
		synchronized(clients) {
			for(ClientHandler c : clients) {
				c.sendServerMessage(message);
			}
		}
	}

	String getName() {
		return channelName;
	}

	ClientHandler getOwner() {
		return channelOwner;
	}

	void handleVideoPacket(Packet p, ClientHandler c) {
		videoHandler.handleVideoPacket(p, c);
	}
}



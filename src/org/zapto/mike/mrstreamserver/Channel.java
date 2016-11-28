package org.zapto.mike.mrstreamserver;

import java.util.ArrayList;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

class Channel{

	protected ObservableList<ClientHandler> clients;
	private ClientHandler channelOwner;
	private String channelName;
	private VideoHandler videoHandler;
	private Server server;
	private ArrayList<String> clientList;


	public Channel(ClientHandler owner, String channelName, Server server) {
		this.channelOwner = owner;
		this.channelName= channelName;
		this.server = server;
		this.clientList = new ArrayList<String>();
		this.videoHandler = new VideoHandler(this);
		clients = FXCollections.observableArrayList();
	}
	
	public Channel(String ownerName, String channelName, Server server) {
		this.channelOwner = new ClientHandler(ownerName, server);
		this.channelName= channelName;
		this.server = server;
		this.clientList = new ArrayList<String>();
		this.videoHandler = new VideoHandler(this);
		clients = FXCollections.observableArrayList();
	}

	void closeChannel() {
		videoHandler.shutdown();
		server.out.println("Closing channel " + channelName);
		synchronized(clients) {
			for(ClientHandler c : clients) {
				c.setChannel(null);
			}
		}
		server.removeChannel(this);
	}

	void addClient(ClientHandler client) {
		clientList.add(client.getName());
		client.sendPacket(new Packet("clientList", "initial", clientList));
		for (ClientHandler clientHandler : clients) {
			clientHandler.sendPacket(new Packet("clientList", "joined", client.getName()));
		}
		clients.add(client);
		client.sendMessage("Owner of channel - " + channelOwner);
	}

	void removeClient(ClientHandler client) {
		for (ClientHandler ch : clients) {
			if(ch != client) {
				ch.sendPacket(new Packet("clientList", "left", client.getName()));
			}
		}
		if(client.getName().equals(channelOwner) && clients.size() >= 1 && !channelName.equals("Root")) {
			clients.remove(client);
			channelOwner = clients.get(0);
			videoHandler.removeClient(client);
		} else {
			clients.remove(client);
			videoHandler.removeClient(client);
		}
		if(clients.size() <= 0 && !channelName.equals("Root")) {
			closeChannel();
			return;
		}
		clientList.remove(client.getName());
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



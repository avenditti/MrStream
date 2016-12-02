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
		client.sendServerMessage("Owner of channel - " + channelOwner);
	}

	void removeClient(ClientHandler client) {
		for (ClientHandler ch : clients) {
			if(ch != client) {
				ch.sendPacket(new Packet("clientList", "left", client.getName()));
			}
		}
		client.sendPacket(new Packet("clientList", "clear"));
		clients.remove(client);
		if(clients.size() <= 0) {
			closeChannel();
			return;
		}
		if(clients.size() >= 1) {
			promotePlayer(clients.get(0).getName(), client);
			videoHandler.removeClient(client);
			clientList.remove(client.getName());
			notifyServerChat(client.getName() + " left the channel");
			client.sendServerMessage("You have left the channel " + channelName);
		}
	}
	
	void promotePlayer(String name, ClientHandler c) {
		if(c.getName().equals(getOwner().getName())) {
			ClientHandler temp = null;
				for (ClientHandler ch : clients) {
					if(ch.getName().equals(name)) {
						temp = ch;
					}
				}
				if(temp == null) {
					c.sendServerMessage("Could not find user");
					return;
				} 
				temp.setChannel(null);
				channelOwner = temp;
				notifyServerChat(name + " was promoted to channel owner");
				temp.sendServerMessage("You have been promoted");
		} else {
			c.sendServerMessage("Insufficent Permissions");
		}
	}
	
	void kickClient(String name, ClientHandler c) {
		if(c.getName().equals(getOwner().getName())) {
			ClientHandler temp = null;
				for (ClientHandler ch : clients) {
					if(!ch.getName().equals(name)) {
						ch.sendPacket(new Packet("clientList", "left", name));
					} else {
						temp = ch;
					}
				}
				if(temp == null) {
					c.sendServerMessage("Could not find user");
					return;
				}
				temp.setChannel(null);
				clients.remove(name);
				videoHandler.removeClient(temp);
				clientList.remove(name);
				temp.sendPacket(new Packet("clientList", "clear"));
				notifyServerChat(name + " was kicked from the channel");
				temp.sendServerMessage("You have been kicked from the channel " + channelName);
		} else {
			c.sendServerMessage("Insufficent Permissions");
		}
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



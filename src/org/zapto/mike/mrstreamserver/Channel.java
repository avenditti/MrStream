package org.zapto.mike.mrstreamserver;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

class Channel{

	protected ObservableList<ClientHandler> clients;
	private ClientHandler channelOwner;
	private String channelName;
	private VideoHandler videoHandler;
//	private Label name;
//	private Label ownerName;
//	private Label population;
//	private GridPane channelPane;


	public Channel(ClientHandler owner, String channelName) {
		this.channelOwner = owner;
		this.channelName= channelName;
		this.videoHandler = new VideoHandler(this);
//		this.name = new Label(channelName);
//		this.ownerName = new Label(channelOwner.getName());
//		this.population = new Label(1 + "");
//		this.channelPane = new GridPane();
//		ColumnConstraints cc = new ColumnConstraints();
//		cc.setPercentWidth(33);
//		channelPane.getColumnConstraints().addAll(cc, cc, cc);
//		channelPane.add(this.name, 0, 0);
//		channelPane.add(this.ownerName, 1, 0);
//		channelPane.add(this.population, 2, 0);
		clients = FXCollections.observableArrayList();
		clients.addListener(new ListChangeListener<ClientHandler>() {

			@Override
			public void onChanged(javafx.collections.ListChangeListener.Change<? extends ClientHandler> c) {
				if(clients.size() > 0) {
					notifyServerChat(clients.get(clients.size()-1).getName() + " joined the channel");
				}
			}

		});

	}

	void closeChannel() {
		videoHandler.shutdown();
		synchronized(clients) {
			for(ClientHandler c : clients) {
				c.setChannel(Server.rootChannel);
			}
		}
	}

	void addClient(ClientHandler client) {
		if(channelOwner == null) {
			channelOwner = client;
		}
		clients.add(client);
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
		notifyServerChat(client.getName() + " left the channel");
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



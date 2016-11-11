package org.zapto.mike.mrstreamserver;

import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;

class Server implements Runnable{

	private ServerSocket serverSock;
	private ArrayList<ClientHandler> globalClientList;
	private ArrayList<Channel> channels;
	private PrintStream out;
	private BooleanProperty stopping;
	static final Channel rootChannel = new Channel(null,"ROOT") {

		@Override
		void removeClient(ClientHandler client) {
			clients.remove(client);
			notifyServerChat(client.getName() + " left the channel");
		}
	};
	private final int port = 25567;

	public Server(PrintStream serverPrint) {
		/*
		 *	Create a boolean listener to shutdown the server socket when closed
		 */
		globalClientList = new ArrayList<ClientHandler>();
		stopping = new SimpleBooleanProperty(false);
		this.out = serverPrint;
		channels = new ArrayList<Channel>();
		channels.add(rootChannel);
		stopping.addListener((obj, oldVal, newVal) -> {
			try {
				serverSock.close();
			} catch (IOException e) {
				out.println("Server Stopped");
			}
		});
		/*
		 * 	Create the server on the given port
		 */
		try {
			serverSock = new ServerSocket(port);
		} catch (IOException e) {
			out.println("Error starting server");
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		/*
		 * Anonymous class for handling connections
		 */
		class ConnectionHandler implements Runnable{

			private Socket sock;
			private Server server;

			public ConnectionHandler(Socket sock, Server server) {
				this.server = server;
				this.sock = sock;
			}

			@Override
			public void run() {
				try {
					ClientHandler client = new ClientHandler(sock, out, server);
					channels.get(0).addClient(client);
					client.setChannel(channels.get(0));
					globalClientList.add(client);
				} catch (IOException | ClassNotFoundException e) {
					try {
						sock.close();
					} catch (IOException e1) {}
				}
			}

		}
		/*
		 *	Wait for a client to connect and when they do create the client information then start a thread to connect the client
		 */
		while(!stopping.get()) {
			try {
				Socket sock = serverSock.accept();
				out.println("Client attempting connection to server");
				new Thread(new ConnectionHandler(sock, this)).start();
			} catch (IOException e) {
				System.out.println("Internal Server Error \nMaybe the server shutdown?");
			}
		}
	}

	void stop() {
		stopping.set(true);
		synchronized (channels) {
			for(Channel c : channels) {
				c.closeChannel();
			}
		}
		while(globalClientList.size() > 0) {
			globalClientList.get(0).closeConnection();
		}
	}

	boolean validName(String name) {
		if(name.equals("")) {
			return false;
		}
		for(ClientHandler c : globalClientList) {
			if(c.getName().equals(name)) {
				return false;
			}
		}
		return true;
	}

	void removeClient(ClientHandler client) {
		globalClientList.remove(client);
		notifyGlobalList(client.getName() + " disconnected from the server");
	}

	void notifyGlobalList(String message) {
		synchronized(globalClientList) {
			for(ClientHandler c : globalClientList) {
				c.sendServerMessage(message);
			}
		}
	}

	String getGlobalClientList() {
		String accum = "";
		if(globalClientList.size() > 0) {
			for(ClientHandler c : globalClientList) {
				accum += c.toString() + "\n";
			}
		} else {
			return "No clients\n";
		}
		return accum;
	}

	String getChannelList() {
		String channelNames = "";
		for(Channel c : channels) {
			channelNames += c.getName() + ":" + c.getOwner();
		}
		return channelNames;
	}

	void addChannel(Channel channel) {
		channels.add(channel);
		sendToGlobalList(new Packet("channel", "newChannel", channel.getName(), channel.getOwner().getName()));
	}

	boolean validChannelName(String name) {
		for(Channel c : channels) {
			if(c.getName().equals(name)) {
				return false;
			}
		}
		return true;
	}

	private void sendToGlobalList(Packet p) {
		for(ClientHandler c : globalClientList) {
			c.sendPacket(p);
		}
	}

	public boolean moveClient(ClientHandler client, String channelName) {
		for(Channel c : channels) {
			if(c.getName().equals(channelName)) {
				c.addClient(client);
				client.setChannel(c);
				return true;
			}
		}
		return false;
	}
}

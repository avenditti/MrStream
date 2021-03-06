package org.zapto.mike.mrstreamserver;

import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

class Server implements Runnable{

	private ServerSocket serverSock;
	private ArrayList<ClientHandler> globalClientList;
	private ArrayList<Channel> channels;
	PrintStream out;
	private BooleanProperty stopping;
	CommandHandler ch;
	private final int port = 25567;

	public Server(PrintStream serverPrint) {
		/*
		 *	Create a boolean listener to shutdown the server socket when closed
		 */
		globalClientList = new ArrayList<ClientHandler>();
		stopping = new SimpleBooleanProperty(false);
		this.out = serverPrint;
		channels = new ArrayList<Channel>();
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
		ch.stop();
		stopping.set(true);
		synchronized (channels) {
			for (int i = 1; i < channels.size(); i++) {
				channels.get(i).closeChannel();
			}
		}
		while(globalClientList.size() > 0) {
			globalClientList.get(0).closeConnection();
		}
		try {
			if(serverSock != null) {
				serverSock.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
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
		if(client.getChannel() != null) {
			client.getChannel().removeClient(client);
		}
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

	String[] getChannelList() {
		synchronized(channels) {
			String[] channelNames = new String[channels.size()];
			for (int i = 0; i < channelNames.length; i++) {
				channelNames[i] = channels.get(i).getName() + ":" + channels.get(i).getOwner().getName();
			}
			return channelNames;
		}
	}

	String getChannelListString() {
		String temp = "";
		for (Channel channel : channels) {
			temp += channel.getName() + "\n";
			for(ClientHandler c : channel.clients) {
				temp += c.getName() + "\n";
			}
		}
		return temp;
	}

	void addChannel(String channelName, ClientHandler owner) {
		Channel channel = new Channel(owner, channelName, this);
		channels.add(channel);
		sendToGlobalList(new Packet("channel", "newChannel", owner.getName(), channelName));
		if(owner.getChannel() != null) {
			owner.getChannel().removeClient(owner);
		}
		moveClient(channel, owner);
	}

	private void moveClient(Channel channel, ClientHandler client) {
		boolean canJoin = true; //Create auth for joining channel here
		/*
		 * Send update channel populations here
		 */
		if(canJoin && channel != client.getChannel()) {
			client.setChannel(channel);
			channel.addClient(client);
		} else {
			//Failed Auth
		}
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

	boolean moveClient(ClientHandler client, String channelName) {
		String currentChannelName = "Root"; // Implement no empty name channel ----------------
		if(client.getChannel() != null) {
			currentChannelName = client.getChannel().getName();
		}
		for(Channel c : channels) {
			System.out.println(channelName);
			if(c.getName().equals(channelName) && !currentChannelName.equals(channelName)) {
				c.addClient(client);
				client.setChannel(c);
				return true;
			}
		}
		return false;
	}

	void removeChannel(Channel channel) {
		channels.remove(channel);
		sendToGlobalList(new Packet("channel", "remove", channel.getName()));
	}
}

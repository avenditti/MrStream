package org.zapto.mike.mrstreamserver;

import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;

class ClientHandler implements Runnable{

	private String name;
	private PacketHandler handler;
	private PrintStream out;
	private Server server;
	private Thread thread;
	private boolean stopping;
	private Channel channel;
	private double currentTime;
	private Socket clientSock;

	public ClientHandler(Socket sock, PrintStream out, Server server) throws IOException, ClassNotFoundException {
		this.name = "null";
		this.stopping = false;
		this.out = out;
		this.server = server;
		this.clientSock = sock;
		handler = new PacketHandler(sock);
		thread = new Thread(this);
		/*
		 * Get a login packet from the client
		 */
		Packet p = handler.recievePacket();
		/*
		 * 	If it is a login packet see if the name is available. If it is not then keep requesting a new name until it is
		 */
		if(p.type.equals("login")) {
			if(!this.server.validName((String)p.data[0])) {
				out.println("Client tried to connect with invalid name: " + (String)p.data[0]);
				handler.sendPacket(new Packet("nameTaken"));
				throw new IOException();
			}
			name = (String)p.data[0];
			handler.sendPacket(new Packet("nameAccepted", name ));
			handler.sendPacket(new Packet("channel", "initList", server.getChannelList()));
			out.println(name + " connected to the server.");
			server.notifyGlobalList(name + " connected to the server.");
			thread.start();
		} else {
			throw new IOException();
		}
	}
	
	public ClientHandler(String name, Server server) {
		this.name = name;
		this.server = server;
	};

	@Override
	public void run() {
		/*
		 * 	Wait for client to send a packet and receive it
		 * Create a thread to handle packets
		 */
		class PacketThread implements Runnable{

			Packet p;

			PacketThread(Packet p) {
				this.p = p;
			}
			@Override
			public void run() {
				try {
					switch(p.getType()) {
					case "chat":
						if(channel != null) {
							out.println(channel.getName() + ": " + name + ":" + p.getData()[0]);
							channel.notifyChannel(name + ": " + (String)p.getData()[0]);
						}
						break;
					case "shutdown":
						closeConnection();
						break;
					case "video":
						handleVideoPacket(p);
						break;
					case "channel":
						handleChannelPacket(p);
						break;
					}
				} catch(IOException e) {
					out.println("Error recieving packet from " + name);
				}
			}

		}
		while(!stopping) {
			try {
				new Thread(new PacketThread(handler.recievePacket())).start();
			} catch(ClassNotFoundException | IOException e) {
				out.println("Client forced disconnection of client " + name);
				closeConnection();
				stopping = true;
			}
		}
	}
	private void handleChannelPacket(Packet p) {
		/*
		 * Determine what type of channel packet has been received and handle it.
		 */
		switch((String)p.getData()[0]) {
		case "join":
			if(joinChannel((String)p.getData()[1])) {
				sendMessage("Joined channel " + channel.getName());
				channel.notifyChannel(name + " has joined the channel");
			} else {
				sendMessage("Cannot join channel " + (String)p.getData()[1]);
			}
			break;
		case "createChannel":
			if(server.validChannelName((String)p.getData()[1])) {
				server.addChannel((String)p.getData()[1], this);
			}
			break;
		case "kick":
			channel.kickClient((String)p.getData()[1], this);
			break;
		case "promote":
			channel.promotePlayer((String)p.getData()[1], this);
		}

	}

	private void handleVideoPacket(Packet p) throws IOException {
		if(channel != null) {
			switch((String)p.getData()[0]) {
			case "play":
				if(name.equals(channel.getOwner().getName())) {
					channel.handleVideoPacket(p, this);
				} else {
					sendServerMessage("Invalid Permissions");
				}
				break;
			case "seekTo":
				if(name.equals(channel.getOwner().getName())) {
					channel.handleVideoPacket(p, this);
				} else {
					sendServerMessage("Invalid Permissions");
				}
				break;
			case "pause":
				if(name.equals(channel.getOwner().getName())) {
					channel.handleVideoPacket(p, this);
				} else {
					sendServerMessage("Invalid Permissions");
				}
				break;
			case "addRequest":
				if(name.equals(channel.getOwner().getName())) {
					channel.handleVideoPacket(p, this);
				} else {
					sendServerMessage("Invalid Permissions");
				}
				break;
			default:
				channel.handleVideoPacket(p, this);
			}
		}
	}

	private boolean joinChannel(String channelName) {
		return server.moveClient(this, channelName);
	}

	void setChannel(Channel c) {
		if(channel != null) {
			channel.removeClient(this);
		}
		channel = c;
		if(c != null) {
			sendPacket(new Packet("channel", "move", c.getName()));
		} else {
			sendPacket(new Packet("channel", "move", ""));
		}
	}

	String getName() {
		return name;
	}

	void closeConnection() {
		if(channel != null) {
			channel.removeClient(this);
		}
		stopping = true;
		handler.stop();
		server.removeClient(this);
	}

	@Override
	public String toString() {
		return name;
	}

	void sendMessage(String message) {
		synchronized(handler) {
			if(!clientSock.isClosed()) {
				handler.sendPacket(new Packet("chat", message));
			}
		}
	}

	void sendPacket(Packet p) {
		synchronized(handler) {
			if(!clientSock.isClosed()) {
				handler.sendPacket(p);
			}
		}
	}

	void sendServerMessage(String message) {
		if(!clientSock.isClosed()) {
			handler.sendPacket(new Packet("serverChat", message));
		}
	}

	Double getCurrentTime() {
		return currentTime;
	}

	Channel getChannel() {
		return channel;
	}
}
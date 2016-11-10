package org.zapto.mike.mrstreamserver;

import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;

class ClientHandler implements Runnable{

	private Socket sock;
	private String name;
	private PacketHandler handler;
	private PrintStream out;
	private Server server;
	private Thread thread;
	private boolean stopping;
	private Channel channel;
	private double currentTime;

	public ClientHandler(Socket sock, PrintStream out, Server server) throws IOException, ClassNotFoundException {
		this.name = "null";
		this.stopping = false;
		this.sock = sock;
		this.out = out;
		this.server = server;
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
			}
			/*
			 * 	Once the name is accepted tell the client
			 */
			name = (String)p.data[0];
			handler.sendPacket(new Packet("nameAccepted", name ));
			out.println(name + " connected to the server.");
			server.notifyGlobalList(name + " connected to the server.");
			thread.start();
		} else {
			throw new IOException();
		}
	}

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
						out.println(name + ":" + p.getData()[0]);
						channel.notifyChannel(name + ": " + (String)p.getData()[0]);
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
		}

	}

	private void handleVideoPacket(Packet p) throws IOException {
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

	private boolean joinChannel(String channelName) {
		return server.moveClient(this, channelName);
	}

	void setChannel(Channel c) {
		channel = c;
	}

	String getName() {
		return name;
	}

	void closeConnection() {
		stopping = true;
		handler.stop();
		server.removeClient(this);
		channel.removeClient(this);
	}

	@Override
	public String toString() {
		return name + " " + sock.getInetAddress().getHostAddress();
	}

	void sendMessage(String message) {
		synchronized(handler) {
			try {
				handler.sendPacket(new Packet("chat", message));
			} catch (IOException e) {
				out.println("Failed to send chat packet to client : " + name);
			}
		}
	}

	void sendPacket(Packet p) {
		synchronized(handler) {
			try {
				handler.sendPacket(p);
			} catch (IOException e) {
				out.println("Failed to send " + p.type + " packet to client :" + name);
				if(e instanceof java.net.SocketException) {
					this.closeConnection();
				}
				e.printStackTrace();
			}
		}
	}

	void sendServerMessage(String message) {
		try {
			handler.sendPacket(new Packet("serverChat", message));
		} catch (IOException e) {
			out.println("Failed to send server chat packet to client : " + name + " MESSAGE: " + message);
			e.printStackTrace();
		}
	}

	Double getCurrentTime() {
		return currentTime;
	}
}
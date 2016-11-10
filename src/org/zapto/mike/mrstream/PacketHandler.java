package org.zapto.mike.mrstream;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.Socket;

import org.zapto.mike.mrstreamserver.Packet;
import org.zapto.mike.mrstreamserver.Video;
import org.zapto.mike.mrstreamserver.VideoClient;

class PacketHandler implements Runnable{

	private Socket sock;
	private ObjectOutputStream oos;
	private ObjectInputStream iis;
	private PrintStream serverOut;
	private PrintStream channelOut;
	private boolean stopping;
	private Object buffer;
	private VideoSync vs;

	public PacketHandler(Socket sock, PrintStream serverOut, PrintStream channelOut) {
		try {
			stopping = false;
			this.sock = sock;
			oos = new ObjectOutputStream(sock.getOutputStream());
			iis = new ObjectInputStream(sock.getInputStream());
			this.serverOut = serverOut;
			this.channelOut = channelOut;
		} catch (IOException e) {
			System.out.println("Error creating streams");
		}
	}

	@Override
	public void run() {
		class PacketThread implements Runnable{

			Packet p;

			PacketThread(Packet p) {
				this.p = p;
			}
			@Override
			public void run() {
				switch(p.getType()) {
				case "chat":
					channelOut.println((String)p.getData()[0]);
					break;
				case "serverChat":
					serverOut.println((String)p.getData()[0]);
					break;
				case "video":
					if(vs != null) {
						switch((String)p.getData()[0]) {
						case "play":
							vs.playVideo();
							break;
						case "pause":
							vs.pauseVideo();
							break;
						case "seek":
							vs.seekTo((Double)p.getData()[1]);
							break;
						case "requestTimeUpdate":
							vs.getTime();
							break;
						case "timeUpdate":
							vs.updateClientTimes((Object[]) p.getData()[1]);
							break;
						case "addRequest":
							if(((String)p.getData()[1]).equals("accepted")) {
								serverOut.println("Add request accepted");
							} else {
								serverOut.println("Add request denied. Reason: " + (String)p.getData()[1]);
							}
							break;
						case "newClient":
							vs.addClientToList((VideoClient)p.getData()[1], (String)p.getData()[2]);
							break;
						case "removeClient":
							vs.removeClientFromList((VideoClient)p.getData()[1], (String)p.getData()[2]);
							break;
						case "loadURL":
							vs.loadVideo((Video)p.getData()[1]);
							break;
						case "canPlay":
							vs.updateCanPlay((String)p.getData()[1], (boolean)p.getData()[2]);
							break;
						case "startUp":
							vs.buildInitalClientList((Object[])p.getData()[1], (String)p.getData()[2]);
							if(p.getData()[3] != null) {
								vs.loadVideo((Video)p.getData()[3]);
								vs.seekTo((double)p.getData()[4]);
							}
							break;
						}
					}
					break;
				}
			}

		}
		while(!stopping) {
			try {
				new Thread(new PacketThread(recievePacket())).start();
			} catch(ConnectionError e) {
				serverOut.println("Client forced disconnection");
				stopping = true;
			}
		}
	}

	void sendPacket(Packet p) throws IOException {
		oos.writeObject(p);
	}

	Packet recievePacket() throws ConnectionError {
		try {
			Object o = iis.readObject();
			return (Packet)o;
		} catch(ClassCastException | ClassNotFoundException | IOException e) {
			throw new ConnectionError();
		}
	}

	void close() {
		try {
			stopping = true;
			sock.close();
			iis.close();
			oos.close();
		} catch (IOException e) {
			serverOut.println("Error closing client streams");
		}
	}

	public Object getBuffer() {
		return buffer;
	}

	public void setVideoSync(VideoSync videoSync) {
		this.vs = videoSync;
	}
}

@SuppressWarnings("serial")
class ConnectionError extends Exception{

}
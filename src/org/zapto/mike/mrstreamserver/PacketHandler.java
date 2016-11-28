package org.zapto.mike.mrstreamserver;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import org.zapto.mike.mrstreamserver.Packet;

public class PacketHandler {

	private Socket sock;
	private ObjectOutputStream oos;
	private ObjectInputStream iis;

	public PacketHandler(Socket sock) {
		try {
			this.sock = sock;
			oos = new ObjectOutputStream(sock.getOutputStream());
			iis = new ObjectInputStream(sock.getInputStream());
		} catch (IOException e) {
			System.out.println("Error creating streams");
		}
	}

	void sendPacket(Packet p) throws IOException{
		synchronized(oos) {
			oos.writeObject(p);
		}
	}

	Packet recievePacket() throws IOException, ClassNotFoundException {
		return (Packet)iis.readObject();
	}

	void stop() {
		try {
			iis.close();
			oos.close();
			sock.close();
		} catch (IOException | NullPointerException e) {
			System.out.println("Error closing client socket");
		}
	}
}

@SuppressWarnings("serial")
class ConnectionError extends Exception{

}

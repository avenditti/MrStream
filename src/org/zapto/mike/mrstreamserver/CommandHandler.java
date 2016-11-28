package org.zapto.mike.mrstreamserver;

import java.io.PrintStream;
import java.util.Scanner;

import javafx.event.EventHandler;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;

class CommandHandler implements Runnable{

	private TextField field;
	private boolean isConsole = true;
	private PrintStream out;
	private Server server;
	private boolean stopping;
	private Scanner scan;

	public CommandHandler(PrintStream out, Server server) {
		this.server = server;
		this.out = out;
		server.ch = this;
	}

	public CommandHandler(TextField field, PrintStream out, Server server) {
		this.server = server;
		this.out = out;
		this.field = field;
		isConsole = false;
		server.ch = this;
	}

	@Override
	public void run() {
		if(isConsole) {
			startConsole();
		} else {
			gui();
		}
	}

	private void gui() {
		field.setOnKeyTyped(new EventHandler<KeyEvent>() {

			@Override
			public void handle(KeyEvent event) {
				if((int)event.getCharacter().toCharArray()[0] == 13) {
					out.println(field.getText());
					execute(field.getText());
				}
			}

		});
	}

	private void execute(String text) {
		switch(text.toLowerCase()) {
		case "list":
			listClients();
			break;
		case "listchannels":
			listChannels();
			break;
		}
	}

	private void listChannels() {
		out.print(server.getChannelListString());
	}
	
	private void listClients() {
		out.print(server.getGlobalClientList());
	}
	
	void stop() {
		if(scan != null) {
			stopping = true;
			scan.close();
		}
	}
	
	private void startConsole() {
		scan = new Scanner(System.in);
		stopping = false;
		while(!stopping) {
			execute(scan.next());
		}
	}


}
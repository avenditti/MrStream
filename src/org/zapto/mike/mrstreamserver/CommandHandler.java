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

	public CommandHandler(PrintStream out, Server server) {
		this.server = server;
		this.out = out;
	}

	public CommandHandler(TextField field, PrintStream out, Server server) {
		this.server = server;
		this.out = out;
		this.field = field;
		isConsole = false;
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
		}
	}

	private void listClients() {
		out.print(server.getGlobalClientList());
	}

	private void startConsole() {
		@SuppressWarnings("resource")
		Scanner scan = new Scanner(System.in);
		while(true) {
			execute(scan.next());
		}
	}


}
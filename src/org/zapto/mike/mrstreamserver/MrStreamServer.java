package org.zapto.mike.mrstreamserver;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class MrStreamServer extends Application{

	public static void main(String[] args) {
		/*
		 * 	Determine if nogui flag exists
		 */
		if(args.length > 0 && args[0].equals("nogui")) {
			/*
			 * 	If nogui flag start without gui
			 */
			Server server = new Server(System.out);
			CommandHandler ch = new CommandHandler(System.out, server);
			new Thread(ch).start();
			new Thread(server).start();
		} else {
			/*
			 * 	If no nogui flag start gui
			 */
			launch();
		}
	}

	public void start(Stage stage) {
		/*
		 * 	Create gui
		 */
		stage = new Stage();
		GridPane grid = new GridPane();
		TextArea textArea = new TextArea();
		TextField field = new TextField();
		PrintStream out = new PrintStream(new OutputStream() {

			@Override
			public void write(int arg0) throws IOException {
				Platform.runLater(new Runnable() {
			        public void run() {
			        	textArea.appendText((char)arg0 + "");
			        }
			    });
			}

		});
		stage.setScene(new Scene(grid, 500, 500));
		textArea.setStyle(""
		        + "-fx-font-family: consolas;"
		        + "-fx-text-fill: lime;"
		        + "-fx-control-inner-background: black");
		textArea.setWrapText(true);
		textArea.setEditable(false);
		textArea.setPrefSize(600, 445);
		grid.add(textArea, 0, 0);
		grid.add(field, 0, 1);
		stage.show();
		//Create the server main listening thread and the command handler thread
		Server server = new Server(out);
		CommandHandler ch = new CommandHandler(field, out, server);
		stage.setOnCloseRequest(new EventHandler<WindowEvent>() {

			@Override
			public void handle(WindowEvent event) {
				server.stop();
			}

		});
		//Start the threads
		new Thread(ch).start();
		new Thread(server).start();
	}
}




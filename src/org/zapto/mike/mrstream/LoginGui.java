package org.zapto.mike.mrstream;

import java.io.IOException;

import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

public class LoginGui {

	@FXML
	VBox vbox;
	@FXML
	Button close;
	@FXML
	TextField serverAddress;
	@FXML
	TextField alias;
	@FXML
	Label statusLabel;
	@FXML
	Button connect;
	@FXML
	Pane background;
	@FXML
	AnchorPane anchorPane;

	private Stage main;
	private MrStream mrStream;

	public LoginGui(Stage stage, MrStream mrStream) {
		main = stage;
		this.mrStream = mrStream;
	}

	@FXML
	private void initialize() {
		FadeTransition fadeOut = new FadeTransition();
		fadeOut.setNode(anchorPane);
		fadeOut.setDuration(new Duration(500));
	    fadeOut.setFromValue(1.0);
	    fadeOut.setToValue(0.0);
	    FadeTransition fadeIn = new FadeTransition();
		fadeIn.setNode(anchorPane);
		fadeIn.setDuration(new Duration(500));
	    fadeIn.setFromValue(0.0);
	    fadeIn.setToValue(1.0);
	    vbox.setOpacity(50);
	    main.setOnShown(new EventHandler<WindowEvent>() {

			@Override
			public void handle(WindowEvent event) {
			    fadeIn.play();
			}

		});
	    close.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
			    fadeOut.play();
			    fadeOut.setOnFinished(new EventHandler<ActionEvent>() {

					@Override
					public void handle(ActionEvent event) {
						main.close();
					}

			    });
			}

		});
	    connect.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				try {
					mrStream.connect(serverAddress.getText(),alias.getText());
				} catch(IOException | ConnectionError e) {
					System.out.println("Server rejected connection");
				}
			}

		});
	}

	void changeStatus(String status) {
		statusLabel.setText(status);
	}

	void closeWindow() {
		close.fireEvent(new ActionEvent());
	}
}

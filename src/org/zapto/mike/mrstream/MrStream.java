package org.zapto.mike.mrstream;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

import org.zapto.mike.mrstreamserver.Packet;

import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

public final class MrStream extends Application {

	@FXML
	MenuButton menuButton;
	@FXML
	VBox rootVBox;
	@FXML
	Pane background;
	@FXML
	TextArea serverText;
	@FXML
	TextArea channelText;
	@FXML
	Tab	channelTab;
	@FXML
	Tab serverTab;
	@FXML
	Button closeButton;
	@FXML
	Button minimizeButton;
	@FXML
	TextField inputText;
	@FXML
	AnchorPane anchorPane;
	@FXML
	Button button1;
	@FXML
	Button promote;
	@FXML
	Button kick;
	@FXML
	Button joinChannel;
	@FXML
	Button createChannel;
	@FXML
	TextField channelName;
	@FXML
	VBox channelList;
	@FXML
	VBox clientList;
	@FXML
	TextField channelNameField;

	private double xOffset = 0;
    private double yOffset = 0;
    private Socket sock;
	private PacketHandler handler;
	private String name;
	private Stage main;
	private PrintStream serverStream;
	private PrintStream channelStream;
	private LoginGui lg;
	private VideoSync videoSync;
	private HashMap<String, Label> clients;
	private ArrayList<Channel> channels;
	private Channel selectedChannel;
	private Label selectedClient;

	@FXML
	private void initialize() {
		inputText.setEditable(false);
		makeDragable(background);
		makeDragable(rootVBox);
		initializeWindowButtons();
	}

	public static void main(String[] args){
		launch();
	}

	@Override
	public void start(Stage stage) throws Exception {
		main = new Stage();
		try {
			channels = new ArrayList<Channel>();
			clients = new HashMap<String, Label>();
			/*
			 * Initialize the gui with the fxml document
			 */
			FXMLLoader fxml = new FXMLLoader(MrStream.class.getResource("fxml/MrStreamGui.fxml"));
			fxml.setController(this);
			this.serverStream = getServerStream();
			this.channelStream = getChannelStream();
			main.initStyle(StageStyle.TRANSPARENT);
			main.setScene(new Scene(fxml.load(),Color.TRANSPARENT));
			createHandlers();
			main.show();
		} catch (IOException e) {
			System.out.println("Error creating main GUI");
		}
	}

	void shutdown() {
		try {
			handler.sendPacket(new Packet("shutdown"));
		} catch (IOException | NullPointerException e) {}
		try {
			videoSync.shutdown();
		} catch(NullPointerException e) {}
		main.close();
	}

	private void createHandlers() {
		createChannel.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				try {
					handler.sendPacket(new Packet("channel", "createChannel", channelNameField.getText()));
				} catch (IOException e) {
				}
			}

		});
		joinChannel.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				try {
					if(selectedChannel != null) {
						handler.sendPacket(new Packet("channel", "join", selectedChannel.getName()));
					}
				} catch (IOException e) {
				}
			}

		});
		main.setOnCloseRequest(new EventHandler<WindowEvent>() {

			@Override
			public void handle(WindowEvent event) {
				shutdown();
			}

		});
		/*
		 * Handle the show video sync button
		 */
		button1.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				if(sock != null) {
					videoSync.show();
				}
			}

		});
		promote.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				try {
					if(selectedClient != null) {
						handler.sendPacket(new Packet("channel", "promote", selectedClient.getText()));
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
		});
		kick.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				try {
					if(selectedClient != null) {
						handler.sendPacket(new Packet("channel", "kick", selectedClient.getText()));
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
		});
		/*
		 * Handle connect option
		 */
		menuButton.getItems().get(0).setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				initializeLoginGui();
			}

		});
		/*
		 * Handle disconnect option
		 */
		menuButton.getItems().get(1).setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				try {
					channelList.getChildren().clear();
					clientList.getChildren().clear();
					handler.close();
					inputText.setEditable(false);
				} catch(NullPointerException e) {
					serverStream.println("Couldnt close socket");
				}

			}

		});

		inputText.setOnKeyPressed(new EventHandler<KeyEvent>() {

			@Override
			public void handle(KeyEvent key) {
				/*
				 * Whenever the user enters text in the chat and presses enter send and presses enter send the data to the server
				 */
				if(key.getCode().equals(KeyCode.ENTER)) {
					try {
						handler.sendPacket(new Packet("chat", inputText.getText()));
						inputText.clear();
					} catch (IOException e) {
						serverStream.println("Error sending message");
					}
				}
			}

		});

	}

	private void initializeLoginGui() {
		try {
			/*
			 * Create the login gui and the handlers for it
			 */
			Stage loginGui = new Stage();
			FXMLLoader fxml = new FXMLLoader(MrStream.class.getResource("fxml/LoginControllerGui.fxml"));
			lg = new LoginGui(loginGui, this);
			fxml.setController(lg);
			loginGui.initOwner(main);
			loginGui.initStyle(StageStyle.TRANSPARENT);
			loginGui.setScene(new Scene(fxml.load(), Color.TRANSPARENT));


			loginGui.show();
		} catch (IOException e) {
			System.out.println("Error creating login GUI");
		}
	}

	String getName() {
		return name;
	}

	void connect(String address, String name) throws ConnectionError, NumberFormatException, UnknownHostException, IOException {
		/*
		 * 	Attempt a login with the given address
		 */
		try {
			handler.close();
		} catch(NullPointerException e) {

		}
		try {
			String[] info = address.split(":");
			if(info.length == 2) {
				sock = new Socket(info[0], Integer.parseInt(info[1]));
			} else {
				sock = new Socket(info[0], 25567);
			}
		} catch (IOException e) {
			lg.changeStatus("No server on this address");
			return;
		}
		handler = new PacketHandler(sock, serverStream, channelStream, this);
		handler.sendPacket(new Packet("login", new Object[] { name }));
		boolean passLogin = false;
		Packet p;
		while(!passLogin) {
			p = (Packet)handler.recievePacket();
			switch(p.getType()) {
			case "nameTaken":
				lg.changeStatus("Name taken");
				throw new ConnectionError();
			case "nameAccepted":
				this.serverStream.println("Connected to the server hosted on " + sock.getInetAddress().getHostAddress());
				passLogin = true;
				this.name = name;
				lg.closeWindow();
				inputText.setEditable(true);
				videoSync = new VideoSync(handler, serverStream);
				handler.setVideoSync(videoSync);
				new Thread(handler).start();
				break;
			default:
				throw new ConnectionError();
			}
		}
	}

	private void initializeWindowButtons() {

		/*
		 * Make the two buttons in the top right minimize and close the window
		 */
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
	    /*
	     * Whenever the window closes or is minimized run the fade in and out animation
	     */
		closeButton.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
			    fadeOut.play();
			    fadeOut.setOnFinished(new EventHandler<ActionEvent>() {

					@Override
					public void handle(ActionEvent event) {
						shutdown();
					}

			    });
			}

		});
		closeButton.setTooltip(new Tooltip("Close the program"));
		minimizeButton.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				fadeOut.play();
			    class Minimize implements Runnable {

					@Override
					public void run() {
						try {
							Thread.sleep(500);
						} catch (InterruptedException e) {}
						Platform.runLater(new Runnable() {

							@Override
							public void run() {
								main.setIconified(true);
							}

						});
					}

			    }
			    new Thread(new Minimize()).start();
			}

		});
		minimizeButton.setTooltip(new Tooltip("Minimize the window"));
		/*
		 * Make the windows fade in when opened and deiconified
		 */
		main.setOnShown(new EventHandler<WindowEvent>() {

			@Override
			public void handle(WindowEvent arg0) {
				fadeIn.play();
			}

		});
		main.iconifiedProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> prop, Boolean oldValue, Boolean newValue) {
				if(!newValue) {
					fadeIn.play();
				}
			}

		});
	}

	private void makeDragable(Pane p) {
		/*
		 * Make the window dragable by the menu bar
		 */
		p.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            }
        });
		p.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                main.setX(event.getScreenX() - xOffset);
                main.setY(event.getScreenY() - yOffset);
            }
        });
	}

	private PrintStream getChannelStream() {
		/*
		 * Create the print stream for the channel tab text box
		 */
		return new PrintStream(new OutputStream() {

			@Override
			public void write(int arg0) throws IOException {
				Platform.runLater(new Runnable() {
			        public void run() {
			        	channelText.appendText((char)arg0 + "");
			        }
			    });
			}

		});
	}

	private PrintStream getServerStream() {
		/*
		 * Create the print stream for the server text box
		 */
		return new PrintStream(new OutputStream() {

			@Override
			public void write(int arg0) throws IOException {
				Platform.runLater(new Runnable() {
			        public void run() {
			        	serverText.appendText((char)arg0 + "");
			        }
			    });
			}

		});
	}

	void newClient(String clientName) {
		Label temp = new Label(clientName);
		clients.put(clientName, temp);
		temp.setOnMouseClicked(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
				if(selectedClient != null) {
					selectedClient.setTextFill(Color.BLACK);
				}
				selectedClient = (Label)event.getSource();
				selectedClient.setTextFill(Color.RED);
			}
			
		});
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				if(!clientList.getChildren().contains(clients.get(clientName))) {
					clientList.getChildren().add(clients.get(clientName));
				}
			}

		});	
	}
	
	void removeClient(String clientName) {
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				clientList.getChildren().remove(clients.get(clientName));
			}

		});
	}
	
	void clearClientList() {
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				clientList.getChildren().clear();
			}

		});
	}
	
	void newChannel(String ownerName, String channelName) {
		Channel c = new Channel(ownerName, channelName);
		synchronized(channels) {
			channels.add(c);
		}
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				channelList.getChildren().add(c.channelPane);
			}

		});
		c.channelPane.setOnMouseClicked(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
				if(selectedChannel != null) {
					selectedChannel.clearColor();
				}
				selectedChannel = c;
				c.highlightChannel();
			}

		});
	}

	void moveChannel(String channelName) {
		synchronized(channels) {
			for(Channel c : channels) {
				if(c.getName().equals(channelName)) {
					/*
					 * Joined Channel Styling
					 */
					c.highlightChannel();
					return;
				}
			}
		}
	}

	public void removeChannel(String channelName) {
		synchronized(channels) {
			for(Channel c : channels) {
				if(c.getName().equals(channelName)) {
					channels.remove(c);
					Platform.runLater(new Runnable() {

						@Override
						public void run() {
							channelList.getChildren().remove(c.channelPane);
						}

					});
					return;
				}
			}
		}
	}

}

class Channel {

	private Label name;
	Label owner;
	Label population;
	GridPane channelPane;

	public Channel(String ownerName, String channelName) {
		name = new Label(channelName);
		owner = new Label(ownerName);
		population = new Label(1 + "");
		channelPane  = new GridPane();
		ColumnConstraints cc = new ColumnConstraints();
		cc.setPercentWidth(33);
		channelPane.getColumnConstraints().addAll(cc, cc, cc);
		channelPane.add(name, 0, 0);
		channelPane.add(owner, 1, 0);
		channelPane.add(population, 2, 0);

	}

	public void clearColor() {
		channelPane.setStyle("-fx-background-color: transparent");
	}

	void highlightChannel() {
		channelPane.setStyle("-fx-background-color: green");
	}

	String getName() {
		return name.getText();
	}
}



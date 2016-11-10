package org.zapto.mike.mrstream;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.HashMap;

import org.zapto.mike.mrstreamserver.Packet;
import org.zapto.mike.mrstreamserver.Video;
import org.zapto.mike.mrstreamserver.VideoClient;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.web.WebErrorEvent;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebView;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

public class VideoSync extends Application {

	@FXML
    Button play;
	@FXML
    Button pause;
	@FXML
    Button seek;
	@FXML
    Button addVideo;
	@FXML
	TextField minuteField;
	@FXML
	TextField secondField;
	@FXML
	TextField videoURL;
	@FXML
	VBox clientListBox;
	@FXML
	Pane background;
	@FXML
	AnchorPane rootPane;
	@FXML
	Slider volumeSlider;

	private Stage videoStage;
	private Stage videoControls;
	private WebView webView;
	private Video currentVideo;
	private PacketHandler handler;
	private PrintStream serverOut;
	private HashMap<String, ClientEntry> clientList;
    private String channelHost;
    private GridPane infoRow;
    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    public void initialize() {

    }

	public VideoSync(PacketHandler handler, PrintStream serverOut) {
		clientList = new HashMap<String, ClientEntry>();
		this.serverOut = serverOut;
		this.handler = handler;
		this.start(new Stage());
	}

	@Override
	public void start(Stage main) {
		try {
			videoStage = new Stage();
			videoControls = new Stage();
			webView = new WebView();
			FXMLLoader fxml = new FXMLLoader(MrStream.class.getResource("fxml/VideoControls.fxml"));
			fxml.setController(this);
			videoControls.initStyle(StageStyle.TRANSPARENT);
			videoControls.setScene(new Scene(fxml.load(),Color.TRANSPARENT));
			videoStage.setScene(new Scene(webView, 660, 500));

			ColumnConstraints c0 = new ColumnConstraints();
			c0.setPercentWidth(25);
			infoRow = new GridPane();
			infoRow.add(new Label("Name"), 0, 0);
			infoRow.add(new Label("Is Host"), 1, 0);
			infoRow.add(new Label("Is Ready"), 2, 0);
			infoRow.add(new Label("Current Time"), 3, 0);
			infoRow.getColumnConstraints().addAll(c0, c0, c0, c0);
			clientListBox.getChildren().add(infoRow);

			makeDragable(rootPane);
			makeDragable(background);

			volumeSlider.valueProperty().addListener((obj, oldVal, newVal) -> {
				setVolume(newVal.intValue());
			});
			volumeSlider.setDisable(true);
			play.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent event) {
					try {
						handler.sendPacket(new Packet("video", "play"));
					} catch (IOException e) {
						serverOut.println("Couldnt request a play");
					}
				}

			});
			pause.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent event) {
					try {
						handler.sendPacket(new Packet("video", "pause"));
					} catch (IOException e) {
						serverOut.println("Couldnt request a pause");
					}
				}

			});
			addVideo.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent event) {
					try {
						handler.sendPacket(new Packet("video", "addRequest", videoURL.getText()));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

			});
			videoStage.setOnCloseRequest(new EventHandler<WindowEvent>() {

				@Override
				public void handle(WindowEvent event) {
					shutdown();
				}

			});
			videoControls.setOnCloseRequest(new EventHandler<WindowEvent>() {

				@Override
				public void handle(WindowEvent event) {
					shutdown();
				}

			});
			seek.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent event) {
					try {
							int minute = Integer.parseInt(minuteField.getText());
							int second = Integer.parseInt(secondField.getText());
							if(minute >= 0 && second >= 0) {
								handler.sendPacket(new Packet("video", "seekTo", new Double(((minute * 60) + second))));
							} else {
								throw new NumberFormatException();
							}
						} catch (IOException | NumberFormatException e) {
							serverOut.println("Invalid time format");
						}
				}

			});
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
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
            	videoControls.setX(event.getScreenX() - xOffset);
            	videoControls.setY(event.getScreenY() - yOffset);
            }
        });
	}

	public void shutdown() {
		pauseVideo();
		try {
			handler.sendPacket(new Packet("video", "shutdown"));
		} catch (IOException e) {
		}
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				clientListBox.getChildren().clear();
				clientListBox.getChildren().add(infoRow);
			}

		});
		clientList.clear();
		videoStage.close();
		videoControls.close();
	}

	void show() {
		try {
			if(!videoStage.isShowing() || !videoControls.isShowing()) {
				handler.sendPacket(new Packet("video", "wantsVideoUpdates"));
			}
			videoStage.show();
			videoControls.show();
		} catch(IOException e) {
			serverOut.println("Couldnt connect to the video sync server");
		}
	}

	void setVolume(int volume) {
		executeScript("setVolume(" + volume + ")");
	}

	void pauseVideo() {
		executeScript("pauseVid()");
	}

	void playVideo() {
		executeScript("playVid()");
	}

	void getTime() {
		executeScript("getTime()");
	}

	void seekTo(double time) {
		executeScript("seekTo(" + time + ")");
	}

	void removeClientFromList(VideoClient v, String host) {
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				clientListBox.getChildren().remove(clientList.get(v.getName()).getGridPane());
				clientList.remove(v.getName());
			}

		});
		if(v.getName().equals(channelHost)) {
			for(ClientEntry c : clientList.values()) {
				if(c.getName().equals(host)) {
					c.setHost(true);
					break;
				}
			}
		}
	}

	public void addClientToList(VideoClient v, String host) {
		ClientEntry ce = new ClientEntry(v, host);
		clientList.put(v.getName(), ce);
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				clientListBox.getChildren().add(ce.getGridPane());
			}

		});
	}

	void buildInitalClientList(Object[] clients, String host) {
		try {
			for(Object o : clients) {
				VideoClient v = (VideoClient)o;
				if(clientList.get(v.getName()) == null) {
					ClientEntry ce = new ClientEntry(v, host);
					clientList.put(v.getName(), ce);
					Platform.runLater(new Runnable() {

						@Override
						public void run() {
							clientListBox.getChildren().add(ce.getGridPane());
						}

					});
				}
			}
			channelHost = host;
		} catch(ClassCastException e) {
			e.printStackTrace();
		}
	}

	void updateCanPlay(String name, boolean canPlay) {
		clientList.get(name).setCanPlay(canPlay);
	}

	void updateClientTimes(Object[] times) {
		synchronized(clientList) {
			String name;
			for(int i = 0; i < times.length; i+=2) {
				name = (String)times[i];
				if(clientList.get(name) != null) {
					clientList.get(name).setTime((Double)times[i+1]);
				} else {
					serverOut.println("Couldnt find client " + name + " for time update");
				}
			}
		}
	}

	void setButtonsDisabled(boolean set) {
		play.setDisable(set);
		pause.setDisable(set);
		seek.setDisable(set);
	}

	private void createHtmlDoc(String url) {
		try {
			PrintWriter p = new PrintWriter("video.html");
			p.write("<html style='background : rgba(0,0,0,1);object-fit: fill;'><body style='background : rgba(0,0,0,0);object-fit: fill;'><video style=\"background: rgba(0,0,0,0);object-fit: fill;\" width=\"100%\" height=\"95%\" id=\"video\"><source src=\""
					+ url + "\" type=\"video/mp4\"></video></body></html><script type=\"text/javascript\" src=\"HTMLPlayer.js\"></script>");
			p.flush();
			p.close();
//            webView.getEngine().documentProperty().addListener((obj) -> {
//            	try {
//
//                    // Use reflection to retrieve the WebEngine's private 'page' field.
//                    Field f = webView.getEngine().getClass().getDeclaredField("page");
//                    f.setAccessible(true);
//                    com.sun.webkit.WebPage page = (com.sun.webkit.WebPage) f.get(webView.getEngine());
//                    page.setBackgroundColor((new java.awt.Color(0, 0, 0, 0)).getRGB());
//
//                } catch (Exception e) {
//                }
//            });
			webView.getEngine().setOnAlert(new EventHandler<WebEvent<String>>() {

				@Override
				public void handle(WebEvent<String> event) {
					String[] eventData = event.getData().split(":");
					try {
						switch(eventData[0]) {
						case "time":
							handler.sendPacket(new Packet("video", "timeUpdate", Double.parseDouble(eventData[1])));
							break;
						case "buffering":
							handler.sendPacket(new Packet("video", "buffering"));
							break;
						case "canPlayThrough":
							handler.sendPacket(new Packet("video", "canPlayThrough"));
							break;
					}
					} catch(IOException e) {

					}
				}

			});

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	private void executeScript(String script) {
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				webView.getEngine().executeScript(script);
			}

		});
	}

	void loadVideo(Video v) {
		try {
			handler.sendPacket(new Packet("video", "cantPlayThrough"));
		} catch (IOException e) {		}
		this.currentVideo = v;
		if(currentVideo != null) {
			loadURL(currentVideo.getUrl());
		} else {
			serverOut.println("Error loading video");
		}
	}
	double height;
	private void loadURL(String url) {
		if(url.contains("www.youtube.com")) {
			webView.getEngine().setOnError(new EventHandler<WebErrorEvent>() {

				@Override
				public void handle(WebErrorEvent e) {
					serverOut.println(e.getMessage());
				}

			});
			File file = new File("YouTubePlayer.html");
			Platform.runLater(new Runnable() {

				@Override
				public void run() {
					webView.getEngine().load("file:///"+ file.getAbsolutePath());
				}

			});
			webView.setDisable(true);
			videoStage.maximizedProperty().addListener((obj, oldVal, newVal) -> {
				if(newVal) {
					ObservableList<Screen> s = Screen.getScreensForRectangle(videoStage.getX(), videoStage.getY(), videoStage.getWidth(), videoStage.getHeight());
					executeScript("setSize(" + s.get(0).getVisualBounds().getWidth() + "," + s.get(0).getVisualBounds().getHeight() + ")");
				} else {
					executeScript("setSize(" + 640 + "," + 480 + ")");
				}
			});
			webView.getEngine().setOnAlert(new EventHandler<WebEvent<String>>() {

				@Override
				public void handle(WebEvent<String> event) {
					String[] eventData = event.getData().split(":");
					try {
						switch(eventData[0]) {
						case "time":
							handler.sendPacket(new Packet("video", "timeUpdate", Double.parseDouble(eventData[1])));
							break;
						case "buffering":
							handler.sendPacket(new Packet("video", "buffering"));
							break;
						case "canPlayThrough":
							handler.sendPacket(new Packet("video", "canPlayThrough"));
							break;
						case "APIReady":
							executeScript("changeVid(\"" + url.substring(32)  + "\")");
							volumeSlider.setDisable(false);
							break;
					}
					} catch(IOException e) {

					}
				}

			});
		} else {
			createHtmlDoc(url);
			volumeSlider.setDisable(false);
			webView.getEngine().setOnError(new EventHandler<WebErrorEvent>() {

				@Override
				public void handle(WebErrorEvent e) {
					serverOut.println(e.getMessage());
				}

			});
			File file = new File("video.html");
			Platform.runLater(new Runnable() {

				@Override
				public void run() {
					webView.getEngine().load("file:///"+ file.getAbsolutePath());
				}

			});
		}
	}
}

class ClientEntry {

	private GridPane pane;
	private Label name;
	private Rectangle canPlayBox;
	private Rectangle isHostBox;
	private boolean canPlay;
	private Label time;

	public ClientEntry(VideoClient vc, String host) {
		ColumnConstraints c0 = new ColumnConstraints();
		c0.setPercentWidth(40);
		ColumnConstraints c1 = new ColumnConstraints();
		c1.setPercentWidth(10);
		name = new Label(vc.getName());
		isHostBox = new Rectangle(20, 20);
		canPlayBox = new Rectangle(20, 20);
		canPlay = vc.canPlay();
		if(canPlay) {
			canPlayBox.setFill(Color.GREEN);
		} else {
			canPlayBox.setFill(Color.RED);
		}
		if(vc.getName().equals(host)) {
			isHostBox.setFill(Color.GREEN);
		} else {
			isHostBox.setFill(Color.RED);
		}
		/*
		 * DO TIME FORMATING ------------------------------------------------------------------------------------------
		 */
		time = new Label();
		pane = new GridPane();
		pane.getColumnConstraints().addAll(c0, c1, c1, c0);
		pane.add(name, 0, 0);
		pane.add(isHostBox, 1, 0);
		pane.add(canPlayBox, 2, 0);
		pane.add(time, 3, 0);
	}

	public void setHost(boolean isHost) {
		if(isHost) {
			isHostBox.setFill(Color.GREEN);
		} else {
			isHostBox.setFill(Color.RED);
		}
	}

	public void setTime(Double time2) {
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				time.setText("Time: " + Math.round(time2));
			}

		});
	}

	void setCanPlay(boolean b) {
		canPlay = b;
		if(canPlay) {
			canPlayBox.setFill(Color.GREEN);
		} else {
			canPlayBox.setFill(Color.RED);
		}
	}

	GridPane getGridPane() {
		return pane;
	}

	boolean canPlay() {
		return canPlay;
	}

	String getName() {
		return name.getText();
	}

	String getTime() {
		return time.getText();
	}
}
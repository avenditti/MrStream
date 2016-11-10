package org.zapto.mike.mrstream;

import java.io.File;
import java.util.ArrayList;

import javafx.event.EventHandler;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebView;

public class YouTubePlayer implements Runnable{


	private ArrayList<String> alerts;

	public YouTubePlayer(WebView view) {
		alerts = new ArrayList<String>();
		view = new WebView();
        view.getEngine().setOnAlert(new EventHandler<WebEvent<String>>() {
            @Override
            public void handle(WebEvent<String> e) {
                switch(e.getData()) {
                	case "pause" :

                }
            }
        });
		File f = new File("YouTubePlayer.html");
		view.getEngine().load("file:///" + f.getAbsolutePath());

	}

	@Override
	public void run() {

	}

	protected void removeAlert(String data) {
		alerts.remove(0);
	}

	protected void addAlert(String data) {
		alerts.add(data);
	}

}

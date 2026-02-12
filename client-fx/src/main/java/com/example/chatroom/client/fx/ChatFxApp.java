package com.example.chatroom.client.fx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ChatFxApp extends Application {
    private FxClient client;
    private MetricsPoller metricsPoller;

    @Override
    public void start(Stage stage) {
        TextField hostField = new TextField("127.0.0.1");
        hostField.setPrefWidth(120);
        TextField portField = new TextField("9000");
        portField.setPrefWidth(80);
        TextField userField = new TextField("student");
        userField.setPrefWidth(120);
        TextField metricsPortField = new TextField("9001");
        metricsPortField.setPrefWidth(80);

        Button connectBtn = new Button("Connect");
        Button disconnectBtn = new Button("Disconnect");
        disconnectBtn.setDisable(true);

        Label statusLabel = new Label("Disconnected");
        Label onlineLabel = new Label("Online: -");

        ListView<String> messageList = new ListView<>();
        TextField inputField = new TextField();
        Button sendBtn = new Button("Send");
        sendBtn.setDisable(true);

        HBox loginBox = new HBox(8,
                new Label("Host"), hostField,
                new Label("Port"), portField,
                new Label("User"), userField,
                new Label("Metrics"), metricsPortField,
                connectBtn,
                disconnectBtn);
        loginBox.setAlignment(Pos.CENTER_LEFT);

        HBox statusBox = new HBox(12, statusLabel, onlineLabel);
        statusBox.setAlignment(Pos.CENTER_LEFT);

        HBox inputBox = new HBox(8, inputField, sendBtn);
        inputBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(inputField, Priority.ALWAYS);

        VBox centerBox = new VBox(8, messageList, inputBox, statusBox);
        centerBox.setPadding(new Insets(8));
        VBox.setVgrow(messageList, Priority.ALWAYS);

        BorderPane root = new BorderPane();
        root.setTop(loginBox);
        root.setCenter(centerBox);
        BorderPane.setMargin(loginBox, new Insets(8));

        connectBtn.setOnAction(e -> {
            String host = hostField.getText().trim();
            int port = Integer.parseInt(portField.getText().trim());
            String userId = userField.getText().trim();
            int metricsPort = Integer.parseInt(metricsPortField.getText().trim());

            client = new FxClient(msg -> Platform.runLater(() -> messageList.getItems().add(msg)),
                    status -> Platform.runLater(() -> statusLabel.setText(status)));
            client.connect(host, port, success -> {
                if (success) {
                    client.login(userId);
                    client.startHeartbeat(5);
                    startMetricsPoller(host, metricsPort, onlineLabel);
                    Platform.runLater(() -> {
                        connectBtn.setDisable(true);
                        disconnectBtn.setDisable(false);
                        sendBtn.setDisable(false);
                        statusLabel.setText("Connected");
                    });
                }
            });
        });

        disconnectBtn.setOnAction(e -> {
            if (client != null) {
                client.sendBye();
                client.close();
                client = null;
            }
            stopMetricsPoller();
            connectBtn.setDisable(false);
            disconnectBtn.setDisable(true);
            sendBtn.setDisable(true);
            statusLabel.setText("Disconnected");
            onlineLabel.setText("Online: -");
        });

        sendBtn.setOnAction(e -> {
            if (client != null) {
                String text = inputField.getText().trim();
                if (!text.isEmpty()) {
                    client.sendChat(text);
                    inputField.clear();
                }
            }
        });

        Scene scene = new Scene(root, 900, 520);
        stage.setTitle("Chatroom FX Client");
        stage.setScene(scene);
        stage.show();
    }

    private void startMetricsPoller(String host, int port, Label onlineLabel) {
        stopMetricsPoller();
        metricsPoller = new MetricsPoller(host, port, online ->
                Platform.runLater(() -> onlineLabel.setText("Online: " + online)));
        metricsPoller.start();
    }

    private void stopMetricsPoller() {
        if (metricsPoller != null) {
            metricsPoller.stop();
            metricsPoller = null;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

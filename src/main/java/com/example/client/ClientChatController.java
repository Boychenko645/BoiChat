package com.example.client;

import com.example.Message;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ClientChatController implements Initializable {

    @FXML
    private ListView<String> userListView;
    @FXML
    private Label chatWithLabel;
    @FXML
    private TextArea chatArea;
    @FXML
    private TextField messageField;
    @FXML
    private Button sendButton;
    @FXML
    private Button backButton;
    @FXML
    private VBox chatBox;
    @FXML
    private ScrollPane chatScrollPane;
    @FXML
    private TextField searchField;

    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private String username;

    private ObservableList<String> allUsers = FXCollections.observableArrayList();

    public void init(String username, Socket socket) {
        this.username = username;
        this.socket = socket;

        try {
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            writer.println(username);
            chatBox.getChildren().clear();

            new Thread(this::listen).start();
        } catch (IOException e) {
            showError("Не вдалося підключитись до сервера.");
            e.printStackTrace();
        }

        Platform.runLater(() -> {
            Stage stage = (Stage) chatBox.getScene().getWindow();
            stage.setOnCloseRequest(e -> {
                try {
                    writer.println("__exit__");
                    writer.flush();
                    socket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });
        });
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        sendButton.setOnAction(e -> sendMessage());
        backButton.setOnAction(e -> disconnectAndBack());

        userListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                chatWithLabel.setText("Чат із: " + newVal);
            }
        });

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String filter = newVal.toLowerCase();
            List<String> filtered = allUsers.stream()
                    .filter(u -> u.toLowerCase().contains(filter))
                    .collect(Collectors.toList());
            userListView.getItems().setAll(filtered);
        });
    }

    private void listen() {
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("__users__:")) {
                    String[] users = line.substring(10).split(",");

                    Platform.runLater(() -> {
                        allUsers.setAll(users);
                        allUsers.remove(username);

                        String filter = searchField.getText().toLowerCase();
                        List<String> filtered = allUsers.stream()
                                .filter(u -> u.toLowerCase().contains(filter))
                                .collect(Collectors.toList());
                        userListView.getItems().setAll(filtered);
                    });
                } else {
                    Message msg = Message.fromXML(line);
                    if (msg != null && msg.sender != null && msg.text != null) {
                        addMessage(msg.sender.equals(username) ? "Я" : msg.sender, msg.text,
                                msg.sender.equals(username));
                    }
                }
            }
        } catch (IOException e) {
            if (!socket.isClosed()) {
                showError("Підключення втрачено.");
            }
        }
    }

    private void sendMessage() {
        String text = messageField.getText();
        String receiver = userListView.getSelectionModel().getSelectedItem();

        if (text.isEmpty() || receiver == null)
            return;

        Message message = new Message(username, receiver, text);
        writer.println(message.toXML());

        messageField.clear();
    }

    private void addMessage(String sender, String text, boolean isOwn) {
        Label msgLabel = new Label(sender + ": " + text);
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(300);
        msgLabel.setStyle("-fx-background-color: " + (isOwn ? "#DCF8C6" : "#FFFFFF")
                + "; -fx-padding: 5; -fx-border-radius: 5; -fx-background-radius: 5;");

        HBox hbox = new HBox(msgLabel);
        hbox.setMaxWidth(Double.MAX_VALUE);
        hbox.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        Platform.runLater(() -> {
            chatBox.getChildren().add(hbox);
            chatScrollPane.setVvalue(1.0);
        });
    }

    private void disconnectAndBack() {
        try {
            writer.println("__exit__");
            writer.flush();
            Thread.sleep(100);
            socket.close();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Login");
            stage.show();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void showError(String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText(msg);
            alert.showAndWait();
        });
    }
}
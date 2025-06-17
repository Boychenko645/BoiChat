package com.example.client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class ClientLoginController implements Initializable {

    @FXML
    private TextField FieldIPAdress;

    @FXML
    private TextField FieldUsername;

    @FXML
    private void btnClickAction(ActionEvent event) {
        String ip = FieldIPAdress.getText().trim();
        String username = FieldUsername.getText().trim();

        if (ip.isEmpty() || username.isEmpty()) {
            System.out.println("IP або ім’я порожнє");
            return;
        }

        try {
            System.out.println("Спроба зʼєднання з IP: " + ip);
            Socket socket = new Socket(ip, 5050);
            System.out.println("Зʼєднання успішне");
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(username);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/chat.fxml"));
            Parent root = loader.load();

            ClientChatController chatController = loader.getController();
            chatController.init(username, socket);

            Stage stage = (Stage) FieldIPAdress.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Chat - " + username);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Помилка: " + e.getMessage());
        }

    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
    }
}

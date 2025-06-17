module com.example {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires java.xml;

    opens com.example.client to javafx.fxml;
    opens com.example to javafx.fxml;

    exports com.example.client;
    exports com.example;
}
module com.example.cycleborrowingsystem {
    requires javafx.controls;
    requires javafx.fxml;
    requires jdk.httpserver;
    requires java.desktop;
    requires java.sql;

    exports com.example.cycleborrowingsystem to javafx.graphics;
    exports com.example.cycleborrowingsystem.models;
    exports com.example.cycleborrowingsystem.dao;

    opens com.example.cycleborrowingsystem to javafx.fxml, javafx.graphics;
    opens com.example.cycleborrowingsystem.controllers to javafx.fxml;
    opens com.example.cycleborrowingsystem.net to javafx.fxml;
    opens com.example.cycleborrowingsystem.models to javafx.fxml;
    opens com.example.cycleborrowingsystem.dao to javafx.fxml;
}

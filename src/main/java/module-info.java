module com.blueprinthell {
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.media;
    requires org.json;

    exports com.blueprinthell;
    exports com.controller;
    exports com.model;
    exports com.view;
    exports com.utils;

    opens com.blueprinthell to javafx.graphics;
    opens com.view to javafx.graphics, javafx.fxml;
}
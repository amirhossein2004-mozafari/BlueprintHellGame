module com.blueprinthell {

    requires javafx.controls;
    requires javafx.fxml;


    exports com.blueprinthell;

    opens com.controller to javafx.fxml;


    opens com.blueprinthell to javafx.fxml;


}

module com.blueprinthell {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;

    requires org.json;
    requires org.hibernate.orm.core;
    requires jakarta.persistence;

    requires java.naming;
    requires java.sql;
    requires java.persistence;

    opens com.blueprinthell.db.entities to org.hibernate.orm.core;

    opens com.blueprinthell to javafx.fxml;
    opens com.model to com.google.gson;


    exports com.blueprinthell;

    exports com.api.dto;
    exports com.api.enums;
    exports com.controller;
    exports com.model;
    exports com.utils;
    exports com.view;
}


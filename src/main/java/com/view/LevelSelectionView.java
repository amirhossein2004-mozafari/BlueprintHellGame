package com.view;

import com.blueprinthell.MainApplication;
import com.controller.MenuController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import com.blueprinthell.Config;

public class LevelSelectionView extends VBox {

    private final MainApplication mainApp;

    public LevelSelectionView(MainApplication mainApp, MenuController menuController) {
        this.mainApp = mainApp;

        this.setAlignment(Pos.CENTER);
        this.setSpacing(30);
        this.setPadding(new Insets(50));
        this.setStyle("-fx-background-color: #25252E;");

        Label titleLabel = new Label("انتخاب مرحله");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 30));
        titleLabel.setTextFill(Config.COLOR_HUD_TEXT);

        GridPane levelsGrid = new GridPane();
        levelsGrid.setAlignment(Pos.CENTER);
        levelsGrid.setHgap(20);
        levelsGrid.setVgap(20);


        Button level1Button = createLevelButton("مرحله ۱");
        level1Button.setOnAction(e -> mainApp.startGame(1));
        levelsGrid.add(level1Button, 0, 0);

        Button level2Button = createLevelButton("مرحله ۲");
        level2Button.setOnAction(e -> mainApp.startGame(2));
        levelsGrid.add(level2Button, 1, 0);


        Button backButton = new Button("بازگشت به منوی اصلی");
        backButton.setPrefWidth(250);
        backButton.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        backButton.setOnAction(e -> mainApp.showMainMenu(menuController));
        this.getChildren().addAll(titleLabel, levelsGrid, backButton);
    }

    private Button createLevelButton(String text) {
        Button button = new Button(text);
        button.setPrefSize(150, 80);
        button.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        return button;
    }
}
package com.view;

import com.blueprinthell.Config;
import com.controller.ShopController;
import com.model.GameState;
import com.model.ShopSkill;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

public class ShopView extends BorderPane {

    private final Label coinsAvailableLabel;

    public ShopView(ShopController shopController, GameState gameState) {

        this.setPrefSize(300, 280);
        this.setStyle("-fx-background-color: " + toRGBCode(Config.COLOR_SHOP_BACKGROUND) + ";" +
                "-fx-border-color: #aaaaaa; -fx-border-width: 1; -fx-background-radius: 8; -fx-border-radius: 8;");
        this.setPadding(new Insets(15));

        Label title = new Label("فروشگاه مهارت‌ها");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        title.setTextFill(Config.COLOR_SHOP_TEXT);
        BorderPane.setAlignment(title, Pos.CENTER);
        this.setTop(title);

        VBox skillsBox = new VBox(12);
        skillsBox.setAlignment(Pos.CENTER_LEFT);
        skillsBox.setPadding(new Insets(10, 0, 10, 0));

        skillsBox.getChildren().add(createSkillButton(
                gameState, ShopSkill.ATAR, "غیرفعال کردن Impact (10ث)",
                Config.ATAR_SKILL_COST,
                () -> shopController.buySkill(ShopSkill.ATAR, Config.ATAR_SKILL_DURATION_SECONDS, Config.ATAR_SKILL_COST)
        ));
        skillsBox.getChildren().add(createSkillButton(
                gameState, ShopSkill.AIRYAMAN, "غیرفعال کردن برخورد (5ث)",
                Config.AIRYAMAN_SKILL_COST,
                () -> shopController.buySkill(ShopSkill.AIRYAMAN, Config.AIRYAMAN_SKILL_DURATION_SECONDS, Config.AIRYAMAN_SKILL_COST)
        ));
        skillsBox.getChildren().add(createSkillButton(
                gameState, ShopSkill.ANAHITA, "پاک کردن نویز همه پکت‌ها (آنی)",
                Config.ANAHITA_SKILL_COST,
                () -> shopController.buySkill(ShopSkill.ANAHITA, 0, Config.ANAHITA_SKILL_COST)
        ));

        this.setCenter(skillsBox);

        HBox bottomBox = new HBox(15);
        bottomBox.setAlignment(Pos.CENTER_RIGHT);
        bottomBox.setPadding(new Insets(10, 0, 0, 0));

        coinsAvailableLabel = new Label();
        coinsAvailableLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        coinsAvailableLabel.setTextFill(Color.GOLD);
        coinsAvailableLabel.textProperty().bind(
                Bindings.format("سکه: %d", gameState.coinsProperty())
        );

        Button closeButton = new Button("بستن");
        closeButton.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        closeButton.setOnAction(e -> shopController.closeShop());

        bottomBox.getChildren().addAll(coinsAvailableLabel, closeButton);
        this.setBottom(bottomBox);
    }

    private HBox createSkillButton(GameState gameState, ShopSkill skill, String description, int cost, Runnable onAction) {
        HBox itemBox = new HBox(10);
        itemBox.setAlignment(Pos.CENTER_LEFT);

        Label descLabel = new Label(description);
        descLabel.setFont(Font.font("Arial", FontPosture.ITALIC, 11));
        descLabel.setTextFill(Config.COLOR_SHOP_TEXT);
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(150);

        Label costLabel = new Label("هزینه: " + cost);
        costLabel.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        costLabel.setTextFill(Color.LIGHTGRAY);

        VBox textContainer = new VBox(2, descLabel, costLabel);

        Button buyButton = new Button("خرید");
        buyButton.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        buyButton.setOnAction(e -> onAction.run());
        buyButton.disableProperty().bind(
                gameState.coinsProperty().lessThan(cost)
        );

        itemBox.getChildren().addAll(textContainer, buyButton);
        HBox.setHgrow(textContainer, Priority.ALWAYS);
        return itemBox;
    }

    private String toRGBCode(Color color) {
        if (color == null) return "#000000FF";
        return String.format("#%02X%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255),
                (int) (color.getOpacity() * 255));
    }

    public void updateAvailableCoins(int coins) {
        if (coinsAvailableLabel != null) {
            coinsAvailableLabel.setText("سکه: " + coins);
        }
    }
}
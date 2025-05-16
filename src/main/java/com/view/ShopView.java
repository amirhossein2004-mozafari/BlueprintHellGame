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
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

public class ShopView extends BorderPane {

    private final Label coinsAvailableLabel;
    private static final double SHOP_PREF_WIDTH = 320;
    private static final double SHOP_PREF_HEIGHT = 300;

    public ShopView(ShopController shopController, GameState gameState) {
        if (shopController == null || gameState == null) {
            throw new IllegalArgumentException("ShopController and GameState cannot be null");
        }

        this.setPrefSize(SHOP_PREF_WIDTH, SHOP_PREF_HEIGHT);
        this.setMinWidth(SHOP_PREF_WIDTH / 1.5);
        this.setMinHeight(SHOP_PREF_HEIGHT / 1.5);
        this.setStyle("-fx-background-color: " + toRGBCode(Config.COLOR_SHOP_BACKGROUND) + ";" +
                "-fx-border-color: #999999; -fx-border-width: 1.5; -fx-background-radius: 10; -fx-border-radius: 10;");
        this.setPadding(new Insets(15));

        Label title = new Label("فروشگاه مهارت‌ها");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        title.setTextFill(Config.COLOR_SHOP_TEXT);
        title.setAlignment(Pos.CENTER);
        BorderPane.setAlignment(title, Pos.CENTER);
        this.setTop(title);

        VBox skillsBox = new VBox(10);
        skillsBox.setAlignment(Pos.TOP_LEFT);
        skillsBox.setPadding(new Insets(10, 0, 10, 0));

        skillsBox.getChildren().add(createSkillButton(
                gameState, "غیرفعال کردن Impact موج ضربه (10ث)", Config.ATAR_SKILL_COST,
                () -> shopController.buySkill(ShopSkill.ATAR, Config.ATAR_SKILL_DURATION_SECONDS, Config.ATAR_SKILL_COST)
        ));
        skillsBox.getChildren().add(createSkillButton(
                gameState, "غیرفعال کردن برخورد پکت (5ث)", Config.AIRYAMAN_SKILL_COST,
                () -> shopController.buySkill(ShopSkill.AIRYAMAN, Config.AIRYAMAN_SKILL_DURATION_SECONDS, Config.AIRYAMAN_SKILL_COST)
        ));
        skillsBox.getChildren().add(createSkillButton(
                gameState, "پاک کردن نویز همه پکت‌ها (آنی)", Config.ANAHITA_SKILL_COST,
                () -> shopController.buySkill(ShopSkill.ANAHITA, 0, Config.ANAHITA_SKILL_COST)
        ));

        this.setCenter(skillsBox);

        HBox bottomControls = new HBox(10);
        bottomControls.setAlignment(Pos.CENTER_RIGHT);
        bottomControls.setPadding(new Insets(15, 0, 0, 0));

        coinsAvailableLabel = new Label();
        coinsAvailableLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        coinsAvailableLabel.setTextFill(Color.GOLD);
        coinsAvailableLabel.textProperty().bind(
                Bindings.format("سکه: %d", gameState.coinsProperty())
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeButton = new Button("بستن (ESC)");
        closeButton.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        closeButton.setOnAction(e -> shopController.closeShop());

        bottomControls.getChildren().addAll(coinsAvailableLabel, spacer, closeButton);
        this.setBottom(bottomControls);
    }

    private HBox createSkillButton(GameState gameState, String description, int cost, Runnable onAction) {
        HBox itemRow = new HBox(10);
        itemRow.setAlignment(Pos.CENTER_LEFT);
        itemRow.setPadding(new Insets(3, 0, 3, 0));

        Label descLabel = new Label(description);
        descLabel.setFont(Font.font("Arial", FontPosture.REGULAR, 12));
        descLabel.setTextFill(Config.COLOR_SHOP_TEXT);
        descLabel.setWrapText(true);
        descLabel.setPrefWidth(180);
        descLabel.setMaxWidth(180);

        Label costLabel = new Label("هزینه: " + cost);
        costLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 11));
        costLabel.setTextFill(Color.LIGHTCYAN);

        VBox textInfoContainer = new VBox(2, descLabel, costLabel);
        textInfoContainer.setAlignment(Pos.CENTER_LEFT);

        Button buyButton = new Button("خرید");
        buyButton.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        buyButton.setMinWidth(50);
        buyButton.setPrefWidth(60);
        buyButton.setOnAction(e -> onAction.run());
        buyButton.disableProperty().bind(
                gameState.coinsProperty().lessThan(cost)
        );

        itemRow.getChildren().addAll(textInfoContainer, buyButton);
        HBox.setHgrow(textInfoContainer, Priority.ALWAYS);
        return itemRow;
    }

    private String toRGBCode(Color color) {
        if (color == null) return "#20202AE5";
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
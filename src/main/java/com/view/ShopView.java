package com.view;

import com.blueprinthell.Config;
import com.controller.ShopController;
import com.model.EffectType;
import com.model.GameState;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.paint.Color;
import javafx.beans.binding.Bindings;
import com.api.enums.AbilityType;

public class ShopView extends BorderPane {

    private final ShopController shopController;
    private final GameState gameState;

    public ShopView(ShopController shopController, GameState gameState) {
        if (shopController == null || gameState == null) {
            throw new IllegalArgumentException("ShopController and GameState cannot be null for ShopView");
        }
        this.shopController = shopController;
        this.gameState = gameState;

        this.setPrefWidth(Config.SHOP_VIEW_DEFAULT_WIDTH);
        this.setMinWidth(Config.SHOP_VIEW_DEFAULT_WIDTH);
        this.setMaxWidth(Config.SHOP_VIEW_DEFAULT_WIDTH);

        this.setPrefHeight(Config.SHOP_VIEW_DEFAULT_HEIGHT);
        this.setMinHeight(Config.SHOP_VIEW_DEFAULT_HEIGHT * 0.8);


        this.setStyle("-fx-background-color: " + formatColorToCss(Config.COLOR_SHOP_BACKGROUND) + ";" +
                "-fx-border-color: " + formatColorToCss(Config.COLOR_SHOP_BUTTON_HOVER) + ";" +
                "-fx-border-width: 2; -fx-background-radius: 5; -fx-border-radius: 5;");
        this.setPadding(new Insets(15));

        VBox mainLayout = new VBox(15);
        mainLayout.setAlignment(Pos.TOP_CENTER);

        Label shopTitle = new Label("فروشگاه");
        shopTitle.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        shopTitle.setTextFill(Config.COLOR_SHOP_TEXT);

        Label currentCoinsLabel = new Label();
        currentCoinsLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        currentCoinsLabel.setTextFill(Color.GOLD);
        currentCoinsLabel.textProperty().bind(
                Bindings.format("سکه های شما: %d", gameState.coinsProperty())
        );

        VBox skillsContainer = new VBox(10);
        skillsContainer.setAlignment(Pos.CENTER_LEFT);
        skillsContainer.setPadding(new Insets(5,0,15,0));


        skillsContainer.getChildren().add(createSkillEntryView(
                "(Atar)",
                "اثر موج‌های Impact را برای " + (int)Config.ATAR_SKILL_DURATION_SECONDS + " ثانیه غیرفعال می‌کند.",
                Config.ATAR_SKILL_COST,
                AbilityType.ATAR,
                Config.ATAR_SKILL_DURATION_SECONDS,
                gameState.atarSkillActiveProperty()
        ));

        skillsContainer.getChildren().add(createSkillEntryView(
                "(Airyaman)",
                "برخورد پاکت‌های موجود در شبکه را برای " + (int)Config.AIRYAMAN_SKILL_DURATION_SECONDS + " ثانیه غیرفعال می‌کند.",
                Config.AIRYAMAN_SKILL_COST,
                AbilityType.AIRYAMAN,
                Config.AIRYAMAN_SKILL_DURATION_SECONDS,
                gameState.airyamanSkillActiveProperty()
        ));

        skillsContainer.getChildren().add(createSkillEntryView(
                "(Anahita)",
                "نویز تمام پاکت‌های حاضر در شبکه را صفر می‌کند.",
                Config.ANAHITA_SKILL_COST,
                AbilityType.ANAHITA,
                0,
                null
        ));

        skillsContainer.getChildren().add(createSkillEntryView(
                "Scroll of Aergia",
                "با انتخاب نقطه‌ای روی سیم، شتاب پکت‌های عبوری را برای ۲۰ ثانیه صفر می‌کند.",
                10,
                () -> shopController.enterPlacementMode(EffectType.AERGIA_NO_ACCELERATION, 10, 20.0)
        ));

        skillsContainer.getChildren().add(createSkillEntryView(
                "Scroll of Sisyphus",
                "به شما اجازه می‌دهد یک سیستم میانی (غیر از Source و Sink) را انتخاب و جابجا کنید.",
                15,
                () -> shopController.enterPlacementMode(EffectType.SISYPHUS_MOVE_SYSTEM, 15, 0)
        ));

        skillsContainer.getChildren().add(createSkillEntryView(
                "Scroll of Eliphas",
                "با انتخاب نقطه‌ای روی سیم، برای ۳۰ ثانیه تمام پکت‌های عبوری را به مرکز سیم بازمی‌گرداند.",
                20,
                () -> shopController.enterPlacementMode(EffectType.ELIPHAS_CENTERING_FORCE, 20, 30.0)
        ));

        Button closeButton = new Button("بستن فروشگاه (ESC)");
        styleButton(closeButton);
        closeButton.setOnAction(e -> this.shopController.closeShop());


        mainLayout.getChildren().addAll(shopTitle, currentCoinsLabel, skillsContainer, closeButton);
        this.setCenter(mainLayout);
        System.out.println("ShopView: UI fully constructed with all skill entries.");
    }

    private VBox createSkillEntryView(String name, String description, int cost, AbilityType ability, double duration, BooleanProperty activeProperty) {
        VBox skillBox = new VBox(5);
        skillBox.setPadding(new Insets(10));
        skillBox.setAlignment(Pos.CENTER_LEFT);
        skillBox.setStyle("-fx-background-color: " + formatColorToCss(Config.COLOR_SHOP_BUTTON.deriveColor(0,1,1,0.3)) +";" +
                "-fx-border-color: " + formatColorToCss(Config.COLOR_SHOP_BUTTON) + "; -fx-border-width: 1; -fx-border-radius: 4;");

        Label nameLabel = new Label(name);
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        nameLabel.setTextFill(Config.COLOR_SHOP_TEXT);

        Label costLabel = new Label("قیمت: " + cost + " سکه");
        costLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 13));
        costLabel.setTextFill(Config.COLOR_SHOP_TEXT);

        Label descLabel = new Label(description);
        descLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        descLabel.setTextFill(Config.COLOR_SHOP_TEXT);
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(Config.SHOP_VIEW_DEFAULT_WIDTH - 60);

        Button buyButton = new Button("خرید");
        styleButton(buyButton);
        buyButton.setOnAction(e -> {
            System.out.println("ShopView: Buy button clicked for " + ability.name());
            this.shopController.buySkill(ability, duration, cost);
        });

        buyButton.disableProperty().bind(
                gameState.coinsProperty().lessThan(cost)
                        .or( (activeProperty != null) ? activeProperty : new SimpleBooleanProperty(false) )
        );

        skillBox.getChildren().addAll(nameLabel, costLabel, descLabel, buyButton);
        if (activeProperty != null) {
            Label activeStatusLabel = new Label();
            activeStatusLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 11));
            activeStatusLabel.textProperty().bind(Bindings.when(activeProperty)
                    .then("وضعیت: فعال")
                    .otherwise("وضعیت: غیرفعال"));
            activeStatusLabel.textFillProperty().bind(Bindings.when(activeProperty)
                    .then(Color.LIMEGREEN.darker())
                    .otherwise(Color.ORANGERED.darker()));
            skillBox.getChildren().add(activeStatusLabel);
        }

        return skillBox;
    }

    private VBox createSkillEntryView(String name, String description, int cost, Runnable buyAction) {
        VBox skillBox = new VBox(5);
        skillBox.setPadding(new Insets(10));
        skillBox.setAlignment(Pos.CENTER_LEFT);
        skillBox.setStyle("-fx-background-color: " + formatColorToCss(Config.COLOR_SHOP_BUTTON.deriveColor(0,1,1,0.3)) +";" +
                "-fx-border-color: " + formatColorToCss(Config.COLOR_SHOP_BUTTON) + "; -fx-border-width: 1; -fx-border-radius: 4;");

        Label nameLabel = new Label(name);
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        nameLabel.setTextFill(Config.COLOR_SHOP_TEXT);

        Label costLabel = new Label("قیمت: " + cost + " سکه");
        costLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 13));
        costLabel.setTextFill(Config.COLOR_SHOP_TEXT);

        Label descLabel = new Label(description);
        descLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        descLabel.setTextFill(Config.COLOR_SHOP_TEXT);
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(Config.SHOP_VIEW_DEFAULT_WIDTH - 60);

        Button buyButton = new Button("خرید");
        styleButton(buyButton);

        buyButton.setOnAction(e -> {
            System.out.println("ShopView: Buy action button clicked for " + name);
            buyAction.run();
        });


        buyButton.disableProperty().bind(
                gameState.coinsProperty().lessThan(cost)
        );

        skillBox.getChildren().addAll(nameLabel, costLabel, descLabel, buyButton);
        return skillBox;
    }

    private void styleButton(Button button) {
        button.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        String baseStyle = "-fx-background-color: " + formatColorToCss(Config.COLOR_SHOP_BUTTON) + "; -fx-text-fill: " + formatColorToCss(Config.COLOR_SHOP_TEXT) + "; -fx-background-radius: 3;";
        String hoverStyle = "-fx-background-color: " + formatColorToCss(Config.COLOR_SHOP_BUTTON_HOVER) + "; -fx-text-fill: " + formatColorToCss(Config.COLOR_SHOP_TEXT) + "; -fx-background-radius: 3;";
        button.setStyle(baseStyle);
        button.setOnMouseEntered(e -> button.setStyle(hoverStyle));
        button.setOnMouseExited(e -> button.setStyle(baseStyle));
    }

    private String formatColorToCss(Color color) {
        if (color == null) return "transparent";
        if (color.getOpacity() < 1.0) {
            return String.format("rgba(%d, %d, %d, %.2f)",
                    (int) (color.getRed() * 255),
                    (int) (color.getGreen() * 255),
                    (int) (color.getBlue() * 255),
                    color.getOpacity());
        } else {
            return String.format("#%02X%02X%02X",
                    (int) (color.getRed() * 255),
                    (int) (color.getGreen() * 255),
                    (int) (color.getBlue() * 255));
        }
    }
}
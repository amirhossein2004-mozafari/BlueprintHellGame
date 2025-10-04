package com.view;

import com.api.dto.LeaderboardEntryDTO;
import com.blueprinthell.MainApplication;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.util.List;

public class LeaderboardView extends VBox {

    public LeaderboardView(MainApplication mainApp, List<LeaderboardEntryDTO> leaderboardData) {
        this.setAlignment(Pos.TOP_CENTER);
        this.setSpacing(15);
        this.setPadding(new Insets(30));
        this.setStyle("-fx-background-color: #2E2E3A;");

        Label title = new Label("لیدربرد ۱۰ نتیجه برتر");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        title.setTextFill(Color.WHITE);

        VBox resultsBox = new VBox(8);
        resultsBox.setAlignment(Pos.CENTER);
        resultsBox.setPadding(new Insets(20));
        resultsBox.setStyle("-fx-background-color: #20202D; -fx-background-radius: 8;");

        if (leaderboardData.isEmpty()) {
            resultsBox.getChildren().add(new Label("هنوز نتیجه‌ای ثبت نشده است!"));
        } else {
            for (int i = 0; i < leaderboardData.size(); i++) {
                LeaderboardEntryDTO entry = leaderboardData.get(i);
                String text = String.format("%d. بازیکن: %s - سکه: %d - از دست رفته: %.1f%%",
                        i + 1, entry.getUsername(), entry.getScore(), entry.getPacketLoss());
                Label entryLabel = new Label(text);
                entryLabel.setFont(Font.font(16));
                entryLabel.setTextFill(Color.LIGHTGRAY);
                resultsBox.getChildren().add(entryLabel);
            }
        }

        Button backButton = new Button("بازگشت به منوی اصلی");
        backButton.setOnAction(e -> mainApp.showMainMenu(new com.controller.MenuController(mainApp)));

        this.getChildren().addAll(title, resultsBox, backButton);
    }
}
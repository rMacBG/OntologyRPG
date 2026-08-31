package org.uni.GUI;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class LevelCompleteOverlay extends StackPane {

    public LevelCompleteOverlay() {
        setPrefSize(850, 650);
        setStyle("-fx-background-color: rgba(0, 0, 0, 0.75);");
        setVisible(false);
    }

    public void show(int currentDungeonLevel, Runnable onNextLevelAction) {
        Platform.runLater(() -> {
            getChildren().clear();

            VBox victoryBox = new VBox(15);
            victoryBox.setAlignment(Pos.CENTER);
            victoryBox.setPadding(new Insets(25));
            victoryBox.setMaxSize(400, 220);
            victoryBox.setStyle("-fx-background-color: #1a1a2e; -fx-border-color: #f1c40f; -fx-border-width: 3px; -fx-background-radius: 10; -fx-border-radius: 10;");

            Label title = new Label("🏆 ЕТАЖЪТ Е ИЗЧИСТЕН!");
            title.setStyle("-fx-text-fill: #f1c40f; -fx-font-size: 20px; -fx-font-weight: bold;");

            Label desc = new Label("Успешно премина през Етаж " + currentDungeonLevel + "!");
            desc.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

            Button nextLevelBtn = new Button("КЪМ ЕТАЖ " + (currentDungeonLevel + 1) + " 🚪");
            nextLevelBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 16;");

            nextLevelBtn.setOnAction(e -> {
                setVisible(false);
                if (onNextLevelAction != null) onNextLevelAction.run();
            });

            victoryBox.getChildren().addAll(title, desc, nextLevelBtn);
            getChildren().add(victoryBox);

            setVisible(true);
            toFront();
            nextLevelBtn.requestFocus();
        });
    }
}

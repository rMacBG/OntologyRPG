package org.uni.GUI;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class MainMenuScreen extends VBox {

    public MainMenuScreen(Runnable onStartGame, Runnable onExitGame) {
        setAlignment(Pos.CENTER);
        setSpacing(20);

        Label title = new Label("Chronicles of Jaba: The Reckoning of the Onterolog");
        title.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 32px; -fx-font-weight: bold;");

        Button startBtn = new Button("START GAME");
        startBtn.setPrefSize(200, 45);
        startBtn.setOnAction(e -> {
            if (onStartGame != null) onStartGame.run();
        });

        Button exitBtn = new Button("EXIT GAME");
        exitBtn.setPrefSize(200, 45);
        exitBtn.setOnAction(e -> {
            if (onExitGame != null) onExitGame.run();
            else Platform.exit();
        });

        getChildren().addAll(title, startBtn, exitBtn);
    }
}
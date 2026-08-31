package org.uni.GUI;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class LootDialogOverlay extends StackPane {

    public LootDialogOverlay() {
        setStyle("-fx-background-color: rgba(0, 0, 0, 0.75);");
        setVisible(false);
    }

    public void show(String lootItem, Runnable onEquipAction, Runnable onCancelAction) {
        Platform.runLater(() -> {
            getChildren().clear();

            VBox lootBox = new VBox(15);
            lootBox.setAlignment(Pos.CENTER);
            lootBox.setPadding(new Insets(25));
            lootBox.setMaxSize(420, 220);
            lootBox.setStyle("-fx-background-color: #1a1a2e; -fx-border-color: #e67e22; -fx-border-width: 3px; -fx-background-radius: 10; -fx-border-radius: 10;");

            Label title = new Label("🎁 НАМЕРЕН ПРЕДМЕТ / СКИЛ!");
            title.setStyle("-fx-text-fill: #e67e22; -fx-font-size: 18px; -fx-font-weight: bold;");

            Label desc = new Label("Чудовището пусна: " + lootItem + "\nИскаш ли да го екипираш веднага?");
            desc.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-text-alignment: center;");

            HBox buttons = new HBox(15);
            buttons.setAlignment(Pos.CENTER);

            Button equipBtn = new Button("ЕКИПИРАЙ");
            equipBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 16;");

            Button cancelBtn = new Button("В РАНИЦАТА");
            cancelBtn.setStyle("-fx-background-color: #7f8c8d; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 16;");

            equipBtn.setOnAction(e -> {
                setVisible(false);
                if (onEquipAction != null) onEquipAction.run();
            });

            cancelBtn.setOnAction(e -> {
                setVisible(false);
                if (onCancelAction != null) onCancelAction.run();
            });

            buttons.getChildren().addAll(equipBtn, cancelBtn);
            lootBox.getChildren().addAll(title, desc, buttons);

            getChildren().add(lootBox);
            setVisible(true);
            toFront();
            equipBtn.requestFocus();
        });
    }
}
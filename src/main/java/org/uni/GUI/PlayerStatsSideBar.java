package org.uni.GUI;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import org.uni.model.ArmorItem;
import org.uni.model.Hero;

public class PlayerStatsSideBar extends VBox {

    private final Label levelLabel = new Label();
    private final Label hpLabel = new Label();
    private final Label atkLabel = new Label();
    private final Label defLabel = new Label();
    private final Label weaponLabel = new Label();
    private final Label armorLabel = new Label();
    private final Label skillLabel = new Label();

    private final MiniMapWidget miniMapWidget = new MiniMapWidget();
    private final TextArea battleLogArea = new TextArea();

    public PlayerStatsSideBar(Runnable onOpenInventory) {
        super(10);
        setPadding(new Insets(15));
        setPrefWidth(280);
        setStyle("-fx-background-color: #2c3e50;");

        Label statsLabel = new Label("PLAYER STATS");
        statsLabel.setStyle("-fx-text-fill: #f1c40f; -fx-font-size: 16px; -fx-font-weight: bold;");

        Label minimapTitle = new Label("🗺️ FLOOR MAP");
        minimapTitle.setStyle("-fx-text-fill: #3498db; -fx-font-size: 14px; -fx-font-weight: bold;");

        Button invBtn = new Button("🎒 INVENTORY");
        invBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold;");
        invBtn.setOnAction(e -> Platform.runLater(onOpenInventory));

        battleLogArea.setEditable(false);
        battleLogArea.setPrefHeight(250);
        battleLogArea.setWrapText(true);
        battleLogArea.setPromptText("Battle Chronolog");
        battleLogArea.setStyle("-fx-control-inner-background: #1e1e1e; -fx-text-fill: #2ecc71;");

        getChildren().addAll(
                statsLabel,
                levelLabel,
                hpLabel,
                atkLabel,
                defLabel,
                weaponLabel,
                armorLabel,
                skillLabel,
                minimapTitle,
                miniMapWidget,
                invBtn,
                new Label("Battle Log:"),
                battleLogArea
        );
    }

    public void updateStats(Hero hero, int dungeonLevel, String weaponName, String skillName) {
        if (hero == null) return;

        ArmorItem currentArmor = hero.getEquippedArmor();
        String armorName = (currentArmor != null) ? currentArmor.getName() : "None";
        String finalWeapon = (weaponName != null && !weaponName.isEmpty()) ? weaponName : "None";
        String finalSkill = (skillName != null && !skillName.isEmpty()) ? skillName : "Basic Strike";

        Platform.runLater(() -> {
            levelLabel.setText("🏰 Floor: " + dungeonLevel);
            levelLabel.setStyle("-fx-text-fill: #f1c40f; -fx-font-weight: bold; -fx-font-size: 13px;");

            hpLabel.setText("❤️ HP: " + hero.getHp() + " / " + hero.getMaxHP());
            hpLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 14px;");

            atkLabel.setText("⚔️ ATK: " + hero.getAtk());
            atkLabel.setStyle("-fx-text-fill: #f1c40f; -fx-font-weight: bold; -fx-font-size: 14px;");

            defLabel.setText("🛡️ DEF: " + hero.getTotalDefense());
            defLabel.setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold; -fx-font-size: 14px;");

            weaponLabel.setText("🗡️ Weapon: " + finalWeapon);
            weaponLabel.setStyle("-fx-text-fill: #ecf0f1; -fx-font-size: 13px;");

            armorLabel.setText("🛡️ Armor: " + armorName);
            armorLabel.setStyle("-fx-text-fill: #ecf0f1; -fx-font-size: 13px;");

            skillLabel.setText("🔮 Skill: " + finalSkill);
            skillLabel.setStyle("-fx-text-fill: #9b59b6; -fx-font-size: 13px;");
        });
    }

    public void appendLog(String message) {
        if (battleLogArea != null) {
            Platform.runLater(() -> battleLogArea.appendText(message + "\n"));
        }
    }

    public MiniMapWidget getMiniMapWidget() {
        return miniMapWidget;
    }
}
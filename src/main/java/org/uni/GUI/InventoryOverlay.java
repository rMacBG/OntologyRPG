package org.uni.GUI;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import org.uni.agents.GUIAgent;
import org.uni.model.ArmorItem;
import org.uni.model.Hero;
import org.uni.model.SkillItem;
import org.uni.model.WeaponItem;
import org.uni.service.CombatService;
import org.uni.service.DatabaseService;

import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.List;


public class InventoryOverlay extends HBox {
    private final DatabaseService db = DatabaseService.getInstance();
    private final CombatService cs;
    private final Runnable onStatsUpdated;
    private Hero hero;
    private String selectedPlayerClass;
    private String selectedPreviewItem = null;

    public InventoryOverlay(CombatService cs, Runnable onStatsUpdated) {
        super(20);
        this.cs = cs;
        this.onStatsUpdated = onStatsUpdated;

        this.setAlignment(Pos.CENTER);
        this.setStyle("-fx-background-color: rgba(0, 0, 0, 0.75);");
        this.setVisible(false);
    }

    public void open(Hero hero, String selectedPlayerClass) {
        this.hero = hero;
        this.selectedPlayerClass = selectedPlayerClass;
        Platform.runLater(() -> {
            refreshUI();
            this.setVisible(true);
        });
    }

    public void close() {
        this.setVisible(false);
    }

    public void refreshUI() {
        this.getChildren().clear();

        HBox root = new HBox(15);
        root.setPadding(new Insets(15));
        root.setStyle("-fx-background-color: #1a1a2e; -fx-border-color: #e74c3c; -fx-border-width: 2; -fx-background-radius: 10; -fx-border-radius: 10;");
        root.setMaxSize(820, 520);

        // 1. EQUIPPED GEAR
        VBox equipPanel = new VBox(10);
        equipPanel.setAlignment(Pos.TOP_CENTER);
        equipPanel.setPrefWidth(220);
        equipPanel.setStyle("-fx-background-color: #16213e; -fx-padding: 10; -fx-background-radius: 8;");

        Label equipTitle = new Label("⚔ EQUIPPED GEAR");
        equipTitle.setStyle("-fx-text-fill: #f1c40f; -fx-font-size: 14px; -fx-font-weight: bold;");

        String currentWeapon = db.getPlayerWeapon(selectedPlayerClass);
        VBox weaponSlot = createEquippedSlotBox("WEAPON", currentWeapon, getItemStatsDescription(currentWeapon), "#e74c3c");

        ArmorItem currentArmorObj = hero.getEquippedArmor();
        String currentArmorName = (currentArmorObj != null) ? currentArmorObj.getName() : null;
        VBox armorSlot = createEquippedSlotBox("ARMOR", currentArmorName, getItemStatsDescription(currentArmorName), "#2ecc71");

        String currentSkillName = db.getPlayerSkillName(selectedPlayerClass);
        VBox skillSlot = createEquippedSlotBox("SKILL", currentSkillName, getItemStatsDescription(currentSkillName), "#f39c12");

        equipPanel.getChildren().addAll(equipTitle, weaponSlot, armorSlot, skillSlot);

        // 2. BACKPACK
        VBox backpackPanel = new VBox(10);
        backpackPanel.setPrefWidth(320);

        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);
        Label backpackTitle = new Label("🎒 BACKPACK");
        backpackTitle.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 16px; -fx-font-weight: bold;");

        Region spacerHeader = new Region();
        HBox.setHgrow(spacerHeader, Priority.ALWAYS);

        Button closeBtn = new Button("❌");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e74c3c; -fx-font-size: 16px; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> close());
        headerBox.getChildren().addAll(backpackTitle, spacerHeader, closeBtn);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(410);
        scrollPane.setStyle("-fx-background: #16213e; -fx-background-color: transparent;");

        VBox itemsContainer = new VBox(8);
        itemsContainer.setPadding(new Insets(8));

        // 3. PREVIEW
        VBox previewPanel = new VBox(10);
        previewPanel.setPrefWidth(240);
        previewPanel.setAlignment(Pos.TOP_CENTER);
        previewPanel.setStyle("-fx-background-color: #16213e; -fx-padding: 12; -fx-background-radius: 8; -fx-border-color: #2980b9; -fx-border-radius: 8;");

        Label previewTitle = new Label("🔮 ITEM PREVIEW");
        previewTitle.setStyle("-fx-text-fill: #3498db; -fx-font-size: 15px; -fx-font-weight: bold;");

        Label previewTextLabel = new Label();
        previewTextLabel.setWrapText(true);
        previewTextLabel.setStyle("-fx-text-fill: #ecf0f1; -fx-font-size: 12px;");
        previewTextLabel.setText(selectedPreviewItem != null ? getItemPreviewDetailsText(selectedPreviewItem) : "Кликнете върху предмет за преглед.");

        previewPanel.getChildren().addAll(previewTitle, previewTextLabel);

        Runnable updatePreviewAction = () -> {
            if (selectedPreviewItem != null) {
                previewTextLabel.setText(getItemPreviewDetailsText(selectedPreviewItem));
            }
        };

        weaponSlot.setOnMouseClicked(e -> { selectedPreviewItem = currentWeapon; updatePreviewAction.run(); });
        armorSlot.setOnMouseClicked(e -> { selectedPreviewItem = currentArmorName; updatePreviewAction.run(); });
        skillSlot.setOnMouseClicked(e -> { selectedPreviewItem = currentSkillName; updatePreviewAction.run(); });

        String invData = db.getPlayerInventory(selectedPlayerClass);
        if (invData != null && !invData.trim().isEmpty()) {
            String[] items = invData.split(",");
            for (String itemRaw : items) {
                String item = itemRaw.trim();
                if (item.isEmpty()) continue;

                HBox itemRow = new HBox(8);
                itemRow.setAlignment(Pos.CENTER_LEFT);
                itemRow.setStyle("-fx-background-color: #0f3460; -fx-padding: 6; -fx-background-radius: 5; -fx-cursor: hand;");
                itemRow.setOnMouseClicked(e -> {
                    selectedPreviewItem = item;
                    updatePreviewAction.run();
                });

                ImageView icon = getItemImageView(item);

                VBox itemDetails = new VBox(2);
                itemDetails.setMaxWidth(140);
                Label nameLbl = new Label(item);
                nameLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;");
                Label statsLbl = new Label(getItemStatsDescription(item));
                statsLbl.setStyle("-fx-text-fill: #f1c40f; -fx-font-size: 10px;");
                itemDetails.getChildren().addAll(nameLbl, statsLbl);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Button actionBtn = new Button();
                actionBtn.setMinWidth(80);

                boolean isEquippedWeapon = item.equalsIgnoreCase(currentWeapon);
                boolean isEquippedArmor = currentArmorName != null && item.equalsIgnoreCase(currentArmorName);
                boolean isEquippedSkill = currentSkillName != null && item.equalsIgnoreCase(currentSkillName);

                if (item.equals("Health Potion")) {
                    actionBtn.setText("DRINK");
                    actionBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px;");
                } else if (isEquippedWeapon || isEquippedArmor || isEquippedSkill) {
                    actionBtn.setText("EQUIPPED");
                    actionBtn.setDisable(true);
                    actionBtn.setStyle("-fx-background-color: #7f8c8d; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 9px;");
                } else {
                    actionBtn.setText("EQUIP");
                    actionBtn.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px;");
                }

                actionBtn.setOnAction(e -> {
                    selectedPreviewItem = item;
                    if (item.equals("Health Potion")) {
                        hero.heal(40);
                        db.removeItemFromInventory(selectedPlayerClass, "Health Potion");
                        db.updatePlayerHP(selectedPlayerClass, hero.getHp());
                    } else if (cs.isSkill(item)) {
                        db.equipSkill(selectedPlayerClass, item);
                    } else {
                        boolean equipped = cs.equipItemForHero(hero, selectedPlayerClass, item);
                        if (equipped && GUIAgent.instance != null) {
                            GUIAgent.instance.sendMessage("EQUIP_ITEM:" + selectedPlayerClass + ":" + item);
                        }
                    }
                    if (onStatsUpdated != null) onStatsUpdated.run();
                    refreshUI();
                });

                itemRow.getChildren().addAll(icon, itemDetails, spacer, actionBtn);
                itemsContainer.getChildren().add(itemRow);
            }
        } else {
            Label emptyLbl = new Label("Your backpack is empty.");
            emptyLbl.setStyle("-fx-text-fill: #7f8c8d; -fx-font-style: italic;");
            itemsContainer.getChildren().add(emptyLbl);
        }

        scrollPane.setContent(itemsContainer);
        backpackPanel.getChildren().addAll(headerBox, scrollPane);

        root.getChildren().addAll(equipPanel, backpackPanel, previewPanel);
        this.getChildren().add(root);
    }

    private VBox createEquippedSlotBox(String slotTitle, String itemName, String itemStats, String borderColor) {
        VBox slot = new VBox(2);
        slot.setAlignment(Pos.CENTER);
        slot.setPadding(new Insets(6));
        slot.setStyle("-fx-background-color: #0f3460; -fx-background-radius: 6; -fx-border-color: " + (itemName != null ? borderColor : "#7f8c8d") + "; -fx-border-radius: 6;");

        Label slotLabel = new Label(slotTitle);
        slotLabel.setStyle("-fx-text-fill: #bdc3c7; -fx-font-size: 10px; -fx-font-weight: bold;");

        if (itemName != null && !itemName.isEmpty()) {
            Label nameLabel = new Label(itemName);
            nameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;");
            ImageView img = getItemImageView(itemName);
            Label statsLabel = new Label(itemStats);
            statsLabel.setWrapText(true);
            statsLabel.setStyle("-fx-text-fill: #f1c40f; -fx-font-size: 10px;");

            slot.getChildren().addAll(slotLabel, img, nameLabel, statsLabel);
        } else {
            Label emptyLabel = new Label("(Empty)");
            emptyLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
            slot.getChildren().addAll(slotLabel, emptyLabel);
        }
        return slot;
    }

    private ImageView getItemImageView(String itemName) {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(40);
        imageView.setFitHeight(40);
        try {
            var stream = getClass().getResourceAsStream("/images/" + itemName + ".png");
            if (stream != null) imageView.setImage(new Image(stream));
        } catch (Exception ignored) {}
        return imageView;
    }

    private String getItemStatsDescription(String itemName) {
        if (itemName == null || itemName.trim().isEmpty()) return "";
        if (itemName.equalsIgnoreCase("Health Potion")) return "Restores +40 HP";

        String category = cs.getItemCategory(itemName);

        if ("WEAPON".equalsIgnoreCase(category)) {
            WeaponItem weapon = cs.loadWeaponFromOntology(itemName);
            if (weapon != null) return "+" + weapon.getBaseDamage() + " Base ATK";
        } else if ("ARMOR".equalsIgnoreCase(category)) {
            ArmorItem armor = cs.loadArmorFromOntology(itemName);
            if (armor != null) {
                List<String> armorParts = new ArrayList<>();
                if (armor.getBaseDef() > 0) armorParts.add("+" + armor.getBaseDef() + " DEF");
                if (armor.getDamageBonus() > 0) armorParts.add("+" + armor.getDamageBonus() + " ATK");
                if (armor.getDamageResistance() > 0) armorParts.add("+" + armor.getDamageResistance() + " Res");
                return String.join(" | ", armorParts);
            }
        } else if ("SKILL".equalsIgnoreCase(category)) {
            SkillItem skill = cs.loadSkillFromOntology(itemName);
            if (skill != null && skill.getName() != null && !skill.getName().equalsIgnoreCase("Basic Strike")) {
                List<String> parts = new ArrayList<>();
                if (skill.getBaseDamage() > 0) parts.add("DMG: " + skill.getBaseDamage());
                if (skill.getDamageBonus() > 0) parts.add("+" + skill.getDamageBonus() + " ATK");
                if (skill.getDamageResistance() > 0) parts.add("+" + skill.getDamageResistance() + " DEF");
                if (skill.getActiveRounds() > 0) parts.add(skill.getActiveRounds() + " Rds");
                if (skill.getCooldown() > 0) parts.add("CD: " + skill.getCooldown() + "t");
                return String.join(" | ", parts);
            }
        }
        return "Equipment / Item";
    }

    private String getItemPreviewDetailsText(String itemName) {
        if (itemName == null || itemName.trim().isEmpty()) return "Няма избран предмет.";
        if (itemName.equalsIgnoreCase("Health Potion")) {
            return "🧪 HEALTH POTION\n\n• Категория: Consumable\n• Ефект: Възстановява 40 HP.";
        }

        String category = cs.getItemCategory(itemName);
        if ("WEAPON".equalsIgnoreCase(category)) {
            WeaponItem weapon = cs.loadWeaponFromOntology(itemName);
            if (weapon != null) {
                return "⚔️ " + weapon.getName().toUpperCase() + "\n\n• Оръжие\n• Базов Атак: +" + weapon.getBaseDamage() + " ATK";
            }
        } else if ("ARMOR".equalsIgnoreCase(category)) {
            ArmorItem armor = cs.loadArmorFromOntology(itemName);
            if (armor != null) {
                return "🛡️ " + armor.getName().toUpperCase() + "\n\n• Броня\n• DEF: +" + armor.getBaseDef();
            }
        } else if ("SKILL".equalsIgnoreCase(category)) {
            SkillItem skill = cs.loadSkillFromOntology(itemName);
            if (skill != null) {
                return "🔮 " + skill.getName().toUpperCase() + "\n\n• Умение\n• Cooldown: " + skill.getCooldown();
            }
        }
        return "Предмет: " + itemName;
    }
}

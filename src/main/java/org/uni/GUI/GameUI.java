package org.uni.GUI;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.uni.agents.GUIAgent;
import org.uni.model.Hero;
import org.uni.model.Item;
import org.uni.model.Monster;
import org.uni.service.CombatService;
import org.uni.service.DatabaseService;
import org.uni.service.DungeonGenerator;
import org.uni.service.OntologyService;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class GameUI extends Application {

    private int playerX = 0;
    private int playerY = 0;
    private final int SIZE = 10;

    private int currentDungeonLevel = 1;
    private int activeMonstersCount = 0;
    private int skillCooldown = 0;
    private DungeonGenerator dungeonGenerator = new DungeonGenerator(SIZE);
    private DungeonGenerator.Tile[][] mapData;


    public static GameUI instance;
    private DatabaseService databaseService = DatabaseService.getInstance();
    private OntologyService ontologyService = new OntologyService();
    private CombatService cs = new CombatService();

    private Button[][] tiles = new Button[SIZE][SIZE];
    private Label info = new Label();
    private String[][] map = new String[SIZE][SIZE];

    private BorderPane mainLayout;
    private GridPane grid;
    private VBox sideBar;
    private TextArea battleLogArea;

    private Label hpLabel = new Label();
    private Label atkLabel = new Label();
    private Label weaponLabel = new Label();
    private Label levelLabel = new Label();
    private boolean isGameOver = false;

    private Hero hero;
    private Monster currentMonster;
    private int currentEnemyX = 0;
    private int currentEnemyY = 0;

//    private String currentEnemyName = "";
//    private int currentEnemyHp = 0;
//    private int currentEnemyMaxHp;
//    private int currentEnemyAtk = 0;
//    private int currentEnemyX = 0;
//    private int currentEnemyY = 0;
//    int currentPlayerHp = databaseService.getHP(this.selectedPlayerClass);
//    int currentPlayerAtk = databaseService.getAttack(this.selectedPlayerClass);
//    private int currentPlayerHpInCombat = 0;
      private String selectedPlayerClass = "WarriorClass";
      private int currentPlayerHpInCombat = 0;

    @Override
    public void start(Stage stage) throws Exception {
        instance = this;

        mainLayout = new BorderPane();
        mainLayout.setStyle("-fx-background-color: #1e1e1e;");

        showMainMenu();

        Scene scene = new Scene(mainLayout, 850, 650);

        scene.setOnKeyPressed(event -> {
            if (mainLayout.getCenter() == grid) {
                switch (event.getCode()) {
                    case W -> movePlayer(playerX, playerY - 1);
                    case S -> movePlayer(playerX, playerY + 1);
                    case A -> movePlayer(playerX - 1, playerY);
                    case D -> movePlayer(playerX + 1, playerY);
                }
            }
        });

        stage.setScene(scene);
        stage.setTitle("Chronicles of Jaba");
        stage.show();

    }

    private void loadLevel(int dungeonLevel) {
        this.currentDungeonLevel = dungeonLevel;
        this.playerX = 0;
        this.playerY = 0;

        List<String> ontologyMonsters = new ArrayList<>();
        ontologyMonsters.add("Dragon");
        ontologyMonsters.add("Goblin");
        ontologyMonsters.add("Demon");

        this.mapData = dungeonGenerator.generateLevel(currentDungeonLevel, ontologyMonsters);

        activeMonstersCount = 0;
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                if (mapData[x][y].type.equals("MONSTER")) {
                    activeMonstersCount++;
                }
            }
        }
        if (battleLogArea != null) {
            battleLogArea.appendText("Entering new floor" + currentDungeonLevel + " --- \n");
            battleLogArea.appendText("Enemies left: " + activeMonstersCount + "\n");
        }

    }
    private void spawnMonsters() {
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                map[x][y] = null;
            }
        }

        String[] monsterPool = {"Dragon", "Goblin", "Demon"};
        Random rand = new Random();
        int spawned = 0;

        while (spawned < 6) {
            int rx = rand.nextInt(SIZE);
            int ry = rand.nextInt(SIZE);

            if ((rx == 0 && ry == 0) || map[rx][ry] != null) {
                continue;
            }

            map[rx][ry] = monsterPool[rand.nextInt(monsterPool.length)];
            spawned++;
        }
    }

    private void showMainMenu() {
        this.isGameOver = false;
        VBox menuBox = new VBox(20);
        menuBox.setAlignment(Pos.CENTER);
        Label title = new Label("Chronicles of Jaba: The Reckoning of the Onterolog");

        title.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 32px; -fx-font-weight: bold;");
        Button startBtn = new Button("START GAME");
        startBtn.setPrefSize(200, 45);
        startBtn.setOnAction(e -> showCharacterCreation());

        Button exitBtn = new Button("EXIT GAME");
        exitBtn.setPrefSize(200, 45);
        exitBtn.setOnAction(e -> Platform.exit());

        menuBox.getChildren().addAll(title, startBtn, exitBtn);
        mainLayout.setRight(null);
        mainLayout.setBottom(null);
        mainLayout.setCenter(menuBox);


    }

    private void showCharacterCreation() {
        VBox creationBox = new VBox(20);
        creationBox.setAlignment(Pos.CENTER);
        creationBox.setPadding(new Insets(30));

        Label header = new Label("CHOOSE YOUR CLASS");
        header.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");

        ToggleGroup group = new ToggleGroup();

        RadioButton rbWarrior = new RadioButton("Warrior (150 HP, 15 ATK - Starts with IronSword)");
        rbWarrior.setToggleGroup(group);
        rbWarrior.setSelected(true);
        rbWarrior.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        rbWarrior.setUserData("WarriorClass");

        RadioButton rbArcher = new RadioButton("Archer (120 HP, 17 ATK) - Starts with ShortBow");
        rbArcher.setToggleGroup(group);
        rbArcher.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        rbArcher.setUserData("ArcherClass");

        RadioButton rbMage = new RadioButton("Wizard (100 HP, 22 ATK) - Starts with StormStaff");
        rbMage.setToggleGroup(group);
        rbMage.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        rbMage.setUserData("WizardClass");

        RadioButton rbRogue = new RadioButton("Assassin (135 HP, 20 ATK) - Starts with SteelDagger");
        rbRogue.setToggleGroup(group);
        rbRogue.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        rbRogue.setUserData("AssassinClass");

        Button confirmBtn = new Button("UNFOLD THE ADVENTURE OF A MILLENIA!");
        confirmBtn.setPrefSize(220, 45);

        confirmBtn.setOnAction(e -> {
            RadioButton selected = (RadioButton) group.getSelectedToggle();
            this.selectedPlayerClass = selected.getUserData().toString();

            String startingWeapon = "IronLongSword";
            if (selectedPlayerClass.equals("ArcherClass")) startingWeapon = "SmallBow";
            else if (selectedPlayerClass.equals("WizardClass")) startingWeapon = "StormStaff";
            else if (selectedPlayerClass.equals("AssassinClass")) startingWeapon = "SteelDagger";

            this.hero = cs.createHero(this.selectedPlayerClass, startingWeapon);

            databaseService.addCustomPlayer(this.selectedPlayerClass, startingWeapon, hero.getHp(), hero.getAtk());

            spawnMonsters();
            loadLevel(1);
            buildGameMap();
        });

        creationBox.getChildren().addAll(header, rbWarrior, rbArcher, rbMage, rbRogue, confirmBtn);
        mainLayout.setCenter(creationBox);
    }

    private void buildGameMap() {
        grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setPadding(new Insets(10));

        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                Button tile = new Button();
                tile.setPrefSize(50, 50);
                tile.setStyle("-fx-background-color: #34495e; -fx-border-color: #2c3e50; -fx-text-fill: white; -fx-font-weight: bold;");
                tiles[x][y] = tile;
                grid.add(tile, x, y);
            }
        }
        sideBar = new VBox(15);
        sideBar.setPadding(new Insets(15));
        sideBar.setPrefWidth(280);
        sideBar.setStyle("-fx-background-color: #2c3e50;");

        Button invBtn = new Button("🎒 INVENTORY");
        invBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold;");
        invBtn.setOnAction(e -> openInventoryWindow());

        Label statsLabel = new Label("PLAYER STATS");
        statsLabel.setStyle("-fx-text-fill: #f1c40f; -fx-font-size: 16px; -fx-font-weight: bold;");

        int pHP = databaseService.getHP(this.selectedPlayerClass);
        int pAtk = databaseService.getAttack(this.selectedPlayerClass);
        String pWeapon = ontologyService.getPropertyValue(this.selectedPlayerClass, "weapon");
        if (pWeapon == null) pWeapon = "Class Weapon";


        hpLabel.setText("HP: " + pHP);
        hpLabel.setStyle("-fx-text-fill: white;");
        atkLabel.setText("ATK: " + pAtk);
        atkLabel.setStyle("-fx-text-fill: white;");
        weaponLabel.setText("Weapon: " + pWeapon);
        weaponLabel.setStyle("-fx-text-fill: white;");

        battleLogArea = new TextArea();
        battleLogArea.setEditable(false);
        battleLogArea.setPrefHeight(300);
        battleLogArea.setWrapText(true);
        battleLogArea.setPromptText("Battle Chronolog");
        battleLogArea.setStyle("-fx-control-inner-background: #1e1e1e; -fx-text-fill: #2ecc71;");

        sideBar.getChildren().addAll(statsLabel, hpLabel, atkLabel, weaponLabel, invBtn, new Label("Battle Log:"), battleLogArea);

        info.setStyle("-fx-text-fill: white; -fx-padding: 10; -fx-font-size: 14px;");
        VBox bottomBox = new VBox(info);
        bottomBox.setStyle("-fx-background-color: #1a1a1a;");

        //spawnMonsters();
        updateMap();

        mainLayout.setCenter(grid);
        mainLayout.setRight(sideBar);
        mainLayout.setBottom(bottomBox);
    }


    private void movePlayer(int newX, int newY) {
        if (mainLayout.getCenter() != grid) {
            return;
        }

        if (newX < 0 || newY < 0 || newX >= SIZE || newY >= SIZE) {
            return;
        }

        DungeonGenerator.Tile targetTile = mapData[newX][newY];

        if (targetTile.type.equals("WALL")) {
            showMessage("Пътят е блокиран от скала/стена!");
            return;
        }

        if (targetTile.type.equals("MONSTER")) {
            String enemyName = targetTile.monsterName;
            System.out.println("Encounter: " + enemyName);

            this.currentEnemyX = newX;
            this.currentEnemyY = newY;

            this.currentMonster = cs.createMonster(enemyName, currentDungeonLevel);

            openTurnBasedCombatScreen();
            return;
        }

        if (targetTile.type.equals("POTION")) {
            hero.heal(30);
            databaseService.updatePlayerHP(this.selectedPlayerClass, hero.getHp());

            mapData[newX][newY] = new DungeonGenerator.Tile("EMPTY", null, "🟩");
            updatePlayerStatsMenu();
            showMessage("Взехте лечебна отвара! +30 HP.");
            if (battleLogArea != null) battleLogArea.appendText("Намерихте отвара и възстановихте 30 HP!\n");
        }

        if (targetTile.type.equals("EXIT")) {
            if (activeMonstersCount <= 0) {
                showNextLevelDialog();
                return;
            } else {
                showMessage("Порталът е заключен! Победете останалите " + activeMonstersCount + " чудовища.");
            }
        }

        playerX = newX;
        playerY = newY;

        updateMap();
    }

    private void showNextLevelDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Етажът е изчистен!");
        alert.setHeaderText("Отлично, Герой!");
        alert.setContentText("Успешно изчистихте Етаж " + currentDungeonLevel + "! Преминавате към Етаж " + (currentDungeonLevel + 1) + ".");
        alert.showAndWait();

        loadLevel(currentDungeonLevel + 1);
        levelLabel.setText("Dungeon Level: " + currentDungeonLevel);
        updateMap();
    }

    private void openTurnBasedCombatScreen() {
        VBox combatScreen = new VBox(20);
        combatScreen.setAlignment(Pos.CENTER);
        combatScreen.setPadding(new Insets(40));
        combatScreen.setStyle("-fx-background-color: #000000; -fx-border-color: red; -fx-border-width: 3;");

        String cleanName = currentMonster.getName().replace("Monster", "").replace("Boss", "").toUpperCase();
        Label fightTitle = new Label(currentMonster.getIcon() + " ENCOUNTER: " + cleanName);
        fightTitle.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 26px; -fx-font-weight: bold;");

        Label weaknessLabel = new Label("Weakness: " + currentMonster.getWeakness() + " | Behavior: " + currentMonster.getBehavior());
        weaknessLabel.setStyle("-fx-text-fill: #bdc3c7; -fx-font-size: 14px;");

        Label enemyHPLabel = new Label("Enemy HP: " + currentMonster.getHp() + " / " + currentMonster.getMaxHp());
        enemyHPLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px;");

        Label enemyAtkLabel = new Label("Damage (ATK): " + currentMonster.getAtk());
        enemyAtkLabel.setStyle("-fx-text-fill: #f1c40f; -fx-font-size: 16px;");

        Label playerHPLabel = new Label("Your HP: " + hero.getHp() + " / " + hero.getMaxHP());
        playerHPLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 18px; -fx-font-weight: bold;");

        Label playerAtkLabel = new Label("Your ATK: " + hero.getAtk());
        playerAtkLabel.setStyle("-fx-text-fill: #3498db; -fx-font-size: 16px;");

        HBox actionButtons = new HBox(20);
        actionButtons.setAlignment(Pos.CENTER);

        Button attackBtn = new Button("[ BASIC ATTACK ]");
        attackBtn.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        Button healBtn = new Button("[ USE POTION ]");
        healBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        Button skillBtn = new Button("[ USE SKILL ]");
        skillBtn.setStyle("-fx-background-color: #8e44ad; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        Button fleeBtn = new Button("[ FLEE ]");
        fleeBtn.setStyle("-fx-background-color: #7f8c8d; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        if (skillCooldown > 0) {
            skillBtn.setDisable(true);
            skillBtn.setStyle("-fx-background-color: #553d67; -fx-text-fill: #888888; -fx-font-size: 16px; -fx-font-weight: bold;");
        }

        attackBtn.setOnAction(e -> {
            if (skillCooldown > 0) {
                skillCooldown--;
            }
            String combatMessage = "FIGHT:ATTACK:" + this.selectedPlayerClass + ":" + currentMonster.getName() +
                    ":" + currentEnemyX + ":" + currentEnemyY + ":" + currentMonster.getHp() +
                    ":" + this.currentDungeonLevel;

            GUIAgent.instance.sendMessage(combatMessage);
        });
        skillBtn.setOnAction(e -> {
            skillCooldown = 2;

            String combatMessage = "FIGHT:SKILL:" + this.selectedPlayerClass + ":" + currentMonster.getName() +
                    ":" + currentEnemyX + ":" + currentEnemyY + ":" + currentMonster.getHp() +
                    ":" + this.currentDungeonLevel;

            GUIAgent.instance.sendMessage(combatMessage);
        });

        healBtn.setOnAction(e -> {
            Item potion = new Item("Health Potion", "HEAL", 40, 1);
            cs.applyItem(hero, potion);

            databaseService.updatePlayerHP(this.selectedPlayerClass, hero.getHp());
            updatePlayerStatsMenu();

            battleLogArea.appendText("Player used a Healing Potion and restored 40 HP!\n");
            showMessage("Restored 40 HP!");

            openTurnBasedCombatScreen();
        });

        fleeBtn.setOnAction(e -> {
            battleLogArea.appendText("You ran away safely!\n");
            mainLayout.setCenter(grid);
            grid.requestFocus();
        });

        actionButtons.getChildren().addAll(attackBtn, skillBtn, healBtn, fleeBtn);
        Label separator = new Label("------------------------");
        combatScreen.getChildren().addAll(fightTitle, weaknessLabel, enemyAtkLabel, enemyHPLabel, separator, playerHPLabel, playerAtkLabel, actionButtons);

        mainLayout.setCenter(combatScreen);
    }





    public void handleMonsterDefeated(int monsterX, int monsterY) {
        Platform.runLater(() -> {

            mapData[monsterX][monsterY] = new DungeonGenerator.Tile("EMPTY", null, "🟩");
            activeMonstersCount--;

            playerX = monsterX;
            playerY = monsterY;
            mainLayout.setCenter(grid);

            if (activeMonstersCount <= 0) {
                unlockPortal();
            }

            updateMap();
            updatePlayerStatsMenu();
            grid.requestFocus();

            if (battleLogArea != null) {
                battleLogArea.appendText("Victory! Defeated " + currentMonster.getName() + "! Остават: " + activeMonstersCount + "\n");
            }
            showMessage("Monster Defeated!");
        });
    }

    private void unlockPortal() {
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                if (mapData[x][y].type.equals("EXIT")) {
                    mapData[x][y].icon = "🚪"; // Отключен портал
                }
            }
        }
        if (battleLogArea != null) {
            battleLogArea.appendText("🎉 Всички чудовища са победени! Порталът (🚪) е отключен!\n");
        }
        showMessage("Порталът е отключен!");
    }

    private void updateMap() {
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                tiles[x][y].setText("");
                tiles[x][y].setStyle("-fx-background-color: #34495e; -fx-border-color: #2c3e50; -fx-text-fill: white;");

                DungeonGenerator.Tile tileData = mapData[x][y];

                if (tileData.type.equals("WALL")) {
                    tiles[x][y].setText("⬛");
                    tiles[x][y].setStyle("-fx-background-color: #111111;");
                } else if (tileData.type.equals("MONSTER")) {
                    tiles[x][y].setText(tileData.icon);
                    tiles[x][y].setStyle("-fx-background-color: #c0392b;");
                } else if (tileData.type.equals("POTION")) {
                    tiles[x][y].setText("🧪");
                    tiles[x][y].setStyle("-fx-background-color: #27ae60;");
                } else if (tileData.type.equals("EXIT")) {
                    tiles[x][y].setText(tileData.icon);
                    tiles[x][y].setStyle("-fx-background-color: #d35400;");
                }
            }
        }
        tiles[playerX][playerY].setText("🧙‍♂️");
        tiles[playerX][playerY].setStyle("-fx-background-color: #f1c40f;");
    }


    public void handleCombatRoundResult(String status, int monsterX, int monsterY, int newEnemyHp, int newPlayerHp, String lootItem, String logMessage) {
        mainLayout.setCenter(grid);
        Platform.runLater(() -> {
            if (currentMonster != null) currentMonster.setHp(newEnemyHp);
            if (hero != null) hero.setHp(newPlayerHp);

            databaseService.updatePlayerHP(this.selectedPlayerClass, newPlayerHp);
            updatePlayerStatsMenu();

            if (battleLogArea != null) {
                battleLogArea.appendText(logMessage + "\n");
            }

            if (status.equals("CONTINUE")) {
                openTurnBasedCombatScreen();
            } else if (status.equals("WIN")) {
                handleMonsterDefeated(monsterX, monsterY);
                if (lootItem != null && !lootItem.equals("NONE")) {
                    if (lootItem.equals("Health Potion")) {
                        if (battleLogArea != null) battleLogArea.appendText("🎁 LOOT: Found Health Potion!\n");
                        showMessage("Found Health Potion!");
                    } else {
                        if (battleLogArea != null) battleLogArea.appendText("⚔️ LOOT: Found " + lootItem + "!\n");
                        showEquipLootDialog(lootItem); // Извикваме диалога за оборудване
                    }
                    updatePlayerStatsMenu();
                }
                mainLayout.setCenter(grid);
            } else if (status.equals("LOSE")) {
                showMessage("GAME OVER! You were defeated.");
                showMainMenu();
            }
        });
    }

    private void showEquipLootDialog(String weaponName) {

        if (!cs.canEquip(this.selectedPlayerClass, weaponName)) {
            showMessage("Намерихте " + weaponName + ", но вашият клас не може да го ползва!");
            if (battleLogArea != null) {
                battleLogArea.appendText("📦 Found " + weaponName + " (Added to inventory, cannot equip).\n");
            }
            return;
        }

        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("New Weapon Found!");
            alert.setHeaderText("🎁 Monster dropped: " + weaponName);
            alert.setContentText("Do you want to equip " + weaponName + " right now?");

            var result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                String equipMessage = "EQUIP_WEAPON:" + this.selectedPlayerClass + ":" + weaponName;
                GUIAgent.instance.sendMessage(equipMessage);
                showMessage("Equipped " + weaponName + "!");
            }
        });
    }

    private javafx.scene.image.ImageView getItemImageView(String itemName) {
        javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView();
        imageView.setFitWidth(40);
        imageView.setFitHeight(40);

        String imagePath = "/images/" + itemName + ".png";
        try {
            var stream = getClass().getResourceAsStream(imagePath);
            if (stream != null) {
                imageView.setImage(new javafx.scene.image.Image(stream));
            } else {
                imageView.setStyle("-fx-border-color: #7f8c8d; -fx-background-color: #2c3e50;");
            }
        } catch (Exception e) {
        }
        return imageView;
    }

    private void openInventoryWindow() {
        Stage invStage = new Stage();
        invStage.initModality(Modality.APPLICATION_MODAL);
        invStage.setTitle("🎒 INVENTORY & EQUIPMENT");

        HBox root = new HBox(20);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #1a1a2e; -fx-border-color: #e74c3c; -fx-border-width: 2;");

        VBox equipPanel = new VBox(15);
        equipPanel.setAlignment(Pos.TOP_CENTER);
        equipPanel.setPrefWidth(220);
        equipPanel.setStyle("-fx-background-color: #16213e; -fx-padding: 15; -fx-background-radius: 8;");

        Label equipTitle = new Label("⚔️ EQUIPPED");
        equipTitle.setStyle("-fx-text-fill: #f1c40f; -fx-font-size: 18px; -fx-font-weight: bold;");

        // This is the weapon slot
        VBox weaponSlot = new VBox(5);
        weaponSlot.setAlignment(Pos.CENTER);
        weaponSlot.setStyle("-fx-background-color: #0f3460; -fx-padding: 10; -fx-background-radius: 5; -fx-border-color: #e74c3c;");
        Label weaponSlotLabel = new Label("WEAPON");
        weaponSlotLabel.setStyle("-fx-text-fill: #bdc3c7; -fx-font-size: 11px;");


        String currentWeapon = databaseService.getPlayerWeapon(this.selectedPlayerClass);
        Label weaponNameLabel = new Label(currentWeapon);
        weaponNameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        ImageView weaponImg = getItemImageView(currentWeapon);
        weaponSlot.getChildren().addAll(weaponSlotLabel, weaponImg, weaponNameLabel);

        //This is the armor slot
        VBox armorSlot = new VBox(5);
        armorSlot.setAlignment(Pos.CENTER);
        armorSlot.setStyle("-fx-background-color: #0f3460; -fx-padding: 10; -fx-background-radius: 5; -fx-border-color: #7f8c8d;");
        Label armorSlotLabel = new Label("ARMOR (Empty)");
        armorSlotLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
        armorSlot.getChildren().addAll(armorSlotLabel);

        //This is the accessory slot
        VBox accessorySlot = new VBox(5);
        accessorySlot.setAlignment(Pos.CENTER);
        accessorySlot.setStyle("-fx-background-color: #0f3460; -fx-padding: 10; -fx-background-radius: 5; -fx-border-color: #7f8c8d;");
        Label accSlotLabel = new Label("ACCESSORY (Empty)");
        accSlotLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
        accessorySlot.getChildren().addAll(accSlotLabel);

        equipPanel.getChildren().addAll(equipTitle, weaponSlot, armorSlot, accessorySlot);

        VBox backpackPanel = new VBox(10);
        backpackPanel.setPrefWidth(320);

        Label backpackTitle = new Label("🎒 BACKPACK");
        backpackTitle.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 18px; -fx-font-weight: bold;");

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(300);
        scrollPane.setStyle("-fx-background: #16213e; -fx-background-color: transparent;");

        VBox itemsContainer = new VBox(8);
        itemsContainer.setPadding(new Insets(10));

        String invData = databaseService.getPlayerInventory(this.selectedPlayerClass);
        if (invData != null && !invData.trim().isEmpty()) {
            String[] items = invData.split(",");

            for (String item : items) {
                if (item.trim().isEmpty()) continue;

                HBox itemRow = new HBox(10);
                itemRow.setAlignment(Pos.CENTER_LEFT);
                itemRow.setStyle("-fx-background-color: #0f3460; -fx-padding: 8; -fx-background-radius: 5;");

                ImageView icon = getItemImageView(item);
                Label nameLbl = new Label(item);
                nameLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Button actionBtn = new Button(item.equals("Health Potion") ? "DRINK" : "EQUIP");
                actionBtn.setStyle(item.equals("Health Potion")
                        ? "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;"
                        : "-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-weight: bold;");

                actionBtn.setOnAction(e -> {
                    if (item.equals("Health Potion")) {
                        hero.heal(40);
                        databaseService.removeItemFromInventory(this.selectedPlayerClass, "Health Potion");
                        databaseService.updatePlayerHP(this.selectedPlayerClass, hero.getHp());
                        updatePlayerStatsMenu();
                        showMessage("🧪 Drank Health Potion (+40 HP)");
                        invStage.close();
                    } else {
                        boolean equipped = cs.equipWeaponForHero(hero, this.selectedPlayerClass, item);
                        if (equipped) {
                            String equipMsg = "EQUIP_WEAPON:" + this.selectedPlayerClass + ":" + item;
                            GUIAgent.instance.sendMessage(equipMsg);
                            updatePlayerStatsMenu();
                            showMessage("⚔️ Equipped " + item);
                            invStage.close();
                        } else {
                            showMessage("❌ Your class cannot equip " + item);
                        }
                    }
                });

                itemRow.getChildren().addAll(icon, nameLbl, spacer, actionBtn);
                itemsContainer.getChildren().add(itemRow);
            }
        } else {
            Label emptyLbl = new Label("Your backpack is empty.");
            emptyLbl.setStyle("-fx-text-fill: #7f8c8d; -fx-font-style: italic;");
            itemsContainer.getChildren().add(emptyLbl);
        }

        scrollPane.setContent(itemsContainer);
        backpackPanel.getChildren().addAll(backpackTitle, scrollPane);

        root.getChildren().addAll(equipPanel, backpackPanel);

        Scene scene = new Scene(root);
        invStage.setScene(scene);
        invStage.show();
    }

    public void updatePlayerStatsMenu() {
        if (hero == null) return;
        Platform.runLater(() -> {
            hpLabel.setText("HP: " + hero.getHp() + " / " + hero.getMaxHP());
            atkLabel.setText("ATK: " + hero.getAtk());
            weaponLabel.setText("Active Class: " + this.selectedPlayerClass.replace("Class", ""));
        });
    }

    public void showMessage(String message) {
        Platform.runLater(() -> {
            info.setText(message);
        });
    }
}

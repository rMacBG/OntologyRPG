package org.uni.GUI;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.uni.agents.GUIAgent;
import org.uni.agents.PlayerAgent;
import org.uni.service.CombatService;
import org.uni.service.DatabaseService;
import org.uni.service.OntologyService;

import javax.xml.crypto.Data;
import java.util.Random;


public class GameUI extends Application {

    private int playerX = 0;
    private int playerY = 0;
    private final int SIZE = 10;

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



    private String currentEnemyName = "";
    private int currentEnemyHp = 0;
    private int currentEnemyMaxHp;
    private int currentEnemyAtk = 0;
    private int currentEnemyX = 0;
    private int currentEnemyY = 0;
    int currentPlayerHp = databaseService.getHP(this.selectedPlayerClass);
    int currentPlayerAtk = databaseService.getAttack(this.selectedPlayerClass);
    private int currentPlayerHpInCombat = 0;
    private String selectedPlayerClass = "WarriorClass";
    //private int currentPlayerHpInCombat = 0;

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

        RadioButton rbMage = new RadioButton("Mage (100 HP, 22 ATK) - Starts with StormStaff");
        rbMage.setToggleGroup(group);
        rbMage.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        rbMage.setUserData("MageClass");

        RadioButton rbRogue = new RadioButton("Assassin (135 HP, 20 ATK) - Starts with SteelDagger");
        rbRogue.setToggleGroup(group);
        rbRogue.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        rbRogue.setUserData("AssassinClass");

        Button confirmBtn = new Button("UNFOLD THE ADVENTURE OF A MILLENIA!");
        confirmBtn.setPrefSize(220, 45);

        confirmBtn.setOnAction(e -> {
            RadioButton selected = (RadioButton) group.getSelectedToggle();
            //String selectedClassIndividual = selected.getUserData().toString();
            this.selectedPlayerClass = selected.getUserData().toString();


            int initialHP = cs.getIntProperty(this.selectedPlayerClass, "hasHP");
            int initialATK = cs.getIntProperty(this.selectedPlayerClass, "hasBaseDamage");

//            if (initialHP == 0) {
//                if (selectedClassIndividual.equals("WarriorClass")) initialHP = 150;
//                else if (selectedClassIndividual.equals("ArcherClass")) initialHP = 120;
//                else if (selectedClassIndividual.equals("MageClass")) initialHP = 100;
//                else if (selectedClassIndividual.equals("RogueClass")) initialHP = 135;
//            }
//            if (initialATK == 0) {
//                if (selectedClassIndividual.equals("WarriorClass")) initialATK = 15;
//                else if (selectedClassIndividual.equals("ArcherClass")) initialATK = 17;
//                else if (selectedClassIndividual.equals("MageClass")) initialATK = 22;
//                else if (selectedClassIndividual.equals("RogueClass")) initialATK = 20;
//            }
            String startingWeapon = "Starting Weapon";
            if (selectedPlayerClass.equals("WarriorClass")) startingWeapon = "IronSword";
            else if (selectedPlayerClass.equals("ArcherClass")) startingWeapon = "ShortBow";
            else if (selectedPlayerClass.equals("MageClass")) startingWeapon = "StormStaff";
            else if (selectedPlayerClass.equals("RogueClass")) startingWeapon = "SteelDagger";

//            ontologyService.updateIndividualProperty("Player", "hasHP", initialHP);
//            ontologyService.updateIndividualProperty("Player", "hasBaseDamage", initialATK);

            databaseService.addCustomPlayer(this.selectedPlayerClass, startingWeapon, initialHP, initialATK);
            this.currentPlayerHpInCombat = initialHP;
            //databaseService.updatePlayerHP("Player", initialHP);

            spawnMonsters();
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

        sideBar.getChildren().addAll(statsLabel, hpLabel, atkLabel, weaponLabel, new Label("Battle Log:"), battleLogArea);

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


        if (map[newX][newY] != null) {
            String enemy = map[newX][newY];
            System.out.println("Encounter: " + enemy);

            this.currentEnemyX = newX;
            this.currentEnemyY = newY;
            this.currentEnemyName = enemy;

            this.currentEnemyMaxHp = cs.getIntProperty(this.currentEnemyName, "hasHP");

            if (this.currentEnemyMaxHp == 0) {
                this.currentEnemyMaxHp = 100;
            }

            this.currentEnemyHp = this.currentEnemyMaxHp;

            int dbHP = databaseService.getHP(this.selectedPlayerClass);
            this.currentPlayerHpInCombat = dbHP > 0 ? dbHP : 150;

            openTurnBasedCombatScreen();
            return;
        }
        playerX = newX;
        playerY = newY;

        updateMap();
    }

    private void openTurnBasedCombatScreen() {
        VBox combatScreen = new VBox(20);
        combatScreen.setAlignment(Pos.CENTER);
        combatScreen.setPadding(new Insets(40));
        combatScreen.setStyle("-fx-background-color: #000000; -fx-border-color: red; -fx-border-width: 3;");


        String cleanName = currentEnemyName.replace("Monster", "").replace("Boss", "").toUpperCase();
        Label fightTitle = new Label("ENCOUNTER: " + cleanName);
        fightTitle.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 26px; -fx-font-weight: bold;");

        String typeText = "Regular Monster";
        String typeColor = "#3498db";

        if (currentEnemyName.contains("Boss")) {
            typeText = "BOSS 👑";
            typeColor = "#e67e22";
        }

        Label typeLabel = new Label("Type: " + typeText);
        typeLabel.setStyle("-fx-text-fill: " + typeColor + "; -fx-font-size: 16px; -fx-font-weight: bold;");

        Label enemyHPLabel = new Label("Enemy HP: " + this.currentEnemyHp + " / " + this.currentEnemyMaxHp);
        enemyHPLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px;");

        String attackIndividual = cs.getAttack(currentEnemyName);
        if (attackIndividual != null) {
            this.currentEnemyAtk = cs.getIntProperty(attackIndividual, "hasBaseDamage");
        } else {
            this.currentEnemyAtk = cs.getIntProperty(currentEnemyName, "hasBaseDamage");
        }

        Label enemyAtkLabel = new Label("Damage (ATK): " + this.currentEnemyAtk);
        enemyAtkLabel.setStyle("-fx-text-fill: #f1c40f; -fx-font-size: 16px;");

        Label playerHPLabel = new Label("Your HP: " + this.currentPlayerHpInCombat);
        playerHPLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 18px; -fx-font-weight: bold;");

        this.currentPlayerAtk = cs.getIntProperty(this.selectedPlayerClass, "hasBaseDamage");
        Label playerAtkLabel = new Label("Your ATK: " + this.currentPlayerAtk);
        playerAtkLabel.setStyle("-fx-text-fill: #3498db; -fx-font-size: 16px;");


        HBox actionButtons = new HBox(20);
        actionButtons.setAlignment(Pos.CENTER);

        Button attackBtn = new Button("[ ATTACK ]");
        attackBtn.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        Button healBtn = new Button("[ USE POTION ]");
        healBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        Button fleeBtn = new Button("[ FLEE ]");
        fleeBtn.setStyle("-fx-background-color: #7f8c8d; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        attackBtn.setOnAction(e -> {
            String currentHpString = String.valueOf(this.currentEnemyHp);

            String combatMessage = "FIGHT:" + this.selectedPlayerClass + ":" + currentEnemyName + ":" + currentEnemyX + ":" + currentEnemyY + ":" + currentHpString;

            GUIAgent.instance.sendMessage(combatMessage);
        });

        healBtn.setOnAction(e -> {
            //int currentHP = cs.getIntProperty(this.selectedPlayerClass, "hasHp");
            int nextHP = this.currentPlayerHpInCombat + 40;
            this.currentPlayerHpInCombat = nextHP;

            databaseService.updatePlayerHP(this.selectedPlayerClass, nextHP);
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

        actionButtons.getChildren().addAll(attackBtn, healBtn, fleeBtn);
        Label separator = new Label("------------------------");
        combatScreen.getChildren().addAll(fightTitle,typeLabel, enemyAtkLabel, enemyHPLabel, separator, playerHPLabel, playerAtkLabel, actionButtons);

        mainLayout.setCenter(combatScreen);
    }

    public void handleMonsterDefeated(int monsterX, int monsterY) {
        Platform.runLater(() -> {
            map[monsterX][monsterY] = null;
            playerX = monsterX;
            playerY = monsterY;
            mainLayout.setCenter(grid);

            updateMap();

            updatePlayerStatsMenu();
            grid.requestFocus();

            if (battleLogArea != null) {
                String cleanName = currentEnemyName.replace("Monster", "").replace("Boss", "");
                battleLogArea.appendText("Victory! Defeated the " + currentEnemyName + "!\n");
            }
            showMessage("Monster Defeated! Position updated.");
        });
    }

    private void updateMap() {
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                tiles[x][y].setText("");
                tiles[x][y].setStyle("-fx-background-color: #34495e; -fx-border-color: #2c3e50; -fx-text-fill: white;");

                if (map[x][y] != null) {
                    if (map[x][y].equals("Dragon")) {
                        tiles[x][y].setText("🐲");
                        tiles[x][y].setStyle("-fx-background-color: #c0392b;");
                    } else if (map[x][y].equals("Goblin")) {
                        tiles[x][y].setText("👺");
                        tiles[x][y].setStyle("-fx-background-color: #27ae60;");
                    } else if (map[x][y].equals("Demon")) {
                        tiles[x][y].setText("😈");
                        tiles[x][y].setStyle("-fx-background-color: #8e44ad;");
                    }
                }
            }
        }
        tiles[playerX][playerY].setText("🧙‍♂️");
        tiles[playerX][playerY].setStyle("-fx-background-color: #f1c40f;");
    }

    public void handleCombatRoundResult(String status, int monsterX, int monsterY, int newEnemyHp, int newPlayerHp, String logMessage) {
        Platform.runLater(() -> {
            this.currentEnemyHp = newEnemyHp;
            this.currentPlayerHpInCombat = newPlayerHp;

            databaseService.updatePlayerHP(this.selectedPlayerClass, newPlayerHp);
            updatePlayerStatsMenu();

            if (battleLogArea != null) {
                battleLogArea.appendText(logMessage + "\n");
            }

            if (status.equals("CONTINUE")) {
                openTurnBasedCombatScreen();
            }
            else if (status.equals("WIN")) {
                handleMonsterDefeated(monsterX, monsterY);
                mainLayout.setCenter(grid);
            }
            else if (status.equals("LOSE")) {
                showMessage("GAME OVER! You were defeated.");
                mainLayout.setCenter(grid);
            }
        });
    }

    public void updatePlayerStatsMenu() {
        int currentHP = this.currentPlayerHpInCombat;
        if (currentHP <= 0) {
            currentHP = databaseService.getHP(this.selectedPlayerClass);
        }
        int currentAtk = databaseService.getAttack(this.selectedPlayerClass);
        //if (currentAtk <= 0) currentAtk = 15;

        String currentWeapon = databaseService.getPlayerWeapon(this.selectedPlayerClass);
        if (currentWeapon == null) currentWeapon = "Class Weapon";

        int finalHP = currentHP;
        int finalAtk = currentAtk;
        Platform.runLater(() -> {
            hpLabel.setText("HP: " + finalHP);
            atkLabel.setText("ATK: " + finalAtk);
            weaponLabel.setText("Active Class: " + this.selectedPlayerClass.replace("Class", ""));
        });
    }

    public void showMessage(String message) {
        Platform.runLater(() -> {
            info.setText(message);
        });
    }
}


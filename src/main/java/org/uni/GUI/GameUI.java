package org.uni.GUI;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.effect.Light;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.uni.agents.GUIAgent;
import org.uni.model.*;
import org.uni.service.CombatService;
import org.uni.service.DatabaseService;
import org.uni.service.DungeonGenerator;
import org.uni.service.OntologyService;
import org.uni.model.Room.RoomType;
import org.uni.model.Room.Direction;

import java.util.*;
import java.util.List;


public class GameUI extends Application {

    private MonsterAI monsterAI;
    private InventoryOverlay inventoryOverlay;
    private PlayerStatsSideBar sideBar;
    private LevelCompleteOverlay levelCompleteOverlay;
    private LootDialogOverlay lootOverlay;
    private CombatOverlay combatOverlay;

    private int playerX = 0;
    private int playerY = 0;
    private final int SIZE = 10;

    private int currentDungeonLevel = 1;
    private int activeMonstersCount = 0;
    private int skillCooldown = 0;
    private int activeSkillRounds = 0;
    private SkillItem currentActiveSkill = null;

    private DungeonGenerator dungeonGenerator = new DungeonGenerator(SIZE);
    private Set<Room> visitedRooms = new HashSet<>();
    private List<Room> currentFloorRooms;
    private GridPane miniMapGrid = new GridPane();
    private Room currentRoom;
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
    private TextArea battleLogArea;
    private StackPane mainStackPane;
    private boolean isGameOver = false;

    private Label levelLabel = new Label();


    private boolean isTransitioningLevel = false;

    private Hero hero;
    private Monster currentMonster;
    private int currentEnemyX = 0;
    private int currentEnemyY = 0;

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
            boolean isInventoryOpen = inventoryOverlay != null && inventoryOverlay.isVisible();
            boolean isLevelCompleteOpen = levelCompleteOverlay != null && levelCompleteOverlay.isVisible();

            if (!isInventoryOpen && !isLevelCompleteOpen) {
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

    public void loadLevel(int dungeonLevel) {
        this.currentDungeonLevel = dungeonLevel;

        List<String> ontologyMonsters = List.of("Dragon", "Goblin", "Demon");
        List<String> ontologyBosses = List.of("DragonBoss", "DemonLord");

        this.currentFloorRooms = dungeonGenerator.generateFloor(currentDungeonLevel, ontologyMonsters, ontologyBosses);

        if (sideBar != null && sideBar.getMiniMapWidget() != null) {
            sideBar.getMiniMapWidget().resetForNewFloor(currentFloorRooms);
        }

        if (sideBar != null) {
            sideBar.appendLog("--- Влизате в Етаж " + currentDungeonLevel + " (Общо стаи: " + currentFloorRooms.size() + ") ---");
        }

        if (currentFloorRooms != null && !currentFloorRooms.isEmpty()) {
            loadRoom(currentFloorRooms.get(0), SIZE / 2, SIZE / 2);
        }
    }

    private void loadRoom(Room room, int startX, int startY) {
        if (room == null) return;

        this.currentRoom = room;
        this.mapData = room.getGrid();
        this.playerX = startX;
        this.playerY = startY;

        activeMonstersCount = 0;
        if (mapData != null) {
            for (int x = 0; x < SIZE; x++) {
                for (int y = 0; y < SIZE; y++) {
                    if (mapData[x][y] != null && "MONSTER".equals(mapData[x][y].type)) {
                        activeMonstersCount++;
                    }
                }
            }
        }

        if (sideBar != null) {
            sideBar.appendLog("Влязохте в Стая #" + room.getId() + " [" + room.getType() + "]. Остават чудовища: " + activeMonstersCount);
        }

        updateMap();
        updateMiniMap();
        updatePlayerStatsMenu();

        if (mainLayout != null) {
            mainLayout.requestFocus();
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
        MainMenuScreen mainMenu = new MainMenuScreen(
                this::showCharacterCreation,
                Platform::exit
        );

        mainLayout.setRight(null);
        mainLayout.setBottom(null);
        mainLayout.setCenter(mainMenu);


    }

    private record HeroClassOption(String id, String labelText, String defaultWeapon) {
    }

    private void showCharacterCreation() {
        VBox creationBox = new VBox(20);
        creationBox.setAlignment(Pos.CENTER);
        creationBox.setPadding(new Insets(30));

        Label header = new Label("CHOOSE YOUR CLASS");
        header.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");
        creationBox.getChildren().add(header);

        ToggleGroup group = new ToggleGroup();

        List<HeroClassOption> classOptions = List.of(
                new HeroClassOption("WarriorClass", "Warrior (150 HP, 15 ATK) - Starts with Iron Longsword", "IronLongSword"),
                new HeroClassOption("ArcherClass", "Archer (120 HP, 17 ATK) - Starts with Small Bow", "SmallBow"),
                new HeroClassOption("WizardClass", "Wizard (100 HP, 22 ATK) - Starts with Storm Staff", "StormStaff"),
                new HeroClassOption("AssassinClass", "Assassin (135 HP, 20 ATK) - Starts with Steel Dagger", "SteelDagger")
        );

        for (int i = 0; i < classOptions.size(); i++) {
            HeroClassOption option = classOptions.get(i);
            RadioButton rb = new RadioButton(option.labelText());
            rb.setToggleGroup(group);
            rb.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
            rb.setUserData(option);

            if (i == 0) {
                rb.setSelected(true);
            }

            creationBox.getChildren().add(rb);
        }

        Button confirmBtn = new Button("UNFOLD THE ADVENTURE OF A MILLENIA!");
        confirmBtn.setPrefSize(220, 45);

        confirmBtn.setOnAction(e -> {
            RadioButton selected = (RadioButton) group.getSelectedToggle();
            HeroClassOption selectedOption = (HeroClassOption) selected.getUserData();

            this.selectedPlayerClass = selectedOption.id();
            String startingWeapon = selectedOption.defaultWeapon();

            String startingSkill = switch (this.selectedPlayerClass) {
                case "WarriorClass" -> "SteelHelmet";
                case "ArcherClass" -> "LeatherQuiver";
                case "WizardClass" -> "ChargedLightning";
                case "AssassinClass" -> "FissureGrenade";
                default -> "SteelHelmet";
            };

            this.hero = cs.createHero(this.selectedPlayerClass, startingWeapon);

            databaseService.addCustomPlayer(
                    this.selectedPlayerClass,
                    startingWeapon,
                    startingSkill,
                    hero.getHp(),
                    hero.getAtk(),
                    hero.getTotalDefense()
            );

            databaseService.addLootToInventory(this.selectedPlayerClass, startingSkill);
            databaseService.addLootToInventory(this.selectedPlayerClass, "Health Potion");

            switch (this.selectedPlayerClass) {
                case "WarriorClass" -> databaseService.addLootToInventory(this.selectedPlayerClass, "SteelHeavyArmor");
                case "WizardClass" -> databaseService.addLootToInventory(this.selectedPlayerClass, "MagicRobe");
                case "ArcherClass", "AssassinClass" ->
                        databaseService.addLootToInventory(this.selectedPlayerClass, "LeatherLightArmor");
            }

            spawnMonsters();
            loadLevel(1);
            buildGameMap();
        });

        creationBox.getChildren().add(confirmBtn);
        mainLayout.setCenter(creationBox);
    }

    private void buildGameMap() {
        monsterAI = new MonsterAI(cs, SIZE);

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

        sideBar = new PlayerStatsSideBar(this::openInventoryWindow);

        info.setStyle("-fx-text-fill: white; -fx-padding: 10; -fx-font-size: 14px;");
        VBox bottomBox = new VBox(info);
        bottomBox.setStyle("-fx-background-color: #1a1a1a;");

        inventoryOverlay = new InventoryOverlay(cs, this::updatePlayerStatsMenu);
        levelCompleteOverlay = new LevelCompleteOverlay();
        lootOverlay = new LootDialogOverlay();

        combatOverlay = new CombatOverlay(cs, new CombatOverlay.CombatCallbacks() {
            @Override
            public void onLog(String message) { sideBar.appendLog(message); }
            @Override
            public void onStatsUpdate() { updatePlayerStatsMenu(); }
            @Override
            public void onCloseCombat() {
                mainLayout.setCenter(mainStackPane);
                mainLayout.requestFocus();
            }
        });

        updateMap();
        updateMiniMap();
        updatePlayerStatsMenu();

        mainStackPane = new StackPane();
        mainStackPane.getChildren().addAll(grid, inventoryOverlay, levelCompleteOverlay, lootOverlay);

        mainLayout.setCenter(mainStackPane);
        mainLayout.setRight(sideBar);
        mainLayout.setBottom(bottomBox);
    }

    private void movePlayer(int newX, int newY) {
        if (isTransitioningLevel || (inventoryOverlay != null && inventoryOverlay.isVisible())) {
            return;
        }

        if (newX < 0 || newY < 0 || newX >= SIZE || newY >= SIZE) {
            return;
        }

        DungeonGenerator.Tile targetTile = mapData[newX][newY];

        if ("WALL".equals(targetTile.type)) {
            showMessage("Пътят е блокиран от стена!");
            return;
        }

        if ("EXIT".equals(targetTile.type)) {
            playerX = newX;
            playerY = newY;
            updateMap();
            showNextLevelDialog();
            return;
        }

        if ("DOOR".equals(targetTile.type)) {
            if (activeMonstersCount > 0) {
                showMessage("🔒 Вратата е заключена! Избийте останалите " + activeMonstersCount + " чудовища в стаята!");
                return;
            }

            Direction dir = targetTile.doorDirection;
            Room nextRoom = currentRoom.getDoors().get(dir);

            if (nextRoom != null) {
                int spawnX = SIZE / 2;
                int spawnY = SIZE / 2;

                switch (dir) {
                    case NORTH -> spawnY = SIZE - 2;
                    case SOUTH -> spawnY = 1;
                    case WEST  -> spawnX = SIZE - 2;
                    case EAST  -> spawnX = 1;
                }

                loadRoom(nextRoom, spawnX, spawnY);
                return;
            }
        }

        if ("MONSTER".equals(targetTile.type)) {
            String enemyName = targetTile.monsterName;
            this.currentEnemyX = newX;
            this.currentEnemyY = newY;
            this.currentMonster = cs.createMonster(enemyName, currentDungeonLevel);
            openTurnBasedCombatScreen();
            return;
        }

        if ("POTION".equals(targetTile.type)) {
            hero.heal(30);
            databaseService.updatePlayerHP(this.selectedPlayerClass, hero.getHp());
            mapData[newX][newY] = new DungeonGenerator.Tile("EMPTY", null, "🟩");
            updatePlayerStatsMenu();
            showMessage("Взехте лечебна отвара! +30 HP.");
            if (battleLogArea != null) battleLogArea.appendText("Намерихте отвара и възстановихте 30 HP!\n");
        }

        playerX = newX;
        playerY = newY;

        monsterAI.wanderMonstersInRoom(mapData, playerX, playerY, activeMonstersCount, (mx, my, monsterName) -> {
            this.currentEnemyX = mx;
            this.currentEnemyY = my;
            this.currentMonster = cs.createMonster(monsterName, currentDungeonLevel);
            openTurnBasedCombatScreen();
        });

        updateMap();
    }

    private void showNextLevelDialog() {
        Platform.runLater(() -> {
            levelCompleteOverlay.getChildren().clear();

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
                levelCompleteOverlay.setVisible(false);
                levelCompleteOverlay.getChildren().clear();

                currentDungeonLevel++;

                playerX = 0;
                playerY = 0;

                loadLevel(currentDungeonLevel);

                if (levelLabel != null) {
                    levelLabel.setText("Dungeon Level: " + currentDungeonLevel);
                }

                updateMap();
                isTransitioningLevel = false;

                if (mainLayout != null) {
                    mainLayout.requestFocus();
                }
            });

            victoryBox.getChildren().addAll(title, desc, nextLevelBtn);
            levelCompleteOverlay.getChildren().add(victoryBox);

            levelCompleteOverlay.setVisible(true);
            levelCompleteOverlay.toFront();
            nextLevelBtn.requestFocus();
        });
    }


    private void openTurnBasedCombatScreen() {
        VBox combatScreen = new VBox(15);
        combatScreen.setAlignment(Pos.CENTER);
        combatScreen.setPadding(new Insets(30));
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

        Label skillStatusHUD = new Label();
        if (activeSkillRounds > 0) {
            skillStatusHUD.setText("✨ ACTIVE BUFF: " + activeSkillRounds + " rounds remaining!");
            skillStatusHUD.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 15px; -fx-font-weight: bold; -fx-border-color: #2ecc71; -fx-padding: 5;");
        } else if (skillCooldown > 0) {
            skillStatusHUD.setText("⏳ SKILL COOLDOWN: " + skillCooldown + " turns left");
            skillStatusHUD.setStyle("-fx-text-fill: #e67e22; -fx-font-size: 14px; -fx-font-weight: bold;");
        } else {
            skillStatusHUD.setText("✅ Skill Ready to Use!");
            skillStatusHUD.setStyle("-fx-text-fill: #f1c40f; -fx-font-size: 14px;");
        }

        Label playerHPLabel = new Label("Your HP: " + hero.getHp() + " / " + hero.getMaxHP());
        playerHPLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 18px; -fx-font-weight: bold;");

        Label playerAtkLabel = new Label("Your ATK: " + hero.getAtk());
        playerAtkLabel.setStyle("-fx-text-fill: #3498db; -fx-font-size: 16px;");

        Label playerDefLabel = new Label("Your DEF: " + hero.getTotalDefense());
        playerDefLabel.setStyle("-fx-text-fill: #9b59b6; -fx-font-size: 16px;");

        HBox actionButtons = new HBox(15);
        actionButtons.setAlignment(Pos.CENTER);

        Button attackBtn = new Button("[ BASIC ATTACK ]");
        attackBtn.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;");

        Button healBtn = new Button("[ USE POTION ]");
        healBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;");

        String currentSkillName = databaseService.getPlayerSkillName(this.selectedPlayerClass);
        if (currentSkillName == null || currentSkillName.isEmpty()) currentSkillName = "Basic Strike";
        SkillItem skillObj = cs.loadSkillFromOntology(currentSkillName);

        String skillBtnText = "[ " + currentSkillName.toUpperCase() + " ]";
        if (activeSkillRounds > 0) {
            skillBtnText += " (ACTIVE: " + activeSkillRounds + ")";
        } else if (skillCooldown > 0) {
            skillBtnText += " (CD: " + skillCooldown + ")";
        }

        Button skillBtn = new Button(skillBtnText);
        skillBtn.setStyle("-fx-background-color: #8e44ad; -fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;");

        if (activeSkillRounds > 0 || skillCooldown > 0) {
            skillBtn.setDisable(true);
            skillBtn.setStyle("-fx-background-color: #553d67; -fx-text-fill: #888888; -fx-font-size: 15px; -fx-font-weight: bold;");
        }

        Button fleeBtn = new Button("[ FLEE ]");
        fleeBtn.setStyle("-fx-background-color: #7f8c8d; -fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;");

        attackBtn.setOnAction(e -> {
            processTurnRounds();
            String combatMessage = "FIGHT:ATTACK:" + this.selectedPlayerClass + ":" + currentMonster.getName() +
                    ":" + currentEnemyX + ":" + currentEnemyY + ":" + currentMonster.getHp() +
                    ":" + this.currentDungeonLevel;
            GUIAgent.instance.sendMessage(combatMessage);
        });

        skillBtn.setOnAction(e -> {
            currentActiveSkill = skillObj;
            if (skillObj.getActiveRounds() > 0) {
                activeSkillRounds = skillObj.getActiveRounds();
                if (sideBar != null) sideBar.appendLog("✨ Activated " + skillObj.getName() + " for " + activeSkillRounds + " rounds!");
            } else {
                skillCooldown = skillObj.getCooldown();
            }

            String combatMessage = "FIGHT:SKILL:" + this.selectedPlayerClass + ":" + currentMonster.getName() +
                    ":" + currentEnemyX + ":" + currentEnemyY + ":" + currentMonster.getHp() +
                    ":" + this.currentDungeonLevel;
            GUIAgent.instance.sendMessage(combatMessage);
        });

        healBtn.setOnAction(e -> {
            processTurnRounds();
            hero.heal(40);
            databaseService.updatePlayerHP(this.selectedPlayerClass, hero.getHp());
            updatePlayerStatsMenu();
            if (sideBar != null) sideBar.appendLog("Player used a Healing Potion and restored 40 HP!");
            openTurnBasedCombatScreen();
        });

        fleeBtn.setOnAction(e -> {
            if (sideBar != null) sideBar.appendLog("You ran away safely!");
            mainLayout.setCenter(mainStackPane);
            mainLayout.requestFocus();
        });

        actionButtons.getChildren().addAll(attackBtn, skillBtn, healBtn, fleeBtn);
        Label separator = new Label("--------------------------------------------------");

        combatScreen.getChildren().addAll(
                fightTitle, weaknessLabel, enemyAtkLabel, enemyHPLabel,
                skillStatusHUD, separator, playerHPLabel, playerAtkLabel, playerDefLabel, actionButtons
        );
        mainLayout.setCenter(combatScreen);
    }

    public void handleMonsterDefeated(int monsterX, int monsterY) {
        Platform.runLater(() -> {
            activeMonstersCount--;

            if (currentRoom.getType() == RoomType.BOSS && activeMonstersCount <= 0) {
                mapData[monsterX][monsterY] = new DungeonGenerator.Tile("EXIT", null, "🚪");
                showMessage("🎉 Босът е победен! На негово място се появи врата към следващия етаж!");
                if (sideBar != null) {
                    sideBar.appendLog("🚪 На мястото на боса се появи врата за следващия етаж!");
                }
            } else {
                mapData[monsterX][monsterY] = new DungeonGenerator.Tile("EMPTY", null, "🟩");
                playerX = monsterX;
                playerY = monsterY;

                if (activeMonstersCount <= 0) {
                    showMessage("🎉 Стаята е изчистена! Вратите са отключени!");
                }
            }

            mainLayout.setCenter(mainStackPane);
            updateMap();
            updateMiniMap();
            updatePlayerStatsMenu();
            mainLayout.requestFocus();
        });
    }

    private void updateMap() {
        if (mapData == null || tiles == null) return;

        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                if (tiles[x][y] == null || mapData[x][y] == null) continue;

                DungeonGenerator.Tile tileData = mapData[x][y];

                tiles[x][y].setText("");
                tiles[x][y].setStyle("-fx-background-color: #34495e; -fx-border-color: #2c3e50; -fx-text-fill: white;");

                if ("WALL".equals(tileData.type)) {
                    tiles[x][y].setText("⬛");
                    tiles[x][y].setStyle("-fx-background-color: #111111;");
                } else if ("MONSTER".equals(tileData.type)) {
                    tiles[x][y].setText(tileData.icon != null ? tileData.icon : "👾");
                    tiles[x][y].setStyle("-fx-background-color: #c0392b;");
                } else if ("POTION".equals(tileData.type)) {
                    tiles[x][y].setText("🧪");
                    tiles[x][y].setStyle("-fx-background-color: #27ae60;");
                } else if ("EXIT".equals(tileData.type)) {
                    tiles[x][y].setText(tileData.icon != null ? tileData.icon : "🚪");
                    tiles[x][y].setStyle("-fx-background-color: #d35400;");
                }
                else if ("DOOR".equals(tileData.type)) {
                    if (activeMonstersCount > 0) {
                        tiles[x][y].setText("🔒");
                        tiles[x][y].setStyle("-fx-background-color: #7f8c8d;");
                    } else {
                        tiles[x][y].setText("🚪");
                        tiles[x][y].setStyle("-fx-background-color: #27ae60;");
                    }
                }
            }
        }
        if (playerX >= 0 && playerX < SIZE && playerY >= 0 && playerY < SIZE) {
            if (tiles[playerX][playerY] != null) {
                tiles[playerX][playerY].setText("🧙‍♂️");
                tiles[playerX][playerY].setStyle("-fx-background-color: #f1c40f;");
            }
        }
    }


    public void handleCombatRoundResult(String status, int monsterX, int monsterY, int newEnemyHp, int newPlayerHp, String lootItem, String logMessage) {
        mainLayout.setCenter(mainStackPane);
        Platform.runLater(() -> {
            if (currentMonster != null) currentMonster.setHp(newEnemyHp);
            if (hero != null) hero.setHp(newPlayerHp);

            databaseService.updatePlayerHP(this.selectedPlayerClass, newPlayerHp);
            updatePlayerStatsMenu();

            if (sideBar != null) {
                sideBar.appendLog(logMessage);
            }

            if (status.equals("CONTINUE")) {
                openTurnBasedCombatScreen();
            } else if (status.equals("WIN")) {
                handleMonsterDefeated(monsterX, monsterY);
                if (lootItem != null && !lootItem.equals("NONE")) {
                    if (lootItem.equals("Health Potion")) {
                        if (sideBar != null) sideBar.appendLog("🎁 LOOT: Found Health Potion!");
                        showMessage("Found Health Potion!");
                    } else {
                        if (sideBar != null) sideBar.appendLog("⚔️ LOOT: Found " + lootItem + "!");
                        showEquipLootDialog(lootItem);
                    }
                    updatePlayerStatsMenu();
                }
                mainLayout.setCenter(mainStackPane);
                mainLayout.requestFocus();
            } else if (status.equals("LOSE")) {
                showMessage("GAME OVER! You were defeated.");
                showMainMenu();
            }
        });
    }

    private void showEquipLootDialog(String lootItem) {
        Platform.runLater(() -> {
            lootOverlay.getChildren().clear();

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
                if (cs.isSkill(lootItem)) {
                    databaseService.equipSkill(this.selectedPlayerClass, lootItem);
                } else {
                    cs.equipItemForHero(hero, this.selectedPlayerClass, lootItem);
                    String equipMessage = "EQUIP_ITEM:" + this.selectedPlayerClass + ":" + lootItem;
                    GUIAgent.instance.sendMessage(equipMessage);
                }
                showMessage("Equipped " + lootItem + "!");
                updatePlayerStatsMenu();

                lootOverlay.setVisible(false);
                mainLayout.requestFocus();
            });

            cancelBtn.setOnAction(e -> {
                showMessage("Предметът е запазен в инвентара.");
                lootOverlay.setVisible(false);
                mainLayout.requestFocus();
            });

            buttons.getChildren().addAll(equipBtn, cancelBtn);
            lootBox.getChildren().addAll(title, desc, buttons);

            lootOverlay.getChildren().add(lootBox);
            lootOverlay.setVisible(true);
            lootOverlay.toFront();
            equipBtn.requestFocus();
        });
    }

    private void openInventoryWindow() {
        inventoryOverlay.open(hero, selectedPlayerClass);
    }

    public void updatePlayerStatsMenu() {
        if (hero == null) return;

        String currentWeapon = databaseService.getPlayerWeapon(this.selectedPlayerClass);
        String currentSkill = databaseService.getPlayerSkillName(this.selectedPlayerClass);

        if (sideBar != null) {
            sideBar.updateStats(hero, currentDungeonLevel, currentWeapon, currentSkill);
        }
    }

    private void processTurnRounds() {
        if (activeSkillRounds > 0) {
            activeSkillRounds--;
            if (activeSkillRounds == 0) {
                if (currentActiveSkill != null) {
                    skillCooldown = currentActiveSkill.getCooldown();
                    if (sideBar != null) {
                        sideBar.appendLog("⌛ Skill " + currentActiveSkill.getName() + " expired! Cooldown started: " + skillCooldown + " turns.");
                    }
                } else {
                    skillCooldown = 3;
                }
            }
        } else if (skillCooldown > 0) {
            skillCooldown--;
            if (skillCooldown == 0 && sideBar != null) {
                sideBar.appendLog("✅ Skill is ready to use again!");
            }
        }
    }


    private void updateMiniMap() {
        if (sideBar != null && sideBar.getMiniMapWidget() != null) {
            sideBar.getMiniMapWidget().update(currentRoom, currentFloorRooms);
        }
    }

    public void showMessage(String message) {
        Platform.runLater(() -> {
            info.setText(message);
        });
    }
}

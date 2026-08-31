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

import java.awt.*;
import java.util.*;
import java.util.List;


public class GameUI extends Application {

    private MonsterAI monsterAI;
    private InventoryOverlay inventoryOverlay;

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
    private VBox sideBar;
    private TextArea battleLogArea;

    private StackPane mainStackPane;
    private StackPane levelCompleteOverlay;
    private StackPane lootOverlay;
    private boolean isGameOver = false;


    private Label hpLabel = new Label();
    private Label atkLabel = new Label();
    private Label defLabel = new Label();
    private Label weaponLabel = new Label();
    private Label armorLabel = new Label();
    private Label skillLabel = new Label();
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

    private void loadLevel(int dungeonLevel) {
        this.currentDungeonLevel = dungeonLevel;

        List<String> ontologyMonsters = List.of("Dragon", "Goblin", "Demon");
        List<String> ontologyBosses = List.of("DragonBoss", "DemonLord");

        // 1. Генериране на стаите за целия етаж
        this.currentFloorRooms = dungeonGenerator.generateFloor(currentDungeonLevel, ontologyMonsters, ontologyBosses);

        // 2. Играчът влиза в първата (START) стая в центъра ѝ
        loadRoom(currentFloorRooms.get(0), SIZE / 2, SIZE / 2);

        if (battleLogArea != null) {
            battleLogArea.appendText("--- Влизате в Етаж " + currentDungeonLevel + " (Общо стаи: " + currentFloorRooms.size() + ") ---\n");
        }
    }

    private void loadRoom(Room room, int startX, int startY) {
        this.currentRoom = room;
        this.visitedRooms.add(room);
        this.mapData = room.getGrid();
        this.playerX = startX;
        this.playerY = startY;

        activeMonstersCount = 0;
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                if ("MONSTER".equals(mapData[x][y].type)) {
                    activeMonstersCount++;
                }
            }
        }

        if (battleLogArea != null) {
            battleLogArea.appendText("Влязохте в Стая #" + room.getId() + " [" + room.getType() + "]. Остават чудовища: " + activeMonstersCount + "\n");
        }

        updateMap();
        updateMiniMap();
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

            // 1. Задаване на началния скил според избрания клас
            String startingSkill = switch (this.selectedPlayerClass) {
                case "WarriorClass" -> "SteelHelmet";
                case "ArcherClass" -> "LeatherQuiver";
                case "WizardClass" -> "ChargedLightning";
                case "AssassinClass" -> "FissureGrenade";
                default -> "SteelHelmet";
            };

            this.hero = cs.createHero(this.selectedPlayerClass, startingWeapon);

            // 2. Създаване на героя в базата данни с неговото оръжие и начален скил
            databaseService.addCustomPlayer(
                    this.selectedPlayerClass,
                    startingWeapon,
                    startingSkill,
                    hero.getHp(),
                    hero.getAtk(),
                    hero.getTotalDefense()
            );

            // 3. Записване на скила и лечебната отвара в раницата (Inventory)
            databaseService.addLootToInventory(this.selectedPlayerClass, startingSkill);
            databaseService.addLootToInventory(this.selectedPlayerClass, "Health Potion");

            // 4. Записване на началната броня в раницата според класа
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
        // 1. Инициализираме AI логиката за чудовищата
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

        sideBar = new VBox(10);
        sideBar.setPadding(new Insets(15));
        sideBar.setPrefWidth(280);
        sideBar.setStyle("-fx-background-color: #2c3e50;");

        Label statsLabel = new Label("PLAYER STATS");
        statsLabel.setStyle("-fx-text-fill: #f1c40f; -fx-font-size: 16px; -fx-font-weight: bold;");
        Label minimapTitle = new Label("🗺️ FLOOR MAP");
        minimapTitle.setStyle("-fx-text-fill: #3498db; -fx-font-size: 14px; -fx-font-weight: bold;");

        Button invBtn = new Button("🎒 INVENTORY");
        invBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold;");
        invBtn.setOnAction(e -> javafx.application.Platform.runLater(this::openInventoryWindow));

        battleLogArea = new TextArea();
        battleLogArea.setEditable(false);
        battleLogArea.setPrefHeight(250);
        battleLogArea.setWrapText(true);
        battleLogArea.setPromptText("Battle Chronolog");
        battleLogArea.setStyle("-fx-control-inner-background: #1e1e1e; -fx-text-fill: #2ecc71;");

        sideBar.getChildren().addAll(
                statsLabel,
                levelLabel,
                hpLabel,
                atkLabel,
                defLabel,
                weaponLabel,
                armorLabel,
                skillLabel,
                minimapTitle,
                miniMapGrid,
                invBtn,
                new Label("Battle Log:"),
                battleLogArea
        );

        info.setStyle("-fx-text-fill: white; -fx-padding: 10; -fx-font-size: 14px;");
        VBox bottomBox = new VBox(info);
        bottomBox.setStyle("-fx-background-color: #1a1a1a;");

        updateMap();
        updatePlayerStatsMenu();

        inventoryOverlay = new InventoryOverlay(cs, this::updatePlayerStatsMenu);

        levelCompleteOverlay = new StackPane();
        levelCompleteOverlay.setPrefSize(850, 650);
        levelCompleteOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.75);");
        levelCompleteOverlay.setVisible(false);

        lootOverlay = new StackPane();
        lootOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.75);");
        lootOverlay.setVisible(false);

        miniMapGrid.setHgap(4);
        miniMapGrid.setVgap(4);
        miniMapGrid.setAlignment(Pos.CENTER);
        miniMapGrid.setStyle("-fx-background-color: #1a1a1a; -fx-padding: 8; -fx-border-color: #34495e; -fx-border-radius: 5;");

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
            // Проверка дали остават живи чудовища
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
                isTransitioningLevel = false; // 🔓 ОСВОБОЖДАВАМЕ ДВИЖЕНИЕТО

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

        // --- ИНДИКАТОР ЗА АКТИВЕН БЪФ И КУУЛДАУН ---
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
                if (battleLogArea != null)
                    battleLogArea.appendText("✨ Activated " + skillObj.getName() + " for " + activeSkillRounds + " rounds!\n");
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
            Item potion = new Item("Health Potion", "HEAL", 40, 1);
            cs.applyItem(hero, potion);
            databaseService.updatePlayerHP(this.selectedPlayerClass, hero.getHp());
            updatePlayerStatsMenu();
            battleLogArea.appendText("Player used a Healing Potion and restored 40 HP!\n");
            openTurnBasedCombatScreen();
        });

        fleeBtn.setOnAction(e -> {
            battleLogArea.appendText("You ran away safely!\n");
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

    private void calculateAndShowSkillPreview(SkillItem skill) {
        if (skill == null) return;

        StringBuilder sb = new StringBuilder();
        sb.append("🔮 SKILL PREVIEW: ").append(skill.getName()).append("\n\n");

        if (skill.getBaseDamage() == 0 && (skill.getDamageResistance() > 0 || skill.getDamageMultiplier() <= 1.0)) {
            sb.append("🛡️ TYPE: Defensive Buff / Gear\n");
            sb.append("• Direct Damage: 0 DMG\n");
            if (skill.getDamageBonus() > 0) {
                sb.append("• Basic Attack Boost: +").append(skill.getDamageBonus()).append(" ATK per strike!\n");
            }
            if (skill.getDamageResistance() > 0) {
                sb.append("• Damage Reduction: -").append(skill.getDamageResistance()).append(" Incoming Damage\n");
            }
            sb.append("• Duration: ").append(skill.getActiveRounds()).append(" turns\n");
        } else {
            sb.append("⚔️ TYPE: Direct Attack Skill\n");
            double mult = skill.getDamageMultiplier();
            int baseAtk = (skill.getBaseDamage() > 0) ? skill.getBaseDamage() : hero.getAtk();

            if (currentMonster != null && skill.getElement() != null) {
                if (skill.getElement().equalsIgnoreCase(currentMonster.getWeakness())) {
                    mult *= 1.4;
                    sb.append("🔥 Element Weakness Match! (+40% DMG)\n");
                }
            }

            int estimatedDamage = (int) Math.round(baseAtk * mult) + skill.getDamageBonus();
            sb.append("• Estimated Damage: ~").append(estimatedDamage).append(" DMG\n");
            sb.append("• Element: ").append(skill.getElement()).append("\n");
        }

        sb.append("• Cooldown After Use: ").append(skill.getCooldown()).append(" turns");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Skill Calculator & Preview");
        alert.setHeaderText("Effect of " + skill.getName());
        alert.setContentText(sb.toString());
        alert.showAndWait();
    }


    public void handleMonsterDefeated(int monsterX, int monsterY) {
        Platform.runLater(() -> {
            activeMonstersCount--;

            if (currentRoom.getType() == RoomType.BOSS && activeMonstersCount <= 0) {
                mapData[monsterX][monsterY] = new DungeonGenerator.Tile("EXIT", null, "🚪"); // или "🪜"
                showMessage("🎉 Босът е победен! На негово място се появи врата към следващия етаж!");
                if (battleLogArea != null) {
                    battleLogArea.appendText("🚪 На мястото на боса се появи врата за следващия етаж!\n");
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

    private void unlockPortal() {
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                if (mapData[x][y].type.equals("EXIT")) {
                    mapData[x][y].icon = "🚪"; // Ако и това забива, промени го на "EXIT" или "[E]"
                }
            }
        }

        Platform.runLater(this::updateMap);

        if (battleLogArea != null) {
            battleLogArea.appendText("🎉 Всички чудовища са победени! Порталът е отключен!\n");
        }
        showMessage("Порталът е отключен!");
    }

    private void updateMap() {
        if (mapData == null || tiles == null) return;

        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                // Безопасна проверка за null
                if (tiles[x][y] == null || mapData[x][y] == null) continue;

                DungeonGenerator.Tile tileData = mapData[x][y];

                // Рестартираме базовия стил
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
        ArmorItem currentArmor = hero.getEquippedArmor();
        String armorName = (currentArmor != null) ? currentArmor.getName() : "None";
        String currentSkill = databaseService.getPlayerSkillName(this.selectedPlayerClass);
        if (currentSkill == null || currentSkill.isEmpty()) {
            currentSkill = "Basic Strike";
        }

        String weaponName = (currentWeapon != null && !currentWeapon.isEmpty()) ? currentWeapon : "None";
        String finalCurrentSkill = currentSkill;

        Platform.runLater(() -> {
            if (levelLabel != null) {
                levelLabel.setText("🏰 Floor: " + currentDungeonLevel);
                levelLabel.setStyle("-fx-text-fill: #f1c40f; -fx-font-weight: bold; -fx-font-size: 13px;");
            }
            if (hpLabel != null) {
                hpLabel.setText("❤️ HP: " + hero.getHp() + " / " + hero.getMaxHP());
                hpLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 14px;");
            }
            if (atkLabel != null) {
                atkLabel.setText("⚔️ ATK: " + hero.getAtk());
                atkLabel.setStyle("-fx-text-fill: #f1c40f; -fx-font-weight: bold; -fx-font-size: 14px;");
            }
            if (defLabel != null) {
                defLabel.setText("🛡️ DEF: " + hero.getTotalDefense());
                defLabel.setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold; -fx-font-size: 14px;");
            }
            if (weaponLabel != null) {
                weaponLabel.setText("🗡️ Weapon: " + weaponName);
                weaponLabel.setStyle("-fx-text-fill: #ecf0f1; -fx-font-size: 13px;");
            }
            if (armorLabel != null) {
                armorLabel.setText("🛡️ Armor: " + armorName);
                armorLabel.setStyle("-fx-text-fill: #ecf0f1; -fx-font-size: 13px;");
            }
            if (skillLabel != null) {
                skillLabel.setText("🔮 Skill: " + finalCurrentSkill);
                skillLabel.setStyle("-fx-text-fill: #9b59b6; -fx-font-size: 13px;");
            }
        });
    }

    private void processTurnRounds() {
        if (activeSkillRounds > 0) {
            activeSkillRounds--;
            if (activeSkillRounds == 0) {
                if (currentActiveSkill != null) {
                    skillCooldown = currentActiveSkill.getCooldown();
                    if (battleLogArea != null) {
                        battleLogArea.appendText("⌛ Skill " + currentActiveSkill.getName() + " expired! Cooldown started: " + skillCooldown + " turns.\n");
                    }
                } else {
                    skillCooldown = 3;
                }
            }
        }
        else if (skillCooldown > 0) {
            skillCooldown--;
            if (skillCooldown == 0 && battleLogArea != null) {
                battleLogArea.appendText("✅ Skill is ready to use again!\n");
            }
        }
    }


    private void updateMiniMap() {
        if (miniMapGrid == null || currentFloorRooms == null || currentFloorRooms.isEmpty()) return;

        Platform.runLater(() -> {
            miniMapGrid.getChildren().clear();

            // 1. Намираме най-малките X и Y координати на стаите на етажа
            int minX = currentFloorRooms.stream().mapToInt(Room::getGridX).min().orElse(0);
            int minY = currentFloorRooms.stream().mapToInt(Room::getGridY).min().orElse(0);

            // 2. Изчисляваме офсет, за да изместим всичко в позитивния спектър (>= 0)
            int offsetX = minX < 0 ? Math.abs(minX) : 0;
            int offsetY = minY < 0 ? Math.abs(minY) : 0;

            for (Room r : currentFloorRooms) {
                Label roomNode = new Label();
                roomNode.setPrefSize(28, 28);
                roomNode.setAlignment(Pos.CENTER);

                if (r.equals(currentRoom)) {
                    roomNode.setText("🧙‍♂️");
                    roomNode.setStyle("-fx-background-color: #f1c40f; -fx-border-color: white; -fx-border-width: 2; -fx-background-radius: 4;");
                } else if (visitedRooms.contains(r)) {
                    if (r.getType() == RoomType.BOSS) {
                        roomNode.setText("👑");
                        roomNode.setStyle("-fx-background-color: #e74c3c; -fx-background-radius: 4;");
                    } else {
                        roomNode.setText("✓");
                        roomNode.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 4;");
                    }
                } else {
                    roomNode.setText("?");
                    roomNode.setStyle("-fx-background-color: #34495e; -fx-text-fill: #7f8c8d; -fx-background-radius: 4;");
                }

                // 3. Добавяме офсета към координатите, за да са винаги 0 или по-големи
                int finalGridX = r.getGridX() + offsetX;
                int finalGridY = r.getGridY() + offsetY;

                miniMapGrid.add(roomNode, finalGridX, finalGridY);
            }
        });
    }

    public void showMessage(String message) {
        Platform.runLater(() -> {
            info.setText(message);
        });
    }
}

package org.uni.GUI;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.uni.agents.GUIAgent;
import org.uni.model.Hero;
import org.uni.model.Item;
import org.uni.model.Monster;
import org.uni.model.SkillItem;
import org.uni.service.CombatService;
import org.uni.service.DatabaseService;

public class CombatOverlay extends VBox {

    private final CombatService cs;
    private final DatabaseService db = DatabaseService.getInstance();
    private final CombatCallbacks callbacks;

    private int activeSkillRounds = 0;
    private int skillCooldown = 0;
    private SkillItem currentActiveSkill = null;

    public interface CombatCallbacks {
        void onLog(String message);
        void onStatsUpdate();
        void onCloseCombat();
    }

    public CombatOverlay(CombatService cs, CombatCallbacks callbacks) {
        super(15);
        this.cs = cs;
        this.callbacks = callbacks;

        setAlignment(Pos.CENTER);
        setPadding(new Insets(30));
        setStyle("-fx-background-color: #000000; -fx-border-color: red; -fx-border-width: 3;");
    }

    public void openCombat(Hero hero, Monster currentMonster, String selectedPlayerClass, int currentDungeonLevel, int enemyX, int enemyY) {
        getChildren().clear();

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

        String currentSkillName = db.getPlayerSkillName(selectedPlayerClass);
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
            String combatMessage = "FIGHT:ATTACK:" + selectedPlayerClass + ":" + currentMonster.getName() +
                    ":" + enemyX + ":" + enemyY + ":" + currentMonster.getHp() +
                    ":" + currentDungeonLevel;
            if (GUIAgent.instance != null) GUIAgent.instance.sendMessage(combatMessage);
        });

        skillBtn.setOnAction(e -> {
            currentActiveSkill = skillObj;
            if (skillObj.getActiveRounds() > 0) {
                activeSkillRounds = skillObj.getActiveRounds();
                if (callbacks != null) callbacks.onLog("✨ Activated " + skillObj.getName() + " for " + activeSkillRounds + " rounds!");
            } else {
                skillCooldown = skillObj.getCooldown();
            }

            String combatMessage = "FIGHT:SKILL:" + selectedPlayerClass + ":" + currentMonster.getName() +
                    ":" + enemyX + ":" + enemyY + ":" + currentMonster.getHp() +
                    ":" + currentDungeonLevel;
            if (GUIAgent.instance != null) GUIAgent.instance.sendMessage(combatMessage);
        });

        healBtn.setOnAction(e -> {
            processTurnRounds();
            Item potion = new Item("Health Potion", "HEAL", 40, 1);
            cs.applyItem(hero, potion);
            db.updatePlayerHP(selectedPlayerClass, hero.getHp());
            if (callbacks != null) {
                callbacks.onStatsUpdate();
                callbacks.onLog("Player used a Healing Potion and restored 40 HP!");
            }
            openCombat(hero, currentMonster, selectedPlayerClass, currentDungeonLevel, enemyX, enemyY);
        });

        fleeBtn.setOnAction(e -> {
            if (callbacks != null) {
                callbacks.onLog("You ran away safely!");
                callbacks.onCloseCombat();
            }
        });

        actionButtons.getChildren().addAll(attackBtn, skillBtn, healBtn, fleeBtn);
        Label separator = new Label("--------------------------------------------------");

        getChildren().addAll(
                fightTitle, weaknessLabel, enemyAtkLabel, enemyHPLabel,
                skillStatusHUD, separator, playerHPLabel, playerAtkLabel, playerDefLabel, actionButtons
        );
    }

    private void processTurnRounds() {
        if (activeSkillRounds > 0) {
            activeSkillRounds--;
            if (activeSkillRounds == 0) {
                if (currentActiveSkill != null) {
                    skillCooldown = currentActiveSkill.getCooldown();
                    if (callbacks != null) {
                        callbacks.onLog("⌛ Skill " + currentActiveSkill.getName() + " expired! Cooldown started: " + skillCooldown + " turns.");
                    }
                } else {
                    skillCooldown = 3;
                }
            }
        } else if (skillCooldown > 0) {
            skillCooldown--;
            if (skillCooldown == 0 && callbacks != null) {
                callbacks.onLog("✅ Skill is ready to use again!");
            }
        }
    }
}

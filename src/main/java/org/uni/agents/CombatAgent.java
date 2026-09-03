package org.uni.agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import org.uni.model.Monster;
import org.uni.model.SkillItem;
import org.uni.service.CombatService;
import org.uni.service.DatabaseService;
import org.uni.service.OntologyService;

import java.util.Random;


public class CombatAgent extends Agent {

    private CombatService combatService;
    private DatabaseService databaseService = DatabaseService.getInstance();
    @Override
    protected void setup() {
        this.combatService = new CombatService();
        System.out.println("Combat Agent  Started!");
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage message = receive();

                if(message == null){
                    block();
                    return;
                }

                String content = message.getContent();
                System.out.println("Combat received:" + message.getContent());
                String[] parts = content
                        .split(":");


                if (parts[0].equals("FIGHT")) {
                    executeBattleRound(message, parts);
                }
            }
        });
    }

    private void executeBattleRound(ACLMessage originalMsg, String[] parts) {

        String actionType = parts[1].trim();
        String playerClass = parts[2].trim();
        String enemyName = parts[3].trim();
        String monsterX = parts[4].trim();
        String monsterY = parts[5].trim();
        String hpPart = parts[6].trim();

        int dungeonLevel = (parts.length > 7) ? Integer.parseInt(parts[7].trim()) : 1;

        Monster tempMonster = combatService.createMonster(enemyName, dungeonLevel);

        int enemyAtk = (tempMonster != null) ? tempMonster.getAtk() : combatService.getMonsterAttackDamage(enemyName);
        if (enemyAtk <= 0) enemyAtk = 10;

        int enemyHP;
        if (hpPart.equals("START") || hpPart.isEmpty()) {
            enemyHP = (tempMonster != null) ? tempMonster.getMaxHp() : 100;
        } else {
            enemyHP = Integer.parseInt(hpPart);
        }
        if (enemyHP <= 0) enemyHP = 100;

        int playerHP = databaseService.getHP(playerClass);
        if (playerHP <= 0) playerHP = combatService.getIntProperty(playerClass, "hasHP");
        if (playerHP <= 0) playerHP = 150;

        int playerClassAtk = databaseService.getAttack(playerClass);
        if (playerClassAtk <= 0) playerClassAtk = combatService.getIntProperty(playerClass, "hasBaseDamage");
        if (playerClassAtk <= 0) playerClassAtk = 15;
        String playerWeapon = databaseService.getPlayerWeapon(playerClass);
        int weaponAtk = combatService.getWeaponBaseDamage(playerWeapon);
        int totalBaseAtk = playerClassAtk + weaponAtk;
        int finalPlayerDamage = calculateDamage(playerClass, enemyName, totalBaseAtk, actionType);

        enemyHP -= finalPlayerDamage;
        if (enemyHP < 0) enemyHP = 0;
        String skillName = getSkillName(playerClass);
        SkillItem skill = combatService.loadSkillFromOntology(skillName);

        String actionUsed = actionType.equalsIgnoreCase("SKILL")
                ? "used [" + skillName + "] on"
                : "attacked";

        String logLine = playerClass.replace("Class", "") + " " + actionUsed + " " + enemyName + " for " + finalPlayerDamage + " dmg. ";

        if (enemyHP <= 0) {
            notifyQuestAgent(enemyName);

            String droppedLoot = combatService.generateLoot(enemyName);
            System.out.println("DEBUG [CombatAgent]: Dropped loot = " + droppedLoot);

            if (!droppedLoot.equalsIgnoreCase("NONE")) {
                databaseService.addLootToInventory(playerClass, droppedLoot);
            }

            String victoryMsg = "ROUND_RESULT:WIN:" + monsterX + ":" + monsterY + ":" + enemyHP + ":" + playerHP + ":" + logLine + " Victory!:" + droppedLoot;

            sendReply(originalMsg, victoryMsg);
            return;
        }
        int playerDEF = databaseService.getDefense(playerClass);

        int actualEnemyAtk = combatService.calculateIncomingMonsterDamage(enemyAtk, playerDEF);

        playerHP -= actualEnemyAtk;
        if (playerHP < 0) playerHP = 0;
        logLine += enemyName + " attacked back for " + actualEnemyAtk + " dmg (" + playerDEF + " blocked).";

        databaseService.updatePlayerHP(playerClass, playerHP);

        if (playerHP <= 0) {
            sendReply(originalMsg, "ROUND_RESULT:LOSE:" + monsterX + ":" + monsterY + ":" + enemyHP + ":" + playerHP + ":" + logLine + " Game Over.");
        } else {
            sendReply(originalMsg, "ROUND_RESULT:CONTINUE:" + monsterX + ":" + monsterY + ":" + enemyHP + ":" + playerHP + ":" + logLine);
        }
    }

    private int activeSkillRounds = 0;
    private int calculateDamage(String playerClass, String enemyName, int currentAtk, String actionType) {
        double multiplier = 1.0;
        String element = "Physical";
        int extraDamage = 0;

        String skillName = getSkillName(playerClass);
        SkillItem skill = combatService.loadSkillFromOntology(skillName);

        if (actionType.equalsIgnoreCase("SKILL")) {
            if (skill != null) {
                if (skill.getActiveRounds() > 0) {
                    this.activeSkillRounds = skill.getActiveRounds();
                }

                if (skill.getBaseDamage() == 0 && (skill.getDamageResistance() > 0 || skill.getDamageMultiplier() <= 1.0)) {
                    System.out.println("🛡️ Activated buff/gear: " + skill.getName() + " for " + activeSkillRounds + " rounds.");
                    return 0;
                }

                multiplier = skill.getDamageMultiplier();
                element = skill.getElement();

                if (skill.getBaseDamage() > 0) {
                    currentAtk = skill.getBaseDamage();
                }

                if (skill.getDamageBonus() > 0) {
                    extraDamage += skill.getDamageBonus();
                }
            } else {
                multiplier = 1.5;
            }
        } else {
            String weaponName = databaseService.getPlayerWeapon(playerClass);
            element = combatService.getWeaponElement(weaponName);

            if (this.activeSkillRounds > 0 && skill != null) {
                if (skill.getDamageBonus() > 0) {
                    extraDamage += skill.getDamageBonus();
                    System.out.println("🔥 Applied Buff Bonus: +" + skill.getDamageBonus() + " DMG (" + activeSkillRounds + " rounds left)");
                }
                this.activeSkillRounds--;
            }
        }

        String weakness = combatService.getWeakness(enemyName);
        String resistance = combatService.getResistance(enemyName);

        if (element != null && !"Physical".equalsIgnoreCase(element) && !"None".equalsIgnoreCase(element)) {
            if (weakness != null && weakness.equalsIgnoreCase(element)) {
                multiplier *= 1.4;
                System.out.println("🔥 SUPER EFFECTIVE! Element: " + element);
            } else if (resistance != null && resistance.equalsIgnoreCase(element)) {
                multiplier *= 0.65;
                System.out.println("🛡️ Monster resists element: " + element);
            }
        }

        Random rand = new Random();
        if (rand.nextInt(100) < 15) {
            multiplier *= 1.8;
            System.out.println("💥 CRITICAL HIT!");
        }

        return (int) Math.round(currentAtk * multiplier) + extraDamage;
    }




    private String getSkillName(String playerClass) {
        String dbSkill = databaseService.getPlayerSkillName(playerClass);
        if (dbSkill != null && !dbSkill.trim().isEmpty() && !dbSkill.equalsIgnoreCase("BASIC STRIKE")) {
            return dbSkill;
        }

        String ontologyClassName = playerClass.endsWith("Class") ? playerClass : playerClass + "Class";

        String ontologySkill = combatService.getStartingSkillForClass(ontologyClassName);
        if (ontologySkill != null && !ontologySkill.equalsIgnoreCase("None") && !ontologySkill.trim().isEmpty()) {
            return ontologySkill;
        }

        ontologySkill = combatService.getStartingSkillForClass(playerClass);
        if (ontologySkill != null && !ontologySkill.equalsIgnoreCase("None") && !ontologySkill.trim().isEmpty()) {
            return ontologySkill;
        }

        return "Basic Strike";
    }
    private void sendReply(ACLMessage message, String content){
        ACLMessage reply = message.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(content);
        send(reply);
    }

    private void notifyQuestAgent(String enemy){

        ACLMessage message =
                new ACLMessage(ACLMessage.REQUEST);

        message.addReceiver(
                new AID("Quest", AID.ISLOCALNAME));

        message.setContent("COMPLETE:" + enemy);
        send(message);
    }


}


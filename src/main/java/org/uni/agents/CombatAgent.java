package org.uni.agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import org.uni.model.Monster;
import org.uni.service.CombatService;
import org.uni.service.DatabaseService;
import org.uni.service.OntologyService;

import java.util.Random;


public class CombatAgent extends Agent {

    private CombatService combatService;
    //private OntologyService ontologyService;
    private DatabaseService databaseService = DatabaseService.getInstance();
    @Override
    protected void setup() {

        this.combatService = new CombatService();
        //this.ontologyService = new OntologyService();
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
        String enemyName = parts[3].trim(); // Оправено! Вече сочи към чудовището (напр. Goblin)
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

        int basePlayerAtk = databaseService.getAttack(playerClass);
        if (basePlayerAtk <= 0) basePlayerAtk = combatService.getIntProperty(playerClass, "hasBaseDamage");
        if (basePlayerAtk <= 0) basePlayerAtk = 15;

        int finalPlayerDamage = calculateDamage(playerClass, enemyName, basePlayerAtk, actionType);

        enemyHP -= finalPlayerDamage;
        if (enemyHP < 0) enemyHP = 0;

        String actionUsed = actionType.equalsIgnoreCase("SKILL") ? getSkillName(playerClass) : "attacked";
        String logLine = playerClass.replace("Class", "") + " " + actionUsed + " " + enemyName + " for " + finalPlayerDamage + " dmg. ";

        if (enemyHP <= 0) {
            notifyQuestAgent(enemyName); // Сега ще изпрати "Goblin", а не "WarriorClass"!
            sendReply(originalMsg, "ROUND_RESULT:WIN:" + monsterX + ":" + monsterY + ":" + enemyHP + ":" + playerHP + ":" + logLine + " Victory!");
            return;
        }
        playerHP -= enemyAtk;
        if (playerHP < 0) playerHP = 0;
        logLine += enemyName + " attacked back for " + enemyAtk + " dmg.";

        databaseService.updatePlayerHP(playerClass, playerHP);

        if (playerHP <= 0) {
            sendReply(originalMsg, "ROUND_RESULT:LOSE:" + monsterX + ":" + monsterY + ":" + enemyHP + ":" + playerHP + ":" + logLine + " Game Over.");
        } else {
            sendReply(originalMsg, "ROUND_RESULT:CONTINUE:" + monsterX + ":" + monsterY + ":" + enemyHP + ":" + playerHP + ":" + logLine);
        }
    }

    private int calculateDamage(String playerClass, String enemyName, int currentAtk, String actionType) {
        double multiplier = 1.0;

        // 1. Умението прави +65% базови щети
        if (actionType.equalsIgnoreCase("SKILL")) {
            multiplier *= 1.65;
        }

        // 2. Вземаме елементите
        String weakness = combatService.getWeakness(enemyName);
        String toughness = combatService.getBehavior(enemyName);
        String playerWeaponElement = databaseService.getPlayerWeapon(playerClass);

        if (playerWeaponElement == null || playerWeaponElement.isEmpty()) {
            if (playerClass.contains("Mage")) playerWeaponElement = "Fire";
            else playerWeaponElement = "None";
        }

        boolean hasElement = !playerWeaponElement.equalsIgnoreCase("None");
        boolean isWeakAgainst = hasElement && weakness != null && weakness.equalsIgnoreCase(playerWeaponElement);
        boolean isToughAgainst = hasElement && toughness != null && toughness.equalsIgnoreCase(playerWeaponElement);

        // 3. Елементално предимство
        if (isWeakAgainst && !isToughAgainst) {
            multiplier *= 1.4; // +40% щети
            System.out.println("🔥 SUPER EFFECTIVE! Weapon element: " + playerWeaponElement);
        } else if (!isWeakAgainst && isToughAgainst) {
            multiplier *= 0.65; // -35% щети
            System.out.println("🛡️ Monster resists element " + playerWeaponElement);
        }

        // 4. Шанс за Критичен удар (15% шанс за x1.8)
        Random rand = new Random();
        if (rand.nextInt(100) < 15) {
            multiplier *= 1.8;
            System.out.println("💥 CRITICAL HIT!");
        }

        return (int) Math.round(currentAtk * multiplier);
    }


    private String getSkillName(String playerClass) {
        if (playerClass.contains("Warrior")) return "used [SHIELD SLAM] on";
        if (playerClass.contains("Mage")) return "cast [FIREBALL] at";
        if (playerClass.contains("Rogue")) return "executed [SHADOW STRIKE] on";
        return "used a special skill on";
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


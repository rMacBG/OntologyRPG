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
        String playerClass = parts[1].trim();
        String enemyName = parts[2].trim();
        String monsterX = parts[3].trim();
        String monsterY = parts[4].trim();
        String hpPart = parts[5].trim();

        int dungeonLevel = (parts.length > 6) ? Integer.parseInt(parts[6].trim()) : 1;

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

        int finalPlayerDamage = calculateDamage(playerClass, enemyName, basePlayerAtk);

        enemyHP -= finalPlayerDamage;
        if (enemyHP < 0) enemyHP = 0;

        String logLine = playerClass.replace("Class", "") + " hit " + enemyName + " for " + finalPlayerDamage + " dmg. ";

        if (enemyHP <= 0) {
            notifyQuestAgent(enemyName);
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

    private int calculateDamage(String playerClass, String enemyName, int currentAtk) {
        String weakness = combatService.getWeakness(enemyName);
        String toughness = combatService.getBehavior(enemyName);
        String playerWeaponElement = databaseService.getPlayerWeapon(playerClass);
        if (playerWeaponElement == null) playerWeaponElement = "None";

        Random rand = new Random();
        boolean hasElement = !playerWeaponElement.equals("None") && !playerWeaponElement.isEmpty();
        boolean isWeakAgainst = hasElement && (weakness != null && weakness.contains(playerWeaponElement));
        boolean isToughAgainst = hasElement && (toughness != null && toughness.contains(playerWeaponElement));

        if (isWeakAgainst && !isToughAgainst) {
            currentAtk = (int) (currentAtk * 1.35);
            if (rand.nextInt(20) == 0) {
                currentAtk = (int) (currentAtk * 2.1);
                System.out.println("CRITICAL HIT! Damage multiplied: " + currentAtk);
            }
        }
        else if (!isWeakAgainst && isToughAgainst) {
            currentAtk = (int) (currentAtk * 0.66);
        }

        return currentAtk;
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


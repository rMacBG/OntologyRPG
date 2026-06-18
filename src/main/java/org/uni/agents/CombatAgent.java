package org.uni.agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import org.uni.service.CombatService;
import org.uni.service.DatabaseService;
import org.uni.service.OntologyService;


public class CombatAgent extends Agent {

    private CombatService combatService;
    private OntologyService ontologyService;
    private DatabaseService databaseService;
    @Override
    protected void setup() {
        System.out.println("Combat Agent  Started!");

        combatService = new CombatService();
        ontologyService = new OntologyService();
        databaseService = new DatabaseService();
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage message = receive();

                if(message == null){
                    block();
                    return;
                }
                String content = message.getContent();

                String[] parts = content
                        .split(":");

                if(parts[0].equals("FIGHT")){
                    String player = parts[1];
                    String enemy = parts[2];

                    String result = simulateFight(player, enemy);

                    sendReply(
                            message,
                            "RESULT:" + result
                    );
                }
            }
        });
        super.setup();
    }
    private void sendReply(ACLMessage message, String content){
        ACLMessage reply = message.createReply();
        reply.setContent(content);

        send(reply);
    }

    private String simulateFight(String player, String enemy){

//        String weapon = ontologyService.getPropertyValue(player, "usesWeapon");
//        if (weapon == null){
//            return "no weapon equipped";
//        }
//
//
//        String behavior = combatService.getBehavior(enemy);
//        String attack = combatService.getAttack(enemy);
//        String weakness = combatService.getWeakness(enemy);
//
//        if(weapon.contains(weakness)){
//            return player + " has an advantage against " + enemy;
//        }

//        return "Enemy: " + enemy +
//               "\nBehavior: " + behavior +
//               "\nAttack:" + attack +
//               "\nWeakness" + weakness;
//
//
//        return player + " fights " + enemy;

        int playerHP = databaseService.getHP(player);
        int enemyHP = databaseService.getHP(enemy);
        int playerAtk = databaseService.getAttack(player);
        int enemyAtk = databaseService.getAttack(enemy);

        while(playerHP > 0 && enemyHP > 0){
            enemyHP -= playerAtk;

            if(enemyHP <= 0){
                return player + " defeated " + enemy;
            }

            playerHP -= enemyAtk;


        }
        return enemy + " defeated " + player;
    }
}

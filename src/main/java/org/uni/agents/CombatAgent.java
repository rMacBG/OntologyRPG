package org.uni.agents;

import jade.core.AID;
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
                System.out.println("Combat received: " + message.getContent());
                String[] parts = content
                        .split(":");

                if(parts[0].equals("FIGHT")){
                    String player = parts[1].trim();
                    String enemy = parts[2].trim();

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

    private void notifyQuestAgent(String enemy){

        ACLMessage message =
                new ACLMessage(ACLMessage.REQUEST);

        message.addReceiver(
                new AID("Quest", AID.ISLOCALNAME));

        message.setContent("COMPLETE:" + enemy);
        send(message);
    }

    private String simulateFight(String player, String enemy){

        int playerHP = databaseService.getHP(player);
        int enemyHP = databaseService.getHP(enemy);
        int playerAtk = databaseService.getAttack(player);
        int enemyAtk = databaseService.getAttack(enemy);

        String behavior = ontologyService.getPropertyValue(enemy, "hasBehavior");
        String attack = ontologyService.getPropertyValue(enemy, "usesAttack");
        String weakness = ontologyService.getPropertyValue(enemy, "weakAgainst");

        StringBuilder battlelog = new StringBuilder();

        battlelog.append("Enemy: ")
                .append(enemy)
                .append("\n");
        battlelog.append("Behavior: ")
                .append(behavior)
                .append("\n");
        battlelog.append("Attack: ")
                .append(attack)
                .append("\n");
        battlelog.append("Weakness: ")
                .append(weakness)
                .append("\n");




        while(playerHP > 0 && enemyHP > 0){
            enemyHP -= playerAtk;
            battlelog.append(player)
                    .append(" hits ")
                    .append(enemy)
                    .append("\n");


            if(enemyHP <= 0){
                battlelog.append(player)
                        .append(" defeated ")
                        .append(enemy);
                String result = battlelog.toString();
                databaseService.addBattle(player, enemy, result);

                notifyQuestAgent(enemy);
                return result;
            }

            playerHP -= enemyAtk;

            battlelog.append(enemy)
                    .append(" attacks ")
                    .append(player)
                    .append("\n");


        }

        battlelog.append(enemy)
                .append(" defeated ")
                .append(player)
                .append("\n");

        battlelog.append(player)
                .append(" lost the battle against")
                .append(enemy);


        String result = battlelog.toString();

        databaseService.addBattle(player, enemy, result);


        return result;
    }
}

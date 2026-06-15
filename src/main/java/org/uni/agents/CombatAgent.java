package org.uni.agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import org.uni.service.CombatService;

public class CombatAgent extends Agent {

    private CombatService combatService;


    @Override
    protected void setup() {
        System.out.println("Combat Agent  Started!");

        combatService = new CombatService();

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage message = receive();

                if(message == null){
                    block();
                    return;
                }

                String[] parts = message.getContent()
                        .split(":");

                if(parts[0].equals("FIGHT")){
                    String enemy = parts[1];

                    String result = simulateFight(enemy);

                    sendReply(
                            message,
                            result
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

    private String simulateFight(String enemy){
        String behavior = combatService.getBehavior(enemy);
        String attack = combatService.getAttack(enemy);
        String weakness = combatService.getWeakness(enemy);

        return "Enemy: " + enemy +
               "\nBehavior: " + behavior +
               "\nAttack:" + attack +
               "\nWeakness" + weakness;



    }
}

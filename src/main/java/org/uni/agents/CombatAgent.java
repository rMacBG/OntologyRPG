package org.uni.agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class CombatAgent extends Agent {
    @Override
    protected void setup() {
        System.out.println("Combat Agent  Started!");

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
            if(enemy.equals("Dragon")){
                return "Dragon has been defeated using fire";
            }

            return "Dragon is defeated! Player won the fight";
    }
}

package org.uni.agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class PlayerAgent extends Agent {


    @Override
    protected void setup() {


        System.out.println("Player Agent started");

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage message = receive();
                        if(message == null){
                            block();
                            return;
                        }

                        String content = message.getContent();
                System.out.println("Player received:" + content);
                String[] parts = content.split(":");
                if (parts[0].equals("FIGHT")) {
                    String enemy = parts[1];
                    fight(enemy);

                    ACLMessage reply = message.createReply();

                    reply.setContent("Started battle with " + enemy);
                    send(reply);
                }
                else if(parts[0].equals("RESULT")){
                    ACLMessage guiMessage = new ACLMessage(ACLMessage.INFORM);
                    guiMessage.addReceiver(new AID("GUI", AID.ISLOCALNAME));
                    guiMessage.setContent(content);
                    send(guiMessage);
                }

            }
        });

        super.setup();
    }

    public void fight(String enemy){
        ACLMessage message = new ACLMessage(ACLMessage.REQUEST);
        message.addReceiver(new AID("Combat",AID.ISLOCALNAME));
        message.setContent("FIGHT:Player: " + enemy);
        send(message);
    }
}

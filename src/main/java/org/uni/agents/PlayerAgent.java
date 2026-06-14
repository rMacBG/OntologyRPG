package org.uni.agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class PlayerAgent extends Agent {

    @Override
    protected void setup() {

        System.out.println("Player agent started.");

        ACLMessage message = new ACLMessage(ACLMessage.REQUEST);

        message.addReceiver(
                new AID("Rpg", AID.ISLOCALNAME));
        message.addReceiver(
                new AID("Quest", AID.ISLOCALNAME));

        message.setContent("GET_WARRIORS");

        send(message);

        System.out.println("Request sent.");

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage reply = receive();
                        if(reply == null){
                            block();
                            return;
                        }

                System.out.println("Response received" + reply.getContent());
            }
        });

        super.setup();
    }
}

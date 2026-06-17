package org.uni.agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class GUIAgent extends Agent {
    @Override
    protected void setup() {
        System.out.println("GUI Agent Started!");

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage message = receive();

                if(message == null){
                    block();
                    return;
                }

                String[] parts = message.getContent().split(":");
                if (parts[0].equals("FIGHT")){
                ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
                request.addReceiver(new AID("Player", AID.ISLOCALNAME));

                request.setContent("FIGHT:" + parts[1]);
                send(request);
                }
            }
        });
        super.setup();
    }
}

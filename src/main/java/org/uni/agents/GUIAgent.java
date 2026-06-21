package org.uni.agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import org.uni.GUI.GameUI;

public class GUIAgent extends Agent {

    public static GUIAgent instance;
    public static GameUI ui;

    @Override
    protected void setup() {
        instance = this;

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
                if(parts[0].equals("RESULT")){
                    if (GameUI.instance != null){
                        GameUI.instance.showMessage(message.getContent());
                    }
            }
            }
        });
        super.setup();
    }

    public void sendMessage(String content){
        ACLMessage message = new ACLMessage(ACLMessage.REQUEST);
        message.addReceiver(new AID("Player", AID.ISLOCALNAME));
        message.setContent(content);

        send(message);
    }
}

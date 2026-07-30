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
                    String x = parts[2];
                    String y = parts[3];
                    String currentEnemyHP;
                    if (parts.length > 4) {
                        currentEnemyHP = parts[4];
                    } else {
                        currentEnemyHP = "START";
                    }
                    fight(enemy, x, y, currentEnemyHP);
                }
                else if (parts[0].equals("ROUND_RESULT")) {
                    ACLMessage guiMessage = new ACLMessage(ACLMessage.INFORM);
                    guiMessage.addReceiver(new AID("GUI", AID.ISLOCALNAME));

                    guiMessage.setContent(content);
                    send(guiMessage);
                }

            }
        });

        super.setup();
    }

    public void fight(String enemy, String x, String y, String currentEnemyHP) {
        System.out.println("Sent to Combat");
        org.uni.service.DatabaseService db = org.uni.service.DatabaseService.getInstance();

        String activePlayerClass = "WarriorClass";
        if (db.getHP("WarriorClass") > 0) activePlayerClass = "WarriorClass";
        else if (db.getHP("WizardClass") > 0) activePlayerClass = "WizardClass";
        else if (db.getHP("ArcherClass") > 0) activePlayerClass = "ArcherClass";
        else if (db.getHP("RogueClass") > 0) activePlayerClass = "RogueClass";
        ACLMessage message = new ACLMessage(ACLMessage.REQUEST);
        message.addReceiver(new AID("Combat", AID.ISLOCALNAME));
        message.setContent("FIGHT:" + activePlayerClass + ":" + enemy + ":" + x + ":" + y + ":" + currentEnemyHP);
        send(message);
    }

}

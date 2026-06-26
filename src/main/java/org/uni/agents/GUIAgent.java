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
                if(parts[0].equals("ROUND_RESULT")){
                    if (GameUI.instance != null) {
                        String status = parts[1].trim();
                        int monsterX = Integer.parseInt(parts[2].trim());
                        int monsterY = Integer.parseInt(parts[3].trim());
                        int enemyHp = Integer.parseInt(parts[4].trim());
                        int playerHp = Integer.parseInt(parts[5].trim());
                        String logMsg = parts[6].trim();
                        System.out.println("--> Values to UI: EnemyHP=" + enemyHp + ", PlayerHP=" + playerHp);

                        javafx.application.Platform.runLater(() -> {
                            GameUI.instance.handleCombatRoundResult(status, monsterX, monsterY, enemyHp, playerHp, logMsg);
                        });
                    }
                    else {
                        javafx.application.Platform.runLater(() -> {
                            GameUI.instance.showMessage("You lost");
                        });
                        System.out.println("CRITICAL: GameUI.instance is NULL! Cannot update screen.");
                    }
                }
            }

        });
        super.setup();
    }

    public void sendMessage(String content){
        ACLMessage message = new ACLMessage(ACLMessage.REQUEST);
        message.addReceiver(new AID("Combat", AID.ISLOCALNAME));
        message.setContent(content);

        send(message);
    }
}

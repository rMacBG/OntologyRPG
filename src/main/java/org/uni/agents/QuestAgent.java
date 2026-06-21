package org.uni.agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import org.uni.service.DatabaseService;
import org.uni.service.OntologyService;


public class QuestAgent extends Agent {
    private OntologyService ontologyService;
    private DatabaseService databaseService;

    @Override
    protected void setup() {

        System.out.println("Quest Agent Started.");

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
                String parts[] = message.getContent()
                        .split(":");

                if (parts[0].equals("GET_QUESTS")){
                    var quests = ontologyService.getIndividualsByClass("Quest");

                    sendReply(message, quests.toString());
                }
                else if(parts[0].equals("GET_DAILY")){
                    var quests = ontologyService.getIndividualsByClass("DailyQuest");

                    sendReply(message, quests.toString());
                }
                else if(parts[0].equals("GET_TARGET")){
                    String questName = parts[1];

                    var target = ontologyService.getPropertyValue(questName, "targetEnemy");

                    sendReply(message, target);

                }
                else if(parts[0].equals("COMPLETE")){
                    String enemy = parts[1];
                    if (enemy.equals("Dragon")){
                        databaseService.addGold(
                                "Player",
                                100
                        );
                    }
                    System.out.println("Quest progress: killed " + enemy);
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
}

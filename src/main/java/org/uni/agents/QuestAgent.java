package org.uni.agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import org.uni.service.OntologyService;

public class QuestAgent extends Agent {
    private OntologyService ontologyService;

    @Override
    protected void setup() {

        System.out.println("Quest Agent Started.");

        ontologyService = new OntologyService();

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

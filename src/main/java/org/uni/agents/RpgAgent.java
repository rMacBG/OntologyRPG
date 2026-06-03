package org.uni.agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import org.uni.service.OntologyService;


public class RpgAgent extends Agent {

    private OntologyService ontologyService;

    @Override
    protected void setup() {

        System.out.println("RPG Agent Started.");

        ontologyService = new OntologyService();

//        System.out.println(
//                ontologyService.getIndividualsByClass("Warrior")
//        );

        addBehaviour(new CyclicBehaviour(){
            @Override
            public void action() {
                ACLMessage message = receive();

                if(message == null){
                    block();
                    return;
                }

                System.out.println("Received: " + message.getContent());

                    String[] parts = message.getContent().split(":");


                if(parts.length < 2) {
                    return;
                }
                    if(parts[0].equals("GET_CLASS")){
                        String className = parts[1];
                        var result = ontologyService.getIndividualsByClass(className);
                        ACLMessage reply =
                                message.createReply();

                        reply.setContent(result.toString());

                        send(reply);
                    }
                    else if(parts[0].equals("GET_TYPES")){
                        String individualName = parts[1];

                        var result = ontologyService.getInferredTypes(individualName);

                        ACLMessage reply = message.createReply();

                        reply.setContent(result.toString());

                        send(reply);
                    }
                }
        });
    }
}

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
            }
        });



        super.setup();


    }
}

package org.uni.agents;

import jade.core.Agent;
import org.uni.service.OntologyService;

public class RpgAgent extends Agent {

    private OntologyService ontologyService;

    @Override
    protected void setup() {

        System.out.println("RPG Agent Started.");

        ontologyService = new OntologyService();

        System.out.println(
                ontologyService.getIndividualsByClass("Warrior")
        );
        super.setup();


    }
}

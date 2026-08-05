package org.uni.agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import org.uni.service.DatabaseService;
import org.uni.service.OntologyService;

import javax.xml.crypto.Data;


public class RpgAgent extends Agent {

    private OntologyService ontologyService;
    private DatabaseService databaseService;
    @Override
    protected void setup() {

        System.out.println("RPG Agent Started.");

        ontologyService = new OntologyService();
        databaseService = new DatabaseService();


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

                        sendReply(message, result.toString());
                    }
                    else if(parts[0].equals("GET_TYPES")){
                        String individualName = parts[1];
                        var result = ontologyService.getInferredTypes(individualName);

                        sendReply(message, result.toString());
                    }
                    else if(parts[0].equals("GET_PROPERTIES")){
                        String individualName = parts[1];
                        String propertyName = parts[2];
                        var result = ontologyService.getPropertiesOfIndividual(individualName, propertyName);

                        sendReply(message, result.toString());
                    }
                    else if (parts[0].equals("CREATE")){
                        String className = parts[1];
                        String individualName = parts[2];
                        ontologyService.addIndividual(className, individualName);

                        sendReply(message, "Created" + individualName);
                    }
                    else if(parts[0].equals("DELETE")){
                        String individualName = parts[1];
                        ontologyService.deleteIndividual(individualName);

                        sendReply(message, "Deleted" + individualName);
                    }

                    else if(parts[0].equals("ADD_PROPERTY")) {

                        String subject = parts[1];
                        String property = parts[2];
                        String object = parts[3];

                        ontologyService.addPropertyToIndividual(subject, property, object
                        );

                        sendReply(message, "Property added");
                    }
                    else if(parts[0].equals("REMOVE_PROPERTY")) {

                        String subject = parts[1];
                        String property = parts[2];
                        String object = parts[3];
                        ontologyService.removePropertyFromIndividual(subject, property, object);

                        sendReply(message, "Property removed");
                    }
                    else if(parts[0].equals("CREATE_PLAYER")){
                        String username = parts[1];

                        databaseService.addPlayer(username);

                        sendReply(message, "Player created");
                    }
                    else if(parts[0].equals("GET_PLAYER")){
                        String username = parts[1];

                        String result =
                                databaseService.getPlayer(username);

                        sendReply(message, result);
                    }
                    else if(parts[0].equals("ADD_GOLD")){
                        String username = parts[1];
                        int gold = Integer.parseInt(parts[2]);

                        databaseService.addGold(
                                username,
                                gold
                        );


                        sendReply(message, "Gold added!");
                    }
                    else if (parts[0].equals("DELETE_PLAYER")){
                        String username = parts[1];

                        databaseService.deletePlayer(username);

                        sendReply(message, "Player deleted.");
                    }
                    else if (parts[0].equals("EQUIP_WEAPON")) {
                        if (parts.length >= 3) {
                            String playerClass = parts[1];
                            String newWeaponName = parts[2];

                            databaseService.equipWeapon(playerClass, newWeaponName);
                            sendReply(message, "EQUIPPED_SUCCESS:" + playerClass + ":" + newWeaponName);
                        }

                    }
                }
        });
    }

    private void sendReply(ACLMessage message, String content) {

        ACLMessage reply = message.createReply();
        reply.setContent(content);
        send(reply);
    }
}

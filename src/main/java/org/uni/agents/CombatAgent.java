package org.uni.agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import org.uni.service.CombatService;
import org.uni.service.OntologyService;

public class CombatAgent extends Agent {

    private CombatService combatService;
    private OntologyService ontologyService;

    @Override
    protected void setup() {
        System.out.println("Combat Agent  Started!");

        combatService = new CombatService();
        ontologyService = new OntologyService();
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage message = receive();

                if(message == null){
                    block();
                    return;
                }

                String[] parts = message.getContent()
                        .split(":");

                if(parts[0].equals("FIGHT")){
                    String player = parts[1];
                    String enemy = parts[2];

                    String result = simulateFight(player, enemy);

                    sendReply(
                            message,
                            result
                    );
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

    private String simulateFight(String player, String enemy){

        String weapon = ontologyService.getPropertyValue(player, "usesWeapon");
        if (weapon == null){
            return "no weapon equipped";
        }


        String behavior = combatService.getBehavior(enemy);
        String attack = combatService.getAttack(enemy);
        String weakness = combatService.getWeakness(enemy);

        if(weapon.contains(weakness)){
            return player + " has an advantage against " + enemy;
        }

//        return "Enemy: " + enemy +
//               "\nBehavior: " + behavior +
//               "\nAttack:" + attack +
//               "\nWeakness" + weakness;


        return player + " fights " + enemy;
    }
}

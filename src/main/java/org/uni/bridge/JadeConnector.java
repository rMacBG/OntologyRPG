package org.uni.bridge;


import jade.core.AID;
import jade.core.AgentContainer;
import jade.lang.acl.ACLMessage;

public class JadeConnector {

    private static AgentContainer container;

    public static void setContainer(AgentContainer mainContainer){

            container = mainContainer;
    }

    public static void send(String content){

        ACLMessage message = new ACLMessage(ACLMessage.REQUEST);
        message.addReceiver(new AID("GUI", AID.ISLOCALNAME));
        message.setContent(content);

        System.out.println("GUI sending: " + content);

    }
}

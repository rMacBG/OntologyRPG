package org.uni.fmi;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import org.uni.agents.PlayerAgent;
import org.uni.agents.QuestAgent;
import org.uni.agents.RpgAgent;

public class Main {
    public static void main(String[] args) {
       Runtime runtime = Runtime.instance();

       Profile profile = new ProfileImpl();
       profile.setParameter(Profile.MAIN_HOST, "localhost");
       profile.setParameter(Profile.MAIN_PORT, "1999");
       profile.setParameter(Profile.GUI, "true");

        AgentContainer mainContainer = runtime.createMainContainer(profile);

        AgentController Rpg;
        AgentController Quest;
        try{
            Rpg = mainContainer.createNewAgent("Rpg", RpgAgent.class.getName(), null);
            Quest = mainContainer.createNewAgent("Quest", QuestAgent.class.getName(), null);
            AgentController client = mainContainer.createNewAgent("Player", PlayerAgent.class.getName(), null);

            Rpg.start();
            Quest.start();
            client.start();
        } catch (StaleProxyException e){
            e.printStackTrace();
        }


    }
}
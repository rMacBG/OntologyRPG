package org.uni.fmi;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import javafx.application.Application;
import org.uni.GUI.GameUI;
import org.uni.agents.*;

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
        AgentController Combat;
        AgentController Gui;
        AgentController MonsterMovement;
        try{
            Rpg = mainContainer.createNewAgent("Rpg", RpgAgent.class.getName(), null);
            Quest = mainContainer.createNewAgent("Quest", QuestAgent.class.getName(), null);
            Combat = mainContainer.createNewAgent("Combat", CombatAgent.class.getName(), null);
            Gui = mainContainer.createNewAgent("Gui", GUIAgent.class.getName(),null);
            MonsterMovement = mainContainer.createNewAgent("Monster", MonsterAgent.class.getName(), null);
            AgentController client = mainContainer.createNewAgent("Player", PlayerAgent.class.getName(), null);


            Rpg.start();
            Quest.start();
            Combat.start();
            Gui.start();
            MonsterMovement.start();
            client.start();
            new Thread(() -> {
                Application.launch(GameUI.class, args);
            }).start();
        } catch (StaleProxyException e){
            e.printStackTrace();
        }


    }
}
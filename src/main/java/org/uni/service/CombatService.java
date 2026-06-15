package org.uni.service;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;

import java.io.FileInputStream;
import java.io.InputStream;

public class CombatService {

    private static final String PATH = "../ontology/CombatOntology.owl";
    private static final String BASE = "http://www.semanticweb.org/combat";
    private static final String NS = "#";

    private OntModel model;

    public CombatService(){
        loadOntology();
    }

    private void loadOntology(){
        model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_RULE_INF);

        try(InputStream in = new FileInputStream(PATH)){
            model.read(in, BASE);

            System.out.println("Combat Ontology loaded!");
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public String getWeakness(String monsterName){
        var monster = model.getIndividual(NS + monsterName);

        if(monster == null){
            return "Unknown";
        }

        var property = model.getProperty(NS + "weakAgainst");

        var value = monster.getPropertyValue(property);

        if(value == null){
            return "None";
        }
        return value.asResource().getLocalName();
    }

    public String getAttack(String monsterName){
        var monster = model.getIndividual(NS + monsterName);
        var property = model.getProperty(NS + "usesAttack");
        var value = monster.getPropertyValue(property);

        if(value == null){
            return "None";
        }

        return value.asResource().getLocalName();
    }

    public String getBehavior(String monsterName){
        var monster = model.getIndividual(NS + monsterName);

        var property = model.getProperty(NS + "hasBehavior");

        var value = monster.getPropertyValue(property);

        if(value == null){
            return "None";

        }

        return value.asResource().getLocalName();
    }
}

package org.uni.service;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class CombatService {

    private static final String PATH = "ontology/CombatOntology.rdf";
    private static final String BASE = "http://www.semanticweb.org/vlady/ontologies/2026/5/Combat_Ontology";
    private static final String NS = BASE + "#";

    private OntModel model;

    public CombatService(){
        loadOntology();
    }

    private void loadOntology(){
        model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_RULE_INF);
        var docManager = model.getDocumentManager();
        docManager.addAltEntry(
                "http://www.semanticweb.org/vlady/ontologies/2026/4/RPG-game-ontology",
                "classpath:ontology/RPGGameOntology.rdf");
        try(InputStream in = getClass()
                .getClassLoader()
                .getResourceAsStream(PATH)){
            model.read(in, BASE, "RDF/XML");
            System.out.println("Combat Ontology loaded!");
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    public String getWeakness(String monsterName){
        var monster = model.getOntClass(NS + monsterName);

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
        var monster = model.getOntClass(NS + monsterName);
        var property = model.getProperty(NS + "usesAttack");
        var value = monster.getPropertyValue(property);

        if(value == null){
            return "None";
        }

        return value.asResource().getLocalName();
    }

    public String getBehavior(String monsterName){
        var monster = model.getOntClass(NS + monsterName);

        var property = model.getProperty(NS + "hasBehavior");

        var value = monster.getPropertyValue(property);

        if(value == null){
            return "None";

        }

        return value.asResource().getLocalName();
    }
}

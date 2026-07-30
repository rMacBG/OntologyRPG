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
    private static final String BASE = "http://www.semanticweb.org/vlady/ontologies/2026/5/Combat_Ontology/";
    private static final String NS = BASE + "#";


    private OntModel model;

    public CombatService() {
        loadOntology();
    }

    private void loadOntology() {
        model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
        var docManager = model.getDocumentManager();
        docManager.addAltEntry(
                "http://www.semanticweb.org/vlady/ontologies/2026/4/RPG-game-ontology",
                "classpath:ontology/RPGGameOntology.rdf");
        try (InputStream in = getClass()
                .getClassLoader()
                .getResourceAsStream(PATH)) {
            model.read(in, BASE, "RDF/XML");
            System.out.println("Combat Ontology loaded!");
        } catch (Exception e) {
            e.printStackTrace();
        }
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("ontology/RPGGameOntology.rdf")) {
            if (in != null) {
                String rpgBase = "http://www.semanticweb.org/vlady/ontologies/2026/4/RPG-game-ontology";
                model.read(in, rpgBase, "RDF/XML");
                //System.out.println("RPG Game Ontology successfully merged into CombatService!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public String getAttack(String monsterName) {
        if (monsterName == null) return "None";

        String attackName = getStringProperty(monsterName, "usesAttack");
        System.out.println("-> No such attack found " + monsterName + ": " + attackName);
        return attackName;
    }

    public String getWeakness(String monsterName) {
        if (model == null || monsterName == null) return "Unknown";

        var monster = model.getOntClass(NS + monsterName);

        if (monster == null) {
            var individual = model.getIndividual(NS + monsterName);
            if (individual != null) {
                var property = model.getProperty(NS + "weakAgainst");
                var value = individual.getPropertyValue(property);
                if (value != null) return value.asResource().getLocalName();
            }
            return "Unknown";
        }

        var property = model.getProperty(NS + "weakAgainst");
        var value = monster.getPropertyValue(property);

        if (value == null) {
            return "None";
        }
        return value.asResource().getLocalName();
    }

    public int getMonsterHP(String monsterName) {
        if(monsterName == null) return 100;
        int hp = getIntProperty(monsterName, "hasHP");
        return hp > 0 ? hp : 100;
    }

    public int getIntProperty(String entityName, String propertyName) {
        if (model == null || entityName == null || propertyName == null) {
            return 0;
        }

        String combatNS = "http://www.semanticweb.org/vlady/ontologies/2026/5/Combat_Ontology#";
        String rpgNS = "http://www.semanticweb.org/vlady/ontologies/2026/4/RPG-game-ontology#";


        var entity = model.getIndividual(combatNS + entityName);
        if (entity == null) entity = model.getIndividual(BASE + entityName);
        if (entity == null) entity = model.getIndividual(rpgNS + entityName);

        if (entity == null) {
            System.out.println("Individual [" + entityName + "] not found!");
            return 0;
        }


        var stmtIter = entity.listProperties();
        while (stmtIter.hasNext()) {
            var stmt = stmtIter.nextStatement();
            var pred = stmt.getPredicate();


            //String predString = (pred != null) ? pred.toString() : "";

                String subjStr = stmt.getSubject().getURI();
                String predStr = stmt.getPredicate().getLocalName();

                if (subjStr != null && subjStr.toLowerCase().endsWith("#" + entityName.toLowerCase())) {
                    if (predStr != null && predStr.equalsIgnoreCase(propertyName)) {
                        try {
                            return stmt.getObject().asLiteral().getInt();
                        } catch (Exception e) {

                        }
                    }
                }
            }

        //System.out.println("No property found " + propertyName + " for " + entityName);
        return 0;
    }

    public int getMonsterAttackDamage(String monsterName) {
        if (monsterName == null) return 10;

        String attackName = getAttack(monsterName);
        int damage = 0;


        if (attackName != null && !attackName.equalsIgnoreCase("None")) {
            damage = getIntProperty(attackName, "hasBaseDamage");
        }

        if (damage <= 0) {
            damage = getIntProperty(monsterName, "hasBaseDamage");
        }
        return damage;
    }

    public String getBehavior(String monsterName) {
        if (model == null || monsterName == null) return "None";

        var monster = model.getOntClass(NS + monsterName);

        if (monster == null) {
            var individual = model.getIndividual(NS + monsterName);
            if (individual != null) {
                var property = model.getProperty(NS + "hasBehavior");
                var value = individual.getPropertyValue(property);
                if (value != null) return value.asResource().getLocalName();
            }
            return "None";
        }

        var property = model.getProperty(NS + "hasBehavior");
        var value = monster.getPropertyValue(property);

        if (value == null) {
            return "None";
        }

        return value.asResource().getLocalName();
    }

    public String getStringProperty(String entityName, String propertyName) {
        if (model == null || entityName == null || propertyName == null) return "None";


        try {
            var individual = model.getIndividual(NS + entityName);
            if (individual != null) {
                var prop = model.getProperty(NS + propertyName);
                var val = individual.getPropertyValue(prop);
                if (val != null) return val.asResource().getLocalName();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        var stmtIter = model.listStatements();
        while (stmtIter.hasNext()) {
            var stmt = stmtIter.nextStatement();
            String subjStr = stmt.getSubject().getURI();
            String predStr = stmt.getPredicate().getLocalName();

            if (subjStr != null && subjStr.toLowerCase().endsWith("#" + entityName.toLowerCase())) {
                if (predStr != null && predStr.equalsIgnoreCase(propertyName)) {
                    if (stmt.getObject().isResource()) {
                        return stmt.getObject().asResource().getLocalName();
                    }
                }
            }
        }
        return "None";
    }
}

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
                System.out.println("RPG Game Ontology successfully merged into CombatService!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public String getAttack(String monsterName) {
        if (monsterName == null) return "None";

        String attackName = getStringProperty(monsterName, "usesAttack");
        System.out.println("-> [РЕЗУЛТАТ ОНТОЛОГИЯ] Намерена атака за " + monsterName + ": " + attackName);
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
//        var monster = model.getOntClass(NS + monsterName);
//        if (monster == null) {
//            return 100;
//        }
//        var property = model.getProperty(NS + "hasHP");
//        var value = monster.getPropertyValue(property);
//
//        if (value == null) {
//            return 100;
//        }
//        try {
//            return value.asLiteral().getInt();
//        } catch (Exception e) {
//            return 100;
//        }
        int hp = getIntProperty(monsterName, "hasHP");
        return hp > 0 ? hp : 100;
    }

    public int getIntProperty(String entityName, String propertyName) {
        if (model == null || entityName == null || propertyName == null) {
            return 0;
        }

        String combatNS = "http://www.semanticweb.org/vlady/ontologies/2026/5/Combat_Ontology#";
        String rpgNS = "http://www.semanticweb.org/vlady/ontologies/2026/4/RPG-game-ontology#";

        // Намираме индивида (WarriorClass) във всички възможни вариации
        var entity = model.getIndividual(combatNS + entityName);
        if (entity == null) entity = model.getIndividual(BASE + entityName);
        if (entity == null) entity = model.getIndividual(rpgNS + entityName);

        if (entity == null) {
            System.out.println("CombatService: Индивидът [" + entityName + "] НЕ е намерен в модела!");
            return 0;
        }

        // Сурово обхождане на триплетите на този индивид
        var stmtIter = entity.listProperties();
        while (stmtIter.hasNext()) {
            var stmt = stmtIter.nextStatement();
            var pred = stmt.getPredicate();

            // Застраховаме се: взимаме пълния стринг на свойството (напр. "http://...#hasHP" или "RPG-game-ontology:hasHP")
            String predString = (pred != null) ? pred.toString() : "";

            // Ако в стринга на свойството се съдържа "hashp" или "hasbasedamage", влизаме вътре!
            if (!predString.isEmpty() && predString.toLowerCase().contains(propertyName.toLowerCase())) {
                var obj = stmt.getObject();
                if (obj != null) {
                    try {
                        String rawValue = obj.isLiteral() ? obj.asLiteral().getString() : obj.toString();

                        // Изчистване на излишните xsd:int декорации на Protégé
                        if (rawValue.contains("^^")) {
                            rawValue = rawValue.substring(0, rawValue.indexOf("^^"));
                        }
                        if (rawValue.contains("#")) {
                            rawValue = rawValue.substring(rawValue.indexOf("#") + 1);
                        }

                        int val = Integer.parseInt(rawValue.trim());
                        System.out.println("-> [СУПЕР УСПЕХ] Извлечено от файла: " + entityName + " -> " + propertyName + " = " + val);
                        return val;
                    } catch (Exception e) {
                        System.err.println("Грешка при парсване на " + propertyName + ": " + e.getMessage());
                    }
                }
            }
        }

        System.out.println("CombatService: Свойството " + propertyName + " за " + entityName + " не беше намерено в нито един триплет.");
        return 0;
    }

    public int getMonsterAttackDamage(String monsterName) {
        if (monsterName == null) return 10;

        String attackName = getAttack(monsterName);
        int damage = 0;

        // План А: Пробваме да вземем щетите от магията (напр. FireBreath)
        if (attackName != null && !attackName.equalsIgnoreCase("None")) {
            System.out.println("-> Пробвам да извлека щети от магията: " + attackName);
            damage = getIntProperty(attackName, "hasBaseDamage");
        }

        // План Б: Ако магията няма дефиниран демидж (върнало е 0), пробваме директно от чудовището
        if (damage <= 0) {
            System.out.println("-> Магията върна 0. Пробвам директно от чудовището: " + monsterName);
            damage = getIntProperty(monsterName, "hasBaseDamage");
        }
        return damage;
    }

    public String getBehavior(String monsterName) {
        if (model == null || monsterName == null) return "None";

        // 1. Проверяваме като онтологичен клас
        var monster = model.getOntClass(NS + monsterName);

        // 2. Авариен изход: Ако не е клас, проверяваме дали не е индивид
        if (monster == null) {
            var individual = model.getIndividual(NS + monsterName);
            if (individual != null) {
                var property = model.getProperty(NS + "hasBehavior");
                var value = individual.getPropertyValue(property);
                if (value != null) return value.asResource().getLocalName();
            }
            // АКО ИЗОБЩО ГО НЯМА: Връщаме "None" вместо краш!
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

        // План А: Пробваме стандартно през Jena индивиди
        try {
            var individual = model.getIndividual(NS + entityName);
            if (individual != null) {
                var prop = model.getProperty(NS + propertyName);
                var val = individual.getPropertyValue(prop);
                if (val != null) return val.asResource().getLocalName();
            }
        } catch (Exception e) {
            // Продължаваме към План Б при грешка
        }

        // План Б: Железният обход по суров текст (както при HP-то)
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

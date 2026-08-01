package org.uni.service;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.uni.model.Hero;
import org.uni.model.Item;
import org.uni.model.Monster;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

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

    public Monster createMonster(String monsterName, int dungeonLevel) {
        if (monsterName == null) return null;

        int baseHp = getMonsterHP(monsterName);
        int baseAtk = getMonsterAttackDamage(monsterName);
        String weakness = getWeakness(monsterName);
        String behavior = getBehavior(monsterName);

        return new Monster(monsterName, baseHp, baseAtk, weakness, behavior, dungeonLevel);
    }

    public Hero createHero(String heroClass, String weaponName) {
        if (heroClass == null) return null;

        int baseHp = getIntProperty(heroClass, "hasHP");
        if (baseHp <= 0) baseHp = 150;

        int baseAtk = getIntProperty(heroClass, "hasBaseDamage");
        if (baseAtk <= 0) baseAtk = 15;

        int weaponAtk = getIntProperty(weaponName, "hasBaseDamage");
        if (weaponAtk > 0) {
            baseAtk += weaponAtk;
        }

        return new Hero(heroClass, baseHp, baseHp, baseAtk, weaponName, new ArrayList<>());
    }

    public String executeAttack(Hero hero, Monster monster) {
        if (hero == null || monster == null) return "ERROR";

        int heroDamage = hero.getAtk();
        monster.takeDamage(heroDamage);

        if (!monster.isAlive()) {
            return "VICTORY";
        }

        int monsterDamage = monster.getAtk();
        hero.takeDamage(monsterDamage);

        if (!hero.isAlive()) {
            return "DEFEAT";
        }

        return "CONTINUE";
    }
    public void applyItem(Hero hero, Item item) {
        if (hero == null || item == null || item.getQuantity() <= 0) return;

        if ("HEAL".equalsIgnoreCase(item.getItemType())) {
            hero.heal(item.getEffectiveValue());
        } else if ("BUFF".equalsIgnoreCase(item.getItemType())) {
            hero.setAtk(hero.getAtk() + item.getEffectiveValue());
        }

        item.setQuantity(item.getQuantity() - 1);
    }

    public String getAttack(String monsterName) {
        if (monsterName == null) return "None";
        return getStringProperty(monsterName, "usesAttack");
    }

    public String getWeakness(String monsterName) {
        if (model == null || monsterName == null || monsterName.isEmpty()) return "Unknown";

        var resIter = model.listSubjects();
        while (resIter.hasNext()) {
            var res = resIter.nextResource();
            if (res.getLocalName() != null && res.getLocalName().equalsIgnoreCase(monsterName)) {

                var stmtIter = res.listProperties();
                while (stmtIter.hasNext()) {
                    var stmt = stmtIter.nextStatement();
                    String propName = stmt.getPredicate().getLocalName();

                    if (propName != null && (propName.equalsIgnoreCase("weakAgainst") || propName.equalsIgnoreCase("hasWeakness"))) {
                        var obj = stmt.getObject();
                        if (obj.isResource()) {
                            return obj.asResource().getLocalName();
                        } else if (obj.isLiteral()) {
                            return obj.asLiteral().getString();
                        }
                    }
                }
            }
        }

        return "Unknown";
    }

    public int getMonsterHP(String monsterName) {
        if (monsterName == null) return 100;
        int hp = getIntProperty(monsterName, "hasHP");
        if (hp <= 0) hp = getIntProperty(monsterName + "Monster", "hasHP");
        return hp > 0 ? hp : 100;
    }

    public int getMonsterAttackDamage(String monsterName) {
        if (monsterName == null) return 0;

        int damage = 0;

        damage = getIntProperty(monsterName, "hasBaseDamage");
        if (damage <= 0) damage = getIntProperty(monsterName, "hasAttackDamage");
        if (damage <= 0) damage = getIntProperty(monsterName, "hasDamage");
        if (damage <= 0) damage = getIntProperty(monsterName, "hasATK");

        if (damage <= 0) {
            String attackName = getAttack(monsterName);
            if (attackName != null && !attackName.equalsIgnoreCase("None")) {
                damage = getIntProperty(attackName, "hasBaseDamage");
                if (damage <= 0) damage = getIntProperty(attackName, "hasAttackDamage");
                if (damage <= 0) damage = getIntProperty(attackName, "hasDamage");
            }
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
        return (value != null) ? value.asResource().getLocalName() : "None";
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
            var ontClass = model.getOntClass(combatNS + entityName);
            if (ontClass == null) ontClass = model.getOntClass(rpgNS + entityName);
            if (ontClass == null) ontClass = model.getOntClass(combatNS + entityName + "Class");
            if (ontClass == null) ontClass = model.getOntClass(rpgNS + entityName + "Class");

            if (ontClass != null) {
                var stmtIter = ontClass.listProperties();
                while (stmtIter.hasNext()) {
                    var stmt = stmtIter.nextStatement();
                    String predStr = stmt.getPredicate().getLocalName();
                    if (predStr != null && predStr.equalsIgnoreCase(propertyName)) {
                        try {
                            return stmt.getObject().asLiteral().getInt();
                        } catch (Exception ignored) {}
                    }
                }
            }
        } else {
            var stmtIter = entity.listProperties();
            while (stmtIter.hasNext()) {
                var stmt = stmtIter.nextStatement();
                String predStr = stmt.getPredicate().getLocalName();
                if (predStr != null && predStr.equalsIgnoreCase(propertyName)) {
                    try {
                        return stmt.getObject().asLiteral().getInt();
                    } catch (Exception ignored) {}
                }
            }
        }

        var stmtIter = model.listStatements();
        while (stmtIter.hasNext()) {
            var stmt = stmtIter.nextStatement();
            String subjStr = stmt.getSubject().getURI();
            String predStr = stmt.getPredicate().getLocalName();

            if (subjStr != null && subjStr.toLowerCase().contains(entityName.toLowerCase())) {
                if (predStr != null && predStr.equalsIgnoreCase(propertyName)) {
                    try {
                        return stmt.getObject().asLiteral().getInt();
                    } catch (Exception ignored) {}
                }
            }
        }

        return 0;
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

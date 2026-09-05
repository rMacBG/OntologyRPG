package org.uni.service;

import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.uni.model.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CombatService {

    private static final String PATH = "ontology/CombatOntology.rdf";
    private static final String BASE = "http://www.semanticweb.org/vlady/ontologies/2026/5/Combat_Ontology";
    private static final String NS = BASE + "#";
    private static final String RPG_NS = "http://www.semanticweb.org/vlady/ontologies/2026/4/RPG-game-ontology#";
    private static OntologyService ontologyService = new OntologyService();
    private static DatabaseService dbService = new DatabaseService();
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
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public Monster createMonster(String monsterName, int dungeonLevel) {
        if (monsterName == null || monsterName.isEmpty()) return null;

        int baseHp = getIntProperty(monsterName, "hasHP");
        if (baseHp <= 0) baseHp = 100;

        int baseAtk = getIntProperty(monsterName, "hasBaseDamage");
        if (baseAtk <= 0) baseAtk = 15;

        String chosenWeakness = getWeakness(monsterName);

        String behavior = getBehavior(monsterName);

        Monster monster = new Monster(monsterName, baseHp, baseAtk, chosenWeakness, behavior, dungeonLevel);
        monster.setMonsterType(monsterName);

        return monster;
    }

    private String getStartingItemForClass(String heroClass, String propertyName) {
        if (heroClass == null || heroClass.trim().isEmpty()) {
            return null;
        }

        Individual hero = model.getIndividual(RPG_NS + heroClass);
        if (hero == null) {
            hero = model.getIndividual(NS + heroClass);
        }
        if (hero == null) {
            return null;
        }

        Property prop = model.getProperty(NS + propertyName);
        if (prop == null || !hero.hasProperty(prop)) {
            prop = model.getProperty(RPG_NS + propertyName);
        }

        RDFNode value = hero.getPropertyValue(prop);
        if (value != null && value.isResource()) {
            return value.asResource().getLocalName();
        }

        return null;
    }

public Hero createHero(String heroClass, String weaponName) {
    if (heroClass == null) return null;

    int baseHp = getIntProperty(heroClass, "hasHP");
    if (baseHp <= 0) baseHp = 150;

    int baseAtk = getIntProperty(heroClass, "hasBaseDamage");
    if (baseAtk <= 0) baseAtk = 15;

    if (weaponName == null || weaponName.trim().isEmpty()) {
        weaponName = getStartingWeaponForClass(heroClass);
    }

    WeaponItem equippedWeapon = null;
    int weaponAtk = 0;

    if (weaponName != null) {
        weaponAtk = getIntProperty(weaponName, "hasBaseDamage");
        if (weaponAtk < 0) weaponAtk = 0;
        equippedWeapon = new WeaponItem(weaponName, 1, weaponAtk);
    }

    int totalAtk = baseAtk + weaponAtk;

    List<Item> inventory = new ArrayList<>();
    if (equippedWeapon != null) {
        inventory.add(equippedWeapon);
    }

    Hero hero = new Hero(heroClass, baseHp, baseHp, totalAtk, equippedWeapon, inventory);

    String defaultArmor = getStartingArmorForClass(heroClass);
    if (defaultArmor != null) {
        ArmorItem initialArmor = loadArmorFromOntology(defaultArmor);
        hero.setEquippedArmor(initialArmor);
        inventory.add(initialArmor);

        DatabaseService.getInstance().updatePlayerDEF(heroClass, hero.getTotalDefense());
    }

    String defaultSkill = getStartingSkillForClass(heroClass);
    if (defaultSkill != null) {

    }

    return hero;
}

    public String getStartingWeaponForClass(String heroClass) {
        return getStartingItemForClass(heroClass, "hasStartingWeapon");
    }

    private String getStartingArmorForClass(String heroClass) {
        return getStartingItemForClass(heroClass, "hasStartingArmor");
    }

    public String getStartingSkillForClass(String heroClass) {
        return getStartingItemForClass(heroClass, "hasStartingSkill");
    }

    public String getWeaponElement(String weaponName) {
        if (model == null || weaponName == null || weaponName.isEmpty()) return "Physical";

        var stmtIter = model.listStatements();
        while (stmtIter.hasNext()) {
            var stmt = stmtIter.nextStatement();
            String subj = stmt.getSubject().getLocalName();
            String pred = stmt.getPredicate().getLocalName();

            if (subj != null && subj.equalsIgnoreCase(weaponName)) {

                if (pred != null && pred.equalsIgnoreCase("usesAttack")) {
                    var attackObj = stmt.getObject();
                    if (attackObj.isResource()) {
                        String attackName = attackObj.asResource().getLocalName();

                        String attackElement = getWeaponElement(attackName);
                        if (!attackElement.equalsIgnoreCase("Physical")) {
                            return attackElement;
                        }
                    }
                }
                if (pred != null && (pred.equalsIgnoreCase("hasElement") || pred.equalsIgnoreCase("usesAttack"))) {
                    var obj = stmt.getObject();
                    if (obj.isResource()) {
                        String elemName = obj.asResource().getLocalName();
                        if (elemName.toLowerCase().contains("lightning")) return "LightningElement";
                        if (elemName.toLowerCase().contains("fire")) return "FireElement";
                        if (elemName.toLowerCase().contains("ice")) return "IceElement";
                        if (elemName.toLowerCase().contains("water")) return "WaterElement";
                        return elemName;
                    }
                }
            }
        }

        return "Physical";
    }

    public String getResistance(String monsterName) {
        if (model == null || monsterName == null || monsterName.isEmpty()) return "None";

        var stmtIter = model.listStatements();
        while (stmtIter.hasNext()) {
            var stmt = stmtIter.nextStatement();
            String subj = stmt.getSubject().getLocalName();
            String pred = stmt.getPredicate().getLocalName();

            if (subj != null && subj.equalsIgnoreCase(monsterName)) {
                if (pred != null && (pred.equalsIgnoreCase("resistsElement") || pred.equalsIgnoreCase("hasToughness"))) {
                    var obj = stmt.getObject();
                    if (obj.isResource()) return obj.asResource().getLocalName();
                    if (obj.isLiteral()) return obj.asLiteral().getString();
                }
            }
        }
        return "None";
    }

    public boolean canEquip(String playerClass, String itemName) {
        if (playerClass == null || itemName == null) return false;

        String itemOntologyClass = getItemClassType(itemName);

        if (itemOntologyClass.isEmpty()) {
            itemOntologyClass = itemName;
        }

        String type = itemOntologyClass.toLowerCase();
        String pClass = playerClass.toLowerCase();

        if (pClass.contains("warrior")) {
            return type.contains("heavyarmor") || type.contains("sword") || type.contains("claymore");
        }

        if (pClass.contains("wizard")) {
            return type.contains("robearmor") || type.contains("staff") || type.contains("wand");
        }

        if (pClass.contains("archer")) {
            return type.contains("lightarmor") || type.contains("bow");
        }

        if (pClass.contains("assassin")) {
            return type.contains("lightarmor") || type.contains("dagger") || type.contains("blade");
        }

        return false;
    }

    public boolean equipWeaponForHero(Hero hero, String playerClass, WeaponItem newWeapon) {
        if (newWeapon == null) return false;

        String weaponName = newWeapon.getName();

        if (!canEquip(playerClass, weaponName)) {
            System.out.println("❌ " + playerClass + " cannot equip " + weaponName + "!");
            return false;
        }

        int weaponAtk = newWeapon.getBaseDamage();
        if (weaponAtk <= 0) {
            weaponAtk = getIntProperty(weaponName, "hasBaseDamage");
            if (weaponAtk <= 0) weaponAtk = 10;
            newWeapon.setBaseDamage(weaponAtk);
        }

        hero.setEquippedWeapon(newWeapon);

        int baseAtk = DatabaseService.getInstance().getAttack(playerClass);
        hero.setAtk(baseAtk + weaponAtk);

        DatabaseService.getInstance().equipWeapon(playerClass, weaponName);

        System.out.println("⚔️ " + playerClass + " successfully equipped " + weaponName + " (+ " + weaponAtk + " ATK)!");

        return true;
    }

    public boolean equipItemForHero(Hero hero, String playerClass, String itemName) {
        if (itemName == null || itemName.trim().isEmpty()) return false;

        if (!canEquip(playerClass, itemName)) {
            System.out.println("❌ " + playerClass + " cannot equip " + itemName + "!");
            return false;
        }

        if (isArmor(itemName)) {
            ArmorItem armor = loadArmorFromOntology(itemName);
            hero.setEquippedArmor(armor);

            DatabaseService.getInstance().updatePlayerDEF(playerClass, hero.getTotalDefense());
            System.out.println("🛡️ " + playerClass + " successfully equipped " + itemName + "!");
            return true;
        }

        WeaponItem weapon = getWeaponItem(itemName);
        return equipWeaponForHero(hero, playerClass, weapon);
    }

    public int calculateIncomingMonsterDamage(int monsterAtk, int playerDef) {
        return Math.max(1, monsterAtk - playerDef);
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

        List<String> weaknesses = new ArrayList<>();

        String queryString = String.format(
                "PREFIX combat: <%s> " +
                        "PREFIX rpg: <%s> " +
                        "SELECT ?weakness WHERE { " +
                        "   { combat:%s combat:weakAgainst ?weakness . } " +
                        "   UNION " +
                        "   { combat:%s rpg:weakAgainst ?weakness . } " +
                        "}", NS, RPG_NS, monsterName, monsterName
        );

        try (QueryExecution qe = QueryExecutionFactory.create(queryString, model)) {
            ResultSet results = qe.execSelect();
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                RDFNode node = soln.get("weakness");
                if (node != null) {
                    if (node.isResource()) {
                        weaknesses.add(node.asResource().getLocalName());
                    } else if (node.isLiteral()) {
                        weaknesses.add(node.asLiteral().getString());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (weaknesses.isEmpty()) {
            return "Unknown";
        }
        Random rand = new Random();
        return weaknesses.get(rand.nextInt(weaknesses.size()));
    }


    public int getMonsterAttackDamage(String monsterName) {
        if (monsterName == null) return 10;

        int damage = getIntProperty(monsterName, "hasBaseDamage");
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
        if (damage <= 0) {
            return monsterName.toLowerCase().contains("boss") ? 25 : 10;
        }

        return damage;
    }

    public int getWeaponBaseDamage(String weaponName){

        if(model == null || weaponName == null || weaponName.isEmpty()) return 0;

        var stmtIter = model.listStatements();
        while(stmtIter.hasNext()){
            var stmt = stmtIter.nextStatement();
            String subj = stmt.getSubject().getLocalName();
            String pred = stmt.getPredicate().getLocalName();

            if(subj != null && subj.equalsIgnoreCase(weaponName)){
                if (pred != null && (pred.equalsIgnoreCase("hasBaseDamage") || pred.equalsIgnoreCase("hasDamage"))) {                    if(stmt.getObject().isLiteral()){
                        return stmt.getObject().asLiteral().getInt();
                    }
                }
            }
        }

            return 0;
    }

    public double getDoubleProperty(String entityName, String propertyName) {
        if (model == null || entityName == null || propertyName == null) return 0.0;

        var stmtIter = model.listStatements();
        while (stmtIter.hasNext()) {
            var stmt = stmtIter.nextStatement();
            String subjStr = stmt.getSubject().getURI();
            String predStr = stmt.getPredicate().getLocalName();

            if (subjStr != null && subjStr.toLowerCase().endsWith("#" + entityName.toLowerCase())) {
                if (predStr != null && predStr.equalsIgnoreCase(propertyName)) {
                    if (stmt.getObject().isLiteral()) {
                        try {
                            return stmt.getObject().asLiteral().getDouble();
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
        return 0.0;
    }

    public String getBehavior(String monsterName) {
        if (model == null || monsterName == null || monsterName.trim().isEmpty()) {
            return "NeutralBehavior";
        }

        String[] namespaces = { NS, RPG_NS };

        for (String ns : namespaces) {
            var individual = model.getIndividual(ns + monsterName);
            if (individual != null) {
                var prop = model.getProperty(ns + "hasBehavior");
                if (prop == null) prop = model.getProperty(NS + "hasBehavior");
                if (prop == null) prop = model.getProperty(RPG_NS + "hasBehavior");

                if (prop != null) {
                    var value = individual.getPropertyValue(prop);
                    if (value != null && value.isResource()) {
                        var res = value.asResource();

                        var typeStmt = res.getProperty(org.apache.jena.vocabulary.RDF.type);
                        if (typeStmt != null && typeStmt.getObject().isResource()) {
                            String typeName = typeStmt.getObject().asResource().getLocalName();
                            if (typeName != null && typeName.contains("Behavior") && !typeName.equals("Behavior")) {
                                return typeName;
                            }
                        }
                        return res.getLocalName();
                    }
                }
            }
            var ontClass = model.getOntClass(ns + monsterName);
            if (ontClass == null) ontClass = model.getOntClass(ns + monsterName + "Class");

            if (ontClass != null) {
                var prop = model.getProperty(ns + "hasBehavior");
                if (prop == null) prop = model.getProperty(NS + "hasBehavior");

                if (prop != null) {
                    var value = ontClass.getPropertyValue(prop);
                    if (value != null && value.isResource()) {
                        return value.asResource().getLocalName();
                    }
                }
            }
        }
        var stmtIter = model.listStatements();
        while (stmtIter.hasNext()) {
            var stmt = stmtIter.nextStatement();
            String subj = stmt.getSubject().getLocalName();
            String pred = stmt.getPredicate().getLocalName();

            if (subj != null && subj.equalsIgnoreCase(monsterName)) {
                if (pred != null && (pred.equalsIgnoreCase("hasBehavior") || pred.equalsIgnoreCase("behavior"))) {
                    if (stmt.getObject().isResource()) {
                        return stmt.getObject().asResource().getLocalName();
                    }
                }
            }
        }

        return "NeutralBehavior";
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

        String[] namespaces = { NS, RPG_NS };

        for (String ns : namespaces) {
            try {
                var individual = model.getIndividual(ns + entityName);
                if (individual != null) {
                    var prop = model.getProperty(ns + propertyName);
                    if (prop == null) prop = model.getProperty(NS + propertyName);
                    if (prop == null) prop = model.getProperty(RPG_NS + propertyName);

                    if (prop != null) {
                        var val = individual.getPropertyValue(prop);
                        if (val != null && val.isResource()) {
                            return val.asResource().getLocalName();
                        } else if (val != null && val.isLiteral()) {
                            return val.asLiteral().getString();
                        }
                    }
                }
            } catch (Exception ignored) {}
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
                    } else if (stmt.getObject().isLiteral()) {
                        return stmt.getObject().asLiteral().getString();
                    }
                }
            }
        }
        return "None";
    }

    public ArmorItem loadArmorFromOntology(String armorName) {
        int baseDef = getIntProperty(armorName, "hasBaseDef");
        int dmgBonus = getIntProperty(armorName, "hasDamageBonus");
        int dmgPenalty = getIntProperty(armorName, "hasDamagePenalty");
        int dmgRes = getIntProperty(armorName, "hasDamageResistance");

        return new ArmorItem(armorName, 1, baseDef, dmgBonus, dmgPenalty, dmgRes);
    }

    public WeaponItem loadWeaponFromOntology(String weaponName) {
        if (weaponName == null || weaponName.trim().isEmpty() || "None".equalsIgnoreCase(weaponName)) {
            return null;
        }

        int weaponAtk = getIntProperty(weaponName, "hasBaseDamage");
        if (weaponAtk <= 0) weaponAtk = getIntProperty(weaponName, "hasAttackDamage");
        if (weaponAtk <= 0) weaponAtk = getIntProperty(weaponName, "hasDamage");
        if (weaponAtk <= 0) weaponAtk = 10;

        String element = getWeaponElement(weaponName);

        WeaponItem weapon = new WeaponItem(weaponName, 1, weaponAtk);


        return weapon;
    }

    public WeaponItem getWeaponItem(String weaponName) {
        int weaponAtk = getIntProperty(weaponName, "hasBaseDamage");
        if (weaponAtk <= 0) weaponAtk = 10;
        return new WeaponItem(weaponName, 1, weaponAtk);
    }

    public boolean isArmor(String itemName) {
        int baseDef = getIntProperty(itemName, "hasBaseDef");
        int res = getIntProperty(itemName, "hasDamageResistance");
        return baseDef > 0 || res > 0;
    }
    public boolean isSkill(String itemName) {
        if (itemName == null || itemName.trim().isEmpty()) return false;

        int manaCost = getIntProperty(itemName, "hasManaCost");
        if (manaCost <= 0) manaCost = getIntProperty(itemName, "manaCost");

        double multiplier = getDoubleProperty(itemName, "hasDamageMultiplier");
        if (multiplier <= 0) multiplier = getDoubleProperty(itemName, "hasMultiplier");

        if (manaCost > 0 || multiplier > 1.0) return true;

        String classType = getItemClassType(itemName);
        return classType.toLowerCase().contains("skill") || classType.toLowerCase().contains("spell");
    }
    public boolean isWeapon(String itemName) {
        if (itemName == null || itemName.trim().isEmpty()) return false;
        int baseAtk = getIntProperty(itemName, "hasBaseDamage");
        return baseAtk > 0;
    }

    public String getItemCategory(String itemName) {
        if (itemName == null || itemName.trim().isEmpty()) return "UNKNOWN";
        if (itemName.equalsIgnoreCase("Health Potion") || itemName.toLowerCase().contains("potion")) return "POTION";
        if (isSkill(itemName)) return "SKILL";
        if (isArmor(itemName)) return "ARMOR";
        if (isWeapon(itemName)) return "WEAPON";

        return "UNKNOWN";
    }

    public SkillItem loadSkillFromOntology(String skillName) {
        if (skillName == null || skillName.trim().isEmpty()) {
            SkillItem defaultSkill = new SkillItem("Basic Strike", 0, 1.4, "All", "Physical");
            defaultSkill.setActiveRounds(0);
            defaultSkill.setCooldown(0);
            return defaultSkill;
        }

        int manaCost = getIntProperty(skillName, "hasManaCost");
        if (manaCost <= 0) manaCost = getIntProperty(skillName, "manaCost");

        double multiplier = getDoubleProperty(skillName, "hasDamageMultiplier");
        if (multiplier <= 0) multiplier = getDoubleProperty(skillName, "hasMultiplier");

        String element = getStringProperty(skillName, "hasElement");
        if ("None".equalsIgnoreCase(element)) element = "Physical";

        String reqClass = getStringProperty(skillName, "requiresClass");
        if ("None".equalsIgnoreCase(reqClass)) reqClass = getStringProperty(skillName, "hasRequiredClass");

        int baseDamage = getIntProperty(skillName, "hasBaseDamage");
        int damageBonus = getIntProperty(skillName, "hasDamageBonus");
        int damageResistance = getIntProperty(skillName, "hasDamageResistance");
        int activeRounds = getIntProperty(skillName, "hasActiveRounds");
        int cooldown = getIntProperty(skillName, "hasCooldown");

        if (multiplier <= 0) {
            multiplier = (baseDamage == 0 && (damageResistance > 0 || activeRounds > 0)) ? 1.0 : 1.5;
        }

        SkillItem skill = new SkillItem(skillName, manaCost, multiplier, reqClass, element);
        skill.setActiveRounds(activeRounds);
        skill.setCooldown(cooldown);
        skill.setBaseDamage(baseDamage);
        skill.setDamageBonus(damageBonus);
        skill.setDamageResistance(damageResistance);

        return skill;
    }

    public String generateLoot(String monsterName) {
        String queryString = String.format(
                "PREFIX combat: <%s> " +
                        "PREFIX rpg: <%s> " +
                        "SELECT ?item WHERE { " +
                        "   combat:%s rpg:dropsItem ?item . " +
                        "}", NS, RPG_NS, monsterName
        );

        List<String> possibleDrops = new ArrayList<>();

        try (QueryExecution qe = QueryExecutionFactory.create(queryString, model)) {
            ResultSet results = qe.execSelect();
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                Resource itemResource = soln.getResource("item");
                if (itemResource != null) {
                    possibleDrops.add(itemResource.getLocalName());
                }
            }
        } catch (Exception e) {
            System.err.println("Грешка при извличане на loot за: " + monsterName);
            e.printStackTrace();
        }

        Random rand = new Random();
        int chance = rand.nextInt(100);

        if (chance < 20) {
            return "Health Potion";
        }

        if (!possibleDrops.isEmpty()) {
            return possibleDrops.get(rand.nextInt(possibleDrops.size()));
        }
        return "No Loot";
    }

    public String getItemClassType(String itemName) {
        if (itemName == null || itemName.trim().isEmpty()) return "";

        String cleanName = itemName.replaceAll("\\s+", "");

        Resource itemRes = model.getResource(NS + cleanName);
        if (itemRes == null || !model.containsResource(itemRes)) {
            itemRes = model.getResource(RPG_NS + cleanName);
        }

        if (itemRes != null && model.containsResource(itemRes)) {
            StmtIterator it = model.listStatements(itemRes, RDF.type, (RDFNode) null);
            while (it.hasNext()) {
                Statement stmt = it.nextStatement();
                Resource typeRes = stmt.getObject().asResource();
                String typeName = typeRes.getLocalName();
                if (typeName != null && (
                        typeName.equalsIgnoreCase("HeavyArmor") ||
                                typeName.equalsIgnoreCase("LightArmor") ||
                                typeName.equalsIgnoreCase("RobeArmor") ||
                                typeName.contains("Sword") || typeName.contains("Staff") ||
                                typeName.contains("Bow") || typeName.contains("Dagger") ||
                                typeName.contains("Skill") || typeName.contains("Spell"))) {
                    return typeName;
                }
            }
        }

        return "";
    }
}

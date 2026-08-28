package org.uni.service;

import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.vocabulary.RDF;
import org.uni.model.*;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CombatService {

    private static final String PATH = "ontology/CombatOntology.rdf";
    private static final String BASE = "http://www.semanticweb.org/vlady/ontologies/2026/5/Combat_Ontology/";
    private static final String NS = BASE + "#";
    private static final String RPG_NS = "http://www.semanticweb.org/vlady/ontologies/2026/4/RPG-game-ontology#";
    private static OntologyService ontologyService = new OntologyService();

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
        if (weaponAtk < 0) weaponAtk = 0;
        WeaponItem equippedWeapon = new WeaponItem(weaponName, 1, weaponAtk);

        int totalAtk = baseAtk + weaponAtk;

        List<Item> inventory = new ArrayList<>();
        inventory.add(equippedWeapon);
        Hero hero = new Hero(heroClass, baseHp, baseHp, totalAtk, equippedWeapon, inventory);


        String defaultArmor = getStartingArmorForClass(heroClass);
        if (defaultArmor != null) {
            ArmorItem initialArmor = loadArmorFromOntology(defaultArmor);
            hero.setEquippedArmor(initialArmor);
            inventory.add(initialArmor);

            DatabaseService.getInstance().updatePlayerDEF(heroClass, hero.getTotalDefense());
        }

        return hero;
    }

    private String getStartingArmorForClass(String heroClass) {
        switch (heroClass) {
            case "WarriorClass": return "SteelHeavyArmor";
            case "WizardClass": return "MagicRobe";
            default: return "LeatherLightArmor";
        }
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

//        public boolean canEquip(String playerClass, String weaponName) {
//            if (playerClass == null || weaponName == null) return false;
//
//            String weapon = weaponName.toLowerCase();
//
//            if (playerClass.contains("Wizard") && (weapon.contains("staff") || weapon.contains("wand"))) return true;
//            if (playerClass.contains("Warrior") && (weapon.contains("sword") || weapon.contains("claymore") || weapon.contains("greatsword"))) return true;
//            if (playerClass.contains("Archer") && (weapon.contains("bow") || weapon.contains("crossbow"))) return true;
//            if (playerClass.contains("Assassin") && (weapon.contains("dagger") || weapon.contains("blade"))) return true;
//
//            return false;
//        }

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
    public ArmorItem loadArmorFromOntology(String armorName) {
        int baseDef = getIntProperty(armorName, "hasBaseDef");
        int dmgBonus = getIntProperty(armorName, "hasDamageBonus");
        int dmgPenalty = getIntProperty(armorName, "hasDamagePenalty");
        int dmgRes = getIntProperty(armorName, "hasDamageResistance");

        return new ArmorItem(armorName, 1, baseDef, dmgBonus, dmgPenalty, dmgRes);
    }

    // 2. Извлича оръжие от онтологията и го превръща във WeaponItem обект
    public WeaponItem getWeaponItem(String weaponName) {
        int weaponAtk = getIntProperty(weaponName, "hasBaseDamage");
        if (weaponAtk <= 0) weaponAtk = 10; // Резервна стойност по подразбиране
        return new WeaponItem(weaponName, 1, weaponAtk);
    }

    // 3. Проверява дали предметът е броня в онтологията
    public boolean isArmor(String itemName) {
        // Ако предметът има стойност за защита или резистентност > 0, го третираме като броня
        int baseDef = getIntProperty(itemName, "hasBaseDef");
        int res = getIntProperty(itemName, "hasDamageResistance");
        return baseDef > 0 || res > 0;
    }


    public String generateLoot(String monsterName) {
        Random rand = new Random();
        int chance = rand.nextInt(100);

        if (chance < 30) {
            return "Health Potion"; // 30% шанс за отвара
        } else if (chance < 65) {
            // 35% шанс за оръжие
            String[] possibleWeapons = {"StormStaff", "SteelDagger", "FloodStaff", "FlameClaymore", "PrecisionBow"};
            return possibleWeapons[rand.nextInt(possibleWeapons.length)];
        } else {
            // 35% шанс за броня (замени имената с тези от твоята онтология)
            String[] possibleArmors = {"LeatherLightArmor", "SteelHeavyArmor", "MagicRobe"};
            return possibleArmors[rand.nextInt(possibleArmors.length)];
        }
    }

    public String getItemClassType(String itemName) {
        if (itemName == null || itemName.trim().isEmpty()) return "";

        String cleanName = itemName.replaceAll("\\s+", "");

        // Използваме полетата на класа NS и RPG_NS
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
                                typeName.contains("Bow") || typeName.contains("Dagger"))) {
                    return typeName;
                }
            }
        }

        return "";
    }
}

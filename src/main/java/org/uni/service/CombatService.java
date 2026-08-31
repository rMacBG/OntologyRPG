package org.uni.service;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.uni.model.*;

import java.io.InputStream;
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

    public String getStartingSkillForClass(String heroClass) {
        if (heroClass == null) return "Basic Strike";

        String skillName = getStringProperty(heroClass, "hasSkill");
        if ("None".equalsIgnoreCase(skillName)) {
            skillName = getStringProperty(heroClass, "hasStartingSkill");
        }
        if ("None".equalsIgnoreCase(skillName)) {
            skillName = getStringProperty(heroClass, "usesSkill");
        }

        return "None".equalsIgnoreCase(skillName) ? "Basic Strike" : skillName;
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

        return "NeutralBehavior"; // Подразбиращо се поведение, ако не е посочено друго
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

    public WeaponItem loadWeaponFromOntology(String weaponName) {
        if (weaponName == null || weaponName.trim().isEmpty() || "None".equalsIgnoreCase(weaponName)) {
            return null;
        }

        int weaponAtk = getIntProperty(weaponName, "hasBaseDamage");
        if (weaponAtk <= 0) weaponAtk = getIntProperty(weaponName, "hasAttackDamage");
        if (weaponAtk <= 0) weaponAtk = getIntProperty(weaponName, "hasDamage");
        if (weaponAtk <= 0) weaponAtk = 10; // Подразбираща се стойност, ако не е намерена щета

        String element = getWeaponElement(weaponName);

        WeaponItem weapon = new WeaponItem(weaponName, 1, weaponAtk);

        // Ако в твоя клас WeaponItem има сетър за стихия (Element)
//        try {
//            weapon.setElement(element);
//        } catch (Exception ignored) {}

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

        // Зареждаме стойностите за щета и защита
        int baseDamage = getIntProperty(skillName, "hasBaseDamage");
        int damageBonus = getIntProperty(skillName, "hasDamageBonus");
        int damageResistance = getIntProperty(skillName, "hasDamageResistance");
        int activeRounds = getIntProperty(skillName, "hasActiveRounds");
        int cooldown = getIntProperty(skillName, "hasCooldown");

        // Ако няма посочен множител: за защитни/бъф умения слагаме 1.0 (вместо 1.5)
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
        Random rand = new Random();
        int chance = rand.nextInt(100);

        if (chance < 20) {
            return "Health Potion";
        } else if (chance < 45) {
            String[] possibleWeapons = {"StormStaff", "SteelDagger", "FloodStaff", "FireyFoldClaymore", "PrecisionBow"};
            return possibleWeapons[rand.nextInt(possibleWeapons.length)];
        } else if (chance < 70) {
            String[] possibleArmors = {"LeatherLightArmor", "SteelHeavyArmor", "MagicRobe", "CrystalChainmail", "DragonScaleLightArmor", "StormPowerMagicRobe"};
            return possibleArmors[rand.nextInt(possibleArmors.length)];
        } else {
            String[] possibleSkills = {"BerserkHelmet", "ArgentQuiver", "SolarGrenade", "FissureGrenade", "SteelHelmet", "LeatherQuiver", "HelmetOfTheGods", "MagicQuiver", "ChargedLightning"};
            return possibleSkills[rand.nextInt(possibleSkills.length)];
        }
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

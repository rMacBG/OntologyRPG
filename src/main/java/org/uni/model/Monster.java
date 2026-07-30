package org.uni.model;

import java.io.Serializable;

public class Monster implements Serializable {
    private String name;
    private String monsterType;
    private int hp;
    private int maxHp;
    private int atk;
    private String icon;
    private String weakness;
    private String behavior;

    public Monster(String name, int baseHp, int baseAtk, String weakness, String behavior, int dungeonLevel) {
        this.name = name;
        this.weakness = weakness;
        this.behavior = behavior;
        double hpMultiplier = 1.0 + ((dungeonLevel - 1) * 0.2);
        double atkMultiplier = 1.0 + ((dungeonLevel - 1) * 0.15);
        this.maxHp = (int) (baseHp * hpMultiplier);
        this.hp = maxHp;
        this.atk = (int) (baseAtk * atkMultiplier);
        this.icon = assignIcon(name);
    }

    private String assignIcon(String name){
        if (name.contains("Dragon")) return "🐲";
        if (name.contains("Goblin")) return "👺";
        if (name.contains("Demon")) return "😈";
        return "👹";
    }

    public void takeDamage(int damage) {
        this.hp = Math.max(0, this.hp - damage);
    }

    public boolean isAlive() {
        return this.hp > 0;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMonsterType() {
        return monsterType;
    }

    public void setMonsterType(String monsterType) {
        this.monsterType = monsterType;
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public void setMaxHp(int maxHp) {
        this.maxHp = maxHp;
    }

    public int getAtk() {
        return atk;
    }

    public void setAtk(int atk) {
        this.atk = atk;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getWeakness() {
        return weakness;
    }

    public void setWeakness(String weakness) {
        this.weakness = weakness;
    }

    public String getBehavior() {
        return behavior;
    }

    public void setBehavior(String behavior) {
        this.behavior = behavior;
    }
}

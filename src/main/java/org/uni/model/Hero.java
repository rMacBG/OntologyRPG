package org.uni.model;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Hero implements Serializable {
    private String heroClass;
    private int hp;
    private int maxHP;
    private int atk;
    private String equippedWeapon;
    private List<Item> inventory;

    public Hero(String heroClass, int hp, int maxHP, int atk, String equippedWeapon, List<Item> inventory) {
        this.heroClass = heroClass;
        this.hp = hp;
        this.maxHP = maxHP;
        this.atk = atk;
        this.equippedWeapon = equippedWeapon;
        this.inventory = (inventory != null) ? inventory : new ArrayList<>();
    }

    public void takeDamage(int damage) {
        this.hp = Math.max(0, this.hp - damage);
    }

    public void heal(int amount) {
        this.hp = Math.min(this.maxHP, this.hp + amount);
    }

    public boolean isAlive() {
        return this.hp > 0;
    }

    public String getHeroClass() {
        return heroClass;
    }

    public void setHeroClass(String heroClass) {
        this.heroClass = heroClass;
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public int getMaxHP() {
        return maxHP;
    }

    public void setMaxHP(int maxHP) {
        this.maxHP = maxHP;
    }

    public int getAtk() {
        return atk;
    }

    public void setAtk(int atk) {
        this.atk = atk;
    }

    public String getEquippedWeapon() {
        return equippedWeapon;
    }

    public void setEquippedWeapon(String equippedWeapon) {
        this.equippedWeapon = equippedWeapon;
    }

    public List<Item> getInventory() {
        return inventory;
    }

    public void setInventory(List<Item> inventory) {
        this.inventory = inventory;
    }
}

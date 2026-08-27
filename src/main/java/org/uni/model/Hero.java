package org.uni.model;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Hero implements Serializable {
    private String heroClass;
    private int hp;
    private int maxHP;
    private int atk;
    private WeaponItem equippedWeapon;
    private ArmorItem equippedArmor;
    private List<Item> inventory;



    public Hero(String heroClass, int hp, int maxHP, int atk, WeaponItem equippedWeapon, List<Item> inventory) {
        this.heroClass = heroClass;
        this.hp = hp;
        this.maxHP = maxHP;
        this.atk = atk;
        this.equippedWeapon = equippedWeapon;
        this.inventory = (inventory != null) ? inventory : new ArrayList<>();
    }

    public int getTotalAttack(){
        if (equippedArmor == null) return atk;
        return Math.max(0, atk + equippedArmor.getDamageBonus() - equippedArmor.getDamagePenalty());
    }

    public int getTotalDefense() {
        if (equippedArmor == null) return 0;
        return equippedArmor.getBaseDef() + equippedArmor.getDamageResistance();
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

    public WeaponItem getEquippedWeapon() {
        return equippedWeapon;
    }

    public void setEquippedWeapon(WeaponItem equippedWeapon) {
        this.equippedWeapon = equippedWeapon;
    }

    public ArmorItem getEquippedArmor() {
        return equippedArmor;
    }

    public void setEquippedArmor(ArmorItem equippedArmor) {
        this.equippedArmor = equippedArmor;
    }

    public List<Item> getInventory() {
        return inventory;
    }

    public void setInventory(List<Item> inventory) {
        this.inventory = inventory;
    }
}

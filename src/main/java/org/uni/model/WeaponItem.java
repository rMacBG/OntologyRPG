package org.uni.model;

public class WeaponItem extends Item {
    private int baseDamage;

    public WeaponItem(String name, int quantity, int baseDamage) {
        super(name, "Weapon", baseDamage, quantity);
        this.baseDamage = baseDamage;
    }

    public int getBaseDamage() { return baseDamage; }
    public void setBaseDamage(int baseDamage) {
        this.baseDamage = baseDamage;
        setEffectiveValue(baseDamage);
    }
}

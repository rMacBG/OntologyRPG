package org.uni.model;

public class ArmorItem extends Item{
    private int baseDef;
    private int damageBonus;
    private int damagePenalty;
    private int damageResistance;

    public ArmorItem(String name, int quantity, int baseDef, int damageBonus, int damagePenalty, int damageResistance) {
        super(name, "Armor", baseDef, quantity);
        this.baseDef = baseDef;
        this.damageBonus = damageBonus;
        this.damagePenalty = damagePenalty;
        this.damageResistance = damageResistance;
    }

    public int getBaseDef() { return baseDef; }
    public void setBaseDef(int baseDef) {
        this.baseDef = baseDef;
        setEffectiveValue(baseDef);
    }

    public int getDamageBonus() { return damageBonus; }
    public void setDamageBonus(int damageBonus) { this.damageBonus = damageBonus; }

    public int getDamagePenalty() { return damagePenalty; }
    public void setDamagePenalty(int damagePenalty) { this.damagePenalty = damagePenalty; }

    public int getDamageResistance() { return damageResistance; }
    public void setDamageResistance(int damageResistance) { this.damageResistance = damageResistance; }
}

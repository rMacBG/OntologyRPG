package org.uni.model;

public class SkillItem extends Item{
    private int manaCost;
    private double damageMultiplier;
    private String requiredClass;
    private String element;

    private int activeRounds;
    private int cooldown;
    private int baseDamage;
    private int damageBonus;
    private int damageResistance;

    public SkillItem(String name, int manaCost, double damageMultiplier, String requiredClass, String element) {
        super(name, "SKILL", 0, 1);

        this.manaCost = manaCost;
        this.damageMultiplier = damageMultiplier;
        this.requiredClass = requiredClass;
        this.element = element;
    }

    public int getManaCost() { return manaCost; }
    public double getDamageMultiplier() { return damageMultiplier; }
    public String getRequiredClass() { return requiredClass; }
    public String getElement() { return element; }

    public int getActiveRounds() {
        return activeRounds;
    }

    public void setActiveRounds(int activeRounds) {
        this.activeRounds = activeRounds;
    }

    public int getCooldown() {
        return cooldown;
    }

    public void setCooldown(int cooldown) {
        this.cooldown = cooldown;
    }

    public int getBaseDamage() {
        return baseDamage;
    }

    public void setBaseDamage(int baseDamage) {
        this.baseDamage = baseDamage;
    }

    public int getDamageBonus() {
        return damageBonus;
    }

    public void setDamageBonus(int damageBonus) {
        this.damageBonus = damageBonus;
    }

    public int getDamageResistance() {
        return damageResistance;
    }

    public void setDamageResistance(int damageResistance) {
        this.damageResistance = damageResistance;
    }
}

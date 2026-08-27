package org.uni.model;

import java.io.Serializable;

public class Item implements Serializable {
    private String name;
    private String itemType;
    private int effectiveValue;
    private int quantity;


    public Item(String name, String itemType, int effectiveValue, int quantity) {
        this.name = name;
        this.itemType = itemType;
        this.effectiveValue = effectiveValue;
        this.quantity = quantity;

    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    public String getItemType() {
        return itemType;
    }
    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public int getEffectiveValue() {
        return effectiveValue;
    }
    public void setEffectiveValue(int effectiveValue) {
        this.effectiveValue = effectiveValue;
    }
    public int getQuantity() {
        return quantity;
    }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}

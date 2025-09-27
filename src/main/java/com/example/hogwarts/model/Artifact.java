package com.example.hogwarts.model;

import java.util.Objects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Artifact {
    private int id;
    private String name;
    private String description;
    private Wizard owner; // can be null
    private final List<Transfer> transfers = new ArrayList<>();
    private int condition;

    public Artifact(String name, String description) {
        this.name = Objects.requireNonNullElse(name, "name must not be null");
        this.description = Objects.requireNonNullElse(description, "description must not be null");
        this.owner = null;
        this.condition = 100;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Wizard getOwner() { return owner; }
    public int getCondition() { return condition; }

    public void setId(int id) { this.id = id; }
    public void setName(String name) {
        this.name = Objects.requireNonNullElse(name, "name must not be null");
    }
    public void setDescription(String description) {
        this.description = Objects.requireNonNullElse(description, "description must not be null");
    }
    void setOwner(Wizard owner) { this.owner = owner; } // package-private to restrict access
    public void setCondition(int condition) {
        this.condition = Math.max(0, Math.min(100, condition));
    }
    public void repair(int amount) {
        this.condition = Math.max(0, Math.min(100, this.condition + amount));
    }
    public void applyWear(int amount) {
        this.condition = Math.max(0, this.condition - amount);
    }
    public boolean canBeAssigned() {
        return this.condition >= 10;
    }

    @Override
    public String toString() {
        return name + " (ID: " + id + ")";
    }

}

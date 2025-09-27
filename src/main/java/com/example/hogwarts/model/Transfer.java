package com.example.hogwarts.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import java.time.LocalDate; 

public class Transfer {
    private int id;
    private String type;
    private Artifact artifact;
	private Wizard wizard;
	private LocalDate timestamp;

    public Transfer(String type, Artifact artifact, Wizard wizard) {
    	if (type != "assign" || type != "unassign") {
    		this.type = "N/A";
    	}
    	this.type = Objects.requireNonNullElse(type, "Transfer type must be non-null.");
    	this.artifact = artifact;
    	this.wizard = wizard;
    	this.timestamp = LocalDate.now();
    }

    public int getId() { return id; }
    public String getType() { return type; }
    public Artifact getArtifact() { return artifact; }
    public Wizard getWizard() { return wizard; }
    public LocalDate getTimestamp() { return timestamp; }

    public void setId(int id) { this.id = id; }
    public void setType(String type) {
        this.type = Objects.requireNonNullElse(type, "Transfer type must be non-null.");
    }
    void setArtifact(Artifact artifact) { this.artifact = artifact; } 	// package-private to restrict access
    void setWizard(Wizard wizard) { this.wizard = wizard; } 			// package-private to restrict access

    @Override
    public String toString() {
        return type + " (: " + id + ")";
    }
}

package com.example.hogwarts.data;

import com.example.hogwarts.model.Artifact;
import com.example.hogwarts.model.Wizard;
import com.example.hogwarts.model.Role;
import com.example.hogwarts.model.User;
import com.example.hogwarts.model.Transfer;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import java.util.*;

/**
 * TODO: Make this a thread-safe singleton
 * TODO: Use atomic integers for ID generation to avoid race conditions
 */
public class DataStore {
    private static DataStore instance; // Singleton instance

    private final List<User> users = new ArrayList<>();
    private final Map<Integer, Wizard> wizards = new HashMap<>();
    private final Map<Integer, Artifact> artifacts = new HashMap<>();
    private final Map<Integer, Transfer> transfers = new HashMap<>();

    private int wizardIdCounter = 1; // Wizard ID generator
    private int artifactIdCounter = 1; // Artifact ID generator
    private int transferIdCounter = 1;

    private User currentUser; // Currently authenticated user

    private DataStore() {
        // Hardcoded users
        this.users.add(new User("admin", "123", Role.ADMIN));
        this.users.add(new User("user", "123", Role.USER));

        // Sample data was AI generated
        Wizard w1 = new Wizard("Harry Potter");
        Wizard w2 = new Wizard("Hermione Granger");
        Wizard w3 = new Wizard("Ron Weasley");
        Wizard w4 = new Wizard("Albus Dumbledore");
        Wizard w5 = new Wizard("Severus Snape");
        
        this.addWizard(w1);
        this.addWizard(w2);
        this.addWizard(w3);
        this.addWizard(w4);
        this.addWizard(w5);

        Artifact a1 = new Artifact("Invisibility Cloak", "A magical cloak that makes the wearer invisible.");
        Artifact a2 = new Artifact("Time-Turner", "A device used for time travel.");
        Artifact a3 = new Artifact("Elder Wand", "The most powerful wand in existence.");
        Artifact a4 = new Artifact("Marauder's Map", "A magical map showing Hogwarts and everyone in it.");
        Artifact a5 = new Artifact("Sorting Hat", "An ancient hat that sorts students into houses.");
        Artifact a6 = new Artifact("Philosopher's Stone", "A legendary stone that grants immortality.");
        Artifact a7 = new Artifact("Pensieve", "A basin for reviewing memories.");
        Artifact a8 = new Artifact("Golden Snitch", "A flying golden ball used in Quidditch.");
        Artifact a9 = new Artifact("Nimbus 2000", "A high-performance racing broomstick.");
        Artifact a10 = new Artifact("Broken Wand", "A damaged wand with unstable magic.");

        this.addArtifact(a1);
        this.addArtifact(a2);
        this.addArtifact(a3);
        this.addArtifact(a4);
        this.addArtifact(a5);
        this.addArtifact(a6);
        this.addArtifact(a7);
        this.addArtifact(a8);
        this.addArtifact(a9);
        this.addArtifact(a10);

        a1.setCondition(100); // Perfect condition
        a2.setCondition(85);  // Good condition
        a3.setCondition(70);  // Fair condition
        a4.setCondition(45);  // Poor condition
        a5.setCondition(25);  // Very poor condition
        a6.setCondition(90);  // Excellent condition
        a7.setCondition(60);  // Fair condition
        a8.setCondition(30);  // Poor condition
        a9.setCondition(5);   // Critical condition (cannot be assigned)
        a10.setCondition(8);  // Critical condition (cannot be assigned)

        this.assignArtifactToWizard(a1.getId(), w1.getId()); // Harry gets Invisibility Cloak
        this.assignArtifactToWizard(a2.getId(), w2.getId()); // Hermione gets Time-Turner
        this.assignArtifactToWizard(a4.getId(), w1.getId()); // Harry also gets Marauder's Map
        this.assignArtifactToWizard(a6.getId(), w4.getId()); // Dumbledore gets Philosopher's Stone
        this.assignArtifactToWizard(a7.getId(), w4.getId()); // Dumbledore also gets Pensieve
        
        // Reset conditions after assignment to show proper initial state
        // (compensate for the -5 wear from assignment)
        a1.setCondition(100);
        a2.setCondition(85);
        a4.setCondition(45);
        a6.setCondition(90);
        a7.setCondition(60);
    }

    public static DataStore getInstance() {
        if (instance == null) {
            instance = new DataStore();
        }
        return instance;
    }

    // User authentication
    public User authenticate(String username, String password) {
        return users.stream()
                .filter(u -> u.getUsername().equals(username) && u.getPassword().equals(password))
                .findFirst()
                .orElse(null);
    }
    
    // Transfers
    public Transfer addTransfer(Transfer transfer) {
    	transfer.setId(transferIdCounter++);
    	this.transfers.put(transfer.getId(), transfer);
    	return transfer;
    }
    
    public Collection<Transfer> findAllTransfersById(int id) {
		return this.transfers.values().stream()
        	.filter(transfer -> transfer.getArtifact().getId() == id)
        	.collect(Collectors.toList());
    }

    // Wizards
    public Wizard addWizard(Wizard wizard) {
        wizard.setId(wizardIdCounter++);
        this.wizards.put(wizard.getId(), wizard);
        return wizard;
    }

    public void deleteWizardById(int id) {
        Wizard wizard = this.wizards.remove(id);
        if (wizard != null) {
            wizard.removeAllArtifacts();
        }
    }

    public Collection<Wizard> findAllWizards() {
        return this.wizards.values();
    }

    public Wizard findWizardById(int id) {
        return this.wizards.get(id);
    }

    // Artifacts
    public Artifact addArtifact(Artifact artifact) {
        artifact.setId(artifactIdCounter++);
        this.artifacts.put(artifact.getId(), artifact);
        return artifact;
    }

    public void deleteArtifactById(int id) {
        Artifact artifact = this.artifacts.remove(id);
        if (artifact != null && artifact.getOwner() != null) {
            artifact.getOwner().removeArtifact(artifact);
        }
    }

    public Collection<Artifact> findAllArtifacts() {
        return this.artifacts.values();
    }

    public Artifact findArtifactById(int id) {
        return this.artifacts.get(id);
    }

    public boolean assignArtifactToWizard(int artifactId, int wizardId) {
        Artifact artifact = this.artifacts.get(artifactId);
        Wizard wizard = this.wizards.get(wizardId);
        if (artifact == null || wizard == null) return false;

        if (!artifact.canBeAssigned()) {
            return false;
        }

        artifact.applyWear(5);
        
        wizard.addArtifact(artifact);
        return true;
    }
    
    public String getAssignmentBlockReason(int artifactId) {
        Artifact artifact = this.artifacts.get(artifactId);
        if (artifact == null) return "Artifact not found";
        
        if (!artifact.canBeAssigned()) {
            return "Artifact condition is too low (< 10). Please repair before assigning.";
        }
        
        return null;
    }
    
    public boolean unassignArtifactFromWizard(int wizardId, int artifactId) {
    	Artifact artifact = this.artifacts.get(artifactId);
    	Wizard wizard = this.wizards.get(wizardId);
    	if (artifact == null || wizard == null) return false;
    	
    	wizard.removeArtifact(artifact);
    	return true;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }
}

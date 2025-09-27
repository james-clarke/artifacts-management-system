package com.example.hogwarts.view;

import com.example.hogwarts.controller.ArtifactController;
import com.example.hogwarts.controller.WizardController;
import com.example.hogwarts.controller.TransferController;
import com.example.hogwarts.data.DataStore;
import com.example.hogwarts.model.Artifact;
import com.example.hogwarts.model.Wizard;
import com.example.hogwarts.model.Wizard;
import com.example.hogwarts.model.Transfer;
import com.example.hogwarts.view.WizardView;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.Collection;

public class ArtifactView extends VBox{
    private final ArtifactController controller;
    private final WizardController wizController;
    private final TransferController transferController;
    private final TableView<Artifact> artifactTable;
	private final ObservableList<Artifact> masterData;
	private final FilteredList<Artifact> filteredData;
    
    private static Runnable artifactSignal;

    public ArtifactView() {
        this.controller = new ArtifactController();
        this.wizController = new WizardController();
        this.transferController = new TransferController();
        this.artifactTable = new TableView<>();
        this.masterData = FXCollections.observableArrayList(controller.findAllArtifacts());
		this.filteredData = new FilteredList<>(masterData, p -> true);

        setSpacing(10);
        setPadding(new Insets(10));
        getChildren().addAll(createSearchBar(), createTable(), createButtons());
        
        // lambda to refresh table data
		artifactSignal = () -> masterData.setAll(controller.findAllArtifacts());
    }
    
	// adds auto-update functionality across views
    public static void updateTable() {
    	artifactSignal.run();
    }

    private TableView<Artifact> createTable() {
        TableColumn<Artifact, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getId()));

        TableColumn<Artifact, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getName()));

        TableColumn<Artifact, Number> conditionCol = new TableColumn<>("Condition");
        conditionCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getCondition()));
        conditionCol.setCellFactory(col -> new TableCell<Artifact, Number>() {
            @Override
            protected void updateItem(Number condition, boolean empty) {
                super.updateItem(condition, empty);
                if (empty || condition == null) {
                    setText(null);
                    setStyle("");
                } else {
                    int conditionValue = condition.intValue();
                    setText(conditionValue + "%");
                }
            }
        });

        TableColumn<Artifact, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button viewButton = new Button("View");
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");
            private final Button unassignButton = new Button("Unassign");
            private final Button transferButton = new Button("Transfers");
            private final HBox buttons = new HBox(5);
            {
                viewButton.setOnAction(e -> {
                    Artifact artifact = getTableView().getItems().get(getIndex());
                    showViewArtifactDialog(artifact);
                });

                editButton.setOnAction(e -> {
                    Artifact artifact = getTableView().getItems().get(getIndex());
                    showEditArtifactDialog(artifact);
                });

                deleteButton.setOnAction(e -> {
                    Artifact artifact = getTableView().getItems().get(getIndex());
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Confirm Deletion");
                    confirm.setHeaderText("Delete Artifact");
                    confirm.setContentText("Are you sure you want to delete \"" + artifact.getName() + "\"?");

                    confirm.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.OK) {
                            controller.deleteArtifact(artifact.getId());
                            masterData.setAll(controller.findAllArtifacts());
                        }
                    });
                });
                
                unassignButton.setOnAction(e -> {
					Artifact artifact = getTableView().getItems().get(getIndex());
					Wizard owner = artifact.getOwner();
					showUnassignArtifactDialogFor(owner, artifact);
                });
                
                transferButton.setOnAction(e -> {
                    Artifact artifact = getTableView().getItems().get(getIndex());
                    showTransferDialog(artifact);
                });
            }
            

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= masterData.size()) {
                    setGraphic(null);
                } else {
                    buttons.getChildren().clear();
                    buttons.getChildren().add(viewButton);
                    if (DataStore.getInstance().getCurrentUser().isAdmin()) {
                        buttons.getChildren().addAll(editButton, deleteButton, unassignButton, transferButton);
                    }
                    setGraphic(buttons);
                }
            }
        });
        
        TableColumn<Artifact, String> ownerCol = new TableColumn<>("Owner");
        ownerCol.setComparator((owner1, owner2) -> {
        	if (owner1.contentEquals("-")) { return 1; }
        	if (owner2.contentEquals("-")) { return -1; }
        	return owner1.compareTo(owner2);
        });
        ownerCol.setCellValueFactory(cell -> {
        	Wizard owner = cell.getValue().getOwner();
        	if (owner != null) {
        		return new ReadOnlyStringWrapper(owner.getName());
        	}
        	return new ReadOnlyStringWrapper("-");
        });

        artifactTable.getColumns().setAll(idCol, nameCol, actionCol, ownerCol);
        artifactTable.setItems(filteredData);
        artifactTable.setPrefHeight(300);
        return artifactTable;
    }

    private HBox createButtons() {
        Button addBtn = new Button("Add");
        HBox box = new HBox(10);
        if (DataStore.getInstance().getCurrentUser().isAdmin()) {
            addBtn.setOnAction(e -> showAddArtifactDialog());
            box.getChildren().add(addBtn);
        }
        return box;
    }

    private void showAddArtifactDialog() {
        Dialog<Artifact> dialog = new Dialog<>();
        dialog.setTitle("Add Artifact");
        dialog.setHeaderText("Enter artifact details:");

        TextField nameField = new TextField();
        TextArea descField = new TextArea();

        VBox content = new VBox(10, new Label("Name:"), nameField, new Label("Description:"), descField);
        content.setPadding(new Insets(10));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                return controller.addArtifact(nameField.getText(), descField.getText());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(artifact -> {
            masterData.setAll(controller.findAllArtifacts());
            artifactTable.getSelectionModel().select(artifact);
        });
    }

    private void showEditArtifactDialog(Artifact artifact) {
        if (artifact == null) return;

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Edit Artifact");
        dialog.setHeaderText("Edit artifact details:");

        TextField nameField = new TextField(artifact.getName());
        TextArea descField = new TextArea(artifact.getDescription());

        VBox content = new VBox(10, new Label("Name:"), nameField, new Label("Description:"), descField);
        content.setPadding(new Insets(10));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                controller.updateArtifact(artifact.getId(), nameField.getText(), descField.getText());
                masterData.setAll(controller.findAllArtifacts());
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void showViewArtifactDialog(Artifact artifact) {
        if (artifact == null) return;

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Artifact Details");
        dialog.setHeaderText("Viewing: " + artifact.getName());

        String ownerName = artifact.getOwner() != null ? artifact.getOwner().getName() : "Unassigned";
        TextArea details = new TextArea(
                "ID: " + artifact.getId() + "\n" +
                        "Name: " + artifact.getName() + "\n" +
                        "Description: " + artifact.getDescription() + "\n" +
                        "Owner: " + ownerName + "\n" +
                        "Condition: " + artifact.getCondition() + "%"
        );
        details.setEditable(false);
        details.setWrapText(true);

        VBox content = new VBox(details);
        content.setPadding(new Insets(10));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }
    
private void showTransferDialog(Artifact artifact) {
    	if (artifact == null) return;

    	Collection<Transfer> transferList = transferController.findAllTransfersById(artifact.getId());

    	Dialog<Void> dialog = new Dialog<>();
    	dialog.setTitle("Transfer Details");
    	dialog.setHeaderText("Viewing Transfers for: " + artifact.getName());

    	StringBuilder sb = new StringBuilder();
    	for (Transfer transfer : transferList) {
        	sb.append("ID: ").append(transfer.getId()).append("\n")
          		.append("Type: ").append(transfer.getType()).append("\n")
          		.append("Wizard: ").append(transfer.getWizard().getName()).append("\n")
          		.append("Timestamp: ").append(transfer.getTimestamp()).append("\n")
          		.append("--------").append("\n");
    	}

    	TextArea details = new TextArea(sb.toString());
    	details.setEditable(false);
    	details.setWrapText(true);

    	VBox content = new VBox(details);
    	content.setPadding(new Insets(10));

    	dialog.getDialogPane().setContent(content);
    	dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
    	dialog.showAndWait();
}
    
    private void showUnassignArtifactDialogFor(Wizard wizard, Artifact artifact) {
    	if (artifact == null || wizard == null) {
    		Alert alert = new Alert(Alert.AlertType.INFORMATION, "No assigned artifacts available.");
            alert.setHeaderText("Nothing to unassign");
            alert.showAndWait();
            return;
    	}
    	
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Unassign Artifact");
        confirm.setHeaderText("Unassign Artifact");
        confirm.setContentText("Are you sure you want to unassign \"" + artifact.getName() + "\" from \"" + wizard.getName() + "\"");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
            	wizController.unassignArtifactFromWizard(wizard, artifact);
            	masterData.setAll(controller.findAllArtifacts());
            	artifactTable.getSelectionModel().select(artifact);
				WizardView.updateTable();
            }
    	});
    }
    
    private HBox createSearchBar() {
    	Label searchLabel = new Label("Search:");
    	TextField searchField = new TextField();
    	searchField.setPromptText("Enter artifact name");

    	searchField.textProperty().addListener((obs, oldVal, newVal) -> {
        	String filter = newVal.toLowerCase();
        	filteredData.setPredicate(artifact -> {
            	if (filter == null || filter.isEmpty()) {
                	return true;
            	}
            	return artifact.getName().toLowerCase().contains(filter);
        	});
    	});

    	HBox box = new HBox(10, searchLabel, searchField);
    	box.setPadding(new Insets(0, 0, 10, 0));
    	return box;
	}
	
	private void showRepairArtifactDialog(Artifact artifact) {
        if (artifact == null) return;

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Repair Artifact");
        dialog.setHeaderText("Repair: " + artifact.getName());

        Label currentConditionLabel = new Label("Current Condition: " + artifact.getCondition() + "%");
        TextField repairAmountField = new TextField();
        repairAmountField.setPromptText("Enter repair amount (1-100)");
        
        Label instructionLabel = new Label("Enter the amount to increase condition by:");

        VBox content = new VBox(10, 
            currentConditionLabel, 
            instructionLabel,
            new Label("Repair Amount:"), 
            repairAmountField
        );
        content.setPadding(new Insets(10));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                try {
                    int repairAmount = Integer.parseInt(repairAmountField.getText().trim());
                    if (repairAmount < 1 || repairAmount > 100) {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Invalid Input");
                        alert.setHeaderText("Invalid Repair Amount");
                        alert.setContentText("Repair amount must be between 1 and 100.");
                        alert.showAndWait();
                        return null;
                    }
                    
                    controller.repairArtifact(artifact.getId(), repairAmount);
                    masterData.setAll(controller.findAllArtifacts());
                    
                    // Show success message with new condition
                    Alert success = new Alert(Alert.AlertType.INFORMATION);
                    success.setTitle("Repair Successful");
                    success.setHeaderText("Artifact Repaired");
                    success.setContentText("\"" + artifact.getName() + "\" has been repaired.\nNew condition: " + 
                                         artifact.getCondition() + "%");
                    success.showAndWait();
                    
                } catch (NumberFormatException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Invalid Input");
                    alert.setHeaderText("Invalid Number");
                    alert.setContentText("Please enter a valid number for repair amount.");
                    alert.showAndWait();
                }
            }
            return null;
        });

        dialog.showAndWait();
    }
}

package com.example.hogwarts.view;

import com.example.hogwarts.controller.WizardController;
import com.example.hogwarts.data.DataStore;
import com.example.hogwarts.model.Artifact;
import com.example.hogwarts.model.Wizard;
import com.example.hogwarts.view.ArtifactView;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class WizardView extends VBox {
    private final WizardController controller;
    private final TableView<Wizard> wizardTable;
    private final ObservableList<Wizard> masterData;
    private final FilteredList<Wizard> filteredData;
    
    private static Runnable wizardSignal;

    public WizardView() {
        this.controller = new WizardController();
        this.wizardTable = new TableView<>();
        this.masterData = FXCollections.observableArrayList(controller.findAllWizards());
        this.filteredData = new FilteredList<>(masterData, p -> true);

        setSpacing(10);
        setPadding(new Insets(10));
        getChildren().addAll(createSearchBar(), createTable(), createButtons());
        
        // lambda to refresh table data
        wizardSignal = () -> masterData.setAll(controller.findAllWizards());
    }
    
    // adds auto-update functionality across views
    public static void updateTable() {
        wizardSignal.run();
    }

    private TableView<Wizard> createTable() {
        TableColumn<Wizard, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getId()));

        TableColumn<Wizard, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getName()));

        TableColumn<Wizard, Number> artifactCountCol = new TableColumn<>("Artifacts");
        artifactCountCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getArtifacts().size()));

        TableColumn<Wizard, Void> actionCol = new TableColumn<>("Actions");

        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button viewButton = new Button("View");
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");
            private final Button assignButton = new Button("Assign");
            private final Button unassignButton = new Button("Unassign");
            private final HBox buttons = new HBox(5);

            {
                viewButton.setOnAction(e -> {
                    Wizard wizard = getTableView().getItems().get(getIndex());
                    showViewWizardDialog(wizard);
                });

                editButton.setOnAction(e -> {
                    Wizard wizard = getTableView().getItems().get(getIndex());
                    showEditWizardDialog(wizard);
                });

                deleteButton.setOnAction(e -> {
                    Wizard wizard = getTableView().getItems().get(getIndex());
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Confirm Deletion");
                    confirm.setHeaderText("Delete Wizard");
                    confirm.setContentText("Are you sure you want to delete \"" + wizard.getName() + "\" and unassign their artifacts?");
                    confirm.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.OK) {
                            controller.deleteWizard(wizard.getId());
                            masterData.setAll(controller.findAllWizards());
                            ArtifactView.updateTable(); // Update artifact view
                        }
                    });
                });

                assignButton.setOnAction(e -> {
                    Wizard wizard = getTableView().getItems().get(getIndex());
                    showAssignArtifactDialogFor(wizard);
                });
                
                unassignButton.setOnAction(e -> {
                    Wizard wizard = getTableView().getItems().get(getIndex());
                    showUnassignArtifactDialogFor(wizard);
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
                        buttons.getChildren().addAll(editButton, deleteButton, assignButton, unassignButton);
                    }
                    setGraphic(buttons);
                }
            }
        });

        wizardTable.getColumns().setAll(idCol, nameCol, artifactCountCol, actionCol);
        wizardTable.setItems(filteredData);
        wizardTable.setPrefHeight(300);
        return wizardTable;
    }

    private HBox createButtons() {
        Button addBtn = new Button("Add");
        HBox buttonBox = new HBox(10);
        if (DataStore.getInstance().getCurrentUser().isAdmin()) {
            addBtn.setOnAction(e -> showAddWizardDialog());
            buttonBox.getChildren().add(addBtn);
        }
        return buttonBox;
    }

    private void showAddWizardDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Wizard");
        dialog.setHeaderText("Enter wizard name:");

        dialog.showAndWait().ifPresent(name -> {
            if (!name.isBlank()) {
                Wizard wizard = controller.addWizard(name);
                masterData.setAll(controller.findAllWizards());
                wizardTable.getSelectionModel().select(wizard);
            }
        });
    }

    private void showEditWizardDialog(Wizard wizard) {
        if (wizard == null) return;

        TextInputDialog dialog = new TextInputDialog(wizard.getName());
        dialog.setTitle("Edit Wizard");
        dialog.setHeaderText("Edit wizard name:");

        dialog.showAndWait().ifPresent(name -> {
            if (!name.isBlank()) {
                controller.updateWizard(wizard.getId(), name);
                masterData.setAll(controller.findAllWizards());
            }
        });
    }

    private void showAssignArtifactDialogFor(Wizard wizard) {
        var unowned = FXCollections.observableArrayList(controller.getUnassignedArtifacts());

        if (unowned.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "No unassigned artifacts available.");
            alert.setHeaderText("Nothing to assign");
            alert.showAndWait();
            return;
        }

        Dialog<Artifact> dialog = new Dialog<>();
        dialog.setTitle("Assign Artifact");
        dialog.setHeaderText("Assign artifact to: " + wizard.getName());

        ListView<Artifact> artifactListView = new ListView<>();
        artifactListView.getItems().addAll(unowned);
        
        artifactListView.setCellFactory(listView -> new ListCell<Artifact>() {
            @Override
            protected void updateItem(Artifact artifact, boolean empty) {
                super.updateItem(artifact, empty);
                if (empty || artifact == null) {
                    setText(null);
                    setStyle("");
                } else {
                    String conditionText = " (Condition: " + artifact.getCondition() + "%)";
                    String warningText = "";
                    
                    // Add warning if condition is too low
                    if (artifact.getCondition() < 10) {
                        warningText = "TOO DAMAGED TO ASSIGN";
                    }
                    
                    setText(artifact.getName() + conditionText + warningText);
                }
            }
        });

        Label instructionLabel = new Label("Select an artifact to assign:");
        Label noteLabel = new Label("Note: Assignment will reduce artifact condition by 5 points.");
        Label warningLabel = new Label("Artifacts with condition < 10% cannot be assigned.");

        VBox content = new VBox(10, instructionLabel, artifactListView, noteLabel, warningLabel);
        content.setPadding(new Insets(10));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                return artifactListView.getSelectionModel().getSelectedItem();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(artifact -> {
            String errorMessage = controller.assignArtifactToWizard(artifact, wizard);
            
            if (errorMessage != null) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Assignment Failed");
                alert.setHeaderText("Cannot Assign Artifact");
                alert.setContentText(errorMessage);
                alert.showAndWait();
            } else {
                masterData.setAll(controller.findAllWizards());
                wizardTable.getSelectionModel().select(wizard);
                ArtifactView.updateTable();
                
                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("Assignment Successful");
                success.setHeaderText("Artifact Assigned");
                success.setContentText("\"" + artifact.getName() + "\" has been assigned to \"" + wizard.getName() + "\".\n" +
                                     "Artifact condition reduced by 5 points due to wear.\n" +
                                     "New condition: " + artifact.getCondition() + "%");
                success.showAndWait();
            }
        });
    }
    
    public void showUnassignArtifactDialogFor(Wizard wizard) {
        var owned = FXCollections.observableArrayList(controller.getAssignedArtifacts(wizard));
        
        if (owned.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "No assigned artifacts available.");
            alert.setHeaderText("Nothing to unassign");
            alert.showAndWait();
            return;
        }
        
        ChoiceDialog<Artifact> dialog = new ChoiceDialog<>(owned.get(0), owned);
        dialog.setTitle("Unassign Artifact");
        dialog.setHeaderText("Unassign from " + wizard.getName());

        dialog.showAndWait().ifPresent(artifact -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Unassign Artifact");
            confirm.setHeaderText("Unassign Artifact");
            confirm.setContentText("Are you sure you want to unassign \"" + artifact.getName() + "\" from \"" + wizard.getName() + "\"");
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    controller.unassignArtifactFromWizard(wizard, artifact);
                    masterData.setAll(controller.findAllWizards());
                    wizardTable.getSelectionModel().select(wizard);
                    ArtifactView.updateTable();
                }
            });
        });
    }

    private void showViewWizardDialog(Wizard wizard) {
        if (wizard == null) return;

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Wizard Details");
        dialog.setHeaderText("Viewing: " + wizard.getName());

        StringBuilder sb = new StringBuilder();
        sb.append("ID: ").append(wizard.getId()).append("\n");
        sb.append("Name: ").append(wizard.getName()).append("\n");
        sb.append("Number of Artifacts: ").append(wizard.getArtifacts().size()).append("\n\n");
        sb.append("Artifacts:\n");
        for (Artifact a : wizard.getArtifacts()) {
            sb.append("  - ").append(a.getName())
              .append(" (ID: ").append(a.getId()).append(")")
              .append(" (Condition: ").append(a.getCondition()).append("%)")
              .append("\n");
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

    private HBox createSearchBar() {
        Label searchLabel = new Label("Search:");
        TextField searchField = new TextField();
        searchField.setPromptText("Enter wizard name");

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String filter = newVal.toLowerCase();
            filteredData.setPredicate(wizard -> {
                if (filter == null || filter.isEmpty()) {
                    return true;
                }
                return wizard.getName().toLowerCase().contains(filter);
            });
        });

        HBox box = new HBox(10, searchLabel, searchField);
        box.setPadding(new Insets(0, 0, 10, 0));
        return box;
    }
}
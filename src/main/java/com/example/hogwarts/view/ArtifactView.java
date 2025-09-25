package com.example.hogwarts.view;

import com.example.hogwarts.controller.ArtifactController;
import com.example.hogwarts.data.DataStore;
import com.example.hogwarts.model.Artifact;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class ArtifactView extends VBox{
    private final ArtifactController controller;
    private final TableView<Artifact> artifactTable;
    private final ObservableList<Artifact> artifactData;

    public ArtifactView() {
        this.controller = new ArtifactController();
        this.artifactTable = new TableView<>();
        this.artifactData = FXCollections.observableArrayList(controller.findAllArtifacts());

        setSpacing(10);
        setPadding(new Insets(10));
        getChildren().addAll(createTable(), createButtons());
    }

    private TableView<Artifact> createTable() {
        TableColumn<Artifact, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getId()));

        TableColumn<Artifact, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getName()));

        TableColumn<Artifact, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button viewButton = new Button("View");
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");
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
                            artifactData.setAll(controller.findAllArtifacts());
                        }
                    });
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= artifactData.size()) {
                    setGraphic(null);
                } else {
                    buttons.getChildren().clear();
                    buttons.getChildren().add(viewButton);
                    if (DataStore.getInstance().getCurrentUser().isAdmin()) {
                        buttons.getChildren().addAll(editButton, deleteButton);
                    }
                    setGraphic(buttons);
                }
            }
        });

        artifactTable.getColumns().setAll(idCol, nameCol, actionCol);
        artifactTable.setItems(artifactData);
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
            artifactData.setAll(controller.findAllArtifacts());
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
                artifactData.setAll(controller.findAllArtifacts());
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
                        "Owner: " + ownerName
        );
        details.setEditable(false);
        details.setWrapText(true);

        VBox content = new VBox(details);
        content.setPadding(new Insets(10));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }
}

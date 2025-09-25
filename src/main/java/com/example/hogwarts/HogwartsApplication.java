package com.example.hogwarts;

import com.example.hogwarts.controller.LoginController;
import com.example.hogwarts.view.LoginView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class HogwartsApplication extends Application {

    @Override
    public void start(Stage primaryStage) {
        LoginView loginView = new LoginView();
        LoginController loginController = new LoginController(loginView);

        Scene scene = new Scene(loginView, 400, 250);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Hogwarts Artifacts Management System");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

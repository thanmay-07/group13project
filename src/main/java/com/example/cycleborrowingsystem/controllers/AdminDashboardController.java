package com.example.cycleborrowingsystem.controllers;

import com.example.cycleborrowingsystem.SceneManager;
import com.example.cycleborrowingsystem.dao.UserDao;
import com.example.cycleborrowingsystem.models.User;
import com.example.cycleborrowingsystem.models.Cycle;
import com.example.cycleborrowingsystem.dao.CycleDao;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.util.List;

public class AdminDashboardController {
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, Long> idColumn;
    @FXML private TableColumn<User, String> nameColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableView<Cycle> cyclesTable;
    @FXML private TableColumn<Cycle, Long> cycleIdColumn;
    @FXML private TableColumn<Cycle, String> cycleModelColumn;
    @FXML private TableColumn<Cycle, Double> cycleLatColumn;
    @FXML private TableColumn<Cycle, Double> cycleLonColumn;

    private final UserDao userDao = new UserDao();
    private final CycleDao cycleDao = new CycleDao();

    @FXML
    private void initialize() {
        if (idColumn != null) idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        if (nameColumn != null) nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        if (emailColumn != null) emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        onRefreshUsers();
        if (cycleIdColumn != null) cycleIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        if (cycleModelColumn != null) cycleModelColumn.setCellValueFactory(new PropertyValueFactory<>("model"));
        if (cycleLatColumn != null) cycleLatColumn.setCellValueFactory(new PropertyValueFactory<>("lat"));
        if (cycleLonColumn != null) cycleLonColumn.setCellValueFactory(new PropertyValueFactory<>("lon"));
        onRefreshCycles();
    }

    @FXML
    private void onRefreshUsers() {
        try {
            List<User> users = userDao.listAll();
            ObservableList<User> data = FXCollections.observableArrayList(users);
            usersTable.setItems(data);
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Failed to load users: " + e.getMessage()).show();
        }
    }

    @FXML
    private void onRefreshCycles() {
        try {
            List<Cycle> cycles = cycleDao.listAll();
            javafx.collections.ObservableList<Cycle> data = javafx.collections.FXCollections.observableArrayList(cycles);
            if (cyclesTable != null) cyclesTable.setItems(data);
        } catch (java.sql.SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Failed to load cycles: " + e.getMessage()).show();
        }
    }

    @FXML
    private void onBack() {
        try {
            SceneManager.switchScene("/com/example/cycleborrowingsystem/landing.fxml");
        } catch (Exception ignored) {}
    }
}



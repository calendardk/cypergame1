package com.cybergame.ui.fxcontroller;

import com.cybergame.controller.EmployeeController;
import com.cybergame.model.entity.Employee;
import com.cybergame.repository.EmployeeRepository;
import com.cybergame.repository.sql.EmployeeRepositorySQL;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.Optional;

public class EmployeeMgmtController {

    @FXML private TableView<Employee> employeeTable;
    @FXML private TableColumn<Employee, Integer> colId;
    @FXML private TableColumn<Employee, String> colName;
    @FXML private TableColumn<Employee, String> colPhone;
    @FXML private TextField txtSearch;

    private final EmployeeRepository repo = new EmployeeRepositorySQL();
    private final EmployeeController controller = new EmployeeController(repo);
    private final ObservableList<Employee> data = FXCollections.observableArrayList();

    // ================= INIT =================
    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("userId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("displayName"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));

        data.addAll(repo.findAll());
        employeeTable.setItems(data);

        employeeTable.setStyle(
                "-fx-control-inner-background: #0d1b2a;" +
                "-fx-base: #0d1b2a;" +
                "-fx-background-color: #0d1b2a;"
        );

        employeeTable.setRowFactory(tv -> {
            TableRow<Employee> row = new TableRow<>();

            // Toggle selection
            row.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
                if (!row.isEmpty() && row.isSelected() && e.getButton() == MouseButton.PRIMARY) {
                    employeeTable.getSelectionModel().clearSelection();
                    e.consume();
                }
            });

            row.selectedProperty().addListener((obs, oldSel, isSelected) -> {
                if (!row.isEmpty()) updateRowStyle(row, isSelected);
            });

            row.itemProperty().addListener((obs, o, n) -> {
                if (n == null) {
                    row.setStyle("-fx-background-color: transparent;");
                } else {
                    updateRowStyle(row, row.isSelected());
                }
            });

            return row;
        });

        Platform.runLater(() -> employeeTable.getSelectionModel().clearSelection());
    }

    private void updateRowStyle(TableRow<Employee> row, boolean isSelected) {
        if (isSelected) {
            row.setStyle("-fx-background-color: #008000; -fx-text-fill: white; -fx-font-weight: bold;");
        } else {
            row.setStyle("-fx-background-color: transparent; -fx-text-fill: white;");
        }
    }

    // ================= SEARCH =================
    @FXML
    private void handleSearch() {
        String key = txtSearch.getText().trim().toLowerCase();
        data.clear();

        if (key.isEmpty()) {
            data.addAll(repo.findAll());
            return;
        }

        for (Employee e : repo.findAll()) {
            if (e.getDisplayName().toLowerCase().contains(key)
                    || e.getPhone().contains(key)) {
                data.add(e);
            }
        }
    }

    // ================= ADD =================
    @FXML
    private void handleAddEmployee() {
        Dialog<Employee> dialog = new Dialog<>();
        dialog.setTitle("Thêm nhân viên");

        ButtonType btnAdd = new ButtonType("Tạo", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnAdd, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField txtUser = new TextField();

        TextField txtPassVisible = new TextField();
        PasswordField txtPassHidden = new PasswordField();

        CheckBox chkHide = new CheckBox("Ẩn mật khẩu");
        chkHide.setSelected(true);

        txtPassVisible.setVisible(false);
        txtPassVisible.setManaged(false);

        chkHide.selectedProperty().addListener((obs, o, hide) -> {
            if (hide) {
                txtPassHidden.setText(txtPassVisible.getText());
                txtPassHidden.setVisible(true);
                txtPassHidden.setManaged(true);
                txtPassVisible.setVisible(false);
                txtPassVisible.setManaged(false);
            } else {
                txtPassVisible.setText(txtPassHidden.getText());
                txtPassVisible.setVisible(true);
                txtPassVisible.setManaged(true);
                txtPassHidden.setVisible(false);
                txtPassHidden.setManaged(false);
            }
        });

        VBox passBox = new VBox(5, txtPassHidden, txtPassVisible, chkHide);

        TextField txtName = new TextField();
        TextField txtPhone = new TextField();

        grid.addRow(0, new Label("Username:"), txtUser);
        grid.addRow(1, new Label("Password:"), passBox);
        grid.addRow(2, new Label("Họ tên:"), txtName);
        grid.addRow(3, new Label("SĐT:"), txtPhone);

        dialog.getDialogPane().setContent(grid);

        Node btnCreateNode = dialog.getDialogPane().lookupButton(btnAdd);
        btnCreateNode.addEventFilter(ActionEvent.ACTION, e -> {
            String pass = chkHide.isSelected() ? txtPassHidden.getText() : txtPassVisible.getText();
            if (txtUser.getText().isBlank() || pass.isBlank()
                    || txtName.getText().isBlank() || txtPhone.getText().isBlank()) {
                showAlert("Vui lòng điền đầy đủ thông tin!");
                e.consume();
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn == btnAdd) {
                String pass = chkHide.isSelected() ? txtPassHidden.getText() : txtPassVisible.getText();
                return controller.createEmployee(
                        txtUser.getText(),
                        pass,
                        txtName.getText(),
                        txtPhone.getText()
                );
            }
            return null;
        });

        dialog.showAndWait().ifPresent(e -> data.add(e));
    }

    // ================= VIEW / EDIT =================
    @FXML
    private void handleViewEmployee() {
        Employee selected = employeeTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Vui lòng chọn nhân viên");
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Thông tin nhân viên");

        ButtonType btnSave = new ButtonType("Lưu", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSave, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField txtUser = new TextField(selected.getUsername());
        txtUser.setDisable(true);

        TextField txtPassVisible = new TextField(selected.getPasswordHash());
        PasswordField txtPassHidden = new PasswordField();
        txtPassHidden.setText(selected.getPasswordHash());

        CheckBox chkHide = new CheckBox("Ẩn mật khẩu");
        chkHide.setSelected(true);

        txtPassVisible.setVisible(false);
        txtPassVisible.setManaged(false);

        chkHide.selectedProperty().addListener((obs, o, hide) -> {
            if (hide) {
                txtPassHidden.setText(txtPassVisible.getText());
                txtPassHidden.setVisible(true);
                txtPassHidden.setManaged(true);
                txtPassVisible.setVisible(false);
                txtPassVisible.setManaged(false);
            } else {
                txtPassVisible.setText(txtPassHidden.getText());
                txtPassVisible.setVisible(true);
                txtPassVisible.setManaged(true);
                txtPassHidden.setVisible(false);
                txtPassHidden.setManaged(false);
            }
        });

        VBox passBox = new VBox(5, txtPassHidden, txtPassVisible, chkHide);

        TextField txtName = new TextField(selected.getDisplayName());
        TextField txtPhone = new TextField(selected.getPhone());

        grid.addRow(0, new Label("Username:"), txtUser);
        grid.addRow(1, new Label("Password:"), passBox);
        grid.addRow(2, new Label("Họ tên:"), txtName);
        grid.addRow(3, new Label("SĐT:"), txtPhone);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == btnSave) {
                String pass = chkHide.isSelected() ? txtPassHidden.getText() : txtPassVisible.getText();
                selected.setPasswordHash(pass);
                selected.setDisplayName(txtName.getText());
                selected.setPhone(txtPhone.getText());
                repo.save(selected);
                employeeTable.refresh();
            }
            return null;
        });

        dialog.showAndWait();
    }

    // ================= DELETE =================
    @FXML
    private void handleDeleteEmployee() {
        Employee selected = employeeTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Chưa chọn nhân viên");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText("Xác nhận xóa");
        confirm.setContentText("Xóa nhân viên: " + selected.getDisplayName() + " ?");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                controller.delete(selected);
                data.remove(selected);
                employeeTable.getSelectionModel().clearSelection();
            }
        });
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.show();
    }
}

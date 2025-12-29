package com.cybergame.ui.fxcontroller;

import com.cybergame.controller.ServiceItemController;
import com.cybergame.model.entity.ServiceItem;
import com.cybergame.repository.sql.ServiceItemRepositorySQL;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class ProductMgmtController implements Initializable {

    // --- FXML ELEMENTS ---
    @FXML private TableView<ServiceItem> productTable;
    @FXML private TextField txtSearch;
    @FXML private Label lblTotal;

    @FXML private TableColumn<ServiceItem, Integer> colId;
    @FXML private TableColumn<ServiceItem, String> colName;
    @FXML private TableColumn<ServiceItem, Double> colPrice;

    // --- DEPENDENCIES ---
    private final ServiceItemRepositorySQL serviceRepo = new ServiceItemRepositorySQL();
    private final ServiceItemController serviceCtrl = new ServiceItemController(serviceRepo);

    private ObservableList<ServiceItem> masterData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        loadData();
        setupSearch();

        // 1. CẤU HÌNH MÀU SẮC (SỬA LẠI THÀNH XANH DƯƠNG)
        // Để khi chọn dòng, nền màu xanh dương sẽ làm nổi bật chữ màu xanh lá của giá tiền
        productTable.setRowFactory(tv -> {
            TableRow<ServiceItem> row = new TableRow<>();
            
            // Toggle chọn (Ấn lại thì bỏ chọn)
            row.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
                if (!row.isEmpty() && row.isSelected() && e.getButton() == MouseButton.PRIMARY) {
                    productTable.getSelectionModel().clearSelection();
                    e.consume();
                }
            });

            // Tô màu khi chọn
            row.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                if (!row.isEmpty()) {
                    if (isSelected) {
                        // --- SỬA Ở ĐÂY: Đổi thành màu #007bff (Xanh Dương) ---
                        // Chữ (text-fill) màu trắng áp dụng cho cột Tên
                        // Cột Giá tiền do setup riêng nên nó sẽ giữ màu xanh lá (nổi trên nền xanh dương)
                        row.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-font-weight: bold;");
                    } else {
                        row.setStyle(""); // Reset về mặc định
                    }
                }
            });
            return row;
        });
        
        Platform.runLater(() -> productTable.getSelectionModel().clearSelection());
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("serviceId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));

        // Format Giá tiền
        colPrice.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getUnitPrice()).asObject());
        colPrice.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : String.format("%,.0f đ", item));
                if (!empty) {
                    // Màu xanh lá cho tiền (sẽ nhìn rõ trên nền trắng HOẶC nền xanh dương)
                    setStyle("-fx-text-fill: -success-color; -fx-font-weight: bold; -fx-alignment: CENTER-RIGHT;");
                }
            }
        });
    }

    private void loadData() {
        if (serviceRepo != null) {
            masterData.setAll(serviceRepo.findAll());
            updateTotalLabel();
        }
    }
    
    private void updateTotalLabel() {
        if (lblTotal != null) {
            lblTotal.setText(String.valueOf(masterData.size()));
        }
    }

    private void setupSearch() {
        FilteredList<ServiceItem> filteredData = new FilteredList<>(masterData, p -> true);

        txtSearch.textProperty().addListener((obs, oldVal, newValue) -> {
            filteredData.setPredicate(item -> {
                if (newValue == null || newValue.isEmpty()) return true;
                return item.getName().toLowerCase().contains(newValue.toLowerCase());
            });
        });

        SortedList<ServiceItem> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(productTable.comparatorProperty());
        productTable.setItems(sortedData);
    }

    // ================== SỰ KIỆN NÚT BẤM ==================

    @FXML 
    private void onAdd() { 
        showDialog(null); 
    }

    @FXML 
    private void onUpdate() {
        ServiceItem selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Lỗi", "Vui lòng chọn món cần sửa giá!");
            return;
        }
        showDialog(selected);
    }

    @FXML 
    private void onDelete() {
        ServiceItem selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Lỗi", "Vui lòng chọn món cần xóa!");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText("Bạn có chắc muốn xóa: " + selected.getName() + "?");
        
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                serviceCtrl.delete(selected);
                masterData.remove(selected);
                productTable.getSelectionModel().clearSelection();
                updateTotalLabel();
            } catch (Exception e) {
                showAlert("Lỗi", "Không thể xóa (Có thể món này đã có trong đơn hàng cũ).");
            }
        }
    }

    // ================== DIALOG NHẬP LIỆU ==================
    private void showDialog(ServiceItem existingItem) {
        Dialog<ServiceItem> dialog = new Dialog<>();
        dialog.setTitle(existingItem == null ? "Thêm Món Mới" : "Sửa Giá Món");
        dialog.setHeaderText(null);

        ButtonType btnSave = new ButtonType("Lưu", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSave, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 50, 10, 10));

        TextField txtName = new TextField(); 
        txtName.setPromptText("Tên dịch vụ/món ăn");
        
        TextField txtPrice = new TextField(); 
        txtPrice.setPromptText("Đơn giá (VNĐ)");

        if (existingItem != null) {
            txtName.setText(existingItem.getName());
            txtPrice.setText(String.format("%.0f", existingItem.getUnitPrice()));
        }

        grid.add(new Label("Tên món:"), 0, 0); grid.add(txtName, 1, 0);
        grid.add(new Label("Giá bán:"), 0, 1); grid.add(txtPrice, 1, 1);

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(txtName::requestFocus);

        dialog.setResultConverter(btn -> {
            if (btn == btnSave) {
                try {
                    String name = txtName.getText().trim();
                    double price = Double.parseDouble(txtPrice.getText().trim());
                    return new ServiceItem(0, name, price);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });

        Optional<ServiceItem> result = dialog.showAndWait();
        result.ifPresent(formData -> {
            if (formData.getName().isEmpty()) {
                showAlert("Lỗi", "Tên món không được để trống!");
                return;
            }
            
            try {
                if (existingItem == null) {
                    ServiceItem newItem = serviceCtrl.createService(formData.getName(), formData.getUnitPrice());
                    masterData.add(newItem);
                    showAlert("Thành công", "Đã thêm: " + newItem.getName());
                } else {
                    existingItem.setName(formData.getName());
                    existingItem.setUnitPrice(formData.getUnitPrice());
                    serviceRepo.save(existingItem);
                    productTable.refresh();
                }
                updateTotalLabel();
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Lỗi", "Có lỗi xảy ra khi lưu dữ liệu.");
            }
        });
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }
}
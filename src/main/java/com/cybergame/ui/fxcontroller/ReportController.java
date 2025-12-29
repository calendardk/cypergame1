package com.cybergame.ui.fxcontroller;

import com.cybergame.model.entity.Invoice;
import com.cybergame.model.entity.OrderItem;
import com.cybergame.model.entity.TopUpHistory;
import com.cybergame.repository.sql.InvoiceRepositorySQL;
import com.cybergame.repository.sql.TopUpHistoryRepositorySQL;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ReportController implements Initializable {

    private final InvoiceRepositorySQL invoiceRepo = new InvoiceRepositorySQL();
    private final TopUpHistoryRepositorySQL topUpRepo = new TopUpHistoryRepositorySQL();

    // --- FXML: STATS LABELS ---
    @FXML private Label lblTotalTopUp;     // 🔥 MỚI: TỔNG TIỀN NẠP (TIỀN TƯƠI)
    @FXML private Label lblTotalRevenue;   // Tổng tiêu thụ (Hóa đơn)
    @FXML private Label lblMachineRevenue; // Tiền máy
    @FXML private Label lblServiceRevenue; // Tiền dịch vụ
    
    // --- FXML: FILTER ---
    @FXML private DatePicker dpFrom;
    @FXML private DatePicker dpTo;

    // --- TABLES ---
    @FXML private TableView<Invoice> tableInvoices;
    @FXML private TableColumn<Invoice, Integer> colInvId;
    @FXML private TableColumn<Invoice, String> colInvTime;
    @FXML private TableColumn<Invoice, String> colInvCustomer;
    @FXML private TableColumn<Invoice, String> colInvComputer;
    @FXML private TableColumn<Invoice, Double> colInvService; 
    @FXML private TableColumn<Invoice, Double> colInvMachine; 
    @FXML private TableColumn<Invoice, Double> colInvTotal;

    @FXML private TableView<OrderItem> tableOrderHistory;
    @FXML private TableColumn<OrderItem, String> colOrdTime;
    @FXML private TableColumn<OrderItem, String> colOrdName;
    @FXML private TableColumn<OrderItem, Integer> colOrdQty;
    @FXML private TableColumn<OrderItem, Double> colOrdPrice;
    @FXML private TableColumn<OrderItem, Double> colOrdTotal;

    @FXML private TableView<TopUpHistory> tableTopUps;
    @FXML private TableColumn<TopUpHistory, Integer> colTopId;
    @FXML private TableColumn<TopUpHistory, String> colTopTime;
    @FXML private TableColumn<TopUpHistory, String> colTopCustomer;
    @FXML private TableColumn<TopUpHistory, String> colTopRole;    
    @FXML private TableColumn<TopUpHistory, String> colTopStaffId; 
    @FXML private TableColumn<TopUpHistory, String> colTopOperator;
    @FXML private TableColumn<TopUpHistory, Double> colTopAmount;
    @FXML private TableColumn<TopUpHistory, String> colTopNote;

    private final ObservableList<Invoice> invoiceList = FXCollections.observableArrayList();
    private final ObservableList<TopUpHistory> topUpList = FXCollections.observableArrayList();
    private final ObservableList<OrderItem> orderList = FXCollections.observableArrayList();

    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupInvoiceTable();
        setupOrderTable();
        setupTopUpTable();

        dpFrom.setValue(LocalDate.now());
        dpTo.setValue(LocalDate.now());

        loadData();
    }

    // ... (Giữ nguyên setupInvoiceTable và setupOrderTable như cũ) ...
    private void setupInvoiceTable() {
        colInvId.setCellValueFactory(new PropertyValueFactory<>("invoiceId"));
        colInvTime.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCreatedAt().format(dtf)));
        colInvCustomer.setCellValueFactory(new PropertyValueFactory<>("accountName"));
        colInvComputer.setCellValueFactory(new PropertyValueFactory<>("computerName"));

        colInvService.setCellValueFactory(cell -> {
            Invoice inv = cell.getValue();
            double serviceTotal = (inv.getOrderItems() != null) ? 
                inv.getOrderItems().stream().mapToDouble(item -> item.getCost()).sum() : 0;
            return new SimpleDoubleProperty(serviceTotal).asObject();
        });
        formatCurrencyColumn(colInvService);

        colInvMachine.setCellValueFactory(cell -> {
            Invoice inv = cell.getValue();
            double total = inv.getTotalAmount();
            double serviceTotal = (inv.getOrderItems() != null) ? 
                inv.getOrderItems().stream().mapToDouble(item -> item.getCost()).sum() : 0;
            double machineMoney = total - serviceTotal;
            if(machineMoney < 0) machineMoney = 0; 
            return new SimpleDoubleProperty(machineMoney).asObject();
        });
        formatCurrencyColumn(colInvMachine);

        colInvTotal.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        formatCurrencyColumn(colInvTotal);
        tableInvoices.setItems(invoiceList);
    }

    private void setupOrderTable() {
        colOrdTime.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getOrderedAt().format(dtf)));
        colOrdName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getServiceItem().getName()));
        colOrdQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colOrdPrice.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getServiceItem().getUnitPrice()));
        formatCurrencyColumn(colOrdPrice);
        colOrdTotal.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getCost()));
        formatCurrencyColumn(colOrdTotal);
        tableOrderHistory.setItems(orderList);
    }

    // --- SETUP TOPUP TABLE ---
    private void setupTopUpTable() {
        colTopId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTopTime.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCreatedAt().format(dtf)));
        colTopCustomer.setCellValueFactory(new PropertyValueFactory<>("accountName"));
        colTopRole.setCellValueFactory(new PropertyValueFactory<>("operatorType"));
        
        colTopStaffId.setCellValueFactory(cell -> {
            Integer id = cell.getValue().getOperatorId();
            return new SimpleStringProperty((id == null || id == 0) ? "-" : String.valueOf(id));
        });

        colTopOperator.setCellValueFactory(new PropertyValueFactory<>("operatorName"));
        colTopAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        formatCurrencyColumn(colTopAmount);
        colTopNote.setCellValueFactory(new PropertyValueFactory<>("note"));
        
        tableTopUps.setItems(topUpList);
    }

    @FXML private void onRefresh() {
        dpFrom.setValue(null); dpTo.setValue(null);
        loadData();
    }

    @FXML private void onFilter() { loadData(); }

    private void loadData() {
        LocalDate from = dpFrom.getValue();
        LocalDate to = dpTo.getValue();

        // 1. TOP UP (TIỀN NẠP) - Quan trọng nhất
        List<TopUpHistory> allTopUps = topUpRepo.findAll();
        List<TopUpHistory> filteredTopUps = allTopUps.stream()
            .filter(t -> {
                LocalDate date = t.getCreatedAt().toLocalDate();
                return (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to));
            })
            .collect(Collectors.toList());
        topUpList.setAll(filteredTopUps);

        // Tính tổng tiền nạp
        double totalCashIn = filteredTopUps.stream().mapToDouble(TopUpHistory::getAmount).sum();
        lblTotalTopUp.setText(String.format("%,.0f VNĐ", totalCashIn));

        // 2. INVOICE (DOANH THU TIÊU THỤ)
        List<Invoice> allInvoices = invoiceRepo.findAll();
        List<Invoice> filteredInvoices = allInvoices.stream()
            .filter(i -> {
                LocalDate date = i.getCreatedAt().toLocalDate();
                return (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to));
            })
            .collect(Collectors.toList());
        invoiceList.setAll(filteredInvoices);

        // Tách tiền
        List<OrderItem> allOrderItems = new ArrayList<>();
        double totalRevenue = 0;
        double totalServiceRevenue = 0;
        double totalMachineRevenue = 0;

        for (Invoice inv : filteredInvoices) {
            double invTotal = inv.getTotalAmount();
            double invService = 0;
            if (inv.getOrderItems() != null) {
                allOrderItems.addAll(inv.getOrderItems());
                invService = inv.getOrderItems().stream().mapToDouble(item -> item.getCost()).sum();
            }
            double invMachine = invTotal - invService;
            if (invMachine < 0) invMachine = 0;

            totalRevenue += invTotal;
            totalServiceRevenue += invService;
            totalMachineRevenue += invMachine;
        }
        orderList.setAll(allOrderItems);

        // Cập nhật Label
        lblTotalRevenue.setText(String.format("%,.0f VNĐ", totalRevenue));
        lblMachineRevenue.setText(String.format("%,.0f VNĐ", totalMachineRevenue));
        lblServiceRevenue.setText(String.format("%,.0f VNĐ", totalServiceRevenue));
    }

    private <T> void formatCurrencyColumn(TableColumn<T, Double> col) {
        col.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : String.format("%,.0f", item));
            }
        });
    }
}
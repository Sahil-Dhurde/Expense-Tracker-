import java.awt.*;
import java.sql.*;
import java.util.Vector;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class ExpenseTrackerGUI extends JFrame {

    // Oracle Database Details
    static final String URL = "jdbc:oracle:thin:@localhost:1521:XE";
    static final String USER = "system";
    static final String PASS = "sahil123";

    private Connection con;
    private DefaultTableModel tableModel; 
    private JTable expenseTable;

    // GUI Components for CRUD
    private JTextField idField, dateField, categoryField, descField, amountField;
    private JButton addButton, updateButton, deleteButton;
    private JLabel statusLabel;

    public ExpenseTrackerGUI() {
        super("💳 Personal Expense Tracker (Oracle DB)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Use a clean Look and Feel
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            // Default look and feel will be used if Nimbus fails
        }
        
        // 1. Initialize Database Connection
        connectDatabase();

        // 2. Setup the GUI Structure
        setLayout(new BorderLayout(10, 10)); // Outer layout
        
        // Header Status Panel
        statusLabel = new JLabel("Status: Disconnected", JLabel.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        add(statusLabel, BorderLayout.NORTH);

        // Main Content: Left (Input) and Center (Table)
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.add(setupInputPanel(), BorderLayout.WEST);
        contentPanel.add(setupTablePanel(), BorderLayout.CENTER);
        add(contentPanel, BorderLayout.CENTER);

        // Footer Button Panel
        add(setupButtonPanel(), BorderLayout.SOUTH);

        // Finalize Frame
        setSize(1000, 600);
        setLocationRelativeTo(null); // Center the window
        setVisible(true);

        // Initial Load of Data
        if (con != null) {
            statusLabel.setText("Status: Connected to Oracle Database Successfully!");
            viewExpenses();
        }
    }
    
    // --- Database Connection ---
    private void connectDatabase() {
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            con = DriverManager.getConnection(URL, USER, PASS);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "<html><b>Database Connection Error:</b><br>" + e.getMessage() + "</html>", 
                "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    // --- GUI Panel Setup ---

    private JPanel setupInputPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY), "Expense Details Form", 0, 0, new Font("Segoe UI", Font.BOLD, 16)));
        panel.setPreferredSize(new Dimension(300, 400));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Components initialization
        idField = new JTextField(15);
        dateField = new JTextField("YYYY-MM-DD", 15);
        categoryField = new JTextField(15);
        descField = new JTextField(15);
        amountField = new JTextField(15);

        String[] labels = {"ID:", "Date:", "Category:", "Description:", "Amount:"};
        JTextField[] fields = {idField, dateField, categoryField, descField, amountField};

        for (int i = 0; i < labels.length; i++) {
            // Label
            gbc.gridx = 0;
            gbc.gridy = i;
            panel.add(new JLabel(labels[i]), gbc);

            // Field
            gbc.gridx = 1;
            panel.add(fields[i], gbc);
        }
        return panel;
    }

    private JPanel setupTablePanel() {
        // Table setup - Columns need to match the SELECT query order
        String[] columnNames = {"ID", "Date", "Category", "Description", "Amount"};
        tableModel = new DefaultTableModel(columnNames, 0);
        expenseTable = new JTable(tableModel);
        expenseTable.setFillsViewportHeight(true);
        expenseTable.getTableHeader().setReorderingAllowed(false);
        
        // Listener to populate input fields when a row is selected
        expenseTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && expenseTable.getSelectedRow() != -1) {
                int selectedRow = expenseTable.getSelectedRow();
                idField.setText(tableModel.getValueAt(selectedRow, 0).toString());
                dateField.setText(tableModel.getValueAt(selectedRow, 1).toString());
                categoryField.setText(tableModel.getValueAt(selectedRow, 2).toString());
                descField.setText(tableModel.getValueAt(selectedRow, 3).toString());
                amountField.setText(tableModel.getValueAt(selectedRow, 4).toString());
            }
        });
        
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder("All Expenses"));
        tablePanel.add(new JScrollPane(expenseTable), BorderLayout.CENTER);
        return tablePanel;
    }

    private JPanel setupButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));

        addButton = new JButton("➕ Add Expense");
        updateButton = new JButton("🔄 Update Expense");
        deleteButton = new JButton("❌ Delete Expense");
        JButton viewAllButton = new JButton("📋 Refresh/View All");

        // Action Listeners
        addButton.addActionListener(e -> addExpense());
        updateButton.addActionListener(e -> updateExpense());
        deleteButton.addActionListener(e -> deleteExpense());
        viewAllButton.addActionListener(e -> viewExpenses());

        panel.add(addButton);
        panel.add(updateButton);
        panel.add(deleteButton);
        panel.add(viewAllButton);
        return panel;
    }

    // --- Validation Logic ---

    private boolean validateInputs(boolean isAdd) {
        String idText = idField.getText().trim();
        String dateText = dateField.getText().trim();
        String category = categoryField.getText().trim();
        String desc = descField.getText().trim();
        String amountText = amountField.getText().trim();

        try {
            // 1. ID Validation
            if (idText.isEmpty()) {
                throw new IllegalArgumentException("ID cannot be empty.");
            }
            int id = Integer.parseInt(idText);
            if (id <= 0) {
                throw new IllegalArgumentException("ID must be a positive integer.");
            }
            // 2. Date Validation
            if (dateText.isEmpty() || !dateText.matches("\\d{4}-\\d{2}-\\d{2}")) {
                throw new IllegalArgumentException("Date must be in YYYY-MM-DD format.");
            }
            java.sql.Date.valueOf(dateText); // Check if it's a valid date

            // 3. Category Validation
            if (category.isEmpty() || category.length() > 50) {
                throw new IllegalArgumentException("Category must be between 1 and 50 characters.");
            }

            // 4. Description Validation
            if (desc.isEmpty() || desc.length() > 100) {
                throw new IllegalArgumentException("Description must be between 1 and 100 characters.");
            }

            // 5. Amount Validation
            if (amountText.isEmpty()) {
                throw new IllegalArgumentException("Amount cannot be empty.");
            }
            double amount = Double.parseDouble(amountText);
            if (amount <= 0) {
                throw new IllegalArgumentException("Amount must be a positive number.");
            }
            
            return true;
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "ID and Amount must be valid numbers.", "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Validation Error", JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }

    // --- CRUD Operations (Refactored to use validation) ---

    private void addExpense() {
        if (con == null || !validateInputs(true)) return;
        try {
            String sql = "INSERT INTO expenses (ID, expense_date, category, description, amount) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, Integer.parseInt(idField.getText().trim()));
            ps.setDate(2, java.sql.Date.valueOf(dateField.getText().trim()));
            ps.setString(3, categoryField.getText().trim());
            ps.setString(4, descField.getText().trim());
            ps.setDouble(5, Double.parseDouble(amountField.getText().trim()));

            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Expense added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            viewExpenses();
            clearInputFields();

        } catch (SQLIntegrityConstraintViolationException e) {
             JOptionPane.showMessageDialog(this, "Error: Expense ID already exists. Please choose a unique ID.", "DB Constraint Error", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database Error adding expense: " + e.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateExpense() {
        if (con == null || !validateInputs(false)) return;
        try {
            int id = Integer.parseInt(idField.getText().trim());

            String sql = "UPDATE expenses SET expense_date=?, category=?, description=?, amount=? WHERE id=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setDate(1, java.sql.Date.valueOf(dateField.getText().trim()));
            ps.setString(2, categoryField.getText().trim());
            ps.setString(3, descField.getText().trim());
            ps.setDouble(4, Double.parseDouble(amountField.getText().trim()));
            ps.setInt(5, id);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                JOptionPane.showMessageDialog(this, "Expense updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                viewExpenses();
                clearInputFields();
            } else {
                JOptionPane.showMessageDialog(this, "Expense ID not found!", "Update Failed", JOptionPane.WARNING_MESSAGE);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database Error updating expense: " + e.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteExpense() {
        if (con == null) return;
        String idText = idField.getText().trim();
        if (idText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter an ID to delete.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            int id = Integer.parseInt(idText);

            String sql = "DELETE FROM expenses WHERE id=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, id);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                JOptionPane.showMessageDialog(this, "Expense deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                viewExpenses();
                clearInputFields();
            } else {
                JOptionPane.showMessageDialog(this, "Expense ID not found!", "Delete Failed", JOptionPane.WARNING_MESSAGE);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid numeric ID to delete.", "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database Error deleting expense: " + e.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void viewExpenses() {
        if (con == null) return;
        try {
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery("SELECT ID, expense_date, category, description, amount FROM expenses ORDER BY expense_date DESC");

            // Clear existing data
            tableModel.setRowCount(0);

            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("id"));
                row.add(rs.getDate("expense_date"));
                row.add(rs.getString("category"));
                row.add(rs.getString("description"));
                row.add(rs.getDouble("amount"));
                tableModel.addRow(row);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database Error viewing expenses: " + e.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // --- Utility ---
    private void clearInputFields() {
        idField.setText("");
        dateField.setText("YYYY-MM-DD");
        categoryField.setText("");
        descField.setText("");
        amountField.setText("");
        expenseTable.clearSelection();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ExpenseTrackerGUI());
    }
}

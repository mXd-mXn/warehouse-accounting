import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import javax.sql.DataSource;

// ==================== 0. Base Entity & Exceptions ====================

abstract class BaseEntity {
    private final long id;
    private String name;

    public BaseEntity(long id, String name) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "Name cannot be null");
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = Objects.requireNonNull(name, "Name cannot be null"); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseEntity that = (BaseEntity) o;
        return id == that.id;
    }

    @Override
    public int hashCode() { return Long.hashCode(id); }

    @Override
    public String toString() {
        return String.format("Entity{id=%d, name='%s'}", id, name);
    }
}

class EntityNotFoundException extends RuntimeException {
    private final long id;

    public EntityNotFoundException(long id) {
        super("Entity with id=" + id + " was not found in storage.");
        this.id = id;
    }

    public long getId() { return id; }
}

// ==================== 1. Domain Exceptions ====================

abstract class WarehouseException extends Exception {
    public WarehouseException(String message) { super(message); }
}

class InvalidPriceException extends WarehouseException {
    private final double invalidPrice;
    public InvalidPriceException(String message, double invalidPrice) {
        super(message);
        this.invalidPrice = invalidPrice;
    }
    public double getInvalidPrice() { return invalidPrice; }
}

class InvalidQuantityException extends WarehouseException {
    private final int invalidQuantity;
    public InvalidQuantityException(String message, int invalidQuantity) {
        super(message);
        this.invalidQuantity = invalidQuantity;
    }
    public int getInvalidQuantity() { return invalidQuantity; }
}

// ==================== 2. Interfaces & Strategies ====================

interface PricingStrategy {
    double calculatePrice(double basePrice, int quantity);
    default String getStrategyName() { return "Unknown Pricing Strategy"; }
}

interface Reportable {
    void printReport();
}

abstract class DiscountPricingStrategy implements PricingStrategy {
    protected double discountPercentage;
    public DiscountPricingStrategy(double discountPercentage) { this.discountPercentage = discountPercentage; }
    protected double applyDiscount(double totalAmount) {
        return totalAmount - (totalAmount * (discountPercentage / 100.0));
    }
    public double getDiscountPercentage() { return discountPercentage; }
}

class RegularPriceStrategy implements PricingStrategy {
    @Override
    public double calculatePrice(double basePrice, int quantity) { return basePrice * quantity; }
    @Override
    public String getStrategyName() { return "Regular Price (No Discount)"; }
}

class BulkDiscountStrategy extends DiscountPricingStrategy {
    private final int minimumQuantity;
    public BulkDiscountStrategy(double discountPercentage, int minimumQuantity) {
        super(discountPercentage);
        this.minimumQuantity = minimumQuantity;
    }
    @Override
    public double calculatePrice(double basePrice, int quantity) {
        double total = basePrice * quantity;
        return (quantity >= minimumQuantity) ? applyDiscount(total) : total;
    }
    @Override
    public String getStrategyName() {
        return "Bulk Discount (" + discountPercentage + "% off for " + minimumQuantity + "+ items)";
    }
}

enum StrategyType { REGULAR, BULK_DISCOUNT }

class PricingStrategyFactory {
    public static PricingStrategy createStrategy(StrategyType type, double discountPercentage, int minQuantity) {
        return switch (type) {
            case REGULAR -> new RegularPriceStrategy();
            case BULK_DISCOUNT -> new BulkDiscountStrategy(discountPercentage, minQuantity);
        };
    }
    public static PricingStrategy createStrategy(StrategyType type) {
        return createStrategy(type, 0.0, 0);
    }
}

// ==================== 3. Observer & Adapter Patterns ====================

interface StockObserver {
    void onStockChanged(String productName, String sku, int oldQty, int newQty);
}

class AuditLogger implements StockObserver {
    @Override
    public void onStockChanged(String productName, String sku, int oldQty, int newQty) {
        System.out.printf("[AUDIT LOG] Item '%s' (%s) quantity changed from %d to %d.%n", productName, sku, oldQty, newQty);
    }
}

class LowStockNotifier implements StockObserver {
    private final int threshold;
    public LowStockNotifier(int threshold) { this.threshold = threshold; }
    @Override
    public void onStockChanged(String productName, String sku, int oldQty, int newQty) {
        if (newQty < threshold) {
            System.out.printf("[WARNING ALERT] Low Stock! Item '%s' (%s) is down to %d items (Threshold: %d)!%n",
                    productName, sku, newQty, threshold);
        }
    }
}

class InventorySubject {
    private final List<StockObserver> observers = new ArrayList<>();
    public void attach(StockObserver observer) { observers.add(observer); }
    public void detach(StockObserver observer) { observers.remove(observer); }
    protected void notifyObservers(String productName, String sku, int oldQty, int newQty) {
        for (StockObserver observer : observers) {
            try { observer.onStockChanged(productName, sku, oldQty, newQty); }
            catch (Exception e) { System.err.println("Error notifying observer: " + e.getMessage()); }
        }
    }
}

class ExternalSupplierApi {
    public double fetchCostInCents(int itemsCount, double itemPriceUSD) {
        return (itemsCount * itemPriceUSD * 0.95) * 100.0;
    }
    public String getVendorName() { return "Global Wholesaler Corp."; }
}

class LegacySupplierAdapter implements PricingStrategy {
    private final ExternalSupplierApi externalApi;
    public LegacySupplierAdapter(ExternalSupplierApi externalApi) { this.externalApi = externalApi; }
    @Override
    public double calculatePrice(double basePrice, int quantity) {
        return externalApi.fetchCostInCents(quantity, basePrice) / 100.0;
    }
    @Override
    public String getStrategyName() { return "Adapter -> Vendor: " + externalApi.getVendorName(); }
}

// ==================== 4. Entity Hierarchy ====================

abstract sealed class WarehouseItem extends BaseEntity implements Reportable, Comparable<WarehouseItem> permits PhysicalProduct {
    private final String sku;
    private double price;
    private int quantity;
    private final InventorySubject inventorySubject = new InventorySubject();
    private PricingStrategy pricingStrategy;

    public WarehouseItem(long id, String name, String sku, int quantity, double price, PricingStrategy pricingStrategy) throws WarehouseException {
        validateData(quantity, price);
        super(id, name);
        this.sku = sku;
        this.quantity = quantity;
        this.price = price;
        this.pricingStrategy = pricingStrategy;
    }

    private static void validateData(int quantity, double price) throws WarehouseException {
        if (price < 0) throw new InvalidPriceException("Price cannot be negative.", price);
        if (quantity < 0) throw new InvalidQuantityException("Quantity cannot be negative.", quantity);
    }

    public String getSku() { return sku; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public PricingStrategy getPricingStrategy() { return pricingStrategy; }

    public void setQuantity(int newQuantity) throws WarehouseException {
        if (newQuantity < 0) throw new InvalidQuantityException("Quantity cannot be negative.", newQuantity);
        int oldQuantity = this.quantity;
        this.quantity = newQuantity;
        inventorySubject.notifyObservers(getName(), sku, oldQuantity, newQuantity);
    }

    public void attachObserver(StockObserver observer) { inventorySubject.attach(observer); }
    public void setPricingStrategy(PricingStrategy pricingStrategy) { this.pricingStrategy = pricingStrategy; }
    public double getFinalPrice() { return pricingStrategy.calculatePrice(price, quantity); }

    public abstract String getItemCategory();

    @Override
    public void printReport() {
        System.out.printf("[%s] ID: %d | Item: %-10s (SKU: %s) | Qty: %d | Base Price: %.2f | Final Cost: %.2f | Strategy: %s",
                getItemCategory(), getId(), getName(), sku, quantity, price, getFinalPrice(), pricingStrategy.getStrategyName());
    }

    @Override
    public int compareTo(WarehouseItem other) { return this.getName().compareToIgnoreCase(other.getName()); }

    @Override
    public String toString() {
        return String.format("WarehouseItem{id=%d, name='%s', sku='%s', price=%.2f, quantity=%d}", getId(), getName(), sku, price, quantity);
    }
}

abstract sealed class PhysicalProduct extends WarehouseItem permits PerishableProduct, ElectronicsProduct {
    private final double weightKg;

    public PhysicalProduct(long id, String name, String sku, int quantity, double price, PricingStrategy pricingStrategy, double weightKg) throws WarehouseException {
        super(id, name, sku, quantity, price, pricingStrategy);
        this.weightKg = weightKg;
    }

    public double getWeightKg() { return weightKg; }
}

final class PerishableProduct extends PhysicalProduct {
    private final LocalDate expirationDate;

    public PerishableProduct(long id, String name, String sku, int quantity, double price, PricingStrategy pricingStrategy, double weightKg, LocalDate expirationDate) throws WarehouseException {
        super(id, name, sku, quantity, price, pricingStrategy, weightKg);
        this.expirationDate = expirationDate;
    }

    public LocalDate getExpirationDate() { return expirationDate; }
    @Override public String getItemCategory() { return "PERISHABLE"; }
    @Override
    public void printReport() {
        super.printReport();
        System.out.printf(" | Exp Date: %s%n", expirationDate);
    }
}

final class ElectronicsProduct extends PhysicalProduct {
    private final int warrantyMonths;

    public ElectronicsProduct(long id, String name, String sku, int quantity, double price, PricingStrategy pricingStrategy, double weightKg, int warrantyMonths) throws WarehouseException {
        super(id, name, sku, quantity, price, pricingStrategy, weightKg);
        this.warrantyMonths = warrantyMonths;
    }

    public int getWarrantyMonths() { return warrantyMonths; }
    @Override public String getItemCategory() { return "ELECTRONICS"; }
    @Override
    public void printReport() {
        super.printReport();
        System.out.printf(" | Warranty: %d months%n", warrantyMonths);
    }
}

// ==================== 5. Database Connection (Thread-safe Singleton + HikariCP) ====================

class DBConnection {
    private static volatile DBConnection instance;
    private final HikariDataSource dataSource;

    private DBConnection() {
        try {
            Properties props = new Properties();
            try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
                if (input == null) throw new RuntimeException("application.properties not found!");
                props.load(input);
            }

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(props.getProperty("db.url"));
            config.setUsername(props.getProperty("db.username"));
            config.setPassword(props.getProperty("db.password"));
            config.setDriverClassName(props.getProperty("db.driver"));
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);

            this.dataSource = new HikariDataSource(config);
            initDatabaseSchema();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Database Connection Pool.", e);
        }
    }

    public static DBConnection getInstance() {
        if (instance == null) {
            synchronized (DBConnection.class) {
                if (instance == null) {
                    instance = new DBConnection();
                }
            }
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    // Автоматична ідемпотентна ініціалізація БД (Рівень 2.4)
    private void initDatabaseSchema() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("init.sql");
             Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            if (in != null) {
                String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                stmt.execute(sql);
            }
        } catch (Exception e) {
            System.err.println("Database schema initialization failed: " + e.getMessage());
        }
    }
}

// ==================== 6. Generic CRUD Interfaces & Service Layer ====================

interface CRUDService<T extends BaseEntity> {
    Optional<T> findById(long id);
    T save(T entity);
    boolean delete(long id);
    List<T> findAll();
    List<T> findAllSorted(Comparator<? super T> comparator);

    default List<T> findAllSortedByName() {
        return findAllSorted(Comparator.comparing(BaseEntity::getName, String.CASE_INSENSITIVE_ORDER));
    }
}

interface WarehouseItemService<T extends WarehouseItem> extends CRUDService<T> {
    List<T> findAllSortedByNameAndByPrice();
}

// ==================== 7. High-Level Abstract JDBC Service (Template Method Pattern) ====================

abstract class AbstractJdbcCrudService<T extends BaseEntity> implements CRUDService<T> {

    protected abstract String getTableName();
    protected abstract String getInsertOrUpdateSql();
    protected abstract T mapRow(ResultSet rs) throws SQLException;
    protected abstract void setParameters(PreparedStatement stmt, T entity) throws SQLException;

    protected Connection getConnection() throws SQLException {
        return DBConnection.getInstance().getConnection();
    }

    @Override
    public Optional<T> findById(long id) {
        String sql = "SELECT * FROM " + getTableName() + " WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error executing findById for id=" + id, e);
        }
        return Optional.empty();
    }

    @Override
    public T save(T entity) {
        Objects.requireNonNull(entity, "Entity cannot be null");
        String sql = getInsertOrUpdateSql();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            setParameters(stmt, entity);
            stmt.executeUpdate();
            return entity;
        } catch (SQLException e) {
            throw new RuntimeException("Error saving entity with id=" + entity.getId(), e);
        }
    }

    @Override
    public boolean delete(long id) {
        String sql = "DELETE FROM " + getTableName() + " WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting entity with id=" + id, e);
        }
    }

    @Override
    public List<T> findAll() {
        String sql = "SELECT * FROM " + getTableName();
        List<T> result = new ArrayList<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching all records.", e);
        }
        return result;
    }

    @Override
    public List<T> findAllSorted(Comparator<? super T> comparator) {
        // У випадку використання Comparator сортування реалізуємо в пам’яті Java
        List<T> list = findAll();
        list.sort(comparator);
        return list;
    }

    // Рівень 3.4: Пагінація через SQL
    public List<T> findAllPaged(int limit, int offset) {
        String sql = "SELECT * FROM " + getTableName() + " LIMIT ? OFFSET ?";
        List<T> result = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            stmt.setInt(2, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Pagination error", e);
        }
        return result;
    }

    // Рівень 3.3: Batch Insert
    public void saveAllBatch(List<T> entities) {
        String sql = getInsertOrUpdateSql();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            for (T entity : entities) {
                setParameters(stmt, entity);
                stmt.addBatch();
            }
            stmt.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Batch insert failed", e);
        }
    }
}

// ==================== 8. JDBC Implementation of WarehouseItemService ====================

class JdbcWarehouseItemServiceImpl extends AbstractJdbcCrudService<WarehouseItem> implements WarehouseItemService<WarehouseItem> {

    @Override
    protected String getTableName() {
        return "warehouse_items";
    }

    @Override
    protected String getInsertOrUpdateSql() {
        return """
            MERGE INTO warehouse_items (id, name, sku, quantity, price, item_type, weight_kg, expiration_date, warranty_months, strategy_type, discount_percentage)
            KEY(id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
    }

    @Override
    protected void setParameters(PreparedStatement stmt, WarehouseItem entity) throws SQLException {
        stmt.setLong(1, entity.getId());
        stmt.setString(2, entity.getName());
        stmt.setString(3, entity.getSku());
        stmt.setInt(4, entity.getQuantity());
        stmt.setDouble(5, entity.getPrice());
        stmt.setString(6, entity.getItemCategory());

        if (entity instanceof PhysicalProduct phys) {
            stmt.setDouble(7, phys.getWeightKg());
        } else {
            stmt.setNull(7, Types.NUMERIC);
        }

        if (entity instanceof PerishableProduct perishable) {
            stmt.setDate(8, java.sql.Date.valueOf(perishable.getExpirationDate()));
        } else {
            stmt.setNull(8, Types.DATE);
        }

        if (entity instanceof ElectronicsProduct electronics) {
            stmt.setInt(9, electronics.getWarrantyMonths());
        } else {
            stmt.setNull(9, Types.INTEGER);
        }

        PricingStrategy strategy = entity.getPricingStrategy();
        if (strategy instanceof BulkDiscountStrategy bulk) {
            stmt.setString(10, StrategyType.BULK_DISCOUNT.name());
            stmt.setDouble(11, bulk.getDiscountPercentage());
        } else {
            stmt.setString(10, StrategyType.REGULAR.name());
            stmt.setDouble(11, 0.0);
        }
    }

    @Override
    protected WarehouseItem mapRow(ResultSet rs) throws SQLException {
        try {
            long id = rs.getLong("id");
            String name = rs.getString("name");
            String sku = rs.getString("sku");
            int quantity = rs.getInt("quantity");
            double price = rs.getDouble("price");
            String itemType = rs.getString("item_type");
            double weight = rs.getDouble("weight_kg");
            String stratType = rs.getString("strategy_type");
            double discountPct = rs.getDouble("discount_percentage");

            PricingStrategy strategy = "BULK_DISCOUNT".equals(stratType) 
                    ? PricingStrategyFactory.createStrategy(StrategyType.BULK_DISCOUNT, discountPct, 10)
                    : PricingStrategyFactory.createStrategy(StrategyType.REGULAR);

            if ("PERISHABLE".equalsIgnoreCase(itemType)) {
                java.sql.Date sqlDate = rs.getDate("expiration_date");
                LocalDate expDate = (sqlDate != null) ? sqlDate.toLocalDate() : LocalDate.now().plusDays(1);
                return new PerishableProduct(id, name, sku, quantity, price, strategy, weight, expDate);
            } else {
                int warranty = rs.getInt("warranty_months");
                return new ElectronicsProduct(id, name, sku, quantity, price, strategy, weight, warranty);
            }
        } catch (WarehouseException e) {
            throw new SQLException("Failed to instantiate domain object from DB row.", e);
        }
    }

    // Рівень 2.2: Виконання сортування безпосередньо через SQL ORDER BY
    @Override
    public List<WarehouseItem> findAllSortedByNameAndByPrice() {
        String sql = "SELECT * FROM warehouse_items ORDER BY name ASC, price ASC";
        List<WarehouseItem> result = new ArrayList<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error executing sorted SQL query", e);
        }
        return result;
    }

    // Рівень 2.5: Операція у транзакції з відкатом (Rollback Demo)
    public void executeBulkPriceUpdateWithTransaction(double priceAdjustment, boolean triggerFailure) {
        String sql = "UPDATE warehouse_items SET price = price + ?";
        Connection conn = null;
        try {
            conn = DBConnection.getInstance().getConnection();
            conn.setAutoCommit(false); // Старт транзакції

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setDouble(1, priceAdjustment);
                stmt.executeUpdate();
            }

            if (triggerFailure) {
                throw new SQLException("Simulated database constraint violation!");
            }

            conn.commit(); // Фіксація транзакції
            System.out.println("[TRANSACTION SUCCESS] Prices updated successfully.");
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback(); // Відкат при помилці
                    System.out.println("[TRANSACTION ROLLBACK] Rolled back all changes due to: " + e.getMessage());
                } catch (SQLException ex) {
                    System.err.println("Rollback failed: " + ex.getMessage());
                }
            }
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("Failed to reset connection state: " + e.getMessage());
                }
            }
        }
    }
}

// ==================== 9. Application Entry Point (Демонстрація) ====================

void main() {
    try (var scanner = new Scanner(System.in)) {
        System.out.println("=== Warehouse Management System (Maven / JDBC / HikariCP Implementation) ===\n");

        PricingStrategy regularStrategy = PricingStrategyFactory.createStrategy(StrategyType.REGULAR);
        PricingStrategy bulkStrategy = PricingStrategyFactory.createStrategy(StrategyType.BULK_DISCOUNT, 15.0, 20);

        // Впроваджуємо JDBC-сервіс замість InMemoryCrudService (Інтерфейс сервісу не змінився!)
        JdbcWarehouseItemServiceImpl warehouseService = new JdbcWarehouseItemServiceImpl();

        System.out.println("--- 1. Demonstrating CRUD Operations via JDBC ---");

        WarehouseItem laptop = new ElectronicsProduct(101L, "Laptop", "SKU101", 10, 1000.0, regularStrategy, 2.3, 24);
        WarehouseItem milkFresh = new PerishableProduct(102L, "Milk", "SKU102", 50, 2.0, bulkStrategy, 1.0, LocalDate.now().plusDays(7));
        WarehouseItem milkOrganic = new PerishableProduct(103L, "Milk", "SKU103", 20, 4.5, regularStrategy, 1.0, LocalDate.now().plusDays(5));
        WarehouseItem keyboard = new ElectronicsProduct(104L, "Keyboard", "SKU104", 15, 40.0, regularStrategy, 0.8, 12);

        warehouseService.save(laptop);
        warehouseService.save(milkFresh);
        warehouseService.save(milkOrganic);
        warehouseService.save(keyboard);

        System.out.println("Saved items into DB:");
        printAll(warehouseService.findAll());

        System.out.println("Updating Laptop (ID 101) with new price...");
        WarehouseItem updatedLaptop = new ElectronicsProduct(101L, "Laptop Pro", "SKU101", 8, 1300.0, regularStrategy, 2.3, 36);
        warehouseService.save(updatedLaptop);

        System.out.println("Delete ID 104 (Keyboard): " + warehouseService.delete(104L));

        System.out.println("\nSearching for non-existent ID 999:");
        try {
            _ = warehouseService.findById(999L)
                    .orElseThrow(() -> new EntityNotFoundException(999L));
        } catch (EntityNotFoundException e) {
            System.out.println("Caught Expected Exception: " + e.getMessage());
        }

        warehouseService.save(keyboard);

        System.out.println("\n--- 2. Database Sorting & Transactions ---");
        System.out.println("1. Sorted by Name (findAllSortedByName):");
        printAll(warehouseService.findAllSortedByName());

        System.out.println("2. Sorted via SQL ORDER BY (findAllSortedByNameAndByPrice):");
        printAll(warehouseService.findAllSortedByNameAndByPrice());

        System.out.println("\n--- 3. Demonstrating Transaction Rollback ---");
        System.out.println("Attempting failed transaction update (+100 to price with intentional crash):");
        warehouseService.executeBulkPriceUpdateWithTransaction(100.0, true);

        System.out.println("Verifying DB prices were NOT modified (Rollback succeeded):");
        printAll(warehouseService.findAll());

    } catch (WarehouseException e) {
        System.out.println("DOMAIN ERROR: " + e.getMessage());
    } finally {
        System.out.println("\nExecution complete.");
    }
}

void printAll(Collection<? extends BaseEntity> items) {
    System.out.println("-----------------------------------------------------------------------------------------");
    for (BaseEntity item : items) {
        System.out.println("  -> " + item);
    }
    System.out.println("-----------------------------------------------------------------------------------------");
}
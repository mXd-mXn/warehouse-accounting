import java.time.LocalDate;
import java.util.*;

public class Main {

    // ==================== 0. Base Entity & Lab 7-9 Exceptions ====================

    /**
     * BaseEntity class (Lab 6 / Lab 7-9).
     * Contains immutable 'id' and mutable 'name'.
     */
    public static abstract class BaseEntity {
        private final long id;
        private String name;

        public BaseEntity(long id, String name) {
            this.id = id;
            this.name = Objects.requireNonNull(name, "Name cannot be null");
        }

        public long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = Objects.requireNonNull(name, "Name cannot be null");
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BaseEntity that = (BaseEntity) o;
            return id == that.id;
        }

        @Override
        public int hashCode() {
            return Long.hashCode(id);
        }

        @Override
        public String toString() {
            return String.format("Entity{id=%d, name='%s'}", id, name);
        }
    }

    /**
     * Custom Unchecked Exception for missing entities (Lab 7-9, Level 1).
     */
    public static class EntityNotFoundException extends RuntimeException {
        private final long id;

        public EntityNotFoundException(long id) {
            super("Entity with id=" + id + " was not found in storage.");
            this.id = id;
        }

        public long getId() {
            return id;
        }
    }

    // ==================== 1. Domain Exceptions (Your Code) ====================

    public static abstract class WarehouseException extends Exception {
        public WarehouseException(String message) {
            super(message);
        }
    }

    public static class InvalidPriceException extends WarehouseException {
        private final double invalidPrice;

        public InvalidPriceException(String message, double invalidPrice) {
            super(message);
            this.invalidPrice = invalidPrice;
        }

        public double getInvalidPrice() {
            return invalidPrice;
        }
    }

    public static class InvalidQuantityException extends WarehouseException {
        private final int invalidQuantity;

        public InvalidQuantityException(String message, int invalidQuantity) {
            super(message);
            this.invalidQuantity = invalidQuantity;
        }

        public int getInvalidQuantity() {
            return invalidQuantity;
        }
    }

    // ==================== 2. Interfaces & Strategies (Your Code) ====================

    public interface PricingStrategy {
        double calculatePrice(double basePrice, int quantity);

        default String getStrategyName() {
            return "Unknown Pricing Strategy";
        }
    }

    public interface Reportable {
        void printReport();
    }

    public static abstract class DiscountPricingStrategy implements PricingStrategy {
        protected double discountPercentage;

        public DiscountPricingStrategy(double discountPercentage) {
            this.discountPercentage = discountPercentage;
        }

        protected double applyDiscount(double totalAmount) {
            return totalAmount - (totalAmount * (discountPercentage / 100.0));
        }
    }

    public static class RegularPriceStrategy implements PricingStrategy {
        @Override
        public double calculatePrice(double basePrice, int quantity) {
            return basePrice * quantity;
        }

        @Override
        public String getStrategyName() {
            return "Regular Price (No Discount)";
        }
    }

    public static class BulkDiscountStrategy extends DiscountPricingStrategy {
        private final int minimumQuantity;

        public BulkDiscountStrategy(double discountPercentage, int minimumQuantity) {
            super(discountPercentage);
            this.minimumQuantity = minimumQuantity;
        }

        @Override
        public double calculatePrice(double basePrice, int quantity) {
            double total = basePrice * quantity;
            if (quantity >= minimumQuantity) {
                return applyDiscount(total);
            }
            return total;
        }

        @Override
        public String getStrategyName() {
            return "Bulk Discount (" + discountPercentage + "% off for " + minimumQuantity + "+ items)";
        }
    }

    public enum StrategyType {
        REGULAR,
        BULK_DISCOUNT
    }

    public static class PricingStrategyFactory {
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

    // ==================== 3. Observer Pattern (Your Code) ====================

    public interface StockObserver {
        void onStockChanged(String productName, String sku, int oldQty, int newQty);
    }

    public static class AuditLogger implements StockObserver {
        @Override
        public void onStockChanged(String productName, String sku, int oldQty, int newQty) {
            System.out.printf("[AUDIT LOG] Item '%s' (%s) quantity changed from %d to %d.%n", productName, sku, oldQty, newQty);
        }
    }

    public static class LowStockNotifier implements StockObserver {
        private final int threshold;

        public LowStockNotifier(int threshold) {
            this.threshold = threshold;
        }

        @Override
        public void onStockChanged(String productName, String sku, int oldQty, int newQty) {
            if (newQty < threshold) {
                System.out.printf("[WARNING ALERT] Low Stock! Item '%s' (%s) is down to %d items (Threshold: %d)!%n",
                        productName, sku, newQty, threshold);
            }
        }
    }

    public static class InventorySubject {
        private final List<StockObserver> observers = new ArrayList<>();

        public void attach(StockObserver observer) {
            observers.add(observer);
        }

        public void detach(StockObserver observer) {
            observers.remove(observer);
        }

        protected void notifyObservers(String productName, String sku, int oldQty, int newQty) {
            for (StockObserver observer : observers) {
                try {
                    observer.onStockChanged(productName, sku, oldQty, newQty);
                } catch (Exception e) {
                    System.err.println("Error notifying observer: " + e.getMessage());
                }
            }
        }
    }

    // ==================== 4. Adapter Pattern (Your Code) ====================

    public static class ExternalSupplierApi {
        public double fetchCostInCents(int itemsCount, double itemPriceUSD) {
            double totalUsd = itemsCount * itemPriceUSD * 0.95;
            return totalUsd * 100.0;
        }

        public String getVendorName() {
            return "Global Wholesaler Corp.";
        }
    }

    public static class LegacySupplierAdapter implements PricingStrategy {
        private final ExternalSupplierApi externalApi;

        public LegacySupplierAdapter(ExternalSupplierApi externalApi) {
            this.externalApi = externalApi;
        }

        @Override
        public double calculatePrice(double basePrice, int quantity) {
            double costInCents = externalApi.fetchCostInCents(quantity, basePrice);
            return costInCents / 100.0;
        }

        @Override
        public String getStrategyName() {
            return "Adapter -> Vendor: " + externalApi.getVendorName() + " (5% Wholesale Integration)";
        }
    }

    // ==================== 5. Entity Hierarchy (Your Code + BaseEntity) ====================

    /**
     * WarehouseItem now extends BaseEntity to fulfill Lab 7-9 requirements.
     */
    public static abstract sealed class WarehouseItem extends BaseEntity implements Reportable, Comparable<WarehouseItem> permits PhysicalProduct {
        private final String sku;
        private double price;
        private int quantity;

        private final InventorySubject inventorySubject = new InventorySubject();
        private PricingStrategy pricingStrategy;

        public WarehouseItem(long id, String name, String sku, int quantity, double price, PricingStrategy pricingStrategy) throws WarehouseException {
            super(id, name);
            validateData(quantity, price);
            this.sku = sku;
            this.quantity = quantity;
            this.price = price;
            this.pricingStrategy = pricingStrategy;
        }

        private void validateData(int quantity, double price) throws WarehouseException {
            if (price < 0) {
                throw new InvalidPriceException("Price cannot be negative.", price);
            }
            if (quantity < 0) {
                throw new InvalidQuantityException("Quantity cannot be negative.", quantity);
            }
        }

        public String getSku() { return sku; }
        public double getPrice() { return price; }
        public int getQuantity() { return quantity; }
        public PricingStrategy getPricingStrategy() { return pricingStrategy; }

        public void setQuantity(int newQuantity) throws WarehouseException {
            if (newQuantity < 0) {
                throw new InvalidQuantityException("Quantity cannot be negative.", newQuantity);
            }
            int oldQuantity = this.quantity;
            this.quantity = newQuantity;
            inventorySubject.notifyObservers(getName(), sku, oldQuantity, newQuantity);
        }

        public void attachObserver(StockObserver observer) {
            inventorySubject.attach(observer);
        }

        public void setPricingStrategy(PricingStrategy pricingStrategy) {
            this.pricingStrategy = pricingStrategy;
        }

        public double getFinalPrice() {
            return pricingStrategy.calculatePrice(price, quantity);
        }

        public abstract String getItemCategory();

        @Override
        public void printReport() {
            System.out.printf("[%s] ID: %d | Item: %-10s (SKU: %s) | Qty: %d | Base Price: %.2f | Final Cost: %.2f | Strategy: %s",
                    getItemCategory(), getId(), getName(), sku, quantity, price, getFinalPrice(), pricingStrategy.getStrategyName());
        }

        /**
         * Natural ordering (Comparable) by Name (case-insensitive).
         */
        @Override
        public int compareTo(WarehouseItem other) {
            return this.getName().compareToIgnoreCase(other.getName());
        }

        @Override
        public boolean equals(Object o) {
            return super.equals(o); // Equals defined by ID in BaseEntity
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public String toString() {
            return String.format("WarehouseItem{id=%d, name='%s', sku='%s', price=%.2f, quantity=%d}", getId(), getName(), sku, price, quantity);
        }
    }

    public static abstract sealed class PhysicalProduct extends WarehouseItem permits PerishableProduct, ElectronicsProduct {
        private final double weightKg;

        public PhysicalProduct(long id, String name, String sku, int quantity, double price, PricingStrategy pricingStrategy) throws WarehouseException {
            this(id, name, sku, quantity, price, pricingStrategy, 1.0);
        }

        public PhysicalProduct(long id, String name, String sku, int quantity, double price, PricingStrategy pricingStrategy, double weightKg) throws WarehouseException {
            super(id, name, sku, quantity, price, pricingStrategy);
            this.weightKg = weightKg;
        }

        public double getWeightKg() {
            return weightKg;
        }

        @Override
        public String toString() {
            return super.toString() + String.format(" [PhysicalProduct{weight=%.2fkg}]", weightKg);
        }
    }

    public static final class PerishableProduct extends PhysicalProduct {
        private final LocalDate expirationDate;

        public PerishableProduct(long id, String name, String sku, int quantity, double price, PricingStrategy pricingStrategy, double weightKg, LocalDate expirationDate) throws WarehouseException {
            super(id, name, sku, quantity, price, pricingStrategy, weightKg);
            this.expirationDate = expirationDate;
        }

        public LocalDate getExpirationDate() {
            return expirationDate;
        }

        @Override
        public String getItemCategory() {
            return "PERISHABLE";
        }

        @Override
        public void printReport() {
            super.printReport();
            System.out.printf(" | Exp Date: %s%n", expirationDate);
        }
    }

    public static final class ElectronicsProduct extends PhysicalProduct {
        private final int warrantyMonths;

        public ElectronicsProduct(long id, String name, String sku, int quantity, double price, PricingStrategy pricingStrategy, double weightKg, int warrantyMonths) throws WarehouseException {
            super(id, name, sku, quantity, price, pricingStrategy, weightKg);
            this.warrantyMonths = warrantyMonths;
        }

        public int getWarrantyMonths() {
            return warrantyMonths;
        }

        public final void extendWarranty(int extraMonths) {
            System.out.printf("Warranty for '%s' extended by %d months.%n", getName(), extraMonths);
        }

        @Override
        public String getItemCategory() {
            return "ELECTRONICS";
        }

        @Override
        public void printReport() {
            super.printReport();
            System.out.printf(" | Warranty: %d months%n", warrantyMonths);
        }
    }

    // ==================== 6. Generic CRUD Service Architecture (Lab 7-9) ====================

    /**
     * Generic CRUD Service Interface.
     * Level 1 & Level 3.
     */
    public interface CRUDService<T extends BaseEntity> {
        Optional<T> findById(long id);

        T save(T entity);

        boolean delete(long id);

        List<T> findAll();

        /**
         * Generic sort method accepting wildcards: Comparator<? super T>.
         * Level 3.
         */
        List<T> findAllSorted(Comparator<? super T> comparator);

        /**
         * Default method sorting by name using natural order or comparator.
         * Level 3.
         */
        default List<T> findAllSortedByName() {
            return findAllSorted(Comparator.comparing(BaseEntity::getName, String.CASE_INSENSITIVE_ORDER));
        }
    }

    /**
     * Entity-specific interface extending CRUDService.
     * Level 2.
     */
    public interface WarehouseItemService<T extends WarehouseItem> extends CRUDService<T> {
        List<T> findAllSortedByNameAndByPrice();
    }

    /**
     * In-Memory CRUD implementation using LinkedHashMap.
     * Level 1, 2, 3.
     */
    public static class InMemoryCrudService<T extends BaseEntity> implements CRUDService<T> {
        protected final Map<Long, T> storage = new LinkedHashMap<>();

        @Override
        public Optional<T> findById(long id) {
            return Optional.ofNullable(storage.get(id));
        }

        @Override
        public T save(T entity) {
            Objects.requireNonNull(entity, "Entity cannot be null");
            storage.put(entity.getId(), entity);
            return entity;
        }

        @Override
        public boolean delete(long id) {
            return storage.remove(id) != null;
        }

        @Override
        public List<T> findAll() {
            return new ArrayList<>(storage.values());
        }

        @Override
        public List<T> findAllSorted(Comparator<? super T> comparator) {
            Objects.requireNonNull(comparator, "Comparator cannot be null");
            List<T> sortedList = new ArrayList<>(storage.values());
            sortedList.sort(comparator);
            return sortedList;
        }
    }

    /**
     * Concrete implementation for WarehouseItem.
     * Level 2.
     */
    public static class WarehouseItemServiceImpl<T extends WarehouseItem>
            extends InMemoryCrudService<T>
            implements WarehouseItemService<T> {

        @Override
        public List<T> findAllSortedByNameAndByPrice() {
            // Secondary sort by price when names are identical
            Comparator<T> nameAndPriceComparator = Comparator
                    .<T, String>comparing(BaseEntity::getName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparingDouble(WarehouseItem::getPrice);

            return findAllSorted(nameAndPriceComparator);
        }
    }

    // ==================== 7. Utility Methods ====================

    /**
     * Print utility method with wildcard upper bound Collection<? extends BaseEntity>.
     * Level 3.
     */
    public static void printAll(Collection<? extends BaseEntity> items) {
        System.out.println("-----------------------------------------------------------------------------------------");
        for (BaseEntity item : items) {
            System.out.println("  -> " + item);
        }
        System.out.println("-----------------------------------------------------------------------------------------");
    }

    // ==================== Main Method ====================
    public static void main(String[] args) {
        var scanner = new Scanner(System.in);

        try {
            System.out.println("=== Warehouse Management System (Lab 7-9 Integration) ===\n");

            PricingStrategy regularStrategy = PricingStrategyFactory.createStrategy(StrategyType.REGULAR);
            PricingStrategy bulkStrategy = PricingStrategyFactory.createStrategy(StrategyType.BULK_DISCOUNT, 15.0, 20);

            // Instantiate Generic Service
            WarehouseItemServiceImpl<WarehouseItem> warehouseService = new WarehouseItemServiceImpl<>();

            System.out.println("--- 1. Demonstrating CRUD Operations (Level 1) ---");

            WarehouseItem laptop = new ElectronicsProduct(101L, "Laptop", "SKU101", 10, 1000.0, regularStrategy, 2.3, 24);
            WarehouseItem milkFresh = new PerishableProduct(102L, "Milk", "SKU102", 50, 2.0, bulkStrategy, 1.0, LocalDate.now().plusDays(7));
            WarehouseItem milkOrganic = new PerishableProduct(103L, "Milk", "SKU103", 20, 4.5, regularStrategy, 1.0, LocalDate.now().plusDays(5));
            WarehouseItem keyboard = new ElectronicsProduct(104L, "Keyboard", "SKU104", 15, 40.0, regularStrategy, 0.8, 12);

            // Save items
            warehouseService.save(laptop);
            warehouseService.save(milkFresh);
            warehouseService.save(milkOrganic);
            warehouseService.save(keyboard);

            System.out.println("Saved 4 items to In-Memory storage:");
            printAll(warehouseService.findAll());

            // Updating item (ID 101)
            System.out.println("Updating Laptop (ID 101) with new price...");
            WarehouseItem updatedLaptop = new ElectronicsProduct(101L, "Laptop Pro", "SKU101", 8, 1300.0, regularStrategy, 2.3, 36);
            warehouseService.save(updatedLaptop);
            System.out.println("Total count after update (should remain 4): " + warehouseService.findAll().size());

            // Delete operations
            System.out.println("Delete ID 104 (Keyboard): " + warehouseService.delete(104L));
            System.out.println("Delete ID 999 (Non-existent): " + warehouseService.delete(999L));

            // Exception handling on missing ID
            System.out.println("\nSearching for non-existent ID 999:");
            try {
                WarehouseItem found = warehouseService.findById(999L)
                        .orElseThrow(() -> new EntityNotFoundException(999L));
            } catch (EntityNotFoundException e) {
                System.out.println("Caught Expected Exception: " + e.getMessage());
            }

            // Restore Keyboard for sorting tests
            warehouseService.save(keyboard);

            System.out.println("\n--- 2. Sorting Operations & Wildcards (Level 2 & 3) ---");

            System.out.println("Storage BEFORE sorting:");
            printAll(warehouseService.findAll());

            System.out.println("1. Sorted by Name (findAllSortedByName):");
            printAll(warehouseService.findAllSortedByName());

            System.out.println("2. Sorted by Name AND Price (Primary: Name, Secondary: Price):");
            printAll(warehouseService.findAllSortedByNameAndByPrice());

            System.out.println("Storage AFTER sorting (verifying storage was NOT mutated):");
            printAll(warehouseService.findAll());

            System.out.println("\n--- 3. Observer & Legacy Adapter Integration ---");

            StockObserver auditLogger = new AuditLogger();
            StockObserver lowStockNotifier = new LowStockNotifier(5);

            laptop.attachObserver(auditLogger);
            laptop.attachObserver(lowStockNotifier);

            System.out.println("Updating Laptop quantity from 10 to 3...");
            laptop.setQuantity(3);

            ExternalSupplierApi legacyApi = new ExternalSupplierApi();
            PricingStrategy adapterStrategy = new LegacySupplierAdapter(legacyApi);
            keyboard.setPricingStrategy(adapterStrategy);
            System.out.println("Updated Keyboard pricing strategy using Legacy Adapter.");
            keyboard.printReport();

        } catch (WarehouseException e) {
            System.out.println("DOMAIN ERROR: " + e.getMessage());
        } finally {
            System.out.println("\nExecution complete.");
            scanner.close();
        }
    }
}
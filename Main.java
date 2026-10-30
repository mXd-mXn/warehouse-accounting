import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

public class Main {

    // ==================== 1. Винятки (Exceptions) ====================
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

    // ==================== 2. Інтерфейси та Стратегії ====================
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

    // ==================== 3. Патерн Спостерігач (Observer) ====================
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

    // ==================== 4. Патерн Адаптер (Adapter) ====================
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

    // ==================== 5. Ієрархія сутностей (Sealed & Static) ====================

    public static abstract sealed class WarehouseItem implements Reportable, Comparable<WarehouseItem> permits PhysicalProduct {
        private final String name;
        private final String sku;
        private final double price;
        private int quantity;

        private final InventorySubject inventorySubject = new InventorySubject();
        private PricingStrategy pricingStrategy;

        public WarehouseItem(String name, String sku, int quantity, double price, PricingStrategy pricingStrategy) throws WarehouseException {
            System.out.println("  [Constructor WarehouseItem] Initializing base warehouse item...");
            validateData(quantity, price);
            this.name = name;
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

        public String getName() { return name; }
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
            inventorySubject.notifyObservers(name, sku, oldQuantity, newQuantity);
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
            System.out.printf("[%s] Item: %-10s (SKU: %s) | Qty: %d | Base Price: %.2f | Final Cost: %.2f | Strategy: %s",
                    getItemCategory(), name, sku, quantity, price, getFinalPrice(), pricingStrategy.getStrategyName());
        }

        @Override
        public int compareTo(WarehouseItem other) {
            return Double.compare(this.getFinalPrice(), other.getFinalPrice());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            WarehouseItem that = (WarehouseItem) o;
            return Double.compare(that.price, price) == 0 &&
                    quantity == that.quantity &&
                    Objects.equals(name, that.name) &&
                    Objects.equals(sku, that.sku);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, sku, price, quantity);
        }

        @Override
        public String toString() {
            return String.format("WarehouseItem{name='%s', sku='%s', price=%.2f, quantity=%d}", name, sku, price, quantity);
        }
    }

    public static abstract sealed class PhysicalProduct extends WarehouseItem permits PerishableProduct, ElectronicsProduct {
        private final double weightKg;

        public PhysicalProduct(String name, String sku, int quantity, double price, PricingStrategy pricingStrategy) throws WarehouseException {
            this(name, sku, quantity, price, pricingStrategy, 1.0);
        }

        public PhysicalProduct(String name, String sku, int quantity, double price, PricingStrategy pricingStrategy, double weightKg) throws WarehouseException {
            super(name, sku, quantity, price, pricingStrategy);
            System.out.println("  [Constructor PhysicalProduct] Initializing physical product attributes...");
            this.weightKg = weightKg;
        }

        public double getWeightKg() {
            return weightKg;
        }

        @Override
        public String toString() {
            return super.toString() + String.format(" [PhysicalProduct{weight=%.2fkg}]", weightKg);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            PhysicalProduct that = (PhysicalProduct) o;
            return Double.compare(that.weightKg, weightKg) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), weightKg);
        }
    }

    public static final class PerishableProduct extends PhysicalProduct {
        private final LocalDate expirationDate;

        public PerishableProduct(String name, String sku, int quantity, double price, PricingStrategy pricingStrategy, double weightKg, LocalDate expirationDate) throws WarehouseException {
            super(name, sku, quantity, price, pricingStrategy, weightKg);
            System.out.println("  [Constructor PerishableProduct] Initializing perishable product attributes...");
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            PerishableProduct that = (PerishableProduct) o;
            return Objects.equals(expirationDate, that.expirationDate);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), expirationDate);
        }
    }

    public static final class ElectronicsProduct extends PhysicalProduct {
        private final int warrantyMonths;

        public ElectronicsProduct(String name, String sku, int quantity, double price, PricingStrategy pricingStrategy, double weightKg, int warrantyMonths) throws WarehouseException {
            super(name, sku, quantity, price, pricingStrategy, weightKg);
            System.out.println("  [Constructor ElectronicsProduct] Initializing electronics attributes...");
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            ElectronicsProduct that = (ElectronicsProduct) o;
            return warrantyMonths == that.warrantyMonths;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), warrantyMonths);
        }
    }

    // ==================== Main Method ====================
    public static void main(String[] args) {
        var scanner = new Scanner(System.in);

        try {
            System.out.println("=== Warehouse Management System ===\n");

            PricingStrategy regularFactoryStrategy = PricingStrategyFactory.createStrategy(StrategyType.REGULAR);
            PricingStrategy bulkFactoryStrategy = PricingStrategyFactory.createStrategy(StrategyType.BULK_DISCOUNT, 15.0, 20);

            System.out.println("--- 1. Chain of Constructors Demonstration ---");
            System.out.println("Creating Laptop (ElectronicsProduct):");
            WarehouseItem laptop = new ElectronicsProduct("Laptop", "SKU101", 10, 1000.0, regularFactoryStrategy, 2.3, 24);

            System.out.println("\nCreating Milk (PerishableProduct):");
            WarehouseItem milk = new PerishableProduct("Milk", "SKU102", 50, 2.0, bulkFactoryStrategy, 1.0, LocalDate.now().plusDays(7));

            System.out.println("\n--- 2. Adapter Pattern & Polymorphism ---");
            ExternalSupplierApi legacyApi = new ExternalSupplierApi();
            PricingStrategy adapterStrategy = new LegacySupplierAdapter(legacyApi);
            WarehouseItem keyboard = new ElectronicsProduct("Keyboard", "SKU103", 15, 40.0, adapterStrategy, 0.8, 12);

            WarehouseItem[] inventory = { laptop, milk, keyboard };

            System.out.println("\n--- Sorting and Printing Reports Polymorphically ---");
            Arrays.sort(inventory);
            for (WarehouseItem item : inventory) {
                item.printReport();
            }

            System.out.println("\n--- 3. Specific Subclass Calls & Pattern Matching ---");
            for (WarehouseItem item : inventory) {
                switch (item) {
                    case ElectronicsProduct electronics -> {
                        System.out.printf("Found Electronics: %s with weight %.1fkg. ", electronics.getName(), electronics.getWeightKg());
                        electronics.extendWarranty(6);
                    }
                    case PerishableProduct perishable -> {
                        System.out.printf("Found Perishable item: %s. Expiration: %s%n", perishable.getName(), perishable.getExpirationDate());
                    }
                }
            }

            System.out.println("\n--- 4. Observer Pattern Demonstration ---");
            StockObserver auditLogger = new AuditLogger();
            StockObserver lowStockNotifier = new LowStockNotifier(5);

            laptop.attachObserver(auditLogger);
            laptop.attachObserver(lowStockNotifier);

            System.out.println("Updating Laptop quantity from 10 to 3...");
            laptop.setQuantity(3);

            System.out.println("\n--- 5. Exception Handling Check ---");
            System.out.print("Enter test price for a new product: ");
            double testPrice = scanner.nextDouble();

            WarehouseItem testProduct = new ElectronicsProduct("TestItem", "SKU000", 10, testPrice, regularFactoryStrategy, 0.5, 6);
            System.out.println("Product successfully created. Final Price: " + testProduct.getFinalPrice());

        } catch (InputMismatchException e) {
            System.out.println("INPUT ERROR: Please enter a valid number.");
        } catch (WarehouseException e) {
            System.out.println("DOMAIN ERROR: " + e.getMessage());
            switch (e) {
                case InvalidPriceException ipe ->
                    System.out.println("Rejected price value: " + ipe.getInvalidPrice());
                case InvalidQuantityException iqe ->
                    System.out.println("Rejected quantity value: " + iqe.getInvalidQuantity());
                default -> {}
            }
        } finally {
            System.out.println("\nExecuting final cleanup...");
            scanner.close();
            System.out.println("Scanner closed.");
        }
    }
}
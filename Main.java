import java.util.ArrayList;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

abstract class WarehouseException extends Exception {
    public WarehouseException(String message) {
        super(message);
    }
}

class InvalidPriceException extends WarehouseException {
    private final double invalidPrice;

    public InvalidPriceException(String message, double invalidPrice) {
        super(message);
        this.invalidPrice = invalidPrice;
    }

    public double getInvalidPrice() {
        return invalidPrice;
    }
}

class InvalidQuantityException extends WarehouseException {
    private final int invalidQuantity;

    public InvalidQuantityException(String message, int invalidQuantity) {
        super(message);
        this.invalidQuantity = invalidQuantity;
    }

    public int getInvalidQuantity() {
        return invalidQuantity;
    }
}

interface PricingStrategy {
    double calculatePrice(double basePrice, int quantity);

    default String getStrategyName() {
        return "Unknown Pricing Strategy";
    }
}

interface Reportable {
    void printReport();
}

abstract class DiscountPricingStrategy implements PricingStrategy {
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

enum StrategyType {
    REGULAR,
    BULK_DISCOUNT
}

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

interface StockObserver {
    void onStockChanged(String productName, String sku, int oldQty, int newQty);
}

class AuditLogger implements StockObserver {
    @Override
    public void onStockChanged(String productName, String sku, int oldQty, int newQty) {
        System.out.printf("[AUDIT LOG] Product '%s' (%s) quantity changed from %d to %d.%n", productName, sku, oldQty, newQty);
    }
}

class LowStockNotifier implements StockObserver {
    private final int threshold;

    public LowStockNotifier(int threshold) {
        this.threshold = threshold;
    }

    @Override
    public void onStockChanged(String productName, String sku, int oldQty, int newQty) {
        if (newQty < threshold) {
            System.out.printf("[WARNING ALERT] Low Stock! Product '%s' (%s) is down to %d items (Threshold: %d)!%n",
                    productName, sku, newQty, threshold);
        }
    }
}

// Subject (Суб'єкт)
class InventorySubject {
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

class ExternalSupplierApi {
    public double fetchCostInCents(int itemsCount, double itemPriceUSD) {
        double totalUsd = itemsCount * itemPriceUSD * 0.95;
        return totalUsd * 100.0;
    }

    public String getVendorName() {
        return "Global Wholesaler Corp.";
    }
}

class LegacySupplierAdapter implements PricingStrategy {
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

class Product extends InventorySubject implements Reportable, Comparable<Product> {
    private final String name;
    private final String sku;
    private final double price;
    private int quantity;
    private PricingStrategy pricingStrategy;

    public Product(String name, String sku, int quantity, double price, PricingStrategy pricingStrategy) throws WarehouseException {
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

    public void setQuantity(int newQuantity) throws WarehouseException {
        if (newQuantity < 0) {
            throw new InvalidQuantityException("Quantity cannot be negative.", newQuantity);
        }
        int oldQuantity = this.quantity;
        this.quantity = newQuantity;
        notifyObservers(name, sku, oldQuantity, newQuantity);
    }

    public void setPricingStrategy(PricingStrategy pricingStrategy) {
        this.pricingStrategy = pricingStrategy;
    }

    public double getFinalPrice() {
        return pricingStrategy.calculatePrice(price, quantity);
    }

    @Override
    public void printReport() {
        System.out.printf("Product: %-10s (SKU: %s) | Qty: %d | Base Price: %.2f | Final Cost: %.2f | Strategy: %s%n",
                name, sku, quantity, price, getFinalPrice(), pricingStrategy.getStrategyName());
    }

    @Override
    public int compareTo(Product other) {
        return Double.compare(this.getFinalPrice(), other.getFinalPrice());
    }
}

void main() {
    var scanner = new Scanner(System.in);

    try {
        System.out.println("=== Warehouse Management System (Java 25) ===\n");

        // 1. Factory Method
        System.out.println("--- 1. Factory Method Demonstration ---");
        PricingStrategy regularFactoryStrategy = PricingStrategyFactory.createStrategy(StrategyType.REGULAR);
        PricingStrategy bulkFactoryStrategy = PricingStrategyFactory.createStrategy(StrategyType.BULK_DISCOUNT, 15.0, 20);

        Product laptop = new Product("Laptop", "SKU101", 10, 1000.0, regularFactoryStrategy);
        Product mouse = new Product("Mouse", "SKU102", 50, 20.0, bulkFactoryStrategy);

        // 2. Adapter
        System.out.println("\n--- 2. Adapter Pattern Demonstration ---");
        ExternalSupplierApi legacyApi = new ExternalSupplierApi();
        PricingStrategy adapterStrategy = new LegacySupplierAdapter(legacyApi);
        Product keyboard = new Product("Keyboard", "SKU103", 15, 40.0, adapterStrategy);

        Product[] inventory = { laptop, mouse, keyboard };

        System.out.println("\n--- Current Inventory ---");
        Arrays.sort(inventory);
        for (var product : inventory) {
            product.printReport();
        }

        // 3. Observer
        System.out.println("\n--- 3. Observer Pattern Demonstration ---");
        StockObserver auditLogger = new AuditLogger();
        StockObserver lowStockNotifier = new LowStockNotifier(5);

        laptop.attach(auditLogger);
        laptop.attach(lowStockNotifier);

        System.out.println("Updating Laptop quantity from 10 to 8...");
        laptop.setQuantity(8);

        System.out.println("\nUpdating Laptop quantity from 8 to 3 (Triggers Low Stock Alert)...");
        laptop.setQuantity(3);

        // 4. Exception Handling
        System.out.println("\n--- 4. Exception Handling Check ---");
        System.out.print("Enter test price for a new product: ");
        double testPrice = scanner.nextDouble();

        Product testProduct = new Product("ErrorItem", "SKU000", 10, testPrice, regularFactoryStrategy);
        System.out.println("Product successfully created: " + testProduct.getFinalPrice());

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
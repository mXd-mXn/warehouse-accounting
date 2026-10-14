import java.util.Arrays;
import java.util.InputMismatchException;
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

class RegularPriceStrategy implements PricingStrategy {
    @Override
    public double calculatePrice(double basePrice, int quantity) {
        return basePrice * quantity;
    }

    @Override
    public String getStrategyName() {
        return "Regular Price (No Discount)";
    }
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

class Product implements Reportable, Comparable<Product> {
    private String name;
    private String sku;
    private int quantity;
    private double price;
    private PricingStrategy pricingStrategy;

    // Конструктор тепер перевіряє дані і може кинути виняток (ЛР №3)
    public Product(String name, String sku, int quantity, double price, PricingStrategy pricingStrategy) throws WarehouseException {
        validateData(quantity, price);
        this.name = name;
        this.sku = sku;
        this.quantity = quantity;
        this.price = price;
        this.pricingStrategy = pricingStrategy;
    }

    // ЛР №3: Валідація даних
    private void validateData(int quantity, double price) throws WarehouseException {
        if (price < 0) {
            throw new InvalidPriceException("Price cannot be negative.", price);
        }
        if (quantity < 0) {
            throw new InvalidQuantityException("Quantity cannot be negative.", quantity);
        }
    }

    // ЛР №4: Зміна стратегії в Runtime
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
    
    // ЛР №3: Обробка винятків через try-catch-finally
    try {
        System.out.println("--- Warehouse System (Labs 3 & 4) ---\n");
        
        // Створюємо товари. Навмисно створюємо один з некоректною ціною для перевірки винятків
        System.out.println("Attempting to create products...");
        
        var validProduct1 = new Product("Laptop", "SKU101", 1, 1000.0, new RegularPriceStrategy());
        var validProduct2 = new Product("Mouse", "SKU102", 50, 20.0, new BulkDiscountStrategy(15.0, 20));
        
        Product[] inventory = { validProduct1, validProduct2 };
        
        // ЛР №4: Поліморфізм та сортування
        System.out.println("\n--- Inventory (Sorted by Final Cost) ---");
        Arrays.sort(inventory);
        for (var product : inventory) {
            product.printReport();
        }
        
        // ЛР №4: Зміна поведінки (Strategy) в Runtime
        System.out.println("\n--- Black Friday! Changing strategy for Laptop ---");
        validProduct1.setPricingStrategy(new BulkDiscountStrategy(25.0, 1));
        validProduct1.printReport();

        // Спровокуємо помилку для ЛР №3
        System.out.println("\n--- Testing Exception Handling ---");
        System.out.print("Enter price for a test product: ");
        double testPrice = scanner.nextDouble(); // Введіть від'ємне число, щоб побачити помилку
        
        // Це кине виняток, якщо ціна < 0
        var invalidProduct = new Product("ErrorItem", "SKU000", 10, testPrice, new RegularPriceStrategy());
        System.out.println("Product created successfully (This won't print if price is negative).");

    } catch (InputMismatchException e) {
        System.out.println("INPUT ERROR: Please enter a valid number.");
    } catch (WarehouseException e) {
        // ЛР №3: Множинна обробка доменних помилок
        System.out.println("DOMAIN ERROR: " + e.getMessage());
        if (e instanceof InvalidPriceException ipe) {
            System.out.println("Rejected price value: " + ipe.getInvalidPrice());
        } else if (e instanceof InvalidQuantityException iqe) {
            System.out.println("Rejected quantity value: " + iqe.getInvalidQuantity());
        }
    } finally {
        System.out.println("\nExecuting final cleanup...");
        scanner.close();
        System.out.println("Scanner closed.");
    }
}
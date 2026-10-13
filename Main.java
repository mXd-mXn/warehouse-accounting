import java.util.Arrays;

// Рівень 1: Інтерфейс, що описує варіативну поведінку (Strategy)
interface PricingStrategy {
    double calculatePrice(double basePrice, int quantity);
    
    // Рівень 2: Default-метод в інтерфейсі
    default String getStrategyName() {
        return "Unknown Pricing Strategy";
    }
}

// Рівень 2: Додатковий інтерфейс для демонстрації множинної реалізації
interface Reportable {
    void printReport();
}

// Рівень 2: Абстрактний клас, що частково реалізує інтерфейс
abstract class DiscountPricingStrategy implements PricingStrategy {
    protected double discountPercentage;

    public DiscountPricingStrategy(double discountPercentage) {
        this.discountPercentage = discountPercentage;
    }

    // Спільна логіка застосування знижки, щоб не дублювати її в підкласах
    protected double applyDiscount(double totalAmount) {
        return totalAmount - (totalAmount * (discountPercentage / 100.0));
    }
}

// Рівень 1 & 3: Конкретна реалізація №1 (Без знижки)
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

// Рівень 1 & 3: Конкретна реалізація №2 (Оптова знижка)
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

// Рівень 1 & 3: Конкретна реалізація №3 (Сезонна знижка на все)
class SeasonalDiscountStrategy extends DiscountPricingStrategy {
    public SeasonalDiscountStrategy(double discountPercentage) {
        super(discountPercentage);
    }

    @Override
    public double calculatePrice(double basePrice, int quantity) {
        return applyDiscount(basePrice * quantity);
    }

    @Override
    public String getStrategyName() {
        return "Seasonal Discount (" + discountPercentage + "% off everything)";
    }
}

// Рівень 3: Контекстний клас (Strategy Pattern)
// Рівень 2: Множинна імплементація інтерфейсів (Reportable та Comparable)
class Product implements Reportable, Comparable<Product> {
    private String name;
    private String sku;
    private int quantity;
    private double price;
    
    // Делегування поведінки через інтерфейс
    private PricingStrategy pricingStrategy;

    public Product(String name, String sku, int quantity, double price, PricingStrategy pricingStrategy) {
        this.name = name;
        this.sku = sku;
        this.quantity = quantity;
        this.price = price;
        this.pricingStrategy = pricingStrategy;
    }

    // Дозволяє змінювати стратегію під час виконання (Runtime)
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

    // Окрема поведінка сортування (порівняння за фінальною ціною)
    @Override
    public int compareTo(Product other) {
        return Double.compare(this.getFinalPrice(), other.getFinalPrice());
    }
}

void main() {
    System.out.println("--- Warehouse Pricing System (Strategy Pattern) ---\n");

    // Створення об'єктів з різними початковими стратегіями
    var laptop = new Product("Laptop", "SKU101", 1, 1000.0, new RegularPriceStrategy());
    var mouse = new Product("Mouse", "SKU102", 50, 20.0, new BulkDiscountStrategy(15.0, 20));
    var keyboard = new Product("Keyboard", "SKU103", 5, 50.0, new SeasonalDiscountStrategy(10.0));

    Product[] inventory = { laptop, mouse, keyboard };

    // Рівень 1: Поліморфне використання в циклі
    System.out.println("Initial State:");
    for (var product : inventory) {
        product.printReport();
    }

    // Рівень 2: Демонстрація сортування (Comparable)
    System.out.println("\nSorted by Final Cost:");
    Arrays.sort(inventory);
    for (var product : inventory) {
        product.printReport();
    }

    // Рівень 3: Зміна стратегії під час виконання (Runtime)
    System.out.println("\n--- Black Friday Event! Changing strategies dynamically ---");
    
    // Змінюємо стратегію для ноутбука без створення нового об'єкта Product
    laptop.setPricingStrategy(new SeasonalDiscountStrategy(25.0));
    
    // Змінюємо стратегію для мишок на ще більшу оптову знижку
    mouse.setPricingStrategy(new BulkDiscountStrategy(30.0, 10));

    for (var product : inventory) {
        product.printReport();
    }
}
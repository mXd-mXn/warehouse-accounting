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

record Product(String name, String sku, int quantity, double price) {
    @Override
    public String toString() {
        return "Product: %s (SKU: %s) | Quantity: %d | Price: %.2f".formatted(name, sku, quantity, price);
    }
}

void main() {
    var scanner = new Scanner(System.in);
    Product[] products = null;

    try {
        System.out.println("--- Warehouse accounting system ---");
        System.out.print("Enter the number of products to add: ");
        int n = scanner.nextInt();
        scanner.nextLine();

        products = new Product[n];

        for (int i = 0; i < n; i++) {
            System.out.println("\nEntering data for product #" + (i + 1));
            System.out.print("Name: ");
            var name = scanner.nextLine();
            
            System.out.print("SKU: ");
            var sku = scanner.nextLine();
            
            System.out.print("Quantity: ");
            int quantity = scanner.nextInt();
            
            System.out.print("Price: ");
            double price = scanner.nextDouble();
            scanner.nextLine();

            products[i] = createProduct(name, sku, quantity, price);
        }

        System.out.print("\nEnter product index to view details: ");
        int indexToView = scanner.nextInt();
        System.out.println(products[indexToView]);

    } catch (InputMismatchException e) {
        System.out.println("Input error: Please enter valid numbers for quantity, price, and array sizes.");
    } catch (ArrayIndexOutOfBoundsException e) {
        System.out.println("Index error: The index you entered is outside the valid range of the array.");
    } catch (WarehouseException e) {
        System.out.println("Domain error: " + e.getMessage());
        if (e instanceof InvalidPriceException ipe) {
            System.out.println("Rejected price value: " + ipe.getInvalidPrice());
        } else if (e instanceof InvalidQuantityException iqe) {
            System.out.println("Rejected quantity value: " + iqe.getInvalidQuantity());
        }
    } finally {
        System.out.println("\nExecuting final cleanup...");
        scanner.close();
        System.out.println("Scanner closed. Warehouse system shutdown.");
    }
}

Product createProduct(String name, String sku, int quantity, double price) throws WarehouseException {
    try {
        validateProductData(quantity, price);
        return new Product(name, sku, quantity, price);
    } catch (WarehouseException e) {
        System.out.println("[SYSTEM LOG]: Attempted to create an invalid product (" + name + ").");
        throw e;
    }
}

void validateProductData(int quantity, double price) throws WarehouseException {
    if (price < 0) {
        throw new InvalidPriceException("Price cannot be negative.", price);
    }
    if (quantity < 0) {
        throw new InvalidQuantityException("Quantity cannot be negative.", quantity);
    }
}
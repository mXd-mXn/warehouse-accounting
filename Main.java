import java.util.Scanner;

record Product(String name, String sku, int quantity, double price) {
    @Override
    public String toString() {
        return "Product: %s (SKU: %s) | Quantity: %d | Price: %.2f".formatted(name, sku, quantity, price);
    }
}

void main() {
    var scanner = new Scanner(System.in);

    System.out.println("--- Warehouse accounting system ---");
    System.out.print("Enter the number of products to add to the array: ");
    int n = scanner.nextInt();
    scanner.nextLine();

    var products = new Product[n];

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

        products[i] = new Product(name, sku, quantity, price);
    }

    System.out.println("\n--- List of products in stock ---");
    for (var product : products) {
        System.out.println(product);
    }

    double thresholdPrice = 1000.0;
    int expensiveCount = 0;
    for (var product : products) {
        if (product.price() > thresholdPrice) {
            expensiveCount++;
        }
    }
    System.out.println("\nNumber of products more expensive than " + thresholdPrice + ": " + expensiveCount);

    System.out.println("\n--- Sorting products by price in ascending order ---");
    bubbleSort(products);
    for (var product : products) {
        System.out.println(product);
    }

    System.out.println("\n--- Searching for a reference product ---");
    var targetProduct = new Product("Laptop", "SKU123", 5, 25000.0);
    System.out.println("Searching for: " + targetProduct);

    int foundIndex = linearSearch(products, targetProduct);
    if (foundIndex != -1) {
        System.out.println("Product found in the array at index: " + foundIndex);
    } else {
        System.out.println("Product with exact characteristics not found.");
    }

    scanner.close();
}

void bubbleSort(Product[] arr) {
    int n = arr.length;
    for (int i = 0; i < n - 1; i++) {
        for (int j = 0; j < n - i - 1; j++) {
            if (arr[j].price() > arr[j + 1].price()) {
                 var temp = arr[j];
                 arr[j] = arr[j + 1];
                 arr[j + 1] = temp;
            }
        }
    }
}

int linearSearch(Product[] arr, Product target) {
    for (int i = 0; i < arr.length; i++) {
        if (arr[i].equals(target)) {
            return i;
        }
    }
    return -1;
}

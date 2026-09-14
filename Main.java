import java.util.Scanner;

void main() {
Scanner  scanner = new Scanner(System.in);

System.out.print("--- Warehouse accounting ---\n");

System.out.print("Enter product name: ");
String name = scanner.nextLine();
System.out.print("Enter article: ");
String sku = scanner.nextLine();
System.out.print("Enter quantity: ");
int quantity = scanner.nextInt();
System.out.print("Enter product price: ");
double price = scanner.nextDouble();
System.out.print("Enter product weight: ");
double weight = scanner.nextDouble();
System.out.print("Enter if product avalible: ");
boolean isInStock = scanner.nextBoolean();
double totalValue = quantity * price;
double totalWeight = weight * quantity;

String stockStatus;
if (totalValue > 50000.0) {
    stockStatus = "High cost of the lot (Premium)";
} else {
    stockStatus = "Regular stock";
}

System.out.println("\n --- Product report ---");
System.out.printf("Product name:  %s%n (sku: %s)%n", name , sku);
System.out.printf("Count: %d%n", quantity);
System.out.printf("Cost per piece: %.2f%n", price);
System.out.printf("Prduct weight: %.2f%n", weight);
System.out.printf("Total product value: %.2f%n", totalValue);
System.out.printf("Total product weight: %.2f%n", totalWeight);
System.out.printf("Status: %s%n", stockStatus);
System.out.printf("Is product avalible: %b%n", isInStock);

scanner.close();
}

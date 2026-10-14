# Warehouse Accounting
A modular Java-based command-line application designed for warehouse inventory management and tracking. Developed as part of a structured academic project series.

## Features
- **Product Entity Management**: Tracks core product attributes including unique SKU, name, category, quantity, price, weight, and stock availability status.
- **Automated Calculations**: Computes total stock value and aggregate physical weight dynamically.
- **Conditional Stock Status**: Categorizes inventory levels using conditional branching logic.
- **Formatted Reporting**: Outputs clean, structured product summaries using localized console formatting.

# Lab 2: Processing Arrays Using Loops

**Domain:** Warehouse accounting

## Description
The project implements a console application for managing an array of products (`Product`). 
The program allows you to:
1. Enter product data from the keyboard and store it in an array of objects.
2. Display the list of products using the overridden `toString()` method.
3. Count the number of products that meet a certain condition (e.g., price higher than a given value).
4. Sort the array of objects by price using the **Bubble Sort** algorithm.
5. Perform a linear search for a specific reference object in the array using the overridden `equals()` method.

# Lab 3: Exception Handling and Custom Exceptions

**Domain:** Warehouse accounting

## Description
This project implements robust exception handling for a warehouse accounting application. It demonstrates the use of standard and custom exceptions to validate domain rules and handle unexpected runtime errors.

**Key Features:**
1. **Standard Exception Handling:** Uses `try-catch-finally` to safely handle `InputMismatchException` (invalid user input types) and `ArrayIndexOutOfBoundsException` (accessing invalid product indices).
2. **Custom Exception Hierarchy:** Implements a base `WarehouseException` (checked exception) with two subclasses: `InvalidPriceException` and `InvalidQuantityException`. Each custom exception carries additional context (the rejected value).
3. **Advanced Flow Control:** Demonstrates multiple `catch` blocks ordered from specific to general, exception re-throwing (logging errors before passing them up the call stack), and guaranteed resource cleanup using the `finally` block to close the `Scanner`.

# Lab 4: Interfaces, Polymorphism, and Strategy Pattern

**Domain:** Warehouse accounting

## Description
This project demonstrates advanced Object-Oriented Programming principles in Java, focusing on dynamic behavior modification using interfaces and the **Strategy Pattern**. 

**Key Features:**
1. **Interfaces & Polymorphism:** Defines a `PricingStrategy` interface. An array of products delegates price calculation to various strategy implementations transparently.
2. **Default Methods:** The interface includes a `default getStrategyName()` method to avoid duplicating basic behavior across all implementations.
3. **Abstract Classes:** A `DiscountPricingStrategy` abstract class provides shared discount calculation logic, adhering to the DRY (Don't Repeat Yourself) principle.
4. **Multiple Interfaces:** The `Product` class implements both `Reportable` (business behavior) and `Comparable` (sorting behavior) simultaneously.
5. **Strategy Pattern:** The `Product` context class accepts a `PricingStrategy` via its constructor and a setter. This allows the system to change pricing rules (e.g., applying a Black Friday discount) dynamically at runtime without recreating the objects.

## How to launch:
Download file from github

	git clone https://github.com/mXd-mXn/warehouse-accounting.git

Compile code

	javac Main.java
	
Launch file

	java Main

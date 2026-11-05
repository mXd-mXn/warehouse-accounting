import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.List;

void main() {
    System.out.println("=== Warehouse Management System (Hibernate ORM / Jakarta Persistence) ===\n");

    HibernateWarehouseItemServiceImpl service = new HibernateWarehouseItemServiceImpl();

    // ------------------ Рівень 1: Базові CRUD операції ------------------
    System.out.println("--- Рівень 1: Демонстрація CRUD ---");
    Category electronicsCategory = new Category(null, "Електроніка");
    Category foodCategory = new Category(null, "Продукти харчування");

    JpaUtil.executeInTransaction(em -> {
        em.persist(electronicsCategory);
        em.persist(foodCategory);
    });

    WarehouseItemEntity laptop = new WarehouseItemEntity(null, "Ноутбук Dell", "SKU-LAP-01", 10, new BigDecimal("25000.00"), StrategyType.REGULAR);
    electronicsCategory.addItem(laptop);

    WarehouseItemEntity milk = new WarehouseItemEntity(null, "Молоко 2.5%", "SKU-MILK-01", 100, new BigDecimal("45.00"), StrategyType.BULK_DISCOUNT);
    foodCategory.addItem(milk);

    service.save(laptop);
    service.save(milk);

    System.out.println("Збережені записи:");
    service.findAll().forEach(item -> System.out.println("  -> " + item));

    // Оновлення
    laptop.setPrice(new BigDecimal("27000.00"));
    service.save(laptop);
    System.out.println("Після оновлення ціни ноутбука: " + service.findByIdOrThrow(laptop.getId()));

    // Тест EntityNotFoundException
    System.out.println("\nТест виключення для неіснуючого ID (999):");
    try {
        service.findByIdOrThrow(999L);
    } catch (EntityNotFoundException e) {
        System.out.println("Успішно перехоплено: " + e.getMessage());
    }

    // ------------------ Рівень 3: JPQL & Пагінація ------------------
    System.out.println("\n--- Рівень 3: JPQL Запити ---");
    System.out.println("1. Сортування за назвою та ціною:");
    service.findAllSortedByNameAndPrice().forEach(i -> System.out.println("  -> " + i));

    System.out.println("\n2. Пошук за категорією 'Продукти харчування':");
    service.findByCategory(foodCategory).forEach(i -> System.out.println("  -> " + i));

    System.out.println("\n3. Пагінація (offset=0, limit=1):");
    service.findPaged(0, 1).forEach(i -> System.out.println("  -> " + i));

    // ------------------ Демонстрація типових проблем ORM ------------------
    System.out.println("\n--- Демонстрація типових проблем ---");
    demoLazyInitializationException(laptop.getId());
    demoNPlusOneProblem(service);

    JpaUtil.close();
    System.out.println("\nРобота завершена успішно.");
}

private static void demoLazyInitializationException(Long id) {
    System.out.println("\n--- Демонстрація LazyInitializationException ---");
    WarehouseItemEntity detachedItem = JpaUtil.executeWithResult(em -> em.find(WarehouseItemEntity.class, id));
        
    try {
        // Звернення до LAZY-зв'язку поза сесією EntityManager
        System.out.println("Категорія товару: " + detachedItem.getCategory().getName());
    } catch (org.hibernate.LazyInitializationException e) {
        System.out.println("ПІДТВЕРДЖЕНО Помилку LazyInitializationException!");
        System.out.println("Причина: Спроба завантажити LAZY-властивість без активної транзакції/сесії.");
    }
}

    private static void demoNPlusOneProblem(HibernateWarehouseItemServiceImpl service) {
    System.out.println("\n--- Демонстрація проблеми N+1 та її рішення ---");
    System.out.println("1. Без JOIN FETCH (генерує 1 запит на список + N запитів на кожну категорію при ітерації):");
    List<WarehouseItemEntity> itemsNoFetch = service.findAll();
    JpaUtil.executeInTransaction(em -> {
        for (WarehouseItemEntity item : itemsNoFetch) {
            WarehouseItemEntity managed = em.merge(item);
            if (managed.getCategory() != null) {
                System.out.println("  - Товар: " + managed.getName() + " -> Категорія: " + managed.getCategory().getName());
            }
        }
    });

    System.out.println("\n2. З виправленням через JOIN FETCH (все завантажується за 1 SQL-запит):");
    List<WarehouseItemEntity> itemsWithFetch = service.findAllWithCategoryFetch();
    for (WarehouseItemEntity item : itemsWithFetch) {
        System.out.println("  - Товар: " + item.getName() + " -> Категорія: " + (item.getCategory() != null ? item.getCategory().getName() : "N/A"));
    }
}
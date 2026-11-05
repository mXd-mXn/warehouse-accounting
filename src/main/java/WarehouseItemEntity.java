import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "warehouse_items")
public class WarehouseItemEntity extends BaseEntity {

    @Column(name = "sku", nullable = false, unique = true, length = 50)
    private String sku;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "price", nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "item_type", nullable = false, length = 50)
    private String itemType;

    @Column(name = "weight_kg", precision = 10, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "warranty_months")
    private Integer warrantyMonths;

    @Enumerated(EnumType.STRING)
    @Column(name = "strategy_type", length = 50)
    private StrategyType strategyType = StrategyType.REGULAR;

    @Column(name = "discount_percentage", precision = 5, scale = 2)
    private BigDecimal discountPercentage = BigDecimal.ZERO;

    public WarehouseItemEntity() {}

    public WarehouseItemEntity(Long id, String name, String sku, Integer quantity, BigDecimal price, StrategyType strategyType) {
        super(id, name);
        this.sku = sku;
        this.quantity = quantity;
        this.price = price;
        this.strategyType = strategyType;
        this.itemType = "GENERAL";
    }

    // Getters and Setters
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }

    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }

    public LocalDate getExpirationDate() { return expirationDate; }
    public void setExpirationDate(LocalDate expirationDate) { this.expirationDate = expirationDate; }

    public Integer getWarrantyMonths() { return warrantyMonths; }
    public void setWarrantyMonths(Integer warrantyMonths) { this.warrantyMonths = warrantyMonths; }

    public StrategyType getStrategyType() { return strategyType; }
    public void setStrategyType(StrategyType strategyType) { this.strategyType = strategyType; }

    public BigDecimal getDiscountPercentage() { return discountPercentage; }
    public void setDiscountPercentage(BigDecimal discountPercentage) { this.discountPercentage = discountPercentage; }

    @Override
    public String toString() {
        return String.format("WarehouseItemEntity{id=%d, name='%s', sku='%s', price=%s, qty=%d, strategy=%s}",
                getId(), getName(), sku, price, quantity, strategyType);
    }
}
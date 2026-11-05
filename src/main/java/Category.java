import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories")
public class Category extends BaseEntity {

    @OneToMany(mappedBy = "category", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private List<WarehouseItemEntity> items = new ArrayList<>();

    public Category() {}

    public Category(Long id, String name) {
        super(id, name);
    }

    public List<WarehouseItemEntity> getItems() { return items; }

    // Допоміжні синхронізаційні методи для підтримки цілісності зв'язку в пам'яті
    public void addItem(WarehouseItemEntity item) {
        items.add(item);
        item.setCategory(this);
    }

    public void removeItem(WarehouseItemEntity item) {
        items.remove(item);
        item.setCategory(null);
    }

    @Override
    public String toString() {
        return "Category{id=" + getId() + ", name='" + getName() + "'}";
    }
}
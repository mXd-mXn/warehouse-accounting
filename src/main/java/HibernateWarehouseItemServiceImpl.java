import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.TypedQuery;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class HibernateWarehouseItemServiceImpl implements CRUDService<WarehouseItemEntity> {

    @Override
    public Optional<WarehouseItemEntity> findById(long id) {
        return JpaUtil.executeWithResult(em -> {
            WarehouseItemEntity item = em.find(WarehouseItemEntity.class, id);
            return Optional.ofNullable(item);
        });
    }

    public WarehouseItemEntity findByIdOrThrow(long id) {
        return findById(id).orElseThrow(() -> new EntityNotFoundException("Entity with id=" + id + " was not found."));
    }

    @Override
    public WarehouseItemEntity save(WarehouseItemEntity entity) {
        return JpaUtil.executeWithResult(em -> {
            if (entity.getId() == null) {
                em.persist(entity);
                return entity;
            } else {
                return em.merge(entity);
            }
        });
    }

    @Override
    public boolean delete(long id) {
        return JpaUtil.executeWithResult(em -> {
            WarehouseItemEntity item = em.find(WarehouseItemEntity.class, id);
            if (item == null) {
                throw new EntityNotFoundException("Entity with id=" + id + " was not found for deletion.");
            }
            em.remove(item);
            return true;
        });
    }

    @Override
    public List<WarehouseItemEntity> findAll() {
        return JpaUtil.executeWithResult(em -> 
            em.createQuery("SELECT i FROM WarehouseItemEntity i", WarehouseItemEntity.class).getResultList()
        );
    }

    @Override
    public List<WarehouseItemEntity> findAllSorted(Comparator<? super WarehouseItemEntity> comparator) {
        List<WarehouseItemEntity> list = findAll();
        list.sort(comparator);
        return list;
    }

    // === Рівень 3: JPQL методи ===

    public List<WarehouseItemEntity> findAllSortedByNameAndPrice() {
        return JpaUtil.executeWithResult(em -> {
            String jpql = "SELECT i FROM WarehouseItemEntity i ORDER BY i.name ASC, i.price ASC";
            return em.createQuery(jpql, WarehouseItemEntity.class).getResultList();
        });
    }

    public List<WarehouseItemEntity> findByCategory(Category category) {
        return JpaUtil.executeWithResult(em -> {
            String jpql = "SELECT i FROM WarehouseItemEntity i WHERE i.category = :category";
            TypedQuery<WarehouseItemEntity> query = em.createQuery(jpql, WarehouseItemEntity.class);
            query.setParameter("category", category);
            return query.getResultList();
        });
    }

    public List<WarehouseItemEntity> findPaged(int offset, int limit) {
        return JpaUtil.executeWithResult(em -> {
            String jpql = "SELECT i FROM WarehouseItemEntity i ORDER BY i.id ASC";
            TypedQuery<WarehouseItemEntity> query = em.createQuery(jpql, WarehouseItemEntity.class);
            query.setFirstResult(offset);
            query.setMaxResults(limit);
            return query.getResultList();
        });
    }

    // Виправлення проблеми N+1 через JOIN FETCH
    public List<WarehouseItemEntity> findAllWithCategoryFetch() {
        return JpaUtil.executeWithResult(em -> {
            String jpql = "SELECT DISTINCT i FROM WarehouseItemEntity i LEFT JOIN FETCH i.category";
            return em.createQuery(jpql, WarehouseItemEntity.class).getResultList();
        });
    }
}
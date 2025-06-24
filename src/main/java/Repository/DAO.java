package Repository;

import java.util.List;
import java.util.Optional;

/**
 * Padrão DAO – Data Access Object genérico
 */
public interface DAO<T, ID> {
    Optional<T> get(ID id);
    List<T> getAll();
    boolean save(T entity);
    boolean update(T entity);
    boolean delete(ID id);
}

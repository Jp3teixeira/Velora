// service/CrudService.java
package service;

import java.util.List;
import java.util.Optional;

public interface CrudService<T, ID> {
    Optional<T> findById(ID id);
    List<T> findAll();
    T create(T entity);
    T update(T entity);
    void delete(ID id);
}

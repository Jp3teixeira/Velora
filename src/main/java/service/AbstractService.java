// service/AbstractService.java
package service;

import Repository.DAO;
import Database.DataAccessException;

import java.util.List;
import java.util.Optional;

public abstract class AbstractService<T, ID> implements CrudService<T, ID> {
    protected final DAO<T, ID> dao;

    protected AbstractService(DAO<T, ID> dao) {
        this.dao = dao;
    }

    @Override
    public Optional<T> findById(ID id) {
        try {
            return dao.get(id);
        } catch (DataAccessException e) {
            throw e;  // ou envie para um logger
        }
    }

    @Override
    public List<T> findAll() {
        return dao.getAll();
    }

    @Override
    public T create(T entity) {
        if (dao.save(entity)) return entity;
        throw new DataAccessException("Erro ao criar " + entity.getClass().getSimpleName());
    }

    @Override
    public T update(T entity) {
        if (dao.update(entity)) return entity;
        throw new DataAccessException("Erro ao atualizar " + entity.getClass().getSimpleName());
    }

    @Override
    public void delete(ID id) {
        if (!dao.delete(id)) {
            throw new DataAccessException("Erro ao apagar ID " + id);
        }
    }
}

package com.studio.booking.dao;

import java.util.List;
import java.util.Optional;

/**
 * Базовый интерфейс DAO с типовыми CRUD-операциями.
 *
 * @param <T> тип сущности
 */
public interface Dao<T> {

    /** Найти сущность по идентификатору. */
    Optional<T> findById(int id);

    /** Получить все сущности. */
    List<T> findAll();

    /** Сохранить новую сущность (возвращает её с присвоенным id). */
    T save(T entity);

    /** Обновить существующую сущность. */
    boolean update(T entity);

    /** Удалить сущность по идентификатору. */
    boolean delete(int id);
}

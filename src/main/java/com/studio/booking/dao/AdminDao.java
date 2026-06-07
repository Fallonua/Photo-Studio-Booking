package com.studio.booking.dao;

import com.studio.booking.model.Admin;

import java.util.Optional;

/**
 * DAO для учётной записи администратора.
 */
public interface AdminDao {

    Optional<Admin> find();

    boolean checkPassword(String password);
}

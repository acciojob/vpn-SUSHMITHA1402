package com.driver.services;

import java.util.List;

import com.driver.model.Admin;
import com.driver.model.User;

public interface ConnectionService {

    public User connect(int userId, String countryName) throws Exception;

    public User disconnect(int userId) throws Exception;

    public User communicate(int senderId, int receiverId) throws Exception;
}
package com.example.mapper;

import com.example.entity.User;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserMapper {

    List<User> selectAll(@Param("username") String username,
                         @Param("name") String name,
                         @Param("currentUserId") Integer currentUserId,
                         @Param("currentUserRole") String currentUserRole);

    List<User> selectByManagerId(Integer managerId);

    User selectById(Integer id);

    User selectByUsername(String username);

    void insert(User user);

    void updateById(User user);

    void updatePassword(User user);

    void deleteById(Integer id);
}
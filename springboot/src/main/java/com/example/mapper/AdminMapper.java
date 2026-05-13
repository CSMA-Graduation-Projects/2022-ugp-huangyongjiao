package com.example.mapper;

import com.example.entity.Admin;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AdminMapper {

    List<Admin> selectAll(@Param("username") String username,
                          @Param("name") String name,
                          @Param("currentUserId") Integer currentUserId,
                          @Param("currentUserRole") String currentUserRole);

    List<Admin> selectManagerOptions();

    Admin selectById(Integer id);

    Admin selectByUsername(String username);

    void insert(Admin admin);

    void updateById(Admin admin);

    void updatePassword(Admin admin);

    void deleteById(Integer id);
}
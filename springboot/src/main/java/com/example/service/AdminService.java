package com.example.service;

import com.example.common.enums.ResultCodeEnum;
import com.example.entity.Account;
import com.example.entity.Admin;
import com.example.exception.CustomException;
import com.example.mapper.AdminMapper;
import com.example.mapper.UserMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {

    @Resource
    private AdminMapper adminMapper;

    @Resource
    private UserMapper userMapper;

    public Admin login(Account account) {
        Admin dbAdmin = adminMapper.selectByUsername(account.getUsername());
        if (dbAdmin == null) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if (!dbAdmin.getPassword().equals(account.getPassword())) {
            throw new CustomException(ResultCodeEnum.USER_ACCOUNT_ERROR);
        }
        dbAdmin.setPassword(null);
        return dbAdmin;
    }

    public List<Admin> selectAll(String username, String name, Integer currentUserId, String currentUserRole) {
        return adminMapper.selectAll(username, name, currentUserId, currentUserRole);
    }

    public List<Admin> selectManagerOptions() {
        return adminMapper.selectManagerOptions();
    }

    public Admin selectById(Integer id) {
        return adminMapper.selectById(id);
    }

    public void add(Admin admin, Integer currentUserId, String currentUserRole) {
        if (!"SUPER_ADMIN".equals(currentUserRole)) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }

        Admin sameAdmin = adminMapper.selectByUsername(admin.getUsername());
        if (sameAdmin != null) {
            throw new CustomException(ResultCodeEnum.USER_EXIST_ERROR);
        }
        if (userMapper.selectByUsername(admin.getUsername()) != null) {
            throw new CustomException(ResultCodeEnum.USER_EXIST_ERROR);
        }

        if (admin.getPassword() == null || admin.getPassword().trim().isEmpty()) {
            admin.setPassword("123456");
        }
        if (admin.getRole() == null || admin.getRole().trim().isEmpty()) {
            admin.setRole("ADMIN");
        }

        adminMapper.insert(admin);
    }

    public void updateById(Admin admin, Integer currentUserId, String currentUserRole) {
        Admin dbAdmin = adminMapper.selectById(admin.getId());
        if (dbAdmin == null) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }

        if ("SUPER_ADMIN".equals(currentUserRole)) {
            Admin sameAdmin = adminMapper.selectByUsername(admin.getUsername());
            if (sameAdmin != null && !sameAdmin.getId().equals(admin.getId())) {
                throw new CustomException(ResultCodeEnum.USER_EXIST_ERROR);
            }
            if (userMapper.selectByUsername(admin.getUsername()) != null) {
                throw new CustomException(ResultCodeEnum.USER_EXIST_ERROR);
            }
            adminMapper.updateById(admin);
            return;
        }

        if ("ADMIN".equals(currentUserRole) && currentUserId.equals(admin.getId())) {
            admin.setRole("ADMIN");
            adminMapper.updateById(admin);
            return;
        }

        throw new CustomException(ResultCodeEnum.PARAM_ERROR);
    }

    public void updatePassword(Account account) {
        Admin dbAdmin = adminMapper.selectByUsername(account.getUsername());
        if (dbAdmin == null) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if (!dbAdmin.getPassword().equals(account.getPassword())) {
            throw new CustomException(ResultCodeEnum.PARAM_PASSWORD_ERROR);
        }
        dbAdmin.setPassword(account.getNewPassword());
        adminMapper.updatePassword(dbAdmin);
    }

    public void deleteById(Integer id, Integer currentUserId, String currentUserRole) {
        Admin dbAdmin = adminMapper.selectById(id);
        if (dbAdmin == null) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }

        if (!"SUPER_ADMIN".equals(currentUserRole)) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }

        if (currentUserId.equals(id)) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }

        adminMapper.deleteById(id);
    }
}
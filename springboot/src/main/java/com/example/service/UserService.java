package com.example.service;

import com.example.common.enums.ResultCodeEnum;
import com.example.entity.Account;
import com.example.entity.Admin;
import com.example.entity.User;
import com.example.exception.CustomException;
import com.example.mapper.AdminMapper;
import com.example.mapper.UserMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private AdminMapper adminMapper;

    public User login(Account account) {
        User dbUser = userMapper.selectByUsername(account.getUsername());
        if (dbUser == null) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if (!dbUser.getPassword().equals(account.getPassword())) {
            throw new CustomException(ResultCodeEnum.USER_ACCOUNT_ERROR);
        }
        dbUser.setPassword(null);
        return dbUser;
    }

    public List<User> selectAll(String username, String name, Integer currentUserId, String currentUserRole) {
        return userMapper.selectAll(username, name, currentUserId, currentUserRole);
    }

    public List<User> selectByManagerId(Integer managerId) {
        return userMapper.selectByManagerId(managerId);
    }

    public User selectById(Integer id) {
        return userMapper.selectById(id);
    }

    public void add(User user, Integer currentUserId, String currentUserRole) {
        if (userMapper.selectByUsername(user.getUsername()) != null) {
            throw new CustomException(ResultCodeEnum.USER_EXIST_ERROR);
        }
        if (adminMapper.selectByUsername(user.getUsername()) != null) {
            throw new CustomException(ResultCodeEnum.USER_EXIST_ERROR);
        }

        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            user.setPassword("123456");
        }

        if ("SUPER_ADMIN".equals(currentUserRole)) {
            if (user.getManagerId() == null) {
                throw new CustomException(ResultCodeEnum.PARAM_ERROR);
            }
            userMapper.insert(user);
            return;
        }

        if ("ADMIN".equals(currentUserRole)) {
            user.setManagerId(currentUserId);
            userMapper.insert(user);
            return;
        }

        throw new CustomException(ResultCodeEnum.PARAM_ERROR);
    }

    public void updateById(User user, Integer currentUserId, String currentUserRole) {
        User dbUser = userMapper.selectById(user.getId());
        if (dbUser == null) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }

        User sameUser = userMapper.selectByUsername(user.getUsername());
        if (sameUser != null && !sameUser.getId().equals(user.getId())) {
            throw new CustomException(ResultCodeEnum.USER_EXIST_ERROR);
        }
        if (adminMapper.selectByUsername(user.getUsername()) != null) {
            throw new CustomException(ResultCodeEnum.USER_EXIST_ERROR);
        }

        if ("SUPER_ADMIN".equals(currentUserRole)) {
            if (user.getManagerId() == null) {
                throw new CustomException(ResultCodeEnum.PARAM_ERROR);
            }
            userMapper.updateById(user);
            return;
        }

        if ("ADMIN".equals(currentUserRole) && currentUserId.equals(dbUser.getManagerId())) {
            user.setManagerId(currentUserId);
            userMapper.updateById(user);
            return;
        }

        throw new CustomException(ResultCodeEnum.PARAM_ERROR);
    }

    public void updatePassword(Account account) {
        User dbUser = userMapper.selectByUsername(account.getUsername());
        if (dbUser == null) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if (!dbUser.getPassword().equals(account.getPassword())) {
            throw new CustomException(ResultCodeEnum.PARAM_PASSWORD_ERROR);
        }
        dbUser.setPassword(account.getNewPassword());
        userMapper.updatePassword(dbUser);
    }

    public void deleteById(Integer id, Integer currentUserId, String currentUserRole) {
        User dbUser = userMapper.selectById(id);
        if (dbUser == null) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }

        if ("SUPER_ADMIN".equals(currentUserRole)) {
            userMapper.deleteById(id);
            return;
        }

        if ("ADMIN".equals(currentUserRole) && currentUserId.equals(dbUser.getManagerId())) {
            userMapper.deleteById(id);
            return;
        }

        throw new CustomException(ResultCodeEnum.PARAM_ERROR);
    }
}
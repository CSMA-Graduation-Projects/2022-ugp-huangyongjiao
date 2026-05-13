package com.example.service;

import com.example.common.enums.ResultCodeEnum;
import com.example.entity.RequirementSpec;
import com.example.entity.User;
import com.example.exception.CustomException;
import com.example.mapper.RequirementSpecMapper;
import com.example.mapper.UserMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RequirementSpecService {

    @Resource
    private RequirementSpecMapper requirementSpecMapper;

    @Resource
    private UserMapper userMapper;

    public List<RequirementSpec> selectAll(String requirementName, String moduleName,
                                           Integer currentUserId, String currentUserRole) {
        return requirementSpecMapper.selectAll(requirementName, moduleName, currentUserId, currentUserRole);
    }

    public RequirementSpec selectById(Integer id) {
        return requirementSpecMapper.selectById(id);
    }

    public void add(RequirementSpec requirementSpec, Integer currentUserId, String currentUserRole) {
        if ("SUPER_ADMIN".equals(currentUserRole)) {
            requirementSpec.setCreatorId(currentUserId);
            requirementSpec.setCreatorRole("SUPER_ADMIN");
            requirementSpec.setManagerId(null);
        } else if ("ADMIN".equals(currentUserRole)) {
            requirementSpec.setCreatorId(currentUserId);
            requirementSpec.setCreatorRole("ADMIN");
            requirementSpec.setManagerId(currentUserId);
        } else if ("USER".equals(currentUserRole)) {
            User currentUser = userMapper.selectById(currentUserId);
            if (currentUser == null) {
                throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
            }
            requirementSpec.setCreatorId(currentUserId);
            requirementSpec.setCreatorRole("USER");
            requirementSpec.setManagerId(currentUser.getManagerId());
        } else {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }

        requirementSpecMapper.insert(requirementSpec);
    }

    public void updateById(RequirementSpec requirementSpec, Integer currentUserId, String currentUserRole) {
        RequirementSpec dbInfo = requirementSpecMapper.selectById(requirementSpec.getId());
        if (dbInfo == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }

        if ("SUPER_ADMIN".equals(currentUserRole)) {
            requirementSpec.setCreatorId(dbInfo.getCreatorId());
            requirementSpec.setCreatorRole(dbInfo.getCreatorRole());
            requirementSpec.setManagerId(dbInfo.getManagerId());
            requirementSpecMapper.updateById(requirementSpec);
            return;
        }

        if ("ADMIN".equals(currentUserRole)) {
            boolean canEditOwn = "ADMIN".equals(dbInfo.getCreatorRole())
                    && currentUserId.equals(dbInfo.getCreatorId());
            boolean canEditManagedUser = "USER".equals(dbInfo.getCreatorRole())
                    && currentUserId.equals(dbInfo.getManagerId());
            if (canEditOwn || canEditManagedUser) {
                requirementSpec.setCreatorId(dbInfo.getCreatorId());
                requirementSpec.setCreatorRole(dbInfo.getCreatorRole());
                requirementSpec.setManagerId(dbInfo.getManagerId());
                requirementSpecMapper.updateById(requirementSpec);
                return;
            }
        }

        if ("USER".equals(currentUserRole)) {
            boolean canEditOwn = "USER".equals(dbInfo.getCreatorRole())
                    && currentUserId.equals(dbInfo.getCreatorId());
            if (canEditOwn) {
                requirementSpec.setCreatorId(dbInfo.getCreatorId());
                requirementSpec.setCreatorRole(dbInfo.getCreatorRole());
                requirementSpec.setManagerId(dbInfo.getManagerId());
                requirementSpecMapper.updateById(requirementSpec);
                return;
            }
        }

        throw new CustomException(ResultCodeEnum.PARAM_ERROR);
    }

    public void deleteById(Integer id, Integer currentUserId, String currentUserRole) {
        RequirementSpec dbInfo = requirementSpecMapper.selectById(id);
        if (dbInfo == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }

        if ("SUPER_ADMIN".equals(currentUserRole)) {
            requirementSpecMapper.deleteById(id);
            return;
        }

        if ("ADMIN".equals(currentUserRole)) {
            boolean canDeleteOwn = "ADMIN".equals(dbInfo.getCreatorRole())
                    && currentUserId.equals(dbInfo.getCreatorId());
            boolean canDeleteManagedUser = "USER".equals(dbInfo.getCreatorRole())
                    && currentUserId.equals(dbInfo.getManagerId());
            if (canDeleteOwn || canDeleteManagedUser) {
                requirementSpecMapper.deleteById(id);
                return;
            }
        }

        if ("USER".equals(currentUserRole)) {
            boolean canDeleteOwn = "USER".equals(dbInfo.getCreatorRole())
                    && currentUserId.equals(dbInfo.getCreatorId());
            if (canDeleteOwn) {
                requirementSpecMapper.deleteById(id);
                return;
            }
        }

        throw new CustomException(ResultCodeEnum.PARAM_ERROR);
    }
}
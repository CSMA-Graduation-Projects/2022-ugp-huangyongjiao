package com.example.service;

import com.example.common.enums.ResultCodeEnum;
import com.example.entity.ModelConfig;
import com.example.entity.User;
import com.example.exception.CustomException;
import com.example.mapper.ModelConfigMapper;
import com.example.mapper.UserMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ModelConfigService {

    @Resource
    private ModelConfigMapper modelConfigMapper;

    @Resource
    private UserMapper userMapper;

    public List<ModelConfig> selectAll(String modelName, String status, Integer currentUserId, String currentUserRole) {
        return modelConfigMapper.selectAll(modelName, status, currentUserId, currentUserRole);
    }

    public List<ModelConfig> selectEnabledList(Integer currentUserId, String currentUserRole) {
        return modelConfigMapper.selectEnabledList(currentUserId, currentUserRole);
    }

    public ModelConfig selectById(Integer id) {
        return modelConfigMapper.selectById(id);
    }

    public void add(ModelConfig modelConfig, Integer currentUserId, String currentUserRole) {
        if ("SUPER_ADMIN".equals(currentUserRole)) {
            modelConfig.setCreatorId(currentUserId);
            modelConfig.setCreatorRole("SUPER_ADMIN");
            modelConfig.setManagerId(null);
        } else if ("ADMIN".equals(currentUserRole)) {
            modelConfig.setCreatorId(currentUserId);
            modelConfig.setCreatorRole("ADMIN");
            modelConfig.setManagerId(currentUserId);
        } else if ("USER".equals(currentUserRole)) {
            User currentUser = userMapper.selectById(currentUserId);
            if (currentUser == null) {
                throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
            }
            modelConfig.setCreatorId(currentUserId);
            modelConfig.setCreatorRole("USER");
            modelConfig.setManagerId(currentUser.getManagerId());
        } else {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }

        modelConfigMapper.insert(modelConfig);
    }

    public void updateById(ModelConfig modelConfig, Integer currentUserId, String currentUserRole) {
        ModelConfig dbConfig = modelConfigMapper.selectById(modelConfig.getId());
        if (dbConfig == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }

        boolean canOperate = currentUserId.equals(dbConfig.getCreatorId())
                && currentUserRole.equals(dbConfig.getCreatorRole());

        if (!canOperate) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }

        modelConfig.setCreatorId(dbConfig.getCreatorId());
        modelConfig.setCreatorRole(dbConfig.getCreatorRole());
        modelConfig.setManagerId(dbConfig.getManagerId());
        modelConfigMapper.updateById(modelConfig);
    }

    public void deleteById(Integer id, Integer currentUserId, String currentUserRole) {
        ModelConfig dbConfig = modelConfigMapper.selectById(id);
        if (dbConfig == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }

        boolean canOperate = currentUserId.equals(dbConfig.getCreatorId())
                && currentUserRole.equals(dbConfig.getCreatorRole());

        if (!canOperate) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }

        modelConfigMapper.deleteById(id);
    }

    public ModelConfig selectVisibleById(Integer id, Integer currentUserId, String currentUserRole) {
        List<ModelConfig> list = modelConfigMapper.selectAll(null, null, currentUserId, currentUserRole);
        return list.stream()
                .filter(item -> item.getId().equals(id))
                .findFirst()
                .orElse(null);
    }
}
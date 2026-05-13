package com.example.service;

import com.example.common.enums.ResultCodeEnum;
import com.example.entity.GenerateRecord;
import com.example.exception.CustomException;
import com.example.mapper.GenerateRecordMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GenerateRecordService {

    @Resource
    private GenerateRecordMapper generateRecordMapper;

    public List<GenerateRecord> selectAll(String functionName, String modelName, String strategy,
                                          Integer currentUserId, String currentUserRole) {
        return generateRecordMapper.selectAll(functionName, modelName, strategy, currentUserId, currentUserRole);
    }

    public GenerateRecord selectById(Integer id) {
        return generateRecordMapper.selectById(id);
    }

    public void deleteById(Integer id, Integer currentUserId, String currentUserRole) {
        GenerateRecord dbRecord = generateRecordMapper.selectById(id);
        if (dbRecord == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }

        if ("SUPER_ADMIN".equals(currentUserRole)) {
            generateRecordMapper.deleteById(id);
            return;
        }

        if ("ADMIN".equals(currentUserRole)) {
            boolean canDeleteOwn = "ADMIN".equals(dbRecord.getCreatorRole())
                    && currentUserId.equals(dbRecord.getCreatorId());
            boolean canDeleteManagedUser = "USER".equals(dbRecord.getCreatorRole())
                    && currentUserId.equals(dbRecord.getManagerId());
            if (canDeleteOwn || canDeleteManagedUser) {
                generateRecordMapper.deleteById(id);
                return;
            }
        }

        if ("USER".equals(currentUserRole)) {
            boolean canDeleteOwn = "USER".equals(dbRecord.getCreatorRole())
                    && currentUserId.equals(dbRecord.getCreatorId());
            if (canDeleteOwn) {
                generateRecordMapper.deleteById(id);
                return;
            }
        }

        throw new CustomException(ResultCodeEnum.PARAM_ERROR);
    }
}
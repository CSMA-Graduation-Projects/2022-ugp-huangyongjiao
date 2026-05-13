package com.example.mapper;

import com.example.entity.ModelConfig;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ModelConfigMapper {

    List<ModelConfig> selectAll(@Param("modelName") String modelName,
                                @Param("status") String status,
                                @Param("currentUserId") Integer currentUserId,
                                @Param("currentUserRole") String currentUserRole);

    List<ModelConfig> selectEnabledList(@Param("currentUserId") Integer currentUserId,
                                        @Param("currentUserRole") String currentUserRole);

    ModelConfig selectById(Integer id);

    void insert(ModelConfig modelConfig);

    void updateById(ModelConfig modelConfig);

    void deleteById(Integer id);
}
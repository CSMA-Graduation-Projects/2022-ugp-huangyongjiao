package com.example.mapper;

import com.example.entity.RequirementSpec;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RequirementSpecMapper {

    List<RequirementSpec> selectAll(@Param("requirementName") String requirementName,
                                    @Param("moduleName") String moduleName,
                                    @Param("currentUserId") Integer currentUserId,
                                    @Param("currentUserRole") String currentUserRole);

    RequirementSpec selectById(Integer id);

    void insert(RequirementSpec requirementSpec);

    void updateById(RequirementSpec requirementSpec);

    void deleteById(Integer id);
}
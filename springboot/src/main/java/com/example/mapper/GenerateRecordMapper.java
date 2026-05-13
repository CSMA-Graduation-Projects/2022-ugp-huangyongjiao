package com.example.mapper;

import com.example.entity.GenerateRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface GenerateRecordMapper {

    List<GenerateRecord> selectAll(@Param("functionName") String functionName,
                                   @Param("modelName") String modelName,
                                   @Param("strategy") String strategy,
                                   @Param("currentUserId") Integer currentUserId,
                                   @Param("currentUserRole") String currentUserRole);

    GenerateRecord selectById(Integer id);

    void insert(GenerateRecord generateRecord);

    void deleteById(Integer id);

    void updateEvaluationDetail(GenerateRecord generateRecord);

    void updateCoverage(GenerateRecord generateRecord);

    void updatePublicCompareResult(GenerateRecord generateRecord);
}

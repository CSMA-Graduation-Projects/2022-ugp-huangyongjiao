package com.example.mapper;

import com.example.entity.FunctionInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FunctionInfoMapper {

    List<FunctionInfo> selectAll(@Param("functionName") String functionName,
                                 @Param("className") String className,
                                 @Param("language") String language,
                                 @Param("currentUserId") Integer currentUserId,
                                 @Param("currentUserRole") String currentUserRole);

    FunctionInfo selectById(Integer id);

    void insert(FunctionInfo functionInfo);

    void insertBatch(@Param("list") List<FunctionInfo> list);

    int countDuplicate(FunctionInfo functionInfo);

    Integer findSameFunctionId(@Param("functionName") String functionName,
                               @Param("codeText") String codeText);

    int updatePublicTestInfo(FunctionInfo functionInfo);

    void updateById(FunctionInfo functionInfo);

    void deleteById(Integer id);
}

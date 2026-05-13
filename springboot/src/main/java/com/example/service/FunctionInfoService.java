package com.example.service;

import com.example.common.enums.ResultCodeEnum;
import com.example.entity.FunctionInfo;
import com.example.entity.User;
import com.example.exception.CustomException;
import com.example.mapper.FunctionInfoMapper;
import com.example.mapper.UserMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class FunctionInfoService {

    @Resource
    private FunctionInfoMapper functionInfoMapper;

    @Resource
    private UserMapper userMapper;

    public List<FunctionInfo> selectAll(String functionName, String className, String language,
                                        Integer currentUserId, String currentUserRole) {
        return functionInfoMapper.selectAll(functionName, className, language, currentUserId, currentUserRole);
    }

    public FunctionInfo selectById(Integer id) {
        return functionInfoMapper.selectById(id);
    }

    public List<FunctionInfo> selectVisibleByIds(List<Integer> ids, Integer currentUserId, String currentUserRole) {
        List<FunctionInfo> list = selectAll(null, null, null, currentUserId, currentUserRole);
        if (ids == null || ids.isEmpty()) {
            return list;
        }
        java.util.Set<Integer> idSet = new java.util.HashSet<>(ids);
        return list.stream()
                .filter(item -> item.getId() != null && idSet.contains(item.getId()))
                .toList();
    }

    public void add(FunctionInfo functionInfo, Integer currentUserId, String currentUserRole) {
        if ("SUPER_ADMIN".equals(currentUserRole)) {
            functionInfo.setCreatorId(currentUserId);
            functionInfo.setCreatorRole("SUPER_ADMIN");
            functionInfo.setManagerId(null);
        } else if ("ADMIN".equals(currentUserRole)) {
            functionInfo.setCreatorId(currentUserId);
            functionInfo.setCreatorRole("ADMIN");
            functionInfo.setManagerId(currentUserId);
        } else if ("USER".equals(currentUserRole)) {
            User currentUser = userMapper.selectById(currentUserId);
            if (currentUser == null) {
                throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
            }
            functionInfo.setCreatorId(currentUserId);
            functionInfo.setCreatorRole("USER");
            functionInfo.setManagerId(currentUser.getManagerId());
        } else {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }

        normalizePublicTestFields(functionInfo);
        functionInfoMapper.insert(functionInfo);
    }

    public Map<String, Object> batchAdd(List<FunctionInfo> functionInfoList, Integer currentUserId, String currentUserRole) {
        Map<String, Object> result = new LinkedHashMap<>();
        int totalCount = functionInfoList == null ? 0 : functionInfoList.size();
        int insertedCount = 0;
        int duplicateCount = 0;
        int publicTestUpdatedCount = 0;
        int failedCount = 0;
        List<String> duplicateMessages = new ArrayList<>();
        List<String> failedMessages = new ArrayList<>();

        if (functionInfoList == null || functionInfoList.isEmpty()) {
            result.put("totalCount", totalCount);
            result.put("insertedCount", insertedCount);
            result.put("duplicateCount", duplicateCount);
            result.put("publicTestUpdatedCount", publicTestUpdatedCount);
            result.put("failedCount", failedCount);
            result.put("duplicateMessages", duplicateMessages);
            result.put("failedMessages", failedMessages);
            return result;
        }

        for (int i = 0; i < functionInfoList.size(); i++) {
            FunctionInfo functionInfo = functionInfoList.get(i);
            try {
                normalizePublicTestFields(functionInfo);

                if (isPublicDatasetImport(functionInfo)) {
                    BatchImportItemResult itemResult = importPublicDatasetFunction(functionInfo, currentUserId, currentUserRole);
                    if (itemResult.inserted()) {
                        insertedCount++;
                    } else if (itemResult.updated()) {
                        publicTestUpdatedCount++;
                    } else {
                        duplicateCount++;
                    }
                    if (itemResult.message() != null && !itemResult.message().isBlank()) {
                        duplicateMessages.add("第" + (i + 1) + "行：" + itemResult.message());
                    }
                    continue;
                }

                if (functionInfoMapper.countDuplicate(functionInfo) > 0) {
                    duplicateCount++;
                    duplicateMessages.add("第" + (i + 1) + "行：" + functionInfo.getFunctionName() + " 已存在");
                    continue;
                }
                add(functionInfo, currentUserId, currentUserRole);
                insertedCount++;
            } catch (Exception e) {
                failedCount++;
                failedMessages.add("第" + (i + 1) + "行：" + (functionInfo == null ? "未知函数" : functionInfo.getFunctionName())
                        + "，原因：" + e.getMessage());
            }
        }

        result.put("totalCount", totalCount);
        result.put("insertedCount", insertedCount);
        result.put("duplicateCount", duplicateCount);
        result.put("publicTestUpdatedCount", publicTestUpdatedCount);
        result.put("failedCount", failedCount);
        result.put("duplicateMessages", duplicateMessages);
        result.put("failedMessages", failedMessages);
        return result;
    }

    public void updateById(FunctionInfo functionInfo, Integer currentUserId, String currentUserRole) {
        FunctionInfo dbInfo = functionInfoMapper.selectById(functionInfo.getId());
        if (dbInfo == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        preservePublicTestFields(functionInfo, dbInfo);

        if ("SUPER_ADMIN".equals(currentUserRole)) {
            functionInfo.setCreatorId(dbInfo.getCreatorId());
            functionInfo.setCreatorRole(dbInfo.getCreatorRole());
            functionInfo.setManagerId(dbInfo.getManagerId());
            normalizePublicTestFields(functionInfo);
            functionInfoMapper.updateById(functionInfo);
            return;
        }

        if ("ADMIN".equals(currentUserRole)) {
            boolean canEditOwn = "ADMIN".equals(dbInfo.getCreatorRole())
                    && currentUserId.equals(dbInfo.getCreatorId());
            boolean canEditManagedUser = "USER".equals(dbInfo.getCreatorRole())
                    && currentUserId.equals(dbInfo.getManagerId());
            if (canEditOwn || canEditManagedUser) {
                functionInfo.setCreatorId(dbInfo.getCreatorId());
                functionInfo.setCreatorRole(dbInfo.getCreatorRole());
                functionInfo.setManagerId(dbInfo.getManagerId());
                normalizePublicTestFields(functionInfo);
                functionInfoMapper.updateById(functionInfo);
                return;
            }
        }

        if ("USER".equals(currentUserRole)) {
            boolean canEditOwn = "USER".equals(dbInfo.getCreatorRole())
                    && currentUserId.equals(dbInfo.getCreatorId());
            if (canEditOwn) {
                functionInfo.setCreatorId(dbInfo.getCreatorId());
                functionInfo.setCreatorRole(dbInfo.getCreatorRole());
                functionInfo.setManagerId(dbInfo.getManagerId());
                normalizePublicTestFields(functionInfo);
                functionInfoMapper.updateById(functionInfo);
                return;
            }
        }

        throw new CustomException(ResultCodeEnum.PARAM_ERROR);
    }

    private void normalizePublicTestFields(FunctionInfo functionInfo) {
        if (functionInfo == null) {
            return;
        }
        if (functionInfo.getPublicAssertCount() == null) {
            functionInfo.setPublicAssertCount(0);
        }
    }

    private boolean isPublicDatasetImport(FunctionInfo functionInfo) {
        if (functionInfo == null) {
            return false;
        }
        String source = nullToEmpty(functionInfo.getPublicTestSource()).trim();
        return "HumanEval".equalsIgnoreCase(source) || "MBPP".equalsIgnoreCase(source);
    }

    private BatchImportItemResult importPublicDatasetFunction(FunctionInfo parsed,
                                                             Integer currentUserId,
                                                             String currentUserRole) {
        Integer existingId = functionInfoMapper.findSameFunctionId(
                parsed.getFunctionName(),
                nullToEmpty(parsed.getCodeText()).trim()
        );

        if (existingId == null) {
            add(parsed, currentUserId, currentUserRole);
            return BatchImportItemResult.insertedResult();
        }

        FunctionInfo existing = functionInfoMapper.selectById(existingId);
        if (existing != null && samePublicTestInfo(existing, parsed)) {
            return BatchImportItemResult.skippedResult(parsed.getFunctionName() + " 已存在，公开断言未变化");
        }

        parsed.setId(existingId);
        functionInfoMapper.updatePublicTestInfo(parsed);
        return BatchImportItemResult.updatedResult(parsed.getFunctionName() + " 已存在，已更新公开断言");
    }

    private boolean samePublicTestInfo(FunctionInfo existing, FunctionInfo parsed) {
        if (existing == null || parsed == null) {
            return false;
        }
        return Objects.equals(nullToEmpty(existing.getPublicTestContent()), nullToEmpty(parsed.getPublicTestContent()))
                && Objects.equals(nullToEmpty(existing.getPublicTestSource()), nullToEmpty(parsed.getPublicTestSource()))
                && Objects.equals(normalizeCount(existing.getPublicAssertCount()), normalizeCount(parsed.getPublicAssertCount()));
    }

    private Integer normalizeCount(Integer value) {
        return value == null ? 0 : value;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private void preservePublicTestFields(FunctionInfo functionInfo, FunctionInfo dbInfo) {
        if (functionInfo == null || dbInfo == null) {
            return;
        }
        if (functionInfo.getPublicTestContent() == null) {
            functionInfo.setPublicTestContent(dbInfo.getPublicTestContent());
        }
        if (functionInfo.getPublicTestSource() == null) {
            functionInfo.setPublicTestSource(dbInfo.getPublicTestSource());
        }
        if (functionInfo.getPublicAssertCount() == null) {
            functionInfo.setPublicAssertCount(dbInfo.getPublicAssertCount());
        }
    }

    public void deleteById(Integer id, Integer currentUserId, String currentUserRole) {
        FunctionInfo dbInfo = functionInfoMapper.selectById(id);
        if (dbInfo == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }

        if ("SUPER_ADMIN".equals(currentUserRole)) {
            functionInfoMapper.deleteById(id);
            return;
        }

        if ("ADMIN".equals(currentUserRole)) {
            boolean canDeleteOwn = "ADMIN".equals(dbInfo.getCreatorRole())
                    && currentUserId.equals(dbInfo.getCreatorId());
            boolean canDeleteManagedUser = "USER".equals(dbInfo.getCreatorRole())
                    && currentUserId.equals(dbInfo.getManagerId());
            if (canDeleteOwn || canDeleteManagedUser) {
                functionInfoMapper.deleteById(id);
                return;
            }
        }

        if ("USER".equals(currentUserRole)) {
            boolean canDeleteOwn = "USER".equals(dbInfo.getCreatorRole())
                    && currentUserId.equals(dbInfo.getCreatorId());
            if (canDeleteOwn) {
                functionInfoMapper.deleteById(id);
                return;
            }
        }

        throw new CustomException(ResultCodeEnum.PARAM_ERROR);
    }

    private record BatchImportItemResult(boolean inserted, boolean updated, String message) {
        private static BatchImportItemResult insertedResult() {
            return new BatchImportItemResult(true, false, null);
        }

        private static BatchImportItemResult updatedResult(String message) {
            return new BatchImportItemResult(false, true, message);
        }

        private static BatchImportItemResult skippedResult(String message) {
            return new BatchImportItemResult(false, false, message);
        }
    }
}

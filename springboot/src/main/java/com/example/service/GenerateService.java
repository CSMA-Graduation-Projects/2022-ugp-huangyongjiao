package com.example.service;

import com.example.common.config.DeepSeekConfig;
import com.example.entity.BatchExperimentItem;
import com.example.entity.BatchExperimentModelStat;
import com.example.entity.BatchExperimentRequest;
import com.example.entity.BatchExperimentResponse;
import com.example.entity.BatchExperimentTaskCurrentItem;
import com.example.entity.BatchExperimentTaskSnapshot;
import com.example.entity.CoverageResult;
import com.example.entity.EvaluationResult;
import com.example.entity.GenerateRecord;
import com.example.entity.GenerateRequest;
import com.example.entity.GenerateResponse;
import com.example.entity.FunctionInfo;
import com.example.entity.ModelCallResult;
import com.example.entity.ModelConfig;
import com.example.entity.User;
import com.example.mapper.GenerateRecordMapper;
import com.example.mapper.UserMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class GenerateService {

    private static final int EVALUATION_MODEL_TIMEOUT_SECONDS = 180;

    @Resource
    private UserMapper userMapper;

    @Resource
    private DeepSeekConfig deepSeekConfig;

    @Resource
    private GenerateRecordMapper generateRecordMapper;

    @Resource
    private ModelConfigService modelConfigService;

    @Resource
    private FunctionInfoService functionInfoService;

    @Resource
    private BatchExperimentTaskService batchExperimentTaskService;

    @Resource
    private CoverageService coverageService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public GenerateResponse generateTestCase(GenerateRequest request) {
        enrichGenerateRequestFromFunctionInfo(request);
        String prompt = buildPrompt(request);

        ModelRuntimeConfig runtimeConfig = getRuntimeConfig(
                request.getStrategy(),
                request.getModelConfigId(),
                request.getCurrentUserId(),
                request.getCurrentUserRole()
        );

        ModelCallResult modelCallResult = callModel(prompt, runtimeConfig);
        String resultText = normalizeGeneratedResult(modelCallResult.getContent(), request);

        GenerateResponse response = new GenerateResponse();
        response.setResultText(resultText);

        Integer recordId = saveGenerateRecord(request, prompt, resultText, runtimeConfig.getModelName(), null, modelCallResult);
        response.setRecordId(recordId);
        GenerateRecord publicCompareRecord = compareWithPublicTests(request, recordId, resultText);
        fillPublicCompareResponse(response, publicCompareRecord);

        return response;
    }

    public EvaluationResult evaluateTestCase(GenerateRequest request) {
        String resultText = nullToEmpty(request.getResultText()).trim();
        if (resultText.isEmpty()) {
            throw new RuntimeException("请先生成测试用例，再进行评估");
        }

        if (isFailureText(resultText)) {
            EvaluationResult result = normalizeEvaluationResult(
                    buildEvaluationFailureResult(request, resultText),
                    request,
                    resultText
            );
            if (request.getRecordId() != null) {
                persistEvaluationDetail(request.getRecordId(), result);
                applyCoverageAfterEvaluation(request, resultText, result);
            }
            return result;
        }

        String sourceContext = buildEvaluationSourceContext(request);
        EvaluationResult result;
        try {
            result = evaluateByPromptAndResult(request, sourceContext, resultText);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("评估失败原因：" + e.getMessage());
            result = buildEvaluationFallbackResult(request);
            result.setGeneratedTestCase(resultText);
        }

        result = normalizeEvaluationResult(result, request, resultText);

        if (request.getRecordId() != null) {
            persistEvaluationDetail(request.getRecordId(), result);
            applyCoverageAfterEvaluation(request, resultText, result);
        }
        return result;
    }

    public BatchExperimentResponse batchExperiment(BatchExperimentRequest request) {
        validateBatchRequest(request);

        List<FunctionInfo> functionList = functionInfoService.selectVisibleByIds(
                request.getFunctionIds(),
                request.getCurrentUserId(),
                request.getCurrentUserRole()
        );
        if (functionList.isEmpty()) {
            throw new RuntimeException("未获取到可执行批量实验的函数数据");
        }

        Map<Integer, FunctionInfo> functionMap = new HashMap<>();
        for (FunctionInfo functionInfo : functionList) {
            functionMap.put(functionInfo.getId(), functionInfo);
        }

        List<ModelConfig> enabledModels = modelConfigService.selectEnabledList(request.getCurrentUserId(), request.getCurrentUserRole());
        Map<Integer, ModelConfig> modelMap = enabledModels.stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(ModelConfig::getId, item -> item, (a, b) -> a));

        List<Integer> validFunctionIds = request.getFunctionIds().stream()
                .filter(functionMap::containsKey)
                .collect(Collectors.toList());
        if (validFunctionIds.isEmpty()) {
            throw new RuntimeException("选中的函数均不可见或已不存在");
        }

        List<Integer> validModelIds = request.getModelConfigIds().stream()
                .filter(modelMap::containsKey)
                .collect(Collectors.toList());
        if (validModelIds.isEmpty()) {
            throw new RuntimeException("选中的模型均不可用或未启用");
        }

        BatchExperimentResponse response = new BatchExperimentResponse();
        response.setBatchNo(buildBatchNo());
        response.setFunctionCount(validFunctionIds.size());
        response.setOutputLanguage(nullToEmpty(request.getOutputLanguage()));
        response.setRunCount(request.getRunCount());

        int totalTaskCount = validFunctionIds.size() * validModelIds.size() * request.getStrategies().size() * request.getRunCount();
        response.setTotalTaskCount(totalTaskCount);

        List<BatchExperimentItem> itemList = new ArrayList<>();
        List<Integer> evaluationScores = new ArrayList<>();
        List<Integer> testerScores = new ArrayList<>();
        Map<Integer, BatchModelStatCounter> modelStatCounterMap = new LinkedHashMap<>();
        int successCount = 0;
        int failedCount = 0;

        for (Integer modelId : validModelIds) {
            ModelConfig modelConfig = modelMap.get(modelId);
            if (modelConfig != null) {
                modelStatCounterMap.put(modelId, new BatchModelStatCounter(modelId, modelConfig.getModelName()));
            }
        }

        for (Integer functionId : validFunctionIds) {
            FunctionInfo functionInfo = functionMap.get(functionId);
            if (functionInfo == null) {
                continue;
            }

            for (Integer modelId : validModelIds) {
                ModelConfig modelConfig = modelMap.get(modelId);
                if (modelConfig == null) {
                    continue;
                }

                for (String strategy : request.getStrategies()) {
                    Integer previousRecordId = null;
                    String previousResultText = "";
                    String previousEvaluationSummary = "";

                    for (int runIndex = 1; runIndex <= request.getRunCount(); runIndex++) {
                        BatchExperimentItem item = new BatchExperimentItem();
                        item.setFunctionId(functionInfo.getId());
                        item.setFunctionName(functionInfo.getFunctionName());
                        item.setClassName(functionInfo.getClassName());
                        item.setLanguage(functionInfo.getLanguage());
                        item.setRemark(functionInfo.getRemark());
                        item.setModelConfigId(modelId);
                        item.setModelName(modelConfig.getModelName());
                        item.setStrategy(strategy);
                        item.setRunIndex(runIndex);
                        item.setPreviousRecordId(previousRecordId);
                        item.setOutputLanguage(request.getOutputLanguage());
                        item.setCreateTime(currentDateTimeText());
                        GenerateRequest singleRequest = buildSingleGenerateRequest(request, functionInfo, modelId, strategy);
                        singleRequest.setRunIndex(runIndex);
                        singleRequest.setPreviousRecordId(previousRecordId);
                        singleRequest.setRegenerate(runIndex > 1);
                        singleRequest.setPreviousResultText(runIndex > 1 ? previousResultText : "");
                        singleRequest.setPreviousEvaluationSummary(runIndex > 1 ? previousEvaluationSummary : "");

                        try {
                            GenerateResponse generateResponse = generateTestCase(singleRequest);
                            String resultText = nullToEmpty(generateResponse.getResultText());

                            item.setRecordId(generateResponse.getRecordId());
                            item.setResultText(resultText);
                            item.setGeneratedCaseCount(countGeneratedCaseCount(resultText));

                            singleRequest.setResultText(resultText);
                            singleRequest.setRecordId(generateResponse.getRecordId());

                            EvaluationResult evaluationResult;
                            try {
                                evaluationResult = evaluateTestCase(singleRequest);
                            } catch (Exception e) {
                                evaluationResult = buildPendingEvaluationResult(singleRequest, false, e.getMessage());
                                if (singleRequest.getRecordId() != null) {
                                    persistEvaluationDetail(singleRequest.getRecordId(), evaluationResult);
                                }
                            }
                            fillBatchExperimentEvaluationFields(item, evaluationResult);

                            if (generateResponse.getRecordId() != null) {
                                GenerateRecord dbRecord = generateRecordMapper.selectById(generateResponse.getRecordId());
                                if (dbRecord != null) {
                                    fillBatchExperimentRecordFields(item, dbRecord);
                                }
                            }

                            item.setStatus(isFailureText(resultText) ? "失败" : "成功");
                            item.setMessage(buildBatchMessage(resultText, evaluationResult));

                            if ("成功".equals(item.getStatus())) {
                                successCount++;
                            } else {
                                failedCount++;
                            }

                            if (item.getEvaluationScore() != null) {
                                evaluationScores.add(item.getEvaluationScore());
                            }
                            if (item.getTesterEvaluationScore() != null) {
                                testerScores.add(item.getTesterEvaluationScore());
                            }

                            previousRecordId = item.getRecordId();
                            previousResultText = item.getResultText();
                            previousEvaluationSummary = buildIterationEvaluationSummary(singleRequest, evaluationResult);
                        } catch (Exception e) {
                            item.setStatus("失败");
                            item.setMessage(nullToEmpty(e.getMessage()));
                            item.setResultText(nullToEmpty(item.getResultText()));
                            item.setGeneratedCaseCount(countGeneratedCaseCount(item.getResultText()));
                            EvaluationResult pendingEvaluationResult = buildPendingEvaluationResult(singleRequest, true, e.getMessage());
                            fillBatchExperimentEvaluationFields(item, pendingEvaluationResult);
                            if (singleRequest.getRecordId() != null) {
                                persistEvaluationDetail(singleRequest.getRecordId(), pendingEvaluationResult);
                            }
                            failedCount++;

                            previousRecordId = item.getRecordId();
                            previousResultText = item.getResultText();
                            previousEvaluationSummary = buildIterationEvaluationSummary(singleRequest, pendingEvaluationResult);
                        }

                        BatchModelStatCounter counter = modelStatCounterMap.get(modelId);
                        if (counter != null) {
                            counter.accept(item);
                        }
                        itemList.add(item);
                    }
                }
            }
        }

        response.setSuccessCount(successCount);
        response.setFailedCount(failedCount);
        response.setAverageEvaluationScore(calculateAverageScore(evaluationScores));
        response.setAverageTesterScore(calculateAverageScore(testerScores));
        response.setModelStats(buildBatchModelStats(modelStatCounterMap));
        response.setItems(itemList);
        return response;
    }

    public void ensureGenerationAllowed() {
        batchExperimentTaskService.assertGenerationUnlocked();
    }

    public BatchExperimentTaskSnapshot startBatchExperimentTask(BatchExperimentRequest request) {
        PreparedBatchExperimentContext context = prepareBatchExperimentContext(request);
        BatchExperimentRequest requestSnapshot = buildBatchExperimentSnapshotRequest(request, context);
        String batchNo = buildBatchNo();

        BatchExperimentTaskSnapshot snapshot = batchExperimentTaskService.createTask(
                requestSnapshot,
                batchNo,
                context.totalTaskCount
        );

        batchExperimentTaskService.submitTask(() -> runBatchExperimentTask(snapshot.getTaskId(), requestSnapshot, context, batchNo));
        return batchExperimentTaskService.getTaskSnapshot(snapshot.getTaskId());
    }

    public BatchExperimentTaskSnapshot getCurrentBatchExperimentTask() {
        return batchExperimentTaskService.getCurrentTaskSnapshot();
    }

    public BatchExperimentTaskSnapshot getBatchExperimentTask(String taskId) {
        return batchExperimentTaskService.getTaskSnapshot(taskId);
    }

    public BatchExperimentTaskSnapshot terminateBatchExperimentTask(String taskId) {
        return batchExperimentTaskService.requestTerminate(taskId);
    }

    private void runBatchExperimentTask(String taskId, BatchExperimentRequest request, PreparedBatchExperimentContext context, String batchNo) {
        BatchExperimentExecutionResult executionResult = executeBatchExperiment(request, context, batchNo, taskId);
        String failureMessage = nullToEmpty(executionResult.failureMessage).trim();
        if (!failureMessage.isEmpty()) {
            batchExperimentTaskService.markFailed(taskId, executionResult.response, failureMessage);
            return;
        }
        if (executionResult.terminated) {
            batchExperimentTaskService.markTerminated(taskId, executionResult.response);
            return;
        }
        batchExperimentTaskService.markCompleted(taskId, executionResult.response);
    }

    private PreparedBatchExperimentContext prepareBatchExperimentContext(BatchExperimentRequest request) {
        validateBatchRequest(request);

        List<FunctionInfo> functionList = functionInfoService.selectVisibleByIds(
                request.getFunctionIds(),
                request.getCurrentUserId(),
                request.getCurrentUserRole()
        );
        if (functionList.isEmpty()) {
            throw new RuntimeException("未获取到可执行批量生成的函数数据");
        }

        Map<Integer, FunctionInfo> functionMap = new HashMap<>();
        for (FunctionInfo functionInfo : functionList) {
            functionMap.put(functionInfo.getId(), functionInfo);
        }

        List<ModelConfig> enabledModels = modelConfigService.selectEnabledList(
                request.getCurrentUserId(),
                request.getCurrentUserRole()
        );
        Map<Integer, ModelConfig> modelMap = enabledModels.stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(ModelConfig::getId, item -> item, (a, b) -> a));

        List<Integer> validFunctionIds = request.getFunctionIds().stream()
                .filter(functionMap::containsKey)
                .collect(Collectors.toList());
        if (validFunctionIds.isEmpty()) {
            throw new RuntimeException("选中的函数均不可见或已不存在");
        }

        List<Integer> validModelIds = request.getModelConfigIds().stream()
                .filter(modelMap::containsKey)
                .collect(Collectors.toList());
        if (validModelIds.isEmpty()) {
            throw new RuntimeException("选中的模型均不可用或未启用");
        }

        int totalTaskCount = validFunctionIds.size() * validModelIds.size() * request.getStrategies().size() * request.getRunCount();
        return new PreparedBatchExperimentContext(validFunctionIds, functionMap, validModelIds, modelMap, totalTaskCount);
    }

    private BatchExperimentRequest buildBatchExperimentSnapshotRequest(BatchExperimentRequest request, PreparedBatchExperimentContext context) {
        BatchExperimentRequest snapshot = new BatchExperimentRequest();
        snapshot.setFunctionIds(new ArrayList<>(context.validFunctionIds));
        snapshot.setModelConfigIds(new ArrayList<>(context.validModelIds));
        snapshot.setStrategies(new ArrayList<>(request.getStrategies()));
        snapshot.setRunCount(request.getRunCount());
        snapshot.setOutputLanguage(nullToEmpty(request.getOutputLanguage()));
        snapshot.setEvaluationModelConfigId(request.getEvaluationModelConfigId());
        snapshot.setCurrentUserId(request.getCurrentUserId());
        snapshot.setCurrentUserRole(request.getCurrentUserRole());
        return snapshot;
    }

    private BatchExperimentExecutionResult executeBatchExperiment(
            BatchExperimentRequest request,
            PreparedBatchExperimentContext context,
            String batchNo,
            String taskId
    ) {
        BatchExperimentResponse response = buildBatchExperimentBaseResponse(
                request,
                batchNo,
                context.validFunctionIds.size(),
                context.totalTaskCount
        );
        List<BatchExperimentItem> itemList = new ArrayList<>();
        List<Integer> evaluationScores = new ArrayList<>();
        List<Integer> testerScores = new ArrayList<>();
        Map<Integer, BatchModelStatCounter> modelStatCounterMap = new LinkedHashMap<>();
        int successCount = 0;
        int failedCount = 0;
        boolean terminated = false;
        String failureMessage = "";

        for (Integer modelId : context.validModelIds) {
            ModelConfig modelConfig = context.modelMap.get(modelId);
            if (modelConfig != null) {
                modelStatCounterMap.put(modelId, new BatchModelStatCounter(modelId, modelConfig.getModelName()));
            }
        }

        try {
            outer:
            for (Integer functionId : context.validFunctionIds) {
                FunctionInfo functionInfo = context.functionMap.get(functionId);
                if (functionInfo == null) {
                    continue;
                }

                for (Integer modelId : context.validModelIds) {
                    ModelConfig modelConfig = context.modelMap.get(modelId);
                    if (modelConfig == null) {
                        continue;
                    }

                    for (String strategy : request.getStrategies()) {
                        Integer previousRecordId = null;
                        String previousResultText = "";
                        String previousEvaluationSummary = "";

                        for (int runIndex = 1; runIndex <= request.getRunCount(); runIndex++) {
                            if (taskId != null && batchExperimentTaskService.isTerminationRequested(taskId)) {
                                terminated = itemList.size() < context.totalTaskCount;
                                break outer;
                            }

                            BatchExperimentItem item = buildBatchExperimentItem(
                                    request,
                                    functionInfo,
                                    modelConfig,
                                    strategy,
                                    runIndex,
                                    previousRecordId
                            );
                            if (taskId != null) {
                                batchExperimentTaskService.updateCurrentItem(
                                        taskId,
                                        buildBatchExperimentCurrentItem(
                                                itemList.size() + 1,
                                                context.totalTaskCount,
                                                functionInfo,
                                                modelConfig,
                                                strategy,
                                                runIndex
                                        )
                                );
                            }

                            GenerateRequest singleRequest = buildSingleGenerateRequest(request, functionInfo, modelId, strategy);
                            singleRequest.setRunIndex(runIndex);
                            singleRequest.setPreviousRecordId(previousRecordId);
                            singleRequest.setRegenerate(runIndex > 1);
                            singleRequest.setPreviousResultText(runIndex > 1 ? previousResultText : "");
                            singleRequest.setPreviousEvaluationSummary(runIndex > 1 ? previousEvaluationSummary : "");

                            try {
                                GenerateResponse generateResponse = generateTestCase(singleRequest);
                                String resultText = nullToEmpty(generateResponse.getResultText());

                                item.setRecordId(generateResponse.getRecordId());
                                item.setResultText(resultText);
                                item.setGeneratedCaseCount(countGeneratedCaseCount(resultText));

                                singleRequest.setResultText(resultText);
                                singleRequest.setRecordId(generateResponse.getRecordId());

                                EvaluationResult evaluationResult;
                                try {
                                    evaluationResult = evaluateTestCase(singleRequest);
                                } catch (Exception e) {
                                    evaluationResult = buildPendingEvaluationResult(singleRequest, false, e.getMessage());
                                    if (singleRequest.getRecordId() != null) {
                                        persistEvaluationDetail(singleRequest.getRecordId(), evaluationResult);
                                    }
                                }
                                fillBatchExperimentEvaluationFields(item, evaluationResult);

                                if (generateResponse.getRecordId() != null) {
                                    GenerateRecord dbRecord = generateRecordMapper.selectById(generateResponse.getRecordId());
                                    if (dbRecord != null) {
                                        fillBatchExperimentRecordFields(item, dbRecord);
                                    }
                                }

                                item.setStatus(isFailureText(resultText) ? "失败" : "成功");
                                item.setMessage(buildBatchMessage(resultText, evaluationResult));

                                if ("成功".equals(item.getStatus())) {
                                    successCount++;
                                } else {
                                    failedCount++;
                                }

                                if (item.getEvaluationScore() != null) {
                                    evaluationScores.add(item.getEvaluationScore());
                                }
                                if (item.getTesterEvaluationScore() != null) {
                                    testerScores.add(item.getTesterEvaluationScore());
                                }

                                previousRecordId = item.getRecordId();
                                previousResultText = item.getResultText();
                                previousEvaluationSummary = buildIterationEvaluationSummary(singleRequest, evaluationResult);
                            } catch (Exception e) {
                                item.setStatus("失败");
                                item.setMessage(nullToEmpty(e.getMessage()));
                                item.setResultText(nullToEmpty(item.getResultText()));
                                item.setGeneratedCaseCount(countGeneratedCaseCount(item.getResultText()));
                                EvaluationResult pendingEvaluationResult = buildPendingEvaluationResult(singleRequest, true, e.getMessage());
                                fillBatchExperimentEvaluationFields(item, pendingEvaluationResult);
                                if (singleRequest.getRecordId() != null) {
                                    persistEvaluationDetail(singleRequest.getRecordId(), pendingEvaluationResult);
                                }
                                failedCount++;

                                previousRecordId = item.getRecordId();
                                previousResultText = item.getResultText();
                                previousEvaluationSummary = buildIterationEvaluationSummary(singleRequest, pendingEvaluationResult);
                            }

                            BatchModelStatCounter counter = modelStatCounterMap.get(modelId);
                            if (counter != null) {
                                counter.accept(item);
                            }
                            itemList.add(item);

                            if (taskId != null) {
                                batchExperimentTaskService.updateProgress(taskId, itemList.size(), successCount, failedCount);
                                if (batchExperimentTaskService.isTerminationRequested(taskId) && itemList.size() < context.totalTaskCount) {
                                    terminated = true;
                                    break outer;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            failureMessage = defaultIfBlank(nullToEmpty(e.getMessage()).trim(), "批量生成执行失败");
        }

        response.setSuccessCount(successCount);
        response.setFailedCount(failedCount);
        response.setAverageEvaluationScore(calculateAverageScore(evaluationScores));
        response.setAverageTesterScore(calculateAverageScore(testerScores));
        response.setModelStats(buildBatchModelStats(modelStatCounterMap));
        response.setItems(itemList);

        return new BatchExperimentExecutionResult(response, terminated, failureMessage);
    }

    private BatchExperimentResponse buildBatchExperimentBaseResponse(
            BatchExperimentRequest request,
            String batchNo,
            int functionCount,
            int totalTaskCount
    ) {
        BatchExperimentResponse response = new BatchExperimentResponse();
        response.setBatchNo(batchNo);
        response.setFunctionCount(functionCount);
        response.setOutputLanguage(nullToEmpty(request.getOutputLanguage()));
        response.setRunCount(request.getRunCount());
        response.setTotalTaskCount(totalTaskCount);
        return response;
    }

    private BatchExperimentItem buildBatchExperimentItem(
            BatchExperimentRequest request,
            FunctionInfo functionInfo,
            ModelConfig modelConfig,
            String strategy,
            int runIndex,
            Integer previousRecordId
    ) {
        BatchExperimentItem item = new BatchExperimentItem();
        item.setFunctionId(functionInfo.getId());
        item.setFunctionName(functionInfo.getFunctionName());
        item.setClassName(functionInfo.getClassName());
        item.setLanguage(functionInfo.getLanguage());
        item.setRemark(functionInfo.getRemark());
        item.setModelConfigId(modelConfig.getId());
        item.setModelName(modelConfig.getModelName());
        item.setStrategy(strategy);
        item.setRunIndex(runIndex);
        item.setPreviousRecordId(previousRecordId);
        item.setOutputLanguage(request.getOutputLanguage());
        item.setCreateTime(currentDateTimeText());
        return item;
    }

    private BatchExperimentTaskCurrentItem buildBatchExperimentCurrentItem(
            int currentIndex,
            int totalTaskCount,
            FunctionInfo functionInfo,
            ModelConfig modelConfig,
            String strategy,
            int runIndex
    ) {
        BatchExperimentTaskCurrentItem currentItem = new BatchExperimentTaskCurrentItem();
        currentItem.setCurrentIndex(currentIndex);
        currentItem.setTotalTaskCount(totalTaskCount);
        currentItem.setFunctionId(functionInfo.getId());
        currentItem.setFunctionName(functionInfo.getFunctionName());
        currentItem.setClassName(functionInfo.getClassName());
        currentItem.setModelConfigId(modelConfig.getId());
        currentItem.setModelName(modelConfig.getModelName());
        currentItem.setStrategy(strategy);
        currentItem.setRunIndex(runIndex);
        return currentItem;
    }

    private static class PreparedBatchExperimentContext {
        private final List<Integer> validFunctionIds;
        private final Map<Integer, FunctionInfo> functionMap;
        private final List<Integer> validModelIds;
        private final Map<Integer, ModelConfig> modelMap;
        private final int totalTaskCount;

        private PreparedBatchExperimentContext(
                List<Integer> validFunctionIds,
                Map<Integer, FunctionInfo> functionMap,
                List<Integer> validModelIds,
                Map<Integer, ModelConfig> modelMap,
                int totalTaskCount
        ) {
            this.validFunctionIds = validFunctionIds;
            this.functionMap = functionMap;
            this.validModelIds = validModelIds;
            this.modelMap = modelMap;
            this.totalTaskCount = totalTaskCount;
        }
    }

    private static class BatchExperimentExecutionResult {
        private final BatchExperimentResponse response;
        private final boolean terminated;
        private final String failureMessage;

        private BatchExperimentExecutionResult(BatchExperimentResponse response, boolean terminated, String failureMessage) {
            this.response = response;
            this.terminated = terminated;
            this.failureMessage = failureMessage;
        }
    }

    private void validateBatchRequest(BatchExperimentRequest request) {
        if (request == null) {
            throw new RuntimeException("批量实验参数不能为空");
        }
        if (request.getCurrentUserId() == null || nullToEmpty(request.getCurrentUserRole()).trim().isEmpty()) {
            throw new RuntimeException("当前用户信息缺失");
        }
        if (request.getFunctionIds() == null || request.getFunctionIds().isEmpty()) {
            throw new RuntimeException("请至少选择一个函数");
        }
        if (request.getModelConfigIds() == null || request.getModelConfigIds().isEmpty()) {
            throw new RuntimeException("请至少选择一个模型");
        }
        if (request.getStrategies() == null || request.getStrategies().isEmpty()) {
            throw new RuntimeException("请至少选择一个生成策略");
        }
        if (request.getRunCount() == null || request.getRunCount() <= 0) {
            throw new RuntimeException("生成次数必须大于 0");
        }
        if (request.getRunCount() > 20) {
            throw new RuntimeException("单次批量实验的生成次数建议不超过 20");
        }
    }

    private GenerateRequest buildSingleGenerateRequest(BatchExperimentRequest batchRequest, FunctionInfo functionInfo, Integer modelId, String strategy) {
        GenerateRequest request = new GenerateRequest();
        request.setModelConfigId(modelId);
        request.setEvaluationModelConfigId(batchRequest.getEvaluationModelConfigId());
        request.setStrategy(strategy);
        request.setSourceType("function");
        request.setOutputLanguage(batchRequest.getOutputLanguage());
        request.setFunctionId(functionInfo.getId());
        request.setTesterCaseCount(functionInfo.getTesterCaseCount());
        request.setFunctionName(functionInfo.getFunctionName());
        request.setClassName(functionInfo.getClassName());
        request.setLanguage(functionInfo.getLanguage());
        request.setCodeText(functionInfo.getCodeText());
        request.setInputDesc(functionInfo.getInputDesc());
        request.setOutputDesc(functionInfo.getOutputDesc());
        request.setRemark(functionInfo.getRemark());
        request.setPublicTestContent(functionInfo.getPublicTestContent());
        request.setPublicTestSource(functionInfo.getPublicTestSource());
        request.setPublicAssertCount(functionInfo.getPublicAssertCount());
        request.setCurrentUserId(batchRequest.getCurrentUserId());
        request.setCurrentUserRole(batchRequest.getCurrentUserRole());
        request.setRegenerate(false);
        return request;
    }

    private void enrichGenerateRequestFromFunctionInfo(GenerateRequest request) {
        if (request == null || isRequirementSource(request) || request.getFunctionId() == null) {
            return;
        }

        try {
            FunctionInfo functionInfo = functionInfoService.selectById(request.getFunctionId());
            if (functionInfo == null) {
                return;
            }
            request.setLanguage(defaultIfBlank(request.getLanguage(), functionInfo.getLanguage()));
            request.setCodeText(defaultIfBlank(request.getCodeText(), functionInfo.getCodeText()));
            request.setFunctionName(defaultIfBlank(request.getFunctionName(), functionInfo.getFunctionName()));
            request.setClassName(defaultIfBlank(request.getClassName(), functionInfo.getClassName()));
            request.setInputDesc(defaultIfBlank(request.getInputDesc(), functionInfo.getInputDesc()));
            request.setOutputDesc(defaultIfBlank(request.getOutputDesc(), functionInfo.getOutputDesc()));
            request.setRemark(defaultIfBlank(request.getRemark(), functionInfo.getRemark()));
            request.setPublicTestContent(defaultIfBlank(request.getPublicTestContent(), functionInfo.getPublicTestContent()));
            request.setPublicTestSource(defaultIfBlank(request.getPublicTestSource(), functionInfo.getPublicTestSource()));
            if (request.getPublicAssertCount() == null) {
                request.setPublicAssertCount(functionInfo.getPublicAssertCount());
            }
            if (request.getTesterCaseCount() == null) {
                request.setTesterCaseCount(functionInfo.getTesterCaseCount());
            }
        } catch (Exception ignored) {
        }
    }

    private String buildBatchNo() {
        return "BATCH-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    private Integer calculateAverageScore(List<Integer> scores) {
        if (scores == null || scores.isEmpty()) {
            return null;
        }
        int total = 0;
        for (Integer score : scores) {
            if (score != null) {
                total += score;
            }
        }
        return Math.round((float) total / scores.size());
    }

    private void fillBatchExperimentEvaluationFields(BatchExperimentItem item, EvaluationResult evaluationResult) {
        if (item == null || evaluationResult == null) {
            return;
        }
        item.setEvaluationScore(evaluationResult.getScore());
        item.setNormalPathCoverage(evaluationResult.getNormalPathCoverage());
        item.setBoundaryCoverage(evaluationResult.getBoundaryCoverage());
        item.setExceptionCoverage(evaluationResult.getExceptionCoverage());
        item.setSyntaxNorm(evaluationResult.getSyntaxNorm());
        item.setSuggestionText(serializeSuggestionText(evaluationResult.getSuggestions()));
        if (evaluationResult.getSuggestions() == null) {
            item.setSuggestions(new ArrayList<>());
        } else {
            item.setSuggestions(new ArrayList<>(evaluationResult.getSuggestions()));
        }
    }

    private void fillBatchExperimentRecordFields(BatchExperimentItem item, GenerateRecord dbRecord) {
        if (item == null || dbRecord == null) {
            return;
        }

        item.setRunIndex(dbRecord.getRunIndex() != null ? dbRecord.getRunIndex() : item.getRunIndex());
        item.setPreviousRecordId(dbRecord.getPreviousRecordId() != null ? dbRecord.getPreviousRecordId() : item.getPreviousRecordId());
        item.setCreateTime(defaultIfBlank(dbRecord.getCreateTime(), item.getCreateTime()));
        item.setTesterEvaluationScore(dbRecord.getTesterEvaluationScore());
        item.setResultText(defaultIfBlank(dbRecord.getResultText(), item.getResultText()));
        item.setEvaluationScore(dbRecord.getEvaluationScore() != null ? dbRecord.getEvaluationScore() : item.getEvaluationScore());
        item.setNormalPathCoverage(defaultIfBlank(dbRecord.getNormalPathCoverage(), item.getNormalPathCoverage()));
        item.setBoundaryCoverage(defaultIfBlank(dbRecord.getBoundaryCoverage(), item.getBoundaryCoverage()));
        item.setExceptionCoverage(defaultIfBlank(dbRecord.getExceptionCoverage(), item.getExceptionCoverage()));
        item.setSyntaxNorm(defaultIfBlank(dbRecord.getSyntaxNorm(), item.getSyntaxNorm()));
        item.setSuggestionText(defaultIfBlank(dbRecord.getSuggestionText(), item.getSuggestionText()));
        item.setSuggestions(parseSuggestionText(item.getSuggestionText(), item.getSuggestions()));
        item.setLineCoverage(dbRecord.getLineCoverage());
        item.setBranchCoverage(dbRecord.getBranchCoverage());
        item.setCoverageStatus(dbRecord.getCoverageStatus());
        item.setCoverageMessage(dbRecord.getCoverageMessage());
    }

    private String buildIterationEvaluationSummary(GenerateRequest request, EvaluationResult evaluationResult) {
        if (evaluationResult == null) {
            return "";
        }

        String suggestions = cleanSuggestionList(evaluationResult.getSuggestions()).stream()
                .map(item -> "- " + item)
                .collect(Collectors.joining("\n"));

        if (isEnglishOutput(request)) {
            return String.join("\n",
                    "Normal Path Coverage: " + defaultIfBlank(evaluationResult.getNormalPathCoverage(), "N/A"),
                    "Boundary Coverage: " + defaultIfBlank(evaluationResult.getBoundaryCoverage(), "N/A"),
                    "Exception Coverage: " + defaultIfBlank(evaluationResult.getExceptionCoverage(), "N/A"),
                    "Syntax Compliance: " + defaultIfBlank(evaluationResult.getSyntaxNorm(), "N/A"),
                    "Overall Score: " + (evaluationResult.getScore() == null ? "N/A" : evaluationResult.getScore()),
                    "Suggestions:",
                    suggestions.isEmpty() ? "- N/A" : suggestions
            );
        }

        return String.join("\n",
                "正常路径覆盖：" + defaultIfBlank(evaluationResult.getNormalPathCoverage(), "暂无"),
                "边界条件覆盖：" + defaultIfBlank(evaluationResult.getBoundaryCoverage(), "暂无"),
                "异常分支覆盖：" + defaultIfBlank(evaluationResult.getExceptionCoverage(), "暂无"),
                "语法规范性：" + defaultIfBlank(evaluationResult.getSyntaxNorm(), "暂无"),
                "综合评分：" + (evaluationResult.getScore() == null ? "暂无" : evaluationResult.getScore()),
                "改进建议：",
                suggestions.isEmpty() ? "- 暂无" : suggestions
        );
    }

    private void persistEvaluationDetail(Integer recordId, EvaluationResult evaluationResult) {
        if (recordId == null || evaluationResult == null) {
            return;
        }

        GenerateRecord record = new GenerateRecord();
        record.setId(recordId);
        record.setEvaluationScore(evaluationResult.getScore());
        record.setNormalPathCoverage(nullToEmpty(evaluationResult.getNormalPathCoverage()));
        record.setBoundaryCoverage(nullToEmpty(evaluationResult.getBoundaryCoverage()));
        record.setExceptionCoverage(nullToEmpty(evaluationResult.getExceptionCoverage()));
        record.setSyntaxNorm(nullToEmpty(evaluationResult.getSyntaxNorm()));
        record.setSuggestionText(serializeSuggestionText(evaluationResult.getSuggestions()));
        generateRecordMapper.updateEvaluationDetail(record);
    }

    private void applyCoverageAfterEvaluation(GenerateRequest request, String resultText, EvaluationResult evaluationResult) {
        if (request == null || request.getRecordId() == null) {
            return;
        }

        CoverageResult coverageResult;
        try {
            CoverageContext coverageContext = buildCoverageContext(request);
            if (!coverageContext.isFunctionCandidate()) {
                coverageResult = buildCoverageResult(false, "SKIPPED", null, null, "非 Python 函数，未执行 coverage.py 覆盖率统计");
            } else if (!isPythonFunction(coverageContext.language(), coverageContext.functionCode())) {
                coverageResult = buildCoverageResult(false, "SKIPPED", null, null, "非 Python 函数，未执行 coverage.py 覆盖率统计");
            } else {
                coverageResult = coverageService.runPythonCoverage(coverageContext.functionCode(), resultText);
            }
            if (coverageResult == null) {
                coverageResult = buildCoverageResult(false, "FAILED", null, null, "覆盖率统计未返回结果");
            }
        } catch (Exception e) {
            coverageResult = buildCoverageResult(false, "FAILED", null, null, "覆盖率统计失败：" + nullToEmpty(e.getMessage()));
        }

        persistCoverage(request.getRecordId(), coverageResult);
        fillCoverageEvaluationFields(evaluationResult, coverageResult);
    }

    private CoverageContext buildCoverageContext(GenerateRequest request) {
        FunctionInfo functionInfo = null;
        if (request.getFunctionId() != null) {
            try {
                functionInfo = functionInfoService.selectById(request.getFunctionId());
            } catch (Exception ignored) {
            }
        }

        String functionCode = defaultIfBlank(
                request.getCodeText(),
                functionInfo == null ? "" : functionInfo.getCodeText()
        );
        String language = defaultIfBlank(
                request.getLanguage(),
                functionInfo == null ? "" : functionInfo.getLanguage()
        );
        boolean functionCandidate = !isRequirementSource(request) || !functionCode.trim().isEmpty();
        return new CoverageContext(functionCandidate, functionCode, language);
    }

    private boolean isPythonFunction(String language, String functionCode) {
        if (isPythonLanguage(language)) {
            return true;
        }
        return nullToEmpty(language).trim().isEmpty() && looksLikePythonCode(functionCode);
    }

    private boolean isPythonLanguage(String language) {
        String value = nullToEmpty(language).trim().toLowerCase();
        return value.equals("python") || value.equals("py") || value.contains("python") || value.contains("py");
    }

    private boolean looksLikePythonCode(String codeText) {
        String code = nullToEmpty(codeText);
        return code.matches("(?s).*\\bdef\\s+\\w+\\s*\\(.*")
                || code.matches("(?m)^\\s*from\\s+\\w+(?:\\.\\w+)*\\s+import\\s+.+")
                || code.matches("(?m)^\\s*import\\s+\\w+.*")
                || code.matches("(?m)^\\s*class\\s+\\w+\\s*(?:\\(|:).*");
    }

    private void persistCoverage(Integer recordId, CoverageResult coverageResult) {
        if (recordId == null || coverageResult == null) {
            return;
        }

        GenerateRecord record = new GenerateRecord();
        record.setId(recordId);
        record.setLineCoverage(coverageResult.getLineCoverage());
        record.setBranchCoverage(coverageResult.getBranchCoverage());
        record.setCoverageStatus(coverageResult.getStatus());
        record.setCoverageMessage(coverageResult.getMessage());
        generateRecordMapper.updateCoverage(record);
    }

    private void fillCoverageEvaluationFields(EvaluationResult evaluationResult, CoverageResult coverageResult) {
        if (evaluationResult == null || coverageResult == null) {
            return;
        }
        evaluationResult.setLineCoverage(coverageResult.getLineCoverage());
        evaluationResult.setBranchCoverage(coverageResult.getBranchCoverage());
        evaluationResult.setCoverageStatus(coverageResult.getStatus());
        evaluationResult.setCoverageMessage(coverageResult.getMessage());
    }

    private CoverageResult buildCoverageResult(boolean success, String status, Double lineCoverage, Double branchCoverage, String message) {
        CoverageResult result = new CoverageResult();
        result.setSuccess(success);
        result.setStatus(status);
        result.setLineCoverage(lineCoverage);
        result.setBranchCoverage(branchCoverage);
        result.setMessage(message);
        return result;
    }

    private EvaluationResult normalizeEvaluationResult(EvaluationResult evaluationResult, GenerateRequest request, String resultText) {
        EvaluationResult result = evaluationResult == null ? new EvaluationResult() : evaluationResult;
        result.setGeneratedTestCase(nullToEmpty(resultText));
        result.setNormalPathCoverage(defaultIfBlank(
                result.getNormalPathCoverage(),
                isEnglishOutput(request)
                        ? "Evaluation completed, but the model did not return normal path coverage details."
                        : "评估已执行，但模型未返回正常路径覆盖说明。"
        ));
        result.setBoundaryCoverage(defaultIfBlank(
                result.getBoundaryCoverage(),
                isEnglishOutput(request)
                        ? "Evaluation completed, but the model did not return boundary-condition coverage details."
                        : "评估已执行，但模型未返回边界条件覆盖说明。"
        ));
        result.setExceptionCoverage(defaultIfBlank(
                result.getExceptionCoverage(),
                isEnglishOutput(request)
                        ? "Evaluation completed, but the model did not return exception-branch coverage details."
                        : "评估已执行，但模型未返回异常分支覆盖说明。"
        ));
        result.setSyntaxNorm(defaultIfBlank(
                result.getSyntaxNorm(),
                isEnglishOutput(request)
                        ? "Evaluation completed, but the model did not return syntax-compliance details."
                        : "评估已执行，但模型未返回语法规范性说明。"
        ));

        List<String> cleanedSuggestions = cleanSuggestionList(result.getSuggestions());
        if (cleanedSuggestions.isEmpty()) {
            cleanedSuggestions.add(
                    isEnglishOutput(request)
                            ? "Evaluation completed, but the model did not return improvement suggestions. Please review the generated test cases manually."
                            : "评估已执行，但模型未返回改进建议，请结合生成结果人工复核。"
            );
        }
        result.setSuggestions(cleanedSuggestions);
        return result;
    }

    private EvaluationResult buildPendingEvaluationResult(GenerateRequest request, boolean generationFailed, String reason) {
        EvaluationResult result = new EvaluationResult();
        result.setGeneratedTestCase(nullToEmpty(request == null ? null : request.getResultText()));

        String reasonText = nullToEmpty(reason).trim();
        boolean englishOutput = isEnglishOutput(request);
        if (generationFailed) {
            result.setNormalPathCoverage(englishOutput
                    ? "Evaluation was not executed because the batch generation step failed."
                    : "未执行评估，因为本条批量生成记录在生成阶段已失败。");
            result.setBoundaryCoverage(englishOutput
                    ? "Evaluation was not executed because the batch generation step failed."
                    : "未执行评估，因为本条批量生成记录在生成阶段已失败。");
            result.setExceptionCoverage(englishOutput
                    ? "Evaluation was not executed because the batch generation step failed."
                    : "未执行评估，因为本条批量生成记录在生成阶段已失败。");
            result.setSyntaxNorm(englishOutput
                    ? "No valid generated test case was produced, so syntax compliance could not be evaluated."
                    : "未形成有效测试用例，暂时无法完成语法规范性评估。");
        } else {
            result.setNormalPathCoverage(englishOutput
                    ? "The record has been generated, but the evaluation details are still being completed."
                    : "该记录已生成，但评估明细仍在补齐中。");
            result.setBoundaryCoverage(englishOutput
                    ? "The record has been generated, but the evaluation details are still being completed."
                    : "该记录已生成，但评估明细仍在补齐中。");
            result.setExceptionCoverage(englishOutput
                    ? "The record has been generated, but the evaluation details are still being completed."
                    : "该记录已生成，但评估明细仍在补齐中。");
            result.setSyntaxNorm(englishOutput
                    ? "The record has been generated, but the evaluation details are still being completed."
                    : "该记录已生成，但评估明细仍在补齐中。");
        }

        List<String> suggestions = new ArrayList<>();
        suggestions.add(reasonText.isEmpty()
                ? (englishOutput
                    ? "Please reopen the detail dialog later or rerun the batch generation if the evaluation details are still missing."
                    : "建议稍后重新打开详情查看；若评估内容仍缺失，可重新发起批量生成。")
                : (englishOutput
                    ? "Please check the evaluation process status. Detail: " + reasonText
                    : "建议检查评估流程状态，详细原因：" + reasonText));
        result.setSuggestions(suggestions);
        return result;
    }

    private String serializeSuggestionText(List<String> suggestions) {
        if (suggestions == null || suggestions.isEmpty()) {
            return null;
        }

        List<String> cleanedSuggestions = suggestions.stream()
                .map(item -> nullToEmpty(item).trim())
                .filter(item -> !item.isEmpty())
                .collect(Collectors.toList());
        if (cleanedSuggestions.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(cleanedSuggestions);
        } catch (Exception e) {
            return String.join("\n", cleanedSuggestions);
        }
    }

    private List<String> parseSuggestionText(String suggestionText, List<String> fallbackSuggestions) {
        String text = nullToEmpty(suggestionText).trim();
        if (!text.isEmpty()) {
            try {
                JsonNode node = objectMapper.readTree(text);
                if (node.isArray()) {
                    List<String> result = new ArrayList<>();
                    for (JsonNode item : node) {
                        String value = nullToEmpty(item.asText()).trim();
                        if (!value.isEmpty()) {
                            result.add(value);
                        }
                    }
                    if (!result.isEmpty()) {
                        return result;
                    }
                }
            } catch (Exception e) {
                List<String> result = new ArrayList<>();
                for (String item : text.split("\\r?\\n+")) {
                    String value = nullToEmpty(item).trim();
                    if (!value.isEmpty()) {
                        result.add(value);
                    }
                }
                if (!result.isEmpty()) {
                    return result;
                }
            }
        }

        return cleanSuggestionList(fallbackSuggestions);
    }

    private List<String> cleanSuggestionList(List<String> suggestions) {
        if (suggestions == null) {
            return new ArrayList<>();
        }

        return suggestions.stream()
                .map(item -> nullToEmpty(item).trim())
                .filter(item -> !item.isEmpty())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return nullToEmpty(value).trim().isEmpty() ? defaultValue : value;
    }

    private List<BatchExperimentModelStat> buildBatchModelStats(Map<Integer, BatchModelStatCounter> counterMap) {
        List<BatchExperimentModelStat> stats = new ArrayList<>();
        if (counterMap == null || counterMap.isEmpty()) {
            return stats;
        }

        for (BatchModelStatCounter counter : counterMap.values()) {
            BatchExperimentModelStat stat = new BatchExperimentModelStat();
            stat.setModelConfigId(counter.getModelConfigId());
            stat.setModelName(counter.getModelName());
            stat.setTotalCount(counter.getTotalCount());
            stat.setSuccessCount(counter.getSuccessCount());
            stat.setFailedCount(counter.getFailedCount());
            stat.setAverageEvaluationScore(calculateAverageScore(counter.getEvaluationScores()));
            stat.setAverageTesterScore(calculateAverageScore(counter.getTesterScores()));
            stats.add(stat);
        }
        return stats;
    }

    private String currentDateTimeText() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private String buildBatchMessage(String resultText, EvaluationResult evaluationResult) {
        if (isFailureText(resultText)) {
            return firstLine(resultText);
        }
        if (evaluationResult == null) {
            return "评估结果为空";
        }
        if (evaluationResult.getScore() == null) {
            return "已生成，评估明细已返回，但评估分数暂未返回";
        }
        return "已生成并完成评估";
    }

    private String firstLine(String text) {
        String value = nullToEmpty(text).replace("\r", "").trim();
        if (value.isEmpty()) {
            return "";
        }
        int index = value.indexOf("\r");
        return index >= 0 ? value.substring(0, index).trim() : value;
    }

    private String buildPrompt(GenerateRequest request) {
        if (Boolean.TRUE.equals(request.getRegenerate())) {
            return buildRegeneratePrompt(request);
        }
        if ("cot".equals(request.getStrategy())) {
            return buildCotPrompt(request);
        }
        return buildNormalPrompt(request);
    }

    private String buildNormalPrompt(GenerateRequest request) {
        String languageRule = buildLanguageRule(request);

        if (isRequirementSource(request)) {
            return "You are now acting as an experienced software test engineer. Please generate test cases directly according to the given software requirement specification.\n\n"
                    + "Output requirements:\n"
                    + "1. " + languageRule + "\n"
                    + "2. Do not write analysis, summary, or explanatory text.\n"
                    + "3. Start directly with the test cases.\n"
                    + "4. Each test case must strictly contain the following five fields in this exact order:\n"
                    + getCaseFieldTemplate(request)
                    + "5. Do not use any markdown symbols before or after field names.\n"
                    + "6. Strictly forbid output of *, **, #, -, >, ``` and other markdown formatting symbols.\n"
                    + "7. Leave one blank line between test cases.\n"
                    + "8. Generate 6 test cases.\n"
                    + "9. Cover normal flow, boundary scenarios, exception scenarios, and invalid input scenarios.\n\n"
                    + "Wrong example: **" + getFieldLabel(request, "caseNo") + "** TC-001\n"
                    + "Correct example: " + getFieldLabel(request, "caseNo") + "TC-001\n\n"
                    + "Requirement Name: " + nullToEmpty(request.getRequirementName()) + "\n"
                    + "Module Name: " + nullToEmpty(request.getModuleName()) + "\n"
                    + "Requirement Content:\n" + nullToEmpty(request.getRequirementContent()) + "\n\n"
                    + "Preconditions: " + nullToEmpty(request.getPreconditionDesc()) + "\n"
                    + "Expected Description: " + nullToEmpty(request.getExpectedDesc()) + "\n"
                    + "Remark: " + nullToEmpty(request.getRemark()) + "\n\n"
                    + "Please output the test cases directly.";
        }

        return "You are now acting as an experienced software test engineer. Please generate function-level test cases directly according to the given function information.\n\n"
                + "Output requirements:\n"
                + "1. " + languageRule + "\n"
                + "2. Do not write analysis or summary.\n"
                + "3. Output test cases directly.\n"
                + "4. Each test case must strictly contain the following five fields in this exact order:\n"
                + getCaseFieldTemplate(request)
                + "5. Do not use any markdown symbols before or after field names.\n"
                + buildMarkdownFormatRule(request, 6, false, false)
                + "7. Leave one blank line between test cases.\n"
                + "8. Generate 6 test cases.\n"
                + "9. Cover normal inputs, boundary values, null values, invalid values, and exception scenarios.\n\n"
                + "Wrong example: **" + getFieldLabel(request, "caseNo") + "** TC-001\n"
                + "Correct example: " + getFieldLabel(request, "caseNo") + "TC-001\n\n"
                + buildPythonUnittestRequirement(request)
                + buildTesterParticipationHint(request)
                + "Function Name: " + nullToEmpty(request.getFunctionName()) + "\n"
                + "Class Name: " + nullToEmpty(request.getClassName()) + "\n"
                + "Function Code:\n" + nullToEmpty(request.getCodeText()) + "\n\n"
                + "Input Description: " + nullToEmpty(request.getInputDesc()) + "\n"
                + "Output Description: " + nullToEmpty(request.getOutputDesc()) + "\n"
                + "Remark: " + nullToEmpty(request.getRemark()) + "\n\n"
                + "Please output the test cases directly.";
    }

    private String buildCotPrompt(GenerateRequest request) {
        String languageRule = buildLanguageRule(request);

        if (isRequirementSource(request)) {
            if (isEnglishOutput(request)) {
                return "You are now acting as a senior software test engineer. Please generate test cases based on the software requirement specification by following the method of \"analyze the requirement first, then design test cases\".\n\n"
                        + "Your task is not to provide generic analysis. You must design test cases strictly around business rules, input constraints, preconditions, and exception scenarios in the requirement specification.\n\n"
                        + "Please output strictly in the following two parts:\n\n"
                        + "Part 1: Requirement Analysis\n"
                        + "Please output the following from a testing perspective:\n"
                        + "1. Requirement Objective: explain in one sentence what this requirement is intended to achieve.\n"
                        + "2. Key Business Rule Identification: list the key processing rules, constraints, and decision scenarios in the requirement.\n"
                        + "3. Inputs and Preconditions: summarize the input data requirements, preconditions, and dependent conditions.\n"
                        + "4. Key Test Points: identify the mandatory normal flows, boundary scenarios, and exception scenarios that must be covered.\n\n"
                        + "Part 2: Test Cases\n"
                        + "Please generate test cases according to the key points identified in Part 1.\n"
                        + "Requirements:\n"
                        + "1. Each key business rule must correspond to at least one test case.\n"
                        + "2. Normal scenarios, boundary scenarios, exception scenarios, and invalid input scenarios must be covered.\n"
                        + "3. If the requirement contains state checks, conditional constraints, field validation, or process branches, they must be clearly reflected in the test cases.\n"
                        + "4. Each test case must strictly contain the following fields in this exact order:\n"
                        + getCaseFieldTemplate(request)
                        + "5. Do not use any markdown symbols before or after field names.\n"
                        + "6. Strictly forbid output of *, **, #, -, >, ``` and other markdown formatting symbols.\n"
                        + "7. Leave one blank line between test cases.\n\n"
                        + "Output requirements:\n"
                        + "1. " + languageRule + "\n"
                        + "2. You must output \"Requirement Analysis\" first, then \"Test Cases\".\n"
                        + "3. The analysis should be concise but must cover the key rules.\n"
                        + "4. The number of test cases may be more than 5 as long as all key rules are sufficiently covered.\n"
                        + "5. Do not use markdown format.\n"
                        + "6. Do not write a summary or irrelevant explanation.\n"
                        + "7. The test cases must directly match the requirement rules and must not be generic.\n"
                        + "8. Wrong example: **" + getFieldLabel(request, "caseNo") + "** TC-001\n"
                        + "9. Correct example: " + getFieldLabel(request, "caseNo") + "TC-001\n\n"
                        + "Requirement Name: " + nullToEmpty(request.getRequirementName()) + "\n"
                        + "Module Name: " + nullToEmpty(request.getModuleName()) + "\n"
                        + "Requirement Content:\n" + nullToEmpty(request.getRequirementContent()) + "\n\n"
                        + "Preconditions: " + nullToEmpty(request.getPreconditionDesc()) + "\n"
                        + "Expected Description: " + nullToEmpty(request.getExpectedDesc()) + "\n"
                        + "Remark: " + nullToEmpty(request.getRemark()) + "\n\n"
                        + "Please start outputting now.";
            }

            return "你现在扮演一名资深软件测试工程师，请采用“先分析需求，再设计测试用例”的方式完成基于需求规格说明书的测试用例生成。\n\n"
                    + "你的任务不是泛泛分析，而是严格围绕需求中的业务规则、输入约束、前置条件和异常场景设计测试用例。\n\n"
                    + "请严格按以下两部分输出：\n\n"
                    + "第一部分：需求分析\n"
                    + "请按测试视角输出以下内容：\n"
                    + "1. 需求目标：一句话说明该需求要实现什么；\n"
                    + "2. 关键业务规则识别：列出需求中的关键处理规则、约束条件、判断场景；\n"
                    + "3. 输入与前置条件：总结输入数据要求、前置条件和依赖条件；\n"
                    + "4. 关键测试点：指出必须覆盖的正常流程、边界场景和异常场景。\n\n"
                    + "第二部分：测试用例\n"
                    + "请根据第一部分识别出的关键点生成测试用例。\n"
                    + "要求：\n"
                    + "1. 每一个关键业务规则至少对应一条测试用例；\n"
                    + "2. 必须覆盖正常场景、边界场景、异常场景、非法输入场景；\n"
                    + "3. 如果需求中存在状态判断、条件限制、字段校验或流程分支，必须明确体现在测试用例中；\n"
                    + "4. 每条测试用例必须严格包含以下字段，并按这个顺序输出：\n"
                    + getCaseFieldTemplate(request)
                    + "5. 字段名前后禁止出现任何 markdown 符号；\n"
                    + "6. 严禁输出 *, **, #, -, >, ``` 等格式符号；\n"
                    + "7. 测试用例之间空一行；\n\n"
                    + "输出要求：\n"
                    + "1. " + languageRule + "\n"
                    + "2. 必须先输出“需求分析”，再输出“测试用例”；\n"
                    + "3. 分析要简洁，但必须写出关键规则；\n"
                    + "4. 测试用例数量可以超过 5 条，只要能完整覆盖关键规则；\n"
                    + "5. 不要使用 markdown 格式；\n"
                    + "6. 不要写总结，不要写无关解释；\n"
                    + "7. 测试用例必须直接贴合需求规则，不要空泛；\n"
                    + "8. 错误示例：**" + getFieldLabel(request, "caseNo") + "** TC-001；\n"
                    + "9. 正确示例：" + getFieldLabel(request, "caseNo") + "TC-001。\n\n"
                    + "需求名称：" + nullToEmpty(request.getRequirementName()) + "\n"
                    + "所属模块：" + nullToEmpty(request.getModuleName()) + "\n"
                    + "需求内容：\n" + nullToEmpty(request.getRequirementContent()) + "\n\n"
                    + "前置条件：" + nullToEmpty(request.getPreconditionDesc()) + "\n"
                    + "预期说明：" + nullToEmpty(request.getExpectedDesc()) + "\n"
                    + "备注：" + nullToEmpty(request.getRemark()) + "\n\n"
                    + "请开始输出。";
        }

        if (isEnglishOutput(request)) {
            return "You are now acting as a senior software test engineer. Please generate function-level test cases by following the method of \"identify code branches first, then design test cases\".\n\n"
                    + "Your task is not to provide generic analysis. You must design test cases strictly around the conditional judgments, return branches, and exception branches in the function code.\n\n"
                    + "Please output strictly in the following two parts:\n\n"
                    + "Part 1: Function Analysis\n"
                    + "Please output the following from a testing perspective:\n"
                    + "1. Functional Description: explain in one sentence what the function does.\n"
                    + "2. Key Branch Identification: list the logical branches corresponding to each if / else / return / throw in the code.\n"
                    + "3. Input Constraints: summarize parameter types, valid ranges, and null restrictions.\n"
                    + "4. Key Test Points: identify the branches and boundary conditions that must be covered.\n\n"
                    + "Part 2: Test Cases\n"
                    + "Please generate test cases according to the key branches identified in Part 1.\n"
                    + "Requirements:\n"
                    + "1. Each key logic branch must correspond to at least one test case.\n"
                    + "2. No scenario corresponding to any return or throw in the code may be omitted.\n"
                    + "3. Normal scenarios, null scenarios, boundary value scenarios, invalid value scenarios, and exception scenarios must be covered.\n"
                    + "4. If the code contains age checks, length checks, empty-string checks, or exception throwing logic, these must be explicitly reflected in the test cases.\n"
                    + "5. Each test case must strictly contain the following fields in this exact order:\n"
                    + getCaseFieldTemplate(request)
                    + "6. Do not use any markdown symbols before or after field names.\n"
                    + buildMarkdownFormatRule(request, 7, false, false)
                    + "8. Leave one blank line between test cases.\n\n"
                    + "Output requirements:\n"
                    + "1. " + languageRule + "\n"
                    + "2. You must output \"Function Analysis\" first, then \"Test Cases\".\n"
                    + "3. The function analysis should be concise but must cover the key branches.\n"
                    + "4. The number of test cases may be more than 5 as long as all key branches are covered.\n"
                    + buildMarkdownFormatRule(request, 5, false, true)
                    + "6. Do not write any summary or irrelevant explanation.\n"
                    + "7. The test cases must directly match the code branches and must not be generic.\n"
                    + "8. Wrong example: **" + getFieldLabel(request, "caseNo") + "** TC-001\n"
                    + "9. Correct example: " + getFieldLabel(request, "caseNo") + "TC-001\n\n"
                    + buildPythonUnittestRequirement(request)
                    + buildTesterParticipationHint(request)
                    + "Function Name: " + nullToEmpty(request.getFunctionName()) + "\n"
                    + "Class Name: " + nullToEmpty(request.getClassName()) + "\n"
                    + "Function Code:\n" + nullToEmpty(request.getCodeText()) + "\n\n"
                    + "Input Description: " + nullToEmpty(request.getInputDesc()) + "\n"
                    + "Output Description: " + nullToEmpty(request.getOutputDesc()) + "\n"
                    + "Remark: " + nullToEmpty(request.getRemark()) + "\n\n"
                    + "Please start outputting now.";
        }

        return "你现在扮演一名资深软件测试工程师，请采用“先识别代码分支，再设计测试用例”的方式完成函数级测试用例生成。\n\n"
                + "你的任务不是泛泛分析，而是严格围绕函数代码中的条件判断、返回分支和异常分支设计测试用例。\n\n"
                + "请严格按以下两部分输出：\n\n"
                + "第一部分：函数分析\n"
                + "请按测试视角输出以下内容：\n"
                + "1. 功能说明：一句话说明函数作用；\n"
                + "2. 关键分支识别：列出代码中每一个 if / else / return / throw 对应的逻辑分支；\n"
                + "3. 输入约束：总结参数类型、合法范围、空值限制；\n"
                + "4. 关键测试点：指出必须覆盖的分支和边界条件。\n\n"
                + "第二部分：测试用例\n"
                + "请根据第一部分识别出的关键分支生成测试用例。\n"
                + "要求：\n"
                + "1. 每一个关键逻辑分支至少对应一条测试用例；\n"
                + "2. 不能遗漏代码中的任何 return 或 throw 对应场景；\n"
                + "3. 必须覆盖正常场景、空值场景、边界值场景、非法值或异常场景；\n"
                + "4. 如果代码中存在年龄判断、长度判断、空字符串判断、异常抛出等逻辑，必须明确体现在测试用例中；\n"
                + "5. 每条测试用例必须严格包含以下字段，并按这个顺序输出：\n"
                + getCaseFieldTemplate(request)
                + "6. 字段名前后禁止出现任何 markdown 符号；\n"
                + buildMarkdownFormatRule(request, 7, true, false)
                + "8. 测试用例之间空一行；\n\n"
                + "输出要求：\n"
                + "1. " + languageRule + "\n"
                + "2. 必须先输出“函数分析”，再输出“测试用例”；\n"
                + "3. 函数分析要简洁，但必须写出关键分支；\n"
                + "4. 测试用例数量可以超过 5 条，只要能完整覆盖关键分支；\n"
                + buildMarkdownFormatRule(request, 5, true, true)
                + buildMarkdownFormatRule(request, 6, true, false)
                + "7. 不要写总结，不要写无关解释；\n"
                + "8. 测试用例必须直接贴合代码分支，不要空泛；\n"
                + "9. 错误示例：**" + getFieldLabel(request, "caseNo") + "** TC-001；\n"
                + "10. 正确示例：" + getFieldLabel(request, "caseNo") + "TC-001。\n\n"
                + buildPythonUnittestRequirement(request)
                + buildTesterParticipationHint(request)
                + "函数名称：" + nullToEmpty(request.getFunctionName()) + "\n"
                + "所属类名：" + nullToEmpty(request.getClassName()) + "\n"
                + "函数代码：\n" + nullToEmpty(request.getCodeText()) + "\n\n"
                + "输入说明：" + nullToEmpty(request.getInputDesc()) + "\n"
                + "输出说明：" + nullToEmpty(request.getOutputDesc()) + "\n"
                + "备注：" + nullToEmpty(request.getRemark()) + "\n\n"
                + "请开始输出。";
    }

    private String buildRegeneratePrompt(GenerateRequest request) {
        String languageRule = buildLanguageRule(request);

        if (isRequirementSource(request)) {
            return "You are now acting as a senior software test engineer. Based on the existing test cases and the existing evaluation results, you need to regenerate and optimize the test cases generated from the software requirement specification.\n\n"
                    + "Please improve strictly around the requirement content itself and supplement the missing or insufficiently covered parts in the original test cases.\n\n"
                    + "Task requirements:\n"
                    + "1. Keep the reasonable parts of the existing test cases.\n"
                    + "2. According to the evaluation comments, supplement deficiencies in normal flow, boundary conditions, exception branches, and invalid inputs.\n"
                    + "3. Output more complete and more standardized test cases.\n"
                    + "4. " + languageRule + "\n"
                    + "5. Each test case must strictly contain the following five fields in this exact order:\n"
                    + getCaseFieldTemplate(request)
                    + "6. Do not use any markdown symbols before or after field names.\n"
                    + "7. Strictly forbid output of *, **, #, -, >, ``` and other markdown formatting symbols.\n"
                    + "8. Leave one blank line between test cases.\n"
                    + "9. Do not use markdown and do not write a summary.\n\n"
                    + "Requirement Name: " + nullToEmpty(request.getRequirementName()) + "\n"
                    + "Module Name: " + nullToEmpty(request.getModuleName()) + "\n"
                    + "Requirement Content:\n" + nullToEmpty(request.getRequirementContent()) + "\n\n"
                    + "Preconditions: " + nullToEmpty(request.getPreconditionDesc()) + "\n"
                    + "Expected Description: " + nullToEmpty(request.getExpectedDesc()) + "\n"
                    + "Remark: " + nullToEmpty(request.getRemark()) + "\n\n"
                    + "Previous-round generated test cases:\n"
                    + nullToEmpty(request.getPreviousResultText()) + "\n\n"
                    + "Summary of previous-round evaluation results:\n"
                    + nullToEmpty(request.getPreviousEvaluationSummary()) + "\n\n"
                    + "Please directly output the optimized complete test cases.";
        }

        return "You are now acting as a senior software test engineer. Based on the existing test cases and the existing evaluation results, you need to regenerate and optimize the test cases.\n\n"
                + "Please improve strictly around the function code itself and supplement the missing or insufficiently covered parts in the original test cases.\n\n"
                + "Task requirements:\n"
                + "1. Keep the reasonable parts of the existing test cases.\n"
                + "2. According to the evaluation comments, supplement deficiencies in normal paths, boundary conditions, and exception branches.\n"
                + "3. Output more complete and more standardized test cases.\n"
                + "4. " + languageRule + "\n"
                + "5. Each test case must strictly contain the following five fields in this exact order:\n"
                + getCaseFieldTemplate(request)
                + "6. Do not use any markdown symbols before or after field names.\n"
                + buildMarkdownFormatRule(request, 7, false, false)
                + "8. Leave one blank line between test cases.\n"
                + buildMarkdownFormatRule(request, 9, false, true)
                + buildPythonUnittestRequirement(request)
                + buildTesterParticipationHint(request)
                + "Function Name: " + nullToEmpty(request.getFunctionName()) + "\n"
                + "Class Name: " + nullToEmpty(request.getClassName()) + "\n"
                + "Function Code:\n" + nullToEmpty(request.getCodeText()) + "\n\n"
                + "Input Description: " + nullToEmpty(request.getInputDesc()) + "\n"
                + "Output Description: " + nullToEmpty(request.getOutputDesc()) + "\n"
                + "Remark: " + nullToEmpty(request.getRemark()) + "\n\n"
                + "Previous-round generated test cases:\n"
                + nullToEmpty(request.getPreviousResultText()) + "\n\n"
                + "Summary of previous-round evaluation results:\n"
                + nullToEmpty(request.getPreviousEvaluationSummary()) + "\n\n"
                + "Please directly output the optimized complete test cases.";
    }

    private String buildLanguageRule(GenerateRequest request) {
        if (isEnglishOutput(request)) {
            return "All field names and all content must be in English.";
        }
        return "所有字段名和所有内容都必须使用中文。";
    }

    private String buildMarkdownFormatRule(GenerateRequest request, int number, boolean chinese, boolean generalRule) {
        boolean python = shouldAppendPythonUnittestRequirement(request);
        if (chinese) {
            if (python) {
                if (generalRule) {
                    return number + ". 测试用例说明部分不要使用 markdown 格式；最后的 Python unittest 代码块是唯一允许的 Markdown 内容。\n";
                }
                return number + ". 测试用例说明部分严禁输出 *, **, #, -, >, ``` 等 markdown 符号；最后必须输出的 ```python 代码块除外。\n";
            }
            if (generalRule) {
                return number + ". 不要使用 markdown 格式；\n";
            }
            return number + ". 严禁输出 *, **, #, -, >, ``` 等格式符号；\n";
        }

        if (python) {
            if (generalRule) {
                return number + ". Do not use Markdown format in the test case descriptions; the final Python unittest code block is the only allowed Markdown content.\n";
            }
            return number + ". Strictly forbid *, **, #, -, >, ``` and other Markdown symbols in the test case descriptions, except for the final required ```python code block.\n";
        }
        if (generalRule) {
            return number + ". Do not use markdown and do not write a summary.\n";
        }
        return number + ". Strictly forbid output of *, **, #, -, >, ``` and other markdown formatting symbols.\n";
    }

    private String buildPythonUnittestRequirement(GenerateRequest request) {
        if (!shouldAppendPythonUnittestRequirement(request)) {
            return "";
        }
        if (isEnglishOutput(request)) {
            return """
                    Additional Python unittest requirement:
                    After all test case descriptions, output exactly one executable Python unittest Markdown code block.
                    The code block must:
                    1. Start with ```python and end with ```.
                    2. Include import unittest.
                    3. Define a test class, for example class TestGeneratedCases(unittest.TestCase):.
                    4. Define test methods whose names start with test_.
                    5. Not redefine the target function.
                    6. Assume the target function is already importable through from target_module import *.
                    7. Not write from target_module import *, because CoverageService will add that import automatically.
                    8. Not output any other Python code outside the unittest test code.
                    9. Write assertions strictly according to the actual behavior of the given function code, not according to guessed business rules.
                    10. Do not assert exception handling that the function code does not implement.
                    11. If the function code does not explicitly raise exceptions, do not use with self.assertRaises(...).
                    12. For Python slicing, empty strings, negative indexes, and similar Python language behavior, infer the expected result from actual Python semantics.
                    13. Boundary cases are allowed, but their expected results must match the current function implementation.
                    14. The unittest code is used for coverage collection; prioritize runnable tests and correct assertions.
                    15. Do not test None input unless the function code explicitly handles None.
                    16. Do not test mismatched input types unless the function code explicitly handles type errors.

                    Required code block format example:
                    ```python
                    import unittest

                    class TestGeneratedCases(unittest.TestCase):
                        def test_case_1(self):
                            self.assertEqual(target_function_call, expected_result)

                    if __name__ == "__main__":
                        unittest.main()
                    ```

                    """;
        }
        return """
                如果被测代码语言为 Python，请在所有测试用例说明之后额外输出且只输出一个可执行的 Python unittest Markdown 代码块。
                代码块必须满足：
                1. 必须以 ```python 开头，以 ``` 结束。
                2. 必须包含 import unittest。
                3. 必须定义一个测试类，例如 class TestGeneratedCases(unittest.TestCase):。
                4. 测试方法必须以 test_ 开头。
                5. 测试代码不需要重新定义被测函数。
                6. 测试代码应假设被测函数已经可以通过 from target_module import * 导入。
                7. 不要在测试代码中写 from target_module import *，因为 CoverageService 会自动补充该导入语句。
                8. 不要输出除 unittest 测试代码以外的其他 Python 代码。
                9. 测试代码必须严格根据被测函数的实际代码行为编写断言，不要根据任务描述臆测额外业务规则。
                10. 不要断言被测函数没有实现的异常处理逻辑。
                11. 如果函数代码没有显式抛出异常，不要写 with self.assertRaises(...)。
                12. 对于 Python 切片、空字符串、负数下标等情况，应按照 Python 实际运行语义推断期望结果。
                13. 对边界场景可以保留测试，但期望结果必须与当前函数代码实际行为一致。
                14. 测试代码只用于覆盖率统计，应优先保证可运行和断言正确。
                15. 不要测试 None 输入，除非函数代码中显式处理 None。
                16. 不要测试类型不匹配输入，除非函数代码中显式处理类型异常。

                必须使用如下代码块格式：
                ```python
                import unittest

                class TestGeneratedCases(unittest.TestCase):
                    def test_case_1(self):
                        self.assertEqual(被测函数调用, 期望结果)

                if __name__ == "__main__":
                    unittest.main()
                ```

                """;
    }

    private boolean shouldAppendPythonUnittestRequirement(GenerateRequest request) {
        if (request == null || isRequirementSource(request)) {
            return false;
        }
        return isPythonFunction(request.getLanguage(), request.getCodeText());
    }

    private boolean isEnglishOutput(GenerateRequest request) {
        return "en".equalsIgnoreCase(nullToEmpty(request.getOutputLanguage()).trim());
    }

    private String getCaseFieldTemplate(GenerateRequest request) {
        if (isEnglishOutput(request)) {
            return "Test Case ID:\n"
                    + "Test Objective:\n"
                    + "Input Data:\n"
                    + "Execution Steps:\n"
                    + "Expected Result:\n";
        }
        return "用例编号：\n"
                + "测试目的：\n"
                + "输入数据：\n"
                + "执行步骤：\n"
                + "预期结果：\n";
    }

    private String getFieldLabel(GenerateRequest request, String fieldKey) {
        if (isEnglishOutput(request)) {
            return switch (fieldKey) {
                case "caseNo" -> "Test Case ID: ";
                case "purpose" -> "Test Objective: ";
                case "input" -> "Input Data: ";
                case "steps" -> "Execution Steps: ";
                case "expected" -> "Expected Result: ";
                default -> "";
            };
        }
        return switch (fieldKey) {
            case "caseNo" -> "用例编号：";
            case "purpose" -> "测试目的：";
            case "input" -> "输入数据：";
            case "steps" -> "执行步骤：";
            case "expected" -> "预期结果：";
            default -> "";
        };
    }

    private String normalizeGeneratedResult(String resultText, GenerateRequest request) {
        if (shouldAppendPythonUnittestRequirement(request)) {
            return normalizeGeneratedResultPreservingCodeBlocks(resultText);
        }
        return normalizeGeneratedResult(resultText);
    }

    private String normalizeGeneratedResult(String resultText) {
        String text = nullToEmpty(resultText)
                .replace("\r", "")
                .replace("```", "")
                .replace("**", "")
                .replace("__", "")
                .replace("###", "")
                .replace("##", "")
                .replace("#", "")
                .trim();

        StringBuilder builder = new StringBuilder();
        for (String line : text.split("\n")) {
            String cleaned = line.replaceFirst("^\\s*[-*>]+\\s*", "").trim();
            if (!cleaned.isEmpty()) {
                builder.append(cleaned).append("\n");
            } else {
                builder.append("\n");
            }
        }
        return builder.toString().trim();
    }

    private String normalizeGeneratedResultPreservingCodeBlocks(String resultText) {
        String text = nullToEmpty(resultText).replace("\r", "").trim();
        if (text.isEmpty()) {
            return "";
        }

        Pattern codeBlockPattern = Pattern.compile("(?s)```.*?```");
        Matcher matcher = codeBlockPattern.matcher(text);
        StringBuilder builder = new StringBuilder();
        int lastIndex = 0;
        while (matcher.find()) {
            appendCleanedGeneratedText(builder, text.substring(lastIndex, matcher.start()));
            appendBlock(builder, matcher.group().trim());
            lastIndex = matcher.end();
        }
        appendCleanedGeneratedText(builder, text.substring(lastIndex));
        return builder.toString().trim();
    }

    private void appendCleanedGeneratedText(StringBuilder builder, String segment) {
        String cleaned = normalizeGeneratedResult(segment);
        if (cleaned.isEmpty()) {
            return;
        }
        appendBlock(builder, cleaned);
    }

    private void appendBlock(StringBuilder builder, String block) {
        if (block == null || block.trim().isEmpty()) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append("\n\n");
        }
        builder.append(block.trim());
    }

    private String buildEvaluationSourceContext(GenerateRequest request) {
        if (isRequirementSource(request)) {
            if (isEnglishOutput(request)) {
                return "Requirement Name: " + nullToEmpty(request.getRequirementName()) + "\n"
                        + "Module Name: " + nullToEmpty(request.getModuleName()) + "\n"
                        + "Requirement Content:\n" + nullToEmpty(request.getRequirementContent()) + "\n\n"
                        + "Preconditions: " + nullToEmpty(request.getPreconditionDesc()) + "\n"
                        + "Expected Description: " + nullToEmpty(request.getExpectedDesc()) + "\n"
                        + "Remark: " + nullToEmpty(request.getRemark()) + "\n";
            }
            return "需求名称：" + nullToEmpty(request.getRequirementName()) + "\n"
                    + "所属模块：" + nullToEmpty(request.getModuleName()) + "\n"
                    + "需求内容：\n" + nullToEmpty(request.getRequirementContent()) + "\n\n"
                    + "前置条件：" + nullToEmpty(request.getPreconditionDesc()) + "\n"
                    + "预期说明：" + nullToEmpty(request.getExpectedDesc()) + "\n"
                    + "备注：" + nullToEmpty(request.getRemark()) + "\n";
        }

        if (isEnglishOutput(request)) {
            return buildTesterParticipationHint(request)
                    + "Function Name: " + nullToEmpty(request.getFunctionName()) + "\n"
                    + "Class Name: " + nullToEmpty(request.getClassName()) + "\n"
                    + "Function Code:\n" + nullToEmpty(request.getCodeText()) + "\n\n"
                    + "Input Description: " + nullToEmpty(request.getInputDesc()) + "\n"
                    + "Output Description: " + nullToEmpty(request.getOutputDesc()) + "\n"
                    + "Remark: " + nullToEmpty(request.getRemark()) + "\n";
        }

        return buildTesterParticipationHint(request)
                + "函数名称：" + nullToEmpty(request.getFunctionName()) + "\n"
                + "所属类名：" + nullToEmpty(request.getClassName()) + "\n"
                + "函数代码：\n" + nullToEmpty(request.getCodeText()) + "\n\n"
                + "输入说明：" + nullToEmpty(request.getInputDesc()) + "\n"
                + "输出说明：" + nullToEmpty(request.getOutputDesc()) + "\n"
                + "备注：" + nullToEmpty(request.getRemark()) + "\n";
    }

    private String buildEvaluationPrompt(GenerateRequest request, String sourceContext, String resultText) {
        if (isEnglishOutput(request)) {
            return "You are a professional software test case review expert. Please evaluate the quality of the generated test cases based on the given business context and generated test cases.\n\n"
                    + "Please evaluate the following dimensions and return the result in JSON format:\n"
                    + "1. normalPathCoverage: coverage of normal execution paths\n"
                    + "2. boundaryCoverage: coverage of boundary conditions\n"
                    + "3. exceptionCoverage: coverage of exception branches\n"
                    + "4. syntaxNorm: syntax and structural compliance evaluation\n"
                    + "5. score: an integer score from 0 to 100\n"
                    + "6. suggestions: improvement suggestions, returned as a string array\n\n"
                    + "Output requirements:\n"
                    + "1. Return JSON only. Do not write explanations.\n"
                    + "2. The JSON structure must be exactly:\n"
                    + "{\n"
                    + "  \"normalPathCoverage\": \"...\",\n"
                    + "  \"boundaryCoverage\": \"...\",\n"
                    + "  \"exceptionCoverage\": \"...\",\n"
                    + "  \"syntaxNorm\": \"...\",\n"
                    + "  \"score\": 85,\n"
                    + "  \"suggestions\": [\"...\", \"...\"]\n"
                    + "}\n\n"
                    + "Business Context:\n"
                    + sourceContext + "\n"
                    + "Generated Test Cases:\n"
                    + nullToEmpty(resultText);
        }

        return "你是一名专业的软件测试用例评审专家。请根据给定的业务上下文和生成出的测试用例，对测试用例质量进行评估。\n\n"
                + "请从以下维度进行评估，并给出 JSON 格式结果：\n"
                + "1. normalPathCoverage：正常路径覆盖情况\n"
                + "2. boundaryCoverage：边界条件覆盖情况\n"
                + "3. exceptionCoverage：异常分支覆盖情况\n"
                + "4. syntaxNorm：语法与结构规范性\n"
                + "5. score：综合评分，0-100 的整数\n"
                + "6. suggestions：改进建议，返回字符串数组\n\n"
                + "输出要求：\n"
                + "1. 只能返回 JSON，不要写解释。\n"
                + "2. JSON 结构必须如下：\n"
                + "{\n"
                + "  \"normalPathCoverage\": \"...\",\n"
                + "  \"boundaryCoverage\": \"...\",\n"
                + "  \"exceptionCoverage\": \"...\",\n"
                + "  \"syntaxNorm\": \"...\",\n"
                + "  \"score\": 85,\n"
                + "  \"suggestions\": [\"...\", \"...\"]\n"
                + "}\n\n"
                + "业务上下文：\n"
                + sourceContext + "\n"
                + "生成出的测试用例：\n"
                + nullToEmpty(resultText);
    }

    private String callReasonerForEvaluation(GenerateRequest request, String prompt) {
        ModelRuntimeConfig runtimeConfig = getEvaluationRuntimeConfig(request);

        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", runtimeConfig.getModelName());
            requestBody.put("temperature", 0.2);
            requestBody.put("max_tokens", expandRequestMaxTokens(runtimeConfig.getMaxTokens()));

            ObjectNode responseFormat = requestBody.putObject("response_format");
            responseFormat.put("type", "json_object");

            ArrayNode messages = requestBody.putArray("messages");
            ObjectNode userMessage = objectMapper.createObjectNode();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(userMessage);

            HttpRequest requestMessage = HttpRequest.newBuilder()
                    .uri(URI.create(runtimeConfig.getApiUrl()))
                    .timeout(Duration.ofSeconds(EVALUATION_MODEL_TIMEOUT_SECONDS))
                    .header("Content-Type", "application/json")
                    .header("Authorization", buildAuthorizationHeader(runtimeConfig.getApiKey()))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString(), StandardCharsets.UTF_8))
                    .build();

            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(requestMessage, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("评估模型调用失败：HTTP " + response.statusCode() + " - " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new RuntimeException("评估模型返回内容异常：" + response.body());
            }

            JsonNode message = choices.get(0).path("message");
            String content = extractEvaluationContent(message);
            if (content.isEmpty()) {
                throw new RuntimeException("评估模型未返回有效内容：" + response.body());
            }
            return content;
        } catch (Exception e) {
            throw new RuntimeException("评估模型调用异常：" + e.getMessage(), e);
        }
    }

    private String extractEvaluationContent(JsonNode message) {
        String content = message.path("content").asText("").trim();
        if (!content.isEmpty()) {
            return content;
        }

        String reasoningContent = message.path("reasoning_content").asText("").trim();
        if (!reasoningContent.isEmpty()) {
            String jsonText = extractJsonObject(reasoningContent);
            if (jsonText.startsWith("{") && jsonText.endsWith("}")) {
                return jsonText;
            }
            return reasoningContent;
        }

        return "";
    }

    private EvaluationResult parseEvaluationResult(String jsonText) {
        try {
            String cleaned = extractJsonObject(jsonText);
            JsonNode root = objectMapper.readTree(cleaned);

            EvaluationResult result = new EvaluationResult();
            result.setNormalPathCoverage(root.path("normalPathCoverage").asText(""));
            result.setBoundaryCoverage(root.path("boundaryCoverage").asText(""));
            result.setExceptionCoverage(root.path("exceptionCoverage").asText(""));
            result.setSyntaxNorm(root.path("syntaxNorm").asText(""));

            if (!root.path("score").isMissingNode() && !root.path("score").isNull()) {
                result.setScore(root.path("score").asInt());
            }

            List<String> suggestions = new ArrayList<>();
            JsonNode suggestionsNode = root.path("suggestions");
            if (suggestionsNode.isArray()) {
                for (JsonNode node : suggestionsNode) {
                    String value = node.asText("").trim();
                    if (!value.isEmpty()) {
                        suggestions.add(value);
                    }
                }
            }
            result.setSuggestions(suggestions);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("解析评估结果失败：" + e.getMessage(), e);
        }
    }

    private String extractJsonObject(String text) {
        String source = nullToEmpty(text).trim();
        int start = source.indexOf('{');
        int end = source.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return source.substring(start, end + 1);
        }
        return source;
    }

    private GenerateRecord compareWithPublicTests(GenerateRequest request, Integer recordId, String resultText) {
        if (request == null || recordId == null || isRequirementSource(request) || isFailureText(resultText)) {
            return null;
        }

        FunctionInfo functionInfo = null;
        if (request.getFunctionId() != null) {
            try {
                functionInfo = functionInfoService.selectById(request.getFunctionId());
            } catch (Exception ignored) {
            }
        }

        String publicAssertions = defaultIfBlank(
                request.getPublicTestContent(),
                functionInfo == null ? "" : functionInfo.getPublicTestContent()
        ).trim();
        if (publicAssertions.isEmpty()) {
            return null;
        }

        String functionCode = defaultIfBlank(
                request.getCodeText(),
                functionInfo == null ? "" : functionInfo.getCodeText()
        );
        int publicTotalFallback = request.getPublicAssertCount() != null && request.getPublicAssertCount() > 0
                ? request.getPublicAssertCount()
                : countPublicAssertionFallback(publicAssertions);
        int generatedCaseFallback = countGeneratedCaseCount(resultText);

        GenerateRecord compareRecord = new GenerateRecord();
        compareRecord.setId(recordId);
        try {
            String prompt = buildPublicComparePrompt(functionCode, resultText, publicAssertions);
            String rawResult = callReasonerForEvaluation(request, prompt);
            applyPublicCompareResult(compareRecord, rawResult, publicTotalFallback, generatedCaseFallback);
        } catch (Exception e) {
            compareRecord.setPublicCompareResult(buildPublicCompareFailureMessage(e));
            compareRecord.setPublicCoveredCount(0);
            compareRecord.setPublicPartialCount(0);
            compareRecord.setPublicMissingCount(0);
            compareRecord.setPublicExtraResult("");
            compareRecord.setGeneratedCaseCount(0);
            compareRecord.setPublicMatchedCaseCount(0);
            compareRecord.setPublicExtraCaseCount(0);
            compareRecord.setPublicInvalidCaseCount(0);
            compareRecord.setPublicExtraRate(BigDecimal.ZERO);
            compareRecord.setPublicExpandRate(BigDecimal.ZERO);
        }

        try {
            generateRecordMapper.updatePublicCompareResult(compareRecord);
        } catch (Exception e) {
            System.out.println("公开测试基准对比分析结果保存失败：" + e.getMessage());
        }
        return compareRecord;
    }

    private String buildPublicCompareFailureMessage(Exception e) {
        if (isTimeoutException(e)) {
            return "公开测试基准对比分析失败：评估模型请求超时，当前超时时间为 "
                    + EVALUATION_MODEL_TIMEOUT_SECONDS
                    + " 秒。生成记录已保存，不影响测试用例生成结果；请稍后重试或更换评估模型。";
        }
        return "公开测试基准对比分析失败：" + nullToEmpty(e == null ? null : e.getMessage())
                + "。生成记录已保存，不影响测试用例生成结果。";
    }

    private boolean isTimeoutException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof HttpTimeoutException) {
                return true;
            }
            String message = nullToEmpty(current.getMessage()).toLowerCase();
            if (message.contains("request timed out") || message.contains("timeout") || message.contains("timed out")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String buildPublicComparePrompt(String functionCode, String generatedResult, String publicAssertions) {
        return """
                你是一名软件测试分析人员。现在需要比较“大模型生成的测试用例”和“公开数据集提供的测试断言”。

                请完成两个正式分析任务：

                第一部分：公开测试基准覆盖分析
                判断每条公开断言是否被大模型生成结果覆盖，用于说明生成结果是否遗漏 HumanEval / MBPP 等公开测试基准中的测试点。

                第二部分：生成用例增量价值分析
                判断大模型生成的每条测试用例，是否属于公开断言之外的有效新增测试点，用于说明系统是否补充了公开测试基准之外的新测试场景。

                判断规则：
                1. 公开断言覆盖分析：
                   - 如果生成用例包含等价输入、执行逻辑和预期结果，则标记为 covered；
                   - 如果只覆盖相似场景，但输入、边界或预期结果不完整，则标记为 partial；
                   - 如果生成结果没有体现该断言对应测试点，则标记为 missing。
                2. 生成用例增量价值分析：
                   - 如果生成用例与公开断言语义一致，则标记为 public_matched；
                   - 如果生成用例不属于公开断言，但属于有效测试场景，则标记为 extra_valid；
                   - 如果生成用例重复、无意义、无法执行、与函数逻辑无关，则标记为 invalid_or_duplicate。
                3. 新增测试点类型建议：
                   boundary、empty_input、exception、different_length、negative、float、special_structure、normal_extension、other。
                4. 不要求文本完全一致，重点判断测试语义是否一致。
                5. 不要输出推理过程，只输出 JSON。

                函数代码：
                %s

                大模型生成的测试用例：
                %s

                公开断言内容：
                %s

                请严格按照下面 JSON 格式输出：

                {
                  "benchmarkCoverageAnalysis": {
                    "publicTotal": 3,
                    "coveredCount": 3,
                    "partialCount": 0,
                    "missingCount": 0,
                    "benchmarkCoverageRate": 100.00,
                    "benchmarkComprehensiveRate": 100.00,
                    "items": [
                      {
                        "assertion": "公开断言原文",
                        "status": "covered / partial / missing",
                        "reason": "判断原因",
                        "matchedGeneratedCase": "对应生成用例",
                        "suggestedCase": "如果缺失或部分覆盖，给出建议补充的测试用例"
                      }
                    ],
                    "summary": "公开测试基准覆盖分析总结"
                  },
                  "incrementalValueAnalysis": {
                    "generatedCaseCount": 7,
                    "publicMatchedCaseCount": 3,
                    "extraValidCount": 4,
                    "invalidOrDuplicateCount": 0,
                    "extraRate": 57.14,
                    "expandRate": 133.33,
                    "extraItems": [
                      {
                        "generatedCaseId": "TC-004",
                        "scenario": "测试场景说明",
                        "type": "boundary / empty_input / exception / different_length / negative / float / special_structure / normal_extension / other",
                        "reason": "为什么认为它是公开测试基准之外的有效测试点",
                        "value": "该测试点的补充价值"
                      }
                    ],
                    "invalidItems": [
                      {
                        "generatedCaseId": "TC-xxx",
                        "reason": "无效或重复原因"
                      }
                    ],
                    "summary": "生成用例增量价值分析总结"
                  }
                }
                """.formatted(nullToEmpty(functionCode), nullToEmpty(generatedResult), nullToEmpty(publicAssertions));
    }

    private int countPublicAssertionFallback(String publicAssertions) {
        String text = nullToEmpty(publicAssertions).replace("\r", "").trim();
        if (text.isEmpty()) {
            return 0;
        }

        int assertLineCount = 0;
        int nonEmptyLineCount = 0;
        for (String line : text.split("\n")) {
            String normalized = line.trim();
            if (normalized.isEmpty()) {
                continue;
            }
            nonEmptyLineCount++;
            if (normalized.contains("assert") || normalized.contains("断言")) {
                assertLineCount++;
            }
        }
        return assertLineCount > 0 ? assertLineCount : nonEmptyLineCount;
    }

    private void applyPublicCompareResult(GenerateRecord record, String rawResult,
                                          int publicTotalFallback, int generatedCaseFallback) {
        String rawText = nullToEmpty(rawResult).trim();
        try {
            String cleaned = extractJsonObject(stripJsonCodeFence(rawText));
            JsonNode root = objectMapper.readTree(cleaned);
            JsonNode benchmarkNode = resolveBenchmarkCoverageNode(root);
            JsonNode incrementalNode = resolveIncrementalValueNode(root);
            int[] itemCounts = countPublicCompareStatuses(benchmarkNode.path("items"));

            Integer coveredCount = readOptionalInt(benchmarkNode, "coveredCount");
            Integer partialCount = readOptionalInt(benchmarkNode, "partialCount");
            Integer missingCount = readOptionalInt(benchmarkNode, "missingCount");

            record.setPublicCompareResult(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root));
            record.setPublicCoveredCount(coveredCount == null ? itemCounts[0] : coveredCount);
            record.setPublicPartialCount(partialCount == null ? itemCounts[1] : partialCount);
            record.setPublicMissingCount(missingCount == null ? itemCounts[2] : missingCount);
            applyIncrementalValueResult(record, incrementalNode, publicTotalFallback, generatedCaseFallback);
        } catch (Exception e) {
            record.setPublicCompareResult(rawText);
            record.setPublicCoveredCount(0);
            record.setPublicPartialCount(0);
            record.setPublicMissingCount(0);
            record.setPublicExtraResult("");
            record.setGeneratedCaseCount(0);
            record.setPublicMatchedCaseCount(0);
            record.setPublicExtraCaseCount(0);
            record.setPublicInvalidCaseCount(0);
            record.setPublicExtraRate(BigDecimal.ZERO);
            record.setPublicExpandRate(BigDecimal.ZERO);
        }
    }

    private JsonNode resolveBenchmarkCoverageNode(JsonNode root) {
        if (root == null) {
            return objectMapper.createObjectNode();
        }
        JsonNode benchmarkNode = root.path("benchmarkCoverageAnalysis");
        return benchmarkNode != null && benchmarkNode.isObject() ? benchmarkNode : root;
    }

    private JsonNode resolveIncrementalValueNode(JsonNode root) {
        if (root == null) {
            return null;
        }
        JsonNode incrementalNode = root.path("incrementalValueAnalysis");
        if (incrementalNode != null && incrementalNode.isObject()) {
            return incrementalNode;
        }
        if (root.has("generatedCaseCount") || root.has("extraValidCount") || root.has("extraItems")) {
            return root;
        }
        return null;
    }

    private void applyIncrementalValueResult(GenerateRecord record, JsonNode incrementalNode,
                                             int publicTotalFallback, int generatedCaseFallback) throws Exception {
        if (incrementalNode == null || !incrementalNode.isObject()) {
            record.setPublicExtraResult("");
            record.setGeneratedCaseCount(0);
            record.setPublicMatchedCaseCount(0);
            record.setPublicExtraCaseCount(0);
            record.setPublicInvalidCaseCount(0);
            record.setPublicExtraRate(BigDecimal.ZERO);
            record.setPublicExpandRate(BigDecimal.ZERO);
            return;
        }

        Integer generatedCaseCount = readOptionalInt(incrementalNode, "generatedCaseCount");
        Integer publicMatchedCaseCount = readOptionalInt(incrementalNode, "publicMatchedCaseCount");
        Integer extraValidCount = readOptionalInt(incrementalNode, "extraValidCount");
        Integer invalidOrDuplicateCount = readOptionalInt(incrementalNode, "invalidOrDuplicateCount");

        int generated = generatedCaseCount == null ? Math.max(generatedCaseFallback, 0) : Math.max(generatedCaseCount, 0);
        int matched = publicMatchedCaseCount == null ? 0 : Math.max(publicMatchedCaseCount, 0);
        int extra = extraValidCount == null ? countArrayItems(incrementalNode.path("extraItems")) : Math.max(extraValidCount, 0);
        int invalid = invalidOrDuplicateCount == null ? countArrayItems(incrementalNode.path("invalidItems")) : Math.max(invalidOrDuplicateCount, 0);
        int publicTotal = Math.max(
                nullToZero(record.getPublicCoveredCount()) + nullToZero(record.getPublicPartialCount()) + nullToZero(record.getPublicMissingCount()),
                Math.max(publicTotalFallback, 0)
        );

        BigDecimal extraRate = readOptionalBigDecimal(incrementalNode, "extraRate");
        BigDecimal expandRate = readOptionalBigDecimal(incrementalNode, "expandRate");

        record.setPublicExtraResult(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(incrementalNode));
        record.setGeneratedCaseCount(generated);
        record.setPublicMatchedCaseCount(matched);
        record.setPublicExtraCaseCount(extra);
        record.setPublicInvalidCaseCount(invalid);
        record.setPublicExtraRate(normalizeRate(extraRate == null ? calculatePercentRate(extra, generated) : extraRate));
        record.setPublicExpandRate(normalizeRate(expandRate == null ? calculatePercentRate(extra, publicTotal) : expandRate));
    }

    private int[] countPublicCompareStatuses(JsonNode itemsNode) {
        int coveredCount = 0;
        int partialCount = 0;
        int missingCount = 0;
        if (itemsNode != null && itemsNode.isArray()) {
            for (JsonNode item : itemsNode) {
                String status = item.path("status").asText("").trim().toLowerCase();
                if ("covered".equals(status)) {
                    coveredCount++;
                } else if ("partial".equals(status)) {
                    partialCount++;
                } else if ("missing".equals(status)) {
                    missingCount++;
                }
            }
        }
        return new int[]{coveredCount, partialCount, missingCount};
    }

    private int countArrayItems(JsonNode itemsNode) {
        return itemsNode != null && itemsNode.isArray() ? itemsNode.size() : 0;
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private Integer readOptionalInt(JsonNode root, String fieldName) {
        if (root == null || fieldName == null) {
            return null;
        }
        JsonNode value = root.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        if (value.canConvertToInt()) {
            return value.asInt();
        }
        try {
            return Integer.parseInt(value.asText("").trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private BigDecimal readOptionalBigDecimal(JsonNode root, String fieldName) {
        if (root == null || fieldName == null) {
            return null;
        }
        JsonNode value = root.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        try {
            if (value.isNumber()) {
                return value.decimalValue();
            }
            String text = value.asText("").trim();
            return text.isEmpty() ? null : new BigDecimal(text);
        } catch (Exception ignored) {
            return null;
        }
    }

    private BigDecimal calculatePercentRate(int numerator, int denominator) {
        if (denominator <= 0 || numerator <= 0) {
            return BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, BigDecimal.ROUND_HALF_UP);
    }

    private BigDecimal normalizeRate(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        }
        return value.setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    private String stripJsonCodeFence(String text) {
        String source = nullToEmpty(text).trim();
        if (source.startsWith("```")) {
            source = source.replaceFirst("(?is)^```\\s*json\\s*", "");
            source = source.replaceFirst("(?is)^```\\s*", "");
            source = source.replaceFirst("(?is)\\s*```$", "");
        }
        return source.trim();
    }

    private void fillPublicCompareResponse(GenerateResponse response, GenerateRecord record) {
        if (response == null || record == null) {
            return;
        }
        response.setPublicCompareResult(record.getPublicCompareResult());
        response.setPublicCoveredCount(record.getPublicCoveredCount());
        response.setPublicPartialCount(record.getPublicPartialCount());
        response.setPublicMissingCount(record.getPublicMissingCount());
        response.setPublicExtraResult(record.getPublicExtraResult());
        response.setGeneratedCaseCount(record.getGeneratedCaseCount());
        response.setPublicMatchedCaseCount(record.getPublicMatchedCaseCount());
        response.setPublicExtraCaseCount(record.getPublicExtraCaseCount());
        response.setPublicInvalidCaseCount(record.getPublicInvalidCaseCount());
        response.setPublicExtraRate(record.getPublicExtraRate());
        response.setPublicExpandRate(record.getPublicExpandRate());
    }

    private EvaluationResult buildEvaluationFallbackResult(GenerateRequest request) {
        EvaluationResult result = new EvaluationResult();
        if (isEnglishOutput(request)) {
            result.setNormalPathCoverage("Model evaluation failed. Unable to accurately analyze normal path coverage.");
            result.setBoundaryCoverage("Model evaluation failed. Unable to accurately analyze boundary condition coverage.");
            result.setExceptionCoverage("Model evaluation failed. Unable to accurately analyze exception branch coverage.");
            result.setSyntaxNorm("Model evaluation failed. Unable to accurately analyze syntax and structural compliance.");
            result.setScore(60);
            List<String> suggestions = new ArrayList<>();
            suggestions.add("Please check whether the evaluation model interface is available.");
            suggestions.add("Please manually supplement boundary, exception, and null-value test cases.");
            result.setSuggestions(suggestions);
            return result;
        }

        result.setNormalPathCoverage("模型评估失败，无法准确分析正常路径覆盖情况");
        result.setBoundaryCoverage("模型评估失败，无法准确分析边界条件覆盖情况");
        result.setExceptionCoverage("模型评估失败，无法准确分析异常分支覆盖情况");
        result.setSyntaxNorm("模型评估失败，无法准确分析语法规范性");
        result.setScore(60);
        List<String> suggestions = new ArrayList<>();
        suggestions.add("建议检查模型评估接口是否正常可用");
        suggestions.add("建议人工补充边界值、异常分支和空值场景的测试用例");
        result.setSuggestions(suggestions);
        return result;
    }

    private EvaluationResult buildEvaluationFailureResult(GenerateRequest request, String resultText) {
        EvaluationResult result = new EvaluationResult();
        if (isEnglishOutput(request)) {
            result.setNormalPathCoverage("Test case generation failed and no valid coverage was formed.");
            result.setBoundaryCoverage("Test case generation failed and boundary conditions were not covered.");
            result.setExceptionCoverage("Test case generation failed and exception branches were not covered.");
            result.setSyntaxNorm("The generated result is abnormal and no standardized test case was formed.");
            result.setScore(0);
            result.setGeneratedTestCase(resultText);
            List<String> suggestions = new ArrayList<>();
            suggestions.add("Please check whether the model configuration is correct, such as API URL, API Key, model name, and network connection.");
            suggestions.add("Please check whether the input function code or requirement content is complete.");
            result.setSuggestions(suggestions);
            return result;
        }

        result.setNormalPathCoverage("测试用例生成失败，未形成有效覆盖");
        result.setBoundaryCoverage("测试用例生成失败，无法覆盖边界条件");
        result.setExceptionCoverage("测试用例生成失败，无法覆盖异常分支");
        result.setSyntaxNorm("生成结果异常，未形成规范测试用例");
        result.setScore(0);
        result.setGeneratedTestCase(resultText);
        List<String> suggestions = new ArrayList<>();
        suggestions.add("请检查模型配置是否正确，例如 API 地址、Key、模型名称和网络连接");
        suggestions.add("请检查输入的函数代码或需求内容是否完整");
        result.setSuggestions(suggestions);
        return result;
    }

    private EvaluationResult evaluateByPromptAndResult(GenerateRequest request, String sourceContext, String resultText) {
        String evaluationPrompt = buildEvaluationPrompt(request, sourceContext, resultText);
        String jsonText = callReasonerForEvaluation(request, evaluationPrompt);
        EvaluationResult result = parseEvaluationResult(jsonText);
        result.setGeneratedTestCase(nullToEmpty(resultText));
        return result;
    }

    private ModelRuntimeConfig getEvaluationRuntimeConfig(GenerateRequest request) {
        Integer evaluationModelConfigId = request.getEvaluationModelConfigId();
        if (evaluationModelConfigId != null) {
            ModelConfig selectedConfig = modelConfigService.selectVisibleById(
                    evaluationModelConfigId,
                    request.getCurrentUserId(),
                    request.getCurrentUserRole()
            );
            if (selectedConfig != null && "启用".equals(selectedConfig.getStatus())) {
                BigDecimal temperature = selectedConfig.getTemperature() != null
                        ? selectedConfig.getTemperature()
                        : BigDecimal.valueOf(0.2);
                int maxTokens = selectedConfig.getMaxTokens() != null && selectedConfig.getMaxTokens() > 0
                        ? selectedConfig.getMaxTokens()
                        : 2200;
                return new ModelRuntimeConfig(
                        buildFinalApiUrl(selectedConfig.getApiUrl()),
                        sanitizeApiKey(selectedConfig.getApiKey()),
                        sanitizeConfigText(selectedConfig.getModelName()),
                        temperature.doubleValue(),
                        maxTokens
                );
            }
        }

        return new ModelRuntimeConfig(
                buildFinalApiUrl(deepSeekConfig.getBaseUrl()),
                sanitizeApiKey(deepSeekConfig.getApiKey()),
                sanitizeConfigText(deepSeekConfig.getChatModel()),
                0.2,
                2200
        );
    }

    private ModelRuntimeConfig getRuntimeConfig(String strategy, Integer modelConfigId, Integer currentUserId, String currentUserRole) {
        int defaultMaxTokens = "cot".equals(strategy) ? 2400 : 1600;

        if (modelConfigId != null) {
            ModelConfig selectedConfig = modelConfigService.selectVisibleById(modelConfigId, currentUserId, currentUserRole);
            if (selectedConfig != null && "启用".equals(selectedConfig.getStatus())) {
                String finalApiUrl = buildFinalApiUrl(selectedConfig.getApiUrl());
                String finalApiKey = sanitizeApiKey(selectedConfig.getApiKey());
                String finalModelName = sanitizeConfigText(selectedConfig.getModelName());

                BigDecimal temperature = selectedConfig.getTemperature() != null
                        ? selectedConfig.getTemperature()
                        : BigDecimal.valueOf(0.7);
                int finalTemperature = (int) (temperature.doubleValue() * 10);
                int baseMaxTokens = selectedConfig.getMaxTokens() != null && selectedConfig.getMaxTokens() > 0
                        ? selectedConfig.getMaxTokens()
                        : defaultMaxTokens;
                return new ModelRuntimeConfig(finalApiUrl, finalApiKey, finalModelName, finalTemperature / 10.0, baseMaxTokens);
            }
        }

        String defaultModel = sanitizeConfigText(deepSeekConfig.getChatModel());
        return new ModelRuntimeConfig(
                buildFinalApiUrl(deepSeekConfig.getBaseUrl()),
                sanitizeApiKey(deepSeekConfig.getApiKey()),
                defaultModel,
                0.7,
                defaultMaxTokens
        );
    }

    private ModelCallResult callModel(String prompt, ModelRuntimeConfig runtimeConfig) {
        long startTime = System.currentTimeMillis();
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", runtimeConfig.getModelName());
            requestBody.put("temperature", runtimeConfig.getTemperature());
            requestBody.put("max_tokens", expandRequestMaxTokens(runtimeConfig.getMaxTokens()));

            ArrayNode messages = requestBody.putArray("messages");
            ObjectNode userMessage = objectMapper.createObjectNode();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(userMessage);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(runtimeConfig.getApiUrl()))
                    .timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "application/json")
                    .header("Authorization", buildAuthorizationHeader(runtimeConfig.getApiKey()))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString(), StandardCharsets.UTF_8))
                    .build();

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            long latencyMs = System.currentTimeMillis() - startTime;
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                ModelCallResult result = tryBuildModelCallResult(response.body(), "生成失败：HTTP " + response.statusCode() + " - " + response.body(), latencyMs);
                return result;
            }

            JsonNode root = objectMapper.readTree(response.body());
            Integer promptTokens = extractTokenCount(root, "prompt_tokens");
            Integer completionTokens = extractTokenCount(root, "completion_tokens");
            Integer totalTokens = extractTokenCount(root, "total_tokens");
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                return new ModelCallResult("生成失败：模型返回结果为空", promptTokens, completionTokens, totalTokens, latencyMs);
            }

            JsonNode message = choices.get(0).path("message");
            String content = message.path("content").asText("");
            if (content == null || content.trim().isEmpty()) {
                return new ModelCallResult("生成失败：模型未返回有效内容", promptTokens, completionTokens, totalTokens, latencyMs);
            }
            return new ModelCallResult(content, promptTokens, completionTokens, totalTokens, latencyMs);
        } catch (Exception e) {
            long latencyMs = System.currentTimeMillis() - startTime;
            return new ModelCallResult("生成失败：" + e.getMessage(), null, null, null, latencyMs);
        }
    }

    private ModelCallResult tryBuildModelCallResult(String responseBody, String fallbackContent, Long latencyMs) {
        try {
            JsonNode root = objectMapper.readTree(nullToEmpty(responseBody));
            return new ModelCallResult(
                    fallbackContent,
                    extractTokenCount(root, "prompt_tokens"),
                    extractTokenCount(root, "completion_tokens"),
                    extractTokenCount(root, "total_tokens"),
                    latencyMs
            );
        } catch (Exception ignored) {
            return new ModelCallResult(fallbackContent, null, null, null, latencyMs);
        }
    }

    private Integer extractTokenCount(JsonNode root, String fieldName) {
        if (root == null || fieldName == null) {
            return null;
        }

        JsonNode usage = root.path("usage");
        if (usage.isMissingNode() || usage.isNull()) {
            return null;
        }

        JsonNode value = usage.path(fieldName);
        if (value.isMissingNode() || value.isNull() || !value.canConvertToInt()) {
            return null;
        }
        return value.asInt();
    }

    private int expandRequestMaxTokens(int originalMaxTokens) {
        int safeOriginal = Math.max(originalMaxTokens, 1);
        long expanded = (long) safeOriginal * 5;
        return expanded > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) expanded;
    }

    private boolean isFailureText(String text) {
        String content = nullToEmpty(text).trim();
        return content.startsWith("生成失败：") || content.startsWith("请求失败：");
    }

    private String sanitizeConfigText(String value) {
        String cleaned = nullToEmpty(value)
                .replace("\uFEFF", "")
                .replace("\r", "")
                .replace("\n", "")
                .replace("\t", "")
                .trim();

        if (cleaned.length() >= 2) {
            boolean doubleQuoted = cleaned.startsWith("\"") && cleaned.endsWith("\"");
            boolean singleQuoted = cleaned.startsWith("'") && cleaned.endsWith("'");
            if (doubleQuoted || singleQuoted) {
                cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
            }
        }
        return cleaned;
    }

    private String sanitizeApiKey(String apiKey) {
        String cleaned = sanitizeConfigText(apiKey);
        if (cleaned.regionMatches(true, 0, "Bearer ", 0, 7)) {
            cleaned = cleaned.substring(7).trim();
        }
        return cleaned.replace(" ", "");
    }

    private String buildAuthorizationHeader(String apiKey) {
        String cleanedKey = sanitizeApiKey(apiKey);
        if (cleanedKey.isEmpty()) {
            throw new RuntimeException("模型配置异常：API Key 为空或格式不正确");
        }
        return "Bearer " + cleanedKey;
    }

    private String buildFinalApiUrl(String rawApiUrl) {
        String value = sanitizeConfigText(rawApiUrl);
        if (value.isEmpty()) {
            return "https://api.deepseek.com/chat/completions";
        }
        if (value.endsWith("/chat/completions")) {
            return value;
        }
        if (value.endsWith("/")) {
            return value + "chat/completions";
        }
        return value + "/chat/completions";
    }

    private Integer saveGenerateRecord(GenerateRequest request, String prompt, String resultText, String modelName,
                                       Integer evaluationScore, ModelCallResult modelCallResult) {
        GenerateRecord record = new GenerateRecord();

        if (isRequirementSource(request)) {
            record.setFunctionName(request.getRequirementName());
            record.setSourceType("requirement");
            record.setFunctionId(null);
        } else {
            record.setFunctionId(request.getFunctionId());
            record.setFunctionName(request.getFunctionName());
            record.setSourceType("function");
        }

        record.setPromptText(prompt);
        record.setResultText(resultText);
        record.setModelName(modelName);
        if (modelCallResult != null) {
            record.setPromptTokens(modelCallResult.getPromptTokens());
            record.setCompletionTokens(modelCallResult.getCompletionTokens());
            record.setTotalTokens(modelCallResult.getTotalTokens());
            record.setLatencyMs(modelCallResult.getLatencyMs());
        }
        record.setStrategy(request.getStrategy());
        record.setRunIndex(request.getRunIndex());
        record.setPreviousRecordId(request.getPreviousRecordId());
        record.setEvaluationScore(evaluationScore);

        Integer testerCaseCount = request.getTesterCaseCount();
        Integer llmCaseCount = countGeneratedCaseCount(resultText);
        Integer testerEvaluationScore = calculateTesterEvaluationScore(llmCaseCount, testerCaseCount);
        record.setTesterCaseCount(testerCaseCount);
        record.setLlmCaseCount(llmCaseCount);
        record.setTesterEvaluationScore(testerEvaluationScore);

        Integer currentUserId = request.getCurrentUserId();
        String currentUserRole = request.getCurrentUserRole();

        record.setCreatorId(currentUserId);
        record.setCreatorRole(currentUserRole);

        if ("ADMIN".equals(currentUserRole)) {
            record.setManagerId(currentUserId);
        } else if ("USER".equals(currentUserRole)) {
            User currentUser = userMapper.selectById(currentUserId);
            if (currentUser != null) {
                record.setManagerId(currentUser.getManagerId());
            }
        } else {
            record.setManagerId(null);
        }

        generateRecordMapper.insert(record);
        return record.getId();
    }

    private String buildTesterParticipationHint(GenerateRequest request) {
        if (isRequirementSource(request) || request.getTesterCaseCount() == null || request.getTesterCaseCount() <= 0) {
            return "";
        }
        if (isEnglishOutput(request)) {
            return "Human Tester Reference: the tester expects about " + request.getTesterCaseCount()
                    + " test cases for this function. Please treat this as a reference baseline and ensure sufficient coverage.\n";
        }
        return "测试人员参考意见：测试人员认为该函数大约应设计 " + request.getTesterCaseCount()
                + " 条测试用例。请将其作为参考基线，并保证覆盖充分。\n";
    }

    private Integer countGeneratedCaseCount(String resultText) {
        String text = nullToEmpty(resultText).replace("\r", "").trim();
        if (text.isEmpty() || isFailureText(text)) {
            return 0;
        }

        int caseIdCount = 0;
        for (String line : text.split("\n")) {
            String normalized = line.trim();
            if (normalized.matches("(?i)^(用例编号|Test\\s*Case\\s*ID)\\s*[:：].*")) {
                caseIdCount++;
            }
        }
        if (caseIdCount > 0) {
            return caseIdCount;
        }

        int purposeCount = 0;
        for (String line : text.split("\n")) {
            String normalized = line.trim();
            if (normalized.matches("(?i)^(测试目的|Test\\s*Purpose)\\s*[:：].*")) {
                purposeCount++;
            }
        }
        if (purposeCount > 0) {
            return purposeCount;
        }

        return 1;
    }

    private Integer calculateTesterEvaluationScore(Integer llmCaseCount, Integer testerCaseCount) {
        if (testerCaseCount == null || testerCaseCount <= 0 || llmCaseCount == null || llmCaseCount < 0) {
            return null;
        }
        BigDecimal ratio = BigDecimal.valueOf(llmCaseCount)
                .divide(BigDecimal.valueOf(testerCaseCount), 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        int score = ratio.setScale(0, BigDecimal.ROUND_HALF_UP).intValue();
        return Math.max(score, 0);
    }

    private boolean isRequirementSource(GenerateRequest request) {
        return "requirement".equalsIgnoreCase(nullToEmpty(request.getSourceType()).trim());
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static class BatchModelStatCounter {
        private final Integer modelConfigId;
        private final String modelName;
        private final List<Integer> evaluationScores = new ArrayList<>();
        private final List<Integer> testerScores = new ArrayList<>();
        private int totalCount;
        private int successCount;
        private int failedCount;

        public BatchModelStatCounter(Integer modelConfigId, String modelName) {
            this.modelConfigId = modelConfigId;
            this.modelName = modelName;
        }

        public void accept(BatchExperimentItem item) {
            totalCount++;

            if ("鎴愬姛".equals(item.getStatus())) {
                successCount++;
            } else {
                failedCount++;
            }

            if (item.getEvaluationScore() != null) {
                evaluationScores.add(item.getEvaluationScore());
            }
            if (item.getTesterEvaluationScore() != null) {
                testerScores.add(item.getTesterEvaluationScore());
            }
        }

        public Integer getModelConfigId() {
            return modelConfigId;
        }

        public String getModelName() {
            return modelName;
        }

        public List<Integer> getEvaluationScores() {
            return evaluationScores;
        }

        public List<Integer> getTesterScores() {
            return testerScores;
        }

        public int getTotalCount() {
            return totalCount;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getFailedCount() {
            return failedCount;
        }
    }

    private static class ModelRuntimeConfig {
        private final String apiUrl;
        private final String apiKey;
        private final String modelName;
        private final double temperature;
        private final int maxTokens;

        public ModelRuntimeConfig(String apiUrl, String apiKey, String modelName, double temperature, int maxTokens) {
            this.apiUrl = apiUrl;
            this.apiKey = apiKey;
            this.modelName = modelName;
            this.temperature = temperature;
            this.maxTokens = maxTokens;
        }

        public String getApiUrl() {
            return apiUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public String getModelName() {
            return modelName;
        }

        public double getTemperature() {
            return temperature;
        }

        public int getMaxTokens() {
            return maxTokens;
        }
    }

    private record CoverageContext(boolean isFunctionCandidate, String functionCode, String language) {
    }
}

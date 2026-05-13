package com.example.controller;

import com.example.common.Result;
import com.example.entity.BatchExperimentRequest;
import com.example.entity.GenerateRequest;
import com.example.service.GenerateService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/generate")
public class GenerateController {

    @Resource
    private GenerateService generateService;

    @PostMapping("/testcase")
    public Result generateTestCase(@RequestBody GenerateRequest request) {
        generateService.ensureGenerationAllowed();
        return Result.success(generateService.generateTestCase(request));
    }

    @PostMapping("/evaluate")
    public Result evaluateTestCase(@RequestBody GenerateRequest request) {
        return Result.success(generateService.evaluateTestCase(request));
    }

    @PostMapping("/batchExperiment")
    public Result batchExperiment(@RequestBody BatchExperimentRequest request) {
        return Result.success(generateService.startBatchExperimentTask(request));
    }

    @GetMapping("/batchExperiment/current")
    public Result currentBatchExperimentTask() {
        return Result.success(generateService.getCurrentBatchExperimentTask());
    }

    @GetMapping("/batchExperiment/{taskId}")
    public Result batchExperimentTask(@PathVariable String taskId) {
        return Result.success(generateService.getBatchExperimentTask(taskId));
    }

    @PostMapping("/batchExperiment/{taskId}/terminate")
    public Result terminateBatchExperimentTask(@PathVariable String taskId) {
        return Result.success(generateService.terminateBatchExperimentTask(taskId));
    }
}

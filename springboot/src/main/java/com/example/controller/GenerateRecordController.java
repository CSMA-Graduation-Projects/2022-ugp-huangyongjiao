package com.example.controller;

import com.example.common.Result;
import com.example.service.GenerateRecordService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/generateRecord")
public class GenerateRecordController {

    @Resource
    private GenerateRecordService generateRecordService;

    @GetMapping("/selectAll")
    public Result selectAll(@RequestParam(required = false) String functionName,
                            @RequestParam(required = false) String modelName,
                            @RequestParam(required = false) String strategy,
                            @RequestParam Integer currentUserId,
                            @RequestParam String currentUserRole) {
        return Result.success(generateRecordService.selectAll(functionName, modelName, strategy, currentUserId, currentUserRole));
    }

    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        return Result.success(generateRecordService.selectById(id));
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id,
                         @RequestParam Integer currentUserId,
                         @RequestParam String currentUserRole) {
        generateRecordService.deleteById(id, currentUserId, currentUserRole);
        return Result.success();
    }
}
package com.example.controller;

import com.example.common.Result;
import com.example.entity.ModelConfig;
import com.example.service.ModelConfigService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/modelConfig")
public class ModelConfigController {

    @Resource
    private ModelConfigService modelConfigService;

    @GetMapping("/selectAll")
    public Result selectAll(@RequestParam(required = false) String modelName,
                            @RequestParam(required = false) String status,
                            @RequestParam Integer currentUserId,
                            @RequestParam String currentUserRole) {
        return Result.success(modelConfigService.selectAll(modelName, status, currentUserId, currentUserRole));
    }

    @GetMapping("/selectEnabledList")
    public Result selectEnabledList(@RequestParam Integer currentUserId,
                                    @RequestParam String currentUserRole) {
        return Result.success(modelConfigService.selectEnabledList(currentUserId, currentUserRole));
    }

    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        return Result.success(modelConfigService.selectById(id));
    }

    @PostMapping("/add")
    public Result add(@RequestBody ModelConfig modelConfig,
                      @RequestParam Integer currentUserId,
                      @RequestParam String currentUserRole) {
        modelConfigService.add(modelConfig, currentUserId, currentUserRole);
        return Result.success();
    }

    @PutMapping("/update")
    public Result update(@RequestBody ModelConfig modelConfig,
                         @RequestParam Integer currentUserId,
                         @RequestParam String currentUserRole) {
        modelConfigService.updateById(modelConfig, currentUserId, currentUserRole);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id,
                         @RequestParam Integer currentUserId,
                         @RequestParam String currentUserRole) {
        modelConfigService.deleteById(id, currentUserId, currentUserRole);
        return Result.success();
    }
}
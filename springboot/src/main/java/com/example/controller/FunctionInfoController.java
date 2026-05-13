package com.example.controller;

import com.example.common.Result;
import com.example.entity.FunctionInfo;
import com.example.service.FunctionInfoService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/functionInfo")
public class FunctionInfoController {

    @Resource
    private FunctionInfoService functionInfoService;

    @GetMapping("/selectAll")
    public Result selectAll(@RequestParam(required = false) String functionName,
                            @RequestParam(required = false) String className,
                            @RequestParam(required = false) String language,
                            @RequestParam Integer currentUserId,
                            @RequestParam String currentUserRole) {
        return Result.success(functionInfoService.selectAll(functionName, className, language, currentUserId, currentUserRole));
    }

    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        return Result.success(functionInfoService.selectById(id));
    }

    @PostMapping("/add")
    public Result add(@RequestBody FunctionInfo functionInfo,
                      @RequestParam Integer currentUserId,
                      @RequestParam String currentUserRole) {
        functionInfoService.add(functionInfo, currentUserId, currentUserRole);
        return Result.success();
    }

    @PostMapping("/batchAdd")
    public Result batchAdd(@RequestBody List<FunctionInfo> functionInfoList,
                           @RequestParam Integer currentUserId,
                           @RequestParam String currentUserRole) {
        return Result.success(functionInfoService.batchAdd(functionInfoList, currentUserId, currentUserRole));
    }

    @PutMapping("/update")
    public Result update(@RequestBody FunctionInfo functionInfo,
                         @RequestParam Integer currentUserId,
                         @RequestParam String currentUserRole) {
        functionInfoService.updateById(functionInfo, currentUserId, currentUserRole);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id,
                         @RequestParam Integer currentUserId,
                         @RequestParam String currentUserRole) {
        functionInfoService.deleteById(id, currentUserId, currentUserRole);
        return Result.success();
    }
}

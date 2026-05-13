package com.example.controller;

import com.example.common.Result;
import com.example.entity.RequirementSpec;
import com.example.service.RequirementSpecService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/requirementSpec")
public class RequirementSpecController {

    @Resource
    private RequirementSpecService requirementSpecService;

    @GetMapping("/selectAll")
    public Result selectAll(@RequestParam(required = false) String requirementName,
                            @RequestParam(required = false) String moduleName,
                            @RequestParam Integer currentUserId,
                            @RequestParam String currentUserRole) {
        return Result.success(requirementSpecService.selectAll(requirementName, moduleName, currentUserId, currentUserRole));
    }

    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        return Result.success(requirementSpecService.selectById(id));
    }

    @PostMapping("/add")
    public Result add(@RequestBody RequirementSpec requirementSpec,
                      @RequestParam Integer currentUserId,
                      @RequestParam String currentUserRole) {
        requirementSpecService.add(requirementSpec, currentUserId, currentUserRole);
        return Result.success();
    }

    @PutMapping("/update")
    public Result update(@RequestBody RequirementSpec requirementSpec,
                         @RequestParam Integer currentUserId,
                         @RequestParam String currentUserRole) {
        requirementSpecService.updateById(requirementSpec, currentUserId, currentUserRole);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id,
                         @RequestParam Integer currentUserId,
                         @RequestParam String currentUserRole) {
        requirementSpecService.deleteById(id, currentUserId, currentUserRole);
        return Result.success();
    }
}
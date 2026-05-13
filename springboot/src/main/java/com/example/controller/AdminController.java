package com.example.controller;

import com.example.common.Result;
import com.example.entity.Account;
import com.example.entity.Admin;
import com.example.service.AdminService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Resource
    private AdminService adminService;

    @PostMapping("/login")
    public Result login(@RequestBody Account account) {
        return Result.success(adminService.login(account));
    }

    @GetMapping("/selectAll")
    public Result selectAll(@RequestParam(required = false) String username,
                            @RequestParam(required = false) String name,
                            @RequestParam Integer currentUserId,
                            @RequestParam String currentUserRole) {
        return Result.success(adminService.selectAll(username, name, currentUserId, currentUserRole));
    }

    @GetMapping("/selectManagerOptions")
    public Result selectManagerOptions() {
        return Result.success(adminService.selectManagerOptions());
    }

    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        return Result.success(adminService.selectById(id));
    }

    @PostMapping("/add")
    public Result add(@RequestBody Admin admin,
                      @RequestParam Integer currentUserId,
                      @RequestParam String currentUserRole) {
        adminService.add(admin, currentUserId, currentUserRole);
        return Result.success();
    }

    @PutMapping("/update")
    public Result update(@RequestBody Admin admin,
                         @RequestParam Integer currentUserId,
                         @RequestParam String currentUserRole) {
        adminService.updateById(admin, currentUserId, currentUserRole);
        return Result.success();
    }

    @PutMapping("/updatePassword")
    public Result updatePassword(@RequestBody Account account) {
        adminService.updatePassword(account);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id,
                         @RequestParam Integer currentUserId,
                         @RequestParam String currentUserRole) {
        adminService.deleteById(id, currentUserId, currentUserRole);
        return Result.success();
    }
}
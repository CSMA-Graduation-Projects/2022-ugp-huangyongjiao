package com.example.controller;

import com.example.common.Result;
import com.example.entity.Account;
import com.example.entity.User;
import com.example.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @PostMapping("/login")
    public Result login(@RequestBody Account account) {
        return Result.success(userService.login(account));
    }

    @GetMapping("/selectAll")
    public Result selectAll(@RequestParam(required = false) String username,
                            @RequestParam(required = false) String name,
                            @RequestParam Integer currentUserId,
                            @RequestParam String currentUserRole) {
        return Result.success(userService.selectAll(username, name, currentUserId, currentUserRole));
    }

    @GetMapping("/selectByManagerId/{managerId}")
    public Result selectByManagerId(@PathVariable Integer managerId) {
        return Result.success(userService.selectByManagerId(managerId));
    }

    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        return Result.success(userService.selectById(id));
    }

    @PostMapping("/add")
    public Result add(@RequestBody User user,
                      @RequestParam Integer currentUserId,
                      @RequestParam String currentUserRole) {
        userService.add(user, currentUserId, currentUserRole);
        return Result.success();
    }

    @PutMapping("/update")
    public Result update(@RequestBody User user,
                         @RequestParam Integer currentUserId,
                         @RequestParam String currentUserRole) {
        userService.updateById(user, currentUserId, currentUserRole);
        return Result.success();
    }

    @PutMapping("/updatePassword")
    public Result updatePassword(@RequestBody Account account) {
        userService.updatePassword(account);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id,
                         @RequestParam Integer currentUserId,
                         @RequestParam String currentUserRole) {
        userService.deleteById(id, currentUserId, currentUserRole);
        return Result.success();
    }
}
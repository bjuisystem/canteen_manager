package com.javaweb.canteen.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.javaweb.canteen.common.R;
import com.javaweb.canteen.entity.MyUser;
import com.javaweb.canteen.exception.CustomException;
import com.javaweb.canteen.service.MyUserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/user")
public class MyUserController {

    @Autowired
    private MyUserService myUserService;

    /**
     * 获取当前用户信息接口
     */
    @GetMapping("/getInfo")
    public R<MyUser> getInfo(HttpServletRequest request) {
        MyUser user = (MyUser) request.getSession().getAttribute("currUser");
        return R.success(user);
    }


    /**
     * 用户分页接口
     */
    @PreAuthorize("hasRole('manager')")
    @GetMapping("/page")
    public R<Page<MyUser>> getAllUsers(int page, int limit,
                                 @RequestParam(required = false) String name){

        Page<MyUser> pageInfo = new Page<>(page, limit);

        LambdaQueryWrapper<MyUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StrUtil.isNotEmpty(name), MyUser::getName, name)
                .orderByAsc(MyUser::getUserId);

        myUserService.page(pageInfo, queryWrapper);

        return R.success(pageInfo);
    }

    /**
     * 添加用户接口
     */
    @PreAuthorize("hasRole('manager')")
    @PostMapping("/add")
    public R<String> addUser(@RequestBody MyUser myUser){
        if (StrUtil.isEmpty(myUser.getUsername())) {
            throw new CustomException("用户名为空");
        }
        if (StrUtil.isEmpty(myUser.getName())) {
            throw new CustomException("昵称为空");
        }
        if (StrUtil.isEmpty(myUser.getTelephone())) {
            throw new CustomException("电话为空");
        }
        if (StrUtil.isEmpty(myUser.getDepartment())) {
            throw new CustomException("部门为空");
        }
        if (StrUtil.isEmpty(myUser.getWorkStation())) {
            throw new CustomException("工位信息为空");
        }
        if (StrUtil.isEmpty(myUser.getRole())) {
            throw new CustomException("角色为空");
        }
        validateUniqueRole(myUser.getRole(), null);
        myUser.setPassword(DigestUtils.sha256Hex("123456"));
        boolean res = myUserService.save(myUser);
        if (res) {
            return R.success("添加成功");
        }else{
            return R.fail("添加失败");
        }
    }

    /**
     * 批量导入用户（CSV格式）
     * 格式：name,username,sex,telephone,department,workStation,role
     */
    @PreAuthorize("hasRole('manager')")
    @PostMapping("/import")
    public R<String> importUsers(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return R.fail("文件为空");
        }
        int skip = 0;
        List<MyUser> batch = new ArrayList<>();
        boolean hasManager = myUserService.count(new LambdaQueryWrapper<MyUser>().eq(MyUser::getRole, "manager")) > 0;
        boolean hasChef = myUserService.count(new LambdaQueryWrapper<MyUser>().eq(MyUser::getRole, "chef")) > 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                String lower = trimmed.toLowerCase();
                if (lower.contains("username") || trimmed.contains("登录名")) {
                    continue;
                }
                String[] parts = trimmed.split(",");
                if (parts.length < 7) {
                    skip++;
                    continue;
                }
                String name = parts[0].trim();
                String username = parts[1].trim();
                String sex = parts[2].trim();
                String telephone = parts[3].trim();
                String department = parts[4].trim();
                String workStation = parts[5].trim();
                String role = parts[6].trim();

                if (StrUtil.isEmpty(name) || StrUtil.isEmpty(username) || StrUtil.isEmpty(telephone)
                        || StrUtil.isEmpty(department) || StrUtil.isEmpty(workStation) || StrUtil.isEmpty(role)) {
                    skip++;
                    continue;
                }

                LambdaQueryWrapper<MyUser> exists = new LambdaQueryWrapper<>();
                exists.eq(MyUser::getUsername, username);
                if (myUserService.count(exists) > 0) {
                    skip++;
                    continue;
                }

                if ("manager".equals(role)) {
                    if (hasManager) {
                        return R.fail("餐厅经理已存在，无法导入");
                    }
                    hasManager = true;
                }
                if ("chef".equals(role)) {
                    if (hasChef) {
                        return R.fail("厨房主管已存在，无法导入");
                    }
                    hasChef = true;
                }

                MyUser u = new MyUser();
                u.setName(name);
                u.setUsername(username);
                u.setSex(sex);
                u.setTelephone(telephone);
                u.setDepartment(department);
                u.setWorkStation(workStation);
                u.setRole(role);
                u.setPassword(DigestUtils.sha256Hex("123456"));
                batch.add(u);
            }
        } catch (Exception e) {
            return R.fail("导入失败: " + e.getMessage());
        }

        if (!batch.isEmpty()) {
            myUserService.saveBatch(batch);
        }
        return R.success("导入成功 " + batch.size() + " 条，跳过 " + skip + " 条");
    }

    /**
     * 修改用户接口
     */
    @PreAuthorize("hasRole('manager')")
    @PutMapping("/update")
    public R<String> updateUser(@RequestBody MyUser myUser, HttpServletRequest request){
        if (StrUtil.isEmpty(myUser.getUsername())) {
            throw new CustomException("用户名为空");
        }
        if (StrUtil.isEmpty(myUser.getName())) {
            throw new CustomException("昵称为空");
        }
        if (StrUtil.isEmpty(myUser.getTelephone())) {
            throw new CustomException("电话为空");
        }
        if (StrUtil.isEmpty(myUser.getDepartment())) {
            throw new CustomException("部门为空");
        }
        if (StrUtil.isEmpty(myUser.getWorkStation())) {
            throw new CustomException("工位信息为空");
        }
        if (StrUtil.isEmpty(myUser.getRole())) {
            throw new CustomException("角色为空");
        }
        MyUser user = (MyUser) request.getSession().getAttribute("editUser");
        myUser.setUserId(user.getUserId());
        validateUniqueRole(myUser.getRole(), user.getUserId());
        boolean res = myUserService.updateById(myUser);
        if (res) {
            return R.success("修改成功");
        }else{
            return R.fail("修改失败");
        }
    }

    /**
     * 删除用户接口
     */
    @PreAuthorize("hasRole('manager')")
    @DeleteMapping("/delete/{userId}")
    public R<String> deleteUser(@PathVariable Long userId){
        boolean res = false;
        if (userId != null) {
            res = myUserService.removeById(userId);
        }
        if (res) {
            return R.success("删除成功");
        }else{
            return R.fail("删除失败");
        }
    }

    /**
     * 批量删除用户接口
     */
    @PreAuthorize("hasRole('manager')")
    @DeleteMapping("/deleteBatch/{userIds}")
    public R<String> deleteBatchUser(@PathVariable String userIds){
        boolean res = true;
        String[] ids = userIds.split(",");
        for (String id : ids) {
            Long userId = Long.valueOf(id);
            boolean flag = myUserService.removeById(userId);
            res = res && flag;
        }
        if (res) {
            return R.success("批量删除成功");
        }else{
            return R.fail("批量删除失败");
        }
    }

    /**
     * 修改用户密码接口
     */
    @PostMapping("/updatePwd")
    public R<String> updatePsw(@RequestParam String password, HttpServletRequest request){
        MyUser user = (MyUser) request.getSession().getAttribute("currUser");
        String username = user.getUsername();
        LambdaUpdateWrapper<MyUser> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(MyUser::getUsername, username).set(MyUser::getPassword, DigestUtils.sha256Hex(password));
        boolean res = myUserService.update(null, updateWrapper);
        if (res) {
            user.setPassword(DigestUtils.sha256Hex(password));
            request.getSession().setAttribute("currUser", user);
            return R.success("更新成功");
        }else{
            return R.fail("更新失败");
        }
    }

    private void validateUniqueRole(String role, Long excludeUserId) {
        if (!"manager".equals(role) && !"chef".equals(role)) {
            return;
        }
        LambdaQueryWrapper<MyUser> roleWrapper = new LambdaQueryWrapper<>();
        roleWrapper.eq(MyUser::getRole, role);
        if (excludeUserId != null) {
            roleWrapper.ne(MyUser::getUserId, excludeUserId);
        }
        long count = myUserService.count(roleWrapper);
        if (count > 0) {
            throw new CustomException("该角色已存在，请先调整现有人员");
        }
    }
}

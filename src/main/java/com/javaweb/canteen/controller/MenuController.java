package com.javaweb.canteen.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.javaweb.canteen.common.MyTimeUtils;
import com.javaweb.canteen.common.R;
import com.javaweb.canteen.entity.Menu;
import com.javaweb.canteen.entity.Recipe;
import com.javaweb.canteen.exception.CustomException;
import com.javaweb.canteen.service.MenuService;
import com.javaweb.canteen.service.RecipeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

@Slf4j
@RestController
@RequestMapping("/menu")
public class MenuController {

    @Autowired
    private MenuService menuService;

    @Autowired
    private RecipeService recipeService;

    /**
     *  查询某个菜单接口
     */
    @GetMapping("{menuId}")
    public R<Menu> getMenuInfo(@PathVariable Long menuId){
                Menu menu = menuService.getById(menuId);

                if (menu != null && (menu.getDeleted() == null || menu.getDeleted() == 0)) {
            return R.success(menu);
        }else{
            return R.fail("获取菜品信息失败");
        }
    }

    /**
     *  本周菜单分页接口
     */
    @GetMapping("/pageThisWeek")
    public R<Page<Menu>> getWeekMenu(int page, int limit,
                                     @RequestParam(required = false) String name){
        Page<Menu> pageInfo = new Page<>(page, limit);

        LambdaQueryWrapper<Menu> queryWrapper = new LambdaQueryWrapper<>();

        // 获取当前时间的开始与结束
        Date weekOfBeginTime = MyTimeUtils.getWeekOfBeginTime();
        Date weekOfEndTime = MyTimeUtils.getWeekOfEndTime();

        queryWrapper.eq(StringUtils.isNotEmpty(name), Menu::getName, name)
                .between(Menu::getCreateTime, weekOfBeginTime, weekOfEndTime)
                .and(w -> w.eq(Menu::getDeleted, 0).or().isNull(Menu::getDeleted))
                .orderByDesc(Menu::getCreateTime);

        menuService.page(pageInfo, queryWrapper);

        return R.success(pageInfo);
    }

    /**
     *  下周菜单分页接口
     */
    @GetMapping("/pageNextWeek")
    public R<Page<Menu>> getNextWeekMenu(int page, int limit,
                                      @RequestParam(required = false) String name){
        Page<Menu> pageInfo = new Page<>(page, limit);

        LambdaQueryWrapper<Menu> queryWrapper = new LambdaQueryWrapper<>();

        // 获取下一周开始与结束
        Date nextWeekOfBeginTime = MyTimeUtils.getNextWeekOfBeginTime();
        Date nextWeekOfEndTime = MyTimeUtils.getNextWeekOfEndTime();

        queryWrapper.eq(StringUtils.isNotEmpty(name), Menu::getName, name)
                .between(Menu::getCreateTime, nextWeekOfBeginTime, nextWeekOfEndTime)
                .and(w -> w.eq(Menu::getDeleted, 0).or().isNull(Menu::getDeleted))
                .orderByDesc(Menu::getCreateTime);

        menuService.page(pageInfo, queryWrapper);

        return R.success(pageInfo);
    }

    /**
     *  添加菜单接口
     */
    @PreAuthorize("hasRole('manager')")
    @PostMapping("/add/{recipeId}")
    public R<String> add(@PathVariable Long recipeId,
                         @RequestParam(defaultValue = "next") String week){
        Recipe recipe = recipeService.getById(recipeId);
        String name = recipe.getName();

        Date beginTime;
        Date endTime;
        String weekLabel;
        if ("this".equalsIgnoreCase(week)) {
            beginTime = MyTimeUtils.getWeekOfBeginTime();
            endTime = MyTimeUtils.getWeekOfEndTime();
            weekLabel = "本周";
        } else {
            beginTime = MyTimeUtils.getNextWeekOfBeginTime();
            endTime = MyTimeUtils.getNextWeekOfEndTime();
            weekLabel = "下周";
        }

        LambdaQueryWrapper<Menu> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Menu::getName, name).between(Menu::getCreateTime, beginTime, endTime);
        Menu isExist = menuService.getOne(queryWrapper);
        boolean res;
        if (isExist == null){
            Menu menu = new Menu();
            menu.setName(recipe.getName());
            menu.setCategory(recipe.getCategory());
            menu.setPicture(recipe.getPicture());
            menu.setUnit(recipe.getUnit());
            menu.setPrice(recipe.getPrice());
            menu.setCreateTime(beginTime);
            menu.setDeleted(0);
            res = menuService.save(menu);
        }else{
            return R.fail("当前菜品已被添加到" + weekLabel);
        }
        if (res) {
            return R.success("添加" + weekLabel + "菜单成功");
        }else{
            return R.fail("添加" + weekLabel + "菜单失败");
        }
    }

    /**
     *  删除菜单接口
     */
    @PreAuthorize("hasRole('manager')")
    @DeleteMapping("/delete/{menuId}")
    public R<String> delete(@PathVariable Long menuId){
        boolean res = false;
        if (menuId != null) {
            LambdaUpdateWrapper<Menu> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Menu::getMenuId, menuId).set(Menu::getDeleted, 1);
            res = menuService.update(null, updateWrapper);
        }
        if (res) {
            return R.success("退选成功");
        }else{
            return R.fail("退选失败");
        }
    }

    /**
     * 修改菜单接口
     */
    @PreAuthorize("hasRole('manager')")
    @PutMapping("/update")
    public R<String> update(@RequestBody Menu menu, HttpServletRequest request) {
        if (StrUtil.isEmpty(menu.getName())) {
            throw new CustomException("名字为空!");
        }
        if (StrUtil.isEmpty(menu.getCategory())) {
            throw new CustomException("分类为空!");
        }
        if (StrUtil.isEmpty(menu.getUnit())) {
            throw new CustomException("计量单位为空!");
        }
        if (menu.getPrice() == null) {
            throw new CustomException("价格为空!");
        }
        Menu editMenu = (Menu) request.getSession().getAttribute("editMenu");
        if (StrUtil.isEmpty(menu.getPicture())){
            menu.setPicture(editMenu.getPicture());
        }
        Long menuId = editMenu.getMenuId();
        LambdaQueryWrapper<Menu> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Menu::getMenuId, menuId);
        boolean res = menuService.update(menu, lambdaQueryWrapper);
        if (res) {
            return R.success("修改成功！");
        }else{
            return R.fail("修改失败！");
        }
    }


    /**
     *  批量删除菜单接口
     */
    @PreAuthorize("hasRole('manager')")
    @DeleteMapping("/deleteBatch/{menuIds}")
    public R<String> deleteBatch(@PathVariable String menuIds){
        boolean res = true;
        String[] ids = menuIds.split(",");
        for (String id : ids) {
            Long menuId = Long.valueOf(id);
            LambdaUpdateWrapper<Menu> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Menu::getMenuId, menuId).set(Menu::getDeleted, 1);
            boolean flag = menuService.update(null, updateWrapper);
            res = res && flag;
        }
        if (res) {
            return R.success("批量退选成功");
        }else{
            return R.fail("批量退选失败");
        }
    }

    /**
     * 复用到下一周菜单接口
     */
    @PreAuthorize("hasRole('manager')")
    @PostMapping("/multiplex/{menuIds}")
    public R<String> multiplex(@PathVariable String menuIds) {
        StringBuilder sb = new StringBuilder();
        boolean res = true;
        String[] ids = menuIds.split(",");

        for (String id : ids) {
            Long menuId = Long.valueOf(id);
            Menu menu = menuService.getById(menuId);

            if (menu != null) {
                // 判断是否下一周菜单已经存在该菜品
                String name = menu.getName();
                Date nextWeekOfBeginTime = MyTimeUtils.getNextWeekOfBeginTime();
                Date nextWeekOfEndTime = MyTimeUtils.getNextWeekOfEndTime();
                LambdaQueryWrapper<Menu> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(Menu::getName, name).between(Menu::getCreateTime, nextWeekOfBeginTime, nextWeekOfEndTime);
                Menu one = menuService.getOne(queryWrapper);

                // 不存在
                if (one == null) {
                    menu.setCreateTime(nextWeekOfBeginTime);
                    menu.setMenuId(null);
                    res = res && menuService.save(menu);
                }else {
                    res = false;
                    sb.append(menu.getName()).append("、");
                }
            }
        }
        if (res) {
            return R.success("批量复用成功");
        }else {
            return R.fail("批量复用失败，[" + sb.substring(0,sb.length()-1) + "]已存在，无法复用，其余复用成功");
        }
    }
}

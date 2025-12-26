package com.javaweb.canteen.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.javaweb.canteen.common.R;
import com.javaweb.canteen.entity.History;
import com.javaweb.canteen.entity.Menu;
import com.javaweb.canteen.entity.Recipe;
import com.javaweb.canteen.exception.CustomException;
import com.javaweb.canteen.service.HistoryService;
import com.javaweb.canteen.service.MenuService;
import com.javaweb.canteen.service.RecipeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/menu")
public class MenuController {

    @Autowired
    private MenuService menuService;

    @Autowired
    private RecipeService recipeService;

    @Autowired
    private HistoryService historyService;

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
     *  当前菜单分页接口（取最新生成的菜单）
     */
    @GetMapping("/pageCurrent")
    public R<Page<Menu>> getCurrentMenu(int page, int limit,
                                        @RequestParam(required = false) String name){
        Page<Menu> pageInfo = new Page<>(page, limit);
        History latest = historyService.getOne(new LambdaQueryWrapper<History>()
                .orderByDesc(History::getHisId)
                .last("limit 1"));
        if (latest == null || StrUtil.isEmpty(latest.getMenuIds())) {
            pageInfo.setRecords(new ArrayList<>());
            pageInfo.setTotal(0);
            return R.success(pageInfo);
        }
        List<Long> menuIds = parseMenuIds(latest.getMenuIds());
        if (menuIds.isEmpty()) {
            pageInfo.setRecords(new ArrayList<>());
            pageInfo.setTotal(0);
            return R.success(pageInfo);
        }
        LambdaQueryWrapper<Menu> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Menu::getMenuId, menuIds)
                .eq(StringUtils.isNotEmpty(name), Menu::getName, name)
                .and(w -> w.eq(Menu::getDeleted, 0).or().isNull(Menu::getDeleted))
                .orderByDesc(Menu::getCreateTime);

        menuService.page(pageInfo, queryWrapper);

        return R.success(pageInfo);
    }

    /**
     *  生成菜单（从食谱中选取若干菜品）
     */
    @PreAuthorize("hasRole('manager')")
    @PostMapping("/generate")
    public R<String> generate(@RequestBody List<Long> recipeIds){
        if (recipeIds == null || recipeIds.isEmpty()) {
            throw new CustomException("请选择要生成菜单的菜品");
        }
        List<Long> uniqueIds = recipeIds.stream().distinct().collect(Collectors.toList());
        List<Recipe> recipes = recipeService.listByIds(uniqueIds);
        if (recipes.size() != uniqueIds.size()) {
            throw new CustomException("存在无效的菜品ID");
        }
        Date now = new Date();
        List<Long> menuIds = new ArrayList<>();
        for (Recipe recipe : recipes) {
            Menu menu = new Menu();
            menu.setName(recipe.getName());
            menu.setCategory(recipe.getCategory());
            menu.setPicture(recipe.getPicture());
            menu.setUnit(recipe.getUnit());
            menu.setPrice(recipe.getPrice());
            menu.setCreateTime(now);
            menu.setDeleted(0);
            boolean saved = menuService.save(menu);
            if (!saved) {
                return R.fail("生成菜单失败");
            }
            menuIds.add(menu.getMenuId());
        }
        History history = new History();
        history.setTimeRange(DateUtil.formatDateTime(now));
        history.setMenuIds(menuIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        historyService.save(history);
        return R.success("菜单生成成功");
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
    @PostMapping("/reuse/{hisId}")
    public R<String> reuse(@PathVariable Long hisId) {
        History history = historyService.getById(hisId);
        if (history == null || StrUtil.isEmpty(history.getMenuIds())) {
            return R.fail("未找到可复用的历史菜单");
        }
        List<Long> menuIds = parseMenuIds(history.getMenuIds());
        if (menuIds.isEmpty()) {
            return R.fail("历史菜单为空");
        }
        List<Menu> oldMenus = menuService.listByIds(menuIds);
        Date now = new Date();
        List<Long> newMenuIds = new ArrayList<>();
        for (Menu oldMenu : oldMenus) {
            Menu menu = new Menu();
            menu.setName(oldMenu.getName());
            menu.setCategory(oldMenu.getCategory());
            menu.setPicture(oldMenu.getPicture());
            menu.setUnit(oldMenu.getUnit());
            menu.setPrice(oldMenu.getPrice());
            menu.setCreateTime(now);
            menu.setDeleted(0);
            boolean saved = menuService.save(menu);
            if (!saved) {
                return R.fail("复用菜单失败");
            }
            newMenuIds.add(menu.getMenuId());
        }
        History newHistory = new History();
        newHistory.setTimeRange(DateUtil.formatDateTime(now));
        newHistory.setMenuIds(newMenuIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        historyService.save(newHistory);
        return R.success("复用成功，已生成新菜单");
    }

    private List<Long> parseMenuIds(String menuIds) {
        if (StrUtil.isEmpty(menuIds)) {
            return new ArrayList<>();
        }
        String[] parts = menuIds.split(",");
        List<Long> ids = new ArrayList<>();
        for (String part : parts) {
            if (StrUtil.isNotEmpty(part)) {
                ids.add(Long.valueOf(part.trim()));
            }
        }
        return ids;
    }
}

package com.javaweb.canteen.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.javaweb.canteen.common.R;
import com.javaweb.canteen.entity.Menu;
import com.javaweb.canteen.entity.MyUser;
import com.javaweb.canteen.entity.ShopCart;
import com.javaweb.canteen.exception.CustomException;
import com.javaweb.canteen.service.MenuService;
import com.javaweb.canteen.service.ShopCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.math.BigDecimal;

@Slf4j
@RestController
@RequestMapping("/shopCart")
public class ShopCartController {
    @Autowired
    private ShopCartService shopCartService;
    @Autowired
    private MenuService menuService;

    /**
     * 购物车添加接口
     */
    @PostMapping("/add")
    public R<String> saveInfo(@RequestBody ShopCart shopCart, HttpServletRequest request){
        if (shopCart == null) {
            throw new CustomException("购物车基本信息为空,无法加入购物车");
        }
        if (shopCart.getMenuId() != null) {
            Menu menu = menuService.getById(shopCart.getMenuId());
            if (menu == null || (menu.getDeleted() != null && menu.getDeleted() == 1)) {
                return R.fail("菜品不存在或已下架");
            }
        }
        boolean res;
        String name = shopCart.getName();
        MyUser user = (MyUser) request.getSession().getAttribute("currUser");
        Long userId = user.getUserId();
        LambdaQueryWrapper<ShopCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShopCart::getUserId, userId);
        if (shopCart.getMenuId() != null) {
            queryWrapper.eq(ShopCart::getMenuId, shopCart.getMenuId());
        } else {
            queryWrapper.eq(ShopCart::getName, name);
        }
        ShopCart info = shopCartService.getOne(queryWrapper);
        if (info != null) {
            BigDecimal weight = shopCart.getWeight().add(info.getWeight());
            BigDecimal total = info.getPrice().multiply(weight);
            LambdaUpdateWrapper<ShopCart> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(ShopCart::getUserId, userId)
                    .eq(ShopCart::getScId, info.getScId())
                    .set(ShopCart::getWeight, weight)
                    .set(ShopCart::getTotalPrice, total);
            res = shopCartService.update(null, updateWrapper);
        }else{
            shopCart.setUserId(userId);
            shopCart.setTotalPrice(shopCart.getPrice().multiply(shopCart.getWeight()));
            res = shopCartService.save(shopCart);
        }
        if (res) {
            return R.success("加入成功");
        }else{
            return R.fail("加入失败");
        }
    }

    /**
     * 购物车信息获取接口
     */
    @GetMapping("/get")
    public R<List<ShopCart>> getInfo(HttpServletRequest request){
        MyUser currUser = (MyUser) request.getSession().getAttribute("currUser");
        Long userId = currUser.getUserId();
        LambdaQueryWrapper<ShopCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShopCart::getUserId, userId);
        List<ShopCart> list = shopCartService.list(queryWrapper);
        if (list != null) {
            return R.success(list);
        }else {
            return R.fail("获取购物车信息失败");
        }
    }

    /**
     * 购物车删除接口
     */
    @DeleteMapping("/delete/{scId}")
    public R<String> delete(@PathVariable Long scId){
        if (scId == null)  return R.fail("无法获取购物车编号");
        boolean res = shopCartService.removeById(scId);
        if (res) {
            return R.success("删除成功");
        }else{
            return R.fail("删除失败");
        }
    }
    
}

package com.javaweb.canteen.controller;

import com.javaweb.canteen.common.R;
import com.javaweb.canteen.entity.TimeConfig;
import com.javaweb.canteen.service.TimeConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/timeConfig")
public class TimeConfigController {

    @Autowired
    private TimeConfigService timeConfigService;

    /**
     * 获取当前时间配置（所有角色均可查看）
     */
    @GetMapping("/current")
    public R<TimeConfig> getCurrentConfig() {
        TimeConfig config = timeConfigService.getCurrentConfig();
        return R.success(config);
    }

    /**
     * 更新时间配置（仅经理角色可操作）
     */
    @PreAuthorize("hasRole('manager')")
    @PutMapping("/update")
    public R<String> updateConfig(@RequestBody TimeConfig timeConfig) {
        boolean success = timeConfigService.updateConfig(timeConfig);
        if (success) {
            return R.success("时间配置更新成功");
        } else {
            return R.fail("时间配置更新失败");
        }
    }
}
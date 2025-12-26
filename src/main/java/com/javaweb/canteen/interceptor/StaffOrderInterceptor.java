package com.javaweb.canteen.interceptor;

import com.javaweb.canteen.common.R;
import com.javaweb.canteen.common.ResponseUtil;
import com.javaweb.canteen.entity.TimeConfig;
import com.javaweb.canteen.service.TimeConfigService;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * User order time window check.
 */
@Component
public class StaffOrderInterceptor implements HandlerInterceptor {
    private static final DateTimeFormatter TIME_PARSER = DateTimeFormatter.ofPattern("H:mm:ss");
    private static final LocalTime DEFAULT_DEADLINE = LocalTime.of(9, 0, 0);
    private static final LocalTime DEFAULT_MEAL_START = LocalTime.of(11, 30, 0);

    private final TimeConfigService timeConfigService;

    public StaffOrderInterceptor(TimeConfigService timeConfigService) {
        this.timeConfigService = timeConfigService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        TimeConfig config = timeConfigService.getCurrentConfig();
        LocalTime orderDeadline = parseTime(config != null ? config.getOrderDeadline() : null, DEFAULT_DEADLINE);
        LocalTime mealStart = parseTime(config != null ? config.getMealStartTime() : null, DEFAULT_MEAL_START);
        LocalTime now = LocalTime.now();

        boolean allowOrder;
        if (orderDeadline.isBefore(mealStart)) {
            // Allow before deadline (today) or after meal start (next day).
            allowOrder = !now.isAfter(orderDeadline) || !now.isBefore(mealStart);
        } else {
            // Overlapping config: only allow within [mealStart, orderDeadline].
            allowOrder = !now.isBefore(mealStart) && !now.isAfter(orderDeadline);
        }

        if (!allowOrder) {
            ResponseUtil.out(response, R.fail("当前时间不允许点餐"));
            return false;
        }
        return true;
    }

    private LocalTime parseTime(String value, LocalTime fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        String normalized = value.trim();
        if (normalized.length() == 5) {
            normalized = normalized + ":00";
        }
        try {
            return LocalTime.parse(normalized, TIME_PARSER);
        } catch (DateTimeParseException ex) {
            return fallback;
        }
    }
}

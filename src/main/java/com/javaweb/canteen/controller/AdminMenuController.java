package com.javaweb.canteen.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaweb.canteen.entity.MyUser;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/admin")
public class AdminMenuController {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/menu")
    public List<Map<String, Object>> getMenu(HttpServletRequest request) throws IOException {
        MyUser user = (MyUser) request.getSession().getAttribute("currUser");
        String role = user == null ? "" : user.getRole();
        List<Map<String, Object>> menus = loadMenuJson();
        return filterMenus(menus, role);
    }

    private List<Map<String, Object>> loadMenuJson() throws IOException {
        ClassPathResource resource = new ClassPathResource("static/admin/data/menu.json");
        try (InputStream in = resource.getInputStream()) {
            return objectMapper.readValue(in, new TypeReference<List<Map<String, Object>>>() {});
        }
    }

    private List<Map<String, Object>> filterMenus(List<Map<String, Object>> menus, String role) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> item : menus) {
            Map<String, Object> copy = new HashMap<>(item);
            Object childrenObj = copy.get("children");
            if (childrenObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> children = (List<Map<String, Object>>) childrenObj;
                List<Map<String, Object>> filteredChildren = filterMenus(children, role);
                if (!filteredChildren.isEmpty()) {
                    copy.put("children", filteredChildren);
                    result.add(copy);
                }
                continue;
            }

            Object hrefObj = copy.get("href");
            String href = hrefObj == null ? "" : hrefObj.toString();
            if (isAllowed(href, role)) {
                result.add(copy);
            }
        }
        return result;
    }

    private boolean isAllowed(String href, String role) {
        if (href == null || href.isEmpty()) {
            return true;
        }
        if ("/back/toPerson".equals(href)) {
            return true;
        }
        Set<String> roles = allowedRoles(href);
        return roles.isEmpty() ? true : roles.contains(role);
    }

    private Set<String> allowedRoles(String href) {
        switch (href) {
            case "/back/toUser":
            case "/back/toAddMenu":
            case "/back/toMenu":
            case "/back/toNextMenu":
            case "/back/toHistoryMenu":
            case "/back/toTimeConfig":
                return setOf("manager");
            case "/back/toRecipe":
                return setOf("manager", "chef");
            case "/back/toNoMeal":
                return setOf("chef");
            case "/back/toUndelivered":
                return setOf("caterer");
            case "/back/toCompleted":
            case "/back/toSale":
                return setOf("manager", "treasurer");
            default:
                return Collections.emptySet();
        }
    }

    private Set<String> setOf(String... roles) {
        Set<String> set = new HashSet<>();
        Collections.addAll(set, roles);
        return set;
    }
}

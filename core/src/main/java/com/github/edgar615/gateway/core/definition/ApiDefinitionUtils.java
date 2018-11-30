package com.github.edgar615.gateway.core.definition;

import com.github.edgar615.util.base.AntPathMatcher;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Api的工具类.
 *
 * @author Edgar  Date 2017/6/20
 */
class ApiDefinitionUtils {
    private ApiDefinitionUtils() {
        throw new AssertionError("Not instantiable: " + ApiDefinitionUtils.class);
    }

    /**
     * 按照 相等>正则>ant的优先级匹配
     *
     * @param apiDefinitions
     * @return ApiDefinition的列表
     */
    @Deprecated
    public static List<ApiDefinition> extractInOrder(List<ApiDefinition> apiDefinitions) {
        if (apiDefinitions.size() <= 1) {//只有一个
            return apiDefinitions;
        }
        //先判断相等
        List<ApiDefinition> apiList = apiDefinitions.stream()
                .filter(d -> !d.antStyle() && !d.regexStyle())
                .collect(Collectors.toList());
        if (!apiList.isEmpty()) {
            return apiList;
        }
        //判断正则
        //优先选择正则匹配的API
        List<ApiDefinition> regexApiList = apiDefinitions.stream()
                .filter(d -> d.regexStyle())
                .collect(Collectors.toList());
        if (!regexApiList.isEmpty()) {
            return regexApiList;
        }
        List<ApiDefinition> antApiList = apiDefinitions.stream()
                .filter(d -> d.antStyle())
                .collect(Collectors.toList());
        return antApiList;
    }

    static boolean match(ApiDefinition definition, JsonObject filter) {
        for (String key : filter.fieldNames()) {
            boolean match;
            switch (key) {
                case "name":
                    match = match(definition.name(), filter.getString("name"));
                    break;
                case "method":
                    match = match(definition.method().name(), filter.getString("method"));
                    break;
                case "path":
                    match = matchPath(definition, filter.getString("path"));
                    break;
                default:
                    match = true;
                    break;
            }

            if (!match) {
                return false;
            }
        }

        return true;
    }

    static boolean matchPath(ApiDefinition definition, String expected) {
        if (expected.endsWith("/") && expected.length() > 1) {
            expected = expected.substring(0, expected.length() - 1);
        }
        if (definition instanceof AntPathApiDefinition) {
            AntPathMatcher matcher = new AntPathMatcher.Builder().build();
            return !matchIgnore((AntPathApiDefinition) definition, matcher, expected)
                    && matcher.isMatch(definition.path(), expected);
        } else if (definition instanceof RegexPathApiDefinition) {
            RegexPathApiDefinition regexPathApiDefinition = (RegexPathApiDefinition) definition;
            Pattern pattern = regexPathApiDefinition.pattern();
            Matcher matcher = pattern.matcher(expected);
            return matcher.matches();
        } else {
            return definition.path().equals(expected);
        }

    }

    static boolean matchIgnore(AntPathApiDefinition definition, AntPathMatcher matcher,
                               String expected) {
        return definition.ignoredPatterns()
                .stream().anyMatch(p -> matcher.isMatch(p, expected));
    }

    static boolean match(Object actual, Object expected) {
        if (actual == null) {
            return false;
        }
        if ("*".equals(expected)) {
            return true;
        }
        if (actual instanceof String) {
            if (((String) actual).equalsIgnoreCase(expected.toString())) {
                return true;
            }
            if (expected.toString().startsWith("*")) {
                return actual.toString().toLowerCase()
                        .endsWith(expected.toString().substring(1).toLowerCase());
            }
            if (expected.toString().endsWith("*")) {
                return actual.toString().toLowerCase()
                        .startsWith(
                                expected.toString().substring(0, expected.toString().length() - 1)
                                        .toLowerCase());
            }
        }
        return actual.equals(expected);
    }
}

package com.github.edgar615.gateway.core.definition;

import com.google.common.base.Preconditions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.Shareable;

import java.util.List;
import java.util.stream.Collectors;

/**
 * API定义的接口.
 * <p>
 * API名称约定的规范：[业务线].[应用名].[动作].[版本]
 * 将ApiDefinition声明为Shareable，是为了将API存储在LocalMap中，避免频繁转换为String引起的性能损耗。
 *
 * @author Edgar  Date 2016/9/13
 */
public interface ApiDefinition extends Shareable {

    /**
     * 创建完全匹配路径的API
     * @param name API名称
     * @param method 方法
     * @param path 路径
     * @param endpoints 远程服务
     * @return ApiDefinition
     */
    static ApiDefinition create(String name, HttpMethod method, String path,
                                List<Endpoint> endpoints) {
        return new ApiDefinitionImpl(name, method, path, endpoints);
    }

    /**
     * 创建ANT风格路径匹配的API
     * @param name API名称
     * @param method 方法
     * @param path 路径
     * @param endpoints 远程服务
     * @return ApiDefinition
     */
    static ApiDefinition antStyle(String name, HttpMethod method, String path,
                                  List<Endpoint> endpoints) {
        return new AntPathApiDefinition(name, method, path, endpoints);
    }

    /**
     * 创建正则匹配路径的API
     * @param name API名称
     * @param method 方法
     * @param path 路径
     * @param endpoints 远程服务
     * @return ApiDefinition
     */
    static ApiDefinition regexStyle(String name, HttpMethod method, String path,
                                    List<Endpoint> endpoints) {
        return new RegexPathApiDefinition(name, method, path, endpoints);
    }

    /**
     * 从JSON转换为ApiDefinition
     * @param jsonObject ApiDefinition的JSON定义
     * @return ApiDefinition
     */
    static ApiDefinition fromJson(JsonObject jsonObject) {
        return ApiDefinitionDecoder.instance().apply(jsonObject);
    }

    /**
     * API名称,必须唯一，约定按照[业务线].[应用名].[动作].[版本]的格式
     *
     * @return 名称
     */
    String name();

    /**
     * 请求方法 GET | POST | DELETE | PUT.
     *
     * @return 请求方法 GET | POST | DELETE | PUT.
     */
    HttpMethod method();

    /**
     * API路径
     * 示例：/tasks，匹配请求：/tasks.
     * 示例：/tasks/123/abandon，匹配请求/tasks/123/abandon
     *
     * @return API路径
     */
    String path();

    /**
     * 远程请求定义
     *
     * @return 远程请求定义
     */
    List<Endpoint> endpoints();

    /**
     * 插件列表
     *
     * @return 插件列表
     */
    List<ApiPlugin> plugins();

    /**
     * 增加一个插件.同一个名字的插件有且只能有一个，后加入的插件会覆盖掉之前的同名插件
     *
     * @param plugin 插件
     * @return ApiDefinition
     */
    ApiDefinition addPlugin(ApiPlugin plugin);

    /**
     * 删除一个插件.
     *
     * @param name 插件名称
     * @return ApiDefinition
     */
    ApiDefinition removePlugin(String name);

    /**
     * 根据插件名称返回插件
     *
     * @param name 插件名称
     * @return 如果未找到对应的插件，返回null;
     */
    default ApiPlugin plugin(String name) {
        Preconditions.checkNotNull(name, "name cannot be null");
        List<ApiPlugin> apiPlugins =
                plugins().stream()
                        .filter(p -> name.equalsIgnoreCase(p.name()))
                        .collect(Collectors.toList());
        if (apiPlugins.isEmpty()) {
            return null;
        }
        return apiPlugins.get(0);
    }

    /**
     * 是否是ant风格
     *
     * @return
     */
    default boolean antStyle() {
        return this instanceof AntPathApiDefinition;
    }

    /**
     * 是否是regex风格
     *
     * @return
     */
    default boolean regexStyle() {
        return this instanceof RegexPathApiDefinition;
    }

    default JsonObject toJson() {
        return ApiDefinitionEncoder.instance().apply(this);
    }

    default boolean match(JsonObject filter) {
        return ApiDefinitionUtils.match(this, filter);
    }
}

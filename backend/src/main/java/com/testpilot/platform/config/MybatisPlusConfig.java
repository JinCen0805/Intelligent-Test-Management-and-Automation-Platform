package com.testpilot.platform.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置。
 *
 * <p>审计字段（createdAt/updatedAt）与逻辑删除由实体自身的生命周期方法 + {@code @TableLogic}
 * 维护，不再注册 {@code MetaObjectHandler}/{@code GlobalConfig}（它们在没有
 * {@code @TableField(fill=...)} 的实体上是死配置，容易误导）。
 */
@Configuration
@MapperScan(basePackages = "com.testpilot.platform", annotationClass = Mapper.class)
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}

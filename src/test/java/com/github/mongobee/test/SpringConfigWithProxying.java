package com.github.mongobee.test;

import com.github.mongobee.ChangeLogsSupplier;
import com.github.mongobee.SpringContextChangeLogsSupplier;
import com.github.mongobee.SpringContextChangeLogsSupplierTest;
import com.github.mongobee.changeset.ChangeLog;
import com.github.mongobee.changeset.ChangeSet;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@ComponentScan(
    basePackages = "com.github.mongobee.test.changelogs",
    includeFilters = { @ComponentScan.Filter({ ChangeLog.class }) })
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class SpringConfigWithProxying {

  @Bean
  ChangeLogsSupplier changeLogsSupplier() {
    return new SpringContextChangeLogsSupplier();
  }

  @Bean
  ChangeLogsAspect changeLogsAspect() {
    return new ChangeLogsAspect();
  }

  @Aspect
  public static class ChangeLogsAspect {

    @org.aspectj.lang.annotation.Before(
        "execution(public * *(..)) && @annotation(changeSet)")
    public void beforeChangeSet(ChangeSet changeSet) {

    }
  }
}

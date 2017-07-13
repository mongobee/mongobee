package com.github.mongobee.test;

import com.github.mongobee.ChangeLogsSupplier;
import com.github.mongobee.SpringContextChangeLogsSupplier;
import com.github.mongobee.SpringContextChangeLogsSupplierTest;
import com.github.mongobee.changeset.ChangeLog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(
    basePackages = "com.github.mongobee.test.changelogs",
    includeFilters = { @ComponentScan.Filter({ ChangeLog.class }) })
public class SpringConfig {

  @Bean
  ChangeLogsSupplier changeLogsSupplier() {
    return new SpringContextChangeLogsSupplier();
  }
}

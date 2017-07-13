package com.github.mongobee;

import com.github.mongobee.changeset.ChangeLog;
import com.github.mongobee.exception.MongobeeException;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Collection;

/**
 * Provides all beans annotated with {@link ChangeLog} from the Spring context (set with
 * {@link #setApplicationContext(ApplicationContext)}).
 *
 * <p>A possible usage is to have Spring register changelogs as beans by performing a component scan on
 * packages containing ChangeLog classes, using {@link ChangeLog} as a component identifier (e.g., with
 * {@link org.springframework.context.annotation.ComponentScan#includeFilters()}). This allows the scripts to take
 * advantage of dependency injection, be proxied by Spring with additional generic functionality
 * such as common logging, etc.
 */
public class SpringContextChangeLogsSupplier
    implements ChangeLogsSupplier, ApplicationContextAware {

  private ApplicationContext applicationContext;

  @Override
  public Collection<Object> get() throws MongobeeException {
    return applicationContext.getBeansWithAnnotation(ChangeLog.class).values();
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }
}

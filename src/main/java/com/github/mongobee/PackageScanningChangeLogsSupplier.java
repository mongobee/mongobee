package com.github.mongobee;

import com.github.mongobee.changeset.ChangeLog;
import com.github.mongobee.exception.MongobeeException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;

/**
 * A ChangeLogsSupplier that employs a {@link ClassPathScanningCandidateComponentProvider}
 * to find change logs for a given package name.
 *
 * <p>This supplier will filter out any class annotated with {@link org.springframework.context.annotation.Profile}
 * where none of the specified profiles are active in the Spring environment.
 */
public class PackageScanningChangeLogsSupplier implements ChangeLogsSupplier {

  private final String changeLogsScanPackage;
  private final Environment environment;

  /**
   * Constructor that allows the package scanner to use a new {@link org.springframework.core.env.StandardEnvironment}
   * as the Spring environment
   */
  public PackageScanningChangeLogsSupplier(String changeLogsScanPackage) {
    this(changeLogsScanPackage, null);
  }

  public PackageScanningChangeLogsSupplier(String changeLogsScanPackage, Environment environment) {
    this.changeLogsScanPackage = changeLogsScanPackage;
    this.environment = environment;
  }

  @Override
  public Collection<Object> get() throws MongobeeException {
    ClassPathScanningCandidateComponentProvider classProvider =
        new ClassPathScanningCandidateComponentProvider(false);
    if (environment != null) {
      classProvider.setEnvironment(environment);
    }
    classProvider.addIncludeFilter(new AnnotationTypeFilter(ChangeLog.class));
    Set<BeanDefinition> beanDefinitions = classProvider.findCandidateComponents(changeLogsScanPackage);

    Collection<Object> changeLogObjs = new LinkedList<>();
    Class<?> cls = null;
    String beanClassName = null;
    try {
      for (BeanDefinition beanDefinition : beanDefinitions) {
        beanClassName = beanDefinition.getBeanClassName();
        cls = Class.forName(beanClassName);
        Object o = cls.getConstructor().newInstance();
        changeLogObjs.add(o);
      }
    } catch (ClassNotFoundException e) {
      throw new MongobeeException(
          String.format("failed to load class for name: %s", beanClassName), e);
    } catch (NoSuchMethodException e) {
      throw new MongobeeException(
          String.format("ChangeLog class %s needs a no-args constructor", cls.getName()), e);
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw new MongobeeException(String.format("could not instantiate class %s", cls.getName()), e);
    }

    return changeLogObjs;
  }
}

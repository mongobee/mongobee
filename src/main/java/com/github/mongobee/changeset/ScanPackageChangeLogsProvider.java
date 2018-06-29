package com.github.mongobee.changeset;

import org.reflections.Reflections;

import java.util.Set;

public class ScanPackageChangeLogsProvider implements ChangeLogsProvider {
  private final String changeLogsBasePackage;

  public ScanPackageChangeLogsProvider(String changeLogsBasePackage) {
    this.changeLogsBasePackage = changeLogsBasePackage;
  }

  @Override
  public Set<Class<?>> load() {
    Reflections reflections = new Reflections(changeLogsBasePackage);
    return reflections.getTypesAnnotatedWith(ChangeLog.class); // TODO remove dependency, do own method
  }
}

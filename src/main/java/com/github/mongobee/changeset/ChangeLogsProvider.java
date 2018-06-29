package com.github.mongobee.changeset;

import java.util.Set;

public interface ChangeLogsProvider {
  Set<Class<?>> load();
}

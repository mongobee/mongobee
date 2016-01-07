package com.github.mongobee.utils;

import com.github.mongobee.changeset.ChangeEntry;

import java.lang.reflect.Method;
import java.util.List;

public interface ChangeService {
    List<Class<?>> fetchChangeLogs();

    List<Method> fetchChangeSets(Class<?> type);

    boolean isRunAlwaysChangeSet(Method changesetMethod);

    ChangeEntry createChangeEntry(Method changesetMethod);
}

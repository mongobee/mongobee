/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.mongobee.utils;

/**
 * Miscellaneous class utility methods.
 */
public class ClassUtils {

  /**
   * Determine whether the {@link Class} identified by the supplied name is present
   * and can be loaded. Will return {@code false} if either the class or
   * one of its dependencies is not present or cannot be loaded.
   * @param className the name of the class to check
   * @param classLoader the class loader to use
   * (may be {@code null}, which indicates the default class loader)
   * @return whether the specified class is present
   */
  public static boolean isPresent(String className, ClassLoader classLoader) {
    try {
      Class.forName(className);
      return true;
    }
    catch (Throwable ex) {
      // Class or one of its dependencies is not present...
      return false;
    }
  }

}

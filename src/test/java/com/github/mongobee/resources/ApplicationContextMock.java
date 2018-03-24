package com.github.mongobee.resources;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Locale;
import java.util.Map;

/**
 * Created by dim777 on 20.11.2017.
 */
public class ApplicationContextMock implements ApplicationContext {
  @Nullable
  @Override
  public String getId() {
    return null;
  }

  @Override
  public String getApplicationName() {
    return null;
  }

  @Override
  public String getDisplayName() {
    return null;
  }

  @Override
  public long getStartupDate() {
    return 0;
  }

  @Nullable
  @Override
  public ApplicationContext getParent() {
    return null;
  }

  @Override
  public AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException {
    return null;
  }

  @Nullable
  @Override
  public BeanFactory getParentBeanFactory() {
    return null;
  }

  @Override
  public boolean containsLocalBean(String s) {
    return false;
  }

  @Override
  public boolean containsBeanDefinition(String s) {
    return false;
  }

  @Override
  public int getBeanDefinitionCount() {
    return 0;
  }

  @Override
  public String[] getBeanDefinitionNames() {
    return new String[0];
  }

  @Override
  public String[] getBeanNamesForType(ResolvableType resolvableType) {
    return new String[0];
  }

  @Override
  public String[] getBeanNamesForType(@Nullable Class<?> aClass) {
    return new String[0];
  }

  @Override
  public String[] getBeanNamesForType(@Nullable Class<?> aClass, boolean b, boolean b1) {
    return new String[0];
  }

  @Override
  public <T> Map<String, T> getBeansOfType(@Nullable Class<T> aClass) throws BeansException {
    return null;
  }

  @Override
  public <T> Map<String, T> getBeansOfType(@Nullable Class<T> aClass, boolean b, boolean b1) throws BeansException {
    return null;
  }

  @Override
  public String[] getBeanNamesForAnnotation(Class<? extends Annotation> aClass) {
    return new String[0];
  }

  @Override
  public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> aClass) throws BeansException {
    return null;
  }

  @Nullable
  @Override
  public <A extends Annotation> A findAnnotationOnBean(String s, Class<A> aClass) throws NoSuchBeanDefinitionException {
    return null;
  }

  @Override
  public Object getBean(String s) throws BeansException {
    return null;
  }

  @Override
  public <T> T getBean(String s, @Nullable Class<T> aClass) throws BeansException {
    return null;
  }

  @Override
  public Object getBean(String s, Object... objects) throws BeansException {
    return null;
  }

  @Override
  public <T> T getBean(Class<T> aClass) throws BeansException {
    return null;
  }

  @Override
  public <T> T getBean(Class<T> aClass, Object... objects) throws BeansException {
    return null;
  }

  @Override
  public boolean containsBean(String s) {
    return false;
  }

  @Override
  public boolean isSingleton(String s) throws NoSuchBeanDefinitionException {
    return false;
  }

  @Override
  public boolean isPrototype(String s) throws NoSuchBeanDefinitionException {
    return false;
  }

  @Override
  public boolean isTypeMatch(String s, ResolvableType resolvableType) throws NoSuchBeanDefinitionException {
    return false;
  }

  @Override
  public boolean isTypeMatch(String s, @Nullable Class<?> aClass) throws NoSuchBeanDefinitionException {
    return false;
  }

  @Nullable
  @Override
  public Class<?> getType(String s) throws NoSuchBeanDefinitionException {
    return null;
  }

  @Override
  public String[] getAliases(String s) {
    return new String[0];
  }

  @Override
  public void publishEvent(Object o) {

  }

  @Override
  public String getMessage(String s, @Nullable Object[] objects, @Nullable String s1, Locale locale) {
    return null;
  }

  @Override
  public String getMessage(String s, @Nullable Object[] objects, Locale locale) throws NoSuchMessageException {
    return null;
  }

  @Override
  public String getMessage(MessageSourceResolvable messageSourceResolvable, Locale locale) throws NoSuchMessageException {
    return null;
  }

  @Override
  public Environment getEnvironment() {
    return null;
  }

  @Override
  public Resource[] getResources(String s) throws IOException {
    return new Resource[0];
  }

  @Override
  public Resource getResource(String s) {
    return null;
  }

  @Nullable
  @Override
  public ClassLoader getClassLoader() {
    return null;
  }
}

/**
 *    Copyright 2009-2017 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.executor.loader;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.ibatis.executor.ExecutorException;

import org.apache.ibatis.reflection.ExceptionUtil;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.property.PropertyCopier;
import org.apache.ibatis.reflection.property.PropertyNamer;

/**
 * @author Clinton Begin
 */
// Enhanced:提高  Deserialization：反序列化
  // 就是只要对目标对象调用反序列化操作，就会触发这个代理对象承接操作。操作中完成懒加载
  // 因为从数据库中取出的结果是序列化的，只要使用就好反序列化
public abstract class AbstractEnhancedDeserializationProxy {

  // 对象给GC回收前的处置方法
  protected static final String FINALIZE_METHOD = "finalize";
  // 序列化时的替换方法
  protected static final String WRITE_REPLACE_METHOD = "writeReplace";
  // 被代理类
  private final Class<?> type;
  // 所有没有加载的属性
  private final Map<String, ResultLoaderMap.LoadPair> unloadedProperties;
  // 对象工厂，创建对象时使用
  private final ObjectFactory objectFactory;
  // 构造函数的属性类型列表，创建对象时使用
  private final List<Class<?>> constructorArgTypes;
  // 构造函数的属性列表，创建对象时使用
  private final List<Object> constructorArgs;
  // 标志正在载入中的锁，防止属性并发载入
  private final Object reloadingPropertyLock;
  // 标志已经有属性正在载入
  private boolean reloadingProperty;

  protected AbstractEnhancedDeserializationProxy(Class<?> type, Map<String, ResultLoaderMap.LoadPair> unloadedProperties,
          ObjectFactory objectFactory, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
    this.type = type;
    this.unloadedProperties = unloadedProperties;
    this.objectFactory = objectFactory;
    this.constructorArgTypes = constructorArgTypes;
    this.constructorArgs = constructorArgs;
    this.reloadingPropertyLock = new Object();
    this.reloadingProperty = false;
  }

  public final Object invoke(Object enhanced, Method method, Object[] args) throws Throwable {
    final String methodName = method.getName();
    try {
      if (WRITE_REPLACE_METHOD.equals(methodName)) {
        // 用户对对象调用了序列化方法
        final Object original;
        // 调用构造方法生成一个原对象
        if (constructorArgTypes.isEmpty()) {
          original = objectFactory.create(type);
        } else {
          original = objectFactory.create(type, constructorArgTypes, constructorArgs);
        }
        // 把代理对象的属性都拷贝给原对象
        PropertyCopier.copyBeanProperties(type, enhanced, original);
        return this.newSerialStateHolder(original, unloadedProperties, objectFactory, constructorArgTypes, constructorArgs);
      } else {
        // 防止并发载入
        synchronized (this.reloadingPropertyLock) {
          // 确定是对属性的操作方法，并且不是finalize方法、没有属性正在载入中
          if (!FINALIZE_METHOD.equals(methodName) && PropertyNamer.isProperty(methodName) && !reloadingProperty) {
            // 找到该方法操作的属性
            final String property = PropertyNamer.methodToProperty(methodName);
            // 该属性对应的键值
            final String propertyKey = property.toUpperCase(Locale.ENGLISH);
            if (unloadedProperties.containsKey(propertyKey)) {
              // 取出该属性对应键值的LoadPair
              final ResultLoaderMap.LoadPair loadPair = unloadedProperties.remove(propertyKey);
              if (loadPair != null) {
                try {
                  // 开始属性载入操作
                  reloadingProperty = true;
                  loadPair.load(enhanced);
                } finally {
                  reloadingProperty = false;
                }
              } else {
                /* I'm not sure if this case can really happen or is just in tests -
                 * we have an unread property but no loadPair to load it. */
                throw new ExecutorException("An attempt has been made to read a not loaded lazy property '"
                        + property + "' of a disconnected object");
              }
            }
          }

          return enhanced;
        }
      }
    } catch (Throwable t) {
      throw ExceptionUtil.unwrapThrowable(t);
    }
  }

  protected abstract AbstractSerialStateHolder newSerialStateHolder(
          Object userBean,
          Map<String, ResultLoaderMap.LoadPair> unloadedProperties,
          ObjectFactory objectFactory,
          List<Class<?>> constructorArgTypes,
          List<Object> constructorArgs);

}

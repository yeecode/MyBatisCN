/**
 *    Copyright 2009-2019 the original author or authors.
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
package org.apache.ibatis.executor.loader.cglib;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import org.apache.ibatis.executor.loader.AbstractEnhancedDeserializationProxy;
import org.apache.ibatis.executor.loader.AbstractSerialStateHolder;
import org.apache.ibatis.executor.loader.ProxyFactory;
import org.apache.ibatis.executor.loader.ResultLoaderMap;
import org.apache.ibatis.executor.loader.WriteReplaceInterface;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.reflection.ExceptionUtil;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.property.PropertyCopier;
import org.apache.ibatis.reflection.property.PropertyNamer;
import org.apache.ibatis.session.Configuration;

/**
 * @author Clinton Begin
 */
public class CglibProxyFactory implements ProxyFactory {

  private static final String FINALIZE_METHOD = "finalize";
  private static final String WRITE_REPLACE_METHOD = "writeReplace";

  public CglibProxyFactory() {
    try {
      Resources.classForName("net.sf.cglib.proxy.Enhancer");
    } catch (Throwable e) {
      throw new IllegalStateException("Cannot enable lazy loading because CGLIB is not available. Add CGLIB to your classpath.", e);
    }
  }

  // 创建一个代理
  @Override
  public Object createProxy(Object target, ResultLoaderMap lazyLoader, Configuration configuration, ObjectFactory objectFactory, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
    return EnhancedResultObjectProxyImpl.createProxy(target, lazyLoader, configuration, objectFactory, constructorArgTypes, constructorArgs);
  }

  // 创建一个反序列化的代理
  public Object createDeserializationProxy(Object target, Map<String, ResultLoaderMap.LoadPair> unloadedProperties, ObjectFactory objectFactory, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
    return EnhancedDeserializationProxyImpl.createProxy(target, unloadedProperties, objectFactory, constructorArgTypes, constructorArgs);
  }

  /**
   * 创建代理对象
   * @param type 被代理对象类型
   * @param callback 回调对象
   * @param constructorArgTypes 构造方法参数类型列表
   * @param constructorArgs 构造方法参数类型
   * @return 代理对象
   */
  static Object crateProxy(Class<?> type, Callback callback, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
    Enhancer enhancer = new Enhancer();
    enhancer.setCallback(callback);
    // 创建的是代理对象是原对象的子类
    enhancer.setSuperclass(type);
    try {
      // 获取类中的writeReplace方法
      type.getDeclaredMethod(WRITE_REPLACE_METHOD);
      if (LogHolder.log.isDebugEnabled()) {
        LogHolder.log.debug(WRITE_REPLACE_METHOD + " method was found on bean " + type + ", make sure it returns this");
      }
    } catch (NoSuchMethodException e) {
      // 如果没找到writeReplace方法，则设置代理类继承WriteReplaceInterface接口，该接口中有writeReplace方法
      enhancer.setInterfaces(new Class[]{WriteReplaceInterface.class});
    } catch (SecurityException e) {
      // 什么都不做
    }
    Object enhanced;
    if (constructorArgTypes.isEmpty()) {
      enhanced = enhancer.create();
    } else {
      Class<?>[] typesArray = constructorArgTypes.toArray(new Class[constructorArgTypes.size()]);
      Object[] valuesArray = constructorArgs.toArray(new Object[constructorArgs.size()]);
      enhanced = enhancer.create(typesArray, valuesArray);
    }
    return enhanced;
  }

  private static class EnhancedResultObjectProxyImpl implements MethodInterceptor {

    // 被代理类
    private final Class<?> type;
    // 要懒加载的属性Map
    private final ResultLoaderMap lazyLoader;
    // 是否是激进懒加载
    private final boolean aggressive;
    // 能够触发懒加载的方法名“equals”, “clone”, “hashCode”, “toString”。这四个方法名在Configuration中被初始化。
    private final Set<String> lazyLoadTriggerMethods;
    // 对象工厂
    private final ObjectFactory objectFactory;
    // 被代理类构造函数的参数类型列表
    private final List<Class<?>> constructorArgTypes;
    // 被代理类构造函数的参数列表
    private final List<Object> constructorArgs;

    private EnhancedResultObjectProxyImpl(Class<?> type, ResultLoaderMap lazyLoader, Configuration configuration, ObjectFactory objectFactory, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
      this.type = type;
      this.lazyLoader = lazyLoader;
      this.aggressive = configuration.isAggressiveLazyLoading();
      this.lazyLoadTriggerMethods = configuration.getLazyLoadTriggerMethods();
      this.objectFactory = objectFactory;
      this.constructorArgTypes = constructorArgTypes;
      this.constructorArgs = constructorArgs;
    }

    public static Object createProxy(Object target, ResultLoaderMap lazyLoader, Configuration configuration, ObjectFactory objectFactory, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
      final Class<?> type = target.getClass();
      EnhancedResultObjectProxyImpl callback = new EnhancedResultObjectProxyImpl(type, lazyLoader, configuration, objectFactory, constructorArgTypes, constructorArgs);
      Object enhanced = crateProxy(type, callback, constructorArgTypes, constructorArgs);
      PropertyCopier.copyBeanProperties(type, target, enhanced);
      return enhanced;
    }

    /**
     * 代理类的拦截方法
     * @param enhanced 代理对象本身
     * @param method 被调用的方法
     * @param args 每调用的方法的参数
     * @param methodProxy 用来调用父类的代理
     * @return 方法返回值
     * @throws Throwable
     */
    @Override
    public Object intercept(Object enhanced, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
      // 取出被代理类中此次被调用的方法的名称
      final String methodName = method.getName();
      try {
        synchronized (lazyLoader) { // 防止属性的并发加载
          if (WRITE_REPLACE_METHOD.equals(methodName)) { // 被调用的是writeReplace方法
            // 创建一个原始对象
            Object original;
            if (constructorArgTypes.isEmpty()) {
              original = objectFactory.create(type);
            } else {
              original = objectFactory.create(type, constructorArgTypes, constructorArgs);
            }
            // 将被代理对象的属性拷贝进入新创建的对象
            PropertyCopier.copyBeanProperties(type, enhanced, original);
            if (lazyLoader.size() > 0) { // 存在懒加载属性
              // 则此时返回的信息要更多，不仅仅是原对象，还有相关的懒加载的设置等信息。因此使用CglibSerialStateHolder进行一次封装
              return new CglibSerialStateHolder(original, lazyLoader.getProperties(), objectFactory, constructorArgTypes, constructorArgs);
            } else {
              // 没有未懒加载的属性了，那直接返回原对象进行序列化
              return original;
            }
          } else {
            if (lazyLoader.size() > 0 && !FINALIZE_METHOD.equals(methodName)) { // 存在懒加载属性且被调用的不是finalize方法
              if (aggressive || lazyLoadTriggerMethods.contains(methodName)) { // 设置了激进懒加载或者被调用的方法是能够触发全局懒加载的方法
                // 完成所有属性的懒加载
                lazyLoader.loadAll();
              } else if (PropertyNamer.isSetter(methodName)) { // 调用了属性写方法
                // 则先清除该属性的懒加载设置。该属性不需要被懒加载了
                final String property = PropertyNamer.methodToProperty(methodName);
                lazyLoader.remove(property);
              } else if (PropertyNamer.isGetter(methodName)) { // 调用了属性读方法
                final String property = PropertyNamer.methodToProperty(methodName);
                // 如果该属性是尚未加载的懒加载属性，则进行懒加载
                if (lazyLoader.hasLoader(property)) {
                  lazyLoader.load(property);
                }
              }
            }
          }
        }
        // 触发被代理类的相应方法。能够进行到这里的是除去writeReplace方法外的方法，例如读写方法、toString方法等
        return methodProxy.invokeSuper(enhanced, args);
      } catch (Throwable t) {
        throw ExceptionUtil.unwrapThrowable(t);
      }
    }
  }

  private static class EnhancedDeserializationProxyImpl extends AbstractEnhancedDeserializationProxy implements MethodInterceptor {

    private EnhancedDeserializationProxyImpl(Class<?> type, Map<String, ResultLoaderMap.LoadPair> unloadedProperties, ObjectFactory objectFactory,
            List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
      super(type, unloadedProperties, objectFactory, constructorArgTypes, constructorArgs);
    }

    public static Object createProxy(Object target, Map<String, ResultLoaderMap.LoadPair> unloadedProperties, ObjectFactory objectFactory,
            List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
      final Class<?> type = target.getClass();
      EnhancedDeserializationProxyImpl callback = new EnhancedDeserializationProxyImpl(type, unloadedProperties, objectFactory, constructorArgTypes, constructorArgs);
      Object enhanced = crateProxy(type, callback, constructorArgTypes, constructorArgs);
      PropertyCopier.copyBeanProperties(type, target, enhanced);
      return enhanced;
    }

    @Override
    public Object intercept(Object enhanced, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
      final Object o = super.invoke(enhanced, method, args);
      return o instanceof AbstractSerialStateHolder ? o : methodProxy.invokeSuper(o, args);
    }

    @Override
    protected AbstractSerialStateHolder newSerialStateHolder(Object userBean, Map<String, ResultLoaderMap.LoadPair> unloadedProperties, ObjectFactory objectFactory,
            List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
      return new CglibSerialStateHolder(userBean, unloadedProperties, objectFactory, constructorArgTypes, constructorArgs);
    }
  }

  private static class LogHolder {
    private static final Log log = LogFactory.getLog(CglibProxyFactory.class);
  }

}

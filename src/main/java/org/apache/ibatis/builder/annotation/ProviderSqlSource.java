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
package org.apache.ibatis.builder.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import org.apache.ibatis.annotations.Lang;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;

/**
 * @author Clinton Begin
 * @author Kazuki Shimizu
 */

// SqlSource的子类，能够根据*Provider的信息初始化得到
  // 调用入口唯一，在MapperAnnotationBuilder:getSqlSourceFromAnnotations中
public class ProviderSqlSource implements SqlSource {
  // Configuration对象
  private final Configuration configuration;
  // *Provider注解上type属性所指的类
  private final Class<?> providerType;
  // 语言驱动
  private final LanguageDriver languageDriver;
  // 含有注解的接口方法
  private final Method mapperMethod;
  // *Provider注解上method属性所指的方法
  private Method providerMethod;
  // 给定SQL语句的方法对应的参数
  private String[] providerMethodArgumentNames;
  // 给定SQL语句的方法对应的参数类型
  private Class<?>[] providerMethodParameterTypes;
  // ProviderContext对象
  private ProviderContext providerContext;
  // ProviderContext编号
  private Integer providerContextIndex;

  /**
   * @deprecated Please use the {@link #ProviderSqlSource(Configuration, Object, Class, Method)} instead of this.
   */
  @Deprecated
  public ProviderSqlSource(Configuration configuration, Object provider) {
    this(configuration, provider, null, null);
  }

  /**
   * 创建ProviderSqlSource对象
   * @param configuration 全局配置信息
   * @param provider *provider注解
   * @param mapperType Mapper接口
   * @param mapperMethod Mapper接口内含有*provider注解的方法
   */
  public ProviderSqlSource(Configuration configuration, Object provider, Class<?> mapperType, Method mapperMethod) {
    String providerMethodName;
    try {
      this.configuration = configuration;
      this.mapperMethod = mapperMethod;
      Lang lang = mapperMethod == null ? null : mapperMethod.getAnnotation(Lang.class);
      this.languageDriver = configuration.getLanguageDriver(lang == null ? null : lang.value());
      this.providerType = getProviderType(provider, mapperMethod);
      providerMethodName = (String) provider.getClass().getMethod("method").invoke(provider);

      if (providerMethodName.length() == 0 && ProviderMethodResolver.class.isAssignableFrom(this.providerType)) {
        this.providerMethod = ((ProviderMethodResolver) this.providerType.getDeclaredConstructor().newInstance())
            .resolveMethod(new ProviderContext(mapperType, mapperMethod, configuration.getDatabaseId()));
      }
      if (this.providerMethod == null) {
        providerMethodName = providerMethodName.length() == 0 ? "provideSql" : providerMethodName;
        for (Method m : this.providerType.getMethods()) {
          if (providerMethodName.equals(m.getName()) && CharSequence.class.isAssignableFrom(m.getReturnType())) {
            if (this.providerMethod != null) {
              throw new BuilderException("Error creating SqlSource for SqlProvider. Method '"
                  + providerMethodName + "' is found multiple in SqlProvider '" + this.providerType.getName()
                  + "'. Sql provider method can not overload.");
            }
            this.providerMethod = m;
          }
        }
      }
    } catch (BuilderException e) {
      throw e;
    } catch (Exception e) {
      throw new BuilderException("Error creating SqlSource for SqlProvider.  Cause: " + e, e);
    }
    if (this.providerMethod == null) {
      throw new BuilderException("Error creating SqlSource for SqlProvider. Method '"
          + providerMethodName + "' not found in SqlProvider '" + this.providerType.getName() + "'.");
    }
    this.providerMethodArgumentNames = new ParamNameResolver(configuration, this.providerMethod).getNames();
    this.providerMethodParameterTypes = this.providerMethod.getParameterTypes();
    for (int i = 0; i < this.providerMethodParameterTypes.length; i++) {
      Class<?> parameterType = this.providerMethodParameterTypes[i];
      if (parameterType == ProviderContext.class) {
        if (this.providerContext != null) {
          throw new BuilderException("Error creating SqlSource for SqlProvider. ProviderContext found multiple in SqlProvider method ("
              + this.providerType.getName() + "." + providerMethod.getName()
              + "). ProviderContext can not define multiple in SqlProvider method argument.");
        }
        this.providerContext = new ProviderContext(mapperType, mapperMethod, configuration.getDatabaseId());
        this.providerContextIndex = i;
      }
    }
  }

  /**
   * 获取一个BoundSql对象
   * @param parameterObject 参数对象
   * @return BoundSql对象
   */
  public BoundSql getBoundSql(Object parameterObject) {
    // 获取SqlSource对象
    SqlSource sqlSource = createSqlSource(parameterObject);
    // 从SqlSource中获取BoundSql对象
    return sqlSource.getBoundSql(parameterObject);
  }

  /**
   * 获取一个BoundSql对象
   * @param parameterObject 参数对象
   * @return SqlSource对象
   */
  private SqlSource createSqlSource(Object parameterObject) {
    try {
      // SQL字符串信息
      String sql;
      if (parameterObject instanceof Map) { // 参数是Map
        int bindParameterCount = providerMethodParameterTypes.length - (providerContext == null ? 0 : 1);
        if (bindParameterCount == 1 &&
          (providerMethodParameterTypes[Integer.valueOf(0).equals(providerContextIndex) ? 1 : 0].isAssignableFrom(parameterObject.getClass()))) {
          // 调用*Provider注解的type类中的method方法，从而获得SQL字符串
          sql = invokeProviderMethod(extractProviderMethodArguments(parameterObject));
        } else {
          @SuppressWarnings("unchecked")
          Map<String, Object> params = (Map<String, Object>) parameterObject;
          // 调用*Provider注解的type类中的method方法，从而获得SQL字符串
          sql = invokeProviderMethod(extractProviderMethodArguments(params, providerMethodArgumentNames));
        }
      } else if (providerMethodParameterTypes.length == 0) {
        // *Provider注解的type类中的method方法无需入参
        sql = invokeProviderMethod();
      } else if (providerMethodParameterTypes.length == 1) {
        if (providerContext == null) {
          // *Provider注解的type类中的method方法有一个入参
          sql = invokeProviderMethod(parameterObject);
        } else {
          // *Provider注解的type类中的method方法入参为providerContext对象
          sql = invokeProviderMethod(providerContext);
        }
      } else if (providerMethodParameterTypes.length == 2) {
        sql = invokeProviderMethod(extractProviderMethodArguments(parameterObject));
      } else {
        throw new BuilderException("Cannot invoke SqlProvider method '" + providerMethod
          + "' with specify parameter '" + (parameterObject == null ? null : parameterObject.getClass())
          + "' because SqlProvider method arguments for '" + mapperMethod + "' is an invalid combination.");
      }
      Class<?> parameterType = parameterObject == null ? Object.class : parameterObject.getClass();
      // 调用languageDriver生成SqlSource对象
      return languageDriver.createSqlSource(configuration, sql, parameterType);
    } catch (BuilderException e) {
      throw e;
    } catch (Exception e) {
      throw new BuilderException("Error invoking SqlProvider method '" + providerMethod
          + "' with specify parameter '" + (parameterObject == null ? null : parameterObject.getClass()) + "'.  Cause: " + extractRootCause(e), e);
    }
  }

  private Throwable extractRootCause(Exception e) {
    Throwable cause = e;
    while(cause.getCause() != null) {
      cause = e.getCause();
    }
    return cause;
  }

  private Object[] extractProviderMethodArguments(Object parameterObject) {
    if (providerContext != null) {
      Object[] args = new Object[2];
      args[providerContextIndex == 0 ? 1 : 0] = parameterObject;
      args[providerContextIndex] = providerContext;
      return args;
    } else {
      return new Object[] { parameterObject };
    }
  }

  private Object[] extractProviderMethodArguments(Map<String, Object> params, String[] argumentNames) {
    Object[] args = new Object[argumentNames.length];
    for (int i = 0; i < args.length; i++) {
      if (providerContextIndex != null && providerContextIndex == i) {
        args[i] = providerContext;
      } else {
        args[i] = params.get(argumentNames[i]);
      }
    }
    return args;
  }

  private String invokeProviderMethod(Object... args) throws Exception {
    Object targetObject = null;
    if (!Modifier.isStatic(providerMethod.getModifiers())) {
      targetObject = providerType.newInstance();
    }
    CharSequence sql = (CharSequence) providerMethod.invoke(targetObject, args);
    return sql != null ? sql.toString() : null;
  }

  private Class<?> getProviderType(Object providerAnnotation, Method mapperMethod)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Class<?> type = (Class<?>) providerAnnotation.getClass().getMethod("type").invoke(providerAnnotation);
    Class<?> value = (Class<?>) providerAnnotation.getClass().getMethod("value").invoke(providerAnnotation);
    if (value == void.class && type == void.class) {
      throw new BuilderException("Please specify either 'value' or 'type' attribute of @"
          + ((Annotation) providerAnnotation).annotationType().getSimpleName()
          + " at the '" + mapperMethod.toString() + "'.");
    }
    if (value != void.class && type != void.class && value != type) {
      throw new BuilderException("Cannot specify different class on 'value' and 'type' attribute of @"
          + ((Annotation) providerAnnotation).annotationType().getSimpleName()
          + " at the '" + mapperMethod.toString() + "'.");
    }
    return value == void.class ? type : value;
  }

}

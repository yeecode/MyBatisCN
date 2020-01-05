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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.ibatis.builder.BuilderException;

/**
 * The interface that resolve an SQL provider method via an SQL provider class.
 *
 * <p> This interface need to implements at an SQL provider class and
 * it need to define the default constructor for creating a new instance.
 *
 * @since 3.5.1
 * @author Kazuki Shimizu
 */
public interface ProviderMethodResolver {

  /**
   * Resolve an SQL provider method.
   *
   * <p> The default implementation return a method that matches following conditions.
   * <ul>
   *   <li>Method name matches with mapper method</li>
   *   <li>Return type matches the {@link CharSequence}({@link String}, {@link StringBuilder}, etc...)</li>
   * </ul>
   * If matched method is zero or multiple, it throws a {@link BuilderException}.
   *
   * @param context a context for SQL provider
   * @return an SQL provider method
   * @throws BuilderException Throws when cannot resolve a target method
   */

  /**
   * 从@*Provider注解的type属性所指向的类中找出method属性中所指的方法
   * @param context 包含@*Provider注解中的type值和method值
   * @return 找出的指定方法
   */
  default Method resolveMethod(ProviderContext context) {
    // 找出同名方法
    List<Method> sameNameMethods = Arrays.stream(getClass().getMethods())
        .filter(m -> m.getName().equals(context.getMapperMethod().getName()))
        .collect(Collectors.toList());

    // 如果没有找到指定的方法，则@*Provider注解中的type属性所指向的类中不含有method属性中所指的方法。
    if (sameNameMethods.isEmpty()) {
      throw new BuilderException("Cannot resolve the provider method because '"
          + context.getMapperMethod().getName() + "' not found in SqlProvider '" + getClass().getName() + "'.");
    }
    // 根据返回类型再次判断，返回类型必须是CharSequence类或其子类
    List<Method> targetMethods = sameNameMethods.stream()
        .filter(m -> CharSequence.class.isAssignableFrom(m.getReturnType()))
        .collect(Collectors.toList());
    if (targetMethods.size() == 1) {
      // 方法唯一，返回该方法
      return targetMethods.get(0);
    }

    if (targetMethods.isEmpty()) {
      throw new BuilderException("Cannot resolve the provider method because '"
          + context.getMapperMethod().getName() + "' does not return the CharSequence or its subclass in SqlProvider '"
          + getClass().getName() + "'.");
    } else {
      throw new BuilderException("Cannot resolve the provider method because '"
          + context.getMapperMethod().getName() + "' is found multiple in SqlProvider '" + getClass().getName() + "'.");
    }
  }

}

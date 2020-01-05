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
package org.apache.ibatis.plugin;

import java.util.Properties;

/**
 * @author Clinton Begin
 */
// 拦截器    // Invocation：调用
  // 这是插件接口，所有插件需要实现该接口
public interface Interceptor {
  /**
   * 该方法内是拦截器拦截到目标方法时的操作
   * @param invocation 拦截到的目标方法的信息
   * @return 经过拦截器处理后的返回结果
   * @throws Throwable
   */
  Object intercept(Invocation invocation) throws Throwable;

  /**
   * 用返回值替代入参对象。
   * 通常情况下，可以调用Plugin的warp方法来完成，因为warp方法能判断目标对象是否需要拦截，并根据判断结果返回相应的对象来替换目标对象
   * @param target MyBatis传入的支持拦截的几个类（ParameterHandler、ResultSetHandler、StatementHandler、Executor）的实例
   * @return 如果当前拦截器要拦截该实例，则返回该实例的代理；如果不需要拦截该实例，则直接返回该实例本身
   */
  default Object plugin(Object target) {
    return Plugin.wrap(target, this);
  }

  /**
   * 设置拦截器的属性
   * @param properties 要给拦截器设置的属性
   */
  default void setProperties(Properties properties) {
    // NOP
  }

}

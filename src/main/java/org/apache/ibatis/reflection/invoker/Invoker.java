/**
 *    Copyright 2009-2015 the original author or authors.
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
package org.apache.ibatis.reflection.invoker;

import java.lang.reflect.InvocationTargetException;

/**
 * 针对于 Class 中 Field 和 Method 的调用，在 MyBatis 中封装了 Invoker 对象来统一处理(有使用到适配器模式)。
 * @author Clinton Begin
 */
public interface Invoker {
  // 方法执行调用器
  Object invoke(Object target, Object[] args) throws IllegalAccessException, InvocationTargetException;
  // 传入参数或者传出参数的类型（如有一个入参就是入参，否则是出参）
  Class<?> getType();
}

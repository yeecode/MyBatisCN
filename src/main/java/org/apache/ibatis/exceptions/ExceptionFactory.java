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
package org.apache.ibatis.exceptions;

import org.apache.ibatis.executor.ErrorContext;

/**
 * @author Clinton Begin
 */
public class ExceptionFactory {

  // 不允许实例化该类
  private ExceptionFactory() {
    // Prevent Instantiation
  }

  // 静态方法，直接调用

  /**
   * 生成一个RuntimeException异常
   * @param message 异常信息
   * @param e 异常
   * @return 新的RuntimeException异常
   */
  public static RuntimeException wrapException(String message, Exception e) {
    return new PersistenceException(ErrorContext.instance().message(message).cause(e).toString(), e);
  }

}

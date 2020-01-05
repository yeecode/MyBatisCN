/**
 *    Copyright 2009-2018 the original author or authors.
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
package org.apache.ibatis.reflection;

import org.apache.ibatis.io.Resources;

/**
 * To check the existence of version dependent classes.
 * 因为不同JDK提供的包不禁相同，该方法来判断当前的JDK到底是不是存在指定的包。
 * 里面有大量静态代码块，因此会在类的初始化阶段完成相关的判断。
 */
public class Jdk {

  /**
   * <code>true</code> if <code>java.lang.reflect.Parameter</code> is available.
   * @deprecated Since 3.5.0, Will remove this field at feature(next major version up)
   */
  @Deprecated
  public static final boolean parameterExists;

  static {
    boolean available = false;
    try {
      Resources.classForName("java.lang.reflect.Parameter");
      available = true;
    } catch (ClassNotFoundException e) {
      // ignore
    }
    parameterExists = available;
  }

  /**
   * @deprecated Since 3.5.0, Will remove this field at feature(next major version up)
   */
  @Deprecated
  public static final boolean dateAndTimeApiExists;

  static {
    boolean available = false;
    try {
      Resources.classForName("java.time.Clock");
      available = true;
    } catch (ClassNotFoundException e) {
      // ignore
    }
    dateAndTimeApiExists = available;
  }

  /**
   * @deprecated Since 3.5.0, Will remove this field at feature(next major version up)
   */
  @Deprecated
  public static final boolean optionalExists;

  static {
    boolean available = false;
    try {
      Resources.classForName("java.util.Optional");
      available = true;
    } catch (ClassNotFoundException e) {
      // ignore
    }
    optionalExists = available;
  }

  private Jdk() {
    super();
  }
}

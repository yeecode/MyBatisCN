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
package org.apache.ibatis.reflection.factory;

import java.util.List;
import java.util.Properties;

/**
 * ObjectFactory 主要作用是创建 Java 对象实例。
 * 在 MyBatis中，每次查询都会创建一个新的结果对象实例，这就需要 ObjectFactory 类来创建这些实例。ObjectFactory 类通过反射创建 Java 对象实例，同时可以根据需要自定义创建对象的方式。
 *
 * ObjectFactory 接口有两个主要方法：
 * - create(Class type)：用于创建指定类型的对象实例。
 * - setProperties(Properties properties)：用于设置 ObjectFactory 的属性。

 * 除了这两个方法之外，ObjectFactory类还有其他一些方法，例如isCollection()和isMap()等，这些方法可以用于判断对象是否是集合或映射类型。
 * 在MyBatis中，默认使用DefaultObjectFactory类来创建Java对象实例。如果需要自定义创建对象的方式，可以实现ObjectFactory接口，并将自定义的ObjectFactory类配置到MyBatis的配置文件中。
 *
 * MyBatis uses an ObjectFactory to create all needed new Objects.
 * @author Clinton Begin
 * 对象工厂
 */
public interface ObjectFactory {

  /**
   * Sets configuration properties.
   * @param properties configuration properties
   */
  default void setProperties(Properties properties) {
    // NOP
  }

  /**
   * Creates a new object with default constructor.
   * @param type Object type
   * @return
   */
  <T> T create(Class<T> type);

  /**
   * Creates a new object with the specified constructor and params.
   * @param type Object type
   * @param constructorArgTypes Constructor argument types
   * @param constructorArgs Constructor argument values
   * @return
   */
  <T> T create(Class<T> type, List<Class<?>> constructorArgTypes, List<Object> constructorArgs);

  /**
   * Returns true if this object can have a set of other objects.
   * It's main purpose is to support non-java.util.Collection objects like Scala collections.
   *
   * @param type Object type
   * @return whether it is a collection or not
   * @since 3.1.0
   */
  <T> boolean isCollection(Class<T> type);

}

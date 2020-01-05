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
package org.apache.ibatis.reflection.wrapper;

import java.util.List;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.property.PropertyTokenizer;

/**
 * @author Clinton Begin
 * 对象或者对象集合的包装器
 */
public interface ObjectWrapper {

  // get一个属性的值
  Object get(PropertyTokenizer prop);

  // set一个属性的值
  void set(PropertyTokenizer prop, Object value);

  // 找到指定属性
  String findProperty(String name, boolean useCamelCaseMapping);

  // 获得getter列表
  String[] getGetterNames();

  // 获得setter列表
  String[] getSetterNames();

  // 获得getter的类型
  Class<?> getSetterType(String name);

  // 获得setter的类型
  Class<?> getGetterType(String name);

  // 查看指定属性是否有setter
  boolean hasSetter(String name);

  // 查看指定属性是否有getter
  boolean hasGetter(String name);

  // 生成一个属性的实例
  MetaObject instantiatePropertyValue(String name, PropertyTokenizer prop, ObjectFactory objectFactory);

  // 判断是否是集合
  boolean isCollection();

  // 添加元素
  void add(Object element);

  // 添加全部元素
  <E> void addAll(List<E> element);

}

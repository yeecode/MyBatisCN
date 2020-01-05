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
package org.apache.ibatis.executor.result;

import java.util.Map;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.ReflectorFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;

/**
 * @author Clinton Begin
 */
public class DefaultMapResultHandler<K, V> implements ResultHandler<V> {
  // Map形式的映射结果
  private final Map<K, V> mappedResults;
  // Map的键。由用户指定，是结果对象中的某个属性名
  private final String mapKey;
  // 对象工厂
  private final ObjectFactory objectFactory;
  // 对象包装工厂
  private final ObjectWrapperFactory objectWrapperFactory;
  // 反射工厂
  private final ReflectorFactory reflectorFactory;

  @SuppressWarnings("unchecked")
  public DefaultMapResultHandler(String mapKey, ObjectFactory objectFactory, ObjectWrapperFactory objectWrapperFactory, ReflectorFactory reflectorFactory) {
    this.objectFactory = objectFactory;
    this.objectWrapperFactory = objectWrapperFactory;
    this.reflectorFactory = reflectorFactory;
    this.mappedResults = objectFactory.create(Map.class);
    this.mapKey = mapKey;
  }

  /**
   * 处理一个结果
   * @param context 一个结果
   */
  @Override
  public void handleResult(ResultContext<? extends V> context) {
    // 从结果上下文中取出结果对象
    final V value = context.getResultObject();
    // 获得结果对象的元对象
    final MetaObject mo = MetaObject.forObject(value, objectFactory, objectWrapperFactory, reflectorFactory);
    // 基于元对象取出key对应的值
    final K key = (K) mo.getValue(mapKey);
    mappedResults.put(key, value);
  }

  public Map<K, V> getMappedResults() {
    return mappedResults;
  }
}

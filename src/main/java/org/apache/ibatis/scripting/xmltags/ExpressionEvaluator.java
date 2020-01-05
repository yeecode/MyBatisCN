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
package org.apache.ibatis.scripting.xmltags;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.builder.BuilderException;

/**
 * @author Clinton Begin
 *
 * 表达式求值器
 */
public class ExpressionEvaluator {

  /**
   * 对结果为true/false形式的表达式进行求值
   * @param expression 表达式
   * @param parameterObject 参数对象
   * @return 求值结果
   */
  public boolean evaluateBoolean(String expression, Object parameterObject) {
    // 获取表达式的值
    Object value = OgnlCache.getValue(expression, parameterObject);
    if (value instanceof Boolean) { // 如果确实是Boolean形式的结果
      return (Boolean) value;
    }
    if (value instanceof Number) { // 如果是数值形式的结果
      return new BigDecimal(String.valueOf(value)).compareTo(BigDecimal.ZERO) != 0;
    }
    return value != null;
  }

  /**
   * 对结果为迭代形式的表达式进行求值
   * @param expression 表达式
   * @param parameterObject 参数对象
   * @return 求值结果
   */
  public Iterable<?> evaluateIterable(String expression, Object parameterObject) {
    // 获取表达式的结果
    Object value = OgnlCache.getValue(expression, parameterObject);
    if (value == null) {
      throw new BuilderException("The expression '" + expression + "' evaluated to a null value.");
    }
    if (value instanceof Iterable) { // 如果结果是Iterable
      return (Iterable<?>) value;
    }
    if (value.getClass().isArray()) { // 结果是Array
      // 原注释：得到的Array可能是原始的，因此调用Arrays.asList()可能会抛出ClassCastException。因此要手工转为ArrayList
      int size = Array.getLength(value);
      List<Object> answer = new ArrayList<>();
      for (int i = 0; i < size; i++) {
        Object o = Array.get(value, i);
        answer.add(o);
      }
      return answer;
    }
    if (value instanceof Map) { // 结果是Map
      return ((Map) value).entrySet();
    }
    throw new BuilderException("Error evaluating expression '" + expression + "'.  Return value (" + value + ") was not iterable.");
  }

}

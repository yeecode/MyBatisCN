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
package org.apache.ibatis.scripting.xmltags;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ognl.Ognl;
import ognl.OgnlException;

import org.apache.ibatis.builder.BuilderException;

/**
 * Caches OGNL parsed expressions.
 *
 * @author Eduardo Macarron
 *
 * @see <a href='http://code.google.com/p/mybatis/issues/detail?id=342'>Issue 342</a>
 */
public final class OgnlCache {
  // MyBatis提供的OgnlMemberAccess对象
  private static final OgnlMemberAccess MEMBER_ACCESS = new OgnlMemberAccess();
  // MyBatis提供的OgnlClassResolver对象
  private static final OgnlClassResolver CLASS_RESOLVER = new OgnlClassResolver();
  // 缓存解析后的OGNL表达式,用以提高效率
  private static final Map<String, Object> expressionCache = new ConcurrentHashMap<>();

  private OgnlCache() {
    // Prevent Instantiation of Static Class
  }

  /**
   * 读取表达式的结果
   * @param expression 表达式
   * @param root 根环境
   * @return 表达式结果
   */
  public static Object getValue(String expression, Object root) {
    try {
      // 创建默认的上下文环境
      Map context = Ognl.createDefaultContext(root, MEMBER_ACCESS, CLASS_RESOLVER, null);
      // 依次传入表达式树、上下文、根，从而获得表达式的结果
      return Ognl.getValue(parseExpression(expression), context, root);
    } catch (OgnlException e) {
      throw new BuilderException("Error evaluating expression '" + expression + "'. Cause: " + e, e);
    }
  }

  /**
   * 解析表达式，得到解析后的表达式树
   * @param expression 表达式
   * @return 表达式树
   * @throws OgnlException
   */
  private static Object parseExpression(String expression) throws OgnlException {
    // 先从缓存中获取
    Object node = expressionCache.get(expression);
    if (node == null) {
      // 缓存没有则直接解析，并放入缓存
      node = Ognl.parseExpression(expression);
      expressionCache.put(expression, node);
    }
    return node;
  }

}

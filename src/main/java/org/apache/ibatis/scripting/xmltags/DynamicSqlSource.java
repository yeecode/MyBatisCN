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

import org.apache.ibatis.builder.SqlSourceBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.session.Configuration;

/**
 * @author Clinton Begin
 *
 * SqlSource的重要实现，用以解析动态SQL语句。
 */
public class DynamicSqlSource implements SqlSource {

  private final Configuration configuration;

  // 整个sqlSource的根节点。
  // 举例子，例如： SELECT *
  //        FROM `user`
  //        WHERE id IN
  //        <foreach item="id" collection="array" open="(" separator="," close=")">
  //            #{id}
  //        </foreach>

  // 那根节点是一个MixedSqlNode，其List分别是：
  // StaticTextSqlNode: SELECT *  FROM `user` WHERE id IN
  // ForEachSqlNode：拆解后的foreachSqlNode信息
  private final SqlNode rootSqlNode;

  public DynamicSqlSource(Configuration configuration, SqlNode rootSqlNode) {
    this.configuration = configuration;
    this.rootSqlNode = rootSqlNode;
  }

  // 关键方法，获取到sql语句中只含有？，并且整理好参数的boundSql

  /**
   * 获取一个BoundSql对象
   * @param parameterObject 参数对象
   * @return BoundSql对象
   */
  @Override
  public BoundSql getBoundSql(Object parameterObject) {
    // 创建DynamicSqlSource的辅助类，用来记录DynamicSqlSource解析出来的
    // * SQL片段信息
    // * 参数信息
    DynamicContext context = new DynamicContext(configuration, parameterObject);
    // 这里会逐层（对于mix的node而言）调用apply。最终不同的节点会调用到不同的apply,完成各自的解析
    // 解析完成的东西拼接到DynamicContext中，里面含有#{}
    // 在这里，动态节点和${}都被替换掉了。
    rootSqlNode.apply(context);
    // 处理占位符、汇总参数信息
    // RawSqlSource也会焦勇这一步
    SqlSourceBuilder sqlSourceParser = new SqlSourceBuilder(configuration);
    Class<?> parameterType = parameterObject == null ? Object.class : parameterObject.getClass();
    // 使用SqlSourceBuilder处理#{}，将其转化为？
    // 相关参数放进了context.bindings
    // *** 最终生成了StaticSqlSource对象，然后由它生成BoundSql
    SqlSource sqlSource = sqlSourceParser.parse(context.getSql(), parameterType, context.getBindings());
    BoundSql boundSql = sqlSource.getBoundSql(parameterObject);
    // 把context.getBindings()的参数放到boundSql的metaParameters中进行保存
    context.getBindings().forEach(boundSql::setAdditionalParameter);
    return boundSql;
  }

}

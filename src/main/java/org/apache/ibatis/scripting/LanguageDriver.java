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
package org.apache.ibatis.scripting;

import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.Configuration;

// 脚本语言解释器
// 在接口上注解的SQL语句，就是由它进行解析的
// @Select("select * from `user` where id = #{id}")
//User queryUserById(Integer id);
public interface LanguageDriver {

  /**
   * Creates a {@link ParameterHandler} that passes the actual parameters to the the JDBC statement.
   *
   * @param mappedStatement The mapped statement that is being executed
   * @param parameterObject The input parameter object (can be null)
   * @param boundSql The resulting SQL once the dynamic language has been executed.
   * @return
   * @author Frank D. Martinez [mnesarco]
   * @see DefaultParameterHandler
   */
  /**
   * 创建参数处理器。参数处理器能将实参传递给JDBC statement。
   * @param mappedStatement 完整的数据库操作节点
   * @param parameterObject 参数对象
   * @param boundSql 数据库操作语句转化的BoundSql对象
   * @return 参数处理器
   */
  ParameterHandler createParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql);

  /**
   * Creates an {@link SqlSource} that will hold the statement read from a mapper xml file.
   * It is called during startup, when the mapped statement is read from a class or an xml file.
   *
   * @param configuration The MyBatis configuration
   * @param script XNode parsed from a XML file
   * @param parameterType input parameter type got from a mapper method or specified in the parameterType xml attribute. Can be null.
   * @return
   */

  /**
   * 创建SqlSource对象（基于映射文件的方式）。该方法在MyBatis启动阶段，读取映射接口或映射文件时被调用
   * @param configuration 配置信息
   * @param script 映射文件中的数据库操作节点
   * @param parameterType 参数类型
   * @return SqlSource对象
   */
  SqlSource createSqlSource(Configuration configuration, XNode script, Class<?> parameterType);

  /**
   * Creates an {@link SqlSource} that will hold the statement read from an annotation.
   * It is called during startup, when the mapped statement is read from a class or an xml file.
   *
   * @param configuration The MyBatis configuration
   * @param script The content of the annotation
   * @param parameterType input parameter type got from a mapper method or specified in the parameterType xml attribute. Can be null.
   * @return
   */

  /**
   * 创建SqlSource对象（基于注解的方式）。该方法在MyBatis启动阶段，读取映射接口或映射文件时被调用
   * @param configuration 配置信息
   * @param script 注解中的SQL字符串
   * @param parameterType 参数类型
   * @return SqlSource对象，具体来说是DynamicSqlSource和RawSqlSource中的一种
   */
  SqlSource createSqlSource(Configuration configuration, String script, Class<?> parameterType);
}

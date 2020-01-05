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
package org.apache.ibatis.mapping;

import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

/**
 * Should return an id to identify the type of this database.
 * That id can be used later on to build different queries for each database type
 * This mechanism enables supporting multiple vendors or versions
 *
 * @author Eduardo Macarron
 *
 * MyBatis 可以根据不同的数据库厂商执行不同的语句，这种多厂商的支持是基于映射语句中的 databaseId 属性。
 * MyBatis 会加载不带 databaseId 属性和带有匹配当前数据库 databaseId 属性的所有语句。
 * 如果同时找到带有 databaseId 和不带 databaseId 的相同语句，则后者会被舍弃。
 *
 *
 * config文件中配置：
 * <databaseIdProvider type="DB_VENDOR">
 *   <property name="MySQL" value="mysql"/>
 *   <property name="Oracle" value="oracle" />
 * </databaseIdProvider>
 *
 */
public interface DatabaseIdProvider {

  default void setProperties(Properties p) {
    // NOP
  }

  String getDatabaseId(DataSource dataSource) throws SQLException;
}

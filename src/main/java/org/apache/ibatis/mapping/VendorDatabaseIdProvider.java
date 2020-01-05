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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;

/**
 * Vendor DatabaseId provider.
 *
 * It returns database product name as a databaseId.
 * 它能够返回数据库的产品名称作为databaseId
 *
 * If the user provides a properties it uses it to translate database product name
 * 如果用户设置了properties，那该类会使用properties来翻译数据库产品名
 * key="Microsoft SQL Server", value="ms" will return "ms".
 *
 * It can return null, if no database product name or
 * a properties was specified and no translation was found.
 *
 * @author Eduardo Macarron
 *
 * vendor：摊贩;小贩;卖主;卖方
 * 返回数据库产品名
 */
public class VendorDatabaseIdProvider implements DatabaseIdProvider {

  private Properties properties;

  @Override
  public String getDatabaseId(DataSource dataSource) {
    if (dataSource == null) {
      throw new NullPointerException("dataSource cannot be null");
    }
    try {
      return getDatabaseName(dataSource);
    } catch (Exception e) {
      LogHolder.log.error("Could not get a databaseId from dataSource", e);
    }
    return null;
  }

  @Override
  public void setProperties(Properties p) {
    this.properties = p;
  }

  /**
   * 获取当前的数据源类型的别名
   * @param dataSource 数据源
   * @return 数据源类型别名
   * @throws SQLException
   */
  private String getDatabaseName(DataSource dataSource) throws SQLException {
    // 获取当前连接的数据库名
    String productName = getDatabaseProductName(dataSource);
    // 如果设置有properties值，则根据将获取的数据库名称作为模糊的key,映射为对应的value
    if (this.properties != null) {
      for (Map.Entry<Object, Object> property : properties.entrySet()) {
        if (productName.contains((String) property.getKey())) {
          return (String) property.getValue();
        }
      }
      // 没有找到对应映射
      return null;
    }
    return productName;
  }

  // 从连接中获取当前数据库的产品名
  private String getDatabaseProductName(DataSource dataSource) throws SQLException {
    Connection con = null;
    try {
      con = dataSource.getConnection();
      DatabaseMetaData metaData = con.getMetaData();
      return metaData.getDatabaseProductName();
    } finally {
      if (con != null) {
        try {
          con.close();
        } catch (SQLException e) {
          // ignored
        }
      }
    }
  }

  private static class LogHolder {
    private static final Log log = LogFactory.getLog(VendorDatabaseIdProvider.class);
  }

}

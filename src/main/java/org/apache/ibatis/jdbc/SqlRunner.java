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
package org.apache.ibatis.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;

/**
 * @author Clinton Begin
 */
public class SqlRunner {

  public static final int NO_GENERATED_KEY = Integer.MIN_VALUE + 1001;

  private final Connection connection;
  private final TypeHandlerRegistry typeHandlerRegistry;
  private boolean useGeneratedKeySupport;

  public SqlRunner(Connection connection) {
    this.connection = connection;
    this.typeHandlerRegistry = new TypeHandlerRegistry();
  }

  public void setUseGeneratedKeySupport(boolean useGeneratedKeySupport) {
    this.useGeneratedKeySupport = useGeneratedKeySupport;
  }

  /**
   * Executes a SELECT statement that returns one row.
   *
   * @param sql  The SQL
   * @param args The arguments to be set on the statement.
   * @return The row expected.
   * @throws SQLException If less or more than one row is returned
   */
  public Map<String, Object> selectOne(String sql, Object... args) throws SQLException {
    List<Map<String, Object>> results = selectAll(sql, args);
    if (results.size() != 1) {
      throw new SQLException("Statement returned " + results.size() + " results where exactly one (1) was expected.");
    }
    return results.get(0);
  }

  /**
   * Executes a SELECT statement that returns multiple rows.
   *
   * @param sql  The SQL
   * @param args The arguments to be set on the statement.
   * @return The list of rows expected.
   * @throws SQLException If statement preparation or execution fails
   */

  /**
   * 执行多个数据的查询操作，即SELECT操作
   * @param sql 要查询的SQL语句
   * @param args SQL语句的参数
   * @return 查询结果
   * @throws SQLException
   */
  public List<Map<String, Object>> selectAll(String sql, Object... args) throws SQLException {
    PreparedStatement ps = connection.prepareStatement(sql);
    try {
      setParameters(ps, args);
      ResultSet rs = ps.executeQuery();
      return getResults(rs);
    } finally {
      try {
        ps.close();
      } catch (SQLException e) {
        //ignore
      }
    }
  }

  /**
   * Executes an INSERT statement.
   *
   * @param sql  The SQL
   * @param args The arguments to be set on the statement.
   * @return The number of rows impacted or BATCHED_RESULTS if the statements are being batched.
   * @throws SQLException If statement preparation or execution fails
   */
  public int insert(String sql, Object... args) throws SQLException {
    PreparedStatement ps;
    if (useGeneratedKeySupport) {
      // 设置允许自增主键
      ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
    } else {
      ps = connection.prepareStatement(sql);
    }

    try {
      setParameters(ps, args);
      ps.executeUpdate();
      if (useGeneratedKeySupport) {
        // 返回自增的键值
        List<Map<String, Object>> keys = getResults(ps.getGeneratedKeys());
        if (keys.size() == 1) {
          Map<String, Object> key = keys.get(0);
          Iterator<Object> i = key.values().iterator();
          if (i.hasNext()) {
            Object genkey = i.next();
            if (genkey != null) {
              try {
                return Integer.parseInt(genkey.toString());
              } catch (NumberFormatException e) {
                //ignore, no numeric key support
              }
            }
          }
        }
      }
      return NO_GENERATED_KEY;
    } finally {
      try {
        ps.close();
      } catch (SQLException e) {
        //ignore
      }
    }
  }

  /**
   * Executes an UPDATE statement.
   *
   * @param sql  The SQL
   * @param args The arguments to be set on the statement.
   * @return The number of rows impacted or BATCHED_RESULTS if the statements are being batched.
   * @throws SQLException If statement preparation or execution fails
   */
  public int update(String sql, Object... args) throws SQLException {
    PreparedStatement ps = connection.prepareStatement(sql);
    try {
      setParameters(ps, args);
      return ps.executeUpdate();
    } finally {
      try {
        ps.close();
      } catch (SQLException e) {
        //ignore
      }
    }
  }

  /**
   * Executes a DELETE statement.
   *
   * @param sql  The SQL
   * @param args The arguments to be set on the statement.
   * @return The number of rows impacted or BATCHED_RESULTS if the statements are being batched.
   * @throws SQLException If statement preparation or execution fails
   */
  public int delete(String sql, Object... args) throws SQLException {
    return update(sql, args);
  }

  /**
   * Executes any string as a JDBC Statement.
   * Good for DDL
   *
   * @param sql The SQL
   * @throws SQLException If statement preparation or execution fails
   */
  public void run(String sql) throws SQLException {
    Statement stmt = connection.createStatement();
    try {
      stmt.execute(sql);
    } finally {
      try {
        stmt.close();
      } catch (SQLException e) {
        //ignore
      }
    }
  }

  public void closeConnection() {
    try {
      connection.close();
    } catch (SQLException e) {
      //ignore
    }
  }

  private void setParameters(PreparedStatement ps, Object... args) throws SQLException {
    for (int i = 0, n = args.length; i < n; i++) {
      if (args[i] == null) {
        // SqlRunner需要Null实例来表示与JDBC类型兼容的空值
        throw new SQLException("SqlRunner requires an instance of Null to represent typed null values for JDBC compatibility");
      } else if (args[i] instanceof Null) {
        ((Null) args[i]).getTypeHandler().setParameter(ps, i + 1, null, ((Null) args[i]).getJdbcType());
      } else {
        TypeHandler typeHandler = typeHandlerRegistry.getTypeHandler(args[i].getClass());
        if (typeHandler == null) {
          throw new SQLException("SqlRunner could not find a TypeHandler instance for " + args[i].getClass());
        } else {
          typeHandler.setParameter(ps, i + 1, args[i], null);
        }
      }
    }
  }

  /**
   * 处理数据库操作的返回结果
   * @param rs 返回的结果
   * @return 处理后的结果列表
   * @throws SQLException
   */
  private List<Map<String, Object>> getResults(ResultSet rs) throws SQLException {
    try {
      List<Map<String, Object>> list = new ArrayList<>();
      // 返回结果的字段名列表，按照字段顺序排列
      List<String> columns = new ArrayList<>();
      // 返回结果的类型处理器列表，按照字段顺序排列
      List<TypeHandler<?>> typeHandlers = new ArrayList<>();
      // 获取返回结果的表信息、字段信息等
      ResultSetMetaData rsmd = rs.getMetaData();
      for (int i = 0, n = rsmd.getColumnCount(); i < n; i++) {
        // 记录字段名
        columns.add(rsmd.getColumnLabel(i + 1));
        // 记录字段的对应类型处理器
        try {
          Class<?> type = Resources.classForName(rsmd.getColumnClassName(i + 1));
          TypeHandler<?> typeHandler = typeHandlerRegistry.getTypeHandler(type);
          if (typeHandler == null) {
            typeHandler = typeHandlerRegistry.getTypeHandler(Object.class);
          }
          typeHandlers.add(typeHandler);
        } catch (Exception e) {
          // 默认的类型处理器是Object处理器
          typeHandlers.add(typeHandlerRegistry.getTypeHandler(Object.class));
        }
      }
      // 循环处理结果
      while (rs.next()) {
        Map<String, Object> row = new HashMap<>();
        for (int i = 0, n = columns.size(); i < n; i++) {
          // 字段名
          String name = columns.get(i);
          // 对应处理器
          TypeHandler<?> handler = typeHandlers.get(i);
          // 放入结果中，key为字段名大写，value为取出的结果值
          row.put(name.toUpperCase(Locale.ENGLISH), handler.getResult(rs, name));
        }
        list.add(row);
      }
      return list;
    } finally {
      if (rs != null) {
        try {
          rs.close();
        } catch (Exception e) {
          // ignore
        }
      }
    }
  }

}

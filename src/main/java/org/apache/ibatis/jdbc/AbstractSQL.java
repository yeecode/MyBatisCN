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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Clinton Begin
 * @author Jeff Butler
 * @author Adam Gent
 * @author Kazuki Shimizu
 */
public abstract class AbstractSQL<T> {

  private static final String AND = ") \nAND (";
  private static final String OR = ") \nOR (";

  private final SQLStatement sql = new SQLStatement();

  public abstract T getSelf();

  public T UPDATE(String table) {
    sql().statementType = SQLStatement.StatementType.UPDATE;
    sql().tables.add(table);
    return getSelf();
  }

  public T SET(String sets) {
    sql().sets.add(sets);
    return getSelf();
  }

  /**
   * @since 3.4.2
   */
  public T SET(String... sets) {
    sql().sets.addAll(Arrays.asList(sets));
    return getSelf();
  }

  public T INSERT_INTO(String tableName) {
    sql().statementType = SQLStatement.StatementType.INSERT;
    sql().tables.add(tableName);
    return getSelf();
  }

  public T VALUES(String columns, String values) {
    INTO_COLUMNS(columns);
    INTO_VALUES(values);
    return getSelf();
  }

  /**
   * @since 3.4.2
   */
  public T INTO_COLUMNS(String... columns) {
    sql().columns.addAll(Arrays.asList(columns));
    return getSelf();
  }

  /**
   * @since 3.4.2
   */
  public T INTO_VALUES(String... values) {
    List<String> list = sql().valuesList.get(sql().valuesList.size() - 1);
    for (String value : values) {
      list.add(value);
    }
    return getSelf();
  }

  /**
   * 进行SELECT的操作
   *
   * @param columns SELECT操作的列表
   * @return 整个对象自身。return自身创造了一种类似于建造者模式的形态，可以级联操作，例如：this.SELECT().WHERE()
   *
   * 对象自身包括内部的SafeAppendable【静态类】
   * 在SafeAppendable的属性中存储了：
   *  - 操作的类型
   *  - 操作的元素列表
   */
  public T SELECT(String columns) {
    // 调用该方法表明了是SELECT操作，传入的是SELECT操作对应的LIST
    sql().statementType = SQLStatement.StatementType.SELECT;
    sql().select.add(columns);
    return getSelf();
  }

  /**
   * @since 3.4.2
   */
  public T SELECT(String... columns) {
    sql().statementType = SQLStatement.StatementType.SELECT;
    sql().select.addAll(Arrays.asList(columns));
    return getSelf();
  }

  public T SELECT_DISTINCT(String columns) {
    sql().distinct = true;
    SELECT(columns);
    return getSelf();
  }

  /**
   * @since 3.4.2
   */
  public T SELECT_DISTINCT(String... columns) {
    sql().distinct = true;
    SELECT(columns);
    return getSelf();
  }

  public T DELETE_FROM(String table) {
    sql().statementType = SQLStatement.StatementType.DELETE;
    sql().tables.add(table);
    return getSelf();
  }

  public T FROM(String table) {
    sql().tables.add(table);
    return getSelf();
  }

  /**
   * @since 3.4.2
   */
  public T FROM(String... tables) {
    sql().tables.addAll(Arrays.asList(tables));
    return getSelf();
  }

  public T JOIN(String join) {
    sql().join.add(join);
    return getSelf();
  }

  /**
   * @since 3.4.2
   */
  public T JOIN(String... joins) {
    sql().join.addAll(Arrays.asList(joins));
    return getSelf();
  }

  public T INNER_JOIN(String join) {
    sql().innerJoin.add(join);
    return getSelf();
  }

  /**
   * @since 3.4.2
   */
  public T INNER_JOIN(String... joins) {
    sql().innerJoin.addAll(Arrays.asList(joins));
    return getSelf();
  }

  public T LEFT_OUTER_JOIN(String join) {
    sql().leftOuterJoin.add(join);
    return getSelf();
  }

  /**
   * @since 3.4.2
   */
  public T LEFT_OUTER_JOIN(String... joins) {
    sql().leftOuterJoin.addAll(Arrays.asList(joins));
    return getSelf();
  }

  public T RIGHT_OUTER_JOIN(String join) {
    sql().rightOuterJoin.add(join);
    return getSelf();
  }

  /**
   * @since 3.4.2
   */
  public T RIGHT_OUTER_JOIN(String... joins) {
    sql().rightOuterJoin.addAll(Arrays.asList(joins));
    return getSelf();
  }

  public T OUTER_JOIN(String join) {
    sql().outerJoin.add(join);
    return getSelf();
  }

  /**
   * @since 3.4.2
   */
  public T OUTER_JOIN(String... joins) {
    sql().outerJoin.addAll(Arrays.asList(joins));
    return getSelf();
  }

  public T WHERE(String conditions) {
    sql().where.add(conditions);
    sql().lastList = sql().where;
    return getSelf();
  }

  /**
   * @since 3.4.2
   */
  public T WHERE(String... conditions) {
    sql().where.addAll(Arrays.asList(conditions));
    sql().lastList = sql().where;
    return getSelf();
  }

  public T OR() {
    sql().lastList.add(OR);
    return getSelf();
  }

  public T AND() {
    sql().lastList.add(AND);
    return getSelf();
  }

  public T GROUP_BY(String columns) {
    sql().groupBy.add(columns);
    return getSelf();
  }

  /**
   * @since 3.4.2
   */
  public T GROUP_BY(String... columns) {
    sql().groupBy.addAll(Arrays.asList(columns));
    return getSelf();
  }

  public T HAVING(String conditions) {
    sql().having.add(conditions);
    sql().lastList = sql().having;
    return getSelf();
  }

  /**
   * @since 3.4.2
   */
  public T HAVING(String... conditions) {
    sql().having.addAll(Arrays.asList(conditions));
    sql().lastList = sql().having;
    return getSelf();
  }

  public T ORDER_BY(String columns) {
    sql().orderBy.add(columns);
    return getSelf();
  }

  /**
   * @since 3.4.2
   */
  public T ORDER_BY(String... columns) {
    sql().orderBy.addAll(Arrays.asList(columns));
    return getSelf();
  }

  /**
   * Set the limit variable string(e.g. {@code "#{limit}"}).
   *
   * @param variable a limit variable string
   * @return a self instance
   * @see #OFFSET(String)
   * @since 3.5.2
   */
  public T LIMIT(String variable) {
    sql().limit = variable;
    sql().limitingRowsStrategy = SQLStatement.LimitingRowsStrategy.OFFSET_LIMIT;
    return getSelf();
  }

  /**
   * Set the limit value.
   *
   * @param value an offset value
   * @return a self instance
   * @see #OFFSET(long)
   * @since 3.5.2
   */
  public T LIMIT(int value) {
    return LIMIT(String.valueOf(value));
  }

  /**
   * Set the offset variable string(e.g. {@code "#{offset}"}).
   *
   * @param variable a offset variable string
   * @return a self instance
   * @see #LIMIT(String)
   * @since 3.5.2
   */
  public T OFFSET(String variable) {
    sql().offset = variable;
    sql().limitingRowsStrategy = SQLStatement.LimitingRowsStrategy.OFFSET_LIMIT;
    return getSelf();
  }

  /**
   * Set the offset value.
   *
   * @param value an offset value
   * @return a self instance
   * @see #LIMIT(int)
   * @since 3.5.2
   */
  public T OFFSET(long value) {
    return OFFSET(String.valueOf(value));
  }

  /**
   * Set the fetch first rows variable string(e.g. {@code "#{fetchFirstRows}"}).
   *
   * @param variable a fetch first rows variable string
   * @return a self instance
   * @see #OFFSET_ROWS(String)
   * @since 3.5.2
   */
  public T FETCH_FIRST_ROWS_ONLY(String variable) {
    sql().limit = variable;
    sql().limitingRowsStrategy = SQLStatement.LimitingRowsStrategy.ISO;
    return getSelf();
  }

  /**
   * Set the fetch first rows value.
   *
   * @param value a fetch first rows value
   * @return a self instance
   * @see #OFFSET_ROWS(long)
   * @since 3.5.2
   */
  public T FETCH_FIRST_ROWS_ONLY(int value) {
    return FETCH_FIRST_ROWS_ONLY(String.valueOf(value));
  }

  /**
   * Set the offset rows variable string(e.g. {@code "#{offset}"}).
   *
   * @param variable a offset rows variable string
   * @return a self instance
   * @see #FETCH_FIRST_ROWS_ONLY(String)
   * @since 3.5.2
   */
  public T OFFSET_ROWS(String variable) {
    sql().offset = variable;
    sql().limitingRowsStrategy = SQLStatement.LimitingRowsStrategy.ISO;
    return getSelf();
  }

  /**
   * Set the offset rows value.
   *
   * @param value an offset rows value
   * @return a self instance
   * @see #FETCH_FIRST_ROWS_ONLY(int)
   * @since 3.5.2
   */
  public T OFFSET_ROWS(long value) {
    return OFFSET_ROWS(String.valueOf(value));
  }

  /*
   * used to add a new inserted row while do multi-row insert.
   *
   * @since 3.5.2
   */
  public T ADD_ROW() {
    sql().valuesList.add(new ArrayList<>());
    return getSelf();
  }

  private SQLStatement sql() {
    return sql;
  }

  public <A extends Appendable> A usingAppender(A a) {
    sql().sql(a);
    return a;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sql().sql(sb);
    return sb.toString();
  }

  // Appendable接口:StringBuilder/StringBuffer等都是它的子类，表征具有可拼接性，字符等可以拼接在后面。
  // 一个安全的拼接器，安全是因为外部调用不到。而内部又通过实例化调用

  private static class SafeAppendable {
    // 主串
    private final Appendable a;
    // 主串是否为空
    private boolean empty = true;

    public SafeAppendable(Appendable a) {
      super();
      this.a = a;
    }

    /**
     * 向主串拼接一段字符串
     * @param s 被拼接的字符串
     * @return SafeAppendable内部类自身
     */
    public SafeAppendable append(CharSequence s) {
      try {
        // 要拼接的串长度不为零，则拼完后主串也不是空了
        if (empty && s.length() > 0) {
          empty = false;
        }
        // 拼接
        a.append(s);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      return this;
    }

    /**
     * 判断当前主串是否为空
     * @return 当前主串是否为空
     */
    public boolean isEmpty() {
      return empty;
    }
  }

  // 表征一段完整的SQL语句
  private static class SQLStatement {

    // 语句的类型常量
    public enum StatementType {
      DELETE, INSERT, SELECT, UPDATE
    }

    private enum LimitingRowsStrategy {
      NOP {
        @Override
        protected void appendClause(SafeAppendable builder, String offset, String limit) {
          // NOP
        }
      },
      ISO {
        @Override
        protected void appendClause(SafeAppendable builder, String offset, String limit) {
          if (offset != null) {
            builder.append(" OFFSET ").append(offset).append(" ROWS");
          }
          if (limit != null) {
            builder.append(" FETCH FIRST ").append(limit).append(" ROWS ONLY");
          }
        }
      },
      OFFSET_LIMIT {
        @Override
        protected void appendClause(SafeAppendable builder, String offset, String limit) {
          if (limit != null) {
            builder.append(" LIMIT ").append(limit);
          }
          if (offset != null) {
            builder.append(" OFFSET ").append(offset);
          }
        }
      };

      protected abstract void appendClause(SafeAppendable builder, String offset, String limit);

    }

    // 当前语句的语句类型
    StatementType statementType;

    // 语句片段信息
    List<String> sets = new ArrayList<>();
    List<String> select = new ArrayList<>();
    List<String> tables = new ArrayList<>();
    List<String> join = new ArrayList<>();
    List<String> innerJoin = new ArrayList<>();
    List<String> outerJoin = new ArrayList<>();
    List<String> leftOuterJoin = new ArrayList<>();
    List<String> rightOuterJoin = new ArrayList<>();
    List<String> where = new ArrayList<>();
    List<String> having = new ArrayList<>();
    List<String> groupBy = new ArrayList<>();
    List<String> orderBy = new ArrayList<>();
    List<String> lastList = new ArrayList<>();
    List<String> columns = new ArrayList<>();
    List<List<String>> valuesList = new ArrayList<>();
    // 表征是否去重，该字段仅仅对于SELECT操作有效，它决定是SELECT还是SELECT DISTINCT
    boolean distinct;
    // 结果偏移量
    String offset;
    // 结果总数约束
    String limit;
    // 结果约束策略
    LimitingRowsStrategy limitingRowsStrategy = LimitingRowsStrategy.NOP;

    public SQLStatement() {
      // Prevent Synthetic Access
      valuesList.add(new ArrayList<>());
    }

    // 单词Clause：子句

    /**
     * sql子句拼接操作
     * 传参例子：
     * sqlClause(builder, "WHERE", where, "(", ")", " AND ");
     * 功能：
     * 拼接WHERE,拼接（，循环拼接 item AND ……, 拼接）
     *
     * @param builder 拼接器
     * @param keyword SQL子句的关键词
     * @param parts SQL子句内容，用List存放
     * @param open 开始符号
     * @param close 结束符号
     * @param conjunction 连接词。SQL子句放在List中，该连词负责将List中的各个元素连接起来
     */
    private void sqlClause(SafeAppendable builder, String keyword, List<String> parts, String open, String close,
                           String conjunction) {
      if (!parts.isEmpty()) {
        if (!builder.isEmpty()) {
          builder.append("\n");
        }
        builder.append(keyword);
        builder.append(" ");
        builder.append(open);
        String last = "________"; // 存储上个词语
        for (int i = 0, n = parts.size(); i < n; i++) {
          String part = parts.get(i);
          // parts 中可能本身存在连接词，要跳过这些词
          if (i > 0 && !part.equals(AND) && !part.equals(OR) && !last.equals(AND) && !last.equals(OR)) {
            builder.append(conjunction);
          }
          builder.append(part);
          last = part;
        }
        builder.append(close);
      }
    }

    // SELECT 操作的拼接
    // 因为SELECT操作（其他操作也是）的字符拼接是固定的，因此只要给定各个keyword的list即可按照顺序完成拼接

    /**
     * 将SQL语句片段信息拼接为一个完整的SELECT语句
     * @param builder 语句拼接器
     * @return 拼接完成的SQL语句字符串
     */
    private String selectSQL(SafeAppendable builder) {
      if (distinct) {
        sqlClause(builder, "SELECT DISTINCT", select, "", "", ", ");
      } else {
        sqlClause(builder, "SELECT", select, "", "", ", ");
      }

      sqlClause(builder, "FROM", tables, "", "", ", ");
      joins(builder); // JOIN操作相对复杂，调用单独的joins子方法进行操作
      sqlClause(builder, "WHERE", where, "(", ")", " AND ");
      sqlClause(builder, "GROUP BY", groupBy, "", "", ", ");
      sqlClause(builder, "HAVING", having, "(", ")", " AND ");
      sqlClause(builder, "ORDER BY", orderBy, "", "", ", ");
      limitingRowsStrategy.appendClause(builder, offset, limit);
      return builder.toString();
    }

    // JOIN 操作的拼接，其他都一样
    private void joins(SafeAppendable builder) {
      sqlClause(builder, "JOIN", join, "", "", "\nJOIN ");
      sqlClause(builder, "INNER JOIN", innerJoin, "", "", "\nINNER JOIN ");
      sqlClause(builder, "OUTER JOIN", outerJoin, "", "", "\nOUTER JOIN ");
      sqlClause(builder, "LEFT OUTER JOIN", leftOuterJoin, "", "", "\nLEFT OUTER JOIN ");
      sqlClause(builder, "RIGHT OUTER JOIN", rightOuterJoin, "", "", "\nRIGHT OUTER JOIN ");
    }

    /**
     * 将SQL语句片段信息拼接为一个完整的INSERT语句
     * @param builder 语句拼接器
     * @return 拼接完成的SQL语句字符串
     */
    private String insertSQL(SafeAppendable builder) {
      sqlClause(builder, "INSERT INTO", tables, "", "", "");
      sqlClause(builder, "", columns, "(", ")", ", ");
      for (int i = 0; i < valuesList.size(); i++) {
        sqlClause(builder, i > 0 ? "," : "VALUES", valuesList.get(i), "(", ")", ", ");
      }
      return builder.toString();
    }

    private String deleteSQL(SafeAppendable builder) {
      sqlClause(builder, "DELETE FROM", tables, "", "", "");
      sqlClause(builder, "WHERE", where, "(", ")", " AND ");
      limitingRowsStrategy.appendClause(builder, null, limit);
      return builder.toString();
    }

    private String updateSQL(SafeAppendable builder) {
      sqlClause(builder, "UPDATE", tables, "", "", "");
      joins(builder);
      sqlClause(builder, "SET", sets, "", "", ", ");
      sqlClause(builder, "WHERE", where, "(", ")", " AND ");
      limitingRowsStrategy.appendClause(builder, null, limit);
      return builder.toString();
    }

    /**
     * 根据语句类型，调用不同的语句拼接器拼接SQL语句
     * @param a 起始字符串
     * @return 拼接完成后的结果
     */
    public String sql(Appendable a) {
      SafeAppendable builder = new SafeAppendable(a);
      if (statementType == null) {
        return null;
      }
      String answer;
      switch (statementType) {
        case DELETE:
          answer = deleteSQL(builder);
          break;
        case INSERT:
          answer = insertSQL(builder);
          break;
        case SELECT:
          answer = selectSQL(builder);
          break;
        case UPDATE:
          answer = updateSQL(builder);
          break;
        default:
          answer = null;
      }
      return answer;
    }
  }
}
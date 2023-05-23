/**
 * Copyright 2009-2015 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ibatis.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 类型处理器接口 : TypeHandler 是所有类型处理器都需要实现的接口。如果要自定义类型处理器的话，也是实现该接口。
 * @author Clinton Begin
 */
public interface TypeHandler<T> {

    /**
     * 向 PreparedStatement 中设置参数
     */
    void setParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException;

    /**
     * 从结果集中获取结果
     * @param columnName column name, when configuration <code>useColumnLabel</code> is <code>false</code>
     */
    T getResult(ResultSet rs, String columnName) throws SQLException;

    /**
     * 从结果集中获取结果
     */
    T getResult(ResultSet rs, int columnIndex) throws SQLException;

    /**
     * 从结果集中获取结果
     */
    T getResult(CallableStatement cs, int columnIndex) throws SQLException;

}

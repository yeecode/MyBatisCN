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
package org.apache.ibatis.executor.keygen;

import java.sql.Statement;
import java.util.List;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.RowBounds;

/**
 * @author Clinton Begin
 * @author Jeff Butler
 */
public class SelectKeyGenerator implements KeyGenerator {

  // 用户生成主键的SQL语句的特有标志，该标志会追加在用于生成主键的SQL语句的id的后方
  public static final String SELECT_KEY_SUFFIX = "!selectKey";
  // 插入前执行还是插入后执行
  private final boolean executeBefore;
  // 用户生成主键的SQL语句
  private final MappedStatement keyStatement;

  public SelectKeyGenerator(MappedStatement keyStatement, boolean executeBefore) {
    this.executeBefore = executeBefore;
    this.keyStatement = keyStatement;
  }

  /**
   * 数据插入前进行的操作
   * @param executor 执行器
   * @param ms 映射语句对象
   * @param stmt Statement对象
   * @param parameter SQL语句实参对象
   */
  @Override
  public void processBefore(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
    if (executeBefore) {
      processGeneratedKeys(executor, ms, parameter);
    }
  }

  /**
   * 数据插入后进行的操作
   * @param executor 执行器
   * @param ms 映射语句对象
   * @param stmt Statement对象
   * @param parameter SQL语句实参对象
   */
  @Override
  public void processAfter(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
    if (!executeBefore) {
      processGeneratedKeys(executor, ms, parameter);
    }
  }

  /**
   * 执行一段SQL语句后获取一个值，然后将该值赋给Java对象的自增属性
   *
   * @param executor 执行器
   * @param ms 插入操作的SQL语句（不是生成主键的SQL语句）
   * @param parameter 插入操作的对象
   */
  private void processGeneratedKeys(Executor executor, MappedStatement ms, Object parameter) {
    try {
      // keyStatement为生成主键的SQL语句；keyStatement.getKeyProperties()拿到的是要自增的属性
      if (parameter != null && keyStatement != null && keyStatement.getKeyProperties() != null) {
        // 要自增的属性
        String[] keyProperties = keyStatement.getKeyProperties();
        final Configuration configuration = ms.getConfiguration();
        final MetaObject metaParam = configuration.newMetaObject(parameter);
        if (keyProperties != null) {
          // 为生成主键的SQL语句创建执行器keyExecutor。
          // 原注释：不要关闭keyExecutor，因为它会被父级的执行器关闭
          Executor keyExecutor = configuration.newExecutor(executor.getTransaction(), ExecutorType.SIMPLE);
          // 执行SQL语句，得到主键值
          List<Object> values = keyExecutor.query(keyStatement, parameter, RowBounds.DEFAULT, Executor.NO_RESULT_HANDLER);
          // 主键值必须唯一
          if (values.size() == 0) {
            throw new ExecutorException("SelectKey returned no data.");
          } else if (values.size() > 1) {
            throw new ExecutorException("SelectKey returned more than one value.");
          } else {
            MetaObject metaResult = configuration.newMetaObject(values.get(0));
            if (keyProperties.length == 1) {
              // 要自增的主键只有一个，为其赋值
              if (metaResult.hasGetter(keyProperties[0])) {
                // 从metaResult中用getter得到主键值
                setValue(metaParam, keyProperties[0], metaResult.getValue(keyProperties[0]));
              } else {
                // 可能返回的直接就是主键值本身
                setValue(metaParam, keyProperties[0], values.get(0));
              }
            } else {
              // 要把执行SQL得到的值赋给多个属性
              handleMultipleProperties(keyProperties, metaParam, metaResult);
            }
          }
        }
      }
    } catch (ExecutorException e) {
      throw e;
    } catch (Exception e) {
      throw new ExecutorException("Error selecting key or setting result to parameter object. Cause: " + e, e);
    }
  }

  private void handleMultipleProperties(String[] keyProperties,
      MetaObject metaParam, MetaObject metaResult) {
    String[] keyColumns = keyStatement.getKeyColumns();

    if (keyColumns == null || keyColumns.length == 0) {
      // no key columns specified, just use the property names
      for (String keyProperty : keyProperties) {
        setValue(metaParam, keyProperty, metaResult.getValue(keyProperty));
      }
    } else {
      if (keyColumns.length != keyProperties.length) {
        throw new ExecutorException("If SelectKey has key columns, the number must match the number of key properties.");
      }
      for (int i = 0; i < keyProperties.length; i++) {
        setValue(metaParam, keyProperties[i], metaResult.getValue(keyColumns[i]));
      }
    }
  }

  private void setValue(MetaObject metaParam, String property, Object value) {
    if (metaParam.hasSetter(property)) {
      metaParam.setValue(property, value);
    } else {
      throw new ExecutorException("No setter found for the keyProperty '" + property + "' in " + metaParam.getOriginalObject().getClass().getName() + ".");
    }
  }
}

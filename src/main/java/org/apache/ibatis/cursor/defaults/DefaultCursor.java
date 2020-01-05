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
package org.apache.ibatis.cursor.defaults;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.resultset.DefaultResultSetHandler;
import org.apache.ibatis.executor.resultset.ResultSetWrapper;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

/**
 * This is the default implementation of a MyBatis Cursor.
 * This implementation is not thread safe.
 *
 * @author Guillaume Darmont / guillaume@dropinocean.com
 */
// 默认游标实现，线程不安全
public class DefaultCursor<T> implements Cursor<T> {

  // 结果集处理器
  private final DefaultResultSetHandler resultSetHandler;
  // 该结果集对应的ResultMap信息，来源于Mapper中的<ResultMap>节点
  private final ResultMap resultMap;
  // 返回结果的详细信息
  private final ResultSetWrapper rsw;
  // 结果的起止信息
  private final RowBounds rowBounds;
  // ResultHandler的子类，起到暂存结果的作用
  private final ObjectWrapperResultHandler<T> objectWrapperResultHandler = new ObjectWrapperResultHandler<>();
  // 内部迭代器
  private final CursorIterator cursorIterator = new CursorIterator();
  // 迭代器存在标志位
  private boolean iteratorRetrieved;
  // 游标状态
  private CursorStatus status = CursorStatus.CREATED;
  // 记录已经映射的行
  private int indexWithRowBound = -1;

  private enum CursorStatus {

    /**
     * A freshly created cursor, database ResultSet consuming has not started.
     */
    CREATED, // 表征游标新创建，结果集尚未消费
    /**
     * A cursor currently in use, database ResultSet consuming has started.
     */
    OPEN, // 表征游标正在被使用中，结果集正在被消费
    /**
     * A closed cursor, not fully consumed.
     */
    CLOSED, // 表征游标已经被关闭，但其中的结果集未被完全消费
    /**
     * A fully consumed cursor, a consumed cursor is always closed.
     */
    CONSUMED // 表征游标已经被关闭，其中的结果集已经被完全消费
  }

  public DefaultCursor(DefaultResultSetHandler resultSetHandler, ResultMap resultMap, ResultSetWrapper rsw, RowBounds rowBounds) {
    this.resultSetHandler = resultSetHandler;
    this.resultMap = resultMap;
    this.rsw = rsw;
    this.rowBounds = rowBounds;
  }

  @Override
  public boolean isOpen() {
    return status == CursorStatus.OPEN;
  }

  @Override
  public boolean isConsumed() {
    return status == CursorStatus.CONSUMED;
  }

  @Override
  public int getCurrentIndex() {
    return rowBounds.getOffset() + cursorIterator.iteratorIndex;
  }

  /**
   * 返回迭代器
   * @return 迭代器
   */
  @Override
  public Iterator<T> iterator() {
    if (iteratorRetrieved) { // 如果迭代器已经给出
      throw new IllegalStateException("Cannot open more than one iterator on a Cursor");
    }
    if (isClosed()) { // 如果游标已经关闭
      throw new IllegalStateException("A Cursor is already closed.");
    }
    // 表明迭代器已经给出
    iteratorRetrieved = true;
    // 返回迭代器
    return cursorIterator;
  }

  // 关闭
  @Override
  public void close() {
    if (isClosed()) {
      return;
    }

    ResultSet rs = rsw.getResultSet();
    try {
      if (rs != null) {
        rs.close();
      }
    } catch (SQLException e) {
      // ignore
    } finally {
      status = CursorStatus.CLOSED;
    }
  }

  /**
   * 考虑边界限制（翻页限制），从数据库中获取下一个对象
   * @return 下一个对象
   */
  protected T fetchNextUsingRowBound() {
    // 从数据库查询结果中取出下一个对象
    T result = fetchNextObjectFromDatabase();
    while (result != null && indexWithRowBound < rowBounds.getOffset()) { // 如果对象存在但不满足边界限制，则持续读取数据库结果中的下一个，直到边界起始位置
      result = fetchNextObjectFromDatabase();
    }
    return result;
  }

  /**
   * 从数据库获取下一个对象
   * @return 下一个对象
   */
  protected T fetchNextObjectFromDatabase() {
    if (isClosed()) {
      return null;
    }
    try {
      status = CursorStatus.OPEN;
      if (!rsw.getResultSet().isClosed()) { // 结果集尚未关闭
        // 从结果集中取出一条记录，将其转化为对象，并存入到objectWrapperResultHandler中
        resultSetHandler.handleRowValues(rsw, resultMap, objectWrapperResultHandler, RowBounds.DEFAULT, null);
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    // 获得存入到objectWrapperResultHandler中的对象
    T next = objectWrapperResultHandler.result;
    if (next != null) { // 读到了新的对象
      // 更改索引，表明记录索引加一
      indexWithRowBound++;
    }

    if (next == null || getReadItemsCount() == rowBounds.getOffset() + rowBounds.getLimit()) { // 没有新对象或者已经到了rowBounds边界
      // 游标内的数据已经消费完毕
      close();
      status = CursorStatus.CONSUMED;
    }
    // 清除objectWrapperResultHandler中的该对象，已准备迎接下一对象
    objectWrapperResultHandler.result = null;
    return next;
  }

  private boolean isClosed() {
    return status == CursorStatus.CLOSED || status == CursorStatus.CONSUMED;
  }

  private int getReadItemsCount() {
    return indexWithRowBound + 1;
  }

  private static class ObjectWrapperResultHandler<T> implements ResultHandler<T> {

    private T result;

    /**
     * 从结果上下文中取出并处理结果
     * @param context 结果上下文
     */
    @Override
    public void handleResult(ResultContext<? extends T> context) {
      // 取出结果上下文中的一条结果
      this.result = context.getResultObject();
      // 关闭结果上下文
      context.stop();
    }
  }

  private class CursorIterator implements Iterator<T> {
    // 缓存下一个要返回的对象，在next操作中完成写入
    T object;
    // next方法中返回的对象的索引
    int iteratorIndex = -1;

    /**
     * 判断是否还有下一个元素，如果有则顺便写入object中
     * @return 是否还有下一个元素
     */
    @Override
    public boolean hasNext() {
      // 如果object!=null，则显然有下一个对象，就是object本身
      if (object == null) {
        // 判断是否还能获取到新的，顺便放到object中
        object = fetchNextUsingRowBound();
      }
      return object != null;
    }

    /**
     * 返回下一个元素
     * @return 下一个元素
     */
    @Override
    public T next() {
      T next = object;

      if (next == null) { // object中无对象
        // 尝试去获取一个
        next = fetchNextUsingRowBound();
      }

      if (next != null) {
        // 此时，next中是这次要返回的对象。object要么本来为null,要么已经取到next中。故清空
        object = null;
        iteratorIndex++;
        // 返回next中的对象
        return next;
      }
      throw new NoSuchElementException();
    }

    /**
     * 删除当前的元素。不允许该操作，故直接抛出异常
     */
    @Override
    public void remove() {
      throw new UnsupportedOperationException("Cannot remove element from Cursor");
    }
  }
}

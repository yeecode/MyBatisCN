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
package org.apache.ibatis.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.parsing.GenericTokenParser;
import org.apache.ibatis.parsing.TokenHandler;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;

/**
 * @author Clinton Begin
 *
 * SqlSource的解析器
 *
 */
public class SqlSourceBuilder extends BaseBuilder {

  // 能够处理的占位符属性
  private static final String PARAMETER_PROPERTIES = "javaType,jdbcType,mode,numericScale,resultMap,typeHandler,jdbcTypeName";

  public SqlSourceBuilder(Configuration configuration) {
    super(configuration);
  }


  // 这里解析的对象是SqlNode拼接结束的，即<if> <where>等节点的结果都已经解析结束。然后在这里继续处理

  /**
   * 将DynamicSqlSource和RawSqlSource中的“#{}”符号替换掉，从而将他们转化为StaticSqlSource
   * @param originalSql sqlNode.apply()拼接之后的sql语句。已经不包含<if> <where>等节点，也不含有${}符号
   * @param parameterType 实参类型
   * @param additionalParameters 附加参数
   * @return 解析结束的StaticSqlSource
   */
  public SqlSource parse(String originalSql, Class<?> parameterType, Map<String, Object> additionalParameters) {
    // 用来完成#{}处理的处理器
    ParameterMappingTokenHandler handler = new ParameterMappingTokenHandler(configuration, parameterType, additionalParameters);
    // 通用的占位符解析器，用来进行占位符替换
    GenericTokenParser parser = new GenericTokenParser("#{", "}", handler);
    // 将#{}替换为?的SQL语句
    String sql = parser.parse(originalSql);
    // 生成新的StaticSqlSource对象
    return new StaticSqlSource(configuration, sql, handler.getParameterMappings());
  }

  // 用以替换占位符的处理器
  // 用来处理形如#｛ id, javaType= int, jdbcType=NUMERIC, typeHandler=DemoTypeHandler ｝
  private static class ParameterMappingTokenHandler extends BaseBuilder implements TokenHandler {

    // 每个#{}中的东西对应一个ParameterMapping。所有的#{}都放在这个list
    private List<ParameterMapping> parameterMappings = new ArrayList<>();
    // 参数类型
    private Class<?> parameterType;
    // 参数的Meta对象
    private MetaObject metaParameters;

    public ParameterMappingTokenHandler(Configuration configuration, Class<?> parameterType, Map<String, Object> additionalParameters) {
      super(configuration);
      this.parameterType = parameterType;
      this.metaParameters = configuration.newMetaObject(additionalParameters);
    }

    public List<ParameterMapping> getParameterMappings() {
      return parameterMappings;
    }

    /**
     * 在这里，${}被替换为？
     * 但同时，用户传入的实际参数也被记录了
     * @param content 包含
     * @return
     */
    @Override
    public String handleToken(String content) {
      parameterMappings.add(buildParameterMapping(content));
      return "?";
    }

    /**
     *
     * @param content 形如id, javaType= int, jdbcType=NUMERIC, typeHandler=DemoTypeHandler
     * @return
     */
    private ParameterMapping buildParameterMapping(String content) {
      // 将参数转化为map
      Map<String, String> propertiesMap = parseParameterMapping(content);
      String property = propertiesMap.get("property");
      Class<?> propertyType;
      if (metaParameters.hasGetter(property)) { // issue #448 get type from additional params
        propertyType = metaParameters.getGetterType(property);
      } else if (typeHandlerRegistry.hasTypeHandler(parameterType)) {
        propertyType = parameterType;
      } else if (JdbcType.CURSOR.name().equals(propertiesMap.get("jdbcType"))) {
        propertyType = java.sql.ResultSet.class;
      } else if (property == null || Map.class.isAssignableFrom(parameterType)) {
        propertyType = Object.class;
      } else {
        MetaClass metaClass = MetaClass.forClass(parameterType, configuration.getReflectorFactory());
        if (metaClass.hasGetter(property)) {
          propertyType = metaClass.getGetterType(property);
        } else {
          propertyType = Object.class;
        }
      }
      ParameterMapping.Builder builder = new ParameterMapping.Builder(configuration, property, propertyType);
      Class<?> javaType = propertyType;
      String typeHandlerAlias = null;
      for (Map.Entry<String, String> entry : propertiesMap.entrySet()) {
        String name = entry.getKey();
        String value = entry.getValue();
        if ("javaType".equals(name)) {
          javaType = resolveClass(value);
          builder.javaType(javaType);
        } else if ("jdbcType".equals(name)) {
          builder.jdbcType(resolveJdbcType(value));
        } else if ("mode".equals(name)) {
          builder.mode(resolveParameterMode(value));
        } else if ("numericScale".equals(name)) {
          builder.numericScale(Integer.valueOf(value));
        } else if ("resultMap".equals(name)) {
          builder.resultMapId(value);
        } else if ("typeHandler".equals(name)) {
          typeHandlerAlias = value;
        } else if ("jdbcTypeName".equals(name)) {
          builder.jdbcTypeName(value);
        } else if ("property".equals(name)) {
          // Do Nothing
        } else if ("expression".equals(name)) {
          throw new BuilderException("Expression based parameters are not supported yet");
        } else {
          throw new BuilderException("An invalid property '" + name + "' was found in mapping #{" + content + "}.  Valid properties are " + PARAMETER_PROPERTIES);
        }
      }
      if (typeHandlerAlias != null) {
        builder.typeHandler(resolveTypeHandler(javaType, typeHandlerAlias));
      }
      return builder.build();
    }

    private Map<String, String> parseParameterMapping(String content) {
      try {
        // content = id, javaType= int, jdbcType=NUMERIC, typeHandler=DemoTypeHandler ;
        return new ParameterExpression(content);
      } catch (BuilderException ex) {
        throw ex;
      } catch (Exception ex) {
        throw new BuilderException("Parsing error was found in mapping #{" + content + "}.  Check syntax #{property|(expression), var1=value1, var2=value2, ...} ", ex);
      }
    }
  }

}

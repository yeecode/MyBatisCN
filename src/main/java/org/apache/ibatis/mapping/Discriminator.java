/**
 *    Copyright 2009-2015 the original author or authors.
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

import java.util.Collections;
import java.util.Map;

import org.apache.ibatis.session.Configuration;

/**
 * @author Clinton Begin
 * 属于ResultMap，是ResultMap附属的鉴别器
 * 类似于一个switch语句吧
 *
 */

/*

鉴别器：mybatis可以使用discriminator判断某列的值，然后根据某列的值改变封装行为。


<resultMap id="userMap" type="User" autoMapping="false">
    <id property="id" column="id" javaType="Integer"  jdbcType="INTEGER"  typeHandler="org.apache.ibatis.type.IntegerTypeHandler"/>
    <result property="name" column="name"/>
    <discriminator javaType="int" column="sex">
        <case value="0" resultMap="boyUserMap"/>
        <case value="1" resultMap="girlUserMap"/>
    </discriminator>
</resultMap>

<resultMap id="girlUserMap" type="Girl" extends="userMap">
    <result property="email" column="email"/>
</resultMap>

<resultMap id="boyUserMap" type="Boy" extends="userMap">
    <result property="age" column="age"/>
</resultMap>
*/

public class Discriminator {

  // 存储条件判断行的信息，如<discriminator javaType="int" column="sex">中的信息
  private ResultMapping resultMapping;

  // 存储选择项的信息，键为value值，值为resultMap值。如<case value="0" resultMap="boyUserMap"/>中的信息
  private Map<String, String> discriminatorMap;

  Discriminator() {
  }

  public static class Builder {
    private Discriminator discriminator = new Discriminator();

    public Builder(Configuration configuration, ResultMapping resultMapping, Map<String, String> discriminatorMap) {
      discriminator.resultMapping = resultMapping;
      discriminator.discriminatorMap = discriminatorMap;
    }

    public Discriminator build() {
      assert discriminator.resultMapping != null;
      assert discriminator.discriminatorMap != null;
      assert !discriminator.discriminatorMap.isEmpty();
      //lock down map
      discriminator.discriminatorMap = Collections.unmodifiableMap(discriminator.discriminatorMap);
      return discriminator;
    }
  }

  public ResultMapping getResultMapping() {
    return resultMapping;
  }

  public Map<String, String> getDiscriminatorMap() {
    return discriminatorMap;
  }

  public String getMapIdFor(String s) {
    return discriminatorMap.get(s);
  }

}

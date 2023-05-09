package com.guorong.ibatis.reflection;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 我们可以通过MetaObject对象解析复杂的表达式来对提供的对象进行操作
 */
class MetaObjectTest {

    @Test
    void testShouldGetAndSetField() {
        MetaObjectBean metaObjectBean = new MetaObjectBean();
        MetaObject metaObject = SystemMetaObject.forObject(metaObjectBean);
        String value = "张三";
        metaObject.setValue("name", value);
        Assertions.assertTrue(value.equals(metaObject.getValue("name")));
    }



    static class MetaObjectBean {
        private String name;
        private String desc;
    }
}

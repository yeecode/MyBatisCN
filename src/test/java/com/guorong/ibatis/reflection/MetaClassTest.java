package com.guorong.ibatis.reflection;

import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaClass;
import org.junit.jupiter.api.Test;

/**
 * 在Reflector中可以针对普通的属性操作，但是如果出现了比较复杂的属性，比如 private Person person;
 * 这种，我们要查找的表达式 person.userName.针对这种表达式的处理，这时就可以通过MetaClass来处理了。
 * 通过 Reflector 和 ReflectorFactory 的组合使用 实现对复杂的属性表达式的解析
 */
public class MetaClassTest {

    @Test
    public void test() {
        DefaultReflectorFactory reflectorFactory = new DefaultReflectorFactory();
        MetaClass metaClass = MetaClass.forClass(Person.class, reflectorFactory);
        System.out.println(metaClass.hasGetter("name"));
        System.out.println(metaClass.hasSetter("name"));
    }




    static class Person {
        private String id;
        private String name;
        private ClassInfo classInfo;
    }

    // 班级信息
    static class ClassInfo {
        private String className;
        public String getClassName() {
            return className;
        }
        public void setClassName(String className) {
            this.className = className;
        }
    }

}

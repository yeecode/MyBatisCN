package com.guorong.ibatis.reflection;

import org.apache.ibatis.reflection.Reflector;
import org.junit.jupiter.api.Test;

public class ReflectorTest {

    @Test
    public void test01() {
        Reflector reflector = new Reflector(Person.class);
        String[] propertyNames = reflector.getGetablePropertyNames();
        Class<?> clazz = reflector.getSetterType("id");
        System.out.println(clazz.getSimpleName());
    }



    static class Person {
        private String id;
        private String name;
        private Integer age;
        public String getId() {
            return id;
        }
        public void setId(String id) {
            this.id = id;
        }
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }
    }
}

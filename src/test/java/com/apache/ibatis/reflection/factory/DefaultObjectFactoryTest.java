package com.apache.ibatis.reflection.factory;


import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.junit.Test;

public class DefaultObjectFactoryTest {

    private ObjectFactory objectFactory = new DefaultObjectFactory();


    @Test
    public void testCreate() {
        Person person = objectFactory.create(Person.class);
        System.out.println(person);
    }


    public static class Person{}

}

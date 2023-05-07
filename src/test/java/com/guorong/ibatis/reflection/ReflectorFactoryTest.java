package com.guorong.ibatis.reflection;

import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.Reflector;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ReflectorFactoryTest {

    @Test
    public void testCacheIsTrue() {
        DefaultReflectorFactory reflectorFactory = new DefaultReflectorFactory();
        Reflector oneReflector = reflectorFactory.findForClass(Object.class);
        Reflector twoReflector = reflectorFactory.findForClass(Object.class);
        Assertions.assertTrue(oneReflector == twoReflector);
    }


    @Test
    public void testCacheIsFalse() {
        DefaultReflectorFactory reflectorFactory = new DefaultReflectorFactory();
        reflectorFactory.setClassCacheEnabled(Boolean.FALSE);
        Reflector oneReflector = reflectorFactory.findForClass(Object.class);
        Reflector twoReflector = reflectorFactory.findForClass(Object.class);
        Assertions.assertFalse(oneReflector == twoReflector);
    }




}

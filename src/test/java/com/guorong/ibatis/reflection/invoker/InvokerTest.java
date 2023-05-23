package com.guorong.ibatis.reflection.invoker;

import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.Reflector;
import org.apache.ibatis.reflection.invoker.Invoker;
import org.junit.jupiter.api.Test;

class InvokerTest {


    @Test
    void test() throws Exception {
        Reflector reflector = new DefaultReflectorFactory().findForClass(Person.class);
        // 通过默认构造器创建对象
        Object object = reflector.getDefaultConstructor().newInstance();
        // 获取get和set方法调用器
        Invoker setInvoker = reflector.getSetInvoker("name");
        Invoker getInvoker = reflector.getGetInvoker("name");
        setInvoker.invoke(object, new Object[]{"张三"});
        getInvoker.invoke(object, new Object[]{});
    }


    static class Person {
        public String getName() {
            System.out.println(String.format("getName() 执行了...."));
            return "hello";
        }

        public void setName(String name) {
            System.out.println(String.format("setName() 执行了...."));
        }

        public void sayHello() {
            System.out.println("sayHello() 执行了....");
        }
    }

}

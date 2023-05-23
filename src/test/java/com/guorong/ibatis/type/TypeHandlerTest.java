package com.guorong.ibatis.type;

import org.apache.ibatis.type.IntegerTypeHandler;
import org.junit.jupiter.api.Test;

class TypeHandlerTest {

    @Test
    void test() {
        IntegerTypeHandler typeHandler = new IntegerTypeHandler();
        System.out.println(typeHandler.getRawType());
    }

}

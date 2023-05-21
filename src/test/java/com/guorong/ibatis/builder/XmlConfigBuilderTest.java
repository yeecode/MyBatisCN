package com.guorong.ibatis.builder;

import org.apache.ibatis.builder.xml.XMLConfigBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

class XmlConfigBuilderTest {

    @Test
    void twiceParseConfigurationXmlFile() throws IOException {
        String resource = "com/guorong/ibatis/builder/MinimalMapperConfig.xml";
        try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
            XMLConfigBuilder xmlConfigBuilder = new XMLConfigBuilder(inputStream);
            // 解析配置文件
            xmlConfigBuilder.parse();
            // 第二次解析(不允许重复解析)
            xmlConfigBuilder.parse();
        }
    }

    @Test
    void test() throws IOException {
        String resource = "com/guorong/ibatis/builder/MinimalMapperConfig.xml";
        try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
            XMLConfigBuilder xmlConfigBuilder = new XMLConfigBuilder(inputStream);
            Configuration configuration = xmlConfigBuilder.getConfiguration();
            TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
            TypeHandler<String> typeHandler = typeHandlerRegistry.getTypeHandler(String.class);
            System.out.println("-------------");
        }
    }
}

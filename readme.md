## binding包说明

binding包具有以下两个功能：

1. 维护映射接口中抽象方法与数据库操作节点之间的关联关系。
2. 为映射接口中的抽象方法接入对应的数据库操作。

### MapperProxy

MapperProxy 是一个代理类，它是 Mapper 接口的动态代理对象，用于处理 Mapper 接口方法的调用。MapperProxy对象通常由MapperProxyFactory对象进行创建和管理。

MapperProxy 对象主要包含以下属性：

1. sqlSession：SqlSession对象，用于执行SQL语句。

2. methodCache：缓存Mapper方法的Method对象。

3. mapperInterface：Mapper接口的Class对象。

MapperProxy对象的主要作用是将Mapper方法的调用转换成对SqlSession对象的调用。当Mapper方法被调用时，MapperProxy会根据方法名和参数类型，从MethodCache中获取MethodSignature对象，然后将该对象传递给SqlSession对象进行执行。执行完成后，MapperProxy会将结果转换成Mapper方法的返回类型，并返回给调用者。

MapperProxy对象的创建和解析是MyBatis的核心之一，它对Mapper方法的执行和结果处理都有很大的影响。因此，在使用MyBatis时，我们需要对MapperProxy对象有深入的了解，才能更好地发挥其优势。同时，我们也需要注意MapperProxy对象的性能问题，避免创建过多的MapperProxy对象，以及避免频繁地调用Mapper方法。

### MapperMethod

MapperMethod类中只包含 SqlCommand 和 MethodSignature 两个属性，分别表示SQL语句和Mapper接口中对应方法的签名信息。

SqlCommand 表示Mapper接口中对应方法的SQL语句，包括SQL语句的CRUD类型、SQL语句的唯一标识符。SqlCommand是通过解析Mapper接口中的注解或XML文件中的映射语句得到的。

MethodSignature 表示 Mapper 接口中对应方法的签名信息，包括方法名、返回类型、参数类型等信息。MethodSignature是通过Java反射API得到的。

MapperMethod类中的`execute`方法，用于执行Mapper接口中定义的方法。在执行过程中，MapperMethod会根据SqlCommand和MethodSignature属性，构造对应的SqlSession执行器，并传递方法的参数，最终返回查询结果或者执行结果。

总之，MapperMethod是MyBatis框架中的一个辅助类，用于描述Mapper接口中方法的SQL语句和签名信息，并生成对应的SqlSession执行器，执行Mapper接口中定义的方法。



## builder包说明

MyBatis的`builder`模块是MyBatis的一个重要组成部分，它主要负责创建`SqlSessionFactory`对象。在MyBatis中，`SqlSessionFactory`是一个线程安全的、重量级的对象，因此需要在应用程序启动时创建一次，然后在应用程序的整个生命周期中重复使用。`SqlSessionFactoryBuilder`就是用来创建`SqlSessionFactory`对象的。

`builder`模块的主要类包括：

- `XMLConfigBuilder`：解析MyBatis的配置文件，创建`Configuration`对象，并对配置文件中的各个配置项进行校验和合并。
- `XMLMapperBuilder`：解析Mapper映射文件，将映射文件中的SQL语句和参数映射到`MappedStatement`对象中。
- `MapperBuilderAssistant`：辅助`XMLMapperBuilder`类进行Mapper映射文件的解析，负责创建`MappedStatement`对象和其他相关对象。
- `MapperAnnotationBuilder`：解析Mapper接口上的注解，将注解中的SQL语句和参数映射到`MappedStatement`对象中。

在创建`SqlSessionFactory`对象的过程中，`builder`模块会读取MyBatis的配置文件和Mapper映射文件，将它们解析成MyBatis内部使用的对象，并将这些对象组合起来构建出一个完整的`SqlSessionFactory`对象，最终返回给应用程序使用。

总的来说，`builder`模块是MyBatis的一个重要组成部分，它负责将MyBatis的配置文件和Mapper映射文件解析成MyBatis内部使用的对象，并将这些对象组合起来构建出一个完整的`SqlSessionFactory`对象。

### XMLConfigBuilder

`XMLConfigBuilder`是MyBatis中的一个类，用于解析`mybatis-config.xml`文件，读取其中的配置信息，生成`Configuration`对象，从而为MyBatis的使用提供配置支持。

XMLConfigBuilder的主要作用有：

1. 解析mybatis-config.xml文件，生成Configuration对象。

2. 解析并生成DataSource和TransactionFactory等对象。

3. 解析映射文件（Mapper.xml），并将解析结果存储在Configuration对象中，供SqlSessionFactory使用。

XMLConfigBuilder的解析过程如下：

1. 解析mybatis-config.xml文件，生成Document对象。

2. 从Document对象中获取各个节点的信息，如dataSource、transactionManager、mapper等节点信息。

3. 根据节点信息生成对应的对象，如DataSource、TransactionFactory、MapperRegistry等对象。

4. 将生成的对象存储在Configuration对象中，供SqlSessionFactory使用。

XMLConfigBuilder是MyBatis框架中非常重要的一个类，它的解析过程对MyBatis的使用和配置起着重要的作用。








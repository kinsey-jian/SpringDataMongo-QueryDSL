### MongoDB相关
 1. MongoDB是一个面向文档的数据库，它并不是关系型数据库，直接存取BSON，这意味着MongoDB更加灵活，因为可以在文档中直接插入数组之类的复杂数据类型，所以没必要遵守关系型数据库的三大范式，
传统的三大范式导致为了满足一个数据的查询，需要多张表的join，每张表都对应着磁盘一次读取，这和数据放在一个collection里面一次性读取完全不一样的。
 2. 在扩展性上，应用数据快速增长，关系型数据库通过分库分表会带来繁重的工作量和技术复杂度，而在MongoDB有非常有效的，现成的解决方案。通过自带的Mongos集群，只需要在适当的时候继续添加Mongo分片，
 就可以实现程序段自动水平扩展和路由，一方面缓解单个节点的读写压力，另外一方面可有效地均衡磁盘容量的使用情况。整个mongos集群对应用层完全透明，并可完美地做到各个Mongos集群组件的高可用性。
 3. 在我们使用ES CQRS后 view层使用MongoDB将会更加高效的进行数据查询，统计分析等

本文内容包含MongoDB与QueryDSL的整合，document采用DBREf和内嵌以及MySQL性能对比（本机测试），MongoDB数据迁移 以及MongoDB索引和执行计划等
#### MongoDB 与 QueryDSL整合
pom文件
``` pom
<dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-mongodb-reactive</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>
        <dependency>
            <groupId>com.querydsl</groupId>
            <artifactId>querydsl-mongodb</artifactId>
            <version>${querydsl.version}</version>
        </dependency>
        <dependency>
            <groupId>com.querydsl</groupId>
            <artifactId>querydsl-apt</artifactId>
            <version>${querydsl.version}</version>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct-jdk8</artifactId>
            <version>${org.mapstruct.version}</version>
        </dependency>
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct-processor</artifactId>
            <version>${org.mapstruct.version}</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>io.projectreactor</groupId>
            <artifactId>reactor-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
            <plugin>
                <groupId>com.mysema.maven</groupId>
                <artifactId>apt-maven-plugin</artifactId>
                <version>1.1.3</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>process</goal>
                        </goals>
                        <configuration>
                            <outputDirectory>target/generated-sources/mongo</outputDirectory>
                            <processor>org.springframework.data.mongodb.repository.support.MongoAnnotationProcessor</processor>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
``` 
配置文件
``` 
server:
  port: 9321
spring:
  data:
    mongodb:
      uri: mongodb://test:123456@localhost:27017/test
``` 
要实现级联保存，先自定义CascadeSave注解
``` Java
@Retention(RetentionPolicy.RUNTIME) //生命周期 永远不会丢弃
@Target(ElementType.FIELD) //作用在field上
public @interface CascadeSave {
}
```
注解的处理，重写`AbstractMongoEventListener`的`onBeforeConvert`方法，
利用Spring返回工具类`ReflectionUtils.FieldCallback`，重写doWith方法，
在doWith方法里面找这个Field上是否有对应的注解(CascadeSave和DBRef)，然后`mongoOperations`进行持久化，只需在启动注入`CascadeSaveMongoEventListener`即可
``` Java
public class CascadeSaveMongoEventListener extends AbstractMongoEventListener<Object> {

    @Autowired
    private MongoOperations mongoOperations;

    @Override
    public void onBeforeConvert(final BeforeConvertEvent<Object> event) {
        final Object source = event.getSource();
        ReflectionUtils.doWithFields(source.getClass(), new CascadeCallback(source, mongoOperations));
    }
}

@Getter
@Setter
public class CascadeCallback implements ReflectionUtils.FieldCallback {

    private Object source;
    private MongoOperations mongoOperations;

    CascadeCallback(final Object source, final MongoOperations mongoOperations) {
        this.source = source;
        this.setMongoOperations(mongoOperations);
    }

    @Override
    public void doWith(final Field field) throws IllegalArgumentException, IllegalAccessException {
        ReflectionUtils.makeAccessible(field);

        if (field.isAnnotationPresent(DBRef.class) && field.isAnnotationPresent(CascadeSave.class)) {
            final Object fieldValue = field.get(getSource());

            if (fieldValue != null) {
                final CustomFieldCallback callback = new CustomFieldCallback();

                ReflectionUtils.doWithFields(fieldValue.getClass(), callback);

                getMongoOperations().save(fieldValue);
            }
        }

    }
}

 @Bean
 public CascadeSaveMongoEventListener cascadeControlMongoEventListener() {
     return new CascadeSaveMongoEventListener();
 }
```
User document 和 Address document, 要实现级联保存加上`@DBRef @CascadeSave`注解即可
@CreatedDate @LastModifiedDate 只要在Application上加上`@EnableMongoAuditing`即可，
@CreatedBy @LastModifiedBy 继承AuditorAware 重写getCurrentAuditor，跟Jpa一样
``` java
@Getter
@Setter
@Document
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndexes({@CompoundIndex(name = "idx_name_age", def = "{'name' : 1, 'age': 1}")})
public class User implements Serializable {

    @Id
    private String id;

    private String name;

    private Integer age;

    @Indexed(direction = IndexDirection.ASCENDING)
    private String email;

    @Indexed(direction = IndexDirection.ASCENDING)
    private String tel;

    private double source;

    @DBRef
    @CascadeSave
    private Address address;

    @Version
    private Long version;

    @CreatedDate
    private LocalDateTime createDate;

    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    @CreatedBy
    private String createdUser;

    @LastModifiedBy
    private String lastModifiedUser;
}

@Document
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Address implements Serializable {

    @Id
    private String id;

    private String province;

    private String city;

    private String area;

    private String detailAddress;

}

@Configuration
public class UserAuditor implements AuditorAware<String> {
    @Override
    public Optional<String> getCurrentAuditor() {
        return Optional.of("张三");
    }
}
```
Repository很简单，继承`QuerydslPredicateExecutor<T>`即可，可以像jpa一样写些简单的查询
``` java
@Repository
public interface UserRepository extends MongoRepository<User, String>, QuerydslPredicateExecutor<User> {
      List<User> findByAgeBetween(int s, int e);
      long countByAge(int age);
}
```
动态查询，同样可以采用QueryDSL core 的`BooleanBuilder`来实现，跟我们原来的写法一样
``` java
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonFilter {

    private String search;

    public BooleanBuilder toExpression() {
        BooleanBuilder builder = new BooleanBuilder();
        QUser user = QUser.user;
        if(!StringUtils.isEmpty(search)){
            builder.and(user.email.contains(search).or(user.tel.contains(search)));
        }
        return builder;
    }
}

Page<User> page = userRepository.findAll(filter.toExpression(), pageable);
```
很多时候我们需要进行分组查询，分组查询我们利用`Aggregation`来做，尽管还是有点麻烦
 - Aggregation.match()方法作为分组查询条件Criteria.where()里面写字段名称,条件必须是group后得到的数据中的
 - Aggregation.group()分组内容，sum,count,max,min,avg等等一些基本的函数，first是去第一个值 必须取别名(as)
 - Aggregation.project()是接收字段，接收的model的属性，project("age").andExpression("字段名*[0]", 10).as("source")
   `andExpression`可以实现自定义函数 group后的字段 可以做四则远算
 - 然后利用mongoTemplate.aggregate(aggregation, 查询的document, 返回的model)
 
``` java
//条件
MatchOperation matchOperation = Aggregation.match(Criteria.where("createDate").gte(LocalDateTime.of(2018, 8, 1, 0, 0)).lte(LocalDateTime.now()));
//排序
SortOperation sortOperation = Aggregation.sort(Sort.Direction.DESC, "age");
//分组
GroupOperation groupOperation = Aggregation.group("age").count().as("count").first("age").as("age").first("createDate").as("createDate");
//接收字段
ProjectionOperation projectionOperation = project("age", "count", "createDate");

Aggregation aggregation = Aggregation.newAggregation(matchOperation, groupOperation, projectionOperation, sortOperation);
List<StatisticsModel> results = mongoTemplate.aggregate(aggregation, User.class, StatisticsModel.class).getMappedResults();
```

#### DBRef 和 内嵌
关于我们在设计document时候，什么情况下采用内嵌，什么情况下采用DBRef呢
从对象的角度看
mongodb 单个document限制大小为16m，所以内嵌的数组不能过大
1. 采用内嵌的情况
  - 如果是线性细节对象，优先考虑内嵌
  - 一个对象对另外一个对象是包含关系，采用内嵌
  - 几个简单的对象，可以单独作为一个collection
  - 性能关系，内嵌性能最好
2. 采用引用的情况
 - 当内嵌的数组存在无限增长（或者很大1000以上），这种存在超过16m的限制，采用引用的方式，在多的一方记录一的一方
 - 当一个对象需要单独来处理的时候
 
从DDD的角度来看
我的个人理解对aggregate的理解是
 - 聚合作为一种边界，主要用于维护业务完整性，此时应遵循业务规则中定义的不变量
 - 作为聚合边界内的非聚合根实体对象，若可能被别的调用者单独调用，则应该作为单独的聚合分离出来
 - 在聚合边界内的非聚合根对象，与聚合根之间应该存在直接或间接的引用关系，且可以通过对象的引用方式；若必须采用Id来引用，则说明被引用的对象不属于该聚合
 - 若一个对象缺少另一个对象作为其主对象就不可能存在，则该对象一定属于该主对象的聚合边界内
 - 若一个实体对象，可能被多个聚合引用，则该实体对象应首先考虑作为单独的聚合
 
例如：账单与保证金，账单与保证金没有强约束性关系，账单可以没有保证金，保证金可以单独出来查询，所以两者是两个aggregate
    如果账单脱离BillOrigin，那么账单就没有意义了，至少要知道账单是那个对象账单，当然设计的时候也可以考虑部分不经常变更的数据冗余内嵌
1. 下面是内嵌和DBRef性能测试
* 插入100万条数据

    |    内嵌      |     DBRef     |    MySQL     |
    | :----------:| :-----------: | :----------: |
    | 225114ms    | 631095ms      |   940519ms   |
    | 229159ms    | 648557ms      |   952588ms   |
    | 233977ms    | $1      |   7   |
* 查询100万条数据

    |    内嵌    |  DBRef    |    MySQL   |
    | :--------:| :-------: | :--------: |
    | 18528ms    | 410950ms |   465746ms |
    | 19874ms    | 412671ms |   492935ms |
    | 19874ms    | $1      |    476925ms |
* 分页查询100条数据

    |    内嵌   |  DBRef  |    MySQL |
    | :-------:| :------:| :-------:|
    | 108ms    | 252ms   |   479ms  |
    | 107ms    | 240ms   |   493ms  |
    | 102ms    | 257ms   |   541ms  |
* 查询一条数据

    |  内嵌  | DBRef  |  MySQL  |
    | :-----:| :-----:| :------:|
    | 64ms   | 70ms   |   71ms  |
    | 59ms   | 62ms   |   64ms  |
    | 60ms   | 71ms   |   68ms  |
以上基于本地测试的数据，内嵌的性能在数据量比较大的时候有很大的优势
而且内嵌可以实现对内嵌数组进行查询，可以建立数组内的索引，而引用关联则不可以
``` java
 List<User> findByAddress_DetailAddress(String address);
 
 QUser user = QUser.user;
 userRepository.findAll(user.address.detailAddress.eq("科技大厦"));
```

### MongoDB数据迁移
我们在使用MySQL的时候，数据迁移脚本使用flyway在项目启动的时候来完成迁移，而在MongoDB中我们使用Mongobee来项目启动时完成数据迁移
pom文件中加入相关的依赖
``` pom
<dependency>
    <groupId>com.github.mongobee</groupId>
    <artifactId>mongobee</artifactId>
    <version>0.13</version>
    <exclusions>
        <exclusion>
            <groupId>org.mongodb</groupId>
            <artifactId>mongo-java-driver</artifactId>
        </exclusion>
    </exclusions>
</dependency>
``` 
添加config
``` java
@Configuration
@EnableConfigurationProperties(MongoProperties.class)
public class MongobeeConfig {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MongoProperties properties;

    @Bean
    public Mongobee mongobee() {
        Mongobee mongobee = new Mongobee(properties.getUri());
        mongobee.setChangeLogsScanPackage("com.kinsey.mongodemo.shell"); //扫描changesets的类
        mongobee.setMongoTemplate(mongoTemplate);
        return mongobee;
    }
}
```
数据具体迁移实现
在迁移的class上必须加上`@ChangeLog`注解，每个方法上就是我们具体迁移的实现
`@ChangeLog` 参数 order表示 多个changelog的时候执行顺序
`@ChangeSet`注解作用在每个方法上，参数有
 - order 每个方法的执行顺序，可以是字符串，数字，时间
 - id change set的名称，必须唯一
 - author 更改人
 - runAlways 可选 默认false 表示是否可以始终运行，就是每次启动都会去运行
mongobee 提供了如下的方式
``` java
@ChangeSet(order = "001", id = "someChangeWithoutArgs", author = "testAuthor")
public void someChange1() {
   // method without arguments can do some non-db changes
}

@ChangeSet(order = "002", id = "someChangeWithMongoDatabase", author = "testAuthor")
public void someChange2(MongoDatabase db) {
  // type: com.mongodb.client.MongoDatabase : original MongoDB driver v. 3.x, operations allowed by driver are possible
  // example: 
  MongoCollection<Document> mycollection = db.getCollection("mycollection");
  Document doc = new Document("testName", "example").append("test", "1");
  mycollection.insertOne(doc);
}

@ChangeSet(order = "003", id = "someChangeWithDb", author = "testAuthor")
public void someChange3(DB db) {
  // This is deprecated in mongo-java-driver 3.x, use MongoDatabase instead
  // type: com.mongodb.DB : original MongoDB driver v. 2.x, operations allowed by driver are possible
  // example: 
  DBCollection mycollection = db.getCollection("mycollection");
  BasicDBObject doc = new BasicDBObject().append("test", "1");
  mycollection .insert(doc);
}

@ChangeSet(order = "004", id = "someChangeWithJongo", author = "testAuthor")
public void someChange4(Jongo jongo) {
  // type: org.jongo.Jongo : Jongo driver can be used, used for simpler notation
  // example:
  MongoCollection mycollection = jongo.getCollection("mycollection");
  mycollection.insert("{test : 1}");
}

@ChangeSet(order = "005", id = "someChangeWithSpringDataTemplate", author = "testAuthor")
public void someChange5(MongoTemplate mongoTemplate) {
  // type: org.springframework.data.mongodb.core.MongoTemplate
  // Spring Data integration allows using MongoTemplate in the ChangeSet
  // example:
  mongoTemplate.save(myEntity);
}

@ChangeSet(order = "006", id = "someChangeWithSpringDataTemplate", author = "testAuthor")
public void someChange5(MongoTemplate mongoTemplate, Environment environment) {
  // type: org.springframework.data.mongodb.core.MongoTemplate
  // type: org.springframework.core.env.Environment
  // Spring Data integration allows using MongoTemplate and Environment in the ChangeSet
}
```

在它提供的方法中 我个人认为还是采用`MongoTemplate`来进行编写更加方便，因为在我们平常使用的Spring data mongodb 也是在`MongoTemplate`
上做了一层封装。例如下面`someChange1`方法中我们在user的一条记录增加一个action字段，在`someChange2`删除action字段
``` java
@com.github.mongobee.changeset.ChangeLog
public class ChangeLog {

    @ChangeSet(order = "001", id = "addActionAndUpdate", author = "testAuthor")
    public void someChange1(MongoTemplate mongoTemplate) {
        User user = mongoTemplate.findById("5b7cd8c70cf6500a0d4545d8", User.class);
        user.setName("kinsey-test");
        user.setAction("IN");
        mongoTemplate.save(user);
    }


    @ChangeSet(order = "002", id = "removeAction", author = "testAuthor")
    public void someChange2(MongoTemplate mongoTemplate) {
        User user = mongoTemplate.findById("5b7cd8c70cf6500a0d4545d8", User.class);
        user.setName("kinsey-1");
        user.setAction(null);
        mongoTemplate.save(user);
    }

}
``` 

### MongoDB索引
#### Spring Data MongoDB 创建索引
1. 在相应的property上加上@Indexed注解就可以创建索引
2. @Indexed 常用的可选参数有 name, direction，expireAfterSeconds，sparse，unique
  - name 顾名思义 索引名称;
  - direction 排序规则 IndexDirection.ASCENDING 升序 IndexDirection.DESCENDING 降序;
  - expireAfterSeconds 过期时间索引(TTL) 在一段时间后会过期的索引，在索引过期后，相应的数据会被删除，适合存储在一段时间之后会失效的数据;
  - sparse 稀疏索引 只包含有索引字段的文档的条目，即使索引字段包含一个空值，也就是说稀疏索引可以跳过那些索引键不存在的文档，因为他并非包含所有的文档;
  - unique 唯一索引 唯一索引可以确保集合的每个文档的指定键都有唯一值
3. 创建组合索引 
  @CompoundIndexes({@CompoundIndex(name = "name_age", def = "{'name' : 1, 'age': 1}")})
  def 就是组合字段 value 1 代表升序 -1 代表降序

#### 复合索引
1. 在多个键上建立的索引就是复合索引，有时候我们的查询不是单条件的，可能是多条件，比如查找年龄在10-20名字叫‘Tom’的用户，那么我们可以建立“age”和“name”  的联合索引来加速查询
2. 先插入100万条数据，然后创建三个索引
```
db.user.ensureIndex({"age":1});
db.user.ensureIndex({"name":1,"age":1});
db.user.ensureIndex({"age":1,"name":1});
```

 - 使用hint()强制走指定的索引，explain("executionStats")查看执行计划,json太长，筛选出重点
 
```
db.user.find({"age":{"$gte":10,"$lte":12},"name":"Tom1"}).hint({"age":1}).explain("executionStats");
...
"executionStats" : {
        "executionSuccess" : true, 
        "nReturned" : 100000.0, //返回条目
        "executionTimeMillis" : 425.0, //整体耗时
        "totalKeysExamined" : 300000.0, 
        "totalDocsExamined" : 300000.0, //文档扫描条目
        "executionStages" : {
            "stage" : "FETCH", //查询类型 COLLSCAN是全表扫描，FETCH + IXSCAN 索引扫描+根据索引去检索指定document              
            "nReturned" : 100000.0, 
            "executionTimeMillisEstimate" : 386.0, //检索document获得数据的耗时
            ...
            "inputStage" : {
                "stage" : "IXSCAN", 
                "nReturned" : 300000.0, 
                "executionTimeMillisEstimate" : 133.0, //扫描300000行索引耗时
                ...
                
db.user.find({"age":{"$gte":10,"$lte":12},"name":"Tom1"}).hint({"age":1,"name":1}).explain("executionStats");
...
"executionStats" : {
        "executionSuccess" : true, 
        "nReturned" : 100000.0, 
        "executionTimeMillis" : 151.0, 
        "totalKeysExamined" : 100001.0, 
        "totalDocsExamined" : 100000.0, 
        "executionStages" : {
            "stage" : "FETCH", 
            "nReturned" : 100000.0, 
            "executionTimeMillisEstimate" : 125.0, 
            ...
            "inputStage" : {
                "stage" : "IXSCAN", 
                "nReturned" : 100000.0, 
                "executionTimeMillisEstimate" : 52.0, 
            ...
```

可见复合索引能够大幅度提高查询速度，所以多条件查询下，应正确的使用复合索引

1. 删除原来的索引，创建 name age source 组合索引

``` sql
//1
db.user.dropIndexes();
db.user.ensureIndex({'name':1,'age':1,'source':1})

db.user.find({"name":"Tom1"}).explain("executionStats"); //走索引
db.user.find({"age":18}).explain("executionStats"); //不走
db.user.find({"source":{"$gte":85}}).explain("executionStats"); //不走
db.user.find({"name":"Tom1","source":{"$gte":85}}).explain("executionStats"); //走索引
db.user.find({"age":18,"source":{"$gte":85}}).explain("executionStats"); //不走
db.user.find({"source":{"$gte":85}}).sort({"age":1}).explain("executionStats"); //不走
db.user.find({"name":"Tom1").sort({"source":1}).explain("executionStats"); //走索引
db.user.find({"age":18}).sort({"name":1}).explain("executionStats"); //走索引

db.user.find().sort({"name":1,"age":1}).explain("executionStats"); //走索引
db.user.find().sort({"age":1,"name":1}).explain("executionStats"); //不走索引
db.user.find().sort({"name":1,"age":-1}).explain("executionStats"); //走索引

//2
db.user.dropIndexes();
db.user.ensureIndex({"name":1,"age":1});
db.user.ensureIndex({"age":1,"name":1});

db.user.find({"age":{"$gte":12.0,"$lte":15.0}}).sort({"name":1}).limit(100).hint({"age":1,"name":1}).explain("executionStats");
...
    "nReturned" : 100.0, 
    "executionTimeMillis" : 821.0, 
    "totalKeysExamined" : 400000.0, 
    "totalDocsExamined" : 400000.0, 
    
db.user.find({"age":{"$gte":12.0,"$lte":15.0}}).sort({"name":1}).limit(100).hint({"name":1,"age":1}).explain("executionStats");
...
    "executionSuccess" : true, 
    "nReturned" : 100.0, 
    "executionTimeMillis" : 339.0, 
    "totalKeysExamined" : 200100.0, 
    "totalDocsExamined" : 200100.0, 
```
 - 由此可见走索引条件 query条件或者sort里面必须有 name的查询条件,find()内的顺序无关
 - 多个索引上排序问题 {'name' : 1, 'age': 1} 可以支持的排序是 {'name' : 1, 'age': 1} 和 {'name' : -1, 'age': -1}，但是不支持{'age' : 1, 'name': 1} 和
{'name' : 1, 'age': -1}; 所以排序key的顺序必须和它们在索引中的排列顺序一致，必须和索引中的对应key的排序顺序 完全相同,或者完全相反
 - 复合索引中还有一种情况 有对一个键排序并只要有限个结果的情景 基于排序键的索引，效果比较好

#### TTL索引
 - TTL索引是特殊的单字段索引，MongoDB可以用来在一定时间之后或者在一个特定的时钟时间自动删除集合中的文档
 - TTL索引是单字段索引，复合索引不支持TTL并且会忽略expireAfterSeconds选项. 字段必须是date 或者包含date的数组才有效
 - 例如：db.user.createIndex({"createDate":1},{expireAfterSeconds:10})
TTL索引在索引字段值的时间经过特定秒数的时间之后，TTL索引会将文档进行过期操作，如果该字段是一个数组，并且在索引中有多个数据值，MongoDB使用数组中的最低（例如，最早的）日期值来计算过期阈值。
 - TTL索引无法保证过期数据会在过期之后马上被删除。在文档过期时间和MongoDB从数据库中删除文档之间可能会有一段时间的延迟。
   删除过期文档的后台进程每60秒运行一次。因此，文档在文档的过期时间段和后台任务运行的时间段之间可能还会保存在集合中。

#### 稀疏索引
在异构数据文档中，稀疏索引发挥很大的作用，只包含有索引字段的文档的条目，即使索引字段包含一个空值。也就是稀疏索引可以跳过那些索引键不存在的文档。
这样的好处就是在不造成索引空间浪费的前提下提高检索效率，节省了空间提高了效率
创建稀疏索引 db.user.createIndex({ source: 1 } , { sparse: true })

```
# 创建异构数据
db.user.insertMany([
    { "_id" : ObjectId("523b6e32fb408eea0eec2647"), "name" : "tom" },
    { "_id" : ObjectId("523b6e61fb408eea0eec2648"), "name" : "king", "age" : 12 },
    { "_id" : ObjectId("523b6e6ffb408eea0eec2649"), "name" : "nina", "age" : 18 }
    ])
db.user.createIndex({ age: 1 } , { sparse: true });
# 查询age
db.user.find({"age":{$lt:30}}).explain("executionStats") //走索引
# 返回结果，只返回含有age的数据
{ "_id" : ObjectId("523b6e61fb408eea0eec2648"), "name" : "king", "age" : 12 },
{ "_id" : ObjectId("523b6e6ffb408eea0eec2649"), "name" : "nina", "age" : 18 }
# 没有查询条件，仅排序
db.user.find().sort({"age":1}).explain("executionStats") //不走索引
# 返回结果发现全部返回，并没有走索引
{ "_id" : ObjectId("523b6e32fb408eea0eec2647"), "name" : "tom" },
{ "_id" : ObjectId("523b6e61fb408eea0eec2648"), "name" : "king", "age" : 12 },
{ "_id" : ObjectId("523b6e6ffb408eea0eec2649"), "name" : "nina", "age" : 18 }
```
                                          

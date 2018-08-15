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
注解的处理，重写`AbstractMongoEventListener`的`onBeforeConvert`方法，利用Spring返回工具类`ReflectionUtils.FieldCallback`，
重写doWith方法，在doWith方法里面找这个Field上是否有对应的注解(CascadeSave和DBRef)，然后`mongoOperations`进行持久化，只需在启动注入`CascadeSaveMongoEventListener`即可
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
```
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
Repository很简单，继承`QuerydslPredicateExecutor<T>`即可
```
@Repository
public interface UserRepository extends MongoRepository<User, String>, QuerydslPredicateExecutor<User> {
    User findByAddress_DetailAddress(String address);
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
``` json
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
```
# 1
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

# 2
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
 - 由此可见走索引条件 query条件或者sort里面必须有 name的查询条件
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
                                          
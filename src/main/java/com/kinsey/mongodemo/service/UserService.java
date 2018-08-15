package com.kinsey.mongodemo.service;

import com.kinsey.mongodemo.entity.Address;
import com.kinsey.mongodemo.entity.User;
import com.kinsey.mongodemo.entity.repository.UserRepository;
import com.kinsey.mongodemo.model.PageVO;
import com.kinsey.mongodemo.model.PersonFilter;
import com.kinsey.mongodemo.model.PersonModel;
import com.kinsey.mongodemo.model.StatisticsModel;
import com.querydsl.core.BooleanBuilder;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.project;


/**
 * Created by zj on 2018/8/6
 */
@Service
@AllArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final MongoTemplate mongoTemplate;

    public void createPerson(User person) {
        userRepository.save(person);
    }

    public PageVO<PersonModel> list(Pageable pageable, PersonFilter filter) {
        BooleanBuilder builder = filter.toExpression();
        Page<User> page = userRepository.findAll(builder, pageable);
        List<PersonModel> models = page.getContent().stream().map(PersonModel::new).collect(Collectors.toList());
        return PageVO.build(page, models);
    }

    public PersonModel findById(String id) {
        Optional<User> userOptional = userRepository.findById(id);
        return userOptional.map(PersonModel::new).orElse(null);
    }

    /**
     * 根据年龄分组
     */
    public void queryGroupByAge() {
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
        System.out.println(results);
    }

    /**
     * 根据年龄分组统计分数
     */
    public void queryGroupByAgeCountSource() {
        //条件
        MatchOperation matchOperation = Aggregation.match(Criteria.where("address.$detailAddress").in("就不猜"));
        //排序
        SortOperation sortOperation = Aggregation.sort(Sort.Direction.DESC, "age");
        //分组
        GroupOperation groupOperation = Aggregation.group("age").sum("source").as("totalSource").first("age").as("age");
        //接收字段
        ProjectionOperation projectionOperation = project("age").andExpression("totalSource*[0]", 10).as("source");

        Aggregation aggregation = Aggregation.newAggregation(matchOperation, groupOperation, projectionOperation, sortOperation);
        List<StatisticsModel> results = mongoTemplate.aggregate(aggregation, User.class, StatisticsModel.class).getMappedResults();
        System.out.println(results);
    }


    /**
     * 159502 ms
     */
    public void testDBref(){

        Instant s = Instant.now();
        List<User> list = new ArrayList<>();
        for(int i = 0; i<1000000; i++){
            User user = new User();
            user.setAge(10);
            user.setEmail("12344");
            user.setName("sfdgffdg");
            user.setTel("1273824");
            user.setSource(89);
            Address address = new Address();
            address.setArea("sadsf");
            address.setCity("sadsd");
            address.setDetailAddress("juer");
            address.setProvince("hsdjhaseuw");
            user.setAddress(address);
            list.add(user);
        }
        userRepository.saveAll(list);
        Instant now = Instant.now();
        System.err.println(Duration.between(s,now).toMillis());

    }

    /**
     * 349922 ms
     * @return
     */
    public List<User> testDbrefQuery(){
        Instant s = Instant.now();
        List<User> all = userRepository.findAll();
        Instant now = Instant.now();
        System.err.println(Duration.between(s,now).toMillis());
        return all;
    }

    public User testDbrefFindOne(){
        Instant s = Instant.now();
        Optional<User> all = userRepository.findById("");
        Instant now = Instant.now();
        System.err.println(Duration.between(s,now).toMillis());
        return all.get();
    }
}

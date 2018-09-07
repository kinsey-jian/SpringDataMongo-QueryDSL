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

import java.time.LocalDateTime;
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
        userRepository.insert(person);
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

    public void deleteAll(){
        userRepository.deleteAll();
    }

    /**
     * 根据年龄分组
     */
    public void queryGroupByAge() {
        userRepository.queryGroupByAge();
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

}

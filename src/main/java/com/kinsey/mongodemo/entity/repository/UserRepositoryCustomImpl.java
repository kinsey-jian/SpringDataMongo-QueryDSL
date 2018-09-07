package com.kinsey.mongodemo.entity.repository;

import com.kinsey.mongodemo.entity.User;
import com.kinsey.mongodemo.model.StatisticsModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.project;

/**
 * Created by zj on 2018/9/7
 */
public class UserRepositoryCustomImpl implements UserRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
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
        System.out.println(results+"111111");
    }
}

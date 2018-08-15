package com.kinsey.mongodemo.entity.repository;

import com.kinsey.mongodemo.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

/**
 * Created by zj on 2018/8/6
 */
@Repository
public interface UserRepository extends MongoRepository<User, String>, QuerydslPredicateExecutor<User> {
    User findByAddress_DetailAddress(String address);
}

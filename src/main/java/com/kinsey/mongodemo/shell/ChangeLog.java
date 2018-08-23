package com.kinsey.mongodemo.shell;

import com.github.mongobee.changeset.ChangeSet;
import com.kinsey.mongodemo.entity.User;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Arrays;

/**
 * Created by zj on 2018/8/22
 */
@com.github.mongobee.changeset.ChangeLog
public class ChangeLog {

    @ChangeSet(order = "001", id = "addActionAndUpdate", author = "testAuthor")
    public void someChange1(MongoTemplate mongoTemplate) {
        User user = mongoTemplate.findById("5b7e13fd0cf6502655984bce", User.class);
        user.setName("kinsey-1");
        user.setAction(Arrays.asList("IN", "OUT"));
        mongoTemplate.save(user);
    }


    @ChangeSet(order = "002", id = "removeAction", author = "testAuthor")
    public void someChange2(MongoTemplate mongoTemplate) {
        User user = mongoTemplate.findById("5b7e13fd0cf6502655984bce", User.class);
        user.setName("kinsey-2");
        user.setAction(null);
        mongoTemplate.save(user);
    }

}

package com.kinsey.mongodemo.service;

import com.kinsey.mongodemo.entity.Address;
import com.kinsey.mongodemo.entity.User;
import com.kinsey.mongodemo.entity.repository.UserRepository;
import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;


@RunWith(SpringRunner.class)
@SpringBootTest
public class UserServiceTest {

    @Autowired
    private UserRepository userRepository;

    @Before
    public void before() throws Exception {
        List<User> list = new ArrayList<>();
        for(int i = 0; i<10000; i++){
            User user = new User();
            user.setAge(10+i%10);
            user.setEmail("888888@ku.com");
            user.setName("Tom"+i%10);
            user.setTel("1273824");
            user.setSource(88+i%10);
            Address address = new Address();
            address.setArea("滨江");
            address.setCity("杭州");
            address.setDetailAddress("科技大厦");
            address.setProvince("浙江");
            user.setAddress(address);
            list.add(user);
        }
        userRepository.saveAll(list);
    }


    @Test
    public void testQueryGroupByAgeCountSource() throws Exception {
    }


    @Test
    public void testTestDBref() throws Exception {
    }


    @Test
    public void testTestDbrefQuery() throws Exception {
    }


    @Test
    public void testTestDbrefFindOne() throws Exception {
    }


} 

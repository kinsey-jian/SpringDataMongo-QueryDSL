package com.kinsey.mongodemo.service;

import com.kinsey.mongodemo.entity.Address;
import com.kinsey.mongodemo.entity.QAddress;
import com.kinsey.mongodemo.entity.QUser;
import com.kinsey.mongodemo.entity.User;
import com.kinsey.mongodemo.entity.repository.UserRepository;
import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


@RunWith(SpringRunner.class)
@SpringBootTest
public class UserServiceTest {

    @Autowired
    private UserRepository userRepository;

    /**
     * 内嵌 225114ms 229159ms 233977ms
     *
     * DBRef 631095ms 648557ms
     * @throws Exception
     */
    @Test
    public void testSave1() throws Exception {
        Instant s = Instant.now();
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
        System.out.println(Duration.between(s,Instant.now()).toMillis());
    }

    /**
     * 内嵌 18528ms 19874ms 18891ms
     * DBRef 410950ms
     * @throws Exception
     */
    @Test
    public void testQueryAll1() throws Exception {
        Instant s = Instant.now();
        PageRequest pageRequest = PageRequest.of(0, 100);
        userRepository.findAll(pageRequest);
        System.out.println(Duration.between(s,Instant.now()).toMillis());
    }

    /**
     * 内嵌 108ms 107ms 102ms
     * DBRef 252ms 240ms 257ms
     * @throws Exception
     */
    @Test
    public void testQueryPage() throws Exception {
        Instant s = Instant.now();
        PageRequest pageRequest = PageRequest.of(0, 100);
        userRepository.findAll(pageRequest);
        System.out.println(Duration.between(s,Instant.now()).toMillis());
    }

    /**
     * 内嵌 64ms 59ms 60ms
     * DBRef 70ms 62ms 71ms
     * @throws Exception
     */
    @Test
    public void testQueryOne1() throws Exception {
        Instant s = Instant.now();
        userRepository.findById("5b74456daa071703b49fa045");
        System.out.println(Duration.between(s,Instant.now()).toMillis());
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

    @Test
    public void testQuery() {
        Instant s = Instant.now();
        List<User> users = userRepository.findByAddress_DetailAddress("科技大厦");
        System.out.println(Duration.between(s,Instant.now()).toMillis());
    }

    @Test
    public void testQueryDsl() {
        Instant s = Instant.now();
        QUser user = QUser.user;
        QAddress address = QAddress.address;
        Iterable<User> 科技大厦 = userRepository.findAll(address.detailAddress.eq("科技大厦"));
        //List<User> users = userRepository.findByAddress_DetailAddress("科技大厦");
        System.out.println(Duration.between(s,Instant.now()).toMillis());
    }
} 

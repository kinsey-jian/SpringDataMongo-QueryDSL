package com.kinsey.mongodemo.service;

import com.kinsey.mongodemo.entity.Address;
import com.kinsey.mongodemo.entity.QAddress;
import com.kinsey.mongodemo.entity.QUser;
import com.kinsey.mongodemo.entity.User;
import com.kinsey.mongodemo.entity.repository.UserRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@RunWith(SpringRunner.class)
@SpringBootTest
public class UserServiceTest {

    @Autowired
    private UserRepository userRepository;

    /**
     * 内嵌 225114ms 229159ms 233977ms
     * DBRef 631095ms 648557ms
     * MySQL 940519ms 952588ms
     * @throws Exception
     */
    @Test
    public void testSave1() throws Exception {
        Instant s = Instant.now();
        List<User> list = new ArrayList<>();
        for (int i = 0; i < 1000000; i++) {
            User user = new User();
            user.setAge(10 + i % 10);
            user.setEmail("888888@ku.com");
            user.setName("Tom" + i % 10);
            user.setTel("1273824");
            user.setSource(88 + i % 10);
            Address address = new Address();
            address.setArea("AAA");
            address.setCity("BBB");
            address.setDetailAddress("CCC");
            address.setProvince("DD");
            user.setAddress(address);
            list.add(user);
        }
        userRepository.saveAll(list);
        System.out.println(Duration.between(s, Instant.now()).toMillis());
    }

    /**
     * 内嵌 18528ms 19874ms 18891ms
     * DBRef 410950ms 412671ms
     * MySQL 465746ms 492935ms 476925ms
     * @throws Exception
     */
    @Test
    public void testQueryAll1() throws Exception {
        Instant s = Instant.now();
        userRepository.findAll();
        System.out.println(Duration.between(s, Instant.now()).toMillis());
    }

    /**
     * 内嵌 108ms 107ms 102ms
     * DBRef 252ms 240ms 257ms
     * MySQL 479ms 493ms 541ms
     * @throws Exception
     */
    @Test
    public void testQueryPage() throws Exception {
        Instant s = Instant.now();
        PageRequest pageRequest = PageRequest.of(0, 100);
        userRepository.findAll(pageRequest);
        System.out.println(Duration.between(s, Instant.now()).toMillis());
    }

    /**
     * 内嵌 64ms 59ms 60ms
     * DBRef 70ms 62ms 71ms
     * MySQL 71ms 64ms 68ms
     * @throws Exception
     */
    @Test
    public void testQueryOne1() throws Exception {
        Instant s = Instant.now();
        userRepository.findById("5b76691daa07172c3909c6d6");
        System.out.println(Duration.between(s, Instant.now()).toMillis());
    }

    @Test
    public void testDelete() throws Exception {
        userRepository.deleteById("5b76691daa07172c3909c6d6");
    }


    @Test
    public void testUpdate() throws Exception {
        Optional<User> optionalUser = userRepository.findById("5b76691daa07172c3909c6d9");
        optionalUser.ifPresent(u -> {
            u.setName("狗蛋");
            Address address = u.getAddress();
            address.setDetailAddress("好玩的");
            u.setAddress(address);
            userRepository.save(u);
        });
    }


    @Test
    public void testQuery() {
        Instant s = Instant.now();
        List<User> users = userRepository.findByAddress_DetailAddress("科技大厦");
        System.out.println(Duration.between(s, Instant.now()).toMillis());
    }

    @Test
    public void testQueryDsl() {
        Instant s = Instant.now();
        QUser user = QUser.user;
        QAddress address = QAddress.address;
        Iterable<User> users = userRepository.findAll(user.address.detailAddress.eq("科技大厦"));
        //List<User> users = userRepository.findByAddress_DetailAddress("科技大厦");
        System.out.println(Duration.between(s, Instant.now()).toMillis());
    }
} 

package com.kinsey.mongodemo.service;

import com.kinsey.mongodemo.entity.Address;
import com.kinsey.mongodemo.entity.QAddress;
import com.kinsey.mongodemo.entity.QUser;
import com.kinsey.mongodemo.entity.User;
import com.kinsey.mongodemo.entity.repository.UserRepository;
import org.databene.contiperf.PerfTest;
import org.databene.contiperf.junit.ContiPerfRule;
import org.junit.Rule;
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

    @Rule
    public ContiPerfRule i = new ContiPerfRule();
    /**
     * 内嵌 224727ms 229159ms 222713ms
     * DBRef 631095ms 648557ms 673470ms
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
     * 内嵌 18528ms 18694ms 18945ms
     * DBRef 410950ms 412671ms 417126ms
     * MySQL 465746ms 492935ms 476925ms
     * @throws Exception
     */
    @Test
    @PerfTest(threads = 20, invocations = 20)
    public void testQueryAll1() throws Exception {
        Instant s = Instant.now();
        userRepository.findAll();
        System.out.println(Duration.between(s, Instant.now()).toMillis());
    }

    /**
     * 内嵌 82ms 85ms 80ms
     * DBRef 202ms 211ms 213ms
     * MySQL 479ms 493ms 541ms
     * @throws Exception
     */
    @Test
    @PerfTest(threads = 200, invocations = 200)
    public void testQueryPage() throws Exception {
        Instant s = Instant.now();
        PageRequest pageRequest = PageRequest.of(0, 100);
        userRepository.findAll(pageRequest);
        System.out.println(Duration.between(s, Instant.now()).toMillis());
    }

    /**
     * 内嵌 43ms 42ms 43ms
     * DBRef 49ms 45ms 47ms
     * MySQL 71ms 64ms 68ms
     * @throws Exception
     */
    @Test
    @PerfTest(threads = 200,duration = 10000,rampUp = 10, warmUp = 9000)
    public void testQueryOne1() throws Exception {
        Instant s = Instant.now();
        userRepository.findById("5b7ec1bd3f17d504599d80a3");
        System.out.println(Duration.between(s, Instant.now()).toMillis());
    }

    @Test
    public void testDelete() throws Exception {
        userRepository.deleteById("5b7eb0b83f17d50292c21c0a");
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
        /*Instant s = Instant.now();
        List<User> users = userRepository.findByAddress_DetailAddress("科技大厦");
        System.out.println(Duration.between(s, Instant.now()).toMillis());*/
        userRepository.queryGroupByAge();
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

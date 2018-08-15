package com.kinsey.mongodemo.model;

import com.kinsey.mongodemo.entity.User;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

/**
 * Created by zj on 2018/8/6
 */
@Getter
@Setter
public class PersonModel implements Serializable {

    private String id;

    private String name;

    private Integer age;

    private String email;

    private String tel;

    private String province;

    private String city;

    private String area;

    private String detailAddress;

    public PersonModel(User person) {
        this.id = person.getId();
        this.name = person.getName();
        this.age = person.getAge();
        this.email = person.getEmail();
        this.tel = person.getTel();
        if (Objects.nonNull(person.getAddress())) {
            this.province = person.getAddress().getProvince();
            this.city = person.getAddress().getCity();
            this.area = person.getAddress().getArea();
            this.detailAddress = person.getAddress().getDetailAddress();
        }
    }
}

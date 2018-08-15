package com.kinsey.mongodemo.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Created by zj on 2018/8/6
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonRequest {

    private String name;

    private String email;

    private String tel;

    private int age;

    private double source;

    private Address address;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Address{
        private String province;

        private String city;

        private String area;

        private String detailAddress;
    }
}

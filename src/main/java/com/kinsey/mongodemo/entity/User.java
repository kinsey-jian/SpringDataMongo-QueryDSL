package com.kinsey.mongodemo.entity;

import com.kinsey.mongodemo.annotation.CascadeSave;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.IndexDirection;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Created by zj on 2018/8/6
 */
@Getter
@Setter
@Document
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndexes({@CompoundIndex(name = "idx_name_age", def = "{'name' : 1, 'age': 1}")})
public class User implements Serializable {

    @Id
    private String id;

    private String name;

    private Integer age;

    @Indexed(direction = IndexDirection.ASCENDING)
    private String email;

    @Indexed(direction = IndexDirection.ASCENDING)
    private String tel;

    private double source;

    @DBRef
    @CascadeSave
    private Address address;

    @Version
    private Long version;

    @CreatedDate
    private LocalDateTime createDate;

    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    @CreatedBy
    private UserVO createdUser;

    @LastModifiedBy
    private UserVO lastModifiedUser;

    private List<String> action;
}
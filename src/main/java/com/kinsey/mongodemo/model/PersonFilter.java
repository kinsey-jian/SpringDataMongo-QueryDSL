package com.kinsey.mongodemo.model;

import com.kinsey.mongodemo.entity.QUser;
import com.querydsl.core.BooleanBuilder;
import lombok.*;
import org.springframework.util.StringUtils;

/**
 * Created by zj on 2018/8/6
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonFilter {

    private String search;

    public BooleanBuilder toExpression() {
        BooleanBuilder builder = new BooleanBuilder();
        QUser user = QUser.user;
        if(!StringUtils.isEmpty(search)){
            builder.and(user.email.contains(search).or(user.tel.contains(search)));
        }
        return builder;
    }
}

package com.kinsey.mongodemo.mapper;

import com.kinsey.mongodemo.entity.User;
import com.kinsey.mongodemo.model.PersonRequest;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * Created by zj on 2018/8/6
 */
@Mapper
public interface PersonMapper {
    static PersonMapper getInstance() {
        return Mappers.getMapper(PersonMapper.class);
    }
    User PersonRequestToPerson(PersonRequest request);
}

package com.kinsey.mongodemo.config;

import com.kinsey.mongodemo.entity.UserVO;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

/**
 * Created by zj on 2018/8/8
 */
@Configuration
public class UserAuditor implements AuditorAware<UserVO> {
    @Override
    public Optional<UserVO> getCurrentAuditor() {
        return Optional.of(new UserVO("123","kinsey"));
    }
}

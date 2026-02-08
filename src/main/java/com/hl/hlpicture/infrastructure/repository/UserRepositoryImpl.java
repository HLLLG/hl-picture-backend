package com.hl.hlpicture.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hl.hlpicture.domain.user.entity.User;
import com.hl.hlpicture.domain.user.repository.UserRepository;
import com.hl.hlpicture.infrastructure.mapper.UserMapper;
import org.springframework.stereotype.Service;

/**
 * 用户仓储实现类
 */
@Service
public class UserRepositoryImpl extends ServiceImpl<UserMapper, User> implements UserRepository {
}

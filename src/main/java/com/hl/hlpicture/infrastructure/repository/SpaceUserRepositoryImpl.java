package com.hl.hlpicture.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hl.hlpicture.domain.space.entity.SpaceUser;
import com.hl.hlpicture.domain.space.repository.SpaceUserRepository;
import com.hl.hlpicture.infrastructure.mapper.SpaceUserMapper;
import org.springframework.stereotype.Service;

/**
 * 空间成员仓储实现类
 */
@Service
public class SpaceUserRepositoryImpl extends ServiceImpl<SpaceUserMapper, SpaceUser> implements SpaceUserRepository {
}

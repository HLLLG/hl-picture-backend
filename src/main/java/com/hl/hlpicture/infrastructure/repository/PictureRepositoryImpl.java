package com.hl.hlpicture.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hl.hlpicture.domain.picture.entity.Picture;
import com.hl.hlpicture.domain.picture.repository.PictureRepository;
import com.hl.hlpicture.infrastructure.mapper.PictureMapper;
import org.springframework.stereotype.Service;

/**
 * 图片仓储实现类
 */
@Service
public class PictureRepositoryImpl extends ServiceImpl<PictureMapper, Picture> implements PictureRepository {
}

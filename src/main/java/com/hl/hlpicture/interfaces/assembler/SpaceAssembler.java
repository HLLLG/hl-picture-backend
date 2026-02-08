package com.hl.hlpicture.interfaces.assembler;

import com.hl.hlpicture.domain.space.entity.Space;
import com.hl.hlpicture.interfaces.dto.space.SpaceAddRequest;
import com.hl.hlpicture.interfaces.dto.space.SpaceEditRequest;
import com.hl.hlpicture.interfaces.dto.space.SpaceUpdateRequest;
import org.springframework.beans.BeanUtils;

/**
 * 空间转换器
 *
 * @author huangli
 * @date 2024/6/17 17:28
 */
public class SpaceAssembler {

    public static Space toSpaceEntity(SpaceAddRequest request) {
        Space space = new Space();
        BeanUtils.copyProperties(request, space);
        return space;
    }

    public static Space toSpaceEntity(SpaceUpdateRequest request) {
        Space space = new Space();
        BeanUtils.copyProperties(request, space);
        return space;
    }

    public static Space toSpaceEntity(SpaceEditRequest request) {
        Space space = new Space();
        BeanUtils.copyProperties(request, space);
        return space;
    }
}

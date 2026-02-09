package com.hl.hlpicture.application.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hl.hlpicture.domain.space.entity.Space;
import com.hl.hlpicture.domain.user.entity.User;
import com.hl.hlpicture.interfaces.dto.space.analyze.*;
import com.hl.hlpicture.interfaces.vo.space.analyze.*;

import java.util.List;

/**
* @author hegl
* @createDate 2026-01-05 01:03:33
*/
public interface SpaceAnalyzeApplicationService extends IService<Space> {

    /**
     * 获取空间使用分析结果
     * @param spaceUsageAnalyzeResponse
     * @param loginUser
     * @return
     */
    SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeResponse, User loginUser);

    /**
     * 获取空间分类使用分析结果
     * @param spaceCategoryAnalyzeRequest
     * @param loginUser
     * @return
     */
    List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser);

    /**
     * 获取空间等级分析结果
     * @param spaceLevelAnalyzeRequest
     * @param loginUser
     * @return
     */
    List<SpaceLevelAnalyzeResponse> getSpaceLevelAnalyze(SpaceLevelAnalyzeRequest spaceLevelAnalyzeRequest, User loginUser);

    /**
     * 获取空间标签使用分析结果
     * @param spaceTagAnalyzeRequest
     * @param loginUser
     * @return
     */
    List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser);

    /**
     * 获取空间大小使用分析结果
     * @param spaceSizeAnalyzeRequest
     * @param loginUser
     * @return
     */
    List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser);

    /**
     * 获取空间用户使用分析结果
     * @param spaceUserAnalyzeRequest
     * @param loginUser
     * @return
     */
    List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser);

    /**
     * 获取空间排名分析结果(仅管理员可用)
     * @param spaceRankAnalyzeRequest
     * @param loginUser
     * @return
     */
    List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser);

}

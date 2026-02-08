package com.hl.hlpicture.application.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hl.hlpicture.application.service.PictureApplicationService;
import com.hl.hlpicture.infrastructure.exception.BusinessException;
import com.hl.hlpicture.infrastructure.exception.ErrorCode;
import com.hl.hlpicture.infrastructure.exception.ThrowUtils;
import com.hl.hlpicture.infrastructure.mapper.SpaceMapper;
import com.hl.hlpicture.interfaces.dto.space.analyze.*;
import com.hl.hlpicture.domain.picture.entity.Picture;
import com.hl.hlpicture.domain.space.entity.Space;
import com.hl.hlpicture.domain.user.entity.User;
import com.hl.hlpicture.interfaces.vo.space.analyze.*;
import com.hl.hlpicture.application.service.SpaceAnalyzeApplicationService;
import com.hl.hlpicture.application.service.SpaceApplicationService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author hegl
 * @createDate 2026-01-05 01:03:33
 */
@Service
public class SpaceAnalyzeApplicationServiceImpl implements SpaceAnalyzeApplicationService {


    @Resource
    private SpaceApplicationService spaceApplicationService;

    @Resource
    private PictureApplicationService pictureApplicationService;


    @Override
    public SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest,
                                                          User loginUser) {
        Long spaceId = spaceUsageAnalyzeRequest.getSpaceId();
        boolean queryPublic = spaceUsageAnalyzeRequest.isQueryPublic();
        boolean queryAll = spaceUsageAnalyzeRequest.isQueryAll();
        if (queryAll || queryPublic) {
            // 查询所有空间和公共空间逻辑
            // 仅管理员可查询
            this.checkSpaceAnalyzeAuth(spaceUsageAnalyzeRequest, loginUser);
            // 构建查询条件，需要到picture中查询
            QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("picSize");
            fillAnalyzeQueryWrapper(spaceUsageAnalyzeRequest, queryWrapper);
            // 查询数据库
            List<Object> objects = pictureApplicationService.getBaseMapper().selectObjs(queryWrapper);
            // 计算总大小
            long usedSize = objects.stream().mapToLong(obj -> (Long) obj).sum();
            long userCount = objects.size();
            SpaceUsageAnalyzeResponse response = new SpaceUsageAnalyzeResponse();
            response.setUsedSize(usedSize);
            response.setUsedCount(userCount);
            // 公共空间和全空间没有最大限制
            response.setMaxSize(null);
            response.setSizeUsageRatio(null);
            response.setMaxCount(null);
            response.setCountUsageRatio(null);
            return response;
        } else {
            // 私有空间分析逻辑
            // 校验参数
            ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR, "空间 ID 不合法");
            // 校验权限
            Space space = spaceApplicationService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 仅空间创建者可查询
            this.checkSpaceAnalyzeAuth(spaceUsageAnalyzeRequest, loginUser);
            // 构造返回结果，私有空间可以直接从space表中获取
            SpaceUsageAnalyzeResponse response = new SpaceUsageAnalyzeResponse();
            response.setUsedSize(space.getTotalSize());
            response.setUsedCount(space.getTotalCount());
            response.setMaxSize(space.getMaxSize());
            response.setMaxCount(space.getMaxCount());
            // 计算使用比例
            double usedSize = NumberUtil.round(space.getTotalSize() * 100.0 / space.getMaxSize(), 2).doubleValue();
            double userCount = NumberUtil.round(space.getTotalCount() * 100.0 / space.getMaxCount(), 2).doubleValue();
            response.setSizeUsageRatio(usedSize);
            response.setCountUsageRatio(userCount);
            return response;
        }
    }

    @Override
    public List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser) {
        // 校验权限
        this.checkSpaceAnalyzeAuth(spaceCategoryAnalyzeRequest, loginUser);
        // 构建查询条件，需要到picture中查询
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        // 根据分析范围填充查询条件
        fillAnalyzeQueryWrapper(spaceCategoryAnalyzeRequest, queryWrapper);
        // 使用 MyBatis-Plus 进行分组查询
        queryWrapper.select("category", "COUNT(*) AS count", "SUM(picSize) AS totalSize");
        queryWrapper.groupBy("category");
        // 查询数据库，并转换结果
        return pictureApplicationService.getBaseMapper().selectMaps(queryWrapper).stream().map(result -> {
            String category = result.get("category") != null ? result.get("category").toString() : "未分类";
            Long count = ((Number) result.get("count")).longValue();
            Long totalSize = ((Number) result.get("totalSize")).longValue();
            return new SpaceCategoryAnalyzeResponse(category, count, totalSize);
        }).collect(Collectors.toList());
    }

    @Override
    public List<SpaceLevelAnalyzeResponse> getSpaceLevelAnalyze(SpaceLevelAnalyzeRequest spaceLevelAnalyzeRequest, User loginUser) {
        // 校验权限，仅管理员可查询
        ThrowUtils.throwIf(!loginUser.isAdmin(), ErrorCode.NO_AUTH_ERROR);
        // 构建查询条件，需要到space中查询
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        // 取前N名
        queryWrapper.select("spaceLevel", "count(*) AS count")
                .groupBy("spaceLevel");
        // 查询数据库
        return this.getBaseMapper().selectMaps(queryWrapper)
                .stream()
                .map(result -> {
                    String spaceLevel = result.get("spaceLevel").toString();
                    Long count = ((Number) result.get("count")).longValue();
                    return new SpaceLevelAnalyzeResponse(spaceLevel, count);
                }).collect(Collectors.toList());
    }

    @Override
    public List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser) {
        // 校验权限
        this.checkSpaceAnalyzeAuth(spaceTagAnalyzeRequest, loginUser);
        // 构建查询条件，需要到picture中查询
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        // 根据分析范围填充查询条件
        fillAnalyzeQueryWrapper(spaceTagAnalyzeRequest, queryWrapper);
        queryWrapper.select("tags");
        // 查询数据库，并统计标签使用情况
        List<String> tagJsonList = pictureApplicationService.getBaseMapper().selectObjs(queryWrapper)
                .stream()
                // 过滤空标签
                .filter(ObjUtil::isNotEmpty)
                // 转换为string数组
                .map(Object::toString)
                .collect(Collectors.toList());
        // 统计标签使用情况
        // 将{[], [], ...} 转换为 Map<String, Long> 形式的标签使用次数统计
        Map<String, Long> tagCountMap = tagJsonList.stream()
                .flatMap(tagJson -> JSONUtil.toList(tagJson, String.class).stream())
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));
        // 转换为响应对象，并降序排序
        return tagCountMap.entrySet().stream()
                .map(entry -> new SpaceTagAnalyzeResponse(entry.getKey(), entry.getValue()))
                .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
                .collect(Collectors.toList());
    }

    @Override
    public List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser) {
        // 校验权限
        this.checkSpaceAnalyzeAuth(spaceSizeAnalyzeRequest, loginUser);
        // 构建查询条件，需要到picture中查询
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        // 根据分析范围填充查询条件
        fillAnalyzeQueryWrapper(spaceSizeAnalyzeRequest, queryWrapper);
        queryWrapper.select("picSize");
        // 查询数据库，并统计大小使用情况
        List<Long> picSizes = pictureApplicationService.getBaseMapper().selectObjs(queryWrapper)
                .stream()
                .filter(ObjUtil::isNotNull)
                .map(obj -> (Long) obj)
                .collect(Collectors.toList());
        // 定义大小区间，并统计各个区间的图片数量（使用有序map）
        Map<String, Long> sizeRanges = new LinkedHashMap<>();
        sizeRanges.put("<100KB", picSizes.stream().filter(picSize -> picSize < 100 * 1024).count());
        sizeRanges.put("100KB-500KB", picSizes.stream().filter(picSize -> picSize >= 100 * 1024 && picSize < 500 * 1024).count());
        sizeRanges.put("500KB-1MB", picSizes.stream().filter(picSize -> picSize >= 500 * 1024 && picSize < 1 * 1024 * 1024).count());
        sizeRanges.put(">1MB", picSizes.stream().filter(picSize -> picSize >= 1 * 1024 * 1024).count());
        // 构造响应对象
        return sizeRanges.entrySet().stream()
                .map(entry -> new SpaceSizeAnalyzeResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser) {
        // 校验权限
        this.checkSpaceAnalyzeAuth(spaceUserAnalyzeRequest, loginUser);
        // 构建查询条件，需要到picture中查询
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        // 根据分析范围填充查询条件
        fillAnalyzeQueryWrapper(spaceUserAnalyzeRequest, queryWrapper);
        // 填充 userId 查询条件
        Long userId = spaceUserAnalyzeRequest.getUserId();
        queryWrapper.eq(ObjUtil.isNotNull(userId), "userId", userId);
        // 分析维度：每日、每周、每月
        String timeDimension = spaceUserAnalyzeRequest.getTimeDimension();
        switch (timeDimension) {
            case "day":
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m-%d') AS period", "COUNT(*) AS count");
                break;
            case "week":
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%u') AS period", "COUNT(*) AS count");
                break;
            case "month":
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m') AS period", "COUNT(*) AS count");
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的时间维度：" + timeDimension);
        }
        // 分组和排序
        queryWrapper.groupBy("period").orderByAsc("period");

        // 查询数据库
        List<Map<String, Object>> selectMaps = pictureApplicationService.getBaseMapper().selectMaps(queryWrapper);
        // 转换结果
        return selectMaps.stream().map(result -> {
            String period = result.get("period").toString();
            Long count = ((Number) result.get("count")).longValue();
            return new SpaceUserAnalyzeResponse(period, count);
        }).collect(Collectors.toList());
    }

    @Override
    public List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest,
                                           User loginUser) {
        // 校验权限，仅管理员可查询
        ThrowUtils.throwIf(!loginUser.isAdmin(), ErrorCode.NO_AUTH_ERROR);
        // 构建查询条件，需要到space中查询
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        // 取前N名
        queryWrapper.select("id", "spaceName", "userId" , "totalSize")
                .orderByDesc("totalSize")
                .last("LIMIT " + spaceRankAnalyzeRequest.getTopN());
        // 查询数据库
        return this.getBaseMapper().selectList(queryWrapper);
    }


    /**
     * 校验空间分析权限
     *
     * @param spaceAnalyzeRequest
     * @param loginUser
     */
    private void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser) {
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        boolean queryPublic = spaceAnalyzeRequest.isQueryPublic();
        boolean queryAll = spaceAnalyzeRequest.isQueryAll();
        // 校验权限
        if (queryAll || queryPublic) {
            // 全空间分析和公共图库分析, 仅管理员和空间创建者可编辑
            ThrowUtils.throwIf(!loginUser.isAdmin(), ErrorCode.NO_AUTH_ERROR);
        } else {
            // 私有空间分析, 仅空间创建者可编辑
            ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR);
            Space space = spaceApplicationService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            spaceApplicationService.checkSpaceAuth(space, loginUser);
        }
    }

    private static void fillAnalyzeQueryWrapper(SpaceAnalyzeRequest spaceAnalyzeRequest,
                                                QueryWrapper<Picture> queryWrapper) {
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        boolean queryPublic = spaceAnalyzeRequest.isQueryPublic();
        boolean queryAll = spaceAnalyzeRequest.isQueryAll();
        if (queryAll) {
            // 全空间分析, 无需添加额外条件
            return;
        }
        if (queryPublic) {
            // 公共图库分析
            queryWrapper.isNull("spaceId");
            return;
        }
        if (spaceId != null && spaceId > 0) {
            // 私有空间分析
            queryWrapper.eq("spaceId", spaceId);
            return;
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "未指定分析空间范围");
    }
}





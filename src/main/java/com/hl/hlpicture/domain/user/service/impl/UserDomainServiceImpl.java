package com.hl.hlpicture.domain.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hl.hlpicture.domain.user.entity.User;
import com.hl.hlpicture.domain.user.repository.UserRepository;
import com.hl.hlpicture.domain.user.service.UserDomainService;
import com.hl.hlpicture.domain.user.valueobject.UserRoleEnum;
import com.hl.hlpicture.infrastructure.exception.BusinessException;
import com.hl.hlpicture.infrastructure.exception.ErrorCode;
import com.hl.hlpicture.infrastructure.exception.ThrowUtils;
import com.hl.hlpicture.interfaces.dto.user.UserQueryRequest;
import com.hl.hlpicture.interfaces.vo.user.LoginUserVO;
import com.hl.hlpicture.interfaces.vo.user.UserVO;
import com.hl.hlpicture.domain.user.valueobject.UserConstant;
import com.hl.hlpicture.share.auth.StpKit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author hegl
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2025-12-16 00:53:25
 */
@Service
@Slf4j
public class UserDomainServiceImpl implements UserDomainService {

    @Resource
    private UserRepository userRepository;

    @Override
    public Long registerUser(String userAccount, String userPassword, String checkPassword) {

        // 2. 检查账户是否重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        Long count = userRepository.getBaseMapper().selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        // 3. 加密密码
        String encryptPassWord = getEncryptPassWord(userPassword);
        // 4， 写入数据库
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassWord);
        user.setUserName("无名");
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean saveResult = userRepository.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
        }
        return user.getId();
    }

    @Override
    public LoginUserVO loginUser(String userAccount, String userPassword, HttpServletRequest request) {

        // 2 加密密码
        String encryptPassWord = getEncryptPassWord(userPassword);
        // 3 根据账号获取用户信息
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        User user = userRepository.getBaseMapper().selectOne(queryWrapper);
        // 不存在，抛异常
        ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR, "账号不存在或密码错误");
        log.info("user login failed, userPassword cannot match userAccount");
        // 4 校验密码
        ThrowUtils.throwIf(!user.getUserPassword().equals(encryptPassWord), ErrorCode.PARAMS_ERROR, "账号不存在或密码错误");
        // 5 更新用户修改时间
        UpdateWrapper<User> updateWrapper = new UpdateWrapper<User>().eq("editTime", DateTime.now());
        userRepository.update(user, updateWrapper);
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);
        // 记录用户登录态到Sa-Token, 便于空间鉴权时使用
        StpKit.SPACE.login(user.getId());
        StpKit.SPACE.getSession().set(UserConstant.USER_LOGIN_STATE, user);
        // 6 脱敏
        return this.getLoginUserVO(user);
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 判断用户是否已登录
        Object attribute = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User curentUser = (User) attribute;
        ThrowUtils.throwIf(curentUser == null || curentUser.getId() == null, ErrorCode.NOT_LOGIN_ERROR);
        // 从数据库中查询（不追求性能，保证数据的最新状态）
        Long userId = curentUser.getId();
        curentUser = userRepository.getById(userId);
        ThrowUtils.throwIf(curentUser == null, ErrorCode.NOT_LOGIN_ERROR);
        // 3 更新用户修改时间
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, curentUser);
        return curentUser;
    }

    @Override
    public boolean loginOutUser(HttpServletRequest request) {
        // 判断用户是否已登录
        Object attribute = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        ThrowUtils.throwIf(attribute == null, ErrorCode.OPERATION_ERROR, "用户未登录");
        // 移除登录态
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return true;
    }

    /**
     * 获取加密后的密码
     *
     * @param password
     * @return
     */
    @Override
    public String getEncryptPassWord(String password) {
        // 加盐，混淆密码
        final String SALT = "hl";
        password = SALT + password;
        return DigestUtils.md5DigestAsHex(password.getBytes());
    }

    /**
     * 脱敏处理
     *
     * @param user
     * @return
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper<User> getUserQueryWrapper(UserQueryRequest queryRequest) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        Long id = queryRequest.getId();
        String userAccount = queryRequest.getUserAccount();
        String userName = queryRequest.getUserName();
        String userProfile = queryRequest.getUserProfile();
        String userRole = queryRequest.getUserRole();
        String sortField = queryRequest.getSortField();
        String sortOrder = queryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(StrUtil.isNotEmpty(userRole), "userRole", userRole);
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.like(StrUtil.isNotEmpty(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotEmpty(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotEmpty(userProfile), "userProfile", userProfile);
        queryWrapper.orderBy(StrUtil.isNotBlank(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public String getEncryptPassword(String userPassword) {//盐值，混淆密码
        final String SALT = "hl";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }

    @Override
    public Long addUser(User user) {
        //默认密码 12345678
        final String DEFAULT_PASSWORD = "12345678";
        String encryptPassword = this.getEncryptPassWord(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        boolean result = userRepository.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return user.getId();
    }

    @Override
    public Boolean removeById(Long id) {
        return userRepository.removeById(id);
    }

    @Override
    public boolean updateById(User user) {
        return userRepository.updateById(user);
    }

    @Override
    public User getById(long id) {
        return userRepository.getById(id);
    }

    @Override
    public Page<User> page(Page<User> userPage, QueryWrapper<User> querywrapper) {
        return userRepository.page(userPage, querywrapper);
    }

    @Override
    public List<User> listByIds(Set<Long> userIdset) {
        return userRepository.listByIds(userIdset);
    }
}




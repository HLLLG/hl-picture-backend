package com.hl.hlpicturebackend.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hl.hlpicturebackend.exception.BusinessException;
import com.hl.hlpicturebackend.exception.ErrorCode;
import com.hl.hlpicturebackend.mapper.UserMapper;
import com.hl.hlpicturebackend.model.entity.User;
import com.hl.hlpicturebackend.model.enums.UserRoleEnum;
import com.hl.hlpicturebackend.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

/**
* @author hegl
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-12-16 00:53:25
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    @Override
    public Long registerUser(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验参数
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4 ) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度过短");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入密码不一致");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        // 2. 检查账户是否重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        Long count = this.baseMapper.selectCount(queryWrapper);
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
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
        }
        return user.getId();
    }

    /**
     * 获取加密后的密码
     * @param password
     * @return
     */
    @Override
    public String getEncryptPassWord(String password) {
        // 加盐，混淆密码
        final String SALT = "hl";
        return DigestUtils.md5DigestAsHex(password.getBytes());
    }
}





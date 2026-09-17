package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserInfoService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
@Resource
private IUserService iUserService;
    @Override
    public Result follow(Long followId, Boolean isFollow) {
        //获取登录的用户
        Long userId = UserHolder.getUser().getId();
        String key="follows:"+userId;
        //判断关注还是取关
if (isFollow)
{ //关注，新增
    Follow follow = new Follow();
follow.setUserId(userId);
follow.setFollowUserId(followId);
    boolean isSuccess = save(follow);
    if (isSuccess){
        //把关注用户的ID放进Redis
        stringRedisTemplate.opsForSet().add(key,followId.toString());
    }
}else {
//取消关注，删除
    Boolean eq = remove(new QueryWrapper<Follow>()
            .eq("user_id", userId)
            .eq("follow_user_id", followId));

    if (eq){
        stringRedisTemplate.opsForSet().remove(key, followId.toString());
    }


}
        return Result.ok();
    }

    @Override
    public Result isFollow(Long followId) {
        //查询是否关注
        Long userId = UserHolder.getUser().getId();
        Integer count = query().eq("user_id", userId)
                .eq("follow_user_id", followId).count();
        if (count>0){
            return Result.ok(count>0);
        }return Result.ok();
    }

    @Override
    public Result followCommons(Long followId) {
        //获取当前用户
        Long userId = UserHolder.getUser().getId();
        String key="follows:"+userId;
        String key2="follows:"+followId;
        //求交集
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(key, key2);
        if (intersect==null||intersect.isEmpty()){
            return Result.ok(Collections.emptyList());
        }
        //解析ID集合
        List<Long> collect = intersect.stream().map(Long::valueOf).collect(Collectors.toList());
        //查询用户
        List<UserDTO> userDTOS = iUserService.listByIds(collect).stream().map(user -> BeanUtil.copyProperties(user, UserDTO.class)).collect(Collectors.toList());
        return Result.ok(userDTOS);
    }
}

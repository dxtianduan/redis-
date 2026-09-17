package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.service.IFollowService;
import com.hmdp.service.impl.FollowServiceImpl;
import io.swagger.annotations.Api;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 * 关注相关接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/follow")
@Api(tags = "09-关注模块")
public class FollowController {

    @Resource
    private IFollowService iFollowService;
@PutMapping("/{id}/{isFollow}")
    public Result follow(@PathVariable("id") Long followId,@PathVariable("isFollow") Boolean isFollow){
    return iFollowService.follow(followId,isFollow);}
    @GetMapping("/or/not/{id}")
    public Result follow(@PathVariable("id") Long followId){
    return iFollowService.isFollow(followId);}
@GetMapping("/common/{id}")
    public Result followCommon(@PathVariable("id") Long followId){
    return iFollowService.followCommons(followId);
}

}

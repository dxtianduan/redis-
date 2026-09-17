package com.hmdp.controller;


import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.service.IBlogService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 探店笔记相关接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/blog")
@Api(tags = "06-探店笔记模块")
public class BlogController {

    @Resource
    private IBlogService blogService;
    @Resource
    private IUserService userService;
@GetMapping("/of/user")
public Result querBlogByUserId(@RequestParam(value = "current",defaultValue = "1")Integer current,@RequestParam("id")Long id){
    Page<Blog> page = blogService.query().eq("user_id", id).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
    List<Blog> records = page.getRecords();
    return Result.ok(records);
}
    @PostMapping
    @ApiOperation(value = "发布探店笔记", notes = "需要登录。userId 不用传，后端从登录态里取；图片地址请先用『上传图片』接口上传拿到文件名，再用英文逗号拼起来。返回笔记id。")
    public Result saveBlog(
            @ApiParam(value = "笔记内容：标题、图片、文字描述", required = true)
            @RequestBody Blog blog) {

        return blogService.saveBlog(blog);
    }

    @PutMapping("/like/{id}")
    @ApiOperation(value = "给笔记点赞", notes = "需要登录。注意：课程此刻只做了『点赞数+1』，还没实现『同一人只能点一次』和『取消点赞』。")
    public Result likeBlog(
            @ApiParam(value = "笔记id", required = true, example = "1")
            @PathVariable("id") Long id) {

        return blogService.likeBlog(id);
    }

    @GetMapping("/of/me")
    @ApiOperation(value = "查询我的笔记", notes = "需要登录。传页码从1开始，每页10条。")
    public Result queryMyBlog(
            @ApiParam(value = "页码，从1开始", example = "1")
            @RequestParam(value = "current", defaultValue = "1") Integer current) {
        UserDTO user = UserHolder.getUser();
        Page<Blog> page = blogService.query()
                .eq("user_id", user.getId()).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }

    @GetMapping("/hot")
    @ApiOperation(value = "查询热门笔记", notes = "不用登录。按点赞数从高到低排，每页10条，返回时会带上发布者的昵称和头像。")
    public Result queryHotBlog(
            @ApiParam(value = "页码，从1开始", example = "1")
            @RequestParam(value = "current", defaultValue = "1") Integer current) {
        return blogService.queryHotBlog(current);}


        @GetMapping("/{id}")
                public Result queryBlogByid(@PathVariable("id") long id){
            return blogService.queryBlogById(id);
    }
    @GetMapping("/likes/{id}")
    public Result queryBlogLikes(@PathVariable("id") long id) {
        return blogService.queryBlogLikes(id);
    }
    }

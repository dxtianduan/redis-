package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {
    @Resource
    private IUserService userService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public Result queryHotBlog(Integer current) {
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        List<Blog> records = page.getRecords();
        records.forEach(blog -> {
            this.queryBlogById(blog.getId());
            this.isBlogLiked(blog);
        });
            return Result.ok(records);
    }


    @Override
    public Result queryBlogById(long id) {
        Blog blog = getById(id);
        if (blog==null){
            return  Result.fail("笔记不存在");
        }
        extracted(blog);
        //查询是否被点赞
        isBlogLiked(blog);
        return Result.ok(blog);
    }

    private void isBlogLiked(Blog blog) {
        //获取当前用户
        Long id1 = UserHolder.getUser().getId();
        //判断是否点赞
        String key="blog:like:"+blog.getId();
        Double score = stringRedisTemplate.opsForZSet().score(key, id1.toString());
        blog.setIsLike(score!=null);
    }

    @Override
    public Result likeBlog(Long id) {
        //获取当前用户
        Long id1 = UserHolder.getUser().getId();
        //判断是否点赞
String key="blog:like:"+id;
        Double score = stringRedisTemplate.opsForZSet().score(key, id1.toString());
        //若点赞，改数据库，存Redis，set集合
if (score==null){
    boolean update = update().setSql("liked=liked+1").eq("id", id).update();
    if (update){
        stringRedisTemplate.opsForZSet().add(key,id1.toString(),System.currentTimeMillis());
    }
}else
{
    //若未点赞，改数据库，存Redis
    boolean update = update().setSql("liked=liked-1").eq("id", id).update();
    stringRedisTemplate.opsForZSet().remove(key,id1.toString());
}
        return Result.ok();
    }

    private void extracted(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }

}

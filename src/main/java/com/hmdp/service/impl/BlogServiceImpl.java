package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.dto.ScrollResult;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
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
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {
    @Resource
    private IUserService userService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private FollowServiceImpl followService;
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
    public Result queryBlogById(Long id) {
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
        UserDTO user = UserHolder.getUser();
        if (user==null){
            //用户未登录，无需查询是否点赞
            return ;
        }
        Long id1 = user.getId();
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

    @Override
    public Result queryBlogLikes(long id) {
        String key="blog:like:"+id;
        //查询top5范围点赞用户
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        if (top5==null||top5.isEmpty()){
return Result.ok(Collections.emptyList());
        }
        //解析出用户ID
        List<Long> collect = top5.stream().map(Long::valueOf).collect(Collectors.toList());
        String join = StrUtil.join(",", collect);
        //查询用户
        List<UserDTO> userDTOS = userService.query().in("id",collect).last("ORDER BY FIELD(id,"+join+")").list()
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        //返回
        return Result.ok(userDTOS);
    }

    @Override
    public Result saveBlog(Blog blog) {
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        boolean save = save(blog);
        if (!save){
            return Result.fail("新增笔记失败");
        }
//查询作者所有粉丝
        List<Follow> followUserId = followService.query().eq("follow_user_id", user.getId()).list();
        //推送给所有粉丝
        for (Follow follow : followUserId) {
            //获取粉丝ID
            Long userId = follow.getUserId();
            //推送
            String key="fees:"+userId;
            stringRedisTemplate.opsForZSet().add(key,blog.getId().toString(),System.currentTimeMillis());
        }
    return Result.ok(blog.getId());
    }

    @Override
    public Result querBlogOfFollow(Long max, Integer offset) {
        //获取当前用户
        Long id = UserHolder.getUser().getId();
        //查询收件箱
        String key="fees:"+id;
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate
                .opsForZSet()
                .reverseRangeByScoreWithScores(key, 0, max, offset, 2);
        //非空判断
        if (typedTuples==null||typedTuples.isEmpty())
        {
            return Result.ok();
        }
        //解析数据：blogid,mintime,offset
        List<Long> objects = new ArrayList<>(typedTuples.size());
        long mintime=0;
        int os=1;
        for (ZSetOperations.TypedTuple<String> typedTuple : typedTuples) {
            //获取ID
            String value = typedTuple.getValue();
            objects.add(Long.valueOf(value));
            //获取分数
            long l = typedTuple.getScore().longValue();
            if (l==mintime){
                os++;
            }else {
                mintime= l;
                os=1;
            }


        }
        //根据ID查询blog
        String join = StrUtil.join(",", objects);
        List<Blog> blogs1 = query().in("id", objects).last("ORDER BY FIELD(id," +join + ")").list();
        for (Blog blog : blogs1) {
            extracted(blog);
            //查询是否被点赞
            isBlogLiked(blog);
        }
        //封装，返回
        ScrollResult scrollResult = new ScrollResult();
        scrollResult.setList(blogs1);
        scrollResult.setOffset(os);
        scrollResult.setMinTime(mintime);
        return Result.ok(scrollResult);
    }

    private void extracted(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }

}

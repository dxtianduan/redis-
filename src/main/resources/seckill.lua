-- 秒杀资格预判断脚本
-- 调用方式：KEYS 为空，ARGV[1] = 优惠券id，ARGV[2] = 用户id
-- 返回值：0 = 有购买资格；1 = 库存不足；2 = 重复下单；3 = 不是秒杀券/活动不存在；4 = 库存数据非法

-- 1. 从参数里取出优惠券id 和 用户id
local voucherId = ARGV[1]
local userId = ARGV[2]

-- 2. 拼出两个 key
-- 2.1 库存key：值是"还剩几件"，用 incrby 加减
local stockKey = 'seckill:stock:' .. voucherId
-- 2.2 订单key：用 Set 存"已经下过单的用户id"
local orderKey = 'seckill:order:' .. voucherId

-- 3. 判断库存
-- 【修复1】原代码写的是：
--     tonumber(redis.call('get', stockKey) <= 0)
--   括号放错了位置。比较运算写在了 tonumber() 的里面，
--   等于拿"字符串"和数字 0 比较，Lua 会直接报错。
--   正确写法是先把返回值转成数字，再和 0 比较。
-- 【修复2】先单独判断 key 是否存在。
--   普通券（type=0）压根没有库存 key，如果直接 tonumber(false) 会报错，
--   报出来的就是前端那句含糊的"服务器异常"。这里显式区分成返回值 3，
--   前端就能看到"该优惠券不是秒杀券或活动不存在"这种能定位问题的提示。
local stockStr = redis.call('get', stockKey)
if (stockStr == false) then
    -- 库存 key 不存在：不是秒杀券，或者库存还没同步到 Redis
    return 3
end

local stock = tonumber(stockStr)
if (stock <= 0) then
    -- 库存不足
    return 1
end

-- 4. 判断是否重复下单
-- sismember：判断 userId 是否已经在 Set 里，返回 1 = 已经在
if (redis.call('sismember', orderKey, userId) == 1) then
    -- 重复下单
    return 2
end

-- 5. 扣库存（预扣，真正落库由后台线程去 MySQL 做）
redis.call('incrby', stockKey, -1)
-- 6. 记录下单用户
redis.call('sadd', orderKey, userId)

-- 7. 返回 0 = 有购买资格
return 0

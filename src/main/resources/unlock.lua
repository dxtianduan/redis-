-- 释放锁的 Lua 脚本
-- 注意：Redis 的 Lua 里，key 参数必须写成 KEYS(带 S)，参数写成 ARGV
-- 写成 KEY 会报：Script attempted to access nonexistent global variable 'KEY'
if (redis.call('get', KEYS[1]) == ARGV[1]) then
    -- 是自己的锁才删，避免删掉别人的锁
    return redis.call('del', KEYS[1])
end
return 0

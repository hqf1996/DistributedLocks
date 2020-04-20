package RedisLock;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Collections;

/**
 * @Author: hqf
 * @description: 基于redis的加锁与解锁
 * @Data: Create in 15:20 2020/4/20
 * @Modified By:
 */
public class myRedisLock {
    private static final String SET_IF_NOT_EXIST = "NX";
    private static final String SET_WITH_EXPIRE_TIME = "PX";
    private static final int expireTime = 1000;  //过期时间
    private static final int timeout = 10000;    //超时时间
    private JedisPool jedisPool = new JedisPool("127.0.0.1", 6379);

    /**
     * 加锁
     * @param lock_key
     * @param requestId
     * @return
     */
    public boolean lock(String lock_key, String requestId) {
        Jedis jedis = jedisPool.getResource();
        long time1 = System.currentTimeMillis();
        try {
            while (true) {
                // 加锁成功才会跳出
                String result = jedis.set(lock_key, requestId, SET_IF_NOT_EXIST, SET_WITH_EXPIRE_TIME, expireTime);
                if ("OK".equals(result)) {
                    return true;
                }
                // 否则循环等待
                Thread.sleep(100);
                // 如果超过超时时间，则返回false
                if (System.currentTimeMillis()-time1 > timeout) {
                    return false;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            jedis.close();
        }
        return false;
    }

    /**
     * 解锁
     * @param lock_key
     * @param requestId
     * @return
     */
    public boolean unlock(String lock_key, String requestId) {
        Jedis jedis = jedisPool.getResource();
        // LUA脚本
        String script =
                "if redis.call('get',KEYS[1]) == ARGV[1] then" +
                        "   return redis.call('del',KEYS[1]) " +
                        "else" +
                        "   return 0 " +
                        "end";
        try {
            Object result = jedis.eval(script, Collections.singletonList(lock_key), Collections.singletonList(requestId));
            if ("1".equals(result)) {
                return true;
            } else {
                return false;
            }
        } finally {
            jedis.close();
        }

    }
}

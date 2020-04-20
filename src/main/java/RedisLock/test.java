package RedisLock;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * @Author: hqf
 * @description:
 * @Data: Create in 15:57 2020/4/20
 * @Modified By:
 */
public class test {
    public static void main(String[] args) {
        Jedis jedis = new JedisPool("127.0.0.1", 6379).getResource();
        System.out.println(jedis.get("test"));
    }
}

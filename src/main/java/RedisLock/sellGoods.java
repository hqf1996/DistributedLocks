package RedisLock;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * @Author: hqf
 * @description: 模仿多个线程进行商品的买卖
 * @Data: Create in 14:29 2020/4/20
 * @Modified By:
 */
public class sellGoods extends Thread{
    private static final String lock_key = "redis_lock";   //锁的key
    private static myRedisLock redisLock = new myRedisLock();
    private int goodsNums = 10;   // 1000个商品
    private static int CustomsNums = 15;   //15个顾客来抢商品
    private static CountDownLatch latch = new CountDownLatch(CustomsNums);

    @Override
    public void run() {
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String requestId = UUID.randomUUID().toString();
        redisLock.lock(lock_key, requestId);
        if (goodsNums > 0) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            goodsNums--;
            System.out.println(goodsNums);
        }
        redisLock.unlock(lock_key, requestId);
    }

    public static void main(String[] args) {
        sellGoods test = new sellGoods();
        for (int i = 0 ; i < CustomsNums ; ++i) {
            new Thread(test).start();
            latch.countDown();
        }
    }
}

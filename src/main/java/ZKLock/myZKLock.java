package ZKLock;


import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * @Author: hqf
 * @description:
 * @Data: Create in 11:05 2020/4/21
 * @Modified By:
 */
public class myZKLock implements Lock {

    private String lockPath;     // 锁的目录
    private ZkClient client;  // 客户端
    private ThreadLocal<String> curPath = new ThreadLocal<>(); // 当前锁的目录
    private ThreadLocal<String> beforePath = new ThreadLocal<>(); // 前一个锁的目录

    public myZKLock(String lockPath) {
        if (lockPath.equals("") || lockPath == null) {
            throw new IllegalArgumentException("路径不能为空！");
        }
        this.lockPath = lockPath;
        client = new ZkClient("localhost:2181");
        if (!this.client.exists(lockPath)) {
            this.client.createPersistent(lockPath, true);
            System.out.println("创建成功");
        }
    }

    @Override
    public void lock() {
        // 如果成功获得锁，返回
        if (tryLock()) {
            return;
        }
        // 否则监听等待并尝试加锁
        try {
            waitForLock();
            lock();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void lockInterruptibly() throws InterruptedException {

    }

    /**
     * 尝试获取分布式锁
     * @return
     */
    @Override
    public boolean tryLock() {
        if (this.curPath.get() == null || !this.client.exists(this.curPath.get())) {
            // 如果是一个新的请求，还未获取过锁，就先创建一个临时有序的结点号，相当于先排队等待
            String path = this.client.createEphemeralSequential(lockPath+"/", "lock");
            curPath.set(path);
//            System.out.println(Thread.currentThread().getName() + "创建临时结点 " + path);
        }
        // 取得一个最小的结点序号，也就是最先创建的那个先获得锁
        List<String> children = this.client.getChildren(lockPath);
        Collections.sort(children);
        // 如果当前请求是最小的，则获得分布式锁
        if (curPath.get().equals(lockPath+"/"+children.get(0))) {
//            System.out.println(Thread.currentThread().getName() + "获取分布式锁成功");
            return true;
        } else {
            // 否则则获取它的前一个结点
            int index = children.indexOf(curPath.get().substring(lockPath.length()+1));
            System.out.println(Thread.currentThread().getName() + " " + index);
            String prepath = lockPath+"/"+children.get(index-1);
            beforePath.set(prepath);
        }
        return false;
    }

    /**
     * 阻塞等待获得锁
     * @throws InterruptedException
     */
    public void waitForLock() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        IZkDataListener listener = new IZkDataListener() {
            @Override
            public void handleDataChange(String s, Object o) throws Exception {

            }

            @Override
            public void handleDataDeleted(String s) throws Exception {
                // 监听到被删除，则不阻塞
                latch.countDown();
            }
        };
        this.client.subscribeDataChanges(this.beforePath.get(), listener);
        // 当发现前一个结点还存在就一直阻塞
        if (this.client.exists(this.beforePath.get())) {
            latch.await();
        }
        // before删除，取消监听
        client.unsubscribeDataChanges(this.beforePath.get(), listener);
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return false;
    }

    /**
     * 解锁
     */
    @Override
    public void unlock() {
        if (this.curPath.get() != null) {
            this.client.delete(this.curPath.get());
            this.curPath.set(null);
        }
    }

    @Override
    public Condition newCondition() {
        return null;
    }
}

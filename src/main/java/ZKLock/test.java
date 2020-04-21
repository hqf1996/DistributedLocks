package ZKLock;

import org.I0Itec.zkclient.ZkClient;

import java.io.IOException;
import java.util.List;

/**
 * @Author: hqf
 * @description:
 * @Data: Create in 11:21 2020/4/21
 * @Modified By:
 */
public class test {
    public static void main(String[] args) throws IOException {
        ZkClient client = new ZkClient("localhost:2181");
        for (int i = 0 ; i < 3 ; ++i) {
            String node = client.createEphemeralSequential("/myTest"+"/", "locked");
            System.out.println(node);
        }


        List<String> children = client.getChildren("/myTest");
        for (String child : children) {
            System.out.println(child);
        }
        System.out.println(children.size());
    }
}

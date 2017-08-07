package top.devgo.consulclient;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.health.model.Check;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ConsulClientApplicationTests {

    private static Logger logger = LoggerFactory.getLogger(ConsulClientApplicationTests.class);

    @Test
    public void contextLoads() {
    }

    @Autowired
    private ConsulClient consulClient;

    @Test
    public void listMembers() {
        consulClient.getAgentMembers().getValue().forEach(agent -> {
            // role：consul---代表服务端   role：node---代表客户端
            logger.info("agent: {} , role: {}", agent.getAddress(), agent.getTags().get("role"));
        });
    }

}

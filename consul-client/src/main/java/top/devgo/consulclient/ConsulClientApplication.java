package top.devgo.consulclient;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

@SpringBootApplication
@EnableDiscoveryClient
@RestController
@EnableConfigurationProperties({ConsulDynamicConfig.class})
public class ConsulClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConsulClientApplication.class, args);
	}

    @Autowired
    private ConsulDynamicConfig consulConfig;

	@Value("${config.greeting}")
	private String greeting;

    @RequestMapping("/")
    public String home() {
        return greeting +" "+ consulConfig.getText();
    }

    @RequestMapping(value = "/yas", method = POST)
    public YasResp yetAnotherService(@RequestBody YasResq yasResq) {
        System.out.println("yas token: " + yasResq.getToken());
        return new YasResp(0, "ok");
    }

}

@Data
@ConfigurationProperties(prefix = "config")
class ConsulDynamicConfig {
    private String text;
}

@Data
@AllArgsConstructor
class YasResp {
    private int code;
    private String msg;
}

@Data
class YasResq {
    private String token;
}

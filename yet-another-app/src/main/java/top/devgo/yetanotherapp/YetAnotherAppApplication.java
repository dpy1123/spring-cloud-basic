package top.devgo.yetanotherapp;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.agent.model.NewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;


import javax.annotation.PreDestroy;
import java.util.Collections;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;

@SpringBootApplication
public class YetAnotherAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(YetAnotherAppApplication.class, args);
	}

	@Bean
    public RouterFunction router(){
	    return RouterFunctions.route(GET("/").or(GET("/health")),
                request-> {
                    System.out.println("checked!");
                    return ServerResponse.ok().body(BodyInserters.fromObject("ok"));
                });
    }


    @Bean
    public ConsulClient consulClient(){
        return new ConsulClient("localhost", 8500);
    }

    @Autowired
    ConsulClient client;

    @Value("${spring.application.name}")
    String appName;

    final String appId = "outer-app-id";

    @Component
    public class RegisterService2Consul implements CommandLineRunner {
        @Override
        public void run(String... strings) throws Exception {
            // register new service with associated health check
            NewService newService = new NewService();
            newService.setId(appId);
            newService.setTags(Collections.singletonList("outer-app"));
            newService.setName(appName);
            newService.setPort(8080);

            NewService.Check serviceCheck = new NewService.Check();
            serviceCheck.setHttp("http://localhost:8080/health");
            serviceCheck.setInterval("10s");
            newService.setCheck(serviceCheck);

            client.agentServiceRegister(newService);
            System.out.println("registered!");
        }
    }

    @PreDestroy
    public void deregister() {
        client.agentServiceDeregister(appId);
        System.out.println("deregister!");
    }
}

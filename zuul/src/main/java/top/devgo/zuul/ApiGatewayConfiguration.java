package top.devgo.zuul;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

//@Configuration
public class ApiGatewayConfiguration {

//    @Bean(name="zuul.CONFIGURATION_PROPERTIES")
//    @RefreshScope
//    @ConfigurationProperties("zuul")
//    @Primary
//    public ZuulProperties zuulProperties() {
//        System.out.println("======> Called");
//        return new ZuulProperties();
//    }

}
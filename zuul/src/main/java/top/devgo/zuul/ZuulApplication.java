package top.devgo.zuul;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.netflix.ribbon.support.RibbonRequestCustomizer;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandFactory;
import org.springframework.cloud.netflix.zuul.filters.route.SimpleHostRoutingFilter;
import org.springframework.cloud.netflix.zuul.util.ZuulRuntimeException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;

import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.*;


@SpringBootApplication
@EnableZuulProxy
@EnableConfigurationProperties({ZuulProperties.class})
public class ZuulApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZuulApplication.class, args);
    }


    @Bean(name = "zuul.CONFIGURATION_PROPERTIES")
    @RefreshScope
    @ConfigurationProperties("zuul")
    @Primary
    /**
     * @source https://github.com/spring-cloud/spring-cloud-netflix/issues/706
     */
    public ZuulProperties zuulProperties() {
        System.out.println("======> Called");
        return new ZuulProperties();
    }

    @Bean
    public PreProcessFilter preProcessFilter(){
        return new PreProcessFilter();
    }


    @Bean
    public EnhanceHostRoutingFilter enhanceHostRoutingFilter(@Autowired ProxyRequestHelper helper, @Autowired ZuulProperties properties) {
        return new EnhanceHostRoutingFilter(helper, properties);
    }

    @Bean
    public EnhanceRibbonRoutingFilter enhanceRibbonRoutingFilter(@Autowired ProxyRequestHelper helper,
                                                                 @Qualifier("ribbonCommandFactory") RibbonCommandFactory<?> ribbonCommandFactory){
        //init param from  https://github.com/spring-cloud/spring-cloud-netflix/blob/master/spring-cloud-netflix-core/src/main/java/org/springframework/cloud/netflix/zuul/ZuulProxyAutoConfiguration.java
        return new EnhanceRibbonRoutingFilter(helper, ribbonCommandFactory, Collections.emptyList());
    }

    @PreDestroy
    public void destroy(){

    }
}


class PreProcessFilter extends ZuulFilter {

    @Autowired
    private ObjectMapper mapper;

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 0;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    /**
     *
     * @param ctx
     * @return
     * @throws IOException
     * @source https://stackoverflow.com/questions/41896886/how-transform-client-request-body-with-a-zuul-filter
     */
    public String getRequestBody(RequestContext ctx) throws IOException {
        InputStream in = (InputStream) ctx.get("requestEntity");
        if (in == null) {
            in = ctx.getRequest().getInputStream();
        }
        String body = StreamUtils.copyToString(in, Charset.forName("UTF-8"));
        ctx.set("requestEntity", new ByteArrayInputStream(body.getBytes("UTF-8")));
        return body;
    }

    private boolean checkToken(String token){
        return "123".equals(token);
    }

    private void pass(RequestContext ctx){
        ctx.setSendZuulResponse(true);// 对该请求进行路由
        ctx.setResponseStatusCode(200);
        ctx.set("isSuccess", true);// 设值，让下一个Filter看到上一个Filter的状态
    }

    private void ban(RequestContext ctx, int errorCode, String respBody){
        ctx.setSendZuulResponse(false);// 过滤该请求，不对其进行路由
        ctx.setResponseStatusCode(errorCode);// 返回错误码
        ctx.setResponseBody(respBody);// 返回错误内容
        ctx.set("isSuccess", false);
    }

    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        String method = ctx.getRequest().getMethod();
        String token = null;

        if ("POST".equalsIgnoreCase(method)) {
            String contentType = ctx.getRequest().getContentType();
            if ("application/json".equalsIgnoreCase(contentType)){
                try {
                    String body = getRequestBody(ctx);
                    Map data = mapper.readValue(body, Map.class);
                    token = data.get("token").toString();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else if ("application/x-www-form-urlencoded".equalsIgnoreCase(contentType)){
                token = ctx.getRequest().getParameter("token");
            }
        }else if ("GET".equalsIgnoreCase(method)) {
            token = ctx.getRequest().getParameter("token");
        }


        if (checkToken(token)) {
            pass(ctx);
            //add custom header
            //insight from http://dockone.io/article/2186
            EnhanceHttpServletRequest newRequest = new EnhanceHttpServletRequest(ctx.getRequest());
            Map<String, String> header = new HashMap<>();
            header.put("X-HEADER", "yolo~");
            newRequest.setNewHeader(header);
            ctx.setRequest(newRequest);
        } else {
            ban(ctx, 403, "{\"result\":\"token is not correct!\"}");
        }

        return null;
    }
}



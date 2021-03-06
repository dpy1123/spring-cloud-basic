package top.devgo.zuul;

import com.netflix.client.ClientException;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.netflix.ribbon.support.RibbonRequestCustomizer;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommand;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandContext;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandFactory;
import org.springframework.cloud.netflix.zuul.util.ZuulRuntimeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.MultiValueMap;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.*;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.REQUEST_ENTITY_KEY;

public class EnhanceRibbonRoutingFilter extends ZuulFilter {

    private static final Log log = LogFactory.getLog(EnhanceRibbonRoutingFilter.class);

    protected ProxyRequestHelper helper;
    protected RibbonCommandFactory<?> ribbonCommandFactory;
    protected List<RibbonRequestCustomizer> requestCustomizers;
    private boolean useServlet31 = true;

    public EnhanceRibbonRoutingFilter(ProxyRequestHelper helper,
                               RibbonCommandFactory<?> ribbonCommandFactory,
                               List<RibbonRequestCustomizer> requestCustomizers) {
        this.helper = helper;
        this.ribbonCommandFactory = ribbonCommandFactory;
        this.requestCustomizers = requestCustomizers;
        // To support Servlet API 3.1 we need to check if getContentLengthLong exists
        try {
            HttpServletRequest.class.getMethod("getContentLengthLong");
        } catch(NoSuchMethodException e) {
            useServlet31 = false;
        }
    }

    public EnhanceRibbonRoutingFilter(RibbonCommandFactory<?> ribbonCommandFactory) {
        this(new ProxyRequestHelper(), ribbonCommandFactory, null);
    }

    @Override
    public String filterType() {
        return ROUTE_TYPE;
    }

    @Override
    public int filterOrder() {
        return RIBBON_ROUTING_FILTER_ORDER;
    }

    @Override
    public boolean shouldFilter() {
        RequestContext ctx = RequestContext.getCurrentContext();
        return (ctx.getRouteHost() == null && ctx.get(SERVICE_ID_KEY) != null
                && ctx.sendZuulResponse());
    }

    @Override
    public Object run() {
        RequestContext context = RequestContext.getCurrentContext();
        this.helper.addIgnoredHeaders();
        try {
            RibbonCommandContext commandContext = buildCommandContext(context);
            ClientHttpResponse response = forward(commandContext);
            setResponse(response);
            return response;
        }
        catch (ZuulException ex) {
            throw new ZuulRuntimeException(ex);
        }
        catch (Exception ex) {
            throw new ZuulRuntimeException(ex);
        }
    }

    protected RibbonCommandContext buildCommandContext(RequestContext context) {
        HttpServletRequest request = context.getRequest();

        MultiValueMap<String, String> headers = this.helper
                .buildZuulRequestHeaders(request);
        MultiValueMap<String, String> params = this.helper
                .buildZuulRequestQueryParams(request);
        String verb = getVerb(request);
        InputStream requestEntity = getRequestBody(request);
        if (request.getContentLength() < 0 && !verb.equalsIgnoreCase("GET")) {
            context.setChunkedRequestBody();
        }

        String serviceId = (String) context.get(SERVICE_ID_KEY);
        Boolean retryable = (Boolean) context.get(RETRYABLE_KEY);

        String uri = this.helper.buildZuulRequestURI(request);

        // remove double slashes
        uri = uri.replace("//", "/");

        long contentLength = useServlet31 ? request.getContentLengthLong(): request.getContentLength();


        //[enhance]
        if (request instanceof EnhanceHttpServletRequest){
            EnhanceHttpServletRequest enhanceRequest = (EnhanceHttpServletRequest) request;
            enhanceRequest.getNewHeader().keySet().forEach(key -> headers.set(key, enhanceRequest.getNewHeader().get(key)));
            enhanceRequest.getHeaders2Remove().forEach(name -> headers.remove(name));
        }

        return new RibbonCommandContext(serviceId, verb, uri, retryable, headers, params,
                requestEntity, this.requestCustomizers, contentLength);
    }

    protected ClientHttpResponse forward(RibbonCommandContext context) throws Exception {
        Map<String, Object> info = this.helper.debug(context.getMethod(),
                context.getUri(), context.getHeaders(), context.getParams(),
                context.getRequestEntity());

        RibbonCommand command = this.ribbonCommandFactory.create(context);
        try {
            ClientHttpResponse response = command.execute();
            this.helper.appendDebug(info, response.getStatusCode().value(),
                    response.getHeaders());
            return response;
        }
        catch (HystrixRuntimeException ex) {
            return handleException(info, ex);
        }

    }

    protected ClientHttpResponse handleException(Map<String, Object> info,
                                                 HystrixRuntimeException ex) throws ZuulException {
        int statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
        Throwable cause = ex;
        String message = ex.getFailureType().toString();

        ClientException clientException = findClientException(ex);
        if (clientException == null) {
            clientException = findClientException(ex.getFallbackException());
        }

        if (clientException != null) {
            if (clientException
                    .getErrorType() == ClientException.ErrorType.SERVER_THROTTLED) {
                statusCode = HttpStatus.SERVICE_UNAVAILABLE.value();
            }
            cause = clientException;
            message = clientException.getErrorType().toString();
        }
        info.put("status", String.valueOf(statusCode));
        throw new ZuulException(cause, "Forwarding error", statusCode, message);
    }

    protected ClientException findClientException(Throwable t) {
        if (t == null) {
            return null;
        }
        if (t instanceof ClientException) {
            return (ClientException) t;
        }
        return findClientException(t.getCause());
    }

    protected InputStream getRequestBody(HttpServletRequest request) {
        InputStream requestEntity = null;
        try {
            requestEntity = (InputStream) RequestContext.getCurrentContext()
                    .get(REQUEST_ENTITY_KEY);
            if (requestEntity == null) {
                requestEntity = request.getInputStream();
            }
        }
        catch (IOException ex) {
            log.error("Error during getRequestBody", ex);
        }
        return requestEntity;
    }

    protected String getVerb(HttpServletRequest request) {
        String method = request.getMethod();
        if (method == null) {
            return "GET";
        }
        return method;
    }

    protected void setResponse(ClientHttpResponse resp)
            throws ClientException, IOException {
        RequestContext.getCurrentContext().set("zuulResponse", resp);
        this.helper.setResponse(resp.getStatusCode().value(),
                resp.getBody() == null ? null : resp.getBody(), resp.getHeaders());
    }

}

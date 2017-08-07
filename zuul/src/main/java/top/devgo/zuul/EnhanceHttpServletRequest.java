package top.devgo.zuul;


import lombok.Getter;
import lombok.Setter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.*;

@Getter
@Setter
public class EnhanceHttpServletRequest extends HttpServletRequestWrapper {


    /**
     * Constructs a request object wrapping the given request.
     *
     * @param request The request to wrap
     * @throws IllegalArgumentException if the request is null
     */
    public EnhanceHttpServletRequest(HttpServletRequest request) {
        super(request);
    }

    private Map<String, String> newHeader = new HashMap<>();

    private Set<String> headers2Remove = new HashSet<>();

    /**
     * 增加自定义header
     * @param key
     * @param value
     */
    public void addHeader(String key, String value) {
        newHeader.put(key, value);
    }

    /**
     * 增加待删除的header
     * @param key
     */
    public void addRemoveHeaders(String... key) {
        headers2Remove.addAll(Arrays.asList(key));
    }
}

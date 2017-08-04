package top.devgo.zuul;


import lombok.Getter;
import lombok.Setter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.HashMap;
import java.util.Map;

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

}

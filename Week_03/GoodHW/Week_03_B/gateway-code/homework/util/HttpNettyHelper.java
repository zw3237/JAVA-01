package daiwei.geekbang.homework.util;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.nio.charset.StandardCharsets;

/**
 * Created by Daiwei on 2021/1/25
 */
public class HttpNettyHelper {

    private HttpNettyHelper() {}

    public static DefaultFullHttpResponse genUnAuthResp() {
        String unAuthJson = "{\"code\": 401, \"msg\":\"Authentication failed.\"}";
        return genBaseResp(unAuthJson.getBytes(StandardCharsets.UTF_8));
    }


    public static DefaultFullHttpResponse genBaseResp(byte[] bytes) {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                Unpooled.wrappedBuffer(bytes));
        response.headers().set("Content-Type", "application/json");
        response.headers().setInt("Content-Length", response.content().readableBytes());
        return response;
    }

    public static DefaultFullHttpResponse genEmptyResp() {
        return genBaseResp(new byte[0]);
    }

    public static DefaultFullHttpResponse genFailedResp() {
        String failedJson = "{\"code\": 500, \"msg\":\"invoke failed.\"}";
        return genBaseResp(failedJson.getBytes(StandardCharsets.UTF_8));
    }
}

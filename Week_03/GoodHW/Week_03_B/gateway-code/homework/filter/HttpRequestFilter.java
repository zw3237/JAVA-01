package daiwei.geekbang.homework.filter;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

/**
 * Created by Daiwei on 2021/1/29
 */
public interface HttpRequestFilter {

    boolean filter(FullHttpRequest request, ChannelHandlerContext ctx);
}

package com.geek.gatewaydemo.inbound;

import com.geek.gatewaydemo.filter.FilterRegister;
import com.geek.gatewaydemo.filter.HeaderHttpRequestFilter;
import com.geek.gatewaydemo.filter.HttpRequestFilter;
import com.geek.gatewaydemo.outbound.httpclient4.HttpOutboundHandler;
import com.google.common.collect.Lists;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class HttpInboundHandler extends ChannelInboundHandlerAdapter {

    private static Logger logger = LoggerFactory.getLogger(HttpInboundHandler.class);
    private final List<String> proxyServer;
    private HttpOutboundHandler handler;
    private FilterRegister filterRegister = new FilterRegister();

    public HttpInboundHandler(List<String> proxyServer) {
        this.proxyServer = proxyServer;
        this.handler = new HttpOutboundHandler(this.proxyServer);
    }
    
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            FullHttpRequest fullRequest = (FullHttpRequest) msg;
    
            handler.handle(fullRequest, ctx, filterRegister.getRequestFilterChain());
    
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

}

package com.example.httpclientpropagation30;

import io.netty.channel.*;

import java.util.Map;
import java.util.function.Supplier;

public class MyNettyChannelHandler extends ChannelDuplexHandler {

    private Supplier<Map<String, String>> additionalContext;

    public MyNettyChannelHandler(Supplier<Map<String, String>> additionalContext) {
        this.additionalContext = additionalContext;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
// -----> here value is null
        System.out.println("read: " + additionalContext.get().toString());
        ctx.fireChannelRead(msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
// -----> here value is null
        System.out.println("write: " + additionalContext.get().toString());
        ctx.write(msg, promise);
    }
}

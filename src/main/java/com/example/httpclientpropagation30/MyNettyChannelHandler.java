package com.example.httpclientpropagation30;

import io.micrometer.context.ContextSnapshot;
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
        try (ContextSnapshot.Scope scope = ContextSnapshot.captureFrom(ctx.channel()).setThreadLocals()) {
            System.out.println("read: " + additionalContext.get().toString());
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        try (ContextSnapshot.Scope scope = ContextSnapshot.captureFrom(ctx.channel()).setThreadLocals()) {
            System.out.println("write: " + additionalContext.get().toString());
            ctx.write(msg, promise);
        }
    }
}

package network.netty.server;

import network.Network;
import network.message.*;
import network.message.payload.MessagePayload;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class MessageHandler extends SimpleChannelInboundHandler<Message<? extends MessagePayload>> {
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        try {
            super.exceptionCaught(ctx, cause);
            cause.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message<? extends MessagePayload> msg) throws Exception {
        if (Network.getMyID().equals(msg.getDstID())) {
            Network.addMsg(msg);
        } else {
            Network.sendMsg(msg);
        }

    }
}

package network.netty.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;

import java.nio.charset.Charset;

public class NettyHandler extends SimpleChannelInboundHandler<Object> {


    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        super.channelReadComplete(ctx);
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        DatagramPacket packet = (DatagramPacket) msg;
        String message = packet.content().toString(Charset.defaultCharset());
        System.out.println("Received Message : " + message);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

}
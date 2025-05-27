package network.netty.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;

public class Client {
    int port;
    String IP;
    Channel channel;
    EventLoopGroup workGroup = new NioEventLoopGroup();

    /**
     * Constructor
     * @param IP {@link String} IP of server
     * @param port {@link Integer} port of server
     */
    public Client(String IP, int port){
        this.port = port;
        this.IP = IP;
    }

    /**
     * 	Startup the client
     *
     * @return {@link ChannelFuture}
     * @throws Exception
     */
    public ChannelFuture startup() throws Exception {
        try{
            Bootstrap b = new Bootstrap();
            b.group(workGroup);
            b.channel(NioDatagramChannel.class);
            b.handler(new ChannelInitializer<DatagramChannel>() {
                protected void initChannel(DatagramChannel datagramChannel) throws Exception {
                	datagramChannel.pipeline().addLast(new NettyHandler());
                }
            });
            ChannelFuture channelFuture = b.connect(this.IP, this.port).sync();
            this.channel = channelFuture.channel();

            return channelFuture;
        }finally{
        }
    }

    /**
     *	Shutdown a client
     */
    public void shutdown(){
        workGroup.shutdownGracefully();
    }
}
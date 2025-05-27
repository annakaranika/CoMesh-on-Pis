package network.netty.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.DatagramChannel;

/**
 * {@link PipelineFactory} is the pipeline Factory interface.
 */
public interface PipelineFactory {

	// Socket Channel initializer
	ChannelInitializer<DatagramChannel> createInitializer();
}

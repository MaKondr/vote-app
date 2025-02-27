package client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class Client {
    static final String HOST = System.getProperty("host", "127.0.0.1");
    static final int PORT = Integer.parseInt(System.getProperty("port", "8007"));
    static final int SIZE = Integer.parseInt(System.getProperty("size", "256"));

    public static void start() throws InterruptedException {
        EventLoopGroup group = new NioEventLoopGroup();
               try {
                        Bootstrap b = new Bootstrap();
                       b.group(group)
                        .channel(NioSocketChannel.class)
                        .option(ChannelOption.TCP_NODELAY, true)
                        .handler(new ChannelInitializer<SocketChannel>() {
                 @Override
                public void initChannel(SocketChannel ch) throws Exception {
                                          ChannelPipeline p = ch.pipeline();
                                         p.addLast(new LoggingHandler(LogLevel.INFO));
                                          p.addLast(new EchoClientHandler());
                                     }
               });

                        // Start the client.
                        ChannelFuture f = b.connect(HOST, PORT).sync();

                        // Wait until the connection is closed.
                         f.channel().closeFuture().sync();
                     } finally {
                         // Shut down the event loop to terminate all threads.
                          group.shutdownGracefully();
                      }
    }
}

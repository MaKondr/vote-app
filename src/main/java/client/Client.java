package client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.util.Scanner;

public class Client {
    private final String host;
    private final int port;

    public Client(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void run() throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new StringDecoder(), new StringEncoder(), new ClientHandler());
                        }
                    });
            ChannelFuture f = b.connect(host, port).sync();
            Channel channel = f.channel();

            Scanner scanner = new Scanner(System.in);
            while (channel.isActive()) {
                String input = scanner.nextLine();
                channel.writeAndFlush(input + "\n");
                if ("exit".equals(input)) break;
            }
            channel.closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }
}

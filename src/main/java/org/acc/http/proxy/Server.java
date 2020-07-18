package org.acc.http.proxy;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import lombok.extern.log4j.Log4j2;
import org.acc.http.proxy.certificate.CertificateImpl;
import org.acc.http.proxy.certificate.CertificatePool;
import org.acc.http.proxy.handler.HandlerName;
import org.acc.http.proxy.handler.HttpHandler;

import java.util.function.Consumer;

/**
 * 服务
 */
@Log4j2
public final class Server {
    public static void main(String[] args) {
        if (args.length == 0) {
            log.error("请指定代理运行端口");
            return;
        }

        Server server = new Server();
        server.run(Integer.parseInt(args[0]));
    }

    public void run(int port) {
        run(port, null, null);
    }

    public void runWithConsumer(int port, Consumer<FullHttpRequest> consumer) {
        run(port, consumer, new CertificatePool(new CertificateImpl()));
    }

    private void run(int port, Consumer<FullHttpRequest> consumer, CertificatePool certificatePool) {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline channelPipeline = socketChannel.pipeline();

                            channelPipeline.addLast(HandlerName.HTTP_SERVER_CODEC, new HttpServerCodec());
                            channelPipeline.addLast(HandlerName.HTTP_OBJECT_AGGREGATOR, new HttpObjectAggregator(1024 * 1024));
                            channelPipeline.addLast(HandlerName.HTTP_HANDLER, new HttpHandler(certificatePool, consumer));
                        }
                    });
            ChannelFuture channelFuture = serverBootstrap.bind(port).sync();

            log.info("代理服务启动成功，运行于 {} 端口", port);

            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.error(e);
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}

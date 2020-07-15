package org.acc.http.proxy;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import lombok.extern.log4j.Log4j2;
import org.acc.http.proxy.certificate.CertificateImpl;
import org.acc.http.proxy.certificate.CertificatePool;
import org.acc.http.proxy.handler.HandlerName;
import org.acc.http.proxy.handler.HttpHandler;

/**
 * 服务
 */
@Log4j2
public final class Server {
    public void startUp(int port) {
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

                            channelPipeline.addLast(HandlerName.HTTP_CODEC, new HttpServerCodec());
                            channelPipeline.addLast(HandlerName.HTTP_HANDLER, new HttpHandler(new CertificatePool(new CertificateImpl())));
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

# http-proxy
基于netty实现的http(s)代理<br/><br/>
##### 1.不需要获取请求信息时，运行 run(int port)（不会动态生成证书，代理速度较快）
##### 2.需要获取请求信息时，运行runWithConsumer(int port, Consumer<HttpRequest> consumer) （需要动态生成证书，速度相对较慢）
#### RabbitMQ入门
该项目全部参考RabbitMQ官网自行学习.有疑问.看文档.  
http://www.rabbitmq.com/documentation.html

#### 安装
1. 安装erlang编译环境.
> yum -y install make ncurses-devel gcc gcc-c++ unixODBC unixODBC-devel openssl openssl-devel
2. 安装erlang(rabbitMQ的精简版erlang,从github上下载) ,输入 erl 验证是否安装成功
> rpm -i erlang-20.1.7.1-1.el7.centos.x86_64.rpm 
3. 安装rabbitMQ
> rpm --import https://dl.bintray.com/rabbitmq/Keys/rabbitmq-release-signing-key.asc
> yum install rabbitmq-server-3.7.2-1.el7.noarch.rpm
4. 启动(官网有一个插件库的页面.介绍了很多插件,插件只需要一次启用即可.)
> 系统启动时启动 chkconfig rabbitmq-server on
> 服务启动 service rabbitmq-server start
> 服务停止 service rabbitmq-server stop
> 服务是否启动 service rabbitmq-server  status
> webSocket插件启用 rabbitmq-plugins enable rabbitmq_web_mqtt

5. 配置文件
>
    默认安装完成后,有个配置文件的例子,将其复制到生效的目录
    cp /usr/share/doc/rabbitmq-server-3.7.2/rabbitmq.config.example /etc/rabbitmq/rabbitmq.config
    注意,该例子文件,对所有的配置都做了介绍.
    需要增加配置时,大部分时候只需要将注释去掉,再稍稍修改下即可. 
    注意括号的位置等即可.
    并且要将末尾的逗号删除
>

6.  web界面插件启用 rabbitmq-plugins enable rabbitmq_management
>
    默认端口 15672
    默认帐号密码 guest/guest
    
    如果想使用guest/guest通过远程机器访问，
    需要在rabbitmq配置文件中(/etc/rabbitmq/rabbitmq.config)中设置loopback_users为[] 
    
    修改,以便在外网访问
    [{rabbitmq_management,
      [{listener, [{port, 15672},
                   {ip, "0.0.0.0"}
                  ]}
      ]}
    ].
>
配置成功..好麻烦..特别是这个配置文件的格式.反人类..

#### 分布式
RabbitMQ的分布式分为三种方式.可以组合使用
* clustering(集群,需要同一局域网)
> 将多台机器连接在一起,使用Erlang通信.所有节点需要有相同版本的erlang和rabbitMQ.
* federation(联邦)
> 
    允许单台服务器上的交换机或队列接收发布到另一台服务器上交换机或队列的消息，可以是单独机器或集群。
    federation队列类似于单向点对点连接，消息会在联盟队列之间转发任意次，直到被消费者接受。
    通常使用federation来连接internet上的中间服务器，用作订阅分发消息或工作队列。
>
* The Shovel(铲子|欲善其事)
>
    也是将一个broker队列中的消息转发到另一个交易所上
>

#### 集群
集群所需的所有数据/状态都在所有节点上复制,除了队列.默认情况下,队列的数据只保留在一个节点上.尽管它们可以在其他节点上存取.  
节点间需要通过主机名连接.

1. 三台机器上配置好单机rabbitMQ
2. 复制其中一个节点的erlang cookie到其他节点(通过它确认是否可以相互通信)   
/var/lib/rabbitmq/.erlang.cookie或者$HOME/.erlang.cookie  
注意权限

3. 在各节点机器配置hosts,例如
>
    192.168.1.1 node0
    192.168.1.2 node1
    192.168.1.3 node2
>
4. 建立集群
>
    以hidden0为主节点，在node1上： 
    rabbitmqctl stop_app 
    rabbitmqctl join_cluster rabbit@node0
    rabbitmqctl start_app 
    node2上的操作与node1的雷同
    
    查看集群信息
    rabbitmqctl cluster_status
>

* 还可以配置队列镜像.因为只是集群的话,队列中的数据仍旧只是在单个节点上.
![管理界面配置镜像队列](1.png)




#### 简介
* broker:经纪人,也就是MQ本身.
* produce:生产者:负责发送消息.
* consumer:消费者:负责接收消息.
* exchange:交易所:所有消息都是发送给他的.由它调度给相应队列
* queue:队列:rabbitMQ内部的消息缓冲器.
* routing:路由:队列下的更进一步划分,只有绑定指定路由的队列,才能收到生产者发送的指定了对应路由的消息
* topic:主题:定义了路由的格式,并加入了通配符,就形成了主题.更细化消息的消费者能接收的消息.

* 所以.对于消息的过滤.我们首先是指定交易所,然后指定队列和路由.如果需要进一步细分,就可以使用主题.

#### Exchange 交易所
* 生产者永远无法将消息直接发送到queue队列中,只能发送消息到exchange(交易所).交易所负责处理消息的传递.

* 交易所类型分为
    * direct:直接.消息进入到biding key和routing key完全相同的队列中
    * topic:主题
    * headers:头
    * fanout:分散(展开):直接将收到的消息广播到它知道的所有队列中.无法指定路由
* rabbitMQ默认有一个为 ""的交易所.发送到该交换所,消息将被传递到指定的routeKey指定的队列中.
* 如下声明交易所和类型
>  channel.exchangeDeclare("logs", BuiltinExchangeType.FANOUT);
* 当我们的业务不关心哪个队列时,可以让mq为我们生成队列,名字随机.. 非持久的/独占的/自动删除的
> 	String queueName = channel.queueDeclare().getQueue();
* 将指定的队列绑定到指定的交易所
>  channel.queueBind(queueName, "logs", "");


#### 注意点
* 定义队列和交易所的方法都是幂等的,只有其不存在时才会被创建,而无法修改,要修改只能删除后新建

*  确认模式和Qos(最大为确认数量,也就是预先获取消息数)
>
    确认模式和QoS预取值对消费者吞吐量有显着影响。
    一般来说，增加预取将提高向消费者传递消息的速度。
    自动确认模式可以产生最佳的传送速率。
    但是，在这两种情况下，交付但还未处理的消息的数量也将增加，从而增加消费者RAM消耗。
  
    自动确认模式或无限制预取手动确认模式应谨慎使用。
    消费者在没有确认的情况下消耗大量的消息将导致其所连接的节点上的内存消耗增长。
    找到一个合适的预取值是一个试验和错误的问题，并且会随工作负载而变化。
    100到300范围内的值通常提供最佳的吞吐量，并且不会面临压倒性消费者的重大风险。
>

* 需要持久化时.发送消息需要如下
> channel.basicPublish("",QUEUE_NAME, MessageProperties.PERSISTENT_TEXT_PLAIN,message.getBytes());

* 将消息队列和消息标志为持久,不能100%保证消息不丢失.
在消息队列收到消息,并准备将消息持久化时,如果服务宕机,仍会丢失.
此时,需要生产者确认模式或者事务


* Qos:消息默认轮询分配给每个消费者.但可能出现,某个消费者消费的都是耗时任务,就会导致
分配数量虽然均匀,但执行时间不同,一个已经在空闲了,另一个却还在执行.但后续的消息仍然
是平均分配.
如下代码设置了消息的预先获取数量,也就是通道上允许的最大的未确认任务数,确认完消息后,才能再获取.
不会产生因轮询和消费时间不均导致的大量任务堆积在一个消费者上.
即使在手动确认模式下， QoS预取设置对使用basic.get（“pull API”）获取的消息也没有影响。
>
		/**
		 * 通道上允许的最大的未确认任务数.
		 * 达到顶峰后,mq会停止向该通道传递消息,直到有消息被确认或拒绝
		 * 0表示无限
		 *
		 * 1. 数量
		 * 2. 是否全局.如果是true,该设置将被整个通道使用,而不是单个消费者
		 * 不传,默认false,表示每个消费者的最大未确认数都为1,
		 * 为true,则表示多个消费者的最大未确认数之和为1.
		 *
		 * 如下设置.表示每个消费者的数是10,但多个消费者最大数之和不能超过15
		 * channel.basicQos(10,false);
		 * channel.basicQos(10,true);
		 */
		channel.basicQos(1);
>

* 设置发送消息的属性
>
    /**
     * 定义发送的属性
     * 包括消息格式/是否持久化等
     * 还有消息id也可以自定义
     */
    AMQP.BasicProperties properties = new AMQP.BasicProperties().builder()
    		.contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)//内容类型,此处可以使用MediaType
    		.deliveryMode(JmsProperties.DeliveryMode.NON_PERSISTENT.getValue())//消息为持久化(2).或者瞬态(任何其他值)
    		.headers()//该可以传递一个map<String,Object>集合,类似http请求头
    		.expiration("60000")//消息过期时间
    		.build();
    //发送时传入
    channel.basicPublish("",QUEUE_NAME,properties,message.getBytes());
>

* 主动拉取消息
>   
    boolean autoAck = false;
    GetResponse response = channel.basicGet(queueName, autoAck);
    if (response == null) {
        // No message retrieved.
    } else {
        AMQP.BasicProperties props = response.getProps();
        byte[] body = response.getBody();
        long deliveryTag = response.getEnvelope().getDeliveryTag();
>

* mandatory标志.在basicPublish()重载方法中传入.如果消息无法到达.例如路由不存在等.
可以通过如下方式监听.
>
    channel.addReturnListener(new ReturnListener() {
        public void handleReturn(int replyCode,
                                      String replyText,
                                      String exchange,
                                      String routingKey,
                                      AMQP.BasicProperties properties,
                                      byte[] body)
        throws IOException {
            ...
        }
    });
>

* 可以尝试使用nio,并设置其属性
>
    ConnectionFactory connectionFactory = new ConnectionFactory();
    connectionFactory.useNio();
    
    connectionFactory.setNioParams(new NioParams().setNbIoThreads(4));
>

* 设置自动恢复连接设置,以及恢复连接的间隔
>
    ConnectionFactory factory = new ConnectionFactory（）;
    factory.setAutomaticRecoveryEnabled（true）;
    factory.setNetworkRecoveryInterval（10000）;

>

* 可以定义多个连接地址
>
    Address[] addresses = {new Address("192.168.1.4"), new Address("192.168.1.5")};
    factory.newConnection(addresses);
>

### 官方标准api 学习 base包
##### hello(base.hello) 发送/消费队列中的消息  
1. 依赖
>
            <dependency>
                <groupId>com.rabbitmq</groupId>
                <artifactId>amqp-client</artifactId>
                <version>5.1.1</version>
            </dependency>
>
2. 发送消息到队列.详见Send类
3. 成功后可以进入ip:15672管理界面查看.这个管理界面看的我很舒服
4. 消费消息,见Receive类.
5. 我开了50个生产者线程,每个线程发送10W消息,然后一个消费者消费..管理页面那曲线看得我很舒畅.信息很详细

#### work queue (base.workQueue) 发送任务到队列,模拟需要长时间消费的任务.
* 开启多个消费者监听一个队列.默认情况下,消息会轮询给每个消费者.
* 为防止消费者发生异常导致消息处理失败,消息还丢失了的情况.
mq支持消息确认的模式.如下.创建消费者时.不自动确认.即可
>   
    /**
     * 在通道中注入消费者,
     * 中间的参数为 是否自动确认(告诉队列,消费成功)
     */
    channel.basicConsume(QUEUE_NAME, false, consumer);
>
* 我试验了下.开启手动确认模式,消费完成后却不确认.  
    此时队列中多出两条未确认消息.但一直没有重新发给我的消费者.  
    在我将消费者重启后,才重新收到了两条消息..很好.很智能..
    
* 当一个队列被第一次定义后,将无法更改属性.也就是说.执行如下代码,只有第一行生效
> channel.queueDeclare(QUEUE_NAME, false, false, false, null);
> channel.queueDeclare(QUEUE_NAME, true, false, false, null);

* 消息确认或拒绝的几种方法
>
        /**
         * 自动确认模式下,消息发送后自动默认为确认,
         * 可以提高吞吐量.
         * 手动确认可以限制每次的预先获取数量,但自动确认没有.
         * 如果消息过多,可能导致服务器宕机.
         * 所以需要保证生产者的消息的稳定性
         */
        /**
         * 手动确认.
         * 1. 该交付标签.也就是消息id
         * 2. 是该消费者缓存的全部消息,还是只是当前消息.(也就是批量操作)
         * 还有就是.如果之前处理了交付标签为1,2,3的任务,还未确认,该次处理为4,那么将该multiple
         * 设置为true,将会将1,2,3和4都设置为确认
         */
        channel.basicAck(envelope.getDeliveryTag(),false);
        /**
         * 手动拒绝
         * 1.消息id
         * 2.是该消费者缓存的全部消息,还是只是当前消息.
         * 3.是否删除消息,还是让mq重发消息. true:重发
         */
        channel.basicNack(envelope.getDeliveryTag(),false,true);
        /**
         * 手动拒绝单个消息
         * 1.消息id
         * 3.是否删除消息,还是让mq重发消息. true:重发
         */
        channel.basicReject(envelope.getDeliveryTag(),true);
>
    
#### 发布/订阅模式
之前的模式是每条消息只会被一个消费者消费.
该模式,一条消息可以被若干消费者同时消费.

为了说明该模式.官方文档上...建立了一个简单的日志系统.
发布者发布日志后,会被两个消费者消费.一个负责保存日志,一个负责将日志在屏幕展示

* 在生产者(发布者)定义交易所,并将消息发往交易所
>
    //声明交易所
    channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT);
    //发送消息到 自定义的交易所,不指定队列名
    channel.basicPublish(EXCHANGE_NAME, "", null, message.getBytes());
>
* 消费者(订阅者)定义自己的通道(可以是临时的,自动删除的),将其绑定到指定交易所
>
    //声明交易所
    channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT);
    //生成一个无名队列. 非持久的/独占的/自动删除的
    String queueName = channel.queueDeclare().getQueue();
    //绑定队列到交易所
    channel.queueBind(queueName, EXCHANGE_NAME, "");
>
* 然后.发往该交易所的消息就会被发往所有和该交易所绑定的通道.(因为该交易所的类型是fanout)
* 如果需要增加订阅者,无非就是多启动一个而已.

#### 路由 Routing
在fanout类型的交易所中,路由无法使用,因为该类型的交易所会将消息发往所有和该交易所绑定的队列中去.  
如果使用direct类型的交易所,就需要路由匹配的队列才能接收到对应消息.  
所以.路由.是比队列更细致的指定消费者.

例如,我们需要将上面的日志系统进一步细分.指定级别的日志发送到指定的订阅者处.
可以根据日志级别划分路由.然后某个消费者需要消费哪些等级的日志,就可以直接绑定路由即可

* 声明一个类型为direct的交易所
> channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT);
* 发送时指定路由为error 或 info 或 warn,
> channel.basicPublish(EXCHANGE_NAME, "error", null, message.getBytes());
* 消费者绑定该交易所和自己的队列,并绑定指定的若干路由
>
    //绑定指定的若干路由到该通道
    for (String routing : routings) {
    	//绑定队列到交易所,并指定路由
    	channel.queueBind(queueName, EXCHANGE_NAME, routing);
    }
>
* 这样.就可以通过路由更细粒度的划分消费者.

#### 主题 topic - 更进一步细分的路由
就是将交换所的类型设置为topic,并且指定了路由的格式,并可以在路由中使用通配符.

如果需要将日志系统再划分,例如不仅要区分日志级别,还需要区分日志来源,例如A/B/C/D项目中的日志,
并指定不同的消费者.就可以使用主题.

* 使用主题后,路由不能任意起名,需要类似 "a.b.c","user.name"这样,最多255个字节.
* 之后,在消费者将队列绑定路由时,可以使用通配符指定路由.
>
    *(星号):代表一个单词
    #(井号):代表零个或多个单词
    使用# ,表示匹配所有路由.
    例子:
    user.* 匹配:  user.name   user.age  不匹配: user.age.name 
    user.# 匹配: user.age.name  user.age
>

* 这个就不打代码了.和路由基本没差


#### 远程过程调用 RPC
可以使用rabbitMQ实现远程过程调用

* 调用方
>
    //定义一个用来存储回调数据的队列
    String callbackQueue = channel.queueDeclare().getQueue();
    String uuid = UUID.randomUUID().toString();
    //定义回调队列和本次调用的id
    AMQP.BasicProperties properties = new AMQP.BasicProperties().builder()
    		.correlationId(uuid)//每个消息的id
    		.replyTo(callbackQueue)//绑定回调队列
    		.build();
    //发送消息
    channel.basicPublish("",QUEUE_NAME,properties,"100".getBytes());
    //消费处理回调队列 的回调挤结果
    channel.basicConsume(callbackQueue,true,new DefaultConsumer(channel){
    	@Override
    	public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
    		log.info("收到回调结果:{},id:{}",new String(body),properties.getCorrelationId());
    	}
    });
>

* 远程过程
>
    //返回结果
    String result = "";
    //传递过来的数字
    String message = new String(body, Encoder.UTF_8);
    int n = Integer.parseInt(message);
    //计算结果
    result += a(n);
    log.info("接收到消息:{},consumerTag:{},envelope:{}",message,consumerTag,envelope);
    //回调时需要将id返回,
    AMQP.BasicProperties properties1 = new AMQP.BasicProperties().builder()
    		.correlationId(properties.getCorrelationId())
    		.build();
    //将消息发送给调用者定义的回调队列
    channel.basicPublish("",properties.getReplyTo(),properties1,result.getBytes());
>

* 其逻辑就是.调用方在发送的消息属性中,指定回调的队列名和该消息id.然后发送给rpc队列.调用.
* 远程过程方.就从rpc队列中获取消息.然后执行方法.将结果.带上传入的消息id,传回调用方指定的回调队列即可.


#### 事务
.找遍官方文档,没有找到对事务的介绍.随手看了篇博客...贼简单.
>
    //开启事务
    channel.txSelect();
    //提交事务
    channel.txCommit();
    //回滚事务
    channel.txRollback();
>

#### 生产者确认模式
消费者确认模式是保证每条消息都被消费者成功消费.
该模式是保证消息一定到达mq.
使用事务可以有更好的保证,但是性能会进一步降低.
事务和该模式不能共存.

* 开启确认模式
>
    	/**
    	 * 将该通道声明为生产者确认模式
    	 */
    	channel.confirmSelect();
>
* 消息id的获取方式(在发送每条消息前调用),通过该id判断是哪个消息确认或未确认
>
    //获取下一个发送的消息id,可用来在确认回调时判断是哪条消息没有成功
    long nextPublishSeqNo = channel.getNextPublishSeqNo();
>
* 几种获取确认结果的方式
    * 同步等待:发送消息后,同步等待确认结果.可以设置超时时间
    >
        boolean b = channel.waitForConfirms();
    >
    * 同步批量等待:发送若干消息后,调用,等待一个成功或失败的结果
    >
        //同步批量等待确认结果,就算只有一个失败了,也会返回false.
        channel.waitForConfirmsOrDie();
    >   
    * 异步监听器,监听结果.
    >
            //开启监听器,异步等待确认结果
            channel.addConfirmListener(new ConfirmListener() {
            	//成功确认结果
            	@Override
            	public void handleAck(long deliveryTag, boolean multiple) throws IOException {
            		//此处的multiple和消费者确认中我们自己传递的参数是同一种,表示是否批量
            		log.info("发送成功.deliveryTag:{},multiple:{}",deliveryTag,multiple);
            	}
        
            	//未确认结果
            	@Override
            	public void handleNack(long deliveryTag, boolean multiple) throws IOException {
            		log.info("发送失败.deliveryTag:{},multiple:{}",deliveryTag,multiple);
            	}
            });
    >

* 我个人认为,将同步批量等待放入一个异步线程,同时传入每批发送的消息.是比较好的处理方法.  
这样,在一批消息失败后,可以直接再次调用,重新发送.(..如果往细了考虑,还需要停止之前的发送主线程...)
否则.如果使用异步监听.就必然要多维护一组消息和消息id对应的集合.  
  


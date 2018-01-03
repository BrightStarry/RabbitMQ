package com.zx.rabbitmq.base.routing;

import com.rabbitmq.client.*;
import com.sun.xml.internal.fastinfoset.Encoder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * author:ZhengXing
 * datetime:2018/1/3 0003 10:50
 * 消费者 接收消息
 */
@Slf4j
public class Receive {
	//交易所名字
	private static final String EXCHANGE_NAME = "logs";

	//传入参数为 要绑定的路由 和 该消费者线程id
	public static void receive(int id, String[] routings) throws IOException, TimeoutException {
		//创建连接工厂
		ConnectionFactory connectionFactory = new ConnectionFactory();
		//设置主机或ip
		connectionFactory.setHost("106.14.7.29");
		//设置端口
		connectionFactory.setPort(5672);
		//创建连接
		Connection connection = connectionFactory.newConnection();
		//创建通道
		Channel channel = connection.createChannel();


		//声明交易所,类型为直接
		channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT);
		//生成一个无名队列. 非持久的/独占的/自动删除的
		String queueName = channel.queueDeclare().getQueue();

		//绑定指定的若干路由到该通道
		for (String routing : routings) {
			//绑定队列到交易所,并指定路由
			channel.queueBind(queueName, EXCHANGE_NAME, routing);
		}

		//创建消费者
		DefaultConsumer consumer = new DefaultConsumer(channel){
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
				String message = new String(body, Encoder.UTF_8);
				log.info("id:{},接收到消息:{},consumerTag:{},envelope:{}",id,message,consumerTag,envelope);
			}
		};

		channel.basicConsume(queueName, true, consumer);


	}

	/**
	 * 开启两个消费者, 分别订阅 error路由和info/warn路由的消息
	 * @param args
	 * @throws IOException
	 * @throws TimeoutException
	 */
	public static void main(String[] args) throws IOException, TimeoutException {
		new Thread(()->{
			try {
				receive(0,new String[]{"error"});
			} catch (IOException | TimeoutException e) {
				e.printStackTrace();
			}
		}).start();
		new Thread(()->{
			try {
				receive(1,new String[]{"info","warn"});
			} catch (IOException | TimeoutException e) {
				e.printStackTrace();
			}
		}).start();
	}
}

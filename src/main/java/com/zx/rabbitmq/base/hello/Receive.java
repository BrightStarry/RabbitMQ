package com.zx.rabbitmq.base.hello;

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
	//队列名
	private static final String QUEUE_NAME = "zx";

	public static void receive() throws IOException, TimeoutException {
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
		//声明队列
		channel.queueDeclare(QUEUE_NAME, false, false, false, null);

		//创建消费者
		DefaultConsumer consumer = new DefaultConsumer(channel){
			/**
			 * 处理交付 ,也就是消费消息
			 * @param consumerTag 消费者标签
			 * @param envelope 信封类,包含了交付者标签/是否重新交付/路由key等信息
			 * @param properties 基本属性
			 * @param body 消息
			 * @throws IOException
			 */
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
				String message = new String(body, Encoder.UTF_8);
				log.info("接收到消息:{},consumerTag:{},envelope:{}",message,consumerTag,envelope);
			}
		};

		/**
		 * 在通道中注入消费者,
		 * 中间的参数为 是否自动确认(告诉队列,消费成功)
		 */
		channel.basicConsume(QUEUE_NAME, true, consumer);


		log.info("消费者结束");
	}

	public static void main(String[] args) throws IOException, TimeoutException {
		receive();
	}
}

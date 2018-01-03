package com.zx.rabbitmq.base.publishSubscription;

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


		//声明交易所
		channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT);
		//生成一个无名队列. 非持久的/独占的/自动删除的
		String queueName = channel.queueDeclare().getQueue();
		//绑定队列到交易所
		channel.queueBind(queueName, EXCHANGE_NAME, "");

		//创建消费者
		DefaultConsumer consumer = new DefaultConsumer(channel){
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
				String message = new String(body, Encoder.UTF_8);
				log.info("接收到消息:{},consumerTag:{},envelope:{}",message,consumerTag,envelope);
			}
		};

		channel.basicConsume(queueName, true, consumer);


	}

	public static void main(String[] args) throws IOException, TimeoutException {
		receive();
	}
}

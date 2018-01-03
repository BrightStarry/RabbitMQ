package com.zx.rabbitmq.base.rpc;

import com.rabbitmq.client.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.jms.JmsProperties;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * author:ZhengXing
 * datetime:2018/1/3 0003 10:30
 * 生产者发送类
 */
@Slf4j
public class Send {


	private static final String QUEUE_NAME = "zx";

	public static void send() throws IOException, TimeoutException, InterruptedException {
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



		Thread.sleep(20000);
		//关闭
		channel.close();
		connection.close();


	}

	public static void main(String[] args) throws IOException, TimeoutException, InterruptedException {
		send();
	}
}

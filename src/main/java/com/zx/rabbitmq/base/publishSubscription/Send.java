package com.zx.rabbitmq.base.publishSubscription;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * author:ZhengXing
 * datetime:2018/1/3 0003 10:30
 * 生产者发送类
 */
@Slf4j
public class Send {

	//交易所名字
	private static final String EXCHANGE_NAME = "logs";

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


		//声明交易所
		channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT);

		//发送消息
		String message = "zhengxing";


		//发送消息到 自定义的交易所,不指定路由
		channel.basicPublish(EXCHANGE_NAME, "", null, message.getBytes());
		log.info("发送消息:{},成功.", message);


		//关闭
		channel.close();
		connection.close();

	}

	public static void main(String[] args) throws IOException, TimeoutException, InterruptedException {
		send();
	}
}

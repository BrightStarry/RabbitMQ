package com.zx.rabbitmq.base.rpc;

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
			 * 模拟一个远程过程. 计算完成后.
			 */
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
				log.info("---------");
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
			}
		};
		channel.basicConsume(QUEUE_NAME, true, consumer);


		log.info("消费者开启");
	}

	private static int a(int n) {
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return n += 1000;
	}

	public static void main(String[] args) throws IOException, TimeoutException {
		receive();
	}
}

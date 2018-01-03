package com.zx.rabbitmq.base.hello;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jms.JmsProperties;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * author:ZhengXing
 * datetime:2018/1/3 0003 10:30
 * 生产者发送类
 */
@Slf4j
public class Send {


	//队列名
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

		/**
		 * 声明一个队列.该方法是幂等的.只有队列不存在时才会被创建
		 * 1.队列名
		 * 2.是否持久的
		 * 3.是否独占的(注册到该connection)
		 * 4. 是否自动删除(服务器将在不再使用时删除它)
		 * 5.Map<String, Object> 该队列的其他属性
		 */
		channel.queueDeclare(QUEUE_NAME, false, false, false, null);


		/**
		 * 定义发送的属性
		 * 包括消息格式/是否持久化等
		 * 还有消息id也可以自定义
		 */
		AMQP.BasicProperties properties = new AMQP.BasicProperties().builder()
				.contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)//内容类型,此处可以使用MediaType
				.deliveryMode(JmsProperties.DeliveryMode.NON_PERSISTENT.getValue())//消息为持久化(2).或者瞬态(任何其他值)
				.build();


		/**
		 * 1. 将消息发布到... (后面的发布订阅模式)
		 * 2. 路由key(此处是队列名)
		 * 3. 其他属性, route header(路由头)等
		 * 4. 消息
		 */
		for (int i = 0; i < 1; i++) {
			new Thread(()->{
				for (int j = 0; j < 1; j++) {
					try {
						String message = "zhengxing" + j;
							channel.basicPublish("",QUEUE_NAME,null,message.getBytes());
						log.info("发送消息:{},成功.",message);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}).start();
		}


		Thread.sleep(1000);
		//关闭
		channel.close();
		connection.close();

	}

	public static void main(String[] args) throws IOException, TimeoutException, InterruptedException {
		send();
	}
}

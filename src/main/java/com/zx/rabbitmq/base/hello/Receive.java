package com.zx.rabbitmq.base.hello;

import com.rabbitmq.client.*;
import com.rabbitmq.client.impl.DefaultExceptionHandler;
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

		//设置异常处理器,里面有许许许许许许多多捕获各类异常的方法,此处使用适配器实现类
		connectionFactory.setExceptionHandler(new DefaultExceptionHandler(){
			//处理连接恢复时的异常.
			@Override
			public void handleConnectionRecoveryException(Connection conn, Throwable exception) {
				log.info("连接恢复异常");
			}
		});
		//心跳间隔
		connectionFactory.setRequestedHeartbeat(60);
		//自动恢复
		connectionFactory.setAutomaticRecoveryEnabled(true);
		//自动恢复间隔
		connectionFactory.setNetworkRecoveryInterval(10);
		//创建连接
		Connection connection = connectionFactory.newConnection();
		//当开启连接自动恢复后,可以通过如下方式,设置监听器(也可以设置在通道处)
		Recoverable recoverable = (Recoverable) connection;
		recoverable.addRecoveryListener(new RecoveryListener() {
			//当自动恢复完成后调用
			@Override
			public void handleRecovery(Recoverable recoverable) {
				log.info("自动恢复完成");
			}

			//开始自动恢复前调用.此时未执行任何自动恢复步骤
			@Override
			public void handleRecoveryStarted(Recoverable recoverable) {
				log.info("自动恢复开始");
			}
		});


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
				log.info("1---接收到消息:{},consumerTag:{},envelope:{}",message,consumerTag,envelope);
			}

			@Override
			public void handleConsumeOk(String consumerTag) {
				log.info("消费者注册成功");
			}

			@Override
			public void handleCancelOk(String consumerTag) {
				log.info("消费者取消注册成功");
			}

			@Override
			public void handleCancel(String consumerTag) throws IOException {
				log.info("消费者取消注册");
			}

			@Override
			public void handleRecoverOk(String consumerTag) {
				log.info("再次获取成功");
			}


		};



		/**
		 * 在通道中注入消费者,
		 * 中间的参数为 是否自动确认(告诉队列,消费成功)
		 */
		String s = channel.basicConsume(QUEUE_NAME, true, consumer);




		log.info("消费者开始");
	}

	public static void main(String[] args) throws IOException, TimeoutException {
		receive();
	}
}

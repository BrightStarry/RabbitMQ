package com.zx.rabbitmq.base.workQueue;

import com.rabbitmq.client.*;
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
		 * 将该通道声明为生产者确认模式
		 */
		channel.confirmSelect();

		//发送消息
		/**
		 * 1. 将消息发布到... (后面的发布订阅模式)
		 * 2. 路由key(此处是队列名)
		 * 3. 其他属性, route header(路由头)等
		 * 4. 消息
		 */
		String message = "zhengxing..";

		//获取下一个发送的消息id,可用来在确认回调时判断是哪条消息没有成功
		long nextPublishSeqNo = channel.getNextPublishSeqNo();

		channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
		/**
		 * 如果是持久化的消息队列,需要如下传输消息
		 */
//		channel.basicPublish("",QUEUE_NAME, MessageProperties.PERSISTENT_TEXT_PLAIN,message.getBytes());


		//同步等待确认结果,可以设置超时时间
//		boolean b = channel.waitForConfirms();

		//同步批量等待确认结果,就算只有一个失败了,也会返回false.
//		channel.waitForConfirmsOrDie();

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


		log.info("发送消息:{},", message);


		//关闭
		channel.close();
		connection.close();

	}

	public static void main(String[] args) throws IOException, TimeoutException, InterruptedException {
		send();
	}
}

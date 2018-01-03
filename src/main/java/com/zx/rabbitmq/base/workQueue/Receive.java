package com.zx.rabbitmq.base.workQueue;

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

	public static void receive(int id) throws IOException, TimeoutException {
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

		//创建消费者
		DefaultConsumer consumer = new DefaultConsumer(channel){
			/**
			 * 处理交付 ,也就是消费消息
			 * @param consumerTag 消费者标签
			 * @param envelope 信封类,包含了交付者标签(确认了,是每个消费者独立的id,递增,重启后重新累加)/是否重新交付/路由key等信息
			 * @param properties 基本属性
			 * @param body 消息
			 * @throws IOException
			 */
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
				String message = new String(body, Encoder.UTF_8);

				log.info("id:{},接收到消息:{},consumerTag:{},envelope:{}",id,message,consumerTag,envelope);
				try {
					//根据消息的"."模拟执行任务的时间
					doWork(message);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

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
//				channel.basicNack(envelope.getDeliveryTag(),false,true);
				/**
				 * 手动拒绝单个消息
				 * 1.消息id
				 * 3.是否删除消息,还是让mq重发消息. true:重发
				 */
//				channel.basicReject(envelope.getDeliveryTag(),true);

				log.info("id:{},消息:{},任务执行完成",id,message);

			}
		};

		/**
		 * 在通道中注入消费者,
		 * 中间的参数为 是否自动确认(告诉队列,消费成功)
		 * 此处设置为手动提交
		 */
		channel.basicConsume(QUEUE_NAME, false, consumer);


		log.info("id:{},消费者开始",id);
	}


	/**
	 * 计算任务消息中包含的....的个数.每个"."表示需要消耗1s处理该任务
	 */
	private static void doWork(String task) throws InterruptedException {
		for (char ch: task.toCharArray()) {
			if (ch == '.') Thread.sleep(1000);
		}
	}

	public static void main(String[] args) throws IOException, TimeoutException {
		//运行两个消费者
		new Thread(()->{
			try {
				receive(0);
			} catch (IOException | TimeoutException e) {
				e.printStackTrace();
			}
		}).start();
		new Thread(()->{
			try {
				receive(1);
			} catch (IOException | TimeoutException e) {
				e.printStackTrace();
			}
		}).start();

	}


}

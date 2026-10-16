package com.bookstore.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.bookstore.entity.Order;
import com.bookstore.entity.OrderItem;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderMailService {

	private final Environment environment;
	private final ObjectProvider<JavaMailSender> mailSender;

	public boolean notifyEstablished(Order order) {
		String host = environment.getProperty("spring.mail.host", "");
		String from = environment.getProperty("spring.mail.username", "");
		JavaMailSender sender = mailSender.getIfAvailable();
		if (host.isBlank() || from.isBlank() || sender == null || order.getUser() == null) {
			return false;
		}
		try {
			MimeMessage message = sender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
			helper.setFrom(from);
			helper.setTo(order.getUser().getEmail());
			helper.setSubject("書店訂單 #" + order.getId() + " 已成立");
			helper.setText(body(order));
			sender.send(message);
			return true;
		} catch (Exception ex) {
			log.warn("訂單通知信沒有寄出：{}", ex.getMessage());
			return false;
		}
	}

	private String body(Order order) {
		StringBuilder text = new StringBuilder();
		text.append("你的訂單 #").append(order.getId()).append(" 已成立。\n");
		text.append("貨態：已抵達\n\n");
		if (order.getOrderItems() != null) {
			for (OrderItem item : order.getOrderItems()) {
				text.append(item.getTitle()).append(" × ").append(item.getQuantity()).append('\n');
			}
		}
		text.append("\n合計 NT$ ").append(order.getTotalPrice()).append("\n\n");
		text.append("收件人：").append(order.getRecipientName()).append('\n');
		text.append("電話：").append(order.getRecipientPhone()).append('\n');
		text.append("地址：").append(order.getShippingAddress()).append('\n');
		return text.toString();
	}
}

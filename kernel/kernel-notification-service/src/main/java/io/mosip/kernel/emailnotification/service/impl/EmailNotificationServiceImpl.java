package io.mosip.kernel.emailnotification.service.impl;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import io.mosip.kernel.core.templatemanager.spi.TemplateManager;
import io.mosip.kernel.emailnotification.constant.ApiName;
import io.mosip.kernel.emailnotification.exception.ApisResourceAccessException;
import io.mosip.kernel.emailnotification.util.HTMLFormatter;
import io.mosip.kernel.emailnotification.util.TemplateGenerator;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import io.mosip.kernel.core.notification.spi.EmailNotification;
import io.mosip.kernel.emailnotification.constant.MailNotifierConstants;
import io.mosip.kernel.emailnotification.dto.ResponseDto;
import io.mosip.kernel.emailnotification.exception.NotificationException;
import io.mosip.kernel.emailnotification.util.EmailNotificationUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service implementation class for {@link EmailNotification}.
 * 
 * @author Sagar Mahapatra
 * @since 1.0.0
 */
@Service
public class EmailNotificationServiceImpl implements EmailNotification<MultipartFile[], ResponseDto> {

	Logger LOGGER = LoggerFactory.getLogger(EmailNotificationServiceImpl.class);
	/**
	 * Autowired reference for {@link JavaMailSender}
	 */
	@Autowired
	private JavaMailSender emailSender;

	/**
	 * Autowired reference for {@link EmailNotificationUtils}
	 */
	@Autowired
	EmailNotificationUtils emailNotificationUtils;

	/**
	 * Optionally an email address can be configured.
	 */
	@Nullable
	@Value("${mosip.kernel.notification.email.from:#{null}}")
	private String fromEmailAddress;
	
	@Value("${mosip.kernel.mail.proxy-mail:false}")
	private boolean isProxytrue;

	@Value("${mosip.kernel.mail.content.html.enable:true}")
	private boolean isHtmlEnable;

	@Value("${mosip.kernel.mail.content.template.code:#{null}}")
	private String templateTypeCode;

	@Autowired
	TemplateGenerator templateGenerator;

	@Autowired
	HTMLFormatter htmlFormatter;
	/**
	 * SendEmail
	 * 
	 * @param mailTo
	 *            email address to which mail will be sent.
	 * @param mailCc
	 *            email addresses to be cc'ed.
	 * @param mailSubject
	 *            the subject.
	 * @param mailContent
	 *            the content.
	 * @param attachments
	 *            the attachments.
	 * @return the response dto.
	 */
	@Override
	public ResponseDto sendEmail(String[] mailTo, String[] mailCc, String mailSubject, String mailContent,
			MultipartFile[] attachments) {
		ResponseDto dto = new ResponseDto();
		LOGGER.info("To Request : " + String.join(",", mailTo));
		LOGGER.info("Template Code : " + String.join(",", templateTypeCode));
		LOGGER.info("Proxy : " + isProxytrue);
		LOGGER.info("Mail Subject : " + String.join(",", mailSubject));
		LOGGER.info("Mail Content : " + String.join(",", mailContent));
		LOGGER.info("Attachment : " +  (attachments != null));
		
		try {
			if(templateTypeCode != null) {
				Map<String, Object> attributes = new LinkedHashMap<>();
				attributes.put("mailContent",htmlFormatter.formatText(mailContent));
				InputStream stream = templateGenerator.getTemplate(templateTypeCode, attributes, MailNotifierConstants.LANGUAGE.getValue());
				mailContent = IOUtils.toString(stream, "UTF-8");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ApisResourceAccessException e) {
			e.printStackTrace();
		}

		LOGGER.info("Mail Content 2 : " + mailContent);
		if(!isProxytrue) {
			LOGGER.info("Sending Mail to Email");
			send(mailTo, mailCc, mailSubject, mailContent, attachments);
		}

		dto.setStatus(MailNotifierConstants.MESSAGE_SUCCESS_STATUS.getValue());
		dto.setMessage(MailNotifierConstants.MESSAGE_REQUEST_SENT.getValue());
		return dto;
	}

	@Async
	private void send(String[] mailTo, String[] mailCc, String mailSubject, String mailContent,
			MultipartFile[] attachments) {
		EmailNotificationUtils.validateMailArguments(fromEmailAddress, mailTo, mailSubject, mailContent);
		/**
		 * Creates the message.
		 */
		MimeMessage message = emailSender.createMimeMessage();
		MimeMessageHelper helper;
		try {
			helper = new MimeMessageHelper(message, true);
			/**
			 * Sets to, subject, content.
			 */
			helper.setTo(mailTo);
			
			if (null != fromEmailAddress){
				helper.setFrom(fromEmailAddress);
			}
			if (mailCc != null) {
				helper.setCc(mailCc);
			}
			if (mailSubject != null) {
				helper.setSubject(mailSubject);
			}
			helper.setText(mailContent,isHtmlEnable);
		} catch (MessagingException exception) {
			throw new NotificationException(exception);
		}
		if (attachments != null) {
			/**
			 * Adds attachments.
			 */
			emailNotificationUtils.addAttachments(attachments, helper);
		}
		/**
		 * Sends the mail.
		 */
		emailNotificationUtils.sendMessage(message, emailSender);
	}
}

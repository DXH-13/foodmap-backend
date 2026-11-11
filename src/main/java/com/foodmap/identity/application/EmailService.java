package com.foodmap.identity.application;

import com.foodmap.identity.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Gửi email xác minh tài khoản và đặt lại mật khẩu.
 *
 * <p>Dev dùng Mailpit (localhost:1025, xem {@code application.yml}) — mở UI Mailpit
 * là thấy email. Gửi lỗi thì <b>ném lỗi ra ngoài</b>, không nuốt lặng: service gọi
 * sẽ cho transaction rollback (đăng ký thì user chưa được tạo, client nhận 500 và
 * thử lại được). Token gốc chỉ xuất hiện trong nội dung email, không bao giờ nằm
 * trong response body.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    /** Mailpit chấp nhận mọi địa chỉ gửi trong môi trường dev. */
    private static final String FROM_ADDRESS = "noreply@foodmap.vn";

    private final JavaMailSender mailSender;
    private final MessageSource messages;

    public EmailService(JavaMailSender mailSender, MessageSource messages) {
        this.mailSender = mailSender;
        this.messages = messages;
    }

    /** Gửi mã xác minh email cho tài khoản mới đăng ký hoặc yêu cầu gửi lại. */
    public void sendVerificationEmail(User user, String rawToken) {
        Locale locale = localeOf(user);
        String subject = messages.getMessage("email.verify.subject", null, locale);
        String body = messages.getMessage("email.verify.body",
                new Object[]{rawToken, user.getDisplayName()}, locale);
        send(user.getEmail(), subject, body);
    }

    /** Gửi liên kết đặt lại mật khẩu (trong email chỉ có mã token, không có mật khẩu). */
    public void sendPasswordResetEmail(User user, String rawToken) {
        Locale locale = localeOf(user);
        String subject = messages.getMessage("email.reset_password.subject", null, locale);
        String body = messages.getMessage("email.reset_password.body",
                new Object[]{rawToken, user.getDisplayName()}, locale);
        send(user.getEmail(), subject, body);
    }

    private void send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(FROM_ADDRESS);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
        } catch (MailException ex) {
            // Không nuốt lỗi gửi mail: log rõ và để exception lan ra — service gọi
            // sẽ rollback và client nhận 500 INTERNAL_ERROR kèm traceId.
            log.error("Khong gui duoc email toi {}", to, ex);
            throw ex;
        }
    }

    private Locale localeOf(User user) {
        return "en".equals(user.getPreferredLocale()) ? Locale.ENGLISH : Locale.of("vi");
    }
}

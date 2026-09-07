package com.banking.transactionservice.service;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendSimpleEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("shubhamnishad110@gmail.com");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) {
            return accountNumber;
        }
        String lastFour = accountNumber.substring(accountNumber.length() - 4);
        String masked = "*".repeat(accountNumber.length() - 4);
        return masked + lastFour;
    }

    public void sendTransactionNotification(String to, String transactionType, BigDecimal amount,
                                            BigDecimal availableBalance, String accountNumber,
                                            String counterpartyAccountNumber, LocalDateTime transactionDate) {
        String subject = transactionType + " Successful - ₹" + amount;

        StringBuilder body = new StringBuilder();
        body.append("Transaction Type: ").append(transactionType).append("\n");
        body.append("Amount: ₹").append(amount).append("\n");
        body.append("Account: ").append(maskAccountNumber(accountNumber)).append("\n");
        if (counterpartyAccountNumber != null) {
            body.append("To/From Account: ").append(maskAccountNumber(counterpartyAccountNumber)).append("\n");
        }
        body.append("Available Balance: ₹").append(availableBalance).append("\n");
        body.append("Date: ").append(formatDate(transactionDate));

        sendSimpleEmail(to, subject, body.toString());
    }

    public void sendFraudAlertEmail(String to, String transactionType, BigDecimal amount,
                                    Integer riskScore, String flagReason) {
        String subject = "Security Alert: Unusual Activity Detected";

        StringBuilder body = new StringBuilder();
        body.append("We detected unusual activity on your account.\n\n");
        body.append("Transaction Type: ").append(transactionType).append("\n");
        body.append("Amount: ₹").append(amount).append("\n");
        body.append("Risk Score: ").append(riskScore).append("/100\n");
        body.append("Reason: ").append(flagReason).append("\n\n");
        body.append("If this was you, no action is needed. If you don't recognize this activity, please contact support immediately.");

        sendSimpleEmail(to, subject, body.toString());
    }

    private String formatDate(LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
        return dateTime.format(formatter);
    }

    public void sendOtpEmail(String to, String otp, BigDecimal amount) {
        String subject = "OTP Verification Required";
        StringBuilder body = new StringBuilder();
        body.append("We detected unusual activity on your account. Please verify to process this transaction.\n\n");
        body.append("Amount: ₹").append(amount).append("\n");
        body.append("OTP: ").append(otp).append("\n");
        body.append("This code expires in 5 minutes.");

        sendSimpleEmail(to, subject, body.toString());
    }



}
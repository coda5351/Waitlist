package com.waitlist.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.waitlist.model.EventName;
import com.google.auth.oauth2.GoogleCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Sends push notifications via Firebase Cloud Messaging (FCM) using the
 * Firebase Admin SDK.
 */
@Service
public class FirebaseService {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseService.class);

    private boolean initialized;

    public FirebaseService(@Value("${firebase.service-account-json:}") String serviceAccountJson) {
        serviceAccountJson = loadRawFileContent("firebase-adminsdk.json");

        if (serviceAccountJson == null || serviceAccountJson.isBlank()) {
            logger.warn("Firebase service account JSON is not configured; push notifications will be disabled");
            this.initialized = false;
            return;
        }

        try (InputStream serviceAccount = new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8))) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            FirebaseApp.initializeApp(options);
            this.initialized = true;
        } catch (Exception e) {
            logger.error("Failed to initialize Firebase Admin SDK", e);
            this.initialized = false;
        }
    }

    private String loadRawFileContent(String filePath) {
        java.nio.file.Path path = java.nio.file.Paths.get(filePath);
        if (!java.nio.file.Files.exists(path)) {
            logger.warn("File not found: {}", path);
            return null;
        }
        try {
            logger.info("Loading Firebase service account JSON directly from {}", path);
            return java.nio.file.Files.readString(path, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.warn("Failed to load {}", path, e);
        }
    
        return null;
    }

    /**
     * Returns true if the message was accepted by the Firebase service.
     * @param eventName the type of event triggering the notification, used to determine title and default message if messageBody is not provided
     * @param deviceToken the target device token to send the notification to
     * @param messageBody optional custom message body to override the default message for the event type
     */
    public boolean sendFirebaseNotification(EventName eventName, String deviceToken, String messageBody) {
        if (!initialized) {
            logger.error("sendFirebaseNotification: Firebase is not initialized");
            return false;
        }
        if (deviceToken == null || deviceToken.isBlank()) {
            logger.error("sendFirebaseNotification: Device token is {}; cannot send Firebase notification", deviceToken == null ? "null" : "blank");
            return false;
        }

        try {
            Message message = Message.builder()
                    .setToken(deviceToken)
                    .setNotification(Notification.builder()
                            .setTitle(eventName.getTitle())
                            .setBody(messageBody != null ? messageBody : eventName.getMessage())
                            .build())
                    .build();

            logger.info("sendFirebaseNotification: Sending Firebase notification to token {}: {}", deviceToken, messageBody);
            String response = FirebaseMessaging.getInstance().send(message);
            logger.info("sendFirebaseNotification: Firebase notification sent successfully: {}", response);
            return true;
        } catch (Exception e) {
            logger.error("sendFirebaseNotification: Failed to send Firebase notification", e);
            return false;
        }
    }
}

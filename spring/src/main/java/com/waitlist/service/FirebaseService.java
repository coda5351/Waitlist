package com.waitlist.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
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
        // Allow overriding via a local secret.properties file outside of source control.
        if (serviceAccountJson == null || serviceAccountJson.isBlank()) {
            serviceAccountJson = loadServiceAccountJsonFromSecretFile();
        }

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

    private String loadServiceAccountJsonFromSecretFile() {
        // First, look for a properties file that may contain the JSON.
        String value = loadPropertyFromFiles("secret.properties", ".." + java.io.File.separator + "secret.properties", "secret.properties");
        if (value != null && !value.isBlank()) {
            return value;
        }

        // Fall back to looking for the service account JSON file directly.
        value = loadRawFileContent("firebase-adminsdk.json", ".." + java.io.File.separator + "firebase-adminsdk.json", "firebase-adminsdk.json");
        if (value != null && !value.isBlank()) {
            return value;
        }

        return null;
    }

    private String loadPropertyFromFiles(String... paths) {
        java.util.Properties props = new java.util.Properties();
        for (String p : paths) {
            java.nio.file.Path path = java.nio.file.Paths.get(p);
            if (!java.nio.file.Files.exists(path)) {
                continue;
            }
            try (java.io.InputStream in = java.nio.file.Files.newInputStream(path)) {
                props.load(in);
                String value = props.getProperty("firebase.service-account-json");
                if (value != null && !value.isBlank()) {
                    logger.info("Loaded Firebase service account JSON from {}", path);
                    return value;
                }
            } catch (Exception e) {
                logger.warn("Failed to load {}", path, e);
            }
        }
        return null;
    }

    private String loadRawFileContent(String... paths) {
        for (String p : paths) {
            java.nio.file.Path path = java.nio.file.Paths.get(p);
            if (!java.nio.file.Files.exists(path)) {
                continue;
            }
            try {
                logger.info("Loading Firebase service account JSON directly from {}", path);
                return java.nio.file.Files.readString(path, StandardCharsets.UTF_8);
            } catch (Exception e) {
                logger.warn("Failed to load {}", path, e);
            }
        }
        return null;
    }

    /**
     * Sends a simple notification to a single device token.
     * <p>
     * Returns true if the message was accepted by the Firebase service.
     */
    public boolean sendTableReadyNotification(String deviceToken, String messageBody) {
        if (!initialized) {
            logger.error("sendTableReadyNotification: Firebase is not initialized");
            return false;
        }
        if (deviceToken == null || deviceToken.isBlank()) {
            logger.error("sendTableReadyNotification: Device token is {}; cannot send Firebase notification", deviceToken == null ? "null" : "blank");
            return false;
        }

        try {
            Message message = Message.builder()
                    .setToken(deviceToken)
                    .setNotification(Notification.builder()
                            .setTitle("Your table is ready")
                            .setBody(messageBody)
                            .build())
                    .build();

            logger.info("sendTableReadyNotification: Sending Firebase notification to token {}: {}", deviceToken, messageBody);
            String response = FirebaseMessaging.getInstance().send(message);
            logger.info("sendTableReadyNotification: Firebase notification sent successfully: {}", response);
            return true;
        } catch (Exception e) {
            logger.error("sendTableReadyNotification: Failed to send Firebase notification", e);
            return false;
        }
    }
}

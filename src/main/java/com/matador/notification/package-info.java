/**
 * Notification module: sends transactional email and SMS in response to domain events.
 * Records each notification and flushes pending ones via a background sender.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Notification")
package com.matador.notification;

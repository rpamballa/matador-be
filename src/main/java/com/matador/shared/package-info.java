/**
 * Cross-cutting, non-domain infrastructure shared by all business modules:
 * id generation, money, time, error handling, auditing, security, and config.
 *
 * <p>Declared as an {@code OPEN} module so its sub-packages are addressable from
 * every other module without being treated as hidden internals.
 */
@org.springframework.modulith.ApplicationModule(type = org.springframework.modulith.ApplicationModule.Type.OPEN)
package com.matador.shared;

package com.matador.shared.time;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * All time access goes through an injected {@link Clock}. Tests override this bean
 * with a fixed clock to make time-sensitive trip logic deterministic.
 */
@Configuration
public class TimeConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}

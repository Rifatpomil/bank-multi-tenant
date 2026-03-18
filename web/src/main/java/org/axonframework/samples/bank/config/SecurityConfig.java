/*
 * Copyright (c) 2016. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.samples.bank.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Protects /admin/** with HTTP Basic auth when axon.admin.rebuild-enabled=true.
 * Configure axon.admin.username and axon.admin.password in application properties.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${axon.admin.username:admin}")
    private String adminUsername;

    @Value("${axon.admin.password:admin}")
    private String adminPassword;

    @Value("${axon.admin.rebuild-enabled:false}")
    private boolean rebuildEnabled;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        if (rebuildEnabled) {
            http
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/admin/**").hasRole("ADMIN")
                            .anyRequest().permitAll()
                    )
                    .httpBasic(httpBasic -> {})
                    .csrf(csrf -> csrf.disable());
        } else {
            http
                    .authorizeHttpRequests(auth -> auth
                            .anyRequest().permitAll()
                    )
                    .csrf(csrf -> csrf.disable());
        }
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        if (rebuildEnabled) {
            var user = User.withUsername(adminUsername)
                    .password("{noop}" + adminPassword)
                    .roles("ADMIN")
                    .build();
            return new InMemoryUserDetailsManager(user);
        }
        return new InMemoryUserDetailsManager();
    }
}

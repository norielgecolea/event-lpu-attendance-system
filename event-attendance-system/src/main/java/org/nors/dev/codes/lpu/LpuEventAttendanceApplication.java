package org.nors.dev.codes.lpu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication(exclude = {
        HibernateJpaAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class
})
public class LpuEventAttendanceApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(LpuEventAttendanceApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(LpuEventAttendanceApplication.class, args);
    }
}

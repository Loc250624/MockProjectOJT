package com.ojtsu26.elearning.seeder;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
public class CourseDataFixer implements CommandLineRunner {

    private final CoreDataSeeder coreDataSeeder;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Running Course Data Fixer to apply premium thumbnails and ensure category coverage...");
        coreDataSeeder.seed();
        System.out.println("Course Data Fixer execution completed successfully.");
    }
}

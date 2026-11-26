package com.hcltech.sample.reactive.webflux.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * <strong>Caution</strong>: BoundedElasticScheduler is a shared resource, and for smaller applications, it is recommended to use the shared instance provided by Reactor.
 * However, for larger applications, for better debugging and monitoring, you may want to create a separate owned instance of BoundedElasticScheduler.
 * <p>
 * Choose configuration parameters based on your application's needs. The parameters are:
 * - threadCap: The maximum number of threads that can be created. Adjust based on expected load and resource availability.
 * - taskQueueSize: The maximum number of tasks that can be queued before new tasks are rejected.
 * - threadNamePrefix: A prefix for the names of the threads created by this scheduler. This can help with debugging and monitoring. Default is "boundedElastic-".
 */
@Configuration
class SchedulerConfig {

    @Bean(destroyMethod = "dispose")
    Scheduler userServiceScheduler() {
        return Schedulers.newBoundedElastic(10, 1_000, "user-service-worker");
    }

    @Bean(destroyMethod = "dispose")
    Scheduler tenantServiceScheduler() {
        return Schedulers.newBoundedElastic(10, 1_000, "tenant-service-worker");
    }
}

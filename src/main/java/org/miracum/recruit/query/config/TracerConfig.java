package org.miracum.recruit.query.config;

import io.jaegertracing.internal.propagation.TraceContextCodec;
import io.opentracing.contrib.java.spring.jaeger.starter.TracerBuilderCustomizer;
import io.opentracing.propagation.Format;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TracerConfig {

  @Bean
  @ConditionalOnProperty(value = "opentracing.jaeger.enabled", havingValue = "false")
  public io.opentracing.Tracer noopTracer() {
    return io.opentracing.noop.NoopTracerFactory.create();
  }

  @Bean
  public TracerBuilderCustomizer traceContextJaegerTracerCustomizer() {
    return builder -> {
      var injector = new TraceContextCodec.Builder().build();

      builder
          .registerInjector(Format.Builtin.HTTP_HEADERS, injector)
          .registerExtractor(Format.Builtin.HTTP_HEADERS, injector);

      builder
          .registerInjector(Format.Builtin.TEXT_MAP, injector)
          .registerExtractor(Format.Builtin.TEXT_MAP, injector);
    };
  }
}

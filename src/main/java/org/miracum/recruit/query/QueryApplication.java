package org.miracum.recruit.query;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.okhttp.client.OkHttpRestfulClientFactory;
import io.jaegertracing.internal.propagation.TraceContextCodec;
import io.opentracing.Span;
import io.opentracing.contrib.java.spring.jaeger.starter.TracerBuilderCustomizer;
import io.opentracing.contrib.okhttp3.OkHttpClientSpanDecorator;
import io.opentracing.contrib.okhttp3.TracingInterceptor;
import io.opentracing.propagation.Format;
import io.opentracing.util.GlobalTracer;
import java.util.Arrays;
import okhttp3.Connection;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.camel.opentracing.starter.CamelOpenTracing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
@Configuration
@CamelOpenTracing
public class QueryApplication {
    public static void main(String[] args) {
        SpringApplication.run(QueryApplication.class, args);
    }

    @Bean
    public FhirContext fhirContext() {
        var fhirContext = FhirContext.forR4();

        var opNameDecorator =
                new OkHttpClientSpanDecorator() {
                    @Override
                    public void onRequest(Request request, Span span) {
                        // add the operation name to the span
                        span.setOperationName(request.url().encodedPath());
                    }

                    @Override
                    public void onError(Throwable throwable, Span span) {}

                    @Override
                    public void onResponse(Connection connection, Response response, Span span) {}
                };

        var tracingInterceptor =
                new TracingInterceptor(
                        GlobalTracer.get(),
                        Arrays.asList(OkHttpClientSpanDecorator.STANDARD_TAGS, opNameDecorator));

        var okclient =
                new OkHttpClient.Builder()
                        .addInterceptor(tracingInterceptor)
                        .addNetworkInterceptor(tracingInterceptor)
                        .build();
        var okHttpFactory = new OkHttpRestfulClientFactory(fhirContext);
        okHttpFactory.setHttpClient(okclient);

        fhirContext.setRestfulClientFactory(okHttpFactory);
        return fhirContext;
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

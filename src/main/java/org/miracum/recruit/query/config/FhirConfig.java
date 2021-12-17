package org.miracum.recruit.query.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.okhttp.client.OkHttpRestfulClientFactory;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.okhttp3.OkHttpMetricsEventListener;
import io.opentracing.Span;
import io.opentracing.contrib.okhttp3.OkHttpClientSpanDecorator;
import io.opentracing.contrib.okhttp3.TracingInterceptor;
import io.opentracing.util.GlobalTracer;
import java.time.Duration;
import java.util.Arrays;
import okhttp3.Connection;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FhirConfig {

  @Bean
  public FhirContext fhirContext(
      @Value("${fhir.server.timeout-in-seconds}") long fhirServerTimeoutSeconds) {
    var fhirContext = FhirContext.forR4();

    var opNameDecorator =
        new OkHttpClientSpanDecorator() {
          @Override
          public void onError(Throwable throwable, Span span) {}

          @Override
          public void onRequest(Request request, Span span) {
            // add the operation name to the span
            span.setOperationName(request.url().encodedPath());
          }

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
            .callTimeout(Duration.ofSeconds(fhirServerTimeoutSeconds))
            .connectTimeout(Duration.ofSeconds(fhirServerTimeoutSeconds))
            .readTimeout(Duration.ofSeconds(fhirServerTimeoutSeconds))
            .writeTimeout(Duration.ofSeconds(fhirServerTimeoutSeconds))
            .addNetworkInterceptor(tracingInterceptor)
            .eventListener(
                OkHttpMetricsEventListener.builder(Metrics.globalRegistry, "fhir.client").build())
            .build();
    var okHttpFactory = new OkHttpRestfulClientFactory(fhirContext);
    okHttpFactory.setHttpClient(okclient);

    fhirContext.setRestfulClientFactory(okHttpFactory);
    return fhirContext;
  }

  @Bean
  public IGenericClient fhirClient(
      FhirContext fhirContext, @Value("${fhir.url}") String fhirServerUrl) {
    return fhirContext.newRestfulGenericClient(fhirServerUrl);
  }
}

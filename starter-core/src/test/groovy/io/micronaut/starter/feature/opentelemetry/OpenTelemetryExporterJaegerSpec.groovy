package io.micronaut.starter.feature.opentelemetry

import io.micronaut.starter.ApplicationContextSpec
import io.micronaut.starter.BuildBuilder
import io.micronaut.starter.application.ApplicationType
import io.micronaut.starter.application.generator.GeneratorContext
import io.micronaut.starter.build.BuildTestUtil
import io.micronaut.starter.build.BuildTestVerifier
import io.micronaut.starter.feature.Category
import io.micronaut.starter.fixture.CommandOutputFixture
import io.micronaut.starter.options.BuildTool
import io.micronaut.starter.options.Language
import spock.lang.Subject
import spock.lang.Unroll

class OpenTelemetryExporterJaegerSpec extends ApplicationContextSpec implements CommandOutputFixture {

    @Subject
    OpenTelemetryExporterJaeger feature = beanContext.getBean(OpenTelemetryExporterJaeger)

    void 'tracing-opentelemetry-exporter-jaeger feature is in the tracing category'() {
        expect:
        feature.category == Category.TRACING
    }

    void 'tracing-opentelemetry-exporter-jaeger feature is not visible'() {
        expect:
        !feature.isVisible()
    }

    void 'test otel.traces.exporter configuration'() {
        when:
        GeneratorContext commandContext = buildGeneratorContext(['tracing-opentelemetry-exporter-jaeger'])

        then:
        commandContext.configuration.get('otel.traces.exporter') == 'otlp'
        commandContext.configuration.get('otel.exporter.otlp.endpoint') == 'http://localhost:4317'

    }

    @Unroll
    void 'feature tracing-opentelemetry-exporter-jaeger does not support type: #applicationType'(ApplicationType applicationType) {
        expect:
        !feature.supports(applicationType)

        where:
        applicationType << [ApplicationType.CLI]
    }

    @Unroll
    void 'feature tracing-opentelemetry-exporter-jaeger supports #applicationType'(ApplicationType applicationType) {
        expect:
        feature.supports(applicationType)

        where:
        applicationType << (ApplicationType.values().toList() - ApplicationType.CLI)
    }

    void 'test gradle tracing-opentelemetry-exporter-jaeger feature for language=#language'(Language language, BuildTool buildTool) {
        when:
        String template = new BuildBuilder(beanContext, buildTool)
                .language(language)
                .features(['tracing-opentelemetry-exporter-jaeger'])
                .render()
        BuildTestVerifier verifier = BuildTestUtil.verifier(buildTool, language, template)

        then:
        verifier.hasDependency("io.opentelemetry", "opentelemetry-exporter-otlp")
        !verifier.hasDependency("io.opentelemetry", "opentelemetry-exporter-jaeger")

        where:
        [language, buildTool] << [Language.values().toList(), BuildTool.values()].combinations()
    }
}

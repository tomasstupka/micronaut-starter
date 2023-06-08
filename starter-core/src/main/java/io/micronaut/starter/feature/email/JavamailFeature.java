/*
 * Copyright 2017-2021 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.starter.feature.email;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.starter.application.generator.GeneratorContext;
import io.micronaut.starter.build.dependencies.Dependency;
import jakarta.inject.Singleton;

@Singleton
public class JavamailFeature extends EmailFeature {

    private static final Dependency ECLIPSE_ANGUS = Dependency.builder()
            .groupId("org.eclipse.angus")
            .artifactId("angus-mail")
            .runtime()
            .build();

    public JavamailFeature(TemplateEmailFeature templateEmailFeature) {
        super(templateEmailFeature);
    }

    @Override
    @NonNull
    public String getDescription() {
        return "Integration with Javamail to send transactional emails";
    }

    @Override
    public String getTitle() {
        return "Javamail Email";
    }

    @Override
    public String getModule() {
        return "javamail";
    }

    @Override
    public void apply(GeneratorContext generatorContext) {
        super.apply(generatorContext);
        generatorContext.addDependency(ECLIPSE_ANGUS);
    }

    @Override
    @Nullable
    public String getMicronautDocumentation() {
        return "https://micronaut-projects.github.io/micronaut-email/latest/guide/index.html#javamail";
    }

    @Override
    @Nullable
    public String getThirdPartyDocumentation() {
        return "https://jakartaee.github.io/mail-api/";
    }
}

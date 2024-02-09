/*
 * Copyright 2017-2023 original authors
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
package io.micronaut.starter.cli.command;

import io.micronaut.context.annotation.Prototype;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.util.StringUtils;
import io.micronaut.starter.application.ApplicationType;
import io.micronaut.starter.application.generator.ProjectGenerator;
import io.micronaut.starter.feature.Feature;
import io.micronaut.starter.feature.architecture.Arm;
import io.micronaut.starter.feature.architecture.CpuArchitecture;
import io.micronaut.starter.feature.architecture.X86;
import io.micronaut.starter.feature.aws.AmazonApiGateway;
import io.micronaut.starter.feature.aws.AwsApiFeature;
import io.micronaut.starter.feature.aws.AwsLambdaFeatureValidator;
import io.micronaut.starter.feature.aws.Cdk;
import io.micronaut.starter.feature.aws.LambdaFunctionUrl;
import io.micronaut.starter.feature.aws.LambdaTrigger;
import io.micronaut.starter.feature.function.awslambda.AwsLambda;
import io.micronaut.starter.feature.graalvm.GraalVM;
import io.micronaut.starter.feature.graalvm.GraalVMFeatureValidator;
import io.micronaut.starter.options.BuildTool;
import io.micronaut.starter.options.JdkVersion;
import io.micronaut.starter.options.Language;
import io.micronaut.starter.options.Options;
import io.micronaut.starter.options.TestFramework;
import org.jline.reader.LineReader;
import picocli.CommandLine;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static picocli.CommandLine.Help.Ansi.AUTO;

@CommandLine.Command(name = CreateLambdaBuilderCommand.NAME, description = "A guided walk-through to create an lambda function")
@Prototype
public class CreateLambdaBuilderCommand extends BuilderCommand {

    public static final String NAME = "create-aws-lambda";

    public CreateLambdaBuilderCommand(
            ProjectGenerator projectGenerator,
            List<Feature> features
    ) {
        super(projectGenerator, features);
    }

    @Override
    public GenerateOptions createGenerateOptions(LineReader reader) {
        Set<String> applicationFeatures = new HashSet<>();
        applicationFeatures.add(AwsLambda.FEATURE_NAME_AWS_LAMBDA);
        CodingStyle codingStyle = getCodingStyle(reader);
        ApplicationType applicationType = applicationTypeForCodingStyle(codingStyle);
        if (codingStyle == CodingStyle.CONTROLLERS) {
            Feature apiFeature = getApiTrigger(applicationType, reader);
            applicationFeatures.add(apiFeature.getName());
        } else {
            Feature trigger = getTrigger(reader);
            applicationFeatures.add(trigger.getName());
        }

        LambdaDeployment deployment = getLambdaDeployment(reader);
        if (deployment == LambdaDeployment.NATIVE_EXECUTABLE) {
            applicationFeatures.add(GraalVM.FEATURE_NAME_GRAALVM);
        }

        getArchitecture(reader).ifPresent(architecture -> {
            if (architecture instanceof Arm) {
                applicationFeatures.add(Arm.NAME);
            } else if (architecture instanceof X86) {
                applicationFeatures.add(X86.NAME);
            }
        });

        getCdk(reader).ifPresent(f -> applicationFeatures.add(f.getName()));

        Language language = getLanguage(deployment, reader);
        TestFramework testFramework = getTestFramework(reader, language);
        BuildTool buildTool = getBuildTool(reader, language);
        JdkVersion jdkVersion = getJdkVersion(deployment, reader);
        Options options = new Options(language, testFramework, buildTool, jdkVersion);
        return new GenerateOptions(applicationType, options, applicationFeatures);
    }

    protected JdkVersion getJdkVersion(LambdaDeployment deployment, LineReader reader) {
        JdkVersion[] versions = jdkVersionsForDeployment(deployment);
        JdkVersion defaultOption = versions.length > 0 ? versions[0] : JdkVersion.JDK_17;
        out("Choose the target JDK. (enter for default)");

        for (int i = 0; i < versions.length; i++) {
            out(AUTO.string("@|blue " + (versions[i].equals(defaultOption) ? '*' : ' ') + (i + 1) + ")|@ " + versions[i].majorVersion()));
        }
        int option = getOption(reader, versions.length);
        out("");
        if (option == -1) {
            return defaultOption;
        }
        int choice = option - 1;
        return versions[choice];
    }

    static JdkVersion[] jdkVersionsForDeployment(LambdaDeployment deployment) {
        switch (deployment) {
            case NATIVE_EXECUTABLE:
                return new JdkVersion[]{
                        JdkVersion.JDK_17
                };
            case FAT_JAR:
            default:
                List<JdkVersion> supportedJdks = AwsLambdaFeatureValidator.supportedJdks();
                JdkVersion[] arr = new JdkVersion[supportedJdks.size()];
                supportedJdks.toArray(arr);
                return arr;
        }
    }

    protected ApplicationType applicationTypeForCodingStyle(CodingStyle codingStyle) {
        switch (codingStyle) {
            case HANDLER:
                return ApplicationType.FUNCTION;
            case CONTROLLERS:
            default:
                return ApplicationType.DEFAULT;

        }
    }

    static Language[] languagesForDeployment(LambdaDeployment deployment) {
        return deployment == LambdaDeployment.NATIVE_EXECUTABLE ?
                Stream.of(Language.values())
                        .filter(GraalVMFeatureValidator::supports)
                        .toArray(Language[]::new) :
                Language.values();
    }

    protected Feature getApiTrigger(ApplicationType applicationType, LineReader reader) {
        Feature defaultFeature = features.stream().filter(AmazonApiGateway.class::isInstance).findFirst()
                .orElseThrow(() -> new ConfigurationException("default feature " + LambdaFunctionUrl.NAME + " not found"));
        out("Choose your trigger. (enter for " + defaultFeature.getTitle() + ")");
        return getFeatureOption(
                apiTriggerFeatures(applicationType, features),
                Feature::getTitle,
                defaultFeature,
                reader);
    }

    protected Feature getTrigger(LineReader reader) {
        Feature defaultFeature = features.stream().filter(LambdaFunctionUrl.class::isInstance).findFirst()
                .orElseThrow(() -> new ConfigurationException("default feature " + LambdaFunctionUrl.NAME + " not found"));
        out("Choose your trigger. (enter for " + defaultFeature.getTitle() + ")");
        return getFeatureOption(
                triggerFeatures(features),
                Feature::getTitle,
                defaultFeature,
                reader);
    }

    protected Optional<Feature> getArchitecture(LineReader reader) {
        List<Feature> cpuArchitecturesFeatures = features.stream()
                .filter(CpuArchitecture.class::isInstance)
                .toList();
        String defaultCpuArchitecture = X86.NAME;
        out("Choose your Lambda Architecture. (enter for " + defaultCpuArchitecture + ")");
        String option = getListOption(
                cpuArchitecturesFeatures.stream()
                        .map(Feature::getName)
                        .sorted()
                        .toList(),
                o -> o,
                defaultCpuArchitecture,
                reader);
        return cpuArchitecturesFeatures
                .stream()
                .filter(f -> f.getName().equals(option))
                .findFirst();
    }

    protected Language getLanguage(LambdaDeployment deployment, LineReader reader) {
        out("Choose your preferred language. (enter for default)");
        return getEnumOption(
                languagesForDeployment(deployment),
                language -> StringUtils.capitalize(language.getName()),
                Language.DEFAULT_OPTION,
                reader);
    }

    protected LambdaDeployment getLambdaDeployment(LineReader reader) {
        out("How do you want to deploy?. (enter for Java runtime)");
        return getEnumOption(
                LambdaDeployment.class,
                LambdaDeployment::getDescription,
                LambdaDeployment.FAT_JAR,
                reader);
    }

    protected CodingStyle getCodingStyle(LineReader reader) {
        out("How do you want to write your application? (enter for Controllers)");
        return getEnumOption(
                CodingStyle.class,
                CodingStyle::getDescription,
                CodingStyle.CONTROLLERS,
                reader);
    }

    protected Optional<Feature> getCdk(LineReader reader) {
        out("Do you want to generate infrastructure as code with CDK? (enter for yes)");
        return getYesOrNo(reader) == YesOrNo.YES
                ? features.stream().filter(Cdk.class::isInstance).findFirst()
                : Optional.empty();
    }

    static List<Feature> apiTriggerFeatures(ApplicationType applicationType, Collection<Feature> features) {
        return features.stream()
                .filter(AwsApiFeature.class::isInstance)
                .filter(f -> f.supports(applicationType))
                .sorted(Comparator.comparing(Feature::getTitle).reversed())
                .toList();
    }

    static List<Feature> triggerFeatures(Collection<Feature> features) {
        return features.stream()
                .filter(LambdaTrigger.class::isInstance)
                .sorted((o1, o2) -> {
                    if (o1 instanceof AwsApiFeature && (o2 instanceof AwsApiFeature)) {
                        return o2.getTitle().compareTo(o1.getTitle());
                    }
                    if (o1 instanceof AwsApiFeature) {
                        return -1;
                    }
                    if (o2 instanceof AwsApiFeature) {
                        return 1;
                    }
                    return o1.getTitle().compareTo(o2.getTitle());
                })
                .toList();
    }
}

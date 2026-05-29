package io.sdkman.kotlintoolchain.plugins.jib

import com.google.cloud.tools.jib.api.*
import com.google.cloud.tools.jib.frontend.CredentialRetrieverFactory
import org.jetbrains.amper.plugins.*
import java.nio.file.Path

@TaskAction
fun buildAndPush(
    @Input runtimeClasspath: Classpath,
    container: ContainerSettings,
    baseImage: BaseImageSettings,
    targetImage: TargetImageSettings,
) {
    val containerizer = Containerizer.to(targetImage.toRegistryImages())
    targetImage.effectiveTags().forEach { containerizer.withAdditionalTag(it) }
    jibContainerBuilder(runtimeClasspath, container, baseImage).containerize(containerizer)
}

@TaskAction
fun buildTar(
    @Input runtimeClasspath: Classpath,
    container: ContainerSettings,
    baseImage: BaseImageSettings,
    targetImageName: String,
    @Output outputTar: Path,
) {
    jibContainerBuilder(runtimeClasspath, container, baseImage)
        .containerize(Containerizer.to(TarImage.at(outputTar).named(targetImageName)))
}

@TaskAction
fun buildToDockerDaemon(
    @Input runtimeClasspath: Classpath,
    container: ContainerSettings,
    baseImage: BaseImageSettings,
    targetImageName: String,
) {
    jibContainerBuilder(runtimeClasspath, container, baseImage)
        .containerize(Containerizer.to(DockerDaemonImage.named(ImageReference.parse(targetImageName))))
}

private fun jibContainerBuilder(
    runtimeClasspath: Classpath,
    container: ContainerSettings,
    baseImage: BaseImageSettings,
): JibContainerBuilder = JavaContainerBuilder.from(baseImage.toRegistryImage())
    .addDependencies(runtimeClasspath.resolvedFiles)
    .addJvmFlags(container.jvmArgs)
    .setMainClass(container.mainClass)
    .toContainerBuilder()
    .apply {
        if (container.entryPoint != null) {
            setEntrypoint(container.entryPoint)
        }
        if (container.ports.isNotEmpty()) {
            setExposedPorts(Ports.parse(container.ports))
        }
        if (container.environment.isNotEmpty()) {
            setEnvironment(container.environment)
        }
        container.user?.let { setUser(it) }
    }

private fun BaseImageSettings.toRegistryImage(): RegistryImage {
    val imageReference = ImageReference.parse(fullName)
    val registryImage = RegistryImage.named(imageReference)
    registryImage.configureCredentials(imageReference, credHelper, auth)
    return registryImage
}

private fun TargetImageSettings.toRegistryImages(): RegistryImage {
    val imageReference = ImageReference.parse(name)
    val registryImage = RegistryImage.named(imageReference)
    registryImage.configureCredentials(imageReference, credHelper, auth)
    return registryImage
}

/** Env var that overrides the configured target image tags (comma-separated). */
private const val TARGET_IMAGE_TAGS_ENV = "JIB_TARGET_IMAGE_TAGS"

/**
 * Tags to publish, honoring a `JIB_TARGET_IMAGE_TAGS` override.
 *
 * The toolchain pinned by `./kotlin` exposes no `--setting` flag, so environment
 * variables are how release CI customizes the published tags, e.g.
 * `JIB_TARGET_IMAGE_TAGS=1.2.4,<sha>,latest`. When unset or blank, the configured
 * [TargetImageSettings.tags] are used.
 */
private fun TargetImageSettings.effectiveTags(): List<String> =
    System.getenv(TARGET_IMAGE_TAGS_ENV)
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?.takeIf { it.isNotEmpty() }
        ?: tags

private fun RegistryImage.configureCredentials(
    imageReference: ImageReference,
    credHelper: String?,
    auth: Credentials?,
) {
    val credentialRetrieverFactory = CredentialRetrieverFactory.forImage(imageReference) { logEvent ->
        println("${logEvent.level} ${logEvent.message}")
    }
    addCredentialRetriever(credentialRetrieverFactory.dockerConfig())
    addCredentialRetriever(credentialRetrieverFactory.wellKnownCredentialHelpers())
    if (credHelper != null) {
        addCredentialRetriever(credentialRetrieverFactory.dockerCredentialHelper(credHelper))
    }
    if (auth != null) {
        val basicAuth = credentialRetrieverFactory.known(Credential.from(auth.username, auth.password), "basic auth")
        addCredentialRetriever(basicAuth)
    }
}

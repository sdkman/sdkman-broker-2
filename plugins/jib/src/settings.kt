import org.jetbrains.amper.plugins.Configurable

@Configurable
interface JibSettings {
    val container: ContainerSettings
    val baseImage: BaseImageSettings
    val targetImage: TargetImageSettings
}

@Configurable
interface ContainerSettings {
    /**
     * The main class of the application to run.
     */
    val mainClass: String

    /**
     * JVM arguments passed to the `java` command in the default entrypoint of the container to start the application.
     */
    val jvmArgs: List<String>
        get() = emptyList()

    /**
     * When specified, overrides the default entrypoint of the base image.
     * The default entrypoint is the `java` command with the runtime classpath, [jvmArgs] and [mainClass].
     */
    val entryPoint: List<String>?

    /**
     * Ports the container exposes at runtime (sets `EXPOSE` entries on the image). Each entry is either a bare port
     * number (e.g. `"8080"`, defaults to TCP) or `"<port>/<protocol>"` (e.g. `"8080/tcp"`, `"53/udp"`).
     */
    val ports: List<String>
        get() = emptyList()

    /**
     * Environment variables baked into the image as defaults; can be overridden at container runtime.
     */
    val environment: Map<String, String>
        get() = emptyMap()

    /**
     * The user (and optionally group) the container runs as. May be a name or numeric id, with optional group
     * (e.g. `"1000"` or `"nobody:nogroup"`).
     */
    val user: String?
}

@Configurable
interface BaseImageSettings {
    /**
     * The full name of the image, including the registry and tag.
     */
    val fullName: String

    /**
     * The username/password authentication used to pull the image from a private registry.
     *
     * Cannot be provided together with [credHelper].
     */
    val auth: Credentials?

    /**
     * The name of the Docker Credential Helper to use to authenticate when pulling the image from a private registry.
     *
     * Cannot be provided together with [auth].
     */
    val credHelper: String?
}

@Configurable
interface TargetImageSettings {
    /**
     * The name of the image, including the registry. The tag can be omitted, and several tags can be provided instead
     * using the [tags] list.
     */
    val name: String

    /**
     * The username/password authentication used to push the image to a registry.
     *
     * Cannot be provided together with [credHelper].
     */
    val auth: Credentials?

    /**
     * The name of the Docker Credential Helper to use to authenticate when pushing the image to a Docker registry.
     *
     * Cannot be provided together with [auth].
     */
    val credHelper: String?

    /**
     * The tags to apply to the image.
     */
    val tags: List<String>
        get() = emptyList()
}

@Configurable
interface Credentials {
    val username: String
    val password: String
}

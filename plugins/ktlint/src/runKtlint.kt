package io.sdkman.amper.plugins.ktlint

import org.jetbrains.amper.plugins.Input
import org.jetbrains.amper.plugins.Output
import org.jetbrains.amper.plugins.TaskAction
import java.io.File
import java.nio.file.Path
import kotlin.concurrent.thread
import kotlin.io.path.createParentDirectories
import kotlin.io.path.isDirectory
import kotlin.io.path.pathString
import kotlin.io.path.writeText

@TaskAction
fun runKtlintCheck(
    @Input commonParameters: CommonKtlintSettings,
    @Output outputReport: Path,
) = runKtlint(
    commonParameters = commonParameters,
    outputReport = outputReport,
    format = false,
)

@TaskAction
fun runKtlintFormat(
    @Input commonParameters: CommonKtlintSettings,
    @Output outputReport: Path,
) = runKtlint(
    commonParameters = commonParameters,
    outputReport = outputReport,
    format = true,
)

private fun runKtlint(
    commonParameters: CommonKtlintSettings,
    outputReport: Path,
    format: Boolean,
) {
    val sourceDirs = commonParameters.sources.sourceDirectories.filter { it.isDirectory() }
    if (sourceDirs.isEmpty()) {
        outputReport.createParentDirectories()
        outputReport.writeText("")
        return
    }

    outputReport.createParentDirectories()

    val args = mutableListOf<String>().apply {
        add(KTLINT_MAIN_CLASS)
        if (format) {
            add("--format")
        }
        commonParameters.settings.editorConfigPath?.let {
            add("--editorconfig=${it.pathString}")
        }
        commonParameters.rulesetClasspath?.resolvedFiles?.forEach {
            add("--ruleset=${it.pathString}")
        }
        add("--reporter=plain")
        add("--reporter=checkstyle,output=${outputReport.pathString}")
        sourceDirs.forEach { dir ->
            add("${dir.pathString}${File.separator}**${File.separator}*.kt")
        }
    }

    val ktlintCp = commonParameters.ktlintClasspath.resolvedFiles.joinToString(File.pathSeparator)
    val commandLine = buildList {
        add(ProcessHandle.current().info().command().orElse("java"))
        // ktlint reaches into JDK internals via reflection on modern JREs.
        add("--add-opens=java.base/java.lang=ALL-UNNAMED")
        add("-cp")
        add(ktlintCp)
        addAll(args)
    }

    val process = ProcessBuilder(commandLine)
        .redirectErrorStream(true)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .start()

    val capture = thread { process.inputStream.copyTo(System.out) }
    val exitCode = process.waitFor()
    capture.join()

    check(exitCode == 0) {
        "ktlint terminated with code = $exitCode. See the log above for details."
    }
}

private const val KTLINT_MAIN_CLASS = "com.pinterest.ktlint.Main"

import com.resentclient.oss.eaglercraft.build.impl.wasm
import com.resentclient.oss.eaglercraft.build.tasks.wasm.internal.CompileWasmBootstrapTask
import org.teavm.gradle.api.OptimizationLevel
import org.teavm.gradle.api.WasmDebugInfoLocation
import org.teavm.gradle.api.WasmDebugInfoLevel

plugins {
	id("java")
	id("org.teavm") version "0.12.1-EAGLER-R3"

	id("com.resentclient.oss.eaglercraft.build") version "0.0.0"
}

java {
	sourceCompatibility = JavaVersion.VERSION_17
	targetCompatibility = JavaVersion.VERSION_17
}

sourceSets {
	named("main") {
		java.srcDirs(
			"../src/main/java",
			"../src/wasm-gc-teavm/java",
		)
		resources.srcDirs(
			"../src/teavm/resources"
		)
	}
}

repositories {
	maven {
		name = "eagler-teavm"
		url = uri("https://eaglercraft-teavm-fork.github.io/maven/")
	}
}

dependencies {
	teavm(teavm.libs.jso)
	teavm(teavm.libs.jsoApis)
	compileOnly("org.teavm:teavm-core:0.12.1-EAGLER-R3") // workaround for a few hacks
	implementation(libs.jorbis)
	implementation(libs.bundles.common)
}

val wasmFolder = "javascript"
val wasmOutputFileName = "classes.wasm"

teavm.wasmGC {
	targetFileName = "../" + wasmOutputFileName
	optimization = OptimizationLevel.AGGRESSIVE
	outOfProcess = false
	fastGlobalAnalysis = false
	processMemory = 512
	mainClass = "net.lax1dude.eaglercraft.internal.wasm_gc_teavm.MainClass"
	outputDir = file(wasmFolder)
	properties = mapOf("java.util.TimeZone.autodetect" to "true")
	debugInformation = true
	debugInfoLocation = WasmDebugInfoLocation.EXTERNAL;
	debugInfoLevel = WasmDebugInfoLevel.DEOBFUSCATION;
	minDirectBuffersSize = 32
	maxDirectBuffersSize = 512
	disassembly = true
}

tasks.withType<JavaCompile> {
	options.encoding = "UTF-8"
}

eaglercraftBuild {
	suites {
		wasm("main") {
			val srcFolder = "../src/wasm-gc-teavm/js"

			closureCompiler = file("buildtools/closure-compiler.jar")
			closureMainClass = "com.google.javascript.jscomp.CommandLineRunner"
			closureInputFiles = files(
				"$srcFolder/externs.js",
				"$srcFolder/eagruntime_util.js",
				"$srcFolder/eagruntime_main.js",
				"$srcFolder/platformApplication.js",
				"$srcFolder/platformAssets.js",
				"$srcFolder/platformAudio.js",
				"$srcFolder/platformFilesystem.js",
				"$srcFolder/platformInput.js",
				"$srcFolder/platformNetworking.js",
				"$srcFolder/platformOpenGL.js",
				"$srcFolder/platformRuntime.js",
				"$srcFolder/platformScreenRecord.js",
				"$srcFolder/platformVoiceClient.js",
				"$srcFolder/platformWebRTC.js",
				"$srcFolder/platformWebView.js",
				"$srcFolder/clientPlatformSingleplayer.js",
				"$srcFolder/serverPlatformSingleplayer.js",
				"$srcFolder/WASMGCBufferAllocator.js",
				"$srcFolder/fix-webm-duration.js",
				"$srcFolder/teavm_runtime.js",
				"$srcFolder/eagruntime_entrypoint.js"
			)
			runtimeOutput = file("javascript/eagruntime.js")

			epwSource = file("$wasmFolder/epw_src.txt")
			epwMeta = file("$wasmFolder/epw_meta.txt")
			epwSearchDirectory = file(wasmFolder)
			clientBundleOutputDir = file("javascript_dist")
		}.apply {
			epkSources = file("../desktopRuntime/resources")
			epkOutput = file("javascript/assets.epk")

			languageMetadataInput = file("../target_teavm_javascript/javascript/lang")
			languageEpkOutput = file("javascript/lang.tmp.epk")

			sourceGeneratorTaskName = "generateWasmGC"
		}
	}
}

// The build plugin reads javascript_dist/bootstrap.js when it generates the offline
// download, but it never registers the task that produces that file, so on a clean
// checkout makeMainWasmClientBundle fails with "bootstrap.js (No such file or
// directory)". Register the bootstrap compile here and hook it up, so the documented
// MakeWASMClientBundle script works from scratch without a manual CompileBootstrapJS
// step first.
val compileMainWasmBootstrap = tasks.register<CompileWasmBootstrapTask>("compileMainWasmBootstrap") {
	group = "eaglercraft"
	description = "Compiles the WASM-GC bootstrap loader with Closure Compiler"

	val bootstrapSrcFolder = file("../src/wasm-gc-teavm-bootstrap/js")
	closureInputFiles.from(
		"$bootstrapSrcFolder/externs.js",
		"$bootstrapSrcFolder/main.js"
	)
	output.set(file("javascript_dist/bootstrap.js"))

	// The plugin's task only populates the Closure arguments; it never sets a main
	// class or classpath, so point it at the bundled Closure Compiler.
	mainClass.set("com.google.javascript.jscomp.CommandLineRunner")
	classpath = files(file("buildtools/closure-compiler.jar"))
}

// The plugin registers the bundle task lazily, so match by type and wire the
// dependency in a way that applies whenever the task is finally realized.
tasks.withType(com.resentclient.oss.eaglercraft.build.tasks.wasm.MakeWasmClientBundleTask::class.java).configureEach {
	dependsOn(compileMainWasmBootstrap)
}
plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	kotlin("plugin.serialization") version "1.9.25"
	id("org.springframework.boot") version "3.5.0"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.jamesonzeller"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-cache")
	implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
	implementation("org.jsoup:jsoup:1.20.1")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.0")
	implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
	implementation("org.jetbrains.kotlin:kotlin-reflect")

	implementation("com.google.auth:google-auth-library-oauth2-http:1.19.0")
	implementation("com.google.api-client:google-api-client:2.4.1")
	implementation("com.google.apis:google-api-services-calendar:v3-rev20220715-2.0.0")

	implementation("io.ktor:ktor-client-core:2.3.5")
	implementation("io.ktor:ktor-client-cio:2.3.5")
	implementation("io.ktor:ktor-client-content-negotiation:2.3.5")
	implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.5")

	testImplementation("org.springframework.boot:spring-boot-starter-test") {
		exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
	}
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("io.mockk:mockk:1.13.10")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

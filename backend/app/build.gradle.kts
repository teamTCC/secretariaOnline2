plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":modules:iam"))
    implementation(project(":modules:academico"))
    implementation(project(":modules:solicitacoes"))
    implementation(project(":modules:formativas"))
    implementation(project(":modules:estagio"))
    implementation(project(":modules:tcc"))
    implementation(project(":modules:presenca"))
    implementation(project(":modules:comunicacao"))
    implementation(project(":modules:notificacoes"))
    implementation(project(":modules:auditoria"))
    implementation(project(":modules:arquivos"))
    implementation(project(":modules:bff"))

    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("org.flywaydb:flyway-core")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
}

springBoot {
    mainClass.set("br.ufpr.sept.so2.SecretariaOnlineApplicationKt")
}

tasks.bootJar {
    archiveFileName.set("secretaria-online-2.jar")
}

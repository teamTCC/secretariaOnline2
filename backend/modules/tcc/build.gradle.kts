plugins {
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
}

dependencies {
    api(project(":shared"))
    implementation(project(":modules:iam"))
    implementation(project(":modules:academico"))
    implementation(project(":modules:arquivos"))
    implementation(project(":modules:notificacoes"))
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.mockk:mockk:1.13.13")
}

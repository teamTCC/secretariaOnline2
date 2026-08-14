plugins {
    kotlin("plugin.spring")
}

dependencies {
    api(project(":shared"))
    implementation(project(":modules:iam"))
    implementation(project(":modules:academico"))
    implementation(project(":modules:solicitacoes"))
    implementation(project(":modules:formativas"))
    implementation(project(":modules:estagio"))
    implementation(project(":modules:tcc"))
    implementation(project(":modules:presenca"))
    implementation(project(":modules:comunicacao"))
    implementation(project(":modules:notificacoes"))
    implementation(project(":modules:arquivos"))
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")
    implementation("org.springframework.boot:spring-boot-starter-cache")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.mockk:mockk:1.13.13")
}

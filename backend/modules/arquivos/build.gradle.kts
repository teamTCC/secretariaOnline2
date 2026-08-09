plugins {
    kotlin("plugin.spring")
}

dependencies {
    api(project(":shared"))
    implementation("io.minio:minio:8.5.12")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.mockk:mockk:1.13.13")
}

plugins {
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
}

dependencies {
    api(project(":shared"))
    implementation(project(":modules:comunicacao"))
    implementation("org.springframework.boot:spring-boot-starter-mail")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.mockk:mockk:1.13.13")
}

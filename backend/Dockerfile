# ── Stage 1: build ───────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copy Gradle wrapper first so the wrapper download layer is cached separately
COPY gradlew gradlew.bat gradle.properties settings.gradle.kts build.gradle.kts ./
COPY gradle/ gradle/

# Copy all subproject build files (layer-cache friendly)
COPY shared/build.gradle.kts        shared/build.gradle.kts
COPY app/build.gradle.kts           app/build.gradle.kts
COPY modules/iam/build.gradle.kts          modules/iam/build.gradle.kts
COPY modules/academico/build.gradle.kts    modules/academico/build.gradle.kts
COPY modules/solicitacoes/build.gradle.kts modules/solicitacoes/build.gradle.kts
COPY modules/formativas/build.gradle.kts   modules/formativas/build.gradle.kts
COPY modules/estagio/build.gradle.kts      modules/estagio/build.gradle.kts
COPY modules/tcc/build.gradle.kts          modules/tcc/build.gradle.kts
COPY modules/presenca/build.gradle.kts     modules/presenca/build.gradle.kts
COPY modules/comunicacao/build.gradle.kts  modules/comunicacao/build.gradle.kts
COPY modules/notificacoes/build.gradle.kts modules/notificacoes/build.gradle.kts
COPY modules/auditoria/build.gradle.kts    modules/auditoria/build.gradle.kts
COPY modules/arquivos/build.gradle.kts     modules/arquivos/build.gradle.kts
COPY modules/bff/build.gradle.kts          modules/bff/build.gradle.kts

# Download dependencies (cached unless build files change)
RUN ./gradlew dependencies --no-daemon -q 2>&1 | tail -5

# Copy full source
COPY . .

# Build fat jar (skip tests — run them in CI before building the image)
RUN ./gradlew :app:bootJar --no-daemon -q

# ── Stage 2: runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=builder /build/app/build/libs/secretaria-online-2.jar app.jar

USER appuser

EXPOSE 8080

# JVM flags: smaller heap footprint, GC logging, container-aware sizing
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]

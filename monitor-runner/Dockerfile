# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage com Playwright
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/monitor-runner-1.0.0.jar app.jar

# Instalar dependências do Playwright (Firefox)
RUN apt-get update && apt-get install -y \
    wget \
    gnupg \
    ca-certificates \
    fonts-liberation \
    libasound2 \
    libatk-bridge2.0-0 \
    libatk1.0-0 \
    libatspi2.0-0 \
    libcups2 \
    libdbus-1-3 \
    libdrm2 \
    libgbm1 \
    libgtk-3-0 \
    libnspr4 \
    libnss3 \
    libwayland-client0 \
    libxcomposite1 \
    libxdamage1 \
    libxfixes3 \
    libxkbcommon0 \
    libxrandr2 \
    xvfb \
    && rm -rf /var/lib/apt/lists/*

# Instalar Playwright browsers
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
RUN java -cp app.jar org.springframework.boot.loader.JarLauncher || true
RUN apt-get update && apt-get install -y maven && \
    mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install firefox" || \
    wget -q https://playwright.azureedge.net/builds/firefox/1421/firefox-ubuntu-20.04.zip && \
    unzip -q firefox-ubuntu-20.04.zip -d /ms-playwright && \
    rm firefox-ubuntu-20.04.zip

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]

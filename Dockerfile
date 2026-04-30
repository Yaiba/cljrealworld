# Stage 1: build everything
FROM clojure:temurin-21-tools-deps AS builder
WORKDIR /app

# Install Node for shadow-cljs
RUN apt-get update && apt-get install -y nodejs npm && rm -rf /var/lib/apt/lists/*

# Cache npm deps
COPY package*.json ./
RUN npm install

# Cache JVM deps
COPY deps.edn build.clj shadow-cljs.edn ./
RUN clj -P -A:backend:build

# Build ClojureScript
COPY src/ src/
COPY resources/ resources/
RUN npx shadow-cljs release app

# Build uberjar
RUN clj -T:build uber

# Stage 2: runtime — slim JRE only
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/app.jar app.jar
EXPOSE 3000
CMD ["java", "-jar", "app.jar"]

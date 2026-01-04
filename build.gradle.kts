plugins {
    java
    `maven-publish`
}

group = "io.github.spaceleam"
version = "1.0.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

dependencies {
    // Zero production dependencies - pure Java!
    
    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
    testImplementation("org.assertj:assertj-core:3.26.3")
}

tasks.test {
    useJUnitPlatform()
    
    // Memory optimization buat laptop 8GB
    maxHeapSize = "1g"
    minHeapSize = "512m"
    
    // Parallel execution (optimal buat dual-core)
    maxParallelForks = 2
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:unchecked")
}

// Publishing configuration (buat nanti publish ke Maven Central)
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            
            pom {
                name.set("Cadence")
                description.set("Lightweight in-memory rate limiter using Token Bucket algorithm")
                url.set("https://github.com/SpaceLeam/cadence")
                
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                
                developers {
                    developer {
                        id.set("spaceleam")
                        name.set("SpaceLeam")
                    }
                }
                
                scm {
                    connection.set("scm:git:git://github.com/SpaceLeam/cadence.git")
                    developerConnection.set("scm:git:ssh://github.com:SpaceLeam/cadence.git")
                    url.set("https://github.com/SpaceLeam/cadence")
                }
            }
        }
    }
}

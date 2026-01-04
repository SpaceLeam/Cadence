plugins {
    java
    jacoco
    `maven-publish`
}

group = "io.github.spaceleam"
version = "1.0.0"

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
    
    // Generate coverage report after tests
    finalizedBy(tasks.jacocoTestReport)
}

jacoco {
    toolVersion = "0.8.12"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:unchecked")
}

tasks.javadoc {
    options.encoding = "UTF-8"
    (options as StandardJavadocDocletOptions).apply {
        links("https://docs.oracle.com/en/java/javase/21/docs/api/")
        addBooleanOption("html5", true)
        addStringOption("Xdoclint:none", "-quiet")
    }
    isFailOnError = false
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

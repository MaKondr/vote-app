plugins {
    id("java")
}

group = "edu.makondr.project"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    // https://mvnrepository.com/artifact/io.netty/netty-all
    implementation("io.netty:netty-all:4.1.119.Final")
    // https://mvnrepository.com/artifact/info.picocli/picocli
    implementation("info.picocli:picocli:4.7.6")

}

tasks.test {
    useJUnitPlatform()
}
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <!-- ================================================================
         PARENT - Spring Boot manages ALL dependency versions for us.
         We don't need to specify versions for Spring deps manually.
         This is called a "Bill of Materials (BOM)" pattern.
    ================================================================ -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.11</version>
        <relativePath/>
    </parent>

    <!-- ================================================================
         PROJECT IDENTITY
    ================================================================ -->
    <groupId>com.capestart</groupId>
    <artifactId>student-library</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>student-library</name>
    <description>Student Library API - Intern Project at Capestart</description>

    <!-- ================================================================
         VERSION VARIABLES
         Industry standard: never hardcode versions inside dependencies.
         Define once here, reference with ${variable.name} everywhere.
         When you want to upgrade, change it in ONE place only.
    ================================================================ -->
    <properties>
        <java.version>21</java.version>
        <mapstruct.version>1.6.3</mapstruct.version>
        <lombok.version>1.18.34</lombok.version>
        <lombok-mapstruct-binding.version>0.2.0</lombok-mapstruct-binding.version>
        <maven-compiler-plugin.version>3.13.0</maven-compiler-plugin.version>
    </properties>

    <!-- ================================================================
         DEPENDENCIES
    ================================================================ -->
    <dependencies>

        <!-- WEB: Builds REST API endpoints (GET, POST, PUT, DELETE) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- JPA: Lets us talk to DB using Java classes instead of raw SQL -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- VALIDATION: @NotNull, @Email, @Size, @NotBlank on DTOs -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- H2: In-memory database, resets on every restart (perfect for learning) -->
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- LIQUIBASE: Manages DB table creation + pre-inserts data on startup -->
        <dependency>
            <groupId>org.liquibase</groupId>
            <artifactId>liquibase-core</artifactId>
        </dependency>

        <!-- LOMBOK: Generates getters/setters/constructors at compile time
             @Data, @Builder, @NoArgsConstructor, @AllArgsConstructor etc.
             optional=true means it's NOT included in final jar (compile-only) -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>${lombok.version}</version>
            <optional>true</optional>
        </dependency>

        <!-- MAPSTRUCT: Auto-generates Entity <-> DTO conversion code
             You write the interface, MapStruct generates the implementation -->
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
            <version>${mapstruct.version}</version>
        </dependency>

        <!-- TEST: JUnit5 + Mockito for unit and integration testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

    </dependencies>

    <!-- ================================================================
         BUILD PLUGINS
    ================================================================ -->
    <build>
        <plugins>

            <!-- SPRING BOOT PLUGIN: Packages app as executable JAR
                 'mvn spring-boot:run' uses this to start your app -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <!-- Exclude Lombok from final JAR — it's compile-only -->
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>

            <!-- COMPILER PLUGIN: Tells Java HOW to compile our code.
                 annotationProcessorPaths = tools that run BEFORE compilation
                 to generate code (Lombok generates getters, MapStruct generates mappers)

                 ⚠️ ORDER IS CRITICAL:
                 1. Lombok runs FIRST  → generates getters/setters on Entity/DTO
                 2. MapStruct runs SECOND → reads those getters/setters to build mapper
                 3. Binding runs THIRD  → ensures they cooperate cleanly

                 If MapStruct runs before Lombok, it can't find the methods → BUILD FAILS -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>${maven-compiler-plugin.version}</version>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                    <annotationProcessorPaths>

                        <!-- STEP 1: Lombok generates boilerplate code -->
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>${lombok.version}</version>
                        </path>

                        <!-- STEP 2: MapStruct generates mapper implementations -->
                        <path>
                            <groupId>org.mapstruct</groupId>
                            <artifactId>mapstruct-processor</artifactId>
                            <version>${mapstruct.version}</version>
                        </path>

                        <!-- STEP 3: Binding ensures Lombok + MapStruct work together -->
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok-mapstruct-binding</artifactId>
                            <version>${lombok-mapstruct-binding.version}</version>
                        </path>

                    </annotationProcessorPaths>
                </configuration>
            </plugin>

        </plugins>
    </build>

</project>
```

---

## ⚡ After Pasting

Press `Ctrl + Shift + O` to reload Maven. Wait for **"Sync finished"** at the bottom.

Then verify in the **Maven panel** (right side of IntelliJ) you see:
```
student-library
  └── Dependencies
        ├── mapstruct 1.6.3 ✅
        ├── lombok 1.18.34 ✅
        └── ... all others ✅
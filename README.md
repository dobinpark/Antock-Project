# Antock-Project

## 1. 프로젝트 개요 (Project Overview)

본 프로젝트는 공정거래위원회에서 제공하는 통신판매사업자 관련 CSV 데이터를 다운로드하고, 외부 API(주소 API, 통신판매 사업자 정보 API)를 활용하여 데이터를 가공 및 정제한 후 데이터베이스에 저장하는 기능을 제공하는 Spring Boot 기반 백엔드 애플리케이션입니다.

### 주요 기능 (Key Features)

-   **CSV 데이터 다운로드 (`SeleniumCsvDownloader`)**: 공정거래위원회 웹사이트에서 특정 지역(시/도, 구/군)의 통신판매사업자 CSV 파일을 Selenium을 사용하여 다운로드합니다.
-   **CSV 데이터 파싱 (`OpenCsvParser`)**: 다운로드한 CSV 파일을 파싱하여 Java `Map` 형태의 데이터 리스트로 변환합니다.
-   **외부 API 연동**:
    -   **통신판매사업자 상세 정보 조회 (`FtcAntockerApiClient`)**: 사업자등록번호를 이용하여 법인등록번호 등의 상세 정보를 외부 API로부터 조회합니다. (실제 API 연동 구현 필요)
    -   **주소 정보 조회 (`JusoAddressApiClient`)**: 사업장 주소 문자열을 이용하여 행정구역 코드 등 주소 관련 정보를 외부 주소 API로부터 조회합니다. (WebClient 사용)
-   **데이터 처리 및 가공 (`AntockerDataProcessor`, `AntockerService`)**: CSV 데이터와 외부 API 조회 결과를 조합하여 `Antocker` 엔티티 형태로 가공합니다. 중복 데이터(사업자등록번호 기준)를 제거하는 로직을 포함합니다.
-   **데이터베이스 저장 (`AntockerRepository`, `AntockerService`)**: 가공된 `Antocker` 엔티티 정보를 H2 데이터베이스에 저장합니다.
-   **REST API 제공 (`AntockerController`)**: 외부로부터 특정 지역의 데이터 처리 작업을 트리거하는 REST API 엔드포인트를 제공합니다. (예: `POST /api/antockers/process?condition=서울,강남구`)
-   **비동기 처리 (`AsyncConfig`, `@Async`)**: 데이터 처리 및 저장 등 시간이 소요될 수 있는 작업을 별도의 스레드 풀에서 비동기적으로 처리하여 시스템 응답성을 향상시킵니다.

## 2. 아키텍처 (Architecture)

본 프로젝트는 계층형 아키텍처를 따르며, 주요 구성 요소는 다음과 같습니다.

```mermaid
graph LR
    Client -- HTTP Request --> AntockerController[Antocker Controller<br>(REST API)]
    AntockerController -- Calls --> AntockerService[Antocker Service<br>(비동기 처리, 중복 제거)]
    AntockerService -- Uses --> SeleniumCsvDownloader[Selenium CSV Downloader]
    AntockerService -- Uses --> OpenCsvParser[OpenCSV Parser]
    AntockerService -- Uses --> AntockerDataProcessor[Antocker Data Processor<br>(API 호출, 데이터 매핑)]
    AntockerService -- Uses --> AntockerRepository[Antocker Repository<br>(JPA)]

    SeleniumCsvDownloader -- Downloads --> ExternalSite[공정거래위원회 사이트]
    OpenCsvParser -- Parses --> CSVFile[다운로드된 CSV]
    AntockerDataProcessor -- Calls --> FtcAntockerApiClient[FTC API Client]
    AntockerDataProcessor -- Calls --> JusoAddressApiClient[Juso API Client]
    AntockerRepository -- Interacts --> H2Database[(H2 Database)]

    FtcAntockerApiClient -- HTTP --> FtcApi[외부 통신판매사업자 API]
    JusoAddressApiClient -- HTTP --> JusoApi[외부 주소 API]

    subgraph "Core Logic"
        direction TB
        AntockerController
        AntockerService
        AntockerDataProcessor
        AntockerRepository
    end

    subgraph "External Interaction"
        direction TB
        SeleniumCsvDownloader
        OpenCsvParser
        FtcAntockerApiClient
        JusoAddressApiClient
    end

    subgraph "Data & External Systems"
        direction TB
        H2Database
        ExternalSite
        CSVFile
        FtcApi
        JusoApi
    end

    style H2Database fill:#ccf,stroke:#333,stroke-width:2px
```

_(위 다이어그램은 Mermaid 문법으로 작성되었으며, GitHub 등에서 렌더링됩니다.)_

### 주요 컴포넌트 설명

-   **`AntockerController`**: 외부 HTTP 요청을 받아 데이터 처리 작업을 `AntockerService`에 위임합니다.
-   **`AntockerService`**: 핵심 비즈니스 로직을 담당합니다. CSV 다운로드, 파싱, 데이터 처리(`AntockerDataProcessor` 호출), 중복 데이터 필터링, 데이터베이스 저장(`AntockerRepository` 사용) 등 전체 프로세스를 조율하고 `@Async`를 통해 비동기적으로 실행합니다.
-   **`AntockerDataProcessor`**: 개별 CSV 행 데이터를 입력받아 외부 API 클라이언트(`FtcAntockerApiClient`, `JusoAddressApiClient`)를 호출하고, 필요한 정보를 취합하여 `Antocker` 엔티티 객체를 생성합니다. `@Async`로 실행될 수 있습니다.
-   **`SeleniumCsvDownloader`**: Selenium WebDriver를 사용하여 공정거래위원회 사이트에서 CSV 파일을 다운로드합니다. (`prod` 프로파일에서 활성화)
-   **`OpenCsvParser`**: OpenCSV 라이브러리를 사용하여 CSV 파일을 파싱합니다.
-   **`FtcAntockerApiClient` / `JusoAddressApiClient`**: 외부 API와의 통신을 담당하는 인터페이스 및 구현체입니다. (`JusoAddressApiClient`는 WebClient 사용)
-   **`AntockerRepository`**: Spring Data JPA를 사용하여 H2 데이터베이스의 `Antocker` 테이블과 상호작용합니다.
-   **`Antocker`**: 데이터베이스 테이블과 매핑되는 JPA 엔티티 클래스입니다.
-   **`AsyncConfig`**: 비동기 작업 실행을 위한 스레드 풀을 설정합니다.
-   **`GlobalExceptionHandler`**: 애플리케이션 전역의 예외를 처리하여 일관된 오류 응답을 제공합니다.

## 3. 기술 스택 (Tech Stack)

-   **Language**: Java 17
-   **Framework**: Spring Boot 3.4.2
    -   Spring Web
    -   Spring Data JPA
    -   Spring WebFlux (for WebClient)
    -   Spring Boot Validation
    -   Spring Boot Test
    -   Spring Boot Logging (Logback)
-   **Database**: H2 Database (In-Memory or File-based)
-   **ORM**: Hibernate (via Spring Data JPA)
-   **CSV Parsing**: OpenCSV 5.9
-   **HTTP Client**: Spring WebClient (Asynchronous)
-   **Web Scraping/Automation**: Selenium Java 4.x
-   **WebDriver Management**: WebDriverManager 5.3.2
-   **Build Tool**: Gradle
-   **Utilities**: Lombok
-   **Testing**: JUnit 5, Mockito, Spring Test

## 4. 실행 방법 (Getting Started)

### 사전 요구사항

-   Java 17 JDK 설치
-   Gradle 설치 (또는 프로젝트 내 Gradle Wrapper 사용)
-   (선택) H2 데이터베이스 관련 도구 (별도 서버 모드 사용 시)
-   (`prod` 프로파일 실행 시) Chrome 브라우저 및 ChromeDriver 호환 버전 설치 (WebDriverManager가 자동 설정을 시도)

### 설정 단계

1.  **저장소 클론**:
    ```bash
    git clone <repository-url>
    cd Antock-Project
    ```

2.  **환경 설정 (`src/main/resources/application.properties`)**:
    -   기본적으로 H2 인메모리 데이터베이스를 사용하도록 설정되어 있을 수 있습니다. (`spring.datasource.url=jdbc:h2:mem:testdb`)
    -   필요에 따라 H2 파일 모드 또는 다른 데이터베이스 설정을 수정합니다.
    -   외부 API Key 등 필요한 설정 값을 확인하고 추가합니다. (예: `api.address.key`)

3.  **빌드**: 프로젝트 루트 디렉토리에서 다음 명령어를 실행합니다.
    ```bash
    ./gradlew build
    ```
    (또는 Windows: `gradlew.bat build`)
    -   `-x test` 옵션을 추가하여 테스트를 제외하고 빌드할 수 있습니다: `./gradlew build -x test`

### 실행

1.  **JAR 파일 실행**: `build/libs/` 디렉토리에 생성된 JAR 파일을 실행합니다.
    ```bash
    java -jar build/libs/Antock-Project-0.0.1-SNAPSHOT.jar
    ```
    -   특정 프로파일 활성화 (예: `prod` 프로파일, `application-prod.properties` 필요):
        ```bash
        java -jar -Dspring.profiles.active=prod build/libs/Antock-Project-0.0.1-SNAPSHOT.jar
        ```
        (단, `prod` 프로파일 실행 시 `selenium.enabled=true` 설정 및 WebDriver 환경 필요)

2.  **Gradle 사용 실행**:
    ```bash
    ./gradlew bootRun
    ```
    (또는 Windows: `gradlew.bat bootRun`)
    -   프로파일 지정: `./gradlew bootRun --args='--spring.profiles.active=prod'`

3.  **접속 URL**: 기본 설정은 `http://localhost:8080` 입니다. (`application.properties`의 `server.port` 확인)

## 5. API 테스트 방법 (API Testing)

(주의: 아래 API 경로는 예상이며, 실제 `AntockerController` 구현에 따라 다를 수 있습니다.)

`curl` 또는 Postman과 같은 도구를 사용하여 API를 테스트할 수 있습니다.

### 5.1. 특정 지역 데이터 처리 작업 시작 (`POST /api/antockers/process`)

-   **`curl` 예시**: (쿼리 파라미터로 지역 조건 전달)
    ```bash
    curl --location --request POST 'http://localhost:8080/api/antockers/process?condition=서울,강남구'
    ```
-   **Postman 설정**:
    -   Method: `POST`
    -   URL: `http://localhost:8080/api/antockers/process`
    -   Params:
        -   Key: `condition`
        -   Value: `서울,강남구` (쉼표로 구분된 시/도, 구/군)
-   **성공 응답 (202 Accepted)**: 비동기 작업 시작을 알리는 응답. 실제 결과는 비동기 처리 후 DB에 저장됩니다.
    ```json
    // 예상 응답 (실제 구현에 따라 다름)
    {
        "statusCode": 202,
        "message": "Data processing started for condition: 서울,강남구",
        "data": null 
    }
    ```
-   **실패 응답 (예: 400 Bad Request)**: 잘못된 `condition` 형식 등
    ```json
    // 예상 응답
    {
        "statusCode": 400,
        "message": "Invalid condition format. Please use '시,구' format.",
        "error": "Bad Request",
        "path": "/api/antockers/process"
        // ... (GlobalExceptionHandler 설정에 따른 상세 내용)
    }
    ```

## 6. 데이터베이스 (Database)

-   **DBMS**: H2 (In-Memory 또는 File)
-   **ORM**: Hibernate (via Spring Data JPA)
-   **엔티티**: `src/main/java/antock/Antock_Project/domain/antocker/entity/Antocker.java`
-   **설정**: `src/main/resources/application.properties` 내 `spring.datasource.*`, `spring.jpa.*` 설정
-   **스키마 관리**: `spring.jpa.hibernate.ddl-auto` 설정에 따라 자동 관리 (예: `update`, `create`, `create-drop`, `validate`, `none`). 기본적으로 `update` 또는 `create-drop` (테스트 시) 사용 가능성 높음. 별도의 마이그레이션 도구(Flyway, Liquibase)는 현재 사용되지 않는 것으로 보입니다.
-   **데이터 시딩**: 별도의 Seeding 메커니즘은 구현되어 있지 않은 것으로 보입니다. 필요시 초기 데이터 삽입 로직 추가 필요.

### 테이블 구조 (`Antocker` 엔티티 기준)

-   **`ANTOCKER` 테이블**: 통신판매사업자 정보를 저장합니다.
    -   `id` (PK, Auto Increment)
    -   `business_registration_number` (사업자등록번호, Unique)
    -   `corporate_registration_number` (법인등록번호)
    -   `company_name` (상호명)
    -   `address` (사업장소재지)
    -   `administrative_code` (행정구역코드)
    -   `created_at`, `updated_at` (날짜/시간 관련 필드는 엔티티 정의에 따라 추가 가능)

### 샘플 쿼리 (H2 Console 또는 DB Client 사용)

-   **모든 Antocker 조회**: `SELECT * FROM ANTOCKER;`
-   **특정 사업자번호로 조회**: `SELECT * FROM ANTOCKER WHERE business_registration_number = '1234567890';`
-   **특정 상호명 포함 데이터 조회**: `SELECT * FROM ANTOCKER WHERE company_name LIKE '%테스트%';`

## 7. 테스트 (Testing)

-   **Framework**: JUnit 5
-   **Mocking**: Mockito
-   **Spring Integration**: Spring Boot Test, `@SpringBootTest`, `@MockBean`, `@DataJpaTest` 등
-   **테스트 종류**:
    -   단위 테스트 (`*Test.java`): 개별 컴포넌트(서비스, 프로세서 등)의 로직을 Mock 객체를 사용하여 검증합니다.
    -   통합 테스트 (`*Test.java` with `@SpringBootTest`): 애플리케이션 컨텍스트를 로드하여 여러 컴포넌트의 상호작용을 검증합니다. (예: `AntockProjectApplicationTests`)
-   **실행 명령어**:
    -   모든 테스트 실행: `./gradlew test` (또는 `gradlew.bat test`)
    -   테스트 커버리지 리포트 생성 (JaCoCo 플러그인 필요): `./gradlew test jacocoTestReport` (build.gradle 설정 확인 필요)
    -   특정 테스트 클래스 실행: `./gradlew test --tests *AntockerServiceTest`

## 8. CI/CD

-   현재 CI/CD 파이프라인은 설정되어 있지 않습니다.

## 9. 주요 의사결정 및 구현 세부 내용 (Decisions & Details)

-   **비동기 처리**: `@Async`와 `CompletableFuture`를 사용하여 시간이 오래 걸리는 I/O 작업(CSV 다운로드, 외부 API 호출)과 데이터 처리 로직을 비동기적으로 수행하여 메인 스레드 블로킹을 최소화하고 응답성을 개선했습니다.
-   **Selenium 사용**: 공정거래위원회 사이트가 동적으로 컨텐츠를 로드하거나 JavaScript 기반의 상호작용이 필요하여, 단순 HTTP 요청으로는 CSV 다운로드가 어려워 Selenium을 통한 브라우저 자동화 방식을 채택했습니다. 이는 환경 설정의 복잡성을 증가시키는 단점이 있습니다. (`prod` 프로파일에서만 활성화)
-   **WebClient 사용**: 주소 API 호출 등 비동기 HTTP 통신이 필요한 부분에는 Spring WebFlux의 `WebClient`를 사용하여 Non-blocking I/O를 구현했습니다.
-   **오류 처리**: `GlobalExceptionHandler`를 통해 애플리케이션 전역에서 발생하는 예외를 일관된 형식으로 처리하고 로깅합니다. `BusinessException` 커스텀 예외를 정의하여 비즈니스 로직 관련 오류를 구분합니다.

## 10. 잠재적인 문제점 및 트러블 슈팅 (Troubleshooting & Known Issues)

(기존 섹션 내용과 최근 이슈 통합)

### 10.1. 외부 API 연동 오류

-   **네트워크 문제**: 외부 API 서버와의 통신 실패 (방화벽, 타임아웃 등).
    -   *Troubleshooting*: API 요청/응답 로깅 강화, `WebClient` 타임아웃 설정 확인, 네트워크 환경 점검.
-   **API 응답 형식 변경**: 외부 API 응답 구조가 변경되어 DTO 파싱 오류 발생.
    -   *Troubleshooting*: API 문서 확인, DTO 클래스 업데이트, 유연한 파싱 로직 고려.
-   **API 키 또는 인증 문제**: 유효하지 않은 API 키 사용.
    -   *Troubleshooting*: `application.properties` 등 설정 파일의 API 키 확인.
-   **API 서버 자체 오류**: 외부 API 서버 불안정 또는 오류.
    -   *Troubleshooting*: API 서버 상태 확인, 재시도 로직(Retry) 구현 고려 (`WebClient` 확장 기능).

### 10.2. CSV 파일 처리 오류

-   **CSV 다운로드 실패**: 공정거래위원회 웹사이트 구조 변경, 로그인 필요, 접근 차단 등으로 다운로드 실패. (Selenium 관련 오류 포함)
    -   *Troubleshooting*: Selenium 스크립트 (CSS 셀렉터, 동작 로직) 업데이트, 사이트 변경 사항 확인, User-Agent 등 헤더 설정 변경 시도.
-   **CSV 파싱 오류**: 다운로드된 CSV 파일 형식 오류 (구분자, 인용 부호 등), 데이터 손상, 인코딩 불일치.
    -   *Troubleshooting*: OpenCSV 파싱 설정 확인, CSV 파일 직접 검사, 인코딩 형식(UTF-8, EUC-KR 등) 명시적 지정 시도.
-   **CSV 데이터 없음**: 특정 조건으로 다운로드 시 결과 데이터가 없는 경우.
    -   *Troubleshooting*: 정상적인 빈 파일 처리 로직 확인.

### 10.3. 비동기 처리 관련 오류

-   **스레드 풀 고갈**: 과도한 비동기 요청으로 스레드 풀 부족.
    -   *Troubleshooting*: `AsyncConfig`의 스레드 풀 크기, 큐 용량 등 설정 검토 및 튜닝.
-   **비동기 작업 예외 전파**: `@Async` 메소드에서 발생한 예외가 호출 측으로 제대로 전달되지 않거나 처리되지 않는 문제.
    -   *Troubleshooting*: `CompletableFuture` 반환 타입 사용 및 예외 처리 로직(예: `handle`, `exceptionally`) 구현 확인.
-   **트랜잭션 관리**: `@Async`와 `@Transactional` 함께 사용 시 트랜잭션 전파 문제.
    -   *Troubleshooting*: 트랜잭션 경계 확인, 별도 서비스로 분리 등 고려.

### 10.4. Selenium/WebDriver 관련 오류

-   **WebDriver 설정 오류**: 로컬/서버 환경에 ChromeDriver 부재 또는 경로 오류, WebDriverManager 자동 설정 실패.
    -   *Troubleshooting*: WebDriverManager 로그 확인, 필요시 ChromeDriver 수동 설치 및 경로 지정.
-   **브라우저 호환성**: 설치된 Chrome 브라우저 버전과 ChromeDriver 버전 불일치.
    -   *Troubleshooting*: 버전 확인 및 호환되는 버전 사용, WebDriverManager 버전 업데이트 고려.
-   **`SessionNotCreatedException` (테스트 환경)**: `@SpringBootTest` 실행 시 테스트 컨텍스트 로딩 중 WebDriver 초기화 시도.
    -   *Troubleshooting*: 아래 "10.5. 빌드 및 테스트 오류" 항목 참고.

### 10.5. 빌드 및 테스트 오류 (최근 진행 내용)

-   **컴파일 오류 (Compilation Errors)**:
    -   *원인*: DTO 구조 변경, 인터페이스 시그니처 불일치, 잘못된 메소드 호출 등.
    -   *해결*: 관련 클래스 코드 전반적 수정 (import, 메소드 호출, 반환 타입 처리 등).
-   **Mockito `UnnecessaryStubbingException` (테스트 코드)**:
    -   *원인*: Mockito Strict Stubbing 정책 위반 (사용되지 않는 Mock 설정).
    -   *해결*: 각 테스트 메소드별 필요한 Mock만 설정하도록 리팩토링.
-   **`SessionNotCreatedException` (테스트 컨텍스트 로딩 실패)**:
    -   *원인*: `@SpringBootTest` 로딩 시 `@Profile("prod")`의 `SeleniumCsvDownloader` 빈 초기화 시도.
    -   *해결 시도*: `@MockBean`, `@ActiveProfiles`, `@ConditionalOnProperty`, `@SpringBootTest(classes=...)` 등 적용했으나 실패.
    -   *현재 상태*: 근본 원인 분석 및 해결 방안 모색 중. 테스트 환경에서 WebDriver 초기화를 확실히 방지할 설정/구조 변경 필요.

## 11. 제출자 정보

-   **이름**: 박도빈

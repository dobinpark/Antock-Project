<h2>1. 프로젝트 개요</h2>
프로젝트 명칭 : Antock-Project
<br><br>
프로젝트 목적 : 공정거래위원회 관련 CSV 데이터를 다운로드하고,<br>
외부 API(주소 API, 통신판매 사업자 정보 API)를 활용하여 데이터를 가공 및 저장하는 기능을 제공.
<br><br>
주요 기능 :<br>
<h4>1. CSV 데이터 다운로드(FtcCsvDownloader)</h4>
공정거래위원회 웹사이트에서 특정 지역(시/도, 구/군)의<br>
통신판매사업자 CSV 파일을 다운로드 합니다.

<h4>2. CSV 데이터 파싱(OpenCsvParser)</h4>
다운로드 받은 CSV 파일을 파싱하여 데이터 맵 형태로 변환

<h4>3. 외부 API 연동(FtcAntockerApiClient, JusoAddressApiClient)</h4>
1) 통신판매사업자 상세 정보 API : 사업자등록번호를 이용하여 법인등록번호 등의 상세 정보를 조회<br>
2) 주소 API : 사업장 주소를 이용하여 행정구역 코드를 조회

<h4>4. 데이터 처리 및 가공(AntockerDataProcessor, AntockerService)</h4>
다운로드 및 파싱된 데이터를 기반으로 외부 API를 호출하여 필요한 정보를 추가하고,<br>
데이터베이스에 저장하기 위한 형태로 가공

<h4>5. 데이터베이스 저장(AntockerStorageService, AntockerRepository, Antocker)</h4>
가공된 통신판매사업자 정보를 H2 데이터베이스에 저장

<h4>6. REST API 제공(AntockerController)</h4>
외부 요청을 받아 통신판매사업자 정보 처리 작업을 시작하는 REST API를 제공

<h4>7. 비동기 처리(AsyncConfig, @Async)</h4>
CSV 데이터 다운로드, 파싱, 외부 API 호출, 데이터 저장 등의 작업을<br>
비동기적으로 처리하여 효율성을 높임

<h2>2. 기술 스택</h2>
<h4>프로그래밍 언어 : Java 17</h4>

<h4>Spring 프레임워크 : Spring Boot 3.4.2</h4>
<h5>1) Spring Web</h5>
<h5>2) Spring Data JPA</h5>
<h5>3) Spring WebFlux</h5>
<h5>4) Spring Boot Starter Validation</h5>
<h5>5) Spring Boot Starter Test</h5>
<h5>6) Spring Boot Starter Logging</h5>

<h4>데이터베이스 : H2 Database</h4>

<h4>CSV 파싱 라이브러리 : OpenCSV 5.9</h4>

<h4>HTTP 클라이언트 : WebClient (Spring WebFlux 에서 제공, 비동기 HTTP 요청에 사용)</h4>

<h4>로깅 : Slf4j (인터페이스), Logback (구현체)</h4>

<h4>유틸리티 라이브러리 : Lombok</h4>

<h4>테스팅 라이브러리 : JUnit Mockito, Spring Test</h4>

<h4>웹 드라이버 관리 : WebDriverManager 5.3.2 (Selenium WebDriver 설정 및 관리를 자동화)</h4>

<h4>Selenium (주석 처리): Selenium Java 2.41.0 및 4.8.3 (웹 브라우저 자동화 라이브러리, 현재 build.gradle 에는<br>
  주석 처리되어 있지만,관련 의존성이 남아있는 것으로 보아 웹 스크래핑 또는 테스트 자동화에 사용될 가능성이 있음)<br>
빌드 도구: Gradle

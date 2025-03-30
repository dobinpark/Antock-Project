<h2>1. 프로젝트 개요</h2>
프로젝트 명칭 : Antock-Project
<br><br>
프로젝트 목적 : 공정거래위원회 관련 CSV 데이터를 다운로드하고,<br>
외부 API(주소 API, 통신판매 사업자 정보 API)를 활용하여 데이터를 가공 및 저장하는 기능을 제공.
<br><br>
주요 기능 :<br>
<h4>1. CSV 데이터 다운로드(FtcCsvDownloader)</h4>
공정거래위원회 웹사이트에서 특정 지역(시/도, 구/군)의<br>
통신판매사업자 CSV 파일을 다운로드 합니다.<br>
<h4>2. CSV 데이터 파싱(OpenCsvParser)</h4>
다운로드 받은 CSV 파일을 파싱하여 데이터 맵 형태로 변환<br>
<h4>외부 API 연동(FtcAntockerApiClient, JusoAddressApiClient) </h4>
1) 통신판매사업자 상세 정보 API : 사업자등록번호를 이용하여 법인등록번호 등의 상세 정보를 조회<br>
2) 주소 API : 사업장 주소를 이용하여 행정구역 코드를 조회

package antock.Antock_Project.domain.antocker.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter // 필요에 따라 추가 (JPA에서는 보통 필요)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "antocker", indexes = {
        @Index(name = "idx_business_reg_num", columnList = "businessRegistrationNumber", unique = true),
        @Index(name = "idx_company_name", columnList = "companyName")
})
public class Antocker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String businessRegistrationNumber; // 사업자등록번호 (NN, UQ)

    @Column(nullable = true, length = 13) // 법인등록번호는 13자리
    private String corporateRegistrationNumber; // 법인등록번호 (Nullable)

    @Column(nullable = true)
    private String companyName; // 상호명 (Nullable, CSV에 따라 다를 수 있음)

    @Column(nullable = true, length = 255) // 주소 길이 고려
    private String address; // 사업장소재지 (Nullable)

    @Column(nullable = true, length = 10) // 행정구역코드 길이 고려 (예: 10자리)
    private String administrativeCode; // 행정구역코드 (Nullable)

    // --- CSV 및 API에서 가져올 수 있는 다른 필요한 필드들 추가 --- 
    // 예시:
    // @Column(length = 50)
    // private String representativeName; // 대표자명
    //
    // @Column(length = 20)
    // private String businessType; // 업태
    //
    // @Column(length = 50)
    // private String businessItem; // 종목
    //
    // @Column(length = 20)
    // private String reportStatus; // 신고상태 (예: 사업중, 휴업, 폐업)
    // 
    // @Column
    // private LocalDate reportDate; // 신고일자

    // --- 시스템 관리용 필드 (선택 사항) ---
    // @Column(nullable = false, updatable = false)
    // private LocalDateTime createdAt; // 생성일시
    // 
    // @Column(nullable = false)
    // private LocalDateTime updatedAt; // 수정일시
    // 
    // @PrePersist
    // protected void onCreate() {
    //     createdAt = updatedAt = LocalDateTime.now();
    // }
    // 
    // @PreUpdate
    // protected void onUpdate() {
    //     updatedAt = LocalDateTime.now();
    // }

    // toString 등 필요한 메서드 오버라이드 (Lombok @ToString 사용 가능)
    @Override
    public String toString() {
        return "Antocker{" +
                "id=" + id +
                ", businessRegistrationNumber='" + businessRegistrationNumber + '\'' +
                ", corporateRegistrationNumber='" + corporateRegistrationNumber + '\'' +
                ", companyName='" + companyName + '\'' +
                ", address='" + address + '\'' +
                ", administrativeCode='" + administrativeCode + '\'' +
                // ... 다른 필드 추가
                '}';
    }
}

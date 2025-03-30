package antock.Antock_Project.domain.antocker.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Antocker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String companyName; // 상호

    @Column(nullable = false)
    private String bizRegNum; // 사업자 등록번호

    private String crpRegNum; // 법인 등록번호

    @Column(nullable = false)
    private String address; // 사업장 주소

    private String admCd; // 행정 구역코드
}

package antock.Antock_Project.domain.antocker.repository;

import antock.Antock_Project.domain.antocker.entity.Antocker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AntockerRepository extends JpaRepository<Antocker, Long> {

    /**
     * 사업자등록번호로 Antocker 정보를 조회합니다.
     *
     * @param businessRegistrationNumber 조회할 사업자등록번호
     * @return 조회된 Antocker 정보 (Optional)
     */
    Optional<Antocker> findByBusinessRegistrationNumber(String businessRegistrationNumber);

    // 필요에 따라 다른 조회 메소드 추가 가능 (예: 상호명으로 검색, 특정 조건으로 목록 조회 등)
    // List<Antocker> findByCompanyNameContaining(String companyName);
}

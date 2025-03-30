package antock.Antock_Project.domain.antocker.repository;

import antock.Antock_Project.domain.antocker.entity.Antocker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AntockerRepositoryTest {

    @Autowired
    private AntockerRepository antockerRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void save_SellerEntity_Success() {
        // Given
        Antocker antocker = Antocker.builder()
                .companyName("테스트 상호")
                .bizRegNum("1234567890")
                .address("테스트 주소")
                .build();

        // When
        Antocker savedAntocker = antockerRepository.save(antocker);
        entityManager.flush();

        // Then
        assertThat(savedAntocker.getId()).isNotNull();
        assertThat(savedAntocker.getCompanyName()).isEqualTo("테스트 상호");
        assertThat(savedAntocker.getBizRegNum()).isEqualTo("1234567890");
        assertThat(savedAntocker.getAddress()).isEqualTo("테스트 주소");

        Antocker foundAntocker = antockerRepository.findById(savedAntocker.getId()).orElse(null);
        assertThat(foundAntocker).isNotNull();
        assertThat(foundAntocker.getCompanyName()).isEqualTo("테스트 상호");
    }

    @Test
    void findAll_Sellers_Success() {
        // Given
        Antocker antocker1 = Antocker.builder()
                .companyName("테스트 상호1")
                .bizRegNum("1111111111")
                .address("테스트 주소1")
                .build();
        Antocker antocker2 = Antocker.builder()
                .companyName("테스트 상호2")
                .bizRegNum("2222222222")
                .address("테스트 주소2")
                .build();
        antockerRepository.saveAll(List.of(antocker1, antocker2));
        entityManager.flush();

        // When
        List<Antocker> antockers = antockerRepository.findAll();

        // Then
        assertThat(antockers).hasSize(2);
        assertThat(antockers).extracting("companyName").containsExactly("테스트 상호1", "테스트 상호2");
    }
}

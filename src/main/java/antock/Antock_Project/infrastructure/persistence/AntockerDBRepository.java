package antock.Antock_Project.infrastructure.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import antock.Antock_Project.domain.antocker.entity.Antocker;
import antock.Antock_Project.domain.antocker.repository.AntockerRepository;
import antock.Antock_Project.infrastructure.storage.AntockerStorageService;

@Repository
public class AntockerDBRepository implements AntockerStorageService {

    private final AntockerRepository antockerRepository;

    public AntockerDBRepository(AntockerRepository antockerRepository) {
        this.antockerRepository = antockerRepository;
    }

    @Override
    public void saveAll(List<Antocker> antockers) {
        antockerRepository.saveAll(antockers);
    }
}

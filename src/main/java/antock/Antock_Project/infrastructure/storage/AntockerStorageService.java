package antock.Antock_Project.infrastructure.storage;

import java.util.List;
import antock.Antock_Project.domain.antocker.entity.Antocker;

public interface AntockerStorageService {
    void saveAll(List<Antocker> antockers);
}

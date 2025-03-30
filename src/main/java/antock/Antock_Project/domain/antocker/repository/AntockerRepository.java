package antock.Antock_Project.domain.antocker.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import antock.Antock_Project.domain.antocker.entity.Antocker;

@Repository
public interface AntockerRepository extends JpaRepository<Antocker, Long> {
    
}

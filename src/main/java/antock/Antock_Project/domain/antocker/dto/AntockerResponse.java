package antock.Antock_Project.domain.antocker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AntockerResponse {

    private String message;
    private boolean success;
}

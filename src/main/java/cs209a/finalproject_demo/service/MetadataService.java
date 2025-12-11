package cs209a.finalproject_demo.service;

import cs209a.finalproject_demo.dataset.LocalDatasetRepository;
import cs209a.finalproject_demo.dto.MetadataResponse;
import org.springframework.stereotype.Service;

import java.time.ZoneId;

@Service
public class MetadataService {

    private final LocalDatasetRepository repository;
    private final ZoneId zoneId = ZoneId.systemDefault();

    public MetadataService(LocalDatasetRepository repository) {
        this.repository = repository;
    }

    public MetadataResponse snapshot() {
        return new MetadataResponse(
                repository.findAllThreads().size(),
                repository.totalAnswerCount(),
                repository.totalCommentCount(),
                repository.minCreationInstant().map(instant -> instant.atZone(zoneId).toLocalDate()).orElse(null),
                repository.maxCreationInstant().map(instant -> instant.atZone(zoneId).toLocalDate()).orElse(null)
        );
    }
}










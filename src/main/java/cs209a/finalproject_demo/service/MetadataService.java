package cs209a.finalproject_demo.service;

import cs209a.finalproject_demo.dataset.LocalDatasetRepository;
import cs209a.finalproject_demo.dto.MetadataResponse;
import cs209a.finalproject_demo.model.Question;
import cs209a.finalproject_demo.model.QuestionThread;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

@Service
public class MetadataService {

    private final LocalDatasetRepository repository;
    private final ZoneId zoneId = ZoneId.systemDefault();

    public MetadataService(LocalDatasetRepository repository) {
        this.repository = repository;
    }

    /**
     * 优化版本：只调用一次findAllThreads()，在内存中计算所有统计数据
     * 避免重复数据库查询，提升性能
     */
    public MetadataResponse snapshot() {
        // 只加载一次数据
        List<QuestionThread> threads = repository.findAllThreads();
        
        if (threads.isEmpty()) {
            return new MetadataResponse(0, 0, 0, null, null);
        }
        
        // 在内存中计算所有统计数据
        int threadCount = threads.size();
        
        int answerCount = threads.stream()
                .mapToInt(t -> t.answers().size())
                .sum();
        
        int commentCount = (int) threads.stream()
                .mapToLong(t -> t.questionComments().size() +
                        t.answerComments().values().stream().mapToInt(List::size).sum())
                .sum();
        
        List<Question> questions = threads.stream()
                .map(QuestionThread::question)
                .toList();
        
        Instant minInstant = questions.stream()
                .map(Question::creationInstant)
                .min(Instant::compareTo)
                .orElse(null);
        
        Instant maxInstant = questions.stream()
                .map(Question::creationInstant)
                .max(Instant::compareTo)
                .orElse(null);
        
        return new MetadataResponse(
                threadCount,
                answerCount,
                commentCount,
                minInstant != null ? minInstant.atZone(zoneId).toLocalDate() : null,
                maxInstant != null ? maxInstant.atZone(zoneId).toLocalDate() : null
        );
    }
}

















package cs209a.finalproject_demo.model;

import java.util.List;
import java.util.Map;

public record QuestionThread(
        Question question,
        List<Answer> answers,
        List<Comment> questionComments,
        Map<Long, List<Comment>> answerComments
) {
}




















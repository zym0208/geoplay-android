package be.kuleuven.gt.javabean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuizQuestion {
    private String question;
    private String correctAnswer;
    private ArrayList<String> incorrectAnswers;

    public QuizQuestion(String question, String correctAnswer, ArrayList<String> incorrectAnswers) {
        this.question = question;
        this.correctAnswer = correctAnswer;
        this.incorrectAnswers = incorrectAnswers;
    }

    // Getters and setters
    public String getQuestion() { return question; }
    public String getCorrectAnswer() { return correctAnswer; }
    public List<String> getIncorrectAnswers() { return incorrectAnswers; }

    public ArrayList<String> getRandomizedAnswers() {
        ArrayList<String> answerList = new ArrayList<>();
        answerList.add(correctAnswer);
        answerList.addAll(incorrectAnswers);

        Collections.shuffle(answerList);
        return answerList;


    }
    public boolean isCorrect(String answer) {
        return correctAnswer.equals(answer);
    }
}


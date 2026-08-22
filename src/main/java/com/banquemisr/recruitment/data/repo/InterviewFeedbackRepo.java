package com.banquemisr.recruitment.data.repo;

import com.banquemisr.recruitment.data.entity.InterviewFeedbackEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewFeedbackRepo extends JpaRepository<InterviewFeedbackEntity,String> {
    List<InterviewFeedbackEntity> findByApplicationApplicationId(String applicationId);
}

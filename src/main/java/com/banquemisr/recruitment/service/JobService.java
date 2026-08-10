package com.banquemisr.recruitment.service;


import com.banquemisr.recruitment.data.repo.JobRepo;
import org.springframework.stereotype.Service;

@Service
public class JobService {

    private final JobRepo jobRepo;

    public JobService(JobRepo jobRepo) {
        this.jobRepo = jobRepo;
    }


}

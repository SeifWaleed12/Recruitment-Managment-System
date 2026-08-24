package com.banquemisr.recruitment.data.enums;

import java.util.EnumSet;
import java.util.Set;

public enum ApplicationStatus {
    APPLIED {
        @Override
        public Set<ApplicationStatus> nextStates(){
            return EnumSet.of(SCREENING, REJECTED, WITHDRAWN);
        }
    },
    SCREENING{
        @Override
        public Set<ApplicationStatus> nextStates(){
            return EnumSet.of(INTERVIEW,REJECTED,WITHDRAWN);
        }
    },
    INTERVIEW{
        @Override
        public Set<ApplicationStatus> nextStates(){
            return EnumSet.of(OFFER, REJECTED, WITHDRAWN);
        }
    },
    OFFER{
        @Override
        public Set<ApplicationStatus> nextStates(){
            return EnumSet.of(HIRED, REJECTED, WITHDRAWN);
        }
    },
    HIRED{
        @Override
        public Set<ApplicationStatus> nextStates(){
            return EnumSet.noneOf(ApplicationStatus.class); // no more states after that
        }
    },
    REJECTED{
        @Override
        public Set<ApplicationStatus> nextStates(){
            return EnumSet.noneOf(ApplicationStatus.class);
        }
    },
    WITHDRAWN{
        @Override
        public Set<ApplicationStatus> nextStates(){
            return EnumSet.noneOf(ApplicationStatus.class);
        }
    };

    public abstract Set<ApplicationStatus> nextStates();

    public boolean canTransitionTo(ApplicationStatus target){
        return nextStates().contains(target);
    }
}

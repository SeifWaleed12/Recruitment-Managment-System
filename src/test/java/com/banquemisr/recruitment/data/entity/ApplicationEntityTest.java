package com.banquemisr.recruitment.data.entity;

import com.banquemisr.recruitment.data.enums.ApplicationStatus;
import com.banquemisr.recruitment.exception.IllegalStateTransitionException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApplicationEntityTest {

    @Test
    void transitionTo_legalTransition_updatesStatus() {
        ApplicationEntity application = ApplicationEntity.builder()
                .status(ApplicationStatus.APPLIED)
                .build();

        application.transitionTo(ApplicationStatus.SCREENING);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.SCREENING);
    }

    @Test
    void transitionTo_illegalTransition_throwsAndLeavesStatusUnchanged() {
        ApplicationEntity application = ApplicationEntity.builder()
                .status(ApplicationStatus.APPLIED)
                .build();

        assertThatThrownBy(() -> application.transitionTo(ApplicationStatus.HIRED))
                .isInstanceOf(IllegalStateTransitionException.class)
                .hasMessageContaining("Cannot transition application from APPLIED to HIRED");

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.APPLIED); // unchanged
    }

    @Test
    void transitionTo_fromTerminalState_alwaysThrows() {
        ApplicationEntity application = ApplicationEntity.builder()
                .status(ApplicationStatus.HIRED)
                .build();

        assertThatThrownBy(() -> application.transitionTo(ApplicationStatus.SCREENING))
                .isInstanceOf(IllegalStateTransitionException.class);
    }

    @Test
    void transitionTo_toRejected_isAlwaysLegalFromNonTerminalStates() {
        ApplicationEntity application = ApplicationEntity.builder()
                .status(ApplicationStatus.INTERVIEW)
                .build();

        application.transitionTo(ApplicationStatus.REJECTED);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
    }
}
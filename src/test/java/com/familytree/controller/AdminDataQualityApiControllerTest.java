package com.familytree.controller;

import com.familytree.dto.DataQualityReportDto;
import com.familytree.services.DataQualityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDataQualityApiControllerTest {

    @Mock
    private DataQualityService dataQualityService;

    private AdminDataQualityApiController controller() {
        return new AdminDataQualityApiController(dataQualityService);
    }

    @Test
    void reportDelegatesToService() {
        DataQualityReportDto report = new DataQualityReportDto(List.of(), List.of(), List.of(), List.of());
        when(dataQualityService.buildReport()).thenReturn(report);

        assertThat(controller().report()).isEqualTo(report);
    }
}

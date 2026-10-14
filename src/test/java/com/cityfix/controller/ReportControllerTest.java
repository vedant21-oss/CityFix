package com.cityfix.controller;

import com.cityfix.dto.CreateReportRequest;
import com.cityfix.exception.DuplicateUpvoteException;
import com.cityfix.exception.ReportNotFoundException;
import com.cityfix.model.Category;
import com.cityfix.model.Report;
import com.cityfix.model.ReportStatus;
import com.cityfix.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    private Report sampleReport(Long id, ReportStatus status, int upvoteCount) {
        Report report = new Report();
        report.setId(id);
        report.setTitle("Pothole");
        report.setCategory(Category.POTHOLE);
        report.setArea("Downtown");
        report.setStatus(status);
        report.setUpvoteCount(upvoteCount);
        return report;
    }

    @Test
    void createReportReturns201() throws Exception {
        Report report = new Report();
        report.setId(1L);
        report.setTitle("Pothole");
        report.setCategory(Category.POTHOLE);
        report.setStatus(ReportStatus.REPORTED);
        when(reportService.createReport(any(CreateReportRequest.class))).thenReturn(report);

        String json = """
                {
                  "title": "Pothole",
                  "description": "Deep hole in the road",
                  "category": "POTHOLE",
                  "area": "Downtown",
                  "address": "Main St near no. 42",
                  "reporterName": "Ana",
                  "reporterEmail": "ana@example.com"
                }
                """;

        mockMvc.perform(post("/api/reports").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("REPORTED"));
    }

    @Test
    void getMissingReportReturns404() throws Exception {
        when(reportService.findReportById(99L)).thenThrow(new ReportNotFoundException(99L));

        mockMvc.perform(get("/api/reports/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Report with id 99 not found"));
    }

    @Test
    void createReportWithoutTitleReturns400() throws Exception {
        String json = """
                {
                  "description": "Deep hole in the road",
                  "category": "POTHOLE",
                  "area": "Downtown",
                  "reporterName": "Ana",
                  "reporterEmail": "ana@example.com"
                }
                """;

        mockMvc.perform(post("/api/reports").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void updateStatusReturns200WithTheNewStatus() throws Exception {
        when(reportService.updateStatus(1L, ReportStatus.IN_PROGRESS))
                .thenReturn(sampleReport(1L, ReportStatus.IN_PROGRESS, 0));

        mockMvc.perform(patch("/api/reports/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        verify(reportService).updateStatus(1L, ReportStatus.IN_PROGRESS);
    }

    @Test
    void updateStatusWithoutAStatusReturns400() throws Exception {
        mockMvc.perform(patch("/api/reports/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("status")));

        verify(reportService, never()).updateStatus(any(), any());
    }

    @Test
    void updateStatusForAMissingReportReturns404() throws Exception {
        when(reportService.updateStatus(99L, ReportStatus.FIXED)).thenThrow(new ReportNotFoundException(99L));

        mockMvc.perform(patch("/api/reports/99/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"FIXED\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Report with id 99 not found"));
    }

    @Test
    void upvoteReportReturns201WithTheNewCount() throws Exception {
        when(reportService.upvoteReport(1L, "ben@example.com"))
                .thenReturn(sampleReport(1L, ReportStatus.REPORTED, 3));

        mockMvc.perform(post("/api/reports/1/upvotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"voterEmail\": \"ben@example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.upvoteCount").value(3));

        verify(reportService).upvoteReport(1L, "ben@example.com");
    }

    @Test
    void upvoteReportTwiceReturns409() throws Exception {
        when(reportService.upvoteReport(1L, "ben@example.com"))
                .thenThrow(new DuplicateUpvoteException("ben@example.com"));

        mockMvc.perform(post("/api/reports/1/upvotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"voterEmail\": \"ben@example.com\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("ben@example.com has already upvoted this report"));
    }

    @Test
    void upvoteReportWithAnInvalidEmailReturns400() throws Exception {
        mockMvc.perform(post("/api/reports/1/upvotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"voterEmail\": \"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("voterEmail")));

        verify(reportService, never()).upvoteReport(any(), any());
    }

    @Test
    void listReportsPassesTheFiltersAndSortToTheService() throws Exception {
        when(reportService.findReports("Downtown", ReportStatus.REPORTED, Category.POTHOLE, "votes"))
                .thenReturn(List.of(sampleReport(1L, ReportStatus.REPORTED, 5)));

        mockMvc.perform(get("/api/reports")
                        .param("area", "Downtown")
                        .param("status", "REPORTED")
                        .param("category", "POTHOLE")
                        .param("sort", "votes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1));

        verify(reportService).findReports("Downtown", ReportStatus.REPORTED, Category.POTHOLE, "votes");
    }

    @Test
    void listReportsWithoutParametersSortsByNewest() throws Exception {
        when(reportService.findReports(null, null, null, "newest")).thenReturn(List.of());

        mockMvc.perform(get("/api/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(reportService).findReports(null, null, null, "newest");
    }

    @Test
    void getStatsReturnsCountsByStatusAndCategory() throws Exception {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        byStatus.put("REPORTED", 2L);
        byStatus.put("IN_PROGRESS", 1L);
        byStatus.put("FIXED", 0L);
        byStatus.put("REJECTED", 0L);

        Map<String, Long> byCategory = new LinkedHashMap<>();
        byCategory.put("POTHOLE", 3L);
        byCategory.put("OTHER", 0L);

        when(reportService.getStats()).thenReturn(Map.of("byStatus", byStatus, "byCategory", byCategory));

        mockMvc.perform(get("/api/reports/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byStatus.REPORTED").value(2))
                .andExpect(jsonPath("$.byStatus.IN_PROGRESS").value(1))
                .andExpect(jsonPath("$.byStatus.FIXED").value(0))
                .andExpect(jsonPath("$.byCategory.POTHOLE").value(3))
                .andExpect(jsonPath("$.byCategory.OTHER").value(0));
    }
}

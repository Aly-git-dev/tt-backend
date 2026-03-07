package com.upiiz.platform_api.repositories;

import com.upiiz.platform_api.entities.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface ChatReportContextRepo extends JpaRepository<ChatReportContext, ChatReportContext.PK> {
    List<ChatReportContext> findByReportIdOrderByContextIndexAsc(Long reportId);
}
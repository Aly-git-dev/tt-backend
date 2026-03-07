package com.upiiz.platform_api.repositories;

import com.upiiz.platform_api.entities.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface ChatMessageReportRepo extends JpaRepository<ChatMessageReport, Long> {
    List<ChatMessageReport> findByStatusOrderByCreatedAtDesc(String status);
}

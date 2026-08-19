package com.hyperproof.riskregister.repository;

import com.hyperproof.riskregister.domain.FrameworkControl;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FrameworkControlRepository extends JpaRepository<FrameworkControl, Long> {

    List<FrameworkControl> findAllByOrderByFrameworkAscCodeAsc();
}

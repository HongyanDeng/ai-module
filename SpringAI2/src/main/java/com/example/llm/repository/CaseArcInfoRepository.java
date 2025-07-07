package com.example.llm.repository;

import com.example.llm.entity.CaseArcInfo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CaseArcInfoRepository extends JpaRepository<CaseArcInfo, String>{

     //List<CaseArcInfo> findUuidByPersonId(String personId);

    List<CaseArcInfo> findByPsnNo(String psnNo);
}

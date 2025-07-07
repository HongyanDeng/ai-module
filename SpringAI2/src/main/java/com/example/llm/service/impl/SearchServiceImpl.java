package com.example.llm.service.impl;


import com.example.llm.repository.CaseArcInfoRepository;
import com.example.llm.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.example.llm.entity.CaseArcInfo;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final CaseArcInfoRepository caseArcInfoRepository;

    @Override
    public String searchResource(String personId,String name, String type, String searchContent) {
        /**
         * 输出接收到的查询条件
         */
        log.info("personId:{},name: {}, type: {}, searchContent: {}",personId, name, type, searchContent);

        /**
         * 接收到查询条件，现在需要在数据表中根据人员编号查询对应的案件编号，
         * 传输过来的的人员编号是personId,表中的人员编号是psnNo
         */
        /**
         * 利用人员编号查询数据库中的信息，返回案件编号UUID
         */
        List<CaseArcInfo> caseArcInfo = caseArcInfoRepository.findByPsnNo(personId);

        if (caseArcInfo != null&& !caseArcInfo.isEmpty()) {
            String uuid = caseArcInfo.get(0).getUuid(); // 获取 uuid 字段
            log.info("获取到的 UUID 是：{}", uuid); // 打印日志查看 uuid
            return uuid; // 返回 uuid

        } else {
            log.warn("未找到对应的案件信息");
            return "未找到对应的案件信息";
        }

       }
}

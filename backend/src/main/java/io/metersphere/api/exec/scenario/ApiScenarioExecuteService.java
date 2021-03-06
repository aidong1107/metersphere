package io.metersphere.api.exec.scenario;

import com.alibaba.fastjson.JSON;
import io.metersphere.api.cache.TestPlanReportExecuteCatch;
import io.metersphere.api.dto.EnvironmentType;
import io.metersphere.api.dto.RunModeDataDTO;
import io.metersphere.api.dto.automation.APIScenarioReportResult;
import io.metersphere.api.dto.automation.ExecuteType;
import io.metersphere.api.dto.automation.RunScenarioRequest;
import io.metersphere.api.dto.definition.RunDefinitionRequest;
import io.metersphere.api.dto.definition.request.ParameterConfig;
import io.metersphere.api.exec.queue.DBTestQueue;
import io.metersphere.api.exec.utils.GenerateHashTreeUtil;
import io.metersphere.api.jmeter.JMeterService;
import io.metersphere.api.service.*;
import io.metersphere.base.domain.ApiScenarioExample;
import io.metersphere.base.domain.ApiScenarioWithBLOBs;
import io.metersphere.base.domain.TestPlanApiScenario;
import io.metersphere.base.mapper.ApiScenarioMapper;
import io.metersphere.base.mapper.ApiScenarioReportMapper;
import io.metersphere.base.mapper.TestPlanApiScenarioMapper;
import io.metersphere.base.mapper.ext.ExtApiScenarioMapper;
import io.metersphere.commons.constants.APITestStatus;
import io.metersphere.commons.constants.ApiRunMode;
import io.metersphere.commons.constants.ReportTriggerMode;
import io.metersphere.commons.constants.TriggerMode;
import io.metersphere.commons.exception.MSException;
import io.metersphere.commons.utils.FileUtils;
import io.metersphere.commons.utils.LogUtil;
import io.metersphere.commons.utils.ServiceUtils;
import io.metersphere.commons.utils.SessionUtils;
import io.metersphere.constants.RunModeConstants;
import io.metersphere.dto.JmeterRunRequestDTO;
import io.metersphere.dto.MsExecResponseDTO;
import io.metersphere.dto.RunModeConfigDTO;
import io.metersphere.i18n.Translator;
import io.metersphere.plugin.core.MsTestElement;
import io.metersphere.service.EnvironmentGroupProjectService;
import io.metersphere.track.service.TestPlanScenarioCaseService;
import io.metersphere.utils.LoggerUtil;
import org.apache.commons.beanutils.BeanComparator;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections4.comparators.FixedOrderComparator;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.jorphan.collections.HashTree;
import org.mybatis.spring.SqlSessionUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ApiScenarioExecuteService {
    @Resource
    private ApiScenarioEnvService apiScenarioEnvService;
    @Resource
    private ApiScenarioMapper apiScenarioMapper;
    @Resource
    private ApiExecutionQueueService apiExecutionQueueService;
    @Resource
    private ExtApiScenarioMapper extApiScenarioMapper;
    @Resource
    private TestPlanApiScenarioMapper testPlanApiScenarioMapper;
    @Resource
    private ApiScenarioReportMapper apiScenarioReportMapper;
    @Resource
    @Lazy
    private TestPlanScenarioCaseService testPlanScenarioCaseService;
    @Resource
    private ApiScenarioReportService apiScenarioReportService;
    @Resource
    private RemakeReportService remakeReportService;
    @Resource
    private EnvironmentGroupProjectService environmentGroupProjectService;
    @Resource
    private ApiScenarioReportStructureService apiScenarioReportStructureService;
    @Resource
    private ApiScenarioSerialService apiScenarioSerialService;
    @Resource
    private ApiScenarioParallelService apiScenarioParallelService;
    @Resource
    private TcpApiParamService tcpApiParamService;
    @Resource
    private JMeterService jMeterService;
    @Resource
    private SqlSessionFactory sqlSessionFactory;

    public List<MsExecResponseDTO> run(RunScenarioRequest request) {
        if (LoggerUtil.getLogger().isDebugEnabled()) {
            LoggerUtil.debug("Scenario run-??????????????????-????????????????????????????????? " + JSON.toJSONString(request) + " ???");
        }
        if (StringUtils.isEmpty(request.getTriggerMode())) {
            request.setTriggerMode(ReportTriggerMode.MANUAL.name());
        }
        if (request.getConfig() == null) {
            request.setConfig(new RunModeConfigDTO());
        }

        if (StringUtils.equals("GROUP", request.getConfig().getEnvironmentType()) && StringUtils.isNotEmpty(request.getConfig().getEnvironmentGroupId())) {
            request.getConfig().setEnvMap(environmentGroupProjectService.getEnvMap(request.getConfig().getEnvironmentGroupId()));
        }

        // ??????????????????
        String serialReportId = null;
        LoggerUtil.info("Scenario run-??????????????????-?????????????????????????????? ");
        List<ApiScenarioWithBLOBs> apiScenarios = this.get(request);
        // ???????????????????????????????????????????????????
        if (apiScenarios != null && apiScenarios.size() == 1 && (apiScenarios.get(0).getStepTotal() == null || apiScenarios.get(0).getStepTotal() == 0)) {
            MSException.throwException((apiScenarios.get(0).getName() + "???" + Translator.get("automation_exec_info")));
        }
        // ????????????
        LoggerUtil.info("Scenario run-??????????????????-????????????????????????????????????????????????");
        apiScenarioEnvService.checkEnv(request, apiScenarios);
        // ??????????????????
        if (request.getConfig() != null && StringUtils.equals(request.getConfig().getReportType(), RunModeConstants.SET_REPORT.toString()) && StringUtils.isNotEmpty(request.getConfig().getReportName())) {
            if (request.getConfig().getMode().equals(RunModeConstants.SERIAL.toString())) {
                request.setExecuteType(ExecuteType.Completed.name());
            } else {
                request.setExecuteType(ExecuteType.Marge.name());
            }
            serialReportId = UUID.randomUUID().toString();
        }
        List<MsExecResponseDTO> responseDTOS = new LinkedList<>();

        Map<String, RunModeDataDTO> executeQueue = new LinkedHashMap<>();
        List<String> scenarioIds = new ArrayList<>();
        StringBuilder scenarioNames = new StringBuilder();

        LoggerUtil.info("Scenario run-??????????????????-?????????????????????");
        if (StringUtils.equalsAny(request.getRunMode(), ApiRunMode.SCENARIO_PLAN.name(), ApiRunMode.SCHEDULE_SCENARIO_PLAN.name(), ApiRunMode.JENKINS_SCENARIO_PLAN.name())) {
            //??????????????????
            assemblyPlanScenario(apiScenarios, request, executeQueue, scenarioIds, scenarioNames);
        } else {
            // ??????????????????
            assemblyScenario(apiScenarios, request, executeQueue, scenarioIds, scenarioNames, serialReportId);
        }
        LoggerUtil.info("Scenario run-??????????????????-??????????????????????????????" + executeQueue.size());

        if (GenerateHashTreeUtil.isSetReport(request.getConfig())) {
            LoggerUtil.info("Scenario run-??????????????????-????????????????????????" + serialReportId);
            request.getConfig().setReportId(UUID.randomUUID().toString());

            APIScenarioReportResult report = apiScenarioReportService.init(request.getConfig().getReportId(),
                    JSON.toJSONString(CollectionUtils.isNotEmpty(scenarioIds) && scenarioIds.size() > 50 ? scenarioIds.subList(0, 50) : scenarioIds),
                    scenarioNames.length() >= 3000 ? scenarioNames.substring(0, 2000) : scenarioNames.deleteCharAt(scenarioNames.length() - 1).toString(),
                    ReportTriggerMode.MANUAL.name(), ExecuteType.Saved.name(), request.getProjectId(), request.getReportUserID(), request.getConfig(), JSON.toJSONString(scenarioIds));

            report.setName(request.getConfig().getReportName());
            report.setId(serialReportId);
            request.getConfig().setAmassReport(serialReportId);
            apiScenarioReportMapper.insert(report);
            responseDTOS.add(new MsExecResponseDTO(JSON.toJSONString(scenarioIds), serialReportId, request.getRunMode()));
            // ????????????????????????
            if (request.getConfig() != null && request.getConfig().getMode().equals(RunModeConstants.PARALLEL.toString())) {
                apiScenarioReportStructureService.save(apiScenarios, serialReportId, request.getConfig() != null ? request.getConfig().getReportType() : null);
            }
        }
        // ????????????
        if (executeQueue != null && executeQueue.size() > 0) {
            String reportType = request.getConfig().getReportType();
            DBTestQueue executionQueue = apiExecutionQueueService.add(executeQueue, request.getConfig().getResourcePoolId(), ApiRunMode.SCENARIO.name(), serialReportId, reportType, request.getRunMode(), request.getConfig().getEnvMap());
            if (request.getConfig() != null && request.getConfig().getMode().equals(RunModeConstants.SERIAL.toString())) {
                if (StringUtils.isNotEmpty(serialReportId)) {
                    apiScenarioReportStructureService.save(apiScenarios, serialReportId, request.getConfig() != null ? request.getConfig().getReportType() : null);
                }
                SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH);
                ApiScenarioReportMapper batchMapper = sqlSession.getMapper(ApiScenarioReportMapper.class);
                // ???????????????????????????????????????
                if (StringUtils.isEmpty(serialReportId)) {
                    for (String reportId : executeQueue.keySet()) {
                        APIScenarioReportResult report = executeQueue.get(reportId).getReport();
                        report.setStatus(APITestStatus.Waiting.name());
                        batchMapper.insert(report);
                        responseDTOS.add(new MsExecResponseDTO(executeQueue.get(reportId).getTestId(), reportId, request.getRunMode()));
                    }
                    sqlSession.flushStatements();
                    if (sqlSession != null && sqlSessionFactory != null) {
                        SqlSessionUtils.closeSqlSession(sqlSession, sqlSessionFactory);
                    }
                }
                if (executionQueue.getQueue() != null) {
                    apiScenarioSerialService.serial(executionQueue, executionQueue.getQueue());
                }
            } else {
                apiScenarioParallelService.parallel(executeQueue, request, serialReportId, responseDTOS, executionQueue);
            }
        }
        return responseDTOS;
    }

    private List<ApiScenarioWithBLOBs> get(RunScenarioRequest request) {
        ServiceUtils.getSelectAllIds(request, request.getCondition(),
                (query) -> extApiScenarioMapper.selectIdsByQuery(query));
        List<String> ids = request.getIds();
        ApiScenarioExample example = new ApiScenarioExample();
        example.createCriteria().andIdIn(ids);
        List<ApiScenarioWithBLOBs> apiScenarios = apiScenarioMapper.selectByExampleWithBLOBs(example);
        if (request.getConfig() != null && request.getConfig().getMode().equals(RunModeConstants.SERIAL.toString())) {
            if (request.getCondition() == null || !request.getCondition().isSelectAll()) {
                // ??????id??????????????????
                FixedOrderComparator<String> fixedOrderComparator = new FixedOrderComparator<String>(ids);
                fixedOrderComparator.setUnknownObjectBehavior(FixedOrderComparator.UnknownObjectBehavior.BEFORE);
                BeanComparator beanComparator = new BeanComparator("id", fixedOrderComparator);
                Collections.sort(apiScenarios, beanComparator);
            }
        }
        return apiScenarios;
    }


    /**
     * ????????????????????????????????????????????????????????????
     */
    private void assemblyPlanScenario(List<ApiScenarioWithBLOBs> apiScenarios, RunScenarioRequest request, Map<String, RunModeDataDTO> executeQueue, List<String> scenarioIds, StringBuilder scenarioNames) {
        String reportId = request.getId();
        Map<String, String> planScenarioIdMap = request.getScenarioTestPlanIdMap();
        if (MapUtils.isEmpty(planScenarioIdMap)) {
            return;
        }
        String projectId = request.getProjectId();
        Map<String, ApiScenarioWithBLOBs> scenarioMap = apiScenarios.stream().collect(Collectors.toMap(ApiScenarioWithBLOBs::getId, Function.identity(), (t1, t2) -> t1));
        for (Map.Entry<String, String> entry : planScenarioIdMap.entrySet()) {
            String testPlanScenarioId = entry.getKey();
            String scenarioId = entry.getValue();
            ApiScenarioWithBLOBs scenario = scenarioMap.get(scenarioId);

            if (scenario.getStepTotal() == null || scenario.getStepTotal() == 0) {
                continue;
            }
            APIScenarioReportResult report;
            Map<String, String> planEnvMap = new HashMap<>();

            //?????????????????????????????????????????????????????????createScenarioReport?????????????????????????????????
            // ???????????????????????????????????????
            TestPlanApiScenario planApiScenario = testPlanApiScenarioMapper.selectByPrimaryKey(testPlanScenarioId);
            String envJson = planApiScenario.getEnvironment();
            String envType = planApiScenario.getEnvironmentType();
            String envGroupId = planApiScenario.getEnvironmentGroupId();
            if (StringUtils.equals(envType, EnvironmentType.JSON.toString()) && StringUtils.isNotBlank(envJson)) {
                planEnvMap = JSON.parseObject(envJson, Map.class);
            } else if (StringUtils.equals(envType, EnvironmentType.GROUP.toString()) && StringUtils.isNotBlank(envGroupId)) {
                planEnvMap = environmentGroupProjectService.getEnvMap(envGroupId);
            }
            if (StringUtils.isEmpty(projectId)) {
                projectId = testPlanScenarioCaseService.getProjectIdById(testPlanScenarioId);
            }
            if (StringUtils.isEmpty(projectId)) {
                projectId = scenario.getProjectId();
            }
            report = apiScenarioReportService.init(reportId, testPlanScenarioId, scenario.getName(), request.getTriggerMode(),
                    request.getExecuteType(), projectId, request.getReportUserID(), request.getConfig(), scenario.getId());

            if (report != null && StringUtils.isNotEmpty(request.getTestPlanReportId())) {
                Map<String, String> scenarioReportIdMap = new HashMap<>();
                scenarioReportIdMap.put(testPlanScenarioId, report.getId());
                TestPlanReportExecuteCatch.updateTestPlanThreadInfo(request.getTestPlanReportId(), null, scenarioReportIdMap, null);
            }
            scenarioIds.add(scenario.getId());
            if (request.getConfig() != null && StringUtils.isNotBlank(request.getConfig().getResourcePoolId())) {
                RunModeDataDTO runModeDataDTO = new RunModeDataDTO();
                runModeDataDTO.setTestId(testPlanScenarioId);
                runModeDataDTO.setPlanEnvMap(planEnvMap);
                runModeDataDTO.setReport(report);
                runModeDataDTO.setReportId(report.getId());
                executeQueue.put(report.getId(), runModeDataDTO);
            } else {
                try {
                    // ?????????????????????HashTree
                    RunModeDataDTO runModeDataDTO = new RunModeDataDTO(report, testPlanScenarioId);
                    if (request.getConfig() != null && !request.getConfig().getMode().equals(RunModeConstants.SERIAL.toString())) {
                        runModeDataDTO.setHashTree(GenerateHashTreeUtil.generateHashTree(scenario, reportId, planEnvMap, request.getConfig().getReportType()));
                    }
                    executeQueue.put(report.getId(), runModeDataDTO);
                } catch (Exception ex) {
                    scenarioIds.remove(scenario.getId());
                    if (StringUtils.equalsAny(request.getTriggerMode(), TriggerMode.BATCH.name(), TriggerMode.SCHEDULE.name())) {
                        remakeReportService.remakeScenario(request.getRunMode(), testPlanScenarioId, scenario, report);
                    } else {
                        MSException.throwException(ex);
                    }
                }
            }

            scenarioNames.append(scenario.getName()).append(",");

            // ??????????????????
            if (request.getConfig() == null || !StringUtils.equals(request.getConfig().getReportType(), RunModeConstants.SET_REPORT.toString())) {
                apiScenarioReportStructureService.save(scenario, report.getId(), request.getConfig() != null ? request.getConfig().getReportType() : null);
            }
            // ????????????ID
            reportId = UUID.randomUUID().toString();
        }
    }

    /**
     * ????????????????????????????????????????????????
     */
    private void assemblyScenario(List<ApiScenarioWithBLOBs> apiScenarios, RunScenarioRequest request, Map<String, RunModeDataDTO> executeQueue, List<String> scenarioIds, StringBuilder scenarioNames, String serialReportId) {
        String reportId = request.getId();
        for (int i = 0; i < apiScenarios.size(); i++) {
            ApiScenarioWithBLOBs item = apiScenarios.get(i);
            if (item.getStepTotal() == null || item.getStepTotal() == 0) {
                continue;
            }
            APIScenarioReportResult report = apiScenarioReportService.init(reportId, item.getId(), item.getName(), request.getTriggerMode(),
                    request.getExecuteType(), item.getProjectId(), request.getReportUserID(), request.getConfig(), item.getId());
            scenarioIds.add(item.getId());
            scenarioNames.append(item.getName()).append(",");
            if (request.getConfig() != null && StringUtils.isNotBlank(request.getConfig().getResourcePoolId())) {
                RunModeDataDTO runModeDataDTO = new RunModeDataDTO();
                runModeDataDTO.setTestId(item.getId());
                runModeDataDTO.setPlanEnvMap(new HashMap<>());
                if (request.getConfig().getEnvMap() != null) {
                    runModeDataDTO.setPlanEnvMap(request.getConfig().getEnvMap());
                }
                runModeDataDTO.setReport(report);
                runModeDataDTO.setReportId(report.getId());
                executeQueue.put(report.getId(), runModeDataDTO);
            } else {
                // ???????????????HashTree
                try {
                    RunModeDataDTO runModeDataDTO = new RunModeDataDTO(report, item.getId());
                    if (request.getConfig() != null && !request.getConfig().getMode().equals(RunModeConstants.SERIAL.toString())) {
                        HashTree hashTree = GenerateHashTreeUtil.generateHashTree(item, StringUtils.isNotEmpty(serialReportId) ? serialReportId + "-" + i : reportId, request.getConfig().getEnvMap(), request.getConfig().getReportType());
                        runModeDataDTO.setHashTree(hashTree);
                    }
                    runModeDataDTO.setPlanEnvMap(request.getConfig().getEnvMap());
                    executeQueue.put(report.getId(), runModeDataDTO);
                } catch (Exception ex) {
                    scenarioIds.remove(item.getId());
                    if (StringUtils.equalsAny(request.getTriggerMode(), TriggerMode.BATCH.name(), TriggerMode.SCHEDULE.name())) {
                        remakeReportService.remakeScenario(request.getRunMode(), null, item, report);
                    } else {
                        MSException.throwException(ex);
                    }
                }
            }
            // ??????????????????
            if (request.getConfig() == null || !StringUtils.equals(request.getConfig().getReportType(), RunModeConstants.SET_REPORT.toString())) {
                apiScenarioReportStructureService.save(item, report.getId(), request.getConfig() != null ? request.getConfig().getReportType() : null);
            }
            // ????????????ID
            reportId = UUID.randomUUID().toString();
        }
    }

    public void testElement(RunDefinitionRequest request) {
        if (request.getTestElement() != null) {
            tcpApiParamService.checkTestElement(request.getTestElement());
        }
    }

    public String debug(RunDefinitionRequest request, List<MultipartFile> bodyFiles, List<MultipartFile> scenarioFiles) {
        Map<String, String> map = request.getEnvironmentMap();
        String envType = request.getEnvironmentType();
        if (StringUtils.equals(envType, EnvironmentType.GROUP.toString())) {
            String environmentGroupId = request.getEnvironmentGroupId();
            map = environmentGroupProjectService.getEnvMap(environmentGroupId);
        }
        ParameterConfig config = new ParameterConfig();
        if (map != null) {
            apiScenarioEnvService.setEnvConfig(map, config);
        }
        HashTree hashTree = null;
        try {
            this.testElement(request);
            hashTree = request.getTestElement().generateHashTree(config);
            LogUtil.info(request.getTestElement().getJmx(hashTree));
        } catch (Exception e) {
            MSException.throwException(e.getMessage());
        }
        APIScenarioReportResult report = apiScenarioReportService.init(request.getId(), request.getScenarioId(), request.getScenarioName(), ReportTriggerMode.MANUAL.name(), request.getExecuteType(), request.getProjectId(),
                SessionUtils.getUserId(), request.getConfig(), request.getId());
        apiScenarioReportMapper.insert(report);
        if (request.isSaved()) {
            ApiScenarioWithBLOBs scenario = apiScenarioMapper.selectByPrimaryKey(request.getScenarioId());
            apiScenarioReportStructureService.save(scenario, report.getId(), request.getConfig() != null ? request.getConfig().getReportType() : null);
        } else {
            if (request.getTestElement() != null && CollectionUtils.isNotEmpty(request.getTestElement().getHashTree())) {
                ApiScenarioWithBLOBs scenario = new ApiScenarioWithBLOBs();
                scenario.setId(request.getScenarioId());
                MsTestElement testElement = request.getTestElement().getHashTree().get(0).getHashTree().get(0);
                if (testElement != null) {
                    scenario.setName(testElement.getName());
                    scenario.setScenarioDefinition(JSON.toJSONString(testElement));
                    apiScenarioReportStructureService.save(scenario, report.getId(), request.getConfig() != null ? request.getConfig().getReportType() : null);
                }
            }
        }
        uploadBodyFiles(request.getBodyFileRequestIds(), bodyFiles);
        FileUtils.createBodyFiles(request.getScenarioFileIds(), scenarioFiles);
        String runMode = StringUtils.isEmpty(request.getRunMode()) ? ApiRunMode.SCENARIO.name() : request.getRunMode();
        // ??????????????????
        JmeterRunRequestDTO runRequest = new JmeterRunRequestDTO(request.getId(), request.getId(), runMode, hashTree);
        runRequest.setDebug(true);
        jMeterService.run(runRequest);
        return request.getId();
    }

    private void uploadBodyFiles(List<String> bodyFileRequestIds, List<MultipartFile> bodyFiles) {
        if (CollectionUtils.isNotEmpty(bodyFileRequestIds)) {
            bodyFileRequestIds.forEach(requestId -> {
                FileUtils.createBodyFiles(requestId, bodyFiles);
            });
        }
    }
}


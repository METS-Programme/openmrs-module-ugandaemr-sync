package org.openmrs.module.ugandaemrsync.tasks;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.common.DateUtil;
import org.openmrs.module.reporting.common.MessageUtil;
import org.openmrs.module.reporting.common.ObjectUtil;
import org.openmrs.module.reporting.evaluation.EvaluationContext;
import org.openmrs.module.reporting.evaluation.EvaluationException;
import org.openmrs.module.reporting.evaluation.EvaluationUtil;
import org.openmrs.module.reporting.report.ReportData;
import org.openmrs.module.reporting.report.ReportDesign;
import org.openmrs.module.reporting.report.ReportDesignResource;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.reporting.report.definition.service.ReportDefinitionService;
import org.openmrs.module.reporting.report.renderer.RenderingException;
import org.openmrs.module.reporting.report.renderer.RenderingMode;
import org.openmrs.module.reporting.report.renderer.TextTemplateRenderer;
import org.openmrs.module.reporting.report.renderer.template.TemplateEngine;
import org.openmrs.module.reporting.report.renderer.template.TemplateEngineManager;
import org.openmrs.module.reporting.report.service.ReportService;
import org.openmrs.module.reporting.report.util.ReportUtil;
import org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService;
import org.openmrs.module.ugandaemrsync.server.SyncGlobalProperties;
import org.openmrs.module.ugandaemrsync.api.UgandaEMRHttpURLConnection;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.*;

import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.openmrs.module.ugandaemrsync.UgandaEMRSyncConfig.*;

/**
 * Posts Analytics data to the central server
 */

@Component
public class SendAnalyticsDataToCentralServerTask extends AbstractTask {

    protected Log log = LogFactory.getLog(getClass());
    Date startDate;
    Date endDate;
    Date lastSubmissionDateSet;

    UgandaEMRHttpURLConnection ugandaEMRHttpURLConnection = new UgandaEMRHttpURLConnection();

    SyncGlobalProperties syncGlobalProperties = new SyncGlobalProperties();

    @Autowired
    @Qualifier("reportingReportDefinitionService")
    protected ReportDefinitionService reportingReportDefinitionService;

    @Override
    public void execute() {
        Date todayDate = new Date();

        Properties properties = Context.getService(UgandaEMRSyncService.class).getUgandaEMRProperties();

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss");
        DateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 1);
        startDate= todayDate;
        endDate = cal.getTime();
        if (!isGpAnalyticsServerUrlSet()) {
            return;
        }
        if (!isGpDhis2OrganizationUuidSet()) {
            return;
        }

        String analyticsServerUrlEndPoint = syncGlobalProperties.getGlobalProperty(GP_ANALYTICS_SERVER_URL);
        String analyticsBaseUrl = ugandaEMRHttpURLConnection.getBaseURL(analyticsServerUrlEndPoint);

        String strSubmissionDate = Context.getAdministrationService()
                .getGlobalPropertyObject(GP_ANALYTICS_TASK_LAST_SUCCESSFUL_SUBMISSION_DATE).getPropertyValue();


        //Check internet connectivity
        if (!ugandaEMRHttpURLConnection.isConnectionAvailable()) {
            return;
        }

        //Check destination server availability
        if (!ugandaEMRHttpURLConnection.isServerAvailable(analyticsBaseUrl)) {
            return;
        }


            log.info("Sending analytics data to central server ");
            String facilityMetadata = null;
            try {
                facilityMetadata = getAnalyticsDataExport();
            } catch (EvaluationException e) {
                throw new RuntimeException(e);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            String dataEntryData = extractDataEntryStats(DateUtil.formatDate(startDate, "yyyy-MM-dd"),DateUtil.formatDate(endDate, "yyyy-MM-dd"));

            String jsonObject = "{"+ "\"metadata\":"  +facilityMetadata+ ",\"dataentry\":" +dataEntryData+"}";

            HttpResponse httpResponse = ugandaEMRHttpURLConnection.httpPost(analyticsServerUrlEndPoint, jsonObject, syncGlobalProperties.getGlobalProperty(GP_DHIS2_ORGANIZATION_UUID), syncGlobalProperties.getGlobalProperty(GP_DHIS2_ORGANIZATION_UUID));
            if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK || httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED) {

                ReportUtil.updateGlobalProperty(GP_ANALYTICS_TASK_LAST_SUCCESSFUL_SUBMISSION_DATE,
                        dateTimeFormat.format(lastSubmissionDateSet));
                log.info("Analytics data has been sent to central server");
            } else {
                log.info("Http response status code: " + httpResponse.getStatusLine().getStatusCode() + ". Reason: "
                        + httpResponse.getStatusLine().getReasonPhrase());
            }

    }

    private String extractDataEntryStats(String dateToday,String dateTmro) {
        String baseUrl = "http://localhost:8080";
        String baseUrl1 = "http://localhost:8081";
        String endpoint = "/openmrs/ws/rest/v1/dataentrystatistics?fromDate="+dateToday+"&toDate="+dateTmro+"&encUserColumn=creator&groupBy=creator";
        String url1 = baseUrl1 + endpoint;

        String url = baseUrl + endpoint;
        String response = getDataFromEndpoint(url1);
        if (response == "") {
            response = getDataFromEndpoint(url);
        }
        return response;
    }

    private String getAnalyticsDataExport() throws EvaluationException, IOException {
        EvaluationContext context = new EvaluationContext();
        ReportDefinitionService service = Context.getService(ReportDefinitionService.class);
        ReportDefinition rd = service.getDefinitionByUuid(ANALYTICS_DATA_EXPORT_REPORT_DEFINITION_UUID);
        ReportData reportData = null;
        if (rd != null) {

            Map<String, Object> parameterValues = new HashMap<String, Object>();
            context.setParameterValues(parameterValues);
            context.addParameterValue("endDate", endDate);
            context.addParameterValue("startDate", startDate);
            reportData = service.evaluate(rd, context);

        }


        List<ReportDesign> reportDesigns = Context.getService(ReportService.class).getReportDesigns(rd, null, false);

        ReportDesign reportDesign = reportDesigns.stream().filter(p -> "JSON".equals(p.getName())).findAny().orElse(null);


            String reportRendergingMode = JSON_REPORT_RENDERER_TYPE + "!" + reportDesign.getUuid();
            RenderingMode renderingMode = new RenderingMode(reportRendergingMode);
            if (!renderingMode.getRenderer().canRender(rd)) {
                throw new IllegalArgumentException("Unable to render Report with " + reportRendergingMode);
            }

            File file = new File(OpenmrsUtil.getApplicationDataDirectory() + "analytics");
            FileOutputStream fileOutputStream = new FileOutputStream(file);

            Writer pw = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8);
            TextTemplateRenderer textTemplateRenderer = new TextTemplateRenderer();
            ReportDesignResource reportDesignResource = textTemplateRenderer.getTemplate(reportDesign);
            String templateContents = new String(reportDesignResource.getContents(), StandardCharsets.UTF_8);
            templateContents = fillTemplateWithReportData(pw, templateContents, reportData, reportDesign, fileOutputStream);


            return templateContents;
    }




    public boolean isGpAnalyticsServerUrlSet() {
        if (isBlank(syncGlobalProperties.getGlobalProperty(GP_ANALYTICS_SERVER_URL))) {
            log.info("Analytics server URL is not set");
            return false;
        }
        return true;
    }

    public boolean isGpDhis2OrganizationUuidSet() {
        if (isBlank(syncGlobalProperties.getGlobalProperty(GP_DHIS2_ORGANIZATION_UUID))) {
            log.info("DHIS2 Organization UUID is not set");
            return false;
        }
        return true;
    }

    public String getDataFromEndpoint(String url) {

        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(url);
            CloseableHttpResponse response = httpClient.execute(httpGet);

            if (response.getStatusLine().getStatusCode() == 200 || response.getStatusLine().getStatusCode() == 200)
                return EntityUtils.toString(response.getEntity());
            else
                return "";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String fillTemplateWithReportData(Writer pw, String templateContents, ReportData reportData, ReportDesign reportDesign, FileOutputStream fileOutputStream) throws IOException, RenderingException {

        try {
            TextTemplateRenderer textTemplateRenderer = new TextTemplateRenderer();
            Map<String, Object> replacements = textTemplateRenderer.getBaseReplacementData(reportData, reportDesign);
            String templateEngineName = reportDesign.getPropertyValue("templateType", (String) null);
            TemplateEngine engine = TemplateEngineManager.getTemplateEngineByName(templateEngineName);
            if (engine != null) {
                Map<String, Object> bindings = new HashMap();
                bindings.put("reportData", reportData);
                bindings.put("reportDesign", reportDesign);
                bindings.put("data", replacements);
                bindings.put("util", new ObjectUtil());
                bindings.put("dateUtil", new DateUtil());
                bindings.put("msg", new MessageUtil());
                templateContents = engine.evaluate(templateContents, bindings);
            }

            String prefix = textTemplateRenderer.getExpressionPrefix(reportDesign);
            String suffix = textTemplateRenderer.getExpressionSuffix(reportDesign);
            templateContents = EvaluationUtil.evaluateExpression(templateContents, replacements, prefix, suffix).toString();
            pw.write(templateContents.toString());
            return templateContents;

        } catch (RenderingException var17) {
            throw var17;
        } catch (Throwable var18) {
            throw new RenderingException("Unable to render results due to: " + var18, var18);
        }
    }



}

package org.openmrs.module.ugandaemrsync.tasks;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.auth.BasicScheme;
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

import org.springframework.stereotype.Component;


import java.io.File;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.io.OutputStreamWriter;

import java.nio.charset.StandardCharsets;
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

    UgandaEMRHttpURLConnection ugandaEMRHttpURLConnection = new UgandaEMRHttpURLConnection();

    SyncGlobalProperties syncGlobalProperties = new SyncGlobalProperties();


    @Override
    public void execute() {
        Date todayDate = new Date();

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

            System.out.println(jsonObject);
            HttpResponse httpResponse = ugandaEMRHttpURLConnection.httpPost(analyticsServerUrlEndPoint, jsonObject, syncGlobalProperties.getGlobalProperty(GP_DHIS2_ORGANIZATION_UUID), syncGlobalProperties.getGlobalProperty(GP_DHIS2_ORGANIZATION_UUID));
            if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK || httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED) {
                log.info("Analytics data has been sent to central server");
            } else {
                log.info("Http response status code: " + httpResponse.getStatusLine().getStatusCode() + ". Reason: "
                        + httpResponse.getStatusLine().getReasonPhrase());
            }

    }

    private String extractDataEntryStats(String dateToday, String dateTmro) {
        String baseurl = Context.getAdministrationService().getGlobalProperty("ugandaemrsync.api.baseurl");
        String alternativeBaseurl = Context.getAdministrationService().getGlobalProperty("ugandaemrsync.api.baseurlAlternative");
        String endpoint = "/openmrs/ws/rest/v1/dataentrystatistics?fromDate=" + dateToday + "&toDate=" + dateTmro + "&encUserColumn=creator&groupBy=creator";
        String url1 = alternativeBaseurl + endpoint;
        String url = baseurl + endpoint;
        String response = "[]";
        try {
            response = getDataFromEndpoint(url,url1);
            System.out.println(response);
            if (!response.isEmpty() && response.charAt(0) != '[') {
                response = "[]";
            }
        } catch (Exception e) {
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

            File file = new File(OpenmrsUtil.getApplicationDataDirectory() + "/analytics");
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

    public String getDataFromEndpoint(String url,String alternativeUrl) {
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("admin", "Admin123");
        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(url);

            httpGet.addHeader(new BasicScheme().authenticate(credentials, httpGet, null));
            CloseableHttpResponse response = httpClient.execute(httpGet);

            if (response.getStatusLine().getStatusCode() == 200 || response.getStatusLine().getStatusCode() == 201)
                return EntityUtils.toString(response.getEntity());
            else if (response.getStatusLine().getStatusCode() == 404){
                HttpGet httpGet1 = new HttpGet(alternativeUrl);
                httpGet1.addHeader(new BasicScheme().authenticate(credentials, httpGet, null));
                CloseableHttpResponse response1 = httpClient.execute(httpGet1);
                if (response1.getStatusLine().getStatusCode() == 200 || response1.getStatusLine().getStatusCode() == 201){
                    return EntityUtils.toString(response.getEntity());
                }else{
                    return "[]";
                }
            }else{
                return "[]";
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (AuthenticationException e) {
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

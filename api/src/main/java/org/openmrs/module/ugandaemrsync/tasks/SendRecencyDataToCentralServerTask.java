package org.openmrs.module.ugandaemrsync.tasks;

import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.evaluation.EvaluationContext;
import org.openmrs.module.reporting.evaluation.parameter.Mapped;
import org.openmrs.module.reporting.report.ReportData;
import org.openmrs.module.reporting.report.ReportRequest;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.reporting.report.definition.service.ReportDefinitionService;
import org.openmrs.module.reporting.report.renderer.RenderingMode;
import org.openmrs.module.ugandaemrsync.UgandaEMRSyncConfig;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.ugandaemrsync.server.UgandaEMRHttpURLConnection;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.*;

/**
 * Posts recency data to the central server
 */

@Component
public class SendRecencyDataToCentralServerTask extends AbstractTask {
	
	protected Log log = LogFactory.getLog(getClass());
	
	UgandaEMRHttpURLConnection ugandaEMRHttpURLConnection = new UgandaEMRHttpURLConnection();
	
	//TODO: use syncGlobalProperties once it has been persisted on ugandaEMR database
	// SyncGlobalProperties syncGlobalProperties = new SyncGlobalProperties();
	
	@Autowired
	@Qualifier("reportingReportDefinitionService")
	protected ReportDefinitionService reportingReportDefinitionService;
	
	@Override
	public void execute() {
		log.info("Sending recency data to central server ");
		System.out.println("Sending recency data to central server");
		
		//SyncGlobalProperties syncGlobalProperties = new SyncGlobalProperties();
		//String recencyServerUrl = syncGlobalProperties.getGlobalProperty(UgandaEMRSyncConfig.RECENCY_UPLOADS_SERVER_URL);
		String recencyServerUrl = UgandaEMRSyncConfig.RECENCY_SERVER_URL;
		String testUrl = recencyServerUrl.substring(recencyServerUrl.indexOf("https://"),
		    recencyServerUrl.indexOf("recency"));
		//        try {
		// TODO: Add code to verify if there is internet connection and if MIRTH Server is available (log this if not)
		// Data Successfully uploaded
		
		//Check internet connectivity
		if (!ugandaEMRHttpURLConnection.isConnectionAvailable()) {
			return;
		}
		//Check destination server availability
		if (!ugandaEMRHttpURLConnection.isServerAvailable(testUrl)) {
			return;
		}
		String bodyText = renderReport();
		ugandaEMRHttpURLConnection.httpPost(recencyServerUrl, bodyText);
		
	}
	
	private String renderReport() {
		ReportDefinitionService reportDefinitionService = Context.getService(ReportDefinitionService.class);
		String strOutput = new String();
		try {
			ReportDefinition rd = reportDefinitionService.getDefinitionByUuid(UgandaEMRSyncConfig.RECENCY_DEFININATION_UUID);
			if (rd == null) {
				throw new IllegalArgumentException("Unable to find report with passed uuid = "
				        + UgandaEMRSyncConfig.RECENCY_DEFININATION_UUID);
			}
			RenderingMode renderingMode = new RenderingMode(UgandaEMRSyncConfig.REPORT_RENDERING_MODE);
			if (!renderingMode.getRenderer().canRender(rd)) {
				throw new IllegalArgumentException("Rendering mode chosen cannot render passed report definition");
			}
			
			EvaluationContext context = new EvaluationContext();
			ReportData reportData = reportDefinitionService.evaluate(rd, context);
			ReportRequest reportRequest = new ReportRequest();
			reportRequest.setReportDefinition(new Mapped<ReportDefinition>(rd, context.getParameterValues()));
			reportRequest.setRenderingMode(renderingMode);
			
			FileOutputStream fileOutputStream = new FileOutputStream(UgandaEMRSyncConfig.RECENCY_CSV_FILE_NAME);
			renderingMode.getRenderer().render(reportData, renderingMode.getArgument(), fileOutputStream);
			
			strOutput = this.readOutputFile(strOutput);
		}
		catch (Exception e) {
			log.info(e.toString());
		}
		
		return strOutput;
	}
	
	public String readOutputFile(String strOutput) throws Exception {
		FileInputStream fstreamItem = new FileInputStream(UgandaEMRSyncConfig.RECENCY_CSV_FILE_NAME);
		DataInputStream inItem = new DataInputStream(fstreamItem);
		BufferedReader brItem = new BufferedReader(new InputStreamReader(inItem));
		String phraseItem;
		
		while ((phraseItem = brItem.readLine()) != null) {
			strOutput = strOutput + phraseItem + System.lineSeparator(); /* consider using $$$ as the delimiter if Mirth is unable to read newline */
		}
		System.out.println(strOutput);
		brItem.close();
		
		return strOutput;
	}
}

package com.jenkins.testresultsaggregator.reporter;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;

import com.jenkins.testresultsaggregator.TestResultsAggregatorProjectAction;
import com.jenkins.testresultsaggregator.data.AggregatedDTO;
import com.jenkins.testresultsaggregator.data.DataDTO;
import com.jenkins.testresultsaggregator.data.DataJobDTO;
import com.jenkins.testresultsaggregator.data.JobStatus;
import com.jenkins.testresultsaggregator.helper.LocalMessages;

public class XMLReporter {
	public static final String REPORT_XML_FILE = "aggregated.xml";
	private PrintStream logger;
	private File workspace;
	public static final String S = "<";
	public static final String SE = "</";
	public static final String E = ">";
	
	public static final String ROOT = "Aggregated";
	public static final String RESULTS = "Results";
	public static final String JOBS = "JOBS";
	public static final String JOB = "JOB";
	public static final String NAME = "Name";
	public static final String FNAME = "FName";
	public static final String STATUS = "Status";
	public static final String URL = "URL";
	
	public XMLReporter(PrintStream logger, File rootDir) {
		this.logger = logger;
		this.workspace = rootDir;
	}
	
	public void generateXMLReport(AggregatedDTO aggregated) {
		try {
			logger.print(LocalMessages.GENERATE.toString() + " " + LocalMessages.XML_REPORT.toString());
			String fileName = workspace.getAbsolutePath() + System.getProperty("file.separator") + REPORT_XML_FILE;
			PrintWriter writer = new PrintWriter(fileName, "UTF-8");
			writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			writer.println(S + ROOT + E);
			
			writer.println(S + RESULTS + E);
			writer.println("<" + TestResultsAggregatorProjectAction.SUCCESS + ">" + aggregated.getCountJobSuccess() + "</" + TestResultsAggregatorProjectAction.SUCCESS + ">");
			writer.println("<" + TestResultsAggregatorProjectAction.ABORTED + ">" + aggregated.getCountJobAborted() + "</" + TestResultsAggregatorProjectAction.ABORTED + ">");
			writer.println("<" + TestResultsAggregatorProjectAction.FAILED + ">" + aggregated.getCountJobFailures() + "</" + TestResultsAggregatorProjectAction.FAILED + ">");
			writer.println("<" + TestResultsAggregatorProjectAction.RUNNING + ">" + aggregated.getCountJobRunning() + "</" + TestResultsAggregatorProjectAction.RUNNING + ">");
			writer.println("<" + TestResultsAggregatorProjectAction.UNSTABLE + ">" + aggregated.getCountJobUnstable() + "</" + TestResultsAggregatorProjectAction.UNSTABLE + ">");
			
			writer.println("<" + TestResultsAggregatorProjectAction.TOTAL + ">" + aggregated.getResults().getTotal() + "</" + TestResultsAggregatorProjectAction.TOTAL + ">");
			writer.println("<" + TestResultsAggregatorProjectAction.TOTAL_P + ">" + aggregated.getResults().getTotalDif() + "</" + TestResultsAggregatorProjectAction.TOTAL_P + ">");
			writer.println("<" + TestResultsAggregatorProjectAction.SUCCESS + ">" + aggregated.getResults().getPass() + "</" + TestResultsAggregatorProjectAction.SUCCESS + ">");
			writer.println("<" + TestResultsAggregatorProjectAction.SUCCESS_P + ">" + aggregated.getResults().getPassDif() + "</" + TestResultsAggregatorProjectAction.SUCCESS_P + ">");
			writer.println("<" + TestResultsAggregatorProjectAction.FAILED + ">" + aggregated.getResults().getFail() + "</" + TestResultsAggregatorProjectAction.FAILED + ">");
			writer.println("<" + TestResultsAggregatorProjectAction.FAILED_P + ">" + aggregated.getResults().getFailDif() + "</" + TestResultsAggregatorProjectAction.FAILED_P + ">");
			writer.println("<" + TestResultsAggregatorProjectAction.ABORTED + ">" + aggregated.getResults().getSkip() + "</" + TestResultsAggregatorProjectAction.ABORTED + ">");
			writer.println("<" + TestResultsAggregatorProjectAction.ABORTED_P + ">" + aggregated.getResults().getSkipDif() + "</" + TestResultsAggregatorProjectAction.ABORTED_P + ">");
			
			writer.println(SE + RESULTS + E);
			
			writer.println(S + JOBS + E);
			for (DataDTO data : aggregated.getData()) {
				for (DataJobDTO dataJob : data.getJobs()) {
					if (dataJob.getJenkinsBuild() != null) {
						writer.println(S + JOB + E);
						writer.println(xmlTag(NAME, dataJob.getJobName()));
						writer.println(xmlTag(FNAME, dataJob.getJobFriendlyName()));
						writer.println(xmlTag(STATUS, dataJob.getResultsDTO().getCurrentResult()));
						
						if (JobStatus.DISABLED.name().equalsIgnoreCase(dataJob.getResultsDTO().getCurrentResult())) {
							writer.println(xmlTag(URL, dataJob.getJenkinsJob().getUrl().toString()));
							writer.println(xmlTag(TestResultsAggregatorProjectAction.TOTAL, dataJob.getResultsDTO().getTotal()));
							writer.println(xmlTag(TestResultsAggregatorProjectAction.TOTAL_P, dataJob.getResultsDTO().getTotalDif()));
							writer.println(xmlTag(TestResultsAggregatorProjectAction.SUCCESS, dataJob.getResultsDTO().getPass()));
							writer.println(xmlTag(TestResultsAggregatorProjectAction.SUCCESS_P, dataJob.getResultsDTO().getPassDif()));
							writer.println(xmlTag(TestResultsAggregatorProjectAction.ABORTED, dataJob.getResultsDTO().getSkip()));
							writer.println(xmlTag(TestResultsAggregatorProjectAction.ABORTED_P, dataJob.getResultsDTO().getSkipDif()));
							writer.println(xmlTag(TestResultsAggregatorProjectAction.FAILED, dataJob.getResultsDTO().getFail()));
							writer.println(xmlTag(TestResultsAggregatorProjectAction.FAILED_P, dataJob.getResultsDTO().getFailDif()));
						} else if (JobStatus.NOT_FOUND.name().equalsIgnoreCase(dataJob.getResultsDTO().getCurrentResult())) {
							writer.println(xmlTag(URL, null));
							writer.println(xmlTag(TestResultsAggregatorProjectAction.TOTAL, 0));
							writer.println(xmlTag(TestResultsAggregatorProjectAction.TOTAL_P, 0));
							writer.println(xmlTag(TestResultsAggregatorProjectAction.SUCCESS, 0));
							writer.println(xmlTag(TestResultsAggregatorProjectAction.SUCCESS_P, 0));
							writer.println(xmlTag(TestResultsAggregatorProjectAction.ABORTED, 0));
							writer.println(xmlTag(TestResultsAggregatorProjectAction.ABORTED_P, 0));
							writer.println(xmlTag(TestResultsAggregatorProjectAction.FAILED, 0));
							writer.println(xmlTag(TestResultsAggregatorProjectAction.FAILED_P, 0));
						} else {
							writer.println(xmlTag(URL, dataJob.getJenkinsJob().getLastBuild().getUrl().toString()));
							writer.println(xmlTag(TestResultsAggregatorProjectAction.TOTAL, dataJob.getResultsDTO().getTotal()));
							writer.println(xmlTag(TestResultsAggregatorProjectAction.TOTAL_P, dataJob.getResultsDTO().getTotalDif()));
							writer.println(xmlTag(TestResultsAggregatorProjectAction.SUCCESS, dataJob.getResultsDTO().getPass()));
							writer.println(xmlTag(TestResultsAggregatorProjectAction.SUCCESS_P, dataJob.getResultsDTO().getPassDif()));
							writer.println(xmlTag(TestResultsAggregatorProjectAction.ABORTED, dataJob.getResultsDTO().getSkip()));
							writer.println(xmlTag(TestResultsAggregatorProjectAction.ABORTED_P, dataJob.getResultsDTO().getSkipDif()));
							writer.println(xmlTag(TestResultsAggregatorProjectAction.FAILED, dataJob.getResultsDTO().getFail()));
							writer.println(xmlTag(TestResultsAggregatorProjectAction.FAILED_P, dataJob.getResultsDTO().getFailDif()));
						}
						writer.println(SE + JOB + E);
					}
				}
			}
			writer.println(SE + JOBS + E);
			
			writer.println(SE + ROOT + E);
			writer.close();
			logger.println(LocalMessages.FINISHED.toString() + " " + LocalMessages.XML_REPORT.toString());
		} catch (
		
		IOException e) {
			logger.println("");
			logger.printf(LocalMessages.ERROR_OCCURRED.toString() + ": " + e.getMessage());
		}
	}
	
	private String xmlTag(String tag, Object content) {
		if (content != null) {
			return "<" + tag + ">" + content + "</" + tag + ">";
		}
		return "<" + tag + "></" + tag + ">";
	}
}

package com.jenkins.testresultsaggregator.helper;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.google.common.base.Strings;
import com.jenkins.testresultsaggregator.data.AggregateJobDTO;
import com.jenkins.testresultsaggregator.data.ChangeSetDTO;
import com.jenkins.testresultsaggregator.data.DataDTO;
import com.jenkins.testresultsaggregator.data.DataJobDTO;
import com.jenkins.testresultsaggregator.data.JenkinsBuildDTO;
import com.jenkins.testresultsaggregator.data.JenkinsJobDTO;
import com.jenkins.testresultsaggregator.data.JobStatus;
import com.jenkins.testresultsaggregator.data.ResultsDTO;

import hudson.util.Secret;

public class Collector {
	
	public static final String JOB = "job";
	public static final String API_JSON_URL = "api/json";
	public static final String LASTBUILD = "lastBuild";
	public static final String CHANGES = "changes";
	public static final String FAILCOUNT = "failCount";
	public static final String SKIPCOUNT = "skipCount";
	public static final String TOTALCOUNT = "totalCount";
	public static final String CONSOLE_OUTPUT = "console";
	public static final String TESTNG_REPORT = "testngreports";
	public static final String JUNIT_REPORT = "testReport";
	
	private String username;
	private Secret password;
	private String jenkinsUrl;
	private PrintStream logger;
	
	public Collector(PrintStream logger, String username, Secret password, String jenkinsUrl) {
		this.username = username;
		this.password = password;
		this.jenkinsUrl = jenkinsUrl;
		this.logger = logger;
	}
	
	public void collectResults(List<DataDTO> dataJob) throws InterruptedException {
		List<DataJobDTO> allDataJobDTO = new ArrayList<>();
		for (DataDTO temp : dataJob) {
			if (temp.getJobs() != null && !temp.getJobs().isEmpty()) {
				allDataJobDTO.addAll(temp.getJobs());
			}
		}
		ReportThread[] threads = new ReportThread[allDataJobDTO.size()];
		int index = 0;
		for (DataJobDTO tempDataJobDTO : allDataJobDTO) {
			threads[index] = new ReportThread(tempDataJobDTO);
			index++;
		}
		index = 0;
		for (ReportThread thread : threads) {
			thread.start();
			index++;
			if (index % 3 == 0) {
				Thread.sleep(1000);
			}
		}
		for (ReportThread thread : threads) {
			thread.join();
		}
	}
	
	private String authenticationString() {
		if (!Strings.isNullOrEmpty(username) && !Strings.isNullOrEmpty(password.getPlainText())) {
			return Base64.getEncoder().encodeToString(new String(username + ":" + new String(password.getPlainText())).getBytes());
		}
		return null;
	}
	
	public String getAPIConnection() throws IOException {
		URL jobUrlAPI = new URL(jenkinsUrl + "/" + API_JSON_URL);
		return Http.get(jobUrlAPI, authenticationString());
	}
	
	public JenkinsJobDTO getJobInfo(DataJobDTO dataJobDTO) {
		try {
			URL jobUrlAPI = new URL(jenkinsUrl + "/" + JOB + "/" + dataJobDTO.getJobName() + "/" + API_JSON_URL);
			String reply = Http.get(jobUrlAPI, authenticationString());
			return Deserialize.initializeObjectMapper().readValue(reply, JenkinsJobDTO.class);
		} catch (IOException e) {
		}
		return null;
	}
	
	public JenkinsBuildDTO getJobResults(DataJobDTO dataJobDTO) {
		try {
			URL jobUrlAPILastBuild = new URL(dataJobDTO.getJenkinsJob().getUrl() + "/" + LASTBUILD + "/" + API_JSON_URL);
			// Get Latest
			String reply = Http.get(jobUrlAPILastBuild, authenticationString());
			return Deserialize.initializeObjectMapper().readValue(reply, JenkinsBuildDTO.class);
		} catch (IOException e) {
		}
		return null;
	}
	
	public ResultsDTO getResults(DataJobDTO dataJobDTO) {
		if (dataJobDTO.getJenkinsBuild() != null) {
			ResultsDTO resultsDTO = new ResultsDTO();
			// Set Url
			resultsDTO.setUrl(dataJobDTO.getJenkinsJob().getUrl().toString());
			// Set Building status
			resultsDTO.setBuilding(dataJobDTO.getJenkinsBuild().getBuilding());
			// Set Current Result
			resultsDTO.setCurrentResult(dataJobDTO.getJenkinsBuild().getResult());
			// Set Description
			resultsDTO.setDescription(dataJobDTO.getJenkinsBuild().getDescription());
			// Set Duration
			resultsDTO.setDuration(dataJobDTO.getJenkinsBuild().getDuration());
			// Set Number
			resultsDTO.setNumber(dataJobDTO.getJenkinsBuild().getNumber());
			// Set TimeStamp
			if (dataJobDTO.getJenkinsBuild().getTimestamp() != null) {
				try {
					DateFormat formatter = new SimpleDateFormat("dd/MM/YYYY HH:mm:ss:SSS");
					String dateFormatted = formatter.format(new Date(dataJobDTO.getJenkinsBuild().getTimestamp()));
					resultsDTO.setTimestamp(dateFormatted);
				} catch (Exception ex) {
					
				}
			}
			// Set Change Set
			if (dataJobDTO.getJenkinsBuild().getChangeSets() != null) {
				int changes = 0;
				for (ChangeSetDTO tempI : dataJobDTO.getJenkinsBuild().getChangeSets()) {
					changes += tempI.getItems().size();
				}
				resultsDTO.setNumberOfChanges(changes);
			} else {
				resultsDTO.setNumberOfChanges(0);
			}
			// Set Changes URL
			resultsDTO.setChangesUrl(dataJobDTO.getJenkinsJob().getUrl() + "/" + dataJobDTO.getJenkinsBuild().getNumber() + "/" + CHANGES);
			
			// If Job is not running get results
			if (!resultsDTO.isBuilding()) {
				// Calculate FAIL,SKIP and TOTAL Test Results
				for (HashMap<Object, Object> temp : dataJobDTO.getJenkinsBuild().getActions()) {
					if (temp.containsKey(FAILCOUNT)) {
						resultsDTO.setFail((Integer) temp.get(FAILCOUNT));
					}
					if (temp.containsKey(SKIPCOUNT)) {
						resultsDTO.setSkip((Integer) temp.get(SKIPCOUNT));
					}
					if (temp.containsKey(TOTALCOUNT)) {
						resultsDTO.setTotal((Integer) temp.get(TOTALCOUNT));
					}
				}
				// Calculate Pass Results
				resultsDTO.setPass(resultsDTO.getTotal() - Math.abs(resultsDTO.getFail()) - Math.abs(resultsDTO.getSkip()));
				// Add Url
				resultsDTO.setConsoleUrl(dataJobDTO.getJenkinsJob().getUrl().toString() + dataJobDTO.getJenkinsBuild().getNumber() + "/" + CONSOLE_OUTPUT);
				String testNGUrl = dataJobDTO.getJenkinsJob().getUrl().toString() + dataJobDTO.getJenkinsBuild().getNumber() + "/" + TESTNG_REPORT;
				String junitsUrl = dataJobDTO.getJenkinsJob().getUrl().toString() + dataJobDTO.getJenkinsBuild().getNumber() + "/" + JUNIT_REPORT;
				if (Http.getResponseCode(testNGUrl, authenticationString()) == 200) {
					resultsDTO.setReportUrl(testNGUrl);
				} else if (Http.getResponseCode(junitsUrl, authenticationString()) == 200) {
					resultsDTO.setReportUrl(junitsUrl);
				} else {
					resultsDTO.setReportUrl(resultsDTO.getConsoleUrl());
				}
				// Calculate Previous Results
				if (dataJobDTO.getJenkinsBuild().getPreviousBuild() != null) {
					// Get Previous
					try {
						JenkinsBuildDTO jenkinsPreviousBuildDTO = Deserialize.initializeObjectMapper()
								.readValue(Http.get(dataJobDTO.getJenkinsBuild().getPreviousBuild().getUrl() + "/" + API_JSON_URL, authenticationString()), JenkinsBuildDTO.class);
						resultsDTO.setPreviousResult(jenkinsPreviousBuildDTO.getResult());
						// Calculate FAIL,SKIP and TOTAL of the Previous Test
						int previouslyFail = 0;
						int previouslyPass = 0;
						int previouslySkip = 0;
						for (HashMap<Object, Object> temp : jenkinsPreviousBuildDTO.getActions()) {
							if (temp.containsKey(FAILCOUNT)) {
								resultsDTO.setFailDif((Integer) temp.get(FAILCOUNT));
								previouslyFail += (Integer) temp.get(FAILCOUNT);
							}
							if (temp.containsKey(SKIPCOUNT)) {
								resultsDTO.setSkipDif((Integer) temp.get(SKIPCOUNT));
								previouslySkip += (Integer) temp.get(SKIPCOUNT);
							}
							if (temp.containsKey(TOTALCOUNT)) {
								resultsDTO.setTotalDif((Integer) temp.get(TOTALCOUNT));
								previouslyPass += (Integer) temp.get(TOTALCOUNT);
							}
						}
						// Calculate Pass Difference Results
						resultsDTO.setPassDif(previouslyPass - Math.abs(previouslyFail) - Math.abs(previouslySkip));
					} catch (Exception ex) {
						
					}
				}
			} else {
				resultsDTO.setCurrentResult(JobStatus.RUNNING.name());
				// Add Url
				resultsDTO.setConsoleUrl(dataJobDTO.getJenkinsJob().getUrl() + "/" + dataJobDTO.getJenkinsBuild().getNumber() + "/" + CONSOLE_OUTPUT);
				resultsDTO.setReportUrl(resultsDTO.getConsoleUrl());
			}
			return resultsDTO;
		}
		return null;
	}
	
	public class ReportThread extends Thread {
		
		DataJobDTO dataJobDTO;
		
		public ReportThread(DataJobDTO dataJobDTO) {
			this.dataJobDTO = dataJobDTO;
		}
		
		@Override
		public void run() {
			// Get Jenkins Job Info
			dataJobDTO.setJenkinsJob(getJobInfo(dataJobDTO));
			if (dataJobDTO.getJenkinsJob() == null) {
				// Job Not Found
				dataJobDTO.setJenkinsJob(new JenkinsJobDTO());
				dataJobDTO.setResultsDTO(new ResultsDTO(JobStatus.NOT_FOUND.name(), null));
				dataJobDTO.getResultsDTO().setUrl(null);
				dataJobDTO.setAggregate(new AggregateJobDTO());
				dataJobDTO.getAggregate().calculateReport(dataJobDTO.getResultsDTO());
				logger.println(LocalMessages.COLLECT_DATA.toString() + " '" + dataJobDTO.getJobName() + "' " + LocalMessages.JOB_NOT_FOUND.toString());
			} else if (!dataJobDTO.getJenkinsJob().getBuildable()) {
				// Job is Disabled/ Not Buildable
				String tempUrl = dataJobDTO.getJenkinsJob().getUrl().toString();
				dataJobDTO.setJenkinsJob(new JenkinsJobDTO());
				dataJobDTO.setResultsDTO(new ResultsDTO(JobStatus.DISABLED.name(), null));
				dataJobDTO.getResultsDTO().setUrl(tempUrl);
				dataJobDTO.setAggregate(new AggregateJobDTO());
				dataJobDTO.getAggregate().calculateReport(null);
				logger.println(LocalMessages.COLLECT_DATA.toString() + " '" + dataJobDTO.getJobName() + "' " + LocalMessages.JOB_IS_DISABLED.toString());
			} else if (dataJobDTO.getJenkinsJob() != null) {
				// Job Found and is Buildable
				// Get Job Results
				dataJobDTO.setJenkinsBuild(getJobResults(dataJobDTO));
				// Get Actual Results
				dataJobDTO.setResultsDTO(getResults(dataJobDTO));
				logger.println(LocalMessages.COLLECT_DATA.toString() + " '" + dataJobDTO.getJobName() + "' " + LocalMessages.FINISHED.toString());
			} else {
				logger.println("...");
			}
		}
	}
}
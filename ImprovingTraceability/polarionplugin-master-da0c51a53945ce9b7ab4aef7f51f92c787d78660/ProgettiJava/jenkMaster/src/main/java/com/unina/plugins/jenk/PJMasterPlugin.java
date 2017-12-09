package com.unina.plugins.jenk;
import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import jenkins.tasks.SimpleBuildStep;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.kohsuke.stapler.DataBoundConstructor;

import com.polarion.alm.ws.client.WebServiceFactory;
import com.polarion.alm.ws.client.session.SessionWebService;
import com.polarion.alm.ws.client.testmanagement.TestManagementWebService;
import com.polarion.alm.ws.client.types.testmanagement.TestRun;
import com.polarion.alm.ws.client.types.tracker.EnumOptionId;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Map;

import javax.xml.rpc.ServiceException;


public class PJMasterPlugin extends Builder implements SimpleBuildStep {

    private final String mxjob;
    private final String mtpjob;
    private final String address;
    private final String username;
    private final String password;
    private final String port;
	
    private String projectname;
    private boolean ssl;
    private String testrunid;
    private String jobname;
    private String type;
    private String ip;
    public Map<String, String> environmentMap;
    ArrayList<String> testCases;
    ArrayList<String> folders;
    
    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public PJMasterPlugin(String micromax, String mtp, String address, String port, String username, String password, boolean ssl, String ip) {
        this.mxjob = micromax;
        this.mtpjob = mtp;
        this.address = address;
		this.port = port;
        this.username = username;
        this.password = password;
        this.ssl=ssl;
        this.ip=ip;
    }

    public String getMicromax() {
        return this.mxjob;
    }
    
    public String getAddress(){
    	return this.address;
    }

    public String getUsername(){
    	return this.username;
    }

    public String getPassword(){
    	return this.password;
    }

    public String getMtp(){
    	return this.mtpjob;
    }
	
	public String getPort(){
		return this.port;
	}
	
	public String getIp(){
		return this.ip;
	}
	
	public boolean getSsl(){
		return this.ssl;
	}
    
    
        @Override
    public void perform(Run<?,?> build, FilePath workspace, Launcher launcher, TaskListener listener) {
    	
        // prelevo le variabili da Jenkins	
    	try {
			environmentMap = build.getEnvironment((TaskListener)listener);
			this.testrunid = replaceEnvironmentVariables((String)"%TESTRUNID%", environmentMap);
			this.jobname = replaceEnvironmentVariables((String)"%JOB_NAME%", environmentMap);
			this.type = replaceEnvironmentVariables((String)"%TYPE%", environmentMap);
			this.projectname = replaceEnvironmentVariables((String)"%PROJECTNAME%", environmentMap);
    	} catch (IOException | InterruptedException e) {
			listener.getLogger().println(e.getMessage());
		}

    //	listener.getLogger().println("nome TR: "+this.testrunid+" NomeJob: "+this.jobname);
    //	listener.getLogger().println("type: "+this.type+" NomeJob da richiamare: "+getMtp());
    	
    	ContactPolarion(true,listener);
    	
check:	try {

	    	HttpClient client = HttpClientBuilder.create().build();
	    	String url ="";
	    	String testrunenc=URLEncoder.encode(this.testrunid,"UTF-8");
	    	String projectenc=URLEncoder.encode(this.projectname,"UTF-8");
	    	String mxjobenc=URLEncoder.encode(this.mxjob,"UTF-8");
	    	String mtpjobenc=URLEncoder.encode(this.mtpjob,"UTF-8");
	    	String typeenc=URLEncoder.encode(this.type,"UTF-8");
	    	
	    	if(!ssl){
	    		url="http://";
	    	}
	    	else {
	    		url="https://";
	    	}
        	if(this.type.equals(this.mxjob)){
	    		// esegui mx
        		url=url+ip+":"+this.port+"/job/"+mxjobenc+"/buildWithParameters?TESTRUNID="+testrunenc+"&PROJECTNAME="+projectenc+"&TYPE="+typeenc;
        	}else if(this.type.equals(this.mtpjob)){
		   		// esegui mtp
		   		url=url+ip+":"+this.port+"/job/"+mtpjobenc+"/buildWithParameters?TESTRUNID="+testrunenc+"&PROJECTNAME="+projectenc+"&TYPE="+typeenc;;
        	}else{
		    	// notifica errore
		   		break check;
		    }
	        	HttpGet get = new HttpGet(url);
	 	       	client.execute(get);
			} catch (IOException e) {
		    	listener.getLogger().println(e.getMessage());
		    	ContactPolarion(false,listener);
			}

    }

    private static String replaceEnvironmentVariables(String stringa, Map<String, String> map) {
        String result = stringa;
        for (String varName : map.keySet()) {
            result = result.replace("%" + varName + "%", map.get(varName));
            result = result.replace("$" + varName, map.get(varName));
        }
        return result;
    }  

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    //Se stato è true setta il tr a running, se false setta il tr a open
    public void ContactPolarion(boolean stato, TaskListener listener) {
    	SessionWebService sessionService = null;
    	TestManagementWebService testService = null;
    	
    	try {
            WebServiceFactory wsf = new WebServiceFactory(this.address + "/ws/services/");
            listener.getLogger().println("Connecting to '" + this.address + "/ws/services/" + "'");
            sessionService = wsf.getSessionService();
        //  TrackerWebService trackerService = wsf.getTrackerService();
            testService = wsf.getTestManagementService();
            sessionService.logIn(this.username, this.password);
            listener.getLogger().println("Access with username: " + this.username);
            sessionService.beginTransaction();
            TestRun tr = testService.getTestRunById(this.projectname, this.testrunid);
            if(stato)
            	tr.setStatus(new EnumOptionId("running"));
            else
            	tr.setStatus(new EnumOptionId("open"));
            testService.updateTestRun(tr);
        }
        catch (RemoteException | ServiceException | MalformedURLException e) { 
        	listener.getLogger().println("Web Service error or RemoteException: Communication exception  with the server");
        	listener.getLogger().println(e.getMessage());
        }
        finally {
            if (sessionService != null) {
                try {
                    sessionService.endTransaction(false);
                    listener.getLogger().println("Closing connection");
                }
                catch (RemoteException e) {
                	listener.getLogger().println("Error in the closing phase of the session with Polarion");
                	listener.getLogger().println(e.getMessage());
                }
            }
        }
    }
    
    
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public DescriptorImpl() {
            load();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

//        public FormValidation doCheckName(@QueryParameter String job1, @QueryParameter String job2) throws IOException, ServletException {
//            return FormValidation.ok();
//        }
        
        public String getDisplayName() {
            return "PJ Master plugin";
        }
      
    }
    
}


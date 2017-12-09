package com.unina.plugins.jenk;
import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import hudson.tasks.Builder;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import hudson.tasks.BuildStepDescriptor;
import jenkins.tasks.SimpleBuildStep;

import org.jenkinsci.remoting.RoleChecker;
import org.kohsuke.stapler.DataBoundConstructor;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.rpc.ServiceException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import com.polarion.alm.ws.client.WebServiceFactory;
import com.polarion.alm.ws.client.session.SessionWebService;
import com.polarion.alm.ws.client.testmanagement.TestManagementWebService;
import com.polarion.alm.ws.client.tracker.TrackerWebService;
import com.polarion.alm.ws.client.types.testmanagement.TestRun;
import com.polarion.alm.ws.client.types.tracker.EnumOptionId;
import com.polarion.alm.ws.client.types.tracker.WorkItem;

public class PluginTest extends Builder implements SimpleBuildStep, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3026842591902074898L;
	
	private final String servername;
    private final String address;
    private final String username;
    private final String password;
    private final String shareduser;
    private final String sharedpasswd;
    private final String sharedpath;
    private final String mxpath;
    
    private String testrunid;
    private String projectname;
    private String jobname;
    private String type;
    private Boolean scenario;
    public Map<String, String> environmentMap;
    private ArrayList<String> testCases;
    private ArrayList<String> realIds;
    private ArrayList<String> folders;
    
    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public PluginTest(String servername, String address, String username, String password, String shareduser ,String sharedpasswd, String sharedpath, String mxpath, Boolean scenario) {
        this.servername = servername;
        this.address = address;
        this.username = username;
        this.password = password;
        this.sharedpasswd = sharedpasswd;
        this.shareduser = shareduser;
        this.mxpath = mxpath;
        this.sharedpath = sharedpath;
        this.scenario=scenario;
    }

    public String getShareduser(){
    	return this.shareduser;
    }
    
    public String getSharedpasswd(){
    	return this.sharedpasswd;
    }
    
    public String getServername(){
    	return this.servername;
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
    
    public String getSharedpath(){
    	return this.sharedpath;
    }
    
    public String getMxpath(){
    	return this.mxpath;
    }
    
    public Boolean getScenario() {
		return scenario;
	}
    
        @Override
    public void perform(Run<?,?> build, FilePath workspace, Launcher launcher, final TaskListener listener) {
    	
        // prelevo le variabili da Jenkins	
    	try {
			environmentMap = build.getEnvironment((TaskListener)listener);
			this.testrunid = replaceEnvironmentVariables((String)"%TESTRUNID%", environmentMap);
			this.jobname = replaceEnvironmentVariables((String)"%JOB_NAME%", environmentMap);
			this.projectname = replaceEnvironmentVariables((String)"%PROJECTNAME%", environmentMap);
			this.type = replaceEnvironmentVariables((String)"%TYPE%", environmentMap);
    	} catch (IOException | InterruptedException e) {
    		ContactPolarion(listener);
			e.printStackTrace();
			listener.getLogger().println(e.getMessage());
		}
    	
    	
    //Create a callable to execute job on slave
    	Callable<Integer, IOException> task = new Callable<Integer, IOException>() {
    	
    		private static final long serialVersionUID = 1L;

    		public Integer call() throws IOException {
    			// This code will run on the build slave
    			int ret=90;
    			testCases = new ArrayList<String>();
    			realIds = new ArrayList<String>();
    	    	String path = "./workspace/"+jobname;
				listener.getLogger().println("IL PATH DEL TESTRUN E' "+path);
    			/* folders = listFilesForFolder(new File(path),listener);
    			// cerco il TestRun nell'svn di Polarion
    	check:	for(String folder : folders){
    	        	listener.getLogger().println("confronto: "+testrunid+" - "+folder);
    				if(testrunid.equals(folder)){    					
    					// prelevo i TestCase relativi al TestRun
    					 */
    					testCases = getTestCases(path,listener);
    					String nometestrun = "Executed - "+testrunid+".xml";
    					if(!testCases.isEmpty()){
    						// prelevo l'ID vero dei Test Case
							realIds = getRealTestCaseIdFromPolarion(listener,testCases);
							if(type.equals("TestMTP")){ // ESECUZIONE SU MTP
								listener.getLogger().println("eseguo MTP");
								for(String realId : realIds){
								}
								/* Per l'esecuzione con lo stub (va messa fuori il for per funzionare)
		    					CreaXMLStub stub = new CreaXMLStub(testCases);
								stub.doTest("C:/JenkinsSlave/workspace/"+jobname+"/Temp/"+nometestrun,listener);*/
							}else if(type.equals("TestMX")){ // ESECUZIONE SU MICROMAX
								if(!scenario){
									ArrayList<Integer> returnCodes = new ArrayList<Integer>();
									for(String realId : realIds){
										// stampo il realId del test case
							        	listener.getLogger().println("TC: "+realId);
										listener.getLogger().println("eseguo MX");
										String[] parameters = realId.split(",");
										try {
											ProcessBuilder builder = new ProcessBuilder(
													"MxVGUI", "/n", "/r", mxpath+File.separator+parameters[0]+File.separator+parameters[1]+".mxp", mxpath+File.separator+parameters[0]+"/ScenariosAndTestCases/"+parameters[2]);    
											builder.redirectErrorStream(true); 
											Process p = builder.start();
											BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
											String line;
											while ((line = reader.readLine()) != null)
												listener.getLogger().println("tasklist: " + line);
											ret=p.waitFor();
											returnCodes.add(ret);										
											listener.getLogger().println("eseguito MX "+ret);
										} catch (Exception e) {
											// TODO Auto-generated catch block
											ContactPolarion(listener);
											listener.getLogger().println(e.getMessage());										}
									}
									
									//Scrive i risultati in base al valore di ritorno di MxVDEV
									writeTestResults("C:/JenkinsSlave/Results/"+nometestrun, testCases, returnCodes);	
								}
								//Converto tutti i risultati nel formato desiderato leggendo i log
								//convertTestCaseResults(realIds, "C:/JenkinsSlave/workspace/"+jobname+"/Temp/"+nometestrun, testCases);
								else{
									//convertScenarioResults(testCases, new ArrayList<String>(), mxpath+File.separator+parameters[0]+"/ScenariosAndTestCases/"+testrunid ,testrunid);
									String[] parameters = realIds.get(0).split(",");
									Scenario scenario = new Scenario(testrunid,realIds,mxpath+File.separator+parameters[0]+"/ScenariosAndTestCases/");
									if(scenario.createScenario()){
										listener.getLogger().println("Scenario creato");
										try {
											ProcessBuilder builder = new ProcessBuilder(
													"MxVGUI", "/n", "/r", mxpath+File.separator+parameters[0]+File.separator+parameters[1]+".mxp", mxpath+File.separator+parameters[0]+"/ScenariosAndTestCases/"+testrunid+".mxs");    
											builder.redirectErrorStream(true); 
											Process p = builder.start();
											BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
											String line;
											while ((line = reader.readLine()) != null)
												listener.getLogger().println("tasklist: " + line);
											ret=p.waitFor();
											listener.getLogger().println("eseguito MX "+ret);
											convertScenarioResults(testCases, new ArrayList<String>(), mxpath+File.separator+parameters[0]+"/ScenariosAndTestCases/"+testrunid ,testrunid);
											
										} catch (Exception e) {
											ContactPolarion(listener);
											listener.getLogger().println(e.getMessage());
										}
									}else{
										listener.getLogger().println("creazione dello scenario non riuscita");
									}						
									
								}
							}else{ // CONDIZIONE EVENTUALI ERRORI
								ContactPolarion(listener);
								listener.getLogger().println("condizione inaspettata");
							}
						
						publishTest("C:/JenkinsSlave/Results/",sharedpasswd,shareduser,nometestrun, listener, projectname);
					 	}else{
					 		ContactPolarion(listener);
					 		listener.getLogger().println("Nessun tc impostato. Setto tr a open");
					 	//}
					 	//break check;
    				//}
    			}
    			return ret;    	
    		}

			private void writeTestResults(String file, ArrayList<String> testCases, ArrayList<Integer> returnCodes) {
				try {		
					FileWriter writer = new FileWriter(file);
					listener.getLogger().println("inizio ad aggiungere test case");        			    
					writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
					writer.write("<testsuite>\n");
					Iterator<String> ittest = testCases.iterator();
					Iterator<Integer> itrets = returnCodes.iterator();
					while (itrets.hasNext() && ittest.hasNext()) {
						writer.write("\t<testcase classname=\"TC\" name=\""+ittest.next()+"\">\n");
						int ret=itrets.next();
						switch (ret) {
			            case 0:  writer.write("\t</testcase>\n");
			                     break;
			            case 1:  writer.write("\t\t<failure>\n\t\t\tScenario ran and one or more test cases were evaluated as failing\n\t\t</failure>\n");
								 writer.write("\t</testcase>\n");
								 break;
			            case 2:  writer.write("\t\t<error>\n\t\t\tA run-time error occurred during execution of the Scenario, usually caused by an error in VMC code\n\t\t</error>\n");
								 writer.write("\t</testcase>\n");
			                     break;
			            case 3:  writer.write("\t\t<error>\n\t\t\tScenario execution was aborted, or some of the test cases were unable to be run\n\t\t</error>\n");
								 writer.write("\t</testcase>\n");
			                     break;
			            case 4:  writer.write("\t\t<error>\n\t\t\tThe Scenario contained no runnable test cases, or was set to run forever\n\t\t</error>\n");
								 writer.write("\t</testcase>\n");
			                     break;
			            case 5:  writer.write("\t\t<error>\n\t\t\tScenario was not run at all, usually due to project setup error\n\t\t</error>\n");
								 writer.write("\t</testcase>\n");
			                     break;
			            case 6:  writer.write("\t\t<error>\n\t\t\tA Scenario that is part of a regression test is missing\n\t\t</error>\n");
								 writer.write("\t</testcase>\n");
			                     break;
			            default: writer.write("\t\t<error>\n\t\t\tUnknown error\n\t\t</error>\n");
			            		 writer.write("\t</testcase>\n");;
	                     	     break;
						}
					}
					writer.write("</testsuite>\n");
					writer.flush();
					writer.close();
				} catch (Exception  e) {
					ContactPolarion(listener);
					listener.getLogger().println(e.getMessage());
					listener.getLogger().println("dotest: \n"+e.getMessage());
				}
			}
    		
    		private void convertTestCaseResults(ArrayList<String> realIds,String file, ArrayList<String> tests) {
				// TODO Auto-generated method stub
    			try {		
    				FileWriter writer = new FileWriter(file);
    				listener.getLogger().println("inizio ad aggiungere test case");        			    
    				writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    				writer.write("<testsuite>\n");
    				Iterator<String> itreal = realIds.iterator();
    				Iterator<String> ittest = tests.iterator();
    				while (itreal.hasNext() && ittest.hasNext()) {
	    				//Legge il file mxlog
	    				File tr = new File("C:/Users/utente/Desktop/InvertSample/ScenariosAndTestCases/"+itreal.next()+".mxlog");	    
	        	    	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	        		    DocumentBuilder db;
	        			try {
	        				db = dbf.newDocumentBuilder();
	        				Document doc = db.parse(tr);
	        			    doc.getDocumentElement().normalize();
	        			    XPath xpath = XPathFactory.newInstance().newXPath();
	        			    XPathExpression xExpress = xpath.compile("//Str");
	        			    NodeList nl = (NodeList) xExpress.evaluate(doc, XPathConstants.NODESET);
	        			    int index = nl.getLength()-1; 
	        			    if(nl != null){
		    					writer.write("\t<testcase classname=\"TC\" name=\""+ittest.next()+"\">\n");
		    					String esito=nl.item(index).getTextContent();
		    					esito=esito.substring(esito.lastIndexOf(" ")+1);
		    					listener.getLogger().println(esito);   
		    					if(esito.equals("PASS")){
		    						writer.write("\t</testcase>\n");
		    					}
		    					else if(esito.equals("FAILURE")){
		    						writer.write("\t\t<failure>\n\t\t\tProva testo difetto failure (4-7)\n\t\t</failure>\n");
		    						writer.write("\t</testcase>\n");
		    					}else{
		    						writer.write("\t\t<error>\n\t\t\tProva testo difetto errore(5-9)\n\t\t</error>\n");
		    						writer.write("\t</testcase>\n");
		    					}
		    			    }else{
			    			    	listener.getLogger().println("nl vuota");
			    			    }
	        			} catch (ParserConfigurationException | SAXException | IOException | XPathExpressionException e) {
	        				ContactPolarion(listener);
	        				listener.getLogger().println(e.getMessage());
	        			}
    				}
        			writer.write("</testsuite>\n");
    				writer.flush();
    				writer.close();
    			
    			} catch (Exception  e) {
    				ContactPolarion(listener);
    				listener.getLogger().println(e.getMessage());
    				listener.getLogger().println("dotest: \n"+e.getMessage());
    			}	
			}
    		
    		private void convertScenarioResults(ArrayList<String> testcases, ArrayList<String> blocktests, String file, String testrun) {
				// TODO Auto-generated method stub
    			try {		
    				FileWriter writer = new FileWriter("C:/JenkinsSlave/Results/Executed - "+testrun+".xml");
    				File tr = new File(file+".mxlog");	    
    			    DocumentBuilder db;
    		    	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    				db = dbf.newDocumentBuilder();
    				Document doc = db.parse(tr);
    			    doc.getDocumentElement().normalize();
    			    XPath xpath = XPathFactory.newInstance().newXPath();
    			    XPathExpression xExpress = xpath.compile("//Str");
    			    NodeList nl = (NodeList) xExpress.evaluate(doc, XPathConstants.NODESET);
    			    ArrayList<String> ciao = new ArrayList<String>();
    			    if(nl != null){
    			    	writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    					writer.write("<testsuite>\n");
    				    int index=0;
    				    for(String testcase: testcases){
    				    	writer.write("\t<testcase classname=\"TC\" name=\""+testcase+"\">\n");
    				    	String testo=nl.item(index).getTextContent();
    				    	while(!testo.startsWith("Post ")){
    				    		index++;
    				    		testo=nl.item(index).getTextContent();
    				    	}
    							if(testo.substring(testo.lastIndexOf(" ")+1).equals("passed.")){
    								writer.write("\t</testcase>\n");
    							}
    							else if(testo.substring(testo.lastIndexOf(" ")+1).equals("failed.")){
    								writer.write("\t\t<failure>\n\t\t\tTest Case was evaluated as failed\n\t\t</failure>\n");
    								writer.write("\t</testcase>\n");
    							}
    							index++;
    				    	}
    				    for(String block: blocktests){
    				    	writer.write("\t<testcase classname=\"TC\" name=\""+block+"\">\n");
    				    	writer.write("\t\t<error>\n\t\t\tTest Case not found\n\t\t</error>\n");
    						writer.write("\t</testcase>\n");
    				    }
    						
    					
    					writer.write("</testsuite>\n");
    					writer.flush();
    					writer.close();
    			    }else{
    			    	ContactPolarion(listener);
    			    }
    				    
    				}catch(Exception e){
    					ContactPolarion(listener);
    					listener.getLogger().println(e.getMessage());
    				}
			}


    		
			@Override
    		public void checkRoles(RoleChecker arg0) throws SecurityException {
    			// TODO Auto-generated method stub	
    		}
    	};

    	// Get a "channel" to the build machine and run the task there
    	try {
			launcher.getChannel().call(task);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			ContactPolarion(listener);
			listener.getLogger().println(e.getMessage());
		}			
		}
    
    // ritorna un ArrayList contenente le cartelle presenti in un determinato path    
    private ArrayList<String> listFilesForFolder(File folder, TaskListener listener) {
    	ArrayList<String> lista = new ArrayList<String>();
    	if(folder.exists()){
	    	File[] listOfFiles = folder.listFiles();
			  if(listOfFiles != null){	
				for (File file : listOfFiles) {
					 if (file.isDirectory()) {	
				    	lista.add(file.getName());
				    }
				}
			  }
		}
		return lista;
	}

    private String replaceEnvironmentVariables(String stringa, Map<String, String> map) {
        String result = stringa;
        for (String varName : map.keySet()) {
            result = result.replace("%" + varName + "%", map.get(varName));
            result = result.replace("$" + varName, map.get(varName));
        }
        return result;
    }
    
    private ArrayList<String> getTestCases(String testRun,TaskListener listener) {	
		
        ArrayList<String> lista = new ArrayList<String>();
    	File tr = new File(testRun+File.separator+"testrun.xml");	 
    	
    	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	    DocumentBuilder db;
		try {
			db = dbf.newDocumentBuilder();
			Document doc = db.parse(tr);
		    doc.getDocumentElement().normalize();
		    listener.getLogger().println("PATH ASSOLUTO TESTRUN"+tr.getAbsolutePath());
		    listener.getLogger().println("PATH RELATIVO TESTRUN"+tr.getPath());
		    XPath xpath = XPathFactory.newInstance().newXPath();
		    XPathExpression xExpress = xpath.compile("//*[@id='testCase']");
		    NodeList nl = (NodeList) xExpress.evaluate(doc, XPathConstants.NODESET);
		    if(nl != null){
		    	listener.getLogger().println("inizio ad aggiungere test case");
			    for(int i=0; i<nl.getLength();i++){
			    	listener.getLogger().println(nl.item(i).getTextContent());
			    	lista.add(nl.item(i).getTextContent());
			    }
		    }else{
		    	listener.getLogger().println("nl vuota");
		    }
		} catch (ParserConfigurationException | SAXException | IOException | XPathExpressionException e) {
			ContactPolarion(listener);
			listener.getLogger().println(e.getMessage());
			e.printStackTrace();
		}
    
    return lista;
    }
    
    

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

	
    private ArrayList<String> getRealTestCaseIdFromPolarion(TaskListener listener, ArrayList<String> testCases) {
    	ArrayList<String> ids = new ArrayList<String>();
    	SessionWebService sessionService = null;
    	try {
            WebServiceFactory wsf = new WebServiceFactory(this.address + "/ws/services/");
            listener.getLogger().println("Connecting to '" + this.address + "/ws/services/" + "'");
            sessionService = wsf.getSessionService();
            TrackerWebService trackerService = wsf.getTrackerService();
            wsf.getTestManagementService();
            sessionService.logIn(this.username, this.password);
            listener.getLogger().println("Access with username: " + this.username);
            sessionService.beginTransaction();
            for(String test : testCases){
	            WorkItem tc = trackerService.getWorkItemById(this.projectname, test);
	            String real=trackerService.getCustomField(tc.getUri(), "realid").getValue().toString();
	            ids.add(real);
	            listener.getLogger().println("id reale: "+real);
            }            
        }
        catch (RemoteException | ServiceException | MalformedURLException e) { 
        	ContactPolarion(listener);
        	listener.getLogger().println(e.getMessage());
        	listener.getLogger().println("Web Service error or RemoteException: Communication exception  with the server");
        }
        finally {
            if (sessionService != null) {
                try {
                    sessionService.endTransaction(false);
                    listener.getLogger().println("Closing connection");
                }
                catch (RemoteException e) {
                	ContactPolarion(listener);
                	listener.getLogger().println(e.getMessage());
                	listener.getLogger().println("Error in the closing phase of the session with Polarion");
                }
            }
        }
      return ids;
    }
    
    public void ContactPolarion(TaskListener listener) {
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
           	tr.setStatus(new EnumOptionId("open"));
            testService.updateTestRun(tr);
        }
        catch (RemoteException | ServiceException | MalformedURLException e) { 
        	listener.getLogger().println(e.getMessage());
        	listener.getLogger().println("Web Service error or RemoteException: Communication exception  with the server");
        }
        finally {
            if (sessionService != null) {
                try {
                    sessionService.endTransaction(false);
                    listener.getLogger().println("Closing connection");
                }
                catch (RemoteException e) {
                	listener.getLogger().println(e.getMessage());
                	listener.getLogger().println("Error in the closing phase of the session with Polarion");
                }
            }
        }
    }
    
    public void publishTest(String path, String sharedpasswd, String shareduser, String nometestrun, TaskListener listener, String projectname){
		String url = "smb://"+sharedpath+File.separator+projectname+File.separator+nometestrun;
		NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(null, shareduser, sharedpasswd);
		SmbFile dest;
		try {
			dest = new SmbFile(url,auth);
			File source = new File(path+nometestrun);
			if(!dest.exists())
				dest.createNewFile();
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(dest.getOutputStream()));
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(source)));
			String str = "";
			while((str = reader.readLine())!= null){
				writer.write(str);
			}
			writer.flush();
			writer.close();
			reader.close();
			source.delete();
		} catch (Exception e) {
			ContactPolarion(listener);
			listener.getLogger().println(e.getMessage());
			listener.getLogger().println("publish: \n"+e.getMessage());
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

        public String getDisplayName() {
            return "PJ Test Plugin";
        }
      
    }
    
}


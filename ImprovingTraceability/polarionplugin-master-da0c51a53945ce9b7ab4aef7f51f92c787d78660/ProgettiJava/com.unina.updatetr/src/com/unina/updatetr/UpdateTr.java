package com.unina.updatetr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.security.auth.Subject;

import com.polarion.alm.projects.IProjectService;
import com.polarion.alm.projects.model.IProjectGroup;
import com.polarion.alm.tracker.ITestManagementService;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.internal.model.TestRun;
import com.polarion.alm.tracker.model.ITestRun;
import com.polarion.alm.tracker.model.IWorkItem;
//import com.polarion.alm.tracker.model.parameters.IParameter;
//import com.polarion.alm.tracker.model.parameters.IParametersAndDefinitionsManager;
import com.polarion.platform.ITransactionService;
import com.polarion.platform.context.IContext;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.jobs.GenericJobException;
import com.polarion.platform.jobs.IJobDescriptor;
import com.polarion.platform.jobs.IJobStatus;
import com.polarion.platform.jobs.IJobUnit;
import com.polarion.platform.jobs.IJobUnitFactory;
import com.polarion.platform.jobs.IProgressMonitor;
import com.polarion.platform.jobs.spi.AbstractJobUnit;
import com.polarion.platform.jobs.spi.BasicJobDescriptor;
import com.polarion.platform.jobs.spi.JobParameterPrimitiveType;
import com.polarion.platform.jobs.spi.SimpleJobParameter;
import com.polarion.platform.security.AuthenticationFailedException;
import com.polarion.platform.security.ISecurityService;

public class UpdateTr implements IJobUnitFactory {
	
	@Override
	public IJobUnit createJobUnit(String arg0) throws GenericJobException {
		return new Inner(arg0, this);
	}

	@Override
	public IJobDescriptor getJobDescriptor(IJobUnit arg0) {
		BasicJobDescriptor desc = new BasicJobDescriptor("updatetr", arg0);
        JobParameterPrimitiveType stringType = new JobParameterPrimitiveType("String", String.class);  		// tipo String
        
        desc.addParameter(new SimpleJobParameter(desc.getRootParameterGroup(),"path","description",stringType)); 
        desc.addParameter(new SimpleJobParameter(desc.getRootParameterGroup(),"user","description",stringType)); 
		return desc;

	}

	@Override
	public String getName() {
		return IUpdateTr.JOB_NAME;
	}

	private final class Inner extends AbstractJobUnit  implements IUpdateTr{
		
		private String path;
		private String user;
		
		public Inner(String name, IJobUnitFactory creator) {   // costruttore
			super(name, creator);
		}

		protected IJobStatus runInternal(IProgressMonitor progress) {
			IProjectService projectService = (IProjectService) PlatformContext.getPlatform().lookupService(IProjectService.class);
			// definisce il tracker service
			ITrackerService ts = (ITrackerService) PlatformContext.getPlatform().lookupService(ITrackerService.class);
			ISecurityService ss = (ISecurityService) PlatformContext.getPlatform().lookupService(ISecurityService.class);
			Subject sub=new Subject();
			try {
				sub= ss.loginUserFromVault(user, "system");
			} catch (AuthenticationFailedException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
			
			IContext scope = getScope();
			progress.beginTask(getName(), 0);
			
			IProjectGroup project = projectService.getProjectGroupForContextId(scope.getId()); // controllo se il progetto è un progetto di Gruppo
			if (project == null) {
				return getStatusFailed("Scope '" + scope.getId() + "' is not project.", null);
			}	
			
			final ITransactionService TransactionService = PlatformContext.getPlatform().lookupService(ITransactionService.class);
			   final ITestManagementService TestService = PlatformContext.getPlatform().lookupService(ITestManagementService.class);
			
			   
			ss.doAsUser(sub, new PrivilegedAction() {
	            public Object run() {
	            	if(TransactionService.canBeginTx()) {
	     			   TransactionService.beginTx();
	     			   
	     			String nomeProgetto = path.substring(path.lastIndexOf("/")+1 , path.length());      
	      
	     			  
	     			   try {				
	     				
	     				File file =new File(path+"/"+nomeProgetto+".txt");
						if(file.exists() && !file.isDirectory()) { 

							BufferedReader fr = new BufferedReader(new FileReader(file));
							String str = new String();

							while((str = fr.readLine()) != null){
								ITestRun itr = TestService.getTestRun(nomeProgetto, str);
								if(!itr.isUnresolvable() && itr!=null){
									ITestRun itrnew = TestService.getTestRun(nomeProgetto, "Executed - "+str);
									Collection<String> customfields = new ArrayList<String>(); 
									customfields = ((TestRun)itrnew).getCustomFieldsList();
									for(String cf : customfields){
										itrnew.setCustomField(cf, itr.getCustomField(cf));
									}
										//Settaggio altri parametri (keep in history non si può settare)
										//itrnew.setType(itr.getType());
										//itrnew.setSelectTestCasesBy(itr.getSelectTestCasesBy());
										//itrnew.setGroupId(itr.getGroupId());
									itrnew.save();
									itr.delete();
								}
							}
							fr.close();
							file.delete();
						}	     			   
	     			   } catch (IOException e1) {
	     				// TODO Auto-generated catch block
	     				e1.printStackTrace();
	     			} 
	     			   
	     				try {
	     				   TransactionService.commitTx();
	     				} catch (Exception e) {
	     					TransactionService.rollbackTx();
	     					return getStatusFailed("Failure", e);
	     				}
	             }
	                return null;
	            }
	        });
        
        progress.done();
        return getStatusOK("Success");
		}

		@Override
		public void setPath(String path) {
			this.path=path;		
		}

		@Override
		public void setUser(String user) {
			this.user=user;
		}
		
		
	}
	
}

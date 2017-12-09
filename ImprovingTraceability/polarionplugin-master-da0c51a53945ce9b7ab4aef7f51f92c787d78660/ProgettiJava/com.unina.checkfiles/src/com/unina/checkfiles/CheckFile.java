package com.unina.checkfiles;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import com.polarion.alm.tracker.ITestManagementService;
import com.polarion.platform.ITransactionService;
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

public class CheckFile implements IJobUnitFactory {

	@Override
	public IJobUnit createJobUnit(String name) throws GenericJobException {
		// TODO Auto-generated method stub
		return new Inner(name,this);
	}

	@Override
	public IJobDescriptor getJobDescriptor(IJobUnit arg0) {
		BasicJobDescriptor desc = new BasicJobDescriptor("testimporter", arg0);
        JobParameterPrimitiveType stringType = new JobParameterPrimitiveType("String", String.class);  		// tipo String
        
        desc.addParameter(new SimpleJobParameter(desc.getRootParameterGroup(),"path","description",stringType)); 
		return desc;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return ICheckFile.JOB_NAME;
	}
	
private final class Inner extends AbstractJobUnit  implements ICheckFile{
		
		private String path;
		
		public Inner(String name, IJobUnitFactory creator) {   // costruttore
			super(name, creator);
		}

		protected IJobStatus runInternal(IProgressMonitor progress) {
			
			ITransactionService TransactionService = PlatformContext.getPlatform().lookupService(ITransactionService.class);
			ITestManagementService TestService = PlatformContext.getPlatform().lookupService(ITestManagementService.class);
			 
			if(TransactionService.canBeginTx()) {
			   TransactionService.beginTx();
			   
			String nomeProgetto = path.substring(path.lastIndexOf("/")+1 , path.length());      
 
			  
			   try {
				
				File folder = new File(path);
				ArrayList<String> listaTest = listFilesForFolder(folder);
				
				File file =new File(path+"/"+nomeProgetto+".txt");
				BufferedWriter fr = new BufferedWriter(new FileWriter(file, true));
				
				for(String nome : listaTest){
					nome=nome.substring(11 , nome.length());
					nome=nome.substring(0 , nome.lastIndexOf("."));
					fr.write(nome+"\n");
				}
				fr.close();
			   
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
        
        progress.done();
        return getStatusOK("Success");
		}

		@Override
		public void setPath(String path) {
			this.path=path;	
		}
		
		private ArrayList<String> listFilesForFolder(File folder) {
			File[] listOfFiles = folder.listFiles();
			ArrayList<String> lista = new ArrayList<String>();
			// controllare se lista!=null
			for (File file : listOfFiles) {
			    if (file.isFile() && file.getName().substring(file.getName().lastIndexOf(".")+1 , file.getName().length()).equals("xml")) {
			    	lista.add(file.getName());
			    }
			}
			return lista;
		}
		
	}
}

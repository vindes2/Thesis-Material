package com.unina.plugins.jenk;


import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Scenario{

	private String path = "";
	private String nome_file = "";
	private ArrayList<String> lista = null;
	private boolean flag = true;
	
	public Scenario(String nome_file, ArrayList<String> lista, String path){
		this.nome_file = nome_file;
		this.lista = lista;
		this.path = path;
	}
	
	public boolean createScenario(){
		
		try {

			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

			Document doc = docBuilder.newDocument();

			// mxv:MxVDevFile element
			Element mxv = doc.createElement("mxv:MxVDevFile");
			doc.appendChild(mxv);
			mxv.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
			mxv.setAttribute("SchemaVersion", "3.53");
			mxv.setAttribute("Filename", this.nome_file+".mxs");
			mxv.setAttribute("xmlns:mxv", "urn:MicroMax:MxVDev:3.x");

			// scenario elements
			Element versionControl = doc.createElement("VersionControl");
			mxv.appendChild(versionControl);
			versionControl.setAttribute("SubstitutionString", "$Revision: $");
			
			// scenario elements
			Element authoringTool = doc.createElement("AuthoringTool");
			mxv.appendChild(authoringTool);
			authoringTool.setAttribute("Vendor", "MicroMax, Inc.");
			authoringTool.setAttribute("Name", "Mx-VDev");
			authoringTool.setAttribute("Version", "3.36.47.34300");
						
			// scenario elements
			Element scenario = doc.createElement("Scenario");
			mxv.appendChild(scenario);
			scenario.setAttribute("Name", this.nome_file);
			scenario.setAttribute("Duration", "90");
			
			// windowState elements
			Element windowState = doc.createElement("WindowState");
			windowState.setAttribute("Visible", "true");
			scenario.appendChild(windowState);

			// AuthoringTools elements
			Element authoringTools = doc.createElement("AuthoringTools");
			scenario.appendChild(authoringTools);
			
			// tool elements
			Element tool = doc.createElement("Tool");
			authoringTools.appendChild(tool);

			// windowState elements
			Element jobs = doc.createElement("Jobs");
			scenario.appendChild(jobs);
			
			int i = 1;
			for(String s : lista){
				
				// job elements
				Element job = doc.createElement("Job");
				jobs.appendChild(job);
				job.setAttribute("Id", Integer.toString(i));
				i++;
				
				String[] parameters =s.split(",");
				
				// testCase elements
				Element testCase = doc.createElement("Testcase");
				job.appendChild(testCase);
				testCase.appendChild(doc.createTextNode(".\\"+parameters[2]));
				
				// repetition elements
				Element repetition = doc.createElement("Repetitions");
				job.appendChild(repetition);
				repetition.appendChild(doc.createTextNode("1"));
			}
			
			Element requirements = doc.createElement("Requirements");
			scenario.appendChild(requirements);
			
			// write the content into xml file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(this.path+File.separator+this.nome_file+".mxs"));

			// Output to console for testing
			// StreamResult result = new StreamResult(System.out);

			transformer.transform(source, result);
			
		  } catch (ParserConfigurationException pce) {
			pce.printStackTrace();
			flag = false;
		  } catch (Exception tfe) {
			tfe.printStackTrace();
			flag = false;
		  }
		return flag;
	
	}

	

}

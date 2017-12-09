package com.unina.plugins.jenk;

import hudson.FilePath;
import hudson.model.TaskListener;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

public class CreaXMLStub {
	
	private List <String> tests;
	
	public CreaXMLStub(List <String> tests){
		this.tests= tests;
	}
	
	public void doTest(String file, TaskListener listener) {
		try {		
			listener.getLogger().println("posso scrivere dotest");
			FileWriter writer = new FileWriter(file);
			writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			writer.write("<testsuite>\n");
			for(String test : tests){
				writer.write("\t<testcase classname=\"TC\" name=\""+test+"\">\n");
				Integer cifra=Integer.parseInt(test.substring(test.length()-1));
				System.out.println(cifra);
				if(cifra >=0 && cifra <=3){
					writer.write("\t</testcase>\n");
				}
				else if(cifra >=4 && cifra <=6){
					writer.write("\t\t<failure>\n\t\t\tProva testo difetto failure (4-7)\n\t\t</failure>\n");
					writer.write("\t</testcase>\n");
				}else{
					writer.write("\t\t<error>\n\t\t\tProva testo difetto errore(5-9)\n\t\t</error>\n");
					writer.write("\t</testcase>\n");
				}
			}
			writer.write("</testsuite>\n");
			writer.flush();
			writer.close();
		
		} catch (Exception  e) {
			e.printStackTrace();
			listener.getLogger().println("dotest: \n"+e.getMessage());
		}
	}

}

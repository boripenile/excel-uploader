package com.mkyong.controller;

import org.javalite.http.Get;
import org.javalite.http.Http;
import org.javalite.http.Post;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.google.gson.Gson;
import com.mkyong.service.CommonUtil;
import com.mkyong.service.UploadSettlementService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Future;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

@Controller
public class UploadController {

    //Save the uploaded file to this folder
    private static String UPLOADED_FOLDER = Utility.getBasePath();

    @GetMapping("/")
    public String index() {
        return "upload";
    }

    //@Async
    @CrossOrigin(origins= {"*"})
    @PostMapping("/upload") // //new annotation since 4.3
    @ResponseStatus(value = HttpStatus.OK)
    public void singleFileUpload(@RequestParam("file") MultipartFile file,
    		@RequestParam("type") String type, @RequestParam("processor") String processor,
    		@RequestParam("Authorization") String authorization, RedirectAttributes redirectAttributes) 
    				throws Exception{
        try {
        	Properties properties = CommonUtil.loadPropertySettings("settings");
        	System.out.println(properties.get("base_url"));
			String baseUrl = properties.getProperty("base_url");
        	Get get = Http.get(baseUrl + "api/v1/config/switch/" 
            		+ type + "/" + processor, 30000, 30000)
            		.header("Authorization", authorization);
        	System.out.println("Got here.. " + get.responseCode());
        	String data = get.text();
        	if (data != null) {
        		Gson gson = new Gson();
        		ConfigResponse config = gson.fromJson(data, ConfigResponse.class);
        		//System.out.println(config.toString());
        		String fileName = file.getOriginalFilename();
        		byte[] bytes = file.getBytes();
                String fullPathFile = UPLOADED_FOLDER + fileName;
                Path path = Paths.get(fullPathFile);
                Files.write(path, bytes);
                
                UploadSettlementService parserService = new UploadSettlementService();
                String pid = fileName + "_" + System.currentTimeMillis();
                try {
					Future<Long> result = parserService.processAllSheets(fullPathFile, 
							config.getData().getRawColumns(), 1, processor, 
							authorization, fileName, pid);
					Long totalCount = result.get();
					System.out.println("I am done!. Total processed - " + totalCount);
				} catch (Exception e) {
					e.printStackTrace();
				} 
        	}
        
            // Get the file and save it somewhere
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @GetMapping("/uploadStatus")
    public String uploadStatus() {
        return "uploadStatus";
    }

}
package web.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.sun.org.apache.regexp.internal.recompile;

import retrieve.SearchPic;


@Controller
public class SearchController {
	
	@Autowired
	// 一些常用路径
	private final static String webAppRoot = System.getProperty("webAppRoot");
	private final static int webAppRootLen = webAppRoot.length();
	private final static String indexPath = webAppRoot + "index";
	private final static String imagesPath = webAppRoot + "imgs\\test\\download";
	private final static String searchImagePath = webAppRoot + "imgs\\temp";
	private final static int  pageSize = 16;
	
	// 缓存检索结果
	private static LinkedHashMap<String, ArrayList<String>> reslutsCollect = new LinkedHashMap<String, ArrayList<String>>(1000, 7.5f) {
		protected boolean removeEldestEntry(Map.Entry<String, ArrayList<String>> eldest) {
			if(this.size()>750)	return true;
			else return false;
		}
	};
	
	// 首页
	@RequestMapping(value="index", method=RequestMethod.GET)
	public ModelAndView index(HttpServletRequest request){
		
		System.out.println(webAppRoot);
//		File file = new File(".\\sss.txt");
//		System.out.println(file.getAbsolutePath());
		
		ModelAndView mv = new ModelAndView();
		mv.setViewName("index");
		return mv;
	}
	
	// 打开结果页
	@RequestMapping(value="result", method=RequestMethod.GET)
	public ModelAndView result(HttpServletRequest request, String pageIndex) throws IOException{
		ModelAndView mv = new ModelAndView();
		
		// 无参数
		mv.setViewName("index");
		if(pageIndex==null)	return mv;
		int pIndex = Integer.parseInt(pageIndex);
		if(pIndex<0) return mv;
		
		// 设置循环条件
		int start = pIndex * pageSize;
		ArrayList<String> hits = reslutsCollect.get(request.getSession().getId());
		int resultLen = hits.size();
		int end = 0;
		if((pIndex+1) * pageSize<=resultLen) end = start + pageSize;
		else end = resultLen;
		ArrayList<String> resultImagesPath = new ArrayList<String>();
        for (int i = start; i < end; i++) {	// 每页最多显示16张图片
        	resultImagesPath.add(hits.get(i));
        }
		mv.addObject("resultImagesPath", resultImagesPath);
		mv.addObject("pageIndex", pageIndex);
		mv.addObject("total", hits.size());
		mv.setViewName("result");
		return mv;
	}
	
	// 处理检索请求
	@RequestMapping(value="searchImage", method=RequestMethod.POST)
	public ModelAndView searchImage(HttpServletRequest request, MultipartFile imageFile, Integer shapeType, String p1Lng, String p1Lat, String p2Lng, String p2Lat) throws IOException{
//		System.out.println(shapeType + " " + p1Lng + " " + p1Lat + " " + p2Lng + " " + p2Lat);
		
		String tempImagePath = null;
        if(!imageFile.isEmpty()){
        	// 获得sessionId
            String sessionId = request.getSession().getId();
            // 获得文件类型（可以判断如果不是图片，禁止上传）  
            String fileName=imageFile.getOriginalFilename();
            tempImagePath= webAppRoot + "\\imgs\\temp\\"+ sessionId + fileName.substring(fileName.lastIndexOf('.'));
            imageFile.transferTo(new File(tempImagePath));
        }
        ArrayList<String> hits = null;
        if(!tempImagePath.isEmpty()) {
        	// 获得检索结果
//        	hits = searchService.search(indexPath, tempImagePath);
        	if(0==shapeType){
        		try {
        			hits = SearchPic.searchQuery("", tempImagePath, p1Lng, p1Lat);
        			System.out.println("检索结果 " + hits);
        			for(int i=0; i<hits.size(); i++){
        				hits.set(i, "imgs\\PicturesOFflickr_More\\" + hits.get(i) + ".jpg");
        			}
        		} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}
        	} else {
        	//	hits = SearchwithLIRE.search(webAppRoot, tempImagePath, p1Lng, p2Lng, p1Lat, p2Lat);
        	}
        	System.out.println("删除上传图片结果：" + new File(tempImagePath).delete());
        	System.out.println("SessionID : " + request.getSession().getId());
        	reslutsCollect.put(request.getSession().getId(), hits);
        }
        // 提取检索结果
        ArrayList<String> resultImagesPath = new ArrayList<String>();
        if(null == hits)	hits = new ArrayList<>();
        for (int i = 0; i < Math.min(hits.size(), 16); i++) {	// 每页最多显示16张图片
//        	System.out.println(ir.document(hits.documentID(i)).getValues(DocumentBuilder.FIELD_NAME_IDENTIFIER)[0]);
//        	resultImagesPath.add(ir.document(hits.documentID(i)).getValues(DocumentBuilder.FIELD_NAME_IDENTIFIER)[0].substring(webAppRootLen-1));
        	resultImagesPath.add(hits.get(i));
        }
		ModelAndView mv = new ModelAndView();
		mv.addObject("resultImagesPath", resultImagesPath);
		mv.addObject("total", hits.size());
		mv.addObject("pageIndex", 0);
		mv.setViewName("result");
//		mv.setViewName("index");
		System.out.println("total = " + hits.size() + "\n");
		return mv;
	}
	
	// 主函数
	public static void main(String[] args) {
		
	}
}

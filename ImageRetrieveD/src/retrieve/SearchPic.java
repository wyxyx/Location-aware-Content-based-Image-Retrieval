package retrieve;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Random;

import javax.imageio.ImageIO;

import org.apache.commons.math3.stat.descriptive.SynchronizedDescriptiveStatistics;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.spatial.SpatialStrategy;
import org.apache.lucene.spatial.prefix.RecursivePrefixTreeStrategy;
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTree;
import org.apache.lucene.spatial.query.SpatialArgs;
import org.apache.lucene.spatial.query.SpatialOperation;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import com.spatial4j.core.context.SpatialContext;
import com.spatial4j.core.distance.DistanceUtils;
import com.spatial4j.core.shape.Point;
import com.spatial4j.core.shape.Shape;
import net.semanticmetadata.lire.DocumentBuilder;
import net.semanticmetadata.lire.ImageSearchHits;
import net.semanticmetadata.lire.imageanalysis.SurfFeature;
import net.semanticmetadata.lire.imageanalysis.bovw.SurfFeatureHistogramBuilder;
import net.semanticmetadata.lire.impl.SurfDocumentBuilder;
import net.semanticmetadata.lire.impl.TopDocsImageSearcher;
import net.semanticmetadata.lire.impl.VisualWordsImageSearcher;

public class SearchPic {
	// 存的是id号（photoid）
	static ArrayList<String> ldis;
	static ArrayList<String> limg;
	static ArrayList<String> list;
	// 存的是距离distance和imgsim
	static ArrayList<Double> l_dis;
	static ArrayList<Double> l_img;
	
	//存图片的文件夹
	private static String INDEX_FILE_PATH = "H:\\PicturesOFflickr_More";

	//Spatial上下文 
	private static SpatialContext ctx;
	//提供索引和查询模型的策略接口 
	private static SpatialStrategy strategy;
	//索引目录 
	private static Directory directory;

	//Spatial初始化 
	protected static void init() throws IOException {
		ldis = new ArrayList<String>();
		limg = new ArrayList<String>();
		list = new ArrayList<String>();

		l_dis = new ArrayList<Double>();
		l_img = new ArrayList<Double>();
		
		ctx = SpatialContext.GEO; // GEO默认Geohash算法实现
		//设置网格最大为11层
		int maxLevels = 11;
        //Geohash算法实现
		SpatialPrefixTree grid = new GeohashPrefixTree(ctx, maxLevels);

		strategy = new RecursivePrefixTreeStrategy(grid, "myGeoField");
		// 初始化索引目录
		directory = FSDirectory.open(new File("C:\\Users\\huochairen\\Desktop\\Search\\index"));
	}

	//生成位置坐标索引
	private void indexLocPoints() throws Exception {

		IndexWriterConfig cfg = new IndexWriterConfig(Version.LUCENE_42, new WhitespaceAnalyzer(Version.LUCENE_42));

		IndexWriter indexWriter = new IndexWriter(
				FSDirectory.open(new File("C:\\Users\\huochairen\\Desktop\\Search\\index")), cfg);
		// 这里的x,y是经纬度

		//读取txt（id、lnt、lat部分）
		BufferedReader br = null;
		// 逐行添加测试数据到索引中，测试数据文件和源文件在同一个目录下
		br = new BufferedReader(new InputStreamReader(
				new FileInputStream(new File("C:\\Users\\huochairen\\Desktop\\SearchImg\\allpic.txt"))));
		String line = null;
		int num = 0;
		while ((line = br.readLine()) != null) {

			String[] array = line.split(",");
			
			long pid = Long.parseLong(array[0].trim());
			double lon_d = Double.valueOf(array[2]).doubleValue();
			double lat_d = Double.valueOf(array[1]).doubleValue();

			num++;

			Document b = new Document();

			indexWriter.addDocument(createDocument(b, pid, ctx.makePoint(lon_d, lat_d)));
			// System.out.println("num:"+num);
			
		}
		br.close();
		indexWriter.close();
	}

	//创建Document索引对象 
	private Document createDocument(Document doc, long id, Shape shape) {

		// 添加字段
		doc.add(new StoredField("id", id));
		// 添加数值类型的字段
		doc.add(new NumericDocValuesField("id", id));
		doc.add(new Field("id", id + "", Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.YES));

		for (Field f : strategy.createIndexableFields(shape)) {
			doc.add(f);
			Point pt = (Point) shape;
			doc.add(new StoredField(strategy.getFieldName(), pt.getX() + " " + pt.getY()));
			// System.out.println("id:"+id+ " point:"+pt.getX() + " "
			// + pt.getY());
		}
		return doc;
	}

	//位置搜索 
	private static void search(String lon, String lat) throws Exception {
		// ldis = new ArrayList<>();
		IndexReader indexReader = DirectoryReader
				.open(FSDirectory.open(new File("C:\\Users\\huochairen\\Desktop\\Search\\index")));
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);

		double longtitude = Double.parseDouble(lon);
		double latitude = Double.parseDouble(lat);
		// 搜索方圆50千米范围以内,这里longtitude, latitude分别是当前位置的经纬度，以当前位置为圆心，50千米为半径画圆
	
		SpatialArgs args = new SpatialArgs(SpatialOperation.Intersects, ctx.makeCircle(longtitude, latitude,
				DistanceUtils.dist2Degrees(50, DistanceUtils.EARTH_MEAN_RADIUS_KM)));
		// 计算当前用户地点与每个待匹配地点之间的距离
		ValueSource valueSource1 = strategy.makeDistanceValueSource(args.getShape().getCenter());
		
		// 按照距离降序排序
		Sort distSort1 = new Sort(valueSource1.getSortField(false)).rewrite(indexSearcher);

		// 根据SpatialArgs参数创建过滤器
		Filter filter = strategy.makeFilter(args);

		// 开始搜索
		TopDocs docs = indexSearcher.search(new MatchAllDocsQuery(), filter, 10000, distSort1);

		if (docs.scoreDocs.length != 0 && docs.scoreDocs.length >= 10) {

			ScoreDoc[] scoreDocs = docs.scoreDocs;
			for (ScoreDoc scoreDoc : scoreDocs) {
				int docId = scoreDoc.doc;

				Document document = indexSearcher.doc(docId);

				long gotid = document.getField("id").numericValue().longValue();
				String geoField = document.getField(strategy.getFieldName()).stringValue();
				int xy = geoField.indexOf(' ');
				double xPoint = Double.parseDouble(geoField.substring(0, xy));
				double yPoint = Double.parseDouble(geoField.substring(xy + 1));
				double distDEG = ctx.calcDistance(args.getShape().getCenter(), xPoint, yPoint);
				double juli = DistanceUtils.degrees2Dist(distDEG, DistanceUtils.EARTH_MEAN_RADIUS_KM);


				ldis.add(gotid + "");
				list.add(gotid + "");
				l_dis.add(juli);
			}

		} else if (docs.scoreDocs.length == 0 || docs.scoreDocs.length < 10) {
			// 定义一个坐标点(x,y)，即用户选择的地点
			Point pt = ctx.makePoint(longtitude, latitude);
			// 计算用户选择地点与每个待匹配地点之间的距离
			ValueSource valueSource = strategy.makeDistanceValueSource(pt);
			// 根据命中点与当前位置坐标点的距离排序，false表示降序
			Sort distSort = new Sort(valueSource.getSortField(false)).rewrite(indexSearcher);
			TopDocs topdocs = indexSearcher.search(new MatchAllDocsQuery(), 100, distSort);
			ScoreDoc[] scoreDocs = topdocs.scoreDocs;
			for (ScoreDoc scoreDoc : scoreDocs) {
				int docId = scoreDoc.doc;

				Document document = indexSearcher.doc(docId);

				// System.out.println("---"+document.getFields());

				long gotid = document.getField("id").numericValue().longValue();
				String geoField = document.getField(strategy.getFieldName()).stringValue();
				int xy = geoField.indexOf(' ');
				double xPoint = Double.parseDouble(geoField.substring(0, xy));
				double yPoint = Double.parseDouble(geoField.substring(xy + 1));
				double distDEG = ctx.calcDistance(pt, xPoint, yPoint);
				double juli = DistanceUtils.degrees2Dist(distDEG, DistanceUtils.EARTH_MEAN_RADIUS_KM);
				// System.out.println("docId:" + docId + ",id:" + gotid +
				// ",distance:" + juli + "KM");
				ldis.add(gotid + "");
				list.add(gotid + "");
				l_dis.add(juli);
			}
		}

		indexReader.close();
		// for(int k=0;k<l_dis.size();k++){
		// System.out.println("l_dis:"+l_dis.get(k));
		// }
	}

	public static ArrayList<String> searchQuery(String basePath, String queryImage, String lon, String lat)
			throws Exception {
		init();
		search(lon, lat);
		// limg=new ArrayList<>();
		// System.out.println(1 + " " + lon1 + " " + lon2 + " " + lat1 + " " +
		// lat2);

		BufferedImage im = ImageIO.read(new File(queryImage));
		VisualWordsImageSearcher vis = new VisualWordsImageSearcher(40000,
				DocumentBuilder.FIELD_NAME_SURF_VISUAL_WORDS);
		DocumentBuilder builder = new SurfDocumentBuilder();

		Document queryDoc = builder.createDocument(im, "query");

		// IndexReader ir=DirectoryReader.open(FSDirectory.open(new
		// File(basePath + "\\index")));
		IndexReader ir = DirectoryReader.open(FSDirectory.open(new File("H:\\lire\\index_1")));
		SurfFeatureHistogramBuilder sfh = new SurfFeatureHistogramBuilder(ir, 1000, 500);

		queryDoc = sfh.getVisualWords(queryDoc);
		ImageSearchHits hits = vis.search(queryDoc, ir);

		// System.out.println("命中数 ：" + hits.length());

		// searching with a Lucene document instance ...
		for (int i = 0; i < hits.length(); i++) {
			String fileName = hits.doc(i).getValues(DocumentBuilder.FIELD_NAME_IDENTIFIER)[0];

			/// **************************

			// 以下代码为提取文件路径中的文件名部分：
			// File tempFile =new File( fileName.trim());
			// String fileName1 = tempFile.getName();
			// 或者：
			String fName = fileName.trim();

			String fileName1 = fName.substring(fName.lastIndexOf("\\") + 1);// 取“\”之后的部分
			// 去后缀（.jpg），即保留photoid
			String fname = fileName1.replaceAll("\\..*$", "");
			// System.out.println(hits.score(i) + ": \t" + fname);
			limg.add(fname);

			// 将float型hits.score(i)转为double型
			BigDecimal b = new BigDecimal(String.valueOf(hits.score(i)));
			l_img.add(b.doubleValue());

		}
		// for(int k=0;k<hits.length();k++){
		// System.out.println("limg:"+limg.get(k));
		// }
		list.retainAll(limg);
		/*
		 * for(int k=0;k<list.size();k++){
		 * System.out.println("final:"+list.get(k)); }
		 * System.out.println("ldis.size();"+ldis.size());
		 * System.out.println("limg.size();"+limg.size());
		 * System.out.println("list.size();"+list.size());
		 * System.out.println("end.");
		 */
		return getTopK(list);

	}

	

	private static ArrayList<String> getTopK(ArrayList<String> ll) {
		ArrayList<Double> lresults = new ArrayList<Double>();

		ArrayList<String> lresultid = new ArrayList<String>();

		for (int k = 0; k < ll.size(); k++) {

			String id = ll.get(k);
			// System.out.println(id);
			double dis = l_dis.get(ldis.indexOf(id));

			double imgsim = l_img.get(limg.indexOf(id));
			double dissim = 0;
			if (l_dis.get(l_dis.size() - 1) != 0) {
				dissim = 1 - dis / l_dis.get(l_dis.size() - 1);
			}
			else
				dissim = 0;
			//设置参数a为0.5，按照公式计算
			double a = 0.5;
			double result = a * imgsim + (1 - a) * dissim;
			// System.out.println(result);
			lresults.add(result);
			lresultid.add(id);
		}
		// 对结果列表排序（快速排序），输出topk
		Qsort(lresultid, lresults, 0, lresults.size() - 1);

		int k = 10;

		/* for (int i = 0; i < lresults.size(); i++) {
		 System.out.println(lresultid.get(i)+": "+lresults.get(i));
		 }*/
		return lresultid;
	}

	private static int quicksort(ArrayList<String> arrid, ArrayList<Double> array, int low, int high) {
		// 固定的切分方式
		double key = array.get(low);
		String sk = arrid.get(low);
		while (low < high) {
			while (array.get(high) <= key && high > low) {// 从后向前扫描
				high--;
			}
			array.set(low, array.get(high));
			arrid.set(low, arrid.get(high));
			
			while (array.get(low) >= key && high > low) {// 从前向后扫描
				low++;
			}
			
			array.set(high, array.get(low));
			arrid.set(high, arrid.get(low));
		}
		array.set(high, key);
		arrid.set(high, sk);
		// array[high]=key;
		return high;
	}

	private static void Qsort(ArrayList<String> arrid, ArrayList<Double> array, int low, int high) {
		if (low >= high) {
			return;
		}
		int index = quicksort(arrid, array, low, high);
		Qsort(arrid, array, low, index - 1);
		Qsort(arrid, array, index + 1, high);
	}

	public static void main(String[] args) throws Exception {
	
		long startTime = System.currentTimeMillis(); // 获取开始时间

		Random random = new Random();

		int lon = Math.abs(random.nextInt()) % (180 - (-180) + 1) + (-180);
		int lat = Math.abs(random.nextInt()) % (90 - (-90) + 1) + (-90);
		System.out.println(lon + "," + lat);
		
		ArrayList<String> arrl = searchQuery("", "H:\\PicturesOFflickr_More\\10002328806.jpg", lon + "", lat + "");
		System.out.println("arr1 = " + arrl);

		long endTime = System.currentTimeMillis(); // 获取结束时间
		System.out.println("程序运行时间：" + (endTime - startTime) + "ms"); // 输出程序运行时间
		
		/*
		long startTime = System.currentTimeMillis();    //获取开始时间
    	//create_pic_Index imgtest = new create_pic_Index();  
    	//imgtest.init();  
    	//imgtest.indexLocPoints();  
    	//imgtest.search();  
    	
    	int n=50;
    	  ArrayList <String> ar1= new  ArrayList <String> ();
    	  ArrayList <String> ar2= new  ArrayList <String> ();
    	  ArrayList <String> ar= new  ArrayList <String> ();
    	for(int k=0;k<n;k++){
    		
    	Random random = new Random();

    	int lon=  Math.abs(random.nextInt()) % (180 - (-180) + 1)+ (-180);
        int lat =  Math.abs(random.nextInt()) % (90 - (-90) + 1)+ (-90);
       // System.out.println(lon+","+lat);
        ar.add(lon+","+lat+"");
        ArrayList <String> arrl = searchQuery("", "H:\\PicturesOFflickr_More\\10002328806.jpg",lon+"",lat+"");
        
    	//System.out.println(arrl);
    	//System.out.println(arrl.size());
    	
    	ar1.add(arrl.size()+"");
    	
    	
    	long endTime = System.currentTimeMillis();    //获取结束时间
    	//System.out.println("程序运行时间：" + (endTime - startTime) + "ms");    //输出程序运行时间
    	
        long t=endTime - startTime;
        
        ar2.add(t+"");
    	} 
    	  BufferedWriter bw = null;
    	 
    	   FileWriter fw = new FileWriter("C:\\Users\\huochairen\\Desktop\\time\\num4_2.txt", true);
    	    bw = new BufferedWriter(fw);
    	 
    	 for(int i=0;i<ar1.size();i++)
    	 {
    		  bw.write(ar1.get(i)+"\r\n");
    	 }
    	 BufferedWriter bw1 = null;
    	 FileWriter fw1 = new FileWriter("C:\\Users\\huochairen\\Desktop\\time\\coordinate4_2.txt", true);
    	 bw1 = new BufferedWriter(fw1);
    	 for(int i=0;i<ar.size();i++)
    	 {
    		  bw1.write(ar.get(i)+"\r\n");
    	 }
    	 BufferedWriter bw02 = null;
    	 FileWriter fw02 = new FileWriter("C:\\Users\\huochairen\\Desktop\\time\\time4_2.txt", true);
    	 bw02 = new BufferedWriter(fw02);
    	 for(int i=0;i<ar2.size();i++)
    	 {
    		  bw02.write(ar2.get(i)+"\r\n");
    	 }
    	 bw.close();
    	 bw1.close();
    	 bw02.close();
    	 System.out.println("finally------------");
	*/	
	/*	ArrayList <Integer> arr=new  ArrayList <Integer>();  
	 FileReader fr1 = new FileReader("C:\\Users\\huochairen\\Desktop\\time\\time1_1.txt");
	 BufferedReader br = new BufferedReader(fr1);
	 String row=null;
	 while ((row = br.readLine())!= null)
		{
			arr.add(Integer.parseInt(row));
		     
		}	
	 ArrayList <Integer> ar=new  ArrayList <Integer>();  
	 for(int i=1;i<arr.size();i++)
	 {
		 int num=arr.get(i)-arr.get(i-1);
		 ar.add(num);
	 }
	 for(int i=0;i<ar.size();i++)
	 {
		
		 System.out.println(ar.get(i));
	 }*/
	}

}

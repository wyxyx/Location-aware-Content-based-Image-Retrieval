package retrieve;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import javax.imageio.ImageIO;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import net.semanticmetadata.lire.DocumentBuilder;
import net.semanticmetadata.lire.imageanalysis.bovw.SurfFeatureHistogramBuilder;
import net.semanticmetadata.lire.impl.SurfDocumentBuilder;
import net.semanticmetadata.lire.utils.FileUtils;

public class createSURFindex {
	private static String INDEX_FILE_PATH = "C:\\Users\\huochairen\\Desktop\\pic1\\";//"H:\\PicturesOFflickr_More"; //要索引的图片文件目录 
	public static void main(String []args) throws IOException{
		indexFile();
	}
	private static void indexFile() throws IOException{
		// Checking if the file path is there and if it is a directory.
	    File f = new File(INDEX_FILE_PATH);
	    System.out.println("Indexing images in " + INDEX_FILE_PATH);
		DocumentBuilder builder = new SurfDocumentBuilder();
		// File pf=new File("index01");
		
		IndexWriterConfig cfg = new IndexWriterConfig(Version.LUCENE_42, new WhitespaceAnalyzer(Version.LUCENE_42));

		IndexWriter iw =new IndexWriter(FSDirectory.open(new File("newindex")),cfg);
		
		//IndexWriter iw = LuceneUtils.createIndexWriter("index01", true, LuceneUtils.AnalyzerType.WhitespaceAnalyzer);

	    ArrayList<File> images = FileUtils.getAllImageFiles(f, true);
		
	    for (Iterator<File> it = images.iterator(); it.hasNext(); ) {
	           File imageFilePath = it.next();
	           System.out.println("Indexing " + imageFilePath);
	           try {
	               BufferedImage img = ImageIO.read(imageFilePath);
	               Document document = builder.createDocument(img, imageFilePath.getPath());
	             //开始写入,就把文档写进了索引文件里了
	               iw.addDocument(builder.createDocument( ImageIO.read(imageFilePath), imageFilePath.getPath()));
	               document.add(new Field("id", img+"", Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.YES));  
	           } catch (Exception e) {
	               System.err.println("Error reading image or indexing it.");
	               e.printStackTrace();
	           }
	       }
	    // closing the IndexWriter
	       iw.close();
	       System.out.println("Finished indexing.");
	       
	      
	       
	       IndexReader ir=DirectoryReader.open(FSDirectory.open(new File("newindex")));
	       
	       SurfFeatureHistogramBuilder sfh=new SurfFeatureHistogramBuilder(ir,1000,500);
	       sfh.index();
	}
}

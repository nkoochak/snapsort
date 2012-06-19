import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;

public class ListingMatcher {
	ArrayList<Product> products = new ArrayList<Product>();
	ArrayList<CategorizedListing> categorizedListings = new ArrayList<CategorizedListing>();
	ArrayList<Result> results = new ArrayList<Result>();

	double errorTolerance = 0.01;

	public static void main(String[] args) {
		//----Input----: 1. Text file containing list of products (products.txt),
		//				 2. Text file containing list of prices (listings.txt)
		//----Output----: Text file containing matching results (result.txt)
		//----Process----: 1. Read products from file to arraylist, read price listings from file to categorized lists based on their manufacturer
		//				   2. matching Decision making process
		//				   3. Write result from arraylist to file
		//------------------------
		
		ListingMatcher matcher = new ListingMatcher();
		
		//1. Read products and price listings from text files and fill related arrayLists
		matcher.FillProductsFromFile();
		matcher.FillListingsFromFile();
		
		//2. For each product, scan all listing with the same manufacturer to find matched ones and store them in the results arrayList
		int numOfMatched;
		for (int i=0; i<matcher.products.size();i++){
			numOfMatched =0;
			int categoryIndex = -1;
			for (int j=0; j<matcher.categorizedListings.size();j++)
				if(matcher.categorizedListings.get(j).manufacturer.startsWith(matcher.products.get(i).manufacturer)){
					categoryIndex = j;
					break;
				}
			Result r = new Result(matcher.products.get(i).name);
			if(categoryIndex != -1){
				int[] sortedIndex = new int[matcher.categorizedListings.get(categoryIndex).listings.size()];
				//2.1. Use the static rules to find the matching relevancy of a price listing to the current product:
				for(int j=0; j<matcher.categorizedListings.get(categoryIndex).listings.size();j++){
					matcher.SetRelevancy(matcher.categorizedListings.get(categoryIndex).listings.get(j), matcher.products.get(i));
					for(int k=0;k<j;k++){
						if(matcher.categorizedListings.get(categoryIndex).listings.get(sortedIndex[k]).relevancy < matcher.categorizedListings.get(categoryIndex).listings.get(j).relevancy){
							for(int l=j;l>k;l--)
								sortedIndex[l]=sortedIndex[l-1];
							sortedIndex[k] = j;
							break;
						}
						sortedIndex[j] = j;
					}
				}

				//2.2. starting from the beginning of the sorted list, measure the risk of adding a price listing 
				//for the current product and decide whether to add it or not:
				for(int j=0; j<matcher.categorizedListings.get(categoryIndex).listings.size();j++){
					if(matcher.DecisionMaker(matcher.categorizedListings.get(categoryIndex).listings.get(sortedIndex[j]),numOfMatched)){
						numOfMatched++;
						r.listings.add(matcher.categorizedListings.get(categoryIndex).listings.get(sortedIndex[j]));
					}
				}
			}
			matcher.results.add(r);
		}
		
		//3. Store the results in a text file
		matcher.WriteResultsToFile();
	}

	private void SetRelevancy(Listing l, Product p){
		//----Input----: 1. A price listing,
		//				 2. A product	
		//----Output----: set relevancy of the listing to the product (a real number in range of [0,1])	
		//----Process----: Check static rules
		//------------------------
		
		//Case 1: manufacturers are not the same
		if(!l.manufacturer.startsWith(p.manufacturer))
			l.relevancy = 0;
		//Case 2: manufacturers are the same, but listing does not contain product model
		else if(l.title.indexOf(" "+p.model+" ") == -1){
			if(p.ContainModelParts(l.title)==1)
				l.relevancy = 0.9;
			else if(p.ContainModelParts(l.title)==2)
				l.relevancy = 0.2;
			else
				l.relevancy = 0;
		}
		//Case 3: manufacturers are the same, and listing contains product model, but listing contains keyword "for"
		else if(l.title.indexOf(" for ") != -1){
			if(l.title.indexOf(" for ") < l.title.lastIndexOf(p.model))
				l.relevancy = 0;
			else if(l.title.indexOf(" for ") < l.title.lastIndexOf(p.ShortModel()))
				l.relevancy = 0.1;
			else
				l.relevancy = 0.5;
		}
		//Case 4: manufacturers are the same, listing contains product model, and listing does not contain keyword "for"
		else
			l.relevancy = 1;
	}

	private boolean DecisionMaker(Listing l, int numOfMatchedSoFar){
		//----Input----: 1. Relevancy of a listing to a product
		//				 2. Number of reported items so far
		//----Output----: True/False based on relevancy, Expected Monetary Value and errorTolerance threshold
		//----Process----: Measure Expected Monetary Value and compare it with the threshold
		//------------------------
		
		double ExpectedMonetaryValue = (1-l.relevancy) * (double)((double)1/(double)(numOfMatchedSoFar+1));
		if(l.relevancy == 1)
			return true;
		else if (l.relevancy == 0)
			return false;
		else{		
			if (ExpectedMonetaryValue <= errorTolerance)
				return true;
		}
		return false;
	}
	
	private void FillProductsFromFile(){
		//----Input----: Text file (products.txt)
		//----Output----: Filled product array list
		//----Process----: Retrieve information from the file and fill the array list
		//------------------------
		
		try{
			FileInputStream fstream = new FileInputStream("products.txt");
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			while ((strLine = br.readLine()) != null){
				Product p = new Product();			
				p.ToProduct(strLine);
				products.add(p);
			}
			in.close();
		}
		catch (Exception e){
			System.err.println("Error: " + e.getMessage());
		}
	}

	private void FillListingsFromFile(){
		//----Input----: Text file (listing.txt)
		//----Output----: Filled categorized listing array list
		//----Process----: Retrieve information from the file and categorize them into sublists based on their manufacturer, and fill related arraylist
		//------------------------
		try{
			FileInputStream fstream = new FileInputStream("listings.txt");
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			while ((strLine = br.readLine()) != null){
				Listing l = new Listing();
				l.ToListing(strLine);
				int categoryIndex = -1;
				for (int i=0; i<categorizedListings.size();i++)
					if(categorizedListings.get(i).manufacturer.compareTo(l.ManufacturerStartWord())==0){
						categoryIndex = i;
						break;
					}
				if(categoryIndex != -1)
					categorizedListings.get(categoryIndex).listings.add(l);
				else{
					CategorizedListing cl = new CategorizedListing();
					cl.manufacturer = l.ManufacturerStartWord();
					cl.listings.add(l);
					categorizedListings.add(cl);
				}
			}
			in.close();
		}
		catch (Exception e){
			System.err.println("Error: " + e.getMessage());
		}
	}

	private void WriteResultsToFile(){
		try{
			File LogFile = new File("result.txt");
			PrintWriter pw = new PrintWriter(new FileWriter(LogFile));
			for(int i=0;i<results.size();i++)
				pw.println(results.get(i).ToString());
			pw.close();
		}
		catch (Exception e){
			System.err.println("Error: " + e.getMessage());
		}
	}
}

class Product {
	String name = "";
	String manufacturer = "";
	String model = "";
	String family = "";
	String announced_date = "";

	public void ToProduct(String str){
		if(str.startsWith("{\"product_name\":\"")){
			name = str.substring(str.indexOf("{\"product_name\":\"")+17,str.indexOf("\",\"")).toLowerCase();
			str = str.substring(str.indexOf("\",\"")+2);
		}
		if(str.startsWith("\"manufacturer\":\"")){
			manufacturer = str.substring(str.indexOf("\"manufacturer\":\"")+16, str.indexOf("\",\"")).toLowerCase();
			str = str.substring(str.indexOf("\",\"")+2);
		}
		if(str.startsWith("\"model\":\"")){
			model = str.substring(str.indexOf("\"model\":\"")+9, str.indexOf("\",\"")).toLowerCase();
			str = str.substring(str.indexOf("\",\"")+2);
		}
		if(str.startsWith("\"family\":\"")){
			family = str.substring(str.indexOf("\"family\":\"")+10, str.indexOf("\",\"")).toLowerCase();
			str = str.substring(str.indexOf("\",\"")+2);
		}
		if(str.startsWith("\"announced-date\":\"")){
			announced_date = str.substring(str.indexOf("\"announced-date\":\"")+18, str.indexOf("\"}")).toLowerCase();
		}
	}

	public String ShortModel(){
		int lastIndex = model.lastIndexOf(" ");
		if(model.lastIndexOf("-")>lastIndex)
			lastIndex = model.lastIndexOf("-");
		if(model.lastIndexOf("_")>lastIndex)
			lastIndex = model.lastIndexOf("_");
		return model.substring(lastIndex+1);
	}

	public int ContainModelParts(String str){
		//return 0 if str does not contain models part, 
		//return 1 if str contains models part, 
		//return 2 if at least one model part is just number and str contains all model parts
		int result = 1;
		String m = model;
		boolean isNumberic = false;
		int lastIndex = 0;
		int separatorCount =0;
		while(lastIndex != -1){
			lastIndex = m.indexOf(" ",lastIndex+1);
			if( lastIndex != -1)
				separatorCount ++;
		}
		lastIndex = 0;
		while(lastIndex != -1){
			lastIndex = m.indexOf("_",lastIndex+1);
			if( lastIndex != -1)
				separatorCount ++;
		}
		lastIndex = 0;
		while(lastIndex != -1){
			lastIndex = m.indexOf("-",lastIndex+1);
			if( lastIndex != -1)
				separatorCount ++;
		}

		if (separatorCount==0)
			return 0;
		else{
			for(int i=0;i<=separatorCount;i++){
				int seperatorIndex = 100;
				if (m.indexOf(" ") != -1)
					seperatorIndex = m.indexOf(" ");
				if(m.indexOf("-") != -1)
					if(m.indexOf("-") <= seperatorIndex)
						seperatorIndex = m.indexOf("-");
				if(m.indexOf("_") != -1)
					if(m.indexOf("_") <= seperatorIndex)
						seperatorIndex = m.indexOf("_");
				String modelpart = "";
				if(seperatorIndex != 100){
					modelpart = m.substring(0, seperatorIndex).trim();
					if(isInteger(modelpart))
						isNumberic = true;
					if(str.indexOf(" "+modelpart+" ") == -1){
						result = 0;
						break;
					}
					m = m.replace(modelpart, "").trim().replace("_", "").replace("-", "");
				}
				else{
					modelpart = m.trim();
					if(isInteger(modelpart))
						isNumberic = true;
					if(str.indexOf(" "+modelpart+" ") == -1){
						result = 0;
						break;
					}
				}
			}
			if (result==1 && isNumberic)
				result = 2;
			return result;
		}

	}
	public boolean isInteger(String str) {
	    try {
	        Integer.parseInt(str);
	        return true;
	    } catch (NumberFormatException nfe) {}
	    return false;
	}
}

class Listing {
	String title;
	String manufacturer;
	String currency;
	String price;

	double relevancy;

	public void ToListing(String str){
		if(str.startsWith("{\"title\":\"")){
			title = str.substring(str.indexOf("{\"title\":\"")+10,str.indexOf("\",\"")).toLowerCase();
			str = str.substring(str.indexOf("\",\"")+2);
		}
		if(str.startsWith("\"manufacturer\":\"")){
			manufacturer = str.substring(str.indexOf("\"manufacturer\":\"")+16, str.indexOf("\",\"")).toLowerCase();
			str = str.substring(str.indexOf("\",\"")+2);
		}
		if(str.startsWith("\"currency\":\"")){
			currency = str.substring(str.indexOf("\"currency\":\"")+12, str.indexOf("\",\"")).toLowerCase();
			str = str.substring(str.indexOf("\",\"")+2);
		}
		if(str.startsWith("\"price\":\"")){
			price = str.substring(str.indexOf("\"price\":\"")+9, str.indexOf("\"}")).toLowerCase();
		}
	}

	public String ToString(){
		String str ="{\"title\":\"" +	title + "\"," +
				"\"manufacturer\":\"" + manufacturer + "\"," +
				"\"currency\":\"" + currency + "\"," +
				"\"price\":\"" + price + "\"}";
		return str;
	}

	public String ManufacturerStartWord(){
		if(manufacturer.indexOf(" ") != -1)
			return manufacturer.substring(0, manufacturer.indexOf(" ")).trim();
		return manufacturer;
	}
}

class CategorizedListing {
	String manufacturer;
	ArrayList<Listing> listings = new ArrayList<Listing>();
}

class Result {
	String product_name;
	ArrayList<Listing> listings = new ArrayList<Listing>();

	public Result(String name){
		product_name = name;
	}
	public String ToString(){
		String str ="{\"product_name\":\"" + product_name;
		str+="[";
		for(int i=0;i<listings.size();i++){
			str+=listings.get(i).ToString();
			if(i!=listings.size()-1)
				str+=", ";
		}
		str+="]";
		return str;
	}
}


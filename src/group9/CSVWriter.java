package group9;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

public class CSVWriter {

	//Delimiter used in CSV file
	private static final String COMMA_DELIMITER = ",";
	private static final String NEW_LINE_SEPARATOR = "\n";

	private FileWriter fw = null;
	private HashMap<String, ArrayList<Double>> ofs = new HashMap<String, ArrayList<Double>>();
	
	public void addOf(String a, double d)
	{
		if(ofs.get(a) == null)
		{
			ofs.put(a, new ArrayList<Double>());
		}
		ofs.get(a).add(d);
	}
	
	public void startWriting(String fileName)
	{
		try {
			fw = new FileWriter(fileName, true);
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	public void writeData()
	{
		double dataSize = 0;
		
		// compute the means
		HashMap<String, Double> means = new HashMap<String, Double>();
		for(Entry<String, ArrayList<Double>> e : ofs.entrySet())
		{
			double sum = 0;
			for(Double v : e.getValue())
			{
				sum += v;
			}
			means.put(e.getKey(), sum / e.getValue().size());
			dataSize = e.getValue().size();
		}
		
		// compute the standard deviations
		HashMap<String, Double> stds = new HashMap<String, Double>();
		for(Entry<String, ArrayList<Double>> e : ofs.entrySet())
		{
			double mean = means.get(e.getKey());
			double sum = 0;
			for(Double v : e.getValue())
			{
				sum += (v - mean)*(v - mean);
			}
			stds.put(e.getKey(), sum / e.getValue().size());
		}
		
		try {
			for(String a : ofs.keySet())
			{
				fw.append(a);
				fw.append(COMMA_DELIMITER);
				fw.append(String.valueOf(means.get(a)));
				fw.append(COMMA_DELIMITER);
				fw.append(String.valueOf(stds.get(a)));
				fw.append(COMMA_DELIMITER);
			}
			fw.append(String.valueOf(dataSize));
			fw.append(NEW_LINE_SEPARATOR);
		}catch(IOException ioe) {};
	}
	
	public void stopWriting(String fileName)
	{
		try {
			fw.close();
		} catch (IOException e) {}
	}
	
}

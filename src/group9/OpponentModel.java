package group9;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import negotiator.AgentID;
import negotiator.Bid;
import negotiator.issue.IssueDiscrete;
import negotiator.issue.ValueDiscrete;

public class OpponentModel {

	private ArrayList<IssueDiscrete> issues;
	private HashMap<IssueDiscrete, HashMap<ValueDiscrete, HashMap<AgentID, Double>>> opponentPrefs;
	private HashMap<IssueDiscrete, HashMap<ValueDiscrete, HashMap<AgentID, Double>>> valuePrefs;
	private HashMap<IssueDiscrete, HashMap<AgentID, Double>> issuePrefs;
	
	
	public OpponentModel(ArrayList<IssueDiscrete> issues) {
		
		this.issues = issues;
		opponentPrefs = new HashMap<IssueDiscrete, HashMap<ValueDiscrete, HashMap<AgentID, Double>>>();
		valuePrefs = new HashMap<IssueDiscrete, HashMap<ValueDiscrete, HashMap<AgentID, Double>>>();
		issuePrefs = new HashMap<IssueDiscrete, HashMap<AgentID, Double>>();
		
		for(IssueDiscrete i : issues)
		{
			HashMap<ValueDiscrete, HashMap<AgentID, Double>> opponentMap = new HashMap<ValueDiscrete, HashMap<AgentID, Double>>();
			HashMap<ValueDiscrete, HashMap<AgentID, Double>> valueMap = new HashMap<ValueDiscrete, HashMap<AgentID, Double>>(); 
			for(ValueDiscrete v : i.getValues())
			{
				opponentMap.put(v, new HashMap<AgentID, Double>());
				valueMap.put(v, new HashMap<AgentID, Double>());
			}
			opponentPrefs.put(i, opponentMap);
			valuePrefs.put(i, valueMap);
			issuePrefs.put(i, new HashMap<AgentID, Double>());
		}	
		
	}
	
	
	public void updateFrequencies(Bid bid, AgentID agent, double frequencyWeight) {
		for(IssueDiscrete i : issues)
		{
			ValueDiscrete v = (ValueDiscrete)bid.getValue(i.getNumber());	
			Double currentFrequency = opponentPrefs.get(i).get(v).get(agent);
			Double newFrequency;
			try {
				newFrequency = new Double(currentFrequency.doubleValue() + frequencyWeight);
				opponentPrefs.get(i).get(v).replace(agent, newFrequency);
			} catch(NullPointerException npe) {
				newFrequency = new Double(frequencyWeight);
				opponentPrefs.get(i).get(v).put(agent, newFrequency);
			}
		}
	}
	
	
	public void updatePreferences() {
		
		HashMap<AgentID, Double> issueSumFreq = new HashMap<AgentID, Double>();
		HashMap<IssueDiscrete, HashMap<AgentID, Double>> issueMaxFreq = new HashMap<IssueDiscrete, HashMap<AgentID, Double>>();
		
		for(IssueDiscrete i : issues)
		{
			issueMaxFreq.put(i, new HashMap<AgentID, Double>());
			
			// find sums and maximums
			
			HashMap<AgentID, Double> valueSumFreq = new HashMap<AgentID, Double>();
			for(ValueDiscrete v : opponentPrefs.get(i).keySet())
			{
				for(AgentID a : opponentPrefs.get(i).get(v).keySet())
				{
					if(issueSumFreq.get(a) == null)
					{
						issueSumFreq.put(a, new Double(0)); // just for initializing purposes
					}
					Double currentFreq = valueSumFreq.get(a);
					
					if(currentFreq == null)
					{
						valueSumFreq.put(a, opponentPrefs.get(i).get(v).get(a));
						issueMaxFreq.get(i).put(a, opponentPrefs.get(i).get(v).get(a));
					}
					else
					{
						valueSumFreq.replace(a, new Double(currentFreq + opponentPrefs.get(i).get(v).get(a)));
						if(opponentPrefs.get(i).get(v).get(a).doubleValue() > issueMaxFreq.get(i).get(a).doubleValue())
						{
							issueMaxFreq.get(i).replace(a, opponentPrefs.get(i).get(v).get(a));
						}
					}
					
				}
			}
			for(AgentID a : issueSumFreq.keySet())
			{
				issueSumFreq.replace(a, issueSumFreq.get(a) + issueMaxFreq.get(i).get(a));
			}
			
			
			// normalize on values
			
			for(ValueDiscrete v : opponentPrefs.get(i).keySet())
			{
				for(AgentID a : opponentPrefs.get(i).get(v).keySet())
				{
					valuePrefs.get(i).get(v).put(a, new Double(0));
					valuePrefs.get(i).get(v).replace(a, opponentPrefs.get(i).get(v).get(a) / valueSumFreq.get(a));
				}
			}
			
		}
		
		// normalize on issues
		
		for(IssueDiscrete i : issues)
		{
			for(AgentID a : issueSumFreq.keySet())
			{
				issuePrefs.get(i).put(a, new Double(0));
				issuePrefs.get(i).replace(a, issueMaxFreq.get(i).get(a) / issueSumFreq.get(a));
			}
		}
			
	}


	public HashMap<IssueDiscrete, HashMap<ValueDiscrete, HashMap<AgentID, Double>>> getValuePrefs() {
		return valuePrefs;
	}


	public HashMap<IssueDiscrete, HashMap<AgentID, Double>> getIssuePrefs() {
		return issuePrefs;
	}
	
	
	public void printIssuePreferences() {
		System.out.println("Issue Preferences:");
		for(IssueDiscrete i : issues)
		{
			System.out.printf("%15s : ", i.getName());
			for(Entry<AgentID, Double> e : issuePrefs.get(i).entrySet())
			{
				System.out.printf("    %25s = %.3f    ", e.getKey(), e.getValue());
			}
			System.out.println();
		}
//		try {
//			Thread.sleep(1000);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
	}
	
	public void printValuePreferences() {
		System.out.println("Value Preferences:");
		for(IssueDiscrete i : issues)
		{
			System.out.printf("%15s : ", i.getName());
			for(ValueDiscrete v : i.getValues())
			{
				System.out.printf("    %15s : ", v.getValue());
				for(Entry<AgentID, Double> e : valuePrefs.get(i).get(v).entrySet())
				{
					System.out.printf("    %25s = %.3f    ", e.getKey(), e.getValue());
				}
				System.out.println();
			}
		}
//		try {
//			Thread.sleep(1000);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
	}


}
